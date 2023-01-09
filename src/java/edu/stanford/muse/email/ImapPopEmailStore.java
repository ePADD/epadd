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

import com.sun.mail.util.MailSSLSocketFactory;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ImapPopEmailStore extends EmailStore {
	private final static long serialVersionUID = 1L;
    private static final Logger log =  LogManager.getLogger(ImapPopEmailStore.class);

	private ImapPopConnectionOptions connectOptions;
	private static final Properties mstoreProps;
	private transient Store store = null;
	transient public Session session = null;
	
	static {
		 // see http://www.javaworld.com/javatips/jw-javatip115.html
	//	 Security.addProvider( new com.sun.net.ssl.internal.ssl.Provider());

		 mstoreProps = System.getProperties();

		// see RFC 3501 http://www.faqs.org/rfcs/rfc3501.html

		// IMAP supports 2 kinds of login, plain and authenticated.
		// protocol=imap leaks protocol traffic
		// protocol=imap with plain mode disabled at least protects password by using SASL for password only.
		// protocol=imaps has no issues, so plain mode can be left enabled.
		// some buggy servers have trouble with SASL plain auth, disable it
		mstoreProps.put("mail.imap.auth.plain.disable", "true");
		
		// see http://java.sun.com/products/javamail/javadocs/com/sun/mail/imap/package-summary.html for imap properties

		mstoreProps.put("mail.imaps.partialfetch", "false"); // sometimes imap servers have a bug: see http://java.sun.com/products/javamail/FAQ.html#imapserverbug
		mstoreProps.put("mail.imap.partialfetch", "false"); // sometimes imap servers have a bug: see http://java.sun.com/products/javamail/FAQ.html#imapserverbug

		mstoreProps.put("mail.imaps.fetchsize", Integer.toString(1024*1024*8)); // sometimes imap servers have a bug: see http://java.sun.com/products/javamail/FAQ.html#imapserverbug
		mstoreProps.put("mail.imap.fetchsize", Integer.toString(1024*1024*8)); // sometimes imap servers have a bug: see http://java.sun.com/products/javamail/FAQ.html#imapserverbug

		MailSSLSocketFactory socketFactory = null;
		try {
			// security: try and accept bad certs!
			// csl-mail works fine with imap, but not with imaps
			// mstoreProps.put("mail.imaps.socketFactory.class", "edu.stanford.muse.email.DummySSLSocketFactory");
			mstoreProps.put("mail.imaps.ssl.trust", "*");

			socketFactory = new MailSSLSocketFactory();
			socketFactory.setTrustAllHosts(true);
			mstoreProps.put("mail.imaps.ssl.socketFactory", socketFactory);
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	//	mstoreProps.put("mail.debug", "true");
	}

    public static final class OAuth2Provider extends Provider {
	    private static final long serialVersionUID = 1L;

	    OAuth2Provider() {
	      super("Google OAuth2 Provider", 1.0,
	            "Provides the XOAUTH2 SASL Mechanism");
	      put("SaslClientFactory.XOAUTH2",
	          "com.google.code.samples.oauth2.OAuth2SaslClientFactory");
	    }
    }
	
	public ImapPopEmailStore(ImapPopConnectionOptions options, String emailAddress)
	{
		super(options.userName + "@" + options.server, emailAddress);
		this.connectOptions = options;
		this.displayName = computeSimpleDisplayName();
		Security.addProvider(new OAuth2Provider());
	}

	public String getServerHostname() { return connectOptions.server; }

	private String computeSimpleDisplayName()
	{
		// first check if we already have an @. If we do, just use it directly. no point trying user@gapps-hosted-domain@gmail.com
		if (this.connectOptions.userName.contains("@"))
			return this.connectOptions.userName;
		
		if (connectOptions.server.endsWith("gmail.com") || connectOptions.server.endsWith("google.com") || connectOptions.server.endsWith("googlemail.com"))
			return this.connectOptions.userName + "@gmail.com";
		if (connectOptions.server.endsWith("yahoo.com"))
			return this.connectOptions.userName + "@yahoo.com";
		if (connectOptions.server.endsWith("ymail.com"))
			return this.connectOptions.userName + "@ymail.com";
		if (connectOptions.server.endsWith("live.com"))
			return this.connectOptions.userName + "@live.com";
		if (connectOptions.server.endsWith("hotmail.com"))
			return this.connectOptions.userName + "@hotmail.com";
		else
			return connectOptions.server;
	}

	/** obliterate any passwords that may be stored */
	public void wipePasswords() { 
		connectOptions.wipePasswords(); 
	}
	
	/** returns # of messages in folder. -1 if folder cannot be opened.
	 * connect should already have been called */
    protected Folder openFolderWithoutCount(Store store, String fname) throws MessagingException
	{
		Folder folder = null;

		if (fname == null)
			fname = "INBOX";

		folder = store.getDefaultFolder();
		folder = folder.getFolder(fname);

		if (folder == null)
			throw new RuntimeException ("Invalid folder: " + fname);

		log.info("Opening folder " + Util.blurKeepingExtension(fname) + " in r/o mode...");
		try { folder.open(Folder.READ_ONLY); }
		catch (MessagingException me) { return null; }
		return folder;
	}

	protected Pair<Folder,Integer> openFolder(Store store, String fname) throws MessagingException
	{
		Folder folder = openFolderWithoutCount(store, fname);
		int count = -1; // -1 signals invalid folder
		if (folder != null)
			count = folder.getMessageCount();
		log.info("Opened folder " + Util.blurKeepingExtension(fname) + " message count " + count);
		return new Pair<>(folder, count);
	}

	public void computeFoldersAndCounts(String cacheDir /*unused */) throws MessagingException
	{
		if (store == null)
			connect();
		if (!store.isConnected())
			connect();

		doneReadingFolderCounts = false;
		this.folderInfos = new ArrayList<>();
		if ("pop3".equals(connectOptions.protocol) || "pop3s".equals(connectOptions.protocol))
		{
			Folder f = store.getDefaultFolder();
			f = f.getFolder("INBOX");
			f.open(Folder.READ_ONLY);
			int count = f.getMessageCount();
			f.close(false);
			this.folderInfos.add(new FolderInfo(displayName, getAccountID(), "INBOX", "INBOX", count));
		}
		else
			collect_folder_names(store, this.folderInfos, store.getDefaultFolder());

		folderBeingScanned = "";

		if (connectOptions.server.endsWith(".pobox.stanford.edu"))
			this.folderInfos.add(new FolderInfo(displayName, getAccountID(), "INBOX", "INBOX", 0)); // hack for stanford imap, it lists INBOX as a dir folder! TOFIX

		doneReadingFolderCounts = true;
	}

	/** recursively collect all folder names under f into list */
	private void collect_folder_names(Store store, List<FolderInfo> list, Folder f) throws MessagingException
	{
		// ignore hidden files
		if (f.getFullName().startsWith("."))
			return;
		if (f.getFullName().contains("/.") || f.getFullName().contains("\\."))
			return;

		// hack for csl-mail which takes too long to return all the folders
		if (connectOptions.server.startsWith("csl-mail") && f.getFullName().contains("/"))
			return;

		// TOFIX: apparently imap folders can have both messages and children
		Folder f_children[] = null;
		boolean hasMessages = true, hasChildren = false;
		boolean isPop = "pop3".equals(connectOptions.protocol) || "pop3s".equals(connectOptions.protocol);

		if (!isPop)
		{
			// if its imap, check for children
			hasChildren = (f.getType() & Folder.HOLDS_FOLDERS) != 0;
			hasMessages = (f.getType() & Folder.HOLDS_MESSAGES) != 0;
		}

		if (hasMessages)
		{
			folderBeingScanned = f.getFullName();
			Pair<Folder, Integer> pair = openFolder(store, f.getFullName());
			int count = pair.getSecond();
			if (count != -1)
				pair.getFirst().close(false);

			//			System.out.println ("full name = " + Util.blur(f.getFullName()) + " count = " + count);

			list.add (new FolderInfo(getAccountID(), f.getFullName(), f.getFullName(), count));
			folderBeingScanned = null;
		}

		if (hasChildren)
		{
			f_children = f.list();
			for (Folder child : f_children)
				collect_folder_names(store, list, child);
		}
	}

	//	connects to the store, returns it as well as stores it in this.store
	public Store connect() throws MessagingException
	{
		if (Util.nullOrEmpty(connectOptions.protocol)) // should be at least imap or pop
			return null;

		// Get a Session object
		// can customize javamail properties here, see e.g. http://java.sun.com/products/javamail/javadocs/com/sun/mail/imap/package-summary.html
		// login form will prepend the magic string xoauth, if oauth is being used
	    String oauthToken = "";
	    String OAUTH_MAGIC_STRING = "xoauth"; // defined in loginform
		if (connectOptions.password.startsWith(OAUTH_MAGIC_STRING)) {
		    if ("xoauth".length() < connectOptions.password.length())
		    {
		    	oauthToken = connectOptions.password.substring(OAUTH_MAGIC_STRING.length());
			    mstoreProps.put("mail.imaps.sasl.enable", "true");
			    mstoreProps.put("mail.imaps.sasl.mechanisms", "XOAUTH2");
		    	// mstoreProps.put(OAuth2SaslClientFactory.OAUTH_TOKEN_PROP, oauthToken);
			    log.info("Using oauth login for store " + this);
		    }
		}

		session = Session.getInstance(mstoreProps, null);
	//	session.setDebug(DEBUG);
		Store st = session.getStore(connectOptions.protocol);
		st.connect(connectOptions.server, connectOptions.port, connectOptions.userName, Util.nullOrEmpty(oauthToken) ? connectOptions.password : ""); // no password if oauth
		this.store = st;
		return st;
	}
	
	@Override
	public String getAccountID()
	{
		return connectOptions.userName + "." + connectOptions.server;
	}

	public String toString()
	{
		return "IMAP/POP message store with " + connectOptions;
	}
}
