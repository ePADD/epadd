<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@ page import="edu.stanford.muse.ner.featuregen.FeatureDictionary" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
<link rel="icon" type="image/png" href="images/epadd-favicon.png">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Archive Information</title>
<!--	<link rel="stylesheet" type="text/css" href="http://fonts.googleapis.com/css?family=Open+Sans"> -->

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
	AddressBook ab = archive.addressBook;
	String addressBookUpdate = request.getParameter("addressBookUpdate");
	if (!Util.nullOrEmpty(addressBookUpdate))
		ab.initialize(addressBookUpdate);

	Collection<EmailDocument> allDocs = (Collection) JSPHelper.getSessionAttribute(session, "emailDocs");
	Collection<EmailDocument> fullEmailDocs = (Collection) archive.getAllDocs();
	if (allDocs == null)
		allDocs = fullEmailDocs;

	Indexer indexer = null;
	if (archive != null)
		indexer = archive.indexer;

	String bestName = ab.getBestNameForSelf();
	String title = "Email Archive " + (!Util.nullOrEmpty(bestName) ? ("of " + bestName) : "SUMMARY");
	Contact me = ab.getContactForSelf();
	String title_tooltip = title;
	if (me != null)
		title_tooltip = "a.k.a." + me.toTooltip();

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
		pC=" ("+archive.processingMetadata.entityCounts.get(FeatureDictionary.PERSON)+")";
		oC=" ("+archive.processingMetadata.entityCounts.get(FeatureDictionary.ORGANISATION )+")";
		lC=" ("+archive.processingMetadata.entityCounts.get(FeatureDictionary.PLACE)+")";
	}
	if(archive.processingMetadata.numPotentiallySensitiveMessages>=0)
		nS = " ("+archive.processingMetadata.numPotentiallySensitiveMessages+")";

	JSPHelper.log.info("Counts: "+pC+", "+oC+", "+lC);
%>

<%writeProfileBlock(out, bestName, "Date Range: ", IndexUtils.getDateRangeAsString((List) allDocs), "Messages: ", inCount + " incoming, " + outCount + " outgoing.");%>
<br/>

<div id="all-cards" style="text-align: center; margin:auto">
	<div class="cta-box text-center margin30">
		<a href="correspondents">
			<i class="icon-browsetoparrow"></i>
			<img src="images/correspondent.svg"/>
			<p class="cta-text-1">Correspondents (<%=ab.sortedContacts(allDocs).size() %>)</p>
			<p class="cta-text-2">Correspondents (<%=ab.sortedContacts(allDocs).size() %>)</p>
		</a>
	</div>
	
	<div class="cta-box text-center margin30">
			<a href="entities?type=en_person">
			<i class="icon-browsetoparrow"></i>
			<img src="images/person.svg"/>
			<p class="cta-text-1">Persons<%=pC%></p>
			<p class="cta-text-2">Persons<%=pC%></p>
			</a>
	</div>

	<% if (ModeConfig.isDiscoveryMode()) { %>
		<br/>
	<% } %>

	<div class="cta-box text-center margin30">
		<a href="entities?type=en_org">
			<i class="icon-browsetoparrow"></i>
			<img src="images/org.svg"/>
			<p class="cta-text-1">Organizations<%=oC%></p>
			<p class="cta-text-2">Organizations<%=oC%></p>
		</a>
	</div>

	<% if (!ModeConfig.isDiscoveryMode()) { %>
		<br/>
	<% } %>

	<div class="cta-box text-center margin30">
		<a href="entities?type=en_loc">
			<i class="icon-browsetoparrow"></i>
			<img src="images/location.svg"/>
			<p class="cta-text-1">Locations<%=lC%></p>
			<p class="cta-text-2">Locations<%=lC%></p>
		</a>
	</div>	

	<% if (ModeConfig.isDiscoveryMode()) { %>
		<br/>
	<% } else { %>
		<div class="cta-box text-center margin30">
				<a href="image-attachments">
					<i class="icon-browsetoparrow"></i>
					<img src="images/image-attachment.svg"/>
					<p class="cta-text-1">Image attachments (<%=nImageAttachments%>)</p>
					<p class="cta-text-2">Image attachments (<%=nImageAttachments%>)</p>
				</a>
		</div>

		<div class="cta-box text-center margin30">
				<a href="attachments?type=doc">
					<i class="icon-browsetoparrow"></i>
					<img src="images/doc-attachment.svg"/>
					<p class="cta-text-1">Document attachments (<%=nDocAttachments%>)</p>
					<p class="cta-text-2">Document attachments (<%=nDocAttachments%>)</p>
				</a>		
		</div>

		<br/>

		<div class="cta-box text-center margin30">
				<a href="attachments?type=nondoc">
					<i class="icon-browsetoparrow"></i>
				<!--	<i class="fa fa-paperclip" style="font-size:30px"></i> -->
					<img src="images/other-attachment.svg"/>
					<p class="cta-text-1">Other attachments (<%=(nAttachments - nImageAttachments - nDocAttachments)%>)</p>
					<p class="cta-text-2">Other attachments (<%=(nAttachments - nImageAttachments - nDocAttachments)%>)</p>
				</a>
		</div>

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
	<% } %>

</div> <!--  allCards -->
<jsp:include page="footer.jsp"/>
<script>
	$('.cta-box').click(function(e) { var href = $('a', $(e.target)).attr('href'); window.location = href;}); // clicking anywhere in the cta-box should dispatch to the href of the only link inside it
</script>
</body>
</html>
