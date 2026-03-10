# Architecture

## Goals

- Keep PDF parsing and file operations out of Swing code.
- Make the core behavior unit-testable (planning, sanitizing, rename execution).
- Use clear responsibilities (MVC) so the project stays maintainable.
- Support multiple title reading strategies (metadata, LLM, composite).

## Package overview

- `com.rezami.pdfmanager.app`
  - App entry point / wiring (`PdfManagerMain`)
  - `TitleReaderFactory`: factory for creating different title reader strategies
- `com.rezami.pdfmanager.ui` + `com.rezami.pdfmanager.ui.swing`
  - `RenameView` interface + Swing implementation
  - Table model for preview rendering
- `com.rezami.pdfmanager.controller`
  - `RenameController` orchestrates user actions (scan/rename) and updates the view
- `com.rezami.pdfmanager.service`
  - `PdfTitleReader`: strategy interface for title extraction
  - `PdfBoxTitleReader`: reads titles from PDF metadata
  - `LlmTitleReader`: generates titles using LLM and OCR/text extraction
  - `CompositeTitleReader`: chains multiple readers with fallback
  - `PdfFileScanner`: finds PDFs in a folder
  - `RenamePlanner`: builds a deterministic rename plan (including collision handling)
  - `PdfRenamer`: executes the plan using a two‑phase rename with rollback
- `com.rezami.pdfmanager.llm`
  - `LlmClient`: strategy interface for LLM interactions
  - `OllamaClient`: connects to local Ollama server for title generation
- `com.rezami.pdfmanager.ocr`
  - `PdfTextExtractor`: strategy interface for text extraction
  - `PdfBoxTextExtractor`: extracts text using PDFBox (handles native PDFs and OCR'd PDFs with text layers)
- `com.rezami.pdfmanager.domain`
  - Immutable data structures (`RenamePlan`, `RenamePlanEntry`, …)
- `com.rezami.pdfmanager.util`
  - Small utilities (`FileNameSanitizer`, async `TaskRunner`)

## Patterns used

- **MVC / Passive View**: the controller contains the behavior; the view only displays and forwards user events.
- **Strategy**: the `PdfTitleReader`, `LlmClient`, and `PdfTextExtractor` abstractions make implementations replaceable/testable.
- **Composite**: `CompositeTitleReader` chains multiple strategies with fallback behavior.
- **Factory**: `TitleReaderFactory` encapsulates creation of complex object graphs.
- **Two-phase rename**: avoids conflicts and supports swaps by moving sources to unique temp names before final targets.

## LLM Integration

The LLM-based title generation flow:

1. `PdfBoxTextExtractor` extracts text from PDF first page(s)
   - Uses PDFBox's PDFTextStripper for text extraction
   - Handles native text PDFs and OCR'd PDFs with text layers
   - Falls back to extracting from multiple pages if first page has little content
2. `OllamaClient` sends extracted text to local Ollama server
   - Uses a prompt template to generate concise, filename-safe titles
   - Supports configurable model (default: llama3.2:1b)
3. `LlmTitleReader` orchestrates the flow and implements `PdfTitleReader`

This allows seamless switching between metadata-based and AI-based title generation.
