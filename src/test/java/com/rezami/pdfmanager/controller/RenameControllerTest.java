package com.rezami.pdfmanager.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.domain.RenamePlanEntry;
import com.rezami.pdfmanager.domain.RenameStatus;
import com.rezami.pdfmanager.service.RenameService;
import com.rezami.pdfmanager.ui.RenameView;
import com.rezami.pdfmanager.util.ProgressListener;
import com.rezami.pdfmanager.util.TaskRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RenameControllerTest {
    @Mock RenameService renameService;
    @Mock RenameView view;

    @Test
    void onDirectoryChosen_updatesViewAndClearsPlan() {
        when(view.isRecursiveSelected()).thenReturn(false);

        TaskRunner taskRunner = new DirectTaskRunner();
        RenameController controller = new RenameController(renameService, taskRunner, view);

        Path dir = Path.of("some-dir");
        controller.onDirectoryChosen(dir);

        verify(view).setDirectory(dir);
        ArgumentCaptor<RenamePlan> captor = ArgumentCaptor.forClass(RenamePlan.class);
        verify(view).setPlan(captor.capture());
        assertThat(captor.getValue().rootDirectory()).isEqualTo(dir);
        assertThat(captor.getValue().entries()).isEmpty();
        verify(view).appendLog(contains("Selected directory"));
    }

    @Test
    void onScanRequested_withoutDirectory_showsError() {
        TaskRunner taskRunner = new DirectTaskRunner();
        RenameController controller = new RenameController(renameService, taskRunner, view);

        controller.onScanRequested();

        verify(view).showError("No folder selected", "Choose a folder first.", null);
        verifyNoInteractions(renameService);
    }

    @Test
    void onScanRequested_plansAndUpdatesView() throws IOException {
        when(view.isRecursiveSelected()).thenReturn(false);

        TaskRunner taskRunner = new DirectTaskRunner();
        RenameController controller = new RenameController(renameService, taskRunner, view);

        Path dir = Path.of("dir");
        controller.onDirectoryChosen(dir);

        RenamePlan planned = planWithReadyCount(dir, 1);
        when(renameService.plan(eq(dir), eq(false), any(ProgressListener.class))).thenReturn(planned);

        controller.onScanRequested();

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setBusy(true);
        inOrder.verify(view).appendLog("Scanning…");
        inOrder.verify(view).setPlan(planned);
        inOrder.verify(view).appendLog(contains("Scan complete:"));
        inOrder.verify(view).setBusy(false);
    }

    @Test
    void onRenameRequested_withoutScan_showsError() throws IOException {
        when(view.isRecursiveSelected()).thenReturn(false);

        TaskRunner taskRunner = new DirectTaskRunner();
        RenameController controller = new RenameController(renameService, taskRunner, view);

        controller.onDirectoryChosen(Path.of("dir"));
        controller.onRenameRequested();

        verify(view).showError("Nothing to rename", "Scan the folder first.", null);
        verify(renameService, never()).execute(any());
    }

    @Test
    void onRenameRequested_whenNothingReady_logsAndDoesNotExecute() throws IOException {
        when(view.isRecursiveSelected()).thenReturn(false);

        TaskRunner taskRunner = new DirectTaskRunner();
        RenameController controller = new RenameController(renameService, taskRunner, view);

        Path dir = Path.of("dir");
        controller.onDirectoryChosen(dir);

        RenamePlan planned = planWithReadyCount(dir, 0);
        when(renameService.plan(eq(dir), eq(false), any(ProgressListener.class))).thenReturn(planned);

        controller.onScanRequested();
        controller.onRenameRequested();

        verify(view).appendLog("Nothing to rename.");
        verify(renameService, never()).execute(any());
    }

    @Test
    void onRenameRequested_whenNothingSelected_logsAndDoesNotExecute() throws IOException {
        when(view.isRecursiveSelected()).thenReturn(false);
        when(view.selectedReadySources()).thenReturn(Set.of());

        TaskRunner taskRunner = new DirectTaskRunner();
        RenameController controller = new RenameController(renameService, taskRunner, view);

        Path dir = Path.of("dir");
        controller.onDirectoryChosen(dir);

        RenamePlan planned = planWithReadyCount(dir, 1);
        when(renameService.plan(eq(dir), eq(false), any(ProgressListener.class))).thenReturn(planned);

        controller.onScanRequested();
        controller.onRenameRequested();

        verify(view).appendLog("No PDFs selected for rename.");
        verify(renameService, never()).execute(any());
    }

    @Test
    void onRenameRequested_executesOnlySelectedEntriesAndRefreshesPlan() throws IOException {
        when(view.isRecursiveSelected()).thenReturn(false);

        TaskRunner taskRunner = new DirectTaskRunner();
        RenameController controller = new RenameController(renameService, taskRunner, view);

        Path dir = Path.of("dir");
        Path selectedSource = dir.resolve("a.pdf");
        when(view.selectedReadySources()).thenReturn(Set.of(selectedSource));

        controller.onDirectoryChosen(dir);

        RenamePlan initial =
                new RenamePlan(
                        dir,
                        false,
                        List.of(
                                readyEntry(dir.resolve("a.pdf"), dir.resolve("A.pdf")),
                                readyEntry(dir.resolve("b.pdf"), dir.resolve("B.pdf"))));
        RenamePlan refreshed = planWithReadyCount(dir, 0);

        when(renameService.plan(eq(dir), eq(false), any(ProgressListener.class))).thenReturn(initial);
        when(renameService.plan(dir, false)).thenReturn(refreshed);

        controller.onScanRequested();
        clearInvocations(view);
        controller.onRenameRequested();

        verify(renameService).execute(initial.filterReadySources(Set.of(selectedSource)));

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setBusy(true);
        inOrder.verify(view).appendLog("Renaming 1 selected PDF(s)…");
        inOrder.verify(view).setPlan(refreshed);
        inOrder.verify(view).appendLog("Rename complete.");
        inOrder.verify(view).setBusy(false);
    }

    private static RenamePlan planWithReadyCount(Path dir, int readyCount) {
        List<RenamePlanEntry> entries =
                readyCount == 0
                        ? List.of(
                                new RenamePlanEntry(
                                        dir.resolve("a.pdf"),
                                        "a.pdf",
                                        Optional.empty(),
                                        Optional.empty(),
                                        RenameStatus.SKIPPED_NO_TITLE,
                                        "No title"))
                        : List.of(readyEntry(dir.resolve("a.pdf"), dir.resolve("A.pdf")));
        return new RenamePlan(dir, false, entries);
    }

    private static RenamePlanEntry readyEntry(Path source, Path target) {
        return new RenamePlanEntry(
                source,
                source.getFileName().toString(),
                Optional.of(source.getFileName().toString()),
                Optional.of(target),
                RenameStatus.READY,
                "Ready");
    }

    private static final class DirectTaskRunner implements TaskRunner {
        @Override
        public <T> void runAsync(Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
            try {
                onSuccess.accept(task.call());
            } catch (Throwable t) {
                onFailure.accept(t);
            }
        }
    }
}
