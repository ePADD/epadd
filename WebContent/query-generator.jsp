<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@include file="getArchive.jspf" %>
<%
if (ModeConfig.isPublicMode()) {
	// this browse page is also used by Public mode where the following set up may be requried. 
	String archiveId = request.getParameter("aId");
	Sessions.loadSharedArchiveAndPrepareSession(session, archiveId);
	// TODO: should also pass "aId" downstream to leadsAsJson.jsp also. but it still relies on emailDocs and maybe other session attributes, whose dependence should also be eliminated in public mode for being RESTful.
}
%>
<!DOCTYPE html>
<html>
<head>
	<title>Query Generator Results</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>
		
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap-theme.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>
    <link rel="stylesheet" href="css/main.css">
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
                    out.println (Util.escapeHTML(req).replace("\r", "").replace("\n", "<br/>\n"));
                %>
            </div>
        </div>
    </div>
    <div id="myModal" class="modal fade popup-epadd" role="dialog">
        <div class="modal-dialog">

            <!-- Modal content-->
            <div class="modal-content">
                <div class="header-popup">
                    <h2 class="text-left" id="search-term">Steve Wozniak</h2>
                    <div>
                        <a data-dismiss="modal">
                            <img src="images/close.png" alt="close">
                        </a>
                    </div>
                </div>
                <div class="modal-body">
                    <div class="mail-subject">
                        <div>Jan 22,2002.</div>
                        <div>From: <span>raj@gmail.com,</span> To: <span>mandeep@gmail.com</span></div>
                        <div>Subject: <span>Duncan reading group</span></div>
                    </div>
                </div>
                <div class="search-clear-btns">
                    <!--search btn-->
                    <button id="submit-button" type="submit" class="search-btn">
                        <h5>VIEW ALL MESSAGES</h5>
<!--                        <h5>VIEW <span id="nHits">0</span> MESSAGES</h5> -->
                    </button>
                </div>

            </div>

        </div>
    </div>

</div>
</div>

<!-- Pop-up section -->

<script type="text/javascript">
	window.MUSE_URL = "<%=request.getContextPath()%>" // note: getROOTURL() doesn't work right on a public server like epadd.stanford.edu -- it returns localhost:9099/epadd because of port forwarding < % =HTMLUtils.getRootURL(request)%>';
    if(window.MUSE_URL==null)
        window.MUSE_URL="";
    var handle_submit = function() {
        window.open('browse?term="' + $('#search-term').html() + '"');
    };

    $('#submit-button').click(handle_submit);
</script>
<script type="text/javascript" src="js/muse-lens.user.js"></script>
<jsp:include page="footer.jsp"/>
</body>
</html>
