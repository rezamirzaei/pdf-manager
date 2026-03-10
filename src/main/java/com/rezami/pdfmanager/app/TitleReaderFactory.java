package com.rezami.pdfmanager.app;

import com.rezami.pdfmanager.llm.EmbeddedLlamaClient;
import com.rezami.pdfmanager.llm.OllamaClient;
import com.rezami.pdfmanager.ocr.PdfBoxTextExtractor;
import com.rezami.pdfmanager.ocr.PdfTextExtractor;
import com.rezami.pdfmanager.service.CompositeTitleReader;
import com.rezami.pdfmanager.service.LlmTitleReader;
import com.rezami.pdfmanager.service.PdfBoxTitleReader;
import com.rezami.pdfmanager.service.PdfFileScanner;
import com.rezami.pdfmanager.service.PdfRenameService;
import com.rezami.pdfmanager.service.PdfRenamer;
import com.rezami.pdfmanager.service.PdfTitleReader;
import com.rezami.pdfmanager.service.RenamePlanner;
import com.rezami.pdfmanager.service.RenameService;
import com.rezami.pdfmanager.util.FileNameSanitizer;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Factory for creating application components with different configurations.
 * Supports both metadata-based and LLM-based title reading strategies.
 */
public final class TitleReaderFactory {
    private static final Logger LOGGER = Logger.getLogger(TitleReaderFactory.class.getName());

    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3.2:1b";
    private static final String ENV_OLLAMA_URL = "PDF_MANAGER_OLLAMA_URL";
    private static final String ENV_OLLAMA_MODEL = "PDF_MANAGER_OLLAMA_MODEL";
    private static final List<String> FALLBACK_MODEL_PREFERENCE = List.of(
            "llama3.2:1b",
            "llama3.2:3b",
            "llama3.1:8b",
            "llama3:8b",
            "llama3"
    );
    private static final int DEFAULT_MAX_TITLE_LENGTH = 100;
    private static final int DEFAULT_LOCAL_MAX_TEXT_CHARS = 700;
    private static final int DEFAULT_REMOTE_MAX_TEXT_CHARS = 700;

    private TitleReaderFactory() {}

    /**
     * Creates the default metadata-based title reader.
     * Reads titles from PDF Document Information or XMP metadata.
     */
    public static PdfTitleReader createMetadataReader() {
        return new PdfBoxTitleReader();
    }

    public static PdfTitleReader createLocalLlmReader() {
        PdfTextExtractor textExtractor = new PdfBoxTextExtractor();
        EmbeddedLlamaClient localClient = new EmbeddedLlamaClient();
        if (isIntelMac()) {
            LOGGER.warning("Embedded llama.cpp on Intel macOS is not fully validated; verify generated titles carefully.");
        }
        registerShutdownHook(localClient);
        return new LlmTitleReader(textExtractor, localClient, DEFAULT_MAX_TITLE_LENGTH, DEFAULT_LOCAL_MAX_TEXT_CHARS);
    }

    /**
     * Backward-compatible alias for the embedded local LLM mode.
     */
    public static PdfTitleReader createSmartReader() {
        return createLocalLlmReader();
    }

    /**
     * Creates the default built-in reader: metadata first, embedded local LLM as fallback.
     */
    public static PdfTitleReader createBuiltInReader() {
        PdfBoxTitleReader metadataReader = new PdfBoxTitleReader();
        return new CompositeTitleReader(createLocalLlmReader(), metadataReader, true);
    }

    /**
     * Creates an LLM-based title reader that extracts text with PDFBox and uses Ollama.
     * Falls back to metadata if LLM is unavailable.
     */
    public static PdfTitleReader createLlmReader() {
        String resolvedUrl = envOrDefault(ENV_OLLAMA_URL, DEFAULT_OLLAMA_URL);
        String requestedModel = envOrDefault(ENV_OLLAMA_MODEL, DEFAULT_MODEL);
        return createLlmReader(resolvedUrl, requestedModel);
    }

    /**
     * Creates an LLM-based title reader with custom Ollama configuration.
     */
    public static PdfTitleReader createLlmReader(String ollamaUrl, String model) {
        PdfTextExtractor textExtractor = new PdfBoxTextExtractor();
        Optional<OllamaClient> llmClient = createAvailableOllamaClient(ollamaUrl, model);
        if (llmClient.isEmpty()) {
            LOGGER.warning("Ollama is unavailable; using metadata reader instead of LLM reader.");
            return createMetadataReader();
        }

        return new LlmTitleReader(
                textExtractor,
                llmClient.orElseThrow(),
                DEFAULT_MAX_TITLE_LENGTH,
                DEFAULT_REMOTE_MAX_TEXT_CHARS);
    }

