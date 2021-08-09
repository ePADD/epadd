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


import edu.stanford.muse.Config;
import edu.stanford.muse.util.DictUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import javax.mail.*;
import java.io.*;
import java.util.*;

/** email store in mbox format. caches message counts in folders.
 */
@Data
public class MboxEmailStore extends EmailStore implements Serializable {
	private final static long serialVersionUID = 1L;

	private static final Logger log =  LogManager.getLogger(MboxEmailStore.class);

	private static final String CACHE_FILENAME = ".muse.dir";

	private static final Properties mstorProps;
	private FolderCache folderCache;
	private String rootPath;
	private String accountKey;
	private File cacheDir;

	static {
        //http://sourceforge.net/projects/mstor/files/mstor/0.9.13/
		mstorProps = new Properties();
		mstorProps.put("mstor.mbox.metadataStrategy", "NONE");
		//the following two properties are actually different.
		//while cache buffers option enables/disables the caching of the message content to optimise the message content retieval
		//cache.disabled option controls what kind of CacheAdapter is used by Mstor; CacheAdapter (dummy) or EhCacheAdapter
		mstorProps.put("mstor.mbox.cacheBuffers", "disabled");
		mstorProps.put("mstor.cache.disabled", "false");
		// http://code.google.com/p/coucou/source/browse/src/main/resources/mstor.properties?spec=svn9e5ed7be0c8e39027c72a220f745d87b944be826&r=9e5ed7be0c8e39027c72a220f745d87b944be826
		//relaxed parsing uses a relaxed pattern that is adapted to FoxMail export
		//mstorProps.put("mstor.mbox.parsing.relaxed", "true");
		mstorProps.put("mstor.mbox.encoding", "UTF-8"); // note: we are assuming utf-8 because that's what gmail+thunderbird will save as
		// consider adding mstor.cache.disabled true, then won't need ehcache etc
		// see: https://sourceforge.net/tracker/?func=detail&aid=2791167&group_id=114229&atid=667640
	}

	// constructor for de-serialization
	private MboxEmailStore() { }

	public MboxEmailStore(String accountKey, String name, String path) throws IOException
	{
		super(name, "" /* no automatic email address for mbox files */);
		this.accountKey = accountKey;
		this.rootPath = new File(path).getCanonicalPath(); // get canonical because the incoming path may be convoluted, like <tbird profiledir>/../../... etc
	}

	public String getRootPath() { return rootPath; }

	// Placebo method? Stub?
	public Store connect() throws MessagingException
	{
		// Get a Session object
		// can customize javamail properties here if needed, see e.g. http://java.sun.com/products/javamail/javadocs/com/sun/mail/imap/package-summary.html
		mstorProps.setProperty("mail.mime.address.strict", "false");
		Session session = Session.getInstance(mstorProps, null);
		session.setDebug(DEBUG);

		// Get a dummy Store object
		// Although "store" is irrelevant with mbox, connect/close may still be attempted on it.
		// Thus, use /dev/null rather than leaving it at / or unspecified path (which may trigger file io error).
		Store store = session.getStore(new URLName("mstor:" + Util.devNullPath()));

		// Connect to the dummy store?
		store.connect();
		return store;
	}

