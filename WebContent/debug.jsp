<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.util.Log4JUtils"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.ModeConfig"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="java.io.File" %>
<%@ page import="java.util.Date" %>
<!DOCTYPE HTML>
<html lang="en">
<head>
	<title>ePADD Log</title>
	
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	
	<script src="js/jquery.js" type="text/javascript"></script> 
	<script src="js/jquery/jquery.tools.min.js" type="text/javascript"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js" type="text/javascript"></script>
	<script src="js/epadd.js"></script>
</head>
<body class="color:gray;background-color:white;>
<jsp:include page="header.jspf"/>
<br/>

<div id="main" style="margin:1% 5%">
<br/>
We are sorry you encountered a problem with ePADD. Please save this page and send it <%=edu.stanford.epadd.Config.admin %> to help us fix the problem.<br/>

<hr style="color:rgba(0,0,0,0.2)"/>
<b>ePADD version <%=edu.stanford.epadd.Version.version%></b>
<br/>
Build Info: <%= edu.stanford.epadd.Version.buildInfo%>

<p/>

<% if (ModeConfig.isDiscoveryMode()) { return; } %>

	<p>

    <b>Browser</b><p/>
    <%= request.getHeader("User-agent")%>
	<p>
    <b>Memory status</b><p/>
    <%=Util.getMemoryStats()%><br/>

	<br/>

    <b>System properties</b><p/>

	<%
		java.util.Hashtable properties = System.getProperties();
		for (Object obj: properties.entrySet())
		{
			java.util.Map.Entry entry = (java.util.Map.Entry) obj;
			String key = (String) entry.getKey();
			String val = (String) entry.getValue();
			out.println("<b>" + key + "</b>: " + val + "<br>");
		}
	%>
	<br/>
    <b>Session attributes</b><p/>
    <% long created = session.getCreationTime();
       long accessed = session.getLastAccessedTime();
       long activeTimeSecs = (accessed - created)/1000;
       long sessionHours = activeTimeSecs/3600;
	   activeTimeSecs = activeTimeSecs%3600;
       long sessionMins = activeTimeSecs/60;
       long sessionSecs = activeTimeSecs%60;
	%>
    Session id: <%=session.getId()%>, created <%= new Date(created) %>, last accessed <%= new Date(accessed)%><br/>
    Session active for <%=sessionHours%> hours, <%=sessionMins%> minutes, <%=sessionSecs%> seconds<br/><br/>
    <%
    java.util.Enumeration keys = session.getAttributeNames();

    while (keys.hasMoreElements())
    {
      String key = (String)keys.nextElement();
      if (!"emailDocs".equals(key) && !"lexicon".equals(key) && !"fullEmailDocs".equals(key) && !key.startsWith("dataset") && !key.startsWith("docs-docset") && !key.startsWith("GroupsJSON")) // don't print email docs which have email headers (too sensitive), same with dataset
	      out.println("<b>" + key + "</b>: " + JSPHelper.getSessionAttribute(session, key) + "<br>");
      else
    	  out.println("<b>" + key + "</b>: not printed<br>");
    }
    String documentRootPath = application.getRealPath("/").toString();
	%>
	<br/>
	<%
		Log4JUtils.flushAllLogs(); // if we dont flush file is sometimes truncated
		String debugFile = Log4JUtils.LOG_FILE;
		File f = new File(debugFile);
		if (f.exists() && f.canRead())
		{
	%>
			<b>Debug log</b> (from <%=debugFile%>)
			<hr style="color:rgba(0,0,0,0.2)"/>
			<div id="testdiv">
			<%
				String s = Util.getFileContents(debugFile);
				s = Util.escapeHTML(s);
				s = s.replace ("\n", "<br/>\n");
				out.println (s);
			%>
			</div>
	<%
		}
		else
			out.println ("No debug log in " + debugFile);
	%>
	<br/>
	<p/>

</div> <!--  main -->
<%@include file="footer.jsp"%>
</body>
</html>
