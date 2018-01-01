<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@include file="getArchive.jspf" %>
<%! private static String formatMetadataField(String s) { return (s == null) ? "" : Util.escapeHTML(s); } %>

<html>
<head>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	
	<script src="js/jquery.js"></script>
	<link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
	<script src="jqueryFileTree/jqueryFileTree.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<jsp:include page="css/css.jsp"/>
	<title>Accession Metadata</title>
	<script src="js/muse.js" type="text/javascript"></script>
	<script src="js/epadd.js" type="text/javascript"></script>

	<style>
		.div-input-field { display: inline-block; width: 400px; margin-left: 20px; line-height:10px; padding:20px;}
		.input-field {width:350px;}
		.input-field-label {font-size: 12px;}
	</style>
</head>
<body style="background-color: white">
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Collections');</script>

<!-- this is going to be really quick... -->
<script type="text/javascript" src="js/statusUpdate.js"></script>
<%@include file="div_status.jspf"%>
<p>
	<%
	String id = request.getParameter("id");
	writeProfileBlock(out, archive, "", "Accession metadata");
	assert request.getParameter("archiveID")!=null;
	%>


	<br/>
<form id="metadata-form">
<section>
	<div style="margin-left: 170px;max-width:850px;">
	<div class="panel">


		//adding a hidden input field to pass archiveID to the server. This is a common pattern used to pass
		//archiveID in all those forms where POST was used to invoke the server page.
		<div class="div-input-field">
			<div class="input-field">
				<input type="hidden" value="<%=request.getParameter("archiveID")%>" class="form-control" type="text" name="archiveID"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Institution</div>
			<br/>
			<div class="input-field">
				<input title="Institution" value="<%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.institution)%>" class="form-control" type="text" name="institution"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Repository</div>
			<br/>
			<div class="input-field">
				<input title="Repository" value="<%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.repository)%>" class="form-control" type="text" name="repository"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Collection Title</div>
			<br/>
			<div class="input-field">
				<input title="Collection title" value="<%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.collectionTitle)%>" class="form-control" type="text" name="collectionTitle"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Collection ID</div>
			<br/>
			<div class="input-field">
				<input title="Collection ID" value="<%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.collectionID)%>" class="form-control" type="text" name="collectionID"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Accession ID</div>
			<br/>
			<div class="input-field">
				<input title="Accession ID" value="<%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.accessionID)%>" class="form-control" type="text" name="accessionID"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Finding Aid Link</div>
			<br/>
			<div class="input-field">
				<input title="Finding aid link" value="<%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.findingAidLink)%>" class="form-control" type="text" name="findingAidLink"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Catalog Record Link</div>
			<br/>
			<div class="input-field">
				<input title="Catalog record link" value="<%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.catalogRecordLink)%>" class="form-control" type="text" name="catalogRecordLink"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Contact Email Address</div>
			<br/>
			<div class="input-field">
				<input title="Contact email" value="<%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.contactEmail)%>" class="form-control" type="text" name="contactEmail"/>
			</div>
		</div>

		<br/>

		<div class="div-input-field">
			<div class="input-field-label">About</div>
			<br/>
			<div class="input-field">
				<textarea title="About" style="resize:vertical;height:200px;" class="form-control" name="about"><%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.about)%>
				</textarea>
			</div>
		</div>
		<div class="div-input-field">
			<div class="input-field-label">Rights and Conditions</div>
			<br/>
			<div class="input-field">
				<textarea title="Rights and conditions" style="resize:vertical;height:200px;" class="form-control" name="rights"><%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.rights)%>
				</textarea>
			</div>
		</div>
		<div class="div-input-field">
			<div class="input-field-label">Notes</div>
			<br/>
			<div class="input-field">
				<textarea title="Notes" style="resize:vertical;height:200px;" class="form-control" name="notes"><%=archive.processingMetadata == null ? "" : formatMetadataField( archive.processingMetadata.notes)%>
				</textarea>
			</div>
		</div>
		<br/>
		<br/>
		<div style="margin-left:40px">
			<button class="btn btn-cta" id="gobutton">Save <i class="icon-arrowbutton"></i></button><br/><br/>
		</div>
	</div>
	</div>

	<!--
	<label class="col-sm-2 control-label">Collection Image</label>
	<div class="col-sm-4"><input type="file" name="collectionImage" class="form-control" /></div>
	<br/>
	<br/>

	<label class="col-sm-2 control-label">Landing Image</label>
	<div class="col-sm-4"><input type="file" name="bannerImage" class="form-control" /></div>
	<br/>
	<br/>
	-->
</section>
</form>

<script type="text/javascript">
	$('#gobutton').click (function() { fetch_page_with_progress ('ajax/updateAccessionMetadata.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), $('#metadata-form').serialize(), null, 'collection-detail?id=<%=id%>'); return false; });
</script>

 <jsp:include page="footer.jsp"/>
 
</body>
</html>
