<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.email.StaticStatusProvider"%>
<%@page language="java" import="edu.stanford.muse.index.Archive" %>
<%@page language="java" import="edu.stanford.muse.index.Archive.ProcessingMetadata"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page language="java" import="edu.stanford.muse.webapp.SimpleSessions"%>
<%@page language="java" import="org.json.JSONObject"%>
<%@page language="java" import="javax.mail.MessagingException"%><%@ page import="java.io.*"%>
		<%!
        private String getFileName(final Part part) throws MessagingException
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
        private void saveFile(HttpServletRequest request, String param, String filePath) throws IOException, ServletException, MessagingException
        {
            final Part filePart = request.getPart(param);
            final String fileName = getFileName(filePart);
            OutputStream out = null;
            InputStream filecontent = null;

            try {
                out = new FileOutputStream(new File(filePath));
                filecontent = filePart.getInputStream();

                int read = 0;
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
session.setAttribute("statusProvider", new StaticStatusProvider("Updating metadata"));
JSONObject result = new JSONObject();

Archive archive = JSPHelper.getArchive(session);
if (archive == null) 
{
	result.put ("status", 1);
	result.put("error", "Please load an archive first.");
	out.println (result.toString(4));
	return;	
}

try {
	if (archive.processingMetadata == null)
		archive.processingMetadata = new ProcessingMetadata();

	// populate the ownerName field inside pm directly from the archive.ownerNames (its not in the request parameters).
	// we could consider providing it in the request parameters if the archivist needs to have an option to change the name from what appraisal originally assigned.
	if (archive.ownerNames != null && archive.ownerNames.size() > 0)
		archive.processingMetadata.ownerName = archive.ownerNames.iterator().next();

	archive.processingMetadata.institution = request.getParameter("institution");
	archive.processingMetadata.repository = request.getParameter("repository");
	archive.processingMetadata.collectionTitle = request.getParameter("collectionTitle");
	archive.processingMetadata.collectionID = request.getParameter("collectionID");
	archive.processingMetadata.accessionID = request.getParameter("accessionID");
    archive.processingMetadata.contactEmail = request.getParameter("contactEmail");

	archive.processingMetadata.findingAidLink = request.getParameter("findingAidLink");
	archive.processingMetadata.catalogRecordLink = request.getParameter("catalogRecordLink");
	archive.processingMetadata.about = request.getParameter("about");

/*
	saveFile (request, "collectionImage", archive.baseDir + File.separator + Archive.IMAGES_SUBDIR + File.separator + "landingPhoto.png");
	saveFile (request, "bannerImage", archive.baseDir + File.separator + Archive.IMAGES_SUBDIR + File.separator + "bannerImage.png");
*/

	SimpleSessions.saveArchive(archive.baseDir, "default", archive);
	result.put ("status", 0);
	out.println (result.toString(4));
	return;
} catch (Exception e) {
	result.put ("status", 3);
	result.put("error", "Could not update archive metadata: " + e.getMessage());
	out.println (result.toString(4));
}
session.removeAttribute("statusProvider");

%>
