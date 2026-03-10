# PDF Manager (Swing + MVC)

Desktop app that renames PDF files to match their embedded document titles, a bundled local llama.cpp model, or optional Ollama titles.

## What it does

- You choose a folder.
- Click **Scan** to preview file name changes.
- Review the preview checkboxes to opt out any files you want to keep unchanged.
- Click **Rename** to rename the selected PDFs to their titles.
- The app remembers your last folder and subfolder setting between launches.

## Title source

The app supports multiple title reading strategies:

### Embedded Local LLM (`--local-llm`, alias `--smart`)
Uses a bundled local llama.cpp runtime with a GGUF model:
1. Extracts text from the first page using PDFBox
2. Prompts a local `Qwen2.5-0.5B-Instruct` GGUF model to return a concise title

This mode requires no external AI process or daemon. `bash prepare-local-model.sh` downloads the default GGUF to `~/.pdf-manager/models`, and `bash package-native.sh` bundles that model into the installer by default.
On Intel macOS, treat the embedded runtime as experimental until you validate title quality on your own documents.

### Metadata Mode (`--metadata`)
Reads titles from embedded PDF metadata in this order:
1. PDF **Document Information** `Title`
2. XMP metadata (Dublin Core) `dc:title`

If no title is found, the PDF is skipped.

### Ollama Mode (`--llm`)
Uses PDF text extraction and an external local Ollama server to generate titles:
1. Extracts text from the first page using PDFBox
2. Sends text to Ollama to generate a concise, descriptive title

PDFBox can extract text from:
- Native text-based PDFs
- PDFs with embedded fonts
- Scanned PDFs that have been OCR'd (with text layers)

Requires [Ollama](https://ollama.ai/) running locally with a Llama model.
Default preferred model is `llama3.2:1b` for speed, with automatic fallback to other local `llama*` models.
If Ollama is unavailable when the app starts, LLM and composite modes degrade cleanly to metadata mode instead of stalling scans file-by-file.

### Auto Mode (default)
Default startup uses metadata first and falls back to the embedded local LLM when metadata is missing. This keeps scans fast for PDFs that already have a title while staying fully standalone for the rest.

### Composite Mode (`--composite`)
Tries Ollama first, falls back to metadata if Ollama cannot produce a title.

## Rename rules

- Output file names are sanitized for cross-platform compatibility.
- If multiple PDFs resolve to the same title, the app creates unique names like `Title.pdf`, `Title (2).pdf`, …
- Renaming is executed using a two‑phase move to support safe swaps (e.g. `a.pdf` ↔ `b.pdf`) and to avoid partially renamed states.

## Run

Requires **Java 21+**.

- Run tests: `./mvnw test`
- Build runnable jar: `./mvnw -q package`
- Download the embedded model: `bash prepare-local-model.sh`
- Build + run (recommended): `bash run.sh`
- Build fast + run (skips tests): `bash run.sh --fast`
- Build native standalone installer/package: `bash package-native.sh`

### Running modes:

```bash
# Default: auto mode (metadata first, embedded local LLM fallback)
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar

# Force embedded local llama.cpp model
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --local-llm

# Backward-compatible alias
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --smart

# Force metadata mode
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --metadata

# Ollama mode: use external local LLM
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --llm

# Composite mode: Ollama first, metadata fallback
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --composite
```

### Native package

`bash package-native.sh` builds the fat jar, stages the GGUF model next to it, and then uses `jpackage` to create a native package.
On macOS this produces a `.dmg`; on Linux it falls back to `app-image` unless you override `PACKAGE_TYPE`.
Set `PACKAGE_BUNDLE_LOCAL_MODEL=0` if you explicitly want a smaller package without the bundled GGUF.

### Setting up Ollama (for optional Ollama mode)

1. Install Ollama: https://ollama.ai/
2. Start Ollama: `ollama serve`
3. Pull a model (fast default): `ollama pull llama3.2:1b`
4. Run the app with `--llm` flag

### Optional environment overrides

- `PDF_MANAGER_LOCAL_MODEL_PATH` to use a specific local GGUF file
- `PDF_MANAGER_LOCAL_MODEL_DIR` to change where `prepare-local-model.sh` and the app cache the default model
- `PDF_MANAGER_LOCAL_MODEL_URL` to download a different GGUF
- `PDF_MANAGER_OLLAMA_URL` to use a non-default Ollama endpoint
- `PDF_MANAGER_OLLAMA_MODEL` to force a specific model tag

## Development

- Main entry point: `src/main/java/com/rezami/pdfmanager/app/PdfManagerMain.java`
- Swing MVC wiring: controller in `src/main/java/com/rezami/pdfmanager/controller`, view in `src/main/java/com/rezami/pdfmanager/ui/swing`
- Core rename logic: `src/main/java/com/rezami/pdfmanager/service`
- LLM integration: `src/main/java/com/rezami/pdfmanager/llm`
- OCR/Text extraction: `src/main/java/com/rezami/pdfmanager/ocr`
