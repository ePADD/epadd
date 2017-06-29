<%--
  Created by IntelliJ IDEA.
  User: chinmay
  Date: 29/6/17
  Time: 3:06 PM
  To change this template use File | Settings | File Templates.
--%>
<%@page language="java" contentType="application/json; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page import="org.json.JSONObject"%><%@ page import="edu.stanford.muse.webapp.SimpleSessions"%>
<%
JSONObject result = new JSONObject();

try{
        SimpleSessions.saveArchive(session);
        JSPHelper.log.info ("session saved");
	    result.put("status", 0);
	    result.put("message", "Session saved");
	    out.println (result.toString());
	}catch (Exception e) {
	    result.put ("status", 3);
	    result.put("error", "Could not save archive: " + e.getMessage());
	    out.println (result.toString(4));
}
%>
