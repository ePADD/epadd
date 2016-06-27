<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.email.AddressBook"%>
<%@page import="edu.stanford.muse.email.Contact"%>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="java.util.Set" %><%@ page import="edu.stanford.muse.webapp.HTMLUtils"%><%@ page import="edu.stanford.muse.wpmine.Util"%>
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
		AddressBook addressBook = archive.addressBook;

        int suggestionCount = 0;
        outer:
		for (Contact c: addressBook.allContacts()){
			Set<String> names = c.names;
			if (names != null) {
				for (String name: names) {
					if (name.toLowerCase().contains(query)) {
						JSPHelper.log.info("Adding name: " + names);
						JSONObject s = new JSONObject();
						s.put("value", name);
						s.put("name", name);
						suggestions.put(s);
						if (++suggestionCount > MAX_SUGGESTIONS)
						    break outer;
					}
				}
			}

			Set<String> emails = c.emails;
			if (emails != null) {
				for (String email: c.emails) {
					if (email.toLowerCase().contains(query)) {
						JSPHelper.log.info("Adding name: " + names);
						JSONObject s = new JSONObject();
						s.put("value", email); // just get the first of the possibly many names
						s.put("name", email); // just get the first of the possibly many names
						suggestions.put(s);
						if (++suggestionCount > MAX_SUGGESTIONS)
						    break outer;
					}
				}
			}
		}
	}
	response.getWriter().write(obj.toString());
%>