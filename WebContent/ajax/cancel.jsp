<%--<!-- runs on server side -->--%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@ page import="edu.stanford.epadd.util.OperationInfo" %>
<%@ page import="org.json.JSONObject" %>
<%
	JSPHelper.log.warn ("\n\n\n------------------------------\nOperation cancelled!\n------------------------------\n\n\n");
	//read the operation ID for which the status needs to be returned..
	String opID = request.getParameter("opID");
	//check that opID should not be null else send a json to the client informing about this.
	//get operation object for this opID from session.
	OperationInfo operationInfo = JSPHelper.getOperationInfo(session,opID);
	//operationInfo should not be null but if it is then it means that the operation has not started yet.
	if(operationInfo==null){
		JSONObject res = new JSONObject();
		res.put("status",1);
		res.put("errorMessage","Operation not started yet. Try after some time..");
		out.println(res);
	}else{
	    operationInfo.cancel();
		JSONObject res = new JSONObject();
		res.put("status",0);
		out.println(res);
	}

%>
