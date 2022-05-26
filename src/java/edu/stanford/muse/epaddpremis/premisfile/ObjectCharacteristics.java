package edu.stanford.muse.epaddpremis.premisfile;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class ObjectCharacteristics {

    @XmlTransient
    private static final Logger log = LogManager.getLogger(ObjectCharacteristics.class);

    @XmlElement
    @XmlJavaTypeAdapter(CompositionLevelAdapter.class)
    protected CompositionLevel compositionLevel = CompositionLevel.NOT_INITIALISED;

    @XmlElement(name="fixity")
    private final List<Fixity> fixities = new ArrayList<>();

    @XmlElement
    private long size = 0;

    @XmlElement
    private Format format;

    @XmlElement
    private final CreatingApplication creatingApplication = new CreatingApplication();

    ObjectCharacteristics(long size, String fileName) {
        this.size = size;
        this.format = new Format();

        String hash = "";
        try {
            hash = hash(new File(fileName), "MD5");
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("Exception calculating hash for " + fileName + "\n" + e);

            e.printStackTrace();
        }
        fixities.add(new Fixity(hash));

    }

    void setExternalDigest(String digestValue, String digestTool, String digestAlgorithm) {
        if (fixities.size() > 1)
        {
            fixities.remove(1);
        }
        fixities.add(new Fixity(digestValue, digestTool, digestAlgorithm));
    }

    public ObjectCharacteristics() {
    }

    //From http://www.java2s.com/example/java-api/java/security/messagedigest/update-3-6.html
    private static String hash(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] buff = new byte[10240];
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            while (in.available() > 0) {
                int read = in.read(buff);
                digest.update(buff, 0, read);
            }
            byte[] hash = digest.digest();
            String res = "";
            for (byte b : hash) {
                int c = b & 0xFF;
                if (c < 0x10)
                    res += "0";
                res += Integer.toHexString(c);
            }
            return res;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public enum CompositionLevel {
        UNCHANGED("0 - unchanged"), CHANGED("1 - changed"), NOT_INITIALISED("2 - not initialised");

        private final String compositionLevel;

        CompositionLevel(String compositionLevel) {
            this.compositionLevel = compositionLevel;
        }

        public static CompositionLevel fromString(String text) {
            if ("0".equals(text)) {
                text = "0 - unchanged";
            } else if ("1".equals(text)) {
                text = "1 - changed";
            }
            for (CompositionLevel c : CompositionLevel.values()) {
                if (c.compositionLevel.equalsIgnoreCase(text)) {
                    return c;
                }
            }
            return NOT_INITIALISED;
        }

        public String toString() {
            if (this == NOT_INITIALISED)
            {
                return "Unknown";
            }
            // Only print the number, not the text.
            if ("".equals(compositionLevel)) {
                return compositionLevel;
            } else {
                return compositionLevel.substring(0, 1);
            }
        }
    }

    public static class CompositionLevelAdapter extends XmlAdapter<String, CompositionLevel> {
        public String marshal(CompositionLevel compositionLevel) {
            return compositionLevel.toString();
        }

        public CompositionLevel unmarshal(String val) {
            return CompositionLevel.fromString(val);
        }
    }

    protected void setFormatName(String name) {
        format.setFormatName(name);
    }

    protected void setFormatVersion(String version) {
        format.setFormatVersion(version);
    }


    public void setCreatingApplicationName(String creatingApplicationName) {
        this.creatingApplication.creatingApplicationName = creatingApplicationName;
    }

    public void setCreatingApplicationVersion(String creatingApplicationVersion) {
        this.creatingApplication.creatingApplicationVersion = creatingApplicationVersion;
    }

    public void setDateCreatedByApplication(String dateCreatedByApplication) {
        this.creatingApplication.dateCreatedByApplication = dateCreatedByApplication;
    }

    static class CreatingApplication {
        @XmlElement
        private String creatingApplicationName = "";

        @XmlElement
        private String creatingApplicationVersion = "";

        @XmlElement
        private String dateCreatedByApplication = "";

        private CreatingApplication() {
        }
    }
}



