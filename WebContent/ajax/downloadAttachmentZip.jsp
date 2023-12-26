<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>

<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="edu.stanford.muse.datacache.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@include file="../getArchive.jspf" %>

<%
    String archiveID = request.getParameter("archiveID");
    String docsetID = request.getParameter("docsetID");

    JSONObject result = new JSONObject();
    String linkURL = "";
    int status = 0;

    String dir = Archive.TEMP_SUBDIR + File.separator;
    new File(dir).mkdir();//make sure it exists
    //create mbox file in localtmpdir and put it as a link on appURL.

    File f = new File(dir);

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
    JSPHelper.log.info ("download zip has " + selectedDocs.size() + " docs");

    /* Code to export attachments for the given set of documents as zip file*/
    Map<Blob, String> blobToErrorMessage = new LinkedHashMap<>();

    //Copy all attachment to a temporary directory and then zip it and allow transfer to the client
    int nBlobsExported = Archive.getnBlobsExported(selectedDocs, archive.getBlobStore(),null, attachmentdirname, false, null, false, blobToErrorMessage);

    //now zip the attachmentdir and create a zip file in TMP
    String zipfile = Archive.TEMP_SUBDIR+ File.separator + "all-attachments.zip";
    Util.deleteAllFilesWithSuffix(Archive.TEMP_SUBDIR,"zip",JSPHelper.log);
    Util.zipDirectory(attachmentdirname, zipfile);

    //return it's URL to download
    String appURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    String attachmentURL = "serveTemp.jsp?archiveID="+archiveID+"&file=all-attachments.zip" ;
    linkURL = appURL + "/" +  attachmentURL;

    result.put("status", status);
    result.put("url", linkURL);
    out.println(result.toString());
%>