<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page language="java" import="java.io.*"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page contentType="text/html; charset=UTF-8"%>
<%@include file="getArchive.jspf" %>
<%
	AddressBook addressBook = archive.addressBook;
	Collection<EmailDocument> allDocs = (Collection<EmailDocument>) JSPHelper.getSessionAttribute(session, "emailDocs");
	if (allDocs == null)
		allDocs = (Collection) archive.getAllDocs();
	String sort = request.getParameter("sort");
	boolean alphaSort = ("alphabetical".equals(sort));
%>
<html>
<head>
	<title>Edit Correspondents</title>
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

<%writeProfileBlock(out, archive, "Edit address book", "");%>

<!--sidebar content-->
<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>

<nav class="menu1" role="navigation">
	<h2>Edit Correspondents</h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<!--phrase-->
	<div class="search-tips">
		<p>
			This screen allows the user to view and edit the email address(es) and name(s) associated with each correspondent
		<p>

			The archive owner should always be the first individual listed.
		<p>
			The first name listed within each group of names or addresses is the name ePADD will display in all search and browsing results and visualizations. Manually change this display name by adding a new name at the top of this list

		<p>
			If names or addresses are incorrectly associated with the wrong individual, you can manually cut and paste them underneath a different entry

		<p>
			Mailing list addresses can be manually identified and added under the heading “-- [ML]” If this heading does not exist simply create it.

		</p>
	</div>
</nav>
<!--/sidebar-->

<br/>
<br/>
<div style="text-align:center;margin-left:40%;width:20%;">
	<div class="form-group">
		<label for="sort-order">Sort order</label>
		<select id="sort-order" name="sort-order" class="form-control selectpicker">
			<option <%=!alphaSort ? "selected" : ""%> value="volume">Sort by email volume</option>
			<option <%=alphaSort ? "selected" : ""%> value="alpha">Sort alphabetically</option>
		</select>
	</div>

</div>

<script>
	$(document).ready(function() {
		$('#sort-order').change(function (e) {
			if ('alpha' == this.value)
				window.location = 'edit-correspondents?sort=alphabetical';
			else
				window.location = 'edit-correspondents';
		});
	});
</script>

<p>
<div style="text-align:center">
<form method="post" action="browse-top">
    <!--http://stackoverflow.com/questions/254712/disable-spell-checking-on-html-textfields-->
<textarea name="addressBookUpdate" id="text" style="width:600px" rows="40" autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false">
<%!
private static String dumpForContact(Contact c, String description) {
	StringBuilder sb = new StringBuilder();
	String mailingListOutput = (c.mailingListState & (MailingList.SUPER_DEFINITE | MailingList.USER_ASSIGNED)) != 0 ? MailingList.MAILING_LIST_MARKER : "";
	sb.append ("-- " + mailingListOutput + " " + description + "\n");

	// extra defensive. c.names is already supposed to be a set, but sometimes got an extra blank at the end.
	Set<String> uniqueNames = new LinkedHashSet<String>();
	for (String s: c.names)
		if (!Util.nullOrEmpty(s))
			uniqueNames.add(s);
	// uniqueNames.add(s.trim());
	
	Set<String> uniqueEmails = new LinkedHashSet<String>();
	for (String s: c.emails)
		if (!Util.nullOrEmpty(s))
			uniqueEmails.add(s);
	
	for (String s: uniqueNames)
	{
		sb.append (Util.escapeHTML(s) + "\n");
	}
	for (String s: uniqueEmails)
		sb.append (Util.escapeHTML(s) + "\n");
	sb.append("\n");
	return sb.toString();
}
%>
<%
// always toString first contact as self
Contact self = addressBook.getContactForSelf();
if (self != null)
	out.print(dumpForContact(self, "Archive owner"));

if (!alphaSort)
{
	for (Contact c: addressBook.sortedContacts((Collection) archive.getAllDocs()))
		if (c != self)
			out.print(dumpForContact(c, ""));
}
else
{
	// build up a map of best name -> contact, sort it by best name and toString contacts in the resulting order
	List<Contact> allContacts = addressBook.allContacts();
	Map<String, Contact> canonicalBestNameToContact = new LinkedHashMap<String, Contact>();
	for (Contact c: allContacts)
	{
		if (c == self)
			continue;
		String bestEmail = c.pickBestName();
		if (bestEmail == null)
			continue;
		canonicalBestNameToContact.put(bestEmail.toLowerCase(), c);		
	}
	
	List<Pair<String, Contact>> pairs = Util.mapToListOfPairs(canonicalBestNameToContact);
	Util.sortPairsByFirstElement(pairs);
	
	for (Pair<String, Contact> p: pairs)
	{
		Contact c = p.getSecond();
		if (c != self)
			out.print(dumpForContact(c, c.pickBestName()));
	}
}

	
%>
</textarea>
<br/>

<button class="btn btn-cta" type="submit">Save <i class="icon-arrowbutton"></i> </button>
</form>
</div>
<p/>
<br/>
<jsp:include page="footer.jsp"/>
</body>
</html>
