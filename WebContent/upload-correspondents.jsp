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
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="edu.stanford.muse.webapp.SimpleSessions" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="edu.stanford.muse.index.SearchResult" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="java.util.Collection" %>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="au.com.bytecode.opencsv.CSVReader" %>
<%@ page import="com.google.common.collect.LinkedListMultimap" %>
<%@ page import="com.google.common.collect.LinkedHashMultimap" %>
<%@ page import="edu.stanford.muse.index.DataSet" %>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%--<%@include file="getArchive.jspf" %>--%>

<%
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
    InputStream istream = null;
    String docsetID=null;
    List<FileItem> items = upload.parseRequest(request);
    for (FileItem item : items) {
        if (item.isFormField()) {
            if ("archiveID".equals(item.getFieldName()))
                archiveID = item.getString();

        }else {
                String type = null;
                if ("correspondentCSV".equals(item.getFieldName()))
                    type = "correspondentCSV";

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

                if ("text/csv".equalsIgnoreCase(contentType))
                    suffix = "csv";
                else {
                    error = "Sorry, only csv files are accepted. Content-type " + contentType + " for " + item.getFieldName();
                    break;
                }
                istream = item.getInputStream();
            }
    }

            Archive archive = null;
            if ("null".equals(archiveID)) {
                error = "Sorry, no archive ID found";
            } else if (istream == null) {
                error = "No input stream found";
            } else {

                archive = SimpleSessions.getArchiveForArchiveID(archiveID);
                try {
                    //String dir = archive.baseDir + File.separator + Archive.IMAGES_SUBDIR;
                    String path = archive.baseDir + File.separator + Archive.TEMP_SUBDIR + File.separator;
                    String filename = path + File.separator + "correspondents.csv";
                    //create dir if not present
                    new File(path).mkdir();
                    Util.copy_stream_to_file(istream, filename);
                    filesUploaded++;
                    //Now get searchresult for all docs where correspondent is one of the name present in this
                    //csv file..put the result in a docset and invoke bulk-label with archiveID and docID
                    //iterate over csv file and concatenate all addresses by OR_Separator to create a string
                    /// reading docToLabelIDmap from csv
                    StringBuilder correspondentStr = new StringBuilder();
                    try {
                        FileReader fr = new FileReader(filename);
                        CSVReader csvreader = new CSVReader(fr, ',', '"', '\n');

                        // read line by line, except the first line which is header
                        String[] record = null;
                        record = csvreader.readNext();//skip the first line.
                        while ((record = csvreader.readNext()) != null) {
                            correspondentStr.append(record[0]);
                            correspondentStr.append(Util.OR_DELIMITER);
                        }

                        csvreader.close();
                        fr.close();
                    } catch (IOException e) {
                        JSPHelper.log.warn("Unable to read correspondents from csv file");

                    }

                    //creating an emptyh params map. No issue because filterForCorredpondents don't use it anyway.
                    Multimap<String, String> params = LinkedHashMultimap.create();

                    SearchResult inputSet = new SearchResult(archive, params);
                    SearchResult search_result = SearchResult.filterForCorrespondents(inputSet, correspondentStr.toString(), true, true, true, true);
                    //create a docsetid and store the resultset in it
                    docsetID = String.format("docset-%08x", EmailUtils.rng.nextInt());// "dataset-1";

                    DataSet dset = new DataSet(search_result.getDocumentSet(), search_result, "Correspondents");
                    session.setAttribute(docsetID, dset);
                    //now invoke windows.location="bulk-labels?archiveID=<\%=archiveID%\>&docsetID=<\%=docsetID%\>"
                    //at the time of loading of this page (in javascript)
                } catch (Exception e) {
                    Util.print_exception(e, JSPHelper.log);
                    error = "Sorry, there was an error copying the csv file.";
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
            <script>
                window.onload=function () {
                    window.location="bulk-labels?archiveID=<%=archiveID%>&docsetID=<%=docsetID%>"
                }
            </script>
           <%=Util.pluralize(filesUploaded, "file")%> uploaded.
       <% } %>
       <br/>
       <br/>

        </div>

       <jsp:include page="footer.jsp"/>

   </body>
</html>
