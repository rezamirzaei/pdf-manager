package com.rezami.pdfmanager.app;

import com.rezami.pdfmanager.service.CompositeTitleReader;
import com.rezami.pdfmanager.service.LlmTitleReader;
import com.rezami.pdfmanager.service.PdfBoxTitleReader;
import com.rezami.pdfmanager.service.PdfRenameService;
import com.rezami.pdfmanager.service.PdfTitleReader;
import com.rezami.pdfmanager.service.RenamePlanner;
import com.rezami.pdfmanager.service.RenameService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TitleReaderFactoryTest {

    @Test
    void createMetadataReader_returnsPdfBoxTitleReader() {
        PdfTitleReader reader = TitleReaderFactory.createMetadataReader();

        assertThat(reader).isInstanceOf(PdfBoxTitleReader.class);
    }

    @Test
    void createSmartReader_returnsLlmTitleReader() {
        PdfTitleReader reader = TitleReaderFactory.createSmartReader();

        assertThat(reader).isInstanceOf(LlmTitleReader.class);
    }

    @Test
    void createLocalLlmReader_returnsLlmTitleReader() {
        PdfTitleReader reader = TitleReaderFactory.createLocalLlmReader();

        assertThat(reader).isInstanceOf(LlmTitleReader.class);
    }

    @Test
    void createBuiltInReader_returnsCompositeTitleReader() {
        PdfTitleReader reader = TitleReaderFactory.createBuiltInReader();

        assertThat(reader).isInstanceOf(CompositeTitleReader.class);
    }

    @Test
    void createLlmReader_whenOllamaUnavailable_returnsMetadataReader() {
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
        boolean available = TitleReaderFactory.isOllamaAvailable("http://localhost:59999");

        assertThat(available).isFalse();
    }
}
