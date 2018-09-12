<%@ page import="java.io.*" %>
<%@ page import="java.util.List" %>
<%@page language="java" import="org.json.JSONObject"%>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@page language="java" import="org.apache.commons.fileupload.FileItem"%>
<%@page language="java" import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page language="java" import="edu.stanford.muse.email.StaticStatusProvider"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@ page import="gov.loc.repository.bagit.domain.Bag" %>
<%--<%@include file="getArchive.jspf" %>--%>

<%
    String collectionID = request.getParameter("collection");
   session.setAttribute("statusProvider", new StaticStatusProvider("Uploading files"));
   JSONObject result = new JSONObject();

    // Create a factory for disk-based file items
    // Configure a repository (to ensure a secure temp location is used)
    // Create a new file upload handler
    DiskFileItemFactory factory = new DiskFileItemFactory();
    File repository = new File(System.getProperty("java.io.tmpdir"));
    factory.setRepository(repository);
    ServletFileUpload upload = new ServletFileUpload(factory);

    int filesUploaded = 0;
    // Parse the request
    String error = null;
    String archiveID=null;
    List<FileItem> items = upload.parseRequest(request);
    for (FileItem item : items) {
        if (item.isFormField()) {
            if ("collection".equals(item.getFieldName()))
                collectionID = item.getString();
            if("archiveID".equals(item.getFieldName()))
                archiveID=item.getString();
        } else {
            try {
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
                else {
                    error = "Sorry, only PNG files are accepted. Content-type " + contentType + " for " + item.getFieldName();
                    break;
                }

                String dname=null;
                Archive archive=null;
                if(!"null".equals(collectionID))
                    dname = collectionID;
                else if(!"null".equals(archiveID)) {
                    archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
                    dname = new File(archive.baseDir).getName();
                }
                String dir = null;
                String basedir=null;
                Bag bag=null;
                if (type != null && suffix != null) {
                    //String dir = archive.baseDir + File.separator + Archive.IMAGES_SUBDIR;

                    if (ModeConfig.isProcessingMode()) {
                        basedir=edu.stanford.muse.Config.REPO_DIR_PROCESSING + File.separator + dname;
                        bag=Archive.readArchiveBag(basedir);
                        dir = edu.stanford.muse.Config.REPO_DIR_PROCESSING + File.separator + dname + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separator + Archive.IMAGES_SUBDIR;
                    }
                    else if (ModeConfig.isDeliveryMode()) {
                        basedir = edu.stanford.muse.Config.REPO_DIR_DELIVERY + File.separator + dname;
                        bag = Archive.readArchiveBag(basedir);
                        dir = edu.stanford.muse.Config.REPO_DIR_DELIVERY + File.separator + dname + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separator + Archive.IMAGES_SUBDIR;
                    }
                    else if (ModeConfig.isDiscoveryMode()) {
                        basedir = edu.stanford.muse.Config.REPO_DIR_DISCOVERY + File.separator + dname;
                        bag=Archive.readArchiveBag(basedir);
                        dir = edu.stanford.muse.Config.REPO_DIR_DISCOVERY + File.separator + dname + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separator + Archive.IMAGES_SUBDIR;
                    }
                    else if (ModeConfig.isAppraisalMode()) {
                        basedir = Config.REPO_DIR_APPRAISAL + File.separator + dname;
                        bag=Archive.readArchiveBag(basedir);
                        dir = Config.REPO_DIR_APPRAISAL + File.separator + dname + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.IMAGES_SUBDIR;
                    }
                    new File(dir).mkdirs();
                    String filename = dir + File.separator + type + "." + suffix;
                    Util.copy_stream_to_file(item.getInputStream(), filename);
                    filesUploaded++;
                }
                //after uploading the images, update the bag metadata as well.
                Archive.updateFileInBag(bag,dir,basedir);
            } catch (Exception e) {
                Util.print_exception(e, JSPHelper.log);
                error = "Sorry, there was an error uploading files.";
            }

        }
    }
        if (error != null) {
            result.put("status", 1);
            result.put ("error", error);
        } else {
            result.put ("status", 0);
            result.put ("filesUploaded", filesUploaded);
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
            <%
                if(!"null".equals(collectionID)){
            %>
            <button class="btn btn-cta" onclick="window.location='set-images?collection=<%=collectionID%>'; return false;">Back <i class="icon-arrowbutton"></i></button>
            <%}else if(!"null".equals(archiveID)){%>
            <button class="btn btn-cta" onclick="window.location='set-images?archiveID=<%=archiveID%>'; return false;">Back <i class="icon-arrowbutton"></i></button>
            <%}%>
        </div>

       <jsp:include page="footer.jsp"/>

   </body>
</html>
