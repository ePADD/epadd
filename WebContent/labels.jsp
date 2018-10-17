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

	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->

	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css">
	<link rel="stylesheet" href="css/main.css">

	<script src="js/jquery.js"></script>
	<script src="js/jquery.dataTables.min.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>

	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	


</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>
<nav class="menu1" role="navigation">
	<h2><b>Using labels</b></h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<div class="search-tips" style="display:block">

		<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
			<p>[icon 1]  Create a new label.
			<p>[icon 2]  Download a .json label description file.
			<p>[icon 3]  Import a .json label description file.
			<p>General labels can be used to describe to a set of messages, to mark a set of messages as reviewed, or for any other purpose. General labels are not machine-actionable.
			<p>General labels will export from the Appraisal module to the Processing module, but will not export to the Discovery module or to/from the Delivery module.
			<p>Restriction labels can be used to restrict messages, including for a certain period from the current date, or from the date of creation.
			<p>Restricted messages (and associated restriction labels) will export from the Appraisal module to the Processing module, but will not export to the Discovery or Delivery modules unless they are also assigned the “Cleared for release” label within the Appraisal or Processing modules.
			<p>All labels are searchable via Advanced Search.
			<p>Set default labels for all messages from the Dashboard, under the More option.
		<% } else if (ModeConfig.isDeliveryMode()) { %>
			<p>[icon 1]  Create a new label.
			<p>[icon 2]  Download a .json label description file.
			<p>[icon 3]  Import a .json label description file.
			<p>Labels can be used to describe a set of messages or to mark a set of messages as reviewed.
			<p>Labels will not export.
		<% } %>
	</div>
</nav>
<%
	String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
	writeProfileBlock(out, archive, "Manage labels");

%>

<% // new label not available in discovery mode.
  if (!ModeConfig.isDiscoveryMode()) { %>
<div style="margin:auto; width:1100px">
		<div class="button_bar_on_datatable">
			<div title="Create label" class="buttons_on_datatable" onclick="window.location='edit-label?archiveID=<%=archiveID%>'"><img class="button_image_on_datatable" src="images/add_label.svg"></div>
			<div title="Upload label description" class="buttons_on_datatable" id="import-label"><img class="button_image_on_datatable" src="images/upload.svg"></div>
			<div title="Download label description" class="buttons_on_datatable" onclick=exportLabelHandler()><img class="button_image_on_datatable" src="images/download.svg"></div>
		</div>
<% }else{ %>
<div style="margin:auto; width:1100px">
<%} %>
<table id="labels" style="display:none;">
	<thead><tr><th>Label</th><th>Type</th><th>Messages</th>
        <% // this column not available in discovery mode
            if (!ModeConfig.isDiscoveryMode()) { %>
            <th>Actions</th>
        <% } %>
    </tr></thead>
	<tbody>
	</tbody>
</table>
<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

	<div style="clear:both"></div>
	<br/>
	<a href="bulk-labels?archiveID=<%=archiveID%>&allDocs=1">Set labels for all messages</a>
