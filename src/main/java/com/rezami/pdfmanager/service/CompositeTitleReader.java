package com.rezami.pdfmanager.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Composite PDF title reader that tries multiple strategies in order.
 *
 * Default order:
 * 1. Try LLM-based title generation (using OCR/text extraction)
 * 2. Fall back to PDF metadata (Document Information, XMP)
 *
 * This provides the best results: AI-generated titles for scanned documents
 * or documents without metadata, while still using embedded titles when available.
 */
public final class CompositeTitleReader implements PdfTitleReader {

    private final PdfTitleReader primary;
    private final PdfTitleReader fallback;
    private final boolean preferMetadataIfAvailable;

    /**
     * Creates a composite reader that tries primary first, then fallback.
     */
    public CompositeTitleReader(PdfTitleReader primary, PdfTitleReader fallback) {
        this(primary, fallback, false);
    }

    /**
     * Creates a composite reader.
     *
     * @param primary the primary title reader (e.g., LLM-based)
     * @param fallback the fallback reader (e.g., metadata-based)
     * @param preferMetadataIfAvailable if true, try fallback first and use primary only if fallback fails
     */
    public CompositeTitleReader(PdfTitleReader primary, PdfTitleReader fallback,
                                 boolean preferMetadataIfAvailable) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.preferMetadataIfAvailable = preferMetadataIfAvailable;
    }

    @Override
    public Optional<String> readTitle(Path pdfPath) throws IOException {
        Objects.requireNonNull(pdfPath, "pdfPath");

        PdfTitleReader first = preferMetadataIfAvailable ? fallback : primary;
        PdfTitleReader second = preferMetadataIfAvailable ? primary : fallback;

        // Try first reader
        try {
            Optional<String> title = first.readTitle(pdfPath);
            if (title.isPresent() && !title.get().isBlank()) {
                return title;
            }
        } catch (IOException e) {
            // Log and continue to fallback
            System.err.println("Primary reader failed for " + pdfPath + ": " + e.getMessage());
        }

        // Try second reader
        return second.readTitle(pdfPath);
    }

    /**
     * Creates a composite reader that prefers LLM over metadata.
     */
    public static CompositeTitleReader llmFirst(LlmTitleReader llmReader, PdfBoxTitleReader metadataReader) {
        return new CompositeTitleReader(llmReader, metadataReader, false);
    }

    /**
     * Creates a composite reader that prefers metadata over LLM.
     * LLM is only used when metadata is missing.
     */
    public static CompositeTitleReader metadataFirst(PdfBoxTitleReader metadataReader, LlmTitleReader llmReader) {
        return new CompositeTitleReader(llmReader, metadataReader, true);
    }
}

