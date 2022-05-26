<%@ page import="edu.stanford.muse.epaddpremis.EpaddEvent"%>
<%@ page import="edu.stanford.muse.epaddpremis.EpaddPremis"%>
<%@ page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="org.json.JSONObject"%><%@ page import="com.google.gson.Gson"%>
<%@page language="java" contentType="application/json;charset=UTF-8"%>

<%
    String archiveID = request.getParameter("archiveID");
    if (archiveID != null && !archiveID.isEmpty())
    {
        Archive archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        archive.getEpaddPremis().createEvent(new JSONObject(request.getParameter("premisdata")));
    }
%>


