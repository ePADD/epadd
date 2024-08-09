<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page import="java.io.*"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="java.util.*"%>
<%@page import="edu.stanford.muse.util.*"%>
<%@page import="edu.stanford.muse.index.*"%>
<%@page import="javax.mail.Address" %>
<%@page import="javax.mail.internet.InternetAddress" %>
<%@page import="com.google.common.collect.*" %>
<%@page import="edu.stanford.muse.AddressBookManager.Contact" %>
<%@page import="edu.stanford.muse.AddressBookManager.MailingList" %>
<%@page import="org.json.JSONArray" %>
<%@page contentType="text/html; charset=UTF-8"%>
<%@include file="getArchive.jspf" %>
<%
	Collection<EmailDocument> allDocs = (Collection<EmailDocument>) JSPHelper.getSessionAttribute(session, "emailDocs");
	if (allDocs == null)
		allDocs = (Collection) archive.getAllDocs();
%>
<html>
<head>
	<title>Debug Correspondents</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css?v=1.1">

	<script src="js/jquery.js"></script>
	<script src="js/jquery.dataTables.min.js"></script>

	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js?v=1.1"></script>

	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js?v=1.1"></script>
</head>
<body>
<%@include file="header.jspf"%>

<%writeProfileBlock(out, archive, "Debugging address book");%>
<div style="margin-left:170px">

<%
	Multimap<String, String> nameMap = ArrayListMultimap.create();
	Multimap<String, String> emailMap = ArrayListMultimap.create();

	for (Document doc: archive.getAllDocs()) {
		EmailDocument ed = (EmailDocument) doc;
		List<Address> list = ed.getToCCBCC();

		List<Address> allAddrs = new ArrayList<>();
		if (list != null)
			allAddrs.addAll (list);
		if (ed.from != null)
			allAddrs.addAll(Arrays.asList(ed.from));

		for (Address a: allAddrs) {
			InternetAddress ia = (InternetAddress) a;
			String email = ia.getAddress();
			email = EmailUtils.cleanEmailAddress(email);
			String name = ia.getPersonal();
			name = EmailUtils.cleanPersonName(name);
			if (Util.nullOrEmpty (name) || Util.nullOrEmpty (email))
				continue;
			emailMap.put (email, name);
			nameMap.put (name, email);
		}
	}
    %>

	<h2><%=emailMap.size()%> email addresses</h2>
	<%
		List<String> emails = new ArrayList<>(emailMap.keySet());
		emails.sort((e1, e2) -> Integer.compare(LinkedHashMultiset.create(emailMap.get(e2)).elementSet().size(), LinkedHashMultiset.create(emailMap.get(e1)).elementSet().size()));
		AddressBook addressBook = archive.addressBook;

		int count = 0;
		JSONArray resultArray = new JSONArray();

		for (String emailAddr: emails) {
			Contact c = addressBook.lookupByEmail(emailAddr);
			boolean isMailingList = false;
			if (c != null && c.mailingListState == MailingList.DEFINITE)
			    isMailingList = true;

			Multiset<String> namesSet = LinkedHashMultiset.create(emailMap.get (emailAddr));
			int namesSetCount = namesSet.elementSet().size();

			int messageCount =  emailMap.get(emailAddr).size();
			// out.println (count + ". Email address: " + Util.escapeHTML(emailAddr) + (isMailingList ? " [ML]":"") + " (" + Util.pluralize (messageCount, "message") + ") is associated with " + Util.pluralize(namesSetCount, "name") + ":<br/>");

			StringBuilder sb = new StringBuilder();
			for (String name : namesSet.elementSet()) {
                String link = "browse.jsp?debugAddressBookEmail=" + emailAddr + "&archiveID=" + ArchiveReaderWriter.getArchiveIDForArchive(archive)+ "&debugAddressBookName=" + name;
                String linkText = Util.pluralize(namesSet.count(name), "message");
                String linkHTML = "<a target=\"_blank\" href=\"" + link + "\">" + linkText + "</a>";
				sb.append("&nbsp;&nbsp;&nbsp;&nbsp;" + Util.escapeHTML (name) + " (" + linkHTML + ")<br/>\n");
			}

			// out.println (sb.toString());

			JSONArray j = new JSONArray();
			j.put(0, Util.escapeHTML(emailAddr));
			j.put(1, sb.toString());
			j.put(2, namesSetCount);
			j.put(3, messageCount);
			j.put(4, isMailingList ? "[ML]" : "");
			resultArray.put(count++, j);
		}
	%>
	<hr/>
</div>
<br/>

<div style="margin:auto; width:1100px">
	<table id="emailAddrs" style="display:none">
		<thead><tr><th>Email address</th><th>Associated names</th><th># associated names</th><th>Messages</th><th>Mailing list</th></tr></thead>
		<tbody>
		</tbody>
	</table>
	<div id="spinner-div" style="text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>
</div>
<br/>
<br/>

<script>

	$(document).ready(function() {
	    var emailAddrs = <%=resultArray.toString(4)%>;

	var clickable_message = function ( data, type, full, meta ) {
		return '<a target="_blank" title="' + full[5] + '" href="' + full[4] + '">' + data + '</a>'; // full[4] has the URL, full[5] has the title tooltip
	};

	$('#emailAddrs').dataTable({
		data: emailAddrs,
		pagingType: 'simple',
		order:[[2, 'desc']], // col 12 (outgoing message count), descending
		autoWidth: false,
		columnDefs: [{width: "550px", targets: 0}, { className: "dt-right", "targets": [ 2,3 ] },{width: "50%", targets: 0}], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
		fnInitComplete: function() { $('#spinner-div').hide(); $('#emailAddrs').fadeIn(); }
	});
	} );
</script>

<jsp:include page="footer.jsp"/>
</body>
</html>
