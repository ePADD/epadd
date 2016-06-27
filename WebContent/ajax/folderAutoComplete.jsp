<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Collection"%><%@ page import="java.util.Set"%>
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

	Archive archive = (Archive) JSPHelper.getSessionAttribute(session, "archive");
	if (!Util.nullOrEmpty(query) && archive != null) {
        Set<String>	folders = EmailUtils.getAllFoldersInDocs((Collection) archive.getAllDocs());
        int suggestionCount = 0;
        if (folders != null) {
            for (String folder: folders) {
                if (folder.toLowerCase().contains(query)) {
                    JSONObject s = new JSONObject();
                    s.put("value", folder);
                    s.put("name", folder);
                    s.put("folder", folder);
                    suggestions.put(s);
                    if (++suggestionCount > MAX_SUGGESTIONS)
                        break;
                }
            }
        }
	}
	response.getWriter().write(obj.toString());
%>