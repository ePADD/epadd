<%@page contentType="application/json;charset=UTF-8"%>
<%@page import="java.io.*" %>
<%@page import="java.util.List" %>
<%@page import="org.json.JSONObject"%>
<%@page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@page import="org.apache.commons.fileupload.FileItem"%>
<%@page import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page import="edu.stanford.muse.email.StaticStatusProvider"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@page import="edu.stanford.muse.util.Util"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@page import="edu.stanford.muse.Config" %>
<%@page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@page import="gov.loc.repository.bagit.domain.Bag" %>
<%
// note: this is ajax/upload-images.jsp (not just upload-images.jsp, which will be removed)

    String collectionID = request.getParameter("collectionID");
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
            if ("collectionID".equals(item.getFieldName()))
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

                String dname=null;
                Archive archive=null;
                if(collectionID != null && !"null".equals(collectionID))
                    dname = collectionID;
                else if(!"null".equals(archiveID)) {
                    archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
                    dname = new File(archive.baseDir).getName();
                }
                String dir = null;
                String basedir=null;
                Bag bag=null;

                // sgh: disabling check, will allow any file type
                // if (type != null && suffix != null)
                {
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

                    // we copy the file without the .jpg/.png/.gif suffix, but simply as profilePhoto, bannerImage or landingPhoto without an ext.
                    // the browser will figure out the correct file type while rendering.
                    String filename = dir + File.separator + type; // no suffix needed now + "." + suffix;
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

    out.println (result.toString());

    session.removeAttribute("statusProvider");
%>