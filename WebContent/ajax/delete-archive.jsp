<%@page language="java" contentType="application/json; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%><%@ page import="org.json.JSONObject"%>
<%@page language="java" %>
<%
	Archive archive = JSPHelper.getArchive(session);
	JSONObject result = new JSONObject();
      if (archive == null) {
          result.put("status", 1);
          result.put("message", "No archive is currently loaded.");
      } else {
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