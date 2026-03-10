package com.rezami.pdfmanager.service;

import java.util.Objects;

final class TitleTextPreprocessor {
    private TitleTextPreprocessor() {}

    static String prepare(String rawText) {
        Objects.requireNonNull(rawText, "rawText");

        String normalized = rawText
                .replaceAll("\\r\\n|\\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .trim();

        String withoutJournalHeader = normalized.replaceFirst(
                "(?is)^[A-Z0-9][A-Z0-9\\s,.-]{20,}?\\d{4}\\s+\\d+\\s+",
                "");

        return withoutJournalHeader.replaceFirst("(?is)\\babstract\\b.*$", "").trim();
    }
}
