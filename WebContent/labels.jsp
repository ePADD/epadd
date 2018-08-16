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
	
	<style>
		.remove-label {
			cursor: pointer;
			border-bottom: 1px dotted #000;
		}
	</style>

</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
	String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
	writeProfileBlock(out, archive, "Labels", "");

%>

<% // new label not available in discovery mode.
  if (!ModeConfig.isDiscoveryMode()) { %>
    <div style="text-align:center;display:inline-block;vertical-align:top;margin-left:170px">
        <button class="btn-default" onclick="window.location='edit-label?archiveID=<%=archiveID%>'"><i class="fa fa-pencil-o"></i> New label</button> <!-- no labelID param, so it's taken as a new label -->
		&nbsp;&nbsp;
		<button class="btn-default" id="import-label"><i class="fa fa-pencil-o"></i> Import Labels</button>
		&nbsp;&nbsp;
		<button class="btn-default" id="export-label" onclick="exportLabelHandler()"><i class="fa fa-pencil-o"></i> Export Labels</button>

    </div>

<% } %>

<br/>
<br/>

<div style="margin:auto; width:900px">
<table id="labels" style="display:none">
	<thead><tr><th>Label</th><th>Type</th><th>Messages</th>
        <% // this column not available in discovery mode
            if (!ModeConfig.isDiscoveryMode()) { %>
            <th></th><th></th>
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
	var labels = <%=resultArray.toString(5)%>;
    var exportLabelHandler=function(){
        $.ajax({
            type: 'POST',
            url: "ajax/downloadData.jsp",
            data: {archiveID: archiveID, data: "labels"},
            dataType: 'json',
            success: function (data) {
                epadd.alert('Label description file will be downloaded in your download folder!', function () {
                    window.location=data.downloadurl;
                });
            },
            error: function (jq, textStatus, errorThrown) {
                var message = ("Error Exporting file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
                epadd.log(message);
                epadd.alert(message);
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
		    dt_right_targets = [1, 2, 3,4];
			var edit_label_link = function ( data, type, full, meta ) {
				if (full[4])  // system label
					return '<span title="System labels are not editable">Not editable</span>'; // not editable
				return '<a href="edit-label?labelID=' + full[0] + '&archiveID=<%=archiveID%>">Edit</a>';
			};

        var remove_label_link = function ( data, type, full, meta){
                if (full[4])  // system label
                    return '<span title="System labels can not be removed">Not removable</span>'; // not removable
            	return '<span class="remove-label" data-labelID="' + full[0] + '">Remove</span>';
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

            var c = confirm ('Delete the label? This action cannot be undone!');
            if (!c)
                return;

            $.ajax({
                url:'ajax/removeLabels.jsp',
                type: 'POST',
                data: {archiveID: archiveID, labelID: labelID},
                dataType: 'json',
                success: function(response) { if(response.status===0)
                    window.location.reload();
                else{
                    epadd.alert(response.error);
                }
                },
                error: function(response) { epadd.alert('There was an error in saving labels, sorry!');
                }
            });
        }

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
	                {targets: 4, render:remove_label_link},
                <% } %>

            ], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
			//IMP: the event handler for remove_label_link can only be instantiated after the initializatio nof this data table is done.
            fnInitComplete: function() { $('#spinner-div').hide(); $('#labels').fadeIn();$('.remove-label').click (removeLabelFn); }
		});
	} );

</script>

<div style="clear:both"></div>
</div>
<%--modal for specifying the label description file namd and upload button.--%>
<script>
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
            success: function(data) { if(data.status==0){epadd.success('Labels uploaded', function() { window.location.reload(); });}else{epadd.alert(data.error);}},
            error: function(jq, textStatus, errorThrown) { var message = ("Error uploading labels, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown); epadd.log (message); epadd.alert(message); }
        });

    }
</script>
<div>
	<div id="label-upload-modal" class="modal fade" style="z-index:9999">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
					<h4>NOTE: Before uploading a new label description file, make sure that no message contains any label. Otherwise the label semantics will be inconsistent. </h4><br>
                    <h4 class="modal-title">Specify the json file containing label description.</h4>
				</div>
				<div class="modal-body">
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
