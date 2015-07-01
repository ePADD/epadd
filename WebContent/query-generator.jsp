<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%
if (ModeConfig.isPublicMode()) {
	// this browse page is also used by Public mode where the following set up may be requried. 
	String archiveId = request.getParameter("aId");
	Sessions.loadSharedArchiveAndPrepareSession(session, archiveId);
	// TODO: should also pass "aId" downstream to leadsAsJson.jsp also. but it still relies on emailDocs and maybe other session attributes, whose dependence should also be eliminated in public mode for being RESTful.
}
%>
<!DOCTYPE html>
<html>
<head>
	<title>Bulk Search Results</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>
		
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap-theme.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>	
</head>
<body>
<% if (!ModeConfig.isPublicMode()) { %>
<jsp:include page="header.jspf"/>
<script>
	epadd.select_link('#nav1', 'Appraisal');
	epadd.select_link('#nav2', 'Search');
</script>
<% } %>
<div>
<br/>
<%=edu.stanford.muse.util.Messages.getMessage("messages", "bulk-search.help") %>
<p>

<hr/>
<p>

<%
String req = request.getParameter("refText");
out.println (Util.escapeHTML(req).replace("\r", "").replace("\n", "<br/>\n"));
%>
</div>
<script type="text/javascript">
	window.MUSE_URL = '<%=HTMLUtils.getRootURL(request)%>';
</script>
<script type="text/javascript" src="js/muse-lens.user.js"></script>
<jsp:include page="footer.jsp"/>
</body>
</html>
