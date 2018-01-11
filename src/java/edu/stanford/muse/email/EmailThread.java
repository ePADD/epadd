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


import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** not to be confused with a thread to fetch emails.
    threads emails based on exactly the same normalized subject and email addresses of all participants.
    however, not ideal, because recipients can get added to a thread, this class doesn't handle that case.
 */
public class EmailThread
{
	private String normalizedSubject;
	// Set<String> recipients;
	public List<EmailDocument> emails = new ArrayList<>();
	private Date lastDate = null;

	public EmailThread(EmailDocument email, String normalizedSubject)
	{
		this.normalizedSubject = normalizedSubject;
		addMessage(email);
	}

	public int size()
	{
		return emails.size();
	}

	public void addMessage(EmailDocument email)
	{
		emails.add(email);

		Date d = email.getDate();
		if (d == null)
			return;
		if (lastDate == null)
			lastDate = d;
		else if (d.after(lastDate))
			lastDate = d;
	}

	private boolean withinEndDateRange(Date c)
	{
		int N_DAYS = 30;
		long t1 = lastDate.getTime(); // calendar -> date -> time
		long t2 = c.getTime();
		System.out.println (Util.formatDateLong(lastDate));
		System.out.println (Util.formatDateLong(c));

		long latest = t1 + ((long) 1000)*60*60*24*N_DAYS; // make sure to cast to long, otherwise it overflows to -ve int
		return t2 < latest;
	}

	/** checks that this thread and the other email have exactly the same set of recipient email addresses */
	private boolean hasSameRecipients(EmailDocument other)
	{
		if (emails == null || emails.size() == 0)
			return false;

		EmailDocument firstEmail = emails.get(0);
		List<String> x1 = firstEmail.getAllAddrs();
		List<String> x2 = other.getAllAddrs();
		Collections.sort(x1);
		Collections.sort(x2);
		if (x1.size() != x2.size())
			return false;

		for (int i = 0 ; i < x1.size(); i++)
			if (!x1.get(i).equals(x2.get(i)))
				return false;
		return true;
	}

	public boolean belongsToThisThread (EmailDocument email)
	{
		// actually, its not true the the recipients have to be exactly the same. people can get added to a thread as time goes by.
		// ideally, we should arrange messages in order of time, and ensure that the recipient list only gets added to.
		return withinEndDateRange(email.getDate()) && hasSameRecipients(email);
	}

	public String toString()
	{
		return normalizedSubject + " with " + emails.size() + " messages, lastDate = " + lastDate;
	}
}
