<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@include file="getArchive.jspf" %>
<%
/*
if (ModeConfig.isPublicMode()) {
	// this browse page is also used by Public mode where the following set up may be requried. 
	String archiveId = request.getParameter("aId");
	Sessions.loadSharedArchiveAndPrepareSession(session, archiveId);
	// TODO: should also pass "aId" downstream to leadsAsJson.jsp also. but it still relies on emailDocs and maybe other session attributes, whose dependence should also be eliminated in public mode for being RESTful.
}
*/
%>
<!DOCTYPE html>
<html>
<head>
	<title>Query Generator Results</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
<!--    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap-theme.min.css"> -->
    <jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<script src="js/epadd.js"></script>

    <style>
        .search-clear-btns { text-align: center; padding: 35px 0; }
        .search-btn {
            background: #0175bc;
            border: none;
            font-size: 14px;
            font-weight: 700;
            text-transform: uppercase;
            color: #fff;
            padding: 15px 25px;
            border-radius: 4px;
            display: inline-flex;
            justify-content: center;
            margin-right: 25px;
            box-shadow: 0px 2px 7px 1px rgba(153, 177, 200, 0.38);
        }
        img { margin: 3px 0 0 10px;}
        h5 { margin: 0px; color: white; }
    </style>
</head>
<body style="background-color:#f5f5f8; color: #333">
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Search');</script>

<div class="appraisal-bulk-search">

    <div class="container">
        <div class="row">
            <div class="col-md-9 col-md-offset-1 epadd-flex">
                <div class="col-md-6">
                    <h1>Query generator results</h1>
                </div>
                <div class="col-md-6 text-right">
                    <label>
                        <span style="background-color: #e7df9a;">Matched entities</span>
                    </label>
                    <label style="margin-left:10px">
                        <span style="border-bottom: 1px red dotted;">Unmatched Entities</span>
                    </label>
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-md-9 col-md-offset-1 bulksearch-content" style="background-color: white; border: 1px solid #e8ebef;	padding: 35px; line-height: 25px; height: auto">
                <%
                    String req = request.getParameter("refText");
                    String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
                    out.println (Util.escapeHTML(req).replace("\r", "").replace("\n", "<br/>\n"));
                %>
            </div>
        </div>
    </div>

    <div id="myModal" class="modal fade" style="z-index:9999">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 style="font-size: 125%" class="modal-title">Messages matching <span id="search-term"></span></h4>
                </div>
                <div class="modal-body">
                </div>
                <div class="modal-footer">
                    <button id='submit-button' type="button" class="btn btn-default" data-dismiss="modal">View all <span id="nHits"></span> message(s)</button>
                </div>
            </div><!-- /.modal-content -->
        </div><!-- /.modal-dialog -->
    </div><!-- /.modal -->
</div>
</div>

<!-- Pop-up section -->

<script type="text/javascript">
    var archiveID="<%=archiveID%>";
	window.MUSE_URL = "<%=request.getContextPath()%>" // note: getROOTURL() doesn't work right on a public server like epadd.stanford.edu -- it returns localhost:9099/epadd because of port forwarding < % =HTMLUtils.getRootURL(request)%>';
    if(window.MUSE_URL==null)
        window.MUSE_URL="";
    var handle_submit = function() {
        window.open('browse?archiveID=<%=archiveID%>&adv-search=1&termBody=on&termSubject=on&termAttachments=on&termOriginalBody=on&term="' + $('#search-term').html() + '"');
    };

    $('#submit-button').click(handle_submit);
</script>
<script type="text/javascript" src="js/muse-lens.user.js"></script>
<jsp:include page="footer.jsp"/>
</body>
</html>
