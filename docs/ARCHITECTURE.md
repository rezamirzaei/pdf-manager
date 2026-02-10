# Architecture

## Goals

- Keep PDF parsing and file operations out of Swing code.
- Make the core behavior unit-testable (planning, sanitizing, rename execution).
- Use clear responsibilities (MVC) so the project stays maintainable.

## Package overview

- `com.rezami.pdfmanager.app`
  - App entry point / wiring (`PdfManagerMain`)
- `com.rezami.pdfmanager.ui` + `com.rezami.pdfmanager.ui.swing`
  - `RenameView` interface + Swing implementation
  - Table model for preview rendering
- `com.rezami.pdfmanager.controller`
  - `RenameController` orchestrates user actions (scan/rename) and updates the view
- `com.rezami.pdfmanager.service`
  - `PdfBoxTitleReader`: reads titles via PDFBox
  - `PdfFileScanner`: finds PDFs in a folder
  - `RenamePlanner`: builds a deterministic rename plan (including collision handling)
  - `PdfRenamer`: executes the plan using a two‑phase rename with rollback
- `com.rezami.pdfmanager.domain`
  - Immutable data structures (`RenamePlan`, `RenamePlanEntry`, …)
- `com.rezami.pdfmanager.util`
  - Small utilities (`FileNameSanitizer`, async `TaskRunner`)

## Patterns used

- **MVC / Passive View**: the controller contains the behavior; the view only displays and forwards user events.
- **Strategy**: the `PdfTitleReader` abstraction makes title extraction replaceable/testable.
- **Two-phase rename**: avoids conflicts and supports swaps by moving sources to unique temp names before final targets.

