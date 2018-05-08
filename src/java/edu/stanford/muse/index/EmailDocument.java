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


import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.email.CalendarUtil;
import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.email.EmailFetcherThread;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.util.*;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.o;

/** EmailDocument is really like an email header - it stores metadata about a message.
 * use ed.date, ed.to/from/cc/bcc, and ed.getContents() gets its contents, ed.attachments gets its attachments */
public class EmailDocument extends DatedDocument implements Serializable
{
	private static final long serialVersionUID = 1L;
    //private static Log log = LogFactory.getLog(EmailDocument.class);

    public static final int SENT_MASK = 1;
    public static final int RECEIVED_MASK = 2;

	public String folderName, emailSource;
	public Set<String> folderNames = new LinkedHashSet<>(), emailSources = new LinkedHashSet<>(); // email can now belong to multiple folders, folderName field also maintained for backward compatibility
	public Address[] to, from, cc, bcc; // note: for some reason from[] is an array in JavaMail, because it was supposed to be possible for a message to have multiple senders.
	public String messageID;
	private String uniqueID=null;
	public String sentToMailingLists[];
	public List<Blob> attachments;
	public boolean attachmentsYetToBeDownloaded;
	private String errorString; // error string set only if there was an error in reading this message from the original source
	public long threadID;  // note: valid thread ids must be > 1, 0 means uninitialized
	//@TODO review the following after converting doNotTransfer, transferWithRestrictions and reviewed as labels.
	//public boolean doNotTransfer, transferWithRestrictions, reviewed, addedToCart; // this is for libraries etc who may want export after redaction
	
	// default constructor for serialization
	public EmailDocument() { /* */ }
	public EmailDocument(String id) { this.id = id; } /* prob. useful only for errors */
    private static Log log						= LogFactory.getLog(EmailDocument.class);

	public EmailDocument(String id, String emailSource, String folderName, Address[] to, Address[] cc, Address[] bcc, Address[] from, String subject, String messageID, Date date)
	{
		super(id, subject, date);
		this.to = to;
		this.cc = cc;
		this.bcc = bcc;
		this.from = from;
		this.messageID = messageID;

		//		this.url = url;
		if (folderName != null)
			this.folderName = InternTable.intern(folderName); // many messages will have the same foldername so better to intern
		if (emailSource != null)
			this.emailSource = emailSource;
		this.uniqueID = Util.hash(getSignature());
	}

	public boolean hasError() { return errorString != null; }
	public String getErrorString() { return errorString; }
	public void setErrorString(String errorString) { this.errorString = errorString; }
	@Override
	public String getUniqueId()
	{
		if(!Util.nullOrEmpty(uniqueID))
									return uniqueID;
		else
		{
			uniqueID = Util.hash(getSignature());
			return uniqueID;
		}
	}//folderName + "-" + id; }
	
	/** returns a sorted list of strings based on given addresses.
	 * none of the addresses should be null!
	 * x itself can be null in which case an empty list is returned. */
	private List<String> addressesToList(Address[] x)
	{
		List<String> result = new ArrayList<>();
		if (x != null)
			for (Address a: x)
				result.add(a.toString()); // should not be null
		Collections.sort(result);
		return result;
	}

	private boolean addressArraysEqual(Address[] x1, Address[] x2)
	{
		List<String> list1 = addressesToList(x1);
		List<String> list2 = addressesToList(x2);
		if (list1.size() != list2.size())
			return false;

		// list1 and 2 are sorted, so just run down the list comparing
		// corresponding positions
		for (int i = 0; i < list1.size(); i++)
			if (!list1.get(i).equals(list2.get(i)))
				return false;
		return true;
	}

	@Override
	//This is a subtle method. We were earlier using the logic of this.hashcode()-other.hashcode() to decide if one is smaller than other or not.
	//However, this can cause overflow and that will break the transitivity. Note that it is not sufficient to check that the sign of compare(a,b) is opposite of
	//sign of compare(b,a). Moreover, if a<b and b<c and compare(a,c) should be negative and compare(c,a) should be positive. This might break if there is an overflow
	//when computing a-c or c-a and as a result the sign changes.
	public int compareTo(Document o)
	{
		EmailDocument other = (EmailDocument) o;

		if (this == other) return 0;
/*

		if(other==null) return 1;
		int result = super.compareTo(other);
		if (result != 0) return result;
*/


		String thisid = getUniqueId();
		String otherid = other.getUniqueId();
		return thisid.compareTo(otherid);
	}

