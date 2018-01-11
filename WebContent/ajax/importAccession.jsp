<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.io.*" %>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@ page language="java" import="edu.stanford.muse.index.Archive"%>
<%@ page language="java" import="org.apache.commons.io.FileUtils"%>
<%@ page import="java.util.ArrayList"%><%@ page import="org.joda.time.DateTime"%><%@ page import="edu.stanford.muse.index.EmailDocument"%><%@ page import="edu.stanford.muse.index.Document"%>
<%
/* copies new accession into REPO_DIR and then loads it from there */
JSONObject result = new JSONObject();
String baseDir = request.getParameter("accessionFolder");
String collectionDir = request.getParameter("collectionFolder");
if (Util.nullOrEmpty(baseDir))
{
	result.put ("status", 1);
	result.put("error", "No directory specified");
	out.println (result.toString(4));
	return;
}

// check if its really an archive
if (!new File(baseDir + File.separator + Archive.SESSIONS_SUBDIR + File.separator + "default" + SimpleSessions.getSessionSuffix()).exists())
{
	result.put ("status", 2);
	result.put("error", "The specified folder does not appear to contain an ePADD archive.");
	out.println (result.toString(4));
	return;
}

if(Util.nullOrEmpty(collectionDir)){

    String archiveName = Util.filePathTailByPlatformSeparator(baseDir);
    collectionDir = edu.stanford.muse.Config.REPO_DIR_PROCESSING + File.separator + archiveName;
    new File(collectionDir).mkdir();
}

if (!collectionDir.equals(baseDir))
{
	try {
	    //Two possible cases, 1. When an accession is being imported to an empty collection
	    ///2. When an accession is being imported to a non-empty collection.
	    //We use result.status to indicate the caller if this call was made from an existing collection or
	    //from an empty collection. status=0 means this accession was imported to an empty collection and it
	    //succeeded. status=2 means this accession was imported to an existing collection and it succeeded.
	    //Based on that the next page will be rendered.
	    //Note that we are storing mergeResult in a transient field of Archive. It will be used for rendering
	    //the report when a call to displaymergeReport.jsp is made with archiveID.
	    String accessionID=request.getParameter("accessionID");
	    if(new File(collectionDir).listFiles().length==0)//means collection directory is empty
	        {
    	    // delete the existing directory -- Q: should we give user a warning??
	        JSPHelper.log.info ("Copying archive files from " + baseDir + " to " + collectionDir);
		    FileUtils.deleteDirectory(new File(collectionDir));
		    FileUtils.copyDirectory(new File(baseDir), new File(collectionDir), true /* preserve file date */);
	        //when should we add accession ID to all messages of this accession?
	        //for that we should load this accession as well.
	        Archive collection = SimpleSessions.readArchiveIfPresent(collectionDir);
            collection.baseAccessionID=accessionID;//as it is the first accession we can avoid assigning
            //accesion id to each doc by saying that this is a baseAccessionID.
		    result.put("status", 0);
		    result.put ("message", "Import accession completed successfully.");
	        result.put("archiveID",SimpleSessions.getArchiveIDForArchive(collection));
	      }
	        else{
            //read archives present in basedir and collection dir.
            Archive collection = SimpleSessions.readArchiveIfPresent(collectionDir);
            Archive accession = SimpleSessions.readArchiveIfPresent(baseDir);
            //call merge on these archives..
            Util.ASSERT(request.getParameter("accessionID")!=null);
            //Merge result will be stored in a field of collection archive object. It will be used
            //later to show mergeReport.[a transient field that will not be saved]
            collection.merge(accession,accessionID);
            //we don't want to keep any reference to accession directory in our map.
            SimpleSessions.removeFromGlobalArchiveMap(baseDir,accession);

            result.put("status", 0);
		    result.put ("message", "Accession imported and merged with the collection successfully");
	        result.put("archiveID",SimpleSessions.getArchiveIDForArchive(collection));
	        }

		JSPHelper.log.info ("Copy complete, creating new accession metadata object");

		/* update the pm object with new accession info */
		{
            Archive.AccessionMetadata am = new Archive.AccessionMetadata();
            am.id = accessionID;
            am.title = request.getParameter("accessionTitle");
            am.date = request.getParameter("accessionDate");
            am.scope = request.getParameter("accessionScope");
            am.rights = request.getParameter("accessionRights");
            am.notes = request.getParameter("accessionNotes");

            if(Util.nullOrEmpty(am.date))
                am.date = new DateTime().toDate().toString();
            if(Util.nullOrEmpty(am.scope))
                am.scope = "";
            if(Util.nullOrEmpty(am.rights))
                am.rights= "";
            if(Util.nullOrEmpty(am.notes))
                am.notes= "";

            // we update just the PM file in the target dir with this info. no need to go into the (legacy) archive object, because the PM inside it will always be overridden by the one in this file.
            // new archive objects will anyway not have PM objects embedded within them.
            // see SimpleSessions.readArchiveIfPresent()
            {
                String pmDir = collectionDir + File.separatorChar + Archive.SESSIONS_SUBDIR;
                Archive.ProcessingMetadata pm = SimpleSessions.readProcessingMetadata (pmDir, "default");
                if (pm == null)
                    pm = new Archive.ProcessingMetadata();
                if (pm.accessionMetadatas == null)
                    pm.accessionMetadatas = new ArrayList<>();
                pm.accessionMetadatas.add(am);
                SimpleSessions.writeProcessingMetadata (pm, pmDir, "default");
            }
            JSPHelper.log.info ("Accession metadata updated");
        }

	} catch (Exception e) {
		result.put("status", 1);
		result.put ("error", "Unable to import accession: " + e.getMessage());
	}
	out.println (result.toString(4));
}
%>