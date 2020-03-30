<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%
    JSPHelper.checkContainer(request); // do this early on so we are set up
    request.setCharacterEncoding("UTF-8");
%>
<%@page import="edu.stanford.muse.index.*"%>
<%@page import="edu.stanford.muse.util.DetailedFacetItem"%>
<%@page import="edu.stanford.muse.util.EmailUtils"%>
<%@page import="edu.stanford.muse.util.Pair"%>
<%@page import="edu.stanford.muse.webapp.EmailRenderer"%>
<%@page import="edu.stanford.muse.webapp.HTMLUtils"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig"%>

<%@ page import="java.util.*" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="edu.stanford.muse.LabelManager.Label" %>
<%@ page import="edu.stanford.muse.LabelManager.LabelManager" %>
<%@ page import="org.json.JSONArray" %>

<%@include file="getArchive.jspf"%>

<%
    //Get the title of the page from the request parameters
    Pair<String,String> titles = JSPHelper.getTitles(request);
    String pageTitle = titles.first;
    String facetColTitle = titles.second;
    //Create a random docsetID to store the resultset in session object so that it can be used later for downloading or labeling the messages.
    String docsetID = String.format("docset-%08x", EmailUtils.rng.nextInt());// "dataset-1";
    //collect search result
    Pair<Collection<Document>, SearchResult> result = JSPHelper.getSearchResultDocs(request,archive);
    Collection<Document> docs = result.first;
    SearchResult outputSet = result.second;

    //get Original query string
    String origQueryString = JSPHelper.getOrigQueryString(request);
    //get sorting options
    String sortBy = request.getParameter("sortBy");
    //check if the parameter list corresponds to message browsing or attachmentbrowsing
    //if this parameter is absent then isAttachmentBrowsing variable will be set to false that corresponds to message browsing screen.
    boolean isAttachmentBrowsing = "attachments".equalsIgnoreCase(request.getParameter("browseType"));
    boolean isMessageBrowsing = !isAttachmentBrowsing;
    Map<String, Collection<DetailedFacetItem>> facets=null;
    if(isMessageBrowsing)
        //get Facet Information for Message browsing screen.
        facets = JSPHelper.getFacetItemsForMessageBrowsing(request,docs,archive);
    else
        //get Facet Information for Attachment browsing screen.
        facets = JSPHelper.getFacetItemsForAttachmentBrowsing(request,docs,archive);

%>
<!DOCTYPE HTML>
<html lang="en">
<head>
    <META http-equiv="Content-Type" content="text/html; charset=UTF-8">

    <title><%=pageTitle%></title>

    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="css/jquery.qtip.min.css">
    <jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/main.css">

    <script src="js/stacktrace.js" type="text/javascript"></script>
    <script src="js/jquery.js" type="text/javascript"></script>

    <script type='text/javascript' src='js/jquery.qtip.min.js'></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/selectpicker.js"></script>
    <script src="js/modernizr.min.js"></script>
    <script src="js/sidebar.js"></script>

    <script src="js/muse.js" type="text/javascript"></script>
    <script src="js/epadd.js"></script>
    <script type='text/javascript' src='js/utils.js'></script>     <!-- For tool-tips -->

    <style>
        div.facets hr { width: 90%; }
        .navbar { margin-bottom: 0; } /* overriding bootstrap */
        .dropdown-header { font-weight: 600;color: black; font-size: 15px;}
         a.opt { color: black;  padding-left: 1.25em; }
        ul.dropdown-menu.inner li, .multi-select .dropdown-menu ul li { border-bottom: none; }
        svg { fill: blue; color: red; stroke: green; }
    </style>


</head>

<body > <!--  override margin because this page is framed. -->

<%@include file="header.jspf"%>
<%if(isMessageBrowsing){%>
    <%@include file="browseMessages.jspf"%>
<%}else{%>
    <%@include file="browseAttachments.jspf"%>
<%}%>

<div style="clear:both"></div>
<jsp:include page="footer.jsp"/>
</body>
</html>
