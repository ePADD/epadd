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

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import edu.stanford.muse.webapp.JSPHelper;
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
	private Set<String> names = new LinkedHashSet<>();
	private Set<String> emails = new LinkedHashSet<>();
	public int mailingListState = MailingList.DUNNO;

	//	private Set<String> mailers = new LinkedHashSet<String>();
	//	private Set<String> messageIDs = new LinkedHashSet<String>();
	//	private Set<String> ipAddrs = new LinkedHashSet<String>();
	//	private Set<String> lastReceivedHeaders = new LinkedHashSet<String>();

	public Set<String> getNames() { return names;}
	public Set<String> getEmails() {
		return emails;
	}

	public Contact copy() {
		Contact c = new Contact();
		c.names = names.stream().map(name -> new String(name)).collect(Collectors.toSet());
		c.emails = emails.stream().map(email -> new String(email)).collect(Collectors.toSet());
		c.mailingListState = this.mailingListState;
		return c;
	}

	public boolean equals(Contact other){
		if(!getNames().equals(other.getNames()))
			return false;
		if(!getEmails().equals(other.getEmails()))
			return false;
		if(mailingListState!=other.mailingListState)
			return false;
		return true;
	}
	public int hashCode(){
		int hashcode=1;
		for(String email: getEmails())
			hashcode=hashcode+email.hashCode();
		for(String name: getNames())
			hashcode=hashcode+name.hashCode();
		return hashcode;
	}

	public String getFirstEmail()
	{
		Iterator<String> it = emails.iterator();
		String firstEmail = it.hasNext() ? it.next() : "Unknown";
		return firstEmail;
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
		String bestName = "";

		if (names != null) {
			int bestScore = Integer.MIN_VALUE;
			for (String name : names) {
				if (name.length() == 0)
					continue;
				boolean twoOrMoreTokens = (new StringTokenizer(name).countTokens() > 1);
				boolean capitalized = Character.isUpperCase(name.charAt(0));
				boolean comma = name.indexOf(',') >= 0;
				int score = (twoOrMoreTokens ? 100 : 0)
						+ (capitalized ? 10 : 0)
						- (comma ? 1 : 0);

				score = score * 100;
				score += name.length(); // prefer longer names, but only if most other things are equal.

				if (score > bestScore) {
					bestScore = score;
					bestName = name;
				}
			}
		}
		// default: just return the first name
		if (Util.nullOrEmpty(bestName))
			bestName = getFirstEmail();
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

		Set<String> maskedEmails = new LinkedHashSet<>();
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
		List<Date> allDates = new ArrayList<>();
		for (Date d: allDates)
		{
			Calendar c = new GregorianCalendar();
			c.setTime(d);
			int year = c.get(Calendar.YEAR);
			if (year < 1970)
				System.err.println ("WARNING: Calendar year = " + year);
		}
	}

	////////////////////////Code for writing and reading textual representation of this class.

	/*
	The format for writing a contact object is as defined earlier in dumpForContact method in
	edit-addrssbook.jsp file,
	 */
	public void writeObjectToStream(BufferedWriter out, String description) throws IOException {
		StringBuilder sb = new StringBuilder();
		String mailingListOutput = (this.mailingListState & (MailingList.SUPER_DEFINITE | MailingList.USER_ASSIGNED)) != 0 ? MailingList.MAILING_LIST_MARKER : "";
		sb.append ("-- " + mailingListOutput + " " + description + "\n");

		// extra defensive. c.names is already supposed to be a set, but sometimes got an extra blank at the end.
		Set<String> uniqueNames = new LinkedHashSet<>();
		for (String s: this.getNames())
			if (!Util.nullOrEmpty(s))
				uniqueNames.add(s);
		// uniqueNames.add(s.trim());

		Set<String> uniqueEmails = new LinkedHashSet<>();
		for (String s: this.getEmails())
			if (!Util.nullOrEmpty(s))
				uniqueEmails.add(s);

		for (String s: uniqueNames)
		{
			sb.append (Util.escapeHTML(s) + "\n");
		}
		for (String s: uniqueEmails)
			sb.append (Util.escapeHTML(s) + "\n");
		sb.append(AddressBook.PERSON_DELIMITER);
		sb.append("\n");
		out.append(sb.toString());
	}


	public static Contact readObjectFromStream(BufferedReader in) throws IOException {
		String inp = in.readLine();
		if(inp==null)
			return null;
		Contact tmp = new Contact();
		//int state  = 0;
		//0=name_reading,1= email_reading,2=state_reading
		while(inp!=null){
			if(inp.trim().compareTo(AddressBook.PERSON_DELIMITER)==0)
				return tmp;
			else if(inp.trim().contains("@"))
				tmp.getEmails().add(inp.trim());
			else
				tmp.getNames().add(inp.trim());
			inp = in.readLine();
			/*
			if(inp.trim().compareTo("###Names###")==0){
				inp = in.readLine();
				state=0;
			}
			else if(inp.trim().compareTo("###Emails###")==0){
				//means we have finished reading the names and the subsequent strings should be treated as emails
				inp = in.readLine();
				state =1 ;
			}else if(inp.trim().compareTo("###MailingList###")==0){
				//means we have finished reading emails and the subsequent string should be treated as mailing state.
				inp=in.readLine();
				state = 2;
			}
			if(state==0)
				tmp.getNames().add(inp.trim());
			else if (state==1)
				tmp.getEmails().add(inp.trim());
			else {
				tmp.mailingListState = Integer.parseInt(inp.trim());
				//Util.softAssert(tmp.mailingListState,"Some serious issue in reading mailing list state from the contact object",log);
			}*/

		}
		Util.warnIf(true,"Control should not reach here while reading a contact object", JSPHelper.log);
		return tmp;
	}

}