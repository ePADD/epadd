<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>    
<%@page language="java" import="java.util.*"%>    
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>    
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="java.util.stream.Collectors"%><%@ page import="edu.stanford.muse.LabelManager.LabelManager"%>
<%
// does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
//JSPHelper.setPageUncacheable(response);

//////////////// The request will be of the form archiveID, labelid and the label will be removed only if it is not applied to any message. If it is applied to
//at least one message then a message will be sent to the caller with approrpriate status flag. status=0 means success and status neq 0 means not deleted.
int nMessages = 0;
JSONObject obj = new JSONObject();
    Archive archive = JSPHelper.getArchive(request);
if (archive == null) {
    obj.put("status", 1);
    obj.put("error", "No archive in session");
    out.println (obj);
    JSPHelper.log.info(obj);
    return;
}

LabelManager lm = archive.getLabelManager();
String labelid = request.getParameter("labelID");
//archive.getLabelCountsAsJson
Pair<Integer,String> status = lm.removeLabel(labelid);
if(status.first==0){
    obj.put("status", 0);
    out.println (obj);
    JSPHelper.log.info(obj);
}else{
    //means the label was not deleted.. msg contains the error message.
    obj.put("status", 1);
    obj.put("error",status.second);
    out.println (obj);
    JSPHelper.log.info(obj);
}
%>