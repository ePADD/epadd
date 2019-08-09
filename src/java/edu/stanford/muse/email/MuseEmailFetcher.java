/*
 Copyright (C) 2012 The Stanford MobiSocial Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package edu.stanford.muse.email;


import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.exceptions.CancelledException;
import edu.stanford.muse.exceptions.MboxFolderNotReadableException;
import edu.stanford.muse.exceptions.NoDefaultFolderException;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import groovy.lang.Tuple2;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Consumer;

/** important class -- this is the primary class used by clients to fetch email with muse.
 * fetches email from multiple accounts.
 * here's the flow for more elaborate options (custom servers, multiple accounts):
 * call the add*accounts to specify the accounts.
 * then call getFolderInfosAsJson(idx) for each account to get the folder list if needed.
 * then call setupFetchers()
 * then call fetchAndIndexEmails() */

public class MuseEmailFetcher {
    private static final Log log = LogFactory.getLog(MuseEmailFetcher.class);

    public String name, archiveTitle, alternateEmailAddrs; // temp storage--- IMP: Primary email address on email-sources page is named as alternateEmailAddrs
	public Set<String> emailSources; //temp
    private transient List<MTEmailFetcher> fetchers;
	public transient List<EmailStore> emailStores = new ArrayList<>();

	/////////////////////////// account setup stuff
	
	/** clear current emailstores */
	public synchronized void clearAccounts()
	{
		emailStores = new ArrayList<>();
		fetchers = new ArrayList<>();
	}
	
	private synchronized void addEmailStore(EmailStore stores) // should we call this addAccount
	{
		int initialSize = emailStores.size();
		// we could check for duplicates here
		emailStores.add(stores);
		log.info("Email fetcher went from " + initialSize + " stores to " + emailStores.size());
	}
	
	/** sets up fetchers for the emailstores */
	public void setupFetchers(int last_N_msgs)
	{
		// create the fetchers (not connecting yet)
	    fetchers = new ArrayList<>();
	    for (EmailStore store: emailStores)
	    {
	    	MTEmailFetcher f = new MTEmailFetcher(1, store, last_N_msgs);
	    	fetchers.add (f); // # of threads
	    }
	}
	
	public int totalMessagesInSelectedFolders()
	{
		int n = 0;
		for (MTEmailFetcher fetcher: fetchers)
			n += fetcher.getTotalMessagesInAllFolders();
		return n;
	}

	/////////////////////////////////////
	
	public int getNAccounts()
	{
		if (Util.nullOrEmpty(emailStores))
			return 0;
		return emailStores.size();
	}

	public String getFolderInfosAsJson(int accountIdx)
	{
		return emailStores.get(accountIdx).getFolderInfosAsJson();
	}

	public boolean folderInfosAvailable(int accountIdx)
	{
		return emailStores.get(accountIdx).isFolderCountReadingComplete();
	}

	public List<FolderInfo> readFoldersInfos(int accountIdx, String foldersAndCountsCacheDir)
	{
		EmailStore store = emailStores.get(accountIdx);
		try {
			store.doneReadingFolderCounts = false;
			store.computeFoldersAndCounts(foldersAndCountsCacheDir);
			return store.folderInfos;
		} catch (Exception e) {
			store.doneReadingFolderCounts = true;
			String failMessage = getUserDisplayableMessageForException(store, e);
			throw new RuntimeException(failMessage);
		}
	}

	public String getDisplayName(int accountIdx)
	{
		return emailStores.get(accountIdx).getDisplayName();
	}


