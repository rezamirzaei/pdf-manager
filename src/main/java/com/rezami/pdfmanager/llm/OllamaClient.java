package com.rezami.pdfmanager.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * LLM client implementation that connects to a local Ollama server.
 * Uses the Ollama REST API for generating titles.
 *
 * Default model is llama3.2:1b for faster local inference.
 */
public final class OllamaClient implements LlmClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3.2:1b";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(45);
    private static final int MAX_PROMPT_CHARS = 180;
    private static final int MIN_OUTPUT_TOKENS = 8;
    private static final int MAX_OUTPUT_TOKENS = 24;
    private static final Duration TAGS_TIMEOUT = Duration.ofSeconds(6);

    private static final String TITLE_PROMPT_TEMPLATE = """
        Title only, max %d chars, plain words:
        %s
        """;

    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;
    private final Duration timeout;

    public OllamaClient() {
        this(DEFAULT_BASE_URL, DEFAULT_MODEL, DEFAULT_TIMEOUT);
    }

    public OllamaClient(String baseUrl, String model) {
        this(baseUrl, model, DEFAULT_TIMEOUT);
    }

    public OllamaClient(String baseUrl, String model, Duration timeout) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.model = Objects.requireNonNull(model, "model");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    // Constructor for testing with custom HttpClient
    OllamaClient(String baseUrl, String model, Duration timeout, HttpClient httpClient) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.model = Objects.requireNonNull(model, "model");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.gson = new Gson();
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

        String truncatedContent = textContent.length() > MAX_PROMPT_CHARS
                ? textContent.substring(0, MAX_PROMPT_CHARS) + "..."
                : textContent;

        String prompt = String.format(TITLE_PROMPT_TEMPLATE, maxTitleLength, truncatedContent);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("stream", false);
        requestBody.addProperty("keep_alive", "10m");

        JsonObject options = new JsonObject();
        options.addProperty("temperature", 0.0);
        options.addProperty("top_p", 0.9);
        options.addProperty("top_k", 30);
        options.addProperty("repeat_penalty", 1.1);
        options.addProperty("num_ctx", 256);
        options.addProperty("num_predict", outputTokenLimit(maxTitleLength));

        JsonArray stop = new JsonArray();
        stop.add("\n");
        stop.add("\r");
        options.add("stop", stop);
        requestBody.add("options", options);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Ollama API returned status " + response.statusCode() + ": " + response.body());
            }

            String title = parseResponseForTitle(response.body());
            return Optional.ofNullable(title)
                    .map(raw -> sanitizeTitle(raw, maxTitleLength));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(TAGS_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    public List<String> listModels() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(TAGS_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return List.of();
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json == null || !json.has("models") || !json.get("models").isJsonArray()) {
                return List.of();
            }

            List<String> modelNames = new ArrayList<>();
            JsonArray models = json.getAsJsonArray("models");
            models.forEach(modelEntry -> {
                if (modelEntry.isJsonObject()) {
                    JsonObject object = modelEntry.getAsJsonObject();
                    if (object.has("name")) {
                        String modelName = object.get("name").getAsString();
                        if (!modelName.isBlank()) {
                            modelNames.add(modelName.trim());
                        }
                    }
                }
            });
            return List.copyOf(modelNames);
        } catch (IOException | InterruptedException | JsonSyntaxException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    public boolean isModelAvailable(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return false;
        }
        String normalizedModel = modelName.trim();
        return listModels().stream().anyMatch(name -> name.equalsIgnoreCase(normalizedModel));
    }

    @Override
    public String getModelName() {
        return model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private String parseResponseForTitle(String responseBody) throws IOException {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json.has("error")) {
                throw new IOException("Ollama error: " + json.get("error").getAsString());
            }
            if (json.has("response")) {
                return json.get("response").getAsString();
            }
            throw new IOException("No 'response' field in Ollama response");
        } catch (JsonSyntaxException e) {
            throw new IOException("Failed to parse Ollama response: " + e.getMessage(), e);
        }
    }

    private static int outputTokenLimit(int maxTitleLength) {
        int estimatedTokens = Math.max(MIN_OUTPUT_TOKENS, maxTitleLength / 4);
        return Math.min(estimatedTokens, MAX_OUTPUT_TOKENS);
    }

    private static String sanitizeTitle(String title, int maxLength) {
        String firstLine = title.lines().findFirst().orElse(title);
        String cleaned = firstLine
                .replaceAll("^\\p{Punct}+|\\p{Punct}+$", "")
                .replaceAll("[\"'`]", "")
                .replaceAll("[:/\\\\*?<>|]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        int lastSpace = cleaned.lastIndexOf(' ', maxLength - 3);
        if (lastSpace > maxLength / 2) {
            return cleaned.substring(0, lastSpace).trim();
        }

        return cleaned.substring(0, maxLength - 3).trim();
    }
}
