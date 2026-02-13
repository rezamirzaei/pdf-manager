package com.rezami.pdfmanager.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.ui.RenameView;
import com.rezami.pdfmanager.ui.RenameViewListener;

public final class SwingRenameView extends JFrame implements RenameView {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextField directoryField = new JTextField();
    private final JCheckBox recursiveCheckBox = new JCheckBox("Include subfolders");
    private final JButton browseButton = new JButton("Browse…");
    private final JButton scanButton = new JButton("Scan");
    private final JButton renameButton = new JButton("Rename");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JLabel progressLabel = new JLabel("");

    private final RenameTableModel tableModel = new RenameTableModel();
    private final JTable table = new JTable(tableModel);

    private final JTextArea logArea = new JTextArea();

    private RenameViewListener listener;

    public SwingRenameView() {
        super("PDF Manager — Rename PDFs by Title");
        buildUi();
    }

    @Override
    public void setListener(RenameViewListener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void showWindow() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void setBusy(boolean busy) {
        SwingUtilities.invokeLater(
                () -> {
                    browseButton.setEnabled(!busy);
                    scanButton.setEnabled(!busy);
                    renameButton.setEnabled(!busy);
                    recursiveCheckBox.setEnabled(!busy);
                    if (!busy) {
                        progress.setValue(0);
                        progress.setIndeterminate(false);
                        progressLabel.setText("");
                    }
                });
    }

    @Override
    public void setProgress(int current, int total, String message) {
        SwingUtilities.invokeLater(() -> {
            if (total > 0) {
                int percent = (int) ((current * 100.0) / total);
                progress.setIndeterminate(false);
                progress.setValue(percent);
                progressLabel.setText(current + "/" + total);
            } else {
                progress.setIndeterminate(true);
                progressLabel.setText(message);
            }
        });
    }

    @Override
    public void setDirectory(Path directory) {
        directoryField.setText(directory == null ? "" : directory.toString());
    }

    @Override
    public boolean isRecursiveSelected() {
        return recursiveCheckBox.isSelected();
    }

    @Override
    public void setPlan(RenamePlan plan) {
        tableModel.setPlan(plan);
    }

    @Override
    public void appendLog(String message) {
        String line = "[" + TIME.format(LocalTime.now()) + "] " + message;
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    @Override
    public void showError(String title, String message, Throwable cause) {
        String full =
                cause == null
                        ? message
                        : message + "\n\n" + cause.getClass().getSimpleName() + ": " + cause.getMessage();
        JOptionPane.showMessageDialog(this, full, title, JOptionPane.ERROR_MESSAGE);
    }

    private void buildUi() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        directoryField.setEditable(false);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(buildFolderPanel(), BorderLayout.NORTH);
        top.add(buildActionsPanel(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Preview"));
        add(tableScroll, BorderLayout.CENTER);

        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(900, 180));
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
        add(logScroll, BorderLayout.SOUTH);
    }

    private JPanel buildFolderPanel() {
        JPanel folder = new JPanel(new BorderLayout(10, 10));
        folder.add(new JLabel("Folder:"), BorderLayout.WEST);
        folder.add(directoryField, BorderLayout.CENTER);
        folder.add(browseButton, BorderLayout.EAST);

        browseButton.addActionListener(
                e -> {
                    if (listener == null) {
                        return;
                    }
                    Path directory = chooseDirectory();
                    if (directory != null) {
                        listener.onDirectoryChosen(directory);
                    }
                });
        return folder;
    }

    private JPanel buildActionsPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.add(recursiveCheckBox);
        actions.add(scanButton);
        actions.add(renameButton);
        actions.add(progress);
        actions.add(progressLabel);

        progress.setPreferredSize(new Dimension(200, 20));
        progress.setStringPainted(true);
        progressLabel.setPreferredSize(new Dimension(80, 20));

        scanButton.addActionListener(
                e -> {
                    if (listener != null) {
                        listener.onScanRequested();
                    }
                });

        renameButton.addActionListener(
                e -> {
                    if (listener == null) {
                        return;
                    }
                    int answer =
                            JOptionPane.showConfirmDialog(
                                    this,
                                    "This will rename PDF files on disk.\n\nProceed?",
                                    "Confirm rename",
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.WARNING_MESSAGE);
                    if (answer == JOptionPane.OK_OPTION) {
                        listener.onRenameRequested();
                    }
                });

        return actions;
    }

    private Path chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select folder containing PDFs");
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile().toPath();
    }
}

