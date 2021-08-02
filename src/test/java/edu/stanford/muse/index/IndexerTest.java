package edu.stanford.muse.index;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.mail.Address;
import java.util.Collection;
import java.util.Date;
import static org.junit.Assert.assertEquals;

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
        li.indexSubdoc(" ssn 123-45 6789 ", "name 1 is John Smith.  credit card # 1234 5678 9012 3456 ", ed1, null);
        EmailDocument ed2 = new EmailDocument("2", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc(" ssn 123 45 6789", "name 1 is John Smith.  credit card not ending with a non-digit # 1234 5678 9012 345612 ", ed2, null);
        EmailDocument ed3 = new EmailDocument("3", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc(" ssn 123 45 6789", "name 1 is John Smith.  credit card # 111234 5678 9012 3456 ", ed3, null);
        EmailDocument ed4 = new EmailDocument("4", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc(" ssn 123 45 6789", "\nmy \nfirst \n book is \n something ", ed4, null);
        EmailDocument ed5 = new EmailDocument("5", "dummy", "dummy", new Address[0], new Address[0], new Address[0], new Address[0], "", "", new Date());
        li.indexSubdoc("passport number k4190893", "\nmy \nfirst \n book is \n something ", ed5, null);
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
}
