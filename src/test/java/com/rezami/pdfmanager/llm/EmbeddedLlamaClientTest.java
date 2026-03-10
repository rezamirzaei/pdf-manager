package com.rezami.pdfmanager.llm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedLlamaClientTest {
    @TempDir Path tempDir;

    @Test
    void generateTitle_usesEmbeddedEngineAndSanitizesOutput() throws IOException {
        Path modelPath = createFakeModel();
        RecordingEngineFactory engineFactory = new RecordingEngineFactory("Title: Practical PDF Management\nextra");
        EmbeddedLlamaClient client = new EmbeddedLlamaClient(
                resolverFor(modelPath),
                engineFactory,
                "test-model");

        Optional<String> title = client.generateTitle("Sample first page text", 80);

        assertThat(title).contains("Practical PDF Management");
        assertThat(engineFactory.lastPrompt).contains("Sample first page text");
        assertThat(engineFactory.createCount).isEqualTo(1);
    }

    @Test
    void generateTitle_reusesLoadedEngineAcrossCalls() throws IOException {
        Path modelPath = createFakeModel();
        RecordingEngineFactory engineFactory = new RecordingEngineFactory("Generated title");
        EmbeddedLlamaClient client = new EmbeddedLlamaClient(
                resolverFor(modelPath),
                engineFactory,
                "test-model");

        client.generateTitle("First text", 80);
        client.generateTitle("Second text", 80);

        assertThat(engineFactory.createCount).isEqualTo(1);
        assertThat(engineFactory.completeCount).isEqualTo(2);
    }

    @Test
    void isAvailable_returnsTrueWhenModelFileExists() throws IOException {
        Path modelPath = createFakeModel();
        EmbeddedLlamaClient client = new EmbeddedLlamaClient(
                resolverFor(modelPath),
                new RecordingEngineFactory("Generated title"),
                "test-model");

        assertThat(client.isAvailable()).isTrue();
        assertThat(client.getModelName()).isEqualTo("test-model");
    }

    private Path createFakeModel() throws IOException {
        Path modelPath = tempDir.resolve("test.gguf");
        Files.writeString(modelPath, "fake-model", StandardCharsets.UTF_8);
        return modelPath;
    }

    private LocalModelResolver resolverFor(Path modelPath) {
        return new LocalModelResolver(
                modelPath,
                LocalModelResolver.DEFAULT_MODEL_URI,
                tempDir.resolve("cache"),
                List.of(tempDir),
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build());
    }

    private static final class RecordingEngineFactory implements LocalCompletionEngine.Factory {
        private final String response;
        private int createCount;
        private int completeCount;
        private String lastPrompt;

        private RecordingEngineFactory(String response) {
            this.response = response;
        }

        @Override
        public LocalCompletionEngine create(Path modelPath) {
            createCount++;
            return new LocalCompletionEngine() {
                @Override
                public String complete(String prompt, int maxOutputTokens) {
                    completeCount++;
                    lastPrompt = prompt;
                    return response;
                }

                @Override
                public void close() {
                }
            };
        }
    }
}
