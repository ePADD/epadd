<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.Archive" %>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page language="java" import="edu.stanford.muse.webapp.SimpleSessions"%>
<%@page language="java" import="org.json.JSONObject"%>
<%@page language="java" import="javax.mail.MessagingException"%>
<%@ page import="java.io.*"%><%@ page import="edu.stanford.muse.webapp.ModeConfig"%><%@ page import="edu.stanford.muse.Config"%><%@ page import="java.lang.ref.WeakReference"%>
<%
	JSONObject result = new JSONObject();
	if (!ModeConfig.isProcessingMode()) {
		result.put ("status", 1);
		result.put ("errorMessage", "Updating collection metadata is allowed only in ePADD's Processing mode.");
		out.println (result.toString(4));
		return;
	}

	String errorMessage="";
	int errorCode=0;
	Archive.AccessionMetadata ametadata=null;
	String archiveBaseDir = Config.REPO_DIR_PROCESSING + File.separator + request.getParameter ("collection");
    String accessionID = request.getParameter("accessionID");
try {

    // read, edit and write back the pm object. keep the other data inside it (such as accessions) unchanged.
	Archive.CollectionMetadata cm = SimpleSessions.readCollectionMetadata(archiveBaseDir);

	if (cm == null) {
	        errorMessage="Unable to find collection for collection id: " +request.getParameter("collection");
	        errorCode=1;

    }else{
    //get accession metadataobject for this accession id.
    //get accession corresponding to accessionID from cm

	for(Archive.AccessionMetadata am: cm.accessionMetadatas){
	    if(am.id.equals(accessionID.trim()))
	        ametadata = am;
	};
	if(ametadata==null)
        {
	        errorMessage="Unable to find accession metadata for accession id: " +accessionID;
	        errorCode=1;
        }
    }

	    if(errorCode!=0){
	        result.put ("status", errorCode);
	        out.println (errorMessage);
	        return;
	       }

	    //ametadata.id=request.getParameter("accessionID");//notneeded because it is not allowed to change (for now)
	ametadata.notes = request.getParameter("accessionNotes");
	ametadata.rights = request.getParameter("accessionRights");
	ametadata.date = request.getParameter("accessionDate");
	ametadata.scope = request.getParameter("accessionScope");
	ametadata.title = request.getParameter("accessionTitle");

	SimpleSessions.writeCollectionMetadata(cm, archiveBaseDir);
	//if the archive is loaded (in global map) then we need to set the collectionmetadata field to this/or invalidate that.
	//ideally we should invalidate that and getCollectionMetaData's responsibility will be to read it again if invalidated.
	//however for now we will just set it explicitly.
	WeakReference<Archive> warchive= SimpleSessions.getArchiveFromGlobalArchiveMap(archiveBaseDir);
	if(warchive!=null)
	    warchive.get().collectionMetadata= cm;
	result.put ("status", 0);
	out.println (result.toString(4));
	return;
} catch (Exception e) {
	result.put ("status", 3);
	result.put("errorMessage", "Could not update collection metadata: " + e.getMessage());
	out.println (result.toString(4));
}

%>
