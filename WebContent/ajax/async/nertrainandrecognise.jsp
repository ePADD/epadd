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
<%@ page import="edu.stanford.muse.email.StatusProvider" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="edu.stanford.epadd.util.OperationInfo" %>
<%@ page import="java.util.function.Consumer" %>

<%

    //<editor-fold desc="Setting up the operation object to execute this operation asynchronously">
    //get the operation ID from the request parameter.
    String encoding = request.getCharacterEncoding();
    JSPHelper.log.info("request parameter encoding is " + encoding);

    String actionName = request.getRequestURI();
    String opID = request.getParameter("opID");
    Multimap<String,String> paramMap = JSPHelper.convertRequestToMap(request);
    //create a new operation object with the information necessary to run this long running async task.
    final HttpSession fsession = session;
    OperationInfo opinfo = new OperationInfo(actionName,opID,paramMap) {
        @Override
        public void onStart(JSONObject resultJSON) {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            nerTrainAndRecognise(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            cancelNERTrainAndRecognise(setStatusProvider);
        }
    };

    //</editor-fold>

    //<editor-fold desc="Store this operation in global map so that others can access this operation">
    /*Map<String,OperationInfo> operationInfoMap = (Map<String,OperationInfo>) session.getAttribute("operationInfoMap");
    if(operationInfoMap==null)
        operationInfoMap = new LinkedHashMap<>();
    operationInfoMap.put(opID,opinfo);*/
    JSPHelper.setOperationInfo(session,opID,opinfo);
    //</editor-fold>

    //<editor-fold desc="Starting the operation">
    opinfo.run();
    //when canelling this operation, from cancel.jsp call opinfo.cancel() method.
    //when getting the status of this operation, call opinfo.getStatusProvider().getStatus() method.
    //</editor-fold>
    //just send an empty response telling that the operation has been started.
    JSONObject obj = new JSONObject();
    out.println(obj);
    %>


<%!
    public void nerTrainAndRecognise(Multimap<String,String> params, Consumer<StatusProvider> setStatusProvider, HttpSession session, JSONObject resultJSON) {
        setStatusProvider.accept(new StaticStatusProvider("Starting up..."));

        boolean cancelled = false;
        String errorMessage = null;
        String resultPage = null;

        //if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
          //  session.removeAttribute("statusProvider");

        Archive archive = JSPHelper.getArchive(params);

        if (archive != null) {
            try {
                String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
                String modelFile = SequenceModel.RULES_DIRNAME;
                SequenceModel nerModel = null;
//        = (SequenceModel) session.getAttribute("ner");
                session.setAttribute("statusProvider", new StaticStatusProvider("Loading openNLPNER sequence model from: " + modelFile + "..."));
                JSPHelper.log.info("Loading openNLPNER sequence model from: " + modelFile + " ...");
                nerModel = SequenceModel.loadModelFromRules(SequenceModel.RULES_DIRNAME);

                if (nerModel == null) {
                    JSPHelper.log.error("Could not load openNLPNER model from: " + modelFile);
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
                    archive.collectionMetadata.entityCounts = archive.getEntityBookManager().getEntitiesCountMapModuloThreshold(theta);//Archive.getEntitiesCountMapModuloThreshold(archive,theta);

                    JSPHelper.log.info(ner.stats);
                }
//        archive.collectionMetadata.numPotentiallySensitiveMessages = archive.numMatchesPresetQueries();
                JSPHelper.log.info("Number of potentially sensitive messages " + archive.collectionMetadata.numPotentiallySensitiveMessages);

                session.removeAttribute("statusProvider");
                resultPage = "browse-top?archiveID=" + archiveID;
            } catch (CancelledException ce) {
                JSPHelper.log.warn("ePADD openNLPNER entity extraction cancelled by user");
                cancelled = true;
            } catch (Exception e) {
                JSPHelper.log.warn("Exception training/recognised named entities with epadd-ner");
                Util.print_exception(e, JSPHelper.log);
                errorMessage = "Exception training/recognised named entities with epadd-ner";
                // we'll leave archive in this
            }
        } else {
            errorMessage = "No archive in session";
        }
        if (cancelled) {
            resultJSON.put("status", 0);
            resultJSON.put("cancelled", true);
        } else if (errorMessage == null) {
            resultJSON.put("status", 0);
            resultJSON.put("resultPage", resultPage);
        } else {
            resultJSON.put("status", 1);
            resultJSON.put("resultPage", "error");
            resultJSON.put("error", errorMessage);
        }
        //out.println(result);
    }
%>

<%!
    public void cancelNERTrainAndRecognise(Consumer<StatusProvider> setStatusProvider){

    }
%>
