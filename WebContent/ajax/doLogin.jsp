<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>    
<%@page language="java" import="java.util.*"%>    
<%@page language="java" import="edu.stanford.muse.email.*"%>    
<%@page language="java" import="edu.stanford.muse.webapp.*"%>    
<%@page language="java" import="edu.stanford.muse.util.*"%>    
<%@page language="java" import="edu.stanford.muse.index.*"%>    
<%   
// does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
JSPHelper.setPageUncacheable(response);

boolean incremental = request.getParameter("incremental") != null;
if (!incremental)
{
	Archive archive = JSPHelper.getArchive(session,request);
	if (archive != null) {
	    JSPHelper.log.info ("Closing existing archive " + a);
		archive.close();
		session.removeAttribute("archive");
	}
	session.removeAttribute("alternateEmailAddrs");
	session.removeAttribute("cacheDir");
	session.removeAttribute("museEmailFetcher");
}
String idx_str = request.getParameter("accountIdx");
int accountIdx = Integer.parseInt(idx_str);
JSONObject obj = Accounts.login(request, accountIdx);
out.println (obj);
%>   
