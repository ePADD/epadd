package edu.stanford.muse.index;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.weirdkid.emailchemy.api.Converter;
import com.weirdkid.emailchemy.api.ConverterListener;
import com.weirdkid.emailchemy.api.Format;
import com.weirdkid.emailchemy.api.OutputFormat;
import edu.stanford.muse.Config;
import edu.stanford.muse.LabelManager.LabelManager;
import edu.stanford.muse.email.FolderInfo;
import edu.stanford.muse.email.StaticStatusProvider;
import edu.stanford.muse.email.StatusProvider;
import edu.stanford.muse.epaddpremis.EpaddEvent;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import gov.loc.repository.bagit.domain.Bag;
import groovy.lang.Tuple2;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static edu.stanford.muse.index.Archive.AssetType.*;

public class EmailExporter implements StatusProvider, ConverterListener {


    public static final String EXPORT_APPRAISED_EML = "exportAppraisedEml";
    public static final String EXPORT_PROCESSING = "exportProcessing";
    public static final String EXPORT_APPRAISED_MBOX =  "exportAppraisedMbox";
    public static final String EXPORT_ACCESSION_PROCESSING = "exportAccessionProcessing";
    public static final String EXPORT_ACQUISITIONED = "exportAcquisitioned";
    public static final String EXPORT_PROCESSED_EML = "exportProcessedEml";
    public static final String EXPORT_PROCESSED_MBOX = "exportProcessedMbox";
    private File tmpTargetFolder;
    private CountDownLatch countDownLatchAllEmails;
    private final Archive archive;
    private int nMessageConverted;
    private final Consumer<StatusProvider> setStatusProvider;
    private int nEmails;
    private final EmailFormat outputFormat;
    private String dateTimeStringBeginningOfExport;
    private int nMessageExported;

    public enum EmailFormat {EML, MBOX, ERROR}

    private static final Logger log = LogManager.getLogger(EmailExporter.class);

    public EmailExporter(String exportableAssets, Archive archive, Consumer<StatusProvider> setStatusProvider) {
       if (EXPORT_APPRAISED_EML.equals(exportableAssets) || EXPORT_PROCESSED_EML.equals(exportableAssets)) {
            outputFormat = EmailFormat.EML;
        } else {
            outputFormat = EmailFormat.MBOX;
        }
        this.archive = archive;
        this.setStatusProvider = setStatusProvider;
    }

