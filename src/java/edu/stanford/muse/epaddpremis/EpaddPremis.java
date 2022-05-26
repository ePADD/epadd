package edu.stanford.muse.epaddpremis;//Example: ingest
//        Record the MBOX files which were ingested (file level events: MBOX files).
//        This file was ingested locally, here is the fixity check, it was a success
//

//For most events we are just capturing that something has happened in the system, not worrying about the file level operations.
//PREMIS	significant properties 	Significant properties value	Description of the characteristics of a particular object
//        subjectively determined to be important to maintain through preservation actions. Examples include: folder path
//        names, number of folders, number of messages, number of attachments  	number of messages (THERE MAY NEED TO BE
//        MORE ITERATIONS OF THIS FIELD?)
//
//        package edu.stanford.muse.epaddpremis;

import edu.stanford.epadd.Version;
import edu.stanford.muse.epaddpremis.premisfile.PremisFileObject;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.ModeConfig;
import gov.loc.repository.bagit.domain.Bag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "premis")
public class EpaddPremis {

    @XmlTransient
    public static final String FILE_NAME = "epaddPremis.xml";

    @XmlTransient
    private static final Logger log = LogManager.getLogger(EpaddPremis.class);

    @XmlAttribute(name = "xsi:schemaLocation")
    private final String string = "http://www.loc.gov/premis/v3 https://www.loc.gov/standards/premis/premis.xsd";

    @XmlAttribute
    private final String version = "3.0";

    @XmlTransient
    private String baseDir;

    @XmlTransient
    private String pathToDataFolder;

    @XmlTransient
    private Archive archive;


    @XmlElement(name="object")
    private PremisIntellectualEntityObject premisIntellectualEntityObject;


    @XmlElement(name="Object")
    private final List<PremisFileObject> fileObjects = new ArrayList<>();

    @XmlElement(name = "event")
    private final List<EpaddEvent> epaddEvents = new ArrayList<>();
    @XmlElement(name = "agent")
    private Agent agent;
    @XmlElement(name = "rights")
    PremisRights premisRights = new PremisRights();




    public EpaddPremis(String baseDir, String dataFolder, Archive archive) throws JAXBException, IOException {
        this.baseDir = baseDir;
        this.pathToDataFolder = baseDir + File.separatorChar + dataFolder;
        this.archive = archive;
        this.premisIntellectualEntityObject = new PremisIntellectualEntityObject();
        this.premisIntellectualEntityObject.setPreservationLevelDateAssignedToToday();
        setAgent(archive);
    }

    public EpaddPremis() {
    }

    public void setFileID(String fileID, String folderName) {
        for (PremisFileObject fo : fileObjects) {
            // Would be better to use a map <String, PremisFileObject> for storing the file objects.
            // Two files could have the same folder name (but not two files in the same import session).
            // If a fileID already exists for this file then we probably have
            // a file with the same name which has already been imported.
            if ("".equals(fo.getFileID()) && folderName != null && folderName.equals(fo.getFolderName())) {
                fo.setFileID(fileID);
                break;
            }
        }
    }

    private PremisFileObject getFileFromID(String fileID)
    {
        for (PremisFileObject fo : fileObjects) {
            // Would be better to use a map <String, PremisFileObject> for storing the file objects.
            if (fileID != null && fileID.equals(fo.getFileID())) {
                return fo;
            }
        }
        return null;
    }

    public static EpaddPremis createFromFile(String baseDir, String bagDataFolder, Archive archive) {

        String xsdFile = "C:\\Users\\jochen\\dropbox_stuff\\epadd\\premis_schema.txt";
        EpaddPremis epaddPremis = null;
        Path file = Paths.get(baseDir + File.separatorChar + bagDataFolder + File.separatorChar + FILE_NAME);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(EpaddPremis.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            InputStream inputStream = new FileInputStream(file.toFile());

            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema employeeSchema = sf.newSchema(new File(xsdFile));
           //unmarshaller.setSchema(employeeSchema);
            epaddPremis = (EpaddPremis) unmarshaller.unmarshal(inputStream);
            epaddPremis.archive = archive;
            epaddPremis.baseDir = baseDir;
            epaddPremis.pathToDataFolder = baseDir + File.separatorChar + bagDataFolder;
            inputStream.close();
        } catch (Exception e) {
            Util.print_exception("Exception reading Premis XML file", e, LogManager.getLogger(EpaddPremis.class));
            System.out.println("Exception reading Premis XML file " + e);
        }
        if (epaddPremis != null && epaddPremis.premisIntellectualEntityObject != null) {
            epaddPremis.premisIntellectualEntityObject.initialiseSignificantPropertiesMapFromSet();
        }
        return epaddPremis;
    }

    public void setAgent(Archive archive) {

        agent = new Agent("ePADD", "ePADD", "software", Version.version);
    }

    public void createFileObject(String fileName, Long size) {
        fileObjects.add(new PremisFileObject(fileName, size));
    }

    public void createEvent(EpaddEvent.EventType eventType, String eventDetailInformation, String outcome) {
        epaddEvents.add(new EpaddEvent(eventType, eventDetailInformation, outcome, ModeConfig.getModeForDisplay(ArchiveReaderWriter.getArchiveIDForArchive(archive))));
    }

