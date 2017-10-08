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
package edu.stanford.muse.index;

import edu.stanford.muse.email.AddressBook;

import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.Date;

public class PersonDocument extends EmailDocument {
	private static final long serialVersionUID = 1L;
	String pic_url, page_url;
	public String body; // we store the full body here because we don't expect this to be very large
	String comment; // option comment at the time of ingest 
	
	static int nextDocId = 0;
			
	public String getContents() { return body; }
	
	public PersonDocument(AddressBook ab, String friendName, String folder, String description, String body, 
			/* the following params are optional and may be null */ 
			String comment, Date date, String sourceURL, String pictureURL) throws UnsupportedEncodingException
	{
	//	public EmailDocument(int num, String folderName, Address[] to, Address[] cc, Address[] bcc, Address[] from, String subject, String messageID, Date date, String url)
		super(Integer.toString(nextDocId++));
		if (sourceURL == null)
			this.body = body;
		else
			this.body = "Original page: " + sourceURL + "\n" + body;
		this.folderName = folder;
		this.date = (date == null) ? new Date() : date;
		this.description = description;
		
		String myAddr = ab.getContactForSelf().getCanonicalEmail();
		String myName = ab.getContactForSelf().pickBestName();
		this.to = new InternetAddress[]{new InternetAddress(myAddr, myName)};

		String fromAddr = friendName + "@none";
		this.from = new InternetAddress[]{new InternetAddress(fromAddr, friendName)};
		this.pic_url = pictureURL;
		this.page_url = sourceURL;
		this.comment = comment;
		
		this.messageID = null; // make it null explicitly
//		this.url = null;
		
		ab.processContactsFromMessage(this);
		ab.organizeContacts();
	}
}
