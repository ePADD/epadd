<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@page language="java" import="edu.stanford.muse.index.IndexUtils"%>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.webapp.SimpleSessions" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.List" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="edu.stanford.muse.ie.variants.EntityBook" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
<link rel="icon" type="image/png" href="images/epadd-favicon.png">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Archive Information</title>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/epadd.js"></script>

	<style>
		div.cta-box { width:270px; height: 200px; display: inline-block; border-color:black; margin:10px; cursor: pointer; }
		div.cta-box .fa, div.cta-box img { margin-bottom: 10px; height: 30px;}

		div.cta-box .fa { padding-top: 5px; font-size: 28px; }
		div.cta-box:hover .fa, div.cta-box:hover img  { display: none; }

		div.cta-box:hover { color: white; }
		div.cta-box:hover a { color: white; }
	</style>

</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
    //The request params that post to this page can be huge, we may want JSPHelper to ignore the request rather than printing all the post params
    AddressBook ab = archive.addressBook;
    String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	String addressBookUpdate = request.getParameter("addressBookUpdate");
	if (!Util.nullOrEmpty(addressBookUpdate)) {
        archive.addressBook.initialize(addressBookUpdate);
		archive.recreateCorrespondentAuthorityMapper(); // we have to recreate auth mappings since they may have changed
        //SimpleSessions.saveArchive(archive);//instead of saving whole archive object now we save only those parts which changed.
		SimpleSessions.saveAddressBook(archive);
        SimpleSessions.saveCorrespondentAuthorityMapper(archive);
    }

	String entityMerges = request.getParameter("entityMerges");
	if (!Util.nullOrEmpty(entityMerges)) {
		EntityBook entityBook = archive.getEntityBook();
		Short type = -1;
		try {
			type = Short.parseShort (request.getParameter ("entityType"));
			entityBook.initialize (entityMerges,type);
			//SimpleSessions.saveArchive(archive);//instead of saving whole archive object now we save only those parts which changed.
			SimpleSessions.saveEntityBook(archive);
		} catch (Exception e) {
			Util.print_exception("Error in merging entities", e, JSPHelper.log);
		}
	}

	Collection<EmailDocument> allDocs = (Collection) archive.getAllDocs();

	int outCount = ab.getOutMessageCount(allDocs);
	int inCount = allDocs.size()-outCount;
	int nAttachments = EmailUtils.countAttachmentsInDocs(allDocs);
	int nImageAttachments = EmailUtils.countImageAttachmentsInDocs(allDocs);
	//int nDocAttachments = EmailUtils.countDocumentAttachmentsInDocs(allDocs);
	int nFolders = archive.getAllFolders().size();
	boolean statsAvailable = false;
	if(archive.processingMetadata.entityCounts != null)
		statsAvailable = true;
	String nPersonEntities ="", nSensitiveMessages ="";
	int nNonPersonEntities = 0;
	if(statsAvailable){
		nPersonEntities = Integer.toString(archive.processingMetadata.entityCounts.get(NEType.Type.PERSON.getCode()));
		for (short fineType: archive.processingMetadata.entityCounts.keySet()) {
			if (NEType.getTypeForCode (fineType) != NEType.Type.PERSON)
				nNonPersonEntities += archive.processingMetadata.entityCounts.get(fineType);
		}
	}
	if(archive.processingMetadata.numPotentiallySensitiveMessages>=0)
		nSensitiveMessages = " ("+archive.processingMetadata.numPotentiallySensitiveMessages+")";

	int nContacts = ab.allContacts().size();
%>

<% writeProfileBlock(out, archive, "Date Range: ", IndexUtils.getDateRangeAsString(allDocs), "Messages: ", inCount + " incoming, " + outCount + " outgoing.");%>
<br/>

