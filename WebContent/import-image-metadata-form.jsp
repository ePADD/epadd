<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" import="java.util.Set"%>

<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.datacache.Blob" %>
<%@ page import="java.util.List" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="edu.stanford.muse.index.*" %>
<!DOCTYPE HTML>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>ePADD image metadata import</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">


    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
    <link href="css/selectpicker.css" rel="stylesheet" type="text/css" media="screen" />
    <jsp:include page="css/css.jsp"/>

    <script src="js/jquery.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="jqueryFileTree/jqueryFileTree.js"></script>
    <script src="js/jquery.autocomplete.js" type="text/javascript"></script>

    <script src="js/filepicker.js"></script>
    <script src="js/selectpicker.js"></script>

    <script src="js/muse.js"></script>
    <script src="js/epadd.js"></script>
    <style>
        .mini-box { height: 105px; vertical-align:top; background-color: #f5f5f8; display: inline-block; width:200px; padding:20px; margin-right:22px;}

        .mini-box .review-messages  {  display: none; margin-top: 10px;}

        .mini-box-icon { color: #f2c22f; display: inline-block; width: 35px; vertical-align:top; font-size: 175%;}
        .mini-box-description .number { font-size: 22px; margin-bottom:5px; }
        .mini-box-description { font-size: 14px;  display: inline-block; width: 100px; vertical-align:top; }

        .mini-box:hover { background-color: #0075bb; color: white; box-shadow: 1px 1px 5px 0px rgba(0,0,0,0.75);}
        .mini-box:hover .mini-box-description, .mini-box:hover .mini-box-icon { display: none; }
        .mini-box:hover .review-messages  {  display: block;  transition: ease-in-out 0.0s;  cursor:pointer;  }
        i.icon-browsetoparrow { font-weight: 600; font-size: 125%;}

        .go-button, .go-button:hover { background-color: #0075bb; color: #fff; }  /* saumya wants this always filled in, not just on hover */

        .btn-default { height: 37px; }
        label {  font-size: 14px; padding-bottom: 13px; font-weight: 400; color: #404040; } /* taken from form-group label in adv.search.scss */
        .faded { opacity: 0.5; }
        .one-line::after {  content:"";  display:block;  clear:both; }  /* clearfix needed, to take care of floats: http://stackoverflow.com/questions/211383/what-methods-of-clearfix-can-i-use */
        .picker-buttons { margin-top:40px; margin-left:-30px; } /* so that the browse button appears at the right edge of the input box */
        .form-group { margin-bottom: 25px;}
        .review-messages { vertical-align:center; text-align:center;}

    </style>
</head>
<body style="background-color:white;">
<%@include file="header.jspf"%>
<jsp:include page="div_filepicker.jspf"/>
<%@include file="getArchive.jspf" %>

<script>epadd.nav_mark_active('TODO');</script>


<%
        writeProfileBlock(out, archive, "Export archive");

    if (!ModeConfig.isProcessingMode() && !ModeConfig.isAppraisalMode()) {
%>
Error: Export is only available in processing or appraisal modes!
<%
        return;
    }

    // convert req. params to a multimap, so that the rest of the code doesn't have to deal with httprequest directly
    Multimap<String, String> params = JSPHelper.convertRequestToMap(request);
    SearchResult inputSet = new SearchResult(archive,params);
    SearchResult resultSet = SearchResult.selectBlobs(inputSet);
    Set<Document> docset = resultSet.getDocumentSet();
    List<Blob> allAttachments = new LinkedList<>();
    List<Blob> allImageAttachments = new LinkedList<>();

    for (Document doc: docset){
        EmailDocument edoc = (EmailDocument)doc;
        //get all attachments of edoc which satisfied the given filter.
        for(Blob b : resultSet.getAttachmentHighlightInformation(edoc)){
            allAttachments.add(b);
            if (archive.getBlobStore().get_URL_Normalized(b)== null)
                continue;
            if (Util.is_image_filename(archive.getBlobStore().get_URL_Normalized(b)))
                allImageAttachments.add (b);
        }
    }

    int nAttachments = allAttachments.size();
    int nImageAttachments = allImageAttachments.size();
%>

<p>

<div id="all_fields" style="margin-left:170px; width:900px; padding: 10px">
    <b>Import image attachment metadata</b>
    <br/>
    <br/>
    <div onclick="window.location='export-review?archiveID=<%=archiveID%>&type=transfer'" class="mini-box">
        <div class="review-messages">
            <i class="icon-browsetoparrow"></i><br/>View
        </div>

        <div class="mini-box-icon"><i class="fa fa-envelope-o"></i></div>
        <div class="mini-box-description">
            <div class="number"><%=Util.commatize(nAttachments)%></div>
            Attachments
        </div>
    </div>

    <div onclick="window.location='export-review?archiveID=<%=archiveID%>&type=annotated'" class="mini-box">
        <div class="review-messages">
            <i class="icon-browsetoparrow"></i><br/>View
        </div>


        <div class="mini-box-icon"><i class="fa fa-comment-o"></i></div>
        <div class="mini-box-description">
            <div class="number"><%=Util.commatize(nImageAttachments)%></div>
            Image attachments
        </div>
    </div>



    <br/>
    <br/>

    <section>
        <div class="panel">

            <div class="one-line" id="import-image-metadata">
                <div class="form-group col-sm-8">
                    <label for="import-image-metadata-dir">Import image metadata</label>
                    <input id="import-image-metadata-dir" class="dir form-control" type="text" name="name" value=""/>
                </div>
                <div class="form-group col-sm-4 picker-buttons">
                    <button id="export-next-browse" class="btn-default browse-button">Browse</button>
                    <button id="export-next-do" style="margin-left: 10px;" class="go-button faded btn-default">Import</button>
                </div>
            </div>

        </div>
    </section>

</div> <!--  all fields -->

<p>

    <script type="text/javascript">
        $(document).ready(function() {
            new FilePicker($('#import-image-metadata'));
        });

        $('#import-image-metadata .go-button').click (function(e) {
            var $button = $(e.target);
            if ($button.hasClass('faded'))
                return false; // do nothing;
            var baseUrl = 'import-image-metadata-result.jsp';
            var dir = $('.dir', $('#import-image-metadata')).val();
            if (dir && dir.length > 0)
                window.location = baseUrl + '?archiveID=<%=archiveID%>&dir=' + dir;
        });
    </script>

</body>
</html>
