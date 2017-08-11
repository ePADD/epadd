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

import edu.stanford.muse.index.DatedDocument;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.util.DictUtils;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * VIP class.
 * A set of contacts gleaned from email messages.
 * includes some data integration, i.e. people with same name & diff. email addr (or vice versa)
 * are all merged into one contact object.
 * an addressbook can be merged with another, unifying names and email addresses in common.
 * Usage:
 * to setup: create a addressBook(self addrs),
 * call processContactsFromMessage for each message
 * then call addressBook.organizeContacts()
 * to use: call addressBook.lookupContact(...)
 * <p>
 * alternate:
 * use: initialize(text)
 */

public class AddressBook implements Serializable {

    public static Log log = LogFactory.getLog(AddressBook.class);
    private final static long serialVersionUID = 1L;

    /**
     * there are 3 important maps maintained in the address book.
     * emailToContact from email address to corresponding contact
     * nameToContact from a (multi-word) normalized name to corresponding contact (normalized means case-and-space-canonicalized, order independent, etc. See Util.normalizePersonNameForLookup()
     * nameTokenToContacts is from a single token in a word to all names with that word.
     * <p>
     * be careful! lower/upper case for the key to emailToContact. has caused problems before. ideally everything should be lower case.
     * be careful! nameToContact has to be only looked up with the result of Util.normalizePersonNameForLookup(string). the lookup string is different from the name's display string!
     * preferably do not use emailToContact and nameToContact directly -- use lookupEmail or lookupName
     */
    private Map<String, Contact> emailToContact = new LinkedHashMap<String, Contact>(); // do not .put/get directly, use addEmailAddressForContact()
    private Map<String, Contact> nameToContact = new LinkedHashMap<String, Contact>(); // do not access directly, use addNameForContactAndUpdateMaps()
    private Map<String, Set<Contact>> nameTokenToContacts = new LinkedHashMap<String, Set<Contact>>(); // maps, e.g., Jim to all contacts with Jim in its name

    private Contact contactForSelf;
    private Collection<String> dataErrors = new LinkedHashSet<String>();

    /**
     * these are for providing a layer of opaqueness to contact emails in discovery mode
     */
    transient private List<Contact> contactListForIds = new ArrayList<Contact>();
    private Map<Contact, Integer> contactIdMap = new LinkedHashMap<Contact, Integer>();
    transient private Map<String, String> emailMaskingMap = null;

    public Map<Contact, MailingList> mailingListMap = new LinkedHashMap<Contact, MailingList>();

    /**
     * create a new contact set with the given list of self addrs. selfAddrs can be null or empty
     * in which case no addresses are considered selfAddrs
     */
    public AddressBook(String[] selfAddrs, String[] selfNames) {
        setup(selfAddrs, selfNames);
    }

    private AddressBook(Set<String> selfAddrsSet, Set<String> selfNamesSet) {
        String[] selfAddrs = new String[selfAddrsSet.size()];
        int i = 0;
        StringBuilder about = new StringBuilder();
        about.append("Addressbook created with " + selfAddrs.length + " addr(s):\n");
        for (String s : selfAddrsSet) {
            selfAddrs[i++] = s;
            about.append(s + "\n");
        }
        String[] selfNames = new String[selfNamesSet.size()];
        about.append("and " + selfNames.length + " name(s):");

        i = 0;
        for (String s : selfNamesSet) {
            selfNames[i++] = s;
            about.append(s + "\n");
        }
        setup(selfAddrs, selfNames);
        log.info(about);
    }

    private synchronized void setup(String[] selfAddrs, String[] selfNames) {
        // create a new ContactInfo for owner
        Contact c = new Contact();
        contactIdMap.put(c, contactListForIds.size());
        contactListForIds.add(c);
        if (selfAddrs != null)
            for (String s : selfAddrs)
                addEmailAddressForContact(s, c);

        if (selfNames != null)
            for (String n : selfNames)
                addNameForContactAndUpdateMaps(n, c);
        contactForSelf = c;
    }

    /**
     * initialize addressbook from lines of this format:
     * #person 1
     * email1.1
     * email1.2
     * name1.1
     * -- (separator token)
     *
     * @person2 email2.1
     * name2.1
     * etc.
     */
    public void initialize(String text) {
        // todo: decide how to handle names are missing in text, but are associated with some email message in the archive.
        // when lookup is performed on such an archive, it may return null.
        // similar room for inconsistency when user can have the same name or email addr in multiple contacts
        String PERSON_DELIMITER = "--";
        nameToContact.clear();
        emailToContact.clear();
        nameTokenToContacts.clear();

        // Q: what to do if user removed a name or address from the map?
        List<String> lines = Util.tokenize(text, "\r\n");
        List<String> linesForContact = new ArrayList<String>();
        boolean thisPersonIsMailingList = false, nextPersonIsMailingList = false;
        contactForSelf = null; // the first contact is contactForSelf
        for (int i = 0; i <= lines.size(); i++) {
            boolean endOfInput = (i == lines.size());
            boolean endOfPerson = endOfInput; // if end of input, definitely end of person. otherwise, could still be end of person if the line starts with PERSON_DELIMITER
            if (!endOfInput) {
                String line = lines.get(i).trim();
                if (line.startsWith(PERSON_DELIMITER)) {
                    endOfPerson = true;
                    // check if the next characters after PERSON_DELIMITER are the mailing list marker
                    nextPersonIsMailingList = line.substring(PERSON_DELIMITER.length()).trim().startsWith(MailingList.MAILING_LIST_MARKER);
                }
                else {
                    if (!Util.nullOrEmpty(line)) // && !line.startsWith("#")) -- some strange address in jeb bush start with # (!)
                        linesForContact.add(line);
                }
            }

            if (endOfPerson) {
                // end of a contact, process linesForContact
                if (linesForContact.size() > 0) {
                    Contact c = new Contact();
                    if (thisPersonIsMailingList)
                        c.mailingListState |= MailingList.USER_ASSIGNED;
                    for (String s : linesForContact)
                        if (s.contains("@"))
                            addEmailAddressForContact(s, c);
                        else
                            addNameForContactAndUpdateMaps(s, c);

                    if (contactForSelf == null) // this must be the first contact
                        contactForSelf = c;
                }
                linesForContact.clear();
                thisPersonIsMailingList = nextPersonIsMailingList;
                nextPersonIsMailingList = false;
            }
        }
        String firstLines = text.substring(0, text.substring(3).indexOf("--"));
        if (contactForSelf == null || contactForSelf.emails == null || contactForSelf.emails.size() == 0)
            log.warn("Could not identify self in the starting lines: \n" + firstLines);
        else
            log.info("Initialised self contact: " + contactForSelf);
        reassignContactIds();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // the no-arg constructor to do the needed setup when an address book is initialized thru deserialization
    }

