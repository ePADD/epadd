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
    try {
        String modelFile = SequenceModel.RULES_DIRNAME;
        SequenceModel nerModel = (SequenceModel) session.getAttribute("ner");
        session.setAttribute("statusProvider", new StaticStatusProvider("Loading NER sequence model from: " + modelFile + "..."));
        JSPHelper.log.info("Loading NER sequence model from: " + modelFile + " ...");
        try {
            nerModel = SequenceModel.loadModelFromRules(SequenceModel.RULES_DIRNAME);
        } catch (IOException e) {
            Util.print_exception("Could not load the sequence model from: " + modelFile, e, JSPHelper.log);
        }
        if (nerModel == null) {
            JSPHelper.log.error("Could not load NER model from: " + modelFile);
        } else {
            NER ner = new NER(archive, nerModel);
            session.setAttribute("statusProvider", ner);
            ner.recognizeArchive();
            //Here, instead of getting the count of all entities (present in ner.stats object)
            //get the count of only those entities which pass a given thersold.
            //This is to fix a bug where the count of person entities displayed on browse-top.jsp
            //page was different than the count of entities actually displayed following a thersold.
            // @TODO make it more modular
            //archive.processingMetadata.entityCounts = ner.stats.counts;
            double theta = 0.001;
            archive.processingMetadata.entityCounts = Archive.getEntitiesCountMapModuloThersold(archive,theta);

            JSPHelper.log.info(ner.stats);
            System.err.println(ner.stats);
        }
//        archive.processingMetadata.numPotentiallySensitiveMessages = archive.numMatchesPresetQueries();
        JSPHelper.log.info("Number of potentially sensitive messages " + archive.processingMetadata.numPotentiallySensitiveMessages);

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
