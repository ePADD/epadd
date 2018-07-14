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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.stanford.muse.datacache.BlobStore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;

import edu.stanford.muse.index.Archive;
import edu.stanford.muse.util.JSONUtils;
import edu.stanford.muse.util.Util;

/** Multi-threaded email fetcher - deals with a single account (but supports multiple folders). wrapper around multiple instances of EmailFetcher.
 * currently, we only fetch in single threaded mode because some servers (incl. gmail) don't play nice with MT fetching.
 */
public class MTEmailFetcher implements StatusProvider, Serializable {

	// disabling #$!%! ehcache from mstor is essential for reasonable performance
	static { 
		System.setProperty("mstor.cache.disabled", "true"); 
		System.setProperty("net.sf.ehcache.disabled", "true"); 
		System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
		Logger.getLogger("net.sf.ehcache").setLevel(Level.OFF); // and shut the damn thing up!
	}

	private static Log log = LogFactory.getLog(MTEmailFetcher.class);
	private final static long serialVersionUID = 1L;

	private EmailStore emailStore;
	private boolean isCancelled;
	transient private ExecutorService executorService; // don't want this to persist across sessions

	List<FolderInfo> folderInfos; // these are the selected folderInfos (of messages to be fetched) as opposed to the store's folderInfos which are all folderinfo's (of messages already fetched) in the account
	private int N_THREADS;
    private int nTotalMessagesInAllFolders;
	private long startTimeMillis; // start time of current execution
	public EmailFetcherStats stats;

	private FetchConfig fetchConfig;
//	private boolean downloadMessages = false, downloadAttachments = false;
//	private Filter filter;

	//private EmailFetcherThread readFoldersThread = null; // a thread kept around just to establish the connection and get list of folders with their message count
	private EmailFetcherThread aggregateThread = null; // a thread that aggregates results from all threads at the end. (possibly, just points to thread[0])
	private EmailFetcherThread[] threads; // actual fetcher threads
	private Archive archive;

	private MTEmailFetcher(int nThreads, EmailStore store)
	{
		this (nThreads, store, -1);
	}

	public MTEmailFetcher(int nThreads, EmailStore store, int last_N_msgs)
	{
		this.N_THREADS = nThreads;
		this.emailStore = store;
		clearFolderNames();
	}

	public void setFetchConfig (FetchConfig fc) { this.fetchConfig = fc; }

	public void reset()
	{
		clearFolderNames();
	}

	public void setArchive(Archive archive)
	{
		this.archive = archive;
	}

	public void clearFolderNames()
	{
		this.folderInfos = new ArrayList<>();
	}

	/** given a folder name, gets their message counts and sets up folders for this object */
	public int addFolderNameAndComputeMessageCount(String folderName)
	{
		// add the root path for the mbox dir, because foldername does not have it when called from MuseEmailFetcher
		//	if (emailStore instanceof MboxEmailStore)
		//		folderName = ((MboxEmailStore) emailStore).getRootPath() + File.separatorChar + folderName;

		int count = -1;
		try {
			count = emailStore.getNMessages(folderName);
			folderInfos.add (new FolderInfo(emailStore.getAccountID(), folderName, folderName, count));
			log.debug(Util.pluralize(count, "message") + "in folder " + folderName);
		} catch (Exception e) {
			log.warn("Exception reading message count in folder " + folderName);
			Util.print_exception(e, log);
		}
		return count;
	}

	public List<String> getDefaultFolderNames()
	{
		return emailStore.getDefaultFolderNames();
	}

	public EmailStore getStore() { return emailStore; }

	private EmailFetcherThread[] allocateThreads(int n)
	{
		/*
	// clamp down on # threads if # of messages is very small
	if (N_THREADS > nMessagesThisFolder)
		N_THREADS = nMessagesThisFolder;

	// sometimes nMessages is 0, if so just set N_THREADS to 1 to avoid div-by-zero
	if (N_THREADS == 0)
		N_THREADS = 1;

	if (N_THREADS < 0)
	{
		log.error ("WARNING: N_THREADS < 0");
		return null;
	}
	threads = new EmailFetcherThread[N_THREADS];
		 */
		return new EmailFetcherThread[1]; // clamped to 1
	}

	public int getTotalMessagesInAllFolders()
	{
		int n = 0;
		for (FolderInfo fi : folderInfos)
			n += fi.messageCount;
		return n;
	}

