<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="org.json.JSONArray" %>
<%@ page import="edu.stanford.muse.index.DataSet" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.LabelManager.LabelManager" %>
<%@ page import="edu.stanford.muse.LabelManager.Label" %>
<%@ page import="java.util.*" %>

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
	<style>
		.set-all, .unset-all {
			cursor: pointer;
            border-bottom: 1px dotted #000;
        }
	</style>

</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
	String archiveID = SimpleSessions.getArchiveIDForArchive(archive);

    String docsetID = request.getParameter("docsetID");
    if (docsetID == null)
        docsetID = "";
    DataSet browseSet = (DataSet) session.getAttribute(docsetID);

    Collection<Document> docs = (browseSet == null) ? archive.getAllDocs() : browseSet.getDocs();

    String title = Util.pluralize (docs.size(), "message");
    if (!Util.nullOrEmpty(docsetID))
        title += " (" + docsetID + ")";
    writeProfileBlock(out, archive, "Labels for ", title);
%>

<br/>
<br/>

<div style="margin:auto; width:900px">
<table id="labels" style="display:none">
	<thead><tr><th>Label</th><th>Type</th><th>Messages</th>
        <% if (!ModeConfig.isDiscoveryMode()) { %>
        <th></th>
        <th></th>
        <% } %>
    </tr></thead>
	<tbody>
	</tbody>
</table>
<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

	<%
		out.flush(); // make sure spinner is seen


		JSONArray resultArray = archive.getLabelCountsAsJson((Collection) docs);
		Set<Label> restrictionlabs = archive.getLabelManager().getAllLabels(LabelManager.LabType.RESTRICTION);
		List<String> relativeTimedRestrictions = new LinkedList<>();
		restrictionlabs.forEach(lab-> {
		    if(lab.getRestrictionType().equals(LabelManager.RestrictionType.RESTRICTED_FOR_YEARS))
		        relativeTimedRestrictions.add(lab.getLabelID());
		});
	%>
	<script>
	var labels = <%=resultArray.toString(4)%>;
	var relativeTimedRestrictions = [<% for (int i = 0; i < relativeTimedRestrictions.size(); i++) { %>"<%= relativeTimedRestrictions.get(i) %>"<%= i + 1 < relativeTimedRestrictions.size() ? ",":"" %><% } %>];
	// get the href of the first a under the row of this checkbox, this is the browse url, e.g.
	$(document).ready(function() {
		var clickable_message = function ( data, type, full, meta ) {
			return '<a target="_blank" title="' + escapeHTML(full[2]) + '" href="browse?adv-search=1&labelIDs=' + full[0] + '&archiveID=<%=archiveID%>">' + escapeHTML(full[1]) + '</a>'; // full[4] has the URL, full[5] has the title tooltip
		};

        var label_count = function(data, type, full, meta) { return full[3]; };
        var label_type = function(data, type, full, meta) { return full[5]; };

        var set_link = function(data, type, full, meta) {
			return '<span class="set-all" data-labelID="' + full[0] + '">Set for all</span>'; // not editable
        };

        var unset_link = function(data, type, full, meta) {
            return '<span class="unset-all" data-labelID="' + full[0] + '">Unset for all</span>'; // not editable
        };

        $('#labels').dataTable({
			data: labels,
			pagingType: 'simple',
			order:[[1, 'desc']], // (message count), descending
			columnDefs: [
                { className: "dt-right", "targets": [ 1, 2, 3 ] },
                {targets: 0, width: "400px", render:clickable_message},
                {targets: 1, render:label_type},
                {targets: 2, render:label_count},
                <% if (!ModeConfig.isDiscoveryMode()) { %>
                    {targets: 3, render:set_link},
                    {targets: 4, render:unset_link},
                <% } %>
            ], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
            fnInitComplete: function() { $('#spinner-div').hide(); $('#labels').fadeIn(); }
		});
	} );

	// labelID will be a single item here, even though applyLabelsAnnotations can support more than one
	function do_action (labelID, action) {
	    //if this label is of type time restrction with relative time then inform the user that this label will not be set for messages with unidentified dates
		//if this label is of type 'cleared for release' then inform the user that this label will only be set of messages with general restriction labels or with restriction labels where time has expired.
		var msg="";
		/*if(action=="set") {
            if (labelID === "<%=archive.getLabelManager().LABELID_CFR%>")
                msg = "This label will be set for only those messages where time restriction has expired or which contain only general restriction labels. All the messages with \"Cleared for release\" label will " +
                    "be exported to the next module. Be careful!";

            if (relativeTimedRestrictions.indexOf(labelID) > -1)//means labelID is a relative timed restriction label.
                msg = "This label will be set for only those messages where a valid date is present. Note that ePADD puts a dummy date 1 Jan 1960 for the messages with corrupt date field.";
        }
*/		var c = confirm (msg+' Do you want to ' + action + ' this label for these messages?');
        if (!c)
            return;

        //$('#alert-modal').on('hidden.bs.modal', window.location.reload());
        $.ajax({
            url:'ajax/applyLabelsAnnotations.jsp',
            type: 'POST',
            data: {archiveID: archiveID, labelIDs: labelID, docsetID: '<%=docsetID%>', action:action}, // labels will go as CSVs: "0,1,2" or "id1,id2,id3"
            dataType: 'json',
            success: function(response) { if(response.status===1) {
				alert(response.errorMessage);
				window.location.reload();
            }else
                window.location.reload();
            },
            error: function() { epadd.alert('There was an error in saving labels, sorry!');
            }
        });
    }

	$(document).ready(function() {
		$('.set-all').click (function(e) {
		    labelID = $(e.target).attr ('data-labelID');
            do_action (labelID, 'set');
        });
        $('.unset-all').click (function(e) {
            labelID = $(e.target).attr ('data-labelID');
            do_action (labelID, 'unset');
        });
	});

</script>

<div style="clear:both"></div>
</div>
<p>
<br/>
<jsp:include page="footer.jsp"/>

</body>
</html>
