<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="java.io.File" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.index.Archive" %>

<html>
<head>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<link rel="stylesheet" href="css/jquery-ui.css"/>
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<link href="css/bootstrap-treeview.css" rel="stylesheet">

	<jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/jquery-ui/jquery-ui.min.js" type="text/javascript"></script>
	<script src="js/bootstrap-treeview.js" type="text/javascript"></script>

	<script src="js/muse.js" type="text/javascript"></script>
	<script src="js/epadd.js" type="text/javascript"></script>

	<style>
		.div-input-field { display: inline-block; width: 400px; margin-left: 20px; line-height:10px; padding:20px;}
		.input-field {width:350px;}
		.input-field-label {font-size: 12px;}

		.btn-cta {
			color: #fff;
			background-color: #0075bb;
			text-shadow: none;
			border-radius: 3px;
			-moz-transition: 0.3s ease;
			transition: 0.3s ease;
			text-transform: uppercase;
			font-weight: bold;
			box-shadow: 2px 2px 2px rgba(0,0,0,0.15);
			height: 47px;
			padding-top: 10px;
			/*	width:120px; */
		}
		.btn-cta i{ opacity:0.5  }
		.btn-cta:hover, .btn-cta:focus {
			background-color: #0075bb;
			color: #fff;
			box-shadow:0px 0px 0px rgba(0,0,0,0.15);

			-moz-transition:0.3s ease;
			transition:0.3s ease;
		}
		.btn-cta:hover i{ opacity:1;}

		.container { width: 1100px; margin: auto; padding: 0px;}
	</style>
</head>
<body style="background-color: white">

<%-- The header.jspf file was included here --%>
<%@include file="header.jspf"%>

<title>Edit Metadata</title>
<%
	String collectionFolder = request.getParameter("collection");
	String accessionId = request.getParameter("accession");
	archiveID = request.getParameter("archiveID");

	// Currently, we assume there should be one and only one targeted collection to navigate from.
	// If there is no defined, just return

	// This restriction is not available now. User is allowed to prepare metadatas in Appriasal module, too, when any collection is yet created
	//if (collectionFolder ==null)
	//	return;

%>

<script>epadd.nav_mark_active('Collections');</script>
<p>
<%
	if (ModeConfig.isAppraisalMode()) {
%>
<%
//	2022-08-24
        String browseTop;    
        if (archiveID != null) { 
            browseTop = "<a href=\"browse-top?archiveID=" + archiveID + "\">";
        } else {
            browseTop = "<a href=\"browse-top\">";
        }
%>          
	<!--div style="text-align: left; margin:auto; width: 1100px; position: relative;"> Appraisal  &nbsp;&nbsp;&nbsp;&nbsp;| &nbsp;&nbsp;&nbsp;&nbsp; <a href="browse-top?archiveID=<%=archiveID%>">Browse this Collection</a> &nbsp;&nbsp;&nbsp;&nbsp;| &nbsp;&nbsp;&nbsp;&nbsp; Edit Metadata</div-->
	<div style="text-align: left; margin:auto; width: 1100px; position: relative;"> Appraisal  &nbsp;&nbsp;&nbsp;&nbsp;| &nbsp;&nbsp;&nbsp;&nbsp; <%=browseTop%>Browse this Collection</a> &nbsp;&nbsp;&nbsp;&nbsp;| &nbsp;&nbsp;&nbsp;&nbsp; Edit Metadata</div>
<%
	} else {
%>
<div style="text-align: left; margin:auto; width: 1100px; position: relative;"> Processing  &nbsp;&nbsp;&nbsp;&nbsp;| &nbsp;&nbsp;&nbsp;&nbsp; <a href="collection-detail?collection=<%=collectionFolder%>">About this Collection</a> &nbsp;&nbsp;&nbsp;&nbsp;| &nbsp;&nbsp;&nbsp;&nbsp; Edit Metadata</div>
	<% } %>
<br/>
<div style="text-align: left; margin:auto; width: 1100px; position: relative;">

	<!-- Declaration of Tree view stuff-->
	<jsp:include page="edit-metadata-tree.jspf"/>

	<!-- Declaration of Right panel tabs-->
	<jsp:include page="edit-metadata-collection.jspf?archiveID=<%=archiveID%>"/>
	<jsp:include page="edit-metadata-accession.jspf?archiveID=<%=archiveID%>"/>
	<jsp:include page="edit-metadata-file.jspf?archiveID=<%=archiveID%>"/>

</div>

