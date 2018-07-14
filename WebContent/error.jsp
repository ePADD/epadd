<%@page isErrorPage="true" contentType="text/html; charset=UTF-8"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" %>
<%  JSPHelper.log.warn ("Error page reached. code:" + request.getAttribute("javax.servlet.error.status_code") + " message:" + request.getAttribute("javax.servlet.error.message") + " type: " + request.getAttribute("javax.servlet.error.exception_type")); %>

<!DOCTYPE HTML>
<html>
<head>
<link rel="icon" type="image/png" href="images/epadd-favicon.png">
<title>Archive Information</title>
<script src="js/jquery.js"></script>
<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

<jsp:include page="css/css.jsp"/>
<script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<%
	if (exception != null) {
		Util.print_exception("Error page reached! ", exception, JSPHelper.log);
	}
%>

<div style="margin-left:170px">
Sorry! ePADD error.
<% if (ModeConfig.isDiscoveryMode()) { %>
    Please contact <%=edu.stanford.muse.Config.admin %>.
<% } else { %>
    Pleae provide the following details to <%=edu.stanford.muse.Config.admin %>:
    <ul>
        <li>Operating system & version</li>
        <li>Version of JRE(Java Runtime)</li>
        <li>version of ePADD</li>
        <li>Total RAM in your system</li>
        <li>RAM assigned to ePADD</li>
        <li>Name of the screens you went through and the input boxes filled on those screens (or screen prints). state the steps to reproduce the problem.</li>
        <li>If the problem was encountered while importing emails then please mention the source of mbox file and the software (if any) used to convert it to mbox format.</li>
        <li>Number of email messages and the size (in GB) of the archive.</li>
        <li>If necessary, can you share the email files with us to find out the issue?</li>
        <li>Operating system & version</li>
        <li>The debug log given below.</li>
    </ul>
    <% out.println ("<br/>Error code:" + request.getAttribute("javax.servlet.error.status_code") + " type: " + request.getAttribute("javax.servlet.error.exception_type")); %>
<% } %>

</div>
<jsp:include page="footer.jsp"/>

</body>
</html>
