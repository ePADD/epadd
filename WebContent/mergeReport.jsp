<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="edu.stanford.muse.ner.Entity" %>

<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="edu.stanford.muse.ie.variants.EntityBook" %>
<%@ page import="java.util.stream.Stream" %>
<%@ page import="edu.stanford.muse.AddressBookManager.Contact" %>
<%@ page import="edu.stanford.muse.LabelManager.LabelManager" %>
<%@ page import="edu.stanford.muse.LabelManager.Label" %>
<%@ page contentType="text/html; charset=UTF-8"%>
<%@include file="getArchive.jspf" %>
<%
	Collection<EmailDocument> allDocs =  (Collection) archive.getAllDocs();
	String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	Archive.MergeResult mResult = archive.getLastMergeResult();
%>
<html>
<head>
	<title>Merge Report</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css">

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>

	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>

<%writeProfileBlock(out, archive, "Merge Report", "");%>

<!--sidebar content-->
<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>

<%--
<nav class="menu1" role="navigation">
	<h2>Merge report</h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<p>Merge entities by grouping them together using find, cut, and paste commands.
	<p>Unmerge entities by separating them using find, cut, and paste commands.
	<p>The first entity name listed within each group of entity names is the one ePADD will display in all search and browsing results and visualizations. Manually change this display name by moving a new name to the top of this list. Alternatively, you can supply a new entity display name to the top of the list. This supplied entity name does not need to appear in the email archive.
	<p>Assign entities to a different category by cutting and pasting them into a different entity category’s <b>Edit Entities</b> panel.


</nav>
<!--/sidebar-->

<div style="text-align:center;display:inline-block;vertical-align:top;margin-left:170px;margin-top:20px;">
	<select id="sort-order">
		<option <%=!alphaSort?"selected":""%> value="volume">Sort by frequency</option>
		<option <%=alphaSort?"selected":""%> value="alpha">Sort alphabetically</option>
	</select>
</div>
--%>

<script>

	$(document).ready(function() {
		$('#next').onclick(function(e) {
			var url = 'browse-top?archiveID=<%=archiveID%>';
			window.location = url;
		});
	});
</script>

<%
	//Prepare a string variable called resultString by appending data from mResult object.

	// start building the string that goes into the text box
	StringBuilder mergeReport = new StringBuilder();
	mergeReport.append("-------------Merge Report-------------\n");
	//For Index report
	mergeReport.append("Merged accession from "+mResult.accessionDir+"\n");
	mergeReport.append("Accession had " + mResult.nMessagesInAccession+" messages and "+ mResult.nAttachmentsInAccession+" attachments \n");
	mergeReport.append("Collection had " + mResult.nMessagesInCollection+" messages and "+ mResult.nAttachmentsInCollection+" attachments \n");
	mergeReport.append("\n\n\n");
	mergeReport.append("After merging: Collection has "+mResult.nFinalMessages+" messages and "+ mResult.nFinalAttachments+"\n");
	mergeReport.append(mResult.nCommonMessages+" messages were common between the accession and this collection\n");
	mergeReport.append(mResult.nFinalMessages-mResult.nCommonMessages+" messages imported from the accession\n");

	//For Addressbook report
	mergeReport.append("Following contacts in the collection addressbook were updated from the names/emails present in accession addressbook\n\n");
	AddressBook.MergeResult mResultAB = mResult.addressBookMergeResult;
	for(Contact c: mResultAB.mergedContacts.keySet()){
	    Contact oldContactInCollection = mResultAB.mergedContacts.get(c);
	    Set<String> oldElementsInCollectionContact = Util.setUnion(oldContactInCollection.getEmails(),oldContactInCollection.getNames());
	    Set<String> updatedElementsInCollectionContact = Util.setUnion(c.getEmails(),c.getNames());
		//find updated-old to get all new names/emails that were added
	    updatedElementsInCollectionContact.removeAll(oldElementsInCollectionContact);
	    mergeReport.append("-----------"+ updatedElementsInCollectionContact.size()+" names/emails were added in the following contact---------------\n");
		mergeReport.append(c.toString());
	    mergeReport.append("\n----------------\n");
	}

	mergeReport.append("Following new contacts were added from the accession address book\n");
	mergeReport.append("-----------\n");
	for(Contact c: mResultAB.newlycreatedContacts.keySet()){
	    Set<Contact> sourceContactsInAccession = mResultAB.newlycreatedContacts.get(c);
	    Set<String> elementsToSearchFor = new LinkedHashSet<>();
	    sourceContactsInAccession.forEach(cont->{
	        elementsToSearchFor.addAll(cont.getEmails());
	        elementsToSearchFor.addAll(cont.getNames());
	    });
		mergeReport.append(c.toString());
		//Find the potential contact candidates for merging with this contact as below;
		//For each element in elementsToSearch, search for element in the merged addressbook
		// display all those contacts (except that is same as 'c') as potential candidates for merging with this contact.
        //Why can't we do it only for all elements of this contact c? because this contact will not contain
		//those elements which were together in the incoming accession. For example.
		//A1:[xy], A2:[ax][ay].
		//Newly created contact is [a]. But we should list the contact [xy] as a potential candidate for merging.
	}
	//For LabelManager report
	mergeReport.append("\n-----------------------------------------\n");
	mergeReport.append("Following labels from the accession had same name as in the collection. They have been renamed to avoid clash. Please do the cleanup\n");
	LabelManager.MergeResult mResultLM = mResult.labManagerMergeResult;
		mergeReport.append("------------------------------\n");
	for(Pair<Label,Label> p: mResultLM.labelsWithNameClash){
	    mergeReport.append(p.first.toString());
	    mergeReport.append(p.second.toString());
	    mergeReport.append("-------------------------------\n");
	}
	mergeReport.append("Following new labels from the accession were added\n");
	mergeReport.append("------------------------------\n");
	for(Label p: mResultLM.newLabels){
		mergeReport.append(p.toString());
		mergeReport.append("-------------------------------\n");
	}
	//For lexcion merging report.
	mergeReport.append("Following new lexicons were imported from the accession \n");
	for(String s: mResult.newLexicons)
	    mergeReport.append(s+"\n");
	mergeReport.append("----------------------\n");
	mergeReport.append("Following lexicons were already present in the collection. Merge them manually if they are different\n");
	for(String s: mResult.clashedLexicons)
	    mergeReport.append(s+"\n");


%>

<p>

<div style="text-align:center">
    <!--http://stackoverflow.com/questions/254712/disable-spell-checking-on-html-textfields-->
		<textarea name="entityMerges" id="text" style="width:600px" rows="40" autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false"><%=mergeReport.toString()%>
		</textarea>
		<br/>
		<br/>
		<button class="btn btn-cta" id="next">Next<i class="icon-arrowbutton"></i> </button>
	</form>
<br/>

</div>
<p/>
<br/>
<jsp:include page="footer.jsp"/>
</body>
</html>
