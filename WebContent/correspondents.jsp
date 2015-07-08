<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="org.json.JSONArray" %>
<%@ page import="java.util.Collection" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
	<title>Correspondents</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>
	<script src="js/jquery.dataTables.min.js"></script>
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	
	<style type="text/css">
      .js #people {display: none;}
    </style>
		
	<script type="text/javascript" charset="utf-8">

	</script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
	AddressBook ab = archive.addressBook;
	String bestName = ab.getBestNameForSelf();
%>

<%writeProfileBlock(out, bestName, "", "All Correspondents");%>

<div style="text-align:center;display:inline-block;vertical-align:top;margin-left:170px">
	<button class="btn-default" onclick="window.location='graph?view=people'"><i class="fa fa-bar-chart-o"></i> Go To Graph View</button>
</div>
<br/>
<br/>

<div style="margin:auto; width:900px">
<table id="people" style="display:none">
	<thead><th>Name</th><th>Incoming messages</th><th>Outgoing messages</th><th>Mentions</th></thead>
	<tbody>
	</tbody>
</table>
<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

	<%
		out.flush(); // make sure spinner is seen
		Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
		JSONArray resultArray = ab.getCountsAsJson(docs);
	%>
	<script>
	var correspondents = <%=resultArray.toString(4)%>;
// get the href of the first a under the row of this checkbox, this is the browse url, e.g.
	$(document).ready(function() {
		var clickable_message = function ( data, type, full, meta ) {
			return '<a target="_blank" title="' + full[5] + '" href="' + full[4] + '">' + data + '</a>'; // full[4] has the URL, full[5] has the title tooltip
		};

		$('#people').dataTable({
			data: correspondents,
			pagingType: 'simple',
			order:[[2, 'desc']], // col 12 (outgoing message count), descending
			columnDefs: [{width: "550px", targets: 0}, { className: "dt-right", "targets": [ 1,2,3 ] },{width: "50%", targets: 0},{targets: 0, render:clickable_message}], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
			fnInitComplete: function() { $('#spinner-div').hide(); $('#people').fadeIn(); }
		});
	} );


</script>

<div style="clear:both"></div>
</div>
<p>
<br/>
<jsp:include page="footer.jsp"/>

</body>
</html>
