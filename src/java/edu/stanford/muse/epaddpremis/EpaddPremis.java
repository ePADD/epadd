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
/*
	2022-10-19	Added JSON export
	2022-11-01	Moved export2JSON() into printToFiles(), overwrite JSON file, update checksum for JSON file
	2022-11-03	Added createEvent(EpaddEvent.EventType eventType, String eventDetailInformation, String outcome, ZonedDateTime date1)
*/

import edu.stanford.epadd.Version;
import edu.stanford.muse.epaddpremis.premisfile.File;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.ModeConfig;
import gov.loc.repository.bagit.domain.Bag;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "premis")
public class EpaddPremis implements Serializable {

    @XmlTransient
    public static final String XML_FILE_NAME = "epaddPremis.xml";

    @XmlTransient
    public static final String SERIALIZED_FILE_NAME = "premisobject.ser";

    @XmlTransient
    public static final String JSON_FILE_NAME = "epaddPremis.json";

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


    @XmlElement(name = "object")
    private IntellectualEntityBaseClass intellectualEntity = new IntellectualEntity();


    @XmlElement(name = "object")
    private final List<ObjectBaseClass> file = new ArrayList<>();

    @XmlElement(name = "event")
    private final List<EpaddEvent> epaddEvents = new ArrayList<>();
    @XmlElement(name = "agent")
    private Agent agent;
    @XmlElement(name = "rights")
    PremisRights premisRights = new PremisRights();

    static final long serialVersionUID = 3827719114666370610l;


    public EpaddPremis(String baseDir, String dataFolder, Archive archive) throws JAXBException, IOException {
        this.baseDir = baseDir;
        this.pathToDataFolder = baseDir + java.io.File.separatorChar + Archive.BAG_DATA_FOLDER;
        this.archive = archive;
        this.intellectualEntity = new IntellectualEntity();
        this.getIntellectualEntity().setPreservationLevelDateAssignedToToday();
        setAgent(archive);
    }

    public void savePremisObject() {
        try {
            FileOutputStream f = new FileOutputStream(new java.io.File(getSerializedPathAndFileName()));
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(this);
            o.close();
            f.close();
        } catch (Exception e) {
            log.warn("Exception saving EpaddPremis object. " + e);
        }
    }

    public static EpaddPremis readPremisObject(Archive archive, String baseDir, String bagDataFolder) {
        EpaddPremis epaddPremis = null;
        try {
            FileInputStream fi = new FileInputStream(new java.io.File(baseDir + java.io.File.separatorChar + bagDataFolder + java.io.File.separatorChar + SERIALIZED_FILE_NAME));
            ObjectInputStream oi = new ObjectInputStream(fi);
            epaddPremis = (EpaddPremis) oi.readObject();
            epaddPremis.baseDir = baseDir;
            epaddPremis.pathToDataFolder = baseDir + java.io.File.separatorChar + Archive.BAG_DATA_FOLDER;
            oi.close();
            fi.close();
        } catch (Exception e) {
            System.out.println("Exception reading EpaddPremis object. " + e);
            log.warn("Exception reading EpaddPremis object. " + e);

            try {
                epaddPremis = new EpaddPremis(baseDir, Archive.BAG_DATA_FOLDER, archive);
                String details = "There were no Premis files. Maybe this archive was created before the introduction of recording Premis data.";
                epaddPremis.createEvent(EpaddEvent.EventType.PREMIS_FILES_CREATED, details, "success");
            } catch (JAXBException | IOException e2 ) {
                Util.print_exception("Exception creating new EpaddPremis object", e2, LogManager.getLogger(EpaddPremis.class));
            }

        }
        return epaddPremis;
    }

    //When adding an accessing into a non-empty collection we have to merge the two Premis files.
    //addPremisObjects merges one Premis file into another Premis file.
    public boolean addPremisObject(EpaddPremis epaddPremisToAdd)
    {
        this.epaddEvents.addAll(epaddPremisToAdd.getEvents());
        this.file.addAll(epaddPremisToAdd.getFileObjects());
        for (SignificantProperties eachProperty : epaddPremisToAdd.getIntellectualEntity().getSignificantPropertiesSet())
        {
            this.addToSignificantProperty(eachProperty.getType(), eachProperty.getValue());
        }
        return true;
    }

    private PremisRights getPremisRights() {
        return premisRights;
    }

    private List<EpaddEvent> getEvents()
    {
        return this.epaddEvents;
    }

