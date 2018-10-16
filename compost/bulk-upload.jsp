<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<%@include file="../WebContent/getArchive.jspf" %>
<html>
<head>
    <title>Bulk Upload</title>
    <link rel="icon" type="image/png" href="../WebContent/images/epadd-favicon.png">

    <link rel="stylesheet" href="../WebContent/bootstrap/dist/css/bootstrap.min.css">
    <link href="../WebContent/jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
    <jsp:include page="../WebContent/css/css.jsp"/>

    <script src="../WebContent/js/jquery.js"></script>
    <script src="../WebContent/jqueryFileTree/jqueryFileTree.js"></script>
    <script type="text/javascript" src="../WebContent/bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="../WebContent/js/filepicker.js" type="text/javascript"></script>

    <script src="../WebContent/js/epadd.js"></script>

    <style type="text/css">
        .go-button, .go-button:hover { background-color: #0075bb; color: #fff; }  /* saumya wants this always filled in, not just on hover */

        .btn-default { height: 37px; }
        label {  font-size: 14px; padding-bottom: 13px; font-weight: 400; color: #404040; } /* taken from form-group label in adv.search.scss */
        .faded { opacity: 0.5; }
        .one-line::after {  content:"";  display:block;  clear:both; }  /* clearfix needed, to take care of floats: http://stackoverflow.com/questions/211383/what-methods-of-clearfix-can-i-use */
        .picker-buttons { margin-top:40px; margin-left:-30px; } /* so that the browse button appears at the right edge of the input box */
        .form-group { margin-bottom: 25px;}
    </style>
</head>
<body>
    <jsp:include page="../WebContent/header.jspf"/>
    <jsp:include page="../WebContent/div_filepicker.jspf"/>

<% writeProfileBlock(out, false, archive, "Upload correspondent list", 900);
String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
%>

<br/>
<br/>

    <div id="all_fields" style="width:900px; margin:auto">
        <div class="panel">
            <div class="panel-heading">List of correspondents</div>
            <form id="uploadCSVform" method="POST" action="upload-correspondents-for-search" enctype="multipart/form-data" >
                <input type="hidden" value="<%=archiveID%>" name="archiveID"/>
                <input type="file" id="correspondentCSV" name="correspondentCSV" value=""/>

            <%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

        </div>
            <button id="upload-btn" class="btn btn-cta">Upload <i class="icon-arrowbutton"></i></button>

        </form>

    </div>
</body>
</html>
