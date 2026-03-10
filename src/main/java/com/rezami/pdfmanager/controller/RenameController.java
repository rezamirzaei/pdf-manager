package com.rezami.pdfmanager.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.service.RenameService;
import com.rezami.pdfmanager.ui.RenameView;
import com.rezami.pdfmanager.ui.RenameViewListener;
import com.rezami.pdfmanager.util.ProgressListener;
import com.rezami.pdfmanager.util.TaskRunner;

public final class RenameController implements RenameViewListener {
    private final RenameService renameService;
    private final TaskRunner taskRunner;
    private final RenameView view;

    private Path directory;
    private RenamePlan latestPlan;

    public RenameController(RenameService renameService, TaskRunner taskRunner, RenameView view) {
        this.renameService = Objects.requireNonNull(renameService, "renameService");
        this.taskRunner = Objects.requireNonNull(taskRunner, "taskRunner");
        this.view = Objects.requireNonNull(view, "view");
    }

    @Override
    public void onDirectoryChosen(Path directory) {
        this.directory = directory;
        this.latestPlan = null;

        view.setDirectory(directory);
        view.setPlan(new RenamePlan(directory, view.isRecursiveSelected(), java.util.List.of()));
        view.appendLog("Selected directory: " + directory);
    }

    @Override
    public void onScanRequested() {
        if (directory == null) {
            view.showError("No folder selected", "Choose a folder first.", null);
            return;
        }

        boolean recursive = view.isRecursiveSelected();
        view.setBusy(true);
        view.appendLog("Scanning…");

        // Create progress listener that updates the UI
        ProgressListener progressListener = (current, total, message) -> {
            view.setProgress(current, total, message);
        };

        taskRunner.runAsync(
                () -> renameService.plan(directory, recursive, progressListener),
                plan -> {
                    latestPlan = plan;
                    view.setPlan(plan);
                    view.appendLog(
                            "Scan complete: "
                                    + plan.readyCount()
                                    + " ready, "
                                    + plan.skippedCount()
                                    + " skipped, "
                                    + plan.errorCount()
                                    + " errors.");
                    view.setBusy(false);
                },
                error -> {
                    view.showError("Scan failed", "Could not scan folder.", error);
                    view.appendLog("Scan failed: " + error.getMessage());
                    view.setBusy(false);
                });
    }

    @Override
    public void onRenameRequested() {
        if (directory == null) {
            view.showError("No folder selected", "Choose a folder first.", null);
            return;
        }
        if (latestPlan == null) {
            view.showError("Nothing to rename", "Scan the folder first.", null);
            return;
        }
        if (latestPlan.readyCount() == 0) {
            view.appendLog("Nothing to rename.");
            return;
        }
        Set<Path> selectedSources = view.selectedReadySources();
        if (selectedSources.isEmpty()) {
            view.appendLog("No PDFs selected for rename.");
            return;
        }

        RenamePlan planToExecute = latestPlan.filterReadySources(selectedSources);
        boolean recursive = view.isRecursiveSelected();
        view.setBusy(true);
        view.appendLog("Renaming " + planToExecute.readyCount() + " selected PDF(s)…");

        taskRunner.runAsync(
                () -> {
                    renameService.execute(planToExecute);
                    return renameService.plan(directory, recursive);
                },
                refreshedPlan -> {
                    latestPlan = refreshedPlan;
                    view.setPlan(refreshedPlan);
                    view.appendLog("Rename complete.");
                    view.setBusy(false);
                },
                error -> {
                    view.showError("Rename failed", "Some files could not be renamed.", error);
                    view.appendLog("Rename failed: " + error.getMessage());
                    view.setBusy(false);
                });
    }
}
