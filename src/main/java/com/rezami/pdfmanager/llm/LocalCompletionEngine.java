package com.rezami.pdfmanager.llm;

import java.io.IOException;
import java.nio.file.Path;

interface LocalCompletionEngine extends AutoCloseable {
    String complete(String prompt, int maxOutputTokens) throws IOException;

    @Override
    void close();

    interface Factory {
        LocalCompletionEngine create(Path modelPath) throws IOException;
    }
}
