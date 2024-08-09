<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="java.io.File" %>
<%@page import="java.util.Date" %>
<%@page import="edu.stanford.muse.util.Log4JUtils"%>
<%@page import="edu.stanford.muse.util.Util"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig" %>

<%--<%@include file="getArchive.jspf" %>--%>

<!DOCTYPE HTML>
<html lang="en">
<head>
	<title>ePADD Log</title>
	
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js" type="text/javascript"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<script src="js/muse.js" type="text/javascript"></script>
	<script src="js/epadd.js?v=1.1"></script>
</head>
<body class="color:gray;background-color:white;">
<%@include file="header.jspf"%>
<br/>

<div id="main" style="margin:1% 5%">

<br/>
If you have encountered a problem, please provide the following details to <%=edu.stanford.muse.Config.admin %> to help us fix the problem:<br/>
	<ul>
		<li>Total RAM in your system</li>
		<li>Name of the screens you went through and the input boxes filled on those screens (or screen prints). state the steps to reproduce the problem.</li>
		<li>If the problem was encountered while importing emails then please mention the source of mbox file and the software (if any) used to convert it to mbox format.</li>
		<li>Number of email messages and the size (in GB) of the archive.</li>
		<li>If necessary, can you share the email files with us to find out the issue?</li>
		<li>The log given below.</li>
	</ul>


    Jump to: <a href="#configuration">Configuration</a> &bull; <a href="#session">Session</a> &bull;  <a href="#warnings">Warnings</a> &bull; <a href="#debug">Log</a>
    <br/>
<hr style="color:rgba(0,0,0,0.2)"/>
<b>ePADD version <%=edu.stanford.epadd.Version.version%></b>
<br/>
Build Info: <%= edu.stanford.epadd.Version.buildInfo%><br/>

<p> </p>

<% if (ModeConfig.isDiscoveryMode() || ModeConfig.isServerMode()) { return; } %>

	<p>

    <b>Browser</b><br/>
    <%= request.getHeader("User-agent")%>

	<br/><br/>

    <b>Memory status</b><br/>
    <%=Util.getMemoryStats()%><br/>

	<br/><br/>
    <a name="configuration"></a>
	<b>System properties</b><br/>

	<%
		java.util.Properties properties = System.getProperties();
		for (String key: properties.stringPropertyNames())
		{
			String val = properties.getProperty(key);
			out.println("<b>" + Util.escapeHTML (key) + "</b>: " + Util.escapeHTML (val) + "<br>");
		}
	%>
	<br/>
        <a name="session"></a>

        <b>Session attributes</b><p>
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
      if (!"emailDocs".equals(key) && !"lexicon".equals(key) && !"fullEmailDocs".equals(key) && !key.startsWith("dataset") && !key.startsWith("docs-docset") && !key.startsWith("GroupsJSON")) // don't toString email docs which have email headers (too sensitive), same with dataset
	      out.println("<b>" + key + "</b>: " + JSPHelper.getSessionAttribute(session, key) + "<br>");
      else
    	  out.println("<b>" + key + "</b>: not printed<br>");
    }
    %>
	<br/>
	<br/>
	Last error code:<%=request.getAttribute("javax.servlet.error.status_code")%> type: <%=request.getAttribute("javax.servlet.error.exception_type")%>
        <% Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
            if (throwable != null) {
                Util.print_exception(throwable, JSPHelper.log);
            }
        %>

	<br/>
	<br/>
	<%
		out.flush(); // flush the page now, so that the user sees something before the long log file is read.
		//Log4JUtils.flushAllLogs(); // if we dont flush file is sometimes truncated

	// dump the warnings file first
	{
		String debugFile = Log4JUtils.WARNINGS_LOG_FILE;
		File f = new File(debugFile);
		if (f.exists() && f.canRead()) {
			%>
        <a name="warnings"></a>

        <b>Warnings log</b> (from <%=debugFile%>)
			<hr style="color:rgba(0,0,0,0.2)"/>
			<div id="testdiv">
			<%
			String s = Util.getFileContents(debugFile);
			s = Util.escapeHTML(s);
			s = s.replace("\n", "<br/>\n");
			out.println(s);
			%>
		</div>
		<hr style="color:rgba(0,0,0,0.2)"/>
		<%
		} else
			out.println("No warnings log in " + debugFile + "<br/>");
	}

	// now dump the log file. only the latest (current) version of the log file is dumped. if logs have been rolled over, they will not be dumped.
	{
		String debugFile = Log4JUtils.LOG_FILE;
		File f = new File(debugFile);
		if (f.exists() && f.canRead()) {
		%>
    <a name="debug"></a>

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
			<hr style="color:rgba(0,0,0,0.2)"/>
		<%
		}
		else
			out.println ("No debug log in " + debugFile + "<br/>");
	}
	%>
	<br/>
    <p> </p>

</div> <!--  main -->
<%@include file="footer.jsp"%>
</body>
</html>
