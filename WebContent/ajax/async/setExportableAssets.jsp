<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="org.json.JSONArray" %><%@ page import="org.json.JSONObject"%><%@ page import="org.json.CDL"%><%@ page import="org.apache.commons.io.FileUtils"%><%@ page import="au.com.bytecode.opencsv.CSVWriter"%><%@ page import="java.util.*"%><%@ page import="edu.stanford.muse.ner.model.NEType"%><%@ page import="edu.stanford.muse.index.*"%><%@ page import="java.io.*"%><%@ page import="java.util.zip.GZIPOutputStream"%><%@ page import="java.util.zip.ZipOutputStream"%><%@ page import="java.util.zip.ZipEntry"%><%@ page import="edu.stanford.muse.util.EmailUtils"%><%@ page import="edu.stanford.muse.webapp.ModeConfig"%><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%><%@ page import="edu.stanford.muse.email.StatusProvider"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="java.util.function.Consumer"%><%@ page import="edu.stanford.epadd.util.OperationInfo"%><%@ page import="edu.stanford.muse.email.StaticStatusProvider"%>
<%

//This api needs to be supported for both types of flows - long running with status bar and normal (without status bar). Theso two invocation types of this jsp will be identified
//by the presence of operation ID (opID field) in the request. If it is being invoked as a long running operation then the front end will use fetch_page_progress_bar method to invoke it
//which in turn will provide it with an operation ID.

    Archive archive = ArchiveReaderWriter.prepareAndLoadDefaultArchive(request);

    Multimap<String,String> paramMap = JSPHelper.convertRequestToMap(request);
     //String appURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
     String appURL="";
String opID = request.getParameter("opID");

if(opID==null){
    //invoke this as normal ajax call without using operation Info object.
    JSONObject result = new JSONObject();
    setExportableAssets(paramMap,null,session,result, archive);
    out.println(result);
}else{
    //means it is being invoked as a long running operation using fetch_page_progress api in js. use OperationInfo protocol to invoke it.
//<editor-fold desc="Setting up the operation object to execute this operation asynchronously">
    //get the operation ID from the request parameter.
    String encoding = request.getCharacterEncoding();
    JSPHelper.log.info("request parameter encoding is " + encoding);

    String actionName = request.getRequestURI();

    //create a new operation object with the information necessary to run this long running async task.
    final HttpSession fsession = session;
    OperationInfo opinfo = new OperationInfo(actionName,opID,paramMap) {
        @Override
        public void onStart(JSONObject resultJSON) {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            setExportableAssets(this.getParametersMap(),setStatusProvider,fsession,resultJSON, archive);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            cancelGetNormalizedAsset(setStatusProvider);
        }
    };

    JSPHelper.setOperationInfo(session,opID,opinfo);

    opinfo.run();

    JSONObject obj = new JSONObject();
    out.println(obj);
}

%>

<%!
private void setExportableAssets(Multimap<String,String> params, Consumer<StatusProvider> setStatusProvider, HttpSession session, JSONObject resultJSON, Archive archive){
    if(setStatusProvider!=null)
        setStatusProvider.accept(new StaticStatusProvider("setting exportable assets..."));

    String exportableAssets = JSPHelper.getParam(params,"exportableAssets");
    String exportableAssetsFiles = JSPHelper.getParam(params,"exportableAssetsFiles");
	//Archive archive = JSPHelper.getArchive(params);

	 if (archive == null) {
        resultJSON.put("-Err-", "No archive in session");
        resultJSON.put("status", 0);
        // The status should be 1, but in processing mode entering the archive is stopped altogether and the user is stuck
        // on the start page.
        //out.println (result);
        JSPHelper.log.info(resultJSON);
        return;
    }

	 String error="";
    JSONObject exportResult = new JSONObject();

    ArrayList<String> assetsLocation = new ArrayList<String>();

    if (!Util.nullOrEmpty(exportableAssetsFiles)){
        assetsLocation = new ArrayList(Arrays.asList(exportableAssetsFiles.split("\\^-\\^")));
    }

     if ("exportAcquisitioned".equals(exportableAssets)){
        System.out.println("exportableAssets = exportAcquisitioned");
        // Notes: assetsLocation here should be a list of full path filenames
        exportResult = archive.setExportableAssets(Archive.Exportable_Assets.EXPORTABLE_APPRAISAL_CANONICAL_ACQUISITIONED, assetsLocation);
        exportResult = archive.setExportableAssets(Archive.Exportable_Assets.EXPORTABLE_APPRAISAL_NORMALIZED_ACQUISITIONED, assetsLocation);
     } else if ("exportAppraised".equals(exportableAssets)){
        System.out.println("exportableAssets = exportAppraised");
        exportResult = archive.setExportableAssets(Archive.Exportable_Assets.EXPORTABLE_APPRAISAL_NORMALIZED_APPRAISED);
     } else if ("exportProcessing".equals(exportableAssets)){
         System.out.println("exportableAssets = exportProcessing");
        exportResult = archive.setExportableAssets(Archive.Exportable_Assets.EXPORTABLE_PROCESSING_NORMALIZED);
     } else if ("exportAccessionProcessing".equals(exportableAssets)){
        System.out.println("exportableAssets = exportAccessionProcessing");
        // Notes: assetsLocation here should be a list of folder paths
        exportResult = archive.setExportableAssets(Archive.Exportable_Assets.EXPORTABLE_PROCESSING_NORMALIZED, assetsLocation);
     } else if ("exportProcessed".equals(exportableAssets)){
        System.out.println("exportableAssets = exportProcessed");
        exportResult = archive.setExportableAssets(Archive.Exportable_Assets.EXPORTABLE_PROCESSING_NORMALIZED_PROCESSED);
     }

        if (!Util.nullOrEmpty(error)){
            resultJSON.put("status", 1);
            resultJSON.put("error", error);
        } else {
            resultJSON.put("status", 0);
            resultJSON.put("responseText","Exportable assets are ready!");
        }
}
%>

<%!
public void cancelGetNormalizedAsset(Consumer<StatusProvider> setStatusProvider){

}
%>

