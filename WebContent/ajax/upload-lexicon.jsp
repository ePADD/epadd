<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Set"%><%@ page import="edu.stanford.muse.email.StaticStatusProvider"%><%@ page import="java.util.Map"%><%@ page import="java.io.FileReader"%><%@ page import="au.com.bytecode.opencsv.CSVReader"%><%@ page import="java.io.IOException"%><%@ page import="com.google.common.collect.LinkedHashMultimap"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.muse.util.EmailUtils"%><%@ page import="java.io.File"%><%@ page import="edu.stanford.muse.webapp.SimpleSessions"%><%@ page import="edu.stanford.muse.LabelManager.LabelManager"%><%@ page import="edu.stanford.muse.index.*"%>
<%
	   session.setAttribute("statusProvider", new StaticStatusProvider("Uploading files"));
   JSONObject result = new JSONObject();

    // Create a factory for disk-based file items
    // Configure a repository (to ensure a secure temp location is used)
    // Create a new file upload handler

    String error = null;
    Map<String,String> params = JSPHelper.convertRequestToMapMultipartData(request);
    String archiveID = params.get("archiveID");
    Archive archive = null;
    int filesUploaded = 0;
    String docsetID = null;
    if ("null".equals(archiveID)) {
                error = "Sorry, no archive ID found";
            } else {
                String filename = params.get("lexiconfile");
                String lexiconname = params.get("lexicon-name");
                String lexiconlang= params.get("lexicon-lang");
                lexiconname=Lexicon.sanitizeLexiconName(lexiconname);
                archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
                    //Now copy the file 'filename' to lexicon directory
                    String lexicondir = archive.baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.LEXICONS_SUBDIR+ File.separator;
                    String destfilename = lexiconname+"."+lexiconlang+Lexicon.LEXICON_SUFFIX;
                    Util.copy_file(filename,lexicondir+File.separator+destfilename);
                    //update bag metadata
                    archive.updateFileInBag(lexicondir,archive.baseDir);
                    //read lexicons again and set in the archive.
                    archive.updateOrCreateLexiconMap();
                    if(!archive.lexiconMapContains(lexiconname))
                    {
                        error="Some error in uploading the lexicon file";
                    }

                    //caclulate the count cache for this lexicon.
                    //Before recomputing the summary delete the file Lexicon_summary.data  from the basedir.
                     archive.getLexicon(lexiconname).invalidateLexiconSummary(archive,lexiconname);
                    archive.getLexicon(lexiconname).fillL1_Summary(lexiconname,archive,false);
                    //Archive.cacheManager.cacheLexiconListing(lexiconname,archiveID);
            }

        if (error != null) {
            result.put("status", 1);
            result.put ("error", error);
        } else {
            result.put ("status", 0);
            //result.put ("urltoload", "labels.jsp");
        }
out.println (result.toString(4));
    session.removeAttribute("statusProvider");

%>
