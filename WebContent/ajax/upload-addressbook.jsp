<%@page contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Set"%><%@ page import="edu.stanford.muse.email.StaticStatusProvider"%><%@ page import="java.util.Map"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="java.io.FileReader"%><%@ page import="au.com.bytecode.opencsv.CSVReader"%><%@ page import="java.io.IOException"%><%@ page import="com.google.common.collect.LinkedHashMultimap"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.muse.index.SearchResult"%><%@ page import="edu.stanford.muse.util.EmailUtils"%><%@ page import="edu.stanford.muse.index.DataSet"%><%@ page import="java.io.File"%><%@ page import="edu.stanford.muse.webapp.SimpleSessions"%><%@ page import="edu.stanford.muse.LabelManager.LabelManager"%><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
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
                String filename = params.get("addressbookfile");
                archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);

                // update the whole address book by using file upload file image
                ArchiveReaderWriter.updateAddressBook(archive, filename);
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
