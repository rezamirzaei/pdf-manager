package com.rezami.pdfmanager.ui;

import java.nio.file.Path;

import com.rezami.pdfmanager.domain.RenamePlan;

public interface RenameView {
    void setListener(RenameViewListener listener);

    void showWindow();

    void setBusy(boolean busy);

    void setProgress(int current, int total, String message);

    void setDirectory(Path directory);

    void setRecursiveSelected(boolean recursiveSelected);

    boolean isRecursiveSelected();

    void setPlan(RenamePlan plan);

    void appendLog(String message);

    void showError(String title, String message, Throwable cause);
}
