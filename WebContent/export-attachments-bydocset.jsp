<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="java.io.File" %>
<%@ page import="edu.stanford.muse.datacache.Blob" %>ex
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="edu.stanford.muse.index.*" %>
<%@ page import="edu.stanford.muse.datacache.BlobStore" %>
<%@include file="getArchive.jspf" %>
<html>
<head>
	<title>Export Attachments</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<%@include file="header.jspf"%>
<script>epadd.nav_mark_active('Export');</script>
<%
    AddressBook addressBook = archive.addressBook;
	String bestName = addressBook.getBestNameForSelf().trim();
	writeProfileBlock(out, archive,"Export archive");
	//get the options that were displayed for attachment types. This will be used to select attachment extensions if the option 'other'
    //was selected by the user in the drop down box of export.jsp.
    List<String> attachmentTypeOptions = Config.attachmentTypeToExtensions.values().stream().map(x->Util.tokenize(x,";")).flatMap(col->col.stream()).collect(Collectors.toList());
    //forEach(stringStringPair -> attachmentTypeOptions.add(stringStringPair.first));

%>
<div style="margin-left:170px">


<%
    String dir = Archive.TEMP_SUBDIR + File.separator;
    new File(dir).mkdir();//make sure it exists
    //create mbox file in localtmpdir and put it as a link on appURL.

//    String dir = request.getParameter ("dir");
    File f = new File(dir);
    String docsetID=request.getParameter("docsetID");
//    String fname = Util.nullOrEmpty(docsetID) ? "epadd-export-all.mbox" : "epadd-export-" + docsetID + ".mbox";
    String attachmentdirname = f.getAbsolutePath() + File.separator + (Util.nullOrEmpty(docsetID)? "epadd-all-attachments" : "epadd-all-attachments-"+docsetID);

    new File(attachmentdirname).mkdir();



    //check if request contains docsetID then work only on those messages which are in docset
    //else export all messages of mbox.
    Collection<Document> selectedDocs;

    if(!Util.nullOrEmpty(docsetID)){
        DataSet docset = (DataSet) session.getAttribute(docsetID);
        selectedDocs = docset.getDocs();
    }else {
        selectedDocs = new LinkedHashSet<>(archive.getAllDocs());
    }
    JSPHelper.log.info ("exporting attachments for" + selectedDocs.size() + " docs");



    String appURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();

    /* Code to export attachments for the given set of documents as zip file*/
    Map<Blob, String> blobToErrorMessage = new LinkedHashMap<>();
    //Copy all attachment to a temporary directory and then zip it and allow transfer to the client
    int nBlobsExported = Archive.getnBlobsExported(selectedDocs, archive.getBlobStore(),null, attachmentdirname, false, null, false, blobToErrorMessage);
    //now zip the attachmentdir and create a zip file in TMP
    String zipfile = Archive.TEMP_SUBDIR+ File.separator + "attachments.zip";
    Util.deleteAllFilesWithSuffix(Archive.TEMP_SUBDIR,"zip",JSPHelper.log);
    Util.zipDirectory(attachmentdirname, zipfile);
    //return it's URL to download
    String attachmentURL = "serveTemp.jsp?archiveID="+archiveID+"&file=attachments.zip" ;
    String attachmentDownloadURL = appURL + "/" +  attachmentURL;



%>
    <%--<div id="spinner-div" style="text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>--%>
    <% out.flush();%>
    <br/>
    <br/>
    Exporting <%=nBlobsExported%> attachments.
    <br/>

    <br/>
    <a href =<%=attachmentDownloadURL%>>Download attachments in a zip file</a>
    <p></p>
    This file is in zip format, and contains all attachments in the selected messages.<br/>
    <br/>

</div>
<jsp:include page="footer.jsp"/>
</body>
</html>

<%!
    /*
    To get all attachments for a given set of documents call with attachmentTypeOptions = null, unprocessedonly = false, extensionsNeeded=null,isOtherSelected=false.
     */

%>