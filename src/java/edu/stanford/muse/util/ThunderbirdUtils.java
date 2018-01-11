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
package edu.stanford.muse.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class ThunderbirdUtils {
    private static Log log = LogFactory.getLog(ThunderbirdUtils.class);
    
    private class ThunderbirdAccount {
    	String accountName, serverHostname, serverRealHostName, serverType, username, userRealName, userEmail, dir_rel, fcc_folder;
    	int serverPort;
    }

    private static Map<String, String> readUserPrefs(String prefsFile) throws IOException
    {
		// parse lines like 
		// user_pref("mail.server.server2.directory-rel", "[ProfD]Mail/Local Folders");
		// to create a map of user_pref's
		Map<String, String> map = new LinkedHashMap<>();
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(prefsFile), "UTF-8"));
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
			{
				lnr.close();
				break;
			}
			
			line = line.trim();
			// parse the line. maybe this is better done with regexps
			String startSig = "user_pref(";
			if (line.startsWith(startSig))
			{
				line = line.substring(startSig.length());
				int idx = line.indexOf(",");
				if (idx < 0)
					continue; // not expected format, bail out
				String result[] = Util.splitIntoTwo(line, ',');
				String key = result[0].trim(), value = result[1].trim();

				if (!value.endsWith(");"))
					continue; // not expected format, bail out	

				value = value.substring (0, value.length()-");".length()); // strip out the );
				value = value.trim();
				
				// now remove quotes if present
				if (key.startsWith("\""))
					key = key.substring(1);
				if (value.startsWith("\""))
					value = value.substring(1);
				if (key.endsWith("\""))
					key = key.substring(0, key.length()-1);
				if (value.endsWith("\""))
					value = value.substring(0, value.length()-1);
				map.put(key, value);
			}
		}
		log.info(map.size() + " Thunderbird preferences read from " + prefsFile);
		return map;
    }
    
    /** returns list of accounts; each account has a list of properties, the order is fragile! */
	private static List<List<String>> getThunderbirdAccountsNew()
	{
		// read all user prefs as a map
		try {
			String rootDir = ThunderbirdUtils.getThunderbirdProfileDir();
			String prefs = rootDir + File.separator + "prefs.js";
			File f = new File(prefs);
			if (!f.exists() || !f.canRead())
			{
				EmailUtils.log.info("Thunderbird probably not installed: no prefs.js in directory: " + prefs);
				return null;
			}

			// ok, now have map. look for accounts and their information.
			List<List<String>> result = new ArrayList<>();
			Map<String, String> userPrefsMap = readUserPrefs(prefs);

			// get the account list: original line looks like: user_pref("mail.accountmanager.accounts", "account2,account1");
			String accounts = userPrefsMap.get("mail.accountmanager.accounts");
			
			// sometimes this is null e.g. if tbird has been installed, but the accounts have been deleted
			if (Util.nullOrEmpty(accounts))
				return result;
			
			StringTokenizer st = new StringTokenizer(accounts, ",");
			while (st.hasMoreTokens())
			{
				String account = st.nextToken();
				String server = userPrefsMap.get("mail.account." + account + ".server");
				if (server == null)
					continue;

				// hidden is set to true for "unified folders" account
				if ("true".equals(userPrefsMap.get("mail.server." + server + ".hidden")))
					continue;
				
				String serverType = userPrefsMap.get("mail.server." + server + ".type");
				// ignore a/c if server type is nntp 
				if ("nntp".equals(serverType))
					continue;
				
				String accountName = userPrefsMap.get("mail.server." + server + ".name");
				String serverRealHostName = userPrefsMap.get("mail.server." + server + ".realhostname");
				if (serverRealHostName == null)
					serverRealHostName = userPrefsMap.get("mail.server." + server + ".hostname");

				String userName = userPrefsMap.get("mail.server." + server + ".userName"); // note: userName, not username
				String serverPort = userPrefsMap.get("mail.server." + server + ".port");
			
				String directoryRel = userPrefsMap.get("mail.server." + server + ".directory-rel"); // note: userName, not username
				if (directoryRel != null && directoryRel.startsWith("[ProfD]"))
					directoryRel = directoryRel.replace("[ProfD]", ThunderbirdUtils.getThunderbirdProfileDir() + File.separator);
				// we'll add it later since its later in the param sequence
							
				String ids = userPrefsMap.get("mail.account." + account + ".identities");
				if (ids == null)
				{
						// must be local folders, they don't have any id's
						List<String> accountParams = new ArrayList<>();
						accountParams.add(accountName);
						accountParams.add(serverRealHostName);
						accountParams.add(serverType);
						accountParams.add(userName);
						accountParams.add(null); // no useremail
						accountParams.add(null); // fullname
						accountParams.add(directoryRel);
						
						accountParams.add(null); // no fcc
						accountParams.add(null); // no port
						result.add(accountParams);
						
						log.info(" account: Local Folders " 
								+ " userName: " + userName + " accountName: " + accountName + " hostname: " + serverRealHostName + " serverRealHostName: " + serverRealHostName 
								+ " type: " + serverType + " port: " + serverPort + 	" directoryRel: " + directoryRel);
						continue;
				}
				
				// there may multiple id's under this account, we create multiple entries in the result
				StringTokenizer st1 = new StringTokenizer(ids, ",");
				while (st1.hasMoreTokens())
				{
					// create a result entry for each id
					List<String> accountParams = new ArrayList<>();
					accountParams.add(accountName);
					accountParams.add(serverRealHostName);
					accountParams.add(serverType);
					accountParams.add(userName);
					
					String id = st1.nextToken();
					String useremail = userPrefsMap.get("mail.identity." + id + ".useremail");
					accountParams.add(useremail);
					
					String fullname = userPrefsMap.get("mail.identity." + id + ".fullName");
					accountParams.add(fullname);
			
					accountParams.add(directoryRel);
					
					String fcc_folder_full = userPrefsMap.get("mail.identity." + id + ".fcc_folder");
					String fcc_folder = null;
					if (fcc_folder_full != null)
					{
						// fccFolder imap://hangal@xenon.stanford.edu/Sent
						fcc_folder = fcc_folder_full.replaceAll("[^/]*/+[^/]*/+(.*$)", "$1"); // skip the first 2 tokens, split by /
						if (!fcc_folder_full.equals(fcc_folder)) // only if not equal is it valid
							fcc_folder = null;
					}

					log.info(" account: " + account 
							+ " userName: " + userName + " useremail = " + useremail + " id: " + id + " accountName: " + accountName + " hostname: " + serverRealHostName + " serverRealHostName: " + serverRealHostName 
							+ " type: " + serverType + " port: " + serverPort + 	" directoryRel: " + directoryRel
							+ " fullname = " + fullname
							+ " fcc_folder_full = " + fcc_folder_full
							+ " fcc_folder = " + fcc_folder);
					accountParams.add(fcc_folder);
					
					// tack on port at the end, though we're not using it right now
					accountParams.add(serverPort);
					
					result.add(accountParams);
				}
			}
			return result;
		} catch (Exception e) {
			Util.print_exception(e, log);
		}
		return null;
	}
	
	/** returns a list of thunderbird accounts.
		each list is in turn a list of length exactly 4:
		account name, hostname, server type, user name
	*/
	public static List<List<String>> getThunderbirdAccounts()
	{
		try {
			List<List<String>> newResult = getThunderbirdAccountsNew();
			if (newResult != null)
				return newResult;
		} catch (Exception e) {
			log.warn ("unable to process thunderbird profile with new thunderbird parser");
			Util.print_exception(e, log);
		}
		
		List<List<String>> result = new ArrayList<>();

		try {
			String rootDir = ThunderbirdUtils.getThunderbirdProfileDir();
			String prefs = rootDir + File.separator + "prefs.js";
			File f = new File(prefs);
			if (!f.exists() || !f.canRead())
			{
				EmailUtils.log.info("Thunderbird probably not installed: no prefs.js in directory: " + prefs);
				return result;
			}

			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(prefs), "UTF-8"));

			// example fragment of input
			//        user_pref("mail.server.server2.capability", 21520929);
			//        user_pref("mail.server.server2.directory", "AAAAAAH0AAIAAQxNYWNpbnRvc2ggSEQAAAAAAAAAAAAAAAAAAADGqxmGSCsAAAAJi1ASeGVub24uc3RhbmZvcmQuZWR1AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAmLUcbnohEAAAAAAAAAAP////8AAAkgAAAAAAAAAAAAAAAAAAAACEltYXBNYWlsABAACAAAxqt79gAAABEACAAAxugEgQAAAAEAHAAJi1AACYsXAAmLFQAJiw8AB/mpAAf5qAAAkOcAAgBjTWFjaW50b3NoIEhEOlVzZXJzOmhhbmdhbDpMaWJyYXJ5OlRodW5kZXJiaXJkOlByb2ZpbGVzOjcyeHZzMXd3LmRlZmF1bHQ6SW1hcE1haWw6eGVub24uc3RhbmZvcmQuZWR1AAAOACYAEgB4AGUAbgBvAG4ALgBzAHQAYQBuAGYAbwByAGQALgBlAGQAdQAPABoADABNAGEAYwBpAG4AdABvAHMAaAAgAEgARAASAFZVc2Vycy9oYW5nYWwvTGlicmFyeS9UaHVuZGVyYmlyZC9Qcm9maWxlcy83Mnh2czF3dy5kZWZhdWx0L0ltYXBNYWlsL3hlbm9uLnN0YW5mb3JkLmVkdQATAAEvAAAVAAIADf//AAA=");
			//        user_pref("mail.server.server2.directory-rel", "[ProfD]ImapMail/xenon.stanford.edu");
			//        user_pref("mail.server.server2.download_on_biff", true);
			//        user_pref("mail.server.server2.hostname", "xenon.stanford.edu");
			//        user_pref("mail.server.server2.login_at_startup", true);
			//        user_pref("mail.server.server2.max_cached_connections", 5);
			//        user_pref("mail.server.server2.name", "Stanford CS");
			//        user_pref("mail.server.server2.namespace.other_users", "\"~\"");
			//        user_pref("mail.server.server2.namespace.personal", "\"#mh/\",\"#mhinbox\",\"\"");
			//        user_pref("mail.server.server2.namespace.public", "\"#public/\",\"#news.\",\"#ftp/\",\"#shared/\"");
			//        user_pref("mail.server.server2.timeout", 29);
			//        user_pref("mail.server.server2.type", "imap");
			//        user_pref("mail.server.server2.userName", "hangal");

			// be careful not to match a ...hostname line or a ...namespace line with the .name pattern
			//
			// that's why we need an explicit dot before and a quote after the type of field in the pattern

			// note: there are 2 fields: hostname and realhostname - realhostname has precedence if it exists
			// see: http://forums.mozillazine.org/viewtopic.php?f=39&t=1697195
			Pattern accountNamePat = Pattern.compile(".*\"mail.server.server.*\\.name\".*");
			Pattern hostnamePat = Pattern.compile(".*\"mail.server.server.*\\.hostname\".*");
			Pattern realHostnamePat = Pattern.compile(".*\"mail.server.server.*\\.realhostname\".*");
			Pattern serverTypePat = Pattern.compile(".*\"mail.server.server.*\\.type\".*");
			Pattern usernamePat = Pattern.compile(".*\"mail.server.server.*\\.userName\".*");
			Pattern userRealNamePat = Pattern.compile(".*\"mail.identity.id.*\\.fullName\".*");
			Pattern userEmailPat = Pattern.compile(".*\"mail.identity.id.*\\.useremail\".*");
			Pattern directoryRelPat = Pattern.compile(".*\"mail.server.server.*\\.directory-rel\".*");
			Pattern fccFolderPat = Pattern.compile(".*\"mail.identity.id.*\\.fcc_folder\".*");

			Map<String, String> accountNameMap = new LinkedHashMap<>();
			Map<String, String> hostnameMap = new LinkedHashMap<>();
			Map<String, String> realHostnameMap = new LinkedHashMap<>();
			Map<String, String> serverTypeMap = new LinkedHashMap<>();
			Map<String, String> usernameMap = new LinkedHashMap<>();
			Map<String, String> userEmailMap = new LinkedHashMap<>();
			Map<String, String> userRealNameMap = new LinkedHashMap<>();
			Map<String, String> directoryRelMap = new LinkedHashMap<>();
			Map<String, String> fccFolderMap = new LinkedHashMap<>();

			while (true)
			{
				String line = lnr.readLine();
				if (line == null)
				{
					lnr.close();
					break;
				}

				if (accountNamePat.matcher(line).matches())
				{
					Pair<String,String> pair = ThunderbirdUtils.parseLine(line, "server");
					accountNameMap.put(pair.getFirst(), pair.getSecond());
				}
				if (hostnamePat.matcher(line).matches())
				{
					Pair<String,String> pair = ThunderbirdUtils.parseLine(line, "server");
					hostnameMap.put(pair.getFirst(), pair.getSecond());
				}
				if (realHostnamePat.matcher(line).matches())
				{
					Pair<String,String> pair = ThunderbirdUtils.parseLine(line, "server");
					realHostnameMap.put(pair.getFirst(), pair.getSecond());
				}
				else if (serverTypePat.matcher(line).matches())
				{
					Pair<String,String> pair = ThunderbirdUtils.parseLine(line, "server");
					String serverType = pair.getSecond();
					if ("imap".equals(serverType))
						serverType = "imaps";
					if ("pop".equals(serverType))
						serverType = "pops";
					serverTypeMap.put(pair.getFirst(), serverType);
				}
				else if (usernamePat.matcher(line).matches())
				{
					Pair<String,String> pair = ThunderbirdUtils.parseLine(line, "server");
					usernameMap.put(pair.getFirst(), pair.getSecond());
				}
				else if (userEmailPat.matcher(line).matches())
				{
					Pair<String,String> pair = ThunderbirdUtils.parseLine(line, "id");
					userEmailMap.put(pair.getFirst(), pair.getSecond());
				}
				else if (userRealNamePat.matcher(line).matches())
				{
					Pair<String,String> pair = ThunderbirdUtils.parseLine(line, "id");
					userRealNameMap.put(pair.getFirst(), pair.getSecond());
				}
				else if (directoryRelPat.matcher(line).matches())
				{
					// for local folders the line is like user_pref("mail.server.server1.directory-rel", "[ProfD]../../../../../../tmp/tb");
					// Convert [ProfD]../../../../../../tmp/tb to the correct path by replacing [ProfD] with the profile dir
					Pair<String,String> pair = ThunderbirdUtils.parseLine(line, "server");
					String directoryRel = pair.getSecond();
					if (directoryRel != null)
					{
						if (directoryRel.startsWith("[ProfD]"))
							directoryRel = directoryRel.replace("[ProfD]", ThunderbirdUtils.getThunderbirdProfileDir() + File.separator);
						// we also have to correct the ../../ to \..\... for windows
						directoryRel = directoryRel.replaceAll("/", File.separator);
						directoryRelMap.put(pair.getFirst(), directoryRel);
					}
				}
				else if (fccFolderPat.matcher(line).matches())
				{
					// the line looks like user_pref("mail.identity.id1.fcc_folder", "imap://hangal@xenon.stanford.edu/Sent");

					Pair<String,String> pair = ThunderbirdUtils.parseLine(line, "id");
					String fccFolderFull = pair.getSecond();
					if (fccFolderFull != null)
					{
						// fccFolder imap://hangal@xenon.stanford.edu/Sent
						String fccFolder = fccFolderFull.replaceAll("[^/]*/+[^/]*/+(.*$)", "$1"); // skip the first 2 tokens, split by /
						if (!fccFolderFull.equals(fccFolder)) // only if not equal is it valid
							fccFolderMap.put(pair.getFirst(), fccFolder);
					}
				}
			}

			for (String key: serverTypeMap.keySet())
			{
				String s = serverTypeMap.get(key).toLowerCase();
				// we only know how to handle imap and pop and local folders
				// other things like smart folders, don't list.
				if (!s.startsWith("imap") && !s.startsWith("pop") && !"Local Folders".equals(accountNameMap.get(key)))
					continue;

				List<String> params = new ArrayList<>();
				params.add(accountNameMap.get(key));
				String hostnameToUse = realHostnameMap.get(key);
				if (hostnameToUse == null)
					hostnameToUse = hostnameMap.get(key);
				params.add(hostnameToUse);
				params.add(serverTypeMap.get(key));
				params.add(usernameMap.get(key));
				params.add(userEmailMap.get(key));
				params.add(userRealNameMap.get(key));
				params.add(directoryRelMap.get(key));
				params.add(fccFolderMap.get(key));

				String str = "Tbird accountname=\"" + accountNameMap.get(key) + "\" " +
					"hostname=\"" + hostnameMap.get(key) + "\" " +
					"serverType=\"" + serverTypeMap.get(key) + "\" " +
					"username=\"" + usernameMap.get(key) + "\" " +
					"userEmail=\"" + userEmailMap.get(key) + "\" " +
					"userRealName=\"" + userRealNameMap.get(key) + "\" " +
					"directoryRel=\"" + directoryRelMap.get(key) + "\"" +
					"fccFolder=\"" + fccFolderMap.get(key) + "\"";

				EmailUtils.log.debug(str);
				// System.out.println(str);
				result.add (params);
			}

			lnr.close();
		} catch (Exception e)
		{
			System.err.println ("REAL WARNING: exception trying to read thunderbird prefs" + Util.stackTrace(e));
		}

		return Collections.unmodifiableList(result);
	}

	// input is something like:
	// user_pref("mail.server.server2.hostname", "xenon.stanford.edu");
	// user_pref("mail.identity.id1.useremail", "a@b.c");
	// we return a Pair("2", "xenon.stanford.edu") (no quotes)
	// or Pair("1", "a@b.c") (no quotes)
	private static Pair<String, String> parseLine(String line, String keyword)
	{
		StringTokenizer st = new StringTokenizer(line, ".");
		st.nextToken(); st.nextToken(); // skip up to the second dot
		String id = st.nextToken(); // stuff between second and third dot is the thunderbird id
		// id = "server2" or "id2"
		id = id.substring (keyword.length());
		// id = "2"

		String value = line.substring(line.indexOf(' ')+2); // skip ' ' and "
		// value = xenon.stanford.edu")
		value = value.substring (0, value.indexOf('"'));
		// value = xenon.stanford.edu

		return new Pair<>(id, value);
	}

	/* guess from the user agent what the thunderbird root dir is going to be */
		private static String getThunderbirdProfileDir()
		{
			String tbirdDir = System.getProperty("user.home");
			boolean isMac = false;
			boolean isWindowsXP = false;
			boolean isWindowsVistaOr7 = false;
			boolean isLinux = false;
	//		if (userAgent == null)
	//			userAgent = "";
	//		userAgent = userAgent.toLowerCase();

			String os = System.getProperty("os.name").toLowerCase();
			if (os.startsWith("mac"))
				isMac = true;
			else if (os.contains("linux") || os.contains("solaris") || os.contains("hp-ux"))
				isLinux = true;
			else if (os.startsWith("windows"))
			{
				if (os.endsWith("xp"))
					isWindowsXP = true;
				else // if (os.endsWith("vista))
					isWindowsVistaOr7 = true;
			}

	//		if (userAgent.indexOf("macintosh") >= 0 || userAgent.indexOf("mac os") >= 0)
	//			isMac = true;
	//		else if (userAgent.indexOf("linux") >= 0)
	//			isLinux = true;
	//		else
	//			isWindows = true;
	//
	//		// special hack for xp
	//		if (homeDir.indexOf("Documents and Settings") >= 0)
	//		{
	//			tbirdDir += "\\Application Data\\Thunderbird\\Profiles";
	//			isWindows = true;
	//		}
	//		else
	//		{
	//			if (isMac)
	//				tbirdDir += "/Library/Thunderbird/Profiles";
	//			else if (isWindows)
	//				tbirdDir += "\\AppData\\Roaming\\Thunderbird\\Profiles"; // vista, win7
	//			else if (isLinux)
	//				tbirdDir += "/.thunderbird";
	//		}

	//		tbirdDir += "\\Application Data\\Thunderbird\\Profiles";

			if (isMac)
				tbirdDir += "/Library/Thunderbird/Profiles";
			else if (isWindowsVistaOr7)
				tbirdDir += "\\AppData\\Roaming\\Thunderbird\\Profiles"; // vista, win7
			else if (isWindowsXP)
				tbirdDir += "\\Application Data\\Thunderbird\\Profiles"; // vista, win7
			else if (isLinux)
				tbirdDir += "/.thunderbird";

			File tbirdFile = new File(tbirdDir);
			if (!tbirdFile.exists() || !tbirdFile.canRead())
				return "";

			// look for a file *.default - doing wildcard manually
			File[] files = tbirdFile.listFiles();
			String defaultFolderName = null;
			for (File f: files)
				if (f.isDirectory())
					if (f.getName().endsWith(".default"))
					{
						defaultFolderName = f.getName();
						break;
					}

			if (defaultFolderName != null)
			{
				tbirdDir += File.separator + defaultFolderName;
	//			tbirdDir += File.separator + "Mail" + File.separator + "Local Folders";
			}

			log.debug ("thunderbird dir is " + tbirdDir);
			return tbirdDir;
		}

		/** all the user's own email addrs as one ,-separated string */
		public static String getOwnEmailsAsString(List<List<String>> accounts)
		{
			String userEmails = "";
			for (List<String> account: accounts)
			{
				String userEmail = account.get(4);
				if (userEmail != null)
					userEmails += userEmail + ", ";
			}
			// strip the ,<blank> at the end
			if (userEmails.endsWith(", "))
				userEmails = userEmails.substring (0, userEmails.length()-", ".length());
			return userEmails;
		}

		/** all the user's own email addrs as one ,-separated string */
		public static String getOwnEmailsAndNamesAsString(List<List<String>> accounts)
		{
			String result = "";
			for (List<String> account: accounts)
			{
				String userEmail = account.get(4);
				if (!Util.nullOrEmpty(userEmail))
					result += userEmail + ", ";
				String userName = account.get(5);
				if (userName != null)
					result += userName + ", ";
			}
			// strip the ,<blank> at the end
			if (result.endsWith(", "))
				result = result.substring (0, result.length()-", ".length());
			return result;
		}

		public static List<String> readThunderbirdAccountsRealNames()
		{
			List<List<String>> tbirdAccounts = getThunderbirdAccounts();
			List<String> result = new ArrayList<>();
			for (List<String> x: tbirdAccounts)
			{
				String name = x.get(5);
				if (!Util.nullOrEmpty(name))
					result.add(name); // userRealName is the 6th in the list
			}
			return result;
		}

}
