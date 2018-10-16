<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.net.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@include file="getArchive.jspf" %>
<html>
<head>
	<title>Export Complete</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>
	epadd.nav_mark_active('Export');
</script>

<% try {
    String archiveID= ArchiveReaderWriter.getArchiveIDForArchive(archive);
	AddressBook addressBook = archive.addressBook;
	String bestName = addressBook.getBestNameForSelf();
	writeProfileBlock(out, archive, "Export archive");
%>
<p>
<br/>
<div style="width:1100px;margin:auto" class="panel">

<%

	String rawDir = request.getParameter("dir");
	/*
	List<String> pathTokens = Util.tokenize(dir, "\\/");
	dir = Util.join(pathTokens, File.separator);
	*/
	String dir = new File(rawDir).getAbsolutePath();

	File file = new File(dir);
	if (!file.isDirectory() || !file.canWrite()) {
		out.println ("<p>Sorry, the directory " + dir + " is not writable. Please <a href=\"export?archiveID=+"+archiveID+"&type=doNotTransfer\">go back</a> and select a different directory.");
		return;
	}
	%>
	<div id="spinner-div" style="text-align:center;"><i class="fa fa-spin fa-spinner"></i></div>
	<% out.flush(); %>
	<%
	if (Util.nullOrEmpty(dir)) {
		dir = System.getProperty("java.io.tmpdir");
	}

	//TimeKeeper.snap();
	String folder = dir + File.separator + "ePADD archive of " + bestName;
	List<Document> docsToExport = new ArrayList<Document>();

	/*
	@TODO-Export Take decision on exporting based on labels of this document set
	for (Document d: archive.getAllDocs())
	{
		EmailDocument ed = (EmailDocument) d;
		if (!ed.doNotTransfer)
			docsToExport.add(ed);
	}
*/
		docsToExport = archive.getDocsForExport(Archive.Export_Mode.EXPORT_APPRAISAL_TO_PROCESSING);

		//From appraisal to processing we do not remove any label from labelManager so pass the current
		//label info map as an argument to set labelmanager for the exported archive.
    JSPHelper.log.info("Exporting #"+docsToExport.size()+" docs");
	// to do: need a progress bar here
	try {
		archive.export(docsToExport, Archive.Export_Mode.EXPORT_APPRAISAL_TO_PROCESSING, folder, "default");
	} catch (Exception e) {
		Util.print_exception ("Error trying to export archive", e, JSPHelper.log);
		out.println ("Sorry, error exporting archive: " + e + ". Please see the log file for more details.");
	}
%>
<!-- Archive exported to <%=""%> -->
	ePADD archive exported to: <%=Util.escapeHTML(folder) %>
	<%-- ePADD archive exported to: <%=Util.escapeHTML(folder) %> (time taken: <%=TimeKeeper.since()%>) --%>
	<br/>
	Please zip this folder and submit it to the archives.
<%
} catch (Exception e) {
	Util.print_exception("Error in export", e, JSPHelper.log);
	out.println ("Sorry, export failed: " + e);
}
%>
	</div>
<script>$('#spinner-div').hide();</script>
</body>
</html>
