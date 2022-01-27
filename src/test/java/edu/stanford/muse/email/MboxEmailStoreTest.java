package edu.stanford.muse.email;

import net.fortuna.mstor.MStorStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.mail.MessagingException;
import javax.mail.Store;
import java.io.IOException;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class MboxEmailStoreTest {

    @Test
    public void connectShouldCreateDummyStoreTest() throws IOException, MessagingException {
        MboxEmailStore mboxEmailStore = new MboxEmailStore("", "", "");
        Store store = mboxEmailStore.connect();
        assert store != null;
        assert store instanceof MStorStore;
        assert store.isConnected();
    }

    @Test(expected = NullPointerException.class)
    public void nullPathThrowsError() throws IOException {
        // Should fail if we send it null
        MboxEmailStore nullPathFailure = new MboxEmailStore("", "", null);
    }

    @Test(expected = NullPointerException.class)
    public void invalidNameThrowsError() throws IOException {
        // Should fail if we send it null for the name
        MboxEmailStore nullNameFailure = new MboxEmailStore("", null, "");
    }

    // FIXME: These cases currently all pass with no exceptions thrown, but they should throw exceptions.
    @Ignore
    @Test(expected = NullPointerException.class)
    public void invalidPathThrowsError() throws IOException {
        // Should fail if we send it some other invalid path
        MboxEmailStore badPathFailure = new MboxEmailStore("", "", "this is a bad path");
    }

    @Test
    public void computeFoldersAndCounts() throws IOException, MessagingException {
        String dirPath = "src/test/resources/MboxEmailStoreTest";
        String mboxPath = dirPath + "/mbox";
        MboxEmailStore me = new MboxEmailStore("", "", "");
		me.setRootPath(mboxPath);
		me.computeFoldersAndCounts(dirPath + "/compute");
		MboxEmailStore.FolderCache folderCache = me.getFolderCache();
		assert folderCache != null;

		int messageCount = folderCache.cacheMap.get(mboxPath + "/sample.mbox").messageCount;
		assertEquals(12, messageCount);

		me.deleteAndCleanupFiles();
		assertEquals("Cache directory should be deleted", false, me.getCacheDir().exists());
    }

}
