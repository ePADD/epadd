<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page language="java" import="org.json.JSONObject"%><%@ page import="edu.stanford.muse.index.Archive"%>
<%
// create or edit label endpoint

JSONObject result = new JSONObject();
Archive archive = JSPHelper.getArchive(request);
        if (archive == null)
        {
            result.put ("status", 2);
            result.put("error", "No archive found for archive ID =  " + request.getParameter("archiveID"));
            out.println (result.toString(4));
            return;
        }

        out.println (JSPHelper.createOrEditLabels(archive,request));

%>
