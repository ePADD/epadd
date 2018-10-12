<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@page language="java" import="edu.stanford.muse.index.IndexUtils"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="java.util.Collection" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="edu.stanford.muse.ie.variants.EntityBook" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
<link rel="icon" type="image/png" href="images/epadd-favicon.png">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Archive Information</title>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css">
	<link rel="stylesheet" href="css/main.css">

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>

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
<%
	//This is to handle the scenario when appraisal mode browse-top is being invoked without archiveID. As
	//a default behaviour archive is loaded by reading from epadd-appraisal directory. However as no archiveID
	//is present in the request the headers are not rendered properly. Now we set archiveID from the archive object
	//that was loaded by default from the appraisal directory. This property is then read by header.jspf (if archiveID
	//not passed from the request) and used for rendering the header properly.
	if(archive!=null && request.getParameter("archiveID")==null)
		request.setAttribute("archiveID", ArchiveReaderWriter.getArchiveIDForArchive(archive));
%>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<!--sidebar content-->

<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>
<nav class="menu1" role="navigation">
	<h2><b>Using the Dashboard</b></h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<div class="search-tips" style="display:block">

		<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
			Use the dashboard to browse and interact with an email archive.
			Browse correspondents and extracted entities (including persons, organizations, events, and more),
			create labels that can be assigned to messages, review attachments, create and view results of thematic lexicon searches, and learn more about both the archive and the import into ePADD through reports.
			<br/>
			<br/>

			[Icon 1] Browse, graph, and edit correspondents and associated messages.
			<br/>
			<br/>

			[Icon 2] Browse and edit recognized entities and associated messages.
			<br/>
			<br/>

			[Icon 3] View, add, and edit general and restriction (machine-actionable) labels, and view associated messages.
			<br/>
			<br/>

			[Icon 4] Browse image attachments and associated messages.
			<br/>
			<br/>


			[Icon 5] View other attachments and associated messages.
			<br/>
			<br/>

			[Icon 6] View original message folders or sources and associated messages.
			<br/>
			<br/>

			[Icon 7] Browse, create, and modify lexicon searches.
			<br/>
			<br/>

			[Icon 8] View any import and merge reports.
			<br/>
			<br/>

			[Icon 9] Access additional options, including verifying the bag checksum, adding images associated with the creator, and setting default labels.
			<br/>
			<br/>

			Select Search from the menu bar to view additional options for searching across the archive.
			<br/>
			<br/>
			<b>Screening Email for Sensitive Information</b>
			Regular expression lexicon searches, sensitive lexicon searches, and the disease entity category are particularly helpful to locate potentially sensitive information, including personally identifiable information (PII).
			<br/>
			<br/>
			Use the Advanced Search option, under Search, to further refine criteria to identify potentially sensitive information.
			<br/>
			<br/>
			To restrict a message or set of messages, create a restriction label via the Labels menu option, then apply that label from the message browse screen.
			<br/>
			<br/>

		<% } else if (ModeConfig.isDiscoveryMode()) { %>
			Use the dashboard to browse and interact with an email archive.
			<br/>
			<br/>

			[Icon 1] Browse and graph correspondents and associated messages.
			<br/>
			<br/>
			[Icon 2] Browse recognized entities and associated messages.
			<br/>
			<br/>

			Select Search from the menu bar to view additional options for searching across the archive.
		<% } else if (ModeConfig.isDeliveryMode()) { %>
			Use the dashboard to browse and interact with an email archive.
			Browse correspondents and extracted entities (including persons, organizations, events, and more), create labels that can be assigned to messages, review attachments, create and view results of thematic lexicon searches.
			<br/>
			<br/>

			Select Search from the menu bar to view additional options for searching across the archive.

		<% } %>
	</div>
</nav>

