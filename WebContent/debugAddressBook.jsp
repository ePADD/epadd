<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page language="java" import="java.io.*"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="javax.mail.Address" %>
<%@ page import="javax.mail.internet.InternetAddress" %>
<%@ page import="com.google.common.collect.*" %>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
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
	<title>Debug Correspondents</title>
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

<%writeProfileBlock(out, archive, "Debugging address book", "");%>
<div style="margin-left:170px">

<%

	Multimap<String, String> nameMap = ArrayListMultimap.create();
	Multimap<String, String> emailMap = ArrayListMultimap.create();
    int n = 0;

	for (Document doc: archive.getAllDocs()) {
		EmailDocument ed = (EmailDocument) doc;
		List<Address> list = ed.getToCCBCC();

		List<Address> allAddrs = new ArrayList<>();
		if (list != null)
			allAddrs.addAll (list);
		if (ed.from != null)
			for (Address a: ed.from)
				allAddrs.add (a);

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
            n++;
		}
	}
    %>

	<h2><%=emailMap.size()%> email addresses</h2>
	<%
		List<String> emails = new ArrayList<>(emailMap.keySet());
		Collections.sort(emails, new Comparator<String>() {
			@Override
			public int compare(String e1, String e2) {
				return Integer.compare(emailMap.get(e2).size(), emailMap.get(e1).size());
			}
		});

		for (String emailAddr: emails) {
			out.println ("Email address: " + Util.escapeHTML(emailAddr) + ": #occurrences=" + emailMap.get(emailAddr).size() + " is associated with the following names:<br/>");

			Multiset<String> namesSet = LinkedHashMultiset.create(emailMap.get (emailAddr));

			for (String name : namesSet.elementSet()) {
                String link = "debugAddressBook-message.jsp?email=" + emailAddr + "&name=" + name;
                String linkHTML = "<a target=\"_blank\" href=\"" + link + "\">Search</a>";
				out.println("&nbsp;&nbsp;&nbsp;&nbsp;" + Util.escapeHTML (name) + ": " + namesSet.count(name) + " " + linkHTML + "<br/>\n");
			}
		}
	%>
	<hr/>
	<h2><%=nameMap.size()%> names</h2>

</div>
<p/>
<br/>
<jsp:include page="footer.jsp"/>
</body>
</html>
