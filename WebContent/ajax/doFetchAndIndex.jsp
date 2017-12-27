<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" %>
<%@page language="java" import="edu.stanford.muse.exceptions.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" %><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%
	// core JSP that does fetch, grouping and indexing
	// sets up archive in the session at the end

	session.setAttribute("statusProvider", new StaticStatusProvider("Starting up..."));
	boolean cancelled = false;
	JSONObject result = new JSONObject();
	String errorMessage = null;
	String resultPage = null;
	// simple flow means we're running off the login page and that we'll just use sent folders
	boolean simpleFlow = request.getParameter("simple") != null;
	MuseEmailFetcher m = (MuseEmailFetcher) JSPHelper.getSessionAttribute(session, "museEmailFetcher");
	// m can't be null here, the stores should already have been set up inside it

	try {
	    /* Commenting out the following lines for supporting archiveID. What if there is an archive in
	    the globalDirMap but we do not want to add mails to that?
	    Archive archive = JSPHelper.getArchive(request);
        if (archive == null) {
        	archive = SimpleSessions.prepareAndLoadArchive(m, request);
		}*/

	    Archive archive = SimpleSessions.prepareAndLoadArchive(m, request);
		// get archive id for this archive from archive mapper..

        String archiveID = SimpleSessions.getArchiveIDForArchive(archive);

		// step 1: fetch		
		Collection<EmailDocument> emailDocs = null;

		// note: folder=<server>^-^<sent folder> is already in the request
		String str = request.getParameter("downloadAttachments");
		// a little confusing here... some frontends omit downloadAttachments and some frontends send downloadAttachments=false
		boolean downloadAttachments = !(str == null || "false".equals(str));
		
		
		// now fetch and index... can take a while
		// we'll index messages even in slant mode... not strictly needed, but does not hurt.
		// rewrite for efficiency if slant becomes important.
		m.setupFetchers(-1);

		//get ownername, title, alternateemailaddr from the request..
	    String ownerName = request.getParameter("name");

		String archiveTitle = request.getParameter("archiveTitle");

		String alt = request.getParameter("alternateEmailAddrs");
		if (Util.nullOrEmpty(alt))
		    alt = (String) JSPHelper.getSessionAttribute(session, "alternateEmailAddrs");

		// could also uniquify the emailAddrs here

		archive.updateUserInfo(ownerName,archiveTitle,alt);

        for (EmailStore store : m.emailStores)
            if (!(Util.nullOrEmpty(store.emailAddress)))
                archive.addOwnerEmailAddr(store.emailAddress);
		// the emailStores session var has done its job (check login info and hold on to stores till MEF can take over) and can be removed		
    	session.removeAttribute("museEmailFetcher");


		// this is a special flag used during screening time to read only headers without the message bodies
		boolean downloadMessageText = !"false".equals(request.getParameter("downloadMessages"));

        JSPHelper.fetchAndIndexEmails(archive, m, request, session, downloadMessageText, downloadAttachments, simpleFlow); // download message text, maybe attachments, use default folders
		archive.postProcess();


		emailDocs = (List) archive.getAllDocs();
		AddressBook addressBook = archive.getAddressBook();
		//Set<String> ownNames = IndexUtils.readCanonicalOwnNames(addressBook); // TODO: to be executed somewhere? used to be passed to doIndexing which in turn passed it to recomputeCards.

		if (emailDocs == null) {
			// if we run out of memory parsing mbox files etc, emailDocs == null is usually the manifestation
			errorMessage = "You may not be running with enough memory. Please try again with more memory, or on a folder with fewer messages.";
		} else {
				resultPage = "browse-top?archiveId="+archiveID;
				SimpleSessions.saveArchive(archive);
			}
			try {
				String aStats = archive.getStats();
				JSPHelper.log.info("ARCHIVESTATS-1: " + aStats);
				Pair<String, String> p = Util.fieldsToCSV(archive.addressBook.getStats(), false);
				JSPHelper.log.info("ADDRESSBOOKSTATS-1: " + p.getFirst());
				JSPHelper.log.info("ADDRESSBOOKSTATS-2: " + p.getSecond());
				p = Util.fieldsToCSV(archive.getIndexStats(), false);
				JSPHelper.log.info("INDEXERSTATS-1: " + p.getFirst());
				JSPHelper.log.info("INDEXERSTATS-2: " + p.getSecond());
			} catch (Exception e) { }
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
		result.put("status", 0);
		result.put("cancelled", true);
	} else if (errorMessage == null) {
		result.put("status", 0);
		result.put("resultPage", resultPage);
	} else {
		result.put("status", 1);
		result.put("resultPage", "error");
		result.put("error", errorMessage);
	}
	out.println(result);
	// resultPage is set up to where we want to go next
	session.removeAttribute("statusProvider");
%>
