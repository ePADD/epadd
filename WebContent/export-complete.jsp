<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.net.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="com.google.gson.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
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
	AddressBook addressBook = archive.addressBook;
	String bestName = addressBook.getBestNameForSelf();
	writeProfileBlock(out, bestName, "", "Export archive");
%>
<p>
<br/>
<div style="margin-left:170px">

<%
	AddressBook ab = archive.addressBook;

	String rawDir = request.getParameter("dir");
	/*
	List<String> pathTokens = Util.tokenize(dir, "\\/");
	dir = Util.join(pathTokens, File.separator);
	*/
	String dir = new File(rawDir).getAbsolutePath();

	File file = new File(dir);
	if (!file.isDirectory() || !file.canWrite()) {
		out.println ("<p>Sorry, the directory " + dir + " is not writable. Please <a href=\"export?type=doNotTransfer\">go back</a> and select a different directory.");
		return;
	}
	%>
	<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>
	<% out.flush(); %>
	<%
	if (Util.nullOrEmpty(dir)) {
		dir = System.getProperty("java.io.tmpdir");
	}

	//TimeKeeper.snap();
	String folder = dir + File.separator + "ePADD archive of " + bestName;
	List<EmailDocument> docsToExport = new ArrayList<EmailDocument>();
	for (Document d: archive.getAllDocs())
	{
		EmailDocument ed = (EmailDocument) d;
		if (!ed.doNotTransfer)
			docsToExport.add(ed);
	}
	// to do: need a progress bar here
	archive.export(docsToExport, false /* public mode */, folder, "default");
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
