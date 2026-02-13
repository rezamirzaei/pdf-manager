package com.rezami.pdfmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeTitleReaderTest {

    @TempDir
    Path tempDir;

    @Mock
    private PdfTitleReader primaryReader;

    @Mock
    private PdfTitleReader fallbackReader;

    @Test
    void readTitle_whenPrimarySucceeds_returnsPrimaryResult() throws IOException {
        Path pdf = createTempPdf("test.pdf");
        CompositeTitleReader reader = new CompositeTitleReader(primaryReader, fallbackReader);

        when(primaryReader.readTitle(pdf)).thenReturn(Optional.of("Primary Title"));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("Primary Title");
        verify(primaryReader).readTitle(pdf);
        verifyNoInteractions(fallbackReader);
    }

    @Test
    void readTitle_whenPrimaryReturnsEmpty_usesFallback() throws IOException {
        Path pdf = createTempPdf("test.pdf");
        CompositeTitleReader reader = new CompositeTitleReader(primaryReader, fallbackReader);

        when(primaryReader.readTitle(pdf)).thenReturn(Optional.empty());
        when(fallbackReader.readTitle(pdf)).thenReturn(Optional.of("Fallback Title"));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("Fallback Title");
        verify(primaryReader).readTitle(pdf);
        verify(fallbackReader).readTitle(pdf);
    }

    @Test
    void readTitle_whenPrimaryReturnsBlank_usesFallback() throws IOException {
        Path pdf = createTempPdf("test.pdf");
        CompositeTitleReader reader = new CompositeTitleReader(primaryReader, fallbackReader);

        when(primaryReader.readTitle(pdf)).thenReturn(Optional.of("   "));
        when(fallbackReader.readTitle(pdf)).thenReturn(Optional.of("Fallback Title"));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("Fallback Title");
    }

    @Test
    void readTitle_whenPrimaryThrows_usesFallback() throws IOException {
        Path pdf = createTempPdf("test.pdf");
        CompositeTitleReader reader = new CompositeTitleReader(primaryReader, fallbackReader);

        when(primaryReader.readTitle(pdf)).thenThrow(new IOException("Primary failed"));
        when(fallbackReader.readTitle(pdf)).thenReturn(Optional.of("Fallback Title"));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("Fallback Title");
    }

    @Test
    void readTitle_whenBothFail_returnsEmpty() throws IOException {
        Path pdf = createTempPdf("test.pdf");
        CompositeTitleReader reader = new CompositeTitleReader(primaryReader, fallbackReader);

        when(primaryReader.readTitle(pdf)).thenReturn(Optional.empty());
        when(fallbackReader.readTitle(pdf)).thenReturn(Optional.empty());

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).isEmpty();
    }

    @Test
    void readTitle_whenPreferMetadata_triesFallbackFirst() throws IOException {
        Path pdf = createTempPdf("test.pdf");
        CompositeTitleReader reader = new CompositeTitleReader(primaryReader, fallbackReader, true);

        when(fallbackReader.readTitle(pdf)).thenReturn(Optional.of("Metadata Title"));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("Metadata Title");
        verify(fallbackReader).readTitle(pdf);
        verifyNoInteractions(primaryReader);
    }

    @Test
    void readTitle_whenPreferMetadataAndFallbackEmpty_triesPrimary() throws IOException {
        Path pdf = createTempPdf("test.pdf");
        CompositeTitleReader reader = new CompositeTitleReader(primaryReader, fallbackReader, true);

        when(fallbackReader.readTitle(pdf)).thenReturn(Optional.empty());
        when(primaryReader.readTitle(pdf)).thenReturn(Optional.of("LLM Title"));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("LLM Title");
        verify(fallbackReader).readTitle(pdf);
        verify(primaryReader).readTitle(pdf);
    }

    @Test
    void readTitle_withNullPath_throwsException() {
        CompositeTitleReader reader = new CompositeTitleReader(primaryReader, fallbackReader);

        assertThatThrownBy(() -> reader.readTitle(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("pdfPath");
    }

    @Test
    void constructor_withNullPrimary_throwsException() {
        assertThatThrownBy(() -> new CompositeTitleReader(null, fallbackReader))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("primary");
    }

    @Test
    void constructor_withNullFallback_throwsException() {
        assertThatThrownBy(() -> new CompositeTitleReader(primaryReader, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("fallback");
    }

    @Test
    void llmFirst_createsPrimaryFirstReader() throws IOException {
        // Use interface mocks instead of final class mocks
        PdfTitleReader llmReader = mock(PdfTitleReader.class);
        PdfTitleReader metadataReader = mock(PdfTitleReader.class);
        Path pdf = createTempPdf("test.pdf");

        when(llmReader.readTitle(pdf)).thenReturn(Optional.of("LLM Title"));

        CompositeTitleReader reader = new CompositeTitleReader(llmReader, metadataReader, false);
        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("LLM Title");
        verify(llmReader).readTitle(pdf);
        verifyNoInteractions(metadataReader);
    }

    @Test
    void metadataFirst_createsMetadataPreferredReader() throws IOException {
        // Use interface mocks instead of final class mocks
        PdfTitleReader llmReader = mock(PdfTitleReader.class);
        PdfTitleReader metadataReader = mock(PdfTitleReader.class);
        Path pdf = createTempPdf("test.pdf");

        when(metadataReader.readTitle(pdf)).thenReturn(Optional.of("Metadata Title"));

        CompositeTitleReader reader = new CompositeTitleReader(llmReader, metadataReader, true);
        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("Metadata Title");
        verify(metadataReader).readTitle(pdf);
        verifyNoInteractions(llmReader);
    }

    private Path createTempPdf(String name) throws IOException {
        Path pdf = tempDir.resolve(name);
        Files.write(pdf, new byte[0]);
        return pdf;
    }
}


