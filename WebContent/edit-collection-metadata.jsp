<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="java.io.File" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%! private static String formatMetadataField(String s) { return (s == null) ? "" : Util.escapeHTML(s); } %>

<html>
<head>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js"></script>
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/muse.js" type="text/javascript"></script>
	<script src="js/epadd.js" type="text/javascript"></script>
	<title>Collection Metadata</title>

	<style>
		.div-input-field { display: inline-block; width: 400px; margin-left: 20px; line-height:10px; padding:20px;}
		.input-field {width:350px;}
		.input-field-label {font-size: 12px;}
	</style>
</head>
<body style="background-color: white">
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Collections');</script>

<p>
	<%
	if (!ModeConfig.isProcessingMode()) {
		out.println ("Updating collection metadata is allowed only in ePADD's Processing mode.");
		return;
	}

	String id = request.getParameter("collection");
    String modeBaseDir = Config.REPO_DIR_PROCESSING + File.separator + id;

	// read cm from archive file
	Archive.CollectionMetadata cm = ArchiveReaderWriter.readCollectionMetadata(modeBaseDir);
	if (cm == null) {
	    out.println ("Unable to read processing metadata file for collection: " + id);
	    return;
	}
	%>

<form id="metadata-form">
<section>
	<div style="margin-left: 170px;max-width:850px;">
	<div class="panel">
        <div class="panel-heading">
            Collection: <%=Util.escapeHTML(id)%>
        </div>

        <input type="hidden" name="collection" value="<%=Util.escapeHTML(id)%>"/>

        <div class="div-input-field">
			<div class="input-field-label">Institution</div>
			<br/>
			<div class="input-field">
				<input title="Institution" value="<%=formatMetadataField( cm.institution)%>" class="form-control" type="text" name="institution"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Repository</div>
			<br/>
			<div class="input-field">
				<input title="Repository" value="<%=formatMetadataField( cm.repository)%>" class="form-control" type="text" name="repository"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Collection Title</div>
			<br/>
			<div class="input-field">
				<input title="Collection title" value="<%=formatMetadataField( cm.collectionTitle)%>" class="form-control" type="text" name="collectionTitle"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Collection ID</div>
			<br/>
			<div class="input-field">
				<input title="Collection ID" value="<%=formatMetadataField( cm.collectionID)%>" class="form-control" type="text" name="collectionID"/> <!-- this is the institutional collection ID, not ePADD's -->
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Finding Aid Link</div>
			<br/>
			<div class="input-field">
				<input title="Finding aid link" value="<%=formatMetadataField( cm.findingAidLink)%>" class="form-control" type="text" name="findingAidLink"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Catalog Record Link</div>
			<br/>
			<div class="input-field">
				<input title="Catalog record link" value="<%=formatMetadataField( cm.catalogRecordLink)%>" class="form-control" type="text" name="catalogRecordLink"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Contact Email Address</div>
			<br/>
			<div class="input-field">
				<input title="Contact email" value="<%=formatMetadataField( cm.contactEmail)%>" class="form-control" type="text" name="contactEmail"/>
			</div>
		</div>

		<br/>

		<div class="div-input-field">
			<div class="input-field-label">About</div>
			<br/>
			<div class="input-field">
				<textarea title="About" style="resize:vertical;height:200px;" class="form-control" name="about"><%=cm == null ? "" : formatMetadataField( cm.about)%>
				</textarea>
			</div>
		</div>
		<div class="div-input-field">
			<div class="input-field-label">Rights and Conditions</div>
			<br/>
			<div class="input-field">
				<textarea title="Rights and conditions" style="resize:vertical;height:200px;" class="form-control" name="rights"><%=cm == null ? "" : formatMetadataField( cm.rights)%>
				</textarea>
			</div>
		</div>
		<div class="div-input-field">
			<div class="input-field-label">Notes</div>
			<br/>
			<div class="input-field">
				<textarea title="Notes" style="resize:vertical;height:200px;" class="form-control" name="notes"><%=cm == null ? "" : formatMetadataField( cm.notes)%>
				</textarea>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Scope and Content</div>
			<br/>
			<div class="input-field">
				<textarea title="Scope and Content" style="resize:vertical;height:200px;" class="form-control" name="scopeAndContent"><%=cm == null ? "" : formatMetadataField( cm.scopeAndContent)%>
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
	$('#gobutton').click (function() {
        $.ajax({
            type: 'POST',
            dataType: 'json',
            url: 'ajax/updateCollectionMetadata.jsp',
            data: $('#metadata-form').serialize(),
            success: function (response) {
                if (response && response.status === 0)
                    window.location = 'collection-detail?collection=<%=id%>';
                else
                    epadd.alert ("Sorry, something went wrong while updating collection metadata: " + (response && response.errorMessage ? response.errorMessage : ""));
            },
            error: function (jqxhr, status, ex) {
                epadd.alert("Sorry, there was an error while updating collection metadata: " + status + ". Exception: " + ex);
            }
        });
        return false;
    });
</script>

 <jsp:include page="footer.jsp"/>
 
</body>
</html>