	@Override
	public boolean equals (Object o)
	{
		if (!(o instanceof EmailDocument))
			return false;


		EmailDocument other = (EmailDocument) o;

		if(this.compareTo(other)==0)
			return true;
		else
			return false;
		/*
		// check super equals first (for date and time)
		if (!super.equals(o))
			return false;

		// sometimes there might be missing message-id's in which case we waive
		// the checking of message id's
		if (messageID != null && other.messageID != null && !messageID.equals(other.messageID))
			return false;

		boolean result  = addressArraysEqual(to, other.to) && addressArraysEqual(cc, other.cc) && addressArraysEqual(bcc, other.bcc) && addressArraysEqual(from, other.from);
		if(result)
			Util.softAssert((this.compareTo(other)==0) && (other.compareTo(this)==0), "same hashCode/compareTo==0 should imply equals==true",log);
		return result;
*/
	}

	@Override
	public int hashCode()
	{
		return getUniqueId().hashCode();//Integer.parseInt(Util.hash(getSignature()));/*

		/*int result = super.hashCode();
		if (messageID != null)
			result = result*37 ^ messageID.hashCode();
		if (to != null)
			for (Address x: to)
				result = result*37 ^ x.toString().hashCode(); // ^ 1
		if (cc != null)
			for (Address x: cc)
				result = result*37 ^ x.toString().hashCode(); // ^ 2
		if (bcc != null)
			for (Address x: bcc)
				result = result*37 ^ x.toString().hashCode(); // ^ 4
		if (from != null)
			for (Address x: from)
				result = result*37 ^ x.toString().hashCode();
		// without ^ 1,2,4 above a single email addr in either to/cc/bcc will result in same hash value.
		// but changing the code here now will affect existing archives.

		return result;*/
	}

	/** get all addrs associated with the message -- from, to, cc, bcc */
	public List<String> getAllAddrs()
	{
		List<String> result = new ArrayList<>();
		if (to != null) // to can sometimes be null e.g. for mbox files have a "IMAP server data -- DO NOT DELETE" as the first message
			for (Address a: to)
				if (a instanceof InternetAddress)
					result.add(((InternetAddress) a).getAddress().toLowerCase());
		if (cc != null) // to can sometimes be null e.g. for mbox files have a "IMAP server data -- DO NOT DELETE" as the first message
			for (Address a: cc)
				if (a instanceof InternetAddress)
					result.add(((InternetAddress) a).getAddress().toLowerCase());
		if (bcc != null) // to can sometimes be null e.g. for mbox files have a "IMAP server data -- DO NOT DELETE" as the first message
			for (Address a: bcc)
				if (a instanceof InternetAddress)
					result.add(((InternetAddress) a).getAddress().toLowerCase());
		if (from != null)
			for (Address a: from)
				if (a instanceof InternetAddress)
					result.add(((InternetAddress) a).getAddress().toLowerCase());
		return result;
	}

	public List<String> getAllNames()
	{
		List<String> result = new ArrayList<>();
		if (to != null) // to can sometimes be null e.g. for mbox files have a "IMAP server data -- DO NOT DELETE" as the first message
			for (Address a: to)
				if (a instanceof InternetAddress)
					result.add(EmailUtils.cleanPersonName(((InternetAddress) a).getPersonal()));
		if (cc != null)
			for (Address a: cc)
				if (a instanceof InternetAddress)
					result.add(EmailUtils.cleanPersonName(((InternetAddress) a).getPersonal()));
		if (bcc != null)
			for (Address a: bcc)
				if (a instanceof InternetAddress)
					result.add(EmailUtils.cleanPersonName(((InternetAddress) a).getPersonal()));
		if (from != null)
			for (Address a: from)
				if (a instanceof InternetAddress)
					result.add(EmailUtils.cleanPersonName(((InternetAddress) a).getPersonal()));
		return result;
	}