	/** returns folderinfo's for folders fetched by this fetcher */
	public synchronized List<FolderInfo> run() throws InterruptedException, JSONException
	{
		Thread.currentThread().setName("MTEmailFetcher");
		log.info ("Starting fetcher run on object: " + this);
		log.info ("Folder descriptions: " + getFolderDescriptions());
		isCancelled = false;
		aggregateThread = null;

		// we'll dispatch individual threads using this executorService (over-complicated, but true...)
		// kill any job already running on this object
		try {
			if (executorService != null)
				executorService.shutdownNow();
		} catch (Exception e) { log.error ("Exception trying to shut down executor service: " + e); }

		aggregateThread = null; // this accumulates results across folders

		// compute total # of messages across all folders
		nTotalMessagesInAllFolders = getTotalMessagesInAllFolders();
		log.info ("Total messages in " + folderInfos.size() + " folder(s): " + nTotalMessagesInAllFolders);

		startTimeMillis = System.currentTimeMillis();

		List<FolderInfo> fetchedFolderInfos = new ArrayList<>();

		// process each folders, read from a array copy because we sometimes see a concurrent modification exception here
		for (FolderInfo fi : new ArrayList<>(folderInfos))
		{
			int totalMessagesInFolder = fi.messageCount;

			int from = -1, to = -1;
			if (fetchConfig.filter == null)
			{
				// range is [from..to)
				// but applies only if we don't have a filter. if we have a filter, we don't know message nums apriori, so things like from..to and last_N don't make sense (at least right now)
				// no specific instruction about message #s, just pick all messages in the folder
				from = 1;
				to = totalMessagesInFolder+1;
			}
			//	else from and to must have been directly specified... be careful here ... the same values of from and to are applied to every folder!
			// invariant: to-from = # messages;

			log.info("MTFetcher fetching Msg# [" + from + ".." +  to + ") from " + fi + " fetch config: " + fetchConfig);
			threads = allocateThreads(totalMessagesInFolder);
			N_THREADS = threads.length;


			int msgs_for_each_thread = (to-from)/N_THREADS;
			if (totalMessagesInFolder % N_THREADS != 0)
				msgs_for_each_thread++;

			// create fetcher threads
			for (int i = 0 ; i < N_THREADS; i++)
			{
				int lower = from + i*msgs_for_each_thread;
				int upper = from + (i+1)*msgs_for_each_thread;
				// clamp upper
				if (upper > totalMessagesInFolder)
					upper = totalMessagesInFolder+1;

				threads[i] = new EmailFetcherThread(emailStore, fi, lower, upper); // numbering starts from 1
				threads[i].setThreadID(i);
				threads[i].setArchive(archive);
				threads[i].setFetchConfig(fetchConfig);
			}

			// execute the fetcher threads
			executorService = Executors.newFixedThreadPool(N_THREADS);
			for (int i = 0; i < N_THREADS; i++)
				executorService.execute (threads[i]);
			executorService.shutdown();
			executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);

			for (int i = 0; i < N_THREADS; i++)
			{
				FolderInfo ffinfo = threads[i].getFetchedFolderInfo();
				if (ffinfo != null)
					fetchedFolderInfos.add(ffinfo);
			}

			if (isCancelled)
				return fetchedFolderInfos;

			for (int i = 0; i < N_THREADS; i++)
				threads[i].finish(); // basically computes contacts - may not strictly be necessary to do this for each thread, can do it on aggregate thread also.

			if (N_THREADS > 1)
				log.info ("Merging threads...");
			for (int i = 1; i < N_THREADS; i++)
				threads[0].merge(threads[i]);

			if (aggregateThread == null)
				aggregateThread = threads[0];
			else
				aggregateThread.merge(threads[0]);

			stats = aggregateThread.stats;

			log.info ("Fetch stats for folder " + fi.longName + ": " + stats);
			log.info ("Aggregate size of archive so far: " + archive.getAllDocs().size() + " messages");
			threads = null;
		}

