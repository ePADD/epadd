<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.io.*" %>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="org.apache.commons.io.FileUtils"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.epadd.Config"%>

<%
/* copies new accession into REPO_DIR and then loads it from there */
JSONObject result = new JSONObject();
String baseDir = request.getParameter("dir");
if (Util.nullOrEmpty(baseDir))
{
	result.put ("status", 1);
	result.put("error", "No directory specified");
	out.println (result.toString(4));
	return;
}

String archiveName = Util.filePathTailByPlatformSeparator(baseDir);
String targetDir = Config.REPO_DIR_PROCESSING + File.separator + archiveName;
if (!targetDir.equals(baseDir))
{
	try {
		FileUtils.copyDirectory(new File(baseDir), new File(targetDir), true /* preserve file date */);
		result.put("status", 0);
		result.put ("message", "Import completed successfully.");
	} catch (Exception e) {
		result.put("status", 1);
		result.put ("error", "Unable to import archive: " + e.getMessage());
	}
	out.println (result.toString(4));
}
%>
