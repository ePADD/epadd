/*
	2022-09-13	fixed bug in readArchiveIfPresent
	2022-10-13	added SHA256 checksum
*/
package edu.stanford.muse.index;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.AddressBookManager.CorrespondentAuthorityMapper;
import edu.stanford.muse.AnnotationManager.AnnotationManager;
import edu.stanford.muse.Config;
import edu.stanford.muse.LabelManager.LabelManager;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.email.MuseEmailFetcher;
import edu.stanford.muse.epaddpremis.EpaddPremis;
import edu.stanford.muse.ie.variants.EntityBookManager;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import edu.stanford.muse.webapp.ModeConfig;
import edu.stanford.muse.webapp.SimpleSessions;
import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import org.apache.commons.io.FileUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.LockObtainFailedException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ArchiveReaderWriter{

    public static final String SESSION_SUFFIX = ".archive.v2"; // all session files end with .session
    private static final Logger log	= LogManager.getLogger(SimpleSessions.class);
    //private static String SESSIONS_DIR = null;
    private static   String MUSE_DIRNAME = ".muse"; // clients might choose to override this

    public static   String CACHE_BASE_DIR = null; // overruled (but not overwritten) by session's attribute "cacheDir"
    public static   String CACHE_DIR = null; // overruled (but not overwritten) by session's attribute "cacheDir"

    static {
        MUSE_DIRNAME = SimpleSessions.getVarOrDefault("muse.defaultArchivesDir", System.getProperty("user.home") + File.separator + ".muse");
        CACHE_BASE_DIR = SimpleSessions.getVarOrDefault("muse.dir.cache_base", MUSE_DIRNAME);
        //modified the CACHE_DIR in v6 to hava path as user/data
        CACHE_DIR      = SimpleSessions.getVarOrDefault("muse.dir.cache"  , ArchiveReaderWriter.CACHE_BASE_DIR + File.separator + "user"); // warning/todo: this "-D" not universally honored yet, e.g., user_key and fixedCacheDir in MuseEmailFetcher.java
        //SESSIONS_DIR   = getVarOrDefault("muse.dir.sessions", CACHE_DIR + File.separator + Archive.SESSIONS_SUBDIR); // warning/todo: this "-D" not universally honored yet, e.g., it is hard-coded again in saveSession() (maybe saveSession should actually use getSessinoDir() rather than basing it on cacheDir)
    }

    //#############################################Start: Weak reference cache for the archive object and archive ID################################
    // an archive in a given dir should be loaded only once into memory.
    // this map stores the directory -> archive mapping.
    private static final LinkedHashMap<String, WeakReference<Archive>> globaldirToArchiveMap = new LinkedHashMap<>();
    private static final LinkedHashMap<String,Archive> globalArchiveIDToArchiveMap = new LinkedHashMap<>();
    private static final LinkedHashMap<Archive,String> globalArchiveToArchiveIDMap = new LinkedHashMap<>();

    //#############################################End: Weak reference cache for the archive object and archive#####################################

    //#############################################Start: Reading/loading an archive bag###########################################################
         /**
         * loads session from the given filename, and returns the map of loaded
         * attributes.
         * if readOnly is false, caller MUST make sure to call packIndex.
         * baseDir is Indexer's baseDir (path before "indexes/")
         *
         * @throws IOException
         * @throws LockObtainFailedException
         * @throws CorruptIndexException
         * Change as on Nov 2017-
         * Earlier the whole archive was serialized and deserialized as one big entity. Now it is broken into
         * four main parts, Addressbook, entitybook, correspondentAuthorityMapper and the rest of the object
         * We save all these four components separately in saveArchive. Therefore while reading, we need to read
         * all those separately from appropriate files.
         */
    private static Map<String, Object> loadSessionAsMap(String filename, String baseDir, boolean readOnly,ModeConfig.Mode mode) throws IOException
    {
        log.info("Loading session from file " + filename + " size: " + Util.commatize(new File(filename).length() / 1024) + " KB");

        ObjectInputStream ois = null;
        long startTime = System.currentTimeMillis();

        // keep reading till eof exception
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            ois = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(filename))));

            while (true)
            {
                String key = (String) ois.readObject();
                log.info("loading key: " + key);
                try {
                    Object value = ois.readObject();
                    if (value == null)
                        break;
                    result.put(key, value);
                } catch (InvalidClassException ice)
                {
                    log.error("Bad version for value of key " + key + ": " + ice + "\nContinuing but this key is not set...");
                } catch (ClassNotFoundException cnfe)
                {
                    log.error("Class not found for value of key " + key + ": " + cnfe + "\nContinuing but this key is not set...");
                }
            }
        } catch (EOFException eof) {
            log.info("end of session file reached");
        } catch (Exception e) {
            log.warn("Warning unable to load session: " + Util.stackTrace(e));
            result.clear();
        }

        if (ois != null)
            try {
                ois.close();
            } catch (Exception e) {
                Util.print_exception(e, log);
            }

            log.info("Session loaded successfully");
        // need to set up sentiments explicitly -- now no need since lexicon is part of the session
        log.info("Memory status: " + Util.getMemoryStats());

        Archive archive = (Archive) result.get("archive");
        // no groups in public mode
        if (archive != null)
        {
            long deserializationTime = System.currentTimeMillis();
            log.info("Time taken to read and deserialize archive object: " + (deserializationTime-startTime) + " milliseconds");

            /*
                Read other three modules of Archive object which were set as transient and hence did not serialize.
                */
            //file path names of addressbook, entitybook and correspondentAuthorityMapper data.
            String dir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;

            String addressBookPath = dir + File.separatorChar + Archive.ADDRESSBOOK_SUFFIX;
            String entityBookPath = dir + File.separatorChar + Archive.ENTITYBOOKMANAGER_SUFFIX;
            String cAuthorityPath =  dir + File.separatorChar + Archive.CAUTHORITYMAPPER_SUFFIX;
            String labMapDirPath= dir + File.separatorChar + Archive.LABELMAPDIR;
            String annotationMapPath = dir + File.separatorChar + Archive.ANNOTATION_SUFFIX;
            String blobNormalizationMapPath = dir + File.separatorChar + Archive.BLOBLNORMALIZATIONFILE_SUFFIX;

            //Error handling: For the case when epadd is running first time on an archive that was not split it is possible that
            //above three files are not present. In that case start afresh with importing the email-archive again in processing mode.
            if(!(new File(addressBookPath).exists()) /*|| !(new File(entityBookPath).exists())*/ || !(new File(cAuthorityPath).exists())){
                result.put("archive", null);
                return result;
            }

            log.info("Setting up post-deserialization action");
            archive.postDeserialized(baseDir, readOnly);
            long postDeserializationDuration = System.currentTimeMillis();
            log.info("Post-deserialization action completed in "+ (postDeserializationDuration - deserializationTime) + " milliseconds");
            ///////////////Processing metadata////////////////////////////////////////////////
            //Read collection metadata first because some of the collection's information might be used while loading other modules. Like first date and last date of an archive
            //is used when doc's dates are found corrupted.
            // override the PM inside the archive with the one in the PM file
            //update: since v5 no pm will be inside the archive.
            // this is useful when we import a legacy archive into processing, where we've updated the pm file directly, without updating the archive.
            log.info("Loading collection metadata");
            try {
                archive.collectionMetadata = readCollectionMetadata(baseDir);
            } catch (Exception e) {
                Util.print_exception ("Error trying to read processing metadata file", e, log);
            }
            long collectionMetadataDuration = System.currentTimeMillis();
            if(archive.collectionMetadata!=null) {
                log.info("Collection metadata loaded successfully in " + (collectionMetadataDuration - postDeserializationDuration) + " milliseconds");
            }
            /////////////////AddressBook////////////////////////////////////////////
            log.info("Loading address book");
            archive.addressBook = readAddressBook(addressBookPath,archive.getAllDocs());
            long addressBookLoading = System.currentTimeMillis();
            log.info("Addressbook loaded successfully in "+(addressBookLoading - collectionMetadataDuration)+" milliseconds");

            ////////////////EntityBook/////////////////////////////////////
            log.info("Loading EntityBook Manager");

            EntityBookManager eb = readEntityBookManager(archive,entityBookPath);
            long entityBookLoading = System.currentTimeMillis();
            archive.setEntityBookManager(eb);
            log.info("EntityBook Manager loaded successfully in "+(entityBookLoading-addressBookLoading) + " milliseconds");
            ///////////////CorrespondentAuthorityMapper/////////////////////////////
            long correspondentAuthorityLoading, labelManagerLoading, annotationManagerLoading, blobLoading;
            if(mode!= ModeConfig.Mode.DISCOVERY) {
                CorrespondentAuthorityMapper cmapper = null;
                log.info("Loading Correspondent authority mapper");

                cmapper = CorrespondentAuthorityMapper.readObjectFromStream(cAuthorityPath);
                correspondentAuthorityLoading = System.currentTimeMillis();
                log.info("Correspondent authority mapper loaded successfully in "+(correspondentAuthorityLoading-entityBookLoading) + " milliseconds");
                archive.correspondentAuthorityMapper = cmapper;
            }else{
                correspondentAuthorityLoading = entityBookLoading;
            }
            /////////////////Label Mapper/////////////////////////////////////////////////////
            if(mode!= ModeConfig.Mode.DISCOVERY) {
                log.info("Loading Label Manager");

                LabelManager labelManager = readLabelManager(ArchiveReaderWriter.getArchiveIDForArchive(archive),labMapDirPath);
                archive.setLabelManager(labelManager);
                labelManagerLoading = System.currentTimeMillis();
                log.info("Label Manager loaded successfully in "+(labelManagerLoading-correspondentAuthorityLoading) + " milliseconds");
            }else{
                labelManagerLoading = correspondentAuthorityLoading;
            }
            ///////////////Annotation Manager///////////////////////////////////////////////////////
            if(mode!= ModeConfig.Mode.DISCOVERY) {

                log.info("Loading Annotation Manager");
                AnnotationManager annotationManager = AnnotationManager.readObjectFromStream(annotationMapPath);
                archive.setAnnotationManager(annotationManager);
                annotationManagerLoading = System.currentTimeMillis();
                log.info("Annotation Manager loaded successfully in "+(annotationManagerLoading-labelManagerLoading));
            }else{
                annotationManagerLoading = labelManagerLoading;
            }

            /////////////////////Blob Normalization map (IF exists)//////////////////////////////////////////////////////
            if(new File(blobNormalizationMapPath).exists()) {
                log.info("Computing blob normalization map (An artifact of AMatica tool)");
                archive.getBlobStore().setNormalizationMap(blobNormalizationMapPath);
                blobLoading = System.currentTimeMillis();
                log.info("Blob normalization map computed successfully in "+(blobLoading-annotationManagerLoading) + " milliseconds");
            }else{
                blobLoading = annotationManagerLoading;
            }
            /////////////////////////////Done reading//////////////////////////////////////////////////////
            // most of this code should probably move inside Archive, maybe a function called "postDeserialized()"
            result.put("emailDocs", archive.getAllDocs());
            log.info("Assigning thread IDs");
            archive.assignThreadIds();
            log.info("Thread IDs assigned successfully");
            log.info("Total time spent in archive loading is "+(System.currentTimeMillis()-startTime)+" milliseconds");
        }

        return result;
    }


    /*
    This method reads the lable-info.json file and fills the data structure of the label manager. Note that in future more and more system labels
    might get added to ePADD. However, to make those labels visible for the collections which were ingested using earlier version of ePADD we need to
    do some trick here. Once labels get loaded the system should check if all the system labels are present in label-info.json file or not. If not then
    that label should be added in LabelManager so that the label becomes visible to the archivist. First time we encountered this situation when we
    added a label 'Transfer-only-to-delivery' to transfer a message from processing to delivery mode but not to discovery mode (a restriction on 'do-not-transfer'
    label).
     */
    private static LabelManager readLabelManager(String archiveID, String labMapDirPath) {
        LabelManager labelManager = null;
        try {
            labelManager = LabelManager.readObjectFromStream(archiveID,labMapDirPath);
        } catch (Exception e) {
            Util.print_exception ("Exception in reading label manager from archive, assigning a new label manager", e, log);
            labelManager = new LabelManager(archiveID);
        }
        //Now fill in the system labels which are absent in labelManager object (read from the file).
        labelManager.syncWithSystemLabels();
        return labelManager;

    }

    public static EntityBookManager readEntityBookManager(Archive archive, String entityBookPath) {

        return EntityBookManager.readObjectFromFiles(archive,entityBookPath);
    }

    public static AddressBook readAddressBook(String addressBookPath,Collection<Document> alldocs) {

        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(addressBookPath));
            AddressBook ab = AddressBook.readObjectFromStream(br,alldocs);
            br.close();
            return ab;
        } catch (FileNotFoundException e) {

            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }

    /**
     * reads name.processing.metadata from the given basedir. should be used
     * when quick archive metadata is needed without loading the actual archive
     */
    public static Archive.CollectionMetadata readCollectionMetadata(String baseDir) {
        String processingFilename = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR + File.separatorChar + Config.COLLECTION_METADATA_FILE;
        try (Reader reader = new FileReader(processingFilename)) {
            Archive.CollectionMetadata metadata = new Gson().fromJson(reader, Archive.CollectionMetadata.class);
            //Using timestamp to crate first and last date of the archive using locale specific format.
            //Don't assume that firstDateTS and lastDateTS will be non zero (as they should be). If not proper then don't fill in firstDate and lastDate
           if(metadata.firstDateTS!=0)
                metadata.firstDate = new Date(metadata.firstDateTS);
            if(metadata.lastDateTS!=0)
                metadata.lastDate = new Date(metadata.lastDateTS);
            return metadata;
        } catch (Exception e) {
            Util.print_exception("Unable to read processing metadata from file" + processingFilename, e, log);
        }
        return null;
    }

    //#######################################End: Loading/reading an archive bag#####################################################################

    //#######################################Start: Saving the archive (flat directory or bag) depending upon the mode argument#############################
    //incremental save is used when a module (label,annotation etc) are saved from inside a loaded archive bag. We need to update the bag metadata
    //to reflect these changes otherwise the checksum calculation fails##############################################################################

    /** saves the archive in the current session to the cachedir */
    public static boolean saveArchive(Archive archive, Archive.Save_Archive_Mode mode) throws IOException
    {
        assert archive!=null : new AssertionError("No archive to save.");
        // String baseDir = (String) session.getAttribute("cacheDir");
        return saveArchive(archive.baseDir, "default", archive,mode);
    }

    /** saves the archive in the current session to the cachedir. note: no blobs saved. */
    /* mode attributes select if this archive was already part of a bag or is a first time creation. Based on this flag the ouptput directory
    changes. In case of incremental bag update, the files will be in basedir/data/ subfolder whereas in case of fresh creation the files will be in
    basedir.
     */
    public static boolean saveArchive(String baseDir, String name, Archive archive, Archive.Save_Archive_Mode mode) throws IOException
    {
        /*log.info("Before saving the archive checking if it is still in good shape");
        archive.Verify();*/
        String dir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;
        new File(dir).mkdirs(); // just to be safe
        String filename = dir + File.separatorChar + name + SESSION_SUFFIX;
        log.info("Saving archive to (session) file " + filename);
        /*//file path names of addressbook, entitybook and correspondentAuthorityMapper data.
        String addressBookPath = dir + File.separatorChar + Archive.ADDRESSBOOK_SUFFIX;
        String entityBookPath = dir + File.separatorChar + Archive.ENTITYBOOK_SUFFIX;
        String cAuthorityPath =  dir + File.separatorChar + Archive.CAUTHORITYMAPPER_SUFFIX;
        */
        recalculateCollectionMetadata(archive);



        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filename))))) {
            oos.writeObject("archive");
            oos.writeObject(archive);
        } catch (Exception e1) {
            Util.print_exception("Failed to write archive: ", e1, log);
        }

        if(mode== Archive.Save_Archive_Mode.INCREMENTAL_UPDATE){
            //archive object doesn't get modified so no point in saving it for incremental one.
            //archive.updateFileInBag(filename,archive.baseDir);
        }
        //Now write modular transient fields to separate files-
        //By Dec 2017 there are three transient fields which will be saved and loaded separately
        //1. AddressBook -- Stored in a gzip file with name in the same `	directory as of archive.
        //2. EntityBook
        //3. CorrespondentAuthorityMapper
        //Before final release of v5 in Feb 2018, modularize annotation out of archive.
        /////////////////AddressBook Writing -- In human readable form ///////////////////////////////////
        if(mode!= Archive.Save_Archive_Mode.INCREMENTAL_UPDATE)
            saveAddressBook(archive,mode); //no need to save addressbook while saving an archive in incrremental mode because address book is saved after every explicit modification.
        ////////////////EntityBook Writing -- In human readable form/////////////////////////////////////
        saveEntityBookManager(archive,mode);
        ///////////////CAuthorityMapper Writing-- Serialized///////////////////////////////
        saveCorrespondentAuthorityMapper(archive,mode);
        //////////////LabelManager Writing -- Serialized//////////////////////////////////
        saveLabelManager(archive,mode);

        //////////////AnnotationManager writing-- In human readable form/////////////////////////////////////
        saveAnnotations(archive,mode);
        saveCollectionMetadata(archive,mode);

