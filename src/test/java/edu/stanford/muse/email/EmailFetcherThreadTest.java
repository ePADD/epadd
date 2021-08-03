package edu.stanford.muse.email;

import edu.stanford.muse.index.EmailDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@RunWith(JUnit4.class)
public class EmailFetcherThreadTest{
    MboxEmailStore emailStore;
    FolderInfo folderInfo;
    Folder folder;
    EmailFetcherThread fetcherThread;

    @Before
    public void beforeEach() throws Exception {
        String dirPath = "src/test/resources/MboxEmailStoreTest";
        String mboxPath = dirPath + "/mbox";
        emailStore = new MboxEmailStore("", "", "");
		emailStore.setRootPath(mboxPath);
		emailStore.computeFoldersAndCounts(dirPath + "/compute");
        folderInfo = emailStore.folderInfos.get(0);
        folder = emailStore.get_folder(emailStore.connect(), folderInfo.longName);
        fetcherThread = new EmailFetcherThread(emailStore, folderInfo, 0, 11);
        FetchConfig fetchConfig = new FetchConfig();
        fetchConfig.filter = null;
        fetchConfig.downloadAttachments = true;
        fetchConfig.downloadMessages = true;
        fetcherThread.setFetchConfig(fetchConfig);
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
        MimeMessage mimeMessage = (MimeMessage)mimeMessages[0];
        String expectedId = "someId";
        EmailDocument emailDocument = fetcherThread.convertToEmailDocument(mimeMessage, expectedId);
        assertEquals("Email document ID should match given ID", expectedId, emailDocument.getId());
        assertNotNull("Email document should have a unique ID", emailDocument.getUniqueId());
        assertNotNull("Email document should have a date", emailDocument.date);
    }
}

