<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="edu.stanford.muse.webapp.ModeConfig"%>
<%@ page import="edu.stanford.muse.Config"%>
<%@ page import="java.io.File"%>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="edu.stanford.muse.webapp.SimpleSessions"%><%@ page import="edu.stanford.muse.util.Util"%>
<%

String repositoryName= request.getParameter("repositoryName");
//If repository name is non null then return the information only for this repository else return the collection details for all repositories.
    JSONArray collections = new JSONArray();
	 String modeBaseDir = "";
      if (ModeConfig.isProcessingMode())
          modeBaseDir = Config.REPO_DIR_PROCESSING;
      else if (ModeConfig.isDeliveryMode())
        modeBaseDir = Config.REPO_DIR_DELIVERY;
      else if (ModeConfig.isDiscoveryMode())
        modeBaseDir = Config.REPO_DIR_DISCOVERY;

      File topFile = new File(modeBaseDir);
        JSPHelper.doLogging("Reading collections from: " + modeBaseDir);
        if (!topFile.exists() || !topFile.isDirectory() || !topFile.canRead()) {
            JSONObject obj = new JSONObject();
            obj.put("status", 1);
            obj.put("error", "Please place some archives in " + modeBaseDir);
            out.println (obj);
            JSPHelper.doLogging(obj);
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
                      if(!Util.nullOrEmpty(repositoryName) && !repositoryName.toLowerCase().equals(cm.repository.toLowerCase()))
                          continue; //Means this repository is of no interest. Continue to query the next repository.
                       collectionInfo = new JSONObject();

                      String fileParam = id + "/" + Archive.BAG_DATA_FOLDER+ "/" + Archive.IMAGES_SUBDIR + "/" + "landingPhoto"; // always forward slashes please
                      String url = "serveImage.jsp?file=" + fileParam;

                      //NOTE:When this ajax is called for processing mode home page display then some of these fields will not be available
                      //for those collections which were just exported from appraisal to processing mode. To display the tiles for those
                      //collections correctly we just add placeholder values for some of these fields after nullcheck.
                      if(cm.repository==null)
                          collectionInfo.put("repositoryName","Not Added");
                      else
                          collectionInfo.put("repositoryName",cm.repository);
                      if(cm.institution==null)
                          collectionInfo.put("institutionName","Please Add Institution Name");
                      else
                          collectionInfo.put("institutionName",cm.institution);
                      collectionInfo.put("dataDir",id); //This will always be present in the colleciotn metadata (even when the collection has just been exported from appraisal to processing)
                      collectionInfo.put("landingImageURL",url); //This will always be present in the colleciotn metadata (even when the collection has just been exported from appraisal to processing)

                      if(cm.shortTitle==null){//if short title is null then put ownername if that is non-null else just put "Not added" string.
                          if(cm.ownerName==null)
                              collectionInfo.put("shortTitle","Not Added");
                          else
                              collectionInfo.put("shortTitle",cm.ownerName);
                          }
                      else
                          collectionInfo.put("shortTitle",cm.shortTitle);

                      if(cm.shortDescription==null)
                          {
                               if(cm.ownerName==null)
                                    collectionInfo.put("shortDescription","Not Added");
                               else
                                    collectionInfo.put("shortDescription",cm.ownerName);
                          }
                          }
                      else
                        collectionInfo.put("shortDescription",cm.shortDescription);

                  if(collectionInfo!=null)
                              collections.put(collectionInfo);
        }
    }
}

response.getWriter().write(collections.toString());

%>
