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
<%@ page import="edu.stanford.muse.ie.variants.EntityBookManager" %>
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
		.icon-images { margin-bottom: 10px; height: 30px; width:180px; padding-left:150px; margin-top: -30px; vertical-align: top;}

		div.cta-box .fa { padding-top: 5px; font-size: 28px; }
		/*div.cta-box:hover .fa, div.cta-box:hover img  { display: none; }*/
		div.cta-box h1{text-align: left;
			margin-left: 30px;
			padding-top: 20px;
		font-weight: 700;
		color:#0175BC;
		font-size:36px;
			font-family: 'Roboto', sans-serif;}
		div.cta-box h2{text-align: left;
			margin-left: 30px;font-size:24px;
			font-family: 'Open Sans', sans-serif;
			color:#666666; }
		div.cta-box:hover a h2{ color: white; }
		div.cta-box:hover a h1{ color: white; }
		img.attachment-icon path{
			background-color: white;
		}
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
<%@include file="header.jspf"%>

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

		<img  class="helpdrawer-images" src="images/correspondents.svg">
		Browse, graph, and edit correspondents and associated messages.
			<br/>
			<br/>

		<img class="helpdrawer-images" src="images/entities.svg">
		Browse and edit recognized entities and associated messages.
			<br/>
			<br/>

		<img class="helpdrawer-images" src="images/labels_dashboard.svg">
		View, add, and edit general and restriction (machine-actionable) labels, and view associated messages.
			<br/>
			<br/>

		<img class="helpdrawer-images" src="images/image_attachments.svg">
		Browse image attachments and associated messages.
			<br/>
			<br/>


		<img class="helpdrawer-images" src="images/other_attachements.svg">
		View other attachments and associated messages.
			<br/>
			<br/>

		<img class="helpdrawer-images" src="images/folders.svg">
		View original message folders or sources and associated messages.
			<br/>
			<br/>

		<img class="helpdrawer-images" src="images/lexicon_dashboard.svg">
		Browse, create, and modify lexicon searches.
			<br/>
			<br/>

		<img class="helpdrawer-images" src="images/reports.svg">
		View, import and merge reports.
			<br/>
			<br/>

		<img class="helpdrawer-images" src="images/more.svg">
		Access additional options, including verifying the bag checksum, adding images associated with the creator, and setting default labels.
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

		<img class="helpdrawer-images" src="images/correspondents.svg">
		Browse and graph correspondents and associated messages.
			<br/>
			<br/>
		<img class="helpdrawer-images" src="images/entities.svg">
		Browse recognized entities and associated messages.
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
	JSPHelper.log.warn ("this is a warning");
    AddressBook ab = archive.addressBook;
	String addressBookUpdate = request.getParameter("addressBookUpdate");
	if (!Util.nullOrEmpty(addressBookUpdate)) {
        archive.addressBook.initialize(addressBookUpdate,archive.getAllDocs());
		archive.recreateCorrespondentAuthorityMapper(); // we have to recreate auth mappings since they may have changed
        //SimpleSessions.saveArchive(archive);//instead of saving whole archive object now we save only those parts which changed.
		ArchiveReaderWriter.saveAddressBook(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
        ArchiveReaderWriter.saveCorrespondentAuthorityMapper(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);

        //after this invalidate the cache for removing the result of AddressBook.getCountsAsJSON result.. Or should we recalculate the result here only.
    }

	String entityMerges = request.getParameter("entityMerges");
	if (!Util.nullOrEmpty(entityMerges)) {
		Short type = -1;
		type = Short.parseShort (request.getParameter ("entityType"));
		//get entitybook through entitybook manager.
		EntityBookManager entityBookManager = archive.getEntityBookManager();
		try {

		    entityBookManager.fillEntityBookFromText(entityMerges,type,true);
			//SimpleSessions.saveArchive(archive);//instead of saving whole archive object now we save only those parts which changed.
			ArchiveReaderWriter.saveEntityBookManager(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE,type);
		} catch (Exception e) {
			Util.print_exception("Error in merging entities", e, JSPHelper.log);
		}
	}

	Collection<EmailDocument> allDocs = (Collection) archive.getAllDocs();

	//get number of labels.
	int numlabels = archive.getLabelManager().getAllLabels().size();
	//int nDocAttachments = EmailUtils.countDocumentAttachmentsInDocs(allDocs);
	int nFolders = archive.getAllFolders().size();
	boolean statsAvailable = false;
	if(archive.collectionMetadata.entityCounts != null)
		statsAvailable = true;
	int nPersonEntities = 0;
	String nSensitiveMessages ="";
	int nNonPersonEntities = 0,outCount=0,inCount=0,nAttachments=0,nImageAttachments=0;
	//outCount = archive.collectionMetadata.nOutgoingMessages;
	//inCount = archive.collectionMetadata.nIncomingMessages;
	//if normalization map is available in blobstore recalculate the attachments count.
	if(archive.getBlobStore().isNormalized()){
	    ArchiveReaderWriter.recalculateCollectionMetadata(archive);

	}
	nAttachments = archive.collectionMetadata.nBlobs;//EmailUtils.countAttachmentsInDocs(allDocs);
	nImageAttachments = archive.collectionMetadata.nImageBlobs;//EmailUtils.countImageAttachmentsInDocs(allDocs);

	if(statsAvailable){
		nPersonEntities = archive.collectionMetadata.entityCounts.getOrDefault(NEType.Type.PERSON.getCode(), 0);
		for (short fineType: archive.collectionMetadata.entityCounts.keySet()) {
			if (NEType.getTypeForCode (fineType) != NEType.Type.PERSON)
				nNonPersonEntities += archive.collectionMetadata.entityCounts.getOrDefault(fineType,0);
		}
	}
	if(archive.collectionMetadata.numPotentiallySensitiveMessages>=0)
		nSensitiveMessages = " ("+archive.collectionMetadata.numPotentiallySensitiveMessages+")";

	int nContacts = ab.allContacts().size();
%>

<% writeProfileBlock(out, true, archive, "Dashboard", 864);%>

<div id="all-cards" style="text-align: center; margin:auto">
	<div class="cta-box margin30">
		<a href="correspondents?archiveID=<%=archiveID%>">
			<img  class="icon-images" src="images/correspondents.svg">
			<%--<i class="icon-browsetoparrow"></i>--%>
			<%--<i class="fa fa-address-card-o" style="color:#3182bd" aria-hidden="true"></i>--%>
			<h1 ><%=Util.commatize(nContacts)%></h1>
			<h2 >Correspondents</h2>
			<%--<p class="cta-text-1"><%=nContacts %></p>--%>
			<%--<p class="cta-text-2">Correspondents (<%=nContacts %>)</p>--%>
		</a>
	</div>

	<div class="cta-box  margin30">
		<a href="entity-types?archiveID=<%=archiveID%>">
			<img class="icon-images" src="images/entities.svg">
			<h1><%=Util.commatize(nPersonEntities + nNonPersonEntities)%></h1>
			<h2>Entities</h2>
			<%--<i class="icon-browsetoparrow"></i>--%>
			<%--<i class="fa fa-thumb-tack" style="color:#31a354" aria-hidden="true"></i>--%>
			<%--<p class="cta-text-1">Entities (<%=nPersonEntities + nNonPersonEntities%>)</p>--%>
			<%--<p class="cta-text-2">Entities (<%=nPersonEntities + nNonPersonEntities%>)</p>--%>
		</a>
	</div>




	<% if (!ModeConfig.isDiscoveryMode()) { %>
	<div class="cta-box margin30">
		<a href="labels?archiveID=<%=archiveID%>">
			<img class="icon-images" src="images/labels_dashboard.svg">
			<h1 ><%=Util.commatize(numlabels)%></h1><%-- making it invisible because of the formatting issue--%>
			<h2>Labels</h2>
			<%--<i class="icon-browsetoparrow"></i>--%>
			<%--<i class="fa fa-tags" style="color:#0b967f" aria-hidden="true"></i>--%>
			<%--<p class="cta-text-1">Labels</p>--%>
			<%--<p class="cta-text-2">Labels</p>--%>
		</a>
	</div>
		<br/>

		<div class="cta-box  margin30">
				<%--<a href="image-attachments?archiveID=<%=archiveID%>&attachmentExtension=jpg&attachmentExtension=png&attachmentExtension=gif&attachmentExtension=bmp&attachmentExtension=jpeg&attachmentExtension=svg&attachmentExtension=tif&startDate=&endDate=">--%>
					<a href="browse?archiveID=<%=archiveID%>&attachmentType=jpg;jpeg;svg;png;gif;bmp;tif&browseType=attachments">
					<img class="icon-images" src="images/image_attachments.svg">
					<h1 id="nImageAttachments"><%=Util.commatize(nImageAttachments)%></h1>
					<h2>Image attachments</h2>
					<%--<i class="icon-browsetoparrow"></i>--%>
					<%--<i class="fa fa-picture-o" style="color:#756bb1" aria-hidden="true"></i>--%>
					<%--<p class="cta-text-1">Image attachments (<span id="nImageAttachments"><%=nImageAttachments%></span>)</p>--%>
					<%--<p class="cta-text-2">Image attachments (<%=nImageAttachments%>)</p>--%>
				</a>
		</div>


		<div class="cta-box  margin30">
			<%--<a href="attachments?archiveID=<%=archiveID%>&attachmentType=doc%3Bdocx%3Bpages&attachmentType=ppt%3Bpptx%3Bkey&attachmentType=xls%3Bxlsx%3Bnumbers&attachmentType=htm%3Bhtml%3Bcss%3Bjs&attachmentType=zip%3B7z%3Btar%3Btgz&attachmentType=mp3%3Bogg&attachmentType=avi%3Bmp4&attachmentType=fmp%3Bdb%3Bmdb%3Baccdb&attachmentType=pdf&attachmentType=others&startDate=&endDate=">--%>
				<a href="browse?archiveID=<%=archiveID%>&browseType=attachments">
				<img class="icon-images" src="images/other_attachments.svg">
				<%--<h1 id="nOtherAttachments"><%=Util.commatize(nAttachments - nImageAttachments)%></h1>--%>
					<h1 id="nOtherAttachments"><%=Util.commatize(nAttachments)%></h1>

					<h2>All attachments</h2>
				<%--<i class="icon-browsetoparrow"></i>--%>
				<%--<i class="fa fa-files-o" style="color:brown" aria-hidden="true"></i>--%>
				<%--<p class="cta-text-1">Other attachments (<span id="nOtherAttachments"><%=(nAttachments - nImageAttachments)%></span>)</p>--%>
				<%--<p class="cta-text-2">Other attachments (<%=(nAttachments - nImageAttachments)%>)</p>--%>
			</a>
		</div>

		<div class="cta-box  margin30">
			<a href="by-folder?archiveID=<%=archiveID%>">
				<img class="icon-images" src="images/folders.svg">
				<h1><%=Util.commatize(nFolders)%></h1>
				<h2>Folders</h2>
				<%--<i class="icon-browsetoparrow"></i>--%>
				<%--<i class="fa fa-folder-o" style="color:#636363" aria-hidden="true"></i>--%>
				<%--<p class="cta-text-1">Folders (<%=nFolders%>)</p>--%>
				<%--<p class="cta-text-2">Folders (<%=nFolders%>)</p>--%>
			</a>
		</div>

		<br/>

		<div class="cta-box  margin30">
			<a href="lexicon-top?archiveID=<%=archiveID%>">
				<img class="icon-images" src="images/lexicon_dashboard.svg">
				<h1 style="color:#f5f5f8"></h1><%-- making it invisible because of the formatting issue--%>
				<h2>Lexicon search</h2>
				<%--<i class="icon-browsetoparrow"></i>--%>
				<%--<img src="images/lexicon.png"/>--%>
				<%--<p class="cta-text-1">Lexicon search</p>--%>
				<%--<p class="cta-text-2">Lexicon search</p>--%>
			</a>
		</div>

		<% if (!ModeConfig.isDeliveryMode()) { %>
			<div class="cta-box  margin30">
				<a href="report?archiveID=<%=archiveID%>">
					<img class="icon-images" src="images/reports.svg">
					<h1 style="color:#f5f5f8"></h1><%-- making it invisible because of the formatting issue--%>
					<h2>Reports</h2>
					<%--<i class="icon-browsetoparrow"></i>--%>
					<%--<i class="fa fa-flag-o" style="color:#d41298" aria-hidden="true"></i>--%>
					<%--<p class="cta-text-1">Reports</p>--%>
					<%--<p class="cta-text-2">Reports</p>--%>
				</a>
			</div>
		<% } %>

		<div class="cta-box  margin30">
			<a href="settings?archiveID=<%=archiveID%>">
				<img class="icon-images" src="images/more.svg">
				<h1 style="color:#f5f5f8"></h1><%-- making it invisible because of the formatting issue--%>
				<h2>More</h2>
				<%--<i class="icon-browsetoparrow"></i>--%>
				<%--<i class="fa fa-cog" style="color:#d41298" aria-hidden="true"></i>--%>
				<%--<p class="cta-text-1">More</p>--%>
				<%--<p class="cta-text-2">More</p>--%>
			</a>
		</div>
	<% } %>

</div> <!--  allCards -->

<div>
    <div id="profilePhoto-upload-modal" class="info-modal modal fade" style="z-index:99999">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title">Upload a profile photo (1:1 aspect ratio)</h4>
                </div>
                <div class="modal-body">
                    <form id="uploadProfilePhotoForm" method="POST" enctype="multipart/form-data" >
                        <input type="hidden" value="<%=archiveID%>" name="archiveID"/>
                        <div class="form-group **text-left**">
                            <label for="profilePhoto" class="col-sm-2 control-label **text-left**">File</label>
                            <div class="col-sm-10">
                                <input type="file" id="profilePhoto" name="profilePhoto" value=""/>
                            </div>
                        </div>
                        <%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

                    </form>
                </div>
                <div class="modal-footer">
                    <button id="upload-btn" class="btn btn-cta" onclick="uploadProfilePhotoHandler();return false;">Upload <i class="icon-arrowbutton"></i></button>


                    <%--<button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>--%>
                    <%--<button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>--%>
                </div>
            </div><!-- /.modal-content -->
        </div><!-- /.modal-dialog -->
    </div><!-- /.modal -->
</div>
<script>
    var uploadProfilePhotoHandler=function() {
        //collect archiveID,and addressbookfile field. If  empty return false;
        var filePath = $('#profilePhoto').val();
        if (!filePath) {
            alert('Please provide the path of the profile photo');
            return false;
        }

        var form = $('#uploadProfilePhotoForm')[0];

        // Create an FormData object
        var data = new FormData(form);
        //hide the modal.
        $('#profilePhoto-upload-modal').modal('hide');
        //now send to the backend.. on it's success reload the same page. On failure display the error message.

        $.ajax({
            type: 'POST',
            enctype: 'multipart/form-data',
            processData: false,
            url: "ajax/upload-images.jsp",
            contentType: false,
            cache: false,
            data: data,
            success: function (data) {
                //epadd.success('Profile photo uploaded and applied.', function () {
                    window.location.reload();
                //});
            },
            error: function (jq, textStatus, errorThrown) {
                epadd.error("Error uploading file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
            }
        });
    }

    $('div.profile-pic-edit').click (function() {
        $('#profilePhoto-upload-modal').modal();
    });

</script>
<jsp:include page="footer.jsp"/>
<script>
	$('.cta-box').click(function(e) { var href = $('a', $(e.target)).attr('href'); if (href) { window.location = href;}}); // clicking anywhere in the cta-box should dispatch to the href of the only link inside it
</script>


</body>
</html>
