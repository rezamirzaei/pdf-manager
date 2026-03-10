package com.rezami.pdfmanager.llm;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

final class TitleGenerationSupport {
    private static final int MAX_PROMPT_CHARS = 800;
    private static final int MIN_OUTPUT_TOKENS = 8;
    private static final int MAX_OUTPUT_TOKENS = 32;
    private static final Pattern LEADING_LABEL = Pattern.compile(
            "(?i)^(title|document title|filename|suggested title|answer)\\s*[:\\-]+\\s*");

    private TitleGenerationSupport() {}

    static String buildTitlePrompt(String textContent, int maxTitleLength) {
        Objects.requireNonNull(textContent, "textContent");
        if (maxTitleLength <= 0) {
            throw new IllegalArgumentException("maxTitleLength must be positive");
        }

        String truncatedContent = textContent.length() > MAX_PROMPT_CHARS
                ? textContent.substring(0, MAX_PROMPT_CHARS) + "..."
                : textContent;

        return """
                Extract the best document title from this PDF text.
                Rules:
                - return only the title
                - maximum %d characters
                - no quotes
                - prefer the actual document or paper title
                - ignore author names, affiliations, journal headers, abstract labels, keywords, DOI, URLs, and page numbers

                PDF text:
                %s

                Title:
                """.formatted(maxTitleLength, truncatedContent);
    }

    static int outputTokenLimit(int maxTitleLength) {
        int estimatedTokens = Math.max(MIN_OUTPUT_TOKENS, maxTitleLength / 4);
        return Math.min(estimatedTokens, MAX_OUTPUT_TOKENS);
    }

    static Optional<String> sanitizeTitle(String title, int maxLength) {
        Objects.requireNonNull(title, "title");
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive");
        }

        String normalized = title
                .replace("\\r", "\r")
                .replace("\\n", "\n");

        String firstLine = normalized.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("")
                .trim();

        String cleaned = firstLine.replaceAll("^[\\p{Punct}\\s]+", "").trim();
        cleaned = LEADING_LABEL.matcher(cleaned).replaceFirst("");
        cleaned = cleaned
                .replaceAll("^\\p{Punct}+|\\p{Punct}+$", "")
                .replaceAll("[\"'`]", "")
                .replaceAll("[:/\\\\*?<>|]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isBlank()) {
            return Optional.empty();
        }

        if (cleaned.length() <= maxLength) {
            return Optional.of(cleaned);
        }

        int lastSpace = cleaned.lastIndexOf(' ', maxLength - 3);
        if (lastSpace > maxLength / 2) {
            return Optional.of(cleaned.substring(0, lastSpace).trim());
        }

        return Optional.of(cleaned.substring(0, maxLength - 3).trim());
    }
}
