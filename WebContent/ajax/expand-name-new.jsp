<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="edu.stanford.muse.ie.matching.NameExpansion"%>
<%@ page import="edu.stanford.muse.ie.matching.Matches"%><%@ page import="edu.stanford.muse.webapp.JSPHelper"%>

<%
	response.setContentType("application/json; charset=utf-8");

	Archive archive = JSPHelper.getArchive(session,request);
	if (archive == null) {
	    JSONObject obj = new JSONObject();
	    obj.put("status", 1);
	    obj.put("error", "No archive in session");
	    out.println (obj);
	    JSPHelper.log.info(obj);
	    return;
	}


	String name = request.getParameter("name");
	String docId = request.getParameter("docId");
	EmailDocument ed = archive.docForId(docId);

	Matches matchResults = NameExpansion.getMatches (name, archive, ed, 5);
    out.println (matchResults.toJson());
%>