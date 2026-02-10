package com.rezami.pdfmanager.domain;

public enum RenameStatus {
    READY(false),
    SKIPPED_NO_TITLE(true),
    SKIPPED_SAME_NAME(true),
    ERROR(false);

    private final boolean skipped;

    RenameStatus(boolean skipped) {
        this.skipped = skipped;
    }

    public boolean isSkipped() {
        return skipped;
    }
}

