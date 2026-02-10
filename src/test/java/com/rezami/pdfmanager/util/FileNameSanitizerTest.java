package com.rezami.pdfmanager.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileNameSanitizerTest {
    private final FileNameSanitizer sanitizer = new FileNameSanitizer();

    @Test
    void sanitizeBaseName_whenNull_returnsEmpty() {
        assertThat(sanitizer.sanitizeBaseName(null)).isEmpty();
    }

    @Test
    void sanitizeBaseName_replacesIllegalCharacters() {
        String input = "A/B\\\\C:D*E?F\"G<H>I|J";
        assertThat(sanitizer.sanitizeBaseName(input)).isEqualTo("A B C D E F G H I J");
    }

    @Test
    void sanitizeBaseName_stripsTrailingDotsAndSpaces() {
        assertThat(sanitizer.sanitizeBaseName("Title... ")).isEqualTo("Title");
    }

    @Test
    void sanitizeBaseName_avoidsWindowsReservedNames() {
        assertThat(sanitizer.sanitizeBaseName("CON")).isEqualTo("CON_");
        assertThat(sanitizer.sanitizeBaseName("nul")).isEqualTo("nul_");
    }

    @Test
    void sanitizeBaseName_truncatesVeryLongNames() {
        String input = "a".repeat(10_000);
        String output = sanitizer.sanitizeBaseName(input);
        assertThat(output).isNotBlank();
        assertThat(output.length()).isLessThanOrEqualTo(180);
    }
}

