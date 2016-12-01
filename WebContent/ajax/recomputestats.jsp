<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%
Archive archive = (Archive)request.getSession().getAttribute("archive");
archive.processingMetadata.numPotentiallySensitiveMessages = archive.numMatchesPresetQueries();
System.err.println("Number of potentially sensitive messages " + archive.processingMetadata.numPotentiallySensitiveMessages);
%>