    private void addEmailAddressForContact(String email, Contact c) {
        if (Util.nullOrEmpty(email))
            return;

        email = EmailUtils.cleanEmailAddress(email);
        c.emails.add(email);
        emailToContact.put(email, c);
    }

    /**
     * this is an important method that should be the only way to update a name in a contact, and maintains the nameToContact and nameTokenToContact maps.
     * it normalizes the given name and adds it to c.
     * however, it adds it to the nameToContact map only if its a multi-word name.
     * In any case it also adds it nameTokenToContact map for all words in the name.
     */
    private void addNameForContactAndUpdateMaps(String name, Contact c) {
        if (Util.nullOrEmpty(name))
            return;

        // trim is the one operation we do on the source name. otherwise, we want to retain it in its original form, for capitalization etc.
        name = name.trim();
        c.names.add(name);

        // nameToContact is very important, so only add to it if we're fairly certain about the name.
        if (Util.tokenize(name).size() > 1)
            nameToContact.put(EmailUtils.normalizePersonNameForLookup(name), c);

        List<String> tokens = EmailUtils.normalizePersonNameForLookupAsList(name);
        if (tokens == null) return;
        for (String token : tokens) {
            Set<Contact> contactSet = nameTokenToContacts.get(token);
            if (contactSet == null) {
                contactSet = new LinkedHashSet<Contact>();
                nameTokenToContacts.put(token, contactSet);
            }
            contactSet.add(c);
        }
    }


    /**
     * the Contacts for name and email are unified if they are not already the same
     * returns the contact for name/email (creates a new contact if needed)
     * name could be null (or empty, which is equivalent), email cannot be
     *
     * @return
     */
    private synchronized Contact unifyContact(String email, String name) {
        // we'll implement a weaker pre-condition: both name and email could be null
        Contact cEmail = null;
        Contact cName = null;
        if (Util.nullOrEmpty(email)) {
            log.warn("Confused: email is null or empty:\n" + Util.stackTrace());
            return null;
        }

        if (name != null && name.length() == 0)
            name = null; // map empty name to null -- we don't want to have contacts for empty names

        if (name != null && name.equals(email)) // if the name is exactly the same as the email, it has no content.
            name = null;

        email = email.trim().toLowerCase();
        // get existing contacts for email/name
        cEmail = lookupByEmail(email);
        if (name != null)
            cName = lookupByName(name);

        // if name and email have different CIs, unify them first
        if (cName != null && cEmail != null && cName != cEmail) {
            log.debug("Merging contacts due to message with name=" + name + ", email=" + email + " Contacts are: " + cName + " -- and -- " + cEmail);
            cEmail.unify(cName);
        }

        if (cEmail != null) {
            if (name != null)
                addNameForContactAndUpdateMaps(name, cEmail);
            return cEmail;
        }

        if (cName != null) {
            addEmailAddressForContact(email, cName);
            return cName;
        }

        // neither cEmail nor cName was found. new contact.

        Contact c = new Contact(); // this blank Contact will be set up on the fly later
        contactIdMap.put(c, contactListForIds.size());
        contactListForIds.add(c);
        addEmailAddressForContact(email, c);

        if (name != null) {
            addNameForContactAndUpdateMaps(name, c);
        }

        return c;
    }

    /**
     * returns the number of contacts in this address book
     */
    public int size() {
        return allContacts().size();
    }

    /**
     * returns an unmodifiable set of the owner's email addresses
     */
    public Set<String> getOwnAddrs() {
        if (contactForSelf != null && contactForSelf.emails != null)
            return Collections.unmodifiableSet(contactForSelf.emails);

        return new LinkedHashSet<String>();
    }

    /**
     * returns an unmodifiable set of the owner's names
     */
    public Set<String> getOwnNamesSet() {
        // just trim because some names seem to have spaces remaining at the end
        Set<String> result = new LinkedHashSet<String>();
        for (String s : contactForSelf.names)
            result.add(s.trim());
        return Collections.unmodifiableSet(result);
    }

    /**
     * gets the contact object for this address book's owner
     */
    public Contact getContactForSelf() {
        return contactForSelf;
    }

    /**
     * return best name to display for owner of this address book. empty string if no names available.
     * warning: this overlaps with contact.pickbestname()
     */
    public String getBestNameForSelf() {
        Contact c = getContactForSelf();
        if (c == null || c.names == null || Util.nullOrEmpty(c.names))
            return "";

        return c.names.iterator().next(); // pick first name for self (not best name! because the best name tries to find the longest string etc. Here for the own name, we want to stay with whatever was provided when the archive/addressbook was created.)
    }

    /* subtracts email addresses of the address book's owner from the given list. Used for things like network graphs. */
    public List<String> removeOwnAddrs(List<String> input) {
        return Util.removeStrings(input, getOwnAddrs());
    }

    /**
     * returns the best name to display for the given email address.
     * If we don't have any name for the owner, we'll return the email address.
     */
    public String getBestDisplayNameForEmail(String email) {
        Contact c = this.lookupByEmail(email);
        if (c == null) {
            log.warn("No contact for email address! " + email);
            return "";
        }
        String displayName;
        if (c.names != null && c.names.size() > 0)
            displayName = c.pickBestName(); // just pick the first name
        else
            displayName = email;

        return displayName;
    }

