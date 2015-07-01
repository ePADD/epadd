<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page language="java" import="edu.stanford.muse.email.StaticStatusProvider"%>
<%@page language="java" import="edu.stanford.muse.exceptions.CancelledException"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.ner.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.exceptions.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.groups.*"%>
<%@page language="java" import="edu.stanford.muse.memory.*"%>
<%@page language="java" import="org.json.*"%>
<%
session.setAttribute("statusProvider", new StaticStatusProvider("Starting up..."));

boolean cancelled = false;
JSONObject result = new JSONObject();
String errorMessage = null;
String resultPage = null;

Archive archive = JSPHelper.getArchive(session);

if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
  session.removeAttribute("statusProvider");

if(archive!=null){
	try{
		NER ner = new NER(archive);
		session.setAttribute("statusProvider", ner);
		ner.trainAndRecognise();
		archive.processingMetadata.entityCounts = ner.stats.counts;
		System.err.println(ner.stats);
		
		session.removeAttribute("statusProvider");
		resultPage = "browse-top";
	}catch (CancelledException ce) {
		JSPHelper.log.warn("ePADD NER entity extraction cancelled by user");
		cancelled = true;
	} catch (Exception e) {
		JSPHelper.log.warn("Exception training/recognised named entities with epadd-ner");
		Util.print_exception(e, JSPHelper.log);
		errorMessage = "Exception training/recognised named entities with epadd-ner";
		// we'll leave archive in this
	}
}else{
	errorMessage = "No archive in session";
}
if (cancelled) {
	result.put("status", 0);
	result.put("cancelled", true);
} else if (errorMessage == null) {
	result.put("status", 0);
	result.put("resultPage", resultPage);
} else {
	result.put("status", 1);
	result.put("resultPage", "error");
	result.put("error", errorMessage);
}
out.println(result);
%>
