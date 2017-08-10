<%@ page contentType="text/html; charset=UTF-8"%>
<%
	JSPHelper.checkContainer(request); // do this early on so we are set up
	request.setCharacterEncoding("UTF-8");
%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.datacache.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="java.lang.*"%>
<%@page language="java" import="java.net.*"%>
<%@ page import="com.google.common.collect.Multimap" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html lang="en">
<title>Image Attachments</title>
<head>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">


    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link href="css/selectpicker.css" rel="stylesheet" type="text/css" media="screen" />
    <jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="js/jquery-ui/jquery-ui.css">
    <link rel="stylesheet" href="js/jquery-ui/jquery-ui.theme.css">
    <script src="js/jquery.js"></script>
    <script src="js/jquery-ui/jquery-ui.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/modernizr.min.js"></script>
    <script src="js/selectpicker.js"></script>
	<script src="js/epadd.js"></script>
	<script type="text/javascript" src="js/muse.js"></script>
    <style>
        .date-input-group {  display: flex; }
        .date-input-group .form-control { width:45%;}
        .date-input-group label {
            padding: 5px 18px;
            font-size: 14px;
        }

        .filter-button { margin-top:38px; margin-right:17px; position:absolute; right: 0px;}
        .btn-default { height: 37px; }
        label {  font-size: 14px; padding-bottom: 13px; font-weight: 400; color: #404040; } /* taken from form-group label in adv.search.scss */
        .one-line::after {  content:"";  display:block;  clear:both; }  /* clearfix needed, to take care of floats: http://stackoverflow.com/questions/211383/what-methods-of-clearfix-can-i-use */
        .form-group { margin-bottom: 25px;}

    </style>
</head>  
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
    // convert req. params to a multimap, so that the rest of the code doesn't have to deal with httprequest directly
    Multimap<String, String> params = JSPHelper.convertRequestToMap(request);
    SearchResult inputSet = new SearchResult(archive,params);
    SearchResult resultSet = SearchResult.selectBlobs(inputSet);
    Set<Document> docset = resultSet.getDocumentSet();
    Set<Blob> allAttachments = new LinkedHashSet<>();

    for (Document doc: docset){
        EmailDocument edoc = (EmailDocument)doc;
        allAttachments.addAll(resultSet.getAttachmentHighlightInformation(edoc));
    }

    int nEntriesForPiclens = 0;
    String piclensRSSFilename = "";

	if (!Util.nullOrEmpty(allAttachments)) {
        String userKey = "user";
        String rootDir = JSPHelper.getRootDir(request);
        String cacheDir = (String) JSPHelper.getSessionAttribute(session, "cacheDir");
        JSPHelper.log.info("Will read attachments from blobs subdirectory off cache dir " + cacheDir);

        Set<DatedDocument> docs = new HashSet<>((Collection) archive.getAllDocs());

        String extra_mesg = "";

        // attachmentsForDocs
        String attachmentsStoreDir = cacheDir + File.separator + "blobs" + File.separator;
        BlobStore store = null;
        try {
            store = new BlobStore(attachmentsStoreDir);
        } catch (IOException ioe) {
            JSPHelper.log.error("Unable to initialize attachments store in directory: " + attachmentsStoreDir + " :" + ioe);
            Util.print_exception(ioe, JSPHelper.log);
        }


        // create a dataset object to view
        int i = new Random().nextInt();
        String randomPrefix = String.format("%08x", i);
        JSPHelper.log.info("Root dir for blobset top level page is " + rootDir);
        System.err.println("Root dir for blobset top level page is " + rootDir);
        BlobSet bs = new BlobSet(rootDir, new ArrayList<Blob>(allAttachments), store);

        String appURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        nEntriesForPiclens = bs.generate_top_level_page(randomPrefix, appURL, extra_mesg);
        // attachmentsForDocs

        piclensRSSFilename = userKey + "/" + randomPrefix + ".photos.rss";
        String faviconPlusCSS = "<link rel=\"icon\" type=\"image/png\" href=\"images/muse-favicon.png\">\n<link href=\"css/muse.css\" rel=\"stylesheet\" type=\"text/css\"/>";

        String url = request.getRequestURI();
    }
%>
<%writeProfileBlock(out, archive, "", Util.pluralize(nEntriesForPiclens, "unique attachment"));%>
<br/>
<br/>

<div id="all_fields" style="margin:auto; width:900px; padding: 10px">

    <!-- filter form submits back to the same page -->
    <form action="image-attachments" method="get">
        <section>
            <div class="panel">
                <div class="panel-heading">Filter images</div>

                <div class="one-line">
                    <div class="form-group col-sm-6">
                        <label for="attachmentExtension">Extension</label>
                        <select name="attachmentExtension" id="attachmentExtension" class="form-control multi-select selectpicker" title="Select" multiple>
                            <option value="" selected disabled>Select</option>
                            <option value="jpg">JPG</option>
                            <option value="png">PNG</option>
                            <option value="gif">GIF</option>
                            <option value="bmp">BMP</option>
                        </select>
                    </div>

                    <!--File Size-->
                    <div class="form-group col-sm-6">
                        <label for="attachmentFilesize">File Size</label>
                        <select id="attachmentFilesize" name="attachmentFilesize" class="form-control selectpicker">
                            <option value="" selected disabled>Choose File Size</option>
                            <option value="1">&lt; 5KB</option>
                            <option value="2">5-20KB</option>
                            <option value="3">20-100KB</option>
                            <option value="4">100KB-2MB</option>
                            <option value="5">&gt; 2MB</option>
                            <option value="6">Any</option>
                        </select>
                    </div>
                </div>

                <div class="one-line">
                    <!--Time Range-->
                    <div class="form-group col-sm-6">
                        <label for="time-range">Time Range</label>
                        <div id="time-range" class="date-input-group">
                            <input type = "text" value="<%=request.getParameter ("startDate")==null?"": request.getParameter("startDate")%>" id="startDate" name="startDate" class="form-control" placeholder="YYYY - MM - DD">
                            <label for="endDate">To</label>
                            <input type = "text" value="<%=request.getParameter ("endDate")==null?"": request.getParameter("endDate")%>" id="endDate" name="endDate"  class="form-control" placeholder="YYYY - MM - DD">
                        </div>
                    </div>

                    <div class="form-group col-sm-6">
                        <button type="submit" class="btn-default filter-button">Filter</button>
                    </div>
                </div>
            </div>
        </section>
    </form>
    <!-- end filter form -->

    <!-- initialize filter fields per request params -->
    <% {
        String vals[] = request.getParameterValues ("attachmentFilesize");
        if (vals != null) {
            String jsValString = "[";
            for (String v: vals) { jsValString += "'" + v + "',"; }
            if (vals.length > 0) {
                jsValString = jsValString.substring(0, jsValString.length() - 1);
                jsValString += "]";
            }
    %>
        <script>
            // need a little delay for this to take effect
            $(document).ready (function() { setTimeout (function() { $('#attachmentFilesize').selectpicker ('val', <%=jsValString%>); }, 1000); } );
        </script>
    <% }
    }
    %>

    <% {
        String vals[] = request.getParameterValues ("attachmentExtension");
        if (vals != null) {
            String jsValString = "[";
            for (String v: vals) { jsValString += "'" + v + "',"; }
            if (vals.length > 0) {
                jsValString = jsValString.substring(0, jsValString.length() - 1);
                jsValString += "]";
            }
    %>
    <script>
        // need a little delay for this to take effect
        $(document).ready (function() { setTimeout (function() { $('#attachmentExtension').selectpicker ('val', <%=jsValString%>); }, 1000); } );
    </script>
    <% }
    }
    %>

</div>

<div style="text-align: center">
<% if (nEntriesForPiclens > 0) { %>
	<object id="o" classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000" width="1200" height="720">
		<param name="movie" value="cooliris/cooliris.swf" />
		<param name="flashvars" value="feed=<%= piclensRSSFilename %>" />
		<param name="allowFullScreen" value="true" />
		<param name="allowScriptAccess" value="n" />
	<embed type="application/x-shockwave-flash" src="cooliris/cooliris.swf" width="1200" height="720" flashvars="feed=<%= piclensRSSFilename %>" allowFullScreen="true" allowfullscreen="true" allowScriptAccess="never" allowscriptaccess="never"></embed>
	</object>
	<br/>
</div>
<% } %>
<jsp:include page="footer.jsp"/>
<script>

    $(document).ready(function() {
        //installing input tags as datepickers
        //setting min date as 1 jan 1960. The format is year,month,date. Month is 0 based and all other are 1 based
        $('#startDate').datepicker({
            minDate: new Date(1960, 1 - 1, 1),
            dateFormat: "yy-mm-dd"
        });
        $('#endDate').datepicker({
            minDate: new Date(1960, 1 - 1, 1),
            dateFormat: "yy-mm-dd"
        });

    });
</script>

</body>
</html>
