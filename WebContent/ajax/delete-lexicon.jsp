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
	String errormsg="";
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
	else{
	    JSONObject obj = new JSONObject();
        obj.put("status", 1);
        obj.put("error", "No lexicon exists named "+lexiconName);
        out.println (obj);
        JSPHelper.log.info(obj);
        return;
	    }
	//get the filename for this lexicon
	lex.removeLexicon(archive,archive.baseDir+File.separatorChar+Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.LEXICONS_SUBDIR);
 //read lexicons again and set in the archive.
                    Map<String,Lexicon> map = Archive.createLexiconMap(archive.baseDir+File.separator+Archive.BAG_DATA_FOLDER);
                    archive.setLexiconMap(map);



	result.put("status", 0);
	result.put("archiveID",archiveID);//because on success we would like to redirect to some other page.
	out.println (result.toString(4));
	JSPHelper.log.info(result);
	return;
%>