	/**
	 * set up account for a server-based a/c, either:
	 * a) <server, login, password> for an a/c where the imap server is specified.
	 * or if server is null:
	 * b) <email addr, password> for a standard server like G/Y/H/Stanford
	 * c) <email addr, password> for an a/c whose info is known to Thunderbird
	 * Note: emailAddress is slightly overloaded (can be a plain login name also in case server is specified)
	 * imapServer is ignored (and can be null) for a well-known email address like Gmail/Y/H/Stanford etc.
	 * @throws JSONException 
	 * returns true if the a/c was actually created.
	 */
	public synchronized JSONObject addServerAccount(String server, String protocol, String defaultFolderName, String emailAddress, String password, boolean sentOnly) throws JSONException
	{
		String loginName = emailAddress;
		EmailStore emailStore;
		// if emailaddress is from a known domain, set up imap/pop server
		boolean is_gmail_or_gapps = emailAddress.endsWith("@gmail.com") || emailAddress.endsWith("@googlemail.com") || "imap.gmail.com".equals(server);
		if (is_gmail_or_gapps)// check for imap.gmail.com 'cos this could be a gapps account without ending in @gmail.com. we want same config. in this case
		{
			server = "imap.gmail.com";
			protocol = "imaps";
			defaultFolderName = "[Gmail]/Sent Mail";
			log.info("adding gmail or gapps account for " + emailAddress);
		}

		if ("webmail.bcp.org".equals(server))  // bellarmine special
		{
			defaultFolderName = "Sent items";
			protocol = "imaps";
		}
		
		// guess the server if we have a null server
		if (Util.nullOrEmpty(server) && !Util.nullOrEmpty(emailAddress))
		{
			emailAddress = emailAddress.trim();
			if (emailAddress.endsWith("@live.com") || emailAddress.endsWith("@hotmail.com"))
			{
				server = "pop3.live.com";
				defaultFolderName = "INBOX";
				loginName = emailAddress; // special case for hotmail
			}
			else if (emailAddress.endsWith("@yahoo.com") || emailAddress.endsWith("@ymail.com"))
			{
				server = "imap.mail.yahoo.com";
				protocol = "imaps";
				defaultFolderName = "Sent";
			}
			else if (emailAddress.endsWith("@cs.stanford.edu"))
			{
				// monica special
				server = "lam@cs.stanford.edu".equals(emailAddress) ? "csl-mail.stanford.edu" : "xenon.stanford.edu";
				protocol = "imaps";
//				protocol = "imap";
				defaultFolderName = "Sent";
			}
			else if (emailAddress.endsWith("@stanford.edu"))
			{
				String login = EmailUtils.getLoginFromEmailAddress(emailAddress);
				server = login + ".pobox.stanford.edu";
				protocol = "imaps";
				defaultFolderName = "Sent";
			}
		}
				
		// saw some logs that people just typed in server name as "gmail"
		// so help them out a bit
		if ("gmail".equalsIgnoreCase(server))
			server = "imap.gmail.com";
		else if ("yahoo".equalsIgnoreCase(server) || "ymail".equalsIgnoreCase(server)) 
			server = "imap.mail.yahoo.com";
		else if ("hotmail".equalsIgnoreCase(server) || "microsoft".equalsIgnoreCase(server) || "live".equalsIgnoreCase(server))
			server = "pop3.live.com";
		
		JSONObject result = new JSONObject();

		// if still no server, we don't know what do
		if (Util.nullOrEmpty(server))
		{
			result.put("status", 1);
			result.put("errorMessage", "No server found");
			return result;
		}
		
		// set up the server
		
		// only imap/pop
		// we don't want to support plain imap/pop and get into explaining the security implications to end-users
		if (Util.nullOrEmpty(protocol))
		{
		    protocol = "imaps";
		    if (server.startsWith("pop"))
		        protocol = "pop3s";
		}	
		
	    ImapPopConnectionOptions connection = new ImapPopConnectionOptions(protocol, server, -1, loginName, password);
	    emailStore = new ImapPopEmailStore(connection, emailAddress);
	    if (!Util.nullOrEmpty(defaultFolderName))
	    {
	    	emailStore.addDefaultFolderName(defaultFolderName);
	    	if (sentOnly)
	    	{
		    	result.put("defaultFolder", defaultFolderName);
		    	int count = -1;
		    	try {
		    		count = emailStore.getNMessages(defaultFolderName);
		    	} catch (Exception e) {
		    		log.warn("Exception reading #messages for " + defaultFolderName);
		    		Util.print_exception(e, log);
		    	}
		    	result.put("defaultFolderCount", count);
	    	}
	    }

		String s = doConnect(emailStore);
		if (!Util.nullOrEmpty(s))
		{
			result.put("status", 1);
			result.put("errorMessage", s);
		}
		else
			result.put("status", 0);
		return result;
	}
	
	/** add mbox stores, given comma separated email directories. 
	 * if localFolders, sets the display name of the store to "Local Folders" instead of the full path 
	 * @throws MboxFolderNotReadableException */
	public synchronized String addMboxAccount(String accountKey, String mailDirs, boolean localFolders) throws IOException {
		if (Util.nullOrEmpty(mailDirs))
			return null;
		EmailStore emailStore = new MboxEmailStore(accountKey, localFolders ? "Local Folders" : mailDirs, mailDirs);
		return doConnect(emailStore);
	}

