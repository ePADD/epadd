package edu.stanford.muse.epaddpremis.premisfile;

import edu.stanford.epadd.Version;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

public class Fixity {

    @XmlElement
    private MessageDigestAlgorithm messageDigestAlgorithm;
    @XmlElement
    private String messageDigest;

    @XmlElement
    private String messageDigestOriginator;


    Fixity(String messageDigest) {
        // In the constructor the message digest from ePADD (set automatically during import) is set.
        // message digest from external tool (can be set via GUI) can be set later via setter
        this.messageDigestOriginator = "ePADD " + Version.version;
        this.messageDigestAlgorithm = new MessageDigestAlgorithm("MD5");
        this.messageDigest = messageDigest;
    }

    public Fixity() {
    }

    public Fixity(String messageDigest, String digestTool, String digestAlgorithm) {
        this.messageDigest = messageDigest;
        this.messageDigestOriginator = digestTool;
        this.messageDigestAlgorithm = new MessageDigestAlgorithm(digestAlgorithm);
    }

    static class MessageDigestAlgorithm {

        @XmlAttribute
        private final String authority = "cryptographicHashFunctions";
        @XmlAttribute
        private final String authorityURI = "http://id.loc.gov/vocabulary/preservation/cryptographicHashFunctions";
        @XmlAttribute
        private final String valueURI = "http://id.loc.gov/vocabulary/preservation/cryptographicHashFunctions/md5";
        @XmlValue
        private String algorithm;

        MessageDigestAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        private MessageDigestAlgorithm() {
        }
    }
}
