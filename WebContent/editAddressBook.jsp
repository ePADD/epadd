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
	String bestName = addressBook.getBestNameForSelf();
%>
<html>
<head>
	<title>Edit Correspondents</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>
	
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>
	epadd.select_link('#nav1', 'Appraisal');
	epadd.select_link('#nav2', 'Edit Correspondents');
</script>

<h1 title="title_tooltip">Email Archives of <%= bestName%><br/>
<span style="font-size:60%">Edit Correspondents</span><br/>
</h1>

<p>
<div style="text-align:center">
<div style="width:600px;text-align:left;margin:auto">
	<%=edu.stanford.muse.util.Messages.getMessage("messages", "edit-correspondents")%>
</div>
<br/>
<p>
<% String href = (alphaSort ? "edit-correspondents" : "edit-correspondents?sort=alphabetical"); String text = (alphaSort ? "Sort by email volume" : "Sort alphabetically"); %>
<a href="<%=href%>"><%=text%></a><br/>
<form method="post" action="browse-top">
<textarea name="addressBookUpdate" id="text" style="width:600px" rows="40">
<%!
private static String dumpForContact(Contact c, String description) {
	StringBuilder sb = new StringBuilder();
	sb.append ("-- " + description + "\n");

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
// always print first contact as self
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
	// build up a map of best name -> contact, sort it by best name and print contacts in the resulting order
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
