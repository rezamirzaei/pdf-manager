package com.rezami.pdfmanager.domain;

import java.nio.file.Path;

public record RenameOperation(Path source, Path target) {}

