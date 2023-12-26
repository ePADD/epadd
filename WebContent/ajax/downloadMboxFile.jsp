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
    JSONObject result = new JSONObject();
    String linkURL = "";
    int status = 0;

    String dir = Archive.TEMP_SUBDIR + File.separator;
    new File(dir).mkdir();//make sure it exists
    //create mbox file in localtmpdir and put it as a link on appURL.

    File f = new File(dir);

    String docsetID=request.getParameter("docsetID");
    String fname = Util.nullOrEmpty(docsetID) ? "epadd-export-all.mbox" : "epadd-export-" + docsetID + ".mbox";

    //String attachmentdirname = f.getAbsolutePath() + File.separator + (Util.nullOrEmpty(docsetID)? "epadd-all-attachments" : "epadd-all-attachments-"+docsetID);
    //new File(attachmentdirname).mkdir();

    String pathToFile = f.getAbsolutePath() + File.separator + fname;

    PrintWriter pw = null;
    try {
        pw = new PrintWriter(pathToFile, "UTF-8");
    } catch (Exception e) {
        out.println ("Sorry, error opening mbox file: " + e + ". Please see the log file for more details.");
        status = 1;
    }

    //check if request contains docsetID then work only on those messages which are in docset
    //else export all messages of mbox.
    Collection<Document> selectedDocs;

    if(!Util.nullOrEmpty(docsetID)){
        DataSet docset = (DataSet) session.getAttribute(docsetID);
        selectedDocs = docset.getDocs();
    }else {
        selectedDocs = new LinkedHashSet<>(archive.getAllDocs());
    }
    //JSPHelper.log.info ("export mbox has " + selectedDocs.size() + " docs");


    // either we do tags (+ or -) from selectedTags
    // or we do all docs from allDocs
    BlobStore bs = null;
    bs = archive.getBlobStore();

    String noAttach = request.getParameter("noattach");
    String stripquoted = request.getParameter("stripQuoted");

    boolean noAttachments = "on".equals(noAttach);
    boolean stripQuoted = "on".equals(stripquoted);

    int numOfMessageWritten = 0;
    final int N_MAX_MESSAGE_TO_FLUSHED = 4*1024;    // experimental

    // Try to periodically flush output stream to remedy the mbox file output crash. Experimental
    for (Document ed: selectedDocs) {
        EmailUtils.printToMbox(archive, (EmailDocument) ed, pw, noAttachments ? null: bs, stripQuoted);
        numOfMessageWritten ++;
        if (numOfMessageWritten == N_MAX_MESSAGE_TO_FLUSHED) { // experimental
            numOfMessageWritten = 0;
            pw.flush();
        }
    }
    pw.close();
    String appURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    String contentURL = "serveTemp.jsp?archiveID="+request.getParameter("archiveID")+"&file=" + fname ;
    linkURL = appURL + "/" +  contentURL;

    result.put("status", status);
    result.put("url", linkURL);
    out.println(result.toString());
%>
