package edu.stanford.muse.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UtilTest {
    @Test
    public void truncatePadsShortStringsAndEllipsizesLongStrings() {
        assertEquals("abc  ", Util.truncate("abc", 5));
        assertEquals("ab...", Util.truncate("abcdef", 5));
        assertEquals("???  ", Util.truncate(null, 5));
    }

    @Test
    public void nullOrEmptyHandlesCommonContainers() {
        assertTrue(Util.nullOrEmpty((String) null));
        assertTrue(Util.nullOrEmpty(""));
        assertTrue(Util.nullOrEmpty(new String[0]));
        assertTrue(Util.nullOrEmpty(Collections.emptyList()));
        assertTrue(Util.nullOrEmpty(Collections.emptyMap()));

        assertFalse(Util.nullOrEmpty("value"));
        assertFalse(Util.nullOrEmpty(new String[]{"value"}));
        assertFalse(Util.nullOrEmpty(Collections.singletonList("value")));
        assertFalse(Util.nullOrEmpty(Collections.singletonMap("key", "value")));
    }

    @Test
    public void fileTypeHelpersAreCaseInsensitive() {
        assertTrue(Util.is_doc_filename("Report.PDF"));
        assertTrue(Util.is_image_filename("Photo.JPEG"));
        assertTrue(Util.is_html_filename("index.JSP"));
        assertTrue(Util.is_zip_filename("archive.ZIP"));
        assertFalse(Util.is_pdf_filename("archive.zip"));
    }

    @Test
    public void removeDupsPreservesFirstOccurrenceOrder() {
        List<String> input = Arrays.asList("alpha", "beta", "alpha", "gamma", "beta");

        assertEquals(Arrays.asList("alpha", "beta", "gamma"), Util.removeDups(input));
    }

    @Test
    public void removeDupsReturnsOriginalListWhenNoDuplicatesExist() {
        List<String> input = Arrays.asList("alpha", "beta", "gamma");

        assertSame(input, Util.removeDups(input));
    }

    @Test
    public void hashUsesSha256HexEncoding() {
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", Util.hash("hello"));
    }

    @Test
    public void emailMaskingPreservesOnlyAllowedPieces() {
        assertEquals("Write to ...@... or admin@localhost", Util.maskEmail("Write to alice@example.org or admin@localhost"));
        assertEquals("Write to alice@... or admin@...", Util.maskEmailDomain("Write to alice@example.org or admin@localhost"));
        assertNull(Util.maskEmail(null));
    }

    @Test
    public void cleanNameRemovesGreetingAndRejectsStopWordsOrHyphens() {
        assertEquals("Ada Lovelace", Util.cleanName("Hello Ada Lovelace"));
        assertEquals("Grace Hopper", Util.cleanName("Subject: Grace Hopper"));
        assertNull(Util.cleanName("Board of Directors"));
        assertNull(Util.cleanName("Jean-Luc Picard"));
    }

    @Test
    public void scrubNamesRemovesMarkupQuotesAndDuplicateCleanedNames() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("Ada Lovelace", "Grace Hopper"));

        assertEquals(expected, Util.scrubNames(Arrays.asList(
                " <b>Ada Lovelace</b> ",
                "\"Grace Hopper\"",
                "{Ada Lovelace}"
        )));
    }

    @Test
    public void wholeWordMatchingRejectsPartialWordMatches() {
        assertTrue(Util.occursOnlyAsWholeWord("America and South America", "America"));
        assertFalse(Util.occursOnlyAsWholeWord("Americans in America", "America"));
        assertFalse(Util.occursOnlyAsWholeWord("No matching country", "America"));
    }
}
