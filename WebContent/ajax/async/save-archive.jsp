<%--
  Created by IntelliJ IDEA.
  User: chinmay
  Date: 29/6/17
  Time: 3:06 PM
  To change this template use File | Settings | File Templates.
--%>
<%@page language="java" contentType="application/json; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page import="org.json.JSONObject"%>
<%@ page import="edu.stanford.muse.index.Archive"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%>
<%@ page import="edu.stanford.muse.email.StaticStatusProvider"%>
<%@ page import="edu.stanford.muse.email.StatusProvider"%>
<%@ page import="com.google.common.collect.Multimap"%>
<%@ page import="java.util.function.Consumer"%>
<%@ page import="edu.stanford.epadd.util.OperationInfo"%>
<%
//<editor-fold desc="Setting up the operation object to execute this operation asynchronously">
    //get the operation ID from the request parameter.
    String encoding = request.getCharacterEncoding();
    JSPHelper.log.info("request parameter encoding is " + encoding);

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
            saveArchive(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            cancelSaveArchive(setStatusProvider);
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
public void saveArchive(Multimap<String,String> params, Consumer<StatusProvider> setStatusProvider, HttpSession session, JSONObject resultJSON){
setStatusProvider.accept( new StaticStatusProvider("Saving Archive..."));

Archive archive = JSPHelper.getArchive(params);
        if (archive == null)
        {
            resultJSON.put ("status", 2);
            resultJSON.put("error", "No archive found for archive ID =  " + JSPHelper.getParam(params,"archiveID"));
            //out.println (result.toString(4));
            return;
        }

try{
ArchiveReaderWriter.saveArchive(archive, Archive.Save_Archive_Mode.FRESH_CREATION);
       // ArchiveReaderWriter.saveMutable_Incremental(archive);
        JSPHelper.log.info ("session saved");
	    resultJSON.put("status", 0);
	    resultJSON.put("responseText", "Session saved");
	    //out.println (result.toString(5));
	    //session.removeAttribute("statusProvider");

	}catch (Exception e) {
	    resultJSON.put ("status", 3);
	    resultJSON.put("responseText", "Could not save archive: " + e.getMessage());
	    resultJSON.put("error", "Could not save archive: " + e.getMessage());
	    //out.println (result.toString(4));
	    //session.removeAttribute("statusProvider");
}
}
%>
<%!
public void cancelSaveArchive(Consumer<StatusProvider> setStatusProvider){

}
%>
