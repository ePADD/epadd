<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="java.io.*"%>
<%@page import="edu.stanford.muse.webapp.*"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@page import="edu.stanford.muse.Config"%>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>

<html>
<head>
    <title>Collections</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="css/css.jsp"/>

    <script src="js/jquery.js"></script>
    <script src="js/jquery.autocomplete.js" type="text/javascript"></script>

    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/epadd.js" type="text/javascript"></script>
</head>

<body>
<%@include file="header.jspf"%>
<script>epadd.nav_mark_active('Collections');</script>

<div style="width:1100px; margin:auto">
    <% if (ModeConfig.isProcessingMode()) { %>
        <h1>Welcome to the ePADD processing module.</h1>
  <% } else if (ModeConfig.isDeliveryMode()) { %>
        <h1>Welcome to the ePADD delivery module.</h1>
    <% } else if (ModeConfig.isDiscoveryMode()) { %>
        <h1>Discover historical email from around the world</h1>

    <% } else if (ModeConfig.isAppraisalMode()) { %>
        <div style="text-align:center">Sorry, this page is not available in appraisal mode.</div>
        <% return;
    }
  %>
</div>

  <div style="margin:auto;text-align:center">
  <div style="width:100%;text-align:left;">

    <br/>
  </div>
  <br/>
  <div>

    <%
      String modeBaseDir = "";
      if (ModeConfig.isProcessingMode())
          modeBaseDir = Config.REPO_DIR_PROCESSING;
      else if (ModeConfig.isDeliveryMode())
        modeBaseDir = Config.REPO_DIR_DELIVERY;
      else if (ModeConfig.isDiscoveryMode())
        modeBaseDir = Config.REPO_DIR_DISCOVERY;

      File topFile = new File(modeBaseDir);
        JSPHelper.log.info("Reading collections from: " + modeBaseDir);
        if (!topFile.exists() || !topFile.isDirectory() || !topFile.canRead()) {
          out.println ("Please place some archives in " + modeBaseDir);
        }  else {
          File[] files = topFile.listFiles();
          if (files != null) {
              for (File f : files) {
                  if (!f.isDirectory())
                      continue;

                  String id = f.getName();
                  String archiveFile = f.getAbsolutePath() + File.separator + Archive.BAG_DATA_FOLDER + File.separator +  Archive.SESSIONS_SUBDIR + File.separator + "default" + SimpleSessions.getSessionSuffix();

                  if (!new File(archiveFile).exists())
                      continue;

                  Archive.CollectionMetadata cm = ArchiveReaderWriter.readCollectionMetadata(f.getAbsolutePath());
                  if (cm != null) {
                      String fileParam = id + "/" + Archive.BAG_DATA_FOLDER+ "/" + Archive.IMAGES_SUBDIR + "/" + "landingPhoto"; // always forward slashes please
                      String url = "serveImage.jsp?file=" + fileParam;

                      out.println("<div data-dir=\"" + id + "\" class=\"archive-card\">");
                      %>

                      <%--warning: url might be fragile w.r.t. brackets or quotes--%>
                      <div class="landing-img" style="background-size: contain; background-repeat:no-repeat; background-position: center center; background-image:url('<%=url%>')"> <!-- https://stackoverflow.com/questions/2643305/centering-a-background-image-using-css -->
                      <% if(ModeConfig.isProcessingMode()){%>
                          <div class="landing-photo-edit" style="text-align: right; top: 10px; right: 10px; position: relative;">
                              <img src="images/edit_summary.svg"/>
                          </div>
                      <% } %>
                      </div>

                      <%
                      out.println("<div class=\"landing-img-text\">");
                      out.println(Util.nullOrEmpty(cm.collectionTitle) ? "No name assigned" : cm.collectionTitle);
                      out.println("<br><span class=\"detail\">" + Util.commatize(cm.nDocs) + " messages</span>");
                      out.println("<br><span class=\"detail\">" + Util.commatize(cm.nBlobs) + " attachments</span>");
                      out.println("<br><span class=\"detail\">Collection ID: " + (Util.nullOrEmpty(cm.collectionID) ? "Unassigned" : cm.collectionID) + " </span>");
                      out.println("</div>");
                      out.println("</div>");
                  }
              }
          }
      }
    %>
  </div>
  <br/>

    <script>
    $(document).ready(function() {
      $('.archive-card').click(function(e) {
        var dir = $(e.target).closest('.archive-card').attr('data-dir');
        window.location = 'collection-detail?collection=' + escape(dir); // worried about single quotes in dir
      });

      <% if (ModeConfig.isProcessingMode()) { %>
        $('.upload-btn').click(function(e) {
            //collect archiveID,and addressbookfile field. If  empty return false;
            var filePath = $('#landingPhoto').val();
            if (!filePath) {
                alert('Please provide the path of the landing page image');
                return false;
            }

            var form = $('#uploadLandingPhotoForm')[0];

            // Create an FormData object
            var data = new FormData(form);

            //hide the modal.
            $('#landingPhoto-upload-modal').modal('hide');
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
                        epadd.error('There was an error uploading the landing page image. (' + data.error + ')');
                    }
                },
                error: function (jq, textStatus, errorThrown) {
                    epadd.error("There was an error uploading the landing page image. (status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + ')');
                }
            });
        });

        $('.landing-photo-edit').click (function(e) {
            var collectionID = $(e.target).closest('.archive-card').attr('data-dir');
            $('input[name="collectionID"]').val(collectionID);
            $('#landingPhoto-upload-modal').modal();
            return false;
        });
    <% } %>
    });
  </script>

  <br/>

  <div id="landingPhoto-upload-modal" class="info-modal modal fade" style="z-index:99999">
      <div class="modal-dialog">
          <div class="modal-content">
              <div class="modal-header">
                  <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                  <h4 class="modal-title">Upload a landing page image (4:3 aspect ratio)</h4>
              </div>
              <div class="modal-body">
                  <form id="uploadLandingPhotoForm" method="POST" enctype="multipart/form-data" >
                      <div class="form-group">
                          <input class="collectionID" name="collectionID" type="hidden" value="foobar"/>
                          <label for="landingPhoto" class="col-sm-2 control-label">File</label>
                          <div class="col-sm-10">
                              <input type="file" id="landingPhoto" name="landingPhoto" value=""/>
                          </div>
                      </div>
                      <%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

                  </form>
              </div>
              <div class="modal-footer">
                  <button class="upload-btn btn btn-cta">Upload <i class="icon-arrowbutton"></i></button>


                  <%--<button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>--%>
                  <%--<button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>--%>
              </div>
          </div><!-- /.modal-content -->
      </div><!-- /.modal-dialog -->
  </div><!-- /.modal -->

  <jsp:include page="footer.jsp"/>
</body>
</html>
