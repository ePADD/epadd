package edu.stanford.muse.epaddpremis;

import edu.stanford.muse.epaddpremis.premisfile.File;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;

public class Relationship implements Serializable {
    private static final long serialVersionUID = -7113269669004449012L;
    @XmlElement
    private final RelationshipType relationshipType = new RelationshipType();
    @XmlElement
    private final File.RelationshipSubType relationshipSubType = new File.RelationshipSubType();
    @XmlElement
    private final File.RelatedObjectIdentifier relatedObjectIdentifier = new File.RelatedObjectIdentifier();

    @XmlElement
    @XmlJavaTypeAdapter(RelatedEnvironmentCharacteristicAdapter.class)
    public RelatedEnvironmentCharacteristic relatedEnvironmentCharacteristic = RelatedEnvironmentCharacteristic.NOT_DEFINED;


//
//    @XmlElement
//    @XmlJavaTypeAdapter(EnvironmentPurposeAdapter.class)
//    public RelatedEnvironmentPurpose relatedEnvironmentPurpose = RelatedEnvironmentPurpose.NOT_DEFINED;

    public Relationship() {
    }


    public enum RelatedEnvironmentCharacteristic {
        UNSPECIFIED("unspecified"), KNOWN_TO_WORK("known to work"), MINIMUM("minimum"), RECOMMENDED("recommended"), NOT_DEFINED("");

        private final String relatedEnvironmentCharacteristic;

        RelatedEnvironmentCharacteristic(String relatedEnvironmentCharacteristic) {
            this.relatedEnvironmentCharacteristic = relatedEnvironmentCharacteristic;
        }

        public static RelatedEnvironmentCharacteristic fromString(String text) {
            for (RelatedEnvironmentCharacteristic e : RelatedEnvironmentCharacteristic.values()) {
                if (e.relatedEnvironmentCharacteristic.equalsIgnoreCase(text)) {
                    return e;
                }
            }
            return NOT_DEFINED;
        }

        public String toString() {
            return relatedEnvironmentCharacteristic;
        }
    }

    public enum RelatedEnvironmentPurpose {
        RENDER("render"), EDIT("edit"), PRINT("print"), SEARCH("search"), NOT_DEFINED("");

        private final String relatedEnvironmentPurpose;

        RelatedEnvironmentPurpose(String relatedEnvironmentPurpose) {
            this.relatedEnvironmentPurpose = relatedEnvironmentPurpose;
        }

        public static RelatedEnvironmentPurpose fromString(String text) {
            for (RelatedEnvironmentPurpose e : RelatedEnvironmentPurpose.values()) {
                if (e.relatedEnvironmentPurpose.equalsIgnoreCase(text)) {
                    return e;
                }
            }
            return NOT_DEFINED;
        }

        public String toString() {
            return relatedEnvironmentPurpose;
        }
    }

    private static class RelatedEnvironmentCharacteristicAdapter extends XmlAdapter<String, RelatedEnvironmentCharacteristic> implements Serializable {
        private static final long serialVersionUID = 831784790626624283L;

        public String marshal(RelatedEnvironmentCharacteristic relatedEnvironmentCharacteristic) {
            return relatedEnvironmentCharacteristic.toString();
        }

        public RelatedEnvironmentCharacteristic unmarshal(String val) {
            return RelatedEnvironmentCharacteristic.fromString(val);
        }
    }

    private static class EnvironmentPurposeAdapter extends XmlAdapter<String, RelatedEnvironmentPurpose> implements Serializable {
        private static final long serialVersionUID = -2490535493909151150L;

        public String marshal(RelatedEnvironmentPurpose relatedEnvironmentPurpose) {
            return relatedEnvironmentPurpose.toString();
        }

        public RelatedEnvironmentPurpose unmarshal(String val) {
            return RelatedEnvironmentPurpose.fromString(val);
        }
    }

    private static class RelationshipType implements Serializable {
        private static final long serialVersionUID = 6292322345630987275L;
        @XmlAttribute
        private final String authority = "relationshipType";
        @XmlAttribute
        private final String authorityURI = "http://id.loc.gov/vocabulary/preservation/relationshipType";
        @XmlAttribute
        private final String valueURI = "http://id.loc.gov/vocabulary/preservation/relationshipType/str";
        @XmlValue
        private final String value = "structural";

        private RelationshipType() {
        }
    }

    private static class EnvironmentExtension implements Serializable {
        private static final long serialVersionUID = 737364250730594970L;
        @XmlElement
        public String environmentNote = "";

        @XmlElement
        public String environmentPurpose = "";

        @XmlElement
        public String softwareName = "";

        @XmlElement
        public String softwareVersion = "";
    }

}