    /**
     * returns true iff a's email address is one of the address book owner's
     */
    boolean isMyAddress(InternetAddress a) {
        if (a == null)
            return false;

        String email = EmailUtils.cleanEmailAddress(a.getAddress());
        Contact selfContact = getContactForSelf();
        if (selfContact == null)
            return false;

        return selfContact.emails.contains(email);
    }

    /**
     * preferably use this instead of using emailToContact directly.
     * protects against null input and canonicalizes the input etc.
     * be prepared for it to return null, just in case (the addr book could have been re-initialized directly by the user
     */
    public Contact lookupByEmail(String s) {
        if (s == null)
            return null;
        String s1 = EmailUtils.cleanEmailAddress(s);
        return emailToContact.get(s1);
    }

    public Contact lookupByAddress(Address a) {
        if (a == null || !(a instanceof InternetAddress))
            return null;
        InternetAddress ia = (InternetAddress) a;
        String s = ia.getAddress();
        String s1 = EmailUtils.cleanEmailAddress(s);
        return emailToContact.get(s1);
    }

    /**
     * returns the contact object for the given name.
     * If multi word name, looks up in multi word map.
     * If single word name, looks up in the nameTokenToContacts map and returns the first one in that map (since there can be several)
     */
    public Contact lookupByName(String s) {
        // look among the single tokens
        List<String> tokens = Util.tokenize(s);
        if (tokens.size() == 1) {
            // single word name
            String normalizedName = EmailUtils.normalizePersonNameForLookup(s); // normalize even if single word name
            if (normalizedName == null)
                return null;

            Collection<Contact> c = nameTokenToContacts.get(normalizedName); // note
            if (c != null && c.size() > 0)
                return c.iterator().next(); // ooh... more than one contact has this (single) name arbitrarily returns the first one
            else
                return null;
        } else {
            return lookupByNameMultiWord(s);
        }
    }

    /**
     * this is a lookup for multiword names only.
     * it uses the nameToContact map which is only used for multiword names
     */
    private Contact lookupByNameMultiWord(String s) {
        if (s == null)
            return null;
        String normalizedName = EmailUtils.normalizePersonNameForLookup(s);
        if (normalizedName == null)
            return null;

        return nameToContact.get(normalizedName);
    }

    public Set<Contact> lookupByNameTokenAsSet(String s) {
        if (s == null)
            return null;
        String normalizedName = EmailUtils.normalizePersonNameForLookup(s);
        if (normalizedName == null)
            return null;

        Set<Contact> result = new LinkedHashSet<Contact>();
        Contact c = nameToContact.get(normalizedName);
        if (c != null)
            result.add(c);

        List<String> tokens = EmailUtils.normalizePersonNameForLookupAsList(s);
        if (!Util.nullOrEmpty(tokens)) {
            for (String token : tokens) {
                Set<Contact> set = nameTokenToContacts.get(token);
                if (set != null) {
                    result.addAll(set);
                }
            }
        }

        return result;
    }

    public Contact lookupByEmailOrName(String s) {
        if (s == null)
            return null;

        s = s.trim().toLowerCase();
        Contact c = lookupByEmail(s);
        if (c != null)
            return c;

        // not an email addr, lets try to find a name
        return lookupByName(s);
    }

    /* Single place where a <name, email address> equivalence is registered (and used to build the address book and merge different names/email addresses together.
        Any evidence of a name belonging to an email address should be logged by calling this method.
        Warning: can return null if the email address is null! */
    Contact registerAddress(InternetAddress a) {
        // get email and name and normalize. email cannot be null, but name can be.
        String email = a.getAddress();
        email = EmailUtils.cleanEmailAddress(email);
        String name = a.getPersonal();
        name = EmailUtils.cleanPersonName(name);

        if (!Util.nullOrEmpty(name)) {
            // watch out for bad "names" and ignore them
            if (name.toLowerCase().equals("'" + email.toLowerCase() + "'")) // sometimes the "name" field is just the same as the email address with quotes around it
                name = "";
            if (name.contains("@"))
                name = ""; // name can't be an email address!
        }

        for (String s : DictUtils.bannedStartStringsForEmailAddresses) {
            if (email.toLowerCase().startsWith(s)) {
                log.info("not going to consider name-email pair. email: " + email + " name: " + name + " because email starts with " + s);
                name = ""; // usually something like info@paypal.com or info@evite.com or invitations-noreply@linkedin.com -- we need to ignore the name part of such an email address, so it doesn't get merged with anything else.
                break;
            }
        }

        List nameTokens = Util.tokenize(name);

        if (Util.nullOrEmpty(email)) {
            return null; // we see this happening in the scamletters dataset -- email addr itself is empty!
        }

        Contact c = unifyContact(email, name);

        // DEBUG point: enable this to see all the incoming names and email addrs
        if (c != null) {
            if (!c.emails.contains(email)) {
                if (log.isDebugEnabled())
                    log.debug("merging email " + email + " into contact " + c);
                c.emails.add(email);
            }

            if (!Util.nullOrEmpty(name) && !c.names.contains(name)) {
                if (log.isDebugEnabled() && !c.names.contains(name))
                    log.debug("merging name " + name + " into contact " + c);
                c.names.add(name);
            }
        }

        // look for names from first.last@... style of email addresses
        List<String> namesFromEmailAddress = EmailUtils.parsePossibleNamesFromEmailAddress(email);
        if (log.isDebugEnabled()) {
            for (String s : namesFromEmailAddress)
                log.debug("extracted possible name " + s + " from email " + email);
        }
        if (namesFromEmailAddress != null)
            for (String possibleName : namesFromEmailAddress) {
                if (nameTokens.size() >= 2)
                    unifyContact(email, possibleName);
            }
        return c;
    }

