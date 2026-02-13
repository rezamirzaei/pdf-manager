package com.rezami.pdfmanager.service;

import com.rezami.pdfmanager.llm.LlmClient;
import com.rezami.pdfmanager.ocr.PdfTextExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * PDF title reader that uses OCR/text extraction combined with an LLM to generate
 * meaningful titles for PDF documents.
 *
 * This implementation:
 * 1. Extracts text from the first page of the PDF (using OCR if needed)
 * 2. Sends the text to an LLM to generate a concise, descriptive title
 *
 * Implements the Strategy pattern - can be swapped with PdfBoxTitleReader
 * based on configuration or user preference.
 */
public final class LlmTitleReader implements PdfTitleReader {

    private static final int DEFAULT_MAX_TITLE_LENGTH = 100;
    private static final int DEFAULT_MAX_TEXT_CHARS = 2000;

    private final PdfTextExtractor textExtractor;
    private final LlmClient llmClient;
    private final int maxTitleLength;
    private final int maxTextChars;

    public LlmTitleReader(PdfTextExtractor textExtractor, LlmClient llmClient) {
        this(textExtractor, llmClient, DEFAULT_MAX_TITLE_LENGTH, DEFAULT_MAX_TEXT_CHARS);
    }

    public LlmTitleReader(PdfTextExtractor textExtractor, LlmClient llmClient,
                          int maxTitleLength, int maxTextChars) {
        this.textExtractor = Objects.requireNonNull(textExtractor, "textExtractor");
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");

        if (maxTitleLength <= 0) {
            throw new IllegalArgumentException("maxTitleLength must be positive");
        }
        if (maxTextChars <= 0) {
            throw new IllegalArgumentException("maxTextChars must be positive");
        }

        this.maxTitleLength = maxTitleLength;
        this.maxTextChars = maxTextChars;
    }

    @Override
    public Optional<String> readTitle(Path pdfPath) throws IOException {
        Objects.requireNonNull(pdfPath, "pdfPath");

        if (!Files.isRegularFile(pdfPath)) {
            throw new IOException("Not a file: " + pdfPath);
        }

        // Extract text from the first page
        Optional<String> extractedText = textExtractor.extractText(pdfPath, maxTextChars);

        if (extractedText.isEmpty() || extractedText.get().isBlank()) {
            return Optional.empty();
        }

        String llmInput = prepareLlmInput(extractedText.get());
        if (llmInput.isBlank()) {
            return Optional.empty();
        }

        // Use LLM to generate a title from the extracted text
        return llmClient.generateTitle(llmInput, maxTitleLength);
    }

    /**
     * Checks if the LLM service is available.
     * Useful for graceful degradation or fallback to metadata-based title reading.
     */
    public boolean isLlmAvailable() {
        return llmClient.isAvailable();
    }

    /**
     * Returns the name of the LLM model being used.
     */
    public String getModelName() {
        return llmClient.getModelName();
    }

    public int getMaxTitleLength() {
        return maxTitleLength;
    }

    public int getMaxTextChars() {
        return maxTextChars;
    }

    private static String prepareLlmInput(String rawText) {
        String normalized = rawText
                .replaceAll("\\r\\n|\\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .trim();

        String withoutJournalHeader = normalized.replaceFirst(
                "(?is)^[A-Z0-9][A-Z0-9\\s,.-]{20,}?\\d{4}\\s+\\d+\\s+",
                "");

        String beforeAbstract = withoutJournalHeader.replaceFirst("(?is)\\babstract\\b.*$", "");
        return beforeAbstract.trim();
    }
}
