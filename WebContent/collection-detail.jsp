<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.Config"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.SimpleSessions"%>
<%@page language="java" import="java.io.File"%>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>

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

    String archiveFile = f.getAbsolutePath() + File.separator + Archive.SESSIONS_SUBDIR + File.separator + "default" + SimpleSessions.getSessionSuffix();

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

<div class="collection-detail">
    <div class="heading">
        <% if (!Util.nullOrEmpty(pm.collectionTitle)) { %>
            <%=pm.collectionTitle%>
        <% } else { %>
             <%=ownerName%>, Email Series
        <% } %>
    </div>
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
            <% if (!Util.nullOrEmpty(pm.accessionMetadatas)) { %>
                <span><%= Util.pluralize (pm.accessionMetadatas.size(), "accession")%></span>
            <% } %>
        </p>
        <p>
            <% if (pm.firstDate != null && pm.lastDate != null) { %>
                Date Range<br/>
                <span class="detail"><%=Util.formatDate(pm.firstDate)%> to <%=Util.formatDate(pm.lastDate)%></span>
                <% if (pm.nHackyDates > 0) { %>
            <br/>
                        <%=Util.pluralize(pm.nHackyDates, "message")%> undated
                    </p>
                <% } %>
            <% } %>
        </p><p>
        <p>
            Messages: <span class="detail"><%=Util.commatize(pm.nDocs)%></span>
            <% if (pm.nIncomingMessages > 0 || pm.nOutgoingMessages > 0) { %>
                <br/>
                Incoming: <span class="detail"><%=Util.commatize(pm.nIncomingMessages)%></span><br/>
                Outgoing: <span class="detail"><%=Util.commatize(pm.nOutgoingMessages)%></span>
            <% } %>
        </p><p>
            Attachments: <span class="detail"><%=Util.commatize(pm.nBlobs)%></span>
                <% if (pm.nDocBlobs > 0 || pm.nImageBlobs > 0 || pm.nOtherBlobs > 0) { %>
                    <br/>
                    Images: <span class="detail"><%=Util.commatize(pm.nImageBlobs)%></span><br/>
                    Documents: <span class="detail"><%=Util.commatize(pm.nDocBlobs)%></span><br/>
                    Others: <span class="detail"><%=Util.commatize(pm.nOtherBlobs)%></span>
                <% } %>
        </p>

        <% if (!Util.nullOrEmpty(pm.contactEmail)) { /* show contact email, but only if it actually present */ %>
            <p>
            Contact email<br/>
            <span class="detail"><%=(Util.nullOrEmpty(pm.contactEmail) ? "Unassigned" : pm.contactEmail)%> </span>
        </p><p>
        <% }

        if (ModeConfig.isProcessingMode()) { %>
            <button class="btn-default" id="edit-metadata"><i class="fa fa-pencil"></i> Edit</button>
            <p>
            <button class="btn-default" id="add-accession"><i class="fa fa-import"></i> Add accession</button>

        <% } %>
    </div>

    <div class="related-links">
        <div class="banner-img-text-block">

            <div class="banner-img-text-large">
                About <%=ownerName%>
            </div>

            <br/>
            <%=Util.nullOrEmpty(pm.about) ? "No text assigned" : Util.escapeHTML(pm.about)%>

            <br/>
            <br/>

            <div style="font-weight:bold">Rights and Conditions</div>
                <%=Util.nullOrEmpty(pm.rights) ? "No rights and conditions assigned" : Util.escapeHTML(pm.rights)%>

            <% if (!Util.nullOrEmpty(pm.notes)) { %>
                <br><br>
                <div style="font-weight:bold">Notes</div>
                    <%= Util.escapeHTML(pm.notes)%>
                </div>
            <% } %>

        <% if (!Util.nullOrEmpty(pm.accessionMetadatas)) {
            for (Archive.AccessionMetadata am: pm.accessionMetadatas) { %>
            <div class="panel">
                About Accession <%=am.id%>
                <p>
                Title<br/><%=am.title%>
                <p></p>
                Scope and contents: <%=am.scope%>
                Rights and conditions: <%=am.rights%>
                Notes: <%=am.notes%>
            </div>
        <% }
        } %>

        <div style="text-align:left;margin-top: 40px"> <button id="enter" class="btn btn-cta" style="text-align:left">Enter <i class="icon-arrowbutton"></i> </button></div>

        <div id="stats"></div>

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
        var editmetadataparams = {dir: '<%=id%>',editscreen: '1'};
        //the following call will first load archive and then in that jsp the subsequent screeen 'edit-collection.jsp' will be set as the next page
        //edit-screen parameter passed to loadArchive.jsp will help to distinguish whether to set the enxt screen as edit-collection.jsp or browse-top.jsp
        $('#edit-metadata').click (function() { fetch_page_with_progress ('ajax/loadArchive.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), editmetadataparams , null); /* load_archive_and_call(function() { window.location = "edit-accession"} */});
        $('#add-accession').click (function() { window.location = 'add-accession'});
        //result of succesful ajax/loadArchive should be a call to browse-top page with appropriate archiveID. hence
        //set it as a resultPage of the returned json object in ajax/loadArchive.jsp.
        var enterparams = {dir: '<%=id%>'};
        $('#enter').click(function() { fetch_page_with_progress ('ajax/loadArchive.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), enterparams, null); /* load_archive_and_call(function() { window.location = "browse-top"} */});

    </script>

</body>
</html>
