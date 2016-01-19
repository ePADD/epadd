<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page contentType="text/html; charset=UTF-8"%>


<%@include file="../getArchive.jspf" %>
<html>
<head>
    <Title>Check address book</Title>
    <jsp:include page="../css/css.jsp"/>
    <link rel="icon" type="image/png" href="images/muse-favicon.png">
    <script type="text/javascript" src="js/protovis.js"></script>
    <script type="text/javascript" src="js/muse.js"></script>
</head>
<body>

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
            sb.append (Util.escapeHTML(s) + "\n<br/>");
        }
        for (String s: uniqueEmails)
            sb.append (Util.escapeHTML(s) + "\n<br/>");
        sb.append("\n");
        return sb.toString();
    }
%>
<%
    AddressBook oldAB = archive.getAddressBook();
    AddressBook ab = new AddressBook(new String[]{"Terry Winograd"}, new String[]{"winograd@cs.stanford.edu", "winograd@stanford.edu"});

    for (Document ed: archive.getAllDocs()) {
        ab.processContactsFromMessage((EmailDocument) ed);
    }

    Map<String, Contact> canonicalBestNameToContact = new LinkedHashMap<String, Contact>();
    for (Contact c: ab.allContacts())
    {
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
            out.print(dumpForContact(c, ""));
    }

%>
<jsp:include page="../footer.jsp"/>
</body>
</html>
