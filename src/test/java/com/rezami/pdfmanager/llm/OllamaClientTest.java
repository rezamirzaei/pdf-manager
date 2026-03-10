package com.rezami.pdfmanager.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OllamaClientTest {

    private static final String BASE_URL = "http://localhost:11434";
    private static final String MODEL = "llama3.2:3b";

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private OllamaClient client;

    @BeforeEach
    void setUp() {
        client = new OllamaClient(BASE_URL, MODEL, Duration.ofSeconds(30), httpClient);
    }

    @Test
    void generateTitle_withValidContent_returnsTitle() throws Exception {
        String responseBody = """
            {"model":"llama3.2:3b","response":"Introduction to Machine Learning","done":true}
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(httpResponse);

        Optional<String> title = client.generateTitle("This paper discusses machine learning algorithms...", 100);

        assertThat(title).contains("Introduction to Machine Learning");
        verify(httpClient).send(any(HttpRequest.class), anyBodyHandler());
    }

    @Test
    void generateTitle_withBlankContent_returnsEmpty() throws Exception {
        Optional<String> title = client.generateTitle("   ", 100);

        assertThat(title).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    void generateTitle_withNullContent_throwsException() {
        assertThatThrownBy(() -> client.generateTitle(null, 100))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("textContent");
    }

    @Test
    void generateTitle_withInvalidMaxLength_throwsException() {
        assertThatThrownBy(() -> client.generateTitle("some text", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxTitleLength must be positive");

        assertThatThrownBy(() -> client.generateTitle("some text", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxTitleLength must be positive");
    }

    @Test
    void generateTitle_whenApiReturnsError_throwsIOException() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(httpResponse);

        assertThatThrownBy(() -> client.generateTitle("some text", 100))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Ollama API returned status 500");
    }

    @Test
    void generateTitle_whenResponseMissingField_throwsIOException() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{}");
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(httpResponse);

        assertThatThrownBy(() -> client.generateTitle("some text", 100))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No 'response' field");
    }

    @Test
    void generateTitle_truncatesLongText() throws Exception {
        String responseBody = """
            {"model":"llama3.2:3b","response":"Short Title","done":true}
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(httpResponse);

        String longText = "A".repeat(5000);
        Optional<String> title = client.generateTitle(longText, 100);

        assertThat(title).isPresent();
        verify(httpClient).send(any(HttpRequest.class), anyBodyHandler());
    }

    @Test
    void generateTitle_truncatesLongTitle() throws Exception {
        String responseBody = """
            {"model":"llama3.2:3b","response":"This is a very long title that exceeds the maximum allowed length","done":true}
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(httpResponse);

        Optional<String> title = client.generateTitle("some text", 30);

        assertThat(title).isPresent();
        assertThat(title.get().length()).isLessThanOrEqualTo(30);
    }

    @Test
    void generateTitle_removesProblematicCharacters() throws Exception {
        String responseBody = """
                {"model":"llama3.2:3b","response":"\\"Title: With / invalid\\\\chars\\"\\nextra text","done":true}
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(httpResponse);

        Optional<String> title = client.generateTitle("some text", 100);

        assertThat(title).isPresent();
        assertThat(title.get()).isEqualTo("Title With invalid chars");
    }

    @Test
    void listModels_withValidResponse_returnsNames() throws Exception {
        String responseBody = """
                {"models":[{"name":"llama3.2:3b"},{"name":"phi3:mini"}]}
                """;
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(httpResponse);

        List<String> models = client.listModels();

        assertThat(models).containsExactly("llama3.2:3b", "phi3:mini");
    }

    @Test
    void isModelAvailable_withExistingModel_returnsTrue() throws Exception {
        String responseBody = """
                {"models":[{"name":"llama3.2:3b"},{"name":"phi3:mini"}]}
                """;
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        assertThat(client.isModelAvailable("LLAMA3.2:3B")).isTrue();
        assertThat(client.isModelAvailable("missing:model")).isFalse();
    }

    @Test
    void isAvailable_whenServerResponds_returnsTrue() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(httpResponse);

        assertThat(client.isAvailable()).isTrue();
    }

    @Test
    void isAvailable_whenServerError_returnsFalse() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(httpResponse);

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_whenConnectionFails_returnsFalse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenThrow(new IOException("Connection refused"));

        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    void getModelName_returnsConfiguredModel() {
        assertThat(client.getModelName()).isEqualTo(MODEL);
    }

    @Test
    void getBaseUrl_returnsConfiguredUrl() {
        assertThat(client.getBaseUrl()).isEqualTo(BASE_URL);
    }

    @Test
    void constructor_withNullBaseUrl_throwsException() {
        assertThatThrownBy(() -> new OllamaClient(null, MODEL))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("baseUrl");
    }

    @Test
    void constructor_withNullModel_throwsException() {
        assertThatThrownBy(() -> new OllamaClient(BASE_URL, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("model");
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse.BodyHandler<String> anyBodyHandler() {
        return ArgumentMatchers.any();
    }
}
