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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.stanford.epadd.util.EmailConvert;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** a store is an account, a source of email. */
public abstract class EmailStore implements Serializable {
    private static final Logger log =  LogManager.getLogger(EmailStore.class);
	private final static long serialVersionUID = 1L;

	String displayName;
	public String emailAddress;
	boolean DEBUG = false;

	List<FolderInfo> folderInfos; // folder lists and counts, could be accessed while it is still being computed
	private List<String> defaultFolderNames; // specific default folder name for analysis, e.g. [Gmail]/Sent Mail for gmail

	// the following used for reporting intermediate status
	boolean doneReadingFolderCounts; // will be set when all folders have been identified and counts read
	String folderBeingScanned; // the folder currently being scanned
	String folderBeingScannedShortName; // the folder currently being scanned

	public boolean isFolderCountReadingComplete() { return doneReadingFolderCounts; }
	public String folderBeingScanned() { return folderBeingScanned; }

	public synchronized void addDefaultFolderName(String folderName)
	{
		if (defaultFolderNames == null)
			defaultFolderNames = new ArrayList<>();
		defaultFolderNames.add(folderName);
	}

	/** obliterate any passwords that may be stored */
	public void wipePasswords() { /* default: do nothing */ }
	
	public List<String> getDefaultFolderNames()
	{
		return defaultFolderNames;
	}

	// returns a string for this store that uniquely identifies the account (e.g. user@server, or directory etc.)
	abstract public String getAccountID();
	
	public String getFolderInfosAsJson()
	{
		Gson gson = new Gson();
		JsonObject result = new JsonObject();
		// hoping for TSO :-)
		result.add("doneReadingFolderCounts", new JsonPrimitive(doneReadingFolderCounts));

		List<FolderInfo> tmp = new ArrayList<>();
		if (folderInfos != null)
			tmp.addAll(folderInfos);
		if (folderBeingScanned != null && !doneReadingFolderCounts)
		{
			String s = (folderBeingScannedShortName == null) ? folderBeingScanned : folderBeingScannedShortName;
			tmp.add (new FolderInfo(displayName, getAccountID(), folderBeingScanned, s, -1));
		}

		log.info("Folder infos currently available: " + tmp.size());
		
		// check long names, sometimes it is null in case of errors
		for (FolderInfo fi: tmp)
			if (Util.nullOrEmpty(fi.longName))
				log.warn ("Null or empty long name for folder " + fi);

		JsonElement je = gson.toJsonTree(tmp);
		result.add ("folderInfos", je);
		return result.toString();
	}

	// constructor for de-serialization
	EmailStore() { }

	EmailStore(String name, String emailAddress)
	{
		this.displayName = name;
		if (EmailConvert.EPADD_EMAILCHEMY_TMP.contains(displayName))
		{
	//		displayName = FolderInfo.getDisplayNameForNonMbox(displayName);
		}
		// display name is often used in html pages for attributes and javascript, so its probably very bad to have " or '
		displayName = displayName.replaceAll("\\\\", "/");
		displayName = displayName.replaceAll("'", "");
		
		this.emailAddress = emailAddress;
	}

	public void setCacheDir(String dir)
	{
		String cacheDir = dir;
	}

	abstract public void computeFoldersAndCounts(String cacheDir) throws MessagingException;

	public String getDisplayName() { return displayName; }

	/** returns folder and # of messages in it. -1 if folder cannot be opened. */
	protected abstract Pair<Folder, Integer> openFolder(Store store, String fname) throws MessagingException;

	protected abstract Folder openFolderWithoutCount(Store store, String fname) throws MessagingException;

	/** establishes a new connection using this email store */
	abstract public Store connect() throws MessagingException;

	/* controls printing of imap traffic */
	public void setDebug (boolean b) { DEBUG = b; }

	/** returns an OPEN folder in R/O mode */
	public Folder get_folder(Store store, String folder_name) throws MessagingException
	{
		return openFolderWithoutCount(store, folder_name);
	}

	/** returns # of messages in the given folder */
	public int getNMessages(String fname) throws MessagingException
	{
		// first check if we've already cached it
		if (this.folderInfos != null)
			for (FolderInfo fi : this.folderInfos)
				if (fi.longName.equals(fname))
					return fi.messageCount;
		
		Store store = connect();
		Pair<Folder, Integer> pair = openFolder(store, fname);
		Folder f = pair.getFirst();
		if (f == null)
			return -1;
		int count = pair.getSecond();
		if (count != -1)
			f.close(false);
		store.close();
		return count;
	}

	/** returns # of messages in each of the given folders */
	public int[] getNMessages(String[] fnames) throws MessagingException
	{
		Store store = connect();
		int x[] = new int[fnames.length];
		for (int i = 0; i < x.length; i++)
		{
			Pair<Folder, Integer> pair = openFolder(store, fnames[i]);
			Folder f = pair.getFirst();
			if (f == null)
			{
				x[i] = -1;
				continue;
			}
			int count = pair.getSecond();
			if (count != -1)
				f.close(false);
			x[i] = count;
		}
		try { store.close(); } catch (Exception e) { log.warn ("Exception in closing folder " + this + ":" + e); }
		return x;
	}
}
