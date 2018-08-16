<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
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
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
	AddressBook ab = archive.addressBook;
	String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
	writeProfileBlock(out, archive, "All Correspondents", "");
%>

<div style="text-align:center;display:inline-block;vertical-align:top;margin-left:30px">
	<button class="btn-default" onclick="window.location='graph?archiveID=<%=archiveID%>&view=people'"><i class="fa fa-bar-chart-o"></i> Go To Graph View</button>
	<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
		<button class="btn-default" onclick="window.location='edit-correspondents?archiveID=<%=archiveID%>'"><i class="fa fa-pencil"></i> Edit</button>
		&nbsp;
    	<button class="btn-default" onclick="$('#correspondent-upload-modal').modal('show');"><i class="fa fa-upload"></i> Upload</button>
		&nbsp;
		<button class="btn-default" onclick="exportCorrespondentHandler()"><i class="fa fa-download"></i> Download</button>

	<% } %>
</div>
<br/>
<br/>

<div style="margin:auto; width:1100px">
<table id="people" style="display:none">
	<thead><tr><th>Name</th><th>Incoming messages</th><th>Outgoing messages</th><th>Mentions</th></tr></thead>
	<tbody>
	</tbody>
</table>
<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

	<%
		out.flush(); // make sure spinner is seen
		Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
		JSONArray resultArray = ab.getCountsAsJson(docs, true /* except owner */,archiveID);
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

	var exportCorrespondentHandler=function(){
        $.ajax({
            type: 'POST',
            url: "ajax/downloadData.jsp",
            data: {archiveID: archiveID, data: "addressbook"},
            dataType: 'json',
            success: function (data) {
                epadd.alert('Correspondent list will be downloaded in your browser\'s download folder.', function () {
                    window.location = data.downloadurl;
                }, '' /* no Alert title */);
            },
            error: function (jq, textStatus, errorThrown) {
                var message = ("Error Exporting file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
                epadd.log(message);
                epadd.alert(message);
            }
        });
    }

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
            success: function (data) {
                epadd.alert('Correspondent list uploaded successfully!', function () {
                    window.location.reload();
                });
            },
            error: function (jq, textStatus, errorThrown) {
                var message = ("Error uploading file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
                epadd.log(message);
                epadd.alert(message);
            }
        });
    }

</script>

<div style="clear:both"></div>
</div>
<p>
<br/>
<div>
	<div id="correspondent-upload-modal" class="modal fade" style="z-index:9999">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
					<h4 class="modal-title">Specify the Addressbook file</h4>
				</div>
				<div class="modal-body">
					<form id="uploadcorrespondentform" method="POST" enctype="multipart/form-data" >
						<input type="hidden" value="<%=archiveID%>" name="archiveID"/>
						<div class="form-group **text-left**">
							<label for="addressbookfile" class="col-sm-2 control-label **text-left**">File</label>
							<div class="col-sm-10">
								<input type="file" id="addressbookfile" name="addressbookfile" value=""/>
							</div>
						</div>
						<%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

					</form>
				</div>
				<div class="modal-footer">
					<button id="upload-lexicon-btn" class="btn btn-cta" onclick="uploadCorrespondentHandler();return false;">Upload <i class="icon-arrowbutton"></i></button>


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
