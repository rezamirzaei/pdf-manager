# PDF Manager (Swing + MVC)

Desktop app that renames PDF files to match their embedded document titles or AI-generated titles.

## What it does

- You choose a folder.
- Click **Scan** to preview file name changes.
- Click **Rename** to rename all PDFs in that folder to their titles.

## Title source

The app supports multiple title reading strategies:

### Metadata Mode (`--metadata`)
Reads titles from embedded PDF metadata in this order:
1. PDF **Document Information** `Title`
2. XMP metadata (Dublin Core) `dc:title`

If no title is found, the PDF is skipped.

### LLM Mode (`--llm`)
Uses PDF text extraction and a local LLM (Ollama) to generate titles:
1. Extracts text from the first page using PDFBox
2. Sends text to Ollama to generate a concise, descriptive title

PDFBox can extract text from:
- Native text-based PDFs
- PDFs with embedded fonts
- Scanned PDFs that have been OCR'd (with text layers)

Requires [Ollama](https://ollama.ai/) running locally with a Llama model.
Default preferred model is `llama3.2:1b` for speed, with automatic fallback to other local `llama*` models.

### Composite Mode (`--composite`)
Tries LLM first, falls back to metadata if LLM cannot produce a title.

## Rename rules

- Output file names are sanitized for cross-platform compatibility.
- If multiple PDFs resolve to the same title, the app creates unique names like `Title.pdf`, `Title (2).pdf`, …
- Renaming is executed using a two‑phase move to support safe swaps (e.g. `a.pdf` ↔ `b.pdf`) and to avoid partially renamed states.

## Run

Requires **Java 21+**.

- Run tests: `./mvnw test`
- Build runnable jar: `./mvnw -q package`
- Build + run (recommended): `bash run.sh`
- Build fast + run (skips tests): `bash run.sh --fast`

### Running modes:

```bash
# Default: auto mode (LLM if Ollama is available, otherwise metadata)
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar

# Force metadata mode
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --metadata

# LLM mode: use Ollama for AI-generated titles  
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --llm

# Composite mode: LLM first, metadata fallback
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --composite
```

### Setting up Ollama (for LLM mode)

1. Install Ollama: https://ollama.ai/
2. Start Ollama: `ollama serve`
3. Pull a model (fast default): `ollama pull llama3.2:1b`
4. Run the app with `--llm` flag

### Optional environment overrides

- `PDF_MANAGER_OLLAMA_URL` to use a non-default Ollama endpoint
- `PDF_MANAGER_OLLAMA_MODEL` to force a specific model tag

## Development

- Main entry point: `src/main/java/com/rezami/pdfmanager/app/PdfManagerMain.java`
- Swing MVC wiring: controller in `src/main/java/com/rezami/pdfmanager/controller`, view in `src/main/java/com/rezami/pdfmanager/ui/swing`
- Core rename logic: `src/main/java/com/rezami/pdfmanager/service`
- LLM integration: `src/main/java/com/rezami/pdfmanager/llm`
- OCR/Text extraction: `src/main/java/com/rezami/pdfmanager/ocr`