    private boolean processContacts(List<Address> toCCBCCAddrs, Address fromAddrs[], String[] sentToMailingLists) {
        // let's call registerAddress first just so that the name/email maps can be set up
        if (toCCBCCAddrs != null)
            for (Address a : toCCBCCAddrs)
                if (a instanceof InternetAddress)
                    registerAddress((InternetAddress) a);
        if (fromAddrs != null)
            for (Address a : fromAddrs)
                if (a instanceof InternetAddress)
                    registerAddress((InternetAddress) a);

        Pair<Boolean, Boolean> p = isSentOrReceived(toCCBCCAddrs, fromAddrs);
        boolean sent = p.getFirst();
        boolean received = p.getSecond();

        // update mailing list state info
        MailingList.trackMailingLists(this, toCCBCCAddrs, sent, fromAddrs, sentToMailingLists);

        if (sent && toCCBCCAddrs != null)
            for (Address a : toCCBCCAddrs) {
                if (!(a instanceof InternetAddress))
                    continue;
                registerAddress((InternetAddress) a);
            }

        // for sent messages we don't want to count the from field.
        // note: it's pretty important we have the correct addrs for self.
        // otherwise, any email we *send* is going to get counted as received, and loses all the senders
        // while counting the user as "from"
        if (!sent) {
            if (fromAddrs != null)
                for (Address a : fromAddrs) {
                    if (!(a instanceof InternetAddress))
                        continue;

                    registerAddress((InternetAddress) a);
                }
        }

        if (!sent && !received) {
            // let's assume received since one of the to's might be a mailing list
            if (toCCBCCAddrs == null || toCCBCCAddrs.size() == 0)
                return false;
        }
        return true;
    }

    /**
     * this method should be called with every email doc in the archive
     */
    public synchronized void processContactsFromMessage(EmailDocument ed) {
        List<Address> toCCBCC = ed.getToCCBCC();
        boolean noToCCBCC = false;
        if (toCCBCC == null || toCCBCC.size() == 0) {
            noToCCBCC = true;
            markDataError("No to/cc/bcc addresses for: " + ed);
        }
        if (ed.from == null || ed.from.length == 0)
            markDataError("No from address for: " + ed);
        if (ed.date == null)
            markDataError("No date for: " + ed);
        boolean b = processContacts(ed.getToCCBCC(), ed.from, ed.sentToMailingLists);
        if (!b && !noToCCBCC) // if we already reported no to address problem, no point reporting this error, it causes needless duplication of error messages.
            markDataError("Owner not sender, and no to addr for: " + ed);
    }

    /**
     * checks if s occurs (case normalized) as part of ANY name for ANY contact. used for memory study
     */
    public boolean isStringPartOfAnyAddressBookName(String s) {
        Collection<Contact> contacts = this.allContacts();
        for (Contact c : contacts)
            if (c.checkIfStringPartOfAnyName(s))
                return true;
        return false;
    }

    private void markDataError(String s) {
        log.debug(s);
        dataErrors.add(s);
    }

    /**
     * detect if message sent or received. all 4 return states are possible
     */
    public Pair<Boolean, Boolean> isSentOrReceived(List<Address> toCCBCCAddrs, Address[] fromAddrs) {
        boolean sent = false, received = false;

        Address[] froms = fromAddrs;
        if (froms != null) {
            if (froms.length > 1) {
                log.warn("Alert!: froms.length > 1: " + froms.length);
                for (Address from : froms)
                    log.warn(from);
            }

            for (Address from : froms) {
                if (isMyAddress((InternetAddress) from)) {
                    sent = true;
                    break;
                }
            }
        }

        if (toCCBCCAddrs != null) {
            for (Address addr : toCCBCCAddrs) {
                if (isMyAddress((InternetAddress) addr)) {
                    received = true;
                    break;
                }
            }
        }

        return new Pair<Boolean, Boolean>(sent, received);
    }

    /**
     * how many of the messages in the given collection are outgoing?
     */
    public int getOutMessageCount(Collection<EmailDocument> docs) {
        int count = 0;
        Contact me = getContactForSelf();
        if (me != null) {
            for (EmailDocument ed : docs) {
                String fromEmail = ed.getFromEmailAddress();
                Set<String> selfAddrs = me.getEmails();
                if (selfAddrs.contains(fromEmail))
                    count++;
            }
        }
        return count;
    }

    /* returns a pair of lists of in and out dates of the given docs.
     if inNOut is false, all dates are set to outdates */
    public Pair<List<Date>, List<Date>> splitInOutDates(Collection<DatedDocument> docs, boolean inNOut) {
        // compute in/out dates for each group
        List<Date> inDates = new ArrayList<Date>();
        List<Date> outDates = new ArrayList<Date>();
        for (DatedDocument dd : docs) {
            if (!inNOut)
                outDates.add(dd.date);
            else {
                int sentOrReceived = EmailDocument.RECEIVED_MASK;
                if (dd instanceof EmailDocument)
                    sentOrReceived = ((EmailDocument) dd).sentOrReceived(this);
                if ((sentOrReceived & EmailDocument.SENT_MASK) != 0)
                    outDates.add(dd.date);
                if ((sentOrReceived & EmailDocument.RECEIVED_MASK) != 0)
                    inDates.add(dd.date);
                if (((sentOrReceived & EmailDocument.RECEIVED_MASK) == 0) &&
                        ((sentOrReceived & EmailDocument.SENT_MASK) == 0)) // assume received
                {
                    inDates.add(dd.date);
                }
            }
        }

        return new Pair<>(inDates, outDates);
    }

    public synchronized Collection<String> getDataErrors() {
        if (dataErrors == null)
            dataErrors = new LinkedHashSet<String>();
        return Collections.unmodifiableCollection(dataErrors);
    }

