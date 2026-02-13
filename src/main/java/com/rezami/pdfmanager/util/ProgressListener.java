package com.rezami.pdfmanager.util;

/**
 * Callback interface for tracking progress of long-running operations.
 */
@FunctionalInterface
public interface ProgressListener {

    /**
     * Called when progress is updated.
     *
     * @param current the current item number (1-based)
     * @param total the total number of items
     * @param message a description of the current operation
     */
    void onProgress(int current, int total, String message);

    /**
     * Returns a no-op progress listener.
     */
    static ProgressListener none() {
        return (current, total, message) -> {};
    }
}

