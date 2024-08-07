<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page import="edu.stanford.muse.AddressBookManager.CorrespondentAuthorityMapper"%><%@ page import="gov.loc.repository.bagit.verify.BagVerifier"%><%@ page import="java.io.IOException"%><%@ page import="gov.loc.repository.bagit.domain.Bag"%><%@ page import="gov.loc.repository.bagit.exceptions.*"%><%@ page import="edu.stanford.muse.util.Util"%><%@ page import="edu.stanford.muse.email.StaticStatusProvider"%><%@ page import="java.util.concurrent.Executors"%><%@ page import="java.nio.file.Files"%><%@ page import="java.io.File"%><%@ page import="edu.stanford.muse.email.StatusProvider"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="java.util.function.Consumer"%><%@ page import="edu.stanford.epadd.util.OperationInfo"%>
<%
// does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
JSPHelper.setPageUncacheable(response);
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
            verifyBagChecksum(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            canceclVerifyChecksum(setStatusProvider);
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
public void verifyBagChecksum(Multimap<String,String> params, Consumer<StatusProvider> setStatusProvider, HttpSession session, JSONObject resultJSON){
Archive archive = JSPHelper.getArchive(params);
if (archive == null) {
    resultJSON.put("status", 1);
    resultJSON.put("error", "No archive in session");
    //out.println (obj);
    JSPHelper.doLogging(resultJSON);
    return;
}

Bag bag = archive.getArchiveBag();
BagVerifier bv = new BagVerifier(Executors.newSingleThreadExecutor());
String errorMessage="";
        setStatusProvider.accept(new StaticStatusProvider("Verifying the checksum of archive..."));
        try {
            bv.isValid(bag, true);
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = "IO Exception:" + e.getMessage();
        } catch (MissingPayloadManifestException e) {
            e.printStackTrace();
            errorMessage = "Payload manifest missing: " + e.getMessage();
        } catch (MissingBagitFileException e) {
            e.printStackTrace();
            errorMessage = "Bagit file missing: " + e.getMessage();
        } catch (MissingPayloadDirectoryException e) {
            e.printStackTrace();
            errorMessage = "Payload directory missing: " + e.getMessage();
        } catch (FileNotInPayloadDirectoryException e) {
            e.printStackTrace();
            errorMessage = "No file in payload directory: " + e.getMessage();
        } catch (InterruptedException e) {
            e.printStackTrace();
            errorMessage = "Validation procedure interrupted :" + e.getMessage();
        } catch (MaliciousPathException e) {
            e.printStackTrace();
            errorMessage = "Malicious Path found: " + e.getMessage();
        } catch (CorruptChecksumException e) {
            e.printStackTrace();
            errorMessage = "Checksum is corrupted: " + e.getMessage();
        } catch (VerificationException e) {
            e.printStackTrace();
            errorMessage = "Can not verify the bag: " + e.getMessage();
        } catch (UnsupportedAlgorithmException e) {
            e.printStackTrace();
            errorMessage = "Algorithm used for constructing the bag is not supported: " + e.getMessage();
        } catch (InvalidBagitFileFormatException e) {
            e.printStackTrace();
            errorMessage = "Bag file format is invalid: " + e.getMessage();
        }
        if(Util.nullOrEmpty(errorMessage)){
            resultJSON.put("Status",0);
            resultJSON.put("resultPage","verify-bag.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&checkDone=true&result=success");
        }else{
            resultJSON.put("Status",1);
            resultJSON.put("resultPage","verify-bag.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&checkDone=true&result=failure");
        }
//out.println (result.toString());
        }
%>

<%!
public void canceclVerifyChecksum(Consumer<StatusProvider> setStatusProvider){

}
%>
