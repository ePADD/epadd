<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.wpmine.Util" %><%@ page import="org.json.JSONArray"%><%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.LinkedHashSet"%><%@ page import="java.util.Set"%><%@ page import="edu.stanford.muse.xcoll.CrossCollectionSearch"%><%@ page import="java.util.List"%>
<% 
	String query = request.getParameter("query");
	query = query.toLowerCase();
    if (query != null) {
        if (query.contains(";"))
            query = query.substring(query.lastIndexOf(";")+1);
        query = query.trim();
    }

	JSONObject obj = new JSONObject();
	obj.put("query", query);

	JSONArray suggestions = new JSONArray();
	obj.put("suggestions",suggestions);

    int MAX_SUGGESTIONS = HTMLUtils.getIntParam(request, "MAX_SUGGESTIONS", 5);

	if (!Util.nullOrEmpty(query)) {
        int suggestionCount = 0;

        Set<String> seen = new LinkedHashSet<>();

        List<String> matchingEntities = CrossCollectionSearch.searchForAutocomplete (query, MAX_SUGGESTIONS);
        for (String e: matchingEntities) {
            String lowerCaseE = e.toLowerCase();
            if (!seen.contains(lowerCaseE)) {
                seen.add(lowerCaseE);
                JSONObject s = new JSONObject();
                s.put("value", e);
                s.put("name", e);
                suggestions.put(s);
                if (++suggestionCount > MAX_SUGGESTIONS)
                    break;
            }
        }
	}
	response.getWriter().write(obj.toString());
%>