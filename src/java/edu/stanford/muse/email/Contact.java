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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.muse.util.UnionFindObject;
import edu.stanford.muse.util.Util;

/** little class for managing all info related to a person.
 * natural ordering rates people higher if they have a higher number of out messages,
 * or higher # of in messages if # of out messages is tied. */
public class Contact extends UnionFindObject {
	private final static long serialVersionUID = 1L;

	// we might have track more fields per contact in the future, but right now just emails/names.
	public Set<String> names = new LinkedHashSet<String>();
	public Set<String> emails = new LinkedHashSet<String>();
	public int mailingListState = MailingList.DUNNO;
	public String canonicalEmail = null; // maybe expensive to compute for each term, so cache it the first time it is computed

	//	private Set<String> mailers = new LinkedHashSet<String>();
	//	private Set<String> messageIDs = new LinkedHashSet<String>();
	//	private Set<String> ipAddrs = new LinkedHashSet<String>();
	//	private Set<String> lastReceivedHeaders = new LinkedHashSet<String>();

	public Set<String> getEmails() {
		return emails;
	}

	public String getCanonicalEmail()
	{
		if (canonicalEmail != null)
			return canonicalEmail;

		Iterator<String> it = emails.iterator();
		canonicalEmail = it.hasNext() ? it.next() : "Unknown";
		return canonicalEmail;
	}



	//	void addMailer(String s)
	//	{
	//		mailers.add(s);
	//	}
	//
	//	void addMessageID(String m)
	//	{
	//		messageIDs.add(m);
	//	}
	//
	//	void addLastReceivedHeader(String s)
	//	{
	//		lastReceivedHeaders.add(s);
	//	}
	//
	//	void addIPAddr(String s)
	//	{
	//		ipAddrs.add(s);
	//	}

	/** pick best name to show for the contact. 2 words better than 1, capitalization better than none.
	 * no comma preferably i.e. prefer First Last to Last, First. */
	public String pickBestName()
	{
		if (names == null || names.size() == 0)
			return getCanonicalEmail();

		String bestName = "";
		int bestScore = Integer.MIN_VALUE;
		for (String name: names)
		{
			if (name.length() == 0)
				continue;
			if (name.toLowerCase().indexOf("viagra") >= 0) // unfortunately, sometimes we see "names" such as "viagra official site"
				continue;
			boolean twoOrMoreTokens = (new StringTokenizer(name).countTokens() > 1);
			boolean capitalized = Character.isUpperCase(name.charAt(0));
			boolean comma = name.indexOf(',') >= 0;
			int score = (twoOrMoreTokens ? 100 : 0)
					+ (capitalized ? 10:0)
					- (comma ? 1:0);

			score = score * 100;
			score += name.length(); // prefer longer names, but only if most other things are equal.

			if (score > bestScore)
			{
				bestScore = score;
				bestName = name;
			}
		}
		// default: just return the first name
		if (Util.nullOrEmpty(bestName))
			bestName = getCanonicalEmail();
		return bestName;
	}

	/** checks whether s occurs anywhere (case normalized) within any name for this contact */
	public boolean checkIfStringPartOfAnyName(String s)
	{
		// look for s in any name for any contact.. but only if it occurs as whole word, e.g. chi shouldn't match chinmay
		s = s.toLowerCase();
		for (String name: this.names) 
			if (Util.occursOnlyAsWholeWord(name,  s))
				return true;
		return false;
	}

	public void maskEmailDomain(AddressBook ab)
	{
		if (!Util.nullOrEmpty(canonicalEmail))
			canonicalEmail = ab.getMaskedEmail(canonicalEmail);

		Set<String> maskedEmails = new LinkedHashSet<String>();
		for (String e : emails) {
			maskedEmails.add(ab.getMaskedEmail(e));
		}
		emails = maskedEmails;
		// "names" may also contain email address - see comments in AddressBook:maskEmailDomain()
	}

	/** return all names and emails as a tooltip */
	public String toTooltip()
	{
		String result = "";
		// CR not guaranteed to work in tooltips in non-IE browsers
		// see: http://stackoverflow.com/questions/358874/how-can-i-use-a-carriage-return-in-a-html-tooltip
		for (String s: names)
			result += s + " \n";
		for (String s: emails)
			result += s +  " \n";
		return result;
	}

