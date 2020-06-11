<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page import="edu.stanford.muse.index.EmailDocument"%>
<%@page import="org.json.JSONArray" %>
<%@page import="java.util.Collection" %>
<%@include file="getArchive.jspf" %>

<!DOCTYPE HTML>
<html>

<script src="js/jquery.js"></script>

<head>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<!-- Optional theme -->

	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css">
	<link rel="stylesheet" href="css/main.css">

	<%-- jquery was present here earlier--%>

	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/jquery.dataTables.min.js"></script>

	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>

	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>

	<style type="text/css">
      .js #people {display: none;}
	   .modal-body {
		   /* 100% = dialog height, 120px = header + footer */
		   max-height: calc(100% - 120px);
		   overflow-y: scroll;
	   }

	</style>

	<script type="text/javascript" charset="utf-8">

	</script>
</head>
<body>

<% 	if(archive != null && request.getParameter("archiveID")==null)
	request.setAttribute("archiveID", ArchiveReaderWriter.getArchiveIDForArchive(archive));
%>
<%@include file="header.jspf"%>

<title> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.head-correspondents")%> </title>

<%-- The header.jspf file was included here --%>

<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>
<nav class="menu1" role="navigation">
	<h2><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "correspondents.help.head")%></h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<div class="search-tips" style="display:block">

	<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode() || ModeConfig.isDeliveryMode()) { %>
		<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "correspondents.help.appraisal-processing-delivery")%>

	<% } else if (ModeConfig.isDiscoveryMode()) { %>
		<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "correspondents.help.discovery")%>
		<% } %>
	</div>
</nav>

<% writeProfileBlock(out, archive, edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.profile-correspondents")); %>

<%--
<div style="text-align:center;display:inline-block;vertical-align:top;margin: auto; width: 100%">
	<button class="btn-default" onclick="window.location='graph?archiveID=<%=archiveID%>&view=people'"><i class="fa fa-bar-chart-o"></i> Go To Graph View</button>
	<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
		<button class="btn-default" onclick="window.location='edit-correspondents?archiveID=<%=archiveID%>'"><i class="fa fa-pencil"></i> Edit</button>
		&nbsp;
    	<button class="btn-default" onclick="$('#correspondent-upload-modal').modal('show');"><i class="fa fa-upload"></i> Upload</button>
		&nbsp;
		<button class="btn-default" onclick="exportCorrespondentHandler()"><i class="fa fa-download"></i> Download</button>

	<% } %>
</div>
--%>
<br/>
<br/>

<div style="margin:auto; width:1100px">
	<div class="button_bar_on_datatable">
	<div title=" <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.graph")%> " class="buttons_on_datatable" onclick="window.location='graph?archiveID=<%=archiveID%>&view=people'"><img class="button_image_on_datatable" src="images/graph.svg"></div>
		<%if(!ModeConfig.isDiscoveryMode()){%>
	<div title=" <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.edit")%> " class="buttons_on_datatable" onclick="window.location='edit-correspondents?archiveID=<%=archiveID%>'"><img class="button_image_on_datatable" src="images/edit_correspondent.svg"></div>
		<div title=" <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.upload")%> " class="buttons_on_datatable" onclick="$('#correspondent-upload-modal').modal('show');"><img class="button_image_on_datatable" src="images/upload.svg"></div>
		<%}%>
		<div title=" <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.download")%> " class="buttons_on_datatable" onclick="exportCorrespondentHandler()"><img class="button_image_on_datatable" src="images/download.svg"></div>
	</div>
<table id="people" style="display:none">
	<thead><tr><th> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.name")%> </th><th> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.sent")%> </th><th> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.received")%> </th><th> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.received-from-owner")%> </th></tr></thead>
	<tbody>
	</tbody>
</table>
	<div id="spinner-div" style="text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>

	<%
		out.flush(); // make sure spinner is seen
		Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
		AddressBook ab = archive.getAddressBook();
		JSONArray resultArray = ab.getCountsAsJSON( false /* except owner */,archiveID);
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
			order:[[1, 'desc']], // col 2 (sent message count), descending
			columnDefs: [{width: "550px", targets: 0}, { className: "dt-right", "targets": [ 1,2,3 ] },{width: "50%", targets: 0},{targets: 0, render:clickable_message}], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
			fnInitComplete: function() { $('#spinner-div').hide(); $('#people').fadeIn(); }
		});
	} );

	var exportCorrespondentHandler=function(){
        $.ajax({
            type: 'POST',
            url: "ajax/downloadData.jsp",
            data: {archiveID: archiveID, data: "addressbook"},
            dataType: 'json',
            success: function (data) {
                epadd.info('<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.download-correspondents-message")%>', function () {
                    window.location = data.downloadurl;
                });
            },
            error: function (jq, textStatus, errorThrown) {
                epadd.error("Error downloading data file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
            }
        });
    };

    var uploadCorrespondentHandler=function() {
        //collect archiveID,and addressbookfile field. If  empty return false;
        var addressbookpath = $('#addressbookfile').val();
        if (!addressbookpath) {
            alert('Please provide the path of the address book');
            return false;
        }
        var form = $('#uploadcorrespondentform')[0];

        // Create an FormData object
        var data = new FormData(form);
        //hide the modal.
        $('#correspondent-upload-modal').modal('hide');
        //now send to the backend.. on it's success reload the same page. On failure display the error message.

        $.ajax({
            type: 'POST',
            enctype: 'multipart/form-data',
            processData: false,
            url: "ajax/upload-addressbook.jsp",
            contentType: false,
            cache: false,
            data: data,
            success: function () {
                epadd.success('Correspondent list uploaded and applied.', function () {
                    window.location.reload();
                });
            },
            error: function (jq, textStatus, errorThrown) {
                epadd.error("Error uploading file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
            }
        });
    }

</script>

<div style="clear:both"></div>
</div>
<p>
<br/>
<div>
	<div id="correspondent-upload-modal" class="info-modal modal fade" style="z-index:99999">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
					<h4 class="modal-title"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.upload.addr-file")%> </h4>
				</div>
				<div class="modal-body">
					<form id="uploadcorrespondentform" method="POST" enctype="multipart/form-data" >
						<input type="hidden" value="<%=archiveID%>" name="archiveID"/>
						<div class="form-group **text-left**">
							<label for="addressbookfile" class="col-sm-2 control-label **text-left**"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.upload.file")%> </label>
							<div class="col-sm-10">
								<input type="file" id="addressbookfile" name="addressbookfile" value=""/>
							</div>
						</div>
						<%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

					</form>
				</div>
				<div class="modal-footer">
					<button id="upload-lexicon-btn" class="btn btn-cta" onclick="uploadCorrespondentHandler();return false;"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "correspondents.upload.upload-button")%> <i class="icon-arrowbutton"></i></button>


					<%--<button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>--%>
					<%--<button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>--%>
				</div>
			</div><!-- /.modal-content -->
		</div><!-- /.modal-dialog -->
	</div><!-- /.modal -->
</div>

	<jsp:include page="footer.jsp"/>

</body>
</html>
