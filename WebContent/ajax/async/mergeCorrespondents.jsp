<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>

<%@ page import="org.json.JSONObject"%>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@ page import="edu.stanford.muse.AddressBookManager.Contact"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%>
<%@ page import="edu.stanford.muse.index.SearchResult"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.util.EmailUtils"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="java.util.Arrays"%>
<%@ page import="java.util.HashSet"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.LinkedHashSet"%>
<%@ page import="java.util.Set"%>
<%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.muse.ie.variants.EntityBookManager"%><%@ page import="java.io.File"%><%@ page import="edu.stanford.epadd.util.OperationInfo"%><%@ page import="java.util.function.Function"%><%@ page import="java.util.function.Consumer"%><%@ page import="edu.stanford.muse.epaddpremis.EpaddEvent"%>

<%
{
//<editor-fold desc="Setting up the operation object to execute this operation asynchronously">
//get the operation ID from the request parameter.
String encoding = request.getCharacterEncoding();
JSPHelper.log.info("request parameter encoding is " + encoding);

String actionName = request.getRequestURI();
String opID = request.getParameter("opID");
Multimap<String,String> paramMap = JSPHelper.convertRequestToMap(request);
//create a new operation object with the information necessary to run this long running async task.
final HttpSession fsession = session;
OperationInfo opinfo = new OperationInfo(actionName,opID,paramMap) {
    @Override
    public void onStart(JSONObject resultJSON) {
        //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
        //operationinfo object
        Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
        mergeCorrespondents(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
    }@Override
    public void onCancel() {
        //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
        //operationinfo object
        Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
        cancelMergeCorrespondents(setStatusProvider);
    }
};

//</editor-fold>

//<editor-fold desc="Store this operation in global map so that others can access this operation">
/*Map<String,OperationInfo> operationInfoMap = (Map<String,OperationInfo>) session.getAttribute("operationInfoMap");
if(operationInfoMap==null)
    operationInfoMap = new LinkedHashMap<>();
operationInfoMap.put(opID,opinfo);*/
JSPHelper.setOperationInfo(session,opID,opinfo);
//</editor-fold>

//<editor-fold desc="Starting the operation">
opinfo.run();
//when canelling this operation, from cancel.jsp call opinfo.cancel() method.
//when getting the status of this operation, call opinfo.getStatusProvider().getStatus() method.
//</editor-fold>
//just send an empty response telling that the operation has been started.
JSONObject obj = new JSONObject();
out.println(obj);
}
%>

<%!
/**
* Method that we want to execute asynchronously with progress display in the front end.
* paramsMap is the parameters passed to this ajax from the front end
* setStatusProvider is the method that will be called whenever a new logical operation starts and it's corresponding statusprovider
* need to be set in the operation info object.
*/
public void mergeCorrespondents(Multimap<String,String> paramsMap, Consumer<StatusProvider> setStatusProvider,HttpSession session,JSONObject resultJSON){
    // core JSP that does contact list merging and meta-data stuff

    setStatusProvider.accept(new StaticStatusProvider("Starting up..."));

    JSONObject result = new JSONObject();
    String error = null;
    boolean status = true;

    // Merge two or more correspondents and save them in address book
    String archiveID = JSPHelper.getParam(paramsMap,"archiveID");
    String mergeIDs = JSPHelper.getParam(paramsMap,"mergeIDs");

    //split mergeIDs on colon
    String[] mergeIDs_split = mergeIDs.split(",");
    Set<String> mergeContactID=new LinkedHashSet<>();
    Arrays.stream(mergeIDs_split).forEach(s->mergeContactID.add(s));

    if ("null".equals(archiveID)) {
        error = "Sorry, no archive ID found";
    } else if (mergeContactID.size() < 2 ) {
        error = "Sorry, two or more contact is required for merging";
    } else {
	    Archive archive = JSPHelper.getArchive(paramsMap);
        AddressBook ab = archive.getAddressBook();
        Contact newone = new Contact();
        Set<Contact> mergedContacts = new HashSet<Contact>();

         for (Iterator<String> it = mergeContactID.iterator(); it.hasNext(); ) {
            String contactID = it.next();
            int c_id = -1;
            try {
                c_id = Integer.parseInt(contactID);
            } catch (NumberFormatException e) {
                error = "invalid contact ID: " + c_id;
                break;
            }

            if (c_id >= 0) {
                Contact c = ab.getContact(c_id);

                if ( c == null) {
                    error = "No such Contact with c_id: " + c_id;
                    break;
                } else {
                    mergedContacts.add(c);
                }
            } else {
                error = "invalid contact ID: "+ c_id;
                break;
            }
        }

        // If there is no error received... merge contacts
        if (error == null) {
           //merge all contacts into a new combined one.
            mergedContacts.forEach(contact->{
                newone.merge(contact);
            });

		    //add contact to contactListForIds
			ab.contactListForIds.add(newone);

            // remove merged contacts
            ab.removeContacts(mergedContacts);

            //fill summary objects.
            setStatusProvider.accept(new StaticStatusProvider("Updating statistics of sent/received/received from own summary ..."));
            ab.fillL1_SummaryObject(archive.getAllDocs());

            setStatusProvider.accept(new StaticStatusProvider("Recomputing address book..."));
            EmailDocument.recomputeAddressBook(archive,new LinkedHashSet<>()); //because owner has changed and recomputation assumes owner as a trusted address. So we need to recompute it.

            try {
                archive.recreateCorrespondentAuthorityMapper(); // we have to recreate auth mappings since they may have changed
                ArchiveReaderWriter.saveAddressBook(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
                ArchiveReaderWriter.saveCorrespondentAuthorityMapper(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
                String mergedContactsString = "";
                int i = 1;
                for (Contact c : mergedContacts)
                {
                    mergedContactsString += "Contact " + i + ": ";
                    i++;
                    mergedContactsString += "Name(s): ";
                    int j = 0;
                    for (String name : c.getNames())
                    {
                        if (j != 0)
                        {
                            mergedContactsString += ", ";
                        }
                        j++;
                        mergedContactsString += (name);
                    }
                    j = 0;
                    mergedContactsString += " Email(s): ";
                    for (String email : c.getEmails())
                    {
                        if (j != 0)
                        {
                            mergedContactsString += ", ";
                        }
                        j++;
                        mergedContactsString += (email);
                    }
                    mergedContactsString += " ";
                }
                archive.epaddPremis.createEvent(EpaddEvent.EventType.MERGE_CORRESPONDENTS, mergedContactsString, "success");
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

    }

    if (error != null) {
       resultJSON.put("status", 1);
       resultJSON.put ("error", error);
    } else {
        resultJSON.put ("status", 0);
    }

}
%>

<%!
/**
* Method that we want to execute when this operation is cancelled by the front end.
*/
public void cancelMergeCorrespondents(Consumer<StatusProvider> setStatusProvider){

}
%>
