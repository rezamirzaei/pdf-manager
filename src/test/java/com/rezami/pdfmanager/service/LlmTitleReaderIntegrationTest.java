package com.rezami.pdfmanager.service;

import com.rezami.pdfmanager.app.TitleReaderFactory;
import com.rezami.pdfmanager.llm.OllamaClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that requires a running Ollama instance with a llama model.
 * These tests are skipped if Ollama is not available.
 */
class LlmTitleReaderIntegrationTest {

    @TempDir
    Path tempDir;

    static boolean isOllamaAvailable() {
        return TitleReaderFactory.isOllamaAvailable();
    }

    @Test
    @EnabledIf("isOllamaAvailable")
    void llmTitleReader_generatesTitle_forTextPdf() throws IOException {
        // Create a PDF with text content about machine learning
        Path pdf = tempDir.resolve("ml-paper.pdf");
        String content = """
            Introduction to Machine Learning

            Machine learning is a subset of artificial intelligence that enables
            systems to learn and improve from experience without being explicitly
            programmed. This paper explores the fundamental concepts and applications
            of machine learning in modern software development.

            Keywords: AI, neural networks, deep learning, algorithms
            """;
        savePdfWithText(pdf, content);

        // Create LLM title reader
        PdfTitleReader reader = TitleReaderFactory.createLlmReader();

        // Generate title
        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).isPresent();
        assertThat(title.get()).isNotBlank();
        assertThat(title.get().length()).isLessThanOrEqualTo(100);
    }

    @Test
    @EnabledIf("isOllamaAvailable")
    void compositeTitleReader_usesLlm_whenNoMetadata() throws IOException {
        // Create a PDF with no metadata but with text content
        Path pdf = tempDir.resolve("no-metadata.pdf");
        String content = """
            Financial Analysis Report Q4 2025

            This quarterly report presents the financial performance analysis
            for the fourth quarter of 2025, including revenue growth,
            profit margins, and key performance indicators.
            """;
        savePdfWithText(pdf, content);

        // Create composite reader (metadata first, LLM fallback)
        PdfTitleReader reader = TitleReaderFactory.createCompositeReader(
                "http://localhost:11434", "llama3.2:1b", true);

        // Should use LLM since there's no metadata
        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).isPresent();
        assertThat(title.get()).isNotBlank();
    }

    @Test
    @EnabledIf("isOllamaAvailable")
    void ollamaClient_isAvailable_returnsTrue() {
        OllamaClient client = new OllamaClient();

        assertThat(client.isAvailable()).isTrue();
        assertThat(client.getModelName()).isEqualTo("llama3.2:1b");
    }

    private void savePdfWithText(Path path, String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);

                // Split text into lines (avoid newlines in showText)
                String[] lines = text.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        // Truncate long lines
                        String truncatedLine = line.length() > 80 ? line.substring(0, 80) : line;
                        contentStream.showText(truncatedLine.trim());
                    }
                    contentStream.newLineAtOffset(0, -14);
                }

                contentStream.endText();
            }

            document.save(path.toFile());
        }
    }
}
