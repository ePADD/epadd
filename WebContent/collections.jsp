<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.epadd.Config"%>

<html>
<head>
  <link rel="icon" type="image/png" href="images/epadd-favicon.png">

  <script src="js/jquery.js"></script>

  <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
  <!-- Optional theme -->
  <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
  <jsp:include page="css/css.jsp"/>
  <title>List Accessions</title>
  <script src="js/epadd.js" type="text/javascript"></script>

</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Collections');</script>

<div style="padding-left:170px;padding-right:50px;">
  <% if (ModeConfig.isDiscoveryMode()) { %>
    <h1>Welcome to ePADD.</h1>
    <p>
    ePADD is a platform that allows researchers to browse and search historical email archives.
    <p>
    Messages have been redacted to ensure the privacy of donors and other correspondents.
    Please contact the host repository if you would like to request access to full messages, including any attachments.
  <% } %>
  <% if (ModeConfig.isDeliveryMode()) { %>
    <h1>Welcome to the ePADD delivery module.</h1>
  <% } %>
</div>

<%!
  private static String formatMetadataField(String s)
  {
    if (s == null)
      return "Unassigned";
    else
      return Util.escapeHTML(s);
  }
%>

<%
  if (ModeConfig.isAppraisalMode())
  {
    out.println("<div style=\"text-align:center\">Sorry, this page is not available in appraisal mode.</div>");
    %>
    <jsp:include page="footer.jsp"/>
    <%
    return;
  }
%>
  <div style="margin:auto;text-align:center">
  <div style="width:100%;text-align:left;">

    <br/>
<!--    <%=edu.stanford.muse.util.Messages.getMessage("messages", "discovery.list-archives", new String[]{Config.holder, Config.holderReadingRoom, Config.holderContact}) %> -->
    <br/>
  </div>
  <br/>
  <div>

    <%
      String topDir = "";
      if (ModeConfig.isProcessingMode())
          topDir = Config.REPO_DIR_PROCESSING;
      else if (ModeConfig.isDeliveryMode())
        topDir = Config.REPO_DIR_DELIVERY;
      else if (ModeConfig.isDiscoveryMode())
        topDir = Config.REPO_DIR_DISCOVERY;

      File topFile = new File(topDir);
        JSPHelper.log.info("Reading collections from: "+topDir);
        if (!topFile.exists() || !topFile.isDirectory() || !topFile.canRead()) {
          out.println ("Please place some archives in " + topDir);
        } else {
          File[] files = topFile.listFiles();
          List<Archive.ProcessingMetadata> foundPMs = new ArrayList<Archive.ProcessingMetadata>();
          for (File f: files)
          {
            if (!f.isDirectory())
              continue;

            String id = f.getName();
            String archiveFile = f.getAbsolutePath() + File.separator + Archive.SESSIONS_SUBDIR + File.separator + "default" + Sessions.SESSION_SUFFIX;

            if (!new File(archiveFile).exists())
              continue;

            Archive.ProcessingMetadata pm = SimpleSessions.readProcessingMetadata(f.getAbsolutePath() + File.separator + Archive.SESSIONS_SUBDIR, "default");
            String fileParam = id + "/" + Archive.IMAGES_SUBDIR + "/" + "landingPhoto.png"; // always forward slashes please
            String url = "serveImage.jsp?mode=" + ModeConfig.mode + "&file=" + fileParam;

            out.println ("<div data-dir=\"" + id + "\" class=\"archive-card\">");
            out.println ("<div class=\"landing-img\" style=\"background-image:url('" + url + "')\"></div>");
            out.println ("<div class=\"landing-img-text\">");
            out.println (Util.nullOrEmpty(pm.ownerName) ?  "No name assigned" : pm.ownerName);
            out.println ("<br><span class=\"detail\">" + Util.commatize(pm.nDocs) + " messages</span>");
            out.println ("<br><span class=\"detail\">" + Util.commatize(pm.nBlobs) + " attachments</span>");
            out.println ("<br><span class=\"detail\">Collection ID: " + (Util.nullOrEmpty(pm.collectionID) ? "Unassigned" : pm.collectionID)+ " </span>");
            out.println ("<br><span class=\"detail\">Accession ID: " + (Util.nullOrEmpty(pm.accessionID) ? "Unassigned" : pm.accessionID) + " </span>");
            out.println ("</div>");
            out.println ("</div>");
          }
      }
    %>
  </div>
  <br/>

    <script>
    $(document).ready(function() {
      $('.archive-card').click(function(e) {
        var dir = $(e.target).closest('.archive-card').attr('data-dir');
        window.location = 'collection-detail?id=' + escape(dir); // worried about single quotes in dir
      });
    });
  </script>

  <br/>
  <jsp:include page="footer.jsp"/>
</body>
</html>
