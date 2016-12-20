<%@ page import="java.io.*" %>
<%@ page import="java.util.List" %>
<%@page language="java" import="org.json.JSONObject"%>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="javax.mail.MessagingException" %>
<%@page language="java" import="org.apache.commons.fileupload.FileItem"%>
<%@page language="java" import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page language="java" import="edu.stanford.muse.email.StaticStatusProvider"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@include file="getArchive.jspf" %>
<%!
private String getFileName(final Part part) throws MessagingException
{
    String contentDisposition = part.getHeader("content-disposition");
    /*
    if (Util.nullOrEmpty((contentDispositions)) || contentDispositions.length < 1)
        return null;
    String contentDisposition = contentDispositions[0];
    if (Util.nullOrEmpty(contentDisposition))
        return null;
    */
    for (String content : contentDisposition.split(";")) {
        if (content.trim().startsWith("filename")) {
            return content.substring(
                    content.indexOf('=') + 1).trim().replace("\"", "");
        }
    }
    return null;
}
%>

<%!
protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, MessagingException {
    response.setContentType("text/html;charset=UTF-8");

    // Create path components to save the file
    final String path = "/tmp"; // request.getParameter("destination");
    final javax.servlet.http.Part filePart = request.getPart("profilePhoto");
    final String fileName = getFileName(filePart);
    OutputStream out = null;
    InputStream fileContent = null;
    final PrintWriter writer = response.getWriter();

    try {
        out = new FileOutputStream(new File(path + File.separator + fileName));
        fileContent = filePart.getInputStream();

        int read = 0;
        final byte[] bytes = new byte[1024];

        while ((read = fileContent.read(bytes)) != -1) {
            out.write(bytes, 0, read);
        }
        writer.println("New file " + fileName + " created at " + path);
        System.out.println ("File " + fileName + " being uploaded to " + path);
    } catch (FileNotFoundException fne) {
        writer.println("You either did not specify a file to upload or are "
                + "trying to upload a file to a protected or nonexistent "
                + "location.");
        writer.println("<br/> ERROR: " + fne.getMessage());
       System.out.println ("Problems during file upload. Error: " + fne.getMessage());
    } finally {
        if (out != null) {
            out.close();
        }
        if (fileContent != null) {
            fileContent.close();
        }
        if (writer != null) {
            writer.close();
        }
    }
}
   %>
   
   <%
       session.setAttribute("statusProvider", new StaticStatusProvider("Uploading files"));
       JSONObject result = new JSONObject();

// Create a factory for disk-based file items
DiskFileItemFactory factory = new DiskFileItemFactory();

// Configure a repository (to ensure a secure temp location is used)
ServletContext servletContext = this.getServletConfig().getServletContext();
File repository = new File(System.getProperty("java.io.tmpdir"));
factory.setRepository(repository);

// Create a new file upload handler
ServletFileUpload upload = new ServletFileUpload(factory);

int filesUploaded = 0;
// Parse the request
String error = null;
List<FileItem> items = upload.parseRequest(request);
for (FileItem item : items)
{
	try
    {
        String type = null;
        if ("profilePhoto".equals(item.getFieldName()))
            type = "profilePhoto";
        else if ("bannerImage".equals(item.getFieldName()))
            type = "bannerImage";
        else if ("landingPhoto".equals(item.getFieldName()))
            type = "landingPhoto";

        if (item.getSize() <= 0)
            continue; // if an input field is left empty, its size is 0

        String contentType = item.getContentType();
        String suffix = null;
            /*
			if ("image/jpeg".equals(contentType)) {
				suffix = "jpg";
			}
			if ("image/gif".equals(contentType)) {
				suffix = "gif";
			}
			*/

        if ("image/png".equalsIgnoreCase(contentType))
            suffix = "png";
        else
        {
            error = "Sorry, only PNG files are accepted. Content-type " + contentType + " for " + item.getFieldName();
            break;
        }

        if (type != null && suffix != null) {
            String dir = archive.baseDir + File.separator + Archive.IMAGES_SUBDIR;
            new File(dir).mkdirs();
			String filename = dir + File.separator + type + "." + suffix;
			Util.copy_stream_to_file (item.getInputStream(), filename);
			filesUploaded++;
		}
	} catch (Exception e) { 
		Util.print_exception (e, JSPHelper.log);
        error = "Sorry, there was an error uploading files.";
	}

    if (error != null) {
        result.put("status", 1);
        result.put ("error", error);
    } else {
        result.put ("status", 0);
        result.put ("filesUploaded", filesUploaded);
    }
}
session.removeAttribute("statusProvider");
       %>
<html lang="en">
    <head>
       <title>Upload Images</title>
       <link rel="icon" type="image/png" href="images/epadd-favicon.png">

        <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
        <jsp:include page="css/css.jsp"/>

       <script src="js/jquery.js"></script>
       <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
        <script src="js/epadd.js" type="text/javascript"></script>
    </head>
   <body>
       <jsp:include page="header.jspf"/>
       <script>epadd.nav_mark_active('Collections');</script>
        <div style="margin-left:170px">
       <br/>
       <br/>
        <% if (error != null) { %>
            <%=error%>
       <% } else { %>
           <%=Util.pluralize(filesUploaded, "file")%> uploaded.
       <% } %>
       <br/>
       <br/>
            <button class="btn btn-cta" onclick="window.location='set-images'; return false;">Back <i class="icon-arrowbutton"></i></button>
        </div>

       <jsp:include page="footer.jsp"/>

   </body>
</html>
