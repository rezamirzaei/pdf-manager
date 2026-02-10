package com.rezami.pdfmanager.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;

public final class PdfBoxTitleReader implements PdfTitleReader {
    @Override
    public Optional<String> readTitle(Path pdfPath) throws IOException {
        if (pdfPath == null) {
            throw new IllegalArgumentException("pdfPath must not be null");
        }

        if (!Files.isRegularFile(pdfPath)) {
            throw new IOException("Not a file: " + pdfPath);
        }

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            String title = nullSafeTrimToNull(document.getDocumentInformation().getTitle());
            if (title == null) {
                title = readXmpTitle(document).orElse(null);
            }
            return Optional.ofNullable(title).map(PdfBoxTitleReader::normalizeWhitespace);
        }
    }

    private static Optional<String> readXmpTitle(PDDocument document) {
        PDMetadata metadata = document.getDocumentCatalog().getMetadata();
        if (metadata == null) {
            return Optional.empty();
        }

        try (InputStream inputStream = metadata.createInputStream()) {
            XMPMetadata xmpMetadata = new DomXmpParser().parse(inputStream);
            DublinCoreSchema dc = xmpMetadata.getDublinCoreSchema();
            if (dc == null) {
                return Optional.empty();
            }

            String title = nullSafeTrimToNull(dc.getTitle());
            if (title == null) {
                return Optional.empty();
            }
            return Optional.of(title);
        } catch (IOException | BadFieldValueException | XmpParsingException e) {
            return Optional.empty();
        }
    }

    private static String normalizeWhitespace(String input) {
        return input.trim().replaceAll("\\s+", " ");
    }

    private static String nullSafeTrimToNull(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
