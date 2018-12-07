<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%><%@ page import="java.io.File"%>

<%
	// params:
	// lexicon=<lexicon name>. if no lexicon name, use "default"
	// archive, cacheDir should be in session

	// which lexicon? first check if url param is present, then check if url param is specified
	JSONObject result = new JSONObject();
    Archive archive = JSPHelper.getArchive(request);
    if (archive == null) {
        JSONObject obj = new JSONObject();
        obj.put("status", 1);
        obj.put("error", "No archive in session");
        out.println (obj);
        JSPHelper.log.info(obj);
        return;
    }
    String lexiconName = request.getParameter("lexicon");

    String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
	Lexicon lex = null;
	if (!Util.nullOrEmpty(lexiconName))
		lex = archive.getLexicon(lexiconName);

	boolean freshLexicon = false;
	if (lex == null)
	{
		// it doesn't already exist, we have to create a brand new one
		lex = new Lexicon(archive.baseDir+File.separatorChar+Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.LEXICONS_SUBDIR, lexiconName);
		JSPHelper.log.info ("Creating new lexicon: " + lexiconName);
	}
	else
		JSPHelper.log.info("Updating existing lexicon: " + lexiconName);

	JSPHelper.log.info ("lex lexiconName = " + lexiconName + " loaded lex's lexiconName = " + ((lex == null) ? "(lex is null)" : lex.name));

	// now lexiconName and lex are both set correctly.
	String language = "english";
	Map<String, String[]> map = new LinkedHashMap<String, String[]>(request.getParameterMap());
	// everything but for the following params is a lexicon category. note: these are not allowed as category names.
	map.remove("lexicon");
	map.remove("language");
    map.remove("archiveID");
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
		lex.save(archive.baseDir + java.io.File.separatorChar + Archive.BAG_DATA_FOLDER + File.separator + "lexicons", language,archive);

		//After lexicon is saved, invalidate the cache for the result of getCountsAsJson
		//should we force recomputation here only??Yes
		archive.getLexicon(lexiconName).fillL1_Summary(lexiconName,archive,false);
		//Archive..cacheLexiconListing(lexiconName,ArchiveReaderWriter.getArchiveIDForArchive(archive));

	}

	result.put("status", 0);
	result.put ("nCategories", map.size());
	out.println (result.toString(4));
	return;
%>