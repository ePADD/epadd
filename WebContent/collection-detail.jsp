<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.stanford.muse.Config"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@page import="edu.stanford.muse.util.Util"%>
<%@page import="edu.stanford.muse.webapp.SimpleSessions"%>
<%@page import="java.io.File"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@ page import="edu.stanford.muse.email.FetchStats" %>
<%@ page import="edu.stanford.muse.email.FolderInfo" %>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<html>

<head>

    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="css/bootstrap-dialog.css">
    <jsp:include page="css/css.jsp"/>

    <script src="js/jquery.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/muse.js" type="text/javascript"></script>
    <script src="js/epadd.js" type="text/javascript"></script>
	<script src="js/bootstrap-dialog.js"></script>
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
    </style>
</head>
<body>

<%@include file="header.jspf"%>
<title><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.head-collection-details")%></title>

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
    String archiveDirForJs = archiveDir.replace("\\", "\\\\");
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

    // we add the following code to support file metada requirement in epadd+ project
    // It handles there is case for no accession defined in collection. i.e. If user import collection data by coping and pasting from appraisal folder, processing module treat the archive as in "default" accession.
    // Here we will not create an accession object for "default" accession.
    // In this case, file metadata will belong directly under collection instead of accession. (We may just leave it in and may change it in future)
    if (    ModeConfig.isProcessingMode() &&                      // i.e. creating file metadata only during Processing module
            Util.nullOrEmpty(cm.accessionMetadatas) &&         // i.e. no existing accession metadata defined yet.
            cm.fileMetadatas == null                            // i.e. make sure file metadata created once and there is not any existing file metadata created yet.
    ){
        Archive collection = ArchiveReaderWriter.readArchiveIfPresent(archiveDir);

        List<Archive.FileMetadata> fms = new ArrayList<Archive.FileMetadata>();
        Archive.FileMetadata fm = new Archive.FileMetadata();

        List<FetchStats> fetchStats = collection.allStats;
        int count = 0;

        if(fetchStats!=null) {
            for (FetchStats fs : fetchStats) {
                fm = new Archive.FileMetadata();
                fm.fileID = "Collection/File/" + StringUtils.leftPad(""+count, 4, "0");
                fm.fileFormat = "MBOX";
                fm.notes="";

                if (fs.selectedFolders != null) {
                    for (Pair<String, FolderInfo> p : fs.selectedFolders){
                        fm.filename = Util.escapeHTML(p.getFirst());
                        break;
                    }
                }

                count ++;
                fms.add(fm);
            } // end for
        }   //end if (fetchStats!=null)

        cm.fileMetadatas = fms;
        collection.collectionMetadata = cm;//IMP otherwise in-memory archive processingmetadata and
        //asumme it be an incremental update
        ArchiveReaderWriter.saveCollectionMetadata(collection,Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
        //the updated metadata on disc will be out of sync. It manifests when saving this archive which
        //overwrites the latest on-disc PM data with stale in-memory data.
        JSPHelper.log.info ("File metadata imported");
    }

    String fileParam = f.getName() + "/" + Archive.BAG_DATA_FOLDER+ "/" + Archive.IMAGES_SUBDIR + "/" + "bannerImage"; // always forward slashes please
    String url = "serveImage.jsp?file=" + fileParam;
%>

<div class="collection-detail">
    <div class="breadcrumbs"> <%=ModeConfig.getModeForDisplay(archiveID)%> &nbsp;&nbsp;&nbsp;&nbsp;| &nbsp;&nbsp;&nbsp;&nbsp; <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.about-collection")%> </div>

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
        <div class="heading"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.summary")%>
            <% if (ModeConfig.isProcessingMode()) { %>
                <a href="edit-metadata?collection=<%=id%>" style="cursor:pointer;margin-left:75px;"><img style="height:25px" src="images/edit_summary.svg"/></a>
            <% } %>
        </div>
        <hr/>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.institution")%><br/>
            <b><span class="detail"><%=(Util.nullOrEmpty(cm.institution) ? "Unassigned" : cm.institution)%> </span></b>
            <hr/>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.repository")%><br/>
            <b><span class="detail"><%=(Util.nullOrEmpty(cm.repository) ? "Unassigned" : cm.repository)%> </span></b>
            <hr/>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.coll-id")%><br/>
            <b><span class="detail"><%=(Util.nullOrEmpty(cm.collectionID) ? "Unassigned" : cm.collectionID)%> </span></b>
            <hr/>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.coll-title")%><br/>
            <b><span class="detail"><%=(Util.nullOrEmpty(cm.collectionTitle) ? "Unassigned" : cm.collectionTitle)%> </span></b>
            <hr/>
        <% if (!Util.nullOrEmpty(cm.accessionMetadatas)) { %>
                <b><span><%= Util.pluralize (cm.accessionMetadatas.size(), "accession")%></span></b>
                <hr/>
        <% } %>
            <% if (cm.firstDate != null && cm.lastDate != null) { %>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.date-range")%><br/>
        <span class="detail"><b><%=Util.formatDate(cm.firstDate)%></b> to <b><%=Util.formatDate(cm.lastDate)%></b></span>
                <% if (cm.nHackyDates > 0) { %>
                   <br/><b><%=Util.pluralize(cm.nHackyDates, "message")%> <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.date-undated")%></b>
            <hr/>

                <% } %>
            <% } %>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.messages")%> <span class="detail"><%=Util.commatize(cm.nDocs)%></span><br/></b>
        <%
            int sentbyowner = cm.nOutgoingMessages;
        %>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.sent-by-owner")%> <span class="detail"><b><%=Util.commatize(sentbyowner)%></b></span><br/></b>
    <%--<% if (cm.nIncomingMessages > 0 || cm.nOutgoingMessages > 0) { %>
                <br/>
                Incoming: <span class="detail"><b><%=Util.commatize(cm.nIncomingMessages)%></b></span><br/>
            Outgoing: <span class="detail"><b><%=Util.commatize(cm.nOutgoingMessages)%></b></span>
            <% } %>--%>
        <hr/>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.attachments")%> <span class="detail"><%=Util.commatize(cm.nBlobs)%></span>
                <% if (cm.nDocBlobs > 0 || cm.nImageBlobs > 0 || cm.nOtherBlobs > 0) { %>
                    <br/>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.images")%> <b><span class="detail"><%=Util.commatize(cm.nImageBlobs)%></span><br/></b>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.documents")%> <b><span class="detail"><%=Util.commatize(cm.nDocBlobs)%></span><br/></b>
        <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.others")%> <b><span class="detail"><%=Util.commatize(cm.nOtherBlobs)%></span></b>
                <% } %>
        <hr/>

        <% if (!Util.nullOrEmpty(cm.contactEmail)) { /* show contact email, but only if it actually present */ %>
            <p>
                <%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.con-email")%><br/>
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
            <%--<button class="btn-default" id="edit-photos"><i class="fa fa-pencil"></i> Edit Photos</button>--%>
        </p>
        <br/>

        <% } %>
        <%
            // not handled: if findingAidLink or catalogRecordLink have a double-quote embedded in them!
            if (!Util.nullOrEmpty(cm.findingAidLink)) { %>
        <a target="_blank" href="<%=cm.findingAidLink%>"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.finding-aid")%></a>
        &nbsp;&nbsp;&nbsp;
        <% } %>
        <% if (!Util.nullOrEmpty(cm.catalogRecordLink)) { %>
        <a target="_blank" href="<%=cm.catalogRecordLink%>"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.catalog-rec")%></a>
        <br/>
        <% } %>
		
		<div align="middle"><button id="btn_delete" class="btn-default" onclick="delFolder();">Delete</button></div>
    </div>

    <div class="collection-info">
        <div class="banner-img-text-block">

            <div class="banner-img-text-large">
                <div style="display:inline-block;overflow:hidden;width:745px"/>
                <%=cm.collectionTitle%>
                </div>
                <button class="collection-enter btn btn-cta"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.enter-button")%> <i class="icon-arrowbutton"></i> </button>
                <hr/>
        </div>

            <br/>
            <%=Util.nullOrEmpty(cm.about) ? "(About this archive - unassigned)" : Util.escapeHTML(cm.about)%>
            <div class="epadd-separator"></div>

            <% if (!Util.nullOrEmpty(cm.scopeAndContent)) { %>
                <br>
                <b><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.scope-content")%></b><br/>
                 <%= Util.escapeHTML(cm.scopeAndContent)%>
                <div class="epadd-separator"></div>
            <% } %>

            <% if (!Util.nullOrEmpty(cm.rights)) { %>
                <b><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.rights-cond")%></b><br/>
                 <%= Util.escapeHTML(cm.rights)%>
                <div class="epadd-separator"></div>

        <% } %>

            <% if (!Util.nullOrEmpty(cm.notes)) { %>
                <br>
                <b><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.notes")%></b><br/>
                <%= Util.escapeHTML(cm.notes)%>
                <%--<div class="epadd-separator"></div>--%>

        <% } %>

        <hr style="margin: 20px 0px"/>

        <% if (!Util.nullOrEmpty(cm.accessionMetadatas)) {
                Util.pluralize(cm.accessionMetadatas.size(), "accession");

                for (Archive.AccessionMetadata am: cm.accessionMetadatas) { %>
                    <div class="accession">
                        <div class="accession-heading"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.acc.acc-id")%> <%=formatMetadataField(am.id)%>
                            <% if(ModeConfig.isProcessingMode()){%>
                            <a style="margin-left: 30px" class="edit-accession-metadata"  data-accessionID="<%=am.id%>" href="#"><img style="height:25px" src="images/edit_summary.svg"/></a>
                            <%}%>
                        </div>
                        <div class="accession-content">
                            <b><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.acc.acc-title")%></b>: <%=formatMetadataField(am.title)%><br/>
                            <%--<div class="epadd-separator"></div>--%>

                            <%--<b>Date</b>: <%=formatMetadataField(am.date)%><br/><br/>--%>
                            <%if(!Util.nullOrEmpty(am.scope)){%>
                            <div class="epadd-separator"></div>
                            <p><b><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.acc.scope-content")%></b><br/> <%=formatMetadataField(am.scope)%>
                            <%}%>
                                    <%if(!Util.nullOrEmpty(am.rights)){%>

                            <div class="epadd-separator"></div>
                            <p><b><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.acc.rights-cond")%></b><br/> <%=formatMetadataField(am.rights)%>
                            <%}%>
                                    <%if(!Util.nullOrEmpty(am.notes)){%>
                            <div class="epadd-separator"></div>
                            <p><b><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.acc.notes")%></b><br/> <%=formatMetadataField(am.notes)%>
                                <%}%>
                            <%--<div class="epadd-separator"></div>--%>
                        </div>
                    </div>
                <% } %>

                <% } %>

        <% if(ModeConfig.isProcessingMode()){%>
        <div class="accession">
            <div class="accession-heading">
                <a href="add-accession?collection=<%=id%>"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.acc.add-acc")%></a>
            </div>
        </div>
        <%}%>

            <p></p>


        </div>
        <div style="margin-left: 745px">
            <button class="collection-enter btn btn-cta"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.enter-button")%> <i class="icon-arrowbutton"></i> </button>
        </div>

        <div id="stats"></div>
    </div>

    <% if (ModeConfig.isDiscoveryMode()) { %>
        <p style="margin-top: 15px">
        Email messages in the ePADD Discovery Module have been redacted to ensure the privacy of donors and other correspondents.
        Please contact the host repository if you would like to request access to full messages, including any attachments.
        </p>
    <% } %>

    <br/>
    <br/>