    /* merges self addrs with other's, returns the self contact */
    private Contact mergeSelfAddrs(AddressBook other) {
        // merge CIs for the 2 sets of own addrs
        Contact selfContact = getContactForSelf();
        Contact otherSelfContact = other.getContactForSelf();

        if (selfContact == null)
            selfContact = otherSelfContact;
        else {
            // merge the CIs if needed right here, because they may not merge
            // later if they don't have a common name or email addr
            if (otherSelfContact != null) {
                log.debug("Merging own contacts: " + selfContact + " -- and -- " + otherSelfContact);
                selfContact.unify(otherSelfContact);
            }
        }

        return selfContact;
    }

    /**
     * unifi CIs and recompute nameToInfo and emailToInfo
     */
    private synchronized void recomputeUnifiedContacts(Set<Contact> allContacts) {
        // first set up representative CI -> List of CI's that map to that rep
        Map<Contact, Set<Contact>> reps = new LinkedHashMap<Contact, Set<Contact>>();

        for (Contact c : allContacts) {
            Contact rep = (Contact) c.find();
            Set<Contact> list = reps.get(rep);
            if (list == null) {
                list = new LinkedHashSet<Contact>();
                reps.put(rep, list);
            }
            list.add(c);
        }

        // resultCIs will contain all the unique clusters
        Set<Contact> resultContacts = new LinkedHashSet<Contact>();
        for (Contact rep : reps.keySet()) {
            // merge members of each cluster
            Contact mergedContact = null; // result of merge of each cluster
            Set<Contact> cluster = reps.get(rep); // one equiv. class
            for (Contact ci : cluster) {
                if (mergedContact == null)
                    mergedContact = ci;
                else {
                    if (AddressBook.log.isDebugEnabled())
                        AddressBook.log.debug("Merging \n" + mergedContact + "\n ------- with ------- \n" + ci + "\n -------- due to rep -------- \n" + rep);

                    mergedContact.merge(ci);
                }
            }
            resultContacts.add(mergedContact);
        }

        // now recompute the emailToInfo and nameToInfo maps
        emailToContact.clear();
        nameToContact.clear();
        nameTokenToContacts = new LinkedHashMap<String, Set<Contact>>();
        for (Contact c : resultContacts) {
            // create new versions of c.emails and names here, otherwise can get concurrent mod exception
            for (String s : new ArrayList<String>(c.emails))
                addEmailAddressForContact(s, c);
            for (String s : new ArrayList<String>(c.names))
                addNameForContactAndUpdateMaps(s, c);
        }

        reassignContactIds();
    }

    /**
     * convert given list of email addrs to their canonical form
     */
    public List<String> convertToCanonicalAddrs(List<String> addrs) {
        List<String> result = new ArrayList<String>();
        for (String s : addrs)
            result.add(getCanonicalAddr(s));
        return result;
    }

    /**
     * recomputes contacts merging unified ones. Warning: must be done before using the address book, otherwise, contacts will remain un-unified!
     */
    public synchronized void organizeContacts() {
        Set<Contact> allContacts = new LinkedHashSet<Contact>();
        allContacts.addAll(emailToContact.values());
        allContacts.addAll(nameToContact.values());
        recomputeUnifiedContacts(allContacts);
    }

    public List<Contact> allContacts() {
        // get all ci's into a set first to eliminate dups
        Set<Contact> uniqueContacts = new LinkedHashSet<Contact>();
        uniqueContacts.addAll(emailToContact.values());
        uniqueContacts.addAll(nameToContact.values());

        // now put them in a list and sort
        List<Contact> uniqueContactsList = new ArrayList<Contact>();
        uniqueContactsList.addAll(uniqueContacts);

        return uniqueContactsList;
    }

    /**
     * returns a list of all contacts in the given collection of docs, sorted by outgoing freq.
     */
    public List<Contact> sortedContacts(Collection<EmailDocument> docs) {
        Map<Contact, Integer> contactInCount = new LinkedHashMap<Contact, Integer>(), contactOutCount = new LinkedHashMap<Contact, Integer>();

        // note that we'll count a recipient twice if 2 different email addresses are present on the message.
        // we'll also count the recipient twice if he sends a message to himself
        for (EmailDocument ed : docs) {
            String senderEmail = ed.getFromEmailAddress();
            List<String> allEmails = ed.getAllAddrs();
            for (String email : allEmails) {
                Contact c = lookupByEmail(email);
                if (c != null) {
                    if (senderEmail.equals(email)) {
                        Integer I = contactOutCount.get(c);
                        contactOutCount.put(c, (I == null) ? 1 : I + 1);
                    } else {
                        Integer I = contactInCount.get(c);
                        contactInCount.put(c, (I == null) ? 1 : I + 1);
                    }
                }
            }
        }

        // sort by in count -- note that when processing sent email, in count is the # of messages sent by the owner of the archive to the person #confusing
        List<Pair<Contact, Integer>> pairs = Util.sortMapByValue(contactInCount);
        Set<Contact> sortedContactsSet = new LinkedHashSet<Contact>();
        for (Pair<Contact, Integer> p : pairs)
            sortedContactsSet.add(p.getFirst());
        // then by out count.
        pairs = Util.sortMapByValue(contactOutCount);
        for (Pair<Contact, Integer> p : pairs)
            sortedContactsSet.add(p.getFirst());

        for (Contact c : sortedContactsSet)
            if (getContactId(c) < 0)
                Util.warnIf(true, "Contact has -ve contact id: " + c);
        return new ArrayList<Contact>(sortedContactsSet);
    }

    /**
     * returns a list of all contacts in the given collection of docs, sorted by outgoing freq.
     */
    public List<Pair<Contact, Integer>> sortedContactsAndCounts(Collection<EmailDocument> docs) {
        Map<Contact, Integer> contactToCount = new LinkedHashMap<>();

        // note that we'll count a recipient twice if 2 different email addresses are present on the message.
        // we'll also count the recipient twice if he sends a message to himself
        for (EmailDocument ed : docs) {
            List<String> allEmails = ed.getAllAddrs();
            for (String email : allEmails) {
                Contact c = lookupByEmail(email);
                if (c != null) {
                    Integer I = contactToCount.get(c);
                    contactToCount.put(c, (I == null) ? 1 : I + 1);
                }
            }
        }

        List<Pair<Contact, Integer>> pairs = Util.sortMapByValue(contactToCount);
        return pairs;
    }

