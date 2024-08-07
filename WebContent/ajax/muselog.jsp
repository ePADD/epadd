<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%
String message = request.getParameter("message");
if (message != null) {
	message = message.trim();
	while (message.startsWith("\n"))
		message = message.substring(1);

	if (request.getParameter("warn") != null)
		JSPHelper.doLoggingWarnings("CLIENT LOG warning: " + message);
	else
		JSPHelper.doLogging("CLIENT LOG: " + message);
}
%>
    