<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.Archive" %>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page language="java" import="edu.stanford.muse.webapp.SimpleSessions"%>
<%@page language="java" import="org.json.JSONObject"%>
<%@page language="java" import="javax.mail.MessagingException"%>
<%@ page import="java.io.*"%><%@ page import="edu.stanford.muse.webapp.ModeConfig"%><%@ page import="edu.stanford.muse.Config"%><%@ page import="java.lang.ref.WeakReference"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="gov.loc.repository.bagit.domain.Bag"%><%@ page import="java.util.ArrayList"%><%@ page import="java.util.List"%>
<%
	JSONObject result = new JSONObject();

    // This restriction is not available now. User is allowed to prepare metadatas in Appriasal module
    /*
	if (!ModeConfig.isProcessingMode()) {
		result.put ("status", 1);
		result.put ("errorMessage", "Updating file metadata is allowed only in ePADD's Processing mode.");
		out.println (result.toString(4));
		return;
	}
    */

	String errorMessage="";
	int errorCode=0;
    Archive.FileMetadata fmetadata = null;
	String archiveBaseDir;
    if (ModeConfig.isAppraisalMode()) {
        archiveBaseDir= Config.REPO_DIR_APPRAISAL + File.separator + "user";
    } else {
        archiveBaseDir= Config.REPO_DIR_PROCESSING + File.separator + request.getParameter ("collection");
    }
    String fileID = request.getParameter("fileID");
    String[] fileIDParts;
    int fileOffset = -1;
    int accessionOffset = -1;

try {

    // read, edit and write back the pm object. keep the other data inside it (such as accessions) unchanged.
	Archive.CollectionMetadata cm = ArchiveReaderWriter.readCollectionMetadata(archiveBaseDir);
    List <Archive.AccessionMetadata> ams = cm.accessionMetadatas;

	if (cm == null) {
	        errorMessage="Unable to find collection for collection id: " +request.getParameter("collection");
	        errorCode=1;

    }else{
        //get file metadataobject for this file id.
        //get file corresponding to fileID from cm
        fileIDParts = fileID.split("/");

        try {
            fileOffset = Integer.parseInt(fileIDParts[2]);
        } catch (NumberFormatException nfe){
             errorMessage="Unable to find file metadata for file id: " + fileID;
             errorCode=1;
        }

        if(errorCode!=0){
            result.put ("status", errorCode);
            out.println (errorMessage);
            return;
        }

        if (fileIDParts[0].equals("Collection")){    // fileIDParts[0] is the first prefix of file ID
            fmetadata = cm.fileMetadatas.get(fileOffset);
        }else{
            for (int i=0; i<cm.accessionMetadatas.size(); i++){
                Archive.AccessionMetadata am = cm.accessionMetadatas.get(i);

                if (fileIDParts[0].equals(am.id)){
                    fmetadata = am.fileMetadatas.get(fileOffset);
                    accessionOffset = i;
                    break;
                }
            }
        }
        if(fmetadata==null){
            errorMessage="Unable to find file metadata for file id: " + fileID;
            errorCode=1;
        }
    }

    if(errorCode!=0){
        result.put ("status", errorCode);
        out.println (errorMessage);
        return;
    }

	//fmetadata.fileID = request.getParameter("fileID");      //notneeded because it is not allowed to change
	//fmetadata.fileID = request.getParameter("filename");      //notneeded because it is not allowed to change
	//fmetadata.fileID = request.getParameter("fileFormat");      //notneeded because it is not allowed to change
    fmetadata.notes = request.getParameter("fileNotes");
//finished Format Version continue with Creating Application Name


    fmetadata.preservationLevelRole = request.getParameter("file-preservationLevelRole");
    fmetadata.preservationLevelRationale = request.getParameter("file-preservationLevelRationale");
    fmetadata.preservationLevelDateAssigned = request.getParameter("preservationLevelDateAssigned");
    fmetadata.compositionLevel = request.getParameter("compositionLevel");
    fmetadata.messageDigestAlgorithm = request.getParameter("messageDigestAlgorithm");
    fmetadata.messageDigest = request.getParameter("messageDigest");
    fmetadata.messageDigestOrginator = request.getParameter("messageDigestOrginator");
    fmetadata.formatName = request.getParameter("formatName");
    fmetadata.formatVersion  = request.getParameter("formatVersion");
    fmetadata.creatingApplicationName = request.getParameter("creatingApplicationName");
    fmetadata.creatingApplicationVersion = request.getParameter("creatingApplicationVersion");
    fmetadata.dateCreatedByApplication = request.getParameter("dateCreatedByApplication");
    fmetadata.environmentCharacteristic = request.getParameter("file-environmentCharacteristic");
    fmetadata.relatedEnvironmentPurpose = request.getParameter("file-relatedEnvironmentPurpose");
    fmetadata.relatedEnvironmentNote = request.getParameter("file-environmentNote");
    fmetadata.softwareName = request.getParameter("file-softwareName");
    fmetadata.softwareVersion = request.getParameter("file-softwareVersion");
    /*
    if (fileIDParts[0].equals(cm.collectionID)){
        cm.fileMetadatas.set(fileOffset, fmetadata);
    } else {
        ams.get(accessionOffset)
        ams.set(accessionOffset, ametadata);
        cm.accessionMetadatas = ams;
    }
*/
	//if the archive is loaded (in global map) then we need to set the collectionmetadata field to this/or invalidate that.
	//ideally we should invalidate that and getCollectionMetaData's responsibility will be to read it again if invalidated.
	//however for now we will just set it explicitly.
	WeakReference<Archive> warchive= ArchiveReaderWriter.getArchiveFromGlobalArchiveMap(archiveBaseDir);
	ArchiveReaderWriter.readArchiveIfPresent(archiveBaseDir);
	String dir = archiveBaseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;
	String processingFilename = dir + File.separatorChar + Config.COLLECTION_METADATA_FILE;

	if(warchive!=null){
	    warchive.get().collectionMetadata= cm;
	    ArchiveReaderWriter.saveCollectionMetadata(warchive.get(),Archive.Save_Archive_Mode.INCREMENTAL_UPDATE, fileID);
	    }
	else{
	    //we only need to write the collection metadata without loading the archive. so it's fresh creation.
    	ArchiveReaderWriter.saveCollectionMetadata(cm, archiveBaseDir, ArchiveReaderWriter.readArchiveIfPresent(archiveBaseDir), fileID);

    	 //for updating the checksum we need to first read the bag from the basedir..
        Bag archiveBag=Archive.readArchiveBag(archiveBaseDir);
        if(archiveBag==null)
            result.put("errorMessage","Metadata updated but not able to update the bagit checksum");
	    else
	      Archive.updateFileInBag(archiveBag,processingFilename,archiveBaseDir);
	}


	result.put ("status", 0);
	out.println (result.toString(4));
	return;
} catch (Exception e) {
	result.put ("status", 3);
	result.put("errorMessage", "Could not update collection metadata: " + e.getMessage());
	out.println (result.toString(4));
}

%>
