<%@page contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Set"%><%@ page import="edu.stanford.muse.email.StaticStatusProvider"%><%@ page import="java.util.Map"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="java.io.FileReader"%><%@ page import="au.com.bytecode.opencsv.CSVReader"%><%@ page import="java.io.IOException"%><%@ page import="com.google.common.collect.LinkedHashMultimap"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.muse.index.SearchResult"%><%@ page import="edu.stanford.muse.util.EmailUtils"%><%@ page import="edu.stanford.muse.index.DataSet"%><%@ page import="java.io.File"%><%@ page import="edu.stanford.muse.webapp.SimpleSessions"%><%@ page import="edu.stanford.muse.LabelManager.LabelManager"%><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%><%@ page import="java.nio.file.Path"%><%@ page import="java.nio.file.Paths"%>
<%
	   session.setAttribute("statusProvider", new StaticStatusProvider("Uploading files"));
   JSONObject result = new JSONObject();

    // Create a factory for disk-based file items
    // Configure a repository (to ensure a secure temp location is used)
    // Create a new file upload handler

    String error = null;
    String archiveID = request.getParameter("archiveID");
    Archive archive = null;
    int filesUploaded = 0;
    String docsetID = null;
    if (archiveID == null || "null".equals(archiveID)) {
                error = "Sorry, no archive ID found";
            } else {
                String filenameWithPath = request.getParameter("sidecarpath");
                archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
                String sidecarDir = archive.baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.SIDECAR_DIR;
                Path p = Paths.get(filenameWithPath);
                String sidecarFileName = p.getFileName().toString();
                if (!new File(filenameWithPath).exists())
                {
                    error = "The file \'" + filenameWithPath + "\' does not exist";
                }
                else if (!new File(filenameWithPath).isFile())
                {
                    error = "\'" + filenameWithPath + "\' is not a file";
                }
                else
                {
                    try
                    {
                        Util.copy_file(filenameWithPath,sidecarDir + File.separator + sidecarFileName);
                    }
                    catch (Exception e)
                    {
                        error = "Could not upload \'" + filenameWithPath + "\'";
                    }
                    archive.updateFileInBag(sidecarDir + File.separator + sidecarFileName,archive.baseDir);
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