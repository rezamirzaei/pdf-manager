package com.rezami.pdfmanager.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.domain.RenamePlanEntry;
import com.rezami.pdfmanager.domain.RenameStatus;
import com.rezami.pdfmanager.util.FileNameSanitizer;
import com.rezami.pdfmanager.util.ProgressListener;

public final class RenamePlanner {
    private final PdfFileScanner scanner;
    private final PdfTitleReader titleReader;
    private final FileNameSanitizer sanitizer;

    public RenamePlanner(PdfFileScanner scanner, PdfTitleReader titleReader, FileNameSanitizer sanitizer) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.titleReader = Objects.requireNonNull(titleReader, "titleReader");
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
    }

    public RenamePlan plan(Path directory, boolean recursive) throws IOException {
        return plan(directory, recursive, ProgressListener.none());
    }

    public RenamePlan plan(Path directory, boolean recursive, ProgressListener progressListener) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(progressListener, "progressListener");

        List<Path> pdfs = scanner.scan(directory, recursive);
        int total = pdfs.size();

        List<EntryDraft> drafts = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            Path pdf = pdfs.get(i);
            String fileName = pdf.getFileName().toString();
            progressListener.onProgress(i + 1, total, "Processing: " + fileName);
            drafts.add(draftFor(pdf));
        }

        applyUniquenessPerDirectory(drafts);

        List<RenamePlanEntry> entries =
                drafts.stream()
                        .map(EntryDraft::toEntry)
                        .toList();

        return new RenamePlan(directory, recursive, entries);
    }

    private EntryDraft draftFor(Path pdf) {
        String currentName = pdf.getFileName().toString();
        Optional<String> title;
        try {
            title = titleReader.readTitle(pdf);
        } catch (IOException e) {
            return EntryDraft.error(pdf, currentName, "Failed to read title: " + e.getMessage());
        }

        if (title.isEmpty() || title.get().isBlank()) {
            return EntryDraft.skipped(pdf, currentName, title, RenameStatus.SKIPPED_NO_TITLE, "No title found");
        }

        String baseName = sanitizer.sanitizeBaseName(title.get());
        if (baseName.isBlank()) {
            return EntryDraft.skipped(
                    pdf,
                    currentName,
                    title,
                    RenameStatus.SKIPPED_NO_TITLE,
                    "Title became empty after sanitizing");
        }

        baseName = stripPdfExtension(baseName);
        if (baseName.isBlank()) {
            return EntryDraft.skipped(
                    pdf,
                    currentName,
                    title,
                    RenameStatus.SKIPPED_NO_TITLE,
                    "Title became empty after stripping .pdf");
        }

        String desiredName = baseName + ".pdf";
        if (equalsIgnoreCaseSafe(desiredName, currentName)) {
            return EntryDraft.skipped(
                    pdf, currentName, title, RenameStatus.SKIPPED_SAME_NAME, "Already matches title");
        }

        return EntryDraft.ready(pdf, currentName, title, desiredName);
    }

    private void applyUniquenessPerDirectory(List<EntryDraft> drafts) {
        Map<Path, List<EntryDraft>> byDirectory = new HashMap<>();
        for (EntryDraft draft : drafts) {
            byDirectory.computeIfAbsent(draft.source.getParent(), ignored -> new ArrayList<>()).add(draft);
        }

        for (List<EntryDraft> group : byDirectory.values()) {
            Path directory = group.getFirst().source.getParent();

            Set<String> reserved = new HashSet<>();
            if (directory != null) {
                try (var children = java.nio.file.Files.list(directory)) {
                    children.forEach(child -> reserved.add(key(child.getFileName().toString())));
                } catch (IOException ignored) {
                    // Best-effort: we still avoid collisions with skipped items.
                }
            }

            for (EntryDraft draft : group) {
                if (draft.status == RenameStatus.READY) {
                    reserved.remove(key(draft.currentFileName));
                }
            }

            Set<String> planned = new HashSet<>();
            for (EntryDraft draft : group) {
                if (draft.status != RenameStatus.READY) {
                    continue;
                }

                String uniqueName = makeUnique(draft.desiredFileName, reserved, planned);
                draft.target = draft.source.getParent().resolve(uniqueName);
                planned.add(key(uniqueName));
            }
        }
    }

    private static String makeUnique(String desiredFileName, Set<String> reserved, Set<String> planned) {
        String normalizedDesired = key(desiredFileName);
        if (!reserved.contains(normalizedDesired) && !planned.contains(normalizedDesired)) {
            return desiredFileName;
        }

        String base = desiredFileName.substring(0, desiredFileName.length() - 4);
        for (int i = 2; i < 10_000; i++) {
            String candidate = base + " (" + i + ").pdf";
            String key = key(candidate);
            if (!reserved.contains(key) && !planned.contains(key)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not find a unique name for: " + desiredFileName);
    }

    private static String stripPdfExtension(String baseName) {
        String lower = baseName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".pdf")) {
            return baseName;
        }
        return baseName.substring(0, baseName.length() - 4).trim();
    }

    private static String key(String fileName) {
        return fileName.toLowerCase(Locale.ROOT);
    }

    private static boolean equalsIgnoreCaseSafe(String a, String b) {
        return a.equalsIgnoreCase(b);
    }

    private static final class EntryDraft {
        private final Path source;
        private final String currentFileName;
        private final Optional<String> extractedTitle;
        private final RenameStatus status;
        private final String note;

        private final String desiredFileName;
        private Path target;

        private EntryDraft(
                Path source,
                String currentFileName,
                Optional<String> extractedTitle,
                RenameStatus status,
                String note,
                String desiredFileName) {
            this.source = source;
            this.currentFileName = currentFileName;
            this.extractedTitle = extractedTitle;
            this.status = status;
            this.note = note;
            this.desiredFileName = desiredFileName;
        }

        private static EntryDraft ready(
                Path source, String currentFileName, Optional<String> title, String desiredFileName) {
            return new EntryDraft(
                    source, currentFileName, title, RenameStatus.READY, "Ready to rename", desiredFileName);
        }

        private static EntryDraft skipped(
                Path source,
                String currentFileName,
                Optional<String> title,
                RenameStatus status,
                String note) {
            return new EntryDraft(source, currentFileName, title, status, note, null);
        }

        private static EntryDraft error(Path source, String currentFileName, String note) {
            return new EntryDraft(
                    source, currentFileName, Optional.empty(), RenameStatus.ERROR, note, null);
        }

        private RenamePlanEntry toEntry() {
            return new RenamePlanEntry(
                    source,
                    currentFileName,
                    extractedTitle,
                    Optional.ofNullable(target),
                    status,
                    note);
        }
    }
}