	private void collect_mbox_folders(List<FolderInfo> list, File f, Set<String> seenfolders) {

		if (!f.exists())
			return;

		try {
			if(seenfolders.contains(f.getCanonicalPath()))
                return;
			else
				seenfolders.add(f.getCanonicalPath());
		} catch (IOException e) {
			log.warn("Serious:!! Unable to get the canonical path of "+f);
			return;
		}

		long fileSize = f.length();

		boolean noChildren = !f.isDirectory();
		if (noChildren)
		{
			FolderInfo cachedFolderInfo = folderCache.lookup(f.getPath());
			if (cachedFolderInfo != null)
			{
				if (cachedFolderInfo.messageCount > 0)
					list.add(cachedFolderInfo);
				log.info (Util.blurPath(f.getPath()) + " is in the folder cache, message count: " + cachedFolderInfo.messageCount);
			}
			else
			{
				try {
					// read the file
					String path = f.getPath();
					if (path.endsWith(".msf"))
						return; // explicitly ignore msf files, sometimes it seems to read an actual number of messages even from msf files

					folderBeingScanned = f.getPath();
//					folderBeingScannedShortName = Util.stripFrom(f.getPath(), rootPath + File.separatorChar);
					int idx = folderBeingScanned.lastIndexOf(File.separatorChar);
					if (idx >= 0 && (idx+1) < folderBeingScanned.length())
						folderBeingScannedShortName = folderBeingScanned.substring(idx+1);
					else
						folderBeingScannedShortName = folderBeingScanned;

					Pair<Folder, Integer> pair = openFolder(null, f.getPath());
					int count = pair.getSecond();
					if (count == 1) { // many files are wrongly considered mbox with count 1. Ignore them if they also have a suffix that is known to cause noise. we're being cautious and ignoring these files only if they are noisy
						for(String disallowedFileName : DictUtils.excludedFilesFromImport){
							if (path.endsWith (disallowedFileName))
								return;

						}

						// ignore files from the list of recognized attachment types
						String extension = Util.getExtension(path);
						if (extension != null && Config.allAttachmentExtensions.contains(extension.toLowerCase()))
							return;

						log.info ("Ignoring file " + path + " because it has only 1 message and its name matches a suffix that indicates it's likely not an mbox file.");
					}
					Folder f1 = pair.getFirst();
					boolean validFolder = count > 0 && f1 != null;
					// we'll cache the folder info even if its not a valid folder.
					// this ensures we don't have to scan invalid folders again
				//	if (validFolder)
					{
						if (f1 != null)
							f1.close(false);
						// put the info in the cache
						FolderInfo fi = new FolderInfo(null, folderBeingScanned, folderBeingScannedShortName, count, fileSize);
						folderCache.put(f.getPath(), fi);
						if (validFolder)
							list.add (fi);
						folderBeingScanned = null;
					}
				} catch (Exception e)
				{
					log.error ("Exception trying to read file " + f + ": " + e);
				}
				//			store.close();
			}
		}
		else
		{
			File filesInDir[] = f.listFiles();
			if (filesInDir != null) // somehow this can be null when run on /tmp (maybe due to soft links etc).
				for (File child : filesInDir)
					collect_mbox_folders(list, child,seenfolders);
		}
	}

	protected Folder openFolderWithoutCount(Store s, String fname) throws MessagingException
	{
		if (fname == null)
			fname = "INBOX";

		// ignore the store coming in, we need a new session and store
		// for each folder

		Session session = Session.getInstance(mstorProps, null);
		session.setDebug(DEBUG);

		// Get a Store object
		Store store = session.getStore(new URLName("mstor:" + fname));
		store.connect();

        //This is the root folder in the namespace provided
        //see http://docs.oracle.com/javaee/5/api/javax/mail/Store.html#getDefaultFolder%28%29
		Folder folder = store.getDefaultFolder();
		if (folder == null)
			throw new RuntimeException ("Invalid folder: " + fname);

		log.info ("Opening folder " + Util.blurPath(fname) + " in r/o mode...");
		try {
			folder.open(Folder.READ_ONLY);
		}
		catch (MessagingException me) {
			folder = null;
		}

		return folder;
	}

	@Override
	/** we need the Store s parameter from superclass, but it is not used in the folder store
	 */ protected Pair<Folder, Integer> openFolder(Store s, String fname) throws MessagingException
	{
		Folder folder = openFolderWithoutCount(s, fname);
		int count = 0;
		if (folder != null)
			count = folder.getMessageCount(); // warning, do not close, we need to return an open folder

		return new Pair<>(folder, count);
	}

