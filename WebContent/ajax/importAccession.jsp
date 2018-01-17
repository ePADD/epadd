<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.io.*" %>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@ page language="java" import="edu.stanford.muse.index.Archive"%>
<%@ page language="java" import="org.apache.commons.io.FileUtils"%>
<%@ page import="java.util.ArrayList"%><%@ page import="org.joda.time.DateTime"%><%@ page import="edu.stanford.muse.index.EmailDocument"%><%@ page import="edu.stanford.muse.index.Document"%><%@ page import="java.util.Enumeration"%><%@ page import="java.util.Set"%><%@ page import="java.util.LinkedHashSet"%>
<%
/* copies new accession into REPO_DIR and then loads it from there */
JSONObject result = new JSONObject();
String baseDir = request.getParameter("accessionFolder");
String collectionID = request.getParameter("collectionID");
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
String collectionDir;
if(Util.nullOrEmpty(collectionID)){

    String archiveName = Util.filePathTailByPlatformSeparator(baseDir);
    collectionDir = edu.stanford.muse.Config.REPO_DIR_PROCESSING + File.separator + archiveName;
    new File(collectionDir).mkdirs();
}else
    collectionDir=edu.stanford.muse.Config.REPO_DIR_PROCESSING + File.separator + collectionID;

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
	    Archive collection;
	    if(new File(collectionDir).listFiles().length==0)//means collection directory is empty
	        {
    	    // delete the existing directory -- Q: should we give user a warning??
	        JSPHelper.log.info ("Copying archive files from " + baseDir + " to " + collectionDir);
		    FileUtils.deleteDirectory(new File(collectionDir));
		    FileUtils.copyDirectory(new File(baseDir), new File(collectionDir), true /* preserve file date */);
	        //when should we add accession ID to all messages of this accession?
	        //for that we should load this accession as well.
	        collection = SimpleSessions.readArchiveIfPresent(collectionDir);
            collection.baseAccessionID=accessionID;//as it is the first accession we can avoid assigning
            //accesion id to each doc by saying that this is a baseAccessionID.
		    result.put("status", 0);
		    result.put ("message", "Import accession completed successfully.");
	        result.put("archiveID",SimpleSessions.getArchiveIDForArchive(collection));
	      }
	        else{
            //read archives present in basedir and collection dir.
            collection = SimpleSessions.readArchiveIfPresent(collectionDir);
            Archive accession = SimpleSessions.readArchiveIfPresent(baseDir);
            //call merge on these archives..
            Util.ASSERT(request.getParameter("accessionID")!=null);
            //Merge result will be stored in a field of collection archive object. It will be used
            //later to show mergeReport.[a transient field that will not be saved]
            collection.merge(accession,accessionID);
            //we don't want to keep any reference to accession directory in our map.
            SimpleSessions.removeFromGlobalArchiveMap(baseDir,accession);

            Enumeration<String> enumeration = session.getAttributeNames();
            Set<String> remove = new LinkedHashSet<>();

            while(enumeration.hasMoreElements() ) {
                remove.add(enumeration.nextElement());
            };
            for(String s: remove)
                session.removeAttribute(s);

            result.put("status", 1);
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
                String topDir = collectionDir + File.separatorChar + Archive.SESSIONS_SUBDIR;
                Archive.CollectionMetadata cm = SimpleSessions.readProcessingMetadata (topDir, "default");
                if (cm == null)
                    cm = new Archive.CollectionMetadata();
                if (cm.accessionMetadatas == null)
                    cm.accessionMetadatas = new ArrayList<>();
                cm.accessionMetadatas.add(am);
                SimpleSessions.writeProcessingMetadata (cm, topDir, "default");
                collection.collectionMetadata = cm;//IMP otherwise in-memory archive processingmetadata and
                //the updated metadata on disc will be out of sync. It manifests when saving this archive which
                //overwrites the latest on-disc PM data with stale in-memory data.
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
