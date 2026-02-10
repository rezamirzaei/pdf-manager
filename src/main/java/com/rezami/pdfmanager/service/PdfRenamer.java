package com.rezami.pdfmanager.service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.rezami.pdfmanager.domain.RenameOperation;
import com.rezami.pdfmanager.domain.RenamePlan;

public final class PdfRenamer {
    public void execute(RenamePlan plan) throws IOException {
        Objects.requireNonNull(plan, "plan");

        List<RenameOperation> operations = plan.readyOperations();
        if (operations.isEmpty()) {
            return;
        }

        Map<RenameOperation, Path> tempMoves = new LinkedHashMap<>();
        List<RenameOperation> completed = new LinkedList<>();
        try {
            for (RenameOperation operation : operations) {
                Path tempTarget = createTempSibling(operation.source());
                Files.move(operation.source(), tempTarget);
                tempMoves.put(operation, tempTarget);
            }

            for (Map.Entry<RenameOperation, Path> entry : tempMoves.entrySet()) {
                RenameOperation operation = entry.getKey();
                Path tempSource = entry.getValue();
                Files.move(tempSource, operation.target());
                completed.add(operation);
            }
        } catch (IOException failure) {
            IOException rollbackFailure = rollback(tempMoves, completed, failure);
            throw rollbackFailure;
        }
    }

    private static IOException rollback(
            Map<RenameOperation, Path> tempMoves, List<RenameOperation> completed, IOException originalFailure) {
        IOException rollbackFailure = new IOException("Rename failed; attempted rollback", originalFailure);

        for (int i = completed.size() - 1; i >= 0; i--) {
            RenameOperation operation = completed.get(i);
            if (!Files.exists(operation.target())) {
                continue;
            }
            try {
                Files.move(operation.target(), operation.source());
            } catch (IOException rollbackError) {
                rollbackFailure.addSuppressed(rollbackError);
            }
        }

        for (Map.Entry<RenameOperation, Path> entry : tempMoves.entrySet()) {
            RenameOperation operation = entry.getKey();
            if (completed.contains(operation)) {
                continue;
            }

            Path tempSource = entry.getValue();
            if (!Files.exists(tempSource)) {
                continue;
            }
            try {
                Files.move(tempSource, operation.source());
            } catch (IOException rollbackError) {
                rollbackFailure.addSuppressed(rollbackError);
            }
        }
        return rollbackFailure;
    }

    private static Path createTempSibling(Path source) throws FileAlreadyExistsException {
        Path parent = source.getParent();
        String fileName = source.getFileName().toString();

        for (int attempt = 0; attempt < 20; attempt++) {
            String suffix = ".pdf-manager-tmp-" + UUID.randomUUID();
            Path candidate = parent.resolve(fileName + suffix);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new FileAlreadyExistsException("Could not create a unique temp name for: " + source);
    }
}
