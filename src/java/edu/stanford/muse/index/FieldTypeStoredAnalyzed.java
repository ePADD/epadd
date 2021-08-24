package edu.stanford.muse.index;

import org.apache.lucene.document.FieldType;

public class FieldTypeStoredAnalyzed extends FieldType {
    FieldTypeStoredAnalyzed() {
        super();
        this.setStored(true);
        this.setIndexOptions(org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS);
        this.setTokenized(false);
        this.freeze();
    }
}
