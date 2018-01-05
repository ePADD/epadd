<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.io.*" %>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@ page language="java" import="edu.stanford.muse.index.Archive"%>
<%@ page language="java" import="org.apache.commons.io.FileUtils"%>
<%@ page import="java.util.ArrayList"%>
<%
/* copies new accession into REPO_DIR and then loads it from there */
JSONObject result = new JSONObject();
String baseDir = request.getParameter("accessionFolder");
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

String archiveName = Util.filePathTailByPlatformSeparator(baseDir);
String targetDir = edu.stanford.muse.Config.REPO_DIR_PROCESSING + File.separator + archiveName;
if (!targetDir.equals(baseDir))
{
	try {
	    // delete the existing directory -- Q: should we give user a warning??
	    JSPHelper.log.info ("Copying archive files from " + baseDir + " to " + targetDir);
		FileUtils.deleteDirectory(new File(targetDir));
		FileUtils.copyDirectory(new File(baseDir), new File(targetDir), true /* preserve file date */);

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
                String pmDir = targetDir + File.separatorChar + Archive.SESSIONS_SUBDIR;
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

		result.put("status", 0);
		result.put ("message", "Import accession completed successfully.");
	} catch (Exception e) {
		result.put("status", 1);
		result.put ("error", "Unable to import accession: " + e.getMessage());
	}
	out.println (result.toString(4));
}
%>
