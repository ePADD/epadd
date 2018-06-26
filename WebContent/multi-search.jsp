<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="com.google.common.collect.LinkedHashMultimap"%>
<%@page language="java" import="com.google.common.collect.Multimap"%>
<%@page language="java" import="edu.stanford.muse.datacache.Blob"%>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="java.util.Set" %>
<%@ page import="edu.stanford.muse.index.*" %>
<%@include file="getArchive.jspf" %>

<!-- Input: Field name
     Renders a table containing the list of entities 
     corresponding to this field
-->
<!DOCTYPE HTML>
<html>
<head>
	<title>Multiple search</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<script src="js/jquery.js"></script>
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<script src="js/jquery.dataTables.min.js"></script>
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>
	<style type="text/css">
		.js #terms {display: none;}
	</style>

</head>
<body>
<jsp:include page="header.jspf"/>

<script>epadd.nav_mark_active('Browse');</script>

<%
	String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
	writeProfileBlock(out, archive, "", "");
%>

<br/>

<div style="margin:auto; width:900px">

	<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

<%
		out.flush();

    String[] searchTerms = request.getParameterValues("term");

    JSONArray resultArray = new JSONArray();
    int count = 0;
    for (String term: searchTerms) {
//		int nDocs = archive.indexer.getNumHits(term, false, Indexer.QueryType.FULL);
        Multimap<String, String> params = LinkedHashMultimap.create();
        params.put("termSubject", "on");
        params.put("termBody", "on");
        params.put("termAttachments", "on");
		String searchTerm = term;
		if (!(searchTerm.length() > 2 && searchTerm.startsWith("\"") && searchTerm.endsWith("\"")))
			searchTerm = "\"" + searchTerm + "\"";
		SearchResult inputSet = new SearchResult(archive,params);

		SearchResult resultSet = SearchResult.searchForTerm (inputSet, searchTerm);
        int nDocs = resultSet.getDocumentSet().size();
        JSONArray j = new JSONArray();
        j.put (0, Util.escapeHTML(term));
        j.put (1, nDocs);
        resultArray.put (count++, j);
    }

%>
<table id="terms" style="display:none">
	<thead><tr><th>Term</th><th>Messages</th></tr></thead>
	<tbody>
	</tbody>
</table>
</div>
<p>
<br/>

	<script type="text/javascript">

		$(document).ready(function() {
		    var archiveID = '<%=archiveID%>';
			var click_to_search = function (data) {
				// epadd.do_search will open search result in a new tab.
				// Note, we have to insert onclick into the rendered HTML,
				// we were earlier trying $('.search').click(epadd.do_search) - this does not work because only the few rows initially rendered to html match the $('.search') selector, not the others
				return '<span style="cursor:pointer" onclick="epadd.do_search_incl_attachments(event, archiveID)">' + data + '</span>';
			};

			var entities = <%=resultArray.toString(4)%>;
			$('#terms').dataTable({
				data: entities,
				pagingType: 'simple',
				columnDefs: [{ className: "dt-right", "targets": [ 1 ] },{width: "630px", targets: 0},{targets: 0, render:click_to_search}],
				order:[[1, 'desc']], // col 1 (entity message count), descending
				fnInitComplete: function() { $('#spinner-div').hide(); $('#terms').fadeIn(); }
			});
		} );
	</script>

	<jsp:include page="footer.jsp"/>
</body>
</html>
