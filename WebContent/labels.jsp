<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="org.json.JSONArray" %>
<%@ page import="java.util.Collection" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
	<title>Labels</title>
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
	
	<script type="text/javascript" charset="utf-8">

	</script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
	String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	writeProfileBlock(out, archive, "Labels", "");

%>

<% // new label not available in discovery mode.
  if (!ModeConfig.isDiscoveryMode()) { %>
    <div style="text-align:center;display:inline-block;vertical-align:top;margin-left:170px">
        <button class="btn-default" onclick="window.location='edit-label?archiveID=<%=archiveID%>'"><i class="fa fa-pencil-o"></i> New label</button> <!-- no labelID param, so it's taken as a new label -->
    </div>
<% } %>

<br/>
<br/>

<div style="margin:auto; width:900px">
<table id="labels" style="display:none">
	<thead><tr><th>Label</th><th>Type</th><th>Messages</th>
        <% // this column not available in discovery mode
            if (!ModeConfig.isDiscoveryMode()) { %>
            <th></th>
        <% } %>
    </tr></thead>
	<tbody>
	</tbody>
</table>
<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

	<%
		out.flush(); // make sure spinner is seen
		Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
		JSONArray resultArray = archive.getLabelCountsAsJson((Collection) docs);
	%>
	<script>
	var labels = <%=resultArray.toString(4)%>;
// get the href of the first a under the row of this checkbox, this is the browse url, e.g.
	$(document).ready(function() {
		var clickable_message = function ( data, type, full, meta ) {
			return '<a target="_blank" title="' + escapeHTML(full[2]) + '" href="browse?adv-search=1&labelIDs=' + full[0] + '&archiveID=<%=archiveID%>">' + escapeHTML(full[1]) + '</a>'; // full[4] has the URL, full[5] has the title tooltip
		};

		var dt_right_targets = [1, 2]; // only 2 in discovery mode, convert to [1, 2, 3] in other modes
        <% if (!ModeConfig.isDiscoveryMode()) { %>
		    dt_right_targets = [1, 2, 3];
			var edit_label_link = function ( data, type, full, meta ) {
				if (full[4])  // system label
					return '<span title="System labels are not editable">Not editable</span>'; // not editable
				return '<a href="edit-label?labelID=' + full[0] + '&archiveID=<%=archiveID%>">Edit</a>';
			};
        <% } %>

        var label_count = function(data, type, full, meta) { return full[3]; };
        var label_type = function(data, type, full, meta) { return full[5]; };

        $('#labels').dataTable({
			data: labels,
			pagingType: 'simple',
			order:[[1, 'desc']], // (message count), descending
			columnDefs: [
                { className: "dt-right", "targets": dt_right_targets },
                {targets: 0, width: "400px", render:clickable_message},
                {targets: 1, render:label_type},
                {targets: 2, render:label_count},
                <% if (!ModeConfig.isDiscoveryMode()) { %>
                    {targets: 3, render:edit_label_link},
                <% } %>

            ], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
            fnInitComplete: function() { $('#spinner-div').hide(); $('#labels').fadeIn(); }
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
