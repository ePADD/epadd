<%@page language="java" import="edu.stanford.epadd.Version.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page language="java" import="javax.mail.*"%>
<%@page language="java" %>
<%@page language="java" %>
<%@page language="java" %>
<!DOCTYPE HTML>
<html lang="en">
<head>
	<META http-equiv="Content-Type" content="text/html; charset=UTF-8">

	<title>ePADD Exit</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js" type="text/javascript"></script> 
	<script src="js/jquery/jquery.tools.min.js" type="text/javascript"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/stacktrace.js" type="text/javascript"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js" type="text/javascript"></script>
	<script src="js/epadd.js"></script>
	<jsp:include page="css/css.jsp"/>
</head>
<body>
<% 
if (!"localhost".equals(request.getServerName()))
{
	out.println ("<p><br/><br/>Sorry, exit can only be called when running on your own computer.");
	return;
}
String mesg = "Epadd v" + edu.stanford.epadd.Version.version + " was asked to shut down and exit completely in 2 seconds.";
String s = request.getParameter("message");
if (!Util.nullOrEmpty(s))
	mesg += "Message: " + s;
JSPHelper.log.info (mesg);
out.println (mesg);
%>
</body>
</html>

<% 
out.flush();
Thread.sleep(2000);
System.exit(0);
%>
