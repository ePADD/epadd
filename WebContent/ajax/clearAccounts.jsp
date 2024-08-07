<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>    
<%@page language="java" import="java.util.*"%>    
<%@page language="java" import="edu.stanford.muse.email.*"%>    
<%@page language="java" import="edu.stanford.muse.webapp.*"%>    
<%@page language="java" import="edu.stanford.muse.util.*"%>    
<%@page language="java" import="edu.stanford.muse.index.*"%>    
<%   
// clears the accounts in the current email fetcher
MuseEmailFetcher m = null;
int nAccounts = -1;
synchronized (session) // synchronize, otherwise may lose the fetcher when multiple accounts are specified and are logged in to simult.
{
	m = (MuseEmailFetcher) JSPHelper.getSessionAttribute(session, "museEmailFetcher");
	if (m != null) {
		nAccounts = m.getNAccounts();
		if (nAccounts > 0) {
			JSPHelper.doLogging ("Clearing email fetcher with " + m.getNAccounts() + " account(s)");
			m.clearAccounts();
		}
	}
}

JSONObject obj = new JSONObject();
obj.put ("status", 0);
obj.put ("nAccounts", nAccounts); // return number of accounts cleared
out.println (obj);

%>
