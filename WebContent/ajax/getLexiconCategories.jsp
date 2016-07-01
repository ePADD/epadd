<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%
	// params:
	// lexicon=<lexicon name>. if no lexicon name, use "default"
	// archive, cacheDir should be in session

	// which lexicon? first check if url param is present, then check if url param is specified
	Archive archive = JSPHelper.getArchive(session);
	JSONObject result = new JSONObject();
	if (archive == null) {
		result.put("status", 1);
		result.put ("errorMessage", "No archive is loaded");
		out.println (result.toString(4));
		return;
	}
	String lexiconName = request.getParameter("lexicon");

	Lexicon lex = null;
	if (!Util.nullOrEmpty(lexiconName))
		lex = archive.getLexicon(lexiconName);

	JSPHelper.log.info ("lex lexiconName = " + lexiconName + " loaded lex's lexiconName = " + ((lex == null) ? "(lex is null)" : lex.name));

	// now lexiconName and lex are both set correctly.
	Collection<String> categories = lex.getRawMapFor("english").keySet();

	result.put("status", 0);
	result.put ("categories", categories);
	out.println (result.toString(4));
	return;
%>