<div id="all-cards" style="text-align: center; margin:auto">
	<div class="cta-box text-center margin30">
		<a href="correspondents?archiveID=<%=archiveID%>">
			<i class="icon-browsetoparrow"></i>
			<i class="fa fa-address-card-o" style="color:#3182bd" aria-hidden="true"></i>
			<p class="cta-text-1">Correspondents (<%=nContacts %>)</p>
			<p class="cta-text-2">Correspondents (<%=nContacts %>)</p>
		</a>
	</div>
	
	<div class="cta-box text-center margin30">
			<a href="list-entities.jsp?type=0&archiveID=<%=archiveID%>">
			<i class="icon-browsetoparrow"></i>
			<i class="fa fa-user-o" style="color:#e60707" aria-hidden="true"></i>
			<p class="cta-text-1">Person entities (<%=nPersonEntities%>)</p>
			<p class="cta-text-2">Person entities (<%=nPersonEntities%>)</p>
			</a>
	</div>

	<% if (ModeConfig.isDiscoveryMode()) { %>
		<br/>
	<% } %>

	<div class="cta-box text-center margin30">
		<a href="entity-types?archiveID=<%=archiveID%>">
			<i class="icon-browsetoparrow"></i>
			<i class="fa fa-thumb-tack" style="color:#31a354" aria-hidden="true"></i>
			<p class="cta-text-1">Other entities (<%=nNonPersonEntities%>)</p>
			<p class="cta-text-2">Other entities (<%=nNonPersonEntities%>)</p>
		</a>
	</div>

	<% if (!ModeConfig.isDiscoveryMode()) { %>
		<br/>
        <div class="cta-box text-center margin30">
            <a href="by-folder?archiveID=<%=archiveID%>">
                <i class="icon-browsetoparrow"></i>
				<i class="fa fa-folder-o" style="color:#636363" aria-hidden="true"></i>
                <p class="cta-text-1">Folder view (<%=nFolders%>)</p>
                <p class="cta-text-2">Folder view (<%=nFolders%>)</p>
            </a>
        </div>
    <% } %>

	<% if (!ModeConfig.isDiscoveryMode()) { %>
		<div class="cta-box text-center margin30">
				<a href="image-attachments?archiveID=<%=archiveID%>&attachmentExtension=jpg&attachmentExtension=png&attachmentExtension=gif&attachmentExtension=bmp&startDate=&endDate=">
					<i class="icon-browsetoparrow"></i>
					<i class="fa fa-picture-o" style="color:#756bb1" aria-hidden="true"></i>
					<p class="cta-text-1">Image attachments (<span id="nImageAttachments"><%=nImageAttachments%></span>)</p>
					<p class="cta-text-2">Image attachments (<%=nImageAttachments%>)</p>
				</a>
		</div>


	<div class="cta-box text-center margin30">
		<a href="attachments?archiveID=<%=archiveID%>&attachmentType=doc%3Bdocx%3Bpages&attachmentType=ppt%3Bpptx%3Bkey&attachmentType=xls%3Bxlsx%3Bnumbers&attachmentType=htm%3Bhtml%3Bcss%3Bjs&attachmentType=zip%3B7z%3Btar%3Btgz&attachmentType=mp3%3Bogg&attachmentType=avi%3Bmp4&attachmentType=fmp%3Bdb%3Bmdb%3Baccdb&attachmentType=others&startDate=&endDate=">
			<i class="icon-browsetoparrow"></i>
			<i class="fa fa-files-o" style="color:brown" aria-hidden="true"></i>
			<p class="cta-text-1">Other attachments (<span id="nOtherAttachments"><%=(nAttachments - nImageAttachments)%></span>)</p>
			<p class="cta-text-2">Other attachments (<%=(nAttachments - nImageAttachments)%>)</p>
		</a>
	</div>
	<br/>

		<div class="cta-box text-center margin30">
			<a href="lexicon?archiveID=<%=archiveID%>">
				<i class="icon-browsetoparrow"></i>
				<img src="images/lexicon.png"/>
				<p class="cta-text-1">Lexicon search</p>
				<p class="cta-text-2">Lexicon search</p>
			</a>
		</div>
	<% } %>

	<div class="cta-box text-center margin30">
		<a href="labels?archiveID=<%=archiveID%>">
			<i class="icon-browsetoparrow"></i>
			<i class="fa fa-tags" style="color:#0b967f" aria-hidden="true"></i>
			<p class="cta-text-1">Labels</p>
			<p class="cta-text-2">Labels</p>
		</a>
	</div>

	<% if (ModeConfig.isAppraisalMode()|| ModeConfig.isProcessingMode()) { %>
			<div class="cta-box text-center margin30">
				<a href="report?archiveID=<%=archiveID%>">
					<i class="icon-browsetoparrow"></i>
					<i class="fa fa-flag-o" style="color:#d41298" aria-hidden="true"></i>
					<p class="cta-text-1">Data report</p>
					<p class="cta-text-2">Data report</p>
				</a>
			</div>
		<% } %>

</div> <!--  allCards -->
<jsp:include page="footer.jsp"/>
<script>
	$('.cta-box').click(function(e) { var href = $('a', $(e.target)).attr('href'); if (href) { window.location = href;}}); // clicking anywhere in the cta-box should dispatch to the href of the only link inside it
</script>
</body>
</html>
