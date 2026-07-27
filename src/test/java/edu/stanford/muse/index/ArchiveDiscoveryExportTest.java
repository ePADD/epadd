package edu.stanford.muse.index;

import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.email.FetchConfig;
import edu.stanford.muse.email.FolderInfo;
import edu.stanford.muse.email.MuseEmailFetcher;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ArchiveDiscoveryExportTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void discoveryExportRemovesHtmlPartsAndOriginalUnredactedText() throws Exception {
        File inputMbox = temporaryFolder.newFile("discovery-input.mbox");
        Files.write(inputMbox.toPath(), discoveryMbox().getBytes(StandardCharsets.UTF_8));

       // File archiveDir = temporaryFolder.newFolder("archive");

        File debugRoot = new File("target/discovery-export-debug");
        File archiveDir = new File(debugRoot, "source-archive");

        Archive archive = importMbox(inputMbox, archiveDir);
        try {
            File exportTarget = new File(debugRoot, "discovery-export");

//            File exportTarget = temporaryFolder.newFolder("discovery-export");
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

    private Archive importMbox(File inputMbox, File archiveDir) throws Exception {
        Archive archive = Archive.createArchive();
        BlobStore blobStore = new BlobStore(new File(archiveDir, Archive.BAG_DATA_FOLDER + File.separator + Archive.BLOBS_SUBDIR).getAbsolutePath());
        new File(archiveDir, Archive.BAG_DATA_FOLDER + File.separator + Archive.INDEXES_SUBDIR + File.separator + "emails").mkdirs();
        new File(archiveDir, Archive.BAG_DATA_FOLDER + File.separator + Archive.INDEXES_SUBDIR + File.separator + "attachments").mkdirs();
        archive.setup(archiveDir.getAbsolutePath(), blobStore, new String[0]);
        archive.openForWrite();

        MuseEmailFetcher fetcher = new MuseEmailFetcher();
        String displayName = "Discovery Test Mbox";
        fetcher.addMboxAccount("test-mbox", inputMbox.getAbsolutePath(), displayName, false);
        List<FolderInfo> folders = fetcher.readFoldersInfos(0, temporaryFolder.newFolder("folder-cache").getAbsolutePath());
        assertEquals(1, folders.size());

        FetchConfig fetchConfig = new FetchConfig();
        fetchConfig.downloadMessages = true;
        fetchConfig.downloadAttachments = true;
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
