package edu.stanford.muse.util;

import edu.stanford.muse.email.MboxEmailStore;
import net.fortuna.mstor.connector.mbox.MboxFolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.mail.Folder;
import javax.mail.Message;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

@RunWith(JUnit4.class)
public class ConversionUtilsTest {

    public String getMboxFileText(MboxFolder folder) throws Exception{
        folder.open(Folder.READ_WRITE);
        String text = "";

        for (int i =1; i <= folder.getMessageCount(); ++i) {
            text += new BufferedReader(
                    new InputStreamReader(folder.getMessageAsStream(i), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n")) + "\n";
        }
        return text;
    }

    @Test
    public void saveMessagesAsMboxFile() throws Exception{
        MboxEmailStore mboxEmailStore = new MboxEmailStore("", "", "src/test/resources/MboxEmailStoreTest/mbox/sample.mbox");
        Folder folder = mboxEmailStore.get_folder(null, mboxEmailStore.getRootPath());
        Message[] messages = folder.getMessages();
        File testDir = new File("src/test/resources/ConversionUtilsTest/mbox/");
        if (! testDir.isDirectory()) {
            testDir.mkdir();
        }
        File test1Mbox = new File("src/test/resources/ConversionUtilsTest/mbox/test1.mbox");
        MboxFolder mboxLocation = new MboxFolder(test1Mbox);
        ConversionUtils.saveMessagesAsMbox(messages, mboxLocation);

        MboxFolder folder1 = new MboxFolder(test1Mbox);
        folder1.open(Folder.READ_WRITE);
        int actualCount = folder1.getMessageCount();
        String folderText = getMboxFileText(folder1);
        folder1.delete();

        assertEquals("Message count in exported file: ",  12,
                actualCount);

        assertThat(folderText,
                startsWith("From: A U Thor <a.u.thor@example.com>") );
    }

    @Test
    public void testOutlookMsgsToMboxFolder() throws Exception {
        File msgsDir = new File("src/test/resources/ConversionUtilsTest/msgTest1/");
        List<File> msgList =  Arrays.stream(msgsDir.listFiles()).filter(File::isFile).collect(Collectors.toCollection(ArrayList::new));

        File test1Mbox = new File("src/test/resources/ConversionUtilsTest/mbox/test2.mbox");
        MboxFolder mboxLocation = new MboxFolder(test1Mbox);
        ConversionUtils.outlookMsgsToMboxFolder(msgList, mboxLocation);

        MboxFolder folder1 = new MboxFolder(test1Mbox);
        folder1.open(Folder.READ_WRITE);
        int actualCount = folder1.getMessageCount();
        String folderText = getMboxFileText(folder1);
        folder1.delete();

        assertEquals("Number of expected messages in mbox file", 4, actualCount );

        assertThat(folderText,
                containsString("From: Janette Martin <janette.martin@manchester.ac.uk>\n" +
                "To: Gareth Lloyd <gareth.lloyd@manchester.ac.uk>") );
        assertThat(folderText,
                containsString("From: Jane Gallagher <jane.gallagher@manchester.ac.uk>\n" +
                        "To: Elizabeth Gow <elizabeth.gow@manchester.ac.uk>") );
        assertThat(folderText,
                containsString("From: Janette Martin <janette.martin@manchester.ac.uk>\n" +
                        "To: Gareth Lloyd <gareth.lloyd@manchester.ac.uk>") );
        assertThat(folderText,
                containsString("From: Liza Leonard <liza.leonard@manchester.ac.uk>\n" +
                        "To: Jane Gallagher <jane.gallagher@manchester.ac.uk>") );
    }
}
