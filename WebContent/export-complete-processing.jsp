<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.epadd.Config"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.ie.AuthorisedAuthorities"%>
<%@page language="java" import="edu.stanford.muse.index.Document"%>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.SimpleSessions" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileWriter" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@include file="getArchive.jspf" %>
<html>
<head>
	<title>Export Complete</title>

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
	String bestName = addressBook.getBestNameForSelf().trim();
	writeProfileBlock(out, archive, "", "Export archive");
%>
<div style="margin-left:170px">

<%
	String rawDir = request.getParameter("dir");
	String dir = new File(rawDir).getAbsolutePath();

	File file = new File(dir);
	if (!file.isDirectory() || !file.canWrite()) {
		out.println ("<p>Sorry, the directory " + dir + " is not writable. Please <a href=\"export-processing?type=doNotTransfer\">go back</a> and select a different directory.");
		return;
	}
	%>
	<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>
	<% out.flush(); %>
	<%
	if (Util.nullOrEmpty(dir)) {
		dir = System.getProperty("java.io.tmpdir");
	}

	JSPHelper.log.info("Saving archive in " + archive.baseDir);
	out.println("Saving current archive...<br/>");
	out.flush();
	SimpleSessions.saveArchive(session);

	String folder = dir + File.separator + "ePADD archive of " + bestName + "-Delivery";
	String folderPublic = dir + File.separator + "ePADD archive of " + bestName + "-Discovery";
	List<EmailDocument> docsToExport = new ArrayList<EmailDocument>();
	for (Document d: archive.getAllDocs())
	{
		EmailDocument ed = (EmailDocument) d;
		if (!ed.doNotTransfer && !ed.transferWithRestrictions) // important: for processing, transfer with restrictions is not to be exported!
			docsToExport.add(ed);
		ed.reviewed = false; // always reset reviewed flag, for both discovery and delivery
	}

	// we need to recompute counts here for only the docs that are being exported
	/*
	Map<String,Integer> ec = new HashMap<String,Integer>();
	if(archive.processingMetadata.entityCounts!=null)
		for(String str: archive.processingMetadata.entityCounts.keySet())
			ec.put(str,archive.processingMetadata.entityCounts.get(str));
	else
		ec = null;
	*/
	// archive.processingMetadata.entityCounts = null;
	archive.processingMetadata.numPotentiallySensitiveMessages = -1;
	out.println ("Exporting delivery mode archive...<br/>");
	out.flush();
	archive.export(docsToExport, false, folder, "default");

	String csv = AuthorisedAuthorities.exportRecords(archive, "csv");

	try {
		String filename = folder + File.separator + edu.stanford.muse.Config.AUTHORITIES_CSV_FILENAME;
		FileWriter fw = new FileWriter(new File(filename));
		fw.write(csv);
		fw.close();
	} catch(Exception e) {
		out.println ("Warning: unable to write authorities CSV file into " + file);
		JSPHelper.log.warn(e);
	}

	// NOW EXPORT FOR DISCOVERY
	out.println ("Exporting discovery mode archive...<br/>");
	out.flush();
	JSPHelper.log.info("Exporting for delivery");
	archive.export(docsToExport, true /* public mode */, folderPublic, "default");

	try {
		String filename = folder + File.separator + edu.stanford.muse.Config.AUTHORITIES_CSV_FILENAME;
		FileWriter fw = new FileWriter(new File(filename));
		fw.write(csv);
		fw.close();
	} catch(Exception e) {
		out.println ("Warning: unable to write authorities CSV file into " + file);
		JSPHelper.log.warn(e);
	}

	// Now remember to reload the archive from baseDir, because we've destroyed the archive in memory
	JSPHelper.log.info("Reading back archive...");
	out.println ("Reloading saved archive...<br/>");
	out.flush();
	String baseDir = archive.baseDir;
	archive = SimpleSessions.readArchiveIfPresent(baseDir);
	archive.setBaseDir(baseDir);
	session.setAttribute("archive", archive);
	session.setAttribute("userKey", "user");
	session.setAttribute("cacheDir", archive.baseDir);

//	archive.processingMetadata.entityCounts = ec;

//authority records are exported to authority.csv in delivery mode

%>
<script>$('#spinner-div').hide();</script>
<p>
		<p>
		Full ePADD archive exported to: <%=Util.escapeHTML(folder)%><br/>
		Please copy this folder under &lt;HOME&gt;/<%=Util.filePathTail(Config.REPO_DIR_DELIVERY)%> and start ePADD in delivery mode.<br/>
		<p>
		Public ePADD archive (containing only named entities) exported to: <%=Util.escapeHTML(folderPublic)%><br/>
		Please copy this folder under &lt;HOME&gt;/<%=Util.filePathTail(Config.REPO_DIR_DISCOVERY)%> and start ePADD in discovery mode.
	</div>
<p>
<br/>
<br/>
</body>
</html>
