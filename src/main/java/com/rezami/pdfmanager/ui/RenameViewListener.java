package com.rezami.pdfmanager.ui;

import java.nio.file.Path;

public interface RenameViewListener {
    void onDirectoryChosen(Path directory);

    void onScanRequested();

    void onRenameRequested();
}

