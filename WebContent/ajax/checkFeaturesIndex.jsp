<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.ie.Authority"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.ie.FASTReader"%>
<%@page language="java" import="edu.stanford.muse.ie.FASTPerson"%>

<%@page language="java" import="java.util.*"%>
<%@page language="java" import="org.json.*"%><%@ page import="edu.stanford.muse.ie.InternalAuthorityAssigner"%>
<% 
	InternalAuthorityAssigner assignauthorities = (InternalAuthorityAssigner)request.getSession().getAttribute("authorities");
	Archive archive = JSPHelper.getArchive(session);
	if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
		  session.removeAttribute("statusProvider");
	session.setAttribute("statusProvider", assignauthorities);
	
	if (assignauthorities==null){
		assignauthorities = new InternalAuthorityAssigner();
		session.setAttribute("statusProvider", assignauthorities);
		assignauthorities.initialize(archive);
		if(!assignauthorities.isCancelled())
			request.getSession().setAttribute("authorities", assignauthorities);
		else
			assignauthorities = null;
	}

	String action = request.getParameter("action");
	System.err.println("Received request for: "+action);
	String status = "success";
	boolean success = assignauthorities.checkFeaturesIndex(archive,true);
	
	JSONObject obj = new JSONObject();
	obj.put("action", action);
	obj.put("status", status);
	System.err.println(":::"+obj.toString(4)+":::");
	out.println(obj.toString(4));
	JSPHelper.log.info(obj);
%>
