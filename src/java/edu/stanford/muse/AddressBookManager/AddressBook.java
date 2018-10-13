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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
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
import java.io.*;
import java.text.SimpleDateFormat;
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
    public final static String CONTACT_START_PREFIX = "--";

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
    transient private Map<String, Contact> emailToContact = new LinkedHashMap<>(); // do not .put/get directly, use addEmailAddressForContact()
    transient private Multimap<String, Contact> nameToContact = LinkedHashMultimap.create(); // do not access directly, use addNameForContactAndUpdateMaps()
    //following can be crated from contactListForIds list hence no need to serialize them
    transient private Map<Contact, Integer> contactIdMap = new LinkedHashMap<>();

    private Contact contactForSelf;
    private Collection<String> dataErrors = new LinkedHashSet<>();
    //needed to keep track of mailingList information
    public Map<Contact, MailingList> mailingListMap = new LinkedHashMap<>();
    private List<Contact> contactListForIds = new ArrayList<>();

    /**
     * these are for providing a layer of opaqueness to contact emails in discovery mode
     */
    transient private Map<String, String> emailMaskingMap = null;


    /**
     * create a new contact set with the given list of self addrs. selfAddrs can be null or empty
     * in which case no addresses are considered selfAddrs
     */
    public AddressBook(String[] selfAddrs, String[] selfNames) {
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

    public AddressBook(Contact self){

        contactListForIds.add(self);
        contactForSelf = self;
    }
/*

    public AddressBook copyMutableFields(){
        //sufficient to do only for non-transient fields as it is used during export in delivery/discovery
        AddressBook ab = new AddressBook(this.contactForSelf.copy());
        for(Contact c: contactListForIds){
            if(!c.equals(contactForSelf)){
                ab.contactListForIds.add(c.copy());
            }
        }

        return ab;
    }
*/

    /**
     * initialize addressbook from lines of this format:
     * #person 1
     * email1.1
     * email1.2
     * name1.1
     * -- (separator token)
     * #person2 email2.1
     * name2.1
     * etc.
     */
    public void initialize(String text) {
        BufferedReader br = new BufferedReader(new StringReader(text));

        try {
            AddressBook ab = AddressBook.readObjectFromStream(br);
            //now assign all fields of this address book using ab.
            this.emailToContact = ab.emailToContact;
            this.nameToContact = ab.nameToContact;
            this.contactIdMap = ab.contactIdMap;
            this.contactForSelf = ab.contactForSelf;
            this.contactListForIds = ab.contactListForIds;
            this.mailingListMap = ab.mailingListMap;
            this.dataErrors = ab.dataErrors;
            this.emailMaskingMap = ab.emailMaskingMap;
        } catch (IOException e) {
            e.printStackTrace();
            Util.print_exception("Unable to initialize the addressbook with different contact information",e,log);
        }
        // todo: decide how to handle names are missing in text, but are associated with some email message in the archive.
        // when lookup is performed on such an archive, it may return null.
        // similar room for inconsistency when user can have the same name or email addr in multiple contacts
/*
        nameToContact.clear();
        emailToContact.clear();

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
        if (contactForSelf == null || contactForSelf.getEmails()== null || contactForSelf.getEmails().size() == 0)
            log.warn("Could not identify self in the starting lines: \n" + firstLines);
        else
            log.info("Initialised self contact: " + contactForSelf);
        reassignContactIds();
*/
    }


    private void addEmailAddressForContact(String email, Contact c) {
        if (Util.nullOrEmpty(email))
            return;

        email = EmailUtils.cleanEmailAddress(email);
        c.getEmails().add(email);
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
        c.getNames().add(name);

        // nameToContact is very important, so only add to it if we're fairly certain about the name.
        if (Util.tokenize(name).size() > 1)
            nameToContact.put(EmailUtils.normalizePersonNameForLookup(name), c);
        else {
            //consider the case when there is no space but special character like . as we found in an example. In that case one should separate that out
            StringTokenizer st = new StringTokenizer(name, ".", false);
            if (st.countTokens() > 1)
                nameToContact.put(EmailUtils.normalizePersonNameForLookup(name), c);

        }
    }

    /**
     * the Contacts for name and email are unified if they are not already the same
     * returns the contact for name/email (creates a new contact if needed)
     * name could be null (or empty, which is equivalent), email cannot be
     *
     */
    private synchronized Contact unifyContact(String email, String name) {
        // we'll implement a weaker pre-condition: both name and email could be null
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


           //two cases appear; 1. Found a contact by name but not by email   --- INV: There should be only one contact with that name..Add email to that contact

           //2. Found a contact by email but not by name -- add name to the contact that was found by name
        
           //3. No contact found either by email or by name               

           //Case 3- create a new contact

        Contact cEmail = lookupByEmail(email);
        Collection<Contact> cNames;





        if (name != null) {
            cNames = lookupByName(name);
            if(cNames!=null) {
                if(cNames.size()>1){
                    StringBuilder s = new StringBuilder("INFO:::When building the addressbook, name " + name + " mapped to the following contacts\n");
                    s.append("</br>");
                    for(Contact cName: cNames){
                        s.append(cName.toString());
                        s.append("</br>--------------</br>");
                    }
                    dataErrors.add(s.toString());
                }

                for (Contact cName : cNames) {
                    // if name and email have different CIs, unify them first
                    if (cName != null && cEmail != null && cName != cEmail) {
                        log.debug("Merging contacts due to message with name=" + name + ", email=" + email + " Contacts are: " + cName + " -- and -- " + cEmail);
                        cEmail.unify(cName);
                        addEmailAddressForContact(email, cName);
                        return cName;
                    } else if (cName != null) {
                        log.debug("Merging contacts due to message with name=" + name + " Contact is --" + cName);
                        addEmailAddressForContact(email, cName);
                        return cName;
                    }
                }
            }
        }
        if (cEmail != null) {
            if (name != null)
                addNameForContactAndUpdateMaps(name, cEmail);
            return cEmail;
        }

        // neither cEmail nor cName was found. Case 3 above- create a new contact.
      /*  if(name!=null && EmailUtils.normalizePersonNameForLookup("Gayle B.Harrell").equals( EmailUtils.normalizePersonNameForLookup(name))){
            log.debug("found");
        }
        if(email.compareTo("harrell.gayle@leg.state.fl.us")==0){
            log.debug("foudn case");
        }
*/        Contact c = new Contact(); // this blank Contact will be set up on the fly later
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
    private int size() {
        return allContacts().size();
    }

    /**
     * returns an unmodifiable set of the owner's email addresses
     */
    public Set<String> getOwnAddrs() {
        if (contactForSelf != null && contactForSelf.getEmails()!= null)
            return Collections.unmodifiableSet(contactForSelf.getEmails());

        return new LinkedHashSet<>();
    }

    /**
     * returns an unmodifiable set of the owner's names
     */
    public Set<String> getOwnNamesSet() {
        // just trim because some names seem to have spaces remaining at the end
        Set<String> result = new LinkedHashSet<>();
        for (String s : contactForSelf.getNames())
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
        if (c == null || c.getNames()== null || Util.nullOrEmpty(c.getNames()))
            return "";

        return c.getNames().iterator().next(); // pick first name for self (not best name! because the best name tries to find the longest string etc.
        // Here for the own name, we want to stay with whatever was provided when the archive/addressbook was
        // created.)
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
        if (c.getNames()!= null && c.getNames().size() > 0)
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
        return selfContact != null && selfContact.getEmails().contains(email);

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
     * returns the contact object for the given name. name will be normalized for lookup first.
     * If multi word name, looks up in multi word map.
     * If single word name, looks up in the nameTokenToContacts map and returns the first one in that map (since there can be several)
     */
    public Collection<Contact> lookupByName(String s) {
        if (s == null)
            return Collections.EMPTY_LIST;
        String normalizedName = EmailUtils.normalizePersonNameForLookup(s);
        if (normalizedName == null || normalizedName.equals(""))
            return Collections.EMPTY_LIST;

        return nameToContact.get(normalizedName);
    }

    public Collection<Contact> lookupByEmailOrName(String s) {
        if (s == null)
            return Collections.EMPTY_LIST;

        s = s.trim().toLowerCase();
        Contact c = lookupByEmail(s);
        if (c != null) {
            Set<Contact> result = new LinkedHashSet<>();
            result.add(c);
            return result;
        }

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
        name = Util.unescapeHTML(name);
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
            if (!c.getEmails().contains(email)) {
                if (log.isDebugEnabled())
                    log.debug("merging email " + email + " into contact " + c);
                c.getEmails().add(email);
            }

            if (!Util.nullOrEmpty(name) && !c.getNames().contains(name)) {
                if (log.isDebugEnabled() && !c.getNames().contains(name))
                    log.debug("merging name " + name + " into contact " + c);
                c.getNames().add(name);
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
    public synchronized void processContactsFromMessage(EmailDocument ed, Collection<String> trustedAddrs) {
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

        boolean fromTrustedAddr = false;
        {
            Address[] addrs = ed.from;
            if (ed.from != null) {
                for (Address a : addrs) {
                    InternetAddress ia = (InternetAddress) a;
                    if (trustedAddrs.contains(ia.getAddress().toLowerCase())) {
                        fromTrustedAddr = true;
                        break;
                    }
                }
            }
        }
        boolean b = false;

        if (fromTrustedAddr) {
            b = processContacts(ed.getToCCBCC(), ed.from, ed.sentToMailingLists);
            log.info ("Processing trusted contacts from " + ((ed.from != null && ed.from.length > 0) ? ed.from[0] : ""));
        } else
            log.warn ("Dropping processing of contacts from non-trusted addr" + ((ed.from != null && ed.from.length > 0) ? ed.from[0] : ""));

        if (!b && !noToCCBCC) // if we already reported no to address problem, no point reporting this error, it causes needless duplication of error messages.
            markDataError("Owner not sender, and no to addr for: " + ed);
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

        if (fromAddrs != null) {
            if (fromAddrs.length > 1) {
                log.warn("Alert!: froms.length > 1: " + fromAddrs.length);
                for (Address from : fromAddrs)
                    log.warn(from);
            }

            for (Address from : fromAddrs) {
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

        return new Pair<>(sent, received);
    }

   /* *//**
     * how many of the messages in the given collection are outgoing?
     *//* This was the wrong implementation hence commenting out so that no one uses it in future. The correct logic is written in
      * the saveArchive method of SimplesSession class where incoming and outgoing message fields of collection metadata is set
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
    }*/

    public synchronized Collection<String> getDataErrors() {
        if (dataErrors == null)
            dataErrors = new LinkedHashSet<>();
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
     * unify contacts and recompute nameToContact and emailToContact, also sets up contact ids
     */
    private synchronized void recomputeUnifiedContacts(Set<Contact> allContacts) {

        // first set up representative contact -> List of contact that map to that rep (which means they are in the same eq class)
        Map<Contact, Set<Contact>> reps = new LinkedHashMap<>();

        for (Contact c : allContacts) {
            Contact rep = (Contact) c.find();
            Set<Contact> list = reps.computeIfAbsent(rep, k -> new LinkedHashSet<>());
            list.add(c);
        }

        // resultContacts will contain all the unique clusters
        Set<Contact> resultContacts = new LinkedHashSet<>();
        for (Contact rep : reps.keySet()) {
            // merge members of each cluster into a single contact called mergeContaact
            Contact mergedContact = null;
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

        this.contactListForIds.clear();
        this.contactListForIds.addAll(resultContacts);
        fillTransientFields();
       /* // now recompute the emailToInfo and nameToInfo maps
        emailToContact.clear();
        nameToContact.clear();
        for (Contact c : resultContacts) {
            // create new versions of c.emails and names here, otherwise can get concurrent mod exception
            for (String s : new ArrayList<>(c.getEmails()))
                addEmailAddressForContact(s, c);
            for (String s : new ArrayList<>(c.getNames()))
                addNameForContactAndUpdateMaps(s, c);
        }

        reassignContactIds();*/
    }


    /**
     * recomputes contacts merging unified ones. Warning: must be done before using the address book, otherwise, contacts will remain un-unified!
     */
    public synchronized void organizeContacts() {
        Set<Contact> allContacts = new LinkedHashSet<>();
        allContacts.addAll(emailToContact.values());
        allContacts.addAll(nameToContact.values());
        recomputeUnifiedContacts(allContacts);
    }

    public List<Contact> allContacts() {
        // get all ci's into a set first to eliminate dups
        Set<Contact> uniqueContacts = new LinkedHashSet<>();
        uniqueContacts.addAll(emailToContact.values());
        uniqueContacts.addAll(nameToContact.values());

        // now put them in a list and sort
        List<Contact> uniqueContactsList = new ArrayList<>();
        uniqueContactsList.addAll(uniqueContacts);

        return uniqueContactsList;
    }

    /**
     * returns a list of all contacts in the given collection of docs, sorted by outgoing freq.
     */
    public List<Contact> sortedContacts(Collection<EmailDocument> docs) {
        Map<Contact, Integer> contactInCount = new LinkedHashMap<>(), contactOutCount = new LinkedHashMap<>();

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
        Set<Contact> sortedContactsSet = new LinkedHashSet<>();
        for (Pair<Contact, Integer> p : pairs)
            sortedContactsSet.add(p.getFirst());
        // then by out count.
        pairs = Util.sortMapByValue(contactOutCount);
        for (Pair<Contact, Integer> p : pairs)
            sortedContactsSet.add(p.getFirst());

        for (Contact c : sortedContactsSet)
            if (getContactId(c) < 0)
                Util.warnIf(true, "Contact has -ve contact id: " + c,log);
        return new ArrayList<>(sortedContactsSet);
    }

    /**
     * returns a list of all contacts in the given collection of docs, sorted by outgoing freq.
     */
    List<Pair<Contact, Integer>> sortedContactsAndCounts(Collection<EmailDocument> docs) {
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

        return Util.sortMapByValue(contactToCount);
    }

    /**
     * find all addrs from the given set of email addrs
     * useful for self addrs. user may have missed some
     */
    public String[] computeAllAddrsFor(String emailAddrs[]) {
        Set<String> allMyEmailAddrsSet = new LinkedHashSet<>();
        Collections.addAll(allMyEmailAddrsSet, emailAddrs);

        for (String s : emailAddrs) {
            Contact ci = lookupByEmail(s);
            if (ci == null)
                continue; // user may have given an address which doesn't actually exist in this set
            allMyEmailAddrsSet.addAll(ci.getEmails());
        }

        String[] result = new String[allMyEmailAddrsSet.size()];
        allMyEmailAddrsSet.toArray(result);
        // log.info(EmailUtils.emailAddrsToString(result));
        return result;
    }

    public AddressBookStats getStats() {
        Contact selfContact = getContactForSelf();
        Set<String> emails = (selfContact != null) ? selfContact.getEmails() : new LinkedHashSet<>();
        Set<String> names = (selfContact != null) ? selfContact.getNames(): new LinkedHashSet<>();

        AddressBookStats stats = new AddressBookStats();
        stats.nOwnEmails = emails.size();
        stats.nOwnNames = names.size();

        List<Contact> allContacts = allContacts();
        stats.nContacts = allContacts.size();

        stats.nNames = 0;
        stats.nEmailAddrs = 0;
        for (Contact ci : allContacts) {
            stats.nNames += ci.getNames().size();
            stats.nEmailAddrs += ci.getEmails().size();
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
        Set<Contact> allContacts = new LinkedHashSet<>();
        // now add all other CIs
        allContacts.addAll(emailToContact.values());
        allContacts.addAll(nameToContact.values());
        log.debug("Resetting counts for " + allContacts.size() + " contacts");
        for (Contact ci : allContacts)
            ci.resetInfo();
        // similarities = null;
        mailingListMap.clear();
    }

    public class MergeResult{
        /*mergedContacts map is used to track how a contact in A1 expanded after merging.
         A1: [a b c d e]
         A2: [a b x] [c f]
        Here after merging we will have [abcdefx] and this map will contain [abcdefx]->[abcde] so that
        we can infer that f and x were added. Note that by the condition of type1 there will be a unique
        contact in A2 from where these added names/emails must have come [unique for f and unique for x
        separately]. From the end user point of view
        he has to decide if f and x should be part of this merged contact or not. Therefore this information is
        sufficient for him.
        */
        public Map<Contact,Contact> mergedContacts = new LinkedHashMap<>();

        /* newlycreatedContacts is used to track the split of a contact in A2. The key of this map
        is the contact of A2 after split and the values are [multimap] the set of contacts in A2 from where
        the names/emails of this contact were created.
        Example: A1:[xy], A2:[ax][ay] here a new contact [a] will be created which will have mapping to
         [ax] and [ay] separately. For end user we would list all those contacts where a,x,y are present in
         merged addressbook).
         */
        public SetMultimap<Contact,Contact> newlycreatedContacts = LinkedHashMultimap.create();
    }
/*
Merge algorithm:

Input: A1 - First Address book, A2- Second address book
Assumption: A1 is trusted
common = names(A1) ^ names(A2)
for each C2 in A2:{
Let SC1 = U_e\in [common ^(names(C2)] lookup(e,A1)
if |SC1| = 1 && |U_e\in [names(C2)-common] lookup(e,A2)| = 1
{
//Type 1; --- There exists a unique C1 in A1 such that for all x that are common between A1 and A2,
// x is only in C1.-- We can unambiguously merge C2 to C1.
//Example that will not be covered here.. A1:[a b c d] A2:[a b x], [b c y]. Here b appears in two contacts of A2
//so we still have doubt about it. This is an example of Type 2: Here the merged AB will be A:[a b c d],[x], [y]-- With [x] we will list all
//contacts (merged) where a b or x appear. With [y] we will list all contacts where b c or y appear.
Let C1\in SC1;
Let savedC1 = C1.clone()
C1=C1 U C2
//because no merged name will appear again in A2
mergeResult.mergedContacts.put(C1,savedC1)
//There was only one contact in A2 which caused expansion of C1.
}else{
Let savedC2 = C2.clone()
C2 = C2 - common
add C2 (if not empty) in the merged address book.
mergeResult.newContacts.put(C2,savedC2)
}
 */
    public MergeResult merge(AddressBook other){
        Set<String> elementsInCollection = Util.setUnion(nameToContact.keySet(),emailToContact.keySet());
        Set<String> elementsInAccession =  Util.setUnion(other.nameToContact.keySet(),other.emailToContact.keySet());
        Set<String> commonelements = Util.setIntersection(elementsInCollection,elementsInAccession);
        MergeResult result = new MergeResult();//for filling in the results and displaying as a report.
        ///Folllowing two variables are used for bookkeeping so that this addressbook doesn't get modified
        //while being in this loop.
        Multimap<Contact,Contact> mergeContacts = LinkedHashMultimap.create();
        Set<Contact> newContact = new LinkedHashSet<>();

        for(Contact C2: other.allContacts()){
            Set<String> elementsC2 = Util.setUnion(C2.getEmails(),C2.getNames());
            Set<String> commonC2 = Util.setIntersection(commonelements,elementsC2);
            Set<Contact> presentInCollection = new LinkedHashSet<>();
            for(String element: commonC2){
                if(lookupByName(element)!=null)
                    presentInCollection.addAll(lookupByName(element));
                if(lookupByEmail(element)!=null)
                    presentInCollection.add(lookupByEmail(element));
            }
            Set<String> C2OnlyInA2 = new LinkedHashSet<>(elementsC2);
            C2OnlyInA2.removeAll(commonC2);
            Set<Contact> presentInAccession = new LinkedHashSet<>();
            for(String element: C2OnlyInA2){
                if(other.lookupByName(element)!=null)
                    presentInAccession.addAll(other.lookupByName(element));
                if(other.lookupByEmail(element)!=null)
                    presentInAccession.add(other.lookupByEmail(element));
            }

            if(presentInCollection.size()==1 && presentInAccession.size()==1){
                //Type 1 above:
                Contact C1 = presentInCollection.iterator().next();
                mergeContacts.put(C1,C2);
                //once the iteration over all contacts of A2 is over then we will merge them so as to avoid
                //the change in the behaviour of lookup methods.
            }else{
                //Type 2 above:
                newContact.add(C2);
            }
        }
        /////////////Now process newContact and mergedContact data structure to actually update this address
        /////////book. In this process also fill the mergeResult object.
        for(Contact C1: mergeContacts.keySet()){
            Contact copyC1 = C1.copy();
            Set<Contact> C2 = new LinkedHashSet<Contact>(mergeContacts.get(C1));
            C2.forEach(contact->contact.getEmails().forEach(email-> addEmailAddressForContact(email,C1)));
            C2.forEach(contact->contact.getNames().forEach(name->addNameForContactAndUpdateMaps(name,C1)));
            //put C1 and savedC1 in mergedResult
            result.mergedContacts.put(C1,copyC1);
        }
        //For Tpye 2 processing
        for(Contact C2: newContact){
            Contact copyC2 =C2.copy();
            //C2 = C2 - common
            C2.getNames().removeAll(commonelements);
            C2.getEmails().removeAll(commonelements);
            //add C2 to this addressbook only if it is non-empty.
            if(!(C2.getNames().size()==0 && C2.getEmails().size()==0)) {
                contactListForIds.add(C2);

                //put C2 and savedC2 in mergedResult's multimap. Why multimap?
                //consider common = [x,y] and in A2: [a x] and [a y] then this is a case of type2 [because a appears
                //in 2 different contacts of A2.] here we will have map of [a] -> [a x] and [a] -> [a y] to show
                //that these are possible candidates where a can go.
                result.newlycreatedContacts.put(C2, copyC2);
            }
        }

        //call fillTransientfields to update transient maps
        fillTransientFields();
        return result;
    }
    /**
     * merges one contact set with another. also recomputes unification classes etc.
     * warning doesn't merge inDates, and outDates etc.
     * TODO: fix this for epadd v5
     */
    public void mergeOld(AddressBook other) {
        // TOFIX: mailing lists merging!
        Set<Contact> allContacts = new LinkedHashSet<>();

        // first merge self addrs and find self contact
        Contact selfContact = mergeSelfAddrs(other);
        allContacts.add(selfContact);

        // now add all contacts in either address book
        allContacts.addAll(emailToContact.values());
        allContacts.addAll(nameToContact.values());

        allContacts.addAll(other.emailToContact.values());
        allContacts.addAll(other.nameToContact.values());

        // unify contacts with the same name or email
        for (String email : emailToContact.keySet()) {
            email = email.toLowerCase();
            Contact otherContact = other.lookupByEmail(email);
            if (otherContact != null) {
                Contact thisContact = lookupByEmail(email);
                log.debug("AddressBook merge: merging contacts: " + thisContact + " -- and -- " + otherContact);
                thisContact.unify(otherContact);
            }
        }

        /*
        for (Collection<Contact> names : nameToContact.keySet()) {
            Contact otherContact = other.lookupByName(name); // no need of normalizing because name is already normalized, coming from a keyset
            if (otherContact != null) {
                Contact thisContact = lookupByName(name);
                thisContact.unify(otherContact);
            }
        }
        */

        recomputeUnifiedContacts(allContacts);
        dataErrors.addAll(other.dataErrors);
    }

    private void reassignContactIds() {
        Set<Contact> allContacts = new LinkedHashSet<>();
        allContacts.addAll(emailToContact.values());
        allContacts.addAll(nameToContact.values());
        contactListForIds = new ArrayList<>(allContacts);
        contactIdMap = new LinkedHashMap<>();
        for (int i = 0; i < contactListForIds.size(); i++)
            contactIdMap.put(contactListForIds.get(i), i);
    }

    public int getContactId(Contact c) {
        if (c != null) {
            Integer id = contactIdMap.get(c);
            if (id != null) {
                Util.softAssert(c.equals(getContact(id)), "Inconsistent mapping to contact ID " + id,log);
                return id;
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
        Map<String, V> result = new LinkedHashMap<>();
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
                Util.ASSERT(((Contact) v).getEmails().contains(email));

            maskingMap.put(email, masked_email);
            result.put(masked_email, v);
        }

        return result;
    }

    public void maskEmailDomain() {
        emailMaskingMap = new LinkedHashMap<>();
        Map<String, Integer> duplication_count = new LinkedHashMap<>();
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

        Set<Contact> allContacts = new LinkedHashSet<>();
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
    private String getStatsAsString(boolean blur) {
        // count how many with at least one sent
        List<Contact> list = allContacts();
        int nContacts = list.size();
        int nNames = 0, nEmailAddrs = 0;
        for (Contact ci : list) {
            nNames += ci.getNames().size();
            nEmailAddrs += ci.getEmails().size();
        }

        String result = list.size() + " people, "
                + nEmailAddrs + " email addrs, (" + String.format("%.1f", ((float) nEmailAddrs) / nContacts) + "/contact), "
                + nNames + " names, (" + String.format("%.1f", ((float) nNames) / nContacts) + "/contact)";

        if (contactForSelf != null) {
            result += " \n" + contactForSelf.getEmails().size() + " self emails: ";
            for (String x : contactForSelf.getEmails())
                result += (blur ? Util.blur(x) : x) + "|"; // "&bull; ";
            result += "\n" + contactForSelf.getNames().size() + " self names: ";
            for (String x : contactForSelf.getNames())
                result += (blur ? Util.blur(x) : x) + "|"; // " &bull; ";
        }

        return result;
    }

    public String toString() {
        return getStatsAsString();
    }

    public void verify() {
        Set<Contact> allContacts = new LinkedHashSet<>();

        // a CI in nameToInfo *must* be present in emailToInfo
        for (Contact c : nameToContact.values()) {
            Util.ASSERT(emailToContact.values().contains(c));
            Util.ASSERT(allContacts.contains(c));
        }

        // check that the email->CI and name-> CI maps are correct
        for (Map.Entry<String, Contact> me : emailToContact.entrySet()) {
            String email = me.getKey();
            Contact c = me.getValue();
            Util.ASSERT(c.getEmails().contains(email));
        }

        // verify nameToContacts
        /*
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
        */

        Set<Contact> allCs = new LinkedHashSet<>();
        allCs.addAll(nameToContact.values());
        Util.ASSERT(allCs.size() == allContacts.size());
        allCs.addAll(emailToContact.values());
        for (Contact ci : allCs)
            ci.verify();
    }

  /*  public JSONArray getCountsAsJson(Collection<EmailDocument> docs, String archiveID) {
        return getCountsAsJson(docs, false *//* we don't want to exceptOwner *//*,archiveID);
    }
*/
    /**
     * used primarily by correspondents.jsp
     * // dumps the contacts in docs, and sorts according to sent/recd/mentions
     * // returns an array of (json array of 7 elements:[name, in, out, mentions, url, start-date,end-date])
     *---v6- Modified this method to return 2 more elements (startdate-enddate) so that this method can be used to export confirmed correspondent
     * in a csv format.
     */

    public static JSONArray getCountsAsJson(Collection<EmailDocument> docs, boolean exceptOwner, String archiveID) {
        JSONArray cached = Archive.cacheManager.getCorrespondentListing(archiveID);
        if(cached!=null)
            return cached;
        Archive archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        AddressBook ab = archive.getAddressBook();
        Contact ownContact = ab.getContactForSelf();
        List<Contact> allContacts = ab.sortedContacts((Collection) docs);
        Map<Contact, Set<EmailDocument>> contactInDocs = new LinkedHashMap<>(), contactOutDocs = new LinkedHashMap<>(), contactMentionDocs= new LinkedHashMap<>();

        // compute counts
        for (EmailDocument ed : docs) {
            String senderEmail = ed.getFromEmailAddress();
            Contact senderContact = ab.lookupByEmail(senderEmail);
            if (senderContact == null)
                senderContact = ownContact; // should never happen, we should always have a sender contact

            int x = ed.sentOrReceived(ab);
            // message could be both sent and received
            if ((x & EmailDocument.SENT_MASK) != 0) {
                // this is a sent email, each to/cc/bcc gets +1 outcount.
                // one of them could be own contact also.
                Collection<Contact> toContacts = ed.getToCCBCCContacts(ab);
                for (Contact c : toContacts) {
                    Set<EmailDocument> tmp = contactOutDocs.get(c);
                    if(tmp==null)
                        tmp=new LinkedHashSet<>();
                    tmp.add(ed);
                    contactOutDocs.put(c, tmp);
                }
            }

            boolean received = (x & EmailDocument.RECEIVED_MASK) != 0 // explicitly received
                    || (x & EmailDocument.SENT_MASK) == 0; // its not explicitly sent, so we must count it as received by default

            if (received) {
                // sender gets a +1 in count (could be ownContact also)
                // all others get a mention count.
                Set<EmailDocument> tmp = contactInDocs.get(senderContact);
                if(tmp==null)
                    tmp=new LinkedHashSet<>();
                tmp.add(ed);
                contactInDocs.put(senderContact, tmp);

            }

            if ((x & EmailDocument.SENT_MASK) == 0) {
                // this message is not sent, its received.
                // add mentions for everyone who's not me, who's on the to/cc/bcc of this message.
                Collection<Contact> toContacts = ed.getToCCBCCContacts(ab);
                for (Contact c : toContacts) {
                    if (c == ownContact)
                        continue; // doesn't seem to make sense to give a mention count for sender in addition to incount
                    Set<EmailDocument> tmp = contactMentionDocs.get(c);
                    if(tmp==null)
                        tmp=new LinkedHashSet<>();
                    tmp.add(ed);
                    contactMentionDocs.put(c, tmp);
                }
            }
        }

        JSONArray resultArray = new JSONArray();

        int count = 0;
        for (Contact c : allContacts) {
            if (c == ownContact && exceptOwner)
                continue;

            //	out.println("<tr><td class=\"search\" title=\"" + c.toTooltip().replaceAll("\"", "").replaceAll("'", "") + "\">");
            int contactId = ab.getContactId(c);
            //	out.println ("<a style=\"text-decoration:none;color:inherit;\" href=\"browse?contact=" + contactId + "\">");
            String bestNameForContact = c.pickBestName();
            String url = "browse?adv-search=1&contact=" + contactId+"&archiveID="+archiveID;
            String nameToPrint = Util.escapeHTML(Util.ellipsize(bestNameForContact, 50));
            Integer inCount = contactInDocs.getOrDefault(c,new LinkedHashSet<>()).size(), outCount = contactOutDocs.getOrDefault(c,new LinkedHashSet<>()).size(), mentionCount = contactMentionDocs.getOrDefault(c,new LinkedHashSet<>()).size();
            /*if (inCount == null)
                inCount = 0;
            if (outCount == null)
                outCount = 0;
            if (mentionCount == null)
                mentionCount = 0;*/
            Set<EmailDocument> alldocs = Util.setUnion(contactInDocs.getOrDefault(c,new LinkedHashSet<>()),contactOutDocs.getOrDefault(c,new LinkedHashSet<>()));
            alldocs=Util.setUnion(alldocs,contactMentionDocs.getOrDefault(c,new LinkedHashSet<>()));
            Pair<Date,Date> range = EmailUtils.getFirstLast(alldocs,true);

            JSONArray j = new JSONArray();
            j.put(0, Util.escapeHTML(nameToPrint));
            j.put(1, inCount);
            j.put(2, outCount);
            j.put(3, mentionCount);
            j.put(4, url);
            j.put(5, Util.escapeHTML(c.toTooltip()));
            if(range.first!=null)
                j.put(6,new SimpleDateFormat("MM/dd/yyyy").format(range.first));
            else
                j.put(6,range.first);
            if(range.second!=null)
                j.put(7,new SimpleDateFormat("MM/dd/yyyy").format(range.second));
            else
                j.put(7,range.second);

            resultArray.put(count++, j);
            // could consider putting another string which has more info about the contact such as all names and email addresses... this could be shown on hover
        }
        //store in cache manager.
        Archive.cacheManager.cacheCorrespondentListing(archiveID,resultArray);
        return resultArray;
    }

//    static class AddressBookStats implements Serializable {
//        private static final long serialVersionUID = 1L;
//No need to keep AddressBookStats class serializable as it's instance is not present anywhere.
        static class AddressBookStats {

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

    private void fillTransientFields(){
        contactIdMap.clear();
        nameToContact.clear();
        emailToContact.clear();

        for(int i =0; i<contactListForIds.size();i++){
            Contact c = contactListForIds.get(i);
            contactIdMap.put(c,i);
            for(String name: c.getNames()){
                nameToContact.put(EmailUtils.normalizePersonNameForLookup(name),c);
            }
            for(String email: c.getEmails()){
                emailToContact.put(email,c);
            }
        }


    }

    ///////////////////////////Code for writing and reading address book in Human readable format///////
    /*
    MailingList class and data errors seems to be an issue. In this version we will only write addressbook
    as a list of contacts with the first contact as self contact. After reading we fillTransientFields
    as done in readObjectFromFile method above.
    --Here the format is
    contact object (first one is alwasy self)
    delimiter (##################)
    Series of contact objects separated by delimiter(########################)
     */
    public void
    writeObjectToStream(BufferedWriter out, boolean alphaSort) throws IOException {

        // always toString first contact as self
        Contact self = this.getContactForSelf();
        if (self != null)
            self.writeObjectToStream(out,"Archive owner");

        if (!alphaSort)
        {
            List<Contact> contacts = this.contactListForIds;
            for (Contact c: contacts)
                if (c != self) {
                    c.writeObjectToStream(out, "");
                    //out.write(PERSON_DELIMITER);
            }
        }
        else
        {
            // build up a map of best name -> contact, sort it by best name and toString contacts in the resulting order
            List<Contact> allContacts = this.contactListForIds;
            Map<String, Contact> canonicalBestNameToContact = new LinkedHashMap<>();
            for (Contact c: allContacts)
            {
                if (c == self)
                    continue;
                String bestEmail = c.pickBestName();
                if (bestEmail == null)
                    continue;
                canonicalBestNameToContact.put(bestEmail.toLowerCase(), c);
            }

            List<Pair<String, Contact>> pairs = Util.mapToListOfPairs(canonicalBestNameToContact);
            Util.sortPairsByFirstElement(pairs);

            for (Pair<String, Contact> p: pairs)
            {
                Contact c = p.getSecond();
                if (c != self) {
                    //c.writeObjectToStream(out, c.pickBestName());
                    c.writeObjectToStream(out, "");
                    //out.print(dumpForContact(c, c.pickBestName()));

                }
            }
        }
    }

    /*
    This code is what was earlier initialize method of AddressBook. However, here we only read contacts
    and put them in the list contactListForIds. After that we need to call fillTransientFields method to
    fill transient variables.
     */
    public static AddressBook readObjectFromStream(BufferedReader in) throws IOException {
        Contact self = Contact.readObjectFromStream(in);
        if(self==null)
            return null;

        AddressBook ab = new AddressBook(self);

        Contact c = Contact.readObjectFromStream(in);
        while(c!=null){
            ab.contactListForIds.add(c);
            c = Contact.readObjectFromStream(in);
        }
        //fillTransientVariables
        ab.fillTransientFields();
        //unify-- may be not.. because the invariant "one name can not be in two contacts" might be broken after editing.
        //ab.organizeContacts();
        return ab;

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
            ab.processContactsFromMessage(ed, new LinkedHashSet<>());
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
                ab.processContactsFromMessage(ed1, new LinkedHashSet<>());
                ab.processContactsFromMessage(ed2, new LinkedHashSet<>());
                Util.ASSERT(ab.size() == 3);
            }

            Util.ASSERT(ab.lookupByEmail("mergename@example.com").getNames().size() == 2); // 2 names for this email address
        }

    }
}
