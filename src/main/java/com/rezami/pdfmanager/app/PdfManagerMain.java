package com.rezami.pdfmanager.app;

import com.rezami.pdfmanager.controller.RenameController;
import com.rezami.pdfmanager.service.PdfBoxTitleReader;
import com.rezami.pdfmanager.service.PdfFileScanner;
import com.rezami.pdfmanager.service.PdfRenameService;
import com.rezami.pdfmanager.service.PdfRenamer;
import com.rezami.pdfmanager.service.RenamePlanner;
import com.rezami.pdfmanager.service.RenameService;
import com.rezami.pdfmanager.ui.swing.SwingRenameView;
import com.rezami.pdfmanager.util.FileNameSanitizer;
import com.rezami.pdfmanager.util.SwingTaskRunner;

import javax.swing.UIManager;
import javax.swing.SwingUtilities;

public final class PdfManagerMain {
    private PdfManagerMain() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PdfManagerMain::startSwingApp);
    }

    private static void startSwingApp() {
        setSystemLookAndFeel();

        var scanner = new PdfFileScanner();
        var titleReader = new PdfBoxTitleReader();
        var sanitizer = new FileNameSanitizer();
        var planner = new RenamePlanner(scanner, titleReader, sanitizer);
        var renamer = new PdfRenamer();
        RenameService renameService = new PdfRenameService(planner, renamer);

        var view = new SwingRenameView();
        var taskRunner = new SwingTaskRunner();
        var controller = new RenameController(renameService, taskRunner, view);

        view.setListener(controller);
        view.showWindow();
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep default LAF.
        }
    }
}
