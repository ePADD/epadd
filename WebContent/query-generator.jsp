<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
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
<%--
	<title>Query Generator Results</title>
--%>
    <title>Multi-Entity Search Results</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
<!--    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap-theme.min.css"> -->
    <jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<script src="js/epadd.js"></script>

    <style>
        img { margin: 3px 0 0 10px;}
        h5 { margin: 0px; color: white; }
    </style>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Search');</script>

<%
    //This jsp is being used to handle free text entity extraction+search and one line per term based search. A distincation is made between these
    //two cases by front end by either passing refText parameter or by passing refTextTerms parameter. Based on this, the heading of the page is either
    //query generator result or term search result. This distinction is used by leadsAsJson.jsp to make a minor change in its functioning. If
    //the request is coming for term search result then no entity recognition takes place. Otherwise the entity recognitioni takes place. After that
    //the result is displayed in the same manner by establishing a hyperlink to actual messages containing those terms.
    String req = request.getParameter("refText");
    boolean one_line_per_term_search=false;
    if(req==null){
        req=request.getParameter("refTextTerms");//if querygenerator is being called for one line per term type of search.
        one_line_per_term_search=true;
    }
    String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
    writeProfileBlock(out, archive, one_line_per_term_search ? "Multi-term search results" : "Multi-entity search results");

%>
<div class="appraisal-bulk-search">

    <div class="container">
        <div style="text-align:right">
            <label>
                <%if(one_line_per_term_search){%>
                <span style="background-color: #e7df9a;">Matched terms</span>
                <%}else{%>
                <span style="background-color: #e7df9a;">Matched entities</span>
                <%}%>
            </label>
            <label style="margin-left:10px">
                <%if(one_line_per_term_search){%>
                <span style="border-bottom: 1px red dotted;">Unmatched terms</span>
                <%}else{%>
                <span style="border-bottom: 1px red dotted;">Unmatched Entities</span>
                <%}%>
            </label>
        </div>

        <div class="row">
            <div class="bulksearch-content" style="background-color: white; border: 1px solid #e8ebef;	line-height: 25px; height: auto; padding: 10px;">
                <%

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
	window.MUSE_URL = "<%=request.getContextPath()%>"; // note: getROOTURL() doesn't work right on a public server like epadd.stanford.edu -- it returns localhost:9099/epadd because of port forwarding < % =HTMLUtils.getRootURL(request)%>';
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
