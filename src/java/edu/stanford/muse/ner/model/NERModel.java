package edu.stanford.muse.ner.model;

import edu.stanford.muse.ner.tokenize.Tokenizer;
import edu.stanford.muse.util.Span;

public interface NERModel {
    /**
     * @param messageId - Message-ID header of message
     * @return spans of text found in the content that contain the type and offset info. of the entity
     */
    Span[] find (String messageId);

    void setTokenizer(Tokenizer tokenizer);
}
