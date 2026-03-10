package com.rezami.pdfmanager.app;

import com.rezami.pdfmanager.controller.RenameController;
import com.rezami.pdfmanager.service.CompositeTitleReader;
import com.rezami.pdfmanager.service.LlmTitleReader;
import com.rezami.pdfmanager.service.PdfTitleReader;
import com.rezami.pdfmanager.service.RenameService;
import com.rezami.pdfmanager.ui.swing.SwingRenameView;
import com.rezami.pdfmanager.util.SwingTaskRunner;
import com.rezami.pdfmanager.util.UserPreferences;

import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the PDF Manager application.
 *
 * Usage:
 *   java -jar pdf-manager.jar                  # Auto: metadata first, embedded local LLM fallback
 *   java -jar pdf-manager.jar --metadata   # Force metadata-based title reading
 *   java -jar pdf-manager.jar --local-llm  # Use embedded local llama.cpp model
 *   java -jar pdf-manager.jar --smart      # Backward-compatible alias for --local-llm
 *   java -jar pdf-manager.jar --llm        # Use Ollama for title generation
 *   java -jar pdf-manager.jar --composite  # Try Ollama first, metadata fallback
 */
public final class PdfManagerMain {
    private static final Logger LOGGER = Logger.getLogger(PdfManagerMain.class.getName());

    private static final String ARG_METADATA = "--metadata";
    private static final String ARG_LOCAL_LLM = "--local-llm";
    private static final String ARG_SMART = "--smart";
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
            if (ARG_LOCAL_LLM.equalsIgnoreCase(arg) || ARG_SMART.equalsIgnoreCase(arg)) {
                return TitleReaderMode.LOCAL_LLM;
            }
            if (ARG_LLM.equalsIgnoreCase(arg)) {
                return TitleReaderMode.LLM;
            }
            if (ARG_COMPOSITE.equalsIgnoreCase(arg)) {
                return TitleReaderMode.COMPOSITE;
            }
        }
        return TitleReaderMode.AUTO;
    }

    private static void startSwingApp(TitleReaderMode mode) {
        setSystemLookAndFeel();

        PdfTitleReader titleReader = createTitleReader(mode);
        RenameService renameService = TitleReaderFactory.createRenameService(titleReader);

        var preferences = new UserPreferences();
        var view = new SwingRenameView(preferences);
        var taskRunner = new SwingTaskRunner();
        var controller = new RenameController(renameService, taskRunner, view);

        view.setListener(controller);
        view.setRecursiveSelected(preferences.loadRecursiveSelected());
        preferences.loadLastDirectory().ifPresent(controller::onDirectoryChosen);
        view.showWindow();

        printStartupInfo(mode, titleReader);
    }

    private static PdfTitleReader createTitleReader(TitleReaderMode mode) {
        return switch (mode) {
            case AUTO -> {
                PdfTitleReader reader = TitleReaderFactory.createBuiltInReader();
                LOGGER.info("Auto mode selected metadata with embedded local LLM fallback.");
                yield reader;
            }
            case METADATA -> TitleReaderFactory.createMetadataReader();
            case LOCAL_LLM -> {
                PdfTitleReader reader = TitleReaderFactory.createLocalLlmReader();
                if (reader instanceof LlmTitleReader llmReader) {
                    LOGGER.info("Using embedded local llama.cpp model " + llmReader.getModelName() + ".");
                }
                yield reader;
            }
            case LLM -> {
                PdfTitleReader reader = TitleReaderFactory.createLlmReader();
                if (reader instanceof LlmTitleReader llmReader) {
                    LOGGER.info("Ollama is available. Using LLM-based title generation with model " + llmReader.getModelName() + ".");
                } else {
                    LOGGER.warning("Ollama is not available. Falling back to metadata reader.");
                }
                yield reader;
            }
            case COMPOSITE -> {
                PdfTitleReader reader = TitleReaderFactory.createCompositeReader();
                if (reader instanceof CompositeTitleReader) {
                    LOGGER.info("Using composite reader (LLM first, metadata fallback).");
                } else {
                    LOGGER.warning("Ollama is not available. Using metadata reader for composite mode.");
                }
                yield reader;
            }
        };
    }

    private static void printStartupInfo(TitleReaderMode mode, PdfTitleReader reader) {
        LOGGER.info("PDF Manager started with " + mode + " mode.");
        LOGGER.info("Title reader: " + reader.getClass().getSimpleName());
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not set system look and feel; using default.", e);
        }
    }

    private enum TitleReaderMode {
        AUTO,
        METADATA,
        LOCAL_LLM,
        LLM,
        COMPOSITE
    }
}
