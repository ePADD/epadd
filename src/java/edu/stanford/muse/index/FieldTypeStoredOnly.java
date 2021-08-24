package edu.stanford.muse.index;

import org.apache.lucene.document.FieldType;

public class FieldTypeStoredOnly extends FieldType {
    FieldTypeStoredOnly() {
        super();
        this.setStored(true);
        this.freeze();
    }
}
