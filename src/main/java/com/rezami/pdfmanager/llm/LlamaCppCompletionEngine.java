package com.rezami.pdfmanager.llm;

import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.ModelParameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

final class LlamaCppCompletionEngine implements LocalCompletionEngine {
    private static final int CONTEXT_SIZE = 1024;
    private static final int BATCH_SIZE = 256;
    private static final float TEMPERATURE = 0.1f;
    private static final int TOP_K = 30;
    private static final float TOP_P = 0.9f;
    private static final float REPEAT_PENALTY = 1.1f;

    private final LlamaModel model;

    LlamaCppCompletionEngine(Path modelPath) throws IOException {
        Objects.requireNonNull(modelPath, "modelPath");

        try {
            int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
            ModelParameters parameters = new ModelParameters()
                    .setModel(modelPath.toAbsolutePath().toString())
                    .setCtxSize(CONTEXT_SIZE)
                    .setThreads(threads)
                    .setThreadsBatch(threads)
                    .setBatchSize(BATCH_SIZE)
                    .disableLog();
            this.model = new LlamaModel(parameters);
        } catch (RuntimeException | UnsatisfiedLinkError e) {
            throw new IOException("Could not load embedded llama.cpp model from " + modelPath, e);
        }
    }

    @Override
    public String complete(String prompt, int maxOutputTokens) throws IOException {
        Objects.requireNonNull(prompt, "prompt");
        if (maxOutputTokens <= 0) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }

        try {
            InferenceParameters parameters = new InferenceParameters(prompt)
                    .setNPredict(maxOutputTokens)
                    .setTemperature(TEMPERATURE)
                    .setTopK(TOP_K)
                    .setTopP(TOP_P)
                    .setRepeatPenalty(REPEAT_PENALTY)
                    .setStopStrings("\n", "\r");
            return model.complete(parameters);
        } catch (RuntimeException e) {
            throw new IOException("Embedded llama.cpp inference failed", e);
        }
    }

    @Override
    public void close() {
        model.close();
    }

    static final class Factory implements LocalCompletionEngine.Factory {
        @Override
        public LocalCompletionEngine create(Path modelPath) throws IOException {
            return new LlamaCppCompletionEngine(modelPath);
        }
    }
}
