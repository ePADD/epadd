<%@ page language="java" contentType="text/html; UTF-8" pageEncoding="UTF-8"%>
<%@page import="edu.stanford.muse.ie.FASTRecord"%>
<%@page import="edu.stanford.muse.ie.FASTSearcher"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="org.json.JSONArray" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="java.util.Set" %>
<% 
	/* FAST matches suggest for a name*/
	String name = request.getParameter("query");
	String type = request.getParameter("type");
	JSPHelper.log.info("Searching for name: " + name + " and type: " + type);
	Set<FASTRecord> records = null;
	if(type == null)
		records = FASTSearcher.getMatches(name, FASTRecord.FASTDB.ALL,10);
	else if(type.equals("correspondent")||type.equals("person"))
		records = FASTSearcher.getMatches(name, FASTRecord.FASTDB.PERSON,10);
	else if(type.equals("org"))
		records = FASTSearcher.getMatches(name, FASTRecord.FASTDB.CORPORATE,10);
	else if(type.equals("places"))
		records = FASTSearcher.getMatches(name, FASTRecord.FASTDB.GEOGRAPHIC, 10);
	
	JSONObject obj = new JSONObject();
	obj.put("query",name);
	JSONArray suggestions = new JSONArray();
	for(FASTRecord rec: records){
		Set<String> names = rec.getNames();
		if (names==null)
			continue;
		JSPHelper.log.info("Adding name: " + names);
		String desc = FASTRecord.stringify(names);
		JSONObject s = new JSONObject();
		s.put("value", desc);
		s.put("fastID",rec.id);
		s.put("name",names.iterator().next()); // just get the first of the possibly many names
		suggestions.put(s);
	}
	obj.put("suggestions",suggestions);
	response.getWriter().write(obj.toString());
%>