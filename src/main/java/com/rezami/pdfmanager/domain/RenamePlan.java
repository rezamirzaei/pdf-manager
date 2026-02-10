package com.rezami.pdfmanager.domain;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record RenamePlan(Path rootDirectory, boolean recursive, List<RenamePlanEntry> entries) {
    public RenamePlan {
        Objects.requireNonNull(rootDirectory, "rootDirectory");
        Objects.requireNonNull(entries, "entries");

        entries =
                entries.stream()
                        .sorted(Comparator.comparing(e -> e.source().toString()))
                        .toList();
    }

    public List<RenamePlanEntry> readyEntries() {
        return entries.stream().filter(RenamePlanEntry::isReady).toList();
    }

    public List<RenameOperation> readyOperations() {
        return readyEntries().stream()
                .map(entry -> new RenameOperation(entry.source(), entry.target().orElseThrow()))
                .toList();
    }

    public int readyCount() {
        return readyEntries().size();
    }

    public int skippedCount() {
        return (int) entries.stream().filter(e -> e.status().isSkipped()).count();
    }

    public int errorCount() {
        return (int) entries.stream().filter(e -> e.status() == RenameStatus.ERROR).count();
    }
}