	/*
	This method was added by Chinmay to add the support for fetching and indexing messages directly from gmail.
	It slightly differs in uniformity from the above method (addMboxAccount) in the sense that here the  client creates
	gmailStore object and passes to this function whereas in the above method client only passes some information about the account
	and the method creates MboxEmailStore object. Chinmay's rationale of doing it is if the client already knows
	which method to invoke (addMobx.. or addGmail..) then she may as well pass the constructed store object. Nothing important for now
	but just a brooding thought (sitting in the Dehradun airport I had ample time to document this :) ).
	 */
	public synchronized String addGmailAccount(GmailStore gmailStore){
		return doConnect(gmailStore);

	}

	private String doConnect(EmailStore emailStore)
	{
		// now actual login
		try {
			if (emailStore != null)
			{
				emailStore.connect();
				log.info ("Successful login for account: " + emailStore);
				addEmailStore(emailStore);
			}
		} catch (Exception e)
		{
			return getUserDisplayableMessageForException(emailStore, e);
		}

		return "";		
	}
	
	/** utility method for converting an exception encountered in this fetcher to something that can be shown to the user */
	private static String getUserDisplayableMessageForException(EmailStore store, Exception e)
	{
		String failMessage;

		if (e instanceof AuthenticationFailedException)
		{
			failMessage =  "Invalid password for " + store.getDisplayName() + ". Please try again.";
			log.warn ("Login failed, cause: " + failMessage + "\n" + Util.stackTrace(e));
		}
		else if (e instanceof MessagingException)
		{
			Throwable cause = e.getCause();
			if (cause != null && cause instanceof UnknownHostException)
			{
				if (store instanceof ImapPopEmailStore)
				{
					ImapPopEmailStore ipes = (ImapPopEmailStore) store;
					failMessage = "Unable to contact host: " + ipes.getServerHostname();
				}
				else
					failMessage = "Unknown Host. Not expected with a " + store.getClass().getName() + ". Hmmm...";
			}
			else
			{
				String server = "";
				if (store instanceof ImapPopEmailStore)
				{
					ImapPopEmailStore ipes = (ImapPopEmailStore) store;
					server = "Unknown server: " + ipes.getServerHostname();
				}
				failMessage = "Unable to communicate with server " + server + ": " + e.getMessage() + ". \n";
				if (cause != null)
					failMessage += "Cause: " + cause + "\n";
			}
			log.warn ("Login failed, cause: " + failMessage + "\n" + Util.stackTrace(e));
		}
		else
		{
			Throwable cause = e.getCause();
			failMessage = "Internal error, " + cause;
			log.error ("Exception trying to access folders: \n" + e + " : " + Util.stackTrace(e));
		}
		return failMessage;
	}
	
	/** sets up folderInfo's for each fetcher based on the given request params in allFolders (or using default if useDefaultFolders)
	 * selectedFolders folders are in the <account name>^-^<folder name> format from folders.jsp
	 * if using default folders, selectedFolders can be null
	 */
	private void setupFoldersForFetchers(List<MTEmailFetcher> fetchers, String[] selectedFolders, boolean useDefaultFolders) throws NoDefaultFolderException
	{
	    // now compute foldersForEachFetcher
	    Map<String, Integer> accountNameToFetcherIdx = new LinkedHashMap<>();
	    List<List<String>> foldersForEachFetcher = new ArrayList<>();
	    for (int i = 0; i < fetchers.size(); i++)
	    {
	    	accountNameToFetcherIdx.put (emailStores.get(i).getDisplayName(), i);
	    	foldersForEachFetcher.add(new ArrayList<>());
	    }

	    if (selectedFolders != null)
	    {
			// convert iso8859 to utf-8, e.g. see http://forums.sun.com/thread.jspa?threadID=5362133
			// important to convert encoding for unicode folders
			// convert iso8859 to utf-8, e.g. see http://forums.sun.com/thread.jspa?threadID=5362133
			// and http://illegalargumentexception.blogspot.com/2009/05/java-rough-guide-to-character-encoding.html
			// and http://java.sun.com/developer/technicalArticles/Intl/HTTPCharset/

		    // tokenize the folder params to separate account name from folder name
			final String STORE_FOLDER_SEPARATOR = "^-^";
			for (String folder: selectedFolders)
			{
				// example: folder = GMail^-^MyFolder

				int idx = folder.indexOf(STORE_FOLDER_SEPARATOR);
				if (idx == -1)
				{
					log.error("Bad folder name received: " + folder);
					getDataErrors().add("Bad folder name: Content not parsed for "+folder);
					continue;
				}
				String accountName = folder.substring (0, idx); // example: GMail
				String folderName = folder.substring (idx + STORE_FOLDER_SEPARATOR.length()); // example: MyFolder
				Integer I = accountNameToFetcherIdx.get(accountName);
				if (I == null)
				{
					log.error("Bad account name: " + accountName + " in folder name: " + folder);
					getDataErrors().add("Bad account name: for account "+accountName);
					continue;
				}
				foldersForEachFetcher.get(I).add(folderName);
			}
	    }

		// now we have foldersForEachFetcher
		// fetchers could be run concurrently.
		// but... need to worry about synchronizing filedatastore
		// since all fetchers share it
		for (int i = 0; i < fetchers.size(); i++)
		{
			MTEmailFetcher fetcher = fetchers.get(i);
		    fetcher.clearFolderNames();

			List<String> foldersForThisFetcher = foldersForEachFetcher.get(i);
			boolean usingDefault = false;
			if (useDefaultFolders && (foldersForThisFetcher == null || foldersForThisFetcher.size() == 0))
			{
				foldersForThisFetcher = fetcher.getDefaultFolderNames();
				usingDefault = true;
			}

			if (foldersForThisFetcher == null || foldersForThisFetcher.size() == 0)
				continue;

		    for (String folder: foldersForThisFetcher)
			    fetcher.addFolderNameAndComputeMessageCount(folder);

		    if (usingDefault) {
		    	for (FolderInfo fi: fetcher.folderInfos)
		    		if (fi.messageCount == -1)
		    			throw new NoDefaultFolderException(fetcher.toString(), fi.longName);
		    }
		}
	}

