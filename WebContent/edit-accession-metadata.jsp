<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="java.io.File" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>

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
<%@include file="header.jspf"%>

<!-- this is going to be really quick... -->
<script type="text/javascript" src="js/statusUpdate.js"></script>
<%@include file="div_status.jspf"%>
<%! private static String formatMetadataField(String s) { return (s == null) ? "" : Util.escapeHTML(s); } %>

<p>

		<%
	if (!ModeConfig.isProcessingMode()) {
		out.println ("Updating collection metadata is allowed only in ePADD's Processing mode.");
		return;
	}

	String collectionid = request.getParameter("collection");
    String accessionid = request.getParameter("accessionID");
	String modeBaseDir = Config.REPO_DIR_PROCESSING + File.separator + collectionid;

	// read cm from archive file
	Archive.CollectionMetadata cm = ArchiveReaderWriter.readCollectionMetadata(modeBaseDir);
	if (cm == null) {
	    out.println ("Unable to read processing metadata file for collection: " + collectionid);
	    return;
	}
	//get accession corresponding to accessionID from cm
	Archive.AccessionMetadata ametadata=null;
	for(Archive.AccessionMetadata am: cm.accessionMetadatas){
	    if(am.id.equals(accessionid.trim()))
	        ametadata = am;
	};
	if(ametadata==null)
	    {
	        out.println("Unable to find accession metadata for accession id: " +accessionid);
	        return;
	    }
	%>



	<br/>
<form id="metadata-form">
<section>
	<div style="margin-left: 170px;max-width:850px;">
	<div class="panel">
		<%--<input type="hidden" value="<%=request.getParameter("archiveID")%>" class="form-control" type="text" name="archiveID"/>--%>

			<input type="hidden" name="collection" value="<%=Util.escapeHTML(collectionid)%>"/>

		<div class="div-input-field">
			<div class="input-field-label">Accession ID</div>
			<br/>
			<div class="input-field">
				<input title="Accession ID" value="<%=formatMetadataField(ametadata.id)%>" class="form-control" type="text" readonly name="accessionID"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Accession title</div>
			<br/>
			<div class="input-field">
				<input title="Accession title" value="<%=formatMetadataField(ametadata.title)%>" class="form-control" type="text" name="accessionTitle"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Accession Date</div>
			<br/>
			<div class="input-field">
				<input title="Accession Date" value="<%=formatMetadataField(ametadata.date)%>" class="form-control" type="text" name="accessionDate"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Scope and Content</div>
			<br/>
			<div class="input-field">
				<input title="Scope and Content" value="<%=formatMetadataField(ametadata.scope)%>" class="form-control" type="text" name="accessionScope"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Rights and Conditions</div>
			<br/>
			<div class="input-field">
				<input title="Rights and Conditions" value="<%=formatMetadataField(ametadata.rights)%>" class="form-control" type="text" name="accessionRights"/>
			</div>
		</div>

		<div class="div-input-field">
			<div class="input-field-label">Notes</div>
			<br/>
			<div class="input-field">
				<input title="Notes" value="<%=formatMetadataField(ametadata.notes)%>" class="form-control" type="text" name="accessionNotes"/>
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
            url: 'ajax/updateAccessionMetadata.jsp',
            data: $('#metadata-form').serialize(),
            success: function (response) {
                if (response && response.status === 0)
                    window.location = 'collection-detail?collection=<%=collectionid%>';
                else
                    epadd.error ("Sorry, something went wrong while updating accession metadata: " + (response && response.errorMessage ? response.errorMessage : ""));
            },
            error: function (jqxhr, status, ex) {
                epadd.error("Sorry, there was an error while updating collection metadata: " + status + ". Exception: " + ex);
            }
        });
        return false;

	    <%--fetch_page_with_progress ('ajax/updateAccessionMetadata.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), $('#metadata-form').serialize(), null, 'collection-detail?collection=<%=id%>'); return false;--%>

	});
</script>

 <jsp:include page="footer.jsp"/>
 
</body>
</html>
