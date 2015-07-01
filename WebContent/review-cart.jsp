<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="org.json.JSONArray"%>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
	<title>Save cart</title>
	
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<script src="js/jquery.dataTables.min.js"></script>
		
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	
	<style type="text/css">
      .js #table {display: none;}
    </style>
	
	<script type="text/javascript" charset="utf-8">
	 $('html').addClass('js'); // see http://www.learningjquery.com/2008/10/1-way-to-avoid-the-flash-of-unstyled-content/
	</script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Cart');</script>

<br/>

<%
AddressBook ab = archive.addressBook;
String bestName = ab.getBestNameForSelf();
Contact ownContact = ab.getContactForSelf();
Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
List<Contact> allContacts = ab.sortedContacts((Collection) docs);
Map<Contact, Integer> contactInCount = new LinkedHashMap<Contact, Integer>(), contactOutCount = new LinkedHashMap<Contact, Integer>(), contactMentionCount = new LinkedHashMap<Contact, Integer>();

String type = request.getParameter("type");

// filter all docs to only get the type requested.
Collection<EmailDocument> newDocs = new ArrayList<EmailDocument>();
for (EmailDocument ed: docs)
{
	if (ed.addedToCart)
		newDocs.add(ed);
}

docs = newDocs;
writeProfileBlock(out, bestName, "", Util.pluralize(docs.size(), "message") + " in cart");
%>

<br/>
<form method="post" action="export-cart.jsp">
<div style="margin:auto; width:900px">
<table id="messages">
	<thead><tr><th>Subject</th><th>Date</th><th>Annotation</th></tr></thead>
	<tbody>

<%
	JSONArray resultArray = new JSONArray();
int count = 0;
// compute counts
for (EmailDocument ed: docs)
{
	String docId = ed.getUniqueId();
    String messageURL = "browse?docId=" + docId;
	String subject = !Util.nullOrEmpty(ed.description) ? ed.description : "NONE";
	JSONArray j = new JSONArray();
	j.put (0, Util.escapeHTML(subject));
	j.put (1, ed.dateString());
	j.put (2, Util.ellipsize(ed.comment == null ? "NONE" : Util.escapeHTML(ed.comment), 60));
	j.put (3, messageURL);

	resultArray.put (count++, j);	%>
	<%
}
%>
	</tbody>
</table>
<p>
<br/>
<div style="margin:auto;text-align:center">
<% if (docs.size() > 0) { %>
	<button class="btn btn-cta">Request messages  <i class="icon-arrowbutton"></i> </button>
<% } %>
</div>
</div>
</form>

<p>

	<jsp:include page="footer.jsp"/>
<script>

	$(document).ready(function() {
		var clickable_message = function ( data, type, full, meta ) {
			return '<a target="_blank" href="' + full[3] + '">' + data + '</a>';
		};

		var messages = <%=resultArray.toString(4)%>;
		$('#messages').dataTable({
			data: messages,
			pagingType: 'simple',
			columnDefs: [{width: "50%", targets: 0},{targets: 0,render:clickable_message}],
			order:[[1, 'asc']] // col 1 (date), ascending
		});
		$('#messages').show();
	} );
</script>

</body>
</html>
