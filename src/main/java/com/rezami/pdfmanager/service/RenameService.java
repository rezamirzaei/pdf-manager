package com.rezami.pdfmanager.service;

import java.io.IOException;
import java.nio.file.Path;

import com.rezami.pdfmanager.domain.RenamePlan;

public interface RenameService {
    RenamePlan plan(Path directory, boolean recursive) throws IOException;

    void execute(RenamePlan plan) throws IOException;
}