    private void exportEmails(String targetFolder, boolean includeRestricted, boolean includeDuplicated) {
        if (outputFormat == EmailFormat.EML) {
            tmpTargetFolder = new File(targetFolder + File.separatorChar + "tmp");
            tmpTargetFolder.mkdir();
        }
        // identify total ingested email stores
        Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
        ArrayList<String> folders = new ArrayList<>();
        for (EmailDocument ed : docs) {
            String folder = ed.folderName;
            if (!folders.contains(folder))
                folders.add(folder);
        }

        String emailFolder;
        SearchResult ASearchResult;

        Multimap<String, String> params;
        dateTimeStringBeginningOfExport = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        if (outputFormat == EmailFormat.EML) {
            //Store intermediate Mbox files in tmp directory
            countDownLatchAllEmails = new CountDownLatch(folders.size());
            nMessageConverted = 0;
            new File(targetFolder + File.separatorChar + "exported eml").mkdir();
            new File(targetFolder + File.separatorChar + "exported eml" + File.separatorChar + "eml_" + dateTimeStringBeginningOfExport).mkdir();
        } else if (outputFormat == EmailFormat.MBOX) {
            nMessageExported = 0;
            new File(targetFolder + File.separatorChar + "exported mbox").mkdir();
            new File(targetFolder + File.separatorChar + "exported mbox" + File.separatorChar + "mbox_" + dateTimeStringBeginningOfExport).mkdir();
        }
        nEmails = archive.collectionMetadata.nDocs;
        // for each store, filter out the emailDocument set
        for (String folder : folders) {

            emailFolder = folder;

            // prepare SearchResult object
            params = LinkedHashMultimap.create();

            try {
                params.put("folder", JSPHelper.convertRequestParamToUTF8(emailFolder));
            } catch (UnsupportedEncodingException uee) {
                Util.print_exception("Exception in exportEmails() ", uee, LogManager.getLogger(EmailExporter.class));
                return;
            }

            ASearchResult = new SearchResult(archive, params);

            Pair<Collection<Document>, SearchResult> searchResult = SearchResult.selectDocsAndBlobs(ASearchResult);

            // MBOX files would be reproduced with the same filenames of imported raw files
            String pathToMboxFile;
            if (outputFormat == EmailFormat.MBOX) {
                pathToMboxFile = targetFolder + File.separatorChar + "exported mbox" + File.separatorChar + "mbox_" + dateTimeStringBeginningOfExport + File.separatorChar + Util.filePathTail(emailFolder) + ".mbox";
            } else if (outputFormat == EmailFormat.EML) {
                //If we do export in non Mbox we store the intermediate Mbox files in tmp folder and delete them after the conversion
                pathToMboxFile = targetFolder + File.separatorChar + "tmp" + File.separatorChar + Util.filePathTail(emailFolder);
            } else {
                log.error("unkown output format in EmailExporter. Set to Mbox");
                return;
            }
            PrintWriter pw = null;
            if (outputFormat == EmailFormat.EML) {
                sendStatus("Print " + nEmails + " emails as Mbox files for conversion to EML format");
            } else if (outputFormat == EmailFormat.MBOX) {
                sendStatus("Export " + nEmails + " emails as Mbox files");
            }

            printMboxFiles(includeRestricted, includeDuplicated, emailFolder, searchResult, pathToMboxFile, pw);
            if (outputFormat == EmailFormat.EML) {
                convertToEml(targetFolder, pathToMboxFile);
            }
        }
        if (outputFormat == EmailFormat.EML) {
            //Wait for conversionComplete to be called for each folder.
            try {
                countDownLatchAllEmails.await();
            } catch (InterruptedException e) {
                Util.print_exception("InterruptedException whilst converting emails", e, LogManager.getLogger(EmailExporter.class));
            }
        }
    }

    private void printMboxFiles(boolean includeRestricted, boolean includeDuplicated, String emailFolder, Pair<Collection<Document>, SearchResult> searchResult, String pathToMboxFile, PrintWriter pw) {
        try {
            pw = new PrintWriter(pathToMboxFile, StandardCharsets.UTF_8);

            for (Document d : searchResult.first) {
                EmailDocument ed = (EmailDocument) d;
                // if includeRestricted is set to false, need filter out those labelled with DNT labels
                if (includeRestricted || !archive.getLabelIDs(ed).contains(LabelManager.LABELID_DNT)) {
                    nMessageExported++;
                    if (outputFormat == EmailFormat.MBOX && nMessageExported % 100 == 0) {
                        sendStatus("Exported " + nMessageExported + " out of " + nEmails + " emails.");
                    }
                    EmailUtils.printToMbox(archive, ed, pw, archive.getBlobStore(), false);
                }

            }
            // if includeDuplicated is set to true, need perform deduplication
// 2022-09-09       if (includeDuplicated){
            if (includeDuplicated && archive.getDupMessageInfo() != null) {
                for (Map.Entry<Document, Tuple2<String, String>> entry : archive.getDupMessageInfo().entries()) {
                    Document deduplicate = entry.getKey();
                    Tuple2<String, String> s = entry.getValue();

                    if (emailFolder.equals(s.getFirst())) {
                        System.out.println("generateExportableAssetsNormalizedMbox: Deduplicate for this email document: " + deduplicate.getUniqueId());
                        nMessageExported++;
                        if (outputFormat == EmailFormat.MBOX && nMessageExported % 100 == 0) {
                            sendStatus("Exported " + nMessageExported + " out of " + nEmails + " emails.");
                        }
                        EmailUtils.printToMbox(archive, (EmailDocument) deduplicate, pw, archive.getBlobStore(), false);
                    }
                }
            }


// 2022-09-09        pw.close();

        } catch (Exception e) {
            Util.print_exception("Exception printing Mbox files.", e, LogManager.getLogger(EmailExporter.class));
        } finally {
// 2022-09-09
            if (pw != null) pw.close();
        }
    }

