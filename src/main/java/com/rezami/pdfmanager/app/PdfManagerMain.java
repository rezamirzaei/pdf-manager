package com.rezami.pdfmanager.app;

import com.rezami.pdfmanager.controller.RenameController;
import com.rezami.pdfmanager.service.PdfTitleReader;
import com.rezami.pdfmanager.service.RenameService;
import com.rezami.pdfmanager.ui.swing.SwingRenameView;
import com.rezami.pdfmanager.util.SwingTaskRunner;

import javax.swing.UIManager;
import javax.swing.SwingUtilities;

/**
 * Main entry point for the PDF Manager application.
 *
 * Usage:
 *   java -jar pdf-manager.jar              # Auto: LLM when Ollama is up, metadata fallback
 *   java -jar pdf-manager.jar --metadata   # Force metadata-based title reading
 *   java -jar pdf-manager.jar --llm        # Use LLM (Ollama) for title generation
 *   java -jar pdf-manager.jar --composite  # Try LLM first, metadata fallback
 */
public final class PdfManagerMain {

    private static final String ARG_METADATA = "--metadata";
    private static final String ARG_LLM = "--llm";
    private static final String ARG_COMPOSITE = "--composite";

    private PdfManagerMain() {}

    public static void main(String[] args) {
        TitleReaderMode mode = parseMode(args);
        SwingUtilities.invokeLater(() -> startSwingApp(mode));
    }

    private static TitleReaderMode parseMode(String[] args) {
        for (String arg : args) {
            if (ARG_METADATA.equalsIgnoreCase(arg)) {
                return TitleReaderMode.METADATA;
            }
            if (ARG_LLM.equalsIgnoreCase(arg)) {
                return TitleReaderMode.LLM;
            }
            if (ARG_COMPOSITE.equalsIgnoreCase(arg)) {
                return TitleReaderMode.COMPOSITE;
            }
        }
        if (TitleReaderFactory.isOllamaAvailable()) {
            return TitleReaderMode.LLM;
        }
        return TitleReaderMode.METADATA;
    }

    private static void startSwingApp(TitleReaderMode mode) {
        setSystemLookAndFeel();

        PdfTitleReader titleReader = createTitleReader(mode);
        RenameService renameService = TitleReaderFactory.createRenameService(titleReader);

        var view = new SwingRenameView();
        var taskRunner = new SwingTaskRunner();
        var controller = new RenameController(renameService, taskRunner, view);

        view.setListener(controller);
        view.showWindow();

        printStartupInfo(mode, titleReader);
    }

    private static PdfTitleReader createTitleReader(TitleReaderMode mode) {
        return switch (mode) {
            case METADATA -> TitleReaderFactory.createMetadataReader();
            case LLM -> {
                if (TitleReaderFactory.isOllamaAvailable()) {
                    System.out.println("Ollama is available. Using LLM-based title generation.");
                    yield TitleReaderFactory.createLlmReader();
                } else {
                    System.out.println("Warning: Ollama is not available. Falling back to metadata reader.");
                    yield TitleReaderFactory.createMetadataReader();
                }
            }
            case COMPOSITE -> {
                System.out.println("Using composite reader (LLM first, metadata fallback).");
                yield TitleReaderFactory.createCompositeReader();
            }
        };
    }

    private static void printStartupInfo(TitleReaderMode mode, PdfTitleReader reader) {
        System.out.println("PDF Manager started with " + mode + " mode.");
        System.out.println("Title reader: " + reader.getClass().getSimpleName());
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep default LAF.
        }
    }

    private enum TitleReaderMode {
        METADATA,
        LLM,
        COMPOSITE
    }
}