		return fetchedFolderInfos;
	}

	public void merge (MTEmailFetcher other)
	{
		if (this != other)
			aggregateThread.merge(other.aggregateThread);
	}

	public boolean mayHaveRunOutOfMemory()
	{
		if (aggregateThread != null)
			if (aggregateThread.mayHaveRunOutOfMemory())
				return true;
		return false;
	}

	private BlobStore getAttachmentsStore()
	{
		return aggregateThread.getArchive().getBlobStore();
	}

	public Collection<String> getDataErrors()
	{
		if (aggregateThread != null)
			return aggregateThread.dataErrors;
		return null;
	}

	private String getFolderDescriptions()
	{
		if (folderInfos == null || folderInfos.size() == 0)
			return ""; // some bug, seen once, could be caused by session timeouts etc. better to return null than crash

		String folder_string = emailStore.getDisplayName() + ":" + folderInfos.get(0).shortName;

		if (folderInfos.size() > 1)
		{
			if (folderInfos.size() == 2)
				folder_string += " and " + folderInfos.get(1).shortName;
			else
				folder_string += " and " + (folderInfos.size()-1) + " other folders";
		}
		return folder_string;
	}

	/** terminate the run method of this object */
	private void terminate()
	{
		if (executorService != null)
			executorService.shutdownNow();
	}

	/** for the currently executing set of threads, gets the
	 * # of messages processed successfully, and the # of errors
	 */
	//private Triple<Integer, Integer, Integer> getProcessedCounts()
	//{
	//	int successCount = 0, pendingCount = 0;
	//	int errorCount = 0;
	//
	//	if (threads != null)
	//		for (EmailFetcher ef : threads)
	//		{
	//			//		totalCount += ef.getNTotalMessages();
	//			if (ef != null)
	//			{
	//				successCount += ef.getNUncachedMessagesProcessed();
	//				pendingCount += ef.getNUncachedMessagesRemaining();
	//
	//				errorCount += ef.nErrors;
	//			}
	//		}
	//
	//	return new Triple<Integer, Integer, Integer>(successCount, pendingCount, errorCount);
	//}

	public void cancel()
	{
		// we manually cancel as well as call terminate because executorService.shutdownnow doesn't seem to be reliable
		if (threads != null)
			for (EmailFetcherThread ef : threads)
				ef.cancel();
		log.info ("Cancel requested for " + this + "!");
		terminate();
		isCancelled = true;
	}

	public boolean isCancelled() { return isCancelled; }

	/* returns percentage of messages processed */
	public String getStatusMessage()
	{
		// if we have only 1 thread and it has a static message, return it
		if (threads != null && threads.length == 1 && threads[0] != null && threads[0].currentStatus != null && !"".equals(threads[0].currentStatus))
			return threads[0].currentStatus;

		// if we're done with fetching etc and are doing post-processing
		// aggregateThread has a static status message
		if (aggregateThread != null && !Util.nullOrEmpty(aggregateThread.currentStatus))
			return aggregateThread.currentStatus;

		EmailFetcherStats currentStats = new EmailFetcherStats();

		// compute current state of the world
		// be very defensive here because we don't know if we've actually starting running etc
		if (aggregateThread != null)
			currentStats.merge (aggregateThread.stats); // this gets stats from the folders already done

		if (threads != null)
			for (EmailFetcherThread ef : threads)
				if (ef != null && ef.stats != null)
					currentStats.merge (ef.stats);

		String processedMessage = "Scanning " /* (currentStats.nCachedMessages + currentStats.nUncachedMessagesFetched) + " of " + */ 
				+ ((nTotalMessagesInAllFolders > 0) ? (nTotalMessagesInAllFolders + " "): "") // don't show the header "Scanning 0 messages"
				+ (fetchConfig.downloadMessages ? "messages" : "headers");
		int pctComplete = 0;
		if (nTotalMessagesInAllFolders > 0)
			pctComplete = (int) ((currentStats.nMessagesAdded + currentStats.nMessagesAlreadyPresent + currentStats.nMessagesFiltered)*100L)/nTotalMessagesInAllFolders;

		long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
		long unprocessedSeconds = Util.getUnprocessedMessage(currentStats.nMessagesAdded, nTotalMessagesInAllFolders, elapsedMillis);
		// report errors only if there were any
		if (currentStats.nErrors > 0)
			processedMessage += " -- " + currentStats.nErrors + " errors";
		return JSONUtils.getStatusJSON(processedMessage, pctComplete, elapsedMillis/1000, unprocessedSeconds);
	}

	public String toString()
	{
		return "Fetcher for store: " + emailStore + "\nFields=" + Util.fieldsToString(this);
	}
}
