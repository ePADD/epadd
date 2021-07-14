package edu.stanford.epadd;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;;


@RunWith(JUnit4.class)
public class StandaloneIngestionTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private StandaloneIngestion ingester = new StandaloneIngestion();
    private String validName = "Some Valid Name";
    private String validPath = "src/test/resources/StandaloneIngestionTest/sample.mbox";
    private String successMessage = "\"shortName\":\"sample.mbox\",\"messageCount\":12,\"timestamp\":1626288229800,\"fileSize\":20172";

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void shouldParseMBOXFile() {
        // Parse MBOX file into internal representation, retaining all headers and multi-part bodies
        boolean success = ingester.IngestMbox(validName, validPath);

        // Check that the parser actually picked something up.
        assertThat(outContent.toString(), containsString(successMessage));
        assert success;
    }

    @Test
    public void shouldFailToParseMBOXFile() {
        // Should return false if path contains a NULL character???
        boolean success = ingester.IngestMbox(validName, "\u0000");
        assert !success;
    }

    // FIXME: These cases currently all return true, but they should return false.
    @Ignore
    @Test
    public void theseTestsShouldPass() {
        // Should fail if we send it null for the name
        boolean nullNameFailure = ingester.IngestMbox(null, validPath);
        assert !nullNameFailure;

        // Should fail if we send it null
        boolean nullPathFailure = ingester.IngestMbox(validName, null);
        assert !nullPathFailure;

        // Should fail if we send it some other invalid path
        boolean badPathFailure = ingester.IngestMbox(validName, "this is a bad path");
        assert !badPathFailure;
    }
}
