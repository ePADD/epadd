<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page import="edu.stanford.muse.AddressBookManager.CorrespondentAuthorityMapper"%>
<%
// does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
JSPHelper.setPageUncacheable(response);
String fastIdStr = request.getParameter ("fastId");
JSONObject result = new JSONObject();
Archive archive = JSPHelper.getArchive(request);
if (archive == null) {
    JSONObject obj = new JSONObject();
    obj.put("status", 1);
    obj.put("error", "No archive in session");
    out.println (obj);
    JSPHelper.doLogging(obj);
    return;
}

String name = request.getParameter ("name");
CorrespondentAuthorityMapper cam = archive.getCorrespondentAuthorityMapper();

if (request.getParameter ("unset") == null) {
    long fastId = 0;
    try { fastId = Long.parseLong (fastIdStr); }
    catch (Exception nfe) {
        result.put ("status", 1);
        result.put ("error", "Invalid long for fast id: " + fastIdStr);
    }
    cam.setAuthRecord (name, fastId, request.getParameter ("viafId"), request.getParameter ("wikipediaId"), request.getParameter ("lcnafId"), request.getParameter ("lcshId"), request.getParameter ("localId"), request.getParameter("isManualAssign") != null);
} else {
    cam.unsetAuthRecord (name);
}

out.println (result.toString());
%>
