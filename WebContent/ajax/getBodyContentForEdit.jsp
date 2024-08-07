<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>

<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>

<%

    // does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
    JSPHelper.setPageUncacheable(response);

	String archiveID = request.getParameter("archiveID");
	int currentPage = HTMLUtils.getIntParam(request, "currentPage", -1);

	String datasetId = request.getParameter("docsetID");

    //JSPHelper.doConsoleLogging("getBodyContentForEdit: currentPage =" + currentPage + " || datasetId =" + datasetId );

	JSONObject result = new JSONObject();
	Archive archive = JSPHelper.getArchive(request);

    //DataSet dataset = (DataSet) session.getAttribute(datasetId);
    DataSet dataset = (DataSet) JSPHelper.getSessionAttribute(session, datasetId);

	if (dataset == null) {
        	JSONObject obj = new JSONObject();
        	obj.put("status", 1);
	        obj.put("error", "No dataset in session");
	        out.println (obj);
	        JSPHelper.doLogging(obj);
	} else
	{
            String emailBody = dataset.getPageForEdit(currentPage);

            result.put("status", 0);
            result.put("emailBody", emailBody);
            out.println(result.toString());
	}

%>
