<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="edu.stanford.muse.webapp.ModeConfig"%>
<%@ page import="edu.stanford.muse.Config"%>
<%@ page import="java.io.File"%>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="edu.stanford.muse.webapp.SimpleSessions"%><%@ page import="edu.stanford.muse.util.Util"%><%@ page import="edu.stanford.epadd.RepositoryInfo"%><%@ page import="java.util.LinkedHashMap"%><%@ page import="java.util.Map"%>
<%



String institutionName= request.getParameter("institutionName");
if(!Util.nullOrEmpty(institutionName)){
    //Return only the information related to this institution.
    //Fill the fields.. institutionName, logo url, address, information, phone, fax, email, website
    JSONObject collectionInfo = new JSONObject();
    collectionInfo.put("institutionName",institutionName);
    collectionInfo.put("logoURL","https://toppng.com/uploads/preview/stanford-university-logo-stanford-logo-11563198738mb4vawsk4c.png");
    collectionInfo.put("email","jschne@stanford.edu");
    collectionInfo.put("addressLine1","Green Library");
    collectionInfo.put("addressLine2","Green Library");
    collectionInfo.put("addressLine3","Stanford California 94305-6004");
    collectionInfo.put("phone","(650) 725-1161");
    collectionInfo.put("fax","(650) 723-8690");
    collectionInfo.put("website","http://library.stanford.edu/spc");

    response.getWriter().write(collectionInfo.toString());

    //and return;
    return;
}

//else return the information about all institutions..For now we only return institution name and the number of collection for each institution.
    JSONArray institutions = new JSONArray();
	 String modeBaseDir = "";
      if (ModeConfig.isProcessingMode())
          modeBaseDir = Config.REPO_DIR_PROCESSING;
      else if (ModeConfig.isDeliveryMode())
        modeBaseDir = Config.REPO_DIR_DELIVERY;
      else if (ModeConfig.isDiscoveryMode())
        modeBaseDir = Config.REPO_DIR_DISCOVERY;
/*
Read all collections and crerate a map of institution name, count of archives from that institution, institution address, Information about the institution.
 */
Map<String,RepositoryInfo> repositoryInfoMap = new LinkedHashMap<>();
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
                  if (cm != null) {
                       if(!Util.nullOrEmpty(cm.repository)){
                            if(!repositoryInfoMap.containsKey(cm.repository)){
                               repositoryInfoMap.put(cm.repository,new RepositoryInfo(cm.repository,cm.institution));
                            }
                            repositoryInfoMap.get(cm.repository).numberOfCollections+=1;
                            repositoryInfoMap.get(cm.repository).numberOfMessages+=cm.nIncomingMessages+cm.nOutgoingMessages;
                       }

                  }
        }
    }
    //Now iterate over repositoryInfoMap and fill in the json object with the information about institution and their collections.
    for(String institution: repositoryInfoMap.keySet()){
        JSONObject collectionInfo = new JSONObject();
        collectionInfo.put("institution",repositoryInfoMap.get(institution).institutionName);
        collectionInfo.put("repository",repositoryInfoMap.get(institution).repositoryName);
        collectionInfo.put("numMessages",repositoryInfoMap.get(institution).numberOfMessages);
        collectionInfo.put("numCollections",repositoryInfoMap.get(institution).numberOfCollections);
        institutions.put(collectionInfo);

    }

}

response.getWriter().write(institutions.toString());

%>
