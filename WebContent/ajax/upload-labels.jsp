<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Set"%><%@ page import="edu.stanford.muse.email.StaticStatusProvider"%><%@ page import="java.util.Map"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="java.io.FileReader"%><%@ page import="au.com.bytecode.opencsv.CSVReader"%><%@ page import="java.io.IOException"%><%@ page import="com.google.common.collect.LinkedHashMultimap"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.muse.index.SearchResult"%><%@ page import="edu.stanford.muse.util.EmailUtils"%><%@ page import="edu.stanford.muse.index.DataSet"%><%@ page import="java.io.File"%><%@ page import="edu.stanford.muse.webapp.SimpleSessions"%><%@ page import="edu.stanford.muse.LabelManager.LabelManager"%>
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
                String filename = params.get("labeljson");
                archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
                /*Important precondition: If any message has any label assigned to it then return an error saying that "Some messages have labels assigned to them. Before importing a label description file, please remove them"*/
                boolean isAnylabelApplied = archive.getLabelManager().isAnyLabel();
                if(isAnylabelApplied){
                error="ERROR:Some messages have labels assigned to them. Please remove them first before importing a new set of labels!!";
                }else{


                    //Now copy the file 'filename' to LabMapper directory inside session
                    String labelmapdir = archive.baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.SESSIONS_SUBDIR + File.separator + Archive.LABELMAPDIR + File.separator;
                    Util.copy_file(filename,labelmapdir+File.separator+"label-info.json");
                    //update bag metadata
                    archive.updateFileInBag(labelmapdir,archive.baseDir);
                    //read labelmapper again
                    LabelManager lb = null;
                   try {
                        lb = LabelManager.readObjectFromStream(labelmapdir);
                     } catch (Exception e) {
                         Util.print_exception ("Exception in reading label manager from archive, assigning a new label manager", e, JSPHelper.log);

                    }
                    if(lb==null){
                        error="ERROR: Invalid label description file provided!!";
                    }else{
                    //set as current archive's label mapper
                    archive.setLabelManager(lb);
                    }
                }

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