package com.rezami.pdfmanager.ui.swing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.domain.RenamePlanEntry;

final class RenameTableModel extends AbstractTableModel {
    private static final List<String> COLUMNS =
            List.of("Current Name", "Extracted Title", "New Name", "Status", "Note");

    private final List<RenamePlanEntry> rows = new ArrayList<>();

    void setPlan(RenamePlan plan) {
        rows.clear();
        rows.addAll(plan.entries());
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        RenamePlanEntry entry = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> entry.currentFileName();
            case 1 -> entry.extractedTitle().orElse("");
            case 2 -> entry.target().map(Path::getFileName).map(Path::toString).orElse("");
            case 3 -> entry.status().name();
            case 4 -> entry.note();
            default -> "";
        };
    }
}

