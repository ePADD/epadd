/*
 * Copyright (C) 2012 The Stanford MobiSocial Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 /*
	2022-09-05 	Added handling for IMAP
	2022-09-09	Fixed bug in generateExportableAssetsNormalizedMbox
	2022-10-03	Changed the fileformat to IMAP for IMAP import
    2022-10-03 	Corrected function isMBOX() to check if the source is MBOX 
	2022-10-04	Added file ID same as MBOX for IMAP
	2022-10-13	Added SHA-256 in updateFileInBag	
    2022-10-19  Added PREMIS data export in JSON
	2022-11-01  Moved export2JSON() into epaddPremis.printToFiles()
	2022-11-09	Added function getSidecarDir()
 */
package edu.stanford.muse.index;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.AddressBookManager.CorrespondentAuthorityMapper;
import edu.stanford.muse.AnnotationManager.AnnotationManager;
import edu.stanford.muse.Config;
import edu.stanford.muse.LabelManager.Label;
import edu.stanford.muse.LabelManager.LabelManager;
import edu.stanford.muse.ResultCacheManager.ResultCache;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.email.*;
import edu.stanford.muse.epaddpremis.EpaddEvent;
import edu.stanford.muse.epaddpremis.EpaddPremis;
import edu.stanford.muse.ie.NameInfo;
import edu.stanford.muse.ie.variants.EntityBookManager;
import edu.stanford.muse.ner.NER;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.EmailRenderer;
import edu.stanford.muse.webapp.ModeConfig;
import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.creator.CreatePayloadManifestsVistor;
import gov.loc.repository.bagit.creator.CreateTagManifestsVistor;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.util.PathUtils;
import gov.loc.repository.bagit.writer.ManifestWriter;
import gov.loc.repository.bagit.writer.MetadataWriter;
import groovy.lang.Tuple2;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryparser.classic.ParseException;
import org.joda.time.DateTime;
import org.json.JSONArray;

import javax.mail.Header;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/*
import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.exceptions.*;
import gov.loc.repository.bagit.hash.Hasher;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.util.PathUtils;
import gov.loc.repository.bagit.verify.BagVerifier;
import gov.loc.repository.bagit.writer.BagWriter;
import org.apache.commons.collections4.BagUtils;
import org.apache.commons.collections4.bag.HashBag;
*/

/**
 * Core data structure that represents an archive. Conceptually, an archive is a
 * collection of indexed messages (which can be incrementally updated), along
 * with a blob store. It also has addressbooks, group assigner etc, which are
 * second order properties -- they may be updated independently of the docs (in
 * the future). allDocs is the indexed docs, NOT the ones in the current
 * filter... need to work this out. An archive should be capable of being loaded
 * up in multiple sessions simultaneously. one problem currently is that
 * summarizer is stored in indexer -- however, we should pull it out into
 * per-session state.
 *
 */
@Getter
public class Archive implements Serializable, StatusProvider {
    public static final String UNTOUCHED_IMPORTED_MBOX_FILES = "untouched imported mbox files";
    private static final Logger log =  LogManager.getLogger(Archive.class);
    private static final long serialVersionUID = 1L;

    // the archive structure: the archive's top level dir has these subdirs
    public static final String BAG_DATA_FOLDER="data";
    public static final String BLOBS_SUBDIR = "blobs";
    public static final String SIDECAR_DIR = BLOBS_SUBDIR + File.separatorChar + "sidecarfiles";

    public static final String TEMP_SUBDIR = System.getProperty("java.io.tmpdir");
    public static final String INDEXES_SUBDIR = "indexes";
     public static final String SESSIONS_SUBDIR = "sessions"; // original idea was that there would be different sessions on the same archive (index). but in practice we only have one session
    public static final String LEXICONS_SUBDIR = "lexicons";
    private static final String FEATURES_SUBDIR = "mixtures";
    public static final String IMAGES_SUBDIR = "images";

    public static final String ADDRESSBOOK_SUFFIX = "AddressBook";
    public static final String ENTITYBOOKMANAGER_SUFFIX = "EntityBooks";
    public static final String ENTITYBOOK_SUFFIX = "EntityBook";
    public static final String CAUTHORITYMAPPER_SUFFIX= "CorrespondentAuthorities";
    public static final String ANNOTATION_SUFFIX = "Annotations.csv";
    public static final String LABELMAPDIR= "LabelMapper";
    public static final String BLOBLNORMALIZATIONFILE_SUFFIX="NormalizationInfo.csv";
    public static final String EXPORTABLE_ASSETS_SUBDIR = "exportableAssets";
    static final String DIR_NAME_FILES_BEFORE_CONVERSION_WITH_EMAILCHEMY = "files before conversion with Emailchemy";
    static final String MBOX_FILES_GENERATED_WITH_EMAILCHEMY = "mbox files generated by emailchemy";
    static final String EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR = "AppraisalNormalizedAppraised";
    static final String EXPORTABLE_ASSETS_PROCESSING_NORMALIZED_SUBDIR = "ProcessingNormalized";
    static final String EXPORTABLE_ASSETS_PROCESSING_NORMALIZED_PROCESSED_SUBDIR = "ProcessingNormalizedProcessed";
    static final String EXPORTABLE_ASSETS_APPRAISAL_CANONICAL_ACQUISITIONED_SUBDIR = "AppraisalCanonicalAcquisitioned";
    static final String ASSETS_APPRAISAL_NORMALIZED_ACQUISITIONED_SUBDIR = "AppraisalNormalizedAcquisitioned";
    public transient  static ResultCache cacheManager = new ResultCache();//making igeneratedt static so that it becomes visible for all archives.

    public transient static String processingBaseDir = "";
    private static Set<String> alreadyReadMetadataForNonMboxFiles = new HashSet<>();

    // We read and write the serialized Premis object in EpaddPremis.readPremisObject() and
	// EpaddPremis.savePremisObject()
    public transient EpaddPremis epaddPremis;

    public static void clearAlreadyReadMetadataForNonMboxFiles() {
        alreadyReadMetadataForNonMboxFiles.clear();
    }

    private static boolean isNonMboxFileAlreadyInFileMetadata(String pathOnDisk) {
        boolean nonMboxFileAlreadyFileMetadata = false;//There might be a number of folders in a non mbox file. We want to add the event of importing
        //the non mbox file only once and we also want to add the file only once.
        for (String nonMboxFileName : alreadyReadMetadataForNonMboxFiles) {
            if (pathOnDisk.equals(nonMboxFileName)) {
                nonMboxFileAlreadyFileMetadata = true;
                break;
            }
        }
        return nonMboxFileAlreadyFileMetadata;
    }

    public int getNImageAttachments() {
        int nImages = 0;
        for (Blob b : blobStore.uniqueBlobs) {
            if (blobStore.is_image(b)) {
                nImages++;
            }
        }
        return nImages;
    }

    public int getNAttachedEmails() {
        int nEmails = 0;
        for (Document d : getAllDocs()) {
            EmailDocument ed = (EmailDocument) d;
            for (Blob blob : ed.attachments) {
                if (blob.isRfc822()) {
                    nEmails++;
                }
            }
        }
        return nEmails;
    }

    public int getNAttachmentsForSuffix(String... suffix) {
        for (int i = 0; i < suffix.length; i++) {
            suffix[i] = suffix[i].toLowerCase();
        }
        int nAttachments = 0;
        for (Document d : getAllDocs()) {
            EmailDocument ed = (EmailDocument) d;
            for (Blob blob : ed.attachments) {
                for (String s : suffix) {
                    if (blob.getFilename().toLowerCase().endsWith("." + s)) {
                        nAttachments++;
                        break;
                    }
                }
            }
        }
        return nAttachments;
    }

    public Multimap<Document, Tuple2<String,String>> getDupMessageInfo() {
        if(dupMessageInfo==null)
        {
            dupMessageInfo = LinkedHashMultimap.create();
        }
        return dupMessageInfo;
    }

