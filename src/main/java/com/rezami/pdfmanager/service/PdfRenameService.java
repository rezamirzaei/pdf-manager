package com.rezami.pdfmanager.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.rezami.pdfmanager.domain.RenamePlan;

public final class PdfRenameService implements RenameService {
    private final RenamePlanner planner;
    private final PdfRenamer renamer;

    public PdfRenameService(RenamePlanner planner, PdfRenamer renamer) {
        this.planner = Objects.requireNonNull(planner, "planner");
        this.renamer = Objects.requireNonNull(renamer, "renamer");
    }

    @Override
    public RenamePlan plan(Path directory, boolean recursive) throws IOException {
        return planner.plan(directory, recursive);
    }

    @Override
    public void execute(RenamePlan plan) throws IOException {
        renamer.execute(plan);
    }
}
