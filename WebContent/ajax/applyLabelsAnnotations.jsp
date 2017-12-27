<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>    
<%@page language="java" import="java.util.*"%>    
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>    
<%@page language="java" import="edu.stanford.muse.index.*"%><%@ page import="javax.print.Doc"%><%@ page import="java.util.stream.Collectors"%>
<%
// does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
JSPHelper.setPageUncacheable(response);

// careful here: the params names are deliberately *set*DoNotTransfer instead of doNotTransfer, etc. otherwise, the doNotTransfer that we are trying to set is instead taken as a selector in selectDocs
// was a bug causing flags to only be set, and unable to unset.
/*
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
*/

//////////////// The request can be of the form docID/docsetID, labels, action=set/unset/only [set/unset/only keep label for all docs in the given set], or
// ///////////////docID/docsetID, annotation [set annotation for all docs in the given set]
int nMessages = 0;
Set<Document> docs;
Archive archive = JSPHelper.getArchive(request);
if (archive == null) {
    JSONObject obj = new JSONObject();
    obj.put("status", 1);
    obj.put("error", "No archive in session");
    out.println (obj);
    JSPHelper.log.info(obj);
    return;
}
String archiveID = SimpleSessions.getArchiveIDForArchive(archive);

if(request.getParameter("datasetId")!=null)
    docs = (Set<Document>) session.getAttribute(request.getParameter("datasetId"));
else if (request.getParameter("docId")!=null)
    docs = archive.getAllDocsAsSet().stream().filter(doc->{return doc.getUniqueId().equals(request.getParameter("docId"));}).collect(Collectors.toSet());
else
    {
    docs = (Set<Document>)archive.getAllDocs();
/*
    assert false : new AssertionError("You tried to apply flags only on a set of documents which is not set yet.");
    return;
*/
    }

if (docs != null)
{
    //annotation apply
    String annotationText = request.getParameter("annotation");
    if(!Util.nullOrEmpty(annotationText)){
    	docs.stream().forEach(d->{
    	                            EmailDocument ed = (EmailDocument)d;
    	                            ed.setComment(annotationText);
    	                            });
    }else{
    //labels apply
    Set<String> labelIds = Util.tokenize(request.getParameter("labelIDs"),",").stream().collect(Collectors.toSet());
    Util.softAssert(!Util.nullOrEmpty(labelIds),JSPHelper.log);
    String action = request.getParameter("action");
    if("set".equals(action)){
        archive.setLabels(docs,labelIds);
    }else if("unset".equals(action)){
        archive.unsetLabels(docs,labelIds);
    }else if("override".equals(action)){
        archive.putOnlyTheseLabels(docs,labelIds);
    }else{
        Util.softAssert(true,"Action parameter from the front end is allowed to contain only one of the following three options, set-unset-override",JSPHelper.log);
    }

    }

/*
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
	}*/
	nMessages= docs.size();
}

archive.clearAllAnnotationsCache();

JSONObject obj = new JSONObject();
obj.put("nMessages", nMessages);    	
out.println (obj);
JSPHelper.log.info(obj);
%>