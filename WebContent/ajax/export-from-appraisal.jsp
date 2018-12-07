<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %><%@ page import="org.json.JSONObject"%><%@ page import="edu.stanford.muse.webapp.JSPHelper"%><%@ page import="edu.stanford.muse.util.Util"%>
<%--<%@include file="../getArchive.jspf" %>--%>
<%--<%@include file="../header.jspf"%>--%>

<%

if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
 		  session.removeAttribute("statusProvider");
Archive archive = JSPHelper.getArchive(request);

	JSONObject result= new JSONObject();
	 if (archive == null) {
        result.put("error", "No archive in session");
        result.put("status", 1);
        out.println (result);
        JSPHelper.log.info(result);
        return;
    }
AddressBook addressBook = archive.addressBook;
	String bestName = addressBook.getBestNameForSelf();
	//writeProfileBlock(out, archive, "Export archive");
	String rawDir = request.getParameter("dir");
	 String error="";

	/*
	List<String> pathTokens = Util.tokenize(dir, "\\/");
	dir = Util.join(pathTokens, File.separator);
	*/
	String dir = new File(rawDir).getAbsolutePath();

	File file = new File(dir);
	if (!file.isDirectory() || !file.canWrite()) {
		error = "Sorry, the directory " + dir + " is not writable. Please select a different directory.";
	}else{

		String folder = dir + File.separator + "ePADD archive of " + bestName;
		List<Document> docsToExport = new ArrayList<>();
		docsToExport = archive.getDocsForExport(Archive.Export_Mode.EXPORT_APPRAISAL_TO_PROCESSING);

			//From appraisal to processing we do not remove any label from labelManager so pass the current
			//label info map as an argument to set labelmanager for the exported archive.
		JSPHelper.log.info("Exporting #"+docsToExport.size()+" docs");
		// to do: need a progress bar here
		try {
			archive.export(docsToExport, Archive.Export_Mode.EXPORT_APPRAISAL_TO_PROCESSING, folder, "default",session);
		} catch (Exception e) {
			Util.print_exception ("Error trying to export archive", e, JSPHelper.log);
			error = "Sorry, error exporting archive: " + e + ". Please see the log file for more details.";
		}
	}
	if (!Util.nullOrEmpty(error)){
            result.put("status", 1);
            result.put ("error", error);
            						result.put("responseText", "Error in exporting archive.");
	    } else {
            result.put ("status", 0);
						result.put("responseText", "Operation successful..");
	}
	out.println (result.toString(4));
session.removeAttribute("statusProvider");

%>
	<%--</div>
<script>$('#spinner-div').hide();</script>
</body>
</html>--%>