    private void sendStatus(String message) {
        if (setStatusProvider != null) {
            setStatusProvider.accept(new StaticStatusProvider(message));
        }
    }

    private void convertToEml(String targetExportableAssetsFolder, String pathToMboxFile) {
        sendStatus("Converting " + nEmails + " emails to EML format");
        Format inFormat = Format.forName("mbox");
        OutputFormat outFormat = OutputFormat.forName("eml");
        File mboxFile = new File(pathToMboxFile);

        // One Mbox file is one folder, so the mbox file name becomes the eml folder name.
        String emlDirName = Util.filePathTail(pathToMboxFile);

        // Make sure folder exists
        File emlFolder = new File(targetExportableAssetsFolder + File.separatorChar + "exported eml" + File.separatorChar + "eml_" + dateTimeStringBeginningOfExport + File.separatorChar + emlDirName);
        emlFolder.mkdir();
        Converter converter = new Converter(mboxFile, inFormat, emlFolder, outFormat);
        converter.addConverterListener(this);
        //converter.withLogFile("C:\\Users\\jochen\\log.log");
        converter.run();
    }

    public void exportExportableAssets(Archive.AssetType exportableAssets) {
        exportExportableAssets(exportableAssets, false, true, null);
    }

    public void exportExportableAssets(Archive.AssetType exportableAssets, ArrayList<String> sourceAssetsLocations) {
        exportExportableAssets(exportableAssets, false, true, sourceAssetsLocations);
    }

    private void exportExportableAssets(Archive.AssetType exportableAssets, boolean includeRestricted, boolean includeDuplicated, ArrayList<String> folderLocations) {
        String archiveBaseDir;
        boolean isAppraisal = false;
        if (exportableAssets == APPRAISAL_CANONICAL_ACQUISITIONED || exportableAssets == APPRAISAL_NORMALIZED_ACQUISITIONED || exportableAssets == APPRAISAL_NORMALIZED_APPRAISED) {
            archiveBaseDir = Config.REPO_DIR_APPRAISAL + File.separatorChar + "user" + File.separatorChar;
            isAppraisal = true;
        } else {
            String bestName = archive.addressBook.getBestNameForSelf().trim();
            archiveBaseDir = Config.REPO_DIR_PROCESSING + File.separator + "ePADD archive of " + bestName + File.separatorChar;
        }

        //for updating the checksum we need to first read the bag from the basedir..
        Bag archiveBag = Archive.readArchiveBag(archiveBaseDir);

        String targetFolder = archiveBaseDir;
        String mboxFilename;
        Map<String, Boolean> folderIsNonMboxMap;

        switch (exportableAssets) {
            case APPRAISAL_CANONICAL_ACQUISITIONED:
                targetFolder = saveCanonicalPreservation(folderLocations, targetFolder);
                break;

            case APPRAISAL_NORMALIZED_ACQUISITIONED:
                targetFolder = saveNormalisedAcquisitionedPreservation(folderLocations, targetFolder);
                break;

            case APPRAISAL_NORMALIZED_APPRAISED:
                targetFolder = saveAppraisalAppraised(includeRestricted, includeDuplicated, targetFolder);
                break;

            case PROCESSING_NORMALIZED:

                targetFolder = saveProcessed(folderLocations, archiveBaseDir, archiveBag, targetFolder);
                if (targetFolder == null) return;
                break;

            case PROCESSING_NORMALIZED_PROCESSED:
                targetFolder = targetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_PROCESSING_NORMALIZED_PROCESSED_SUBDIR;
                new File(targetFolder).mkdir();
                // generate normalized MBOX email store
                exportEmails(targetFolder, includeRestricted, includeDuplicated);
                break;
        }
        sendStatus("Updating bag");
        // Finally, update BagIt checksum
        archive.updateFileInBag(targetFolder, archive.baseDir);

        String detailInformation = "";
        String outcome = "";
        if (isAppraisal) {
            detailInformation = "Export from appraisal";
        } else {
            detailInformation = "Export from processing";
        }
        if (archive.epaddPremis != null) {
            archive.epaddPremis.createEvent(EpaddEvent.EventType.EXPORT_FOR_PRESERVATION, detailInformation, outcome);
        }
    }

