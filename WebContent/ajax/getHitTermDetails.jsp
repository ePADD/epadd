<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.lens.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%
JSPHelper.setPageUncacheable(response);
	/** returns details for a single hit term */
// https://developer.mozilla.org/en/http_access_control
response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
//response.setHeader("Access-Control-Allow-Origin", "http://xenon.stanford.edu");
response.setHeader("Access-Control-Allow-Credentials", "true");
response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");

JSONObject result = new JSONObject();
try {	
	response.setContentType("application/json; charset=utf-8");
	
	String term = request.getParameter("term");	
	String pageScoreStr = request.getParameter("pageScore");
	float pageScore = 0.0f;
	try { pageScore = Float.parseFloat(pageScoreStr); } catch (Exception e) { }
	
	String baseURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();


	Archive archive = JSPHelper.getArchive(request);
	if (archive == null) {
	    JSONObject obj = new JSONObject();
	    obj.put("status", 1);
	    obj.put("error", "No archive in session");
	    out.println (obj);
	    JSPHelper.doLogging(obj);
	    return;
	}

	AddressBook ab = archive.addressBook;
	String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
	//session emailDocs is set only when filter is in operation and the case of alldocs being null has to be considered.
	List<EmailDocument> allDocs;
        if(JSPHelper.getSessionAttribute(session, "emailDocs") != null){
                allDocs = (List<EmailDocument>) JSPHelper.getSessionAttribute(session, "emailDocs");
        }
        else{
                allDocs = new ArrayList<EmailDocument>();
                List<Document> allDocsList = archive.getAllDocs();
                for(Document d: allDocsList)
                           allDocs.add((EmailDocument)d);
         }	

	Indexer indexer = archive.indexer;
	
	if (Util.nullOrEmpty(term))
	{
		result.put("displayError", "null or empty ref-text");
		return;
	}

	LensPrefs lensPrefs = (LensPrefs) JSPHelper.getSessionAttribute(session, "lensPrefs");
	if (lensPrefs == null)
	{
		String cacheDir = (String) JSPHelper.getSessionAttribute(session, "cacheDir");
		if (cacheDir != null)
		{
	lensPrefs = new LensPrefs(cacheDir);
	session.setAttribute("lensPrefs", lensPrefs);
		}
	}
	
	Object obj = null;
	try {
		long start = System.currentTimeMillis();
		//list = Lens.getHits (names, lensPrefs, indexer, ab, baseURL, allDocs);
		long end = System.currentTimeMillis();
		//JSPHelper.doConsoleLogging ("normal get hits " + (end - start) + " ms");
		start = end;
		if(allDocs == null){
			   JSPHelper.doLoggingWarnings("Alldocs object is null");
		}
		obj = Lens.detailsForTerm (term, pageScore, archive, ab, baseURL, allDocs);
		((JSONObject) obj).put("url", "/epadd/browse?archiveID="+archiveID+"&adv-search=1&termBody=on&termSubject=on&termAttachments=on&termOriginalBody=on&term=\"" + term + "\"");
		end = System.currentTimeMillis();
		//JSPHelper.doConsoleLogging ("quick get hits " + (end - start) + " ms");
	} catch (Exception e) {
		JSPHelper.doLoggingWarnings ("Exception getting lens hits " + e);
		Util.print_exception(e, JSPHelper.log);
	}

	result.put("result", obj);
	if (Lens.log.isDebugEnabled()) {
		Lens.log.debug (result.toString(4));
	}
} catch (Exception e) {
	result.put("error", "Exception: " + e);
	Util.print_exception (e, JSPHelper.log);
} catch (Error e) {
	// stupid abstract method problem on jetty shows up as an error not as exception
	result.put("error", "Error: " + e);
	Util.print_exception (e, JSPHelper.log);
} finally { 
	out.println (result.toString(4));
}
%>
