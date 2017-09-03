<%@page language="java" contentType="application/json; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%><%@ page import="org.json.JSONObject"%>
<%@page language="java" %>
<%
	JSONObject result = new JSONObject();
    Archive archive = JSPHelper.getArchive(session,request);
    if (archive == null) {
        JSONObject obj = new JSONObject();
        obj.put("status", 1);
        obj.put("error", "No archive in session");
        out.println (obj);
        JSPHelper.log.info(obj);
        return;
    }


	{
      	String baseDir = archive.baseDir;
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
        archive.close();
        // clearCache is really "delete the directory".
        archive.clearCache(baseDir, null);
        result.put("status", 0);
        result.put("message", "Archive deleted.");
    }
	out.println (result.toString());
//	session.removeAttribute("mode");
%>