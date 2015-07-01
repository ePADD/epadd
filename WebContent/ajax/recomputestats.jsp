<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.ie.ie.*"%>
<%
Archive archive = (Archive)request.getSession().getAttribute("archive");
archive.processingMetadata.numPotentiallySensitiveMessages = archive.numMatchesPresetQueries();
System.err.println("Number of potentially sensitive messages " + archive.processingMetadata.numPotentiallySensitiveMessages);
%>