	/* get all participating addrs in this email message, including to, cc, bcc, etc.
	 * except for oneself. */
	public List<String> getAllNonOwnAddrs(Set<String> ownAddrs)
	{
		List<String> result = new ArrayList<>();
		List<String> allAddrs = getAllAddrs();
		for (String s: allAddrs)
			if (!ownAddrs.contains(s))
				result.add(s);
		return result;
	}

	public List<Address> getToCCBCC()
	{
		List<Address> result = new ArrayList<>();
		if (to != null)
			Collections.addAll(result, to);
		if (cc != null)
			Collections.addAll(result, cc);
		if (bcc != null)
			Collections.addAll(result, bcc);
		return result;
	}
	
	/** note: this returns a set, so if the same contact is present multiple times on the message, only one contact is returned */
	public Set<Contact> getToCCBCCContacts(AddressBook ab)
	{
		Set<Contact> result = new LinkedHashSet<>();
		
		if (to != null)		
			for (Address a: to) {
				Contact c = ab.lookupByAddress(a);
				if (c != null)
					result.add(c);
			}
		
		if (cc != null) {
			for (Address a: cc) {
				Contact c = ab.lookupByAddress(a);
				if (c != null)
					result.add(c);
			}
		}
		
		if (bcc != null) {
			for (Address a: bcc) {
				Contact c = ab.lookupByAddress(a);
				if (c != null)
					result.add(c);
			}
		}
		return result;
	}

	/* get all participating addrs in this email message,
	 * including to, cc, bcc, etc.

	 * obsolete: exclude oneself. the sole exception is the special case
	 * when own addrs contains both the sender and all receivers.
	 * ownaddrs can be null or empty.
	 */
	public List<String> getParticipatingAddrsExcept(Set<String> addrs)
	{
		List<String> result = new ArrayList<>();
		List<String> allAddrs = getAllAddrs();
		for (String s: allAddrs)
			if (addrs == null || !addrs.contains(s))
				result.add(s);

		/*
		if (result.size() == 0)
		{
			// no other participants.
			// this should happen only when I send a message to myself
			boolean sentToMe = false, sentByMe = false, sentToOthers = false;

			if (from != null && from.length > 0 && from[0] instanceof InternetAddress)
			{
				String email = ((InternetAddress) from[0]).getAddress().toLowerCase();
				sentByMe = ownAddrs.contains(email);

				if (sentByMe)
				{
					for (Address a: getToCCBCC())
					{
						if (a instanceof InternetAddress)
						{
							String x = ((InternetAddress) a).getAddress().toLowerCase();
							if (ownAddrs.contains(x))
							{
								sentToMe = true;
								result.add(x);
							}
							else
								sentToOthers = true;
						}
					}
				}

				// Actually, this "Unclean data" could have been a draft message whose recipient has not been specified.
				Util.warnIf(!sentToMe || !sentByMe || sentToOthers, "UNCLEAN DATA: Message should have been sent only by me to me, and not to others!");
			}
		}
		*/
		return result;
	}

	/* get all participating addrs in this email message,
	 * including to, cc, bcc, etc.
	 * exclude oneself. the sole exception is the special case
	 * when own addrs contains both the sender and all receivers.
	 * ownaddrs can be null or empty.
	 */
	public List<Contact> getParticipatingContactsExceptOwn(AddressBook addressBook)
	{
		List<Contact> result = new ArrayList<>();
		Contact self = addressBook.getContactForSelf();
		List<String> rawEmailAddrs = getAllAddrs(); // getParticipatingAddrsExcept(addressBook.getOwnAddrs());
		for (String s: rawEmailAddrs)
		{
			Contact c = addressBook.lookupByEmail(s);
			if (c != null && c != self)
				result.add(c);
		}
		return result;
	}

	/* get all participating addrs in this email message,
 * including to, cc, bcc, etc.
 * exclude oneself. the sole exception is the special case
 * when own addrs contains both the sender and all receivers.
 * ownaddrs can be null or empty.
 */
	public Set<Contact> getParticipatingContacts(AddressBook addressBook)
	{
		Set<Contact> result = new LinkedHashSet<>();
		Contact self = addressBook.getContactForSelf();
		List<String> rawEmailAddrs = getAllAddrs(); // getParticipatingAddrsExcept(addressBook.getOwnAddrs());
		for (String s: rawEmailAddrs)
		{
			Contact c = addressBook.lookupByEmail(s);
			if (c != null)
				result.add(c);
		}
		return result;
	}

