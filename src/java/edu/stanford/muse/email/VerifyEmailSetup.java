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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;

public class VerifyEmailSetup {
    private static Log log = LogFactory.getLog(VerifyEmailSetup.class);

	public static Pair<Boolean, String> run ()
	{
        PrintStream savedOut = System.out;
        InputStream savedIn = System.in;
        
		try {
			   // Get a Properties object
	        Properties props = System.getProperties();
	        
	        // Get a Session object
	        Session session = Session.getInstance(props, null);
	        session.setDebug(true);
	        String filename = System.getProperty ("java.io.tmpdir") + File.separatorChar + "verifyEmailSetup";
	        PrintStream ps = new PrintStream(new FileOutputStream(filename));
	        System.setOut(ps);
	        System.setErr(ps);
	  
	        // Get a Store object
	        Store store = null;
	        store = session.getStore("imaps");
	        store.connect("imap.gmail.com", 993, "checkmuse", ""); // not the real password. unfortunately, the checkmuse a/c will get blocked by google.
//	        Folder folder = store.getFolder("[Gmail]/Sent Mail");
//	        int totalMessages = folder.getMessageCount();
	        // System.err.println (totalMessages + " messages!");
	        ps.close();
	        String contents = Util.getFileContents(filename);
	        System.out.println (contents);
	        return new Pair<>(Boolean.TRUE, contents);
		} catch (AuthenticationFailedException e) {
			/* its ok if auth failed. we only want to check if IMAPS network route is blocked.
			 when network is blocked, we'll get something like
			javax.mail.MessagingException: No route to host; 
			nested exception is: 
			java.net.NoRouteToHostException: No route to host 
			...
			*/
			log.info ("Verification succeeded: " + Util.stackTrace(e));
			return new Pair<>(Boolean.TRUE, "");
		} catch (Exception e)  {
			log.warn ("Verification failed: " + Util.stackTrace(e));
			return new Pair<>(Boolean.FALSE, e.toString()); // stack track reveals too much about our code... Util.stackTrace(e));
		} finally
		{
			System.setOut (savedOut);
			System.setIn (savedIn);
		}
	}
	
	public static void main (String args[])
	{
		run();
	}
}
