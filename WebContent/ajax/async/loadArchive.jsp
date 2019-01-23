<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.email.StaticStatusProvider" %>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="org.json.JSONObject"%>
<%@page language="java" import="java.io.File"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.epadd.util.OperationInfo"%><%@ page import="edu.stanford.muse.email.StatusProvider"%><%@ page import="java.util.function.Consumer"%>
<%


    String actionName = request.getRequestURI();
    String opID = request.getParameter("opID");
    Multimap<String,String> paramMap = JSPHelper.convertRequestToMap(request);
    //create a new operation object with the information necessary to run this long running async task.
    final HttpSession fsession = session;
    OperationInfo opinfo = new OperationInfo(actionName,opID,paramMap) {
        @Override
        public void onStart(JSONObject resultJSON) {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            loadArchive(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            cancelLoadArchive(setStatusProvider);
        }
    };

    //</editor-fold>

    //<editor-fold desc="Store this operation in global map so that others can access this operation">
    /*Map<String,OperationInfo> operationInfoMap = (Map<String,OperationInfo>) session.getAttribute("operationInfoMap");
    if(operationInfoMap==null)
        operationInfoMap = new LinkedHashMap<>();
    operationInfoMap.put(opID,opinfo);*/
    JSPHelper.setOperationInfo(session,opID,opinfo);
    //</editor-fold>

    //<editor-fold desc="Starting the operation">
    opinfo.run();
    //when canelling this operation, from cancel.jsp call opinfo.cancel() method.
    //when getting the status of this operation, call opinfo.getStatusProvider().getStatus() method.
    //</editor-fold>
    //just send an empty response telling that the operation has been started.
    JSONObject obj = new JSONObject();
    out.println(obj);

    %>

<%!
public void loadArchive(Multimap<String,String> params, Consumer<StatusProvider> setStatusProvider, HttpSession session, JSONObject resultJSON){

        setStatusProvider.accept(new StaticStatusProvider("Loading archive"));
        // note: though this page can be invoked from fetch_page_with_progress(), in the result json, we don't have a resultPage to go to --
        // the caller of fetch_page_with_progress() should decide where to redirect to after the archive is loaded

        String dir = JSPHelper.getParam(params,"dir");
        Boolean isEditAccessionNextScreen = JSPHelper.getParam(params,"editscreen") != null;
        if (Util.nullOrEmpty(dir))
        {
            resultJSON.put ("status", 1);
            resultJSON.put("error", "No directory specified");
            //out.println (result.toString(4));
            return;
        }

        dir = dir + java.io.File.separator;
        if (ModeConfig.isProcessingMode())
          dir = edu.stanford.muse.Config.REPO_DIR_PROCESSING + File.separator + dir;
        else if (ModeConfig.isDeliveryMode())
            dir = edu.stanford.muse.Config.REPO_DIR_DELIVERY + File.separator + dir;
        else if (ModeConfig.isDiscoveryMode())
            dir = edu.stanford.muse.Config.REPO_DIR_DISCOVERY + File.separator + dir;
        JSPHelper.log.info("Loading archive from: "+dir);

        Archive archive = ArchiveReaderWriter.readArchiveIfPresent(dir);
        if (archive == null)
        {
            resultJSON.put ("status", 2);
            resultJSON.put("error", "No archive found in directory: " + dir);
            //out.println (result.toString(4));
            return;
        }
        //by this time the archive must have an archiveID present in SimpleSession mapper.
        String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
        assert archiveID!=null : new AssertionError("Some serious issue, the archive has been loaded by the readArchiveIfPresent method but the archiveID is absent in the mapper");

        String resultPage;
        if(isEditAccessionNextScreen)
            resultPage = "edit-collection-metadata?id="+dir+"&archiveID="+archiveID;
        else
            resultPage = "browse-top?archiveID="+archiveID;
        try {

		    int nDocs = archive.getAllDocs().size();
            AddressBook ab = archive.addressBook;
            String bestName = ab.getBestNameForSelf();
            resultJSON.put("status", 0);
            resultJSON.put ("owner", bestName);
            resultJSON.put ("nDocs", nDocs);
            resultJSON.put("resultPage", resultPage);
            //out.println (result.toString(4));
            return;
        } catch (Exception e) {
            resultJSON.put ("status", 3);
            resultJSON.put("error", "Could not read the archive found in directory: " + dir);
            //out.println (result.toString(4));
        }
		//session.removeAttribute("statusProvider");
}
%>

<%!
public void cancelLoadArchive(Consumer<StatusProvider> setStatusProvider){

}
%>
