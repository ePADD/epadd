package edu.stanford.muse.epaddpremis.premisfile;

import edu.stanford.muse.email.FolderInfo;
import edu.stanford.muse.epaddpremis.ObjectBaseClass;
import edu.stanford.muse.epaddpremis.PreservationLevel;
import edu.stanford.muse.index.Archive;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class File extends ObjectBaseClass implements Serializable {

    @XmlTransient
    private static final Logger log = LogManager.getLogger(File.class);
    @XmlElement(name = "objectIdentifier")
    private List<ObjectIdentifier> objectIdentifiers = new ArrayList<>();
    @XmlElement
    private PreservationLevel preservationLevel = new PreservationLevel();


    @XmlElement(name = "significantProperties")
    //A map matching property types with SignificantProperties objects would be better but I run into problems with JAXB.
    private Set<SignificantPropertiesFile> significantPropertiesSet = SignificantPropertiesFile.getInitialSet();
    @XmlElement
        private ObjectCharacteristics objectCharacteristics;

    
    @XmlElement(name = "relationship")
    private final RelationshipFile relationshipFile = new RelationshipFile();




    public File(String fileName, long size) {
        fileName = FolderInfo.removeTmpPartOfPath(fileName);
        this.objectIdentifiers.add(new ObjectIdentifier("file name", fileName + "#"));
        this.objectCharacteristics = new ObjectCharacteristics(size, fileName);
    }

    public File() {
    }
    void setSignificantProperty(String type, String value) {
        if (getProperty(SignificantPropertiesFile.PropertyType.fromString(type)) != null) {
            getProperty(SignificantPropertiesFile.PropertyType.fromString(type)).setValue(value);
        }
    }

    private SignificantPropertiesFile getProperty(SignificantPropertiesFile.PropertyType type) {
        for (SignificantPropertiesFile significantProperties : significantPropertiesSet) {
            if (significantProperties.getType().equals(type)) {
                return significantProperties;
            }
        }
        return null;
    }

    protected void setEnvironmentCharacteristics(String characteristics) {
        this.relationshipFile.relatedEnvironmentCharacteristic = RelationshipFile.RelatedEnvironmentCharacteristic.fromString(characteristics);
    }

    protected void setEnvironmentPurpose(String purpose) {
        this.relationshipFile.relatedEnvironmentPurpose = purpose;
    }

    protected void setRelatedEnvironmentPurpose(String purpose) {
        this.relationshipFile.setEnvironmentPurpose(purpose);
    }

    protected void setEnvironmentNote(String note) {
        this.setSignificantProperty("environmentNote", note);
    }

    protected void setEnvironmentSoftwareName(String softwareName) {
        this.setSignificantProperty("softwareName", softwareName);
    }

    protected void setEnvironmentSoftwareVersion(String version) {
        this.setSignificantProperty("softwareVersion", version);
    }

    public String getFirstObjectIdentifierValue() {
        return objectIdentifiers.get(0).objectIdentifierValue;
    }

    public void setCompositionLevel(String level) {
        ObjectCharacteristics.CompositionLevel compositionLevel = ObjectCharacteristics.CompositionLevel.fromString(level);
        this.objectCharacteristics.compositionLevel = compositionLevel;
    }

    public void setMetadata(Archive.FileMetadata fmetadata) {
        //fmetadata.notes = request.getParameter("fileNotes");

        if (fmetadata.preservationLevelRole != null) setPreservationLevelRole(fmetadata.preservationLevelRole);
        if (fmetadata.preservationLevelRationale != null)
            setPreservationLevelRationale(fmetadata.preservationLevelRationale);
        if (fmetadata.preservationLevelDateAssigned != null)
            setPreservationLevelDateAssigned(fmetadata.preservationLevelDateAssigned);
        if (fmetadata.compositionLevel != null) setCompositionLevel(fmetadata.compositionLevel);
        setExternalDigest(fmetadata);
        if (fmetadata.formatName != null) setFormatName(fmetadata.formatName);
        if (fmetadata.formatVersion != null) setFormatVersion(fmetadata.formatVersion);
        if (fmetadata.creatingApplicationName != null) setCreatingApplicationName(fmetadata.creatingApplicationName);
        if (fmetadata.creatingApplicationVersion != null)
            setCreatingApplicationVersion(fmetadata.creatingApplicationVersion);
        if (fmetadata.dateCreatedByApplication != null) setDateCreatedByApplication(fmetadata.dateCreatedByApplication);
        if (fmetadata.environmentCharacteristic != null)
            setEnvironmentCharacteristics(fmetadata.environmentCharacteristic);
        if (fmetadata.relatedEnvironmentPurpose != null)
            setRelatedEnvironmentPurpose(fmetadata.relatedEnvironmentPurpose);
        if (fmetadata.relatedEnvironmentNote != null) setEnvironmentNote(fmetadata.relatedEnvironmentNote);
        if (fmetadata.softwareName != null) setEnvironmentSoftwareName(fmetadata.softwareName);
        if (fmetadata.softwareVersion != null) setEnvironmentSoftwareVersion(fmetadata.softwareVersion);
    }

    private void setExternalDigest(Archive.FileMetadata fmetadata) {
        objectCharacteristics.setExternalDigest(fmetadata.messageDigest, fmetadata.messageDigestOrginator, fmetadata.messageDigestAlgorithm);
    }

    private void setFormatName(String name) {
        objectCharacteristics.setFormatName(name);
    }

    private void setFormatVersion(String version) {
        objectCharacteristics.setFormatVersion(version);
    }

    public void setPreservationLevelRole(String role) {
        preservationLevel.setRole(role);
    }

    public void setPreservationLevelRationale(String rationale) {
        preservationLevel.setRationale(rationale);
    }

    public void setPreservationLevelDateAssigned(String date) {
        preservationLevel.setPreservationLevelDateAssigned(date);
    }

    @XmlTransient
    public String getFileID() {
        if (objectIdentifiers.size() > 1) {
            return objectIdentifiers.get(1).objectIdentifierValue;
        } else {
            return "";
        }
    }

    public void setFileID(String fileID) {
        if (objectIdentifiers.size() == 1) {
            this.objectIdentifiers.add(1, new ObjectIdentifier("fileID", fileID));
        } else if (objectIdentifiers.size() == 2) {
            log.warn("Setting fileID to " + fileID + " for file which already has fileID "
                    + this.objectIdentifiers.get(1).objectIdentifierValue);
        }
    }

    public String getFolderName() {
        String folderName = getFirstObjectIdentifierValue();
        if (folderName != null && !folderName.isEmpty()) {
            folderName = folderName.substring(0, folderName.length() - 1);
        }
        return folderName;
    }

    public void setCreatingApplicationName(String creatingApplicationName) {
        this.objectCharacteristics.setCreatingApplicationName(creatingApplicationName);
    }

    public void setCreatingApplicationVersion(String creatingApplicationVersion) {
        this.objectCharacteristics.setCreatingApplicationVersion(creatingApplicationVersion);
    }

    public void setDateCreatedByApplication(String dateCreatedByApplication) {
        this.objectCharacteristics.setDateCreatedByApplication(dateCreatedByApplication);
    }


    private static class ObjectIdentifier implements Serializable {
        @XmlElement(name = "objectIdentifierType")
        private String objectIdentifierType = "";

        @XmlElement(name = "objectIdentifierValue")
        private String objectIdentifierValue = "";

        private ObjectIdentifier(String type, String value) {
            objectIdentifierValue = value;
            objectIdentifierType = type;
        }

        public ObjectIdentifier() {
        }
    }

    public static class RelationshipSubType implements Serializable  {
        @XmlAttribute
        private final String authority = "relationshipSubType";
        @XmlAttribute
        private final String authorityURI = "http://id.loc.gov/vocabulary/preservation/relationshipType";
        @XmlAttribute
        private final String valueURI = "http://id.loc.gov/vocabulary/preservation/relationshipSubType/isp";
        @XmlValue
        private final String value = "is part of";

        public RelationshipSubType() {
        }
    }

    public static class RelatedObjectIdentifier implements Serializable {
        @XmlElement
        private final String relatedObjectIdentifierType = "local";
        @XmlElement
        private final String relatedObjectIdentifierValue = "";

        public RelatedObjectIdentifier() {
        }
    }
}
