<!-- runs on server side -->
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%
	JSPHelper.log.warn ("\n\n\n------------------------------\nOperation cancelled!\n------------------------------\n\n\n");
	StatusProvider obj = (StatusProvider) JSPHelper.getSessionAttribute(session, "statusProvider");
	JSPHelper.log.info ("Cancelling object: " + obj);
    if (obj != null)
        obj.cancel();
%>
