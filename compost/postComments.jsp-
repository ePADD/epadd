<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%
String docset = request.getParameter("docset"); // this is actually docs-docset-NNN
String comment = request.getParameter("comment");

List<Document> docs = (List<Document>) JSPHelper.getSessionAttribute(session, docset);
if (docs == null)	
{
	JSPHelper.log.error ("Docset " + docset + " is null");
	response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT); // 404.
    return;
}

// if docnum is "all", apply to all docs in this docset
String docNumStr = request.getParameter("page");
if ("all".equalsIgnoreCase(docNumStr))
{
	for (Document d: docs)
		d.setComment(Util.nullOrEmpty(d.comment) ? comment : d.comment + " " + comment);
}
else
{
	int docNum = HTMLUtils.getIntParam(request, "page", -1);	
	if (docNum < 0 || docNum >= docs.size())
	{
		JSPHelper.log.error ("Bad doc num " + docNum + " for docset " + docset + " size " + docs.size());
	    response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT); // 404.
	    return;
	}

	Document d = docs.get(docNum);
	d.setComment(comment);
}
out.println("{\"status\":\"0\"");
%>
