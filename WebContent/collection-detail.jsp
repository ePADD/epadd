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

        .collection-detail { text-align: left; margin:auto; width: 1100px; position: relative;}
        .collection-detail .heading { font-size: 20px;}

        .collection-detail .banner-img-text-block { top: 80px; left: 30px; font-size: 16px; text-align: left;}
        .collection-detail .banner-img-text-large { font-size: 20px; text-align:left;}
        .collection-detail .details { border-right: dotted 1px rgba(127,127,127,0.5);display: inline-block; width: 220px; overflow: hidden;}
        .collection-detail .collection-info { vertical-align: top; display: inline-block; margin-left: 30px; width: 840px;}
        .collection-detail .detail { font-weight: bold; }

        body { background-color: white; }
        hr { margin-top: 10px; margin-bottom: 10px; } /* to override bootstrap */
        div.accession { margin-bottom: 10px;}
        div.accession-heading { border: solid 1px #ccc; padding: 20px 20px; background-color: #f5f5f8; font-weight: 600;}
        div.accession-content { border-bottom: solid 1px #ccc; border-left: solid 1px #ccc; border-right: solid 1px #ccc;padding: 15px 20px;  }

        .banner-img { box-shadow: 1px 1px 1px 0px rgba(0, 0, 0, 0.75); background-color: #e0e4e6; background-repeat: no-repeat; background-size: cover; width: 1100px; height: 300px; position: relative; text-align:center;}
        .banner-img-edit { text-align: right;position: relative;top: 20px; right: 20px; cursor:pointer;}
        div.epadd-separator { height: 3px; width: 25px; border: solid 1px #0175bc; background-color: #0175bc; margin: 20px 0px; }
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
        out.println ("No collection at that location.");
        return;
    }

    String archiveFile = f.getAbsolutePath() + File.separator + Archive.BAG_DATA_FOLDER + File.separator+ Archive.SESSIONS_SUBDIR + File.separator + "default" + SimpleSessions.getSessionSuffix();
    if (!new File(archiveFile).exists()) {
        out.println ("No collection at that location.");
        return;
    }

    Archive.CollectionMetadata cm = ArchiveReaderWriter.readCollectionMetadata(f.getAbsolutePath());
    if (cm == null) {
        out.println ("No metadata for that collection. The archive folder may be corrupted. Please contact your ePADD administrator.");
        return;
    }

    String fileParam = f.getName() + "/" + Archive.BAG_DATA_FOLDER+ "/" + Archive.IMAGES_SUBDIR + "/" + "bannerImage"; // always forward slashes please
    String url = "serveImage.jsp?file=" + fileParam;
%>

<div class="collection-detail">
    <div class="breadcrumbs"> <%=ModeConfig.getModeForDisplay()%> &nbsp;&nbsp;&nbsp;&nbsp;| &nbsp;&nbsp;&nbsp;&nbsp; About this collection </div>

    <p>
    <div class="banner-img" style="background-size: contain; background-repeat:no-repeat; background-position: center center; background-image:url('<%=url%>')"> <!-- https://stackoverflow.com/questions/2643305/centering-a-background-image-using-css -->
    <% if(ModeConfig.isProcessingMode()){%>
        <div class="banner-img-edit">
            <img src="images/edit_summary.svg"/>
        </div>
    <% } %>
    </div>

    <br/>
    <div class="details">
        <div class="heading">Summary
            <% if (ModeConfig.isProcessingMode()) { %>
                <a href="edit-collection-metadata?collection=<%=id%>" style="cursor:pointer;margin-left:75px;"><img style="height:25px" src="images/edit_summary.svg"/></a>
            <% } %>
        </div>
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

    <div class="collection-info">
        <div class="banner-img-text-block">

            <div class="banner-img-text-large">
                <div style="display:inline-block;overflow:hidden;width:745px"/>
                <%=cm.collectionTitle%>
                </div>
                <button class="collection-enter btn btn-cta">Enter <i class="icon-arrowbutton"></i> </button>
                <hr/>
        </div>

            <br/>
            <%=Util.nullOrEmpty(cm.about) ? "(About this archive - unassigned)" : Util.escapeHTML(cm.about)%>
            <div class="epadd-separator"></div>

            <% if (!Util.nullOrEmpty(cm.scopeAndContent)) { %>
                <br>
                <b>Scope and Content</b><br/>
                 <%= Util.escapeHTML(cm.scopeAndContent)%>
                <div class="epadd-separator"></div>
            <% } %>

            <% if (!Util.nullOrEmpty(cm.rights)) { %>
                <b>Rights and Conditions</b><br/>
                 <%= Util.escapeHTML(cm.rights)%>
                <div class="epadd-separator"></div>

        <% } %>

            <% if (!Util.nullOrEmpty(cm.notes)) { %>
                <br>
                <b>Notes</b><br/>
                <%= Util.escapeHTML(cm.notes)%>
                <div class="epadd-separator"></div>

        <% } %>

        <hr style="margin: 20px 0px"/>

        <% if (!Util.nullOrEmpty(cm.accessionMetadatas)) {
                Util.pluralize(cm.accessionMetadatas.size(), "accession");

                for (Archive.AccessionMetadata am: cm.accessionMetadatas) { %>
                    <div class="accession">
                        <div class="accession-heading">Accession ID: <%=formatMetadataField(am.id)%>
                            <% if(ModeConfig.isProcessingMode()){%>
                            <a style="margin-left: 30px" class="edit-accession-metadata"  data-accessionID="<%=am.id%>" href="#"><img style="height:25px" src="images/edit_summary.svg"/></a>
                            <%}%>
                        </div>
                        <div class="accession-content">
                            <b>Title</b>: <%=formatMetadataField(am.title)%><br/>
                            <div class="epadd-separator"></div>

                            <b>Date</b>: <%=formatMetadataField(am.date)%><br/><br/>

                            <div class="epadd-separator"></div>
                            <p><b>Scope and contents</b><br/> <%=formatMetadataField(am.scope)%>
                            <div class="epadd-separator"></div>
                            <p><b>Rights and conditions</b><br/> <%=formatMetadataField(am.rights)%>
                            <div class="epadd-separator"></div>
                            <p><b>Notes</b><br/> <%=formatMetadataField(am.notes)%>
                            <div class="epadd-separator"></div>
                        </div>
                    </div>
                <% } %>

                <% } %>

        <% if(ModeConfig.isProcessingMode()){%>
        <div class="accession">
            <div class="accession-heading">
                <a href="add-accession?collection=<%=id%>">Add accession</a>
            </div>
        </div>
        <%}%>

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
            <button class="collection-enter btn btn-cta">Enter <i class="icon-arrowbutton"></i> </button>
        </div>

        <div id="stats"></div>
    </div>

    <% if (ModeConfig.isDiscoveryMode()) { %>
        Email messages in the ePADD Discovery Module have been redacted to ensure the privacy of donors and other correspondents.
        Please contact the host repository if you would like to request access to full messages, including any attachments.
    <% } %>

    <br/>
    <br/>

<div id="bannerImage-upload-modal" class="modal fade" style="z-index:9999">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title">Upload a banner image (11:3 aspect ratio)</h4>
            </div>
            <div class="modal-body">
                <form id="uploadBannerImageForm" method="POST" enctype="multipart/form-data" >
                    <input type="hidden" value="<%=id%>" name="collectionID"/>
                    <div class="form-group">
                        <label for="bannerImage" class="col-sm-2 control-label">File</label>
                        <div class="col-sm-10">
                            <input type="file" id="bannerImage" name="bannerImage" value=""/>
                        </div>
                    </div>
                    <%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

                </form>
            </div>
            <div class="modal-footer">
                <button id="upload-btn" class="btn btn-cta" onclick="uploadBannerImageHandler();return false;">Upload <i class="icon-arrowbutton"></i></button>


                <%--<button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>--%>
                <%--<button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>--%>
            </div>
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

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
        $('.collection-enter').click(function() { fetch_page_with_progress ('ajax/loadArchive.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), enterparams, null); /* load_archive_and_call(function() { window.location = "browse-top"} */});

        var uploadBannerImageHandler=function() {
            //collect archiveID,and addressbookfile field. If  empty return false;
            var filePath = $('#bannerImage').val();
            if (!filePath) {
                alert('Please provide the path of the banner image');
                return false;
            }

            var form = $('#uploadBannerImageForm')[0];

            // Create an FormData object
            var data = new FormData(form);
            //hide the modal.
            $('#bannerImage-upload-modal').modal('hide');
            //now send to the backend.. on it's success reload the same page. On failure display the error message.

            $.ajax({
                type: 'POST',
                enctype: 'multipart/form-data',
                processData: false,
                url: "ajax/upload-images.jsp",
                contentType: false,
                cache: false,
                data: data,
                success: function (data) {
                    if (data && data.status == 0) {
                        window.location.reload();
                    } else {
                        epadd.error('There was an error uploading the banner image. (' + data.error + ')');
                    };
                },
                error: function (jq, textStatus, errorThrown) {
                    epadd.error("There was an error uploading the banner image. (status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + ')');
                }
            });
        }

        $('.banner-img-edit').click (function() { $('#bannerImage-upload-modal').modal(); });
    </script>

</body>
</html>
