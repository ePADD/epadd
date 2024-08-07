<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Collection"%><%@ page import="java.util.Set"%><%@ page import="java.util.LinkedHashSet"%><%@ page import="edu.stanford.muse.index.EmailDocument"%><%@ page import="java.util.Arrays"%><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%><%@ page import="edu.stanford.muse.ResultCacheManager.ResultCache"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="edu.stanford.muse.email.StatusProvider"%><%@ page import="java.util.function.Consumer"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.epadd.util.OperationInfo"%><%@ page import="java.io.IOException"%><%@ page import="org.apache.lucene.queryparser.classic.ParseException"%><%@ page import="edu.stanford.muse.email.StaticStatusProvider"%>
<%

{
    //<editor-fold desc="Setting up the operation object to execute this operation asynchronously">
    //get the operation ID from the request parameter.
    String encoding = request.getCharacterEncoding();
    JSPHelper.doLogging("request parameter encoding is " + encoding);

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
            recomputeAddressBook(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            cancelRecomputeAddressBook(setStatusProvider);
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
}
%>
<%!
public void recomputeAddressBook(Multimap<String,String> paramsMap, Consumer<StatusProvider> setStatusProvider,HttpSession session,JSONObject resultJSON){
    Archive archive = JSPHelper.getArchive(paramsMap);
setStatusProvider.accept(new StaticStatusProvider("Starting up..."));

    if (archive == null) {
        resultJSON.put("status", 1);
        resultJSON.put("error", "No archive in session");
        //out.println (obj);
        JSPHelper.doLogging(resultJSON);
        return;
    }
    String trustedaddrstring = JSPHelper.getParam(paramsMap,"trustedaddrs");
    //split trustedaddrs on semicolon
    String[] trustedaddrs = trustedaddrstring.split(";");
    Set<String> trustedaddrset=new LinkedHashSet<>();
    Arrays.stream(trustedaddrs).forEach(s->trustedaddrset.add(s));

        	setStatusProvider.accept(new StaticStatusProvider("Recomputing addressbook.."));

    //inovke recomputeaddressbook method of class EmailDocument.java
    AddressBook newaddressbook = EmailDocument.recomputeAddressBook(archive,trustedaddrset);
    if(newaddressbook==null){
        resultJSON.put("status", 1);
        resultJSON.put("error", "Unable to recompute the addressbook!");
        //out.println (obj);
        JSPHelper.doLogging(resultJSON);
    }else{
        //set this as current addressbook.,
        archive.setAddressBook(newaddressbook);
        try {
                        setStatusProvider.accept(new StaticStatusProvider("Recreating authorities.."));

archive.recreateCorrespondentAuthorityMapper(); // we have to recreate auth mappings since they may have changed
} catch (IOException e) {
    e.printStackTrace();
} catch (ParseException e) {
    e.printStackTrace();
}
            setStatusProvider.accept(new StaticStatusProvider("Finishing.."));

        ArchiveReaderWriter.saveAddressBook(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
        ArchiveReaderWriter.saveCorrespondentAuthorityMapper(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);

        //Archive.cacheManager.cacheCorrespondentListing(ArchiveReaderWriter.getArchiveIDForArchive(archive));
        resultJSON.put("status", 0);
        resultJSON.put("message","Addressbook reconstructed successfully!");
        //out.println (obj);
        JSPHelper.doLogging(resultJSON);

    }

}


public void cancelRecomputeAddressBook(Consumer<StatusProvider> setStatusProvider){

}
%>