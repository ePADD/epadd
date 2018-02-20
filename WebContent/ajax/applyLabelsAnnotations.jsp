<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>    
<%@page language="java" import="java.util.*"%>    
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>    
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="java.util.stream.Collectors"%>
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

Collection<Document> docs;
if(!Util.nullOrEmpty(docsetID)) {
    DataSet dataset = (DataSet) session.getAttribute(docsetID);
    docs = dataset.getDocs();
} else if (!Util.nullOrEmpty(docID))
    docs = archive.getAllDocsAsSet().stream().filter(doc->doc.getUniqueId().equals(docID)).collect(Collectors.toSet());
else
    docs = archive.getAllDocs();

boolean error = false;
String errorMessage = "";

if (docs != null)
{
    //annotation apply
    String annotationText = request.getParameter("annotation");
    if(!Util.nullOrEmpty(annotationText)){
        Set<String> docids = docs.stream().map(doc->doc.getUniqueId()).collect(Collectors.toSet());
        archive.getAnnotationManager().setAnnotationToAll(docids,annotationText);
    	archive.clearAllAnnotationsCache();
    }else{
        //labels apply
        Set<String> labelIds = Util.tokenize(request.getParameter("labelIDs"),",").stream().collect(Collectors.toSet());
        // Util.softAssert(!Util.nullOrEmpty(labelIds),JSPHelper.log);
        String action = request.getParameter("action");
        if("set".equals(action)){
            Pair<Integer,String> status = archive.setLabels(docs,labelIds);
            if(status.first>0)
             {
                 error=true;
                errorMessage = status.second;
                Util.softAssert(true, errorMessage,JSPHelper.log);
            }

        }else if("unset".equals(action)){
            archive.unsetLabels(docs,labelIds);
        }else if("override".equals(action)){
            Pair<Integer,String> status = archive.putOnlyTheseLabels(docs,labelIds);
            if(status.first>0)
             {
                 error=true;
                errorMessage = status.second;
                Util.softAssert(true, errorMessage,JSPHelper.log);
            }
        } else {
            error = true;
            errorMessage = "Action parameter is allowed to contain only one of the following three options: set, unset, override. Unknown action: " + action;
            Util.softAssert(true, errorMessage,JSPHelper.log);
        }

    }
	nMessages= docs.size();
}


JSONObject obj = new JSONObject();
obj.put("status", error ?  1 : 0);
if (error)
    obj.put ("errorMessage", errorMessage);
obj.put("nMessages", nMessages);
out.println (obj);
JSPHelper.log.info("AJAX response: " + obj);
%>