<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.Config"%>
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
<%@ page import="java.io.PrintWriter" %>
<%@include file="getArchive.jspf" %>
<html>
<head>
	<title>Export Authorities</title>

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
<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

<%
    String dir = request.getParameter ("dir");

    File f = new File(dir);
    String pathToFile;
    if (f.isDirectory())
        pathToFile = f.getAbsolutePath() + File.separator + "epadd-authorities.csv";
    else
        pathToFile = f.getAbsolutePath();

    PrintWriter pw = null;
    try {
        pw = new PrintWriter(pathToFile, "UTF-8");
    } catch (Exception e) {
        out.println ("Sorry, error opening authorities file: " + e + ". Please see the log file for more details.");
        Util.print_exception("Error opening authorities file: ", e, JSPHelper.log);
        return;
    }

	String exportType = request.getParameter("exportType"); // currently not used

    try {
		String csv = archive.getAuthoritiesAsCSV();
        pw.println(csv);
	} catch(Exception e){
		e.printStackTrace();
		pw.println(e.getMessage());
    }
%>

%>
<script>$('#spinner-div').hide();</script>
<p>
		<p>
		ePADD authorities exported to <%=Util.escapeHTML(pathToFile)%><br/>
	</div>
<p>
<br/>
<br/>
</body>
</html>
