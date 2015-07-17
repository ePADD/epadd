<%@page contentType="application/json; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%
// little jsp to return jsp for folderinfos for the selected account.
// read of folders and accounts in the selected account may be in progress
JSPHelper.setPageUncacheable(response); // important! it was getting falsely cached earlier

MuseEmailFetcher m = (MuseEmailFetcher) JSPHelper.getSessionAttribute(session, "museEmailFetcher");
if (m == null)
{
    response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT); // 404.
    return;
}

// find the requested account

String accountName = request.getParameter("account");
int accountIdx = HTMLUtils.getIntParam(request, "account", -1);
if (accountIdx != -1)
{
	try {
      	// wait for folders to show up for up to 10 secs
 //   	int count = 0;
//    	while (!m.folderInfosAvailable(accountIdx))
//   	{
//    		Thread.sleep (1000);
//    		if (count++ > 10)
//				break;  // TODO: maybe give a proper error mesg here?
 //   	}
		String json = m.getFolderInfosAsJson(accountIdx);
		out.println (json);
		return;
	} catch (Exception e)
	{
		Util.report_exception(e);		
	}
}
// if we reach here, its an error
response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 404.

%>