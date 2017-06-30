<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.xcoll.CrossCollectionSearch" %>
<%@ page import="edu.stanford.muse.xcoll.EntityInfo" %>
<%@ page import="java.util.Collection" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="edu.stanford.muse.email.CalendarUtil" %>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<!DOCTYPE HTML>
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Search</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	<style>
		td > div {
			padding: 5px;
		}

		.option {
			margin-right: 15px;
		}

		.underlined-header { border-bottom: solid 4px #0175bc; }
		.search-header { font-size: 100%; cursor: pointer; padding-bottom:5px;}
	</style>
</head>
<body>
<jsp:include page="header.jspf"/>
<br/>
<br/>
<div style="margin-left:170px">
	<%

    String queryTerm = request.getParameter("term");
    Multimap<Integer, EntityInfo> entityToInfos = CrossCollectionSearch.search (queryTerm);

    out.println ("Search term: " + Util.escapeHTML(queryTerm) + "<br/><br/>");

	if (entityToInfos == null || entityToInfos.isEmpty()) { %>
		No hits.
    	<%
		return;
	}

	for (Integer I: entityToInfos.keySet()) {
        Collection<EntityInfo> infos = entityToInfos.get(I);
        if (infos == null) // should not happen
            continue;
        %>
        <div class="panel">

        <%
        Archive.ProcessingMetadata metadata = CrossCollectionSearch.archiveMetadatas.get(I);
        out.println ("Institution: " + Util.escapeHTML(metadata.institution) + "<br/>");
        out.println ("Repository: " + Util.escapeHTML(metadata.repository) + "<br/>");
        out.println ("Collection: " + Util.escapeHTML(metadata.collectionTitle) + "<br/>");
        %>

        <table>
            <tr><th>Entity</th><th>Messages</th><th>Confirmed</th><th>Date Range</th></tr>
            <% for (EntityInfo info: infos) { %>
            <tr>
                <td><%=Util.escapeHTML(info.displayName)%></td>
                <td><%=info.count%></td>
                <td> <input type="checkbox"/></td>
                <td> <%=CalendarUtil.formatDateForDisplay(info.firstDate)%> - <%=CalendarUtil.formatDateForDisplay(info.lastDate)%></td>
            </tr>
            <% } %>
        </table>
        </div>
    <% } %>

<p>
	<jsp:include page="footer.jsp"/>
</div>
</body>
</html>
