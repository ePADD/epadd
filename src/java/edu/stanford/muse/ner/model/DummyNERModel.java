package edu.stanford.muse.ner.model;

import edu.stanford.muse.ner.tokenize.CICTokenizer;
import edu.stanford.muse.ner.tokenize.Tokenizer;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.util.Triple;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by vihari on 24/02/16.
 * A dummy model that simulates the behavior of a openNLPNER model.
 * This model returns all pseudo proper nouns in the content and hence generally is a super-set of all the possible entities in the content
 */
public class DummyNERModel implements NERModel{
    private Tokenizer tokenizer = new CICTokenizer();
    public Span[] find (String content) {
        // collect all pseudo proper nouns
        List<Triple<String, Integer, Integer>> pns = tokenizer.tokenize(content);
        //we will make a dummy object of type map

        return pns.stream().map(pn -> new Span(pn.getFirst(), pn.getSecond(), pn.getThird())).toArray(Span[]::new);
    }

    @Override
    public void setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }
}
