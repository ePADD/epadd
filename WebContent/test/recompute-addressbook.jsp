<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="edu.stanford.muse.AddressBookManager.Contact" %>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@ page contentType="text/html; charset=UTF-8"%>


<%@include file="../getArchive.jspf" %>
<html>
<head>
    <Title>Check address book</Title>
    <jsp:include page="../css/css.jsp"/>
    <link rel="icon" type="image/png" href="images/muse-favicon.png">
    <script type="text/javascript" src="js/protovis.js?v=1.1"></script>
    <script type="text/javascript" src="js/muse.js"></script>
</head>
<body>

<%!
    private static String dumpForContact(Contact c, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append ("-- " + description + "\n");

        // extra defensive. c.names is already supposed to be a set, but sometimes got an extra blank at the end.
        Set<String> uniqueNames = new LinkedHashSet<String>();
        for (String s: c.getNames())
            if (!Util.nullOrEmpty(s))
                uniqueNames.add(s);
        // uniqueNames.add(s.trim());

        Set<String> uniqueEmails = new LinkedHashSet<String>();
        for (String s: c.getEmails())
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
    DictUtils.initialize();

    AddressBook oldAB = archive.getAddressBook();
    Contact ownContact = oldAB.getContactForSelf();
    String ownEmail = "";
    if (ownContact.getEmails().size() > 0)
        ownEmail = ownContact.getEmails().iterator().next();
    String ownName = "";
    if (ownContact.getNames().size() > 0)
        ownName = ownContact.getNames().iterator().next();

    AddressBook ab = new AddressBook(new String[]{ownEmail}, new String[]{ownName});

    Set<String> trustedAddrs = new LinkedHashSet<>();
    for (String s: archive.ownerEmailAddrs)
        trustedAddrs.add (s);

    for (Document ed: archive.getAllDocs()) {
        ab.processContactsFromMessage((EmailDocument) ed, trustedAddrs);
    }
    ab.organizeContacts();
    archive.addressBook = ab;

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
