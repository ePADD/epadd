<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.lens.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="java.util.Calendar"%>
<%@page language="java" import="java.text.SimpleDateFormat"%>
<%//Archive needs to be loaded since NER is archive dependant%>
<%@include file="../getArchive.jspf" %>
<%@include file="../getNERModel.jspf" %>

<%
JSPHelper.setPageUncacheable(response);
	// https://developer.mozilla.org/en/http_access_control
response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
response.setHeader("Access-Control-Allow-Credentials", "true");
response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");

JSONObject result = new JSONObject();
int callout_lines = HTMLUtils.getIntAttr(session, "lens.callout.lines", 3);
result.put("callout_lines", callout_lines);
try {	
	response.setContentType("application/json; charset=utf-8");
	
	String text = request.getParameter("refText");	
	String url = request.getParameter("refURL");

	String baseURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
	Archive archive = JSPHelper.getArchive(session);
	if (archive == null)
	{
		// display error is a message that can be displayed to the end user, i.e. something he can take action upon.
		result.put("displayError", "<a href=\"" + baseURL + "\">Load an archive into Muse</a>.");
		JSPHelper.log.warn ("No archive loaded for leadsAsJson");
		return;
	}
	Collection<EmailDocument> allDocs = (Collection<EmailDocument>) JSPHelper.getSessionAttribute(session, "emailDocs");
	Indexer	indexer = archive.indexer;
	if (allDocs == null)
		allDocs = (Collection) archive.getAllDocs();
	
	if (Util.nullOrEmpty(text))
	{
		result.put("displayError", "Please provide some text.");
		return;
	}

	List<Pair<String,Float>> names = new ArrayList<Pair<String,Float>>();
	List<Pair<String,Integer>> namesFromArchive = null;
	Map<String, Float> termFreqMap = new LinkedHashMap<String, Float>();
	List<JSONObject> list=null;
	
	boolean normalizeByLength = request.getParameter("normalizeByLength") != null;
	long ner_start_millis = System.currentTimeMillis();

	//names = NER.namesFromText(text, true, NER.defaultTokenTypeWeights, normalizeByLength, 1);
	Pair<Map<Short, List<String>>, List<Triple<String, Integer, Integer>>> p = nerModel.find(text);
	if(p!=null){
	    Map<Short,List<String>> map = p.getFirst();
	    if(map!=null){
	        for(Short k: map.keySet()){
	            JSPHelper.log.info("Entity type: "+k+", "+map.get(k).size());
	            for(String e: map.get(k)){
	                names.add(new Pair<String,Float>(e,new Float(1.0)));
	            }
	        }
	    }
	}
	//names = POS.namesFromPOS(text);
	long ner_end_millis = System.currentTimeMillis();
	JSPHelper.log.info("NER time " + (ner_end_millis - ner_start_millis) + " ms");

	String DATE_FORMAT = "yyyyMMdd";
	SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
	Calendar calendar = Calendar.getInstance();
	JSPHelper.log.info(names.size() + " unique name(s) identified");

	for (Pair<String, Float> pair: names)
	{
		String name = pair.getFirst();
		Float count = pair.getSecond();
		termFreqMap.put(name, 1.0f);
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
	try {
		long start = System.currentTimeMillis();
		//list = Lens.getHits (names, lensPrefs, indexer, ab, baseURL, allDocs);
		long end = System.currentTimeMillis();
		//JSPHelper.log.info ("normal get hits " + (end - start) + " ms");
		start = end;
		list = Lens.getHitsQuick (names, lensPrefs, archive, baseURL, allDocs);
		end = System.currentTimeMillis();
		//JSPHelper.log.info ("quick get hits " + (end - start) + " ms");
	} catch (Exception e) {
		JSPHelper.log.warn ("Exception getting lens hits " + e);
		Util.print_exception(e, JSPHelper.log);
		list = new ArrayList<JSONObject>();
	}
	JSPHelper.log.info (list.size() + " hits after sorting");

	JSONArray jsonArray = new JSONArray();
	int index = 0;
	for (JSONObject o: list)
		jsonArray.put(index++, o);

	result.put("results", jsonArray);
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
