<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="java.util.stream.Collectors"%>
<%@ page import="org.apache.lucene.document.Field"%>

<%
// does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
JSPHelper.setPageUncacheable(response);

//////////////// The request can be of the form docID/docsetID, labels, action=set/unset/only [set/unset/only keep label for all docs in the given set], or
// ///////////////docID/docsetID, annotation [set annotation for all docs in the given set]
int nMessages = 0;

Archive archive = JSPHelper.getArchive(request);
if (archive == null) {
    JSONObject obj = new JSONObject();
    obj.put("status", 1);
    obj.put("error", "No archive in session");
    out.println (obj);
    JSPHelper.log.info(obj);
    return;
}
String docsetID = request.getParameter("docsetID");
String docID = request.getParameter("docId");
boolean error = false;
String errorMessage = "";
Collection<Document> docs = null;

if (!Util.nullOrEmpty(docID))
{
    docs = archive.getAllDocsAsSet().stream().filter(doc->doc.getUniqueId().equals(docID)).collect(Collectors.toSet());
}
else
{
	error = true;
	errorMessage = "No docID";
}
if (docs != null)
{
    String emailBodyText = request.getParameter("modifiedEmail");
    if(emailBodyText!=null)
    {
        Set<String> docids = docs.stream().map(doc->doc.getUniqueId()).collect(Collectors.toSet());
 	    String id2 = "";
    	for (String id : docids)
    	{
    		id2 = id;
    	}
    	archive.openForRead();
        archive.setupForWrite();
        org.apache.lucene.document.Document ldoc = archive.getLuceneDoc(id2);

        // clone the original email content to new field for preservation
        String originalContent = ldoc.get("body");
        ldoc.add(new Field("body-preserved", originalContent, Indexer.full_ft));

        // update (remove and then add back) email content with redacted content
       ldoc.removeField("body");
       ldoc.add(new Field("body", emailBodyText, Indexer.full_ft));

       archive.updateDocument(ldoc);
       EmailDocument ed = archive.docForId(id2);
       archive.close();
       //prepare to read again.
       archive.openForRead();
    }
}


JSONObject obj = new JSONObject();
obj.put("status", error ?  1 : 0);
if (error)
    obj.put ("errorMessage", errorMessage);
obj.put("nMessages", nMessages);
out.println (obj);
JSPHelper.log.info("AJAX response: " + obj);
%>