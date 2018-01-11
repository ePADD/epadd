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
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ImapPrefetcher implements IMAPFolder.ProtocolCommand {

List<Integer> messageNums;
private Session session;

/* start and end are inclusive */
public ImapPrefetcher(Session session, List<Integer> messageNums) {
	this.messageNums = messageNums;
}

/** converts messageNums to a compact message set string. e.g. [1, 2, 3, 5, 6, 9, 11, 12] is converted to 1:3,5:6,9,11-12
 * see grammar for a set in http://james.apache.org/server/rfclist/imap4/rfc2060.txt
 */
static String compactMessageSetString(List<Integer> nums)
{
	// sort the nums first, modifies messageNums, but doesn't matter.
	// messageNums should always be +ve
	Collections.sort(nums);
	
	// identify contiguous ranges and accumulate them in ranges. ranges are inclusive at both ends.
	List<Pair<Integer, Integer>> ranges = new ArrayList<>();
	int INVALID = -2;
	int startOfCurrentRange = INVALID, endOfCurrentRange = INVALID; // -2 means an invalid num
	for (Integer i : nums)
	{
		if (i == endOfCurrentRange+1)
			endOfCurrentRange++; // yay, we're contiguous
		else
		{
			// got a non-contiguous int. add a pair for the prev. range, if we had a valid one
			if (startOfCurrentRange != INVALID)
			{
				Pair<Integer, Integer> p = new Pair<>(startOfCurrentRange, endOfCurrentRange);
				ranges.add(p);
			}
			startOfCurrentRange = endOfCurrentRange = i;
		}
	}
	
	// add a pair for the last one
	if (startOfCurrentRange != INVALID)
	{
		Pair<Integer, Integer> p = new Pair<>(startOfCurrentRange, endOfCurrentRange);
		ranges.add(p);
	}	
	
	if (ranges.size() == 0)
		return ""; // shouldn't happen
	
	StringBuilder sb = new StringBuilder();
	for (Pair<Integer, Integer> p: ranges)
	{
		if (p.getFirst().equals(p.getSecond()))
			sb.append(p.getFirst()); // simple number if start == end
		else
			sb.append(p.getFirst() + ":" + p.getSecond());
		sb.append(",");
	}
	String result = sb.toString();
	result = result.substring(0, result.length()-1); // strip trailing comma
	return result;
}

// see http://stackoverflow.com/questions/8322836/javamail-imap-over-ssl-quite-slow-bulk-fetching-multiple-messages
@Override
public Object doCommand(IMAPProtocol protocol) throws ProtocolException {

	Argument args = new Argument();
	args.writeString(compactMessageSetString(messageNums));

	args.writeString("BODY[]");
	
	Response[] r = protocol.command("FETCH", args);
	List<MimeMessage> result = null;
	Response response = r[r.length - 1];
	if (response.isOK()) 
	{
		result = new ArrayList<>();
		for (int i = 0 ; i < r.length - 1 ; i++) 
		{
			if (r[i] instanceof IMAPResponse) 
			{
				 FetchResponse fetch = (FetchResponse)r[i];

				Object o = fetch.getItem(0);
				if (!(o instanceof BODY))
				{
					EmailFetcherThread.log.warn ("Warning: o is " + o.getClass() + " r.length = " + r.length);
					continue;
				}
				BODY body = (BODY) o;
				ByteArrayInputStream is = body.getByteArrayInputStream();
				try {
					MimeMessage mm = new MimeMessage(session, is);
					result.add(mm);
					//    Contents.getContents(mm, i);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} 
		}
	}
	// dispatch remaining untagged responses
	protocol.notifyResponseHandlers(r);
	protocol.handleResult(response);
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
