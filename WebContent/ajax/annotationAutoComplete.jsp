<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Set"%>
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

	Archive archive = JSPHelper.getArchive(session,request);
	if (archive == null) {
	    obj.put("status", 1);
	    obj.put("error", "No archive in session");
	    out.println (obj);
	    JSPHelper.log.info(obj);
	    return;
	}

    if (!Util.nullOrEmpty(query) && archive != null) {
        Set<String>	annotations = archive.getAllAnnotations();
        int suggestionCount = 0;

        if (annotations != null) {
            for (String annotation: annotations) {
                if (annotation.toLowerCase().contains(query)) {
                    JSONObject s = new JSONObject();
                    s.put("value", annotation);
                    s.put("name", annotation);
                    s.put("annotation", annotation);
                    suggestions.put(s);
                    if (++suggestionCount > MAX_SUGGESTIONS)
                        break;
                }
            }
        }
	}
	response.getWriter().write(obj.toString());
%>