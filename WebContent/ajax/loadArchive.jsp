<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.email.StaticStatusProvider" %>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.SimpleSessions"%>
<%@page language="java" import="org.json.JSONObject"%>
<%@page language="java" import="java.io.File"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig"%>
<%

        session.setAttribute("statusProvider", new StaticStatusProvider("Loading archive"));
        // note: though this page can be invoked from fetch_page_with_progress(), in the result json, we don't have a resultPage to go to --
        // the caller of fetch_page_with_progress() should decide where to redirect to after the archive is loaded

        JSONObject result = new JSONObject();
        String dir = request.getParameter("dir");
        Boolean isEditAccessionNextScreen = request.getParameter("editscreen") == null? false:true;
        if (Util.nullOrEmpty(dir))
        {
            result.put ("status", 1);
            result.put("error", "No directory specified");
            out.println (result.toString(4));
            return;
        }

        dir = dir + java.io.File.separator; //  + "user"; // wish this was cleaner
        if (ModeConfig.isProcessingMode())
          dir = edu.stanford.muse.Config.REPO_DIR_PROCESSING + File.separator + dir;
        else if (ModeConfig.isDeliveryMode())
            dir = edu.stanford.muse.Config.REPO_DIR_DELIVERY + File.separator + dir;
        else if (ModeConfig.isDiscoveryMode())
            dir = edu.stanford.muse.Config.REPO_DIR_DISCOVERY + File.separator + dir;
        JSPHelper.log.info("Loading archive from: "+dir);

        Archive archive = SimpleSessions.readArchiveIfPresent(dir);
        if (archive == null)
        {
            result.put ("status", 2);
            result.put("error", "No archive found in directory: " + dir);
            out.println (result.toString(4));
            return;
        }
        //by this time the archive must have an archiveID present in SimpleSession mapper.
        String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
        assert archiveID!=null : new AssertionError("Some serious issue, the archive has been laoded by the readArchiveIfPresent method but the archiveID is absent in the mapper");

        String resultPage;
        if(isEditAccessionNextScreen)
            resultPage = "edit-collection-metadata?id="+dir+"&archiveID="+archiveID;
        else
            resultPage = "browse-top?archiveID="+archiveID;
        try {

		    int nDocs = archive.getAllDocs().size();
            AddressBook ab = archive.addressBook;
            String bestName = ab.getBestNameForSelf();
            result.put("status", 0);
            result.put ("owner", bestName);
            result.put ("nDocs", nDocs);
            result.put("resultPage", resultPage);
            out.println (result.toString(4));
            return;
        } catch (Exception e) {
            result.put ("status", 3);
            result.put("error", "Could not read the archive found in directory: " + dir);
            out.println (result.toString(4));
        }
		session.removeAttribute("statusProvider");

%>
