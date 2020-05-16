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
	<%@include file="header.jspf"%>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js"></script>
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/muse.js" type="text/javascript"></script>
	<script src="js/epadd.js" type="text/javascript"></script>
	<title><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.head-collection-metadata")%></title>

	<style>
		.div-input-field { display: inline-block; width: 400px; margin-left: 20px; line-height:10px; padding:20px;}
		.input-field {width:350px;}
		.input-field-label {font-size: 12px;}
	</style>
</head>
<body style="background-color: white">

<%-- The header.jspf file was included here --%>

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
			<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.collection") + Util.escapeHTML(id)%>
        </div>

        <input type="hidden" name="collection" value="<%=Util.escapeHTML(id)%>"/>

        <div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.institution")%></div>
			<br/>
			<div class="input-field">
				<input title="Institution" value="<%=formatMetadataField( cm.institution)%>" class="form-control" type="text" name="institution"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.repository")%></div>
			<br/>
			<div class="input-field">
				<input title="Repository" value="<%=formatMetadataField( cm.repository)%>" class="form-control" type="text" name="repository"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.collection-title")%></div>
			<br/>
			<div class="input-field">
				<input title="Collection title" value="<%=formatMetadataField( cm.collectionTitle)%>" class="form-control" type="text" name="collectionTitle"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.short-title")%></div>
			<br/>
			<div class="input-field">
				<input title="Short title" value="<%=formatMetadataField( cm.shortTitle)%>" class="form-control" type="text" name="shortTitle"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.collection-id")%></div>
			<br/>
			<div class="input-field">
				<input title="Collection ID" value="<%=formatMetadataField( cm.collectionID)%>" class="form-control" type="text" name="collectionID"/> <!-- this is the institutional collection ID, not ePADD's -->
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.aid-link")%>k</div>
			<br/>
			<div class="input-field">
				<input title="Finding aid link" value="<%=formatMetadataField( cm.findingAidLink)%>" class="form-control" type="text" name="findingAidLink"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.record-link")%></div>
			<br/>
			<div class="input-field">
				<input title="Catalog record link" value="<%=formatMetadataField( cm.catalogRecordLink)%>" class="form-control" type="text" name="catalogRecordLink"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.contact-email")%></div>
			<br/>
			<div class="input-field">
				<input title="Contact email" value="<%=formatMetadataField( cm.contactEmail)%>" class="form-control" type="text" name="contactEmail"/>
			</div>
		</div>

		<br/>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.short-desc")%></div>
			<br/>
			<div class="input-field">
				<input title="Short description" value="<%=formatMetadataField( cm.shortDescription)%>" class="form-control" type="text" name="shortDescription"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.about")%></div>
			<br/>
			<div class="input-field">
				<textarea title="About" style="resize:vertical;height:200px;" class="form-control" name="about"><%=formatMetadataField(cm.about)%>
				</textarea>
			</div>
		</div>
		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.rights-cond")%></div>
			<br/>
			<div class="input-field">
				<textarea title="Rights and conditions" style="resize:vertical;height:200px;" class="form-control" name="rights"><%=formatMetadataField( cm.rights)%>
				</textarea>
			</div>
		</div>
		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.notes")%></div>
			<br/>
			<div class="input-field">
				<textarea title="Notes" style="resize:vertical;height:200px;" class="form-control" name="notes"><%=formatMetadataField( cm.notes)%>
				</textarea>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.scope-content")%></div>
			<br/>
			<div class="input-field">
				<textarea title="Scope and Content" style="resize:vertical;height:200px;" class="form-control" name="scopeAndContent"><%=formatMetadataField( cm.scopeAndContent)%>
				</textarea>
			</div>
		</div>

		<br/>
		<br/>
		<div style="margin-left:40px">
			<button class="btn btn-cta" id="gobutton"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "edit-collection-metadata.save-button")%> <i class="icon-arrowbutton"></i></button><br/><br/>
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
                    epadd.error ("There was an error updating collection metadata. Please try again, and if the error persists, report it to epadd_project@stanford.edu. Details: " + (response && response.errorMessage ? response.errorMessage : ""));
            },
            error: function (jqxhr, status, ex) {
                epadd.error("There was an error updating collection metadata. Please try again, and if the error persists, report it to epadd_project@stanford.edu. Details: status=" + status + ". Exception: " + ex);
            }
        });
        return false;
    });
</script>

 <jsp:include page="footer.jsp"/>
 
</body>
</html>