	//	void addMailer(String s)
	//	{
	//		mailers.add(s);
	//	}
	//
	//	void addMessageID(String m)
	//	{
	//		messageIDs.add(m);
	//	}
	//
	//	void addLastReceivedHeader(String s)
	//	{
	//		lastReceivedHeaders.add(s);
	//	}
	//
	//	void addIPAddr(String s)
	//	{
	//		ipAddrs.add(s);
	//	}

	//	void addMailer(String s)
	//	{
	//		mailers.add(s);
	//	}
	//
	//	void addMessageID(String m)
	//	{
	//		messageIDs.add(m);
	//	}
	//
	//	void addLastReceivedHeader(String s)
	//	{
	//		lastReceivedHeaders.add(s);
	//	}
	//
	//	void addIPAddr(String s)
	//	{
	//		ipAddrs.add(s);
	//	}

	/** resets everything except names and email addrs */
	public void resetInfo()
	{
		mailingListState = MailingList.DUNNO;
		//	maybeMailingList = 0;
		//		mailers.clear();
		//		messageIDs.clear();
		//		ipAddrs.clear();
		//		lastReceivedHeaders.clear();
	}

	void merge(Contact other)
	{
		names.addAll(other.names);
		emails.addAll(other.emails);
		//		messageIDs.addAll(other.messageIDs);
		//		ipAddrs.addAll(other.ipAddrs);
		//		lastReceivedHeaders.addAll(other.lastReceivedHeaders);
		//		mailers.addAll(other.mailers);
	}

	/** return all emails and names for this user in a format for mozilla contacts matching */
	public String toMozContactsDescriptionStr()
	{
		String result = "";
		for (String s: emails)
			result += s +  " --- ";
		for (String s: names)
			result += s + " --- ";
		return result;
	}

	public JSONObject toJson() throws JSONException
	{
		JSONObject obj = new JSONObject();

		JSONArray arr = new JSONArray();
		int i = 0;
		for (String s: emails)
			arr.put(i++, s);
		obj.put("emailAddrs", arr);

		arr = new JSONArray();
		i = 0;
		for (String s: names)
			arr.put(i++, s);
		obj.put("names", arr);
		return obj;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (String s : names)
			sb.append (s + " ");

		for (String s : emails)
			sb.append (s + " ");

		return sb.toString();
	}

	public String fullToString()
	{
		StringBuilder sb = new StringBuilder();
		for (String s : names)
			sb.append (s + " = ");
		for (String s : emails)
			sb.append (s + " = ");

		//		sb.append ("\n------Mailers---------\n");
		//		for (String s: mailers)
		//			sb.append (s + "\n");
		//
		//		sb.append ("\n------IP addresses---------\n");
		//		for (String s: ipAddrs)
		//			sb.append(s + "\n");
		//
		//		sb.append ("\n------Message IDs---------\n");
		//
		//		for (String s: messageIDs)
		//		{
		//			if (s.endsWith("@mail.gmail.com>"))
		//			{
		//				s = s.substring (1, s.length() - "@mail.gmail.com>".length());
		//				String id = s.substring (0, 8);
		//				s = s.substring(8);
		//				String date = s.substring(0, 10);
		//				s = s.substring(10);
		//
		//				String x = id + " - " + date + " - ";
		//				for (int i = 0; i < s.length(); i++)
		//					x = x + s.charAt(i) + " ";
		//				s = x;
		//			}
		//			sb.append (s + "\n");
		//		}
		//		sb.append ("\n------Last Received Headers---------\n");
		//
		//		for (String s: lastReceivedHeaders)
		//			sb.append (s + "\n");

		return sb.toString();
	}

	public void verify()
	{
		List<Date> allDates = new ArrayList<Date>();
		for (Date d: allDates)
		{
			Calendar c = new GregorianCalendar();
			c.setTime(d);
			int year = c.get(Calendar.YEAR);
			if (year < 1970)
				System.err.println ("WARNING: Calendar year = " + year);
		}
	}
}