</div>

	<%
		out.flush(); // make sure spinner is seen
		Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
		JSONArray resultArray = archive.getLabelCountsAsJson((Collection) docs);
	%>
	<script>
	var labels = <%=resultArray.toString(5)%>;
    var exportLabelHandler=function(){
        $.ajax({
            type: 'POST',
            url: "ajax/downloadData.jsp",
            data: {archiveID: archiveID, data: "labels"},
            dataType: 'json',
            success: function (data) {
                epadd.info_confirm_continue('A label description file called label-info.json will be downloaded in your download folder.', function () {
                    window.location=data.downloadurl;
                });
            },
            error: function (jq, textStatus, errorThrown) {
                epadd.error("Error exporting file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
            }
        });
    }

// get the href of the first a under the row of this checkbox, this is the browse url, e.g.
	$(document).ready(function() {
		var clickable_message = function ( data, type, full, meta ) {
			return '<a target="_blank" title="' + escapeHTML(full[2]) + '" href="browse?adv-search=1&labelIDs=' + full[0] + '&archiveID=<%=archiveID%>">' + escapeHTML(full[1]) + '</a>'; // full[4] has the URL, full[5] has the title tooltip
		};

		var dt_right_targets = [1, 2]; // only 2 in discovery mode, convert to [1, 2, 3] in other modes
        <% if (!ModeConfig.isDiscoveryMode()) { %>
		    dt_right_targets = [1, 2, 3];
			var edit_remove_label_link = function ( data, type, full, meta ) {
			    var returnedhtml='';
				if (full[4])  // system label
                    returnedhtml+='<div title="System labels are not editable" class="buttons_on_datatable_row"><img class="button_image_on_datatable_row disabledDiv" src="images/add_label.svg"></div>'; // not removable
				else
				    returnedhtml+= '<div title="Edit label info" class="buttons_on_datatable_row" onclick="window.location=\'edit-label?labelID='+full[0]+'&archiveID=<%=archiveID%>\'"><img class="button_image_on_datatable_row" src="images/add_label.svg"></div>';
                if (full[4])  // system label
                    returnedhtml+= '<div title="System label can not be removed" class="buttons_on_datatable_row" ><img class="button_image_on_datatable_row disabledDiv" src="images/delete.svg"></div>'; // not removable
                else
                    returnedhtml+= '<div title="Remove label" class="buttons_on_datatable_row remove-label" data-labelID="'+full[0]+'"><img class="button_image_on_datatable_row" src="images/delete.svg"></div>';
				return returnedhtml;
            };

        var remove_label_link = function ( data, type, full, meta){
                if (full[4])  // system label
                    return '<div title="System label can not be removed" class="buttons_on_datatable_row" ><img class="button_image_on_datatable_row disabledDiv" src="images/delete.svg"></div>'; // not removable
	            return '<div title="Remove label" class="buttons_on_datatable_row remove-label" data-labelID="'+full[0]+'"><img class="button_image_on_datatable_row" src="images/delete.svg"></div>';
            //          return '<span title="System labels can not be removed">Not removable</span>'; // not removable
            //	return '<span class="remove-label" data-labelID="' + full[0] + '">Remove</span>';
//			return '<a href="" data-attr = full[0] onclick="return removereq(full[0])">Remove</div>';
            }
		$('#import-label').click(function(){
		   //open modal box to get the json file path that contains label description.
            $('#label-upload-modal').modal('show');
        });
		<% } %>


        //function to actually remove the label (send ajax request etc.)
        var removeLabelFn= function(e) {
            labelID = $(e.target).attr ('data-labelID');

            epadd.warn_confirm_continue('Delete the label? This action cannot be undone.', function() {
                $.ajax({
                    url:'ajax/removeLabels.jsp',
                    type: 'POST',
                    data: {archiveID: archiveID, labelID: labelID},
                    dataType: 'json',
                    success: function(response) {
                        if(response.status===0)
                        	window.location.reload();
	                    else {
    	                    epadd.error('Sorry, there was an error in saving labels.' + response.error);
        	            }
                    },
                    error: function(response) {
                        epadd.error('Sorry, there was an error in saving labels.');
                    }
                });
            });
        }

        var label_count = function(data, type, full, meta) { return full[3]; };
        var label_type = function(data, type, full, meta) { return full[5]; };

        $('#labels').dataTable({
			data: labels,
			pagingType: 'simple',
			order:[[1, 'desc']], // (message count), descending
			columnDefs: [
                { className: "dt-center", "targets": dt_right_targets },
                {targets: 0, width: "400px", render:clickable_message},
                {targets: 1, render:label_type},
                {targets: 2, render:label_count},
                <% if (!ModeConfig.isDiscoveryMode()) { %>
                    {targets: 3, render:edit_remove_label_link}
	               // {targets: 4, render:remove_label_link},
                <% } %>

            ], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
			//IMP: the event handler for remove_label_link can only be instantiated after the initializatio nof this data table is done.
            fnInitComplete: function() { $('#spinner-div').hide(); $('#labels').fadeIn();$('.remove-label').click (removeLabelFn); }
		});
	} );

    var uploadLabelHandler=function(){
        //collect archiveID and labeljson field. If labeljson is empty alert and return false;
        var filename = $('#labeljson').val();
        if(!filename)
        {
            alert('Please provide the path of the json file ');
            return false;
        }
        var form = $('#uploadjsonform')[0];

        // Create an FormData object
        var data = new FormData(form);
        //hide the modal.
        $('#label-upload-modal').modal('hide');
        //now send to the backend.. on it's success reload the labels page. On failure display the error message.

        $.ajax({
            type: 'POST',
            enctype: 'multipart/form-data',
            processData: false,
            url: "ajax/upload-labels.jsp",
            contentType: false,
            cache: false,
            data: data,
            success: function(data) {
                if (data.status==0){
                    epadd.success('Labels uploaded', function() { window.location.reload(); });
                } else {
					epadd.error('Error uploading labels' + data.error);
				}
            },
            error: function(jq, textStatus, errorThrown) { var message = ("Error uploading labels, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown); epadd.error(message); }
        });

    }
</script>


<%--modal for specifying the label description and upload button.--%>
<div>
	<div id="label-upload-modal" class="modal fade" style="z-index:9999">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>

				</div>
				<div class="modal-body">
					<p>
					To import new labels, ensure that you currently have no labels applied to messages. If you do, ePADD cannot import new labels.
					<p>
					Upload a JSON file containing label descriptions.
					<br/>
					<form id="uploadjsonform" method="POST" enctype="multipart/form-data" >
						<input type="hidden" value="<%=archiveID%>" name="archiveID"/>
						<input type="file" id="labeljson" name="labeljson" value=""/>
						<%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

				</form>
				</div>
				<div class="modal-footer">
					<button id="upload-label-btn" class="btn btn-cta" onclick="uploadLabelHandler();return false;">Upload <i class="icon-arrowbutton"></i></button>


				<%--<button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>--%>
					<%--<button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>--%>
				</div>
			</div><!-- /.modal-content -->
		</div><!-- /.modal-dialog -->
	</div><!-- /.modal -->
</div>

<p>
<br/>
<jsp:include page="footer.jsp"/>

</body>
</html>
