<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBookManager.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.Document"%>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="java.io.File" %>
<%@ page import="edu.stanford.muse.datacache.Blob" %>ex
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="java.util.stream.Stream" %>
<%@ page import="java.util.stream.Collector" %>
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
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Export');</script>
<%
    String archiveID= SimpleSessions.getArchiveIDForArchive(archive);
    AddressBook addressBook = archive.addressBook;
	String bestName = addressBook.getBestNameForSelf().trim();
	writeProfileBlock(out, archive, "", "Export archive");
	//get the options that were displayed for attachment types. This will be used to select attachment extensions if the option 'other'
    //was selected by the user in the drop down box of export.jsp.
    List<String> attachmentTypeOptions = Config.attachmentTypeToExtensions.values().stream().map(x->Util.tokenize(x,";")).flatMap(col->col.stream()).collect(Collectors.toList());
    //forEach(stringStringPair -> attachmentTypeOptions.add(stringStringPair.first));

%>
<div style="margin-left:170px">


<%
    String dir = request.getParameter ("dir");
    boolean unprocessedonly = Boolean.parseBoolean(request.getParameter("unprocessedonly"));
    String msgProcessed = unprocessedonly?"Only Unprocessed" : "All (processed and unprocessed";
    File f = new File(dir);
    if (!f.isDirectory()) {
        out.println ("Sorry, you must choose a directory in which the attachments will be exported");
        return;
    }

    if (!f.canWrite()) {
        out.println("Sorry, you must choose a directory in which the attachments will be exported");
        return;
    }

    String type = request.getParameter ("type");
    //Multiple selections in the checkbox dropdown are returned in comma separated form whereas the extensions of one type of documents
    //are separated by semicolon therefore we need the following conversion to uniformly handle them in Util.tokenize method.
    type = type.replaceAll(",",";");
    String extension = request.getParameter ("ext");


            Set<String> extensionsNeeded = new LinkedHashSet<>();
    if (!Util.nullOrEmpty (type))
        extensionsNeeded.addAll(Util.tokenize(type, ";"));
    if (!Util.nullOrEmpty (extension))
        extensionsNeeded.addAll(Util.tokenize(extension, ";"));

            // convert to lower case and remove empty types
    extensionsNeeded = (Set) extensionsNeeded.stream().filter(x -> !Util.nullOrEmpty(x)).map (x -> x.toLowerCase()).collect (Collectors.toSet());
    //a variable to select if the extensions needed contain others.
    boolean isOtherSelected = extensionsNeeded.contains("others");

%>
    <div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

    <br/>
    <br/>
    Exporting <%=msgProcessed%> attachments with extensions: <%=StringUtils.join (extensionsNeeded, ";")%>

    <%
    Map<Blob, String> blobToErrorMessage = new LinkedHashMap<>();
    int nBlobsExported = 0;

    for (Document d: archive.getAllDocs()) {
        EmailDocument ed = (EmailDocument) d;
        List<Blob> blobs = ed.attachments;
        if (Util.nullOrEmpty(blobs))
            continue;

        nextBlob:
        for (Blob blob: blobs) {

            //check if this blob need to be exported or not depending upon the option
            //unprocessedonly and the state of this blob.
            if(unprocessedonly && blob.processedSuccessfully){
                continue nextBlob;
            }
            try {
                String blobName = blob.getName();
                // get rid of any file separators first... don't want them to cause any confusion
                if (blobName == null)
                    blobName = "";


                blobName = blobName.replaceAll("/", "_");
                blobName = blobName.replaceAll("\\\\", "_");
                String base, ext;
                Pair<String, String> pair = Util.splitIntoFileBaseAndExtension(blobName);
                base = pair.getFirst();
                ext = pair.getSecond();
                Util.ASSERT(Util.nullOrEmpty(ext) ? blobName.equals(base) : blobName.equals(base + "." + ext));

                if (!Util.nullOrEmpty(extensionsNeeded)) {
                    if (Util.nullOrEmpty(ext))
                        continue nextBlob;
                    //Proceed to add this attachment only if either
                    //1. other is selected and this extension is not present in the list attachmentOptionType, or
                    //2. this extension is present in the variable extensionNeeded
                    //Q. [What if there is a file with .others extension?]
                    boolean firstcondition = isOtherSelected && !attachmentTypeOptions.contains(ext.toLowerCase());
                    boolean secondcondition = extensionsNeeded.contains(ext.toLowerCase());
                    if (!firstcondition && !secondcondition)
                        continue nextBlob;
                }

                String targetPath = dir + File.separator + blobName;

                if (new File(targetPath).exists()) {
                    // try adding (1), (2) etc to the base part of the blob name... keep the extension unchanged
                    int i = 1;
                    do {
                        String targetFile = base + " (" + (i++) + ")" + (Util.nullOrEmpty(ext) ? "" : "." + ext);
                        targetPath = dir + File.separator + targetFile;
                    } while (new File(targetPath).exists());
                }
                archive.blobStore.createBlobCopy(blob, targetPath);
                nBlobsExported++;

            } catch (Exception e) {
                Util.print_exception ("Error exporting blob", e, JSPHelper.log);
                blobToErrorMessage.put (blob, "Error exporting blob: " + e.getMessage());
            }
            // blob has the right extensions

        }
    }
%>
    <script>$('#spinner-div').hide();</script>

    <p>

    <p>
		<p>
		<%=nBlobsExported%> attachments exported to <%=Util.escapeHTML(dir)%>.<br/>

        <%
        JSPHelper.log.info ("nBlobsExported: " + nBlobsExported);

        if (blobToErrorMessage.size() > 0) {
            out.println (Util.pluralize (blobToErrorMessage.size(), "error") + "occurred.");
            out.println ("<p>Errors:");
            for (Blob b: blobToErrorMessage.keySet())
                out.println (b + "<br/>" + blobToErrorMessage.get(b) + "<p>");
        }
        %>

	</div>
<p>
<br/>
<br/>
</body>
</html>
