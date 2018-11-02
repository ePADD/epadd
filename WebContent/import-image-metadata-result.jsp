<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@ page import="java.io.FilenameFilter" %>
<%@ page import="java.io.File" %>
<%@ page import="org.apache.commons.imaging.common.ImageMetadata" %>
<%@ page import="org.apache.commons.imaging.common.IImageMetadata" %>
<%@ page import="org.apache.commons.imaging.formats.jpeg.JpegImageMetadata" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.datacache.Blob" %>
<%@ page import="edu.stanford.muse.datacache.BlobStore" %>
<%@ page import="java.util.*" %>
<%@ page import="com.google.common.collect.LinkedHashMultimap" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@include file="getArchive.jspf" %>

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

<script>epadd.nav_mark_active('TODO');</script>

<br/>

<%!
    public static class ImageFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name)
        {
            return (Util.is_image_filename(name));
        }
    }

%>
<%
    writeProfileBlock(out, archive,  "Import image metadata");
    %> <br/><br/><%
    BlobStore blobStore = archive.blobStore;
    Map<String, Blob> fullNameToBlob = new LinkedHashMap<>();
    Multimap blobToKeywords = blobStore.getBlobToKeywords();
    if (blobToKeywords == null) {
        blobToKeywords = LinkedHashMultimap.create();
        blobStore.setBlobToKeywords (blobToKeywords);
    }
    for (Document d: archive.getAllDocs()) {
        EmailDocument ed = (EmailDocument) d;
        List<Blob> blobs = ed.attachments;
        if (Util.nullOrEmpty(blobs))
            continue;

        for (Blob blob : blobs) {
            String blobName = archive.getBlobStore().get_URL_Normalized(blob);; // blobStore.full_filename(blob);
            fullNameToBlob.put (blobName.toLowerCase(), blob);
        }
    }

    String dir = request.getParameter ("dir");
    File imageFiles[] = new File(dir).listFiles(new ImageFilenameFilter());

    int nKeywordsExtracted = 0;
    for (File imageFile: imageFiles) {
        JSPHelper.log.info ("imagefile = " + imageFile);
        Blob b = fullNameToBlob.get (imageFile.getName().toLowerCase());
        if (b == null)
            continue;


        IImageMetadata metadata = org.apache.commons.imaging.Imaging.getMetadata(imageFile);
        if (metadata instanceof JpegImageMetadata) {
            JpegImageMetadata jpeg = (JpegImageMetadata) metadata;
            List<ImageMetadata.IImageMetadataItem> items = jpeg.getItems();
            for (ImageMetadata.IImageMetadataItem item: items) {
                // Keywords: "Jeb Bush"
                String s = item.toString();
                if (s.startsWith ("Keywords: ")) {
                    String keywordStr = s.substring ("Keywords: ".length());
                    JSPHelper.log.info ("keyword : " + keywordStr);
                    nKeywordsExtracted++;
                    String[] keywords = keywordStr.split (",");
                    for (String keyword: keywords) {
                        blobToKeywords.put(b, keyword);
                        out.println ("Importing metadata for " + b + " keyword: " + keyword + "<br/>");
                    }
                }
            }
        }
    }
    //blobStore.pack();
%>
<%=nKeywordsExtracted%> images with keywords extracted.
</body>
</html>
