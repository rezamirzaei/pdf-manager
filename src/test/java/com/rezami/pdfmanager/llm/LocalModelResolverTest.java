package com.rezami.pdfmanager.llm;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalModelResolverTest {
    @TempDir Path tempDir;

    @Test
    void ensureModelAvailable_prefersBundledModel() throws IOException {
        Path bundledModel = tempDir.resolve("models").resolve(LocalModelResolver.DEFAULT_MODEL_FILE_NAME);
        Files.createDirectories(bundledModel.getParent());
        Files.writeString(bundledModel, "bundled-model", StandardCharsets.UTF_8);

        LocalModelResolver resolver = new LocalModelResolver(
                null,
                LocalModelResolver.DEFAULT_MODEL_URI,
                tempDir.resolve("cache"),
                List.of(tempDir),
                defaultHttpClient());

        assertThat(resolver.ensureModelAvailable()).isEqualTo(bundledModel.toAbsolutePath().normalize());
    }

    @Test
    void ensureModelAvailable_downloadsModelWhenMissing() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        try {
            server.createContext("/model.gguf", exchange -> {
                byte[] body = "downloaded-model".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();

            URI downloadUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/model.gguf");
            Path cacheDirectory = tempDir.resolve("cache");
            LocalModelResolver resolver = new LocalModelResolver(
                    null,
                    downloadUri,
                    cacheDirectory,
                    List.of(tempDir.resolve("missing-root")),
                    defaultHttpClient());

            Path resolvedModel = resolver.ensureModelAvailable();

            assertThat(resolvedModel).isEqualTo(cacheDirectory.resolve(LocalModelResolver.DEFAULT_MODEL_FILE_NAME));
            assertThat(Files.readString(resolvedModel, StandardCharsets.UTF_8)).isEqualTo("downloaded-model");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void findModelPath_acceptsAnyBundledGgufFile() throws IOException {
        Path bundledModel = tempDir.resolve("models").resolve("custom-model.gguf");
        Files.createDirectories(bundledModel.getParent());
        Files.writeString(bundledModel, "custom", StandardCharsets.UTF_8);

        LocalModelResolver resolver = new LocalModelResolver(
                null,
                LocalModelResolver.DEFAULT_MODEL_URI,
                tempDir.resolve("cache"),
                List.of(tempDir),
                defaultHttpClient());

        assertThat(resolver.findModelPath()).contains(bundledModel.toAbsolutePath().normalize());
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }
}