    /**
     * given an email addr, find a canonical email addr for that contact
     */
    public String getCanonicalAddr(String s) {
        String s1 = s;
        Contact c = lookupByEmail(s);
        // ci can be null
        if (c != null)
            s1 = c.getCanonicalEmail();
        else
            log.error("REAL WARNING: no contact info for email address: " + s);

        return s1;
    }


    /**
     * find all addrs from the given set of email addrs
     * useful for self addrs. user may have missed some
     */
    public String[] computeAllAddrsFor(String emailAddrs[]) {
        Set<String> allMyEmailAddrsSet = new LinkedHashSet<String>();
        for (String s : emailAddrs)
            allMyEmailAddrsSet.add(s);

        for (String s : emailAddrs) {
            Contact ci = lookupByEmail(s);
            if (ci == null)
                continue; // user may have given an address which doesn't actually exist in this set
            allMyEmailAddrsSet.addAll(ci.emails);
        }

        String[] result = new String[allMyEmailAddrsSet.size()];
        allMyEmailAddrsSet.toArray(result);
        // log.info(EmailUtils.emailAddrsToString(result));
        return result;
    }

    public AddressBookStats getStats() {
        Contact selfContact = getContactForSelf();
        Set<String> emails = (selfContact != null) ? selfContact.emails : new LinkedHashSet<String>();
        Set<String> names = (selfContact != null) ? selfContact.names : new LinkedHashSet<String>();

        AddressBookStats stats = new AddressBookStats();
        stats.nOwnEmails = emails.size();
        stats.nOwnNames = names.size();

        List<Contact> allContacts = allContacts();
        stats.nContacts = allContacts.size();

        stats.nNames = 0;
        stats.nEmailAddrs = 0;
        for (Contact ci : allContacts) {
            stats.nNames += ci.names.size();
            stats.nEmailAddrs += ci.emails.size();
        }

//		sb.append("STAT-own-email-name:\t");
//		for (String s: emails)
//		{
//			sb.append (s);
//			sb.append ("\t");
//		}
//		for (String s: names)
//		{
//			sb.append (s);
//			sb.append ("\t");
//		}
//		sb.append ("\n");
//		sb.append("STAT-folders:\t");
//		for (String s: folders)
//		{
//			sb.append (s);
//			sb.append ("\t");
//		}

        return stats;
    }


    public void resetCounts() {
        Set<Contact> allContacts = new LinkedHashSet<Contact>();
        // now add all other CIs
        allContacts.addAll(emailToContact.values());
        allContacts.addAll(nameToContact.values());
        log.debug("Resetting counts for " + allContacts.size() + " contacts");
        for (Contact ci : allContacts)
            ci.resetInfo();
        // similarities = null;
        mailingListMap.clear();
    }

    /**
     * merges one contact set with another. also recomputes unification classes etc.
     * warning doesn't merge inDates, and outDates etc.
     */
    public void merge(AddressBook other) {
        // TOFIX: mailing lists merging!
        Set<Contact> allContacts = new LinkedHashSet<Contact>();

        // first merge self addrs and find self CI
        Contact selfContact = mergeSelfAddrs(other);
        allContacts.add(selfContact);

        // now add all other CIs
        allContacts.addAll(emailToContact.values());
        allContacts.addAll(nameToContact.values());
        allContacts.addAll(other.emailToContact.values());
        allContacts.addAll(other.nameToContact.values());

        // unify CIs with the same name or email
        for (String email : emailToContact.keySet()) {
            email = email.toLowerCase();
            Contact otherContact = other.lookupByEmail(email);
            if (otherContact != null) {
                Contact thisContact = lookupByEmail(email);
                log.debug("AddressBook merge: merging contacts: " + thisContact + " -- and -- " + otherContact);
                thisContact.unify(otherContact);
            }
        }

        for (String name : nameToContact.keySet()) {
            Contact otherContact = other.lookupByName(name); // no need of normalizing because name is already normalized, coming from a keyset
            if (otherContact != null) {
                Contact thisContact = lookupByName(name);
                thisContact.unify(otherContact);
            }
        }

        recomputeUnifiedContacts(allContacts);
        dataErrors.addAll(other.dataErrors);
    }

    private void reassignContactIds() {
        Set<Contact> allContacts = new LinkedHashSet<Contact>();
        allContacts.addAll(emailToContact.values());
        allContacts.addAll(nameToContact.values());
        contactListForIds = new ArrayList<Contact>(allContacts);
        contactIdMap = new LinkedHashMap<Contact, Integer>();
        for (int i = 0; i < contactListForIds.size(); i++)
            contactIdMap.put(contactListForIds.get(i), i);
    }

    public int getContactId(Contact c) {
        if (c != null) {
            Integer id = contactIdMap.get(c);
            if (id != null) {
                Util.softAssert(c.equals(getContact(id)), "Inconsistent mapping to contact ID " + id);
                return id.intValue();
            }
        }
        return -1;
    }

    public Contact getContact(int id) {
        if (contactListForIds == null)
            return null;

        if (id >= 0 && id < contactListForIds.size())
            return contactListForIds.get(id);
        else
            return null;
    }

