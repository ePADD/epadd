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
package edu.stanford.muse.AddressBookManager;

import edu.stanford.muse.webapp.JSPHelper;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/* class to model a mailing list with some members. */
public class MailingList implements java.io.Serializable {
	private final static long serialVersionUID = 1L;
	public final static String MAILING_LIST_MARKER = "[ML]";

	// SUPER_DEFINITE is when we have an explicit List-Post header
	public final static int USER_ASSIGNED = 16, SUPER_DEFINITE = 8, DEFINITE = 1, MAYBE = 2, DEFINITE_NOT = 4, DUNNO = 0;

	private Set<Contact> members = new LinkedHashSet<>();
//	public int state;
	
	private MailingList(Contact ci)
	{
		Contact ci1 = ci;
	}

	private void addMember(Contact c)
	{
		if (c == null) // ignore null contacts
			return;

		members.add(c);
	}

	/** update mailing list state for contacts in ab for 1 message with the given toAddrs (which really refers to all of to/cc/bcc)
	 * sentToMailingLists are confirmed mailingLists that a message has been sent to. */
	static void trackMailingLists(AddressBook ab, List<Address> toAddrs, boolean sent, Address[] froms, String[] sentToMailingLists)
	{
		/*
		 * mailing list states:
		 * all contacts start in state dunno.
		 * we set state to "definite not" if the address/contact was ever a sender
		 * we set state to "definite" if a message was in the corpus but not addressed to the recipient
		 * and all other recipients (if any) were in state definite not.
		 * we set state to "maybe" if a message was in the corpus, but not addressed to the recipient
		 * and there were one or more recipients who were not "definite not".
		 * mailing list members are set only when the message is "definitely" to that list.
		 */
	
		// if we have mailingList evidence in the header, cool -- just mark it as a definite mailing list
		if (sentToMailingLists != null && sentToMailingLists.length > 0)
		{
			for (String mailingList : sentToMailingLists)
			{
				Contact c = ab.lookupByEmail(mailingList);
				if (c != null)
					c.mailingListState |= SUPER_DEFINITE;
			}
	
			if (sentToMailingLists.length == 1)
			{
				// definite list, we make the assumption that all from addresses must belong to this mailing list
				Contact c = ab.lookupByEmail(sentToMailingLists[0]);
	
				if (c != null)
				{
					c.mailingListState |= DEFINITE;
					MailingList ml = ab.mailingListMap.get(c);
					if (ml != null)
						if (froms != null)
							for (Address from: froms)
								if (from instanceof InternetAddress)
									ml.addMember(ab.registerAddress((InternetAddress) from));
				}
			}
		}
	
		// compute toMyAddress (is one of the recipients me ?)
		boolean toMyAddress = false;
		if (toAddrs != null)
			for (Address a : toAddrs)
			{
				if (!(a instanceof InternetAddress))
					continue;
	
				if (ab.isMyAddress((InternetAddress) a))
				{
					toMyAddress = true;
					break;
				}
			}
	
		// if !sent by me and not directly to my address, one of the to's is a mailing list...
		if (!sent && !toMyAddress && toAddrs != null)
		{
			// gather all the possible ml's in the to addr that may be mailing lists,
			// removing definite not's
			List<Contact> possibleLists = new ArrayList<>();
			for (Address a : toAddrs)
			{
				if (!(a instanceof InternetAddress))
					continue;
	
				Contact c = ab.registerAddress((InternetAddress) a);

				if (c == null)
					continue;

				if ((c.mailingListState & DEFINITE_NOT) != 0)
					continue;

                MailingList ml = ab.mailingListMap.computeIfAbsent(c, MailingList::new);

                possibleLists.add(c);
			}
	
			// now, of the possible lists, mark as definite or possible lists
			if (possibleLists.size() == 1)
			{
				// definite list
				Contact c = possibleLists.iterator().next();
				c.mailingListState |= DEFINITE;
				MailingList ml = ab.mailingListMap.get(c); // should be non-null, see above
				if (froms != null)
					for (Address from: froms)
						if (from instanceof InternetAddress)
							ml.addMember(ab.registerAddress((InternetAddress) from));
			}
			else
			{
				for (Contact c: possibleLists)
				{
					MailingList ml = ab.mailingListMap.get(c); // should be non-null, see above
					if(ml==null)
						JSPHelper.log.warn("found null");
					c.mailingListState |= MAYBE; // if only 1 toaddr, definitely ML, otherwise maybe
					if (froms != null)
						for (Address from: froms)
							if (from instanceof InternetAddress)
									ml.addMember(ab.registerAddress((InternetAddress) from));
				}
			}
		}
	
		if (!sent)
		{
			if (froms != null)
				for (Address a : froms)
				{
					if (!(a instanceof InternetAddress))
						continue;
	
					Contact c = ab.registerAddress((InternetAddress) a);
					if (c != null)
						c.mailingListState |= DEFINITE_NOT;
				}
		}
	}
	
//	void setState(int x) { state = x; }
	
}
