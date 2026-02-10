# PDF Manager (Swing + MVC)

Desktop app that renames PDF files to match their embedded document titles.

## What it does

- You choose a folder.
- Click **Scan** to preview file name changes.
- Click **Rename** to rename all PDFs in that folder to their titles.

## Title source

The app reads titles in this order:

1. PDF **Document Information** `Title`
2. XMP metadata (Dublin Core) `dc:title`

If no title is found, the PDF is skipped.

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
- Start app: `java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar`

## Development

- Main entry point: `src/main/java/com/rezami/pdfmanager/app/PdfManagerMain.java`
- Swing MVC wiring: controller in `src/main/java/com/rezami/pdfmanager/controller`, view in `src/main/java/com/rezami/pdfmanager/ui/swing`
- Core rename logic: `src/main/java/com/rezami/pdfmanager/service`
