<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="edu.stanford.muse.webapp.ModeConfig"%>
<%@ page import="edu.stanford.muse.Config"%>
<%@ page import="java.io.File"%>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="edu.stanford.muse.webapp.SimpleSessions"%>
<%


    JSONArray collections = new JSONArray();
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
            JSONObject obj = new JSONObject();
            obj.put("status", 1);
            obj.put("error", "Please place some archives in " + modeBaseDir);
            out.println (obj);
            JSPHelper.log.info(obj);
            return;

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
                  JSONObject collectionInfo = null;
                  if (cm != null) {
                       collectionInfo = new JSONObject();

                      String fileParam = id + "/" + Archive.BAG_DATA_FOLDER+ "/" + Archive.IMAGES_SUBDIR + "/" + "landingPhoto"; // always forward slashes please
                      String url = "serveImage.jsp?file=" + fileParam;

                      collectionInfo.put("dataDir",id);
                      collectionInfo.put("landingImageURL",url);
                      collectionInfo.put("shortTitle",cm.shortTitle);
                      collectionInfo.put("shortDescription",cm.shortDescription);
                  }
                  if(collectionInfo!=null)
                              collections.put(collectionInfo);
        }
    }
}

response.getWriter().write(collections.toString());

%>
