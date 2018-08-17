<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.Config"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.Document"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.List" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
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
String archiveID= ArchiveReaderWriter.getArchiveIDForArchive(archive);
	String bestName = addressBook.getBestNameForSelf().trim();
	writeProfileBlock(out, archive, "", "Export archive");
%>
<div style="margin-left:170px">

<%
	String rawDir = request.getParameter("dir");
	String dir = new File(rawDir).getAbsolutePath();

	File file = new File(dir);
	if (!file.isDirectory() || !file.canWrite()) {
		out.println ("<p>Sorry, the directory " + dir + " is not writable. Please <a href=\"export-processing?archiveID="+archiveID+"&type=doNotTransfer\">go back</a> and select a different directory.");
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
	ArchiveReaderWriter.saveArchive(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);

	String folder = dir + File.separator + "ePADD archive of " + bestName + "-Delivery";
	String folderPublic = dir + File.separator + "ePADD archive of " + bestName + "-Discovery";
	//same set of docs are exported from processing to delivery or discovery only difference being the
		//content of these messages. In case of processing to discovery mode the redaction takes place.
	List<Document> docsToExport = archive.getDocsForExport(Archive.Export_Mode.EXPORT_PROCESSING_TO_DELIVERY);

	/*
	Before v5 we faced an issue where after exporting to delivery/discovery the document's content got
	changed because we are not actually copying the document/index document before making any changes (redaction)
	in their subject/body. One easy way to avoid this is to save the current archive in delivery/discovery mode,
	load it again and then make changes to that (redaction etc) and then saving it again. However, note that
	reading the archive from a directory will result in adding it's archive ID to global archiveID map which
	we don't want now [because we are loading it only temporarily].
	 *//*

	//save current archive to delivery directory
	// save baseDir temporarily and set it back after saving it in delivery/discovery folders.
	String tmpbasedir = archive.baseDir;
	archive.setBaseDir(folder);
	SimpleSessions.saveArchive(archive);
	//save current archive to discovery directory
	archive.setBaseDir(folderPublic);
	SimpleSessions.saveArchive(archive);
	//set baseDir back for the current archive.
	archive.setBaseDir(tmpbasedir);
	//At this point, both folder and folderpublic contain the processing mode archive. We need to read these archives
	//modify them according to the mode (delivery/discovery) and then save them back to their respective folders (folder/folderpublic)
	Archive fordelivery = SimpleSessions.readArchiveIfPresent(folder);*/
	//make sure to remove it from the global map once it's work is done
	// archive.collectionMetadata.entityCounts = null;
	archive.collectionMetadata.numPotentiallySensitiveMessages = -1;
	out.println ("Exporting delivery mode archive...<br/>");
	out.flush();
	try {
		archive.export(docsToExport, Archive.Export_Mode.EXPORT_PROCESSING_TO_DELIVERY, folder, "default");
//		//remove fordelivery archive from the global map
//		SimpleSessions.removeFromGlobalArchiveMap(folder,fordelivery);
	} catch (Exception e) {
		Util.print_exception ("Error trying to export archive", e, JSPHelper.log);
		out.println ("Sorry, error exporting archive: " + e + ". Please see the log file for more details.");
	}

	/* v6- Why were we exporting correspondent authority file separately??- removed now
		*try {
        String csv = archive.getCorrespondentAuthorityMapper().getAuthoritiesAsCSV ();
        if (!Util.nullOrEmpty(csv)) {
            String filename = folder + File.separator + edu.stanford.muse.Config.AUTHORITIES_CSV_FILENAME;
            FileWriter fw = new FileWriter(new File(filename));
            fw.write(csv);
            fw.close();
        } else {
            JSPHelper.log.info ("No authorities information in this archive");
        }
	} catch(Exception e) {
		out.println ("Warning: unable to write authorities CSV file into " + file);
		JSPHelper.log.warn(e);
	}*/

	// NOW EXPORT FOR DISCOVERY
	//Read archive from folderPublic directory and operate on that.
	//Archive forDeliveryPublic = SimpleSessions.readArchiveIfPresent(folderPublic);
	out.println ("Exporting discovery mode archive...<br/>");
	out.flush();
	JSPHelper.log.info("Exporting for discovery");
	/*v6- why were we exporting correspondent authority file separately? Removed now.
		try {
        String csv = archive.getCorrespondentAuthorityMapper().getAuthoritiesAsCSV ();
        if (!Util.nullOrEmpty(csv)) {
            String filename = folder + File.separator + edu.stanford.muse.Config.AUTHORITIES_CSV_FILENAME;
            FileWriter fw = new FileWriter(new File(filename));
            fw.write(csv);
            fw.close();
        }
	} catch(Exception e) {
		out.println ("Warning: unable to write authorities CSV file into " + file);
		JSPHelper.log.warn(e);
	}
	*/
	archive.export(docsToExport, Archive.Export_Mode.EXPORT_PROCESSING_TO_DISCOVERY/* public mode */, folderPublic, "default");


		// Now remember to reload the archive from baseDir, because we've destroyed the archive in memory
		//after export make sure to load the archive again. However, readArchiveIfPresent will not read from
		//memroy if the archive and it's ID is already present in gloablArchiveMap (in SimpleSession).
		//Hence first remove this from the map and then call readArchiveIfPresent method.
		JSPHelper.log.info("Reading back archive...");
		out.println ("Reloading saved archive...<br/>");
		out.flush();
		String baseDir = archive.baseDir;
		ArchiveReaderWriter.removeFromGlobalArchiveMap(baseDir,archive);
		archive = ArchiveReaderWriter.readArchiveIfPresent(baseDir);
	/*archive.setBaseDir(baseDir);
	session.setAttribute("archive", archive);
	session.setAttribute("userKey", "user");
	session.setAttribute("cacheDir", archive.baseDir);
*/
//	archive.collectionMetadata.entityCounts = ec;

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
