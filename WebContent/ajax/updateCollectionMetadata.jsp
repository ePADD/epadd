<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.Archive" %>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page language="java" import="edu.stanford.muse.webapp.SimpleSessions"%>
<%@page language="java" import="org.json.JSONObject"%>
<%@page language="java" import="javax.mail.MessagingException"%>
<%@ page import="java.io.*"%><%@ page import="edu.stanford.muse.webapp.ModeConfig"%><%@ page import="edu.stanford.muse.Config"%><%@ page import="java.lang.ref.WeakReference"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%>
<%!
private String getFileName(final Part part)
{
    String contentDisposition = part.getHeader("content-disposition");
//		if (Util.nullOrEmpty((contentDispositions)) || contentDispositions.length < 1)
//			return null;
//		String contentDisposition = contentDispositions[0];
    if (Util.nullOrEmpty(contentDisposition))
        return null;

    for (String content : contentDisposition.split(";")) {
        if (content.trim().startsWith("filename")) {
            return content.substring(
                    content.indexOf('=') + 1).trim().replace("\"", "");
        }
    }
    return null;
}

/* saves the file type request param into the given filePath */
private void saveFile(HttpServletRequest request, String param, String filePath) throws IOException, ServletException
{
    final Part filePart = request.getPart(param);
    final String fileName = getFileName(filePart);
    OutputStream out = null;
    InputStream filecontent = null;

    try {
        out = new FileOutputStream(new File(filePath));
        filecontent = filePart.getInputStream();

        int read;
        final byte[] bytes = new byte[1024];

        while ((read = filecontent.read(bytes)) != -1) {
            out.write(bytes, 0, read);
        }
        JSPHelper.log.info ("File " + fileName + " being uploaded to " + filePath);
    } catch (FileNotFoundException fne) {
        JSPHelper.log.info ("You either did not specify a file to upload or are "
                + "trying to upload a file to a protected or nonexistent "
                + "location.");
       JSPHelper.log.info  ("Problems during file upload. Error: " + fne.getMessage());
    } finally {
        if (out != null) {
            out.close();
        }
        if (filecontent != null) {
            filecontent.close();
        }
    }
}

%>
<%
	JSONObject result = new JSONObject();
	if (!ModeConfig.isProcessingMode()) {
		result.put ("status", 1);
		result.put ("errorMessage", "Updating collection metadata is allowed only in ePADD's Processing mode.");
		out.println (result.toString(4));
		return;
	}

	String archiveBaseDir = Config.REPO_DIR_PROCESSING + File.separator + request.getParameter ("collection");

try {

    // read, edit and write back the pm object. keep the other data inside it (such as accessions) unchanged.
	Archive.CollectionMetadata cm = ArchiveReaderWriter.readCollectionMetadata(archiveBaseDir);

	if (cm == null) {
	    // not sure we want to init a new cm. we could simply bail out.
	    //JSPHelper.log.warn ("No existing collection metadata in " + archiveBaseDir);
	    cm = new Archive.CollectionMetadata();
    }

	// we could consider providing ownername in the request parameters if the archivist needs to have an option to change the name from what appraisal originally assigned.
	cm.institution = request.getParameter("institution");
	cm.repository = request.getParameter("repository");
	cm.collectionTitle = request.getParameter("collectionTitle");
	cm.collectionID = request.getParameter("collectionID");
    cm.contactEmail = request.getParameter("contactEmail");
    cm.rights = request.getParameter("rights");
    cm.notes = request.getParameter("notes");
    cm.scopeAndContent = request.getParameter("scopeAndContent");

	cm.findingAidLink = request.getParameter("findingAidLink");
	cm.catalogRecordLink = request.getParameter("catalogRecordLink");
	cm.about = request.getParameter("about");

/*
	saveFile (request, "collectionImage", archive.baseDir + File.separator + Archive.IMAGES_SUBDIR + File.separator + "landingPhoto.png");
	saveFile (request, "bannerImage", archive.baseDir + File.separator + Archive.IMAGES_SUBDIR + File.separator + "bannerImage.png");
*/

	//if the archive is loaded (in global map) then we need to set the collectionmetadata field to this/or invalidate that.
	//ideally we should invalidate that and getCollectionMetaData's responsibility will be to read it again if invalidated.
	//however for now we will just set it explicitly.
	WeakReference<Archive> warchive= ArchiveReaderWriter.getArchiveFromGlobalArchiveMap(archiveBaseDir);
	if(warchive!=null){
	    warchive.get().collectionMetadata= cm;
	ArchiveReaderWriter.saveCollectionMetadata(cm, archiveBaseDir);

	}else{
	ArchiveReaderWriter.saveCollectionMetadata(cm, archiveBaseDir);

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
