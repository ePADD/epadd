<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*" %>
<%@page language="java" import="org.json.*"%>
<%@page language="java" %>

<%@page language="java" import="edu.stanford.muse.ie.Authority"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.index.IndexUtils"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page import="edu.stanford.muse.Config"%>
<%@ page import="edu.stanford.muse.ie.AuthorisedAuthorities"%>
<%

// updates the authority record assignments en masse. Individual records are not updated directly.

JSONObject result = new JSONObject();

Archive archive = JSPHelper.getArchive(session);
if (archive == null) 
{
	result.put ("status", 1);
	result.put("error", "Please load an archive first.");
	out.println (result.toString(4));
	return;	
}

try {
	Map<String,Authority> cnameToAuthority = archive.getAuthorities();
	
	// the input "authorities" param is a json string like this:
	// {names: ['n1', 'n2'], ids: ['f1', 'f2'], types: ['fast', 'viaf']}
	// {'n1': [[id1,type1],[id2,type2]], 'n2': {...} ...}
	// the names and ids fields must be arrays of the same length
	String json = request.getParameter("authorities");
	String unauth = request.getParameter("reverted");
	JSONArray obj2 = new JSONArray(unauth);
	JSONObject obj = new JSONObject(json);
	Iterator<?> names = obj.keys();
	int numEntries = 0; 
	
	while(names.hasNext())
	{
		String name = (String) names.next();
		List<String> ids = new ArrayList<String>(),types = new ArrayList<String>();
		JSONArray sources = obj.getJSONArray(name);
		JSONArray sn = (JSONArray)sources.get(1);
		JSONArray l = (JSONArray)sources.get(0);
		for(int i=0;i<sn.length();i++){
			ids.add(l.getString(i));
			types.add(sn.getString(i));	
			JSPHelper.log.info("Adding id: "+l.getString(i)+", type: "+sn.getString(i));
		}
		String cname = IndexUtils.canonicalizeEntity(name);
		cnameToAuthority.put(cname, new Authority(cname,ids.toArray(new String[ids.size()]),types.toArray(new String[types.size()])));
		numEntries++;
	}
	int numRemoved = 0;
	for(int i=0;i<obj2.length();i++){
		String cname = IndexUtils.canonicalizeEntity(obj2.getString(i));
		if(cnameToAuthority.containsKey(cname)){
			JSPHelper.log.info("Removing: "+cname+" from authorised authorities");
			cnameToAuthority.remove(cname);
			numRemoved++;
		} else{
		    JSPHelper.log.warn(obj2.toString()+" does not have an authority assigned");
		}
	}
	
	String filename = archive.baseDir + File.separator + Config.AUTHORITIES_FILENAME;
	
	Util.writeObjectToFile (filename, (Serializable) cnameToAuthority);
	
	result.put ("status", 0);
	result.put("message", "Successfully assigned " + numEntries + " authority records" + " and removed " + numRemoved + " records");
	out.println (result.toString(4));
	return;
} catch (Exception e) {
	e.printStackTrace();
	result.put ("status", 3);
	result.put("error", "Could not update authority records: " + e.getMessage());
	out.println (result.toString(4));
}
%>
