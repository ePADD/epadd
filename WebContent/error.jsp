<%@page isErrorPage="true" contentType="text/html; charset=UTF-8"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.util.Util" %>
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
    Please see the <a href="debug">debug log</a>, and email it to <%=edu.stanford.muse.Config.admin %>
    We'll get back to you as soon as possible.
    <% out.println ("<br/>Error code:" + request.getAttribute("javax.servlet.error.status_code") + " type: " + request.getAttribute("javax.servlet.error.exception_type")); %>
<% } %>

</div>
<jsp:include page="footer.jsp"/>

</body>
</html>
