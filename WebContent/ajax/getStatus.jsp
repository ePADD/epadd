<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%><%@ page import="edu.stanford.epadd.util.OperationInfo"%>
<%
	// prevent caching of this page - sometimes the ajax seems to show stale status
	JSPHelper.setPageUncacheable(response);
    response.setContentType("application/json; charset=utf-8");
    //read the operation ID for which the status needs to be returned..
    String opID = request.getParameter("opID");
    //check that opID should not be null else send a json to the client informing about this.
    //get operation object for this opID from session.
    OperationInfo operationInfo = JSPHelper.getOperationInfo(session,opID);
    //operationInfo should not be null but if it is then it means that the operation has not started yet.
    if(operationInfo==null){
        //means the operation has not started yet. set appropriate json.
        	JSONObject json = new JSONObject();
		try {
			json.put("message", "Starting up ...");
			json.put("sp_not_ready", true);
			out.println(json);
		} catch (JSONException jsone) {
			try {
				json.put("error", jsone.toString());
				out.println(json);
			} catch (Exception e) { Util.report_exception(e); }
		}

    }else{
        if(operationInfo.getResultJSON()==null){
            //means the operation is not yet over, read statusprovider
            StatusProvider obj = operationInfo.getStatusProvider();
            if(obj==null){
            //statusprovider is null implies operation not started yet. set appropriate json.
            	JSONObject json = new JSONObject();
                try {
                    json.put("message", "Starting up ...");
                    json.put("sp_not_ready", true);
                    out.println(json);
                } catch (JSONException jsone) {
                    try {
                        json.put("error", jsone.toString());
                        out.println(json);
                    } catch (Exception e) { Util.report_exception(e); }
                }
            }else{
            //statusprovider is non-null send the status accordingly.
            out.println(obj.getStatusMessage());//getStatusMessage will have one field 'resType' as "progress" to denote that the return information is about the progress not about the completion of the operation
            }
        }else{
            //means the operation is over. Get the resultJSON and send it out. before sending add a field called resType = done in it.
            JSONObject res = operationInfo.getResultJSON();
            res.put("resType","done");
            //remove opinfo object from session map.
            JSPHelper.removeOperationInfo(session,opID);
            out.println(res);
        }
    }

/*
	StatusProvider obj = (StatusProvider) session.getAttribute("statusProvider");
    if (obj == null)
    {
		JSONObject json = new JSONObject();
		try {
			json.put("message", "Starting up ...");
			json.put("sp_not_ready", true);
			out.println(json);
		} catch (JSONException jsone) {
			try {
				json.put("error", jsone.toString());
				out.println(json);
			} catch (Exception e) { Util.report_exception(e); }
		}
    }
    else
        out.println (obj.getStatusMessage());*/
%>
