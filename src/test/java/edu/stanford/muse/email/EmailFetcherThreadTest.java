package edu.stanford.muse.email;

import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import net.didion.jwnl.data.Exc;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class EmailFetcherThreadTest {
    String dirPath = "src/test/resources/MboxEmailStoreTest";
    Archive archive;
    MboxEmailStore emailStore;
    FolderInfo folderInfo;
    Folder folder;
    EmailFetcherThread fetcherThread;

    @Before
    public void beforeEach() throws Exception {
        String mboxPath = dirPath + "/mbox";
        BlobStore blobStore = new BlobStore(dirPath + "/blobs");
        archive = Archive.createArchive();
        archive.setup(dirPath + "/archive", blobStore, null);
        emailStore = new MboxEmailStore("", "", "");
        emailStore.setRootPath(mboxPath);
        emailStore.computeFoldersAndCounts(dirPath + "/compute");
        folderInfo = emailStore.folderInfos.get(0);
        folder = emailStore.get_folder(emailStore.connect(), folderInfo.longName);
        fetcherThread = new EmailFetcherThread(emailStore, folderInfo, 1, 13);
        FetchConfig fetchConfig = new FetchConfig();
        fetchConfig.filter = null;
        fetchConfig.downloadAttachments = true;
        fetchConfig.downloadMessages = true;
        fetcherThread.setFetchConfig(fetchConfig);
        fetcherThread.setArchive(archive);
    }

    @After
    public void afterEach() throws Exception {
        archive.indexer.deleteAndCleanupFiles();
        archive.clear();
        archive.close();
        fetcherThread.finish();
    }

    @Test
    public void testFetchAndIndexMessages() throws Exception {
        Message[] mimeMessages = folder.getMessages();
        int numMessagesBefore = fetcherThread.stats.nMessagesAdded;
        assertEquals(0, numMessagesBefore);

        fetcherThread.fetchAndIndexMessages(folder, mimeMessages, 0, 12);
        int numMessagesAdded = fetcherThread.stats.nMessagesAdded;
        int numMessagesAlreadyPresent = fetcherThread.stats.nMessagesAlreadyPresent;
        assertEquals(12, numMessagesAdded + numMessagesAlreadyPresent);

        assertEquals("Should record number of messages with errors", 4, fetcherThread.dataErrors.size());
        assertEquals("Should record number of messages added", numMessagesAdded, archive.indexer.getStats().getNDocuments());

        // Check that all the docs are stored in the archive
        archive.openForRead();
        List<Document> docs = archive.getAllDocs();
        assertEquals("Archive should contain all messages added", numMessagesAdded, docs.size());

        // Check that the index contains all header info
        String contents = archive.getContents(docs.get(7), true);

        List<String> headerStrings = Arrays.asList(
            "From: Dmitriy Blinov <bda@mnsspb.ru>",
            "To: navy-patches@dinar.mns.mnsspb.ru",
            "Date: Wed, 12 Nov 2008 17:54:41 +0300",
            "Message-Id: <1226501681-24923-1-git-send-email-bda@mnsspb.ru>",
            "X-Mailer: git-send-email 1.5.6.5",
            "MIME-Version: 1.0",
            "Content-Type: text/plain;",
            "charset=utf-8",
            "Content-Transfer-Encoding: 8bit"
        );

        headerStrings.parallelStream().forEach(s -> assertThat("Fetcher should index all headers", contents, containsString(s)));
    }

    @Test
    public void testOpenFolderAndGetMessages() throws Exception {
        Message[] mimeMessages = fetcherThread.openFolderAndGetMessages();
        assertNotNull("Mime message should not be null", mimeMessages);
        assertEquals("Number of message should match the number of messages in mbox file", 12, mimeMessages.length);
    }

    @Test
    public void testConvertToEmailDocument() throws Exception {
        Message[] mimeMessages = folder.getMessages();
        assertEquals("Number of message should match the number of messages in mbox file", 12, mimeMessages.length);
        MimeMessage mimeMessage = (MimeMessage) mimeMessages[0];
        String expectedId = "someId";
        EmailDocument emailDocument = fetcherThread.convertToEmailDocument(mimeMessage, expectedId);
        assertEquals("Email document ID should match given ID", expectedId, emailDocument.getId());
        assertNotNull("Email document should have a unique ID", emailDocument.getUniqueId());
        assertNotNull("Email document should have a date", emailDocument.date);
    }
}

