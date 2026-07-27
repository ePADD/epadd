package edu.stanford.muse.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnionFindSetTest {
    @Test
    public void connectedElementsAreGroupedTogether() {
        UnionFindSet<String> set = new UnionFindSet<>();

        set.unify("alice", "bob");
        set.unify("carol", "dave");
        set.unify("bob", "carol");

        List<List<String>> classes = set.getClassesSortedByClassSize();

        assertEquals(1, classes.size());
        assertEquals(new HashSet<>(Arrays.asList("alice", "bob", "carol", "dave")), new HashSet<>(classes.get(0)));
    }

    @Test
    public void classesAreSortedByDescendingSize() {
        UnionFindSet<String> set = new UnionFindSet<>();

        set.unify("alice", "bob");
        set.unify("carol", "dave");
        set.unify("dave", "erin");
        set.unify("frank", "grace");

        List<List<String>> classes = set.getClassesSortedByClassSize();
        List<Integer> sizes = classes.stream().map(List::size).collect(Collectors.toList());

        assertEquals(Arrays.asList(3, 2, 2), sizes);
        assertTrue(toSet(classes).contains(new HashSet<>(Arrays.asList("carol", "dave", "erin"))));
    }

    private Set<Set<String>> toSet(List<List<String>> classes) {
        return classes.stream().map(HashSet::new).collect(Collectors.toSet());
    }
}
