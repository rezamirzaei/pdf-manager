package com.rezami.pdfmanager.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.rezami.pdfmanager.util.FileNameSanitizer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfRenameServiceIntegrationTest {
    @TempDir Path tempDir;

    @Test
    void endToEnd_renamesPdfsToTheirTitles() throws IOException {
        Path one = tempDir.resolve("download-1.pdf");
        Path two = tempDir.resolve("download-2.pdf");
        Path three = tempDir.resolve("download-3.pdf");

        savePdfWithTitle(one, "Paper 1");
        savePdfWithTitle(two, "Paper 1");
        savePdfWithNoTitle(three);

        PdfFileScanner scanner = new PdfFileScanner();
        PdfTitleReader reader = new PdfBoxTitleReader();
        FileNameSanitizer sanitizer = new FileNameSanitizer();
        RenamePlanner planner = new RenamePlanner(scanner, reader, sanitizer);
        PdfRenamer renamer = new PdfRenamer();
        PdfRenameService service = new PdfRenameService(planner, renamer);

        var plan = service.plan(tempDir, false);
        assertThat(plan.readyCount()).isEqualTo(2);
        assertThat(plan.skippedCount()).isEqualTo(1);

        service.execute(plan);

        assertThat(Files.exists(tempDir.resolve("Paper 1.pdf"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("Paper 1 (2).pdf"))).isTrue();
        assertThat(Files.exists(three)).isTrue();
        assertThat(Files.exists(one)).isFalse();
        assertThat(Files.exists(two)).isFalse();
    }

    private static void savePdfWithTitle(Path path, String title) throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(title);
            document.setDocumentInformation(info);
            document.save(path.toFile());
        }
    }

    private static void savePdfWithNoTitle(Path path) throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(path.toFile());
        }
    }
}

