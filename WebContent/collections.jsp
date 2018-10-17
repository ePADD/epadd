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

<div style="width:100%; margin:auto">
  <%
    // this is the landing page for discovery, so a special message.
    if (ModeConfig.isDiscoveryMode()) { %>
        <h1 style="font-size:32px; color:#0175bc">Welcome to ePADD.</h1>
        <p>
            <!--
        ePADD is a platform that allows researchers to browse and search historical email archives.
        <p>
        Messages have been redacted to ensure the privacy of donors and other correspondents.
        Please contact the host repository if you would like to request access to full messages, including any attachments.
        -->


            <div style="text-align:center; margin:auto; width:600px;">

                <div id="cross-collection-search" style="text-align:center">
                    <form method="get" action="cross-collection-search">

                        <input id="xcoll-search" name="term" size="80" placeholder="Cross-collection entity search"/>

                        <button class="btn btn-cta" style="margin-top: 5px" type="submit" name="Go">Search <i class="icon-arrowbutton"></i></button>

                    </form>
                </div>
    <p>
</div>

<script>
    $(document).ready(function() {
        var autocomplete_params = {
            serviceUrl: 'ajax/xcollSearchAutoComplete.jsp',
            onSearchError: function (query, jqXHR, textStatus, errorThrown) {epadd.log(textStatus+" error: "+errorThrown);},
            preventBadQueries: false,
            showNoSuggestionNotice: true,
            preserveInput: true,
            ajaxSettings: {
                "timeout":5000, /* 5000 instead of 3000 because xcoll search is likely to be slow */
                dataType: "json"
            },
            dataType: "text",
            //100ms
            deferRequestsBy: 100,
            onSelect: function(suggestion) {
                var existingvalue = $(this).val();
                var idx = existingvalue.lastIndexOf(';');
                if (idx <= 0)
                    $(this).val(suggestion.name);
                else
                    $(this).val (existingvalue.substring (0, idx+1) + ' ' + suggestion.name); // take everything up to the last ";" and replace after that
            },
            onHint: function (hint) {
                $('#autocomplete-ajax-x').val(hint);
            },
            onInvalidateSelection: function() {
                epadd.log('You selected: none');
            }
        };
        $('#xcoll-search').autocomplete(autocomplete_params);
    });

</script>
  <% } else if (ModeConfig.isDeliveryMode()) { %>
        <h1>Welcome to the ePADD delivery module.</h1>
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
                      String fileParam = id + "/" + Archive.BAG_DATA_FOLDER+ "/" + Archive.IMAGES_SUBDIR + "/" + "landingPhoto.png"; // always forward slashes please
                      String url = "serveImage.jsp?file=" + fileParam;

                      out.println("<div data-dir=\"" + id + "\" class=\"archive-card\">");
                      out.println("<div class=\"landing-img\" style=\"background-image:url('" + url + "')\"></div>");
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
    });
  </script>

  <br/>
  <jsp:include page="footer.jsp"/>
</body>
</html>