    private String saveProcessed(ArrayList<String> folderLocations, String archiveBaseDir, Bag archiveBag, String targetFolder) {
        String sourceExportableAssetFolder;

        // There are 2 scenarios to be handled. First one is for imported archive collection with default accession.
        // Second one is for accessioned collection with explicitly inputted folder path of sourceAssetFolder.
        if (folderLocations == null) {
            // This case is for imported archive collection with default accession
            System.out.println("imported archive collection with default accession");
            sourceExportableAssetFolder = targetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR;
            //sourceExportableAssetFolder = targetAssetsFolder + BAG_DATA_FOLDER + File.separatorChar + EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + EXPORTABLE_ASSETS_PROCESSING_NORMALIZED_PROCESSED_SUBDIR;
            targetFolder = targetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_PROCESSING_NORMALIZED_SUBDIR;
            final Path sourceFilePath = new File(sourceExportableAssetFolder).toPath();
            final Path targetFilePath = new File(targetFolder).toPath();

            System.out.println("source: " + sourceExportableAssetFolder);
            System.out.println("destination: " + targetFolder);

            if (Files.isDirectory(sourceFilePath)) {
                new File(targetFolder).mkdir();
                try {
                    Util.copyDirectoryFilesIfFilesDoesntExist(sourceExportableAssetFolder, targetFolder);
                } catch (IOException ioe) {
                    Util.print_exception("Exception in setExportableAssets.", ioe, LogManager.getLogger(EmailExporter.class));
                    return null;
                }
            } else {
                System.out.println("source destination NOT exists - 1!!! " + sourceExportableAssetFolder);
                return null;
            }

        } else {
            // This case is for accessioned collection
            System.out.println("import from accession");
            // For each of accession folders, we have to copy:
            // 1. accession's normalized appraised -> collection's processing normalized
            // 2. accession's canonical acquisitioned -> collection's canonical acquisitioned
            // 3. accession's normalized acquisitioned -> collection's normalized acquisitioned
            // 4. accession's normalized appraised -> collection's normalized appraised

            targetFolder = targetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_PROCESSING_NORMALIZED_SUBDIR;
            new File(targetFolder).mkdir();

            // The following target folders are used only by importing new accession
            String targetExportableAssetsFolder2 = "";
            String targetExportableAssetsFolder3 = "";
            String targetExportableAssetsFolder4 = "";

            for (String sourceAssetFolder : folderLocations) {
                // 1. accession's normalized appraised -> collection's processing normalized
                sourceExportableAssetFolder = sourceAssetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR;
                System.out.println("source: " + sourceExportableAssetFolder);
                System.out.println("destination: " + targetFolder);
                final Path sourceFilePath = new File(sourceExportableAssetFolder).toPath();

                if (Files.isDirectory(sourceFilePath)) {
                    try {
                        Util.copy_directory(sourceExportableAssetFolder, targetFolder);
                    } catch (IOException ioe) {
                        Util.print_exception("Exception in setExportableAssets().", ioe, LogManager.getLogger(EmailExporter.class));
                    }
                } else {
                    LogManager.getLogger().warn("source destination does NOT exists!!! " + sourceExportableAssetFolder);
                    return null;
                }

                // 2. accession's canonical acquisitioned -> collection's canonical acquisitioned
                targetExportableAssetsFolder2 = targetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_APPRAISAL_CANONICAL_ACQUISITIONED_SUBDIR;
                new File(targetExportableAssetsFolder2).mkdir();
                sourceExportableAssetFolder = sourceAssetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_APPRAISAL_CANONICAL_ACQUISITIONED_SUBDIR;
                System.out.println("source: " + sourceExportableAssetFolder);
                System.out.println("destination: " + targetExportableAssetsFolder2);
                final Path sourceFilePath2 = new File(sourceExportableAssetFolder).toPath();

                if (Files.isDirectory(sourceFilePath2)) {
                    try {
                        Util.copy_directory(sourceExportableAssetFolder, targetExportableAssetsFolder2);
                    } catch (IOException ioe) {
                        Util.print_exception("Exception in setExportableAssets.", ioe, LogManager.getLogger(EmailExporter.class));
                        return null;
                    }
                } else {
                    log.warn("source destination NOT exists + 3!!! " + sourceExportableAssetFolder);
                    return null;
                }

                // 3. accession's normalized acquisitioned -> collection's normalized acquisitioned
                targetExportableAssetsFolder3 = targetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.ASSETS_APPRAISAL_NORMALIZED_ACQUISITIONED_SUBDIR;
                new File(targetExportableAssetsFolder3).mkdir();
                sourceExportableAssetFolder = sourceAssetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.ASSETS_APPRAISAL_NORMALIZED_ACQUISITIONED_SUBDIR;
                System.out.println("source: " + sourceExportableAssetFolder);
                System.out.println("destination: " + targetExportableAssetsFolder3);
                final Path sourceFilePath3 = new File(sourceExportableAssetFolder).toPath();

                if (Files.isDirectory(sourceFilePath3)) {
                    try {
                        Util.copy_directory(sourceExportableAssetFolder, targetExportableAssetsFolder3);
                    } catch (IOException ioe) {
                        Util.print_exception("Exception in setExportableAssets()", ioe, LogManager.getLogger(EmailExporter.class));
                        return null;
                    }
                } else {
                    log.warn("source destination NOT exists!!! " + sourceExportableAssetFolder);
                    return null;
                }

                // 4. accession's normalized appraised -> collection's normalized appraised
                targetExportableAssetsFolder4 = targetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR;
                new File(targetExportableAssetsFolder4).mkdir();
                sourceExportableAssetFolder = sourceAssetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR;
                System.out.println("source: " + sourceExportableAssetFolder);
                System.out.println("destination: " + targetExportableAssetsFolder4);
                final Path sourceFilePath4 = new File(sourceExportableAssetFolder).toPath();

                if (Files.isDirectory(sourceFilePath4)) {
                    try {
                        Util.copy_directory(sourceExportableAssetFolder, targetExportableAssetsFolder4);
                    } catch (IOException ioe) {
                        Util.print_exception("Exception in setExportableAssets()", ioe, LogManager.getLogger((EmailExporter.class)));
                    }
                    return null;
                } else {
                    log.warn("source destination NOT exists!!! " + sourceExportableAssetFolder);
                    return null;
                }
            }

            Archive.updateFileInBag(archiveBag, targetExportableAssetsFolder2, archiveBaseDir);
            Archive.updateFileInBag(archiveBag, targetExportableAssetsFolder3, archiveBaseDir);
            Archive.updateFileInBag(archiveBag, targetExportableAssetsFolder4, archiveBaseDir);
        }
        return targetFolder;
    }