	/** ensures passwords do not get saved with serialization */
	public void wipePasswords() 
	{
		if (emailStores == null)
			return;
		for (EmailStore store: emailStores)
			store.wipePasswords();
	}
	
	/** returns the effective userKey we should use -- usually just the key of the first email store. might have more sophisticated mapping policies in the future.  */
	public String getEffectiveUserKey()
	{
		if (Util.nullOrEmpty(emailStores))
			return "";
		
		String userKey = "";
		// go through the stores till we have a non-empty key
		for (EmailStore store: emailStores)
		{
			String e = store.emailAddress;
			if (!Util.nullOrEmpty(e))
			{
				userKey = e;
				break;
			}
		}
		return userKey;
	}
	
    /** key method to fetch actual email messages. can take a long time.
     * @param session is used only to set the status provider object. callers who do not need to track status can leave it as null
     * @param selectedFolders is in the format <account name>^-^<folder name>
     * @param session is used only to put a status object in. can be null in which case status object is not set.
     * emailDocs, addressBook and blobstore
     * @throws NoDefaultFolderException 
     * */
	public void fetchAndIndexEmails(Archive archive, String[] selectedFolders, boolean useDefaultFolders, FetchConfig fetchConfig, HttpSession session, Consumer<StatusProvider> setStatusProvider)
				throws InterruptedException, JSONException, NoDefaultFolderException, CancelledException
	{
		setupFetchers(-1);
		
		long startTime = System.currentTimeMillis();

		setStatusProvider.accept(new StaticStatusProvider("Starting to process messages..."));
		//if (session != null)
			//session.setAttribute("statusProvider", new StaticStatusProvider("Starting to process messages..."));

		boolean op_cancelled = false, out_of_mem = false;
		
		BlobStore attachmentsStore = archive.getBlobStore();
		fetchConfig.downloadAttachments = fetchConfig.downloadAttachments && attachmentsStore != null;

		if (Util.nullOrEmpty(fetchers))
		{
			log.warn ("Trying to fetch email with no fetchers, setup not called ?");
			return;
		}
		
	    setupFoldersForFetchers(fetchers, selectedFolders, useDefaultFolders);
	    
	    List<FolderInfo> fetchedFolderInfos = new ArrayList<>();
	    
	    // one fetcher will aggregate everything
		FetchStats stats = new FetchStats();
		MTEmailFetcher aggregatingFetcher = null;

		// a fetcher is one source, like an account or a top-level mbox dir. A fetcher could include multiple folders.
		long startTimeMillis = System.currentTimeMillis();
	    for (MTEmailFetcher fetcher: fetchers)
	    {
	    	// in theory, different iterations of this loop could be run in parallel ("archive" access will be synchronized)

			setStatusProvider.accept(fetcher);

			/*if (session != null)
				session.setAttribute("statusProvider", fetcher);
*/
			fetcher.setArchive(archive);
	    	fetcher.setFetchConfig(fetchConfig);
		    log.info("Memory status before fetching emails: " + Util.getMemoryStats());

		    List<FolderInfo> foldersFetchedByThisFetcher = fetcher.run(); // this is the big call, can run for a long time. Note: running in the same thread, its not fetcher.start();

			// if fetcher was cancelled or out of mem, bail out of all fetchers
			// but don't abort immediately, only at the end, after addressbook has been built for at least the processed messages
			if (fetcher.isCancelled())
			{
				log.info ("NOTE: fetcher operation was cancelled");
				op_cancelled = true;
				break;
			}
			
			if (fetcher.mayHaveRunOutOfMemory())
			{
				log.warn ("Fetcher operation ran out of memory " + fetcher);
				out_of_mem = true;
				break;
			}

			fetchedFolderInfos.addAll(foldersFetchedByThisFetcher);

			if (aggregatingFetcher == null && !Util.nullOrEmpty(foldersFetchedByThisFetcher))
				aggregatingFetcher = fetcher; // first non-empty fetcher

			if (aggregatingFetcher != null)
				aggregatingFetcher.merge (fetcher);

			// add the indexed folders to the stats
			EmailStore store = fetcher.getStore();
			String fetcherDescription = store.displayName + ":" + store.emailAddress;
			for (FolderInfo fi: fetchedFolderInfos)
				stats.selectedFolders.add(new Pair<>(fetcherDescription, fi));
		}

		if (op_cancelled)
			throw new CancelledException();
		if (out_of_mem)
			throw new OutOfMemoryError();

		if (aggregatingFetcher != null) {
			stats.importStats = aggregatingFetcher.stats;
			if (aggregatingFetcher.mayHaveRunOutOfMemory())
				throw new OutOfMemoryError();
		}
        aggregatingFetcher = null; // save memory

        long endTimeMillis = System.currentTimeMillis();
		long elapsedMillis = endTimeMillis - startTimeMillis;
	    log.info(elapsedMillis + " ms for fetch+index, Memory status: " + Util.getMemoryStats());

		List<EmailDocument> allEmailDocs = (List) archive.getAllDocs(); // note: this is all archive docs, not just the ones that may have been just imported

		archive.addFetchedFolderInfos(fetchedFolderInfos);

		if (allEmailDocs.size() == 0)
			log.warn ("0 messages from email fetcher");

		//EmailUtils.cleanDates(allEmailDocs);

		// create a new address book	
		//if (session != null)
			//session.setAttribute("statusProvider", new StaticStatusProvider("Building address book..."));
		setStatusProvider.accept(new StaticStatusProvider("Building address book..."));
		AddressBook addressBook = EmailDocument.buildAddressBook(allEmailDocs, archive.ownerEmailAddrs, archive.ownerNames);
		log.info ("Address book created!!");

		log.info ("Address book stats: " + addressBook.getStats());
		//if (session != null)
			//session.setAttribute("statusProvider", new StaticStatusProvider("Finishing up..."));
		setStatusProvider.accept(new StaticStatusProvider("Finishing up..."));
		archive.setAddressBook(addressBook);

		// we shouldn't really have dups now because the archive ensures that only unique docs are added
		// move sorting to archive.postprocess? 
	    EmailUtils.removeDupsAndSort(allEmailDocs);

	    // report stats
		stats.lastUpdate = new Date().getTime();
		//For issue #254.
		stats.archiveOwnerInput = name;
		stats.archiveTitleInput = archiveTitle;
		stats.primaryEmailInput = alternateEmailAddrs;
		stats.emailSourcesInput = emailSources;
		//////
		stats.userKey = "USER KEY UNUSED"; // (String) JSPHelper.getSessionAttribute(session, "userKey");
		stats.fetchAndIndexTimeMillis = elapsedMillis;

	    updateStats(archive, addressBook, stats);
		//if (session != null)
		//	session.removeAttribute("statusProvider");
		log.info ("Fetch+index complete: " + Util.commatize(System.currentTimeMillis() - startTime) + " ms");


	}