	@Override
	public void computeFoldersAndCounts(String foldersAndCountsDir) {
		doneReadingFolderCounts = false;
		
		cacheDir = new File(foldersAndCountsDir);
		cacheDir.mkdirs(); // ensure cacheDir exists

		// convert the root path to a filename
		String cacheFilePath = foldersAndCountsDir + File.separatorChar + CACHE_FILENAME + "." + rootPath.replaceAll("/", "--").replaceAll("\\\\", "--");
		this.folderCache = new FolderCache(cacheFilePath);
		this.folderInfos = new ArrayList<>();
		Set<String> seenfolders = new LinkedHashSet<>();
		collect_mbox_folders(folderInfos, new File(rootPath),seenfolders);
		doneReadingFolderCounts = true;
		folderBeingScanned = "";

		folderCache.save();
//		try { store.close(); } catch (Exception e) { System.err.println ("Exception in closing folder: " + e); }
	}
	
	@Override
	public String getAccountID()
	{
		return (Util.nullOrEmpty(accountKey) ? "mbox" : accountKey); // always return mbox because the "folder" part of the cache file name will reflect the full path
	}

	public static void main (String args[]) throws MessagingException {
	/*	LineNumberReader lnr = new LineNumberReader (new BufferedReader(new InputStreamReader(new FileInputStream("/Users/hangal/Local Folders/gmail-sent"))));
		int count = 0;
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			if (line.startsWith("From "))
				count++;
		}
		System.out.println (count + " messages found");*/
		MboxEmailStore me = 	new MboxEmailStore();
		me.rootPath = "/home/chinmay/Projects/archive/Bush Small/mbox1";
		me.computeFoldersAndCounts("/home/chinmay/epadd-appraisal/user");
	}

	public String toString()
	{
		return "Folder store with rootpath = \"" + rootPath + "\"";
	}

	/** little class to stash away folder info, so we don't have to parse them again */
	class FolderCache implements Serializable {
		private final static long serialVersionUID = 1L;

		//----------------------- folder cache ops -----------------------
		// Notes: we are never deleting cache entries. so they will live on for mbox files that may have been deleted.
		// doesn't really matter.
		// in case memory becomes a problem, we may consider not caching folders with 0 messages (right now we cache those too)
		Map<String, FolderInfo> cacheMap = new LinkedHashMap<>();
		private final String cacheFile;

		FolderCache(String path)
		{
			cacheFile = path;
			load();
		}

		@SuppressWarnings("unchecked")
		private void load()
		{
			File f = new File(cacheFile);
			if (!f.exists())
			{
				log.info ("folder cache does not exist: " + cacheFile);
				return;
			}

			try {
				log.info ("Reading folder cache from " + cacheFile);
				ObjectInputStream oos = new ObjectInputStream(new FileInputStream(cacheFile));
				cacheMap = (Map<String,FolderInfo>) oos.readObject();
				oos.close();
			} catch (Exception e)
			{
				log.warn ("Error trying to read cache file: " + e);
				e.printStackTrace(System.err);
			}
		}

		// returns the folderinfo if its present in the cache, and the info is current
		// null if info not available (not present or outdated)
		FolderInfo lookup(String file)
		{
			FolderInfo fi = cacheMap.get(file);
			if (fi == null)
				return null;

			// if file has been modified since we read this, return null.
			// no need to invalidate, because this entry will soon get overwritten anyway
			File f = new File(file);
			if (!f.exists() || f.lastModified() > fi.timestamp)
				return null;
			return fi;
		}

		void put(String file, FolderInfo fi)
		{
			cacheMap.put(file, fi);
		}

		void save()
		{
			log.info ("Saving folder cache to " + cacheFile);

			try {
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile));
				oos.writeObject(cacheMap);
				oos.close();
			} catch (IOException ioe)
			{
				log.warn ("Error trying to write cache file: " + ioe);
				ioe.printStackTrace(System.err);
			}
		}
	}

	public void deleteAndCleanupFiles() throws IOException {
        // Clear out the compute directory.
		Arrays.stream(cacheDir.listFiles()).forEach(f -> {
			try {
				f.delete();
			} catch (Exception e) {
				System.out.println("Unable to delete" + f);
			}
		});
		cacheDir.delete();
	}
}