/*        //if normalizationInfo is present save that too..
        if(archive.getBlobStore().getNormalizationMap()!=null){
            saveNormalizationMap(archive,mode);
        }*/
//if archivesave mode is freshcreation then create a bag around basedir and set bag as this one..
        if(mode== Archive.Save_Archive_Mode.FRESH_CREATION){
// 2022-10-13            
//            StandardSupportedAlgorithms algorithm = StandardSupportedAlgorithms.MD5;
            StandardSupportedAlgorithms algorithm[] = { StandardSupportedAlgorithms.MD5, StandardSupportedAlgorithms.SHA256};
            boolean includeHiddenFiles = false;
            try {
                archive.close();


                //First copy the content of archive.baseDir + "/data" to archive.baseDir and then create an in place bag.
                //Why so complicated? Because we wanted to have a uniform directory structure of archive irrespective of the fact whether it is
                //in a bag or not. That structure is 'archive.baseDir + "/data" folder'
                File baseDirFile = Paths.get(archive.baseDir).toFile();
                File baseDirOneUpFile = baseDirFile.getParentFile();

                //29.05.2024 JF
                //Create the tmp folder in the current location rather than the default location (something like /appdata/local/temp).
                //This is in case the current location is on a different drive because there is not enough space in the home directory.
                File tmp = Util.createTempDirectory(baseDirOneUpFile);
                tmp.delete();



                //It seems that if indexer kept the file handles open then move directory failed on windows because of the lock held on those file
                //therefore call archive.close() before moving stuff around
                //archive.close();
                FileUtils.moveDirectory(Paths.get(archive.baseDir+File.separatorChar+Archive.BAG_DATA_FOLDER).toFile(),tmp.toPath().toFile());
                //Files.copy(Paths.get(userDir+File.separatorChar+Archive.BAG_DATA_FOLDER),tmp.toPath(),StandardCopyOption.REPLACE_EXISTING);
                File wheretocopy = Paths.get(archive.baseDir).toFile();
                Util.deleteDir(wheretocopy.getPath(),log);

                FileUtils.moveDirectory(tmp.toPath().toFile(),wheretocopy);

                Bag bag = BagCreator.bagInPlace(Paths.get(archive.baseDir), Arrays.asList(algorithm), includeHiddenFiles);
                archive.openForRead();
                archive.setArchiveBag(bag);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        }else {
            archive.close();

            // re-open for reading
            archive.openForRead();

        }
        return true;
    }


    /*
    From the save button on top nav-bar we should trigger only incremental save of mutable data like addressbook, labelmanager, etc.
    A smarter way will be to save only those parts which changed. This will require some flag to track unchanged data.-- @TODO
     */
    public static void saveMutable_Incremental(Archive archive){
        saveMutable_Incremental(archive, null);
    }


        public static void saveMutable_Incremental(Archive archive, String fileID){

        saveAddressBook(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
        ////////////////EntityBook Writing -- In human readable form/////////////////////////////////////
        saveEntityBookManager(archive,Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
        ///////////////CAuthorityMapper Writing-- Serialized///////////////////////////////
        saveCorrespondentAuthorityMapper(archive,Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
        //////////////LabelManager Writing -- Serialized//////////////////////////////////
        saveLabelManager(archive,Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);

        //////////////AnnotationManager writing-- In human readable form/////////////////////////////////////
        saveAnnotations(archive,Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
        //should we recalculate collection metadata as well??
        recalculateCollectionMetadata(archive);
        saveCollectionMetadata(archive,Archive.Save_Archive_Mode.INCREMENTAL_UPDATE, fileID);

    }

    public static void recalculateCollectionMetadata(Archive archive) {
        if (archive.collectionMetadata == null)
            archive.collectionMetadata = new Archive.CollectionMetadata();

        archive.collectionMetadata.timestamp = new Date().getTime();
        archive.collectionMetadata.tz = TimeZone.getDefault().getID();
        archive.collectionMetadata.nDocs = archive.getAllDocs().size();
        archive.collectionMetadata.nUniqueBlobs = archive.blobStore.uniqueBlobs.size();

        int totalAttachments = 0, images = 0, docs = 0, others = 0, sentMessages = 0, receivedMessages = 0, hackyDates = 0;
        Date firstDate = null, lastDate = null;

        for (Document d: archive.getAllDocs()) {
            if (!(d instanceof EmailDocument))
                continue;
            EmailDocument ed = (EmailDocument) d;
            if (ed.date != null) {
                if (ed.hackyDate)
                    hackyDates++;
                else {
                    if (firstDate == null || ed.date.before(firstDate))
                        firstDate = ed.date;
                    if (lastDate == null || ed.date.after(lastDate))
                        lastDate = ed.date;
                }
            }
            int sentOrReceived = ed.sentOrReceived(archive.addressBook);
            if ((sentOrReceived & EmailDocument.SENT_MASK) != 0)
                sentMessages++;
            if ((sentOrReceived & EmailDocument.RECEIVED_MASK) != 0)
                receivedMessages++;

            if (!Util.nullOrEmpty(ed.attachments))
            {
                totalAttachments += ed.attachments.size();
                for (Blob b: ed.attachments)
                    if (!Util.nullOrEmpty(archive.getBlobStore().get_URL_Normalized(b)))
                    {
                        if (Util.is_image_filename(archive.getBlobStore().get_URL_Normalized(b)))
                            images++;
                        else if (Util.is_doc_filename(archive.getBlobStore().get_URL_Normalized(b)))
                            docs++;
                        else
                            others++;
                    }
            }
        }

        archive.collectionMetadata.firstDate = firstDate;
        archive.collectionMetadata.lastDate = lastDate;
        //Fill in the current locale and unix timestamp values for firstDate and lastDate. For more information on these variables refer to the
        //class definition of CollectionMetaData.
        archive.collectionMetadata.setIngestionLocaleTag(Locale.getDefault().toLanguageTag());
        if(firstDate!=null)
            archive.collectionMetadata.firstDateTS = firstDate.getTime();
        if(lastDate!=null)
            archive.collectionMetadata.lastDateTS = lastDate.getTime();

        archive.collectionMetadata.nIncomingMessages = receivedMessages;
        archive.collectionMetadata.nOutgoingMessages = archive.getAddressBook().getMsgsSentByOwner();
        archive.collectionMetadata.nHackyDates = hackyDates;

        archive.collectionMetadata.nBlobs = totalAttachments;
        archive.collectionMetadata.nUniqueBlobs = archive.blobStore.uniqueBlobs.size();
        archive.collectionMetadata.nImageBlobs = images;
        archive.collectionMetadata.nDocBlobs = docs;
        archive.collectionMetadata.nOtherBlobs = others;
    }

    /*public static void saveNormalizationMap(Archive archive, Archive.Save_Archive_Mode mode){
        String baseDir = archive.baseDir;
        String dir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;
        new File(dir).mkdirs(); // just to be safe
        //file path name of normalization file.
        String normalizationPath = dir + File.separatorChar + Archive.BLOBLNORMALIZATIONFILE_SUFFIX;
        archive.getBlobStore().writeNormalizationMap(normalizationPath);
        //if this was an incremental update , we need to update the bag's metadata as well..
        if(mode== Archive.Save_Archive_Mode.INCREMENTAL_UPDATE)
            archive.updateFileInBag(normalizationPath,baseDir);

    }
*/

   public static void saveAddressBook(Archive archive, Archive.Save_Archive_Mode mode){

        String baseDir = archive.baseDir;
        String dir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;
        new File(dir).mkdirs(); // just to be safe
        //file path name of addressbook data.
        String addressBookPath = dir + File.separatorChar + Archive.ADDRESSBOOK_SUFFIX;
        log.info("Saving addressBook to file " + addressBookPath);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(addressBookPath));
            archive.addressBook.writeObjectToStream (bw, false, false);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //if this was an incremental update in addressbook, we need to update the bag's metadata as well..
        if(mode== Archive.Save_Archive_Mode.INCREMENTAL_UPDATE)
            archive.updateFileInBag(addressBookPath,baseDir);

        //update the summary of addressbook (counts etc used on correspondent listing page).
        //UPDATE: No need to do that as it gets updated as soon as user changes the addressbook and clicks on save.
        //Archive.cacheManager.cacheCorrespondentListing(ArchiveReaderWriter.getArchiveIDForArchive(archive));

    }
    /*
     * updated Address Book by overwriting by an updated file image, usually stored in Archive TEMP_DIR.
     * The updated file must be in well format.
     */
    public static void updateAddressBook(Archive archive, String updatedFileImage){

        //Now copy the file 'filename' inside session
        String sessiondir = archive.baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.SESSIONS_SUBDIR + File.separator;

        try {
            Util.copy_file(updatedFileImage, sessiondir + File.separator + Archive.ADDRESSBOOK_SUFFIX);

            //update bag metadata
            archive.updateFileInBag(sessiondir + File.separator + Archive.ADDRESSBOOK_SUFFIX, archive.baseDir);

            //read addressbook again
            AddressBook ab = ArchiveReaderWriter.readAddressBook(sessiondir + File.separator + Archive.ADDRESSBOOK_SUFFIX, archive.getAllDocs());

            if (ab != null) {
                //set as current archive's addressbook
                archive.setAddressBook(ab);
                //incremental saving of addressbook
                ArchiveReaderWriter.saveAddressBook(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
            } else {
                throw new Exception("Invalid addressbook file used");
            }
        } catch (Exception e){
            log.error("error in updating address book");
        }
    }

    public static void saveEntityBookManager(Archive archive, Archive.Save_Archive_Mode mode){

        for(NEType.Type t: NEType.Type.values()) {
            saveEntityBookManager(archive, mode, t.getCode());
        }



    }

    public static void saveEntityBookManager(Archive archive, Archive.Save_Archive_Mode mode,Short entityType){
        String baseDir = archive.baseDir;
        String dir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR + File.separator + Archive.ENTITYBOOKMANAGER_SUFFIX;
        log.info("Saving entity book of type "+entityType+" to file ");

        archive.getEntityBookManager().writeObjectToFile(dir,entityType);

        String filepath = dir + File.separator + NEType.getTypeForCode(entityType).getDisplayName()+ File.separator + Archive.ENTITYBOOK_SUFFIX;
        //if this was an incremental update in entitybook, we need to update the bag's metadata as well..
        if(mode== Archive.Save_Archive_Mode.INCREMENTAL_UPDATE)
            archive.updateFileInBag(filepath,baseDir);


    }

    public static void saveCorrespondentAuthorityMapper(Archive archive, Archive.Save_Archive_Mode mode){
        String baseDir = archive.baseDir;
        String dir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;
        //file path name of entitybook
        String cAuthorityPath = dir + File.separatorChar + Archive.CAUTHORITYMAPPER_SUFFIX;
        new File(cAuthorityPath).mkdir();
        log.info("Saving correspondent Authority mappings to directory" + cAuthorityPath);

        try {
            archive.getCorrespondentAuthorityMapper().writeObjectToStream(cAuthorityPath);
        } catch (ParseException | IOException e) {
            log.warn("Exception while writing correspondent authority files"+e.getMessage());
            e.printStackTrace();
        }

        //if this was an incremental update in authority mapper, we need to update the bag's metadata as well..
        if(mode== Archive.Save_Archive_Mode.INCREMENTAL_UPDATE)
            archive.updateFileInBag(cAuthorityPath,baseDir);


    }

    public static void saveLabelManager(Archive archive, Archive.Save_Archive_Mode mode){
        String baseDir = archive.baseDir;
        String dir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;
        //file path name of labelMap file
        String labMapDir = dir + File.separatorChar + Archive.LABELMAPDIR;
        new File(labMapDir).mkdir();//create dir if not exists.
        log.info("Saving label mapper to directory " + labMapDir);
        //TEMP: create a map of signature to docid and pass it to Labelmanager.write method.. This is done to expand the csv file's signature to include
        //the signature of the document as well (for readability and debuggability)
        Map<String,String> docidToSignature = new LinkedHashMap<>();
        for(Document d: archive.getAllDocsAsSet()){
            EmailDocument ed = (EmailDocument)d;
            docidToSignature.put(ed.getUniqueId(),ed.getSignature());
        }
        archive.getLabelManager().writeObjectToStream(labMapDir,docidToSignature);

        //if this was an incremental update in label manager, we need to update the bag's metadata as well..
        if(mode== Archive.Save_Archive_Mode.INCREMENTAL_UPDATE)
            archive.updateFileInBag(labMapDir,baseDir);

    }

    private static void saveAnnotations(Archive archive, Archive.Save_Archive_Mode mode){
        String baseDir = archive.baseDir;
        String dir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;

        String annotationcsv = dir + File.separatorChar + Archive.ANNOTATION_SUFFIX;
        Map<String,String> docidToSignature = new LinkedHashMap<>();
        for(Document d: archive.getAllDocsAsSet()){
            EmailDocument ed = (EmailDocument)d;
            docidToSignature.put(ed.getUniqueId(),ed.getSignature());
        }
        archive.getAnnotationManager().writeObjectToStream(annotationcsv,docidToSignature);

        //if this was an incremental update in annotation, we need to update the bag's metadata as well..
        if(mode== Archive.Save_Archive_Mode.INCREMENTAL_UPDATE)
            archive.updateFileInBag(annotationcsv,baseDir);


    }



    /**
* writes name.processing.metadata to the given basedir. should be used
* when quick archive metadata is needed without loading the actual archive
* basedir is up to /.../sessions
     * v5- Instead of serializing now this data gets stored in json format.
*/
    public static void saveCollectionMetadata(Archive.CollectionMetadata cm, String basedir){
        saveCollectionMetadata(cm, basedir,null);
    }
        public static void saveCollectionMetadata(Archive.CollectionMetadata cm, String basedir, Archive archive){
        saveCollectionMetadata(cm, basedir,  archive, null);
    }

    public static void saveCollectionMetadata(Archive.CollectionMetadata cm, String basedir, Archive archive, String fileID){
        EpaddPremis epaddPremis = null;
        if (archive != null) {
            epaddPremis = archive.getEpaddPremis();
        }
        if (epaddPremis == null)
        {
            log.warn("epaddPremis null in ArchiveReaderWriter.saveCollectionMetadata()");
        }
        else {
            if (cm.preservationLevelRationale != null)
                epaddPremis.setPreservationLevelRationale(cm.preservationLevelRationale);
            if (cm.preservationLevelRole != null) epaddPremis.setPreservationLevelRole(cm.preservationLevelRole);
            if (cm.rightsStatementIdentifierType != null)
                epaddPremis.setRightsStatementIdentifierType(cm.rightsStatementIdentifierType);
            if (cm.rightsStatementIdentifierValue != null)
                epaddPremis.setRightsStatementIdentifierValue(cm.rightsStatementIdentifierValue);
            if (cm.statuteDocumentationIdentifierType != null)
                epaddPremis.setStatuteDocumentationIdentifierType(cm.statuteDocumentationIdentifierType);
            if (cm.statuteDocumentationIdentifierValue != null)
                epaddPremis.setStatuteDocumentationIdentifierValue(cm.statuteDocumentationIdentifierValue);
            if (cm.statuteDocumentationRole != null) epaddPremis.setStatuteDocumentationRole(cm.statuteDocumentationRole);
            if (cm.environmentCharacteristic != null)
                epaddPremis.setIntellectualEntityObjectEnvironmentCharacteristics(cm.environmentCharacteristic);
            if (cm.environmentNote != null) epaddPremis.setIntellectualEntityObjectEnvironmentNote(cm.environmentNote);
            if (cm.relatedEnvironmentPurpose != null)
                epaddPremis.setIntellectualEntityObjectEnvironmentPurpose(cm.relatedEnvironmentPurpose);
            if (cm.softwareName != null) epaddPremis.setIntellectualEntityObjectEnvironmentSoftwareName(cm.softwareName);
            if (cm.softwareVersion != null)
                epaddPremis.setIntellectualEntityObjectEnvironmentSoftwareVersion(cm.softwareVersion);
            if (cm.statuteJurisdiction != null) epaddPremis.setStatutestatuteJurisdiction(cm.statuteJurisdiction);

            if (fileID != null)
            {
            for (Archive.FileMetadata fm : cm.fileMetadatas) {
                if (fm.fileID != null && fm.fileID.equals(fileID)) {
                    epaddPremis.setFileMetadata(fm.fileID, fm);
                }
            }
            }

            archive.printEpaddPremis();
        }

        String dir = basedir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;
        String processingFilename = dir + File.separatorChar + Config.COLLECTION_METADATA_FILE;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter fwriter = null;
        try {
            fwriter = new FileWriter(processingFilename);
            gson.toJson(cm,fwriter);
        } catch (IOException e) {
            Util.print_exception("Unable to write processing metadata", e, log);
            e.printStackTrace();
        }finally {
            if(fwriter!=null){
                try {
                    fwriter.close();
                } catch (IOException e) {
                    Util.print_exception("Unable to write processing metadata", e, log);
                    e.printStackTrace();
                }
            }
        }

    }

    /*
Following variant is used when saving an archive. Here an extra argument, mode is passed to denote if it is a fresh archive creation (legacy folder structure
at the top) or an update in the file present in the archive bag.
*/
public static void saveCollectionMetadata(Archive archive, Archive.Save_Archive_Mode mode)
{
        saveCollectionMetadata(archive, mode, null);
    }

    public static void saveCollectionMetadata(Archive archive, Archive.Save_Archive_Mode mode, String fileID)
    {

        Archive.CollectionMetadata cm = archive.collectionMetadata;

    EpaddPremis epaddPremis = archive.getEpaddPremis();
    if (epaddPremis == null)
    {
        log.warn("epaddPremis null in ArchiveReaderWriter.saveCollectionMetadata()");
    }
    else {
        if (cm.preservationLevelRationale != null)
            epaddPremis.setPreservationLevelRationale(cm.preservationLevelRationale);
        if (cm.preservationLevelRole != null) epaddPremis.setPreservationLevelRole(cm.preservationLevelRole);
        if (cm.rightsStatementIdentifierType != null)
            epaddPremis.setRightsStatementIdentifierType(cm.rightsStatementIdentifierType);
        if (cm.rightsStatementIdentifierValue != null)
            epaddPremis.setRightsStatementIdentifierValue(cm.rightsStatementIdentifierValue);
        if (cm.statuteDocumentationIdentifierType != null)
            epaddPremis.setStatuteDocumentationIdentifierType(cm.statuteDocumentationIdentifierType);
        if (cm.statuteDocumentationIdentifierValue != null)
            epaddPremis.setStatuteDocumentationIdentifierValue(cm.statuteDocumentationIdentifierValue);
        if (cm.statuteDocumentationRole != null) epaddPremis.setStatuteDocumentationRole(cm.statuteDocumentationRole);
        if (cm.environmentCharacteristic != null)
            epaddPremis.setIntellectualEntityObjectEnvironmentCharacteristics(cm.environmentCharacteristic);
        if (cm.environmentNote != null) epaddPremis.setIntellectualEntityObjectEnvironmentNote(cm.environmentNote);
        if (cm.relatedEnvironmentPurpose != null)
            epaddPremis.setIntellectualEntityObjectEnvironmentPurpose(cm.relatedEnvironmentPurpose);
        if (cm.softwareName != null) epaddPremis.setIntellectualEntityObjectEnvironmentSoftwareName(cm.softwareName);
        if (cm.softwareVersion != null)
            epaddPremis.setIntellectualEntityObjectEnvironmentSoftwareVersion(cm.softwareVersion);
        if (cm.statuteJurisdiction != null) epaddPremis.setStatutestatuteJurisdiction(cm.statuteJurisdiction);

        for (Archive.FileMetadata fm : cm.fileMetadatas) {
            if (fm.fileID != null && fm.fileID.equals(fileID)) {
                epaddPremis.setFileMetadata(fm.fileID, fm);
            }
        }

        archive.printEpaddPremis();
    }
        String baseDir = archive.baseDir;
        String dir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;
        String processingFilename = dir + File.separatorChar + Config.COLLECTION_METADATA_FILE;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter fwriter = null;
        try {
            fwriter = new FileWriter(processingFilename);
            gson.toJson(cm,fwriter);
        } catch (IOException e) {
            Util.print_exception("Unable to write processing metadata", e, log);
            e.printStackTrace();
        }finally {
            if(fwriter!=null){
                try {
                    fwriter.close();
                } catch (IOException e) {
                    Util.print_exception("Unable to write processing metadata", e, log);
                    e.printStackTrace();
                }
            }
        }
        log.info("The time spent in calculating metadata is "+ System.currentTimeMillis());


        //if this was an incremental update in collection metadata, we need to update the bag's metadata as well..
        if(mode== Archive.Save_Archive_Mode.INCREMENTAL_UPDATE)
            archive.updateFileInBag(processingFilename,baseDir);

    }

    //#################################Start: Saving the archive (flat directory or bag) depending upon the mode argument####################

    public static Archive readArchiveIfPresent(String baseDir){
    return readArchiveIfPresent(baseDir, ModeConfig.Mode.APPRAISAL);//The behaviour of readArchive varies depending upon the mode. If mode is discovery then only addressbook and
        //correspondents are loaded (apart from archive object ofcourse). For non-disocvery modes the behaviour is same (load everything).
    }
    /** VIP method. Should be the single place to load an archive from disk.
* loads an archive from the given directory. always re-uses archive objects loaded from the same directory.
     * this is fine when:
     * - running single-user
     * - running discovery mode epadd, since a single archive should be loaded only once.
     * - even in a hosted mode with different archives in simultaneous play, where different people have their own userKeys and therefore different dirs.
     * It may NOT be fine if  multiple people are operating on their different copies of an archive loaded from the same place. Don't see a use-case for this right now.
     * if you don't like that, tough luck.
     * return the archive, or null if it doesn't exist. */
    public static Archive readArchiveIfPresent(String baseDir, ModeConfig.Mode mode) {
        //check if a valid bag in basedir.
        Bag archiveBag=Archive.readArchiveBag(baseDir);
        if(archiveBag==null)
            return null;

        //else, the bag has been verified.. now load the content.
        String archiveFile = baseDir + File.separator +  Archive.BAG_DATA_FOLDER+ File.separator + Archive.SESSIONS_SUBDIR + File.separator + "default" + SESSION_SUFFIX;
        if (!new File(archiveFile).exists()) {
            return null;
        }

        /*synchronized (globaldirToLoadCountMap) {
            Integer loadCount = globaldirToLoadCountMap.get(baseDir);
            int newCount = (loadCount == null) ? 1 : loadCount + 1;
            globaldirToLoadCountMap.put(baseDir, newCount);
            log.info ("Since server start, the archive: " + archiveFile + " has been (attempted to be) loaded " + Util.pluralize(newCount, "time"));
        }*/

        try {
            // locking the global dir might be inefficient if many people are loading different archives at the same time.
            // not a concern right now. it it does become one, locking a small per-dir object like archiveFile.intern(), along with a ConcurrenctHashMap might handle it.
            synchronized (globaldirToArchiveMap) {
                // the archive is wrapped inside a weak ref to allow the archive object to be collected if there are no references to it (usually the references
                // are in the user sessions).
                WeakReference<Archive> wra = getArchiveFromGlobalArchiveMap(baseDir);
                if (wra != null) {
                    Archive a = wra.get();
                    if (a != null) {
                        log.info("Great, could re-use loaded archive for dir: " + archiveFile + "; archive = " + a);
// 2022-09-13			
						//a.close();	// close the archive before re-use
                        return a;
                    }
                }

                log.info("Archive not already loaded, reading from dir: " + archiveFile);
                Map<String, Object> map = loadSessionAsMap(archiveFile, baseDir, true,mode);
                // read the session map, but only use archive
                Archive a = (Archive) map.get("archive");
                // could do more health checks on archive here
                if (a == null) {
                    log.warn ("Archive key is not present in archive file! The archive must be corrupted! directory:" + archiveFile);
                    return null;
                }
                a.setBaseDir(baseDir);



// no need to read archive authorized authorities, they will be loaded on demand from the legacy authorities.ser file
                addToGlobalArchiveMap(baseDir,a);
                //check if the loaded archive satisfy the verification condtiions. Call verify method on archive.
               /* JSPHelper.log.info("After reading the archive checking if it is in good shape");
                a.Verify();*/
                //assign bag to archive object.
                a.setArchiveBag(archiveBag);

                if(mode!= ModeConfig.Mode.DISCOVERY) {
                    //now intialize the cache for lexicon
                    JSPHelper.log.info("Computing summary of Lexicons");
                    //To reduce the load time that is spent in this summary calculation we first check if there is a file named 'lexicon-summary'
                    //in the basedir. If it is present then the summary datastructure is filled using that file. If not then the load
                    long startTime = System.currentTimeMillis();
                    Lexicon.fillL1_Summary_all(a, false);
                    JSPHelper.log.info("Lexicons summary computed successfully in "+ (System.currentTimeMillis()-startTime) + " milliseconds");

                }
                return a;

            }
        } catch (Exception e) {
            Util.print_exception("Error reading archive from dir: " + archiveFile, e, log);
            throw new RuntimeException(e);
        }
    }

    private static String removeTrailingSlashFromDirName(String dir){
        return new File(dir).getAbsolutePath();
    }

    private static void addToGlobalArchiveMap(String archiveDir, Archive archive){

        String s = removeTrailingSlashFromDirName(archiveDir);
        //add to globalDirmap
        globaldirToArchiveMap.put(s, new WeakReference<>(archive));
        //construct archive ID from the tail of the archiveFile (by sha1)
        String archiveID = Util.hash(s);
        globalArchiveIDToArchiveMap.put(archiveID,archive);
        //for reverse mapping
        globalArchiveToArchiveIDMap.put(archive,archiveID);

    }

    public static void removeFromGlobalArchiveMap(String archiveDir, Archive archive){
        String s = removeTrailingSlashFromDirName(archiveDir);
        globaldirToArchiveMap.remove(s);
        //remove from reverse mapping but first get the archive ID.
        String archiveID = globalArchiveToArchiveIDMap.get(archive);
        globalArchiveToArchiveIDMap.remove(archive);
        //remove from archiveID to archive mapping.
        if(!Util.nullOrEmpty(archiveID))
            globalArchiveIDToArchiveMap.remove(archiveID);
    }

    public static WeakReference<Archive> getArchiveFromGlobalArchiveMap(String archiveFile){
        String s = removeTrailingSlashFromDirName(archiveFile);
        return globaldirToArchiveMap.getOrDefault(s,null);
    }

    //If there is only one archive present in the global map then this funciton returns that
    //else it return null.
    public static Archive getDefaultArchiveFromGlobalArchiveMap(){
        if(globaldirToArchiveMap.size()==1)
            return globaldirToArchiveMap.values().iterator().next().get();
        else
            return null;
    }

    //This function returns the archiveID for the given archive
    public static String getArchiveIDForArchive(Archive archive){

        return globalArchiveToArchiveIDMap.getOrDefault(archive,null);
    }

    //This function returns the archive for the given archiveID
    public static Archive getArchiveForArchiveID(String archiveID){
        return globalArchiveIDToArchiveMap.getOrDefault(archiveID,null);
    }

    /**
     * reads from default dir (usually ~/.muse/user) and sets up cachedir,
     * archive vars.
     */
    public static Archive prepareAndLoadDefaultArchive(HttpServletRequest request) {
        HttpSession session = request.getSession();

        // allow cacheDir parameter to override default location
        String dir = request.getParameter("cacheDir");
        if (Util.nullOrEmpty(dir))
            dir = CACHE_DIR;
        JSPHelper.log.info("Trying to read archive from " + dir);

        Archive archive = readArchiveIfPresent(dir);
        if (archive != null)
        {
            JSPHelper.log.info("Good, archive read from " + dir);

        /*	// always set these three together
            session.setAttribute("userKey", "user");
            session.setAttribute("cacheDir", dir);
            session.setAttribute("archive", archive);
*/
            // is this really needed?
            //Archive.prepareBaseDir(dir); // prepare default lexicon files etc.
/*
            Lexicon lex = archive.getLexicon("general");
            if (lex != null)
                session.setAttribute("lexicon", lex); // set up default general lexicon, so something is in the session as default lexicon (so facets can show it)
*/
        }
        return archive;
    }

    public static Archive prepareAndLoadArchive(MuseEmailFetcher m, Multimap<String,String> paramsMap) throws IOException
    {

        // here's where we create a fresh archive
        String userKey = "user";
        /*if (ModeConfig.isServerMode())
        {
            // use existing key, or if not available, ask the fetcher which has the login email addresses for a key
            userKey = (String) session.getAttribute("userKey");
            if (Util.nullOrEmpty(userKey))
                userKey = m.getEffectiveUserKey();
            Util.ASSERT(!Util.nullOrEmpty(userKey)); // disaster if we got here without a valid user key
        }*/

        int i = new Random().nextInt();
        String randomPrefix = String.format("%08x", i);
        String archiveDir = ModeConfig.isProcessingMode()?Config.REPO_DIR_PROCESSING + File.separator + randomPrefix : CACHE_BASE_DIR + File.separator+userKey;
        //String archiveDir = Sessions.CACHE_BASE_DIR + File.separator + userKey;
        Archive archive = readArchiveIfPresent(archiveDir);

        if (archive != null) {
            JSPHelper.log.info("Good, existing archive found");
        } else {
            JSPHelper.log.info("Creating a new archive in " + archiveDir);
            archive = JSPHelper.preparedArchive(paramsMap, archiveDir, new ArrayList<>());
            //by this time the archive is created
            // add this to global maps archiveID->archive, archive->archiveID
            addToGlobalArchiveMap(archiveDir,archive);

        }

        Lexicon lex = archive.getLexicon("general");
    /*	if (lex != null)
            session.setAttribute("lexicon", lex); // set up default general lexicon, so something is in the session as default lexicon (so facets can show it)
    */
        return archive;
    }

    public static void main(String args[]){
        File tmp = null;
        ArchiveReaderWriter.readCollectionMetadata("C:/Users/Jayant Lohani/epadd-appraisal/user"); //This can be changed as per location
          /*  FileUtils.moveDirectory(Paths.get("/home/chinmay/test").toFile(),Paths.get("/home/chinmay/test2").toFile());
            //Files.copy(Paths.get(userDir+File.separatorChar+Archive.BAG_DATA_FOLDER),tmp.toPath(),StandardCopyOption.REPLACE_EXISTING);
            File wheretocopy = Paths.get("/home/chinmay/test2/test").toFile();
            Util.deleteDir(wheretocopy.getPath(),log);*/

        //FileUtils.moveDirectory(tmp.toPath().toFile(),wheretocopy);

        tmp.delete();


    }

}
