package com.rezami.pdfmanager.service;

import java.io.IOException;
import java.nio.file.Path;

import com.rezami.pdfmanager.domain.RenamePlan;
import com.rezami.pdfmanager.util.ProgressListener;

public interface RenameService {
    RenamePlan plan(Path directory, boolean recursive) throws IOException;

    RenamePlan plan(Path directory, boolean recursive, ProgressListener progressListener) throws IOException;

    void execute(RenamePlan plan) throws IOException;
}

