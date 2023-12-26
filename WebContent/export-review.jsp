<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="org.json.JSONArray"%>
<%@ page import="java.util.*" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
	<title>Review for Export</title>
	
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<script src="js/jquery.dataTables.min.js"></script>
		
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	
	<style type="text/css">
      .js #messages {display: none;}
    </style>
	
	<script type="text/javascript" charset="utf-8">
	 $('html').addClass('js'); // see http://www.learningjquery.com/2008/10/1-way-to-avoid-the-flash-of-unstyled-content/
	</script>
</head>
<body>
<%@include file="header.jspf"%>
<script>epadd.nav_mark_active('Export');</script>

<%
Collection<EmailDocument> docs = (Collection) archive.getAllDocs();

String type = request.getParameter("type");

// filter all docs to only get the type requested.
Collection<EmailDocument> newDocs = new ArrayList<EmailDocument>();
String description = " to be transferred to the repository";
if ("doNotTransfer".equals(type)) {
	description = "  not to be transferred";
} else if ("transferWithRestrictions".equals(type)) {
	description = " to be transferred with restrictions";
} else if ("transfer".equals(type)) {
	description = " to be transferred";
} else if ("annotated".equals(type)) {
	description = " annotated";
}
/*

	@TODO-Export Take decision on exporting based on labels of this document set
for (EmailDocument ed: docs)
{
	if ("doNotTransfer".equals(type)) {
		if (ed.doNotTransfer)
			newDocs.add(ed);
	} else if ("transferWithRestrictions".equals(type)) {
		if (ed.transferWithRestrictions)
			newDocs.add(ed);
	} else if ("annotated".equals(type)) {
		if (!Util.nullOrEmpty(ed.comment))
			newDocs.add(ed);
	} else if ("transfer".equals(type)) {
		if (!ed.transferWithRestrictions && !ed.doNotTransfer)
			newDocs.add(ed);
	}
}
*/

docs = newDocs;
//session.setAttribute("action-docs", docs);
writeProfileBlock(out, archive, Util.pluralize(docs.size(), "message") + description);%>


<br/>

<div style="margin:auto; width:900px">

	<div id="spinner-div" style="text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>
	<br/><% out.flush(); %>

	<table id="messages" style="display:none">
	<thead><tr><th>Subject</th><th>Date</th><th>Annotation</th></tr></thead>
	<tbody>

<%
	JSONArray resultArray = new JSONArray();

// compute counts
	int count = 0;
for (EmailDocument ed: docs)
{
	count++;
	String docId = ed.getUniqueId();
	String subject = ed.description;
	String comment = archive.getAnnotationManager().getAnnotation(ed.getUniqueId());
	if (Util.nullOrEmpty(subject))
		subject = "NONE";
    String messageURL = "browse?archiveID="+archiveID+"&docId=" + docId;
	JSONArray j = new JSONArray();
	j.put (0, Util.escapeHTML(subject));
	j.put (1, ed.dateString());
	j.put (2, Util.ellipsize(Util.nullOrEmpty(comment)? "NONE" : Util.escapeHTML(comment), 60));
	j.put (3, messageURL);

	resultArray.put (count-1, j);
}
%>
	</tbody>
</table>
<p>
<br/>
<div id="export-buttons" style="display:none;margin:auto;text-align:center">
<% if (Util.nullOrEmpty(type)) { %>
	<button class="btn btn-cta" onclick="window.location='export?archiveID=<%=archiveID%>'">Export <i class="icon-arrowbutton"></i> </button>
	<% } %>
</div>
</div>
<p>
<jsp:include page="footer.jsp"/>

<script>

	$(document).ready(function() {
		var clickable_message = function ( data, type, full, meta ) {
			return '<a target="_blank" href="' + full[3] + '">' + data + '</a>';
		};

		var messages = <%=resultArray.toString(4)%>;
		$('#messages').dataTable({
			data: messages,
			pagingType: 'simple',
			autoWidth: false,
			columnDefs: [{width: "540px", targets: 0},{targets: 0,render:clickable_message}],
			order:[[1, 'asc']], // col 1 (date), ascending
			fnInitComplete: function() { $('#spinner-div').hide(); $('#messages').fadeIn(); $('#nav3').fadeIn(); $('#export-buttons').fadeIn()}
		});
	} );
</script>


</body>
</html>
