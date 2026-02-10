package com.rezami.pdfmanager.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.domain.RenamePlanEntry;
import com.rezami.pdfmanager.domain.RenameStatus;
import com.rezami.pdfmanager.util.FileNameSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RenamePlannerTest {
    @TempDir Path tempDir;

    @Test
    void plan_skipsFilesWithMissingTitles() throws IOException {
        Files.writeString(tempDir.resolve("a.pdf"), "a");

        RenamePlanner planner =
                new RenamePlanner(new PdfFileScanner(), titleReader(Map.of("a.pdf", Optional.empty())), new FileNameSanitizer());

        RenamePlan plan = planner.plan(tempDir, false);

        assertThat(plan.entries()).hasSize(1);
        assertThat(plan.entries().getFirst().status()).isEqualTo(RenameStatus.SKIPPED_NO_TITLE);
    }

    @Test
    void plan_skipsWhenFileNameAlreadyMatchesTitle() throws IOException {
        Files.writeString(tempDir.resolve("a.pdf"), "a");

        RenamePlanner planner =
                new RenamePlanner(
                        new PdfFileScanner(), titleReader(Map.of("a.pdf", Optional.of("a"))), new FileNameSanitizer());

        RenamePlan plan = planner.plan(tempDir, false);

        assertThat(plan.entries()).hasSize(1);
        assertThat(plan.entries().getFirst().status()).isEqualTo(RenameStatus.SKIPPED_SAME_NAME);
    }

    @Test
    void plan_makesDuplicateTitlesUnique() throws IOException {
        Files.writeString(tempDir.resolve("one.pdf"), "1");
        Files.writeString(tempDir.resolve("two.pdf"), "2");

        RenamePlanner planner =
                new RenamePlanner(
                        new PdfFileScanner(),
                        titleReader(
                                Map.of("one.pdf", Optional.of("Same"), "two.pdf", Optional.of("Same"))),
                        new FileNameSanitizer());

        RenamePlan plan = planner.plan(tempDir, false);

        assertThat(readyTargetNames(plan))
                .containsExactlyInAnyOrder("Same.pdf", "Same (2).pdf");
    }

    @Test
    void plan_allowsSwappingNamesBetweenReadyOperations() throws IOException {
        Files.writeString(tempDir.resolve("a.pdf"), "A");
        Files.writeString(tempDir.resolve("b.pdf"), "B");

        RenamePlanner planner =
                new RenamePlanner(
                        new PdfFileScanner(),
                        titleReader(
                                Map.of("a.pdf", Optional.of("b"), "b.pdf", Optional.of("a"))),
                        new FileNameSanitizer());

        RenamePlan plan = planner.plan(tempDir, false);

        assertThat(readyTargetNames(plan)).containsExactlyInAnyOrder("a.pdf", "b.pdf");
        assertThat(plan.readyCount()).isEqualTo(2);
    }

    @Test
    void plan_avoidsTargetNamesUsedBySkippedFiles() throws IOException {
        Files.writeString(tempDir.resolve("keep.pdf"), "K");
        Files.writeString(tempDir.resolve("src.pdf"), "S");

        RenamePlanner planner =
                new RenamePlanner(
                        new PdfFileScanner(),
                        titleReader(
                                Map.of(
                                        "keep.pdf", Optional.empty(),
                                        "src.pdf", Optional.of("keep"))),
                        new FileNameSanitizer());

        RenamePlan plan = planner.plan(tempDir, false);

        RenamePlanEntry src = entryByName(plan, "src.pdf");
        assertThat(src.status()).isEqualTo(RenameStatus.READY);
        assertThat(src.target()).isPresent();
        assertThat(src.target().orElseThrow().getFileName().toString()).isEqualTo("keep (2).pdf");
    }

    @Test
    void plan_avoidsTargetNamesUsedByDirectories() throws IOException {
        Files.createDirectory(tempDir.resolve("My Title.pdf"));
        Files.writeString(tempDir.resolve("src.pdf"), "S");

        RenamePlanner planner =
                new RenamePlanner(
                        new PdfFileScanner(),
                        titleReader(Map.of("src.pdf", Optional.of("My Title"))),
                        new FileNameSanitizer());

        RenamePlan plan = planner.plan(tempDir, false);

        RenamePlanEntry src = entryByName(plan, "src.pdf");
        assertThat(src.status()).isEqualTo(RenameStatus.READY);
        assertThat(src.target()).isPresent();
        assertThat(src.target().orElseThrow().getFileName().toString()).isEqualTo("My Title (2).pdf");
    }

    private static PdfTitleReader titleReader(Map<String, Optional<String>> titles) {
        return pdfPath -> titles.getOrDefault(pdfPath.getFileName().toString(), Optional.empty());
    }

    private static RenamePlanEntry entryByName(RenamePlan plan, String fileName) {
        return plan.entries().stream()
                .filter(e -> e.currentFileName().equals(fileName))
                .findFirst()
                .orElseThrow();
    }

    private static java.util.List<String> readyTargetNames(RenamePlan plan) {
        return plan.readyEntries().stream()
                .map(e -> e.target().orElseThrow().getFileName().toString())
                .toList();
    }
}

