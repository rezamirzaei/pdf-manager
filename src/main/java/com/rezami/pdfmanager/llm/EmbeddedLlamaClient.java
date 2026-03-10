package com.rezami.pdfmanager.llm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EmbeddedLlamaClient implements LlmClient, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(EmbeddedLlamaClient.class.getName());

    private final LocalModelResolver modelResolver;
    private final LocalCompletionEngine.Factory engineFactory;
    private final String modelName;

    private LocalCompletionEngine engine;

    public EmbeddedLlamaClient() {
        this(new LocalModelResolver(), new LlamaCppCompletionEngine.Factory(), LocalModelResolver.DEFAULT_MODEL_NAME);
    }

    EmbeddedLlamaClient(LocalModelResolver modelResolver,
                        LocalCompletionEngine.Factory engineFactory,
                        String modelName) {
        this.modelResolver = Objects.requireNonNull(modelResolver, "modelResolver");
        this.engineFactory = Objects.requireNonNull(engineFactory, "engineFactory");
        this.modelName = Objects.requireNonNull(modelName, "modelName");
    }

    @Override
    public Optional<String> generateTitle(String textContent, int maxTitleLength) throws IOException {
        Objects.requireNonNull(textContent, "textContent");
        if (textContent.isBlank()) {
            return Optional.empty();
        }
        if (maxTitleLength <= 0) {
            throw new IllegalArgumentException("maxTitleLength must be positive");
        }

        String prompt = TitleGenerationSupport.buildTitlePrompt(textContent, maxTitleLength);
        String completion = acquireEngine().complete(prompt, TitleGenerationSupport.outputTokenLimit(maxTitleLength));
        return TitleGenerationSupport.sanitizeTitle(completion, maxTitleLength);
    }

    @Override
    public boolean isAvailable() {
        return modelResolver.hasModelReady();
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public synchronized void close() {
        if (engine == null) {
            return;
        }
        try {
            engine.close();
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "Failed to close embedded local model cleanly.", e);
        } finally {
            engine = null;
        }
    }

    private synchronized LocalCompletionEngine acquireEngine() throws IOException {
        if (engine != null) {
            return engine;
        }

        Path modelPath = modelResolver.ensureModelAvailable();
        LOGGER.info("Loading embedded local model from " + modelPath);
        engine = engineFactory.create(modelPath);
        return engine;
    }
}
