<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@ page import="edu.stanford.epadd.util.OperationInfo" %>
<%@ page import="org.json.JSONObject" %>
<%@page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="edu.stanford.muse.email.GmailAuth.AuthenticatedUserInfo"%>
<%@ page import="edu.stanford.muse.email.GmailAuth.GoogleLoginVerifier"%><%@ page import="java.util.List"%><%@ page import="java.util.Arrays"%><%@ page import="java.util.function.Consumer"%><%@ page import="com.google.common.collect.Multimap"%><%@ page import="edu.stanford.muse.index.Archive"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="edu.stanford.muse.index.EmailDocument"%><%@ page import="java.util.Collection"%><%@ page import="edu.stanford.muse.util.Util"%><%@ page import="edu.stanford.muse.exceptions.CancelledException"%><%@ page import="java.io.File"%><%@ page import="edu.stanford.muse.ie.variants.EntityBookManager"%><%@ page import="edu.stanford.muse.index.Lexicon"%>
<%@include file="../../gmail-auth-check.jspf" %>

<%

{
    //<editor-fold desc="Setting up the operation object to execute this operation asynchronously">
    //get the operation ID from the request parameter.
    String encoding = request.getCharacterEncoding();
    JSPHelper.log.info("request parameter encoding is " + encoding);

    String actionName = request.getRequestURI();
    String opID = request.getParameter("opID");
    Multimap<String,String> paramMap = JSPHelper.convertRequestToMap(request);
    //get startdate to fetch and index.
    String startDate = request.getParameter("startDate");
    //get end date to fetch and index.
    String endDate = request.getParameter("endDate");
 //create musemailfetcher.
    MuseEmailFetcher mf = new MuseEmailFetcher();
    /*GmailStore gmailStore  = new GmailStore(authenticatedUserInfo);
    gmailStore.computeFoldersAndCounts(null);
    mf.addGmailAccount(gmailStore);*/
    mf.addServerAccount(null,null,null,authenticatedUserInfo.getUserName(),"xoauth"+authenticatedUserInfo.getAccessToken(),true);
    mf.emailStores.iterator().next().computeFoldersAndCounts("/Users/tech/epadd-appraisal/user");
    mf.name=authenticatedUserInfo.getDisplayName();
    mf.alternateEmailAddrs=authenticatedUserInfo.getUserName();
    mf.archiveTitle=authenticatedUserInfo.getUserName()+"-Gmail";

    //create a new operation object with the information necessary to run this long running async task.
    final HttpSession fsession = session;
    session.setAttribute("museEmailFetcher",mf);
    OperationInfo opinfo = new OperationInfo(actionName,opID,paramMap) {
        @Override
        public void onStart(JSONObject resultJSON) {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            doFetchAndIndex(this.getParametersMap(),setStatusProvider,fsession,resultJSON);
        }@Override
        public void onCancel() {
            //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
            //operationinfo object
            Consumer<StatusProvider> setStatusProvider = statusProvider->this.setStatusProvider(statusProvider);
            cancelFetchAndIndex(setStatusProvider);
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
public void doFetchAndIndex(Multimap<String,String> paramsMap, Consumer<StatusProvider> setStatusProvider,HttpSession session,JSONObject resultJSON){
// core JSP that does fetch, grouping and indexing
	// sets up archive in the session at the end

	//---session.setAttribute("statusProvider", new StaticStatusProvider("Starting up..."));
	setStatusProvider.accept(new StaticStatusProvider("Starting up..."));

	boolean cancelled = false;
	String errorMessage = null;
	String resultPage = null;
	// simple flow means we're running off the login page and that we'll just use sent folders
	boolean simpleFlow = JSPHelper.getParam(paramsMap,"simple") != null;
	MuseEmailFetcher m = (MuseEmailFetcher) JSPHelper.getSessionAttribute(session, "museEmailFetcher");
	// m can't be null here, the stores should already have been set up inside it

	try {
	    /* Commenting out the following lines for supporting archiveID. What if there is an archive in
	    the globalDirMap but we do not want to add mails to that?
	    Archive archive = JSPHelper.getArchive(request);
        if (archive == null) {
        	archive = SimpleSessions.prepareAndLoadArchive(m, request);
		}*/

	    Archive archive = ArchiveReaderWriter.prepareAndLoadArchive(m, paramsMap);
		// get archive id for this archive from archive mapper..

        String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);

		// step 1: fetch
		Collection<EmailDocument> emailDocs = null;

		// note: folder=<server>^-^<sent folder> is already in the request
		String str = JSPHelper.getParam(paramsMap,"downloadAttachments");
		// a little confusing here... some frontends omit downloadAttachments and some frontends send downloadAttachments=false
		boolean downloadAttachments = !(str == null || "false".equals(str));


		// now fetch and index... can take a while
		// we'll index messages even in slant mode... not strictly needed, but does not hurt.
		// rewrite for efficiency if slant becomes important.
		m.setupFetchers(-1);

		//get ownername, title, alternateemailaddr from the request..
	    String ownerName = m.name;
        String archiveTitle = m.archiveTitle;
        String alternameEmailAddrs = m.alternateEmailAddrs;

		archive.updateUserInfo(ownerName,archiveTitle,alternameEmailAddrs);

        for (EmailStore store : m.emailStores)
            if (!(Util.nullOrEmpty(store.emailAddress)))
                archive.addOwnerEmailAddr(store.emailAddress);
		// the emailStores session var has done its job (check login info and hold on to stores till MEF can take over) and can be removed
    	session.removeAttribute("museEmailFetcher");


		// this is a special flag used during screening time to read only headers without the message bodies
		boolean downloadMessageText = !"false".equals(JSPHelper.getParam(paramsMap,"downloadMessages"));

        JSPHelper.fetchAndIndexEmails(archive, m, paramsMap, session, downloadMessageText, downloadAttachments, simpleFlow,setStatusProvider); // download message text, maybe attachments, use default folders
		archive.postProcess();

//assign threadids'
        archive.assignThreadIds();

		emailDocs = (List) archive.getAllDocs();
		/*AddressBook addressBook = archive.getAddressBook();
		//Set<String> ownNames = IndexUtils.readCanonicalOwnNames(addressBook); // TODO: to be executed somewhere? used to be passed to doIndexing which in turn passed it to recomputeCards.

        //add labels to messages which encountered errors during addressbook building.
        setStatusProvider.accept(new StaticStatusProvider("Assigning system labels to messages..."));

        Multimap<String,String> dataErrorsMap = addressBook.getDataErrorsMap();
        for(Map.Entry<String,String> entry: dataErrorsMap.entries()){
            Set<String> labels = new LinkedHashSet<>();
            labels.add(entry.getValue());
            archive.getLabelManager().setLabels(entry.getKey(),labels);
        }*/

		if (emailDocs == null) {
			// if we run out of memory parsing mbox files etc, emailDocs == null is usually the manifestation
			errorMessage = "You may not be running with enough memory. Please try again with more memory, or on a folder with fewer messages.";
		} else {
				resultPage = "browse-top?archiveID="+archiveID;
				//and for entityDoc map as well.//not needed.. just make sure that entitybookmanager is also loaded.
				            String dir = archive.baseDir + File.separatorChar + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.SESSIONS_SUBDIR;

				            String entityBookPath = dir + File.separatorChar + Archive.ENTITYBOOKMANAGER_SUFFIX;
        JSPHelper.log.info("Archive created- Now saving it!!");
        setStatusProvider.accept(new StaticStatusProvider("Saving Archive..."));

				            				ArchiveReaderWriter.saveArchive(archive,Archive.Save_Archive_Mode.FRESH_CREATION);
        JSPHelper.log.info("Archive saved");

        JSPHelper.log.info("Creating entitybooks by reading lucene index.");
        setStatusProvider.accept(new StaticStatusProvider("Finishing soon..."));
				EntityBookManager entityBookManager = EntityBookManager.readObjectFromFiles(archive,entityBookPath);//this one ensures that we have entitybookmanager filled
        JSPHelper.log.info("Entitybooks created successfully");
				archive.setEntityBookManager(entityBookManager);
				ArchiveReaderWriter.saveEntityBookManager(archive,Archive.Save_Archive_Mode.FRESH_CREATION);
				JSPHelper.log.info("Entitybooks saved successfully (first time)");

				//After archive is saved recreate the cache.. For addressbook the cache gets created inside saveAddressBook.
				//Do it for Lexicon cache.
			/*	JSPHelper.log.info("Filling lexicon summaries");
				Lexicon.fillL1_Summary_all(archive,false);
				JSPHelper.log.info("Lexicon summaries filled successfully");

				// (for all entities and also the summary objects- used for cache)
                //Archive.cacheManager.cacheEntitiesListing(archiveID);*/

			}
			/*try {
				String aStats = archive.getStats();
				JSPHelper.log.info("ARCHIVESTATS-1: " + aStats);
				Pair<String, String> p = Util.fieldsToCSV(archive.addressBook.getStats(), false);
				JSPHelper.log.info("ADDRESSBOOKSTATS-1: " + p.getFirst());
				JSPHelper.log.info("ADDRESSBOOKSTATS-2: " + p.getSecond());
				p = Util.fieldsToCSV(archive.getIndexStats(), false);
				JSPHelper.log.info("INDEXERSTATS-1: " + p.getFirst());
				JSPHelper.log.info("INDEXERSTATS-2: " + p.getSecond());
			} catch (Exception e) { }*/

	} catch (CancelledException ce) {
		// op was cancelled, so just go back to where we must have been
		JSPHelper.log.warn("Fetch groups and indexing cancelled by user");
		// need to be careful with incremental indexing here...
		// may need to archive.clear();
		cancelled = true;
	} catch (Throwable t) {
		Util.print_exception("Exception fetching/indexing emails", t, JSPHelper.log);
		if (t instanceof OutOfMemoryError)
		    errorMessage = "Ran out of memory. Please re-run ePADD with more memory";
		else
    		errorMessage = "Exception fetching/indexing emails";
		// we'll leave archive in this
	}

	if (cancelled) {
		resultJSON.put("status", 0);
		resultJSON.put("cancelled", true);
		resultJSON.put("responseText", "Operation cancelled by the user");
		setStatusProvider.accept(new StaticStatusProvider("Operation cancelled by the user"));
	} else if (errorMessage == null) {
		resultJSON.put("status", 0);
		resultJSON.put("resultPage", resultPage);
		resultJSON.put("responseText", "Indexed successfully");
		setStatusProvider.accept(new StaticStatusProvider("Indexed successfully"));
	} else {
		resultJSON.put("status", 1);
		resultJSON.put("resultPage", "error");
		resultJSON.put("error", errorMessage);
		resultJSON.put("responseText", errorMessage);
		setStatusProvider.accept(new StaticStatusProvider(errorMessage));
	}
	//out.println(result);
	// resultPage is set up to where we want to go next
	//session.removeAttribute("statusProvider");
}
%>


<%!
/**
* Method that we want to execute when this operation is cancelled by the front end.
*/
public void cancelFetchAndIndex(Consumer<StatusProvider> setStatusProvider){

}
%>