    public Map<String,Span[]> getAllEntities(int maxdocs) {
        Map<String,Span[]> docs = null;
        try {
             docs = indexer.getAllEntitiesInDocs(maxdocs);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return docs;
    }

    @Override
    public String getStatusMessage() {
        return null;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCancelled() {
        return false;
    }


    public enum ExportMode {EXPORT_APPRAISAL_TO_PROCESSING,EXPORT_PROCESSING_TO_DELIVERY,EXPORT_PROCESSING_TO_DISCOVERY}
    public enum AssetType {APPRAISAL_CANONICAL_ACQUISITIONED, APPRAISAL_NORMALIZED_ACQUISITIONED, APPRAISAL_NORMALIZED_APPRAISED, PROCESSING_NORMALIZED, PROCESSING_NORMALIZED_PROCESSED}
    public static String[] LEXICONS =  new String[]{
                                                    "default.english.lex.txt","Persona.academic.administrator.sensitive.duke.english.lex.txt",
                                                    "Persona.author.princeton.english.lex.txt","Persona.composer.NYPL.english.lex.txt",
            "Persona.environmental.artist.projects.stanford.english.lex.txt","Persona.faculty.UCI.english.lex.txt",
            "Persona.journalist.activist.politics.and.travel.UCB.english.lex.txt","Persona.microbiologist.UIUC.english.lex.txt",
            "Persona.writer.theater.NYPL.english.lex.txt","regex.english.lex.txt","sensitive.english.lex.txt",
            "sensitive-cca-french.french.lex.txt","sensitive-ccca-german.german.lex.txt","sentiments.english.lex.txt"
                                                    }; // this is the default, for Muse. EpaddIntializer will set it differently. don't make it final
    public enum Save_Archive_Mode {INCREMENTAL_UPDATE, FRESH_CREATION}

    ////////////  CACHE variables ////////
    ////////////  CACHE variables ///////////////
    // these 5 variables cache the list of all entities/blob names/annotations/folders/ email sources in the archive
    // of these folders, email source and blob names generally don't change
    // however, entities and annotations must be recomputed any time there is a change
    // for good measure, we invalidate all of them when close() is called on the archive
    private transient Set<String> allEntities, allBlobNames, allAnnotations, allFolders, allEmailSources;
    ////////////  END CACHE variables ///////////////
    private transient Bag archiveBag;
    /* all of the following don't change based on the current filter */
    public Indexer indexer;
    private IndexOptions indexOptions;
    public BlobStore blobStore;
    public transient AddressBook addressBook; //transient because it is saved explicitly
    transient private Map<String, Lexicon> lexiconMap = new HashMap<String, Lexicon>();
    private List<Document> allDocs;                                                    // this is the equivalent of fullEmailDocs earlier
    transient private Set<Document> allDocsAsSet = null;
    transient private Map<Document,Document> allUniqueDocsMap=null;
    private Multimap<Document, Tuple2<String,String>> dupMessageInfo = LinkedListMultimap.create();//added to support more informative messages when finding duplicate mails..
    private transient Map<Long,List<Document>> threadIDToDocs = new LinkedHashMap<>();

    private final Set<FolderInfo> fetchedFolderInfos = new LinkedHashSet<>();    // keep this private since its updated in a controlled way
    transient private LinkedHashMap<String, FolderInfo> fetchedFolderInfosMap = null;
    public final Set<String> ownerNames = new LinkedHashSet<>();
    public final Set<String> ownerEmailAddrs = new LinkedHashSet<>();
    private transient EntityBookManager entityBookManager;//transient because it is saved explicitly
    public transient CorrespondentAuthorityMapper correspondentAuthorityMapper; /* transient because this is saved and loaded separately */
    private Map<String, NameInfo> nameMap;

    private transient LabelManager labelManager; //transient because it will be saved and loaded separately

    private transient AnnotationManager annotationManager;//transient because it will be saved and loaded separately
    public transient CollectionMetadata collectionMetadata = new CollectionMetadata();//setting it as transient since v5 as it will be stored/read separately
    public final List<FetchStats> allStats = new ArrayList<FetchStats>(); // multiple stats because usually there is 1 per import

    public String archiveTitle; // this is the name of this archive

    public Bag getArchiveBag(){
        return archiveBag;
    }

    public void setArchiveBag(Bag bag){
        this.archiveBag=bag;
    }
    public synchronized CorrespondentAuthorityMapper getCorrespondentAuthorityMapper() throws IOException, ParseException {
        // auth mapper is transient, so may have to be created each time. but it will be loaded from a file if it already exists
        if (correspondentAuthorityMapper == null)
            correspondentAuthorityMapper = CorrespondentAuthorityMapper.createCorrespondentAuthorityMapper(this);
        return correspondentAuthorityMapper;
    }
    /** recreates the authority mapper, call this, e.g. if the address book changes. */
    public synchronized void recreateCorrespondentAuthorityMapper() throws IOException, ParseException {
        correspondentAuthorityMapper= CorrespondentAuthorityMapper.createCorrespondentAuthorityMapper(this);
    }

    public synchronized  LabelManager getLabelManager() {
        if(labelManager == null)
            labelManager = new LabelManager(ArchiveReaderWriter.getArchiveIDForArchive(this));
        return labelManager;
    }


    public void setLabelManager(LabelManager labelManager) {
        this.labelManager = labelManager;
    }

    public synchronized AnnotationManager getAnnotationManager(){
        if(annotationManager==null)
            annotationManager = new AnnotationManager();
        return annotationManager;
    }

    public void setAnnotationManager(AnnotationManager annotationManager){ this.annotationManager = annotationManager;}

    public boolean isCandidateForReleaseLabel(Document doc){
        EmailDocument ed = (EmailDocument)doc;
        Set<String> timeRestrictions = getLabelManager().getTimedRestrictions();
        //if no timeRestriction then also the following method will return true;
        return isTimeRestrictionExpired(ed,timeRestrictions);
    }

    /*//This method checks if a given label can be applied to a doc or not. It handles two cases.
    //1. a timed restriction relative label can not be applied to a document with hackydate.
    //2. a cleared for release label can not be applied to a message unless it is a candidate for release.
    public Set<String> getAllowedLabels(Document doc, Set<String> labelIDs){
        EmailDocument ed = (EmailDocument)doc;
        boolean hackydate = EmailFetcherThread.INVALID_DATE.equals(ed.getDate());

        boolean isCandiateForRelease=isCandidateForReleaseLabel(doc);
        //NOTE for RESEARCH: Repeated calculation of a method which is guaranteed to yield the same result; can it be avoided? Food for tought
        Set<String> relativeTimedRestrictionLabels = labelManager.getRelativeTimedRestrictionLabels();
        Set<String> allowedLabels = new LinkedHashSet<>();
        labelIDs.forEach(labid->{
            if(labid.equals(LabelManager.LABELID_CFR)) {
                if (isCandiateForRelease)
                    allowedLabels.add(labid);
            }
            else if(hackydate && relativeTimedRestrictionLabels.contains(labid)){
                //don't add it
            }else{
                allowedLabels.add(labid);
            }
        });
        return allowedLabels;
    }
*/

    public Pair<Integer, String> setLabels(Collection<Document> docs, Set<String> labelIDs){
Set<String> timeRestrictedLabels = labelManager.getRelativeTimedRestrictionLabels();
         int countfail = 0;
int errortype=0;
        String message="";
        for(Document doc: docs){
            Set<String> existinglabs = getLabelIDs((EmailDocument)doc);
            labelManager.setLabels(doc.getUniqueId(), labelIDs);
            boolean isHacky = ((EmailDocument) doc).hackyDate ;//EmailFetcherThread.INVALID_DATE.equals(((EmailDocument)doc).getDate());
            Set<String> allLabels = Util.setUnion(existinglabs,labelIDs);
                //check if existingIds U labelIDs contain cleared for release and is not candidateForRelease..
            if(allLabels.contains(LabelManager.LABELID_CFR) && !isCandidateForReleaseLabel(doc)){
                //don't set the label, log the error message and the count of messages which got this error.
                countfail=countfail+1;
                errortype=1;
                //set the labels to the old labels..
                labelManager.putOnlyTheseLabels(doc.getUniqueId(),existinglabs);
            }else if(Util.setIntersection(allLabels,timeRestrictedLabels).size()!=0 && isHacky){
                //related timed restriction can not be applied on hacky dates.
                countfail = countfail+1;
                errortype=2;
                //set the labels to the old labels..
                labelManager.putOnlyTheseLabels(doc.getUniqueId(),existinglabs);
            }else{
                //not needed explicitly labelManager.setLabels(doc.getUniqueId(),labelIDs);
            }
        }

        if(countfail>0 && errortype==1){
            if(docs.size()==1)
                message="'Cleared for release' label can not coexist with a label that is not expired. Either remove the 'cleared for release' label or remove the time restriction label that has not expired.";
            else
                message = "This label could not be set for "+countfail +" message(s) because 'Cleared for release' label can not coexist with a label that is not expired. Either remove the 'cleared for release' label or remove the time restriction label that has not expired for these messages";

        }

        if(countfail>0 && errortype==2){
            if(docs.size()==1)
                message="A relative timed restriction can not be applied on this message as it's date got corrupted during import";
            else
                message = "This label could not be set for "+countfail +" message(s) because the date/time of these messages got corrupted during import";

        }


        return new Pair(countfail,message);
    }

    public void unsetLabels(Collection<Document> docs, Set<String> labelIDs) {
        docs.forEach(doc -> labelManager.unsetLabels(doc.getUniqueId(), labelIDs));
    }

    public Pair<Integer,String> putOnlyTheseLabels(Collection<Document> docs, Set<String> labelIDs){
        Set<String> timeRestrictedLabels = labelManager.getRelativeTimedRestrictionLabels();
        int countfail = 0;
        int errortype=0;
        String message="";

        for(Document doc: docs){
            Set<String> existinglabs = getLabelIDs((EmailDocument)doc);
            labelManager.putOnlyTheseLabels(doc.getUniqueId(), labelIDs);
            boolean isHacky = ((EmailDocument)doc).hackyDate;// EmailFetcherThread.INVALID_DATE.equals(((EmailDocument)doc).getDate());
            Set<String> allLabels = Util.setUnion(existinglabs,labelIDs);
            //check if existingIds U labelIDs contain cleared for release and is not candidateForRelease..
            if(Util.setUnion(existinglabs,labelIDs).contains(LabelManager.LABELID_CFR) && !isCandidateForReleaseLabel(doc)){
                //don't set the label, log the error message and the count of messages which got this error.
                countfail=countfail+1;
                //set the labels to the old labels..
                errortype=1;
                labelManager.putOnlyTheseLabels(doc.getUniqueId(),existinglabs);
            }else if(Util.setIntersection(allLabels,timeRestrictedLabels).size()!=0 && isHacky){
                //related timed restriction can not be applied on hacky dates.
                countfail = countfail+1;
                errortype=2;
                //set the labels to the old labels..
                labelManager.putOnlyTheseLabels(doc.getUniqueId(),existinglabs);
            }else{
                labelManager.putOnlyTheseLabels(doc.getUniqueId(),labelIDs);
            }
        }


        if(countfail>0 && errortype==1){
            if(docs.size()==1)
                message="'Cleared for release' label can not coexist with a label that is not expired. Either remove the 'cleared for release' label or remove the time restriction label that has not expired.";
            else
                message = "This label could not be set for "+countfail +" message(s) because 'Cleared for release' label can not coexist with a label that is not expired. Either remove the 'cleared for release' label or remove the time restriction label that has not expired for these messages";

        }

        if(countfail>0 && errortype==2){
            if(docs.size()==1)
                message="A relative timed restriction can not be applied on this message as it's date got corrupted during import";
            else
                message = "This label could not be set for "+countfail +" message(s) because the date/time of these messages got corrupted during import";

        }
        return new Pair(countfail,message);
    }
    //get all labels for an email document and a given type
    public Set<String> getLabelIDs(EmailDocument edoc){
        return labelManager.getLabelIDs(edoc.getUniqueId());
    }

    public synchronized EntityBookManager getEntityBookManager() {
        if (entityBookManager == null)
            entityBookManager = new EntityBookManager(this);
        return entityBookManager;
    }

    public synchronized  void setEntityBookManager(EntityBookManager eb){
        entityBookManager = eb;
    }

    /*
     * baseDir is used loosely... it may not be fully reliable, e.g. when the
     * archive moves.
     */
    public String baseDir;
    public File lexiconsDir;
    public File computeDir;
    public File exportableAssetsDir;

    public SentimentStats stats = new SentimentStats();

    // clusters are somewhat ephemeral and not necessarily a core part of the
    // Archive struct. consider moving it elsewhere.
    private List<MultiDoc> docClusters;

    /**
     * @return all the links extracted from the archive content*/
    //This is a better location for this than Indexer, I (@vihari) think
    private List<LinkInfo> getLinks() {
        return indexer.links;
    }

    public Set<Blob> blobsForQuery(String term){return indexer.blobsForQuery(term);}

    public Collection<edu.stanford.muse.index.Document> docsForQuery(String term, int cluster, int threshold, Indexer.QueryType qt){
        Indexer.QueryOptions options = new Indexer.QueryOptions();
        options.setQueryType(qt);
        options.setCluster(cluster);
        options.setThreshold(threshold);
        return indexer.docsForQuery(term, options);
    }

    public Collection<edu.stanford.muse.index.Document> docsForQuery(String term, int cluster, int threshold) {
        Indexer.QueryOptions options = new Indexer.QueryOptions();
        options.setCluster(cluster);
        options.setThreshold(threshold);
        return indexer.docsForQuery(term, options);
    }

    public Collection<edu.stanford.muse.index.Document> docsForQuery(String term, int cluster, Indexer.QueryType qt) {
        Indexer.QueryOptions options = new Indexer.QueryOptions();
        options.setCluster(cluster);
        return indexer.docsForQuery(term, options);
    }

    Collection<Document> docsForQuery(int cluster, Indexer.QueryType qt) {
        Indexer.QueryOptions options = new Indexer.QueryOptions();
        options.setCluster(cluster);
        options.setQueryType(qt);
        return indexer.docsForQuery(null, options);
    }

    public Collection<edu.stanford.muse.index.Document> docsForQuery(String term, Indexer.QueryType qt) {
        Indexer.QueryOptions options = new Indexer.QueryOptions();
        options.setQueryType(qt);
        return indexer.docsForQuery(term, options);
    }

    /** VIP method: main way to search for documents with the term (embedded in options) in the archive*/
    public Collection<Document> docsForQuery(String term, Indexer.QueryOptions options){
        return indexer.docsForQuery(term, options);
    }


    /**
     * @param q - query
     * @param qt - query type
     * @return number of hits for the query*/
    public int countHitsForQuery(String q, Indexer.QueryType qt) {
        return indexer.countHitsForQuery(q, qt);
    }

    public int countHitsForQuery(String q) {
        return indexer.countHitsForQuery(q, Indexer.QueryType.FULL);
    }

    public Pair<String,String> getContentsOfAttachment(String fileName){
        return indexer.getContentsOfAttachment(fileName);
    }

    public EmailDocument docForId(String id){ return indexer.docForId(id);}


    public String getTitle(org.apache.lucene.document.Document doc){
        return indexer.getTitle(doc);
    }

    public Indexer.IndexStats getIndexStats(){
        return indexer.stats;
    }

    static public class AccessionMetadata implements java.io.Serializable {
        private final static long serialVersionUID = 1L; // compatibility
        public String id, title, date, scope, rights, notes;
        public List<FileMetadata> fileMetadatas;
    }

    // We define new serializable object for epadd+ project file metadata requirement.
    // It may be part of collection directly (if no accession) or part of an accession.
    static public class FileMetadata implements java.io.Serializable {
        private final static long serialVersionUID = 1L; // compatibility
        public String fileID, filename, fileFormat, notes;
        public String preservationLevelRole, preservationLevelRationale, preservationLevelDateAssigned, compositionLevel;
        public String messageDigestAlgorithm, messageDigest, messageDigestOrginator;
        public String formatName, formatVersion;
        public String creatingApplicationName, creatingApplicationVersion, dateCreatedByApplication;
        public String environmentCharacteristic, relatedEnvironmentPurpose, relatedEnvironmentNote;
        public String softwareName, softwareVersion;

        public void clone(FileMetadata target) {
            fileID = target.fileID;
            filename = target.filename;
            fileFormat = target.fileFormat;
            notes = target.notes;
            preservationLevelRole = target.preservationLevelRole;
            preservationLevelRationale = target.preservationLevelRationale;
            preservationLevelDateAssigned = target.preservationLevelDateAssigned;
            compositionLevel = target.compositionLevel;
            messageDigestAlgorithm = target.messageDigestAlgorithm;
            messageDigest = target.messageDigest;
            messageDigestOrginator = target.messageDigestOrginator;
            formatName = target.formatName;
            formatVersion = target.formatVersion;
            creatingApplicationName = target.creatingApplicationName;
            creatingApplicationVersion = target.creatingApplicationVersion;
            dateCreatedByApplication = target.dateCreatedByApplication;
            environmentCharacteristic = target.environmentCharacteristic;
            relatedEnvironmentPurpose = target.relatedEnvironmentPurpose;
            relatedEnvironmentNote = target.relatedEnvironmentNote;
            softwareName = target.softwareName;
            softwareVersion = target.softwareVersion;
        }
    }

    // these fields are used in the library setting
    static public class CollectionMetadata implements java.io.Serializable {
        private final static long serialVersionUID = 6304656466358754945L; // compatibility
        public String institution, repository, collectionTitle, collectionID, findingAidLink, catalogRecordLink, contactEmail, rights, notes, scopeAndContent, shortTitle, shortDescription;
        public long timestamp;
        public String tz;
        //To make the first date and last date format agnostic we store them in unix timestamp format (using Date.getTime() method).
        public long firstDateTS, lastDateTS;
        //To keep track of the locale where this ingestion took place.
        private String ingestionLocaleTag;
        public int nDocs, nIncomingMessages, nOutgoingMessages, nHackyDates; // note a message can be both incoming and outgoing.
        //Since v7, nOutgoing messages in this class will be treated as the number of messages sent by the owner.
        public int nBlobs, nUniqueBlobs, nImageBlobs, nDocBlobs, nOtherBlobs; // this is just a cache so we don't have to read the archive
        public String ownerName;
        public String about;
        //will be set by method that computes epadd-ner
        public Map<Short, Integer> entityCounts;
        public int numPotentiallySensitiveMessages = -1;
        //Following two fields have been set as transient to avoid their serialization and deserialization by Gson library.
        transient public Date firstDate, lastDate;
        public List<AccessionMetadata> accessionMetadatas;
        public List<FileMetadata> fileMetadatas;
        public int renamedFiles=0;//to record number of files that were renamed /cleanedup as a result of Amatica integration
        public int normalizedFiles=0;//to record number of files that were normalized (format change) as a result of Amatica integration.
        //Following fields have been defined for ePADD+ metadata
        public String archivalHistory, description, access;
        public String embargoReviewDate, embargoStartDate, embargoDuration, embargoEndDate;
        public String sensitivityReview, processingNote;
        public String preservationLevelRole, preservationLevelRationale;
        public String environmentCharacteristic, relatedEnvironmentPurpose, environmentNote;
        public String softwareName, softwareVersion;
        public String rightsStatementIdentifierType, rightsStatementIdentifierValue;
        public String statuteJurisdiction, statuteDocumentationIdentifierType, statuteDocumentationIdentifierValue, statuteDocumentationRole;

        public String getIngestionLocaleTag(){
            if(!Util.nullOrEmpty(ingestionLocaleTag))
            {
                return ingestionLocaleTag;
            }else
                return Locale.getDefault().toLanguageTag();
        }
        public String toJSON() {
            return new Gson().toJson(this);
        }

        private static String mergeField(String a, String b) {
            if (a == null)
                return b;
            if (b == null)
                return a;
            if (a.equals(b))
                return a;
            else
                return a + "+" + b;
        }

        public void merge(CollectionMetadata other) {
            mergeField(this.institution, other.institution);
            mergeField(this.repository, other.repository);
            mergeField(this.collectionTitle, other.collectionTitle);
            mergeField(this.collectionID, other.collectionID);
            mergeField(this.findingAidLink, other.findingAidLink);
            mergeField(this.catalogRecordLink, other.catalogRecordLink);
            mergeField(this.contactEmail, other.contactEmail);
            mergeField(this.rights, other.rights);
            mergeField(this.notes, other.notes);
            // mergeField(this.tz, other.tz);
        }

        public void setIngestionLocaleTag(String toLanguageTag) {
            ingestionLocaleTag = toLanguageTag;
        }

        public void setFileMetadatas(Archive archive, String[] importedFiles) {
            //JFX refactor this method, split into smaller methods
            EpaddPremis epaddPremis = archive.getEpaddPremis();
            final String STORE_FOLDER_SEPARATOR = "^-^";

            CollectionMetadata cm = archive.collectionMetadata;
            List<Archive.FileMetadata> fms;
            int count = 0;

            if (cm.fileMetadatas != null) {
                count = cm.fileMetadatas.size();
                fms = cm.fileMetadatas;
            } else {
                fms = new ArrayList<Archive.FileMetadata>();
            }

            Archive.FileMetadata fm = new Archive.FileMetadata();

            if (importedFiles != null) {
                for (String fs : importedFiles) {
                    boolean isNonMboxFile = false;
                    boolean alreadyReadNonMboxFile = false;
                    int idx = fs.indexOf(STORE_FOLDER_SEPARATOR);
                    if (idx == -1) {
                        // Bad folder name received. Content not parsed
                        continue;
                    }
                    //String accountName = fs.substring (0, idx); // example: GMail
                    String folderName = fs.substring(idx + STORE_FOLDER_SEPARATOR.length()); // example: MyFolder

                    String pathOnDisk;
                    if (fs.indexOf(STORE_FOLDER_SEPARATOR) != 0) {
                        isNonMboxFile = true;
                        //This is a non mbox file. We have folderName which is something like
                        //myemails.pst/private/inbox and path on disc which is something like
                        //D://myPsts/myEMails.pst. One non mbox file can contain several folders
                        //(each converted into one Mbox file internally), so we have one pathOnDisk and
                        //one or more folderNames.
                        pathOnDisk = fs.substring(0, idx);
                        if (new File(pathOnDisk).isDirectory())
                        {
                            //The user pointed to a folder rather than a specific file.
                            //Get the fileName from folderName and add it to pathOnDisk.
                            String fileName = FolderInfo.getNonMboxFileNameFromTmpPath(folderName);
                            pathOnDisk = pathOnDisk + File.separatorChar + fileName;
                        }
                    }
                    else
                    {
                        //This is an Mbox file. There is one folder per Mbox file and folder name and path
                        //on disk are identical.
                        pathOnDisk = folderName;
                    }
                    if (isNonMboxFile) {
                        if (isNonMboxFileAlreadyInFileMetadata(pathOnDisk)) {
                            alreadyReadNonMboxFile = true;
                        } else {
                            alreadyReadMetadataForNonMboxFiles.add(pathOnDisk);
                        }
                    }
                    if (alreadyReadNonMboxFile)
                    {
                        //Only one entry file metadata per non mbox file, not one entry for each Mbox file
                        //generated from the non mbox file.
                        continue;
                    }
                    folderName = FolderInfo.removeTmpPartOfPath(folderName);
                    fm = new Archive.FileMetadata();
                    fm.fileID = "Collection/File/" + StringUtils.leftPad("" + count, 4, "0");
                    fm.filename = pathOnDisk;
                    if (isNonMboxFile)
                    {
                        fm.fileFormat = "NON_MBOX";
                    }
                    else
                    {
                        fm.fileFormat = "MBOX";
                    }
                    if (epaddPremis != null)
                    {
                        epaddPremis.setFileID(fm.fileID, pathOnDisk);
                    }
                    fm.notes = "";

                    count++;
                    fms.add(fm);
                } // end for
            }   //end if (importedFiles!=null)

            cm.fileMetadatas = fms;
            archive.collectionMetadata = cm;
            clearAlreadyReadMetadataForNonMboxFiles();
        }

	
// 2022-09-05 Added for handling IMAP
        // IMAP is currently not supported 16/1/2023
        public void setFileMetadatasIMAP(Archive archive, String[] importedFiles) {
            EpaddPremis epaddPremis = archive.getEpaddPremis();
            final String STORE_FOLDER_SEPARATOR = "^-^";

            CollectionMetadata cm = archive.collectionMetadata;
            List<Archive.FileMetadata> fms;
            int count = 0;

            if (cm.fileMetadatas != null) {
                count = cm.fileMetadatas.size();
                fms = cm.fileMetadatas;
            } else {
                fms = new ArrayList<Archive.FileMetadata>();
    }

            Archive.FileMetadata fm = new Archive.FileMetadata();
                
            if (importedFiles != null) {
                Set<Document> docset=archive.getAllDocsAsSet();
                EmailDocument doc1 = (EmailDocument) docset.iterator().next(); 
                for (String fs : importedFiles) {
                    
                    int idx = fs.indexOf(STORE_FOLDER_SEPARATOR);
                    if (idx == -1) {
                        // Bad folder name received. Content not parsed
                        continue;
                    }

                    //String accountName = fs.substring (0, idx); // example: GMail
                    String folderName = fs.substring(idx + STORE_FOLDER_SEPARATOR.length()); // example: MyFolder
                    folderName = FolderInfo.removeTmpPartOfPath(folderName);
                    fm = new Archive.FileMetadata();
                    fm.filename = doc1.emailSource;
//                    fm.fileID = folderName;
// 2022-10-04
					fm.fileID = "Collection/IMAP/" + StringUtils.leftPad("" + count, 4, "0") + "/" + folderName;
                    if (epaddPremis != null)
                    {
                        epaddPremis.setFileID(fm.fileID, folderName);
//                        epaddPremis.setFileID(fm.fileID, doc1.emailSource + folderName);
                    }
// 2022-10-03					
//                    fm.fileFormat = "MBOX";
                    fm.fileFormat = "IMAP";
                    fm.notes = "";

                    count++;
                    fms.add(fm);
                } // end for
            }   //end if (importedFiles!=null)

            cm.fileMetadatas = fms;
            archive.collectionMetadata = cm;
        }
    }
// 2022-09-05

    /**
     * set the base dir of the archive, this is the place where all the archive cache is dumped
     * */
    public void setBaseDir(String dir) {
        baseDir = dir;
        blobStore.setDir(dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + BLOBS_SUBDIR );
    }

    /**
     * Internal, please do not use!
     * */
    //is being used in types.jsp -> Can we get rid of types.jsp or this call?
    public void setNameMap(Map<String, NameInfo> nameMap) {
        this.nameMap = nameMap;
    }

    public class SentimentStats implements Serializable { // this is a placeholder
        // right now.. its
        // essentially storing
        // archive cluer's stats
        private final static long serialVersionUID = 1L;
        public Map<String, Integer> sentimentCounts;
    }


    private void setBlobStore(BlobStore blobStore) {
        this.blobStore = blobStore;
    }

    //TODO: this should not be public, being used in doSimpleFlow. At least put some some documentation
    //TODO: this should not be public, being used in doSimpleFlow.
    public void setAddressBook(AddressBook ab) {
        addressBook = ab;
    }

    public BlobStore getBlobStore() {
        return blobStore;
    }

    public AddressBook getAddressBook() {
        return addressBook;
    }

    /** private constructor -- always use createArchive() instead */
    private Archive() { }

    public static Archive createArchive() { return createArchive (""); }

    private static Archive createArchive(String title) {
        Archive archive = new Archive();
        archive.archiveTitle = title;
        return archive;
    }

    public void printEpaddPremis() {
        if (epaddPremis != null) {
            epaddPremis.printToFiles();
        }
    }


    public EpaddPremis getEpaddPremis()
    {
        if (epaddPremis == null)
        {
           log.warn("epaddPremis null in Archive.getEpaddPremise()");
        }
        return epaddPremis;
    }

    public synchronized void openForRead() {
        log.info("Opening archive read only");
        indexer.setupForRead();
    }

    public synchronized void openForWrite() throws IOException {
        log.info("Opening archive for write");

        indexer.setupForWrite();
        if (allDocs != null) {
            // we already have some docs in the index, verify it to make
            // sure the archive's idea of #docs is the same as the index's.
            int docsInIndex = indexer.nDocsInIndex();
            log.info(docsInIndex + " doc(s) in index, " + allDocs.size() + " doc(s) in archive");
            Util.warnIf(indexer.nDocsInIndex() != allDocs.size(),
                    "Warning: archive nDocsInIndex is not the same as Archive alldocs (possible if docs have been deleted?)", log);
        }
    }

    public synchronized void close() {
        log.info("Closing archive");
        if (indexer != null)
            indexer.close();
        /*try {
            if (blobStore != null)
                blobStore.pack(); // ideally, do this only if its dirty
        } catch (Exception e) {
            Util.print_exception(e, log);
        }*/

        // clear all the caches, so they will be recomputed at next use
        allEntities = allBlobNames = allFolders = allEmailSources = allAnnotations = null;
    }

    // create a new/empty archive.
    // baseDir is for specifying base location of Indexer's file-based
    // directories
    /**
     * Setup an archive
     * @param baseDir - base dir of the archive
     * @param blobStore - attchmane blob store
     * @param args - options for loading @see{edu.stanford.muse.webapp.JSPHelper.preparedArchive}, set to nul to empty array for defaults
     * */
    public void setup(String baseDir, BlobStore blobStore, String args[]) throws IOException {
        prepareBaseDir(baseDir);
        indexOptions = new IndexOptions();
        indexOptions.parseArgs(args);
        log.info("Index options are: " + indexOptions);
        indexer = new Indexer(baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER, indexOptions);
        if(blobStore!=null)
            setBlobStore(blobStore);
        try {
            String details = "Premis files created as part of archive creation.";
            epaddPremis = new EpaddPremis(baseDir, Archive.BAG_DATA_FOLDER, this);
            epaddPremis.createEvent(EpaddEvent.EventType.PREMIS_FILES_CREATED, details, "success");
        } catch (Exception e) {
            Util.print_exception("Exception creating new EpaddPremis object", e, LogManager.getLogger(Archive.class));
        }
    }

    /**
     * clear all fields, use when indexer needs to be completely cleared
     */
    public void clear() {
        if (indexer != null)
            indexer.clear();
        if (allDocs != null)
            allDocs.clear();
        if (allDocsAsSet != null)
            allDocsAsSet.clear();
        ownerEmailAddrs.clear();
        ownerNames.clear();
        addressBook = null;
    }

    /*
         * should happen rarely, only while exporting session. fragile operation,
         * make sure blobStore etc are updated consistently
         */
    public void setAllDocs(List<Document> docs) {
        log.info("Updating archive's alldocs to new list of " + docs.size() + " docs");
        allDocs = docs;
        allDocsAsSet = null;

        // reset all these fields, they will be computed afresh
        allEntities = allBlobNames = allAnnotations = allFolders = allEmailSources = null;
    }

    public NameInfo nameLookup(String name) {
        String ctitle = name.toLowerCase().replaceAll(" ", "_");
        if (nameMap != null)
            return nameMap.get(ctitle);
        else
            return null;
    }

    private void addOwnerName(String name) {
        ownerNames.add(name);
        collectionMetadata.ownerName = name;
    }

    private void addOwnerEmailAddrs(Collection<String> emailAddrs) {
        ownerEmailAddrs.addAll(emailAddrs);
    }

    public void addOwnerEmailAddr(String emailAddr) {
        ownerEmailAddrs.add(emailAddr);
    }

    /**
     * This should be the only place that creates the cache dir.
     */
    private void prepareBaseDir(String dir) {
        prepareLexiconsDir(dir);
        CreateExportableAssetsDirIfDoesntExist(dir);
        prepareSidecarDir(dir);
    }

    private void prepareLexiconsDir(String dir) {
        dir = dir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar+ LEXICONS_SUBDIR;
        lexiconsDir = new File(dir);
        lexiconsDir.mkdirs();

        // copy lexicons over to the muse dir
        // unfortunately, hard-coded because we are loading as a ClassLoader resource and not as a file, so we can't use Util.filesWithSuffix()
        // we have a different set of lexicons for epadd and muse which will be set up in LEXICONS by the time we reach here
        if(LEXICONS!=null)
          log.info("copying " + LEXICONS.length + " lexicons to " + dir);
        else
            log.warn("Lexicon is null!!!");
        for (String l : LEXICONS) {
            try {

                if (new File(dir+File.separator + l).exists()) {
                    log.info ("Skipping lexicon " + l + " because it already exists");
                    continue;
                }

                InputStream is = EmailUtils.class.getClassLoader().getResourceAsStream("lexicon/" + l);
                if (is == null) {
                    log.warn("lexicon lexicon/" + l + " not found");
                    continue;
                }

                log.info("copying " + l + " to " + dir);
                Util.copy_stream_to_file(is, dir + File.separator + l);
            } catch (Exception e) {
                Util.print_exception(e, log);
            }
        }
    }

    private void CreateExportableAssetsDirIfDoesntExist(String dir) {
        dir = dir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR;
        exportableAssetsDir = new File(dir);
        exportableAssetsDir.mkdirs();
    }

    private void prepareSidecarDir(String dir) {
        dir = dir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SIDECAR_DIR;
        File sidecarDir = new File(dir);
        sidecarDir.mkdirs();
    }

    /** adds alternateEmailAddrs if specified in the request to the session. alternateEmailAddrs are simply appended to. */
    public void updateUserInfo(String name, String archiveTitle, String alteranteEmailAddrs)
    {
        // set up the owner email addrs from the email addrs saved in the fetcher's stores
        if (!Util.nullOrEmpty(name))
            this.addOwnerName(name);
        if (!Util.nullOrEmpty(archiveTitle))
            this.archiveTitle = archiveTitle;

        if (!Util.nullOrEmpty(alteranteEmailAddrs))
            this.addOwnerEmailAddrs(EmailUtils.parseAlternateEmailAddrs(alteranteEmailAddrs));
    }
    /**
     * returns the final, sorted, deduped version of allDocs that this driver
     * worked on in its last run
     */
    public List<Document> getAllDocs() {
        if (allDocs == null) {
            synchronized (this) {
                if (allDocs == null) {
                    allDocs = new ArrayList<>();
                    allDocsAsSet = new LinkedHashSet<>();
                }
            }
        }
        return allDocs;
    }

    public Set<Document> getAllDocsAsSet() {
        // allDocsAsSet is lazily computed
        if (allDocsAsSet == null) {
            synchronized (this) {
                if (allDocsAsSet == null) {
                    allDocsAsSet = new LinkedHashSet<>(getAllDocs());
                    Util.softAssert(allDocs.size() == allDocsAsSet.size(),log);
                }
            }
        }
        return allDocsAsSet;
    }

    public Map<Document,Document> getAllUniqueDocsMap(){
        // allUniqueDocsMap is lazily computed
        if (allUniqueDocsMap == null) {
            synchronized (this) {
                if (allUniqueDocsMap == null) {
                    allUniqueDocsMap = new LinkedHashMap<>();
                    for(Document doc: allDocsAsSet)
                        allUniqueDocsMap.put(doc,doc);
                    Util.softAssert(allUniqueDocsMap.size() == allDocsAsSet.size(),log);
                }
            }
        }
        return allUniqueDocsMap;

    }


    public void Verify() {
        List<Document> docs = getAllDocs();
        for (Document doc : docs) {
            try {
                org.apache.lucene.document.Document ldoc = this.getLuceneDoc(doc.getUniqueId());
                if (ldoc == null) {
                    log.warn("Some serious error: For document " + doc + " no lucene doc found");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    // work in progress - status provider
    public StatusProvider getStatusProvider() {
        return indexer;
    }

    public Map<String, Collection<Document>> getSentimentMap(Lexicon lex, boolean originalContentOnly, String... captions) {
        if (lex == null) {
            log.warn ("Warning: lexicon is null!");
            return new LinkedHashMap<>();
        }
        return lex.getEmotions(indexer, getAllDocsAsSet(), false /* doNota */, originalContentOnly, captions);
    }

    /**
     * gets original content only!
     */
    public String getContents(Document d, boolean originalContentOnly) {
        return indexer.getContents(d, originalContentOnly);
    }

    public String getContents(org.apache.lucene.document.Document ldoc, boolean originalContentOnly){
        return indexer.getContents(ldoc, originalContentOnly);
    }

    /**
     * gets original headers only!
     */
    public String getHeaders(Document d) {
        return indexer.getHeaders(d);
    }

    public String getPreservedContents(Document d) {
        return indexer.getPreservedContents(d);
    }

    public String getHeaders(org.apache.lucene.document.Document ldoc){
        return indexer.getHeaders(ldoc);
    }

    /*
    private void setupAddressBook(List<Document> docs) {
        // in this case, we don't care whether email addrs are incoming or
        // outgoing,
        // so the ownAddrs can be just a null string
        if (addressBook == null)
            addressBook = new AddressBook(new String[0], new String[0]);
        log.info("Setting up address book for " + docs.size() + " messages (indexing driver)");
        for (Document d : docs)
            if (d instanceof EmailDocument)
                addressBook.processContactsFromMessage((EmailDocument) d);

        addressBook.organizeContacts();
    }
    */

    /*
    public List<LinkInfo> extractLinks(Collection<Document> docs) throws Exception {
        prepareAllDocs(docs, indexOptions);
        indexer.clear();
        indexer.extractLinks(docs);
        return EmailUtils.getLinksForDocs(docs);
    }
    */

    public Collection<DatedDocument> docsInDateRange(Date start, Date end) {
        List<DatedDocument> result = new ArrayList<>();
        if (Util.nullOrEmpty(allDocs))
            return result;

        for (Document d : allDocs) {
            try {
                DatedDocument dd = (DatedDocument) d;
                if ((dd.date.after(start) && dd.date.before(end)) || dd.date.equals(start) || dd.date.equals(end))
                    result.add(dd);
            } catch (Exception e) {
                Util.print_exception(e, log);
            }
        }
        return result;
    }

    public boolean containsDoc(Document doc) {
        return getAllDocsAsSet().contains(doc);
    }

    /**
     * use with caution. pseudo-adds a doc to the archive, but without any
     * subject and without any contents. useful only when doing quick screening
     * to check of emails for memory tests, etc.
     */
    public synchronized boolean addDocWithoutContents(Document doc) {
        if (containsDoc(doc))
            return false;

        getAllDocsAsSet().add(doc);
        getAllDocs().add(doc);
        getAllUniqueDocsMap().put(doc,doc);


        String subject = "", contents = "";

        indexer.indexSubdoc(subject, contents, new ArrayList<Header>(), doc, blobStore, "");

        if (getAllDocs().size() % 100 == 0)
            log.info("Memory status after " + getAllDocs().size() + " emails: " + Util.getMemoryStats());

        return true;
    }

    private synchronized List<LinkInfo> postProcess(Collection<Document> docs) {
        // should we sort the messages by time here?

        log.info(indexer.computeStats());
        log.info(getLinks().size() + " links");
        // prepareAllDocs(docs, io);
        // TODO: should we recomputeCards? call nukeCards for now to invalidate
        // cards since archive may have been modified.

        return getLinks();
    }

    public synchronized boolean addDoc(Document doc, String contents) {
        return addDoc(doc, contents, new ArrayList<Header>(), "");

    }
        /**
         * core method, adds a single doc to the archive. remember to call
         * postProcess at the end of any series of calls to add docs
         */
    public synchronized boolean addDoc(Document doc, String contents, List<Header> header, String textHtmlPart) {
        if (containsDoc(doc))
            return false;

        getAllDocsAsSet().add(doc);
        getAllDocs().add(doc);
        getAllUniqueDocsMap().put(doc,doc);


        String subject = doc.getSubjectWithoutTitle();
        subject = EmailUtils.cleanupSubjectLine(subject);
        indexer.indexSubdoc(subject, contents, header, doc, blobStore, textHtmlPart);

        if (getAllDocs().size() % 100 == 0)
            log.info("Memory status after " + getAllDocs().size() + " emails: " + Util.getMemoryStats());

        return true;
    }

    /**
     * prepares all docs for indexing, incl. applying filters, removing dups and
     * sorting
     *
     * @throws Exception
     */
    /*
    private void prepareAllDocs(Collection<Document> docs, IndexOptions io) throws Exception {
        allDocs = new ArrayList<>();
        allDocs.addAll(docs);
        allDocs = EmailUtils.removeDupsAndSort(allDocs);
        log.info(allDocs.size() + " documents after removing duplicates");

        if (addressBook == null && !io.noRecipients) {
            log.warn("no address book previously set up!");
            setupAddressBook(allDocs); // set up without the benefit of ownaddrs
        }

        if (io.filter != null && addressBook != null) {
            Contact ownCI = addressBook.getContactForSelf(); // may return null
            // if we don't
            // have own info
            io.filter.setOwnContactInfo(ownCI);
        }

        // if no filter, accept doc (default)
        List<Document> newAllDocs = new ArrayList<>();
        for (Document d : allDocs)
            if (io.filter == null || (io.filter != null && io.filter.matches(d)))
                newAllDocs.add(d);

        //EmailUtils.cleanDates(newAllDocs);

        log.info(newAllDocs.size() + " documents after filtering");

        allDocs = newAllDocs;
        Collections.sort(allDocs); // may not be essential
        allDocsAsSet = null;
    }
    */

    private String getFolderInfosMapKey(String accountKey, String longName) {
        return accountKey + "..." + longName;
    }

    private void setupFolderInfosMap() {
        if (fetchedFolderInfosMap == null)
            fetchedFolderInfosMap = new LinkedHashMap<>();
        for (FolderInfo fi : fetchedFolderInfos) {
            fetchedFolderInfosMap.put(getFolderInfosMapKey(fi.accountKey, fi.longName), fi);
        }
    }

    /**
     * adds a collection of folderinfo's to the archive, updating existing ones
     * as needed
     */
    public void addFetchedFolderInfos(Collection<FolderInfo> fis) {
        // if a folderinfo with the same accountKey and longname already exists,
        // its lastSeenUID may need to be updated.

        // first organize a key -> folder info map in case we have a large # of
        // folders
        setupFolderInfosMap();

        for (FolderInfo fi : fis) {
            String key = getFolderInfosMapKey(fi.accountKey, fi.longName);
            FolderInfo existing_fi = fetchedFolderInfosMap.get(key);
            if (existing_fi != null) {
                if (existing_fi.lastSeenUID < fi.lastSeenUID)
                    existing_fi.lastSeenUID = fi.lastSeenUID;
            } else {
                fetchedFolderInfos.add(fi);
                fetchedFolderInfosMap.put(key, fi);
            }
        }
    }

    private FolderInfo getFetchedFolderInfo(String accountID, String fullFolderName) {
        setupFolderInfosMap();
        return fetchedFolderInfosMap.get(getFolderInfosMapKey(accountID, fullFolderName));
    }

    /**
     * returns last seen UID for the specified folder, -1 if its not been seen
     * before
     */
    public long getLastUIDForFolder(String accountID, String fullFolderName) {
        FolderInfo existing_fi = getFetchedFolderInfo(accountID, fullFolderName);
        if (existing_fi != null)
            return existing_fi.lastSeenUID;
        else {
            return -1L;
        }
    }

    /***/
    public List<LinkInfo> postProcess() {
        return postProcess(allDocs);
    }

    // replace subject with extracted names
    private static void replaceDescriptionWithNames(Collection<? extends Document> allDocs, Archive archive) throws Exception {
        for (Document d : allDocs) {
            if (!Util.nullOrEmpty(d.description)) {
                //log.info("Replacing description for docId = " + d.getUniqueId());
                // List<String> names =
                // Indexer.extractNames(d.description);
                // Collections.sort(names);
                // d.description = Util.join(names,
                // Indexer.NAMES_FIELD_DELIMITER);
                d.description = IndexUtils.retainOnlyNames(d.description, archive.getLuceneDoc(d.getUniqueId()));
            }
        }
    }

    // Redact the attachment information present in EmailDocument objects
    private static void redactAttachmentDetailsFromDocs(Collection<? extends Document> allDocs, Archive archive) throws Exception {
        for (Document d : allDocs) {
            EmailDocument ed = (EmailDocument)d;
            List<Blob> redactedList = new LinkedList<>();
            if(ed.attachments.size()!=0){
                for(Blob b: ed.attachments){
                    String fname = archive.getBlobStore().full_filename_original(b,false);
                    String ext = Util.getExtension(fname);
                    //Get its name ellipsized. leaving extension as it is.
                    fname = Util.ellipsize(fname,5);
                    fname = fname+"."+ext;
                    redactedList.add(new EmailAttachmentBlob(fname,0,null));//Create a dummy blob object and add it to email doc at the end.
                }
            }
            ed.attachments=redactedList;
        }
    }

    /**
     * export archive with just the given docs to prepare for public mode.
     * docsToExport should be a subset of what's already in the archive. returns
     * true if successful.
     */
	/*
	 * public boolean trimArchive(Collection<EmailDocument> docsToRetain) throws
	 * Exception { if (docsToRetain == null) return true; // return without
	 * doing anything
	 *
	 * // exports messages in current filter (allEmailDocs) //HttpSession
	 * session = request.getSession(); Collection<Document> fullEmailDocs =
	 * this.getAllDocs(); Indexer indexer = sthis.indexer;
	 *
	 * // compute which docs to remove vs. keep Set<Document> docsToKeep = new
	 * LinkedHashSet<Document>(docsToRetain); Set<Document> docsToRemove = new
	 * LinkedHashSet<Document>(); for (Document d: fullEmailDocs) if
	 * (!docsToKeep.contains(d)) docsToRemove.add(d);
	 *
	 * // remove unneeded docs from the index
	 * indexer.removeEmailDocs(docsToRemove); // CAUTION: permanently change the
	 * index! this.setAllDocs(new ArrayList<Document>(docsToRetain)); return
	 * true; }
	 */

    public List<Document> getDocsForExport(ExportMode mode) {
        return getDocsForExport(mode, false);
    }

    /** Get docs for export based on the module for which exporting is taking place*/
    public List<Document> getDocsForExport(ExportMode mode, boolean permLabelOnly) {
        List<Document> docsToExport = new LinkedList<>();
        if (mode == ExportMode.EXPORT_APPRAISAL_TO_PROCESSING) {
            //any message with DNT label will not be transferred
            for (Document d : getAllDocs()) {
                EmailDocument ed = (EmailDocument) d;
                //export the doc only if it does not contain DNT label
                if (!getLabelIDs(ed).contains(LabelManager.LABELID_DNT))
                    docsToExport.add(d);
            }
        } else if (mode == ExportMode.EXPORT_PROCESSING_TO_DELIVERY || mode == ExportMode.EXPORT_PROCESSING_TO_DISCOVERY) {
            //get a set of general restriction ids from labelManager
            //get a set of timed-restriction ids from LabelManager
            Set<String> genRestriction = getLabelManager().getGenRestrictions();
            Set<String> timedRestriction = getLabelManager().getTimedRestrictions();
            Set<String> permRestriction = getLabelManager().getPermRestrictions();
            //any message with any general restriction label (including DNT) will not be transferred
            //any message with timed-restriction label whose date is not over, will not be transferred
            //everything else will be transferred.
            //@TODO- Not yet implemented the labelAppliesToText part of label.
            for (Document d : getAllDocs()) {
                EmailDocument ed = (EmailDocument) d;
                //if it contains any general restriction label
                boolean genfine = true;
                boolean timefine =true;
                boolean permfine = false;
                if (Util.setIntersection(getLabelIDs(ed), genRestriction).size() != 0) {
                    //if gen restriction - DNT then it must contain cfr label for export (unless timed restriction stops it)
                    boolean isDNT = getLabelIDs(ed).contains(LabelManager.LABELID_DNT);
                    //check if this doc should be transferred to Delivery only.
                    boolean isTTD_ONLY = getLabelIDs(ed).contains(LabelManager.LABELID_TTDONLY);

                    //CATCH: What if a document has DNT label along with CFR?
                    if(isDNT && !getLabelIDs(ed).contains(LabelManager.LABELID_CFR))
                        genfine=false;
                    //if gen restriction - Transfer only to delivery then skip the CFR label check but export this doc only to delivery mode.
                    //If this should be transferred only to Delivery and the export is happening for Discovery then also do not transfer.
                    if(isTTD_ONLY && mode == ExportMode.EXPORT_PROCESSING_TO_DISCOVERY)
                        genfine = false;
                }
                if(!genfine)
                    continue;
                if (Util.setIntersection(getLabelIDs(ed), timedRestriction).size() != 0) {
                    /*//0.If we can not find the correct time of this document then be conservative and
                    //dont' export it.
                    if(EmailFetcherThread.INVALID_DATE.equals(ed.getDate()))
                        continue;
*/
                    //if time restriction has expired and it contains cleared for release label then export it.
                    //if time restriction has expired but it does not contain cfr then don't export it.
                    //if time restriction has not expired then don't export it.
                    if(!(isTimeRestrictionExpired(ed,timedRestriction) && getLabelIDs(ed).contains(LabelManager.LABELID_CFR)))
                        timefine=false;
                }
                if(!timefine) {
                    continue;
                }
                //System.out.println("export-from-processing: before checking permission label "); //debug
                // if permission label only option is set on when export to next module, further check if there are permission labels.
                if (permLabelOnly) {
                    //If permission label is found then export it
                    if (Util.setIntersection(getLabelIDs(ed), permRestriction).size() != 0) {
                        //System.out.println("export-from-processing: permission label found and then export it"); //debug
                        permfine = true;
                    }

                    if (!permfine) {
                        continue;
                    }
                }
                    //else export it
                    docsToExport.add(d);

            }

        }
    return docsToExport;
    }


    /*
    Checks if the timerestrictionlabel applied to a given document has expired or not.
     */
    private boolean isTimeRestrictionExpired(EmailDocument ed, Set<String> timedRestriction){
        //1.means at least one timed restriction label on this doc. Check for the timed data
        //if it is past current date/time then export else dont'
        //2.get those timed restrictions
        boolean isTimedRestrictionExpired=true;
        Date dt = ed.getDate();
        Set<String> timedrestrictionsInDoc = Util.setIntersection(getLabelIDs(ed),timedRestriction);
        //if any of the timedrestriction is not satisfied then don't export it.
        for(String labid: timedrestrictionsInDoc){
            Label l = getLabelManager().getLabel(labid);
            if(l.getRestrictionType()== LabelManager.RestrictionType.RESTRICTED_FOR_YEARS){
                int year = l.getRestrictedForYears();
                Date future = new DateTime(dt).plusYears(year).toDate();
                Date today = new DateTime().toDate();
                if(future.after(today))//means the time is not over for this label
                    isTimedRestrictionExpired =false;//don't add doc
            }else if(l.getRestrictionType() == LabelManager.RestrictionType.RESTRICTED_UNTIL){
                Date today = new DateTime().toDate();
                if(l.getRestrictedUntilTime()>today.getTime())//means the restriction time is not over
                    isTimedRestrictionExpired = false;//don't add doc
            }
        }
        return isTimedRestrictionExpired;
    }
    /**
     * a fresh archive is created under out_dir. name is the name of the session
     * under it. blobs are exported into this archive dir. destructive! but
     * should be so only in memory. original files on disk should be unmodified.
     *
     * @param retainedDocs
     * @throws Exception
     */
    public synchronized String export(Collection<? extends Document> retainedDocs, ExportMode export_mode, String out_dir, String name, Consumer<StatusProvider> setStatusProvider) throws Exception {
        if (Util.nullOrEmpty(out_dir))
            return null;
        File dir = new File(out_dir);
        if (dir.exists() && dir.isDirectory()) {
            log.warn("Overwriting existing directory '" + out_dir + "' (it may already exist)");
            FileUtils.deleteDirectory(dir);
        } else if (!dir.mkdirs()) {
            log.warn("Unable to create directory: " + out_dir);
            return null;
        }
        String statusmsg = export_mode== ExportMode.EXPORT_APPRAISAL_TO_PROCESSING? "Exporting to Processing":(export_mode== ExportMode.EXPORT_PROCESSING_TO_DISCOVERY?"Exporting to Discovery":"Exporting to Delivery");

        boolean exportInPublicMode = export_mode== ExportMode.EXPORT_PROCESSING_TO_DISCOVERY;
        boolean exportInDelivery = export_mode==ExportMode.EXPORT_PROCESSING_TO_DELIVERY;
        setStatusProvider.accept(new StaticStatusProvider(statusmsg+":"+"Preparing base directory.."));

        prepareBaseDir(out_dir);
        if (!exportInPublicMode && new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + LEXICONS_SUBDIR).exists())
            FileUtils.copyDirectory(new File(baseDir +  File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + LEXICONS_SUBDIR),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + LEXICONS_SUBDIR));
        //copy normalization file if it exists
        if (!exportInPublicMode && new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR + File.separator + Archive.BLOBLNORMALIZATIONFILE_SUFFIX).exists())
            FileUtils.copyFile(new File(baseDir +  File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR + File.separator + Archive.BLOBLNORMALIZATIONFILE_SUFFIX),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR + File.separator + Archive.BLOBLNORMALIZATIONFILE_SUFFIX));

        if (new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + IMAGES_SUBDIR).exists())
            FileUtils.copyDirectory(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + IMAGES_SUBDIR),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + IMAGES_SUBDIR));
        //internal disambiguation cache
        if (new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + FEATURES_SUBDIR).exists())
            FileUtils.copyDirectory(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + FEATURES_SUBDIR),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + FEATURES_SUBDIR));
        if (new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + edu.stanford.muse.Config.AUTHORITY_ASSIGNER_FILENAME).exists())
            FileUtils.copyFile(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + edu.stanford.muse.Config.AUTHORITY_ASSIGNER_FILENAME),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + edu.stanford.muse.Config.AUTHORITY_ASSIGNER_FILENAME));


        if (new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + EpaddPremis.XML_FILE_NAME).exists())
            FileUtils.copyFile(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + EpaddPremis.XML_FILE_NAME),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EpaddPremis.XML_FILE_NAME));

        if (new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + EpaddPremis.SERIALIZED_FILE_NAME).exists())
            FileUtils.copyFile(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + EpaddPremis.SERIALIZED_FILE_NAME),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EpaddPremis.SERIALIZED_FILE_NAME));

        if (new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + EpaddPremis.JSON_FILE_NAME).exists())
            FileUtils.copyFile(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + EpaddPremis.JSON_FILE_NAME),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EpaddPremis.JSON_FILE_NAME));

       if (export_mode == ExportMode.EXPORT_APPRAISAL_TO_PROCESSING && new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + ASSETS_APPRAISAL_NORMALIZED_ACQUISITIONED_SUBDIR).exists()) {
           FileUtils.copyDirectory(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + ASSETS_APPRAISAL_NORMALIZED_ACQUISITIONED_SUBDIR),
                   new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + ASSETS_APPRAISAL_NORMALIZED_ACQUISITIONED_SUBDIR));
       }

        if (export_mode == ExportMode.EXPORT_APPRAISAL_TO_PROCESSING && new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + EXPORTABLE_ASSETS_APPRAISAL_CANONICAL_ACQUISITIONED_SUBDIR).exists()) {
            FileUtils.copyDirectory(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + EXPORTABLE_ASSETS_APPRAISAL_CANONICAL_ACQUISITIONED_SUBDIR),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + EXPORTABLE_ASSETS_APPRAISAL_CANONICAL_ACQUISITIONED_SUBDIR));
        }
        if (export_mode == ExportMode.EXPORT_APPRAISAL_TO_PROCESSING && new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR).exists()) {
            FileUtils.copyDirectory(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR));
        }
        if (export_mode == ExportMode.EXPORT_APPRAISAL_TO_PROCESSING && export_mode != ExportMode.EXPORT_PROCESSING_TO_DELIVERY && new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + SIDECAR_DIR).exists()) {
            FileUtils.copyDirectory(new File(baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + SIDECAR_DIR),
                    new File(out_dir + File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + SIDECAR_DIR));
        }

        // save the states that may get modified
        List<Document> savedAllDocs = allDocs;
        LabelManager oldLabelManager= getLabelManager();
        /////////////////saving done//////////////////////////////////
        //change state of the current archive -temporarily//////////
        if (exportInPublicMode){
            //replace description with names;
            allDocs = new ArrayList<>(retainedDocs);
            replaceDescriptionWithNames(allDocs, this);
            //Also replace the attachment information present in EmailDocument Object
            redactAttachmentDetailsFromDocs(allDocs,this);
        }else{
            allDocs = new ArrayList<>(retainedDocs);
        }
        Set<String> retainedDocIDs = retainedDocs.stream().map(Document::getUniqueId).collect(Collectors.toSet());
        LabelManager newLabelManager = getLabelManager().getLabelManagerForExport(retainedDocIDs,export_mode);
        setStatusProvider.accept( new StaticStatusProvider(statusmsg+":"+"Exporting LabelManager.."));

        setLabelManager(newLabelManager);
        // copy index and if for public mode, also redact body and remove title
        // fields
        final boolean redact_body_instead_of_remove = true;
       /* Set<String> docIdSet = new LinkedHashSet<>();
        for (Document d : allDocs)
            docIdSet.add(d.getUniqueId());
        final Set<String> retainedDocIds = docIdSet;*/
        Indexer.FilterFunctor emailFilter = doc -> {
            if (!retainedDocIDs.contains(doc.get("docId")))
                return false;

            if (export_mode == ExportMode.EXPORT_PROCESSING_TO_DISCOVERY)
            {
                doc.removeFields("headers_original");
            }
            if (exportInPublicMode) {
                String text;
                if (redact_body_instead_of_remove) {
                    text = doc.get("body");
                }
                doc.removeFields("body");
                doc.removeFields("body_original");

                if (text != null) {
                    String redacted_text = IndexUtils.retainOnlyNames(text, doc);
                    doc.add(new Field("body", redacted_text, Indexer.full_ft));
                    //this uses standard analyzer, not stemming because redacted bodys only have names.
                }
                String title = doc.get("title");
                doc.removeFields("title");
                if (title != null) {
                    String redacted_title = IndexUtils.retainOnlyNames(text, doc);
                    doc.add(new Field("title", redacted_title, Indexer.full_ft));
                }
            }

            // if export to Delivery mode, we now mask all email addresses here to negate data protection concerns with full email address of people living/active.
            if (exportInDelivery){
                String textNoEmails;
                String text = doc.get("title");
                if (text != null)
                {
                    doc.removeFields("title");
                    textNoEmails = Util.maskEmail(text);
                    doc.add(new Field("title", textNoEmails, Indexer.full_ft));
                }

                text = doc.get("body");
                if (text != null)
                {
                    doc.removeFields("body");
                    textNoEmails = Util.maskEmail(text);
                    doc.add(new Field("body", textNoEmails, Indexer.full_ft));
                }
            }
            return true;
        };

