<%@ page contentType="text/html; charset=UTF-8"%>
<% JSPHelper.checkContainer(request); // do this early on so we are set up
  request.setCharacterEncoding("UTF-8"); %>
<%@page language="java" import="java.util.ArrayList"%>
<%@page language="java" import="java.util.Collection"%>
<%@page language="java" import="java.util.List"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@include file="getArchive.jspf" %>
<%
// we are already logged into all accounts at the point this is called
// we may not have finished reading the folders though.
	session.setMaxInactiveInterval(-1);
    // never let session expire

      response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
      response.setHeader("Pragma","no-cache"); //HTTP 1.0
      response.setDateHeader ("Expires", 0); //prevent caching at the proxy server
	  // remove existing status provider if any because sometimes status provider
	  // for prev. op prints stale status till new one is put in the session
      if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
		  session.removeAttribute("statusProvider");

    // re-read accounts again only if we don't already have them in this session.
    // later we might want to provide a way for users to refresh the list of folders.
  	AddressBook addressBook = archive.addressBook;

%>
<!DOCTYPE HTML>
<html lang="en">
<head>
<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<title>Archive Report</title>

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css"/>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script type="text/javascript" src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>

<div style="min-height:300px;margin: 10px 50px 10px 50px" class="panel rounded">


<%
	List<FetchStats> fetchStats = archive.allStats;
	int count = 0;
	for (FetchStats fs: fetchStats) {
		out.println ("<h2>Import #" + (++count) + "</h2>");
		out.println (fs.toHTML());
		out.println ("<hr/>");
	}

	Collection<String> dataErrors = addressBook.getDataErrors();
	int i = 0;
	if (dataErrors != null) { %>

		<h2><%=dataErrors.size()%> data error(s) in email addresses</h2>
		<p>
		<%
		for (String s: dataErrors) {
			out.println (++i + ". " + Util.escapeHTML(s) + "<br/>\n");
		}
	}
	i = 0;
	dataErrors = archive.getDataErrors();
	if (dataErrors != null) {
		%>
		<hr/>
		<h2><%=dataErrors.size()%> data error(s) in message content and attachments</h2>
		<%
		List<String> dups = new ArrayList<String>();
		i = 0;
		// carve out the dups and report them separately
		for (String s: dataErrors) {
			if (s.startsWith("Duplicate message") || s.startsWith("Message already present"))
				dups.add(s);
			else
				out.println(++i + ". " + Util.escapeHTML(s) + "<br/>\n");
		}

		if (dups.size() > 0) {
			i = 0;
			%>
			<hr/>
			<h2><%=Util.pluralize(dups.size(), "duplicate message")%></h2>
			<%
			for (String s: dups)
				out.println(++i + ". " + Util.escapeHTML(s) + "<br/>\n");
		}
	}
%>

</div>
<jsp:include page="footer.jsp"/>
</body>
</html>
