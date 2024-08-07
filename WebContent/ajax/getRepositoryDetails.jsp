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



String repositoryName= request.getParameter("repositoryName");
if(!Util.nullOrEmpty(repositoryName)){
        JSONObject collectionInfo = new JSONObject();

        String logoURL="", email="", instituteName = "", addressLine1="", addressLine2="", addressLine3="", phone="", fax="", website="", instituteInfo = "";
    //Return only the information related to this institution.
    Map<String,String> repoDetails = Config.getRepoDetails(repositoryName);
    if(repoDetails!=null){
    //Fill the fields.. repositoryName, logo url, address, information, phone, fax, email, website
    //This data is stored in a file called epaddRepoDetails.txt in the home folder.
    logoURL = repoDetails.get("logoURL");
    email = repoDetails.get("email");
    addressLine1 = repoDetails.get("addressLine1");
    addressLine2 = repoDetails.get("addressLine2");
    instituteName = repoDetails.get("instituteName");
    phone = repoDetails.get("phone");
    fax  = repoDetails.get("fax");
    website = repoDetails.get("website");
    instituteInfo = repoDetails.get("instituteInfo");
    }

    //Now write all details in json object and return

    collectionInfo.put("repositoryName",repositoryName);
    collectionInfo.put("logoURL",logoURL);
    collectionInfo.put("email",email);
    collectionInfo.put("addressLine1",addressLine1);
    collectionInfo.put("addressLine2",addressLine2);
    collectionInfo.put("instituteName",instituteName);
    collectionInfo.put("phone",phone);
    collectionInfo.put("fax",fax);
    collectionInfo.put("website",website);
    collectionInfo.put("instituteInfo",instituteInfo);

    response.getWriter().write(collectionInfo.toString());

    //and return;
    return;
}
//If institution name is given then return all repositories related to this institution.
String institutionName = request.getParameter("institutionName");
//else return the information about all repositories..
    JSONArray repositories = new JSONArray();
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
                  if (cm != null) {
                      //if institution name is non null then process this collection only if it belongs to the given institution.
                      if(!Util.nullOrEmpty(institutionName)){
                          if(!cm.institution.toLowerCase().equals(institutionName.toLowerCase())){
                              continue; //skip this collection
                          }
                      }
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
        repositories.put(collectionInfo);

    }

}

response.getWriter().write(repositories.toString());

%>
