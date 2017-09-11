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


import com.google.common.collect.Multimap;
import edu.stanford.muse.exceptions.ReadContentsException;
import edu.stanford.muse.index.DatedDocument;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.internet.InternetAddress;
import javax.mail.search.*;
import java.io.Serializable;
import java.util.*;

/** filter for FETCHING email messages. this is only an AND filter, i.e. all clauses must match for the filter to match */
public class Filter implements Serializable {
	private final static long serialVersionUID = 1L;

    private static Log log = LogFactory.getLog(Filter.class);

	List<String> personNameOrEmails = new ArrayList<String>(); // currently can only be 1 person, but we might want to support more
	Set<Contact> personContacts = new LinkedHashSet<Contact>(); // mirror of personNameOrEmails w/contact objs. setup when we are assigned an address book.
	List<String> keywords = new ArrayList<String>();
	Date startDate, endDate;
	boolean sentMessagesOnly;
	Contact ownCI;
	AddressBook addressBook; // used for matching people

	public void setAddressBook(AddressBook addressBook) {
		this.addressBook = addressBook;
		// set up personContacts here, so we don't have to do it repeatedly for every message
		// we assume the addressbook does not change after this
		for (String s: personNameOrEmails)
		{
			Contact c = addressBook.lookupByEmailOrName(s);
			if (c != null)
				personContacts.add(c);
		}
	}

	// this is an AND filter
	public Filter (String personNameOrEmail, boolean sentMessagesOnly, String calendarString, String keywordString)
	{
		this.sentMessagesOnly = sentMessagesOnly;
		if (!Util.nullOrEmpty(personNameOrEmail))
			personNameOrEmails.add(personNameOrEmail);
		if (!Util.nullOrEmpty(calendarString))
			setupDateRange(calendarString);
		if (!Util.nullOrEmpty(keywordString))
			setupKeywords(keywordString);
	}
	
	// this is an AND filter
	public Filter (boolean sentMessagesOnly, Date startDate, Date endDate, String keywordString)
	{
		this.sentMessagesOnly = sentMessagesOnly;
		this.startDate = startDate;
		this.endDate = endDate;
		if (!Util.nullOrEmpty(keywordString))
			setupKeywords(keywordString);
	}

	/** takes current date and converts it to something like 20130709 for July 9 2013 */
	private static String dateToString(Date d)
	{
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(d);
		int yyyy = c.get(Calendar.YEAR);
		int mm = c.get(Calendar.MONTH) + 1; // rememeber + 1 adj cos calendar is 0 based
		int dd = c.get(Calendar.DATE);
		return String.format("%04d", yyyy) + String.format("%02d", mm) + String.format("%02d", dd);
	}

	/**
	 * returns date range in the format accepted by filter, e.g., 20130709-20130710. Used only by memory study etc.
	 */
	public static String getDateRangeForLast1Year()
	{
		Date d = new Date();
		Date d1 = new Date(d.getTime() - (30L * 12 * 24 * 60 * 60 * 1000));
		return dateToString(d1) + "-" + dateToString(d);		
	}
	
	/** returns date range in the format accepted by filter, e.g., 20130709-20130710 */
	public static String getDateRangeForLastNDays(int n)
	{
		Date d = new Date();
		Date d1 = new Date(d.getTime() - (((long) n) * 24 * 60 * 60 * 1000));
		return dateToString(d1) + "-" + dateToString(d);		
	}
	
	public boolean isEmpty()
	{
		return ((personContacts.size() == 0) && !sentMessagesOnly && startDate == null && endDate == null && keywords.size() == 0);
	}

	public void setSentMessagesOnly(boolean b)
	{
		this.sentMessagesOnly = b;
	}

	public void setOwnContactInfo(Contact ownCI)
	{
		this.ownCI = ownCI;
	}

	/** takes in string of the form 20150101-20150131 (both dates are inclusive) and sets up startDate and endDate accordingly. */
	public void setupDateRange(String s)
	{
		if (s != null)
		{
			Pair<Calendar, Calendar> interval = Util.parseDateInterval(s);
			startDate = interval.getFirst().getTime();
			endDate = interval.getSecond().getTime();
			// adjust endDate forward by (1 day - 1ms) because we want it to be inclusive of the day
			endDate = new Date(endDate.getTime() +  (1000L * 60 * 60 * 24) -1);
		}
	}

	public void setupKeywords (String s)
	{
		if (s != null)
			keywords = Util.parseKeywords(s);
	}

	public void addNameOrEmail(String s)
	{
		personNameOrEmails.add(s);
	}

	public boolean matchesDate (DatedDocument dd)
	{
		if (startDate != null && endDate != null)
		{
			Date d = dd.getDate();
			if (d == null)
				return false;
			if (d.before (startDate) || d.after(endDate))
				return false;
		}
		return true;
	}