	private Collection<String> getDataErrors()
	{
		Collection<String> result = new LinkedHashSet<>();
		if (fetchers == null)
			return result;

		for (MTEmailFetcher fetcher: fetchers) {
            if(fetcher!=null && fetcher.getDataErrors()!=null)
                result.addAll(fetcher.getDataErrors());
        }
		return result;
	}

	/** this should probably move to archive.java */
	private void updateStats(Archive archive, AddressBook addressBook, FetchStats stats)
	{
		Collection<EmailDocument> allEmailDocs = (Collection) archive.getAllDocs();
		// the rest of this is basically stats collection
		int nSent  = 0, nReceived = 0;
		for (EmailDocument ed: allEmailDocs)
		{
			Pair<Boolean, Boolean> p = addressBook.isSentOrReceived(ed.getToCCBCC(), ed.from);
			boolean sent = p.getFirst();
			boolean received = p.getSecond();
			if (sent)
				nSent++;
			if (received)
				nReceived++;
		}
		stats.dataErrors = getDataErrors();
		stats.nMessagesInArchive = allEmailDocs.size();
		/* compute stats for time range */
		if (allEmailDocs.size() > 0)
		{
			Pair<Date, Date> p = EmailUtils.getFirstLast(allEmailDocs);
			stats.firstMessageDate = p.getFirst().getTime();
			stats.lastMessageDate = p.getSecond().getTime();
		}
		//add stat for the duplicate messages that is stored in dupMessageInfo field of archive and is filled by MuseEmailFetcher while fetching messages..
		//the errors of duplicates need to be properly formatted using the map dupmessageinfo
		long sizeSavedFromDupMessages=0;
		long sizeSavedFromDupAttachments=0;
		Collection<String> dupMessages = new LinkedHashSet<>();
		for(Document doc: archive.getDupMessageInfo().keySet()){
			EmailDocument edoc = (EmailDocument)doc;
			StringBuilder sb = new StringBuilder();
			long sizesaved=0;
			long totalsize=0;

			int numofduplicates =  archive.getDupMessageInfo().get(doc).size();//number of duplicates found for this emaildocument
			//get the size of attachments
			sb.append("Duplicate message:"+" Following messages were found as duplicates of message id #"+edoc.getUniqueId()+"("+edoc.folderName+"):\n");
			for(Blob b : edoc.attachments){
				totalsize+=b.size;
			}
			sizesaved = (numofduplicates) * totalsize;
			int count =1;
			for (Tuple2 s :  archive.getDupMessageInfo().get(doc)) {
				sb.append("   "+count+"."+"Message in "+s.getFirst()+"-"+s.getSecond()+"\n");
				count++;
			}
			if (sizesaved != 0) {
				sb.append("***** Saved "+sizesaved+" bytes by detecting these duplicates\n");
				sizeSavedFromDupMessages+=sizesaved;
			}

			dupMessages.add(sb.toString());
		}
		stats.dataErrors.addAll(dupMessages);

		//also add stat for blobstore
		Collection<String> dupBlobMessages = new LinkedHashSet<>();
		Map<Blob, Integer> dupblobs = archive.getBlobStore().getDupBlobCount();
		if(dupblobs.size()>0) {

			for (Blob b : dupblobs.keySet()) {
				dupBlobMessages.add("Duplicate attachments:"+ dupblobs.get(b)+" duplicate attachments found of "+archive.getBlobStore().full_filename_normalized(b)+". Total space saved by not storing these duplicates is "+dupblobs.get(b)*b.size+" bytes\n");
				sizeSavedFromDupAttachments+=dupblobs.get(b)*b.size;
			}
		}
		stats.dataErrors.addAll(dupBlobMessages);
		stats.spaceSavingFromDupMessageDetection=sizeSavedFromDupMessages/1000;
		stats.spaceSavingFromDupAttachmentDetection=sizeSavedFromDupAttachments/1000;
		//stats.dataErrors.add("Space saving from duplicate detection:" +sizeSavedFromDupMessages/1000 + "KB saved by detecting duplicate messages\n");
		//stats.dataErrors.add("Space saving from duplicate detection:" +sizeSavedFromDupAttachments/1000 + "KB saved by detecting duplicate attachments\n");

		archive.addStats(stats);
		log.info("Fetcher stats: " + stats);
	}

	public String toString()
	{
		int nFetchers = Util.nullOrEmpty(fetchers) ? 0 : fetchers.size();
		return "Muse email fetcher with " + nFetchers + " fetcher(s)";
	}

	/*public static void main(String args[]){
		MuseEmailFetcher mf = new MuseEmailFetcher();
		try {
			mf.addEmailStore(new MboxEmailStore("","Koch-test","/Volumes/LaCie/ePADD Data/koch email"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		mf.setupFetchers(1);
	}*/
}