    private String saveAppraisalAppraised(boolean includeRestricted, boolean includeDuplicated, String targetFolder) {
        // Existence of normalized acquisition MBOX is mandatory for creating normalized appraised
        targetFolder = targetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR;
        new File(targetFolder).mkdir();

        // generate normalized MBOX email store
        exportEmails(targetFolder, includeRestricted, includeDuplicated);
        return targetFolder;
    }

    private String saveNormalisedAcquisitionedPreservation(ArrayList<String> folderLocations, String targetAssetsFolder) {
        Map<String, Boolean> folderIsNonMboxMap;
        String mboxFilename;
        targetAssetsFolder = targetAssetsFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.ASSETS_APPRAISAL_NORMALIZED_ACQUISITIONED_SUBDIR;
        new File(targetAssetsFolder).mkdir();

        //<FolderName, isNonMboxFile>
        folderIsNonMboxMap = new HashMap<>();
        for (int i = 0; i < folderLocations.size(); i++) {
            //For non Mbox files. Generated Mbox file and original non Mbox file separated by ^x^
            //E.g. /tempDir/myEmailFile.mbox^x^/user/john/myEmailFile.pst
            //Files which have imported already being Mbox will be like
            //user/john/myEmailFile.mbox^x^
            String folderPair = folderLocations.get(i);

            //List with maximal two elements. 0: Generated Mbox file. 1: original file
            List<String> pairList = new ArrayList(Arrays.asList(folderPair.split("\\^x\\^")));

            if (pairList.size() > 1) {
                //We have two locations (non mbox location and location of resulting mbox file) therefore it is non Mbox file
                folderIsNonMboxMap.put(folderLocations.get(i), true);
                //folderLocations.set(i, )
            } else {
                //Only one location, so file was imported as Mbox already
                folderLocations.set(i, pairList.get(0));
            }
        }
        try {
            for (String sourceAssetFolder : folderLocations) {
                if (folderIsNonMboxMap.get(sourceAssetFolder) != null && folderIsNonMboxMap.get(sourceAssetFolder)) {
                    //Non Mbox
                    List<String> pair = new ArrayList(Arrays.asList(sourceAssetFolder.split("\\^x\\^")));

                    // We use the name with full path of the non Mbox file as directory name
                    // for storing the generated Mbox files, so users know where the generated Mbox files
                    // come from. E.g. /user/john/myEmails.pst means
                    //we use -user-john-myEmails.pst as directory name.
                    String dirNameTakenFromNonMboxFileName = pair.get(1);

                    //Initialised with the same value as dirNameTakenFromNonMboxFileName but later we will use just the file
                    //name without path for copying the non Mbox file for preservation.
                    String mboxFileNameWithTmpPath = pair.get(0);
                    if (new File(dirNameTakenFromNonMboxFileName).isDirectory())
                    {
                        //The user pointed to a directory rather than a specific file name. We take the name
                        //of the current non mbox file from the tmp path to the generated Mbox file.
                        String nonMboxFileName = FolderInfo.getNonMboxFileNameFromTmpPath(mboxFileNameWithTmpPath);
                        dirNameTakenFromNonMboxFileName = nonMboxFileName;
                    }
                    //Can't have / in the directory name
                    dirNameTakenFromNonMboxFileName = dirNameTakenFromNonMboxFileName.replaceAll("/", "-");
                    dirNameTakenFromNonMboxFileName = dirNameTakenFromNonMboxFileName.replaceAll("\\\\", "-");

                    dirNameTakenFromNonMboxFileName = dirNameTakenFromNonMboxFileName.replaceAll(":", "");

                    File dirWithNameTakenFromNonMboxFileName = new File(targetAssetsFolder + File.separatorChar + Archive.MBOX_FILES_GENERATED_WITH_EMAILCHEMY);
                    dirWithNameTakenFromNonMboxFileName.mkdir();
                    dirWithNameTakenFromNonMboxFileName = new File(targetAssetsFolder + File.separatorChar + Archive.MBOX_FILES_GENERATED_WITH_EMAILCHEMY + File.separatorChar + dirNameTakenFromNonMboxFileName);
                    dirWithNameTakenFromNonMboxFileName.mkdir();
                    mboxFilename = Util.filePathTail(mboxFileNameWithTmpPath);
                    Util.copy_file(mboxFileNameWithTmpPath, dirWithNameTakenFromNonMboxFileName.toString() + File.separatorChar + mboxFilename);
                } else {
                    if (sourceAssetFolder.contains("^x^")) {
                        sourceAssetFolder = sourceAssetFolder.substring(0, sourceAssetFolder.indexOf("^x^"));
                    }
                    mboxFilename = Util.filePathTail(sourceAssetFolder);
                    new File(targetAssetsFolder + File.separatorChar + Archive.UNTOUCHED_IMPORTED_MBOX_FILES).mkdir();
                    Util.copy_file(sourceAssetFolder, targetAssetsFolder + File.separatorChar + Archive.UNTOUCHED_IMPORTED_MBOX_FILES + File.separatorChar + mboxFilename);

                }
            }
        } catch (IOException ioe) {
            Util.print_exception("IOException saving files for preservation", ioe, LogManager.getLogger(EmailUtils.class));
            System.out.println("IOException saving files for preservation " + ioe);
        }

/* 2022-09-05
                try {
                    for (String sourceAssetFolder : folderLocations) {
                        mboxFilename = Util.filePathTail(sourceAssetFolder);
                        Util.copy_file(sourceAssetFolder, targetAssetsFolder + File.separatorChar + mboxFilename);
                    }
                } catch (IOException ioe) {
                    returnCode = "4";
                    returnMessage = "Real time error happened during normalization process: unexpected error is found during copying files";
                }
*/
// 2022-09-05 Added handling for IMAP
//        for (String sourceAssetFolder : folderLocations) {
//            mboxFilename = Util.filePathTail(sourceAssetFolder);
//            //sort this out
//            archive.export2mbox(sourceAssetFolder, targetAssetsFolder, mboxFilename);
//        }
        return targetAssetsFolder;
    }

