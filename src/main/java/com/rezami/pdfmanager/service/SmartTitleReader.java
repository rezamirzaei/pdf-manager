package com.rezami.pdfmanager.service;

import com.rezami.pdfmanager.ocr.PdfTextExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Fast built-in title inference that derives a likely title from extracted PDF text.
 * This does not require an external model server and is intended to be packaged with the app.
 */
public final class SmartTitleReader implements PdfTitleReader {
    private static final Pattern NOISE_LINE = Pattern.compile(
            "(?i)^(abstract|keywords?|index terms?|introduction|references|acknowledg(?:e)?ments?)\\b.*");
    private static final Pattern METADATA_LINE = Pattern.compile(
            "(?i).*(doi\\b|https?://|www\\.|@|copyright|©|published|received|accepted|arxiv).*");

    private final PdfTextExtractor textExtractor;
    private final int maxTitleLength;
    private final int maxTextChars;

    public SmartTitleReader(PdfTextExtractor textExtractor, int maxTitleLength, int maxTextChars) {
        this.textExtractor = Objects.requireNonNull(textExtractor, "textExtractor");
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

        Optional<String> extractedText = textExtractor.extractText(pdfPath, maxTextChars);
        if (extractedText.isEmpty() || extractedText.get().isBlank()) {
            return Optional.empty();
        }

        return inferTitle(TitleTextPreprocessor.prepare(extractedText.get()));
    }

    private Optional<String> inferTitle(String preparedText) {
        List<String> lines = Arrays.stream(preparedText.split("\\n+"))
                .map(SmartTitleReader::normalizeSpacing)
                .filter(line -> !line.isBlank())
                .limit(12)
                .toList();

        String bestCandidate = null;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < lines.size(); i++) {
            String singleLine = sanitizeCandidate(lines.get(i));
            int singleScore = scoreCandidate(singleLine, i);
            if (singleScore > bestScore) {
                bestScore = singleScore;
                bestCandidate = singleLine;
            }

            if (i + 1 < lines.size() && shouldCombine(lines.get(i), lines.get(i + 1))) {
                String combined = sanitizeCandidate(lines.get(i) + " " + lines.get(i + 1));
                int combinedScore = scoreCandidate(combined, i) + 6;
                if (combinedScore > bestScore) {
                    bestScore = combinedScore;
                    bestCandidate = combined;
                }
            }
        }

        if (bestCandidate != null && bestScore > 0) {
            return Optional.of(bestCandidate);
        }

        return fallbackFromText(preparedText);
    }

    private int scoreCandidate(String candidate, int index) {
        if (candidate.isBlank()) {
            return Integer.MIN_VALUE;
        }
        if (NOISE_LINE.matcher(candidate).matches() || METADATA_LINE.matcher(candidate).matches()) {
            return Integer.MIN_VALUE;
        }

        int length = candidate.length();
        if (length < 10 || length > Math.max(maxTitleLength + 40, 160)) {
            return Integer.MIN_VALUE;
        }

        int wordCount = countWords(candidate);
        if (wordCount < 2 || wordCount > 20) {
            return Integer.MIN_VALUE;
        }

        int score = 70 - (index * 8);
        if (length >= 20 && length <= maxTitleLength + 20) {
            score += 18;
        } else {
            score += 8;
        }
        if (wordCount >= 3 && wordCount <= 16) {
            score += 18;
        }
        if (!candidate.endsWith(".")) {
            score += 4;
        } else {
            score -= 8;
        }
        if (alphabeticRatio(candidate) > 0.55) {
            score += 10;
        } else {
            score -= 10;
        }
        if (uppercaseRatio(candidate) > 0.85 && wordCount > 6) {
            score -= 18;
        }
        if (digitCount(candidate) > 4) {
            score -= 8;
        }
        if (looksLikeAffiliation(candidate)) {
            score -= 24;
        }
        return score;
    }

    private Optional<String> fallbackFromText(String preparedText) {
        String firstSentence = normalizeSpacing(preparedText.replaceFirst("(?s)[.!?].*$", ""));
        String sanitized = sanitizeCandidate(firstSentence);
        return sanitized.isBlank() ? Optional.empty() : Optional.of(sanitized);
    }

    private static boolean looksLikeAffiliation(String candidate) {
        String lower = candidate.toLowerCase();
        return lower.contains("university")
                || lower.contains("department")
                || lower.contains("institute")
                || lower.contains("school of")
                || lower.contains("faculty of");
    }

    private static boolean looksLikeAuthorLine(String candidate) {
        String normalized = normalizeSpacing(candidate);
        int wordCount = countWords(normalized);
        if (wordCount < 2 || wordCount > 6) {
            return false;
        }
        if (normalized.contains("@") || normalized.contains("http")) {
            return true;
        }

        String[] words = normalized.split("\\s+");
        boolean allNameLike = true;
        for (String word : words) {
            String cleaned = word.replaceAll("[^A-Za-z-]", "");
            if (cleaned.isEmpty()) {
                allNameLike = false;
                break;
            }
            if (!Character.isUpperCase(cleaned.charAt(0))) {
                allNameLike = false;
                break;
            }
            if (!cleaned.substring(1).equals(cleaned.substring(1).toLowerCase())) {
                allNameLike = false;
                break;
            }
        }
        return allNameLike;
    }

    private static boolean shouldCombine(String firstLine, String secondLine) {
        return !looksLikeAffiliation(secondLine) && !looksLikeAuthorLine(secondLine);
    }

    private String sanitizeCandidate(String candidate) {
        String cleaned = normalizeSpacing(candidate)
                .replaceAll("^[\\p{Punct}\\d\\s]+", "")
                .replaceAll("[\\p{Punct}&&[^()\\-]]+$", "")
                .trim();

        if (cleaned.length() <= maxTitleLength) {
            return cleaned;
        }

        int lastSpace = cleaned.lastIndexOf(' ', maxTitleLength - 1);
        if (lastSpace > maxTitleLength / 2) {
            return cleaned.substring(0, lastSpace).trim();
        }
        return cleaned.substring(0, maxTitleLength).trim();
    }

    private static String normalizeSpacing(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private static int countWords(String value) {
        return value.isBlank() ? 0 : value.split("\\s+").length;
    }

    private static int digitCount(String value) {
        return (int) value.chars().filter(Character::isDigit).count();
    }

    private static double alphabeticRatio(String value) {
        long letters = value.chars().filter(Character::isLetter).count();
        return value.isEmpty() ? 0.0 : (double) letters / value.length();
    }

    private static double uppercaseRatio(String value) {
        long letters = value.chars().filter(Character::isLetter).count();
        long uppercaseLetters = value.chars().filter(Character::isUpperCase).count();
        return letters == 0 ? 0.0 : (double) uppercaseLetters / letters;
    }
}
