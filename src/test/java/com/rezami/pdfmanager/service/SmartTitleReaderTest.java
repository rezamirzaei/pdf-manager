package com.rezami.pdfmanager.service;

import com.rezami.pdfmanager.ocr.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartTitleReaderTest {
    @TempDir Path tempDir;

    @Mock PdfTextExtractor textExtractor;

    private SmartTitleReader reader;

    @BeforeEach
    void setUp() {
        reader = new SmartTitleReader(textExtractor, 100, 180);
    }

    @Test
    void readTitle_prefersLeadingTitleLine() throws IOException {
        Path pdf = createTempPdf("paper.pdf");
        when(textExtractor.extractText(eq(pdf), eq(180))).thenReturn(Optional.of("""
                Practical Machine Learning for PDF Organization

                Reza Example
                University of Somewhere
                """));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("Practical Machine Learning for PDF Organization");
        verify(textExtractor).extractText(pdf, 180);
    }

    @Test
    void readTitle_combinesWrappedTitleLines() throws IOException {
        Path pdf = createTempPdf("wrapped.pdf");
        when(textExtractor.extractText(eq(pdf), eq(180))).thenReturn(Optional.of("""
                A Fast Local System
                for Renaming PDF Files

                Jane Doe
                """));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("A Fast Local System for Renaming PDF Files");
    }

    @Test
    void readTitle_stripsJournalHeaderAndAbstractNoise() throws IOException {
        Path pdf = createTempPdf("journal.pdf");
        when(textExtractor.extractText(eq(pdf), eq(180))).thenReturn(Optional.of("""
                IEEE TRANSACTIONS ON COMMUNICATIONS, VOL. 68, NO. 2, FEBRUARY 2020 987 Structure Learning of Sparse GGMs Over Multiple Access Networks
                Abstract A central machine is interested in learning the dependency graph.
                """));

        Optional<String> title = reader.readTitle(pdf);

        assertThat(title).contains("Structure Learning of Sparse GGMs Over Multiple Access Networks");
    }

    @Test
    void readTitle_whenNoTextExtracted_returnsEmpty() throws IOException {
        Path pdf = createTempPdf("empty.pdf");
        when(textExtractor.extractText(eq(pdf), eq(180))).thenReturn(Optional.empty());

        assertThat(reader.readTitle(pdf)).isEmpty();
        verify(textExtractor).extractText(pdf, 180);
    }

    @Test
    void readTitle_withNullPath_throwsException() {
        assertThatThrownBy(() -> reader.readTitle(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("pdfPath");
    }

    @Test
    void readTitle_withNonExistentFile_throwsException() {
        Path nonExistent = tempDir.resolve("missing.pdf");

        assertThatThrownBy(() -> reader.readTitle(nonExistent))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Not a file");
    }

    private Path createTempPdf(String name) throws IOException {
        Path pdf = tempDir.resolve(name);
        Files.write(pdf, new byte[0]);
        return pdf;
    }
}
