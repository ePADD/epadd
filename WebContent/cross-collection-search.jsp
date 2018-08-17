<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.xcoll.CrossCollectionSearch" %>
<%@ page import="edu.stanford.muse.xcoll.EntityInfo" %>
<%@ page import="java.util.Collection" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="edu.stanford.muse.email.CalendarUtil" %>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="java.io.File" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
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
		td,th {
			padding: 5px;
		}

        td.center {
            text-align:center;
        }
        td.right {
            text-align:right;
        }
	</style>
</head>
<body>
<jsp:include page="header.jspf"/>
<br/>
<br/>
<div style="width:1000px; margin-left:170px">
	<%

    String queryTerm = request.getParameter("term");
    Multimap<String, EntityInfo> entityToInfos = CrossCollectionSearch.search (queryTerm);

    out.println ("Search term: " + Util.escapeHTML(queryTerm) + "<br/><br/>");

	if (entityToInfos == null || entityToInfos.isEmpty()) { %>
		No hits.
    	<%
		return;
	}

	for (String I: entityToInfos.keySet()) {
        Collection<EntityInfo> infos = entityToInfos.get(I);
        if (infos == null) // should not happen
            continue;
        %>

    <%
        Archive.CollectionMetadata metadata = ArchiveReaderWriter.readCollectionMetadata(ArchiveReaderWriter.getArchiveForArchiveID(I).baseDir);
        out.println ("Institution: <b>" + Util.escapeHTML(metadata.institution) + "</b> &nbsp;&nbsp;");
        out.println ("Repository: <b>" + Util.escapeHTML(metadata.repository) + "</b> &nbsp;&nbsp;");
        String url = "collection-detail?collection=" + new File(ArchiveReaderWriter.getArchiveForArchiveID(I).baseDir).getName();
        out.println ("Collection: <b><a target=\"blank\" href=\"" + url + "\">" + Util.escapeHTML(metadata.collectionTitle) + "</a></b> &nbsp;&nbsp;");
    %>
        <div class="panel">

        <table>
            <tr><th>Entity</th><th>Occurrences</th><th>Correspondent</th><th>Date Range</th></tr>
            <% for (EntityInfo info: infos) { %>
            <tr><%
                    if(!info.isCorrespondent){
                        String term =  Util.escapeHTML(info.displayName);
                        String entityurl = "browse?adv-search=1&archiveID="+ info.archiveID +"&entity="+Util.URLEncode(term)+"&termSubject=on&termBody=on&termOriginalBody=on";
            %>
                <td><a href=<%=entityurl%> ><%=term%></a></td>
                <% }else{
                    String term =  Util.escapeHTML(info.displayName);
                    String entityurl = "browse?adv-search=1&archiveID="+ info.archiveID +"&correspondent="+Util.URLEncode(term)+"&correspondentTo=on&correspondentFrom=on&correspondentCc=on&correspondentBcc=on";
                %>
                <td><a href=<%=entityurl%>><%=term%></a></td>
                <%}%>

                <td style="text-align:right"><%=info.count%></td>

            <%--<td class="right"><%=info.count%></td>--%>
                <td class="center">
                    <% if (info.isCorrespondent) { %>
                        <i class="fa fa-check" aria-hidden="true"></i>
                    <% } %>
                </td>
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
