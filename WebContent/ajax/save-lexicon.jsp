<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
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

	boolean freshLexicon = false;
	if (lex == null)
	{
		// it doesn't already exist, we have to create a brand new one
		lex = new Lexicon(archive.baseDir, lexiconName);
		JSPHelper.log.info ("Creating new lexicon: " + lexiconName);
	}
	else
		JSPHelper.log.info("Updating existing lexicon: " + lexiconName);

	JSPHelper.log.info ("lex lexiconName = " + lexiconName + " loaded lex's lexiconName = " + ((lex == null) ? "(lex is null)" : lex.name));

	// now lexiconName and lex are both set correctly.
	String language = "english";
	Map<String, String[]> map = new LinkedHashMap<String, String[]>((Map<String, String[]>) request.getParameterMap());
	// everything but for the following params is a lexicon category. note: these are not allowed as category names.
	map.remove("lexicon");
	map.remove("language");

	if (map.size() > 0)
	{
		// convert string->string[] to string -> string
		Map<String, String> newMap = new LinkedHashMap<String, String>();
		for (String key: map.keySet())
		{
			String val = map.get(key)[0];
			if (Util.nullOrEmpty(val))
				continue;
			newMap.put(key, val);
		}
		JSPHelper.log.info ("updating lexicon for " + lexiconName + " in language " + language + " with " + newMap.size() + " entries");
		// should we clear an existing lex first?
		lex.update(language, newMap);
		lex.save(archive.baseDir + java.io.File.separatorChar + "lexicons", language);
	}

	result.put("status", 0);
	result.put ("nCategories", map.size());
	out.println (result.toString(4));
	return;
%>