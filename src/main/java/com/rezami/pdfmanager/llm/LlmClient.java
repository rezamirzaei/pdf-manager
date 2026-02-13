package com.rezami.pdfmanager.llm;

import java.io.IOException;
import java.util.Optional;

/**
 * Strategy interface for Large Language Model interactions.
 * Implementations can connect to different LLM providers (Ollama, OpenAI, etc.)
 */
public interface LlmClient {

    /**
     * Generates a concise title for the given text content.
     *
     * @param textContent the text content to analyze (typically first page of a PDF)
     * @param maxTitleLength maximum length of the generated title
     * @return generated title, or empty if generation fails
     * @throws IOException if communication with the LLM fails
     */
    Optional<String> generateTitle(String textContent, int maxTitleLength) throws IOException;

    /**
     * Checks if the LLM service is available and responsive.
     *
     * @return true if the service is available
     */
    boolean isAvailable();

    /**
     * Returns the name of the model being used.
     *
     * @return model name
     */
    String getModelName();
}

