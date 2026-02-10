package com.rezami.pdfmanager.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public final class PdfFileScanner {
    public List<Path> scan(Path directory, boolean recursive) throws IOException {
        Objects.requireNonNull(directory, "directory");

        if (!Files.isDirectory(directory)) {
            throw new IOException("Not a directory: " + directory);
        }

        try (Stream<Path> stream = recursive ? Files.walk(directory) : Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(PdfFileScanner::isPdf)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static boolean isPdf(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".pdf");
    }
}

