package com.rezami.pdfmanager.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TitleGenerationSupportTest {

    @Test
    void buildTitlePrompt_includesInstructionsAndInput() {
        String prompt = TitleGenerationSupport.buildTitlePrompt("Example PDF content", 80);

        assertThat(prompt).contains("Extract the best document title");
        assertThat(prompt).contains("maximum 80 characters");
        assertThat(prompt).contains("Example PDF content");
    }

    @Test
    void sanitizeTitle_removesLabelsAndInvalidCharacters() {
        assertThat(TitleGenerationSupport.sanitizeTitle("\"Title: A / Practical\\\\Guide\"\\nmore", 100))
                .contains("A Practical Guide");
    }

    @Test
    void sanitizeTitle_rejectsNumericGarbage() {
        assertThat(TitleGenerationSupport.sanitizeTitle("0000000000000000", 100)).isEmpty();
    }

    @Test
    void sanitizeTitle_rejectsRepeatedWordGarbage() {
        assertThat(TitleGenerationSupport.sanitizeTitle("Reluct reluct reluct reluct reluct", 100)).isEmpty();
    }
}
