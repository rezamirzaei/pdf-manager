package com.rezami.pdfmanager.ocr;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Strategy interface for extracting text from PDF files.
 * Implementations can use different methods: direct text extraction, OCR, or hybrid approaches.
 */
public interface PdfTextExtractor {

    /**
     * Extracts text content from the first page (or specified number of characters) of a PDF.
     *
     * @param pdfPath path to the PDF file
     * @param maxCharacters maximum number of characters to extract
     * @return extracted text content, or empty if extraction fails
     * @throws IOException if reading the PDF fails
     */
    Optional<String> extractText(Path pdfPath, int maxCharacters) throws IOException;

    /**
     * Extracts text content from a specific page of a PDF.
     *
     * @param pdfPath path to the PDF file
     * @param pageNumber zero-based page number
     * @param maxCharacters maximum number of characters to extract
     * @return extracted text content, or empty if extraction fails
     * @throws IOException if reading the PDF fails
     */
    Optional<String> extractTextFromPage(Path pdfPath, int pageNumber, int maxCharacters) throws IOException;
}