    public void addToSignificantProperty(String type, int value) {
        if (premisIntellectualEntityObject != null) {
            premisIntellectualEntityObject.addToSignificantProperty(type, value);
        } else {
            log.warn("no premisIntellectualEntityObject in addSignificantProperty");
        }
    }

    public void setIntellectualEntityObjectEnvironmentCharacteristics(String characteristics) {
        this.premisIntellectualEntityObject.setRelatedEnvironmentCharacteristic(characteristics);
    }

    public void setIntellectualEntityObjectEnvironmentPurpose(String purpose) {
        this.premisIntellectualEntityObject.setEnvironmentPurpose(purpose);
    }

    public void setIntellectualEntityObjectEnvironmentNote(String note) {
        this.premisIntellectualEntityObject.setEnvironmentNote(note);
    }

    public void setIntellectualEntityObjectEnvironmentSoftwareName(String softwareName) {
        this.premisIntellectualEntityObject.setEnvironmentSoftwareName(softwareName);
    }

    public void setIntellectualEntityObjectEnvironmentSoftwareVersion(String version) {
        this.premisIntellectualEntityObject.setEnvironmentSoftwareVersion(version);
    }

    public void setSignificantProperty(String type, int value) {
        if (premisIntellectualEntityObject != null) {
            premisIntellectualEntityObject.setSignificantProperty(type, value);
        } else {
            log.warn("no premisIntellectualEntityObject in addSignificantProperty");
        }
    }

    public void createEvent(EpaddEvent.EventType eventType, String eventDetailInformation, String outcome, String linkingObjectIdentifierType, String linkingObjectIdentifierValue) {
        epaddEvents.add(new EpaddEvent(eventType, eventDetailInformation, outcome, linkingObjectIdentifierType, linkingObjectIdentifierValue, ModeConfig.getModeForDisplay(ArchiveReaderWriter.getArchiveIDForArchive(archive))));
    }

    public void createEvent(JSONObject eventJsonObject) {
        epaddEvents.add(new EpaddEvent(eventJsonObject, ModeConfig.getModeForDisplay(ArchiveReaderWriter.getArchiveIDForArchive(archive))));
    }

    private String getPathAndFileName() {
        return pathToDataFolder + File.separatorChar + FILE_NAME;
    }

    public void printToFile() {
        premisIntellectualEntityObject.updateSignificantPropertiesSet();
        try {
            JAXBContext context = JAXBContext.newInstance(EpaddPremis.class);
            Marshaller mar = context.createMarshaller();
            mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
           // mar.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,  "C:\\Users\\jochen\\dropbox_stuff\\epadd\\premis_schema.txt");

            mar.marshal(this, new File(getPathAndFileName()));
            Bag bag = Archive.readArchiveBag(baseDir);
            if (bag == null) {
                log.warn("bag null in EpaddPremis.printToFile()");
                return;
            }
            if (archive == null) {
                log.warn("Archive null in EpaddPremis.printToFile()");
                return;
            }
            Archive.updateFileInBag(bag, getPathAndFileName(), baseDir);
            archive.setArchiveBag(bag);
        } catch (Exception e) {
            Util.print_exception("Exception in EpaddPremis.printToFile() ", e, LogManager.getLogger(EpaddPremis.class));
            System.out.println("Exception in EpaddPremis.printToFile() " + e);
        }
    }

    public void setPreservationLevelRationale(String preservationLevelRationale) {
        this.premisIntellectualEntityObject.setPreservationLevelRationale(preservationLevelRationale);
    }

    public void setPreservationLevelRole(String preservationLevelRole) {
        this.premisIntellectualEntityObject.setPreservationLevelRole(preservationLevelRole);
    }

    public void setRightsStatementIdentifierType(String type) {
        this.premisRights.setRightsStatementIdentifierType(type);
    }

    public void setRightsStatementIdentifierValue(String value) {
        this.premisRights.setRightsStatementIdentifierValue(value);
    }

    public void setStatutestatuteJurisdiction(String value) {
        premisRights.getRightsExtension().getStatuteInformation().setStatuteJurisdiction(value);
    }

    public void setStatuteDocumentationIdentifierType(String value) {
        premisRights.getStatuteInformation().setStatuteDocumentationIdentifierType(value);
    }

    public void setStatuteDocumentationIdentifierValue(String value) {
        premisRights.getStatuteInformation().setStatuteDocumentationIdentifierValue(value);
    }

    public void setStatuteDocumentationRole(String value) {
        premisRights.getStatuteInformation().setStatuteDocumentationRole(value);
    }



    public void setFileMetadata(String fileID, Archive.FileMetadata fmetadata) {
        for (PremisFileObject bc : fileObjects) {
            // Would be better to use a map <String, PremisFileObject> for storing the file objects.
            // Two files could have the same folder name (but not two files in the same import session).
            // If a fileID already exists for this file then we probably have
            // a file with the same name which has already been imported.
            if (fileID.equals(bc.getFileID())) {
                bc.setMetadata(fmetadata);
                break;
            }
        }
        archive.printEpaddPremis();
    }
}

