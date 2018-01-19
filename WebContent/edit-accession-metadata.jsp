<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@include file="getArchive.jspf" %>

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
//	writeProfileBlock(out, archive, "", "Accession metadata");
	assert request.getParameter("archiveID")!=null;

	String id = "", title = "", date = "", scope = "", rights = "", notes = "";
	%>


	<br/>
<form id="metadata-form">
<section>
	<div style="margin-left: 170px;max-width:850px;">
	<div class="panel">
		<input type="hidden" value="<%=request.getParameter("archiveID")%>" class="form-control" type="text" name="archiveID"/>

		<div class="div-input-field">
			<div class="input-field-label">Accession ID</div>
			<br/>
			<div class="input-field">
				<input title="Accession ID" value="<%=id%>" class="form-control" type="text" name="accessionID"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Accession title</div>
			<br/>
			<div class="input-field">
				<input title="Accession title" value="<%=Util.escapeHTML(title)%>" class="form-control" type="text" name="accessionTitle"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Accession Date</div>
			<br/>
			<div class="input-field">
				<input title="Accession Date" value="<%=date%>" class="form-control" type="text" name="accessionDate"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Scope and Content</div>
			<br/>
			<div class="input-field">
				<input title="Scope and Content" value="<%=Util.escapeHTML(scope)%>" class="form-control" type="text" name="accessionScope"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Rights and Conditions</div>
			<br/>
			<div class="input-field">
				<input title="Rights and Conditions" value="<%=Util.escapeHTML(scope)%>" class="form-control" type="text" name="accessionScope"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Notes</div>
			<br/>
			<div class="input-field">
				<input title="Notes" value="<%=Util.escapeHTML(notes)%>" class="form-control" type="text" name="accessionNotes"/>
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
	$('#gobutton').click (function() { fetch_page_with_progress ('ajax/updateCollectionMetadata.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), $('#metadata-form').serialize(), null, 'collection-detail?collection=<%=id%>'); return false; });
</script>

 <jsp:include page="footer.jsp"/>
 
</body>
</html>
