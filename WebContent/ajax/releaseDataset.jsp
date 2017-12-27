<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<% 
JSPHelper.logRequest(request);
JSPHelper.setPageUncacheable(response);
response.setContentType("application/json; charset=utf-8");

String docsetID = request.getParameter("docsetID");
DataSet dataset = (DataSet) JSPHelper.getSessionAttribute(session, docsetID);
boolean error = (dataset == null);
if (error)
	out.println("{status: 'error'}");
else
{
	dataset.clear();
	session.removeAttribute(docsetID);
	out.println("{status: 'ok'}");
}
JSPHelper.logRequestComplete(request);

%>
