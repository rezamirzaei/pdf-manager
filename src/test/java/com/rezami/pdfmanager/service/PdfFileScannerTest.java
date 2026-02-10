package com.rezami.pdfmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfFileScannerTest {
    private final PdfFileScanner scanner = new PdfFileScanner();

    @TempDir Path tempDir;

    @Test
    void scan_whenNotRecursive_returnsOnlyPdfsInDirectory() throws IOException {
        Files.writeString(tempDir.resolve("a.pdf"), "a");
        Files.writeString(tempDir.resolve("b.PDF"), "b");
        Files.writeString(tempDir.resolve("c.txt"), "c");
        Files.createDirectory(tempDir.resolve("sub"));
        Files.writeString(tempDir.resolve("sub").resolve("d.pdf"), "d");

        List<Path> result = scanner.scan(tempDir, false);

        assertThat(result).extracting(p -> p.getFileName().toString()).containsExactly("a.pdf", "b.PDF");
    }

    @Test
    void scan_whenRecursive_includesPdfsInSubfolders() throws IOException {
        Files.writeString(tempDir.resolve("a.pdf"), "a");
        Files.createDirectory(tempDir.resolve("sub"));
        Files.writeString(tempDir.resolve("sub").resolve("d.pdf"), "d");

        List<Path> result = scanner.scan(tempDir, true);

        assertThat(result).extracting(p -> p.getFileName().toString()).containsExactly("a.pdf", "d.pdf");
    }

    @Test
    void scan_whenPathIsNotDirectory_throws() throws IOException {
        Path file = tempDir.resolve("not-a-dir");
        Files.writeString(file, "x");

        assertThatThrownBy(() -> scanner.scan(file, false)).isInstanceOf(IOException.class);
    }
}

