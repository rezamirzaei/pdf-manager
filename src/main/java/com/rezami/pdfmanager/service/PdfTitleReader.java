package com.rezami.pdfmanager.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface PdfTitleReader {
    Optional<String> readTitle(Path pdfPath) throws IOException;
}