<div id="bannerImage-upload-modal" class="info-modal modal fade" style="z-index:99999">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.image-upload")%></h4>
            </div>
            <div class="modal-body">
                <form id="uploadBannerImageForm" method="POST" enctype="multipart/form-data" >
                    <input type="hidden" value="<%=id%>" name="collectionID"/>
                    <div class="form-group">
                        <label for="bannerImage" class="col-sm-2 control-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.image-file")%></label>
                        <div class="col-sm-10">
                            <input type="file" id="bannerImage" name="bannerImage" value=""/>
                        </div>
                    </div>
                    <%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

                </form>
            </div>
            <div class="modal-footer">
                <button id="upload-btn" class="btn btn-cta" onclick="uploadBannerImageHandler();return false;"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection-detail.upload-button")%> <i class="icon-arrowbutton"></i></button>


                <%--<button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>--%>
                <%--<button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>--%>
            </div>
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

    <script>
        $('.edit-accession-metadata').click (function(e) {
            var accessionID=$(e.target).closest('a').attr('data-accessionID'); // e.target is the edit-icon, so we look up to find the closest a
            window.location = 'edit-metadata?collection=<%=id%>&accession='+accessionID;
            return false;
        });

        function delFolder () {
            <%  String getPath = archiveDir.replaceAll("\\\\","/");  %>
            var folder = "<%=getPath%>";
            
            BootstrapDialog.show({
                title: 'Delete Collection',
                message: 'Confirm to delete this Collection?<br>[<%=id%>]',
                type: BootstrapDialog.TYPE_DANGER,
                data: {
                   'folder': folder
                },
                buttons: [
                   {
                    id: 'btn-yes',   
//                  icon: 'glyphicon glyphicon-check',       
                    label: 'Yes',
                    cssClass: 'btn-default', 
                    autospin: false,
                    action: function(dialogRef){    
                        $.ajax({
                            type: "POST",
                            url: "ajax/delFolder",
                            data: { folder: folder },
                            success: function(response) {
                               if (response['result'] === "ok") {
                                    BootstrapDialog.alert(
                                        {
                                         message:response['reason'], 
                                         type: BootstrapDialog.TYPE_SUCCESS,
                                         action: function(dialog) {
                                            dialog.close(); 
                                          },
                                          callback: function(result) {
                                            window.location.href = 'collections';
                                          }    
                                        }
                                    );
                                    dialogRef.close();
//                                    window.location.href = 'collections';
                               } else {
                                    BootstrapDialog.show({message:response['reason'], type: BootstrapDialog.TYPE_WARNING});
                               }
                            },
                            error: function() {
                                    BootstrapDialog.show({message:response['reason'], type: BootstrapDialog.TYPE_WARNING});
                            }
                        });                                
                    }
                  },
                  {
                   id: 'btn-cancel',   
//                 icon: 'glyphicon glyphicon-check',       
                   label: 'No',
                   cssClass: 'btn-default', 
                   autospin: false,
                   action: function(dialogRef){    
                        dialogRef.close();
                   }    
                  }
                ]
            });                    
        }    

        function loadArchive(){
            var enterparams = 'dir='+encodeURIComponent('<%=id%>');
            var page = "ajax/async/loadArchive.jsp";

            try {
                fetch_page_with_progress ('ajax/async/loadArchive.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), enterparams, null);
            } catch(err) { }
        }

        // To support exportable asset, result of success ajax/setExportableAssets should be a call to ajax/loadArchive

        //result of succesful ajax/loadArchive should be a call to browse-top page with appropriate archiveID. hence
        //set it as a resultPage of the returned json object in ajax/loadArchive.jsp.
        /*
        var enterparams = 'dir='+encodeURIComponent('<%=id%>');
        $('.collection-enter').click(function() { fetch_page_with_progress ('ajax/async/loadArchive.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), enterparams, null); });
        */
        $('.collection-enter').click(function() {
            var post_params = '&exportableAssets=exportProcessing&archiveDir=<%=archiveDirForJs%>';
            var page = "ajax/async/setExportableAssets.jsp";

            try {
                fetch_page_with_progress(page, "status", document.getElementById('status'), document.getElementById('status_text'), post_params, loadArchive);
                //fetch_page_with_progress(page, "status", document.getElementById('status'), document.getElementById('status_text'), post_params);   //debug only
            } catch(err) { }
        });

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
                    if (data && data.status === 0) {
                        window.location.reload();
                    } else {
                        epadd.error('There was an error uploading the banner image. (' + data.error + ')');
                    }
                },
                error: function (jq, textStatus, errorThrown) {
                    epadd.error("There was an error uploading the banner image. (status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + ')');
                }
            });
        };

        $('.banner-img-edit').click (function() { $('#bannerImage-upload-modal').modal(); });
    </script>

</body>
</html>
