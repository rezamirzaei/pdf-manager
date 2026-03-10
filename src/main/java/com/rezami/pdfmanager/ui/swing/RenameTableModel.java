package com.rezami.pdfmanager.ui.swing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.domain.RenamePlanEntry;
import com.rezami.pdfmanager.domain.RenameStatus;

final class RenameTableModel extends AbstractTableModel {
    private static final List<String> COLUMNS =
            List.of("Rename", "Current Name", "Extracted Title", "New Name", "Status", "Note");

    private final List<Row> rows = new ArrayList<>();

    void setPlan(RenamePlan plan) {
        rows.clear();
        plan.entries().forEach(entry -> rows.add(new Row(entry, entry.isReady())));
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.size();
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS.get(column);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0 && rows.get(rowIndex).entry().isReady();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (!isCellEditable(rowIndex, columnIndex) || !(value instanceof Boolean selected)) {
            return;
        }

        Row row = rows.get(rowIndex);
        if (row.selected() == selected) {
            return;
        }

        rows.set(rowIndex, row.withSelected(selected));
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = rows.get(rowIndex);
        RenamePlanEntry entry = row.entry();

        return switch (columnIndex) {
            case 0 -> row.selected();
            case 1 -> entry.currentFileName();
            case 2 -> entry.extractedTitle().orElse("");
            case 3 -> entry.target().map(Path::getFileName).map(Path::toString).orElse("");
            case 4 -> formatStatus(entry.status());
            case 5 -> entry.note();
            default -> "";
        };
    }

    int selectedReadyCount() {
        return (int) rows.stream().filter(row -> row.selected() && row.entry().isReady()).count();
    }

    Set<Path> selectedReadySources() {
        return rows.stream()
                .filter(row -> row.selected() && row.entry().isReady())
                .map(row -> row.entry().source())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String formatStatus(RenameStatus status) {
        return switch (status) {
            case READY -> "Ready";
            case SKIPPED_NO_TITLE -> "Skipped: no title";
            case SKIPPED_SAME_NAME -> "Skipped: already matches";
            case ERROR -> "Error";
        };
    }

    private record Row(RenamePlanEntry entry, boolean selected) {
        private Row withSelected(boolean selected) {
            return new Row(entry, selected);
        }
    }
}
