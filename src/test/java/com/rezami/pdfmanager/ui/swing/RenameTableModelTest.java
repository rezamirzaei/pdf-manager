package com.rezami.pdfmanager.ui.swing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.domain.RenamePlanEntry;
import com.rezami.pdfmanager.domain.RenameStatus;
import org.junit.jupiter.api.Test;

class RenameTableModelTest {
    @Test
    void setPlan_populatesTableRows() {
        Path dir = Path.of("dir");
        RenamePlan plan =
                new RenamePlan(
                        dir,
                        false,
                        List.of(
                                new RenamePlanEntry(
                                        dir.resolve("a.pdf"),
                                        "a.pdf",
                                        Optional.of("A"),
                                        Optional.of(dir.resolve("A.pdf")),
                                        RenameStatus.READY,
                                        "Ready"),
                                new RenamePlanEntry(
                                        dir.resolve("b.pdf"),
                                        "b.pdf",
                                        Optional.empty(),
                                        Optional.empty(),
                                        RenameStatus.SKIPPED_NO_TITLE,
                                        "No title")));

        RenameTableModel model = new RenameTableModel();
        model.setPlan(plan);

        assertThat(model.getRowCount()).isEqualTo(2);
        assertThat(model.getColumnCount()).isEqualTo(5);

        assertThat(model.getValueAt(0, 0)).isEqualTo("a.pdf");
        assertThat(model.getValueAt(0, 1)).isEqualTo("A");
        assertThat(model.getValueAt(0, 2)).isEqualTo("A.pdf");
        assertThat(model.getValueAt(0, 3)).isEqualTo("READY");
        assertThat(model.getValueAt(0, 4)).isEqualTo("Ready");

        assertThat(model.getValueAt(1, 0)).isEqualTo("b.pdf");
        assertThat(model.getValueAt(1, 1)).isEqualTo("");
        assertThat(model.getValueAt(1, 2)).isEqualTo("");
        assertThat(model.getValueAt(1, 3)).isEqualTo("SKIPPED_NO_TITLE");
        assertThat(model.getValueAt(1, 4)).isEqualTo("No title");
    }
}

