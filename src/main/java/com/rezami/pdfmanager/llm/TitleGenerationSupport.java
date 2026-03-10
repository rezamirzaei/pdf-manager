package com.rezami.pdfmanager.llm;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

        String bounded = cleaned;
        if (bounded.length() > maxLength) {
            int lastSpace = bounded.lastIndexOf(' ', maxLength - 3);
            if (lastSpace > maxLength / 2) {
                bounded = bounded.substring(0, lastSpace).trim();
            } else {
                bounded = bounded.substring(0, maxLength - 3).trim();
            }
        }

        return looksPlausibleTitle(bounded) ? Optional.of(bounded) : Optional.empty();
    }

    private static boolean looksPlausibleTitle(String candidate) {
        if (candidate.isBlank()) {
            return false;
        }
        if (candidate.chars().allMatch(ch -> Character.isDigit(ch) || Character.isWhitespace(ch))) {
            return false;
        }
        if (longestRepeatedRun(candidate) >= 5) {
            return false;
        }

        long letters = candidate.chars().filter(Character::isLetter).count();
        long digits = candidate.chars().filter(Character::isDigit).count();
        if (letters < 3) {
            return false;
        }

        double letterRatio = (double) letters / candidate.length();
        double digitRatio = (double) digits / candidate.length();
        if (letterRatio < 0.35 || digitRatio > 0.35) {
            return false;
        }

        String[] words = candidate.trim().split("\\s+");
        if (words.length >= 5) {
            Set<String> uniqueWords = new HashSet<>();
            for (String word : words) {
                uniqueWords.add(word.toLowerCase());
            }
            double uniqueWordRatio = (double) uniqueWords.size() / words.length;
            if (uniqueWordRatio < 0.55) {
                return false;
            }
        }

        return distinctLetterScripts(candidate) <= 2;
    }

    private static int longestRepeatedRun(String value) {
        int longest = 1;
        int current = 1;
        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) == value.charAt(i - 1)) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }

    private static int distinctLetterScripts(String value) {
        Set<Character.UnicodeScript> scripts = new HashSet<>();
        value.codePoints()
                .filter(Character::isLetter)
                .forEach(codePoint -> {
                    Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
                    if (script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.INHERITED) {
                        scripts.add(script);
                    }
                });
        return scripts.size();
    }
}
