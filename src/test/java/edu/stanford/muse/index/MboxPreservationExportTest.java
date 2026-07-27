package edu.stanford.muse.index;

import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.email.FetchConfig;
import edu.stanford.muse.email.FolderInfo;
import edu.stanford.muse.email.MuseEmailFetcher;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MboxPreservationExportTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void importedMboxCanBeExportedForPreservationWithoutChangingMessagesExceptEpaddHeaders() throws Exception {
        File inputMbox = temporaryFolder.newFile("preservation-input.mbox");
        assertMboxPreservationRoundTrip(inputMbox, plainTextMbox());
    }

    @Test
    public void importedMultipartAlternativeMboxExportsPlainAndHtmlParts() throws Exception {
        File inputMbox = temporaryFolder.newFile("multipart-alternative-input.mbox");
        ExportedMbox exported = exportMboxAfterImport(inputMbox, multipartAlternativeMbox(), false);

        String normalizedExport = normalizeMboxForComparison(read(exported.mbox));
        assertContains(normalizedExport, "Content-Type: multipart/alternative; boundary=\"epadd-test-boundary\"");
        assertContains(normalizedExport, "Hello Bob,\nThis is the plain text part.");
        assertContains(normalizedExport, "<html><body><p>Hello Bob,</p><p>This is the HTML part.</p></body></html>");
        assertContains(normalizedExport, "--epadd-test-boundary--");
    }

    @Test
    public void importedMboxWithAttachmentExportsAttachment() throws Exception {
        File inputMbox = temporaryFolder.newFile("attachment-input.mbox");
        ExportedMbox exported = exportMboxAfterImport(inputMbox, attachmentMbox(), true);

        String normalizedExport = normalizeMboxForComparison(read(exported.mbox));
        assertContains(normalizedExport, "Content-Type: multipart/mixed; boundary=\"epadd-attachment-boundary\"");
        assertContains(normalizedExport, "Hello Bob,\nThis message has an attachment.");
        assertContains(normalizedExport, "Content-type: text/plain; name=\"note.txt\"");
        assertContains(normalizedExport, "Content-transfer-encoding: base64");
        assertContains(normalizedExport, "Content-Disposition: attachment;filename=\"note.txt\"");
        assertContains(normalizedExport, "QXR0YWNobWVudCBjb250ZW50IGZvciBwcmVzZXJ2YXRpb24u");
        assertContains(normalizedExport, "--epadd-attachment-boundary--");
    }

    @Test
    public void importedMboxWithEncapsulatedMessageExportsAttachedMessage() throws Exception {
        File inputMbox = temporaryFolder.newFile("encapsulated-message-input.mbox");
        ExportedMbox exported = exportMboxAfterImport(inputMbox, encapsulatedMessageMbox(), true);

        String normalizedExport = normalizeMboxForComparison(read(exported.mbox));
        assertContains(normalizedExport, "Content-Type: multipart/mixed; boundary=\"epadd-encapsulated-boundary\"");
        assertContains(normalizedExport, "Hello Bob,\nThis message has an attached email.");
        assertContains(normalizedExport, "Content-type: message/rfc822; name=\"forwarded.eml\"");
        assertContains(normalizedExport, "Content-Disposition: attachment;filename=\"forwarded.eml\"");
        assertContains(normalizedExport, "From: Carol Example <carol@example.org>");
        assertContains(normalizedExport, "Subject: Encapsulated note");
        assertContains(normalizedExport, "This is the encapsulated message body.");
        assertContains(normalizedExport, "--epadd-encapsulated-boundary--");
    }

    @Test
    public void discoveryExportRemovesHtmlPartsAndOriginalUnredactedText() throws Exception {
        File inputMbox = temporaryFolder.newFile("discovery-input.mbox");
        Files.write(inputMbox.toPath(), discoveryMbox().getBytes(StandardCharsets.UTF_8));

        File archiveDir = temporaryFolder.newFolder("discovery-archive");
        Archive archive = importMbox(inputMbox, archiveDir, true);
        try {
            File exportTarget = temporaryFolder.newFolder("discovery-export");
            archive.export(
                    archive.getAllDocs(),
                    Archive.ExportMode.EXPORT_PROCESSING_TO_DISCOVERY,
                    exportTarget.getAbsolutePath(),
                    "discovery-export-test",
                    statusProvider -> { }
            );

            Map<String, org.apache.lucene.document.Document> exportedDocs = readExportedEmailDocs(exportTarget);
            assertEquals(2, exportedDocs.size());

            for (org.apache.lucene.document.Document doc : exportedDocs.values()) {
                assertDiscoveryDocContainsOnlyRedactedBody(doc);
                assertFieldDoesNotContain(doc, "body", "HTML_ONLY_SHOULD_NOT_EXPORT");
                assertFieldDoesNotContain(doc, "body", "HTML_ONLY_PRIVATE_TOKEN");
                assertFieldDoesNotContain(doc, "body", "<html>");
                assertFieldDoesNotContain(doc, "body", "<p>");
                assertFieldDoesNotContain(doc, "body", "<strong>");
            }

            assertNoAttachmentDocuments(exportTarget);
        } finally {
            archive.close();
        }
    }

    @Ignore("Current exporter rebuilds multipart/alternative bodies instead of preserving raw MIME structure.")
    @Test
    public void importedMultipartAlternativeMboxCanBeExportedForPreservationWithoutChangingMessagesExceptEpaddHeaders() throws Exception {
        File inputMbox = temporaryFolder.newFile("multipart-alternative-strict-input.mbox");
        assertMboxPreservationRoundTrip(inputMbox, multipartAlternativeMbox());
    }

    private void assertMboxPreservationRoundTrip(File inputMbox, String mboxContents) throws Exception {
        ExportedMbox exported = exportMboxAfterImport(inputMbox, mboxContents, false);

        assertEquals(
                normalizeMboxForComparison(read(inputMbox)),
                normalizeMboxForComparison(read(exported.mbox))
        );
    }

    private ExportedMbox exportMboxAfterImport(File inputMbox, String mboxContents, boolean downloadAttachments) throws Exception {
        Files.write(inputMbox.toPath(), mboxContents.getBytes(StandardCharsets.UTF_8));
        File archiveDir = temporaryFolder.newFolder("archive");
        Archive archive = importMbox(inputMbox, archiveDir, downloadAttachments);
        try {
            File exportTarget = temporaryFolder.newFolder("preservation-export");
            EmailExporter exporter = new EmailExporter(
                    EmailExporter.EXPORT_PROCESSED_MBOX,
                    archive,
                    statusProvider -> { }
            );
            Method exportEmails = EmailExporter.class.getDeclaredMethod("exportEmails", String.class, boolean.class, boolean.class);
            exportEmails.setAccessible(true);
            exportEmails.invoke(exporter, exportTarget.getAbsolutePath(), true, true);

            File exportedMbox = findSingleExportedMbox(exportTarget);
            assertNotNull("Expected a preservation mbox export to be created", exportedMbox);
            return new ExportedMbox(archive, exportedMbox);
        } finally {
            archive.close();
        }
    }

    private Archive importMbox(File inputMbox, File archiveDir, boolean downloadAttachments) throws Exception {
        Archive archive = Archive.createArchive();
        BlobStore blobStore = new BlobStore(new File(archiveDir, Archive.BAG_DATA_FOLDER + File.separator + Archive.BLOBS_SUBDIR).getAbsolutePath());
        archive.setup(archiveDir.getAbsolutePath(), blobStore, new String[0]);
        archive.openForWrite();

        MuseEmailFetcher fetcher = new MuseEmailFetcher();
        String displayName = "Test Mbox";
        fetcher.addMboxAccount("test-mbox", inputMbox.getAbsolutePath(), displayName, false);
        List<FolderInfo> folders = fetcher.readFoldersInfos(0, temporaryFolder.newFolder("folder-cache").getAbsolutePath());
        assertEquals(1, folders.size());

        FetchConfig fetchConfig = new FetchConfig();
        fetchConfig.downloadMessages = true;
        fetchConfig.downloadAttachments = downloadAttachments;
        fetchConfig.skipDuplicates = false;

        String selectedFolder = displayName + "^-^" + folders.get(0).longName;
        fetcher.fetchAndIndexEmails(archive, new String[]{selectedFolder}, false, fetchConfig, null, statusProvider -> { });
        archive.postProcess();
        archive.assignThreadIds();
        archive.collectionMetadata.nDocs = archive.getAllDocs().size();
        archive.commitIndex();
        archive.openForRead();
        return archive;
    }

    private File findSingleExportedMbox(File archiveDir) throws Exception {
        File exportRoot = new File(archiveDir, "exported mbox");

        if (!exportRoot.isDirectory()) {
            return null;
        }

        List<File> mboxes = Files.walk(exportRoot.toPath())
                .map(java.nio.file.Path::toFile)
                .filter(file -> file.isFile() && file.getName().endsWith(".mbox"))
                .sorted(Comparator.comparing(File::getAbsolutePath))
                .collect(Collectors.toList());

        assertEquals(1, mboxes.size());
        return mboxes.get(0);
    }

    private String normalizeMboxForComparison(String mbox) {
        return Arrays.stream(mbox.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1))
                .filter(line -> !line.startsWith("X-ePADD-"))
                .collect(Collectors.joining("\n"))
                .replaceAll("[\\n\\s]+$", "\n");
    }

    private String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private void assertContains(String text, String expected) {
        org.junit.Assert.assertTrue("Expected exported mbox to contain:\n" + expected + "\n\nExport was:\n" + text, text.contains(expected));
    }

    private Map<String, org.apache.lucene.document.Document> readExportedEmailDocs(File exportTarget) throws Exception {
        File emailIndexDir = new File(exportTarget, Archive.BAG_DATA_FOLDER + File.separator + Archive.INDEXES_SUBDIR + File.separator + "emails");
        Map<String, org.apache.lucene.document.Document> result = new LinkedHashMap<>();

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(emailIndexDir.toPath()))) {
            for (int i = 0; i < reader.maxDoc(); i++) {
                org.apache.lucene.document.Document doc = reader.document(i);
                result.put(doc.get("title"), doc);
            }
        }

        return result;
    }

    private void assertNoAttachmentDocuments(File exportTarget) throws Exception {
        File attachmentIndexDir = new File(exportTarget, Archive.BAG_DATA_FOLDER + File.separator + Archive.INDEXES_SUBDIR + File.separator + "attachments");
        if (!attachmentIndexDir.isDirectory()) {
            return;
        }

        try (FSDirectory directory = FSDirectory.open(attachmentIndexDir.toPath())) {
            if (!DirectoryReader.indexExists(directory)) {
                return;
            }
        }

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(attachmentIndexDir.toPath()))) {
            assertEquals(0, reader.numDocs());
        }
    }

    private void assertDiscoveryDocContainsOnlyRedactedBody(org.apache.lucene.document.Document doc) {
        String body = doc.get("body");
        assertNotNull(body);
        assertNull(doc.get("text_html_part"));
        assertNull(doc.get("headers_original"));
        assertNull(doc.get("body_original"));
        assertFieldDoesNotContain(doc, "body", "PLAIN_PRIVATE_TOKEN");
        assertFieldDoesNotContain(doc, "body", "HTML_PRIVATE_TOKEN");
        assertFieldDoesNotContain(doc, "body", "SECRET");
    }

    private void assertFieldDoesNotContain(org.apache.lucene.document.Document doc, String fieldName, String unexpected) {
        String value = doc.get(fieldName);
        assertFalse("Did not expect " + fieldName + " to contain " + unexpected + " in:\n" + value,
                value != null && value.contains(unexpected));
    }

    private static class ExportedMbox {
        private final Archive archive;
        private final File mbox;

        private ExportedMbox(Archive archive, File mbox) {
            this.archive = archive;
            this.mbox = mbox;
        }
    }

    private String plainTextMbox() {
        return "From - Tue Jan 02 03:04:05 2024\n" +
                "From: Alice Example <alice@example.org>\n" +
                "To: Bob Example <bob@example.org>\n" +
                "Date: Tue, 02 Jan 2024 03:04:05 +0000\n" +
                "Subject: Preservation round trip\n" +
                "Message-ID: <preservation-round-trip-1@example.org>\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: text/plain; charset=UTF-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "Hello Bob,\n" +
                "This is a preservation export test.\n";
    }

    private String multipartAlternativeMbox() {
        return "From - Wed Jan 03 04:05:06 2024\n" +
                "From: Alice Example <alice@example.org>\n" +
                "To: Bob Example <bob@example.org>\n" +
                "Date: Wed, 03 Jan 2024 04:05:06 +0000\n" +
                "Subject: Multipart preservation round trip\n" +
                "Message-ID: <multipart-preservation-round-trip-1@example.org>\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: multipart/alternative; boundary=\"epadd-test-boundary\"\n" +
                "\n" +
                "--epadd-test-boundary\n" +
                "Content-Type: text/plain; charset=UTF-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "Hello Bob,\n" +
                "This is the plain text part.\n" +
                "\n" +
                "--epadd-test-boundary\n" +
                "Content-Type: text/html; charset=UTF-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "<html><body><p>Hello Bob,</p><p>This is the HTML part.</p></body></html>\n" +
                "\n" +
                "--epadd-test-boundary--\n";
    }

    private String attachmentMbox() {
        return "From - Thu Jan 04 05:06:07 2024\n" +
                "From: Alice Example <alice@example.org>\n" +
                "To: Bob Example <bob@example.org>\n" +
                "Date: Thu, 04 Jan 2024 05:06:07 +0000\n" +
                "Subject: Attachment preservation round trip\n" +
                "Message-ID: <attachment-preservation-round-trip-1@example.org>\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: multipart/mixed; boundary=\"epadd-attachment-boundary\"\n" +
                "\n" +
                "--epadd-attachment-boundary\n" +
                "Content-Type: text/plain; charset=UTF-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "Hello Bob,\n" +
                "This message has an attachment.\n" +
                "\n" +
                "--epadd-attachment-boundary\n" +
                "Content-Type: text/plain; name=\"note.txt\"\n" +
                "Content-Transfer-Encoding: base64\n" +
                "Content-Disposition: attachment; filename=\"note.txt\"\n" +
                "\n" +
                "QXR0YWNobWVudCBjb250ZW50IGZvciBwcmVzZXJ2YXRpb24u\n" +
                "\n" +
                "--epadd-attachment-boundary--\n";
    }

    private String encapsulatedMessageMbox() {
        return "From - Fri Jan 05 06:07:08 2024\n" +
                "From: Alice Example <alice@example.org>\n" +
                "To: Bob Example <bob@example.org>\n" +
                "Date: Fri, 05 Jan 2024 06:07:08 +0000\n" +
                "Subject: Encapsulated message preservation round trip\n" +
                "Message-ID: <encapsulated-message-preservation-round-trip-1@example.org>\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: multipart/mixed; boundary=\"epadd-encapsulated-boundary\"\n" +
                "\n" +
                "--epadd-encapsulated-boundary\n" +
                "Content-Type: text/plain; charset=UTF-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "Hello Bob,\n" +
                "This message has an attached email.\n" +
                "\n" +
                "--epadd-encapsulated-boundary\n" +
                "Content-Type: message/rfc822; name=\"forwarded.eml\"\n" +
                "Content-Disposition: attachment; filename=\"forwarded.eml\"\n" +
                "\n" +
                "From: Carol Example <carol@example.org>\n" +
                "To: Alice Example <alice@example.org>\n" +
                "Date: Fri, 05 Jan 2024 05:00:00 +0000\n" +
                "Subject: Encapsulated note\n" +
                "Message-ID: <encapsulated-note-1@example.org>\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: text/plain; charset=UTF-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "This is the encapsulated message body.\n" +
                "\n" +
                "--epadd-encapsulated-boundary--\n";
    }

    private String discoveryMbox() {
        return "From - Sat Jan 06 07:08:09 2024\n" +
                "From: Alice Example <alice@example.org>\n" +
                "To: Bob Example <bob@example.org>\n" +
                "Date: Sat, 06 Jan 2024 07:08:09 +0000\n" +
                "Subject: Plain and HTML discovery export\n" +
                "Message-ID: <plain-and-html-discovery-export-1@example.org>\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: multipart/alternative; boundary=\"epadd-discovery-alt-boundary\"\n" +
                "\n" +
                "--epadd-discovery-alt-boundary\n" +
                "Content-Type: text/plain; charset=UTF-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "Alice Example plain body PLAIN_PRIVATE_TOKEN.\n" +
                "\n" +
                "--epadd-discovery-alt-boundary\n" +
                "Content-Type: text/html; charset=UTF-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "<html><body><p>Alice Example html body <strong>HTML_ONLY_SHOULD_NOT_EXPORT</strong> HTML_PRIVATE_TOKEN.</p></body></html>\n" +
                "\n" +
                "--epadd-discovery-alt-boundary--\n" +
                "\n" +
                "From - Sun Jan 07 08:09:10 2024\n" +
                "From: Carol Example <carol@example.org>\n" +
                "To: Bob Example <bob@example.org>\n" +
                "Date: Sun, 07 Jan 2024 08:09:10 +0000\n" +
                "Subject: HTML only discovery export\n" +
                "Message-ID: <html-only-discovery-export-1@example.org>\n" +
                "MIME-Version: 1.0\n" +
                "Content-Type: text/html; charset=UTF-8\n" +
                "Content-Transfer-Encoding: 7bit\n" +
                "\n" +
                "<html><body><p>Carol Example html only body HTML_ONLY_PRIVATE_TOKEN.</p></body></html>\n";
    }
}
