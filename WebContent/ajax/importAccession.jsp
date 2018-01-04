<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.io.*" %>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@ page language="java" import="edu.stanford.muse.index.Archive"%>
<%@ page language="java" import="org.apache.commons.io.FileUtils"%>
<%@ page import="java.util.ArrayList"%><%@ page import="edu.stanford.muse.Config"%>
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
	    //2. When an accession is being imported to a non-empty collection.
	    if(new File(collectionDir).listFiles().length==0)//means collection directory is empty
	        {
    	    // delete the existing directory -- Q: should we give user a warning??
	        JSPHelper.log.info ("Copying archive files from " + baseDir + " to " + collectionDir);
		    FileUtils.deleteDirectory(new File(collectionDir));
		    FileUtils.copyDirectory(new File(baseDir), new File(collectionDir), true /* preserve file date */);
	        //load archive and open browse page with appropriate archive ID.
	        }
	        else{
            //read archives present in basedir and collection dir.
            //call merge on these archives..
            //add this newly created archive object in global archive map
            //open merge report page with this archive ID.
	        }

		JSPHelper.log.info ("Copy complete, creating new accession metadata object");

		/* update the pm object with new accession info */
		{
            Archive.AccessionMetadata am = new Archive.AccessionMetadata();
            am.id = request.getParameter("accessionID");
            am.title = request.getParameter("accessionTitle");
            am.date = request.getParameter("accessionDate");
            am.scope = request.getParameter("accessionScope");
            am.rights = request.getParameter("accessionRights");
            am.notes = request.getParameter("accessionNotes");

            // we update just the PM file in the target dir with this info. no need to go into the (legacy) archive object, because the PM inside it will always be overridden by the one in this file.
            // new archive objects will anyway not have PM objects embedded within them.
            // see SimpleSessions.readArchiveIfPresent()
            {
                String pmFile = collectionDir + File.separatorChar + Archive.SESSIONS_SUBDIR + File.separatorChar + "default" + Config.PROCESSING_METADATA_SUFFIX;
                Archive.ProcessingMetadata pm = SimpleSessions.readProcessingMetadata (collectionDir + File.separatorChar + Archive.SESSIONS_SUBDIR, "default");
                if (pm == null)
                    pm = new Archive.ProcessingMetadata();
                if (pm.accessionMetadatas == null)
                    pm.accessionMetadatas = new ArrayList<>();
                pm.accessionMetadatas.add(am);
                SimpleSessions.writeProcessingMetadata (pm, collectionDir + File.separatorChar + Archive.SESSIONS_SUBDIR, "default");
            }
            JSPHelper.log.info ("Accession metadata updated");
        }

		result.put("status", 0);
		result.put ("message", "Import accession completed successfully.");
	} catch (Exception e) {
		result.put("status", 1);
		result.put ("error", "Unable to import accession: " + e.getMessage());
	}
	out.println (result.toString(4));
}
%>
