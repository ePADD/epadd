<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.epadd.Version"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.webapp.SimpleSessions" %>
<%@include file="getArchive.jspf" %>
<%
	JSPHelper.log.info ("epadd v" + Version.version + " is running");
if (ModeConfig.isAppraisalMode())
{
/*
	Archive archive = JSPHelper.getArchive(session);
	// try to load default archive if not already present
	if (archive == null)
	{
		JSPHelper.log.info("No archive in session, trying to load default archive");
		archive = SimpleSessions.prepareAndLoadDefaultArchive(request); // if we don't have an archive and are running in desktop mode, try to load archive from given cachedir or from the default dir
		JSPHelper.log.info("Default archive = " + archive);
	}
*/

	// note: for redirecting, have to use responseRedirectURL, otherwise the session var set above is lost, and archive is loaded all over again.
	// see http://stackoverflow.com/questions/4464641/java-session-attribute-is-only-in-next-operation
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