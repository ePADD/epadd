<%@page language="java" contentType="application/json; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="org.json.JSONObject"%>
<%@page language="java" %>
<%

	if (!session.isNew()) {
		session.removeAttribute("userKey");
		session.removeAttribute("emailDocs");
		session.removeAttribute("archive");
		// cache dir?

		session.removeAttribute("museEmailFetcher");
		session.removeAttribute("statusProvider");
		
		JSPHelper.log.info ("session invalidated");
	//	session.invalidate();
	}
	else
	{
		JSPHelper.log.info ("session already invalidated");
		out.println ("session already invalidated");
	}

	JSONObject result = new JSONObject();
	result.put("status", 0);
	result.put("message", "Session ended");
	out.println (result.toString());
//	session.removeAttribute("mode");
%>
