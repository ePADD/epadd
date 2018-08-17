<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page language="java" import="edu.stanford.muse.email.StaticStatusProvider"%>
<%@page language="java" import="edu.stanford.muse.exceptions.CancelledException"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.ner.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" %>
<%@page language="java" %>
<%@page language="java" %>

<%@page language="java" import="org.json.*"%>
<%@ page import="java.io.File" %>
<%@ page import="edu.stanford.muse.ner.model.SequenceModel" %>
<%@ page import="java.io.IOException" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>

<%
session.setAttribute("statusProvider", new StaticStatusProvider("Starting up..."));

boolean cancelled = false;
JSONObject result = new JSONObject();
String errorMessage = null;
String resultPage = null;

    if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
      session.removeAttribute("statusProvider");

Archive archive = JSPHelper.getArchive(request);

if(archive!=null){
    try {
        String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
        String modelFile = SequenceModel.RULES_DIRNAME;
        SequenceModel nerModel = null;
//        = (SequenceModel) session.getAttribute("ner");
        session.setAttribute("statusProvider", new StaticStatusProvider("Loading NER sequence model from: " + modelFile + "..."));
        JSPHelper.log.info("Loading NER sequence model from: " + modelFile + " ...");
        nerModel = SequenceModel.loadModelFromRules(SequenceModel.RULES_DIRNAME);

        if (nerModel == null) {
            JSPHelper.log.error("Could not load NER model from: " + modelFile);
        } else {
            NER ner = new NER(archive, nerModel);
            session.setAttribute("statusProvider", ner);
            ner.recognizeArchive();
            //ner.recognizeArchive(); [why was it called two times??]
            //Here, instead of getting the count of all entities (present in ner.stats object)
            //get the count of only those entities which pass a given threshold.
            //This is to fix a bug where the count of person entities displayed on browse-top.jsp
            //page was different than the count of entities actually displayed following a threshold.
            // @TODO make it more modular
            //archive.collectionMetadata.entityCounts = ner.stats.counts;
            double theta = 0.001;
            archive.collectionMetadata.entityCounts = Archive.getEntitiesCountMapModuloThreshold(archive,theta);

            JSPHelper.log.info(ner.stats);
        }
//        archive.collectionMetadata.numPotentiallySensitiveMessages = archive.numMatchesPresetQueries();
        JSPHelper.log.info("Number of potentially sensitive messages " + archive.collectionMetadata.numPotentiallySensitiveMessages);

        session.removeAttribute("statusProvider");
        resultPage = "browse-top?archiveID="+archiveID;
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
