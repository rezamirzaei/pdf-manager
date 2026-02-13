package com.rezami.pdfmanager.ocr;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * PDF text extractor using Apache PDFBox.
 *
 * PDFBox handles text extraction from:
 * - Native text-based PDFs
 * - PDFs with embedded fonts
 * - PDFs with text layers (common in scanned documents that have been OCR'd)
 *
 * For PDFs that are purely scanned images without text layers, the extraction
 * will return empty, and the file will be skipped or handled by fallback strategies.
 */
public final class PdfBoxTextExtractor implements PdfTextExtractor {

    private static final int MIN_TEXT_LENGTH_THRESHOLD = 50;

    public PdfBoxTextExtractor() {
        // No external dependencies needed - PDFBox handles everything
    }

    @Override
    public Optional<String> extractText(Path pdfPath, int maxCharacters) throws IOException {
        return extractTextFromPage(pdfPath, 0, maxCharacters);
    }

    @Override
    public Optional<String> extractTextFromPage(Path pdfPath, int pageNumber, int maxCharacters) throws IOException {
        Objects.requireNonNull(pdfPath, "pdfPath");
        if (!Files.isRegularFile(pdfPath)) {
            throw new IOException("Not a file: " + pdfPath);
        }
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must be non-negative");
        }
        if (maxCharacters <= 0) {
            throw new IllegalArgumentException("maxCharacters must be positive");
        }

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (pageNumber >= document.getNumberOfPages()) {
                return Optional.empty();
            }

            return extractTextWithPdfBox(document, pageNumber, maxCharacters);
        }
    }

    private Optional<String> extractTextWithPdfBox(PDDocument document, int pageNumber, int maxCharacters)
            throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageNumber + 1);  // PDFTextStripper uses 1-based indexing
        stripper.setEndPage(pageNumber + 1);

        // Configure for better text extraction
        stripper.setSortByPosition(true);  // Sort text by position on page
        stripper.setAddMoreFormatting(true);  // Add extra formatting for readability

        String text = stripper.getText(document);
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalizeText(text);

        // Check if we got meaningful text
        if (normalized.length() < MIN_TEXT_LENGTH_THRESHOLD) {
            // Try extracting from multiple pages if first page has little content
            return tryExtractFromMultiplePages(document, maxCharacters);
        }

        if (normalized.length() > maxCharacters) {
            normalized = normalized.substring(0, maxCharacters);
        }

        return Optional.of(normalized);
    }

    /**
     * Try extracting text from the first few pages if single page extraction fails.
     * This helps with documents that have cover pages or tables of contents.
     */
    private Optional<String> tryExtractFromMultiplePages(PDDocument document, int maxCharacters)
            throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);

        int maxPages = Math.min(3, document.getNumberOfPages());
        stripper.setStartPage(1);
        stripper.setEndPage(maxPages);

        String text = stripper.getText(document);
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalizeText(text);
        if (normalized.length() > maxCharacters) {
            normalized = normalized.substring(0, maxCharacters);
        }

        return normalized.length() >= MIN_TEXT_LENGTH_THRESHOLD
                ? Optional.of(normalized)
                : Optional.empty();
    }

    private static String normalizeText(String text) {
        return text
                .replaceAll("\\r\\n|\\r", "\n")  // Normalize line endings
                .replaceAll("\\n{3,}", "\n\n")   // Collapse multiple blank lines
                .replaceAll("[ \\t]+", " ")       // Collapse horizontal whitespace
                .trim();
    }
}

