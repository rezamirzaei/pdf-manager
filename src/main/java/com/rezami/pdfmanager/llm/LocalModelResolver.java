package com.rezami.pdfmanager.llm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public final class LocalModelResolver {
    static final String DEFAULT_MODEL_NAME = "Qwen2.5-0.5B-Instruct GGUF (q4_0)";
    static final String DEFAULT_MODEL_FILE_NAME = "qwen2.5-0.5b-instruct-q4_0.gguf";
    static final URI DEFAULT_MODEL_URI = URI.create(
            "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/"
                    + DEFAULT_MODEL_FILE_NAME + "?download=1");

    private static final Logger LOGGER = Logger.getLogger(LocalModelResolver.class.getName());
    private static final String ENV_MODEL_PATH = "PDF_MANAGER_LOCAL_MODEL_PATH";
    private static final String ENV_MODEL_URL = "PDF_MANAGER_LOCAL_MODEL_URL";
    private static final String ENV_MODEL_DIR = "PDF_MANAGER_LOCAL_MODEL_DIR";
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(30);

    private final Path configuredModelPath;
    private final URI modelDownloadUri;
    private final Path cacheDirectory;
    private final List<Path> searchRoots;
    private final HttpClient httpClient;

    public LocalModelResolver() {
        this(
                configuredModelPathFromEnv(),
                modelUriFromEnv(),
                cacheDirectoryFromEnv(),
                discoverSearchRoots(),
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(Duration.ofSeconds(20))
                        .build());
    }

    LocalModelResolver(Path configuredModelPath,
                       URI modelDownloadUri,
                       Path cacheDirectory,
                       List<Path> searchRoots,
                       HttpClient httpClient) {
        this.configuredModelPath = configuredModelPath == null ? null : configuredModelPath.toAbsolutePath().normalize();
        this.modelDownloadUri = Objects.requireNonNull(modelDownloadUri, "modelDownloadUri");
        this.cacheDirectory = Objects.requireNonNull(cacheDirectory, "cacheDirectory").toAbsolutePath().normalize();
        this.searchRoots = List.copyOf(Objects.requireNonNull(searchRoots, "searchRoots"));
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public Optional<Path> findModelPath() {
        if (configuredModelPath != null) {
            return regularFile(configuredModelPath);
        }

        Optional<Path> bundledModel = findBundledModel();
        if (bundledModel.isPresent()) {
            return bundledModel;
        }

        return regularFile(cachePath());
    }

    public boolean hasModelReady() {
        return findModelPath().isPresent();
    }

    public Path ensureModelAvailable() throws IOException {
        if (configuredModelPath != null) {
            Optional<Path> existing = regularFile(configuredModelPath);
            if (existing.isPresent()) {
                return existing.orElseThrow();
            }
            return downloadModel(configuredModelPath);
        }

        Optional<Path> bundledModel = findBundledModel();
        if (bundledModel.isPresent()) {
            return bundledModel.orElseThrow();
        }

        Path cachedModel = cachePath();
        Optional<Path> existing = regularFile(cachedModel);
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }

        return downloadModel(cachedModel);
    }

    Path cachePath() {
        return cacheDirectory.resolve(DEFAULT_MODEL_FILE_NAME);
    }

    private Optional<Path> findBundledModel() {
        for (Path root : searchRoots) {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Optional<Path> direct = regularFile(normalizedRoot.resolve(DEFAULT_MODEL_FILE_NAME));
            if (direct.isPresent()) {
                return direct;
            }

            Optional<Path> nested = regularFile(normalizedRoot.resolve("models").resolve(DEFAULT_MODEL_FILE_NAME));
            if (nested.isPresent()) {
                return nested;
            }

            Optional<Path> firstGguf = firstGgufFile(normalizedRoot.resolve("models"));
            if (firstGguf.isPresent()) {
                return firstGguf;
            }
        }
        return Optional.empty();
    }

    private Path downloadModel(Path targetPath) throws IOException {
        Path normalizedTarget = targetPath.toAbsolutePath().normalize();
        Path parent = normalizedTarget.getParent();
        if (parent == null) {
            throw new IOException("Model path has no parent directory: " + normalizedTarget);
        }

        Files.createDirectories(parent);

        Path tempFile = normalizedTarget.resolveSibling(normalizedTarget.getFileName() + ".part");
        LOGGER.info("Downloading embedded local model from " + modelDownloadUri + " to " + normalizedTarget);

        HttpRequest request = HttpRequest.newBuilder(modelDownloadUri)
                .timeout(DOWNLOAD_TIMEOUT)
                .header("User-Agent", "pdf-manager")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("Model download returned status " + response.statusCode());
            }

            try (InputStream body = response.body()) {
                Files.copy(body, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!Files.isRegularFile(tempFile) || Files.size(tempFile) == 0) {
                throw new IOException("Downloaded model file is empty: " + tempFile);
            }

            moveIntoPlace(tempFile, normalizedTarget);
            LOGGER.info("Embedded local model ready at " + normalizedTarget);
            return normalizedTarget;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Model download interrupted", e);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Optional<Path> regularFile(Path candidate) {
        return Files.isRegularFile(candidate) ? Optional.of(candidate.toAbsolutePath().normalize()) : Optional.empty();
    }

    private static Optional<Path> firstGgufFile(Path directory) {
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".gguf"))
                    .sorted()
                    .map(path -> path.toAbsolutePath().normalize())
                    .findFirst();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private static Path configuredModelPathFromEnv() {
        String value = System.getenv(ENV_MODEL_PATH);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Path.of(value.trim());
    }

    private static URI modelUriFromEnv() {
        String value = System.getenv(ENV_MODEL_URL);
        if (value == null || value.isBlank()) {
            return DEFAULT_MODEL_URI;
        }
        return URI.create(value.trim());
    }

    private static Path cacheDirectoryFromEnv() {
        String value = System.getenv(ENV_MODEL_DIR);
        if (value != null && !value.isBlank()) {
            return Path.of(value.trim());
        }
        return Path.of(System.getProperty("user.home"), ".pdf-manager", "models");
    }

    private static List<Path> discoverSearchRoots() {
        Set<Path> roots = new LinkedHashSet<>();
        roots.add(Path.of("").toAbsolutePath().normalize());

        resolveCodeSourceDirectory().ifPresent(codeSource -> {
            roots.add(codeSource);
            if (codeSource.getParent() != null) {
                roots.add(codeSource.getParent());
            }
            if (codeSource.getParent() != null && codeSource.getParent().getParent() != null) {
                roots.add(codeSource.getParent().getParent());
            }
        });

        return new ArrayList<>(roots);
    }

    private static Optional<Path> resolveCodeSourceDirectory() {
        try {
            URI location = LocalModelResolver.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            Path codeSource = Path.of(location).toAbsolutePath().normalize();
            if (Files.isDirectory(codeSource)) {
                return Optional.of(codeSource);
            }
            Path parent = codeSource.getParent();
            return parent == null ? Optional.empty() : Optional.of(parent);
        } catch (URISyntaxException | RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
