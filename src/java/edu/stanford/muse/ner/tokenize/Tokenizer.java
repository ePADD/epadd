package edu.stanford.muse.ner.tokenize;

import edu.stanford.muse.util.Triple;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A named entity extractor, this class is typically used as an initial step in Named Entity Recognizer pipeline.
 * Chunker is probably a more intuitive name for this.
 * */
public interface Tokenizer {
    /**
     * @param content to tokenize
     * Given a free-text (content) as input, extracts and outputs the entity mentions along with their start and end offsets in the content
     * @return triples of entity mentions
     * triples are such that content.substring(triple.getSecond(), triple.getThird()) is triple.getFirst()
     * */
    List<Triple<String, Integer, Integer>> tokenize(String content);

    /**
     * A convenience method to get just tokens without offsets
     */
    default Set<String> tokenizeWithoutOffsets(String content) {
        List<Triple<String, Integer, Integer>> offsets = tokenize(content);
        if(offsets!=null) {
            return offsets.stream().map(t -> t.first).collect(Collectors.toSet());
        }
        else
            return null;
    }
}
