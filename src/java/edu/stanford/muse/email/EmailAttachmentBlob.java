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
import java.util.Date;
import java.util.StringTokenizer;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;

import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.util.Util;

public class EmailAttachmentBlob extends Blob implements Serializable {
	private final static long serialVersionUID = 1L;

private boolean equals(Blob b) { return (size == b.size) && filename.equals (b.getResourceURI());} // simplistic, should check for c-hash
public boolean equals(Object o) { return (o instanceof EmailAttachmentBlob) && equals((EmailAttachmentBlob) o); }
public int hashCode() { return ((int) size) ^ filename.hashCode(); }

public EmailAttachmentBlob (String name, long size, Date date) {
	this.filename = name;
	this.size = size;
  //  sentDate = date;
}

// TODO: doesn't handle level of nesting of the message
public EmailAttachmentBlob (String name, long size, MimeMessage m, Part p) throws MessagingException
{
    this.filename = (name == null) ? "NONAME" : name;

    this.size = size;
 //   this.messageID = m.getMessageID();
  //  this.folderName = "dummy"; // m.getFolder().getFullName();
    this.contentType = p.getContentType();
  //  sentDate = m.getSentDate();
    modifiedDate = m.getSentDate();
    if (modifiedDate == null)
    	modifiedDate = new Date(); // hack
   // subject = m.getSubject();
}

private String get_long_form() { return compute_long_form(); }

// pretty toString a message, ls -l style
private String compute_long_form()
{
    StringBuilder sb = new StringBuilder();

    /*
    String type = new ContentType(part.getContentType()).toString();
    int x = type.indexOf('/');
    if (x >= 0)
        type = type.substring(x+1);
    x = type.indexOf(';');
    if (x >= 0)
        type = type.substring(0, x);
    type = Util.truncate(type, 10);
    sb.append(type);
     */

    String f = (filename != null) ? filename : "NONAME";
    String short_name = Util.ellipsizeKeepingExtension(f, 30);
    sb.append(" " + Util.padWidth(short_name, 30));

    String datestr = (modifiedDate != null) ? modifiedDate.toString() : "Unknown Date Found";
    String full_date="";
    if(modifiedDate==null){
        full_date=datestr;
    }else {
        StringTokenizer st = new StringTokenizer(datestr, " ");
        st.nextToken();
        String mon = st.nextToken();
        String date = st.nextToken();
        st.nextToken();
        st.nextToken();
        String year = st.nextToken();
        full_date = mon + " " + date + " " + year;
    }

    sb.append (" | " + Util.padWidth(full_date, 12));

    String size_str = Long.toString(size/1024) + "KB";
    size_str = Util.truncate (size_str, 10);
    sb.append (" | " + size_str);

    /*	sb.append(" | " + sub);

    if (properties != null && properties.size() > 0)
    {
    	sb.append (properties.size() + " properties: {" + properties);
    	for (String s: properties.keySet())
    		sb.append(s + "->" + properties.get(s));
    	sb.append ("}\n");
    }
*/

    return sb.toString();
}

public String toString() { return get_long_form(); }

}

