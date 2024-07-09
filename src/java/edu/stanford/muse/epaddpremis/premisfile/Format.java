package edu.stanford.muse.epaddpremis.premisfile;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Format  implements Serializable {

    private static final long serialVersionUID = 1810180326171428825L;
    @XmlElement
    private final FormatDesignation formatDesignation = new FormatDesignation();

    public Format() {
    }

    protected void setFormatName(String name) {
        formatDesignation.formatName = name;
    }

    protected void setFormatVersion(String version) {
        formatDesignation.formatVersion = version;
    }

    private static class FormatDesignation implements Serializable {
        private static final long serialVersionUID = 7607219400252768650L;
        @XmlElement
        private String formatName = "";

        @XmlElement
        private String formatVersion = "";

        public FormatDesignation() {
        }
    }
}

