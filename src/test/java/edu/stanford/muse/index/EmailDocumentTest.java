package edu.stanford.muse.index;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.util.Date;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

@RunWith(JUnit4.class)
public class EmailDocumentTest {
    public Indexer li;
    public Archive archive;

    @Test
    public void testEmailDocumentExport() throws Exception {
        // setup index for EmailDocument to print export files
        File testDir = new File("src/test/resources/EmailDocumentTest");
        if (! testDir.exists()) {
            testDir.mkdir();
        }
        li = new Indexer("src/test/resources/EmailDocumentTest/Index", new IndexOptions());
        String file1 = "src/test/resources/EmailDocumentTest/test1.txt";
        String file2 = "src/test/resources/EmailDocumentTest/test2.txt";
        archive = Archive.createArchive();
        archive.indexer = li;

        EmailDocument ed1 = new EmailDocument("1", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc(" ssn 123-45 6789 ", "name 1 is John Smith.  credit card # 1234 5678 9012 3456 ", "", ed1, null);
        EmailDocument ed2 = new EmailDocument("6", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[]{new InternetAddress("a.u.thor@example.com")}, "", "", new Date());
        String title = "another patch";
        String documentText = "Here is a patch from A U Thor.  This addresses the issue raised in themessage:From: Nit Picker <nit.picker@example.net>Subject: foo is too oldMessage-Id: <nitpicker.12121212@example.net>Hopefully this would fix the problem stated there.I have included an extra blank line above, but it does not have to bestripped away here, along with the               \t\t   whitespaces at the end of the above line.  They are expected to be squashedwhen the message is made into a commit log by stripspace,Also, there are three blank lines after this paragraph,two truly blank and another full of spaces in between.            Hope this helps.--- foo |    2 +- 1 files changed, 1 insertions(+), 1 deletions(-)diff --git a/foo b/fooindex 9123cdc..918dcf8 100644--- a/foo+++ b/foo@@ -1 +1 @@-Fri Jun  9 00:44:04 PDT 2006+Fri Jun  9 00:44:13 PDT 2006-- 1.4.0.g6f2b\n";
        String documentHeaders = "From: A U Thor <a.u.thor@example.com>; Date: Fri, 9 Jun 2006 00:44:16 -0700; Garbage: uuuuuuuuu; Subject: [PATCH] another patch";
        li.indexSubdoc(title, documentText, documentHeaders, ed2, null);

        li.close();
        li.setupForRead();

        ed1.exportToFile(file1, archive);
        ed2.exportToFile(file2, archive);

        File testFile1 = new File(file1);
        File testFile2 = new File(file2);
        String testFile1String = FileUtils.readFileToString(testFile1, "utf-8");
        System.out.println(testFile1String);
        assertThat("testFile1 does not contain expected message.",
                testFile1String,
                containsString("MESSAGE: name 1 is John Smith.  credit card # 1234 5678 9012 3456 ")
                );
        assertThat("testFile1 contains HEADER unexpectedly.",
                testFile1String,
                not(containsString("HEADER: "))
        );

        String testFile2String = FileUtils.readFileToString(testFile2, "utf-8");
        System.out.println("========\n" + testFile2String);
        assertThat("testFile2 does not contain expected message.",
                testFile2String,
                containsString("MESSAGE: Here is a patch from A U Thor.  This addresses the issue raised in themessage:From: Nit Picker <nit.picker@example.net>Subject: foo is too oldMessage-Id: <nitpicker.12121212@example.net>Hopefully this would fix the problem stated there.I have included an extra blank line above, but it does not have to bestripped away here, along with the               \t\t   whitespaces at the end of the above line.  They are expected to be squashedwhen the message is made into a commit log by stripspace,Also, there are three blank lines after this paragraph,two truly blank and another full of spaces in between.            Hope this helps.--- foo |    2 +- 1 files changed, 1 insertions(+), 1 deletions(-)diff --git a/foo b/fooindex 9123cdc..918dcf8 100644--- a/foo+++ b/foo@@ -1 +1 @@-Fri Jun  9 00:44:04 PDT 2006+Fri Jun  9 00:44:13 PDT 2006-- 1.4.0.g6f2b")
        );
        assertThat("testFile2 does not contain expected HEADER.",
                testFile2String,
                containsString("HEADERS: From: A U Thor <a.u.thor@example.com>; Date: Fri, 9 Jun 2006 00:44:16 -0700; Garbage: uuuuuuuuu; Subject: [PATCH] another patch\n")
        );
        testFile1.delete();
        testFile2.delete();
        li.deleteAndCleanupFiles();
    }
}
