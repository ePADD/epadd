<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.epadd.Version"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@include file="getArchive.jspf" %>
<%
	JSPHelper.log.info ("epadd v" + Version.version + " is running");
	if (ModeConfig.isAppraisalMode())
	{
		if (archive != null) {
			String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
			RequestDispatcher rd = request.getRequestDispatcher("browse-top?archiveID="+archiveID);
			rd.forward(request, response);
			return;
		}
		else {
			RequestDispatcher rd = request.getRequestDispatcher("email-sources");
			rd.forward(request, response);
			return;
		}
	}
	else
		response.sendRedirect("collections");
%>