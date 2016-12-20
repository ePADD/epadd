<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="com.google.gson.Gson"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="java.util.ArrayList"%>
<%@page language="java" import="java.util.List"%>
<%@ page import="edu.stanford.muse.webapp.SimpleSessions" %>
<%@include file="getArchive.jspf" %>

<html>
<head>
	<title>Transfer actions</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
	<script src="jqueryFileTree/jqueryFileTree.js"></script>
		
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>
</head>

<body>
    <jsp:include page="header.jspf"/>


	<div style="margin-left:10%">
    <%
        String transferActionsArchiveDir = request.getParameter("dir");
        String status = archive.transferActionsFrom (transferActionsArchiveDir);
        out.println (status.replaceAll("\\n", "<br/>"));
        out.flush();
        SimpleSessions.saveArchive(session); // save .session file.
        out.println ("<br/>Current archive saved in: " + archive.baseDir);
    %>
	</div>
</body>
</html>
