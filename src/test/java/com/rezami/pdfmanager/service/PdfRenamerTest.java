package com.rezami.pdfmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.domain.RenamePlanEntry;
import com.rezami.pdfmanager.domain.RenameStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfRenamerTest {
    private final PdfRenamer renamer = new PdfRenamer();

    @TempDir Path tempDir;

    @Test
    void execute_renamesFiles() throws IOException {
        Path a = tempDir.resolve("a.pdf");
        Path b = tempDir.resolve("b.pdf");
        Files.writeString(a, "A");
        Files.writeString(b, "B");

        Path x = tempDir.resolve("x.pdf");
        Path y = tempDir.resolve("y.pdf");

        RenamePlan plan = planOf(List.of(ready(a, x), ready(b, y)));

        renamer.execute(plan);

        assertThat(Files.exists(a)).isFalse();
        assertThat(Files.exists(b)).isFalse();
        assertThat(Files.readString(x)).isEqualTo("A");
        assertThat(Files.readString(y)).isEqualTo("B");
    }

    @Test
    void execute_supportsSwappingFileNames() throws IOException {
        Path a = tempDir.resolve("a.pdf");
        Path b = tempDir.resolve("b.pdf");
        Files.writeString(a, "A");
        Files.writeString(b, "B");

        RenamePlan plan = planOf(List.of(ready(a, b), ready(b, a)));

        renamer.execute(plan);

        assertThat(Files.readString(a)).isEqualTo("B");
        assertThat(Files.readString(b)).isEqualTo("A");
    }

    @Test
    void execute_whenPhaseTwoFails_rollsBackToOriginalState() throws IOException {
        Path a = tempDir.resolve("a.pdf");
        Path b = tempDir.resolve("b.pdf");
        Files.writeString(a, "A");
        Files.writeString(b, "B");

        Path x = tempDir.resolve("x.pdf");
        Path y = tempDir.resolve("y.pdf");

        Files.writeString(y, "CONFLICT");

        RenamePlan plan = planOf(List.of(ready(a, x), ready(b, y)));

        assertThatThrownBy(() -> renamer.execute(plan)).isInstanceOf(IOException.class);

        assertThat(Files.readString(a)).isEqualTo("A");
        assertThat(Files.readString(b)).isEqualTo("B");
        assertThat(Files.exists(x)).isFalse();
        assertThat(Files.readString(y)).isEqualTo("CONFLICT");

        try (var files = Files.list(tempDir)) {
            assertThat(files.map(p -> p.getFileName().toString()))
                    .noneMatch(name -> name.contains(".pdf-manager-tmp-"));
        }
    }

    private static RenamePlan planOf(List<RenamePlanEntry> entries) {
        return new RenamePlan(entries.getFirst().source().getParent(), false, entries);
    }

    private static RenamePlanEntry ready(Path source, Path target) {
        return new RenamePlanEntry(
                source,
                source.getFileName().toString(),
                Optional.of("t"),
                Optional.of(target),
                RenameStatus.READY,
                "Ready");
    }
}
