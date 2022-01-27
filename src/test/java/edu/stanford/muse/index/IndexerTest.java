package edu.stanford.muse.index;

import org.apache.lucene.document.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

@RunWith(JUnit4.class)
public class IndexerTest {
    public Indexer li;

    void runAndTestLookupDocs(String query, Indexer.QueryType queryType, int expected) throws Exception {
        Collection<EmailDocument> docs = li.lookupDocs(query, queryType);
        System.out.println("hits for: " + query + " = " + docs.size());
        assertEquals("hits for: " + query + " not " + docs.size(), expected, docs.size() );
    }

    void runAndTestGetNumHits(String query, Indexer.QueryType queryType, int expected) throws Exception {
        int numDocs = li.getNumHits(query, false, queryType);
        System.out.println("hits for: " + query + " = " + numDocs);
        assertEquals("hits for: " + query + " not " + numDocs, expected, numDocs);
    }

    @Before
    public void setup() throws Exception {
        li = new Indexer("src/test/resources/testIndex", new IndexOptions());

        // public EmailDocument(String id, String folderName, Address[] to, Address[] cc, Address[] bcc, Address[] from, String subject, String messageID, Date date)
        EmailDocument ed1 = new EmailDocument("1", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc(" ssn 123-45 6789 ", "name 1 is John Smith.  credit card # 1234 5678 9012 3456 ", "headers", ed1, null);
        EmailDocument ed2 = new EmailDocument("2", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc(" ssn 123 45 6789", "name 1 is John Smith.  credit card not ending with a non-digit # 1234 5678 9012 345612 ", "headers", ed2, null);
        EmailDocument ed3 = new EmailDocument("3", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc(" ssn 123 45 6789", "name 1 is John Smith.  credit card # 111234 5678 9012 3456 ","headers", ed3, null);
        EmailDocument ed4 = new EmailDocument("4", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc(" ssn 123 45 6789", "\nmy \nfirst \n book is \n something ","headers", ed4, null);
        EmailDocument ed5 = new EmailDocument("5", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc("passport number k4190893", "\nmy \nfirst \n book is \n something ","headers", ed5, null);
        li.close();
        li.setupForRead();
    }

    /***
     * Adssumes the test index has been setup for Read
     * @param ed
     */
    public void addAnotherDoc(EmailDocument ed, String title, String documentText, String headers) throws Exception {
        li.setupForWrite();
        li.indexSubdoc(title, documentText, headers, ed,null);
        li.close();
        li.setupForRead();
    }

    @After
    public void breakDown() throws Exception {
        li.deleteAndCleanupFiles();
    }

    @Test
    public void testGetNumHits() throws Exception {
        runAndTestGetNumHits("ssn", Indexer.QueryType.FULL, 4);
        runAndTestGetNumHits("\"john*smith\"", Indexer.QueryType.FULL, 3);
    }

    @Test
    public void testLookupDocsQueries() throws Exception {
        runAndTestLookupDocs( "john", Indexer.QueryType.FULL, 1);
        runAndTestLookupDocs( "/j..n/\\\\*", Indexer.QueryType.FULL, 1);
        runAndTestLookupDocs( "\"john\"", Indexer.QueryType.FULL, 1);
        runAndTestLookupDocs( "\"john smith\"", Indexer.QueryType.FULL, 1);
        runAndTestLookupDocs( "\"john*smith\"", Indexer.QueryType.FULL, 1);
        runAndTestLookupDocs( "john*smith", Indexer.QueryType.FULL, 0);
        runAndTestLookupDocs( "title:k4190893", Indexer.QueryType.FULL, 1);
        runAndTestLookupDocs( "title:subject", Indexer.QueryType.FULL, 0);
        runAndTestLookupDocs("body:johns", Indexer.QueryType.FULL, 1);
        runAndTestLookupDocs("joh*", Indexer.QueryType.FULL, 1);
        runAndTestLookupDocs("/j..n/", Indexer.QueryType.FULL, 1);
        // look for sequence of 4-4-4-4 . the .* at the beginning and end is needed.
        runAndTestLookupDocs("[0-9]{3}[\\- ]*[0-9]{2}[ \\-]*[0-9]{4}", Indexer.QueryType.REGEX, 1);
        // look for sequence of 3-2-4
        runAndTestLookupDocs("123-45[ \\-]*[0-9]{4}", Indexer.QueryType.REGEX, 1);
        runAndTestLookupDocs("first\\sbook", Indexer.QueryType.REGEX, 0);
        runAndTestLookupDocs("[A-Za-z][0-9]{7}", Indexer.QueryType.REGEX, 1);
    }

    @Test
    public void testGetAllDocsWithFields() throws Exception {
        List<org.apache.lucene.document.Document> allDocsLive = li.getAllDocsWithFields(false);
        assertEquals("Number of docs returned by getAllDocsWithFields is not correct.", 5, allDocsLive.size());
    }

    @Test
    public void testHeadersFieldReturnedAndIsNotSearchable() throws Exception {
        EmailDocument ed = new EmailDocument("6", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[]{ new InternetAddress("a.u.thor@example.com")}, "", "", new Date());
        String title = "another patch";
        String documentTest = "Here is a patch from A U Thor.  This addresses the issue raised in themessage:From: Nit Picker <nit.picker@example.net>Subject: foo is too oldMessage-Id: <nitpicker.12121212@example.net>Hopefully this would fix the problem stated there.I have included an extra blank line above, but it does not have to bestripped away here, along with the               \t\t   whitespaces at the end of the above line.  They are expected to be squashedwhen the message is made into a commit log by stripspace,Also, there are three blank lines after this paragraph,two truly blank and another full of spaces in between.            Hope this helps.--- foo |    2 +- 1 files changed, 1 insertions(+), 1 deletions(-)diff --git a/foo b/fooindex 9123cdc..918dcf8 100644--- a/foo+++ b/foo@@ -1 +1 @@-Fri Jun  9 00:44:04 PDT 2006+Fri Jun  9 00:44:13 PDT 2006-- 1.4.0.g6f2b\n";
        addAnotherDoc(ed, title, documentTest, "From: A U Thor <a.u.thor@example.com>; Date: Fri, 9 Jun 2006 00:44:16 -0700; Garbage: uuuuuuuuu; Subject: [PATCH] another patch");
        List<org.apache.lucene.document.Document> allDocsLive = li.getAllDocsWithFields(false);
        assertEquals("Number of docs returned by getAllDocsWithFields is not correct.", 6, allDocsLive.size());
        allDocsLive.stream().forEach(d -> {
            assertThat(d.getField("headers_original"), is(notNullValue()));
        });
        assertEquals("Complete header returned with doc.", allDocsLive.get(5).get("headers_original"), "From: A U Thor <a.u.thor@example.com>; Date: Fri, 9 Jun 2006 00:44:16 -0700; Garbage: uuuuuuuuu; Subject: [PATCH] another patch");

        runAndTestGetNumHits("A U Thor", Indexer.QueryType.FULL, 1);
        runAndTestGetNumHits("Garbage", Indexer.QueryType.FULL, 0);

        assertEquals("Try another way to query, should return.", 0, li.docsForQuery("Garbage",new Indexer.QueryOptions()).size());
        assertEquals("Try another way to query, should return.", 1, li.docsForQuery("nitpicker",new Indexer.QueryOptions()).size());
    }
}
