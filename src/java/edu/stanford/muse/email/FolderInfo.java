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


import edu.stanford.epadd.util.EmailConvert;
import edu.stanford.muse.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.Serializable;
import java.util.Date;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;


/** little holder object for folder info in folder cache.
 * this has to lightweight because it is converted to json etc.
 * do not introduce refs to other objects in this! */
public class FolderInfo implements Serializable {
	private final static long serialVersionUID = 1L;
    private static final Logger log =  LogManager.getLogger(FolderInfo.class);

    public String displayName;
    public final String accountKey;
    public long lastSeenUID; // the # to allocate upwards from for new messages from this folder. derived for IMAP UID (see Javamail UIDFolder) or otherwise for other types.
    
	public final String longName;
	public String shortName;
	public final int messageCount;
	public final long timestamp; // millis since epoch when this folder was read. used only by mbox folder.
	private long fileSize = -1L; // size in bytes (for mbox files only). If not available, (imap accounts, etc) it will be -1.

	public FolderInfo(String accountKey, String fullName, String shortName, int count) {
		this("", accountKey, fullName, shortName, count, -1L);
	}

	public FolderInfo(String displayName, String accountKey, String fullName, String shortName, int count) {
		this(displayName, accountKey, fullName, shortName, count, -1L);
	}

	// accountKey set to null for mbox folder
	public FolderInfo(String displayName, String accountKey, String fullName, String shortName, int count, long fileSize)
	{
		this.shortName = shortName;
		if (fullName != null && fullName.contains(EmailConvert.EPADD_EMAILCHEMY_TMP)) {
			this.displayName = getDisplayNameForNonMbox(displayName, fullName);
			if (this.shortName != null && this.shortName.length() > 4) {
				this.shortName = shortName.substring(0, shortName.length() - 5);
			}
		} else {
			//If we read Mbox files then we just want to show fullName to the user as that is the full path of the Mbox file
			//provided by the user. (not sure about IMAP, we have to test for IMAP at some point)
			this.displayName = fullName;
		}
		this.accountKey = accountKey;
		this.longName = fullName;
		if (this.longName == null)
			log.warn ("Warning: folder's long name is null! " + Util.stackTrace());
		this.messageCount = count;
		if (this.shortName == null)
			log.warn ("Warning: folder's long name is null! " + Util.stackTrace());
		lastSeenUID = -1L;
		timestamp = new Date().getTime();
		this.fileSize = fileSize;
	}

	public static String getNonMboxFileNameFromTmpPath(String tmpPathNonMboxFile)
	{
		String mBoxPathWithNonMboxFileName = removeTmpPartOfPath(tmpPathNonMboxFile);
		return getFirstPartOfPAth(mBoxPathWithNonMboxFileName);
	}

	public static boolean hasTrailingSlash(String s) {
		//Looking for the index of File.separator doesn't work. We make any / or \ being /:
		s = s.replace("\\", "/");

		int lastIndexOfSlash = s.lastIndexOf("/");
		return lastIndexOfSlash == s.length() - 1;
	}

	static String getFirstPartOfPAth(String s)
	{
		//Looking for the index of File.separator doesn't work. We make any / or \ being /:
		s = s.replace("\\", "/");
		if (s.indexOf("/") != -1) {
			return s.substring(0, s.indexOf("/"));
		}
		else {
			return s;
		}
	}

	public static String getDisplayNameForNonMbox(String pathOriginalNonMboxFile, String pathOfTmpMboxFile) {
		//The folder is generated by Emailchemy from a non Mbox format. The Mbox files generated by Emailchemy are stored in
		//EmailConvert.getTmpDir() (something like C:\apache\apache-tomcat-8.5.71\temp\epadd_emailchemy_tmp7375781447287306347).
		//We don't want to show that path when the user hovers over a folder. We want to show:
		//{name of non Mbox file}/{folder in email account}/{maybe another folder}/{folder name}.mbox
		//For example:
		//Appraisal copy intern17.pst\Outlook Data File\Top of Outlook data file\Sent Items
		//In this way we show were the emails came from (Appraisal copy intern17.pst) and the path within
		//the email account.

		//Full name is the full path of the read mbox file, so in case of a non mbox source something like
		//C:\apache\apache-tomcat-8.5.71\temp\epadd_emailchemy_tmp18114452218571417170\mike_smith.pst\Outlook Data File\Top of Outlook data file\Clutter.mbox
		//We keep the bit after C:\apache\apache-tomcat-8.5.71\temp\epadd_emailchemy_tmp18114452218571417170 (so we keep
		//the folder structure of the email in the original acount).
		String mBoxPathWithNonMboxFileName = removeTmpPartOfPath(pathOfTmpMboxFile);

		String displayName = mBoxPathWithNonMboxFileName;
		//The mbox file was generated by Emailchemy, so we don't want to show the .mbox bit to the user (as the Mbox files
		//are only intermittent when going from e.g. pst to emails stored as Lucene objects)
		//(The condition should always be true as the files from Emailschemy have the .mbox ending. The length should
		//always be > 4. Test just to avoid Exceptions due to something unexpected happening).

		if (displayName.length() > 4 && ".mbox".equals(displayName.substring(displayName.length() - 5)))
		{
			displayName = displayName.substring(0, displayName.length() - 5);
		}
		else
		{
			log.error("Something wrong with pathOriginalNonMboxFile in getDisplayNameForNonMbox(). pathOriginalNonMboxFile = " + pathOriginalNonMboxFile);
		}
		return displayName;
	}

	public static String removeTmpPartOfPath(String pathOfTmpMboxFile) {
		String s;
		if (pathOfTmpMboxFile == null)
		{
			System.out.println("pathOfTmpMboxFile is null in removeTmpPartOfPath");
			log.error("pathOfTmpMboxFile is null in removeTmpPartOfPath");
			return "";
		}
		if (pathOfTmpMboxFile.contains(EmailConvert.getTmpDir())) {
			s = pathOfTmpMboxFile.substring(pathOfTmpMboxFile.lastIndexOf(EmailConvert.getTmpDir()) + EmailConvert.getTmpDir().length());
			//Remove trailing /
			if (s.length() > 1) {
				s = s.substring(1);
			} else {
				log.warn("Something wrong in removeTmpPartOfPath. There should be a leading '/'. Temp string = " + s);
			}
		} else {
			// There is no tmp path to remove
			s = pathOfTmpMboxFile;
		}
		return s;
	}

    public static boolean includesTmpPath(String string) {
		return string.contains(EmailConvert.getTmpDir());
    }

    public String toString()
	{
		return accountKey + ":" + shortName + " messages:" + messageCount; // + " lastUID:" + lastSeenUID;
	}
}
