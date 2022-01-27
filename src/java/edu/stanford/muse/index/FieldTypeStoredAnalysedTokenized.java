package edu.stanford.muse.index;

import org.apache.lucene.document.FieldType;

public class FieldTypeStoredAnalysedTokenized extends FieldType {
    FieldTypeStoredAnalysedTokenized() {
        super();
        this.setStored(true);
        this.setIndexOptions(org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        this.freeze();
    }
}
