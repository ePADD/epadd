<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="edu.stanford.muse.ie.matching.NameExpansion"%>
<%@ page import="edu.stanford.muse.ie.matching.Matches"%>

<%
	response.setContentType("application/json; charset=utf-8");
	Archive archive = (Archive)request.getSession().getAttribute("archive");
	JSONObject result = new JSONObject();
	if (archive == null){
		result.put("result", "ePADD session expired? Please reload the archive.");
		out.println(result.toString(4));
		return;
	}

	String name = request.getParameter("name");
	String docId = request.getParameter("docId");
	EmailDocument ed = archive.docForId(docId);

	Matches matchResults = NameExpansion.getMatches (name, archive, ed, 5);
    out.println (matchResults.toJson());
%>