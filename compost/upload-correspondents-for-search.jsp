<%@ page import="java.io.*" %>
<%@page language="java" import="org.json.JSONObject"%>
<%@page language="java" import="edu.stanford.muse.email.StaticStatusProvider"%>
<%@page language="java" %>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="au.com.bytecode.opencsv.CSVReader" %>
<%@ page import="com.google.common.collect.LinkedHashMultimap" %>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.index.*" %>
<%@ page import="java.util.Map" %>
<%--<%@include file="getArchive.jspf" %>--%>

<%
    session.setAttribute("statusProvider", new StaticStatusProvider("Uploading files"));
   JSONObject result = new JSONObject();

    // Create a factory for disk-based file items
    // Configure a repository (to ensure a secure temp location is used)
    // Create a new file upload handler

    String error = null;
    Map<String,String> params = JSPHelper.convertRequestToMapMultipartData(request);
    String archiveID = params.get("archiveID");
    Archive archive = null;
    int filesUploaded = 0;
    String docsetID = null;
    if ("null".equals(archiveID)) {
                error = "Sorry, no archive ID found";
            } else {
                String filename = params.get("correspondentCSV");
                archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
                try {
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
                    Multimap<String, String> sparams = LinkedHashMultimap.create();

                    SearchResult inputSet = new SearchResult(archive, sparams);
                    SearchResult search_result = SearchResult.filterForCorrespondents(inputSet, correspondentStr.toString(), true, true, true, true);
                    //create a docsetid and store the resultset in it
                    docsetID = String.format("docset-%08x", EmailUtils.rng.nextInt());// "dataset-1";

                    DataSet dset = new DataSet(search_result.getDocumentSet(), search_result, "Correspondents", queryparams);
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
       <link rel="icon" type="image/png" href="../WebContent/images/epadd-favicon.png">

        <link rel="stylesheet" href="../WebContent/bootstrap/dist/css/bootstrap.min.css">
        <jsp:include page="../WebContent/css/css.jsp"/>

       <script src="../WebContent/js/jquery.js"></script>
       <script type="text/javascript" src="../WebContent/bootstrap/dist/js/bootstrap.min.js"></script>
        <script src="../WebContent/js/epadd.js" type="text/javascript"></script>
    </head>
   <body>
       <jsp:include page="../WebContent/header.jspf"/>
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

       <jsp:include page="../WebContent/footer.jsp"/>

   </body>
</html>
