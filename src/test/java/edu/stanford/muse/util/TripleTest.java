package edu.stanford.muse.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TripleTest {
    @Test
    public void equalTriplesHaveEqualHashCodes() {
        Triple<String, Integer, Boolean> first = new Triple<>("alpha", 1, true);
        Triple<String, Integer, Boolean> second = new Triple<>("alpha", 1, true);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void differentTriplesAreNotEqual() {
        assertNotEquals(new Triple<>("alpha", 1, true), new Triple<>("alpha", 1, false));
        assertNotEquals(new Triple<>("alpha", 1, true), "alpha");
    }

    @Test
    public void accessorsReturnConstructorValues() {
        Triple<String, Integer, Boolean> triple = new Triple<>("alpha", 1, true);

        assertEquals("alpha", triple.first());
        assertEquals(Integer.valueOf(1), triple.second());
        assertEquals(Boolean.TRUE, triple.third());
    }
}
