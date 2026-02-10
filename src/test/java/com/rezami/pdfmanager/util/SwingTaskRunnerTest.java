package com.rezami.pdfmanager.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SwingTaskRunnerTest {
    private final SwingTaskRunner taskRunner = new SwingTaskRunner();

    @Test
    void runAsync_callsOnSuccessOnEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> isEdt = new AtomicReference<>();
        AtomicReference<String> value = new AtomicReference<>();

        taskRunner.runAsync(
                () -> "ok",
                result -> {
                    isEdt.set(SwingUtilities.isEventDispatchThread());
                    value.set(result);
                    latch.countDown();
                },
                error -> {
                    isEdt.set(SwingUtilities.isEventDispatchThread());
                    latch.countDown();
                });

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(value.get()).isEqualTo("ok");
        assertThat(isEdt.get()).isTrue();
    }

    @Test
    void runAsync_callsOnFailureWithRootCause() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        taskRunner.runAsync(
                () -> {
                    throw new IOException("boom");
                },
                ignored -> latch.countDown(),
                error -> {
                    errorRef.set(error);
                    latch.countDown();
                });

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(errorRef.get()).isInstanceOf(IOException.class);
        assertThat(errorRef.get().getMessage()).isEqualTo("boom");
    }
}

