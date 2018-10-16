<%@ page contentType="text/html; charset=UTF-8"%>
<% JSPHelper.checkContainer(request); // do this early on so we are set up
  request.setCharacterEncoding("UTF-8"); %>
<%@page language="java" import="edu.stanford.muse.email.FetchStats"%>
<%@page language="java" import="java.util.ArrayList"%>
<%@page language="java" import="java.util.Collection"%>
<%@page language="java" import="java.util.List"%>
<%@include file="getArchive.jspf" %>
<%
// we are already logged into all accounts at the point this is called
// we may not have finished reading the folders though.
	session.setMaxInactiveInterval(-1);
    // never let session expire

      response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
      response.setHeader("Pragma","no-cache"); //HTTP 1.0
      response.setDateHeader ("Expires", 0); //prevent caching at the proxy server
	  // remove existing status provider if any because sometimes status provider
	  // for prev. op prints stale status till new one is put in the session
      if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
		  session.removeAttribute("statusProvider");

    // re-read accounts again only if we don't already have them in this session.
    // later we might want to provide a way for users to refresh the list of folders.
  	AddressBook addressBook = archive.addressBook;

%>
<!DOCTYPE HTML>
<html lang="en">
<head>
<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<title>Archive Report</title>

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css"/>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script type="text/javascript" src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<%writeProfileBlock(out, archive, "Reports");%>


<div style="width: 1100px; margin:auto;min-height:300px;" class="panel rounded">

    Jump to <a href="#errors">Errors</a>
    <br/>
<%
    // warning: somewhat fragile. depends on the error string! Not desirable, but...
    // also note: we'd prefer to keep backward compatibility with existing archives
    // in every element of a errorTypes is a pair. the first element is the prefix of the error string to look for to identify this kind of error
    // the second is the string that will be printed on the page after the number of errors of that type
    String errorTypes[][] = new String[][]{
            {"No to/cc/bcc addresses", "messages without To, Cc or Bcc addresses"},
            {"No from address", "messages without a from address"},
            {"Bad address", "invalid email addresses"},
            {"Guessed date", "messages with a guessed date"},
            {"No date for message", "messages with no date"},
            {"Probably bad date", " messages with a probably bad date"},
            {"attachment filename is null", "attachments with no filename"},
            {"Duplicate message", "duplicate messages"},
            {"Duplicate attachments", "duplicate attachments"},
            {"Bad folder name", " Bad folder name"},
            {"Bad account name", " Bad account name"},
            {"", "other errors"} // this is needed as a catch all, otherwise the remaining errors won't be reported.
    };

	List<FetchStats> fetchStats = archive.allStats;
	int count = 0;
	if(fetchStats!=null) {
        for (FetchStats fs : fetchStats) {
            out.println("<h2>Import #" + (++count) + "</h2>");
            out.println(fs.toHTML());
            out.println("<hr/>");
        }
    }

    // gather all the errors, addressBook + archive
    Collection<String> allErrors = new ArrayList<>();
    {
        Collection<String> addressBookErrors = addressBook.getDataErrors();
        if (addressBookErrors != null)
            allErrors.addAll (addressBookErrors);

        Collection<String> archiveErrors = archive.getDataErrors();
        if (archiveErrors != null)
            allErrors.addAll(archiveErrors);
    }

    // gather the errors by type
    List<String>[] errorsByType = new ArrayList[errorTypes.length];
    for (int t = 0; t < errorsByType.length; t++)
        errorsByType[t] = new ArrayList<String>();

    for (String s: allErrors) {
        for (int t = 0; t < errorTypes.length; t++) {
            String prefix = errorTypes[t][0];
            if (s.startsWith (prefix)) {
                errorsByType[t].add(s);
                break;
            }
        }
    }
    %>
    <a name="errors"></a>
    <p>Summary of error types:
    <p>
    <%
    // print out summary first so we can see everything in one place. hyperlink each error type to anchors below with the details
    for (int t = 0; t < errorTypes.length; t++) {
        if (errorsByType[t].size() == 0)
            continue;
        out.println ("<a href=\"#errorType" + t + "\">" + Util.commatize(errorsByType[t].size()) + " " + errorTypes[t][1] + "<br/>\n");
    }

    // print out details
    for (int t = 0; t < errorTypes.length; t++) {
        if (errorsByType[t].size() == 0)
            continue;
        int i = 0;
        out.println ("<a id=\"errorType" + t + "\">"); // anchor
        out.println ("<h2>" + Util.commatize(errorsByType[t].size()) + " " + errorTypes[t][1] + "</h2>"); // errorTypes[t][1] is the description of the error
        out.println ("</a>");
        for (String s : errorsByType[t])
            out.println(++i + ". " + Util.escapeHTML(s) + "<br/>\n");
        out.println ("<br/><br/><hr/><br/>\n");
    }
%>

</div>
<jsp:include page="footer.jsp"/>
</body>
</html>
