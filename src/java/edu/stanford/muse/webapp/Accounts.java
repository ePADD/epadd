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
package edu.stanford.muse.webapp;


import edu.stanford.muse.email.MuseEmailFetcher;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashSet;

/** class that extracts params from http post by the login page and sets up email stores in the MuseEmailFetcher in the session */
public class Accounts {

	public static Log log = LogFactory.getLog(Accounts.class);
	
	/** does account setup and login (and look up default folder if well-known account) from the given request.
	 * request params are loginName<N>, password<N>, etc (see loginForm for details).
	 * returns an object with {status: 0} if success, or {status: <non-zero>, errorMessage: '... '} if failure.
	 * if success and account has a well-known sent mail folder, the returned object also has something like: 
	 * {defaultFolder: '[Gmail]/Sent Mail', defaultFolderCount: 1033}
	 * accounts on the login page are numbered 0 upwards. */
	public static JSONObject login(HttpServletRequest request, int accountNum) throws IOException, JSONException
	{
		JSONObject result = new JSONObject();
		
		HttpSession session = request.getSession();
		// allocate the fetcher if it doesn't already exist
		MuseEmailFetcher m = null;
		synchronized (session) // synchronize, otherwise may lose the fetcher when multiple accounts are specified and are logged in to simult.
		{
			m = (MuseEmailFetcher) JSPHelper.getSessionAttribute(session, "museEmailFetcher");
			boolean doIncremental = request.getParameter("incremental") != null;
					
			if (m == null || !doIncremental)
			{
				m = new MuseEmailFetcher();
				session.setAttribute("museEmailFetcher", m);
			}

			// store metadata in this object, it will be xferred to the archive later
			m.name = request.getParameter ("name");
			m.alternateEmailAddrs = request.getParameter ("alternateEmailAddrs");
			m.archiveTitle = request.getParameter ("archiveTitle");
		}

		// note: the same params get posted with every accountNum
		// we'll update for all account nums, because sometimes account #0 may not be used, only #1 onwards. This should be harmless.
		// we used to do only altemailaddrs, but now also include the name.
		//updateUserInfo(request);

		String accountType = request.getParameter("accountType" + accountNum);
		if (Util.nullOrEmpty(accountType))
		{
			result.put("status", 1);
			result.put("errorMessage",  "No information for account #" + accountNum);
			return result;
		}
		
		String loginName = request.getParameter("loginName" + accountNum); 
		String password = request.getParameter("password" + accountNum); 
		String protocol = request.getParameter("protocol" + accountNum); 
	//	String port = request.getParameter("protocol" + accountNum);  // we don't support pop/imap on custom ports currently. can support server.com:port syntax some day
		String server = request.getParameter("server" + accountNum); 
		String defaultFolder = request.getParameter("defaultFolder" + accountNum);

		if (server == null)
			server = "";
		if (loginName == null)
			loginName = "";

		server = server.trim();
		loginName = loginName.trim();

		// for these ESPs, the user may have typed in the whole address or just his/her login name
		if (accountType.equals("gmail") && !loginName.contains("@"))
			loginName = loginName + "@gmail.com";
		if (accountType.equals("yahoo") && !loginName.contains("@"))
			loginName = loginName + "@yahoo.com";
		if (accountType.equals("live") && !loginName.contains("@"))
			loginName = loginName + "@live.com";
		if (accountType.equals("stanford") && !loginName.contains("@"))
			loginName = loginName + "@stanford.edu";
		if (accountType.equals("gmail"))
			server = "imap.gmail.com";

		// add imapdb stuff here.
		boolean imapDBLookupFailed = false;
		String errorMessage = "";
		int errorStatus = 0;

		if(accountType.equals("email") && Util.nullOrEmpty(server)) {
			log.info("accountType = email");
			
			defaultFolder = "Sent";
			
			{
				// ISPDB from Mozilla
				imapDBLookupFailed = true;

				String emailDomain = loginName.substring(loginName.indexOf("@") + 1);
				log.info("Domain: " + emailDomain);

				// from http://suhothayan.blogspot.in/2012/05/how-to-install-java-cryptography.html
				// to get around the need for installingthe unlimited strength encryption policy files.
				try {
					Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
					field.setAccessible(true);
					field.set(null, java.lang.Boolean.FALSE);
				} catch (Exception ex) {
					ex.printStackTrace();
				}

//				URL url = new URL("https://live.mozillamessaging.com/autoconfig/v1.1/" + emailDomain);
				URL url = new URL("https://autoconfig.thunderbird.net/v1.1/" + emailDomain);
				try {
					URLConnection urlConnection = url.openConnection();
					InputStream in = new BufferedInputStream(urlConnection.getInputStream());
					
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse( in );
					
					NodeList configList = doc.getElementsByTagName("incomingServer");
					log.info("configList.getLength(): " + configList.getLength());
					
					int i;
					for(i = 0; i < configList.getLength(); i++) {
						Node config = configList.item(i);
						NamedNodeMap attributes = config.getAttributes();
						if(attributes.getNamedItem("type").getNodeValue().equals("imap")) {
							log.info("[" + i + "] type: " + attributes.getNamedItem("type").getNodeValue());
							Node param = config.getFirstChild();
							String nodeName, nodeValue;
							String paramHostName = "";
							String paramUserName = "";
							do {
								if(param.getNodeType() == Node.ELEMENT_NODE) {
									nodeName = param.getNodeName();
									nodeValue = param.getTextContent();
									log.info(nodeName + "=" + nodeValue);
									if(nodeName.equals("hostname")) {
										paramHostName = nodeValue;
									} else if(nodeName.equals("username")) {
										paramUserName = nodeValue;
									}
								}
								param = param.getNextSibling();
							} while(param != null);
							
							log.info("paramHostName = " + paramHostName);
							log.info("paramUserName = " + paramUserName);
							
							server = paramHostName;
							imapDBLookupFailed = false;
							switch (paramUserName) {
								case "%EMAILADDRESS%":
									// Nothing to do with loginName
									break;
								case "%EMAILLOCALPART%":
								case "%USERNAME%":
									// Cut only local part
									loginName = loginName.substring(0, loginName.indexOf('@') - 1);
									break;
								default:
									imapDBLookupFailed = true;
									errorMessage = "Invalid auto configuration";
									break;
							}
							
							break; // break after find first IMAP host name
						}
					}
				} catch (Exception e) {
					Util.print_exception ("Exception trying to read ISPDB", e, log);
					errorStatus = 2; // status code = 2 => ispdb lookup failed
					errorMessage = "No automatic configuration available for " + emailDomain + ", please use the option to provide a private (IMAP) server. \nDetails: " + e.getMessage() + ". \nRunning with java -Djavax.net.debug=all may provide more details.";
				}
			}
		}		
		
		if (imapDBLookupFailed)
		{
			log.info("ISPDB Fail");
			result.put("status", errorStatus); 
			result.put("errorMessage", errorMessage);
			// add other fields here if needed such as server name attempted to be looked up in case the front end wants to give a message to the user
			return result;
		}
		
		boolean isServerAccount = accountType.equals("gmail") || accountType.equals("email") || accountType.equals("yahoo") || accountType.equals("live") || 
				accountType.equals("stanford") || accountType.equals("gapps") || accountType.equals("imap") || accountType.equals("pop") || accountType.startsWith("Thunderbird");
		
		if (isServerAccount)
		{
			boolean sentOnly = "on".equals(request.getParameter("sent-messages-only"));
			return m.addServerAccount(server, protocol, defaultFolder, loginName, password, sentOnly);
		}
		else if (accountType.equals("mbox") || accountType.equals("tbirdLocalFolders"))
		{
			String mboxDir = request.getParameter("mboxDir" + accountNum);
			String emailSource = request.getParameter("emailSource" + accountNum);
			// for non-std local folders dir, tbird prefs.js has a line like: user_pref("mail.server.server1.directory-rel", "[ProfD]../../../../../../tmp/tb");
			log.info("adding mbox account: " + mboxDir);
			errorMessage = m.addMboxAccount(emailSource, mboxDir, accountType.equals("tbirdLocalFolders"));
			//Store this info in m for persistence in the data report issue #254
			if(m.emailSources==null)
				m.emailSources = new LinkedHashSet<>();
			m.emailSources.add(emailSource);
			if (!Util.nullOrEmpty(errorMessage))
			{
				result.put("errorMessage", errorMessage);
				result.put("status", 1);
			}
			else
				result.put("status", 0);
		}
		else
		{
			result.put("errorMessage", "Sorry, unknown account type: " + accountType);
			result.put("status", 1);
		}
		return result;
	}

}
