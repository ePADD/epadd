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
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.Session;
import java.util.ArrayList;
import java.util.List;

/* version of imap prefetcher that only fetches text fr
om the first <part> of imap messages */
public class TextOnlyImapPrefetcher extends ImapPrefetcher implements IMAPFolder.ProtocolCommand {
    private static final Log log = LogFactory.getLog(TextOnlyImapPrefetcher.class);

Session session;

/* start and end are inclusive */
public TextOnlyImapPrefetcher(Session session, List<Integer> messageNums) {
	super (session, messageNums);
}

/** parses a mime message to get its plain text content, going into multiple levels if necessary. the input is an array of lines, this method
 * should only scan between the indices startLine, inclusive, and endLine, exclusive */
private String getPlainTextFromLines(String lines[], int startLine, int endLine)
{
	String s = lines[startLine];
	String mimeSeparator = null;
	
	if (s.startsWith("--") && !s.startsWith("---"))
	{
		// could be the beginning of a mime part
		/* not unusual to get something like these lines:
		 * --20cf3074b2e646ae9804e0778ed1
		Content-Type: multipart/alternative; boundary=20cf3074b2e646ae9004e0778ed0
		
		--20cf3074b2e646ae9004e0778ed0
		Content-Type: text/plain; charset=ISO-8859-1
		*/
		if (lines.length >= startLine + 2)
		{
			mimeSeparator = lines[startLine];
			String contentTypeLine = lines[startLine+1];
			if (contentTypeLine.toLowerCase().startsWith("content-type"))
			{
				if (!contentTypeLine.toLowerCase().startsWith("content-type: text/plain"))
					log.warn ("Content may be nested: separator: " + mimeSeparator + " content-type line is: " + contentTypeLine + "\n");
				
				// skip separator and content-type line
				startLine += 2;
				// sometimes a blank line follows the contentTypeLine, skip it if present
				if (lines.length > startLine && lines[startLine].equals(""))
					startLine++; 
				
				// look for the end of this part
				for (int i = startLine; i < endLine; i++)
					if (lines[i].equals(mimeSeparator))
					{
						endLine = i;
						break;
					}
				
				// ok, now we've narrowed the range of lines to look at to [startLine, endLine)
				// process recursively
				return getPlainTextFromLines(lines, startLine, endLine); // could be nested			
			}
			// else: this started with --, but it doesn't have a content-type line. we don't know what it is, so we'll return all the lines
		}
	}
	
	// assemble all the text
	StringBuilder sb = new StringBuilder();
	for (int i = startLine; i < endLine; i++)
	{
		sb.append(lines[i]);
		sb.append("\n");
	}
	return sb.toString();
}

// see http://stackoverflow.com/questions/8322836/javamail-imap-over-ssl-quite-slow-bulk-fetching-multiple-messages
@Override
public Object doCommand(IMAPProtocol protocol) {
	Argument args = new Argument();
	String compactString = compactMessageSetString(messageNums); 
	log.info ("BODY[1] " + compactString);
	args.writeString(compactString);

	args.writeString("BODY[1]"); //		args.writeString("BODY[TEXT]");
	
	Response[] r = protocol.command("FETCH", args);
	List<String> result = null;
	Response response = r[r.length - 1];
	if (response.isOK()) 
	{
		result = new ArrayList<>();
		for (int i = 0 ; i < r.length - 1 ; i++) 
		{
			String text = r[i].toString();
			if (r[i] instanceof IMAPResponse) 
			{
				r[i] = null; // null out to save memory
				try {
					if (text.startsWith("* "))
					{
						// fetch response text looks like this: * 28820 FETCH (BODY[1] {6321}\n<actual message text>, followed by a trailing )
						// we have to strip the "* 28820 FETCH (BODY[1] {6321}\n"
						// so compute idx = index of first \r or \n
						int idx1 = text.indexOf("\r");
						int idx2 = text.indexOf("\n");
						if (idx1 < 0)
							idx1 = Integer.MAX_VALUE;
						if (idx2 < 0)
							idx2 = Integer.MAX_VALUE;
						
						// if \r \n are consecutive, then we eliminate them both, so pick the max of the 2 indices. otherwise pick the min, i.e. the lower idx
						int idx;
						if (Math.abs(idx1-idx2) == 1)
							idx = Math.max(idx1, idx2);
						else
							idx = Math.min(idx1, idx2);
						
						// now strip everything up to and including idx from text
						if (idx < text.length())
							text = text.substring(idx+1, text.length()-1); // text.length()-1 as the excluded endIndex eliminates the trailing ) char
					}
					
					// correct for \r\n -> \n only
					text = text.replaceAll("\\r\\n", "\n");
	
					// its possible text (= part 1) could be a multipart itself, if so we'll extract the first part within part 1
					// text is a multipart if it starts with --
					// however be careful not to confuse this with a message starting with "------Forwarded message------"
					// another check could be something like: text.matches("^[a-f0-9]")
					if (text.startsWith("--") && !text.startsWith("---")) 
					{
						String[] lines = text.split("[\\n]", -1);
						String newText = getPlainTextFromLines(lines, 0, lines.length);
						if (newText != null)
							text = newText;
					}
				} catch (Exception e) {
					Util.print_exception(e, log); 
				}
				result.add(text);
			} 
		}
	}
	return result;
}

public static void main (String args[])
{
	// test
	List<Integer> list = new ArrayList<>();
	list.add(1);
	list.add(2);
	list.add(3);
	list.add(9);
	list.add(5);
	list.add(6);
	list.add(11);
	list.add(12);
	String s = compactMessageSetString(list);
	System.out.println(s);
	Util.ASSERT ("1:3,5:6,9,11:12".equals(s));
}

}
