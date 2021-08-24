package edu.stanford.muse.index;

import org.apache.lucene.analysis.CharArraySet;

import java.util.Arrays;
import java.util.List;

/***
 * List of stop words for EPADD and static method to get as CharArraySet for Lucene
 * eventually, can be used to call different version of stopwords
 * to avoid compatability issues with older/newer releases
 */
public class StopWords {
    // Warning: changes in this list requires re-indexing of all existing archives.
    static final List<String> stopWords_v1 = Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", /*
             * "not"
             * ,
             */"of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with"
    );

    static public CharArraySet getCharArraySet() {
        CharArraySet stopSet = new CharArraySet(stopWords_v1.size(), false);
        stopSet.addAll(stopWords_v1);
        return CharArraySet.unmodifiableSet(stopSet);
    }
}
