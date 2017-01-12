<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
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
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
<link rel="icon" type="image/png" href="images/epadd-favicon.png">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Archive Information</title>

	<script src="js/jquery.js"></script>
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>

	<style>
		div.cta-box { width:270px; height: 200px; display: inline-block; border-color:black; margin:10px; cursor: pointer; }
		div.cta-box img { margin-bottom: 10px; height: 30px;}
		div.cta-box:hover img { display: none; }
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
	String addressBookUpdate = request.getParameter("addressBookUpdate");
	if (!Util.nullOrEmpty(addressBookUpdate)) {
        archive.addressBook.initialize(addressBookUpdate);
        SimpleSessions.saveArchive(session);
    }

	Collection<EmailDocument> allDocs = (Collection) JSPHelper.getSessionAttribute(session, "emailDocs");
	Collection<EmailDocument> fullEmailDocs = (Collection) archive.getAllDocs();
	if (allDocs == null)
		allDocs = fullEmailDocs;

	int outCount = ab.getOutMessageCount(allDocs);
	int inCount = allDocs.size()-outCount;
	int nAttachments = EmailUtils.countAttachmentsInDocs(allDocs);
	int nImageAttachments = EmailUtils.countImageAttachmentsInDocs(allDocs);
	int nDocAttachments = EmailUtils.countDocumentAttachmentsInDocs(allDocs);

	boolean statsAvailable = false;
	if(archive.processingMetadata.entityCounts != null)
		statsAvailable = true;
	String pC="",oC="",lC="", nS="";
	if(statsAvailable){
		pC=" ("+archive.processingMetadata.entityCounts.get(NEType.Type.PERSON.getCode())+")";
		oC=" ("+archive.processingMetadata.entityCounts.get(NEType.Type.ORGANISATION.getCode() )+")";
		lC=" ("+archive.processingMetadata.entityCounts.get(NEType.Type.PLACE.getCode())+")";
	}
	if(archive.processingMetadata.numPotentiallySensitiveMessages>=0)
		nS = " ("+archive.processingMetadata.numPotentiallySensitiveMessages+")";

	JSPHelper.log.info("Counts: "+pC+", "+oC+", "+lC);
	int nContacts = ab.allContacts().size();
%>

<% writeProfileBlock(out, archive, "Date Range: ", IndexUtils.getDateRangeAsString((List) allDocs), "Messages: ", inCount + " incoming, " + outCount + " outgoing.");%>
<br/>

<div id="all-cards" style="text-align: center; margin:auto">
	<div class="cta-box text-center margin30">
		<a href="correspondents">
			<i class="icon-browsetoparrow"></i>
			<img src="images/correspondent.svg"/>
			<p class="cta-text-1">Correspondents (<%=nContacts %>)</p>
			<p class="cta-text-2">Correspondents (<%=nContacts %>)</p>
		</a>
	</div>
	
	<div class="cta-box text-center margin30">
			<a href="finetypes.jsp?type=0">
			<i class="icon-browsetoparrow"></i>
			<img src="images/person.svg"/>
			<p class="cta-text-1">Person entities<%=pC%></p>
			<p class="cta-text-2">Person entities<%=pC%></p>
			</a>
	</div>

	<% if (ModeConfig.isDiscoveryMode()) { %>
		<br/>
	<% } %>

	<div class="cta-box text-center margin30">
		<a href="finetypes.jsp?type=11">
			<i class="icon-browsetoparrow"></i>
			<img src="images/org.svg"/>
			<p class="cta-text-1">Other entities<%=oC%></p>
			<p class="cta-text-2">Other entities<%=oC%></p>
		</a>
	</div>

	<% if (!ModeConfig.isDiscoveryMode()) { %>
		<br/>
        <div class="cta-box text-center margin30">
            <a href="finetypes.jsp?type=3">
                <i class="icon-browsetoparrow"></i>
                <img src="images/location.svg"/>
                <p class="cta-text-1">Folder view<%=lC%></p>
                <p class="cta-text-2">Folder view<%=lC%></p>
            </a>
        </div>
    <% } %>

	<% if (ModeConfig.isDiscoveryMode()) { %>
		<br/>
	<% } else { %>
		<div class="cta-box text-center margin30">
				<a href="image-attachments">
					<i class="icon-browsetoparrow"></i>
					<img src="images/image-attachment.svg"/>
					<p class="cta-text-1">Image attachments (<span id="nImageAttachments"><%=nImageAttachments%></span>)</p>
					<p class="cta-text-2">Image attachments (<%=nImageAttachments%>)</p>
				</a>
		</div>


	<div class="cta-box text-center margin30">
		<a href="attachments">
			<i class="icon-browsetoparrow"></i>
			<img src="images/other-attachment.svg"/>
			<p class="cta-text-1">Other attachments (<span id="nOtherAttachments"><%=(nAttachments - nImageAttachments)%></span>)</p>
			<p class="cta-text-2">Other attachments (<%=nImageAttachments%>)</p>
		</a>
	</div>
	<br/>

		<div class="cta-box text-center margin30">
			<a href="lexicon">
				<i class="icon-browsetoparrow"></i>
				<img src="images/lexicon.svg"/>
				<p class="cta-text-1">Lexicon search</p>
				<p class="cta-text-2">Lexicon search</p>
			</a>
		</div>

		<% if (ModeConfig.isAppraisalMode()|| ModeConfig.isProcessingMode()) { %>
			<div class="cta-box text-center margin30">
				<a href="browse?sensitive=true">
					<i class="icon-browsetoparrow"></i>
					<img src="images/sensitive-message.svg"/>
					<p class="cta-text-1">Sensitive messages<%=nS%></p>
					<p class="cta-text-2">Sensitive messages<%=nS%></p>
				</a>
			</div>
		<% } %>

		<div class="cta-box text-center margin30">
			<a href="settings">
				<i class="icon-browsetoparrow"></i>
				<img src="images/sensitive-message.svg"/>
				<p class="cta-text-1">Settings</p>
				<p class="cta-text-2">Settings</p>
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
