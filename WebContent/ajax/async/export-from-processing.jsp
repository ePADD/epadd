<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.Config"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.Document"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.List" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %><%@ page import="org.json.JSONObject"%><%@ page import="edu.stanford.muse.email.StaticStatusProvider"%><%@ page import="edu.stanford.muse.index.Archive"%><%@ page import="edu.stanford.muse.email.StatusProvider"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.epadd.util.OperationInfo"%><%@ page import="java.util.function.Consumer"%><%@ page import="java.io.IOException"%>

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
            exportFromProcessing(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            cancelExportFromProcessing(setStatusProvider);
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

<%! public void exportFromProcessing(Multimap<String,String>params, Consumer<StatusProvider> setStatusProvider, HttpSession session, JSONObject resultObject){



//if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
// 		  session.removeAttribute("statusProvider");
Archive archive = JSPHelper.getArchive(params);

	 if (archive == null) {
        resultObject.put("error", "No archive in session");
        resultObject.put("status", 1);
        //out.println (result);
        JSPHelper.doLogging(resultObject);
        return;
    }
    AddressBook addressBook = archive.addressBook;
	String bestName = addressBook.getBestNameForSelf().trim();
	String rawDir = JSPHelper.getParam(params,"dir");
    boolean permLabelOnly = "on".equals(JSPHelper.getParam(params,"permLabelOnly"));
	String dir = new File(rawDir).getAbsolutePath();
	 String error="";

	File file = new File(dir);
	if (!file.isDirectory() || !file.canWrite()) {
		error = "Sorry, the directory " + dir + " is not writable. Please select a different directory.";
	}else{


	//JSPHelper.doConsoleLogging("Saving archive in " + archive.baseDir);
	try {
        ArchiveReaderWriter.saveArchive(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
	} catch (IOException e) {
        e.printStackTrace();
         resultObject.put("status", 1);
         resultObject.put ("error", error);
         resultObject.put("responseText", "Error in saving archive.");
         return;
    }

	String folder = dir + File.separator + "ePADD archive of " + bestName + "-Delivery";
	String folderPublic = dir + File.separator + "ePADD archive of " + bestName + "-Discovery";
	//same set of docs are exported from processing to delivery or discovery only difference being the
		//content of these messages. In case of processing to discovery mode the redaction takes place.
		System.out.println("export-from-processing: before call getDocsForExport: permLabelOnly = "+ permLabelOnly);  //debug
	List<Document> docsToExport = archive.getDocsForExport(Archive.ExportMode.EXPORT_PROCESSING_TO_DELIVERY, permLabelOnly);

	/*
	Before v5 we faced an issue where after exporting to delivery/discovery the document's content got
	changed because we are not actually copying the document/index document before making any changes (redaction)
	in their subject/body. One easy way to avoid this is to save the current archive in delivery/discovery mode,
	load it again and then make changes to that (redaction etc) and then saving it again. However, note that
	reading the archive from a directory will result in adding it's archive ID to global archiveID map which
	we don't want now [because we are loading it only temporarily].
	 *//*

	//save current archive to delivery directory
	// save baseDir temporarily and set it back after saving it in delivery/discovery folders.
	String tmpbasedir = archive.baseDir;
	archive.setBaseDir(folder);
	SimpleSessions.saveArchive(archive);
	//save current archive to discovery directory
	archive.setBaseDir(folderPublic);
	SimpleSessions.saveArchive(archive);
	//set baseDir back for the current archive.
	archive.setBaseDir(tmpbasedir);
	//At this point, both folder and folderpublic contain the processing mode archive. We need to read these archives
	//modify them according to the mode (delivery/discovery) and then save them back to their respective folders (folder/folderpublic)
	Archive fordelivery = SimpleSessions.readArchiveIfPresent(folder);*/
	//make sure to remove it from the global map once it's work is done
	// archive.collectionMetadata.entityCounts = null;
	archive.collectionMetadata.numPotentiallySensitiveMessages = -1;
	try {
		archive.export(docsToExport, Archive.ExportMode.EXPORT_PROCESSING_TO_DELIVERY, folder, "default",setStatusProvider);
//		//remove fordelivery archive from the global map
//		SimpleSessions.removeFromGlobalArchiveMap(folder,fordelivery);
	} catch (Exception e) {
		Util.print_exception ("Error trying to export archive", e, JSPHelper.log);
			error = "Sorry, error exporting archive: " + e + ". Please see the log file for more details.";
			resultObject.put("status", 1);
            resultObject.put ("error", error);
            resultObject.put("responseText", error);
            return;
	}


	// NOW EXPORT FOR DISCOVERY
	//Read archive from folderPublic directory and operate on that.
	//Archive forDeliveryPublic = SimpleSessions.readArchiveIfPresent(folderPublic);
	//With the introduction of 'Transfer only to Delivery' label, the set of docs exported to Delivery will not be same for Discovery.
	docsToExport = archive.getDocsForExport(Archive.ExportMode.EXPORT_PROCESSING_TO_DISCOVERY, permLabelOnly);
	JSPHelper.doLogging("Exporting for discovery");
	/*v6- why were we exporting correspondent authority file separately? Removed now.
		try {
        String csv = archive.getCorrespondentAuthorityMapper().getAuthoritiesAsCSV ();
        if (!Util.nullOrEmpty(csv)) {
            String filename = folder + File.separator + edu.stanford.muse.Config.AUTHORITIES_CSV_FILENAME;
            FileWriter fw = new FileWriter(new File(filename));
            fw.write(csv);
            fw.close();
        }
	} catch(Exception e) {
		out.println ("Warning: unable to write authorities CSV file into " + file);
		JSPHelper.doConsoleLoggingWarnings(e);
	}
	*/
	try {
        archive.export(docsToExport, Archive.ExportMode.EXPORT_PROCESSING_TO_DISCOVERY/* public mode */, folderPublic, "default",setStatusProvider);
    }catch (Exception e) {
		Util.print_exception ("Error trying to export archive", e, JSPHelper.log);
			error = "Sorry, error exporting archive: " + e + ". Please see the log file for more details.";
			resultObject.put("status", 1);
            resultObject.put ("error", error);
            resultObject.put("responseText", error);
            return;
	}
// Now remember to reload the archive from baseDir, because we've destroyed the archive in memory
		//after export make sure to load the archive again. However, readArchiveIfPresent will not read from
		//memroy if the archive and it's ID is already present in gloablArchiveMap (in SimpleSession).
		//Hence first remove this from the map and then call readArchiveIfPresent method.
		JSPHelper.doLogging("Reading back archive...");
		session.setAttribute("statusProvider", new StaticStatusProvider("Reloading saved archive..."));
		String baseDir = archive.baseDir;
		ArchiveReaderWriter.removeFromGlobalArchiveMap(baseDir,archive);

		ArchiveReaderWriter.readArchiveIfPresent(baseDir);
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

	/*archive.setBaseDir(baseDir);
	session.setAttribute("archive", archive);
	session.setAttribute("userKey", "user");
	session.setAttribute("cacheDir", archive.baseDir);
*/
//	archive.collectionMetadata.entityCounts = ec;

//authority records are exported to authority.csv in delivery mode
}
%>

<%! public void cancelExportFromProcessing(Consumer<StatusProvider> setStatusProvider){

}
%>
