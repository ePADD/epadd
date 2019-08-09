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
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.email.GmailStore" %>
<%@ page import="edu.stanford.muse.email.FolderInfo" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.email.GmailStore" %>

<%@include file="gmail-auth-check.jspf" %>

<%

    GmailStore gmailStore = new GmailStore(authenticatedUserInfo);
    //Show folders and number of messages in these folders.
    List<FolderInfo> folderInfos = gmailStore.getFoldersAndCount("me");
    // Allow user to select these folders.
    //Allow user to select start date and end date.

    //When user clicks on import and index then invoke ajax call in the backend with progress bar
    //when done redirect to browse-top page.



%>
<!DOCTYPE HTML>
<html lang="en">
<head>
    <META http-equiv="Content-Type" content="text/html; charset=UTF-8">


    <link rel="icon" type="image/png" href="../images/epadd-favicon.png">

    <link rel="stylesheet" href="../bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="../css/jquery.qtip.min.css">
    <%--<jsp:include page=".."/>--%>
    <link rel="stylesheet" href="../css/sidebar.css">
    <link rel="stylesheet" href="../css/main.css">
    <link href='https://fonts.googleapis.com/css?family=Open+Sans:400,600,400italic' rel='stylesheet' type='text/css'>
    <link rel="stylesheet" href="font-awesome-4.3.0/css/font-awesome.min.css">
    <link href='https://fonts.googleapis.com/css?family=Open+Sans' rel='stylesheet' type='text/css'>
    <link href='https://fonts.googleapis.com/css?family=Open+Sans:Semibold' rel='stylesheet' type='text/css'>

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap-theme.min.css">
    <link rel="stylesheet" type="text/css" href="css/pm-min.css"/>

    <script type="text/javascript" src="//ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js"></script>
    <script type="text/javascript" src="//maxcdn.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script>

    <script src="js/lib/jquery.backstretch.min.js"></script>

    <script src="https://apis.google.com/js/api:client.js"></script>
    <script src="https://apis.google.com/js/client:platform.js" async defer></script>

    <script src="../js/stacktrace.js" type="text/javascript"></script>
    <script src="../js/jquery.js" type="text/javascript"></script>

    <script type='text/javascript' src='../js/jquery.qtip.min.js'></script>
    <script type="text/javascript" src="../bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="../js/selectpicker.js"></script>
    <script src="../js/modernizr.min.js"></script>
    <script src="../js/sidebar.js"></script>

    <script src="../js/muse.js" type="text/javascript"></script>
    <script src="../js/epadd.js"></script>
    <%--<script src="../js/browse.js" type="text/javascript"></script>--%>
    <script type='text/javascript' src='../js/utils.js'></script>     <!-- For tool-tips -->

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
<% for(FolderInfo folderInfo: folderInfos){%>
 <p><%=folderInfo.longName%>, <%=folderInfo.messageCount%></p>
<%}%>
 <br/>
<br/>
<button id="gmail-index" onclick="gmail_fetch_index()">Fetch and Index</button>
<script>"use strict";

    function gmail_fetch_index(){
        //make ajax call of fetching URL
        //data parameters: folders - a comma separated string of folders to fetch data from, startDate/endDate: date in format yyyy/mm/dd.

        try {
            //%5BGmail%5D%2FSent%20Mail
            var post_params = /*getSelectedFolderParams() +*/ '&folder=chinu.pandey@gmail.com^-^%5BGmail%5D%2FSent%20Mail&dateRange=20190131-20190730&sentOnly=true';
            // need to check muse.mode here for page to redirect to actually!
            var page = "ajax/gmail-auth/gmailFetchAndIndex.jsp";
            fetch_page_with_progress(page, "status", document.getElementById('status'), document.getElementById('status_text'), post_params);
        } catch(err) { }


    }
</script>
<div style="clear:both"></div>
<%--
<jsp:include page="../footer.jsp"/>
--%>
</body>
</html>
