package com.rezami.pdfmanager.util;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

public final class SwingTaskRunner implements TaskRunner {
    @Override
    public <T> void runAsync(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onFailure, "onFailure");

        SwingWorker<T, Void> worker =
                new SwingWorker<>() {
                    @Override
                    protected T doInBackground() throws Exception {
                        return task.call();
                    }

                    @Override
                    protected void done() {
                        try {
                            onSuccess.accept(get());
                        } catch (Throwable t) {
                            Throwable cause = t.getCause() == null ? t : t.getCause();
                            onFailure.accept(cause);
                        }
                    }
                };

        worker.execute();
    }
}

