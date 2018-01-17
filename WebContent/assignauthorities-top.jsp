<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="java.util.Collection" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@include file="getArchive.jspf" %>

<!-- currently unused. May need to revive when we do auth. types for entity types other than just correspondents -->
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="icon" type="image/png" href="images/epadd-favicon.png">
<title>Assign authorities</title>
<script src="js/jquery.js"></script>
<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

<jsp:include page="css/css.jsp"/>

<script src="js/muse.js"></script>
<script src="js/epadd.js"></script>

<style>
	td > div {
		padding: 5px;
	}
	div.cta-box { width:270px; height: 200px; display: inline-block; border-color:black; margin:10px;}
	div.cta-box img { margin-bottom: 10px; height: 30px;}
	div.cta-box:hover img { display: none; }
	div.cta-box:hover { color: white; }
	div.cta-box:hover a { color: white; }
</style>

</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Authorities');</script>

		<%
			Collection<EmailDocument> allDocs = (Collection) JSPHelper.getSessionAttribute(session, "emailDocs");
			Collection<EmailDocument> fullEmailDocs = (Collection) archive.getAllDocs();
			if (allDocs == null)
				allDocs = fullEmailDocs;
			boolean statsAvailable = false;
			if(archive.collectionMetadata.entityCounts != null)
				statsAvailable = true;
			String pC="",oC="",lC=""; // , nS="";
			if(statsAvailable){
				pC=" ("+archive.collectionMetadata.entityCounts.get(NEType.Type.PERSON.getCode())+")";
				oC=" ("+archive.collectionMetadata.entityCounts.get(NEType.Type.ORGANISATION.getCode())+")";
				lC=" ("+archive.collectionMetadata.entityCounts.get(NEType.Type.PLACE.getCode())+")";
			}
			AddressBook ab = archive.addressBook;
			writeProfileBlock(out, archive, "", "Assign authorities");%>
<br/>
<div id="all-cards" style="text-align: center; margin:auto">

	<br/>
	<div class="cta-box text-center margin30">
		<a href="assign-authorities?type=correspondent">
			<i class="icon-browsetoparrow"></i>
			<img src="images/correspondent.svg"/>
			<p class="cta-text-1">Correspondents (<%=ab.sortedContacts(allDocs).size() %>)</p>
			<p class="cta-text-2">Correspondents (<%=ab.sortedContacts(allDocs).size() %>)</p>
		</a>
	</div>

			<div class="cta-box text-center margin30">
				<a href="assign-authorities?type=person">
					<i class="icon-browsetoparrow"></i>
					<img src="images/person.svg"/>
					<p class="cta-text-1">Persons<%=pC%></p>
					<p class="cta-text-2">Persons<%=pC%></p>
				</a>
			</div>
<br/>
			<div class="cta-box text-center margin30">
				<a href="assign-authorities?type=org">
					<i class="icon-browsetoparrow"></i>
					<img src="images/org.svg"/>
					<p class="cta-text-1">Organizations<%=oC%></p>
					<p class="cta-text-2">Organizations<%=oC%></p>
				</a>
			</div>

			<div class="cta-box text-center margin30">
				<a href="assign-authorities?type=places">
					<i class="icon-browsetoparrow"></i>
					<img src="images/location.svg"/>
					<p class="cta-text-1">Locations<%=lC%></p>
					<p class="cta-text-2">Locations<%=lC%></p>
				</a>
			</div>
			<br>
			</div>
<br>
<p>
<br/>
<br/>
	<jsp:include page="footer.jsp"/>

</html>