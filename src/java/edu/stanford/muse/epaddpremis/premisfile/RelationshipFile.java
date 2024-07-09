package edu.stanford.muse.epaddpremis.premisfile;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;

public class RelationshipFile implements Serializable {

    private static final long serialVersionUID = -9032939648518691082L;
    @XmlElement(name = "relationshipType")
    private final RelationshipTypeFile relationshipTypeFile = new RelationshipTypeFile();
    @XmlElement(name = "relationshipSubType")
    private final File.RelationshipSubType relationshipSubTypeFile = new File.RelationshipSubType();
    @XmlElement(name = "relatedObjectIdentifier")
    private final File.RelatedObjectIdentifier relatedObjectIdentifierFile = new File.RelatedObjectIdentifier();

    @XmlElement
    public String relatedEnvironmentPurpose = "";

    @XmlElement
    @XmlJavaTypeAdapter(RelationshipFile.RelatedEnvironmentCharacteristicAdapter.class)
    public RelationshipFile.RelatedEnvironmentCharacteristic relatedEnvironmentCharacteristic = RelationshipFile.RelatedEnvironmentCharacteristic.NOT_DEFINED;



    public RelationshipFile() {
    }

    public void setEnvironmentPurpose(String purpose) {
        this.relatedEnvironmentPurpose = purpose;
    }



    public enum RelatedEnvironmentCharacteristic {
        UNSPECIFIED("unspecified"), KNOWN_TO_WORK("known to work"), MINIMUM("minimum"), RECOMMENDED("recommended"), NOT_DEFINED("");

        private final String relatedEnvironmentCharacteristic;

        RelatedEnvironmentCharacteristic(String relatedEnvironmentCharacteristic) {
            this.relatedEnvironmentCharacteristic = relatedEnvironmentCharacteristic;
        }

        public static RelationshipFile.RelatedEnvironmentCharacteristic fromString(String text) {
            for (RelationshipFile.RelatedEnvironmentCharacteristic e : RelationshipFile.RelatedEnvironmentCharacteristic.values()) {
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

        public static RelationshipFile.RelatedEnvironmentPurpose fromString(String text) {
            for (RelationshipFile.RelatedEnvironmentPurpose e : RelationshipFile.RelatedEnvironmentPurpose.values()) {
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

    private static class RelatedEnvironmentCharacteristicAdapter extends XmlAdapter<String, RelationshipFile.RelatedEnvironmentCharacteristic> implements Serializable {
        private static final long serialVersionUID = -582696380954814381L;

        public String marshal(RelationshipFile.RelatedEnvironmentCharacteristic relatedEnvironmentCharacteristic) {
            return relatedEnvironmentCharacteristic.toString();
        }

        public RelationshipFile.RelatedEnvironmentCharacteristic unmarshal(String val) {
            return RelationshipFile.RelatedEnvironmentCharacteristic.fromString(val);
        }
    }

    private static class EnvironmentPurposeAdapter extends XmlAdapter<String, RelationshipFile.RelatedEnvironmentPurpose> implements Serializable {
        private static final long serialVersionUID = 6192505859687268504L;

        public String marshal(RelationshipFile.RelatedEnvironmentPurpose relatedEnvironmentPurpose) {
            return relatedEnvironmentPurpose.toString();
        }

        public RelationshipFile.RelatedEnvironmentPurpose unmarshal(String val) {
            return RelationshipFile.RelatedEnvironmentPurpose.fromString(val);
        }
    }

    private static class RelationshipTypeFile implements Serializable {
        private static final long serialVersionUID = -349697360010365664L;
        @XmlAttribute
        private final String authority = "relationshipType";
        @XmlAttribute
        private final String authorityURI = "http://id.loc.gov/vocabulary/preservation/relationshipType";
        @XmlAttribute
        private final String valueURI = "http://id.loc.gov/vocabulary/preservation/relationshipType/str";
        @XmlValue
        private final String value = "structural";

        private RelationshipTypeFile() {
        }
    }
 }
