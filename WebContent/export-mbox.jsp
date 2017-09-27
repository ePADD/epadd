<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="edu.stanford.muse.datacache.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
    <title>Export</title>

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
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Export');</script>
<% 	AddressBook addressBook = archive.addressBook;
String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	String bestName = addressBook.getBestNameForSelf().trim();
	writeProfileBlock(out, archive, "", "Export archive");
%>
<div style="margin-left:170px">
<div id="spinner-div" style="display:none;text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

<%
    String dir = request.getParameter ("dir");
    File f = new File(dir);
    String pathToFile;
    if (f.isDirectory())
        pathToFile = f.getAbsolutePath() + File.separator + "epadd-export.mbox";
    else
        pathToFile = f.getAbsolutePath();

    PrintWriter pw = null;
    try {
        pw = new PrintWriter(pathToFile, "UTF-8");
    } catch (Exception e) {
        out.println ("Sorry, error opening mbox file: " + e + ". Please see the log file for more details.");
        Util.print_exception("Error opening mbox file: ", e, JSPHelper.log);
        return;
    }


        List<EmailDocument> selectedDocs = (List) archive.getAllDocs();

        JSPHelper.log.info ("export mbox has " + selectedDocs.size() + " docs");

        // either we do tags (+ or -) from selectedTags
        // or we do all docs from allDocs
        String cacheDir = archive.baseDir;
        String attachmentsStoreDir = cacheDir + File.separator + "blobs" + File.separator;
        BlobStore bs = null;
        try {
            bs = new BlobStore(attachmentsStoreDir);
            JSPHelper.log.info ("Good, found attachments store in dir " + attachmentsStoreDir);
        } catch (IOException ioe) {
            JSPHelper.log.error("Unable to initialize attachments store in directory: " + attachmentsStoreDir + " :" + ioe);
        }

        /*
        String rootDir = JSPHelper.getRootDir(request);
        new File(rootDir).mkdirs();
        String userKey = JSPHelper.getUserKey(session);

        String name = request.getParameter("name");
        if (Util.nullOrEmpty(name))
            name = String.format("%08x", EmailUtils.rng.nextInt());
        String filename = name + ".mbox.txt";

        String path = rootDir + File.separator + filename;
        PrintWriter pw = new PrintWriter (path);
        */

        String noAttach = request.getParameter("noattach");
        boolean noAttachments = "on".equals(noAttach);
        boolean stripQuoted = "on".equals(request.getParameter("stripQuoted"));
        for (Document ed: selectedDocs)
            EmailUtils.printToMbox(archive, (EmailDocument) ed, pw, noAttachments ? null: bs, stripQuoted);
        pw.close();
    %>

    <br/>

    Mbox file saved: <%=pathToFile%>.
    <p></p>
    This file is in mbox format, and can be accessed with many email clients (e.g. <a href="http://www.mozillamessaging.com/">Thunderbird</a>.)
    It can also be viewed with a text editor.<br/>
    On Mac OS X, Linux, and other flavors of Unix, you can usually open a terminal window and type the command: <br/>
    <i>mail -f &lt;saved file&gt;</i>
    <br/>
</div>
<jsp:include page="footer.jsp"/>
</body>
</html>