    /** masking stuff is used by epadd only, to hide full email addrs in discovery mode */
    private static <V> Map<String, V> maskEmailDomain(Map<String, V> map, Map<String, String> maskingMap, Map<String, Integer> duplicationCount) {
        Map<String, V> result = new LinkedHashMap<String, V>();
        for (Map.Entry<String, V> e : map.entrySet()) {
            String email = e.getKey();
            //log.info("masking " + email);
            String masked_email = maskingMap.get(email);
            if (masked_email == null) {
                // new email
                masked_email = Util.maskEmailDomain(email);
                if (duplicationCount.containsKey(masked_email)) {
                    // but masked email conflicts with another email
                    //log.debug("Duplicated masked email addr");
                    int count = duplicationCount.get(masked_email) + 1;
                    duplicationCount.put(masked_email, count);
                    masked_email = masked_email + "(" + count + ")";
                } else {
                    duplicationCount.put(masked_email, 0);
                }
            }

            V v = e.getValue();
            if (v instanceof Contact && !email.equals(masked_email)) // if "email" is not masked, it may actually not be an email. e.g., this routine can also be called on nameToContact.
                Util.ASSERT(((Contact) v).emails.contains(email));

            maskingMap.put(email, masked_email);
            result.put(masked_email, v);
        }

        return result;
    }

    public void maskEmailDomain() {
        emailMaskingMap = new LinkedHashMap<String, String>();
        Map<String, Integer> duplication_count = new LinkedHashMap<String, Integer>();
        emailToContact = maskEmailDomain(emailToContact, emailMaskingMap, duplication_count);

        // it seems email address may also appear "in" (not necessarily "as") the key of nameToContact.
        // therefore, we may be tempted to perform maskEmailDomain on nameToContact also.
        // but that can be misleading/wrong because an email address may appear "in" rather than "as" the key,
        // e.g., a name key can be <a@b.com> including the brackets (or can be "a@b.com" with the quotes).
        // this will unfortunately be treated as a different email address from a@b.com simply because they are different strings.
        // so, we can potentially have a@b.com masked as a@...(1) while <a@b.com> is masked as <a@...> (false homonym)
        // or we can have a@b.com masked as a@...(1) while <a@c.com> is also masked as <a@...(1)> (false synonym).
        // this will mislead users to think of one email address as different addresses or vice versa.
        // so, we should not mask nameToContact with maskEmailDomain here and use a different approach.
        //nameToContact = maskEmailDomain(nameToContact, emailMaskingMap, duplication_count);

        Set<Contact> allContacts = new LinkedHashSet<Contact>();
        allContacts.addAll(emailToContact.values());
        allContacts.addAll(nameToContact.values());

        for (Contact c : allContacts) {
            c.maskEmailDomain(this);
        }
    }

    public String getMaskedEmail(String address) {
        address = address.trim().toLowerCase();
        Util.ASSERT(emailMaskingMap != null);
        if (!emailMaskingMap.containsKey(address)) {
            log.warn(address + " had not been masked apriori");
            return Util.maskEmailDomain(address);
        }
        return emailMaskingMap.get(address);
    }

    private String getStatsAsString() {
        return getStatsAsString(true);
    } // blur by default

    // an older and slightly more elaborate version of get stats
    public String getStatsAsString(boolean blur) {
        // count how many with at least one sent
        List<Contact> list = allContacts();
        int nContacts = list.size();
        int nNames = 0, nEmailAddrs = 0;
        for (Contact ci : list) {
            nNames += ci.names.size();
            nEmailAddrs += ci.emails.size();
        }

        String result = list.size() + " people, "
                + nEmailAddrs + " email addrs, (" + String.format("%.1f", ((float) nEmailAddrs) / nContacts) + "/contact), "
                + nNames + " names, (" + String.format("%.1f", ((float) nNames) / nContacts) + "/contact)";

        if (contactForSelf != null) {
            result += " \n" + contactForSelf.emails.size() + " self emails: ";
            for (String x : contactForSelf.emails)
                result += (blur ? Util.blur(x) : x) + "|"; // "&bull; ";
            result += "\n" + contactForSelf.names.size() + " self names: ";
            for (String x : contactForSelf.names)
                result += (blur ? Util.blur(x) : x) + "|"; // " &bull; ";
        }

        return result;
    }

    public String toString() {
        return getStatsAsString();
    }

    public void verify() {
        Set<Contact> allContacts = new LinkedHashSet<Contact>();
        for (Set<Contact> s : nameTokenToContacts.values()) {
            allContacts.addAll(s);
        }

        // a CI in nameToInfo *must* be present in emailToInfo
        for (Contact c : nameToContact.values()) {
            Util.ASSERT(emailToContact.values().contains(c));
            Util.ASSERT(allContacts.contains(c));
        }

        // check that the email->CI and name-> CI maps are correct
        for (Map.Entry<String, Contact> me : emailToContact.entrySet()) {
            String email = me.getKey();
            Contact c = me.getValue();
            Util.ASSERT(c.emails.contains(email));
        }

        for (Map.Entry<String, Contact> me : nameToContact.entrySet()) {
            String name = me.getKey();
            Contact c = me.getValue();
            boolean found = false;
            for (String cname : c.names)
                if (EmailUtils.normalizePersonNameForLookup(cname).equals(name)) {
                    found = true;
                    break;
                }
            Util.ASSERT(found);
        }

        Set<Contact> allCs = new LinkedHashSet<Contact>();
        allCs.addAll(nameToContact.values());
        Util.ASSERT(allCs.size() == allContacts.size());
        allCs.addAll(emailToContact.values());
        for (Contact ci : allCs)
            ci.verify();
    }

    public JSONArray getCountsAsJson(Collection<EmailDocument> docs) {
        return getCountsAsJson(docs, false /* we don't want to exceptOwner */);
    }