	public String getSubject()
	{
		StringBuilder sb = new StringBuilder();
		sb.append ("Subject: " + description + "\n");
		sb.append("\n");
		return sb.toString();
	}

	/** used mostly as a debug routine */
	public String getHeader()
	{
		StringBuilder sb = new StringBuilder();
		sb.append ("Folder: " + folderName + "\n");

		StringBuilder timeSB = new StringBuilder();
		Formatter formatter = new Formatter(timeSB);
		if (date != null)
		{
			Calendar c = new GregorianCalendar();
			c.setTime(date);
			formatter.format ("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
			sb.append ("Date: " + c.get(Calendar.DATE) + " " + CalendarUtil.getDisplayMonth(c) + " " + c.get(Calendar.YEAR) + " " + timeSB + "\n");
		}
		formatter.close(); 
		sb.append ("From: " + getFromString() + "\n");
		sb.append ("To: " + getToString() + "\n");
		String cc = getCcString();
		if (!Util.nullOrEmpty(cc))
			sb.append ("Cc: " + cc + "\n");
		String bcc = getBccString();
		if (!Util.nullOrEmpty(bcc))
			sb.append ("Bcc: " + bcc + "\n");
	
		if (description == null)
			description = "<None>";
		sb.append ("Subject: " + description + "\n");
		if (messageID != null)
			sb.append ("Message-ID: " + messageID + "\n");
		sb.append("\n");
		return sb.toString();
	}

	public String getFromEmailAddress()
	{
		if (from != null && from.length > 0)
		{
			// expect only one from address, warn if more than one
			if (from.length > 1)
			{
				log.warn("WARNING!: Multiple from addresses in message (" + from.length + "): " + Util.join (from, ", ") + " Message: " + this);
				for (Address f: from)
					log.warn(f);
			}

			if (from[0] instanceof InternetAddress)
				return ((InternetAddress) from[0]).getAddress().toLowerCase();
			else
				return "<NOT INTERNET ADDRESS>";
		}

		return "<NO FROM EMAIL ADDRESS!?>";
	}

	/** returns something like sudheendra hangal <hangal@cs.stanford.edu>,
	  i.e. both name and email addr.
	 */
	public String getFromString()
	{
		StringBuilder fromSB = new StringBuilder();
		if (from != null && from.length > 0)
		{
			// expect only one from address, warn if more than one
			if (from.length > 1)
			{
				log.warn("Alert!: froms.length > 1: " + from.length);
				for (Address f: from)
					log.warn (f);
			}
			InternetAddress ia = (InternetAddress) from[0];
			fromSB.append (ia.toUnicodeString() + " ");
		}
		else
			fromSB.append ("<None>");

		return fromSB.toString().trim().toLowerCase();
	}

	public String getToString() { return addrsToString(to); }
	public String getCcString() { return addrsToString(cc); }
	public String getBccString() { return addrsToString(bcc); }

	/** returns prefix: addr1, addr2 etc suitable for mbox format */
	private String addrsToString(Address[] as)
	{
		StringBuilder sb = new StringBuilder();
		if (as != null && as.length > 0)
		{
			for (int i = 0; i < as.length; i++)
			{
				InternetAddress b = (InternetAddress)as[i];

				sb.append (b.toUnicodeString());
				if (i < as.length-1)
					sb.append(", ");
			}
		}
		return sb.toString();
	}
	

	/*
	@Override
	public String getContents() throws ReadContentsException
	{
		byte[] b = Util.getBytesFromStream(new URL(url).openStream());
	//	String contents = new String(CryptoUtils.decryptBytes(b), "UTF-8");
		String contents = new String(b, "UTF-8");
		return contents;

		//		BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
//		String inputLine;
//		StringBuilder sb = new StringBuilder();
//		while ((inputLine = in.readLine()) != null)
//		    sb.append(inputLine + "\n");
//		in.close();
//
//		return sb.toString();
	}
	*/	

	public String toString()
	{
		StringBuilder sb = new StringBuilder(folderName + " Msg#");
		sb.append (super.toString());
		if (attachments != null && attachments.size() > 0)
			sb.append (" [" + attachments.size() + " attachment(s)]");
		return sb.toString();
	}

	/** like string but friendly towards crossword hints */
	public String toStringAsHint()
	{
		// the hint looks a little better with newlines embedded in it.
		// but browser support for newlines in title attributes (which is how a hint is currently shown) may be a little iffy... watch out for this.
		StringBuilder sb = new StringBuilder();
		sb.append("Subject: " + description);
		sb.append("\n");
		sb.append((hackyDate ? " guessed ":"") + "Date: " + dateString());
		sb.append("\n");
		List<Address> addrs = getToCCBCC();
		for (Address addr: addrs)
		{
			sb.append (addr);
			sb.append("\n");
		}
		/*
		String folderHint = folderName;
		int idx = folderName.lastIndexOf('/');
		if (idx >= 0 && idx+1 < folderName.length())
			folderHint = folderName.substring(idx+1);
		//sb.append("Folder: " + folderHint);		 
		sb.append("\n");
		*/
		return sb.toString();
	}

	public String toFullString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append (super.toString());
		if (attachments != null && attachments.size() > 0)
		{
			sb.append ("\n" + attachments.size() + " attachments:");
			for (int i = 0; i < attachments.size(); i++)
				sb.append ("Attachment " + i + ". " + attachments.get(i) + "\n");
		}
		return sb.toString();
	}

