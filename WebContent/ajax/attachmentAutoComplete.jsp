<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Collection"%><%@ page import="java.util.Set"%><%@ page import="java.util.LinkedHashSet"%>
<% 
	String query = request.getParameter("query");
	query = query.toLowerCase();
    if (query != null) {
        if (query.contains(";"))
            query = query.substring(query.lastIndexOf(";")+1);
        query = query.trim();
    }

    boolean extensions = request.getParameter("extensions") != null;

	JSONObject obj = new JSONObject();
	obj.put("query", query);

	JSONArray suggestions = new JSONArray();
	obj.put("suggestions",suggestions);

    int MAX_SUGGESTIONS = HTMLUtils.getIntParam(request, "MAX_SUGGESTIONS", 5);

    Archive archive = JSPHelper.getArchive(request);
    if (archive == null) {
        obj.put("status", 1);
        obj.put("error", "No archive in session");
        out.println (obj);
        JSPHelper.doLogging(obj);
        return;
    }

	if (!Util.nullOrEmpty(query) && archive != null) {
        Set<String>	blobNames = archive.getAllBlobNames();
        int suggestionCount = 0;
        Set<String> seen = new LinkedHashSet<>();
        if (blobNames != null) {
            for (String blobName: blobNames) {
                if (blobName == null)
                    continue;
                if (extensions)
                    blobName = Util.getExtension(blobName);
                if (blobName == null)
                    continue;
                if (blobName.toLowerCase().contains(query)) {
                    // avoid showing the same suggestion twice
                    if (seen.contains(blobName.toLowerCase()))
                        continue;
                    seen.add(blobName.toLowerCase());

                    JSONObject s = new JSONObject();
                    s.put("value", blobName);
                    s.put("name", blobName);
                    suggestions.put(s);
                    if (++suggestionCount > MAX_SUGGESTIONS)
                        break;
                }
            }
        }
	}
	response.getWriter().write(obj.toString());
%>