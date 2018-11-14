<%@page contentType="application/json; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="org.json.JSONObject"%>
<%

	JSONObject result = new JSONObject();
	result.put("status", 0);
	result.put("message", "Session ended");
	out.println (result.toString());
    //With the introduction of archiveID we decide not to remove the archive from the memory (globalMap)
    //Therefore immediately return from here.
	return;
	//However in future we can decide upon the action

/*
	if (!session.isNew()) {
		session.removeAttribute("userKey");
		session.removeAttribute("emailDocs");
		session.removeAttribute("archive");
		session.removeAttribute("cacheDir");
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
*/

//	session.removeAttribute("mode");
%>
