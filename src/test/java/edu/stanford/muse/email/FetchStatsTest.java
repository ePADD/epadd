package edu.stanford.muse.email;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class FetchStatsTest {

    @Test
    public void testFetchStatsToString() {
        FetchStats as = new FetchStats();
        String asToString = as.toString();
        System.out.println(asToString);
        assertTrue("Output string isn't larger than zero", asToString.length() > 0 );
    }
}
