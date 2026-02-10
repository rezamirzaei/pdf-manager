package com.rezami.pdfmanager.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfBoxTitleReaderTest {
    private final PdfBoxTitleReader reader = new PdfBoxTitleReader();

    @TempDir Path tempDir;

    @Test
    void readTitle_readsDocumentInformationTitle() throws IOException {
        Path pdf = tempDir.resolve("info-title.pdf");
        savePdfWithInfoTitle(pdf, "My Article Title");

        assertThat(reader.readTitle(pdf)).contains("My Article Title");
    }

    @Test
    void readTitle_whenNoTitle_returnsEmpty() throws IOException {
        Path pdf = tempDir.resolve("no-title.pdf");
        savePdf(pdf);

        assertThat(reader.readTitle(pdf)).isEmpty();
    }

    @Test
    void readTitle_whenInfoTitleMissing_readsXmpTitle() throws Exception {
        Path pdf = tempDir.resolve("xmp-title.pdf");
        savePdfWithXmpTitle(pdf, "XMP Title");

        assertThat(reader.readTitle(pdf)).contains("XMP Title");
    }

    private static void savePdf(Path path) throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(path.toFile());
        }
    }

    private static void savePdfWithInfoTitle(Path path, String title) throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(title);
            document.setDocumentInformation(info);
            document.save(path.toFile());
        }
    }

    private static void savePdfWithXmpTitle(Path path, String title) throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());

            XMPMetadata xmp = XMPMetadata.createXMPMetadata();
            DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            dc.setTitle(title);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new XmpSerializer().serialize(xmp, out, true);

            PDMetadata metadata = new PDMetadata(document);
            metadata.importXMPMetadata(out.toByteArray());
            document.getDocumentCatalog().setMetadata(metadata);

            document.save(path.toFile());
        }
    }
}