<%
    //The request params that post to this page can be huge, we may want JSPHelper to ignore the request rather than printing all the post params
    AddressBook ab = archive.addressBook;
    String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
	String addressBookUpdate = request.getParameter("addressBookUpdate");
	if (!Util.nullOrEmpty(addressBookUpdate)) {
        archive.addressBook.initialize(addressBookUpdate);
		//archive.recreateCorrespondentAuthorityMapper(); // we have to recreate auth mappings since they may have changed
        //SimpleSessions.saveArchive(archive);//instead of saving whole archive object now we save only those parts which changed.
		ArchiveReaderWriter.saveAddressBook(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
        //ArchiveReaderWriter.saveCorrespondentAuthorityMapper(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);

        //after this invalidate the cache for removing the result of AddressBook.getCountsAsJSON result.. Or should we recalculate the result here only.
    }

	String entityMerges = request.getParameter("entityMerges");
	if (!Util.nullOrEmpty(entityMerges)) {
		EntityBook entityBook = archive.getEntityBook();
		Short type = -1;
		try {
			type = Short.parseShort (request.getParameter ("entityType"));
			entityBook.initialize (entityMerges,type);
			//SimpleSessions.saveArchive(archive);//instead of saving whole archive object now we save only those parts which changed.
			ArchiveReaderWriter.saveEntityBook(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
		} catch (Exception e) {
			Util.print_exception("Error in merging entities", e, JSPHelper.log);
		}
	}

	Collection<EmailDocument> allDocs = (Collection) archive.getAllDocs();

	//int nDocAttachments = EmailUtils.countDocumentAttachmentsInDocs(allDocs);
	int nFolders = archive.getAllFolders().size();
	boolean statsAvailable = false;
	if(archive.collectionMetadata.entityCounts != null)
		statsAvailable = true;
	String nPersonEntities ="", nSensitiveMessages ="";
	int nNonPersonEntities = 0,outCount=0,inCount=0,nAttachments=0,nImageAttachments=0;
	outCount = archive.collectionMetadata.nOutgoingMessages;
	inCount = archive.collectionMetadata.nIncomingMessages;
	//if normalization map is available in blobstore recalculate the attachments count.
	if(archive.getBlobStore().isNormalized()){
	    ArchiveReaderWriter.recalculateCollectionMetadata(archive);

	}
	nAttachments = archive.collectionMetadata.nBlobs;//EmailUtils.countAttachmentsInDocs(allDocs);
	nImageAttachments = archive.collectionMetadata.nImageBlobs;//EmailUtils.countImageAttachmentsInDocs(allDocs);

	if(statsAvailable){
		nPersonEntities = Integer.toString(archive.collectionMetadata.entityCounts.getOrDefault(NEType.Type.PERSON.getCode(),0));
		for (short fineType: archive.collectionMetadata.entityCounts.keySet()) {
			if (NEType.getTypeForCode (fineType) != NEType.Type.PERSON)
				nNonPersonEntities += archive.collectionMetadata.entityCounts.getOrDefault(fineType,0);
		}
	}
	if(archive.collectionMetadata.numPotentiallySensitiveMessages>=0)
		nSensitiveMessages = " ("+archive.collectionMetadata.numPotentiallySensitiveMessages+")";

	int nContacts = ab.allContacts().size();
%>

<% writeProfileBlock(out, archive, "Date Range: ", IndexUtils.getDateRangeAsString(allDocs), "Messages: ", inCount + " incoming, " + outCount + " outgoing.");%>

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
                <p class="cta-text-1">Folders (<%=nFolders%>)</p>
                <p class="cta-text-2">Folders (<%=nFolders%>)</p>
            </a>
        </div>
    <% } %>

	<% if (!ModeConfig.isDiscoveryMode()) { %>
		<div class="cta-box text-center margin30">
				<a href="image-attachments?archiveID=<%=archiveID%>&attachmentExtension=jpg&attachmentExtension=png&attachmentExtension=gif&attachmentExtension=bmp&attachmentExtension=jpeg&attachmentExtension=svg&attachmentExtension=tif&startDate=&endDate=">
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
