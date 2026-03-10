package com.rezami.pdfmanager.app;

import com.rezami.pdfmanager.service.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TitleReaderFactoryTest {

    @Test
    void createMetadataReader_returnsPdfBoxTitleReader() {
        PdfTitleReader reader = TitleReaderFactory.createMetadataReader();

        assertThat(reader).isInstanceOf(PdfBoxTitleReader.class);
    }

    @Test
    void createLlmReader_whenOllamaUnavailable_returnsMetadataReader() {
        PdfTitleReader reader = TitleReaderFactory.createLlmReader(
                "http://localhost:59999", "llama3.2:1b");

        assertThat(reader).isInstanceOf(PdfBoxTitleReader.class);
    }

    @Test
    void createLlmReader_withCustomConfig_whenOllamaUnavailable_returnsMetadataReader() {
        PdfTitleReader reader = TitleReaderFactory.createLlmReader(
                "http://localhost:59999", "llama3.2:1b");

        assertThat(reader).isInstanceOf(PdfBoxTitleReader.class);
    }

    @Test
    void createCompositeReader_whenOllamaUnavailable_returnsMetadataReader() {
        PdfTitleReader reader = TitleReaderFactory.createCompositeReader(
                "http://localhost:59999", "llama3.2:1b", false);

        assertThat(reader).isInstanceOf(PdfBoxTitleReader.class);
    }

    @Test
    void createCompositeReader_withCustomConfig_whenOllamaUnavailable_returnsMetadataReader() {
        PdfTitleReader reader = TitleReaderFactory.createCompositeReader(
                "http://localhost:59999", "llama3.2:1b", true);

        assertThat(reader).isInstanceOf(PdfBoxTitleReader.class);
    }

    @Test
    void createPlanner_returnsRenamePlanner() {
        PdfTitleReader reader = TitleReaderFactory.createMetadataReader();
        RenamePlanner planner = TitleReaderFactory.createPlanner(reader);

        assertThat(planner).isNotNull();
    }

    @Test
    void createRenameService_returnsRenameService() {
        PdfTitleReader reader = TitleReaderFactory.createMetadataReader();
        RenameService service = TitleReaderFactory.createRenameService(reader);

        assertThat(service).isNotNull();
        assertThat(service).isInstanceOf(PdfRenameService.class);
    }

    @Test
    void isOllamaAvailable_whenOllamaNotRunning_returnsFalse() {
        // Using a port that's unlikely to have a service running (valid port range is 1-65535)
        boolean available = TitleReaderFactory.isOllamaAvailable("http://localhost:59999");

        assertThat(available).isFalse();
    }
}

