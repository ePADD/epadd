<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.Config"%>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>

<html>
<head>
    <title>Collections</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="css/css.jsp"/>

    <script src="js/jquery.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/epadd.js" type="text/javascript"></script>
</head>

<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Collections');</script>

<div style="padding-left:170px;padding-right:50px;">
  <%
    // this is the landing page for discovery, so a special message.
    if (ModeConfig.isDiscoveryMode()) { %>
        <h1>Welcome to ePADD.</h1>
        <p>
        ePADD is a platform that allows researchers to browse and search historical email archives.
        <p>
        Messages have been redacted to ensure the privacy of donors and other correspondents.
        Please contact the host repository if you would like to request access to full messages, including any attachments.
  <% } else if (ModeConfig.isDeliveryMode()) { %>
        <h1>Welcome to the ePADD delivery module.</h1>
  <% } %>
</div>

<%
  if (ModeConfig.isAppraisalMode())
  {
    out.println("<div style=\"text-align:center\">Sorry, this page is not available in appraisal mode.</div>");
    return;
  }
%>

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
                      String fileParam = id + "/" + Archive.IMAGES_SUBDIR + "/" + "landingPhoto.png"; // always forward slashes please
                      String url = "serveImage.jsp?file=" + fileParam;

                      out.println("<div data-dir=\"" + id + "\" class=\"archive-card\">");
                      out.println("<div class=\"landing-img\" style=\"background-image:url('" + url + "')\"></div>");
                      out.println("<div class=\"landing-img-text\">");
                      out.println(Util.nullOrEmpty(cm.ownerName) ? "No name assigned" : cm.ownerName);
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