    private IntellectualEntity getIntellectualEntity() {
        return ((IntellectualEntity) intellectualEntity);
    }

    public EpaddPremis() {
    }

    public void setFileID(String fileID, String folderName) {
        for (ObjectBaseClass bc : file) {
            File fo = (File) bc;
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

    //NOT IN USE
    public static EpaddPremis createFromFile(String baseDir, String bagDataFolder, Archive archive) {

        String xsdFile = "C:\\Users\\jochen\\dropbox_stuff\\epadd\\premis_schema.txt";
        EpaddPremis epaddPremis = null;
        Path file = Paths.get(baseDir + java.io.File.separatorChar + bagDataFolder + java.io.File.separatorChar + XML_FILE_NAME);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(EpaddPremis.class, ObjectBaseClass.class, File.class, IntellectualEntity.class, IntellectualEntityBaseClass.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            InputStream inputStream = new FileInputStream(file.toFile());

            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema employeeSchema = sf.newSchema(new java.io.File(xsdFile));
            unmarshaller.setSchema(employeeSchema);
            epaddPremis = (EpaddPremis) unmarshaller.unmarshal(inputStream);
            epaddPremis.archive = archive;
            epaddPremis.baseDir = baseDir;
            epaddPremis.pathToDataFolder = baseDir + java.io.File.separatorChar + bagDataFolder;
            inputStream.close();
        } catch (Exception e) {
            Util.print_exception("Exception reading Premis XML file", e, LogManager.getLogger(EpaddPremis.class));
            System.out.println("Exception reading Premis XML file " + e);
        }
        if (epaddPremis != null && epaddPremis.getIntellectualEntity() != null) {
            epaddPremis.getIntellectualEntity().initialiseSignificantPropertiesMapFromSet();
        }
        return epaddPremis;
    }

    public void setAgent(Archive archive) {

        agent = new Agent("ePADD", "ePADD", "software", Version.version);
    }

    public void createFileObject(String fileName, Long size) {
        file.add(new File(fileName, size));
    }

    private List<ObjectBaseClass> getFileObjects()
    {
        return file;
    }

    public void createEvent(EpaddEvent.EventType eventType, String eventDetailInformation, String outcome) {
        epaddEvents.add(new EpaddEvent(eventType, eventDetailInformation, outcome, ModeConfig.getModeForDisplay()));
        printToFiles();

    }
// 2022-11-03
    public void createEvent(EpaddEvent.EventType eventType, String eventDetailInformation, String outcome, ZonedDateTime date1) {
        epaddEvents.add(new EpaddEvent(eventType, eventDetailInformation, outcome, ModeConfig.getModeForDisplay(), date1));
        printToFiles();
    }

    public void addToSignificantProperty(String type, int value) {
        if (intellectualEntity != null) {
            getIntellectualEntity().addToSignificantProperty(type, value);
        } else {
            log.warn("no premisIntellectualEntityObject in addSignificantProperty");
        }
    }

    private int getSignificantPropertyValue(String type)
    {
        return archive.getEpaddPremis().getIntellectualEntity().getSignificantProperty(type);
    }

    public void setIntellectualEntityObjectEnvironmentCharacteristics(String characteristics) {
        this.getIntellectualEntity().setRelatedEnvironmentCharacteristic(characteristics);
    }

    public void setIntellectualEntityObjectEnvironmentPurpose(String purpose) {
        this.getIntellectualEntity().setEnvironmentPurpose(purpose);
    }

    public void setIntellectualEntityObjectEnvironmentNote(String note) {
        this.getIntellectualEntity().setEnvironmentNote(note);
    }

    public void setIntellectualEntityObjectEnvironmentSoftwareName(String softwareName) {
        this.getIntellectualEntity().setEnvironmentSoftwareName(softwareName);
    }

    public void setIntellectualEntityObjectEnvironmentSoftwareVersion(String version) {
        this.getIntellectualEntity().setEnvironmentSoftwareVersion(version);
    }

    public void setSignificantProperty(String type, int value) {
        if (intellectualEntity != null) {
            getIntellectualEntity().setSignificantProperty(type, value);
        } else {
            log.warn("no premisIntellectualEntityObject in addSignificantProperty");
        }
    }

    public void createEvent(EpaddEvent.EventType eventType, String eventDetailInformation, String outcome, String linkingObjectIdentifierType, String linkingObjectIdentifierValue) {
        epaddEvents.add(new EpaddEvent(eventType, eventDetailInformation, outcome, linkingObjectIdentifierType, linkingObjectIdentifierValue, ModeConfig.getModeForDisplay()));
        printToFiles();

    }

    public void createEvent(JSONObject eventJsonObject) {
        epaddEvents.add(new EpaddEvent(eventJsonObject, ModeConfig.getModeForDisplay()));
        printToFiles();


    }

    private String getXmlPathAndFileName() {
        return pathToDataFolder + java.io.File.separatorChar + XML_FILE_NAME;
    }

    private String getSerializedPathAndFileName() {
        return pathToDataFolder + java.io.File.separatorChar + SERIALIZED_FILE_NAME;
    }

// 2022-11-01
    private String getJsonPathAndFileName() {
        return pathToDataFolder + java.io.File.separatorChar + JSON_FILE_NAME;
    }

// 2022-10-19
    public void export2JSON() {
            InputStream inputStream = null;
            BufferedWriter bufferedWriter = null;
            try {
                java.io.File xmlfile = new java.io.File (pathToDataFolder + java.io.File.separatorChar + XML_FILE_NAME);
                inputStream = new FileInputStream(xmlfile);  
                StringBuilder builder =  new StringBuilder();  
                int ptr = 0;  
                while ((ptr = inputStream.read()) != -1 ) {  
                    builder.append((char) ptr); 
                }  
                String xml  = builder.toString();  
                JSONObject jsonObj = XML.toJSONObject(xml);   
            // Assume default encoding.
// 2022-11-01  			
//                FileWriter fileWriter =  new FileWriter(pathToDataFolder + java.io.File.separatorChar + JSON_FILE_NAME);
				FileWriter fileWriter =  new FileWriter(getJsonPathAndFileName(), false);
                bufferedWriter = new BufferedWriter(fileWriter);

                for(int i= 0 ;i < jsonObj.toString().split(",").length; i ++) {
                    bufferedWriter.write(jsonObj.toString().split(",")[i]);
                    bufferedWriter.write("\n");
                }
                		
            } catch (IOException | JSONException e) {
                Util.print_exception("Exception in EpaddPremis.export2JSON() ", e, LogManager.getLogger(EpaddPremis.class)); 
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                    if (bufferedWriter != null) bufferedWriter.close();	
                } catch (IOException e) {}   
            }
    }	

    public void printToFiles() {
        getIntellectualEntity().updateSignificantPropertiesSet();

        //Serialization for internal use
        savePremisObject();

        //Print XML file for users
        try {
            JAXBContext jc = JAXBContext.newInstance(EpaddPremis.class, IntellectualEntity.class, File.class, ObjectBaseClass.class, IntellectualEntityBaseClass.class, EventDetailInformation.class);
            StringWriter writer = new StringWriter();
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(this, new java.io.File(getXmlPathAndFileName()));
// 2022-11-01            
            export2JSON();
			
            Bag bag = Archive.readArchiveBag(baseDir);
            if (bag == null) {
                log.warn("bag null in EpaddPremis.printToFile()");
                return;
            }
            if (archive == null) {
                log.warn("Archive null in EpaddPremis.printToFile()");
                return;
            }
            Archive.updateFileInBag(bag, getXmlPathAndFileName(), baseDir);
// 2022-11-01            
            Archive.updateFileInBag(bag, getJsonPathAndFileName(), baseDir);    
            archive.setArchiveBag(bag);
        } catch (Exception e) {
            Util.print_exception("Exception in EpaddPremis.printToFile() ", e, LogManager.getLogger(EpaddPremis.class));
            System.out.println("Exception in EpaddPremis.printToFile() " + e);
        }
    }

    public void setPreservationLevelRationale(String preservationLevelRationale) {
        this.getIntellectualEntity().setPreservationLevelRationale(preservationLevelRationale);
    }

    public void setPreservationLevelRole(String preservationLevelRole) {
        this.getIntellectualEntity().setPreservationLevelRole(preservationLevelRole);
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
        for (ObjectBaseClass bc : file) {
            File fo = (File) bc;
            // Would be better to use a map <String, PremisFileObject> for storing the file objects.
            // Two files could have the same folder name (but not two files in the same import session).
            // If a fileID already exists for this file then we probably have
            // a file with the same name which has already been imported.
            if (fileID.equals(fo.getFileID())) {
                fo.setMetadata(fmetadata);
                break;
            }
        }
        archive.printEpaddPremis();
    }
}

