package com.rezami.pdfmanager.domain;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public record RenamePlanEntry(
        Path source,
        String currentFileName,
        Optional<String> extractedTitle,
        Optional<Path> target,
        RenameStatus status,
        String note) {
    public RenamePlanEntry {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(currentFileName, "currentFileName");
        Objects.requireNonNull(extractedTitle, "extractedTitle");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(note, "note");
    }

    public boolean isReady() {
        return status == RenameStatus.READY;
    }
}

