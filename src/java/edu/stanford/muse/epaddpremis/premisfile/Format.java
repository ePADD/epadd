package edu.stanford.muse.epaddpremis.premisfile;

import javax.xml.bind.annotation.XmlElement;

public class Format {

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

    private static class FormatDesignation {
        @XmlElement
        private String formatName = "";

        @XmlElement
        private String formatVersion = "";

        public FormatDesignation() {
        }
    }
}