<script>

	function showRightPanelView(context) {

		if (context=="Collection"){
			$('#collection-tabs').show();
		} else if (context=="Accession"){
			$('#accession-tabs').show();
		} else {
			$('#mbox-tabs').show();
		}
	}

	function hideRightPanelView(context) {

		if (context=="Collection"){
			$('#collection-tabs').hide();
		} else if (context=="Accession"){
			$('#accession-tabs').hide();
		} else {
			$('#mbox-tabs').hide();
		}
	}

	function fillCollectionMetadataFields(node){
		$('#institution').val(node.institution);
		$('#repository').val(node.repository);
		$('#collectionTitle').val(node.collectionTitle);
		$('#shortTitle').val(node.shortTitle);

		if (node.collectionID != 'Collection ID: Unassigned')
			$('#collectionID').val(node.collectionID);

		$('#findingAidLink').val(node.findingAidLink);
		$('#catalogRecordLink').val(node.catalogRecordLink);
		$('#contactEmail').val(node.contactEmail);
		$('#shortDescription').val(node.shortDescription);
		$('#collectionAbout').val(node.about);
		$('#rights').val(node.rights);
		$('#notes').val(node.notes);
		$('#scopeAndContent').val(node.scopeAndContent);

		$('#archivalHistory').val(node.archivalHistory);
		$('#description').val(node.description);
		$('#access').val(node.access);
		$('#embargoReviewDate').val(node.embargoReviewDate);
		$('#embargoStartDate').val(node.embargoStartDate);
		$('#embargoDuration').val(node.embargoDuration);
		$('#embargoEndDate').val(node.embargoEndDate);
		$('#sensitivityReview').val(node.sensitivityReview);
		$('#processingNote').val(node.processingNote);

		$('#preservationLevelRole').val(node.preservationLevelRole);
		$('#preservationLevelRationale').val(node.preservationLevelRationale);
		$('#environmentCharacteristic').val(node.environmentCharacteristic);
		$('#relatedEnvironmentPurpose').val(node.relatedEnvironmentPurpose);
		$('#environmentNote').val(node.environmentNote);
		$('#softwareName').val(node.softwareName);
		$('#softwareVersion').val(node.softwareVersion);
		$('#rightsStatementIdentifierType').val(node.rightsStatementIdentifierType);
		$('#rightsStatementIdentifierValue').val(node.rightsStatementIdentifierValue);
		$('#statuteJurisdiction').val(node.statuteJurisdiction);
		$('#statuteDocumentationIdentifierType').val(node.statuteDocumentationIdentifierType);
		$('#statuteDocumentationIdentifierValue').val(node.statuteDocumentationIdentifierValue);
		$('#statuteDocumentationRole').val(node.statuteDocumentationRole);

		if ( node.access == '<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.access.accessible.value")%>') {
			toggleEmbargoFieldEditable(false);
		}
	}

	function updateCollectionNodeWithMetadata(){

		var selectedNode = $('#tree').treeview('getSelected');

		selectedNode[0].institution = $('#institution').val();
		selectedNode[0].repository = $('#repository').val();
		selectedNode[0].collectionTitle = $('#collectionTitle').val();
		selectedNode[0].shortTitle = $('#shortTitle').val();
		selectedNode[0].collectionID = $('#collectionID').val();
		selectedNode[0].findingAidLink = $('#findingAidLink').val();
		selectedNode[0].catalogRecordLink = $('#catalogRecordLink').val();
		selectedNode[0].contactEmail = $('#contactEmail').val();
		selectedNode[0].shortDescription = $('#shortDescription').val();
		selectedNode[0].about = $('#collectionAbout').val();
		selectedNode[0].rights = $('#rights').val();
		selectedNode[0].notes = $('#notes').val();
		selectedNode[0].scopeAndContent = $('#scopeAndContent').val();

		selectedNode[0].archivalHistory = $('#archivalHistory').val();
		selectedNode[0].description = $('#description').val();
		selectedNode[0].access = $('#access').val();
		selectedNode[0].embargoReviewDate = $('#embargoReviewDate').val();
		selectedNode[0].embargoStartDate = $('#embargoStartDate').val();
		selectedNode[0].embargoDuration = $('#embargoDuration').val();
		selectedNode[0].embargoEndDate = $('#embargoEndDate').val();
		selectedNode[0].sensitivityReview = $('#sensitivityReview').val();
		selectedNode[0].processingNote = $('#processingNote').val();

		selectedNode[0].preservationLevelRole = $('#preservationLevelRole').val();
		selectedNode[0].preservationLevelRationale = $('#preservationLevelRationale').val();
		selectedNode[0].environmentCharacteristic = $('#environmentCharacteristic').val();
		selectedNode[0].relatedEnvironmentPurpose = $('#relatedEnvironmentPurpose').val();
		selectedNode[0].environmentNote = $('#environmentNote').val();
		selectedNode[0].softwareName = $('#softwareName').val();
		selectedNode[0].softwareVersion = $('#softwareVersion').val();
		selectedNode[0].rightsStatementIdentifierType = $('#rightsStatementIdentifierType').val();
		selectedNode[0].rightsStatementIdentifierValue = $('#rightsStatementIdentifierValue').val();

		selectedNode[0].statuteJurisdiction = $('#statuteJurisdiction').val();
		selectedNode[0].statuteDocumentationIdentifierType = $('#statuteDocumentationIdentifierType').val();
		selectedNode[0].statuteDocumentationIdentifierValue = $('#statuteDocumentationIdentifierValue').val();
		selectedNode[0].statuteDocumentationRole = $('#statuteDocumentationRole').val();

		if (selectedNode[0].text == null || selectedNode[0].text != $('#collectionID').val()) {
			// update Collection node text if Collection ID metadata field is changed
			//https://stackoverflow.com/questions/44659721/is-there-a-way-to-change-node-text-in-bootstrap-treeview
			selectedNode[0].text = $('#collectionID').val();
			$('#tree').treeview(true).removeNode([]);				// re-initialize it every time after the tree changed
		}

	}

	function fillAccessionMetadataFields(node){
		$('#accessionID').val(node.accessionID);
		$('#accessionTitle').val(node.accessionTitle);
		$('#accessionScope').val(node.accessionScope);
		$('#accessionRights').val(node.accessionRights);
		$('#accessionNotes').val(node.accessionNotes);
	}


	function updateAccessionNodeWithMetadata(){

		var selectedNode = $('#tree').treeview('getSelected');
		//selectedNode[0].accessionID = $('#accessionID').val();	// no need to update accessID as it is readonly
		selectedNode[0].accessionTitle = $('#accessionTitle').val();
		//selectedNode[0].accessionDate = $('#accessionDate').val();	// no need to update accessionDate as it is readonly
		selectedNode[0].accessionScope = $('#accessionScope').val();
		selectedNode[0].accessionRights = $('#accessionRights').val();
		selectedNode[0].accessionNotes = $('#accessionNotes').val();
	}

	function fillFileMetadataFields(node){
		$('#fileID').val(node.fileID);
		$('#filename').val(node.filename);
		$('#fileFormat').val(node.fileFormat);
		$('#fileNotes').val(node.notes);
		$('#file-preservationLevelRole').val(node.preservationLevelRole);
		$('#file-preservationLevelRationale').val(node.preservationLevelRationale);
		$('#preservationLevelDateAssigned').val(node.preservationLevelDateAssigned);
		$('#compositionLevel').val(node.compositionLevel);
		$('#messageDigestAlgorithm').val(node.messageDigestAlgorithm);
		$('#messageDigest').val(node.messageDigest);
		$('#messageDigestOrginator').val(node.messageDigestOrginator);
		$('#formatName').val(node.formatName);
		$('#formatVersion ').val(node.formatVersion);
		$('#creatingApplicationName').val(node.creatingApplicationName);
		$('#creatingApplicationVersion').val(node.creatingApplicationVersion);
		$('#dateCreatedByApplication').val(node.dateCreatedByApplication);
		$('#file-environmentCharacteristic').val(node.environmentCharacteristic);
		$('#file-relatedEnvironmentPurpose').val(node.relatedEnvironmentPurpose);
		$('#file-environmentNote').val(node.environmentNote);
		$('#file-softwareName').val(node.softwareName);
		$('#file-softwareVersion').val(node.softwareVersion);
	}

	function updateFileNodeWithMetadata(){

		var selectedNode = $('#tree').treeview('getSelected');

		//selectedNode[0].fileID = $('#fileID').val();			// no need to update accessID as it is readonly
		//selectedNode[0].filename = $('#filename').val();		// no need to update accessID as it is readonly
		//selectedNode[0].fileFormat = $('#fileFormat').val();	// no need to update accessID as it is readonly
		selectedNode[0].notes = $('#fileNotes').val();
		selectedNode[0].preservationLevelRole = $('#file-preservationLevelRole').val();
		selectedNode[0].preservationLevelRationale = $('#file-preservationLevelRationale').val();
		selectedNode[0].preservationLevelDateAssigned = $('#preservationLevelDateAssigned').val();
		selectedNode[0].compositionLevel = $('#compositionLevel').val();
		selectedNode[0].messageDigestAlgorithm = $('#messageDigestAlgorithm').val();
		selectedNode[0].messageDigest = $('#messageDigest').val();
		selectedNode[0].messageDigestOrginator = $('#messageDigestOrginator').val();
		selectedNode[0].formatName = $('#formatName').val();
		selectedNode[0].formatVersion  = $('#formatVersion ').val();
		selectedNode[0].creatingApplicationName = $('#creatingApplicationName').val();
		selectedNode[0].creatingApplicationVersion = $('#creatingApplicationVersion').val();
		selectedNode[0].dateCreatedByApplication = $('#dateCreatedByApplication').val();
		selectedNode[0].environmentCharacteristic = $('#file-environmentCharacteristic').val();
		selectedNode[0].relatedEnvironmentPurpose = $('#file-relatedEnvironmentPurpose').val();
		selectedNode[0].environmentNote = $('#file-environmentNote').val();
		selectedNode[0].softwareName = $('#file-softwareName').val();
		selectedNode[0].softwareVersion = $('#file-softwareVersion').val();
	}

	$(document).ready(function () {

		$('#tree').treeview({
			data: metadataTreeJSON,
			color: "#000000",
            <% if (accessionId == null) {%>
			    levels: 2,
            <% }else {%>
                levels: 3,
			<%}%>
			onhoverColor: "#c5c5c5",
            preventUnselect: true,
			selectedColor: "#ffffff",
			selectedBackColor: "#0175bc",
			showBorder: true,
			//expandIcon: 'glyphicon glyphicon-folder-close',
			//collapseIcon: 'glyphicon glyphicon-folder-open',
			onNodeSelected: function(event, node) {
				if (node.type=="Collection"){
					fillCollectionMetadataFields(node);
				} else if (node.type=="Accession"){
					fillAccessionMetadataFields(node);
// 2022-10-03					 
//				} else if (node.type=="MBOX"){
				} else if (node.type=="MBOX" || node.type=="NON_MBOX" ||  node.type=="IMAP"){
					fillFileMetadataFields(node);
				}
				showRightPanelView(node.type);
			},
			onNodeUnselected: function(event, node) {
				hideRightPanelView(node.type);
			},
		});

		$('#collection-tabs').tabs();
		$('#accession-tabs').tabs();
		$('#mbox-tabs').tabs();

		var selectedNodes = $('#tree').treeview('getSelected');

		<% if (accessionId == null) {%>
			showRightPanelView("Collection");
			hideRightPanelView("Accession");
			hideRightPanelView("MBOX");
			fillCollectionMetadataFields(selectedNodes[0]);		// initially load collection metadata to GUI. Do once as there is only 1 collection.
		<%} else {%>
			hideRightPanelView("Collection");
			showRightPanelView("Accession");
			hideRightPanelView("MBOX");
			fillAccessionMetadataFields(selectedNodes[0]);		// initially load collection metadata to GUI. We may call this when user click on other accession nodes on tree.
		<%}%>

		$('#embargoReviewDate').datepicker({
			minDate: new Date(1960, 1 - 1, 1),
			dateFormat: "yy-mm-dd",
			changeMonth: true,
			changeYear: true,
			yearRange: "2000:2100",
			beforeShow: function(i) { if ($(i).attr('readonly')) { return false; } }
		});

		$('#embargoStartDate').datepicker({
			minDate: new Date(1960, 1 - 1, 1),
			dateFormat: "yy-mm-dd",
			changeMonth: true,
			changeYear: true,
			yearRange: "2000:2100",
			beforeShow: function(i) { if ($(i).attr('readonly')) { return false; } }
		});
/*
 * 2022-09-08
		$('#embargoDuration').datepicker({
			minDate: new Date(1960, 1 - 1, 1),
			dateFormat: "yy-mm-dd",
			changeMonth: true,
			changeYear: true,
			yearRange: "2000:2100",
			beforeShow: function(i) { if ($(i).attr('readonly')) { return false; } }
		});

		$('#embargoEndDate').datepicker({
			minDate: new Date(1960, 1 - 1, 1),
			dateFormat: "yy-mm-dd",
			changeMonth: true,
			changeYear: true,
			yearRange: "2000:2100",
			beforeShow: function(i) { if ($(i).attr('readonly')) { return false; } }
		});
*/
		$('#dateCreatedByApplication').datepicker({
			minDate: new Date(1960, 1 - 1, 1),
			dateFormat: "yy-mm-dd",
			changeMonth: true,
			changeYear: true,
			yearRange: "2000:2100",
			beforeShow: function(i) { if ($(i).attr('readonly')) { return false; } }
		});

		$('#preservationLevelDateAssigned').datepicker({
			minDate: new Date(1960, 1 - 1, 1),
			dateFormat: "yy-mm-dd",
			changeMonth: true,
			changeYear: true,
			yearRange: "2000:2100",
			beforeShow: function(i) { if ($(i).attr('readonly')) { return false; } }
		});
	});

</script>

<jsp:include page="footer.jsp"/>

</body>
</html>
