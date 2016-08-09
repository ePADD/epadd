<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.lens.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="java.util.Calendar"%>
<%@page language="java" import="java.text.SimpleDateFormat"%><%@ page import="edu.stanford.muse.ner.featuregen.FeatureDictionary"%><%@ page import="edu.stanford.muse.ner.tokenizer.CICTokenizer"%>
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
	Collection<EmailDocument> allDocs = (Collection<EmailDocument>) JSPHelper.getSessionAttribute(session, "emailDocs");
	//Indexer	indexer = archive.indexer;
	if (allDocs == null)
		allDocs = (Collection) archive.getAllDocs();
	
	if (Util.nullOrEmpty(text))
	{
		result.put("displayError", "Please provide some text.");
		return;
	}

	Map<String, Float> termFreqMap = new LinkedHashMap<>();
	List<JSONObject> list;
	
	long ner_start_millis = System.currentTimeMillis();

//it is not efficient to recognize entities every time, especially since it lags the page by a minute for the first loading of the page.
//The delay is due to loading of DBpedia into memory.
//	Pair<Map<Short, Map<String,Double>>, List<Triple<String, Integer, Integer>>> p = nerModel.find(text);
//	if(p!=null){
//	    Map<Short,Map<String,Double>> map = p.getFirst();
//	    if(map!=null){
//	        for(Short k: map.keySet()){
//	            if(FeatureDictionary.OTHER==k)
//                    continue;
//	            JSPHelper.log.info("Entity type: "+k+", "+map.get(k).size());
//	            for(String e: map.get(k).keySet()){
//	                if(map.get(k).get(e)>1.0E-4)
//	                names.add(new Pair<>(e,new Float(1.0)));
//	            }
//	        }
//	    }
//	}
    List<Triple<String,Integer,Integer>> tokens = new CICTokenizer().tokenize(text, false);
    //tokens.forEach(tok->termFreqMap.put(tok.getFirst(),termFreqMap.getOrDefault(tok.getFirst(),0f)+1f));
    for(Triple<String,Integer,Integer> tok: tokens)
        termFreqMap.put(tok.getFirst(),termFreqMap.getOrDefault(tok.getFirst(),0f)+1f);

    List<Pair<String,Float>> names = new ArrayList<>();
    //termFreqMap.entrySet().forEach(e->names.add(new Pair<>(e.getKey(),e.getValue())));
    for(Map.Entry e: termFreqMap.entrySet())
        names.add(new Pair<>((String)e.getKey(),(Float)e.getValue()));

	long ner_end_millis = System.currentTimeMillis();
	JSPHelper.log.info("NER time " + (ner_end_millis - ner_start_millis) + " ms");

	String DATE_FORMAT = "yyyyMMdd";
	JSPHelper.log.info(termFreqMap.size() + " unique name(s) identified");

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
		list = new ArrayList<>();
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
