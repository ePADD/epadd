<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page language="java" import="org.json.JSONObject"%><%@ page import="edu.stanford.muse.index.Archive"%><%@ page import="edu.stanford.muse.util.Util"%>
<%
JSONObject result = new JSONObject();
Archive archive = JSPHelper.getArchive(request);
        if (archive == null)
        {
            result.put ("status", 2);
            result.put("error", "No archive found for archive ID =  " + request.getParameter("archiveID"));
            out.println (result.toString(4));
            return;
        }


    if(!Util.nullOrEmpty(request.getParameter("labelName"))){
	    //It means that the request parameter contains information about new label creation or label updation
		//call JSPHelper method with request parameter to perform the appropriate action.
		String labelID = JSPHelper.createOrEditLabels(archive,request);
        result.put("status", 0);
        result.put ("labelID", labelID);
        out.println (result.toString(4));
        return;
	}else {
	    result.put ("status", 3);
	    out.println (result.toString(4));
	  }


%>
