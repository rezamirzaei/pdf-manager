package com.rezami.pdfmanager.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class FileNameSanitizer {
    private static final int MAX_BASE_NAME_LENGTH = 180;

    private static final Set<String> WINDOWS_RESERVED_NAMES =
            Set.of(
                    "con",
                    "prn",
                    "aux",
                    "nul",
                    "com1",
                    "com2",
                    "com3",
                    "com4",
                    "com5",
                    "com6",
                    "com7",
                    "com8",
                    "com9",
                    "lpt1",
                    "lpt2",
                    "lpt3",
                    "lpt4",
                    "lpt5",
                    "lpt6",
                    "lpt7",
                    "lpt8",
                    "lpt9");

    public String sanitizeBaseName(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);
        normalized = normalized.replaceAll("\\s+", " ").trim();

        normalized = replaceIllegalFileNameChars(normalized);
        normalized = normalized.replaceAll("\\s+", " ").trim();

        normalized = stripTrailingDotsAndSpaces(normalized);
        normalized = normalized.trim();

        if (normalized.isEmpty()) {
            return "";
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (WINDOWS_RESERVED_NAMES.contains(lower)) {
            normalized = normalized + "_";
        }

        if (normalized.length() > MAX_BASE_NAME_LENGTH) {
            normalized = normalized.substring(0, MAX_BASE_NAME_LENGTH).trim();
            normalized = stripTrailingDotsAndSpaces(normalized);
        }

        return normalized;
    }

    private static String replaceIllegalFileNameChars(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (isIllegal(c)) {
                out.append(' ');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean isIllegal(char c) {
        return c <= 31 || c == 127 || c == '\\' || c == '/' || c == ':' || c == '*'
                || c == '?' || c == '"' || c == '<' || c == '>' || c == '|';
    }

    private static String stripTrailingDotsAndSpaces(String input) {
        int end = input.length();
        while (end > 0) {
            char c = input.charAt(end - 1);
            if (c == '.' || c == ' ') {
                end--;
            } else {
                break;
            }
        }
        return input.substring(0, end);
    }
}