    /**
     * used primarily by correspondents.jsp
     * // dumps the contacts in docs, and sorts according to sent/recd/mentions
     * // returns an array of (json array of 5 elements:[name, in, out, mentions, url])
     */
    public JSONArray getCountsAsJson(Collection<EmailDocument> docs, boolean exceptOwner) {
        Contact ownContact = getContactForSelf();
        List<Contact> allContacts = sortedContacts((Collection) docs);
        Map<Contact, Integer> contactInCount = new LinkedHashMap<Contact, Integer>(), contactOutCount = new LinkedHashMap<Contact, Integer>(), contactMentionCount = new LinkedHashMap<Contact, Integer>();

        // compute counts
        for (EmailDocument ed : docs) {
            String senderEmail = ed.getFromEmailAddress();
            Contact senderContact = this.lookupByEmail(senderEmail);
            if (senderContact == null)
                senderContact = ownContact; // should never happen, we should always have a sender contact

            int x = ed.sentOrReceived(this);
            // message could be both sent and received
            if ((x & EmailDocument.SENT_MASK) != 0) {
                // this is a sent email, each to/cc/bcc gets +1 outcount.
                // one of them could be own contact also.
                Collection<Contact> toContacts = ed.getToCCBCCContacts(this);
                for (Contact c : toContacts) {
                    Integer I = contactOutCount.get(c);
                    contactOutCount.put(c, (I == null) ? 1 : I + 1);
                }
            }

            boolean received = (x & EmailDocument.RECEIVED_MASK) != 0 // explicitly received
                    || (x & EmailDocument.SENT_MASK) == 0; // its not explicitly sent, so we must count it as received by default

            if (received) {
                // sender gets a +1 in count (could be ownContact also)
                // all others get a mention count.
                Integer I = contactInCount.get(senderContact);
                contactInCount.put(senderContact, (I == null) ? 1 : I + 1);
            }

            if ((x & EmailDocument.SENT_MASK) == 0) {
                // this message is not sent, its received.
                // add mentions for everyone who's not me, who's on the to/cc/bcc of this message.
                Collection<Contact> toContacts = ed.getToCCBCCContacts(this);
                for (Contact c : toContacts) {
                    if (c == ownContact)
                        continue; // doesn't seem to make sense to give a mention count for sender in addition to incount
                    Integer I = contactMentionCount.get(c);
                    contactMentionCount.put(c, (I == null) ? 1 : I + 1);
                }
            }
        }

        JSONArray resultArray = new JSONArray();

        int count = 0;
        for (Contact c : allContacts) {
            if (c == ownContact && exceptOwner)
                continue;

            //	out.println("<tr><td class=\"search\" title=\"" + c.toTooltip().replaceAll("\"", "").replaceAll("'", "") + "\">");
            int contactId = getContactId(c);
            //	out.println ("<a style=\"text-decoration:none;color:inherit;\" href=\"browse?contact=" + contactId + "\">");
            String bestNameForContact = c.pickBestName();
            String url = "browse?adv-search=1&contact=" + contactId;
            String nameToPrint = Util.escapeHTML(Util.ellipsize(bestNameForContact, 50));
            Integer inCount = contactInCount.get(c), outCount = contactOutCount.get(c), mentionCount = contactMentionCount.get(c);
            if (inCount == null)
                inCount = 0;
            if (outCount == null)
                outCount = 0;
            if (mentionCount == null)
                mentionCount = 0;

            JSONArray j = new JSONArray();
            j.put(0, Util.escapeHTML(nameToPrint));
            j.put(1, inCount);
            j.put(2, outCount);
            j.put(3, mentionCount);
            j.put(4, url);
            j.put(5, Util.escapeHTML(c.toTooltip()));
            resultArray.put(count++, j);
            // could consider putting another string which has more info about the contact such as all names and email addresses... this could be shown on hover
        }
        return resultArray;
    }


    public static class AddressBookStats implements Serializable {
        private static final long serialVersionUID = 1L;

        public int nOwnEmails, nOwnNames;
        // Calendar firstMessageDate, lastMessageDate;
        // int spanInMonths;
        public int nContacts;
        public int nNames = 0, nEmailAddrs = 0;

        public String toString() {
            // do not use html special chars here!

            String s = "own_email_address: " + nOwnEmails + " own_names: " + nOwnNames + "\n";
            s += "contacts: " + nContacts;
            return s;
        }
    }

    public static void main(String args[]) {
        List<String> list = EmailUtils.parsePossibleNamesFromEmailAddress("mickey.mouse@disney.com");
        System.out.println(Util.join(list, " "));
        list = EmailUtils.parsePossibleNamesFromEmailAddress("donald_duck@disney.com");
        System.out.println(Util.join(list, " "));
        list = EmailUtils.parsePossibleNamesFromEmailAddress("70451.2444@compuserve.com");
        System.out.println(Util.join(list, " "));

        String ownerName = "Owner Name";
        String ownerEmail = "owner@example.com";
        {
            AddressBook ab = new AddressBook(new String[]{ownerEmail}, new String[]{ownerName});
            EmailDocument ed = new EmailDocument();
            try {
                ed.to = new Address[]{new InternetAddress("from@email.com", "From Last")};
                ed.cc = new Address[]{new InternetAddress("cc@email.com", "CC Last")};
                ed.to = new Address[]{new InternetAddress("to@example.com", "To Last")};
                ed.from = new Address[]{new InternetAddress("from@example.com", "From Last")};
            } catch (Exception e) {
                Util.print_exception(e, log);
            }
            ab.processContactsFromMessage(ed);
            Util.ASSERT(ab.size() == 5); // 4 addresses should be added + owner
        }

        {
            AddressBook ab = new AddressBook(new String[]{ownerEmail}, new String[]{ownerName});
            EmailDocument ed1 = new EmailDocument(), ed2 = new EmailDocument();
            try {
                ed1.to = new Address[]{new InternetAddress("Merge Name", "mergename@example.com")};
                ed1.from = new Address[]{new InternetAddress("Merge Name2", "mergename@example.com")};

                ed2.to = new Address[]{new InternetAddress("Merge X Name", "mergeemail1@example.com")};
                ed2.from = new Address[]{new InternetAddress("Merge X Name", "mergeemail2@example.com")};
            } catch (Exception e) {
                ab.processContactsFromMessage(ed1);
                ab.processContactsFromMessage(ed2);
                Util.ASSERT(ab.size() == 3);
            }

            Util.ASSERT(ab.lookupByEmail("mergename@example.com").names.size() == 2); // 2 names for this email address
        }

    }
}
