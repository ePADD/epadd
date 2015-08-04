<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>    
<%@page language="java" import="java.util.*"%>    
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>    
<%@page language="java" import="edu.stanford.muse.index.*"%>    
<%   
// does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
JSPHelper.setPageUncacheable(response);

// careful here: the params names are deliberately *set*DoNotTransfer instead of doNotTransfer, etc. otherwise, the doNotTransfer that we are trying to set is instead taken as a selector in selectDocs
// was a bug causing flags to only be set, and unable to unset.
boolean doNotTransferSet = !Util.nullOrEmpty(request.getParameter("setDoNotTransfer"));
boolean doNotTransfer = "1".equals(request.getParameter("setDoNotTransfer"));
boolean transferWithRestrictionsSet = !Util.nullOrEmpty(request.getParameter("setTransferWithRestrictions"));
boolean transferWithRestrictions = "1".equals(request.getParameter("setTransferWithRestrictions"));
boolean reviewedSet = !Util.nullOrEmpty(request.getParameter("setReviewed"));
boolean reviewed = "1".equals(request.getParameter("setReviewed"));
boolean addToCartSet = !Util.nullOrEmpty(request.getParameter("setAddToCart"));
boolean addToCart = "1".equals(request.getParameter("setAddToCart"));
String annotation = request.getParameter("setAnnotation");
boolean append = "1".equals(request.getParameter("append"));

int nMessages = 0;
Set<Document> docs;
Archive archive = JSPHelper.getArchive(session);
if (archive == null) {
	JSONObject obj = new JSONObject();
	obj.put("status", 1);
	obj.put("error", "No archive in session");
	out.println (obj);
	JSPHelper.log.info(obj);
	return;
}

if (request.getParameter("allDocs") != null)
	docs = archive.getAllDocsAsSet();
else if(session.getAttribute("selectDocs")!=null)
    docs = (Set<Document>)session.getAttribute("selectDocs");
else
 	docs = JSPHelper.selectDocs(request, session, false /* onlyFilteredDocs */, false);

if (docs != null)
{
	JSPHelper.log.info ("applyFlags to " + docs.size() + " message(s)");
	if (doNotTransferSet)
		JSPHelper.log.info ("setting doNotTransfer to " + doNotTransfer);
	if (transferWithRestrictionsSet)
		JSPHelper.log.info ("setting transferWithRestrictions to " + transferWithRestrictions);
	if (reviewedSet)
		JSPHelper.log.info ("setting reviewed to " + reviewed);
	if (addToCartSet)
		JSPHelper.log.info ("setting addToCart to " + addToCart);
	
	for (Document d: docs)
	{
		EmailDocument ed = (EmailDocument) d;
		if (doNotTransferSet)
			ed.doNotTransfer = doNotTransfer;
		if (transferWithRestrictionsSet)
			ed.transferWithRestrictions = transferWithRestrictions;
		if (reviewedSet)
			ed.reviewed = reviewed;
		if (addToCartSet)
			ed.addedToCart = addToCart;
		if (annotation != null) // this can be null coming in from settings page -> reset all reviewed, in which case we don't want to reset all annotations.
		{
			if (append)
				ed.setComment(Util.nullOrEmpty(ed.comment) ? annotation : ed.comment + " " + annotation);
			else
				ed.setComment(annotation);
		}
	}
	nMessages= docs.size();
}

JSONObject obj = new JSONObject();
obj.put("nMessages", nMessages);    	
out.println (obj);
JSPHelper.log.info(obj);
%>