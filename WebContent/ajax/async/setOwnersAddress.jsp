<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Collection"%><%@ page import="java.util.Set"%><%@ page import="java.util.LinkedHashSet"%><%@ page import="edu.stanford.muse.index.EmailDocument"%><%@ page import="java.util.Arrays"%><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%><%@ page import="edu.stanford.muse.ResultCacheManager.ResultCache"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.epadd.util.OperationInfo"%><%@ page import="edu.stanford.muse.email.StatusProvider"%><%@ page import="java.util.function.Consumer"%><%@ page import="edu.stanford.muse.email.StaticStatusProvider"%><%@ page import="java.io.IOException"%><%@ page import="org.apache.lucene.queryparser.classic.ParseException"%>

<%

{
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
            setOwneraddresses(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            cancelSetOwnerAddress(setStatusProvider);
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
<%! public void setOwneraddresses(Multimap<String,String> paramsMap, Consumer<StatusProvider> setStatusProvider,HttpSession session,JSONObject resultJSON){
   //---session.setAttribute("statusProvider", new StaticStatusProvider("Starting up..."));
	setStatusProvider.accept(new StaticStatusProvider("Starting up..."));

    Archive archive = JSPHelper.getArchive(paramsMap);

    if (archive == null) {
        resultJSON.put("status", 1);
        resultJSON.put("error", "No archive in session");
        //out.println (obj);
        JSPHelper.log.info(resultJSON);
        return;
    }
    String owneraddresses = JSPHelper.getParam(paramsMap,"ownersaddress");
    //split owneraddresses on semicolon
    String[] owneraddresses_split = owneraddresses.split(";");
    Set<String> owneraddressesSet=new LinkedHashSet<>();
    Arrays.stream(owneraddresses_split).forEach(s->owneraddressesSet.add(s));

    //return with error if any of the email address is wrong.
    for(String email: owneraddressesSet){
        if(archive.getAddressBook().lookupByEmail(email)==null){
            resultJSON.put("status", 1);
        resultJSON.put("error",email+" is not a valid email addres!! Please check.");
        //out.println (obj);
        JSPHelper.log.info(resultJSON);
        return;
        }
    }

    	setStatusProvider.accept(new StaticStatusProvider("Setting owner's address.."));


    //inovke setOwnerAddresses method of class EmailDocument.java
    EmailDocument.setOwners(archive,owneraddressesSet);//it also recomputes the summary


setStatusProvider.accept(new StaticStatusProvider("Recomputing address book.."));
EmailDocument.recomputeAddressBook(archive,new LinkedHashSet<>()); //because owner has changed and recomputation assumes owner as a trusted address. So we need to recompute it.
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
        resultJSON.put("message","Owner's addresses set successfully!");
        //out.println (obj);
        JSPHelper.log.info(resultJSON);

}

public void cancelSetOwnerAddress(Consumer<StatusProvider> setStatusProvider){

}
%>