	public boolean matches(Document d) throws ReadContentsException 
	{
		// look for any reason to return false, if none of them fire, return true.

		DatedDocument dd = null;
		if (d instanceof DatedDocument)
		{
			dd = (DatedDocument) d;
			if (!matchesDate(dd))
				return false;
		}

		if (keywords != null && keywords.size() > 0)
		{
			log.warn("Filtering by keywords during fetch&index is currently disabled");
			Util.softAssert(false,log);
//			String s = d.getContents().toLowerCase();
//			// check for all keywords, if any absent return false
//			for (String keyword: keywords)
//				if (s.indexOf(keyword) < 0)
//					return false;
		}

		// extra checks for email doc
		if (d instanceof EmailDocument)
		{
			// check if any of the people involved in this message are one of personContacts
			EmailDocument ed = (EmailDocument) d;

			if (personContacts.size() > 0)
			{
				// if we don't have an address book, so assume that it's a match
				// (because this filter might be getting called from EmailFetcher.run()
				// which is still in the process of building the addr book.
				// if this is the case, we will explicitly apply the filter again, so its ok.
				if (addressBook != null)
				{
					List<String> list = ed.getAllAddrs();
					Set<Contact> contactsInThisMessage = new LinkedHashSet<Contact>();
					for (String s: list)
					{
						Contact c = addressBook.lookupByEmail(s);
						if (c != null)
							contactsInThisMessage.add(c);
					}

					contactsInThisMessage.retainAll(personContacts);
					if (contactsInThisMessage.size() == 0)
						return false;
				}
			}

			if (sentMessagesOnly)
			{
				if (ownCI != null)
				{
					String fromEmail = ed.getFromEmailAddress();
					Set<String> ownAddrs = ownCI.getEmails();
					if (!ownAddrs.contains(fromEmail))
						return false;
				}
				else
				{
					log.warn ("WARNING: user error: trying to use sent-only option without setting user's own contact info");
					// in this case, we assume a match implicitly because we don't want to filter out all messages
				}
			}
		}
		return true;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (sentMessagesOnly)
			sb.append("Sent-only");

		if (keywords != null && keywords.size() > 0)
		{
			sb.append (" Keywords: ");
			for (String s: keywords)
				sb.append(s + " ");
		}

		if (personNameOrEmails != null && personNameOrEmails.size() > 0)
		{
			sb.append (" Name/email: ");
			for (String s: personNameOrEmails)
				sb.append(s + " ");
		}

		if (startDate != null)
			sb.append (" Date range: " + Util.formatDate(startDate) + "-" + Util.formatDate(endDate));
		return sb.toString();
	}

	public static Filter parseFilter(Multimap<String, String> request)
	{
		if (request == null)
			return null;
		String keywords = JSPHelper.getParam(request,"keywords");
		String dateRange = JSPHelper.getParam(request,"dateRange");
		String sentOnly = JSPHelper.getParam(request,"sentOnly");
		String filterPersonOrEmail = JSPHelper.getParam(request,"filterPersonOrEmail");
		return new Filter(filterPersonOrEmail, (sentOnly != null), dateRange, keywords);
	}
	
	public boolean isActive()
	{
		return (startDate != null && endDate != null) || !Util.nullOrEmpty(keywords);
	}
	
	/* currently supports sent-only, start/end date and keywords in body/subject. these terms are ANDed. sender matching w/email aliases ORed. 
	 * if dates specified, sent or recd. date terms are used based on the incoming param. */
	public SearchTerm convertToSearchTerm(boolean useReceivedDateTerms)
	{
		// FLAG DEBUG: end date = null
		//endDate = null;
		SearchTerm sentOnlyTerm = null;
		if (sentMessagesOnly)
		{
			List<SearchTerm> fromAddrTerms = new ArrayList<SearchTerm>();
			if (ownCI != null)
			{
				for (String e: ownCI.emails)
					try {
						fromAddrTerms.add(new FromTerm(new InternetAddress(e, ""))); // the name doesn't matter for equality of InternetAddress				
					} catch (Exception ex) { Util.print_exception(ex, log); }
				
				// or all the from terms (not and)
				if (fromAddrTerms.size() >= 1)
				{
					sentOnlyTerm = fromAddrTerms.get(0);
					for (int i = 1; i < fromAddrTerms.size(); i++)
						sentOnlyTerm = new OrTerm(sentOnlyTerm, fromAddrTerms.get(i));
				}
			}
		}
		
		SearchTerm result = sentOnlyTerm;
		
		if (startDate != null)
		{
			SearchTerm startTerm = useReceivedDateTerms ? new ReceivedDateTerm(ComparisonTerm.GT, startDate) : new SentDateTerm(ComparisonTerm.GT, startDate);
			if (result != null)
				result = new AndTerm(result, startTerm);
			else
				result = startTerm;
		}
		
		if (endDate != null)
		{
			SearchTerm endTerm = useReceivedDateTerms ? new ReceivedDateTerm(ComparisonTerm.LT, endDate) : new SentDateTerm(ComparisonTerm.LT, endDate);
			if (result != null)
				result = new AndTerm(result, endTerm);
			else
				result = endTerm;
		}
		
		if (keywords != null)
			for (String s: keywords)
			{
				if (Util.nullOrEmpty(s))
					continue;
				// look for this keyword in both subject and body
				SearchTerm sTerm = new OrTerm (new BodyTerm(s), new SubjectTerm(s));
				if (result != null)
					result = new AndTerm(result, sTerm);
				else
					result = sTerm;
			}
		return result;
	}
	
	public static void main (String args[])
	{
		System.out.println ("date range for last 1 year: " + getDateRangeForLast1Year());
	}
}
