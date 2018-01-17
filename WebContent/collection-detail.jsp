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
    String id = request.getParameter("collectionID");
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

    Archive.CollectionMetadata cm = SimpleSessions.readProcessingMetadata(f.getAbsolutePath() + File.separator + Archive.SESSIONS_SUBDIR, "default");
    String fileParam = f.getName() + "/" + Archive.IMAGES_SUBDIR + "/" + "bannerImage.png"; // always forward slashes please
    String url = "serveImage.jsp?mode=" + ModeConfig.mode + "&file=" + fileParam;
    String ownerName = Util.nullOrEmpty(cm.ownerName) ? "(unassigned name)" : cm.ownerName;
%>

<div class="collection-detail">
    <div class="heading">
        <% if (!Util.nullOrEmpty(cm.collectionTitle)) { %>
            <%=cm.collectionTitle%>
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
        <span class="detail"><%=(Util.nullOrEmpty(cm.institution) ? "Unassigned" : cm.institution)%> </span>
        </p>
        <p>
        Repository<br/>
        <span class="detail"><%=(Util.nullOrEmpty(cm.repository) ? "Unassigned" : cm.repository)%> </span>
        </p>
        <p>
        Collection ID<br/>
        <span class="detail"><%=(Util.nullOrEmpty(cm.collectionID) ? "Unassigned" : cm.collectionID)%> </span>
        </p>
    	<p>
            <% if (!Util.nullOrEmpty(cm.accessionMetadatas)) { %>
                <span><%= Util.pluralize (cm.accessionMetadatas.size(), "accession")%></span>
            <% } %>
        </p>
        <p>
            <% if (cm.firstDate != null && cm.lastDate != null) { %>
                Date Range<br/>
                <span class="detail"><%=Util.formatDate(cm.firstDate)%> to <%=Util.formatDate(cm.lastDate)%></span>
                <% if (cm.nHackyDates > 0) { %>
            <br/>
                        <%=Util.pluralize(cm.nHackyDates, "message")%> undated
                    </p>
                <% } %>
            <% } %>
        </p><p>
        <p>
            Messages: <span class="detail"><%=Util.commatize(cm.nDocs)%></span>
            <% if (cm.nIncomingMessages > 0 || cm.nOutgoingMessages > 0) { %>
                <br/>
                Incoming: <span class="detail"><%=Util.commatize(cm.nIncomingMessages)%></span><br/>
                Outgoing: <span class="detail"><%=Util.commatize(cm.nOutgoingMessages)%></span>
            <% } %>
        </p><p>
            Attachments: <span class="detail"><%=Util.commatize(cm.nBlobs)%></span>
                <% if (cm.nDocBlobs > 0 || cm.nImageBlobs > 0 || cm.nOtherBlobs > 0) { %>
                    <br/>
                    Images: <span class="detail"><%=Util.commatize(cm.nImageBlobs)%></span><br/>
                    Documents: <span class="detail"><%=Util.commatize(cm.nDocBlobs)%></span><br/>
                    Others: <span class="detail"><%=Util.commatize(cm.nOtherBlobs)%></span>
                <% } %>
        </p>

        <% if (!Util.nullOrEmpty(cm.contactEmail)) { /* show contact email, but only if it actually present */ %>
            <p>
            Contact email<br/>
            <span class="detail"><%=(Util.nullOrEmpty(cm.contactEmail) ? "Unassigned" : cm.contactEmail)%> </span>
        </p><p>
        <% }

        if (ModeConfig.isProcessingMode()) { %>
            <button class="btn-default" id="edit-metadata"><i class="fa fa-pencil"></i> Edit</button>
            <br/>
            <br/>
            <button class="btn-default" id="add-accession"><i class="fa fa-import"></i> Add accession</button>

        <% } %>
    </div>

    <div class="related-links">
        <div class="banner-img-text-block">

            <div class="banner-img-text-large">
                About <%=ownerName%>
            </div>

            <br/>
            <%=Util.nullOrEmpty(cm.about) ? "No text assigned" : Util.escapeHTML(cm.about)%>

            <br/>
            <br/>

            <div style="font-weight:bold">Rights and Conditions</div>
                <%=Util.nullOrEmpty(cm.rights) ? "No rights and conditions assigned" : Util.escapeHTML(cm.rights)%>

            <% if (!Util.nullOrEmpty(cm.notes)) { %>
                <br><br>
                <div style="font-weight:bold">Notes</div>
                    <%= Util.escapeHTML(cm.notes)%>
                </div>
            <% } %>

            <% if (!Util.nullOrEmpty(cm.accessionMetadatas)) {
                Util.pluralize(cm.accessionMetadatas.size(), "accession");

                for (Archive.AccessionMetadata am: cm.accessionMetadatas) { %>
                    <hr/>
                    <div>
                        <b>Accession ID</b> <%=am.id%>
                        <p><b>Title</b> <%=am.title%>
                        <p><b>Scope and contents</b><br/> <%=am.scope%>
                        <p><b>Rights and conditions</b><br/> <%=am.rights%>
                        <p><b>Notes</b><br/> <%=am.notes%>
                    </div>
                <% }
        } %>

        <div style="text-align:left;margin-top: 40px"> <button id="enter" class="btn btn-cta" style="text-align:left">Enter <i class="icon-arrowbutton"></i> </button></div>

        <div id="stats"></div>

        <p></p>
        <%
            // not handled: if findingAidLink or catalogRecordLink have a double-quote embedded in them!
            if (!Util.nullOrEmpty(cm.findingAidLink)) { %>
            <div class="heading"><a target="_blank" href="<%=cm.findingAidLink%>">Finding Aid</a></div>
        <% } %>
        <br/>
        <% if (!Util.nullOrEmpty(cm.catalogRecordLink)) { %>
            <div class="heading"><a target="_blank" href="<%=cm.catalogRecordLink%>">Catalog Record</a></div>
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
        //the following call will first load archive and then in that jsp the subsequent screeen 'edit-collection-metadata.jsp' will be set as the next page
        //edit-screen parameter passed to loadArchive.jsp will help to distinguish whether to set the enxt screen as edit-collection-metadata.jsp or browse-top.jsp
        $('#edit-metadata').click (function() { window.location = 'edit-collection-metadata?collectionID=<%=id%>'; });
        $('#add-accession').click (function() { window.location = 'add-accession?collectionID=<%=id%>'});
        //result of succesful ajax/loadArchive should be a call to browse-top page with appropriate archiveID. hence
        //set it as a resultPage of the returned json object in ajax/loadArchive.jsp.
        var enterparams = {dir: '<%=id%>'};
        $('#enter').click(function() { fetch_page_with_progress ('ajax/loadArchive.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), enterparams, null); /* load_archive_and_call(function() { window.location = "browse-top"} */});

    </script>

</body>
</html>
