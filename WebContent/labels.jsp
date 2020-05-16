<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="org.json.JSONArray" %>
<%@ page import="java.util.Collection" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>

<script src="js/jquery.js"></script>
<%@include file="header.jspf"%>

<head>
	<title><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.head-labels")%></title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->

	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css">
	<link rel="stylesheet" href="css/main.css">

	<%-- Jquery was present here earlier --%>
	<script src="js/jquery.dataTables.min.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>

	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	


</head>
<body>
	<%-- The header.jspf file was included here --%>

<script>epadd.nav_mark_active('Browse');</script>

<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>
<nav class="menu1" role="navigation">
	<h2><b><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"help","labels.help.head")%></b></h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<div class="search-tips" style="display:block">

		<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
			<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"help","labels.help.appraisal-processing")%>

		<% } else if (ModeConfig.isDeliveryMode()) { %>
			<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"help","labels.help.delivery")%>
		<% } %>
	</div>
</nav>
<%
	writeProfileBlock(out, archive, edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.manage-labels") );
%>
<div style="margin:auto; width:1100px">

	<div id="spinner-div" style="text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>

<%--new label not available in discovery mode. --%>
	<div class="button_bar_on_datatable">
		<%if(!ModeConfig.isDiscoveryMode()){%>
		<div title="<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.create-label")%>" class="buttons_on_datatable" onclick="window.location='edit-label?archiveID=<%=archiveID%>'"><img class="button_image_on_datatable" src="images/add_label.svg"></div>
		<div title="<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.upload-label-desc")%>" class="buttons_on_datatable" id="import-label"><img class="button_image_on_datatable" src="images/upload.svg"></div>
		<%}%>
		<div title="<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.download-label-desc")%>" class="buttons_on_datatable" onclick=exportLabelHandler()><img class="button_image_on_datatable" src="images/download.svg"></div>
		<%if(!ModeConfig.isDiscoveryMode()){%>
		<div title="<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.set-labels-for-all-mess")%>" class="buttons_on_datatable" onclick="window.location='bulk-labels?archiveID=<%=archiveID%>&allDocs=1'"><img class="button_image_on_datatable" src="images/labels.svg"></div>
	</div>
		<%}%>

	<table id="labels" style="display:none;">
		<thead><tr><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.label")%></th><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.type")%></th><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.messages")%></th>
			<% // this column not available in discovery mode
				if (!ModeConfig.isDiscoveryMode()) { %>
				<th><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.actions")%></th>
			<% } %>
		</tr></thead>
		<tbody>
		</tbody>
	</table>


	<br/>
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
                epadd.info_confirm_continue('<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.download-label-desc-message")%>', function () {
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
            labelID = $(e.target).closest('.buttons_on_datatable_row').attr ('data-labelID');

            epadd.warn_confirm_continue(' Do you want to delete this label? This action cannot be undone, and will also remove the label from any messages to which the label has been applied. If you wish to replace this label with a new label, cancel this action and first apply the new label to this set, then delete this  label.\n',
				function() {
                $.ajax({
                    url:'ajax/removeLabels.jsp',
                    type: 'POST',
                    data: {archiveID: archiveID, labelID: labelID},
                    dataType: 'json',
                    success: function(response) {
                        if(response.status===0)
                        	window.location.reload();
	                    else {
    	                    epadd.error('There was an error deleting the label. Please try again, and if the error persists, report it to epadd_project@stanford.edu.' + response.error);
        	            }
                    },
                    error: function(response) {
                        epadd.error('There was an error deleting the label. Please try again, and if the error persists, report it to epadd_project@stanford.edu.');
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
					epadd.error('There was an error uploading the label file. Please try again, and if the error persists, report it to epadd_project@stanford.edu. Details: ' + data.error);
				}
            },
            error: function(jq, textStatus, errorThrown) { var message = ("Error uploading labels, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown); epadd.error(message); }
        });

    }
</script>


<%--modal for specifying the label description and upload button.--%>
<div>
	<div id="label-upload-modal" class="info-modal modal fade" style="z-index:99999">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>

				</div>
				<div class="modal-body">
					<p>
							<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.upload-label-desc-message")%>
					<br/>
					<form id="uploadjsonform" method="POST" enctype="multipart/form-data" >
						<input type="hidden" value="<%=archiveID%>" name="archiveID"/>
						<input type="file" id="labeljson" name="labeljson" value=""/>
						<%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

				</form>
				</div>
				<div class="modal-footer">
					<button id="upload-label-btn" class="btn btn-cta" onclick="uploadLabelHandler();return false;"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "labels.upload-label.upload-button")%> <i class="icon-arrowbutton"></i></button>


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
