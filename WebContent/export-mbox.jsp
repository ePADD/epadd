<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="edu.stanford.muse.datacache.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@ page import="com.google.common.collect.Multimap"%>
<%@include file="getArchive.jspf" %>

<script src="js/jquery.js"></script>

<%
    Multimap<String,String> paramMap = JSPHelper.convertRequestToMap(request);
    String docsetID = request.getParameter("docsetID");
    String noattach = request.getParameter("noattach");
    String stripQuoted = request.getParameter("stripQuoted");
    //String headerOnly = request.getParameter("headerOnly");
    //String messageType = request.getParameter("messageType");
%>

<script>
    var docsetID = '<%=docsetID%>';
    var noattach = 'on';
    var stripQuoted = 'off';
    var headerOnly = 'off';
    var messageType = 'all';
</script>

<script src="js/downloadMessage.js" type="text/javascript"></script>


<!DOCTYPE HTML>
<html>
<head>
    <title>Export</title>

    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <jsp:include page="css/css.jsp"/>
    <script src="js/muse.js"></script>
    <script src="js/epadd.js?v=1.1"></script>
</head>
<body>
<%@include file="header.jspf"%>
<script>epadd.nav_mark_active('Export');</script>
<%
	writeProfileBlock(out, archive,  "Export archive");
%>
<div style="margin-left:170px">
    <div id="spinner-div" style="display:none; text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>

    <br/>
    <% if (ModeConfig.isProcessingMode() ) { %>
        <a id="a-download-mbox-file-link" href="#">Download mbox file from redacted email store</a>
    <% } else {%>
        <a id="a-download-mbox-file-link" href="#">Download mbox file</a>
    <% } %>
    <p></p>
    This file is in mbox format, and can be accessed with many email clients (e.g. <a href="http://www.mozillamessaging.com/">Thunderbird</a>.)
    It can also be viewed with a text editor.<br/>
    On Mac OS X, Linux, and other flavors of Unix, you can usually open a terminal window and type the command: <br/>
    <i>mail -f &lt;saved file&gt;</i>.
    <p>
        This mbox file may also have extra headers like X-ePADD-Folder, X-ePADD-Labels and X-ePADD-Annotation.
    </p>

    <a id="a-download-search-result-csv-link" href="#">Download search result in csv format</a>
    <p></p>
    This file is in csv format, and contains header information only. It can be viewed in either Excel or a text editor.
    </p>

    <br/>
    <a id="a-download-attachment-zip-link" href="#">Download attachments in a zip file</a>
    <p></p>
    This file is in zip format, and contains all attachments in the selected messages.<br/>
    <br/>

</div>

<div id="invisible-frame" style=\"display:none;margin:0px;\">
</div>

<jsp:include page="footer.jsp"/>
</body>
</html>
