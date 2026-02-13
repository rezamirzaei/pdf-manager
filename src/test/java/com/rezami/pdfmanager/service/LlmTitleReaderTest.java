package com.rezami.pdfmanager.service;

import com.rezami.pdfmanager.llm.LlmClient;
import com.rezami.pdfmanager.ocr.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmTitleReaderTest {

    @TempDir
    Path tempDir;

    @Mock
    private PdfTextExtractor textExtractor;

    @Mock
    private LlmClient llmClient;

    private LlmTitleReader reader;

    @BeforeEach
    void setUp() {
        reader = new LlmTitleReader(textExtractor, llmClient, 100, 2000);
    }

    @Test
    void readTitle_withValidPdf_returnsGeneratedTitle() throws IOException {
        Path pdf = createTempPdf("test.pdf");

        when(textExtractor.extractText(eq(pdf), eq(2000)))
                .thenReturn(Optional.of("Machine learning is a subset of artificial intelligence..."));
        when(llmClient.generateTitle(anyString(), eq(100)))
                .thenReturn(Optional.of("Introduction to Machine Learning"));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("Introduction to Machine Learning");
        verify(textExtractor).extractText(pdf, 2000);
        verify(llmClient).generateTitle(anyString(), eq(100));
    }

    @Test
    void readTitle_stripsJournalHeaderAndAbstract_beforeCallingLlm() throws IOException {
        Path pdf = createTempPdf("paper.pdf");
        String rawText = """
                IEEE TRANSACTIONS ON COMMUNICATIONS, VOL. 68, NO. 2, FEBRUARY 2020 987  Structure Learning of Sparse GGMs Over Multiple Access Networks  Mostafa Tavassolipour
                Abstract A central machine is interested in learning the dependency graph.
                """;

        when(textExtractor.extractText(eq(pdf), eq(2000))).thenReturn(Optional.of(rawText));
        when(llmClient.generateTitle(anyString(), eq(100)))
                .thenReturn(Optional.of("Structure Learning of Sparse GGMs"));

        reader.readTitle(pdf);

        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).generateTitle(inputCaptor.capture(), eq(100));
        String llmInput = inputCaptor.getValue();
        assertThat(llmInput).contains("Structure Learning of Sparse GGMs Over Multiple Access Networks");
        assertThat(llmInput).doesNotContain("IEEE TRANSACTIONS ON COMMUNICATIONS");
        assertThat(llmInput.toLowerCase()).doesNotContain("abstract");
    }

    @Test
    void readTitle_whenNoTextExtracted_returnsEmpty() throws IOException {
        Path pdf = createTempPdf("empty.pdf");

        when(textExtractor.extractText(eq(pdf), eq(2000)))
                .thenReturn(Optional.empty());

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).isEmpty();
        verify(textExtractor).extractText(pdf, 2000);
        verifyNoInteractions(llmClient);
    }

    @Test
    void readTitle_whenBlankTextExtracted_returnsEmpty() throws IOException {
        Path pdf = createTempPdf("blank.pdf");

        when(textExtractor.extractText(eq(pdf), eq(2000)))
                .thenReturn(Optional.of("   "));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).isEmpty();
        verify(textExtractor).extractText(pdf, 2000);
        verifyNoInteractions(llmClient);
    }

    @Test
    void readTitle_whenLlmFails_returnsEmpty() throws IOException {
        Path pdf = createTempPdf("test.pdf");

        when(textExtractor.extractText(eq(pdf), eq(2000)))
                .thenReturn(Optional.of("Some text content"));
        when(llmClient.generateTitle(anyString(), eq(100)))
                .thenReturn(Optional.empty());

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).isEmpty();
    }

    @Test
    void readTitle_whenExtractionFails_propagatesException() throws IOException {
        Path pdf = createTempPdf("error.pdf");

        when(textExtractor.extractText(eq(pdf), eq(2000)))
                .thenThrow(new IOException("Extraction failed"));

        assertThatThrownBy(() -> reader.readTitle(pdf))
                .isInstanceOf(IOException.class)
                .hasMessage("Extraction failed");
    }

    @Test
    void readTitle_whenLlmThrows_propagatesException() throws IOException {
        Path pdf = createTempPdf("test.pdf");

        when(textExtractor.extractText(eq(pdf), eq(2000)))
                .thenReturn(Optional.of("Some text content"));
        when(llmClient.generateTitle(anyString(), eq(100)))
                .thenThrow(new IOException("LLM unavailable"));

        assertThatThrownBy(() -> reader.readTitle(pdf))
                .isInstanceOf(IOException.class)
                .hasMessage("LLM unavailable");
    }

    @Test
    void readTitle_withNullPath_throwsException() {
        assertThatThrownBy(() -> reader.readTitle(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("pdfPath");
    }

    @Test
    void readTitle_withNonExistentFile_throwsException() {
        Path nonExistent = tempDir.resolve("does-not-exist.pdf");

        assertThatThrownBy(() -> reader.readTitle(nonExistent))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Not a file");
    }

    @Test
    void readTitle_withDirectory_throwsException() {
        assertThatThrownBy(() -> reader.readTitle(tempDir))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Not a file");
    }

    @Test
    void constructor_withNullExtractor_throwsException() {
        assertThatThrownBy(() -> new LlmTitleReader(null, llmClient))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("textExtractor");
    }

    @Test
    void constructor_withNullLlmClient_throwsException() {
        assertThatThrownBy(() -> new LlmTitleReader(textExtractor, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("llmClient");
    }

    @Test
    void constructor_withInvalidMaxTitleLength_throwsException() {
        assertThatThrownBy(() -> new LlmTitleReader(textExtractor, llmClient, 0, 2000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxTitleLength must be positive");
    }

    @Test
    void constructor_withInvalidMaxTextChars_throwsException() {
        assertThatThrownBy(() -> new LlmTitleReader(textExtractor, llmClient, 100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxTextChars must be positive");
    }

    @Test
    void isLlmAvailable_delegatesToClient() {
        when(llmClient.isAvailable()).thenReturn(true);

        assertThat(reader.isLlmAvailable()).isTrue();
        verify(llmClient).isAvailable();
    }

    @Test
    void getModelName_delegatesToClient() {
        when(llmClient.getModelName()).thenReturn("llama3.2:3b");

        assertThat(reader.getModelName()).isEqualTo("llama3.2:3b");
        verify(llmClient).getModelName();
    }

    @Test
    void getMaxTitleLength_returnsConfiguredValue() {
        assertThat(reader.getMaxTitleLength()).isEqualTo(100);
    }

    @Test
    void getMaxTextChars_returnsConfiguredValue() {
        assertThat(reader.getMaxTextChars()).isEqualTo(2000);
    }

    private Path createTempPdf(String name) throws IOException {
        Path pdf = tempDir.resolve(name);
        Files.write(pdf, new byte[0]);  // Create empty file for path validation
        return pdf;
    }
}