    private String saveCanonicalPreservation(ArrayList<String> folderLocations, String targetFolder) {
        String mboxFilename;
        targetFolder = targetFolder + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separatorChar + Archive.EXPORTABLE_ASSETS_APPRAISAL_CANONICAL_ACQUISITIONED_SUBDIR;
        new File(targetFolder).mkdir();

        //<FolderName, isNonMboxFile>
        Map<String, Boolean> folderIsNonMboxMap = new HashMap<>();

        for (int i = 0; i < folderLocations.size(); i++) {
            //For non Mbox files. Generated Mbox file and original non Mbox file separated by ^x^
            //E.g. /tempDir/myEmailFile.mbox^x^/user/john/myEmailFile.pst
            //Files which have imported already being Mbox will be like
            //user/john/myEmailFile.mbox^x^
            String folderPair = folderLocations.get(i);

            //List with maximal two elements. 0: Generated Mbox file. 1: original file
            List<String> pairList = new ArrayList(Arrays.asList(folderPair.split("\\^x\\^")));
            if (pairList.size() > 1) {
                //We have two locations (non mbox location and location of resulting mbox file) therefore it is non Mbox file
                folderIsNonMboxMap.put(folderLocations.get(i), true);
            } else {
                //Only one location, so file was imported as Mbox already
                folderLocations.set(i, pairList.get(0));
            }
        }

        try {
            for (String sourceAssetFolder : folderLocations) {
                if (folderIsNonMboxMap.get(sourceAssetFolder) != null && folderIsNonMboxMap.get(sourceAssetFolder)) {
                    //We want to copy the non Mbox file

                    List<String> pair = new ArrayList(Arrays.asList(sourceAssetFolder.split("\\^x\\^")));

                    //If the user pointed to a file then this will be the full original path of the file. If the
                    //user pointed to a directory then this will be the path of that directory (without any file name).
                    String mboxFileNameWithTmpPath = pair.get(0);
                    String pathTheUserPointedTo = pair.get(1);
                    String nonMboxFileWithOriginalPath;
                    File pathPointedTo = new File(pathTheUserPointedTo);
                    if (pathPointedTo.isDirectory()) {
                        //This is a folder. The user pointed to a directory rather than a specific file.
                        //Therefore pathTheUserPointedTo doesn't contain the file name
                        nonMboxFileWithOriginalPath = pathTheUserPointedTo + File.separatorChar + FolderInfo.getNonMboxFileNameFromTmpPath(mboxFileNameWithTmpPath);
                    } else {
                        //This is a file. The user pointed to a specific file.
                        //Therefore pathTheUserPointedTo contains the file name
                        nonMboxFileWithOriginalPath = pathTheUserPointedTo;
                    }
                    String nonMboxFileName = FolderInfo.getNonMboxFileNameFromTmpPath(mboxFileNameWithTmpPath);
                    File dirUntouchedNonMboxFiles = new File(targetFolder + File.separatorChar + Archive.DIR_NAME_FILES_BEFORE_CONVERSION_WITH_EMAILCHEMY);
                    dirUntouchedNonMboxFiles.mkdir();
                    System.out.println("Copy xxx " + nonMboxFileWithOriginalPath);
                    String s = targetFolder + File.separatorChar + Archive.DIR_NAME_FILES_BEFORE_CONVERSION_WITH_EMAILCHEMY + File.separatorChar + nonMboxFileName;
                    if (!new File(s).exists()) {
                        System.out.println("Do Copy xxx " + nonMboxFileWithOriginalPath);

                        //We might have a non mbox file with a number of folders inside. We need to copy the file only once. So check whether file already exists.
                        Util.copy_file(nonMboxFileWithOriginalPath, targetFolder + File.separatorChar + Archive.DIR_NAME_FILES_BEFORE_CONVERSION_WITH_EMAILCHEMY + File.separatorChar + nonMboxFileName);
                    }
                } else {
                    // We want to copy the Mbox file which was imported already being Mbox.
                    if (sourceAssetFolder.contains("^x^")) {
                        sourceAssetFolder = sourceAssetFolder.substring(0, sourceAssetFolder.indexOf("^x^"));
                    }
                    mboxFilename = Util.filePathTail(sourceAssetFolder);
                    new File(targetFolder + File.separatorChar + Archive.UNTOUCHED_IMPORTED_MBOX_FILES).mkdir();
                    Util.copy_file(sourceAssetFolder, targetFolder + File.separatorChar + Archive.UNTOUCHED_IMPORTED_MBOX_FILES + File.separatorChar + mboxFilename);
                }
            }
        } catch (IOException ioe) {
            Util.print_exception("IOException saving files for preservation", ioe, LogManager.getLogger(EmailExporter.class));
        }

/* 2022-09-05
                try {
                    for (String sourceAssetsFile : folderLocations) {
                        // We use the same filenames for canonical acquisitioned assets
                        mboxFilename = Util.filePathTail(sourceAssetsFile);
                        Util.copy_file(sourceAssetsFile, targetFolder + File.separatorChar + mboxFilename);
                    }
                } catch (IOException ioe) {
                    returnCode = "4";
                    returnMessage = "Real time error happened during normalization process: unexpected error is found during copying files";
                }
*/
// 2022-09-05 Added handling for IMAP
//27.12.2022 IMAP support has to be re-added. CHeck whether email source is IMAP if yes use the code below.
//        for (String sourceAssetsFile : folderLocations) {
//            // We use the same filenames for canonical acquisitioned assets
//            mboxFilename = Util.filePathTail(sourceAssetsFile);
//            archive.export2mbox(sourceAssetsFile, targetFolder, mboxFilename);
//        }
// 2022-09-05
        return targetFolder;
    }

    @Override
    public void openingFile(File file) {

    }

    @Override
    public void messageConverted(Date date, String s, File file) {
        nMessageConverted++;
        if (nMessageConverted % 100 == 0) {
            sendStatus("Converted " + nMessageConverted + " from " + nEmails + " emails to EML format (" + (int) ((double) (nMessageConverted) / nEmails * 100) + "%)");
            System.out.println(nMessageConverted + " messages converted");
        }
    }

    @Override
    public void conversionComplete(boolean b) {
        System.out.println("conversionComplete");
        countDownLatchAllEmails.countDown();
        if (countDownLatchAllEmails.getCount() == 0) {
            sendStatus("Cleaning up.");
            try {
                FileUtils.deleteDirectory(this.tmpTargetFolder);
            } catch (IOException e) {
                Util.print_exception("Exception tryimg to delete tmp directory after email conversion to eml", e, LogManager.getLogger(EmailExporter.class));
            }

        }
    }

    @Override
    public void invalidInputFile(File file, Throwable throwable) {

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
}
