<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.epadd.Config"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.Sessions"%>
<%@page language="java" import="edu.stanford.muse.webapp.SimpleSessions"%>
<%@page language="java" import="java.io.File"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@page language="java" %>

<html>
<head>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <script src="js/jquery.js"></script>

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <jsp:include page="css/css.jsp"/>
    <title>Collection Details</title>
    <script src="js/muse.js" type="text/javascript"></script>
    <script src="js/epadd.js" type="text/javascript"></script>
    <style> body { background-color: white; } </style>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Collections');</script>

<!-- need status window on this page because archive might take some time to load -->
<script type="text/javascript" src="js/statusUpdate.js"></script>
<%@include file="div_status.jspf"%>

<%
    /* note: this page doesn't require an archive to be loaded. it should not have a profile block. */

    String topDir = "";
    String id = request.getParameter("id");
    if (ModeConfig.isProcessingMode())
        topDir = Config.REPO_DIR_PROCESSING;
    else if (ModeConfig.isDeliveryMode())
        topDir = Config.REPO_DIR_DELIVERY;
    else if (ModeConfig.isDiscoveryMode())
        topDir = Config.REPO_DIR_DISCOVERY;
    else {
        topDir = Config.REPO_DIR_APPRAISAL;
        id = "user";
    }
    //JSPHelper.log.info("Top dir: "+topDir);
    String archiveDir = topDir + File.separator + id;
    //JSPHelper.log.info("Archive dir: "+archiveDir);

    File f = new File(archiveDir);
    if (!f.isDirectory()) {
        %>
        <jsp:include page="footer.jsp"/>
        <%
        return;
    }

    String archiveFile = f.getAbsolutePath() + File.separator + Archive.SESSIONS_SUBDIR + File.separator + "default" + Sessions.SESSION_SUFFIX;

    if (!new File(archiveFile).exists()) {
        %>
        <jsp:include page="footer.jsp"/>
        <%
        return;
    }

    Archive.ProcessingMetadata pm = SimpleSessions.readProcessingMetadata(f.getAbsolutePath() + File.separator + Archive.SESSIONS_SUBDIR, "default");
    String fileParam = f.getName() + "/" + Archive.IMAGES_SUBDIR + "/" + "bannerImage.png"; // always forward slashes please
    String url = "serveImage.jsp?mode=" + ModeConfig.mode + "&file=" + fileParam;
    String ownerName = Util.nullOrEmpty(pm.ownerName) ? "(unassigned name)" : pm.ownerName;
%>
<%!
    private static String formatMetadataField(String s)
    {
        if (s == null)
            return "Unassigned";
        else
            return Util.escapeHTML(s);
    }
%>

<div class="collection-detail">
    <div class="heading"> <%=ownerName%>, Email Series</div>
    <p>
    <div class="banner-img" style="background-image:url('<%=url%>')">
    </div>

    <p>
    <div class="details">
        <div class="heading">Collection Details</div>
        <p></p>
        <p>
        Institution<br/>
        <span class="detail"><%=(Util.nullOrEmpty(pm.institution) ? "Unassigned" : pm.institution)%> </span>
        </p>
        <p>
        Repository<br/>
        <span class="detail"><%=(Util.nullOrEmpty(pm.repository) ? "Unassigned" : pm.repository)%> </span>
        </p>
        <p>
        Collection ID<br/>
        <span class="detail"><%=(Util.nullOrEmpty(pm.collectionID) ? "Unassigned" : pm.collectionID)%> </span>
        </p>
    	<p>
            Accession ID<br/>
            <span class="detail"><%=(Util.nullOrEmpty(pm.accessionID) ? "Unassigned" : pm.accessionID)%> </span>
        </p>
        <p>
            Messages<br/>
            <span class="detail"><%=Util.commatize(pm.nDocs)%></span>
        </p><p>
            Attachments<br/>
            <span class="detail"><%=Util.commatize(pm.nBlobs)%></span>
        </p>

        <% if (!Util.nullOrEmpty(pm.contactEmail)) { /* show contact email, but only if it actually present */ %>
            <p>
            Contact email<br/>
            <span class="detail"><%=(Util.nullOrEmpty(pm.contactEmail) ? "Unassigned" : pm.contactEmail)%> </span>
        </p><p>
        <% }

        if (ModeConfig.isProcessingMode()) { %>
            <button class="btn-default" id="edit-metadata"><i class="fa fa-pencil"></i> Edit</button>
        <% } %>
    </div>

    <div class="related-links">
        <div class="banner-img-text-block">
            <div class="banner-img-text-large">
                About <%=ownerName%>
            </div>
            <br/>
            <%=Util.nullOrEmpty(pm.about) ? "No text assigned" : Util.escapeHTML(pm.about)%>

            <div style="text-align:left;margin-top: 40px"> <button id="enter" class="btn btn-cta" style="text-align:left">Enter <i class="icon-arrowbutton"></i> </button></div>

            <div id="stats"></div>

        </div>
        <p></p>
        <%
            // not handled: if findingAidLink or catalogRecordLink have a double-quote embedded in them!
            if (!Util.nullOrEmpty(pm.findingAidLink)) { %>
            <div class="heading"><a target="_blank" href="<%=pm.findingAidLink%>">Finding Aid</a></div>
        <% } %>
        <br/>
        <% if (!Util.nullOrEmpty(pm.catalogRecordLink)) { %>
            <div class="heading"><a target="_blank" href="<%=pm.catalogRecordLink%>">Catalog Record</a></div>
            <br/>
        <% } %>

        <% if (ModeConfig.isDiscoveryMode()) { %>
            Email messages in the ePADD Discovery Module have been redacted to ensure the privacy of donors and other correspondents.
            Please contact the host repository if you would like to request access to full messages, including any attachments.
        <% } %>

    </div>
    <br/>
</div>
<br/>
<br/>
    <jsp:include page="footer.jsp"/>

    <script>
        var params = {dir: '<%=id%>'};
        $('#edit-metadata').click (function() { fetch_page_with_progress ('ajax/loadArchive.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), params , null, 'edit-accession?id=<%=id%>'); /* load_archive_and_call(function() { window.location = "edit-accession"} */});
        $('#enter').click(function() { fetch_page_with_progress ('ajax/loadArchive.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), params, null, 'browse-top'); /* load_archive_and_call(function() { window.location = "browse-top"} */});

    </script>

</body>
</html>
