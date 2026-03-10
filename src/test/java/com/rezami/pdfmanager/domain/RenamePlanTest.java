package com.rezami.pdfmanager.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Test
    void filterReadySources_keepsOnlySelectedReadyEntries() {
        Path root = Path.of("/tmp");

        RenamePlanEntry readyA =
                new RenamePlanEntry(
                        root.resolve("a.pdf"),
                        "a.pdf",
                        Optional.of("A"),
                        Optional.of(root.resolve("A.pdf")),
                        RenameStatus.READY,
                        "Ready");
        RenamePlanEntry readyB =
                new RenamePlanEntry(
                        root.resolve("b.pdf"),
                        "b.pdf",
                        Optional.of("B"),
                        Optional.of(root.resolve("B.pdf")),
                        RenameStatus.READY,
                        "Ready");
        RenamePlanEntry skipped =
                new RenamePlanEntry(
                        root.resolve("c.pdf"),
                        "c.pdf",
                        Optional.empty(),
                        Optional.empty(),
                        RenameStatus.SKIPPED_NO_TITLE,
                        "No title");

        RenamePlan filtered = new RenamePlan(root, false, List.of(readyA, readyB, skipped))
                .filterReadySources(Set.of(root.resolve("b.pdf")));

        assertThat(filtered.entries()).containsExactly(readyB, skipped);
        assertThat(filtered.readyOperations())
                .containsExactly(new RenameOperation(root.resolve("b.pdf"), root.resolve("B.pdf")));
    }
}
