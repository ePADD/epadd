package edu.stanford.muse.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PairTest {
    @Test
    public void equalPairsHaveEqualHashCodes() {
        Pair<String, Integer> first = new Pair<>("alpha", 1);
        Pair<String, Integer> second = new Pair<>("alpha", 1);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void differentPairsAreNotEqual() {
        assertNotEquals(new Pair<>("alpha", 1), new Pair<>("alpha", 2));
        assertNotEquals(new Pair<>("alpha", 1), "alpha");
    }

    @Test
    public void settersUpdateValues() {
        Pair<String, Integer> pair = new Pair<>("alpha", 1);

        pair.setFirst("beta");
        pair.setSecond(2);

        assertEquals("beta", pair.getFirst());
        assertEquals(Integer.valueOf(2), pair.getSecond());
    }
}
