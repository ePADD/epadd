<%
/*    
    2022-10-28      Allow custom naming in Export to next ePADD module
*/
%>
<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %><%@ page import="org.json.JSONObject"%><%@ page import="edu.stanford.muse.webapp.JSPHelper"%><%@ page import="edu.stanford.muse.util.Util"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.epadd.util.OperationInfo"%><%@ page import="edu.stanford.muse.email.StatusProvider"%><%@ page import="java.util.function.Consumer"%>
<%--<%@include file="../getArchive.jspf" %>--%>
<%--<%@include file="../header.jspf"%>--%>

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
            exportFromAppraisal(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            cancelExportFromAppraisal(setStatusProvider);
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
<%! public void exportFromAppraisal(Multimap<String,String> params, Consumer<StatusProvider> setStatusProvider, HttpSession session, JSONObject resultObject){

//    if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
// 		  session.removeAttribute("statusProvider");
	Archive archive = JSPHelper.getArchive(params);

	 if (archive == null) {
        resultObject.put("error", "No archive in session");
        resultObject.put("status", 1);
        JSPHelper.log.info(resultObject);
        return;
    }
	AddressBook addressBook = archive.addressBook;
	String bestName = addressBook.getBestNameForSelf();
	//writeProfileBlock(out, archive, "Export archive");
	String rawDir = JSPHelper.getParam(params,"dir");
	 String error="";
// 2022-10-28
        String archive_name = JSPHelper.getParam(params,"archive_name");

	/*
	List<String> pathTokens = Util.tokenize(dir, "\\/");
	dir = Util.join(pathTokens, File.separator);
	*/
	String dir = new File(rawDir).getAbsolutePath();

	File file = new File(dir);
	if (!file.isDirectory() || !file.canWrite()) {
		error = "Sorry, the directory " + dir + " is not writable. Please select a different directory.";
	}else{
// 2022-10-28
//		String folder = dir + File.separator + "ePADD archive of " + bestName;
        String folder;
        if (archive_name != null && archive_name != "")
		{
			folder = dir + File.separator + archive_name;
		}
        else
		{
			folder = dir + File.separator + "ePADD archive of " + bestName;
		}
		List<Document> docsToExport = new ArrayList<>();
		docsToExport = archive.getDocsForExport(Archive.ExportMode.EXPORT_APPRAISAL_TO_PROCESSING);

			//From appraisal to processing we do not remove any label from labelManager so pass the current
			//label info map as an argument to set labelmanager for the exported archive.
		JSPHelper.log.info("Exporting #"+docsToExport.size()+" docs");
		// to do: need a progress bar here
		try {
			archive.export(docsToExport, Archive.ExportMode.EXPORT_APPRAISAL_TO_PROCESSING, folder, "default",setStatusProvider);
		} catch (Exception e) {
			Util.print_exception ("Error trying to export archive", e, JSPHelper.log);
			error = "Sorry, error exporting archive: " + e + ". Please see the log file for more details.";
		}
	}
	if (!Util.nullOrEmpty(error)){
            resultObject.put("status", 1);
            resultObject.put ("error", error);
            						resultObject.put("responseText", "Error in exporting archive.");
	    } else {
            resultObject.put ("status", 0);
						resultObject.put("responseText", "Operation successful..");
	}

	//out.println (result.toString(4));
//session.removeAttribute("statusProvider");
}
%>

<%! public void cancelExportFromAppraisal(Consumer<StatusProvider> setStatusProvider){

}
%>
	<%--</div>
<script>$('#spinner-div').hide();</script>
</body>
</html>--%>
