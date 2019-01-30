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


import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Date;


/** little holder object for folder info in folder cache.
 * this has to lightweight because it is converted to json etc.
 * do not introduce refs to other objects in this! */
public class FolderInfo implements Serializable {
	private final static long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(FolderInfo.class);

    public final String accountKey;
    public long lastSeenUID; // the # to allocate upwards from for new messages from this folder. derived for IMAP UID (see Javamail UIDFolder) or otherwise for other types.
    
	public final String longName;
	public final String shortName;
	public final int messageCount;
	public final long timestamp; // millis since epoch when this folder was read. used only by mbox folder.
	private long fileSize = -1L; // size in bytes (for mbox files only). If not available, (imap accounts, etc) it will be -1.

	public FolderInfo(String accountKey, String fullName, String shortName, int count) {
		this(accountKey, fullName, shortName, count, -1L);
	}

	// accountKey set to null for mbox folder
	public FolderInfo(String accountKey, String fullName, String shortName, int count, long fileSize)
	{
		this.accountKey = accountKey;
		this.longName = fullName;
		if (this.longName == null)
			log.warn ("Warning: folder's long name is null! " + Util.stackTrace());
		this.messageCount = count;
		this.shortName = shortName;
		if (this.shortName == null)
			log.warn ("Warning: folder's long name is null! " + Util.stackTrace());
		lastSeenUID = -1L;
		timestamp = new Date().getTime();
		this.fileSize = fileSize;
	}

	public String toString()
	{
		return accountKey + ":" + shortName + " messages:" + messageCount; // + " lastUID:" + lastSeenUID;
	}
}
