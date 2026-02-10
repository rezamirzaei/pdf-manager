package com.rezami.pdfmanager.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RenamePlanTest {
    @Test
    void countsReadySkippedAndErrors() {
        Path root = Path.of("/tmp");

        RenamePlanEntry ready =
                new RenamePlanEntry(
                        root.resolve("a.pdf"),
                        "a.pdf",
                        Optional.of("A"),
                        Optional.of(root.resolve("A.pdf")),
                        RenameStatus.READY,
                        "Ready");
        RenamePlanEntry skipped =
                new RenamePlanEntry(
                        root.resolve("b.pdf"),
                        "b.pdf",
                        Optional.empty(),
                        Optional.empty(),
                        RenameStatus.SKIPPED_NO_TITLE,
                        "No title");
        RenamePlanEntry error =
                new RenamePlanEntry(
                        root.resolve("c.pdf"),
                        "c.pdf",
                        Optional.empty(),
                        Optional.empty(),
                        RenameStatus.ERROR,
                        "Error");

        RenamePlan plan = new RenamePlan(root, false, List.of(ready, skipped, error));

        assertThat(plan.readyCount()).isEqualTo(1);
        assertThat(plan.skippedCount()).isEqualTo(1);
        assertThat(plan.errorCount()).isEqualTo(1);
        assertThat(plan.readyOperations())
                .containsExactly(new RenameOperation(root.resolve("a.pdf"), root.resolve("A.pdf")));
    }
}

