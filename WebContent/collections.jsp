<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="java.io.*"%>
<%@page import="edu.stanford.muse.webapp.*"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@page import="edu.stanford.muse.Config"%>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>

<html>
<head>

    <link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <link rel="stylesheet" href="css/jquery.qtip.min.css">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="css/css.jsp"/>

<%-- The jquery was present here earlier --%>
    <script src="js/jquery.js"></script>


    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/jquery.autocomplete.js" type="text/javascript"></script>
    <script src="js/jquery.dataTables.min.js"></script>
    <script type='text/javascript' src='js/jquery.qtip.min.js'></script>
    <script src="js/epadd.js" type="text/javascript"></script>
</head>

<body>
<%-- The headder.jspf file was included here --%>

<%@include file="header.jspf"%>

<title><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.head-collections")%></title>

<script src="js/collections.js" type="text/javascript"></script>

<%--<div style="width:1100px; margin:auto">
    <% if (ModeConfig.isProcessingMode()) { %>
        <h1><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.welcome-processing")%></h1>
  <% } else if (ModeConfig.isDeliveryMode()) { %>
        <h1><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.welcome-delivery")%></h1>
    <% } else if (ModeConfig.isDiscoveryMode()) { %>
        <h1><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.welcome-discover")%></h1>

    <% } else if (ModeConfig.isAppraisalMode()) { %>
        <div style="text-align:center"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.welcome-appraisal")%></div>
        <% return;
    }
  %>
</div>--%>

  <%--<div style="margin:auto;text-align:center">
  <div style="width:100%;text-align:left;">

    <br/>
  </div>
  <br/>
  <div>--%>



    <%
      //Check if the request is made for browsing collection or browsing institution via the query parameter.
        String browseType = request.getParameter("browse-type");
        if(Util.nullOrEmpty(browseType)){
            browseType="collection";//set it as default
        }
        if(browseType.toLowerCase().equals("collection")){%>
                <%@include file="browseCollections.jspf"%>
        <%}else{%>
                <%@include file="browseRepositories.jspf"%>
        <%}

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
            //Don't do anything. collections.js will query and load the collections.
      }
    %>
  </div>
  <%--<br/>
  <br/>
--%>
<script>
    var invalidPathMessage =  '<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.no-description")%>';
    var uploadImageErrorMessage = '<%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.no-description")%>';
    isProcessingMode = <%=ModeConfig.isProcessingMode()%>;
</script>
<%--Following three div swill be filled by the code in collections.js after fetching the collection details from the backend.
1. First one is for adding back-to-institution browse page when user clicks on an institution to browse collections by institutions.
2. Second one is for adding institutions's information.
3. Third one is for adding collection details (either tabular form - if browse by institution or tile form - if browse by collection)--%>

<div id="back-to-institution-browse" style="margin:auto;width:1100px;"></div>
<div id="institutionInfo" style="margin:auto;width:1100px;"></div>
<div id="collectionsInfo-header" style="margin:auto;width:1100px;"></div>
<div id="collectionsInfo-details" style="margin:auto;width:1100px;"></div>



  <div id="landingPhoto-upload-modal" class="info-modal modal fade" style="z-index:99999">
      <div class="modal-dialog">
          <div class="modal-content">
              <div class="modal-header">
                  <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                  <h4 class="modal-title"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.image.upload")%></h4>
              </div>
              <div class="modal-body">
                  <form id="uploadLandingPhotoForm" method="POST" enctype="multipart/form-data" >
                      <div class="form-group">
                          <input class="collectionID" name="collectionID" type="hidden" value="foobar"/>
                          <label for="landingPhoto" class="col-sm-2 control-label"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.image.file")%></label>
                          <div class="col-sm-10">
                              <input type="file" id="landingPhoto" name="landingPhoto" value=""/>
                          </div>
                      </div>
                      <%--<input type="file" name="correspondentCSV" id="correspondentCSV" /> <br/><br/>--%>

                  </form>
              </div>
              <div class="modal-footer">
                  <button class="upload-btn btn btn-cta"><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.image.upload-button")%> <i class="icon-arrowbutton"></i></button>


                  <%--<button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>--%>
                  <%--<button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>--%>
              </div>
          </div><!-- /.modal-content -->
      </div><!-- /.modal-dialog -->
  </div><!-- /.modal -->

  <jsp:include page="footer.jsp"/>
</body>
</html>