	/** returns result of checking if message was sent or received by owner.
	 * @return int value with SENT_MASK and RECEIVED_MASK set appropriately. both or neither could also be set.
	 */
	public int sentOrReceived(AddressBook addressBook)
	{
		if (addressBook == null)
			return EmailDocument.SENT_MASK;

		int result = 0;
		String fromAddr = "<NONE>";
		if (from != null && from.length > 0)
			fromAddr = ((InternetAddress) from[0]).getAddress().toLowerCase();
		Contact fromC = addressBook.lookupByEmail(fromAddr);
		Contact ownCI = addressBook.getContactForSelf();

		if (fromC != null)
		{
			if (fromC == ownCI)
				result |= EmailDocument.SENT_MASK;
		}

		Set<Contact> toCCBCC = new LinkedHashSet<>();
		List<Address> addrs = getToCCBCC();
		for (Address addr: addrs)
			if (addr instanceof InternetAddress)
				toCCBCC.add(addressBook.lookupByEmail( ((InternetAddress) addr).getAddress() ));

		// check if all members of the group are included
		for (Contact ci : toCCBCC)
			if (ci == ownCI)
			{
				result |= EmailDocument.RECEIVED_MASK;
				break;
			}
			if(result==0)//if neither sent or received then it is received by default.
				return EmailDocument.RECEIVED_MASK;
		return result;
	}

	/** contentLimit = -1 => no content limit. = 0 => no content. */
	public JSONObject toJSON(int contentLimit) throws JSONException, IOException, GeneralSecurityException
	{
		JSONObject o = new JSONObject();
		o.put("date", CalendarUtil.formatDateForDisplay(date));	
		o.put("from", JSPHelper.formatAddressesAsJSON(from));			
		o.put("to", JSPHelper.formatAddressesAsJSON(to));			
		o.put("cc", JSPHelper.formatAddressesAsJSON(cc));			
		o.put("bcc", JSPHelper.formatAddressesAsJSON(bcc));			
	
		String x = this.description;
		if (x == null)
			x = "<None>";
		o.put("subject", x);
		return o;
	}

	public boolean isThirdPartyName(AddressBook ab, String s)
	{
		if (ab == null || s == null)
			return true;

		List<String> addrs = this.getAllAddrs(); // get all the involved addresses
		// assemble tokens within the names for each address, e.g. "Sudheendra" and "Hangal" are 2 tokens for the addr hangal@cs.stanford.edu
		Set<String> nameTokens = new LinkedHashSet<>();
		for (String addr: addrs)
		{
			Contact c = ab.lookupByEmail(addr);
			if (c == null || c.getNames() == null)
				continue;
			for (String name: c.getNames())
				for (String token: Util.tokenize(name))
					nameTokens.add(token.toLowerCase());
		}
		return !nameTokens.contains(s.toLowerCase());
	}