/*
Moveing it at the end- after changing the basedir of the archive. Because addressbook is getting saved
after maskEmailDomain.
        if (exportInPublicMode) {
            List<Document> docs = this.getAllDocs();
            List<EmailDocument> eds = new ArrayList<>();
            for (Document doc : docs)
                eds.add((EmailDocument) doc);

            EmailUtils.maskEmailDomain(eds, this.addressBook);
        }
*/


        Indexer.FilterFunctor attachmentFilter = doc -> {
            if(exportInPublicMode){
                return false;
            }
            String docId = doc.get("emailDocId");
            if(docId == null){
                Integer di = Integer.parseInt(doc.get("docId"));
                //don't want to print too many messages
                if(di<10)
                    log.error("Looks like this is an old archive, filtering all the attachments!!\n" +
                            "Consider re-indexing with the latest version for a proper export.");
                return false;
            }
            return retainedDocIDs.contains(docId);
        };

        setStatusProvider.accept( new StaticStatusProvider(statusmsg+":"+"Exporting Index.."));

        indexer.copyDirectoryWithDocFilter(out_dir + File.separatorChar + Archive.BAG_DATA_FOLDER, emailFilter, attachmentFilter);
        log.info("Completed exporting indexes");

        setStatusProvider.accept(new StaticStatusProvider(statusmsg+":"+"Exporting Blobs.."));

        // save the blobs in a new blobstore
        if (!exportInPublicMode) {
            log.info("Starting to export blobs, old blob store is: " + blobStore);
            Set<Blob> blobsToKeep = new LinkedHashSet<>();
            for (Document d : allDocs)
                if (d instanceof EmailDocument)
                    if (!Util.nullOrEmpty(((EmailDocument) d).attachments))
                        blobsToKeep.addAll(((EmailDocument) d).attachments);
            String blobsDir = out_dir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar +  BLOBS_SUBDIR;
            new File(blobsDir).mkdirs();
            BlobStore newBlobStore = blobStore.createCopy(blobsDir, blobsToKeep);
            log.info("Completed exporting blobs, newBlobStore in dir: " + blobsDir + " is: " + newBlobStore);
            // switch to the new blob store (important -- the urls and indexes in the new blob store are different from the old one! */
            blobStore = newBlobStore;
        }
        String oldBaseDir = baseDir;
        //change base directory
        setBaseDir(out_dir);

        if (exportInPublicMode) {
            List<Document> docs = this.getAllDocs();
            List<EmailDocument> eds = new ArrayList<>();
            for (Document doc : docs)
                eds.add((EmailDocument) doc);

            EmailUtils.maskEmailDomain(eds, this.addressBook);
        }

        setStatusProvider.accept(new StaticStatusProvider(statusmsg+":"+"Exporting EntityBook Manager.."));

        //now read entitybook manager as well (or build from lucene)
        String outdir = out_dir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;

        String entityBookPath = outdir + File.separatorChar + Archive.ENTITYBOOKMANAGER_SUFFIX;

        EntityBookManager entityBookManager= ArchiveReaderWriter.readEntityBookManager(this,entityBookPath);
        this.setEntityBookManager(entityBookManager);
        //recompute entity count because some documents have been redacted
        double theta = 0.001;
        this.collectionMetadata.entityCounts = this.getEntityBookManager().getEntitiesCountMapModuloThreshold(theta);// getEntitiesCountMapModuloThreshold(this,theta);

        // write out the archive file.. note that this is a fresh creation of archive in the exported folder
        setStatusProvider.accept( new StaticStatusProvider(statusmsg+":"+"Export done. Saving Archive.."));

        ArchiveReaderWriter.saveArchive(out_dir, name, this,Save_Archive_Mode.FRESH_CREATION); // save .session file.
        log.info("Completed saving archive object");

        // restore states
        setBaseDir(oldBaseDir);
        allDocs = savedAllDocs;
        setLabelManager(oldLabelManager);
        return out_dir;
    }





    /*
    Here invariant is that the cached map 'threadIDToDocs' does not get invalidated once an archive is indexed.
     */
    public List<Document> docsWithThreadId(long threadID) {

        if(threadIDToDocs!=null && threadIDToDocs.size()!=0){
            List<Document>  documentList = threadIDToDocs.get(threadID);

            //Had an issue with trying to get a documentList straight after import
            //and it was null. Seems threadIDToDocs is updated when ePADD starts, so import
            //is not taken into account before re-start. Fix:
            //if documentList returned from threadIDToDocs is null then do the same thing as when threadIDToDocs is null
            //or empty.
            if (documentList != null)
            {
                return documentList;
            }
        }
        threadIDToDocs=new LinkedHashMap<>();
        List<Document> result = new ArrayList<>();
        for (Document ed : allDocs) {
            List<Document> doclist = threadIDToDocs.getOrDefault(((EmailDocument)ed).threadID,new ArrayList<>());
            doclist.add(ed);
            threadIDToDocs.put(((EmailDocument)ed).threadID,doclist);
        }
        return threadIDToDocs.getOrDefault(threadID,new ArrayList<>());
    }

    public String getStats() {
        // note: this is a legacy method that does not use the archivestats
        // object above
        StringBuilder sb = new StringBuilder(allDocs.size() + " original docs with " + ownerEmailAddrs.size() + " email addresses " + ownerNames.size()
                + " names for owner ");
        if (addressBook != null)
            sb.append(addressBook.getStats()).append("\n");
        sb.append(indexer.computeStats()).append("\n").append(getLinks().size()).append(" links");
        return sb.toString();
    }

    /**
     * @return html for the given terms, with terms highlighted by the indexer.
     * if IA_links is set, points links to the Internet archive's version of the page.
     * docId is used to initialize a new view created by clicking on a link within this message,
     * date is used to create the link to the IA
     * @args ldoc - lucene doc corresponding to the content
     * s - content of the doc
     * Date
     * docId - Uniquedocid of the emaildocument
     * highlighttermsUnstemmed - terms to highlight in the content (for ex
     * lexicons)
     * highlighttermsstemmed - entities to highlight, generally are names
     * that one doesn't wish to be stemmed.
     * entitiesWithId - authorisedauthorities, for annotation
     * showDebugInfo - enabler to show debug info
     */
    private String annotate(org.apache.lucene.document.Document ldoc, String s, Date date, String docId, String regexToHighlight, Set<String> highlightTerms,
                            Map<String, EmailRenderer.Entity> entitiesWithId, boolean IA_links, boolean showDebugInfo) {
        getAllDocs();
        try {

            s = Highlighter.getHTMLAnnotatedDocumentContents(this,s, (IA_links ? date : null), docId, regexToHighlight, highlightTerms, entitiesWithId, null /* summarizer.importantTermsCanonical */, false);

            //indexer
            //	.getHTMLAnnotatedDocumentContents(s, (IA_links ? date : null), docId, searchTerms, isRegexSearch, highlightTermsStemmed, highlightTermsUnstemmed, entitiesWithId);
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("indexer failed to annotate doc contents " + Util.stackTrace(e));
        }

        return s;
    }

    public String annotate(String s, Date date, String docId, String regexToHighlight, Set<String> highlightTerms,
                           Map<String, EmailRenderer.Entity> entitiesWithId, boolean IA_links, boolean showDebugInfo) {
        return annotate(null, s, date, docId, regexToHighlight, highlightTerms,
                entitiesWithId, IA_links, showDebugInfo);
    }

    public String getTextForContents(Document d) throws Exception {
        org.apache.lucene.document.Document ldoc = indexer.getDoc(d);

        String contents = indexer.getContents(d, false);

        contents = Util.removeMetaTag(contents);

        if (ldoc == null) {
            System.err.println("Lucene Doc is null for: " + d.getUniqueId() + " but the content is " + (contents == null ? "null" : "not null"));
            return null;
        }

        //Mask any email address present in the content of the message
        if (ModeConfig.isPublicMode())
            contents = Util.maskEmailDomain(contents);

        return contents;
    }

        public Pair<StringBuilder, Pair<Boolean, Boolean>> getHTMLForContents(Document d, Date date, String docId, String regexToHighlight, Set<String> highlightTerms,
                                                            Map<String, Map<String, Short>> authorisedEntities, boolean IA_links, boolean inFull, boolean showDebugInfo, boolean preserve) throws Exception {
        org.apache.lucene.document.Document ldoc = indexer.getDoc(d);
        Span[] names = getAllNamesInLuceneDoc(ldoc,true);

        boolean redacted = false;
        String contents = null;

        String redactedContent = ldoc.get("body-preserved");

        // if there is a preservation copy, we assume the email has been redacted
        if (!preserve && redactedContent!= null)
            redacted = true;

        if (preserve)
            contents = redactedContent;

        if (!preserve || contents == null)
           contents = indexer.getContents(d, false);

        //remove meta tags from the body of the message. It was a serious issue #246.
        contents = Util.removeMetaTag(contents);
        Set<String> acrs = Util.getAcronyms(contents);

        if (ldoc == null) {
            System.err.println("Lucene Doc is null for: " + d.getUniqueId() + " but the content is " + (contents == null ? "null" : "not null"));
            return null;
        }

        // Contains all entities and id if it is authorised else null
        Map<String, EmailRenderer.Entity> entitiesWithId = new HashMap<>();
        //we annotate three specially recognized types
        Map<Short,String> recMap = new HashMap<>();
        recMap.put(NEType.Type.PERSON.getCode(),"cp");
        recMap.put(NEType.Type.PLACE.getCode(),"cl");
        recMap.put(NEType.Type.ORGANISATION.getCode(),"co");
        Arrays.stream(names).filter(n -> recMap.keySet().contains(NEType.getCoarseType(n.type).getCode()))
                .forEach(n -> {
                    Set<String> types = new HashSet<>();
                    types.add(recMap.get(NEType.getCoarseType(n.type).getCode()));
                    entitiesWithId.put(n.text, new EmailRenderer.Entity(n.text, authorisedEntities == null ? null : authorisedEntities.get(n), types)); // TOFIX: intellij points out a bug here. authorizedEntities.get(n) will always return false because n is a Span
                });
        acrs.forEach(acr->{
            Set<String> types = new HashSet<>();
            types.add("acr");
            entitiesWithId.put(acr,new EmailRenderer.Entity(acr, authorisedEntities==null?null:authorisedEntities.get(acr),types));
        });

        //Mask any email address present in the content of the message
        if (ModeConfig.isPublicMode())
            contents = Util.maskEmailDomain(contents);

        //don't want "more" button anymore
        String htmlContents;
        if (contents.length() > Config.MAX_TEXT_SIZE_TO_ANNOTATE) // don't try to annotate extraordinarily long messages, probably bad data, as discovered on RF archive
            htmlContents = Util.escapeHTML(contents);
        else
            htmlContents = annotate(ldoc, contents, date, docId, regexToHighlight, highlightTerms, entitiesWithId, IA_links, showDebugInfo);


        StringBuilder sb = new StringBuilder();
        sb.append(htmlContents);

        boolean overflow = false;
        return new Pair<>(sb, new Pair<Boolean, Boolean>(overflow, redacted));
    }

    /* break up docs into clusters, based on existing docClusters
    * Note: Clustering Type MONTHLY and YEARLY not supported*/
    public List<MultiDoc> clustersForDocs(Collection<? extends Document> docs, MultiDoc.ClusteringType ct) {
        //TODO: whats the right thing to do when docClusters is null?
        if (docClusters == null || (ct == MultiDoc.ClusteringType.NONE)) {
            List<MultiDoc> new_mDocs = new ArrayList<>();
            MultiDoc md = new MultiDoc(0,"all");
            docs.forEach(md::add);

            new_mDocs.add(md);
            return new_mDocs;
        }

        Map<Document, Integer> map = new LinkedHashMap<>();
        int i = 0;
        for (MultiDoc mdoc : docClusters) {
            for (Document d : mdoc.docs)
                map.put(d, i);
            i++;
        }

        List<MultiDoc> new_mDocs = new ArrayList<>();
        for (MultiDoc md : docClusters)
            new_mDocs.add(null);

        for (Document d : docs) {
            int x = map.get(d);
            MultiDoc new_mDoc = new_mDocs.get(x);
            if (new_mDoc == null) {
                MultiDoc original = docClusters.get(x);
                new_mDoc = new MultiDoc(original.getUniqueId(), original.description);
                new_mDocs.set(x, new_mDoc);
            }
            new_mDoc.add(d);
        }

        List<MultiDoc> result = new ArrayList<>();
        for (MultiDoc md : new_mDocs)
            if (md != null)
                result.add(md);

        return result;
    }

    public String toString() {
        // be defensive here -- some of the fields may be null
        StringBuilder sb = new StringBuilder();
        if (allDocs != null)
            sb.append("Archive with #docs: ").append(allDocs.size()).append(" address book: ").append(addressBook).append(" ").append(getStats()).append(" ");
        else
            sb.append("Null docs");
        if (indexer != null) {
            if (indexer.stats != null)
                sb.append(Util.fieldsToString(indexer.stats, false));
            else
                sb.append("Null indexer-stats");
        } else
            sb.append("Null indexer");
        return sb.toString();
    }

    //TODO retain only one of the two methods below
    public org.apache.lucene.document.Document getLuceneDoc(String docId) throws IOException {
        return indexer.getLDoc(docId);
    }

    public org.apache.lucene.document.Document getLuceneDoc(String docId, Set<String> fieldsToLoad) throws IOException {
        return indexer.getLDoc(docId, fieldsToLoad);
    }

    private Set<String> getNames(edu.stanford.muse.index.Document d, Indexer.QueryType qt)
    {
        try {
            return new LinkedHashSet<>(getNamesForDocId(d.getUniqueId(), qt));
        } catch (Exception e) {
            Util.print_exception(e, log);
            return new LinkedHashSet<>();
        }
    }



    /** returns all entities in the given doc, both in body and subject */
    /*public synchronized Set<String> getEntitiesInDoc(Document d) {
        Set<String> entities = new LinkedHashSet<>();
        Stream.of(getEntitiesInDoc (d, false)).map(Span::getText).forEach(entities::add);
        Stream.of(getEntitiesInDoc (d, true)).map(Span::getText).forEach(entities::add);
        return entities;
    }*/

    /*public synchronized Set<String> getAllEntities() {

        if (allEntities == null) {
            allEntities = new LinkedHashSet<>();
            for (Document d : getAllDocs()) {
                try {
                    Stream.of(getEntitiesInDoc(d,true)).map(Span::getText).forEach(allEntities::add);
                } catch (Exception e) {
                    Util.print_exception("exception reading fine grained entities", e, log);
                }
            }
        }
        return allEntities;
    }*/

    /*transient private Multimap<Short, Document> entityTypeToDocs = LinkedHashMultimap.create(); // entity type code -> docs containing it
    public synchronized void computeEntityTypeToDocMap() {
        if (entityTypeToDocs != null)
            return;
        entityTypeToDocs = LinkedHashMultimap.create();
        for(Document doc: this.getAllDocs()){
            Span[] es = this.getEntitiesInDoc(doc,true);
            Set<Short> seenInThisDoc = new LinkedHashSet<>(); // type -> docs: one value should contain a doc only once

            double theta = 0.001;
            for(Span sp: es) {
                if (sp.typeScore < theta)
                    continue;
                if (seenInThisDoc.contains (sp.type))
                    continue;
                seenInThisDoc.add (sp.type);

                entityTypeToDocs.put (sp.type, doc);
            }
        }
    }*/

    /*public synchronized Collection<Document> getDocsWithEntityType(short code) {
        return entityTypeToDocs.get (code);
    }*/

    /*public synchronized Set<NEType.Type> getEntityTypes() {
        Set<NEType.Type> result = new LinkedHashSet<>();

        computeEntityTypeToDocMap();
        for (short t: entityTypeToDocs.keys()) {
            result.add (NEType.getTypeForCode(t));
        }
        return result;
    }*/

    //returns a map of names recognised by openNLPNER to frequency
    /*private Map<String, Integer> countNames() {
        Map<String, Integer> name_count = new LinkedHashMap<>();
        for (Document d : getAllDocs()) {
            Set<String> names = getNames(d, Indexer.QueryType.FULL);
            // log.info("Names = " + Util.joinSort(names, "|"));
            for (String n : names) {
                n = n.trim();
                if (n.length() == 0)
                    continue;
                if (name_count.containsKey(n))
                    name_count.put(n, name_count.get(n) + 1);
                else
                    name_count.put(n, 1);
            }
        }

        // for (Map.Entry<String, Integer> e : entries) {
        // log.info("NameCount:" + e.getKey() + "|" + e.getValue());
        // }
        return name_count;
    }*/

    public List<String> getNamesForDocId(String id, Indexer.QueryType qt) throws IOException
    {
        return indexer.getNamesForDocId(id, qt);
    }

    public List<List<String>> getAllNames(Collection<String> ids, Indexer.QueryType qt) throws IOException
    {
        List<List<String>> result = new ArrayList<>();
        for (String id : ids)
            result.add(getNamesForDocId(id, qt));
        return result;
    }

    /**
     * Assign Ids to threads, can help in making out if two emails belong to the same thread
     * Subject/Title of the a message can also be used for the same purpose
     * @return the maximum thread id value assignbed to any thread in th arhchive*/
    public int assignThreadIds() {
        Collection<Collection<EmailDocument>> threads = EmailUtils.threadEmails((Collection) allDocs);
        int thrId = 1; // note: valid thread ids must be > 1
        for (Collection<EmailDocument> thread : threads) {
            for (EmailDocument doc : thread)
                doc.threadID = thrId;
            thrId++;
        }
        //also do the caching here only.
        docsWithThreadId(1);//to trigger caching of doctothreadid map
        return thrId;
    }

    public void postDeserialized(String baseDir, boolean readOnly) throws IOException {

        epaddPremis = EpaddPremis.readPremisObject(this, baseDir, BAG_DATA_FOLDER);
        //this.collectionMetadata = ArchiveReaderWriter.readCollectionMetadata(baseDir);
        log.info(indexer.computeStats());

        indexer.setBaseDir(baseDir+File.separatorChar + Archive.BAG_DATA_FOLDER+ File.separatorChar);
        openForRead();

        if (!readOnly)
            indexer.setupForWrite();
        if (addressBook != null) {
            // addressBook.reassignContactIds();
            addressBook.organizeContacts(); // is this idempotent?
        }

        if (lexiconMap == null) {
            updateOrCreateLexiconMap();
        }

        // recompute... sometimes the processing metadata may be stale, because some messages have been redacted at export.
      //  collectionMetadata.numPotentiallySensitiveMessages = numMatchesPresetQueries();
    }

    /*
    Class for storing the merge result data
     */
    public class MergeResult{
        public int nMessagesInCollection=0;
        public int nAttachmentsInCollection=0;
        public int nMessagesInAccession=0;
        public int nAttachmentsInAccession=0;
        public int nCommonMessages=0;
        public int nFinalMessages=0;
        public int nFinalAttachments=0;
        public String accessionDir;
        ////For AddressBook Merge report
        public AddressBook.MergeResult addressBookMergeResult;
        ///For LabelManager Merge report
        public LabelManager.MergeResult labManagerMergeResult;
        ///For Lexicon merge report
        public Set<String> clashedLexicons;
        public Set<String> newLexicons;
    }

    //////////////////////////Data fields for accessionmerging///////////////////////////
    private transient MergeResult lastMergeResult;
    private Map<String,String> docIDToAccessionID;
    public String baseAccessionID;//This represents the accession ID when an accession is imported
    //to an empty collection. It is used for ensuring a manageable size of docToAccessionMap for common case
    //of single accession archives. While getting to know about the accessionID of a doc if it is not found
    //in the map docToAccessionMap then the ID is assumed to be baseAccessionID. Note that, this id is set
    //from the accession ID of the first accession imported in an empty collection.

    public MergeResult getLastMergeResult(){
        return lastMergeResult;
    }
    public Map<String,String> getDocIDToAccessionID(){
        if(docIDToAccessionID ==null) {
            docIDToAccessionID = new LinkedHashMap<>();
            return docIDToAccessionID;
        }
        else
            return docIDToAccessionID;
    }

    //Postcondition: variable MergeResult is set
    public void merge(Archive other, String accessionID){
        MergeResult result = new MergeResult();
        /////////////////////////////INDEX MERGING AND DOCUMENT COPYING//////////////////////////
        //for each mail document in other process it only if it is not present in this archive.
        //if not present then call indexer's method to add doc and associated attachments to this index.
        result.nMessagesInAccession = other.getAllDocs().size();
        result.nAttachmentsInAccession = other.blobStore.uniqueBlobs.size();
        result.nMessagesInCollection = getAllDocs().size();
        result.nAttachmentsInCollection = blobStore.uniqueBlobs.size();
        result.accessionDir = other.baseDir;
        for(Document doc: other.getAllDocs()) {
            if (!getAllDocs().contains(doc)) {
                EmailDocument edoc = (EmailDocument) doc;
                try {
                    getAllDocs().add(doc);
                    getAllDocsAsSet().add(doc);
                    //add a field called accession id to these documents.
                    getDocIDToAccessionID().put(edoc.getUniqueId(),accessionID);
                    indexer.moveDocAndAttachmentsToThisIndex(other.indexer, edoc,other.getBlobStore(),blobStore);
                } catch (IOException e) {
                    log.warn("Unable to copy document with signature" + ((EmailDocument) doc).getSignature() + " from the incoming archive to this archive ");
                    e.printStackTrace();
                }
            }else
                result.nCommonMessages+=1;
        }
        try {
            indexer.commitAfterAddingDocs();
        } catch (IOException e) {
            log.warn("Some exception in committing the index after merging");
        }

        //indexer.close();//to commit the changes to disc so that the next time indexer is read the updated stuff is read
        /*//pack destbloblstore.
        try {
            blobStore.pack();
        } catch (IOException e) {
            log.warn("Unable to pack blobstore: Serious error");
        }*/
        result.nFinalMessages = getAllDocs().size();
        result.nFinalAttachments = blobStore.uniqueBlobs.size();
        ///////////////////////Address book merging////////////////////////////////////////////////
        result.addressBookMergeResult = addressBook.merge(other.getAddressBook());
        ///////////////////////Label Manager merging///////////////////////////////////////////////
        result.labManagerMergeResult = labelManager.merge(other.getLabelManager());
        ///////////////////////Entity book merging/////////////////////////////////////////////////


        ////////////////////Lexicon merging///////////////////////////////////////////////////////
        //For merging lexicons copy those lexicon files from other's lexicon directory which are not
        //present in this archive's lexicon directory.Report the names of the files which are present
        //and the number of new files imported successfully from other archive.
        String otherLexDir = other.baseDir + File.separatorChar +Archive.BAG_DATA_FOLDER + File.separatorChar + LEXICONS_SUBDIR;
        String thisLexDir = baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + LEXICONS_SUBDIR;
        File otherlexDirFile = new File(otherLexDir);
        result.newLexicons = new LinkedHashSet<>();
        result.clashedLexicons = new LinkedHashSet<>();
        try {
            updateOrCreateLexiconMap();
            Map<String,Lexicon> collectionLexiconMap = lexiconMap;
            if (!otherlexDirFile.exists()) {
                log.warn("'lexicons' directory is missing from the accession");
            } else {
                for (File f : otherlexDirFile.listFiles(new Util.MyFilenameFilter(null, Lexicon.LEXICON_SUFFIX))) {
                    String name = Lexicon.lexiconNameFromFilename(f.getName());
                    if (!collectionLexiconMap.containsKey(name.toLowerCase())) {
                        //means collection does not have any lexicon of this name. copy it to thisLexDir and report it
                        Util.copy_file(f.getAbsolutePath(),thisLexDir+File.separatorChar+f.getName());
                        result.newLexicons.add(name);
                    }else{
                        //means there is a clash on lexicon names. Report it and dont' copy.
                        result.clashedLexicons.add(name);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Unable to merge lexicon map");
        }

        //save result for future reference.
        lastMergeResult = result;

    }
//    public void merge(Archive other) {
//        /* originalContentOnly */
//        other.getAllDocs().stream().filter(doc -> !this.containsDoc(doc)).forEach(doc -> this.addDoc(doc, other.getContents(doc, /* originalContentOnly */false)));
//
//        addressBook.merge(other.addressBook);
//        this.collectionMetadata.merge(other.collectionMetadata);
//    }

    public void removeFromLexiconMap(String lexiconName)
    {
        lexiconMap.remove(lexiconName);
    }

    /*IMP: If there were two lexicons with same name but different languages then the following method will pick only the first one
    So, how do we support a mix language archive?
     */
    public void updateOrCreateLexiconMap() throws IOException {
        String lexDir = baseDir+File.separatorChar + Archive.BAG_DATA_FOLDER+ File.separatorChar + LEXICONS_SUBDIR;
        if (lexiconMap == null)
        {
            lexiconMap = new HashMap<String, Lexicon>();
        }
        File lexDirFile = new File(lexDir);
        if (!lexDirFile.exists()) {
            log.warn("'lexicons' directory is missing from archive");
        } else {
            for (File f : lexDirFile.listFiles(new Util.MyFilenameFilter(null, Lexicon.LEXICON_SUFFIX))) {
                String name = Lexicon.lexiconNameFromFilename(f.getName());
                if (!lexiconMap.containsKey(name.toLowerCase())) {
                    lexiconMap.put(name.toLowerCase(), new Lexicon(lexDir, name));
                }
            }
        }
    }

    public Lexicon getLexicon(String lexName) {
        // lexicon map could be stale, re-read it
        try {
            if(Util.nullOrEmpty(lexiconMap) || lexiconMap.get(lexName.toLowerCase())==null)
                updateOrCreateLexiconMap();
        } catch (Exception e) {
            Util.print_exception("Error trying to read list of lexicons", e, log);
        }
        return lexiconMap.get(lexName.toLowerCase());
    }

    public Set<String> getAvailableLexicons() {
        // lexicon map could be stale, re-read it
        try {
            if(Util.nullOrEmpty(lexiconMap))
                updateOrCreateLexiconMap();
        } catch (Exception e) {
            Util.print_exception("Error trying to read list of lexicons", e, log);
        }
        if (lexiconMap == null)
            return new LinkedHashSet<>();
        return Collections.unmodifiableSet(lexiconMap.keySet());
    }

    public JSONArray getAvailableLexiconsWithCategories(boolean isDelivery) {
        // lexicon map could be stale, re-read it
        JSONArray resultArray = new JSONArray();
        try {
            if(Util.nullOrEmpty(lexiconMap))
                updateOrCreateLexiconMap();
        } catch (Exception e) {
            Util.print_exception("Error trying to read list of lexicons", e, log);
        }
        if (lexiconMap == null)
            return resultArray;
        if(isDelivery){
            //remove sensitive lexicon.
            lexiconMap.remove(Lexicon.SENSITIVE_LEXICON_NAME);
        }
        int count = 0;
        for (String lexiconname: lexiconMap.keySet()) {
            Lexicon lexicon = lexiconMap.get(lexiconname);
            JSONArray result = new JSONArray();
            //what if the lexicon is not of english?? We assume that in one lexicon file there should be only one language.
            //So instead of relying on english
            Lexicon.Lexicon1Lang lex = lexicon.getLexiconForLanguage(lexicon.getLexiconLanguage());

            int numcategories = lex.captionToExpandedQuery.keySet().size();

            result.put (0, lexiconname);
            result.put (1, numcategories);
            resultArray.put (count++, result);
        }
        return resultArray;
    }
    /** returns an array (sorted by doc count for set labels.
     *  each element of the array is itself an array corresponding to the details for one label.
     *  important: labels with 0 count should not be returned. */
    public JSONArray getLabelCountsAsJson(Collection<Document> docs) {
        Map<String, Integer> labelIdToCount = new LinkedHashMap<>();

        for (Label label: getLabelManager().getAllLabels()) {
            labelIdToCount.put (label.getLabelID(), 0);
        }

        for (Document d: docs) {
            Set<String> labelIds = labelManager.getLabelIDs(d.getUniqueId());
            for (String labelId: labelIds) {
                Integer I = labelIdToCount.getOrDefault (labelId, 0);
                labelIdToCount.put (labelId, I+1);
            }
        }

        // sort by count
        List<Pair<String, Integer>> pairs = Util.sortMapByValue(labelIdToCount);

        // assemble the result json object
        int count = 0;
        JSONArray resultArray = new JSONArray();
        for (Pair<String, Integer> p: pairs) {
            String labelId = p.getFirst();

            JSONArray array = new JSONArray();

            Integer docCount = p.getSecond();
            Label label = labelManager.getLabel(labelId);
            array.put (0, labelId);
            array.put (1, label.getLabelName());
            array.put (2, label.getDescription());
            array.put (3, docCount);
            array.put (4, label.isSysLabel());
            //String labelTypeDescription = LabelManager.LabType.RESTRICTION.equals(label.getType()) ? "Restriction" : "General";
            String labelTypeDescription = "General";
            if (LabelManager.LabType.RESTRICTION.equals(label.getType()))
                labelTypeDescription = "Restriction";
            else if (LabelManager.LabType.PERMISSION.equals(label.getType()))
                labelTypeDescription = "Permission";

            if (LabelManager.LabType.RESTRICTION.equals(label.getType()) && LabelManager.RestrictionType.RESTRICTED_FOR_YEARS.equals (label.getRestrictionType())) {
                labelTypeDescription += " for " + Util.pluralize(label.getRestrictedForYears(), "year");
            } else if (LabelManager.LabType.RESTRICTION.equals(label.getType()) && LabelManager.RestrictionType.RESTRICTED_UNTIL.equals (label.getRestrictionType())) {
                long time = label.getRestrictedUntilTime();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                String description = sdf.format(new Date(time));
                labelTypeDescription += " until " + description;
            }
            array.put (5, labelTypeDescription);

            resultArray.put (count++, array);
        }

        return resultArray;
    }

    public void addStats(FetchStats as) {
        allStats.add(as);
    }

    public Collection<String> getDataErrors() {
        Collection<String> result = new LinkedHashSet<>();

        for (FetchStats as : allStats) {
            Collection<String> asErrors = as.dataErrors;
            if (asErrors != null)
                result.addAll(asErrors);
        }

        return result;
    }

    /**Replaces the document in the index with the supplied document*/
    public void updateDocument(org.apache.lucene.document.Document doc) {
        indexer.updateDocument(doc);
    }

    public void setupForWrite() throws IOException{
        indexer.setupForWrite();
    }

    public Span[] getOriginalNamesOfATypeInDoc(edu.stanford.muse.index.Document doc, short type) throws IOException{
        Span[] spans = getAllOriginalNamesInDoc(doc);
        return Arrays.stream(spans).filter(sp -> sp.type == type).toArray(Span[]::new);
    }

    private Span[] getAllOriginalNamesInDoc(edu.stanford.muse.index.Document doc) throws IOException{
        Span[] spans = getAllNamesInDoc(doc, true);
        String oc = getContents(doc, true);
        return Arrays.stream(spans).filter(sp -> sp.end < oc.length()).toArray(Span[]::new);
    }

    /**@return a list of names filtered to remove dictionary matches*/
    public Span[] getNamesOfATypeInDoc(edu.stanford.muse.index.Document d, boolean body, short type) throws IOException{
        return getNamesOfATypeInLuceneDoc(getLuceneDoc(d.getUniqueId()), body, type);
    }

    /**@return list of all names in the lucene doc without filtering dictionary words*/
    private static Span[] getNamesOfATypeInLuceneDoc(org.apache.lucene.document.Document ldoc, boolean body, short type) {
        Span[] allNames = NER.getNames(ldoc, body);
        return Arrays.stream(allNames).filter(s -> type == s.type).toArray(Span[]::new);
    }

    public Span[] getAllNamesInDoc(edu.stanford.muse.index.Document d, boolean body) throws IOException{
        return NER.getNames(d, body, this);
    }

    public static Span[] getAllNamesInLuceneDoc(org.apache.lucene.document.Document ldoc, boolean body){
        return NER.getNames(ldoc, body);
    }

    public Span[] getAllNamesMapToInDoc(edu.stanford.muse.index.Document d, boolean body, short coarseType) throws IOException{
        Span[] allNames = getAllNamesInDoc(d, body);
        return Arrays.stream(allNames).filter(n -> NEType.getCoarseType(n.type).getCode() == coarseType).toArray(Span[]::new);
    }

    /**@return list of all email sources */
    public synchronized Set<String> getAllEmailSources() {
        if (allEmailSources == null) {
            allEmailSources = new LinkedHashSet<>();
            Collection<EmailDocument> docs = (Collection) getAllDocs();
            for (EmailDocument d : docs)
                if (!Util.nullOrEmpty(d.emailSource))
                    allEmailSources.add(d.emailSource);

        }
        return allEmailSources;
    }

    /**@return list of all email sources */
    public synchronized Set<String> getAllFolders() {
        if (allFolders == null) {
            allFolders = new LinkedHashSet<>();
            Collection<EmailDocument> docs = (Collection) getAllDocs();
            for (EmailDocument d : docs)
                if (!Util.nullOrEmpty(d.folderName))
                    allFolders.add(d.folderName);
        }
        return allFolders;
    }

    // invalidate the cache of all annotations. this should be called any time an annotation changes
    public void clearAllAnnotationsCache() {
        allAnnotations = null;
    }

    /**@return list of all annotations */
    public synchronized Set<String> getAllAnnotations() {
        if (allAnnotations == null) {

            allAnnotations = new LinkedHashSet<>();
            for (Document d : getAllDocs()) {
                String annotations = getAnnotationManager().getAnnotation(d.getUniqueId());
                if (!Util.nullOrEmpty(annotations))
                    allAnnotations.add(annotations);
            }
        }
        return allAnnotations;
    }

    /**@return list of all email sources */
    public synchronized Set<String> getAllBlobNames() {
        if (allBlobNames == null) {
            allBlobNames = new LinkedHashSet<>();
            Collection<EmailDocument> docs = (Collection) getAllDocs();
            for (EmailDocument d : docs)
                if (!Util.nullOrEmpty(d.attachments)) {
                    List<Blob> blobs = d.attachments;
                    for (Blob b : blobs) {
                        if (!Util.nullOrEmpty(this.getBlobStore().full_filename_normalized(b,false)))
                            allBlobNames.add(this.getBlobStore().full_filename_normalized(b,false));
                    }
                }
        }
        return allBlobNames;
    }


    /**
     * Recognises names in the supplied text with OpenNLP openNLPNER
     * @Deprecated
     */
    @Deprecated public static Set<String> extractNamesOpenNLP(String text) throws Exception {
        List<Pair<String, Float>> pairs = edu.stanford.muse.index.openNLPNER.namesFromText(text);
        Set<String> names = new LinkedHashSet<>();
        for (Pair<String, ?> p : pairs)
            names.add(p.getFirst());

        return Util.scrubNames(names);
    }

   /* //maps locId (int) to docId (edu.stanford.muse.index.Document) using indexer storage structures
    public Integer getLDocIdForContentDocId(String docId){
        return indexer.contentDocIds.entrySet().stream().filter(e->docId.equals(e.getValue())).findAny().map(Map.Entry::getKey).orElse(0);
    }

    public String getDocIdForContentLDocId(Integer ldocId){
        return indexer.contentDocIds.get(ldocId);
    }*/

    /*public Integer getLDocIdForBlobDocId(String docId){
        return indexer.blobDocIds.entrySet().stream().filter(e->docId.equals(e.getValue())).findAny().map(Map.Entry::getKey).orElse(0);
    }*/

    /*public String getDocIdForBlobLDocId(Integer ldocId) {
        return indexer.blobDocIds.get(ldocId);
    }
*/
    //input is a bag present in userdir
    public static Bag readArchiveBag(String userdir) {
        Path rootDir = Paths.get(userdir);
        BagReader reader = new BagReader();
        String errorMessage = "";
        Bag bag = null;
        try {
            bag = reader.read(rootDir);
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = "Could not read directory " + rootDir;
        } catch (UnparsableVersionException e) {
            e.printStackTrace();
            errorMessage = "Bag is of unparsable version: " + e.getMessage();
        } catch (MaliciousPathException e) {
            e.printStackTrace();
            errorMessage = "Malicious path found for the bag: " + e.getMessage();
        } catch (UnsupportedAlgorithmException e) {
            e.printStackTrace();
            errorMessage = "The bag has checksums for unsupported algorithms: " + e.getMessage();
        } catch (InvalidBagitFileFormatException e) {
            e.printStackTrace();
            errorMessage = "The file format of the bag is invalid :" + e.getMessage();
        }

       /* BagVerifier bv = new BagVerifier(Executors.newSingleThreadExecutor());
        try {
            bv.isValid(bag, true);
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = "IO Exception:" + e.getMessage();
        } catch (MissingPayloadManifestException e) {
            e.printStackTrace();
            errorMessage = "Payload manifest missing: " + e.getMessage();
        } catch (MissingBagitFileException e) {
            e.printStackTrace();
            errorMessage = "Bagit file missing: " + e.getMessage();
        } catch (MissingPayloadDirectoryException e) {
            e.printStackTrace();
            errorMessage = "Payload directory missing: " + e.getMessage();
        } catch (FileNotInPayloadDirectoryException e) {
            e.printStackTrace();
            errorMessage = "No file in payload directory: " + e.getMessage();
        } catch (InterruptedException e) {
            e.printStackTrace();
            errorMessage = "Validation procedure interrupted :" + e.getMessage();
        } catch (MaliciousPathException e) {
            e.printStackTrace();
            errorMessage = "Malicious Path found: " + e.getMessage();
        } catch (CorruptChecksumException e) {
            e.printStackTrace();
            errorMessage = "Checksum is corrupted: " + e.getMessage();
        } catch (VerificationException e) {
            e.printStackTrace();
            errorMessage = "Can not verify the bag: " + e.getMessage();
        } catch (UnsupportedAlgorithmException e) {
            e.printStackTrace();
            errorMessage = "Algorithm used for constructing the bag is not supported: " + e.getMessage();
        } catch (InvalidBagitFileFormatException e) {
            e.printStackTrace();
            errorMessage = "Bag file format is invalid: " + e.getMessage();
        }*/
        // If error in verification return null;
        if (!Util.nullOrEmpty(errorMessage))
            return null;
        else
            return bag;
    }
/* //Read the archive
        try {
            SimpleSessions.readArchiveIfPresent(userdir);
        } catch (IOException e) {
            errorMessage=e.getMessage();
            return null;
        }*//*

        return bag;


    }

    public static void saveArchiveBag(String destDir){
        //first call SimpleSession saveArchive (destDir+"/data")

        //remove bag metadata from destDir (manifest*.txt, bagit.txt etc)


        //then create
    }
*/

// 2022-10-13
    //needed to make this method because sometime we want to update a bag without loading the whole archive.
    public static void updateFileInBag(Bag archiveBag, String fileOrDirectoryName, String baseDir){
        String[] manifestinfofilename = {"manifest-md5.txt", "manifest-sha256.txt"};
        String[] algorithms = {"MD5", "SHA-256"};
        Path filepathname = Paths.get(fileOrDirectoryName);
        Path baginfofile = Paths.get(baseDir+File.separatorChar+"bag-info.txt");
//        Path manifestinfofile = Paths.get(baseDir +File.separatorChar+"manifest-md5.txt");
        Path[] manifestinfofile = new Path[2];
        for (int i=0; i<2; i++) manifestinfofile[i] = Paths.get(baseDir +File.separatorChar+manifestinfofilename[i]);
		
        //updatePayloadManifests(bag, algorithms, includeHidden);
//        MessageDigest messageDigest = null;
//        Map<Manifest, MessageDigest> manifestToMessageDigest= new HashMap<>();
        MessageDigest[] messageDigest = {null, null};
        Map<Manifest, MessageDigest>[] manifestToMessageDigest = new Map[2];
        manifestToMessageDigest[0] = new HashMap<>();
        manifestToMessageDigest[1] = new HashMap<>();
        boolean includeHiddenFiles = false;
        //updateMetadataFile(bag, metadata);

        final String payloadOxum;
        try {
            payloadOxum = PathUtils.generatePayloadOxum(PathUtils.getDataDir(archiveBag));
            archiveBag.getMetadata().upsertPayloadOxum(payloadOxum);
            archiveBag.getMetadata().remove("Bagging-Date"); //remove the old bagging date if it exists so that there is only one
            archiveBag.getMetadata().add("Bagging-Date", new SimpleDateFormat().format(new Date()));
            MetadataWriter.writeBagMetadata(archiveBag.getMetadata(), archiveBag.getVersion(), PathUtils.getBagitDir(archiveBag), archiveBag.getFileEncoding());
        } catch (IOException e) {
            e.printStackTrace();
        }

        MessageDigest[] finalMessageDigest = new MessageDigest[2];
        try {
            for (int i=0; i<2; i++) {
                messageDigest[i] = MessageDigest.getInstance(algorithms[i]);
                finalMessageDigest[i] = messageDigest[i];
            }    
            /*A very subtle bug was introduced. The case is as following; a file gets deleted from the directory but the manifest has entry for the deleted file as well.
            When the manifest construction algorithm is iterated over the directory tree then it recomputes the hash for all files present but it does not remove the hash for deleted files. As a result,
            although the file is deleted, its entry remains in the tag manifest file resulting in the failure of checksum. For fix; we remove entry for all those files
            which are present in the folder that needs to be updated.
             */
            archiveBag.getPayLoadManifests().forEach(manifest->{
                //from manifest remove all the files whose parent path is fileOrDirectoryName
                Map<Path,String> mm = manifest.getFileToChecksumMap().entrySet().stream().filter(entry->
                    !entry.getKey().startsWith(new File(fileOrDirectoryName).toPath())
                        ).collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
                manifest.setFileToChecksumMap(mm);
//                manifestToMessageDigest.put(manifest, finalMessageDigest);
                if ( manifest.getAlgorithm().toString().equals(algorithms[0])) manifestToMessageDigest[0].put(manifest, finalMessageDigest[0]);
                else manifestToMessageDigest[1].put(manifest, finalMessageDigest[1]);
            });
            CreatePayloadManifestsVistor sut1 = new CreatePayloadManifestsVistor(manifestToMessageDigest[0], includeHiddenFiles);
            Files.walkFileTree(filepathname, sut1);
            CreatePayloadManifestsVistor sut2 = new CreatePayloadManifestsVistor(manifestToMessageDigest[1], includeHiddenFiles);
            Files.walkFileTree(filepathname, sut2);
            archiveBag.getPayLoadManifests().clear();
            archiveBag.getPayLoadManifests().addAll(manifestToMessageDigest[0].keySet());
            archiveBag.getPayLoadManifests().addAll(manifestToMessageDigest[1].keySet());
            ManifestWriter.writePayloadManifests(archiveBag.getPayLoadManifests(), PathUtils.getBagitDir(archiveBag),archiveBag.getRootDir(),archiveBag.getFileEncoding());
			
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //updateTagManifests(bag, algorithms, includeHidden);
        try {
//            MessageDigest finalMessageDigest = messageDigest;
//            manifestToMessageDigest.clear();
             manifestToMessageDigest[0].clear();
             manifestToMessageDigest[1].clear();
//            archiveBag.getTagManifests().forEach(manifest->manifestToMessageDigest.put(manifest, finalMessageDigest));
              archiveBag.getTagManifests().forEach( manifest->{
                        if ( manifest.getAlgorithm().toString().equals(algorithms[0])) manifestToMessageDigest[0].put(manifest, finalMessageDigest[0]);
                        else  manifestToMessageDigest[1].put(manifest, finalMessageDigest[1]);
                    }    
               );
			
            final CreateTagManifestsVistor tagVistor1 = new CreateTagManifestsVistor(manifestToMessageDigest[0], includeHiddenFiles);
            Files.walkFileTree(baginfofile,tagVistor1);
            Files.walkFileTree(manifestinfofile[0],tagVistor1);
            Files.walkFileTree(manifestinfofile[1],tagVistor1);

            final CreateTagManifestsVistor tagVistor2 = new CreateTagManifestsVistor(manifestToMessageDigest[1], includeHiddenFiles);
            //Files.walkFileTree(filepathname, tagVistor);
            Files.walkFileTree(baginfofile,tagVistor2);
            Files.walkFileTree(manifestinfofile[0],tagVistor2);
            Files.walkFileTree(manifestinfofile[1],tagVistor2);
            //update bag'stagemanifest
            archiveBag.getTagManifests().clear();
            archiveBag.getTagManifests().addAll(manifestToMessageDigest[0].keySet());
            archiveBag.getTagManifests().addAll(manifestToMessageDigest[1].keySet());
            ManifestWriter.writeTagManifests(archiveBag.getTagManifests(), PathUtils.getBagitDir(archiveBag), archiveBag.getRootDir(), archiveBag.getFileEncoding());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public boolean lexiconMapContains(String lexiconName)
    {
        return lexiconMap.containsKey(lexiconName);
    }

    public static int getnBlobsExported(Collection<Document> docs, BlobStore blobStore, List<String> attachmentTypeOptions, String dir, boolean unprocessedonly, Set<String> extensionsNeeded, boolean isOtherSelected, Map<Blob, String> blobToErrorMessage) {
        int nBlobsExported = 0;

        for (Document d: docs) {
            EmailDocument ed = (EmailDocument) d;
            List<Blob> blobs = ed.attachments;
            if (Util.nullOrEmpty(blobs))
                continue;

            nextBlob:
            for (Blob blob: blobs) {

                //check if this blob need to be exported or not depending upon the option
                //unprocessedonly and the state of this blob.
                if(unprocessedonly && blob.processedSuccessfully){
                    continue nextBlob;
                }
                try {
                    //String blobName = blobStore.get_URL_Normalized(blob);;
                    String blobName= blobStore.full_filename_normalized(blob,true);
                    // get rid of any file separators first... don't want them to cause any confusion
                    if (blobName == null)
                        blobName = "";


                    blobName = blobName.replaceAll("/", "_");
                    blobName = blobName.replaceAll("\\\\", "_");
                    String base, ext;
                    Pair<String, String> pair = Util.splitIntoFileBaseAndExtension(blobName);
                    base = pair.getFirst();
                    ext = pair.getSecond();
                    Util.ASSERT(Util.nullOrEmpty(ext) ? blobName.equals(base) : blobName.equals(base + "." + ext));

                    if (!Util.nullOrEmpty(extensionsNeeded)) {
                        if (Util.nullOrEmpty(ext))
                            continue nextBlob;
                        //Proceed to add this attachment only if either
                        //1. other is selected and this extension is not present in the list attachmentOptionType, or
                        //2. this extension is present in the variable extensionNeeded
                        //Q. [What if there is a file with .others extension?]
                        boolean firstcondition = isOtherSelected && !attachmentTypeOptions.contains(ext.toLowerCase());
                        boolean secondcondition = extensionsNeeded.contains(ext.toLowerCase());
                        if (!firstcondition && !secondcondition)
                            continue nextBlob;
                    }

                    //remove ':' from the blobname if present otherwise it creates issue on windows which doesn't allow filename to contain ':'.
                    blobName = blobName.replaceAll(":","_");
                    String targetPath = dir + File.separator + blobName;

                    if (new File(targetPath).exists()) {
                        // try adding (1), (2) etc to the base part of the blob name... keep the extension unchanged
                        int i = 1;
                        do {
                            String targetFile = base + " (" + (i++) + ")" + (Util.nullOrEmpty(ext) ? "" : "." + ext);
                            //remove ':' from the targetfile if present.
                            targetFile =targetFile.replaceAll(":","_");
                            targetPath = dir + File.separator + targetFile;
                        } while (new File(targetPath).exists());
                    }
                    blobStore.createBlobCopy(blob, targetPath);
                    nBlobsExported++;

                } catch (Exception e) {
                    Util.print_exception ("Error exporting blob", e, log);
                    blobToErrorMessage.put (blob, "Error exporting blob: " + e.getMessage());
                }
                // blob has the right extensions

            }
        }
        return nBlobsExported;
    }
    public void updateFileInBag(String fileOrDirectoryName, String baseDir) {
        //get bag
        Bag archiveBag = this.getArchiveBag();
        updateFileInBag(archiveBag,fileOrDirectoryName,baseDir);

    }


    /*public JSONArray getEntitiesCountAsJSON(Short entityType,int maxrecords){

        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> canonicalToOriginal = new LinkedHashMap<>();

        double cutoff = 0.001;
        getEntitiesInfo( entityType, counts, canonicalToOriginal, cutoff);
        List<Pair<String, Integer>> pairs = Util.sortMapByValue(counts);
         int count = 0;
        JSONArray resultArray = new JSONArray();
        for (Pair<String, Integer> p: pairs) {
            if (++count > maxrecords)
                break;
            String entity = p.getFirst();
            String entityToPrint = canonicalToOriginal.get(entity);
            JSONArray j = new JSONArray();
            j.put (0, Util.escapeHTML(entityToPrint));
            j.put (1, counts.get(entity));

            resultArray.put (count-1, j);
        }
        return resultArray;
    }

    private void getEntitiesInfo(Short entitiyType, Map<String, Integer> counts, Map<String, String> canonicalToOriginal, double cutoff) {
        Collection<EmailDocument> docs = (Collection) getAllDocs();
        for (EmailDocument ed: docs) {
            Span[] es = getEntitiesInDoc(ed,true);
            List<Span> est = new ArrayList<>();
            for(Span e: es)
                if(NEType.getCoarseType(e.type).getCode() == entitiyType)
                    est.add(e);

            Span[] fes = edu.stanford.muse.ie.Util.filterEntitiesByScore(est.toArray(new Span[est.size()]),cutoff);
            //filter the entities to remove obvious junk
            fes = edu.stanford.muse.ie.Util.filterEntities(fes);
            // note that entities could have repetitions.
            // so we create a *set* of entities, but after canonicalization.
            // canonical to original just uses an arbitrary (first) occurrence of the entity
            Set<String> canonicalEntities = new LinkedHashSet<String>();
            for (Span esp: fes) {
                String e = esp.getText();
                String canonicalEntity = IndexUtils.canonicalizeEntity(e);
                if (canonicalToOriginal.get(canonicalEntity) == null)
                    canonicalToOriginal.put(canonicalEntity, e);
                canonicalEntities.add(canonicalEntity);
            }

            for (String ce: canonicalEntities)
            {
                Integer I = counts.get(ce);
                counts.put(ce, (I == null) ? 1 : I+1);
            }
        }
    }
*/
    public static void main(String[] args) {
        try {

           String userDir = "/Users/tech/" + File.separator + "epadd-appraisal" + File.separator + "user/data/data"+File.separator+"blobs";
            // String userDir = System.getProperty("user.home") + File.separator + "epadd-appraisal" + File.separator + "user";
            //Bag b = Archive.readArchiveBag(userDir);
            StandardSupportedAlgorithms algorithm = StandardSupportedAlgorithms.MD5;
BagCreator.bagInPlace(Paths.get(userDir),Arrays.asList(algorithm),false);
            File tmp = Util.createTempDirectory();
            tmp.delete();
            FileUtils.moveDirectory(Paths.get(userDir+File.separatorChar+Archive.BAG_DATA_FOLDER).toFile(),tmp.toPath().toFile());
            //Files.copy(Paths.get(userDir+File.separatorChar+Archive.BAG_DATA_FOLDER),tmp.toPath(),StandardCopyOption.REPLACE_EXISTING);
            File wheretocopy = Paths.get(userDir).toFile();
            wheretocopy.delete();
            FileUtils.moveDirectory(tmp.toPath().toFile(),wheretocopy);

            //Files.move(,Paths.get(userDir),StandardCopyOption.REPLACE_EXISTING);

            boolean includeHiddenFiles = false;
            //BagCreator.
            Bag bag = BagCreator.bagInPlace(Paths.get(userDir), Arrays.asList(algorithm), includeHiddenFiles);
            //write bag to disc.. in place of userDir..
//            Path outputDir = Paths.get(userDir+"/bag");
//
//            BagWriter.write(bag, outputDir); //where bag is a Bag object

       /*     //   System.out.println(bag.getRootDir().toString());
            Bag b = Archive.readArchiveBag(userDir);
            BagWriter.write(b,Paths.get(userDir));
//update it in place.
//b.getPayLoadManifests().forEach(f->f.getFileToChecksumMap().pu);
            b = Archive.readArchiveBag(userDir);
            BagWriter.write(b,Paths.get(userDir));
*/
          //  System.out.println(b.getRootDir().toString());

            Archive archive = ArchiveReaderWriter.readArchiveIfPresent(userDir);
            //make some modification in the file labelcsv.
            //update file.
            archive.updateFileInBag((userDir+File.separatorChar+"data/sessions"+File.separatorChar+"collection-metadata.json"), userDir);
            //then try to read it again..
            archive = ArchiveReaderWriter.readArchiveIfPresent(userDir);

            List<Document> docs = archive.getAllDocs();
            int i=0;
            archive.assignThreadIds();
            NER.NERStats stats = new NER.NERStats();
            for(Document doc: docs) {
                EmailDocument ed = (EmailDocument) doc;
                stats.update(archive.getAllNamesInDoc(ed, true));
                System.out.println(Arrays.asList(archive.getAllNamesInDoc(ed, true)));
                if(i++>20)
                    break;
//                List<Document> threads = archive.docsWithThreadId(ed.threadID);
//                if(threads.size()>0){
//                    int numSent = 0;
//                    for(Document d: threads){
//                        EmailDocument thread = (EmailDocument)d;
//                        int sent = thread.sentOrReceived(archive.addressBook)&EmailDocument.SENT_MASK;
//                        if(sent>0)
//                            numSent++;
//                    }
//                    if(threads.size()!=numSent || threads.size()>2){
//                        System.err.println("Found a thread with "+numSent+" sent and "+threads.size()+" docs in a thread: "+ed.getSubject());
//                        break;
//                    }
//                    if(i%100 == 0)
//                        System.err.println("Scanned: "+i+" docs");
//                }
//                i++;
            }
            System.out.println(stats.counts);
            System.out.println(stats.all);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/***
	 * Deletes and cleans up lexicons files & directory, as well as indexer cache files.
     * Used primarily for cleaning up unit tests.
	 * @throws IOException
	 */
    public void deleteAndCleanupFiles() throws IOException {
        // Clear out the lexicons directory.
		Arrays.stream(lexiconsDir.listFiles()).forEach(f -> {
			try {
			    f.delete();
			} catch (Exception e) {
				System.out.println("Unable to delete" + f);
			}
		});
        lexiconsDir.delete();

        indexer.deleteAndCleanupFiles();
    }
	
// 2022-09-05
    public boolean isMBOX() {
        if (allDocs == null || allDocs.isEmpty()) return true;
        EmailDocument doc1 = (EmailDocument) allDocs.iterator().next();
//        if (doc1.emailSource.contains("@") && doc1.emailSource.contains("imap")) return false;
//        else return true;
// 2022-10-03
//        if (doc1.emailSource.toLowerCase().contains(".mbox")) return true;

       // emailSource is a text field in email-sources.jsp. Added "mbox" to the beginning of the string for the Import Mbox
        // section in order to make isMbox() working.
    if (doc1.emailSource.toLowerCase().contains("mbox")) return true;
        else return false;
    }
    
//    public String export2mbox(String sourceFile, String targetDir, String targetFilename) {
//        if (sourceFile.contains(EmailConvert.EPADD_EMAILCHEMY_TMP)) {
//            //If we have a non mbox email source then we want to add .mbox to the file name.
//            //If we have an Mbox email source then we keep the original name of the Mbox file
//            //which might or might not have the .mbox ending.
//            if (targetFilename.length() > 4 ) {
//                //The condition should always be true because we just have the name of a folder
//                //in an email account and so no .mbox ending.
//                if (!".mbox".equals(targetFilename.substring(targetFilename.length() - 5))) {
//                    targetFilename = targetFilename + ".mbox";
//                }
//            }
//            else
//            {
//                log.error("Something wrong with targetFileName in export2Mbox(). targetFileName = " + targetFilename);
//            }
//        }
//        String returnCode = "0";
//        String targetFile;
//        if (allDocs == null || allDocs.isEmpty()) {
//            return "4";
//        }
//        if (isMBOX()) {
//            targetFile = targetDir + File.separatorChar + targetFilename;
//            try {
//                Util.copy_file(sourceFile, targetFile);
//            } catch (IOException ioe) {
//                returnCode = "4";
//            }
//        } else {
//            EmailDocument doc1 = (EmailDocument) allDocs.iterator().next();
//            targetFile = targetDir + File.separatorChar + doc1.emailSource + ".mbox";
//            PrintWriter pw = null;
//            try {
//                pw = new PrintWriter(targetFile, "UTF-8");
//                boolean stripQuoted = false;
//                for (Document ed: allDocs)
//                    EmailUtils.printToMbox(this, (EmailDocument) ed, pw, getBlobStore(), stripQuoted);
//            } catch(Exception e){
//                //Util.print_exception ("Error exporting authorities", e, JSPHelper.log);
//                e.printStackTrace();
//                returnCode = "4";
//            } finally {
//                if (pw != null) pw.close();
//            }
//        }
//        return returnCode;
//    }
// 2022-09-05

// 2022-11-09    
    public String getSidecarDir() {
        return baseDir + File.separator + BAG_DATA_FOLDER + File.separator + SIDECAR_DIR;
    }
	
}
