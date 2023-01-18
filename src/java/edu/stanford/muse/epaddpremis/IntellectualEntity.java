package edu.stanford.muse.epaddpremis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.*;

public class IntellectualEntity extends IntellectualEntityBaseClass implements Serializable {
    @XmlTransient
    private static final Logger log = LogManager.getLogger(IntellectualEntity.class);

//    @XmlAttribute(name = "xsi:type")
//    private final String type = "premis:intellectualEntity";
    @XmlElement(name = "objectIdentifier")
    private final PremisIdentityObjectIdentifier premisIdentityObjectIdentifier = new PremisIdentityObjectIdentifier();
    @XmlElement
    private final PreservationLevel preservationLevel = new PreservationLevel();
    @XmlElement(name = "significantProperties")
    // Just using the significantPropertiesMap and an adaptor for marshalling and unmarshalling the XML would be
    // better but I run into problems with JAXB.
    // significantPropertiesMap is keeping track of the significant properties. The set significantPropertiesSet is
    // used for marshalling and unmarshalling. significantPropertiesSet gets updated by using the map just before
    // printing (marshalling)the XML file.
    // When reading the XML file significantPropertiesSet is filled automatically in the process of unmarshalling.
    // significantPropertiesMap is then created by using the data in significantPropertiesSet.
    private Set<SignificantProperties> significantPropertiesSet;
    @XmlElement(name = "environmentExtension")
    private final EnvironmentExtensionIntellectualEntity environmentExtensionIntellectualEntity = new EnvironmentExtensionIntellectualEntity();
    @XmlElement
    private final Relationship relationship = new Relationship();


    @XmlTransient
    private final Map<String, SignificantProperties> significantPropertiesMap = new HashMap<>();

    public IntellectualEntity() {
    }

    protected void updateSignificantPropertiesSet() {
        significantPropertiesSet = new HashSet<>();
        for (Map.Entry<String, SignificantProperties> entry : significantPropertiesMap.entrySet()) {
            significantPropertiesSet.add(entry.getValue());
        }
    }

    protected void initialiseSignificantPropertiesMapFromSet() {
        for (SignificantProperties s : significantPropertiesSet) {
            significantPropertiesMap.put(s.getType(), s);
        }
    }
    protected void setPreservationLevelDateAssignedToToday()
    {
        this.preservationLevel.setPreservationLevelDateAssignedToToday();
    }

    protected void setRelatedEnvironmentCharacteristic(String characteristic) {
        this.relationship.relatedEnvironmentCharacteristic = Relationship.RelatedEnvironmentCharacteristic.fromString(characteristic);
    }

    protected void setEnvironmentPurpose(String purpose) {
        this.environmentExtensionIntellectualEntity.environmentPurpose = purpose;
    }

    protected void setEnvironmentNote(String note) {
        this.environmentExtensionIntellectualEntity.environmentNote = note;
    }

    protected void setEnvironmentSoftwareName(String softwareName) {
        this.environmentExtensionIntellectualEntity.softwareName = softwareName;
    }

    protected void setEnvironmentSoftwareVersion(String version) {
        this.environmentExtensionIntellectualEntity.softwareVersion = version;
    }

    public void setPreservationLevelRole(String preservationLevelRole) {
        preservationLevel.setRole(preservationLevelRole);
    }

    public void setPreservationLevelRationale(String preservationLevelRationale) {
        preservationLevel.setRationale(preservationLevelRationale);
    }

    protected void setSignificantProperty(String type, int value) {
        significantPropertiesMap.put(type, new SignificantProperties(type, value));
    }

    protected void addToSignificantProperty(String type, int value) {
        if (significantPropertiesMap.get(type) != null) {
            significantPropertiesMap.get(type).addValue(value);
        } else {
            significantPropertiesMap.put(type, new SignificantProperties(type, value));
        }
    }

    private static class EnvironmentExtensionIntellectualEntity implements Serializable {
        @XmlElement
        public String environmentNote = "";

        @XmlElement
        public String environmentPurpose = "";
        @XmlElement
        public String softwareName = "";
        @XmlElement
        public String softwareVersion = "";
        @XmlTransient
        private final Map<String, SignificantProperties> mimeCountMap = new HashMap<>();
    }

    private static class PremisIdentityObjectIdentifier implements Serializable {
        @XmlElement(name = "objectIdentifierType")
        private final String objectIdentifierType = "local";

        @XmlElement(name = "objectIdentifierValue")
        private String objectIdentifierValue = "";

        private PremisIdentityObjectIdentifier(String objectIdentifierValue) {
            this.objectIdentifierValue = objectIdentifierValue;
        }

        private PremisIdentityObjectIdentifier() {
        }
    }
}