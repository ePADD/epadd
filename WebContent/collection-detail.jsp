<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.Config"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.SimpleSessions"%>
<%@page language="java" import="java.io.File"%>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@ page import="javafx.scene.shape.Arc" %>

<html>
<head>
    <title>Collection Details</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="css/css.jsp"/>

    <script src="js/jquery.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/muse.js" type="text/javascript"></script>
    <script src="js/epadd.js" type="text/javascript"></script>
    <style>
        body { background-color: white; }
        hr { margin-top: 10px; margin-bottom: 10px; } /* to override bootstrap */
        div.accession { margin-bottom: 10px;}
        div.accession-heading { border: solid 1px #ccc; padding: 20px 20px; background-color: #f5f5f8; font-weight: 600;}
        div.accession-content { border-bottom: solid 1px #ccc; border-left: solid 1px #ccc; border-right: solid 1px #ccc;padding: 15px 20px;  }
    </style>
</head>
<body>
<jsp:include page="header.jspf"/>


<!-- need status window on this page because archive might take some time to load -->
<script type="text/javascript" src="js/statusUpdate.js"></script>
<%@include file="div_status.jspf"%>
<%! private static String formatMetadataField(String s) { return (s == null) ? "" : Util.escapeHTML(s); } %>

<%
    /* note: this page doesn't require an archive to be loaded. it should not have a profile block. */

    String modeBaseDir;
    String id = request.getParameter("collection");
    if (ModeConfig.isProcessingMode())
        modeBaseDir = Config.REPO_DIR_PROCESSING;
    else if (ModeConfig.isDeliveryMode())
        modeBaseDir = Config.REPO_DIR_DELIVERY;
    else if (ModeConfig.isDiscoveryMode())
        modeBaseDir = Config.REPO_DIR_DISCOVERY;
    else {
        modeBaseDir = Config.REPO_DIR_APPRAISAL; // not really needed in appraisal, but just leave it in, since we might some day support multiple archives in appraisal
        id = "user";
    }

    String archiveDir = modeBaseDir + File.separator + id;

    File f = new File(archiveDir);
    if (!f.isDirectory()) {
        return;
    }

    String archiveFile = f.getAbsolutePath() + File.separator + Archive.BAG_DATA_FOLDER + File.separator+ Archive.SESSIONS_SUBDIR + File.separator + "default" + SimpleSessions.getSessionSuffix();
    if (!new File(archiveFile).exists()) {
        return;
    }

    Archive.CollectionMetadata cm = ArchiveReaderWriter.readCollectionMetadata(f.getAbsolutePath());
    if (cm == null)
        return;

    String fileParam = f.getName() + "/" + Archive.BAG_DATA_FOLDER+ "/" + Archive.IMAGES_SUBDIR + "/" + "bannerImage.png"; // always forward slashes please
    String url = "serveImage.jsp?file=" + fileParam;
%>

<div class="collection-detail">
    <div class="breadcrumbs"> <%=ModeConfig.getModeForDisplay()%> &nbsp;&nbsp;&nbsp;&nbsp;| &nbsp;&nbsp;&nbsp;&nbsp; About this collection </div>

    <p>
    <div class="banner-img" style="background-image:url('<%=url%>')"> <!-- escape needed? -->
    </div>

    <br/>
    <div class="details">
        <div class="heading">Summary <a href="edit-collection-metadata?collection=<%=id%>" style="cursor:pointer;margin-left:75px;"><img style="height:25px" src="images/edit_summary.svg"/></a></div>
        <hr/>
            Institution<br/>
            <b><span class="detail"><%=(Util.nullOrEmpty(cm.institution) ? "Unassigned" : cm.institution)%> </span></b>
            <hr/>
            Repository<br/>
            <b><span class="detail"><%=(Util.nullOrEmpty(cm.repository) ? "Unassigned" : cm.repository)%> </span></b>
            <hr/>
            Collection ID<br/>
            <b><span class="detail"><%=(Util.nullOrEmpty(cm.collectionID) ? "Unassigned" : cm.collectionID)%> </span></b>
            <hr/>
            <% if (!Util.nullOrEmpty(cm.accessionMetadatas)) { %>
                <b><span><%= Util.pluralize (cm.accessionMetadatas.size(), "accession")%></span></b>
                <hr/>
        <% } %>
            <% if (cm.firstDate != null && cm.lastDate != null) { %>
                Date Range<br/>
                <span class="detail"><%=Util.formatDate(cm.firstDate)%> to <%=Util.formatDate(cm.lastDate)%></span>
                <% if (cm.nHackyDates > 0) { %>
                   <br/><b><%=Util.pluralize(cm.nHackyDates, "message")%> undated</b>
            <hr/>

                <% } %>
            <% } %>
            Messages: <span class="detail"><%=Util.commatize(cm.nDocs)%></span>
            <% if (cm.nIncomingMessages > 0 || cm.nOutgoingMessages > 0) { %>
                <br/>
                Incoming: <span class="detail"><b><%=Util.commatize(cm.nIncomingMessages)%></b></span><br/>
            Outgoing: <span class="detail"><b><%=Util.commatize(cm.nOutgoingMessages)%></b></span>
            <% } %>
        <hr/>
            Attachments: <span class="detail"><%=Util.commatize(cm.nBlobs)%></span>
                <% if (cm.nDocBlobs > 0 || cm.nImageBlobs > 0 || cm.nOtherBlobs > 0) { %>
                    <br/>
                    Images: <b><span class="detail"><%=Util.commatize(cm.nImageBlobs)%></span><br/></b>
                    Documents: <b><span class="detail"><%=Util.commatize(cm.nDocBlobs)%></span><br/></b>
                    Others: <b><span class="detail"><%=Util.commatize(cm.nOtherBlobs)%></span></b>
                <% } %>
        <hr/>

        </p>

        <% if (!Util.nullOrEmpty(cm.contactEmail)) { /* show contact email, but only if it actually present */ %>
            <p>
                Contact email<br/>
                <span class="detail"><%=(Util.nullOrEmpty(cm.contactEmail) ? "Unassigned" : cm.contactEmail)%> </span>
            </p>
        <% }

        if (ModeConfig.isProcessingMode()) { %>
        <p>
            <% if(cm.renamedFiles!=0 || cm.normalizedFiles!=0){%>
            Preservation Actions <br>
            Renamed files: <span class="detail"><%=Util.commatize(cm.renamedFiles)%></span><br/>
            Normalized files: <span class="detail"><%=Util.commatize(cm.normalizedFiles)%></span><br/>
            <%}%>
            <button class="btn-default" id="edit-photos"><i class="fa fa-pencil"></i> Edit Photos</button>
        </p>
        <br/>

        <% } %>
    </div>

    <div class="related-links">
        <div class="banner-img-text-block">

            <div class="banner-img-text-large">
                <div style="display:inline-block;overflow:hidden;width:745px"/>
                <%=cm.collectionTitle%>
                </div>
                <button class="enter btn btn-cta">Enter <i class="icon-arrowbutton"></i> </button>
                <hr/>
        </div>

            <br/>
            <%=Util.nullOrEmpty(cm.about) ? "Unassigned" : Util.escapeHTML(cm.about)%>

            <% if (!Util.nullOrEmpty(cm.scopeAndContent)) { %>
                <br><br>
                <b>Scope and Content</b><br/>
                 <%= Util.escapeHTML(cm.scopeAndContent)%>
            <% } %>

            <% if (!Util.nullOrEmpty(cm.rights)) { %>
                <br><br>
                <b>Rights and Conditions</b><br/>
                 <%= Util.escapeHTML(cm.rights)%>
            <% } %>

            <% if (!Util.nullOrEmpty(cm.notes)) { %>
                <br><br>
                <b>Notes</b><br/>
                <%= Util.escapeHTML(cm.notes)%>
            <% } %>

        <hr style="margin: 20px 0px"/>

        <% if (!Util.nullOrEmpty(cm.accessionMetadatas)) {
                Util.pluralize(cm.accessionMetadatas.size(), "accession");

                for (Archive.AccessionMetadata am: cm.accessionMetadatas) { %>
                    <div class="accession">
                        <div class="accession-heading">Accession ID: <%=formatMetadataField(am.id)%>
                            <a style="margin-left: 30px" class="edit-accession-metadata"  data-accessionID="<%=am.id%>" href="#"><img style="height:25px" src="images/edit_summary.svg"/></a>
                        </div>
                        <div class="accession-content">
                            <b>Title</b>: <%=formatMetadataField(am.title)%><br/>
                            <b>Date</b>: <%=formatMetadataField(am.date)%><br/><br/>

                            <p><b>Scope and contents</b><br/> <%=formatMetadataField(am.scope)%>
                            <p><b>Rights and conditions</b><br/> <%=formatMetadataField(am.rights)%>
                            <p><b>Notes</b><br/> <%=formatMetadataField(am.notes)%>
                        </div>
                    </div>
                <% } %>

                <% } %>

        <div class="accession">
            <div class="accession-heading">
                <a href="add-accession?collection=<%=id%>">Add accession</a>
            </div>
        </div>

            <p></p>
            <%
                // not handled: if findingAidLink or catalogRecordLink have a double-quote embedded in them!
                if (!Util.nullOrEmpty(cm.findingAidLink)) { %>
                    <a target="_blank" href="<%=cm.findingAidLink%>">Finding Aid</a>
                    &nbsp;&nbsp;&nbsp;
                <% } %>
                <% if (!Util.nullOrEmpty(cm.catalogRecordLink)) { %>
                    <a target="_blank" href="<%=cm.catalogRecordLink%>">Catalog Record</a>
                    <br/>
                <% } %>

        </div>
        <div style="margin-left: 745px">
            <button class="enter btn btn-cta">Enter <i class="icon-arrowbutton"></i> </button>
        </div>

        <div id="stats"></div>
    </div>

    <% if (ModeConfig.isDiscoveryMode()) { %>
        Email messages in the ePADD Discovery Module have been redacted to ensure the privacy of donors and other correspondents.
        Please contact the host repository if you would like to request access to full messages, including any attachments.
    <% } %>

    <br/>
    <br/>

    <script>
        $('#add-accession').click (function() { window.location = 'add-accession?collection=<%=id%>'});
        $('#edit-photos').click (function() { window.location = 'set-images?collection=<%=id%>'; });
        $('.edit-accession-metadata').click (function(e) {
            var accessionID=$(e.target).closest('a').attr('data-accessionID'); // e.target is the edit-icon, so we look up to find the closest a
            window.location = 'edit-accession-metadata?collection=<%=id%>&accessionID='+accessionID;
            return false;
        });

        //result of succesful ajax/loadArchive should be a call to browse-top page with appropriate archiveID. hence
        //set it as a resultPage of the returned json object in ajax/loadArchive.jsp.
        var enterparams = {dir: '<%=id%>'};
        $('.enter').click(function() { fetch_page_with_progress ('ajax/loadArchive.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), enterparams, null); /* load_archive_and_call(function() { window.location = "browse-top"} */});

    </script>

</body>
</html>