    /**
     * Creates a composite reader that tries LLM first, then falls back to metadata.
     * This is the recommended configuration for best results.
     */
    public static PdfTitleReader createCompositeReader() {
        String resolvedUrl = envOrDefault(ENV_OLLAMA_URL, DEFAULT_OLLAMA_URL);
        String requestedModel = envOrDefault(ENV_OLLAMA_MODEL, DEFAULT_MODEL);
        return createCompositeReader(resolvedUrl, requestedModel, false);
    }

    /**
     * Creates a composite reader with custom configuration.
     *
     * @param ollamaUrl the Ollama server URL
     * @param model the model to use
     * @param preferMetadata if true, try metadata first and only use LLM as fallback
     */
    public static PdfTitleReader createCompositeReader(String ollamaUrl, String model, boolean preferMetadata) {
        PdfBoxTitleReader metadataReader = new PdfBoxTitleReader();
        Optional<OllamaClient> llmClient = createAvailableOllamaClient(ollamaUrl, model);
        if (llmClient.isEmpty()) {
            LOGGER.warning("Ollama is unavailable; using metadata reader instead of composite reader.");
            return metadataReader;
        }

        PdfTextExtractor textExtractor = new PdfBoxTextExtractor();
        LlmTitleReader llmReader = new LlmTitleReader(textExtractor, llmClient.orElseThrow(),
                DEFAULT_MAX_TITLE_LENGTH, DEFAULT_REMOTE_MAX_TEXT_CHARS);

        return new CompositeTitleReader(llmReader, metadataReader, preferMetadata);
    }

    /**
     * Creates a RenamePlanner with the specified title reader.
     */
    public static RenamePlanner createPlanner(PdfTitleReader titleReader) {
        return new RenamePlanner(
                new PdfFileScanner(),
                titleReader,
                new FileNameSanitizer()
        );
    }

    /**
     * Creates a complete RenameService with the specified title reader.
     */
    public static RenameService createRenameService(PdfTitleReader titleReader) {
        RenamePlanner planner = createPlanner(titleReader);
        PdfRenamer renamer = new PdfRenamer();
        return new PdfRenameService(planner, renamer);
    }

    /**
     * Checks if Ollama is available at the default URL.
     */
    public static boolean isOllamaAvailable() {
        return isOllamaAvailable(DEFAULT_OLLAMA_URL);
    }

    /**
     * Checks if Ollama is available at the specified URL.
     */
    public static boolean isOllamaAvailable(String ollamaUrl) {
        OllamaClient client = new OllamaClient(ollamaUrl, DEFAULT_MODEL);
        return client.isAvailable();
    }

    private static Optional<OllamaClient> createAvailableOllamaClient(String ollamaUrl, String requestedModel) {
        Objects.requireNonNull(ollamaUrl, "ollamaUrl");
        Objects.requireNonNull(requestedModel, "requestedModel");

        String trimmedUrl = ollamaUrl.trim();
        String trimmedRequestedModel = requestedModel.trim();
        OllamaClient probeClient = new OllamaClient(trimmedUrl, trimmedRequestedModel);
        if (!probeClient.isAvailable()) {
            return Optional.empty();
        }

        List<String> availableModels = probeClient.listModels();
        if (availableModels.isEmpty()) {
            return Optional.of(probeClient);
        }

        String resolvedModel = resolveModelName(availableModels, trimmedRequestedModel);
        if (!resolvedModel.equalsIgnoreCase(trimmedRequestedModel)) {
            LOGGER.warning(
                    "Ollama model '" + trimmedRequestedModel + "' is unavailable. Using '" + resolvedModel + "'.");
        }
        return Optional.of(new OllamaClient(trimmedUrl, resolvedModel));
    }

    private static String resolveModelName(List<String> availableModels, String requestedModel) {
        String directMatch = findCaseInsensitiveMatch(availableModels, requestedModel);
        if (directMatch != null) {
            return directMatch;
        }

        for (String preferred : FALLBACK_MODEL_PREFERENCE) {
            String preferredMatch = findCaseInsensitiveMatch(availableModels, preferred);
            if (preferredMatch != null) {
                return preferredMatch;
            }
        }

        for (String modelName : availableModels) {
            if (modelName.toLowerCase(Locale.ROOT).contains("llama")) {
                return modelName;
            }
        }

        return availableModels.getFirst();
    }

    private static String findCaseInsensitiveMatch(List<String> values, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String normalized = candidate.trim();
        for (String value : values) {
            if (value.equalsIgnoreCase(normalized)) {
                return value;
            }
        }
        return null;
    }

    private static String envOrDefault(String key, String fallback) {
        String envValue = System.getenv(key);
        if (envValue == null || envValue.isBlank()) {
            return fallback;
        }
        return envValue.trim();
    }

    private static boolean isIntelMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")
                && System.getProperty("os.arch", "").equalsIgnoreCase("x86_64");
    }

    private static void registerShutdownHook(EmbeddedLlamaClient localClient) {
        Runtime.getRuntime().addShutdownHook(new Thread(localClient::close, "pdf-manager-embedded-llm-shutdown"));
    }
}