	private static void maskEmailDomain(Address[] addrs, AddressBook ab)
	{
		if (addrs != null) {
			for (Address a: addrs) {
				if (a instanceof InternetAddress) {
					InternetAddress i = (InternetAddress) a;
					i.setAddress(ab.getMaskedEmail(i.getAddress()));
				}
			}
		}
	}

	public void maskEmailDomain(AddressBook ab)
	{
		maskEmailDomain(to, ab);
		maskEmailDomain(from, ab);
		maskEmailDomain(cc, ab);
		maskEmailDomain(bcc, ab);
	}

	public String getAllAttachmentContent(BlobStore store, String separator)
	{
		if (Util.nullOrEmpty(attachments)) return "";

		StringBuilder result = new StringBuilder();

		for (Blob b : attachments) {
			if (result.length() != 0) result.append(separator);
			Pair<String,String> content = b.getContent(store);
			result.append(content.getFirst() + separator + content.getSecond()); // return metadata also
		}

		return result.toString();
	}

	public String getSignature() {
		StringBuilder sb = new StringBuilder();
		StringBuilder timeSB = new StringBuilder();
		Formatter formatter = new Formatter(timeSB);
		if (this.date != null) {
			GregorianCalendar cc = new GregorianCalendar();
			cc.setTime(this.date);
			formatter.format("%02d:%02d", Integer.valueOf(cc.get(11)), Integer.valueOf(cc.get(12)));
			sb.append("Date: " + cc.get(5) + " " + CalendarUtil.getDisplayMonth(cc) + " " + cc.get(1) + " " + timeSB + "\n");
		}

		formatter.close();
		sb.append("From: " + this.getFromString() + "\n");
		sb.append("To: " + this.getToString() + "\n");
		String cc1 = this.getCcString();
		if(!Util.nullOrEmpty(cc1)) {
			sb.append("Cc: " + cc1 + "\n");
		}

		String bcc = this.getBccString();
		if(!Util.nullOrEmpty(bcc)) {
			sb.append("Bcc: " + bcc + "\n");
		}

		if(this.description == null) {
			this.description = "<None>";
		}
		if(this.messageID !=null)
			sb.append("MessageID: "+this.messageID + "\n");

		sb.append("Subject: " + this.description + "\n");
		sb.append("\n");
		return sb.toString();
	}

	/** recompute own email addrs, and then all the contacts. to be done after fetching all messages */
	public static AddressBook buildAddressBook(Collection<EmailDocument> docs, Collection<String> ownAddrs, Collection<String> ownNames)
	{
		String[] ownAddrsArray = new String[ownAddrs.size()];
		ownAddrs.toArray(ownAddrsArray);
		String[] ownNamesArray = new String[ownNames.size()];
		ownNames.toArray(ownNamesArray);
		AddressBook addressBook = new AddressBook(ownAddrsArray, ownNamesArray);

		//		log.info("Own addresses: " + EmailUtils.emailAddrsToString(ownAddrs));
		EmailFetcherThread.log.debug ("First pass processing contacts for " + docs.size() + " messages");

		// 2 passes here: first pass just to unify and find all own email addrs
		for (EmailDocument ed: docs)
			addressBook.processContactsFromMessage (ed);
		addressBook.organizeContacts();
		//		ownAddrs = addressBook.computeAllAddrsFor(ownAddrs);

		//		log.info ("All my email addrs: ");
		//		for (String s: ownAddrs)
		//			log.info (s);

		// now look for the real contacts, creating a new contacts set object
		// reporcess all messages because we may have misclassified messages wrt sent/received in the first round
		EmailFetcherThread.log.debug ("Second pass processing contacts for " + docs.size() + " messages");
		//		AddressBook newCS = addressBook.deepClone();
		addressBook.resetCounts();
		for (EmailDocument ed: docs)
			addressBook.processContactsFromMessage(ed);
		addressBook.organizeContacts();
		return addressBook;
	}
}
