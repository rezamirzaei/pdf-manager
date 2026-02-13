package com.rezami.pdfmanager.ocr;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class PdfBoxTextExtractorTest {

    @TempDir
    Path tempDir;

    private PdfBoxTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfBoxTextExtractor();
    }

    @Test
    void extractText_withTextPdf_extractsContent() throws IOException {
        Path pdf = tempDir.resolve("text-doc.pdf");
        savePdfWithText(pdf, "Hello World! This is a test document with some content that should be extracted properly.");

        Optional<String> text = extractor.extractText(pdf, 1000);

        assertThat(text).isPresent();
        assertThat(text.get()).contains("Hello World");
    }

    @Test
    void extractText_withEmptyPdf_returnsEmpty() throws IOException {
        Path pdf = tempDir.resolve("empty.pdf");
        saveEmptyPdf(pdf);

        Optional<String> text = extractor.extractText(pdf, 1000);

        // Empty PDFs should return empty
        assertThat(text).isEmpty();
    }

    @Test
    void extractText_respectsMaxCharacters() throws IOException {
        Path pdf = tempDir.resolve("long-doc.pdf");
        String longContent = "This is a long document with lots of content. ".repeat(100);
        savePdfWithText(pdf, longContent);

        Optional<String> text = extractor.extractText(pdf, 100);

        assertThat(text).isPresent();
        assertThat(text.get().length()).isLessThanOrEqualTo(100);
    }

    @Test
    void extractText_withNullPath_throwsException() {
        assertThatThrownBy(() -> extractor.extractText(null, 1000))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("pdfPath");
    }

    @Test
    void extractText_withNonExistentFile_throwsException() {
        Path nonExistent = tempDir.resolve("does-not-exist.pdf");

        assertThatThrownBy(() -> extractor.extractText(nonExistent, 1000))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Not a file");
    }

    @Test
    void extractText_withInvalidMaxChars_throwsException() throws IOException {
        Path pdf = tempDir.resolve("test.pdf");
        saveEmptyPdf(pdf);

        assertThatThrownBy(() -> extractor.extractText(pdf, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxCharacters must be positive");

        assertThatThrownBy(() -> extractor.extractText(pdf, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxCharacters must be positive");
    }

    @Test
    void extractTextFromPage_withNegativePageNumber_throwsException() throws IOException {
        Path pdf = tempDir.resolve("test.pdf");
        saveEmptyPdf(pdf);

        assertThatThrownBy(() -> extractor.extractTextFromPage(pdf, -1, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageNumber must be non-negative");
    }

    @Test
    void extractTextFromPage_withInvalidPageNumber_returnsEmpty() throws IOException {
        Path pdf = tempDir.resolve("single-page.pdf");
        saveEmptyPdf(pdf);  // Creates a single-page PDF

        Optional<String> text = extractor.extractTextFromPage(pdf, 5, 1000);

        assertThat(text).isEmpty();
    }

    @Test
    void extractText_normalizesWhitespace() throws IOException {
        Path pdf = tempDir.resolve("whitespace-doc.pdf");
        savePdfWithText(pdf, "Hello    World    Multiple    spaces    in    this    long    document    text");

        Optional<String> text = extractor.extractText(pdf, 1000);

        assertThat(text).isPresent();
        // Text should be normalized - no multiple consecutive spaces
        assertThat(text.get()).doesNotContain("    ");
    }

    @Test
    void extractText_fromMultiPagePdf_extractsFromFirstPage() throws IOException {
        Path pdf = tempDir.resolve("multi-page.pdf");
        saveMultiPagePdf(pdf, "First page content with enough text to pass threshold.",
                               "Second page content.");

        Optional<String> text = extractor.extractText(pdf, 1000);

        assertThat(text).isPresent();
        assertThat(text.get()).contains("First page");
    }

    private void saveEmptyPdf(Path path) throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(path.toFile());
        }
    }

    private void savePdfWithText(Path path, String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);

                // Split text into lines to avoid line-too-long issues
                String[] words = text.split(" ");
                StringBuilder line = new StringBuilder();
                for (String word : words) {
                    if (line.length() + word.length() > 80) {
                        contentStream.showText(line.toString());
                        contentStream.newLineAtOffset(0, -14);
                        line = new StringBuilder();
                    }
                    if (line.length() > 0) {
                        line.append(" ");
                    }
                    line.append(word);
                }
                if (line.length() > 0) {
                    contentStream.showText(line.toString());
                }

                contentStream.endText();
            }

            document.save(path.toFile());
        }
    }

    private void saveMultiPagePdf(Path path, String page1Text, String page2Text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Page 1
            PDPage page1 = new PDPage();
            document.addPage(page1);
            try (PDPageContentStream cs = new PDPageContentStream(document, page1)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(page1Text);
                cs.endText();
            }

            // Page 2
            PDPage page2 = new PDPage();
            document.addPage(page2);
            try (PDPageContentStream cs = new PDPageContentStream(document, page2)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(page2Text);
                cs.endText();
            }

            document.save(path.toFile());
        }
    }
}

