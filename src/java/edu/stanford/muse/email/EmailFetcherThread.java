/*
 * Copyright (C) 2012 The Stanford MobiSocial Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.stanford.muse.email;

import com.sun.mail.imap.IMAPFolder;
import edu.stanford.muse.Config;
import edu.stanford.muse.LabelManager.LabelManager;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.index.*;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.JSONUtils;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.HTMLUtils;
import groovy.lang.Tuple2;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.*;

class EmailFetcherStats implements Cloneable, Serializable {
    private final static long serialVersionUID = 1L;

    int nTotalMessages;            // total # of messages to process
    int nMessagesAdded;            // running total messages newly added to the archive
    int nMessagesAlreadyPresent;    // running messages that were already present
    int nErrors = 0;
    int nMessagesFiltered = 0;

    public void merge(EmailFetcherStats other) {
        this.nMessagesAdded += other.nMessagesAdded;
        this.nMessagesAlreadyPresent += other.nMessagesAlreadyPresent;
        this.nMessagesFiltered += other.nMessagesFiltered;
        this.nErrors += other.nErrors;
        this.nTotalMessages += other.nTotalMessages;
    }

    public String toString() {
        return Util.fieldsToString(this);
    }
}

/**
 * Important class for importing email.
 * implements an email fetcher for a range of message #s within a single folder.
 * In contrast, MTEmailFetcher is responsible for an entire email account, including multiple folders.
 * and MuseEmailFetcher is responsible for multiple accounts (but for a single user)
 * email fetcher stats is associated with a single email fetcher
 */
public class EmailFetcherThread implements Runnable, Serializable {
    private final static long serialVersionUID = 1L;

    private static final int IMAP_PREFETCH_BUFSIZE = 20 * 1024 * 1024;
    /* used for buffering imap prefetch data -- necessary for good imap performance*/
    private static final String FORCED_ENCODING = "UTF-8";

    public static final Logger log =  LogManager.getLogger(EmailFetcherThread.class);

    // set up INVALID_DATE
    public static final Date
            INVALID_DATE; // like 0xdeadbeef

    static {
        Calendar c = new GregorianCalendar();
        c.set(Calendar.YEAR, 1960);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        INVALID_DATE = c.getTime();
    }

    private FetchConfig fetchConfig;
    private boolean mayHaveRunOutOfMemory = false;
    private FolderInfo fetchedFolderInfo;
    private transient Folder folder;

    private int threadID;
    private EmailStore emailStore;

    private boolean isCancelled;

    public static boolean verbose = false;
    public static boolean debug = false;

    // notes: begin_msg_index is always correct. end_msg_index = -1  means nMessages in folder.
    // note: msg # begin_msg_index will be processed. msg # end_msg_index will not be processed.
    private int begin_msg_index = 0;
    private int end_msg_index = -1;

    final EmailFetcherStats stats = new EmailFetcherStats();
    String currentStatus;


    private int totalMessagesInFetch;
    private int messagesCompletedInFetch;                        // this fetcher may be part of a bigger fetch operation. we need to track the progress of the bigger fetch in order to track progress accurately.

    public int getTotalMessagesInFetch() {
        return totalMessagesInFetch;
    }

    public void setTotalMessagesInFetch(int totalMessagesInFetch) {
        this.totalMessagesInFetch = totalMessagesInFetch;
    }

    public int getMessagesCompletedInFetch() {
        return messagesCompletedInFetch;
    }

    public void setMessagesCompletedInFetch(int messagesCompletedInFetch) {
        this.messagesCompletedInFetch = messagesCompletedInFetch;
    }

    // stats
    private int nMessagesProcessedSuccess;
    private int nUncachedMessagesProcessed;
    int nMessagesCached; // running count of # of messages processed successfully
    private int nErrors = 0;

    public void cancel() {
        isCancelled = true;
    }

    public void setFetchConfig(FetchConfig fc) {
        this.fetchConfig = fc;
    }

    public int getThreadID() {
        return threadID;
    }

    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }

    public int getNMessagesProcessed() {
        return nMessagesProcessedSuccess;
    }

    public int getNUncachedMessagesProcessed() {
        return nUncachedMessagesProcessed;
    }

    private String folder_name() {
        return fetchedFolderInfo.longName;
    }

    private String email_source() {
        return fetchedFolderInfo.accountKey;
    }

    public boolean mayHaveRunOutOfMemory() {
        return mayHaveRunOutOfMemory;
    }

    //	private String folderPrefix; // prefix for folder files
    private transient Store store;                                        // we don't really need this serialized across sessions

    private transient Archive archive;
    final Collection<String> dataErrors = new LinkedHashSet<>();    // log of input data errors
    private Date prevDate = null;

	/*
     * // comment out unused constructors, so it's cleaner/easier to trace the
	 * setting member fields.
	 * public EmailFetcherThread() { super(); }
	 *
	 * public EmailFetcherThread(EmailStore store, String folder_name)
	 * {
	 * this.emailStore = store;
	 * this.folder_name = folder_name;
	 * }
	 */

    public EmailFetcherThread(EmailStore store, FolderInfo fi, int begin_msg_index, int end_msg_index) {
        this.emailStore = store;
        this.fetchedFolderInfo = fi;
        stats.nTotalMessages = end_msg_index - begin_msg_index;
        this.begin_msg_index = begin_msg_index;
        this.end_msg_index = end_msg_index;
    }

    public void setArchive(Archive a) {
        archive = a;
    }

    public Archive getArchive() {
        return archive;
    }

    /**
     * merges results with another email fetcher. does some lightweight work
     * including updating stats. consider removing this and simplifying in the
     * future
     */
    public void merge(EmailFetcherThread other) {
        verify();
        if (other != null) {
            other.verify();

            // TOFIX: we should eliminate duplicates
            dataErrors.addAll(other.dataErrors);
            stats.merge(other.stats);

            nMessagesProcessedSuccess += other.nMessagesProcessedSuccess;
            nErrors += other.nErrors;
            mayHaveRunOutOfMemory |= other.mayHaveRunOutOfMemory;
        }
        verify();
    }

    /**
     * intern a bunch of addrs, to save memory
     *
     * @throws UnsupportedEncodingException
     */
    private static void internAddressList(Address[] addrs) throws UnsupportedEncodingException {
        if (addrs == null)
            return;

        for (Address a : addrs) {
            if (a instanceof InternetAddress) {
                InternetAddress ia = (InternetAddress) a;
                String address = ia.getAddress(), personal = ia.getPersonal();
                if (address != null)
                    ia.setAddress(InternTable.intern(address));
                if (personal != null)
                    ia.setPersonal(InternTable.intern(personal));
            }
        }
    }

    /**
     * Key method for importing email: converts a javamail obj. to our own data structure (EmailDocument)
     */
    //public EmailDocument convertToEmailDocument(MimeMessage m, int num, String url) throws MessagingException, IOException
    private EmailDocument convertToEmailDocument(MimeMessage m, String id) throws MessagingException, IOException {
        // get the date.


        // prevDate is a hack for the cases where the message is lacking an explicit Date: header. e.g.
        //		From hangal Sun Jun 10 13:46:46 2001
        //		To: ewatkins@stanford.edu
        //		Subject: Re: return value bugs
        // though the date is on the From separator line, the mbox provider fails to parse it and provide it to us.
        // so as a hack, we will assign such messages the same date as the previous one this fetcher has seen! ;-)
        // update: having the exact same date causes the message to be considered a duplicate, so just increment
        // the timestamp it by 1 millisecond!
        // a better fix would be to improve the parsing in the provider
        // note: the above logic was a cute hack but is being disabled for ePADD as we don't want any guessed dates.

        boolean noDate = false;
        boolean probablyWrongDate = false;
        Date d = m.getSentDate();
        if (d == null)
            d = m.getReceivedDate();
//        if (d == null) {
//            if (prevDate != null) {
//                long newTime = prevDate.getTime() + 1L; // added +1 so that this email is not considered the same object as the prev. one if they are in the same thread
//                d = new Date(newTime);
//                dataErrors.add("No date for message id:" + id + ": " + EmailUtils.formatMessageHeader(m) + " assigned approximate date");
//            } else {
//                d = INVALID_DATE; // wrong, but what can we do... :-(
//                dataErrors.add("No date for message id:" + id + ": " + EmailUtils.formatMessageHeader(m) + " assigned deliberately invalid date");
//            }

        if (d == null) {
            d = INVALID_DATE;
            noDate = true;
            dataErrors.add("No date for message id:" + id + ": " + EmailUtils.formatMessageHeader(m) );
    } else {
            Calendar c = new GregorianCalendar();
            c.setTime(d);
            int yy = c.get(Calendar.YEAR);
            if (yy < 1960 || yy > 2020) {
                dataErrors.add("Probably bad date: " + Util.formatDate(c) + " message: " + EmailUtils.formatMessageHeader(m));
                probablyWrongDate = true;


            }
        }

        /*
        if (hackyDate && prevDate != null) {
            long newTime = prevDate.getTime() + 1L; // added +1 so that this email is not considered the same object as the prev. one if they are in the same thread
            d = new Date(newTime);
            Util.ASSERT(!d.equals(prevDate));
        }
        */

        Calendar c = new GregorianCalendar();
        c.setTime(d);

       // prevDate = d;

        Address to[] = null, cc[] = null, bcc[] = null;
        Address[] from = null;
        try {
            // 			allrecip = m.getAllRecipients(); // turns out to be too expensive because it looks for newsgroup headers for imap
            // assemble to, cc, bcc into a list and copy it into allrecip
            List<Address> list = new ArrayList<>();
            from = m.getFrom();
            to = m.getRecipients(Message.RecipientType.TO);
            if (to != null)
                list.addAll(Arrays.asList(to));
            cc = m.getRecipients(Message.RecipientType.CC);
            if (cc != null)
                list.addAll(Arrays.asList(cc));
            bcc = m.getRecipients(Message.RecipientType.BCC);
            if (bcc != null)
                list.addAll(Arrays.asList(bcc));

            // intern the strings in these addresses to save memory cos they are repeated often in a large archive
            internAddressList(from);
            internAddressList(to);
            internAddressList(cc);
            internAddressList(bcc);
        } catch (AddressException ae) {
            String s = "Bad address in folder " + folder_name() + " message id" + id + " " + ae;
            dataErrors.add(s);
        }

        //following case is to handle scenario when an icon file is considered as an mbox file with 1 message.
     /*   if(hackyDate && from==null && to==null && cc==null && bcc==null) {
            dataErrors.add("IMP:::: hacky date with all as null (from, to, cc, bcc) for folder "+folder_name() + " message id " + id );
            //return null;

        }
*/        // take a deep breath. This object is going to live longer than most of us.
        EmailDocument ed = new EmailDocument(id, email_source(), folder_name(), to, cc, bcc, from, m.getSubject(), m.getMessageID(), c.getTime());

        String[] headers = m.getHeader("List-Post");
        if (headers != null && headers.length > 0) {
            // trim the headers because they usually look like: "<mailto:prpl-devel@lists.stanford.edu>"
            ed.sentToMailingLists = new String[headers.length];
            int i = 0;
            for (String header : headers) {
                header = header.trim();
                header = header.toLowerCase();

                if (header.startsWith("<") && header.endsWith(">"))
                    header = header.substring(1, header.length() - 1);
                if (header.startsWith("mailto:") && !"mailto:".equals(header)) // defensive check in case header == "mailto:"
                    header = header.substring(("mailto:").length());
                ed.sentToMailingLists[i++] = header;
            }
        }
       /* if (hackyDate) {
            String s = "Guessed date " + Util.formatDate(c) + " for message id: " + id + ": " + ed.getHeader();
            dataErrors.add(s);
            ed.hackyDate = true;
        }*/
        if(noDate){
            Set<String> lab = new LinkedHashSet<>();
            lab.add(LabelManager.LABELID_NODATE);
            getArchive().getLabelManager().setLabels(ed.getUniqueId(),lab);
            ed.hackyDate=true;
        }
        if(probablyWrongDate){
            Set<String> lab = new LinkedHashSet<>();
            lab.add(LabelManager.LABELID_POSS_BADDATE);
            getArchive().getLabelManager().setLabels(ed.getUniqueId(),lab);
            ed.hackyDate=true;
        }

        // check if the message has attachments.
        // if it does and we're not downloading attachments, then we mark the ed as such.
        // otherwise we had a problem where a message header (and maybe text) was downloaded but without attachments in one run
        // but in a subsequent run where attachments were needed, we thought the message was already cached and there was no
        // need to recompute it, leaving the attachments field in this ed incorrect.
        List<String> attachmentNames = getAttachmentNames(m, m);
        if (!Util.nullOrEmpty(attachmentNames)) {
            ed.attachmentsYetToBeDownloaded = true; // will set it to false later if attachments really were downloaded (not sure why)
            //			log.info ("added " + attachmentNames.size() + " attachments to message: " + ed);
        }
        return ed;
    }

    /*
     * we try to get the attachment names cheaply, i.e. without having to
     * process the whole message
     */
    private List<String> getAttachmentNames(MimeMessage m, Part p) {
        List<String> result = new ArrayList<>();
        try {
            if (p.isMimeType("multipart/*") || p.isMimeType("message/rfc822")) {
                if (p.isMimeType("multipart/alternative"))
                    return result; // ignore alternative's because real attachments don't have alternatives
                DataHandler dh = p.getDataHandler();
                DataSource ds = dh.getDataSource();
                if (ds instanceof MultipartDataSource) {
                    MultipartDataSource mpds = (MultipartDataSource) ds;
                    for (int i = 0; i < mpds.getCount(); i++)
                        result.addAll(getAttachmentNames(m, mpds.getBodyPart(i)));
                } else {
                    String name = ds.getName();
                    if (!Util.nullOrEmpty(name))
                        result.add(name);
                }
            } else {
                String filename = p.getFileName();
                if (filename != null)
                    result.add(filename);
            }
        } catch (Exception e) {
            // sometimes we see javax.mail.MessagingException: Unable to load BODYSTRUCTURE
            // in this case, just ignore, not much we can do i guess.
            Util.print_exception(e, log);
        }
        return result;
    }

    //	public void setEmailCache (DocCache cache)
    //	{
    //		this.cache = cache;
    //	}

    /**
     * this method returns the text content of the message as a list of strings
     * // each element of the list could be the content of a multipart message
     * // m is the top level subject
     * // p is the specific part that we are processing (p could be == m)
     * also sets up names of attachments (though it will not download the
     * attachment unless downloadAttachments is true)
     */
    private List<String> processMessagePart(EmailDocument ed, int messageNum, Message m, Part p, List<Blob> attachmentsList) throws MessagingException, IOException {
        List<String> list = new ArrayList<>(); // return list
        if (p == null) {
            dataErrors.add("part is null: " + folder_name() + " idx " + messageNum);
            Set<String> label = new LinkedHashSet<>();
            label.add(LabelManager.LABELID_PARSING_ERRS);
            archive.getLabelManager().setLabels(ed.getUniqueId(),label);
            return list;
        }

        if (p == m && p.isMimeType("text/html")) {
            /*
            String s = "top level part is html! message:" + m.getSubject() + " " + m.getDescription();
            dataErrors.add(s);
            */
            // we don't normally expect the top-level part to have content-type text/html
            // but we saw this happen on some sample archives pst -> emailchemy. so allow it and handle it by parsing the html
            String html = (String) p.getContent();
            String text = Util.unescapeHTML(html);
            org.jsoup.nodes.Document doc = Jsoup.parse(text);

            StringBuilder sb = new StringBuilder();
            HTMLUtils.extractTextFromHTML(doc.body(), sb);
            list.add(sb.toString());
            return list;
        }

        if (p.isMimeType("text/plain")) {
            //IMP: CHECK - The part p should be an instance of MimeBodyPart
            MimeBodyPart bodyPart = ((MimeBodyPart) p);
            if(bodyPart==null){
                log.warn("WARN!! Expected MimeBodyPart but did not get any... Some invariant about the structure of mbox file is broken. Please contact the developers.");
            }
            String encoding = "quoted-printable";
            String charset =  "utf-8";
            //make sure, p is not wrongly labelled as plain text.
            Enumeration headers = p.getAllHeaders();
            boolean dirty = false;
            if (headers != null)
                while (headers.hasMoreElements()) {
                    Header h = (Header) headers.nextElement();
                    String name = h.getName();
                    String value = h.getValue();
                    if (h.getName().equals("Content-Type")) {
                        //An example of h.getValue() is text/plain; format=flowed; charset=US-ASCII
                        //To get charset from it. First tokenize based on ; and then iterate over all tokens to check that value which starts with charset= and get its value.
                        StringTokenizer multiTokenizer = new StringTokenizer(h.getValue(), ";");
                        while(multiTokenizer.hasMoreTokens()){
                            String token = multiTokenizer.nextToken();
                            if(token.trim().startsWith("charset")){ //trim is needed because sometime there is a space before "charset=utf-8"
                                charset = token.substring(token.indexOf("=") + 1).trim();
                                //Interesting issue: sometime charset is already within double quotes, so windows-1252 will be like "windows-1252"
                                //Therefore we need to strip leading and trailing quotes to get actual charset. But unfortunately it is not uniform. For
                                //example, utf-8 does not have this property so we should not be stripping quotes otherwise it distorts the charset variable
                                //and the decoding fails. So a simple check introduced is to check if the leading and trailing quotes are present in charset then
                                //only we should strip them
                                if(charset.startsWith("\"") && charset.endsWith("\""))
                                    charset = charset.substring(1, charset.length() - 1).toLowerCase();
                            }
                        }
                    } else if (h.getName().toLowerCase().equals("content-encoding") || h.getName().toLowerCase().equals("content-transfer-encoding")) {
                        encoding = h.getValue().toLowerCase();
                    }
                    /*if (name != null && value != null) {
                        if (name.equals("Content-transfer-encoding") && value.equals("base64")) {
                            dirty = true;
                            break;
                        }
                        if(name.equals("Content-Encoding") && value.equals("base64")){
                            encoding = "base64";//what about the behaviour of dirty field @TODO
                        }
                    }*/
                }
            String fname = p.getFileName();
            if (fname != null) {
                int idx = fname.lastIndexOf('.');
                if ((idx < fname.length()) && (idx >= 0)) {
                    String extension = fname.substring(idx);
                    //anything extension other than .txt is suspicious.
                    if (!extension.equals(".txt"))
                        dirty = true;
                }
            }
            if (dirty) {
                dataErrors.add("Dirty message part, has conflicting message part headers."  + folder_name() + " Message# " + messageNum);
                Set<String> label = new LinkedHashSet<>();
                label.add(LabelManager.LABELID_PARSING_ERRS);
                archive.getLabelManager().setLabels(ed.getUniqueId(),label);
                return list;
            }

            log.debug("Message part with content type text/plain");
            String content;
            String type = p.getContentType(); // new InputStreamReader(p.getInputStream(), "UTF-8");
            try {
                // if forced encoding is set, we read the string with that encoding, otherwise we just use whatever p.getContent gives us
                /*if (FORCED_ENCODING != null) {
                    byte b[] = Util.getBytesFromStream(p.getInputStream());
                    content = new String(b, FORCED_ENCODING);
                } else
                    content = (String) p.getContent();*/
                //Cases: encoding              charset [ For now always assume utf-8 charset but it can be different]
                ///1      quoted-printable
                ///2      base64
                if (encoding.toLowerCase().equals("quoted-printable")) {

                    byte b[] = null;
                    ///Following code handles if by chance this part was not an instance of MimeBodyPart (which it should be) then fallback upon getting
                    //inputstream instead of getRawInputStream.
                    if(bodyPart!=null)
                       b  = Util.getBytesFromStream(bodyPart.getRawInputStream());
                    else
                        b  = Util.getBytesFromStream(p.getInputStream());

                    try {
                        b = QuotedPrintableCodec.decodeQuotedPrintable(b);
                    } catch (DecoderException e) {
                        e.printStackTrace();
                        log.error("Unable to decode quoted printable encoded message");
                        dataErrors.add("Unable to decode quoted printable encoded message in  " + folder_name() + " Message #" + messageNum + " type " + type );
                        Set<String> label = new LinkedHashSet<>();
                        label.add(LabelManager.LABELID_PARSING_ERRS);
                        archive.getLabelManager().setLabels(ed.getUniqueId(),label);
                        //return list; //If decoding fails at least try to create content by new String(b,charset) using default encoding.
                    }
                    content = new String(b, charset);
                } else if(encoding.equals("base64")){
                    //Encoding is base64 so perform proper decoding.
                    ///Following code handles if by chance this part was not an instance of MimeBodyPart (which it should be) then fallback upon getting
                    //inputstream instead of getRawInputStream.
                    byte b[] = null;
                    if(bodyPart!=null)
                        b = Util.getBytesFromStream(bodyPart.getRawInputStream());
                    else
                        b = Util.getBytesFromStream(p.getInputStream());
                    //decode it.
                    byte decoded[] = Base64.decodeBase64(b);
                    content = new String(decoded,charset);
                }else{
                    //Encoding is something else (maybe "binary") just read it in the conent without decoding.
                    byte b[] = Util.getBytesFromStream(p.getInputStream());
                    content = new String(b);
                }
            } catch (UnsupportedEncodingException uee) {
                dataErrors.add("Unsupported encoding: " + folder_name() + " Message #" + messageNum + " type " + type + ", using brute force conversion");
                Set<String> label = new LinkedHashSet<>();
                label.add(LabelManager.LABELID_PARSING_ERRS);
                archive.getLabelManager().setLabels(ed.getUniqueId(),label);
                // a particularly nasty issue:javamail can't handle utf-7 encoding which is common with hotmail and exchange servers.
                // we're using the workaround suggested on this page: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4304013
                // though it may be better to consider official support for utf-7 or other encodings.

                // TOFIX: I get an exception for utfutf8-encoding which has a base64 encoding embedded on it.
                // Unsupported encoding: gmail-sent Message #10477 type text/plain; charset=x-utf8utf8; name="newyorker.txt",
                // the hack below doesn't work for it.
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                p.writeTo(bao);
                content = bao.toString();
            }
            list.add(content);
        } else if (p.isMimeType("multipart/*") || p.isMimeType("message/rfc822")) {
            // rfc822 mime type is for embedded mbox format or some such (appears for things like
            // forwarded messages). the content appears to be just a multipart.
            Object o = p.getContent();
            if (o instanceof Multipart) {
                Multipart allParts = (Multipart) o;
                if (p.isMimeType("multipart/alternative")) {
                    // this is an alternative mime type. v common case to have text and html alternatives
                    // so just process the text part if there is one, and avoid fetching the alternatives.
                    // useful esp. because many ordinary messages are alternative: text and html and we don't want to fetch the html.
                    // revisit in future we want to retain the html alternative for display purposes
                    Part[] parts = new Part[allParts.getCount()];
                    for (int i = 0; i < parts.length; i++)
                        parts[i] = allParts.getBodyPart(i);

                    for (Part thisPart : parts) {
                        if (thisPart.isMimeType("text/plain")) {
                            // common case, return quickly
                            list.add((String) thisPart.getContent());
                            log.debug("Multipart/alternative with content type text/plain");
                            return list;
                        }
                    }

                    // no text part, let's look for an html part. this happens for html parts.
                    for (int i = 0; i < allParts.getCount(); i++) {
                        Part thisPart = parts[i];
                        if (thisPart.isMimeType("text/html")) {

                            // common case, return quickly
                            String html = (String) thisPart.getContent();
                            String text = Util.unescapeHTML(html);
                            org.jsoup.nodes.Document doc = Jsoup.parse(text);

                            StringBuilder sb = new StringBuilder();
                            HTMLUtils.extractTextFromHTML(doc.body(), sb);
                            list.add(sb.toString());

                            log.debug("Multipart/alternative with content type text/html");
                            return list;
                        }
                    }

                    // no text or html part. hmmm... blindly process the first part only
                    if (allParts.getCount() >= 1)
                        list.addAll(processMessagePart(ed,messageNum, m, allParts.getBodyPart(0), attachmentsList));
                } else {
                    // process it like a regular multipart
                    for (int i = 0; i < allParts.getCount(); i++) {
                        BodyPart bp = allParts.getBodyPart(i);
                        list.addAll(processMessagePart(ed, messageNum, m, bp, attachmentsList));
                    }
                }
            } else if (o instanceof Part)
                list.addAll(processMessagePart(ed,messageNum, m, (Part) o, attachmentsList));
            else {
                dataErrors.add("Unhandled part content, " + folder_name() + " Message #" + messageNum + "Java type: " + o.getClass() + " Content-Type: " + p.getContentType());
                Set<String> label = new LinkedHashSet<>();
                label.add(LabelManager.LABELID_PARSING_ERRS);
                archive.getLabelManager().setLabels(ed.getUniqueId(),label);
            }
        } else {
            try {
                // do attachments only if downloadAttachments is set.
                // some apps do not need attachments, so this saves some time.
                // however, it seems like a lot of time is taken in imap prefetch, which gets attachments too?
                if (fetchConfig.downloadAttachments)
                    handleAttachments(ed,messageNum, m, p, list, attachmentsList);
            } catch (Exception e) {
                dataErrors.add("Ignoring attachment for " + folder_name() + " Message #" + messageNum + ": " + Util.stackTrace(e));
                Set<String> label = new LinkedHashSet<>();
                label.add(LabelManager.LABELID_ATTCH_ERRS);
                archive.getLabelManager().setLabels(ed.getUniqueId(),label);
            }
        }

        return list;
    }

    /**
     * recursively processes attachments, fetching and saving it if needed
     * parses the given part p, and adds it to hte attachmentsList.
     * in some cases, like a text/html type without a filename, we instead append it to the textlist
     * @throws MessagingException
     */
    private void handleAttachments(EmailDocument ed,int idx, Message m, Part p, List<String> textList, List<Blob> attachmentsList) throws MessagingException {
        String ct = null;
        if (!(m instanceof MimeMessage)) {
            Exception e = new IllegalArgumentException("Not a MIME message!");
            e.fillInStackTrace();
            log.warn(Util.stackTrace(e));
            return;
        }

        String filename = null;
        try {
            filename = p.getFileName();
        } catch (Exception e) {
            // seen this happen with:
            // Folders__gmail-sent Message #12185 Expected ';', got "Message"
            // javax.mail.internet.ParseException: Expected ';', got "Message"

            dataErrors.add("Unable to read attachment name: " + folder_name() + " Message# " + idx);
            Set<String> label = new LinkedHashSet<>();
            label.add(LabelManager.LABELID_ATTCH_ERRS);
            archive.getLabelManager().setLabels(ed.getUniqueId(),label);
            return;
        }

        String sanitizedFName = Util.sanitizeFolderName(emailStore.getAccountID() + "." + folder_name());
        if (filename == null) {
            String tempFname = sanitizedFName + "." + idx;
            dataErrors.add("attachment filename is null for " + sanitizedFName + " Message#" + idx + " assigning it the name: " + tempFname);
            //assign a special label to this message to denote that there was some problem in parsing.
            Set<String> lab = new LinkedHashSet<>();
            lab.add(LabelManager.LABELID_ATTCH_ERRS);
            getArchive().getLabelManager().setLabels(ed.getUniqueId(),lab);
            if (p.isMimeType("text/html")) {
                try {
                    log.info("Turning message " + sanitizedFName + " Message#" + idx + " into text although it is an attachment");
                    String html = (String) p.getContent();
                    String text = Util.unescapeHTML(html);
                    org.jsoup.nodes.Document doc = Jsoup.parse(text);

                    StringBuilder sb = new StringBuilder();
                    HTMLUtils.extractTextFromHTML(doc.body(), sb);
                    textList.add(sb.toString());
                    return;
                } catch (Exception e) {
                    Util.print_exception("Error reading contents of text/html multipart without a filename!", e, log);
                    return;
                }
            }
            filename = tempFname;
        }

        // Replacing any of the disallowed filename characters (\/:*?"<>|&) to _
        // (note: & causes problems with URLs for serveAttachment etc, so it's also replaced)
        String newFilename = Util.sanitizeFileName(filename);

        // Updating filename if it's changed after sanitizing.
        if (!newFilename.equals(filename)) {
            log.info("Filename changed from " + filename + " to " + newFilename);
            filename = newFilename;
        }

        try {
            ct = p.getContentType();
            if (!filename.contains(".")) // no ext in filename... let's fix it if possible
            {
                // Using startsWith instead of equals because sometimes the ct has crud beyond the image/jpeg;...crud....
                // Below are the most common file types, more type can be added if needed

                // Most common APPLICATION TYPE
                if (ct.startsWith("application/pdf"))
                    filename = filename + ".pdf";
                if (ct.startsWith("application/zip"))
                    filename = filename + ",zip";
                // Most common IMAGE TYPE
                if (ct.startsWith("image/jpeg"))
                    filename = filename + ".jpg";
                if (ct.startsWith("image/gif"))
                    filename = filename + ".gif";
                if (ct.startsWith("image/png"))
                    filename = filename + ".png";
                // Most Common VIDEO TYPE
                if (ct.startsWith("video/x-ms-wmv"))
                    filename = filename + ".wmv";
                // Most Common AUDIO TYPE
                if (ct.startsWith("audio/mpeg"))
                    filename = filename + ".mp3";
                if (ct.startsWith("audio/mp4"))
                    filename = filename + ".mp4";
                // Most Common TEXT TYPE
                if (ct.startsWith("text/html"))
                    filename = filename + ".html";
                // Windows Office
                if (ct.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) //Word
                    filename = filename + ".docx";
                if (ct.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) //Excel
                    filename = filename + ".xlsx";
                if (ct.startsWith("application/vnd.openxmlformats-officedocument.presentationml.presentation")) //PowerPoint
                    filename = filename + ".pptx";
            }
            // retain only up to first semi-colon; often ct is something like text/plain; name="filename"' we don't want to log the filename
            int x = ct.indexOf(";");
            if (x >= 0)
                ct = ct.substring(0, x);
            log.info("Attachment content type: " + ct + " filename = " + Util.blurKeepingExtension(filename));
        } catch (Exception pex) {
            dataErrors.add("Can't read CONTENT-TYPE: " + ct + " filename:" + filename + " size = " + p.getSize() + " subject: " + m.getSubject() + " Date : " + m.getSentDate().toString() + "\n Exception: " + pex + "\n" + Util.stackTrace(pex));
            return;
        }

        //	    if (filename == null && !p.isMimeType("text/html") && !p.isMimeType("message/partial")) // expected not to have a filename with mime type text/html
        //	    	log.warn ("Attachment filename is null: " + Util.stackTrace());


        boolean success = true;
        // the size passed in here is the part size, which is not really the binary blob size.
        // when we read the stream below in blobStore.add(), we'll set it again to the binary blob size
        Blob b = new EmailAttachmentBlob(filename, p.getSize(), (MimeMessage) m, p);
//fetchConfig.downloadAttachments=false; Just for testing..
        if (fetchConfig.downloadAttachments) {
            // this containment check is only on the basis of file name and size currently,
            // not on the actual hash
            if (archive.getBlobStore().contains(b)) {
                log.debug("Cache hit! " + b);
            } else {
                try {
                    if (filename.endsWith(".tif"))
                        log.info("Fetching attachment..." + Util.blurKeepingExtension(filename));

                    // performance critical! use large buffer! currently 256KB
                    // stream will be closed by callee

                    long start = System.currentTimeMillis();
                    long nBytes = archive.getBlobStore().add(b, new BufferedInputStream(p.getInputStream(), 256 * 1024));
                    long end = System.currentTimeMillis();
                    if (nBytes != -1) {
                        long diff = end - start;
                        String s = "attachment size " + nBytes + " bytes, fetched in " + diff + " millis";
                        if (diff > 0)
                            s += " (" + (nBytes / diff) + " KB/s)";
                        log.info(s);
                    }

                    Util.ASSERT(archive.getBlobStore().contains(b));

                } catch (IOException ioe) {
                    success = false;
                    dataErrors.add("WARNING: Unable to fetch attachment: filename: " + filename + " size = " + p.getSize() + " subject: " + m.getSubject() + " Date : " + m.getSentDate().toString() + "\nException: " + ioe);
                    ioe.printStackTrace(System.out);
                }
            }

            if (success) {
                attachmentsList.add(b);

              /*  /// generate thumbnail only if not already cached,
                try {
                    archive.getBlobStore().generate_thumbnail(b); // supplement
                } catch (IOException ioe) {
                    log.warn("failed to create thumbnail, filename: " + filename + " size = " + p.getSize() + " subject: " + m.getSubject() + " Date : " + m.getSentDate().toString() + "\nException: " + ioe);
                    ioe.printStackTrace(System.out);
                }*/
            }
        }
    }

    @SuppressWarnings("unused")
    private static String processLastReceived(String header) {
        header = header.toLowerCase();
        StringTokenizer st = new StringTokenizer(header, " \t()[]");
        String x = st.nextToken();
        if (!x.equals("from")) {
            log.warn("Warning: unrecognized header: " + header);
            return null;
        }

        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (Character.isDigit(s.charAt(0))) {
                log.warn("IP address: " + s);
                return s;
            }
        }
        return null;
    }

    private void verify() {
    }

    public void finish() {
        currentStatus = JSONUtils.getStatusJSON("Verifying email headers...");
        currentStatus = JSONUtils.getStatusJSON("");
    }

    /**
     * prepare a status json with up to N_TEASERS teasers from the most recent
     * emails, starting backwards from idx. specifically ask for ArrayList as
     * List.get() can be costly otherwise.
     */
    private static String getStatusJSONWithTeasers(String message, int pctComplete, long secsElapsed, long secsRemaining, ArrayList<EmailDocument> emails, int N_TEASERS) {
        JSONObject json = new JSONObject();
        try {
            json.put("pctComplete", pctComplete);
            json.put("message", message);
            json.put("secsElapsed", secsElapsed);
            json.put("secsRemaining", secsRemaining);
            if (!Util.nullOrEmpty(emails)) {
                JSONArray arr = new JSONArray();
                int idx_end = emails.size();
                int idx_start = idx_end - N_TEASERS;
                if (idx_start < 0)
                    idx_start = 0;
                for (int i = idx_start, j = 0; i < idx_end; i++) {
                    EmailDocument email = emails.get(i);
                    if (email != null) {
                        String subject = email.description;
                        if (!Util.nullOrEmpty(subject))
                            arr.put(j++, subject);
                    }
                }
                json.put("teasers", arr);
            }
        } catch (JSONException jsone) {
            try {
                json.put("error", jsone.toString());
            } catch (Exception e) {
                Util.report_exception(e);
            }
        }
        return json.toString();
    }

    /**
     * best effort to prefetch messages for messages[startMsgIdx] onwards, up to
     * the IMAP_PREFETCH_BUFSIZE
     * return List<String> if bodyTextOnly is true, otherwise List<MimeMessage>
     */
    private List<?> do_imap_prefetch(Message[] messages, int startMsgIdx, Folder folder, boolean bodyTextOnly) {
        // its perfectly ok for correctness for this method to do nothing and return null
        List<?> prefetchedMessages = null;
        try {

            if (IMAP_PREFETCH_BUFSIZE > 0 && folder instanceof IMAPFolder) {
                int prefetch_messages_size = 0;

                int start_message_num = messages[startMsgIdx].getMessageNumber();
                int end_message_num = start_message_num;

                List<Integer> messageNums = new ArrayList<>();

                // figure out message num range to fetch. if anything is unusual -- bad content type, non-consec. msg nums etc -- break out.
                // non consec. message numbers are a problem because they cause a very long imap command string, which we found was returning an "invalid command" response.
                int prev_message_num = -1;
                for (int msgIdx = startMsgIdx; msgIdx < messages.length; msgIdx++) {
                    if (bodyTextOnly) {
                        String contentType = messages[msgIdx].getContentType().toLowerCase();
                        if (!contentType.startsWith("multipart/") && !contentType.startsWith("text/plain")) {
                            log.info("Warn: message idx" + msgIdx + " msg#" + messages[msgIdx].getMessageNumber() + " has unexpected content type " + contentType);
                            break;
                        }
                    }

                    // check if sequence is as expected
                    int next_message_num = messages[msgIdx].getMessageNumber(); // may be better to switch this to uid and prefetcher uses uid fetch
                    if (next_message_num != prev_message_num + 1 && prev_message_num != -1)
                        break;

                    // if this message would push prefetch size beyond the buf size, break out, not including this message
                    if (prefetch_messages_size + messages[msgIdx].getSize() >= IMAP_PREFETCH_BUFSIZE)
                        break;
                    prev_message_num = next_message_num;
                    prefetch_messages_size += messages[msgIdx].getSize();
                    messageNums.add(next_message_num);
                }

                if (messageNums.size() == 0)
                    return null;

                // now we prefetch messages from start_message_num to end_message_num
                long startMillis = System.currentTimeMillis();
                log.info("prefetching " + messageNums.size() + " messages");
                ImapPrefetcher prefetcher = bodyTextOnly ? new TextOnlyImapPrefetcher(((ImapPopEmailStore) emailStore).session, messageNums) : new ImapPrefetcher(((ImapPopEmailStore) emailStore).session, messageNums);
                prefetchedMessages = (List<?>) ((IMAPFolder) folder).doCommand(prefetcher); // start_message_num, end_message_num));
                long elapsedMillis = System.currentTimeMillis() - startMillis;
                long kb_per_sec = prefetch_messages_size / elapsedMillis;
                log.info("prefetched " + messageNums.size() + " messages in " + Util.blur(folder.getName()) + " [" + start_message_num + ":" + end_message_num + "], " + Util.commatize(prefetch_messages_size / 1024) + "KB in " + Util.commatize(elapsedMillis) + "ms (" + Util.commatize(kb_per_sec) + " KB/sec)");
            }
        } catch (Exception e) {
            Util.print_exception(e, log);
        }
        return prefetchedMessages;
    }

    private void fetchHeaders(Message[] messages) throws MessagingException {
        // fetch headers (don't do it for mbox folders, waste of time)
        // this is an essential perf. step so that we fetch the headers in bulk.
        // otherwise it takes a long time to fetch header info one at a time for each message
        if (!(emailStore instanceof MboxEmailStore)) {
            long startTimeMillis = System.currentTimeMillis();
            currentStatus = JSONUtils.getStatusJSON("Reading headers from " + folder.getName() + "...");
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.CONTENT_INFO);
            fp.add(UIDFolder.FetchProfileItem.UID); // important, otherwise reading UIDs takes a long time later
            fp.add("List-Post");
            folder.fetch(messages, fp);
            long endTimeMillis = System.currentTimeMillis();
            log.info("Done fetching headers: " + Util.commatize(endTimeMillis - startTimeMillis) + "ms");
        }
    }

    private void fetchHeaders(int nMessages) throws MessagingException {
        // fetch headers (don't do it for mbox folders, waste of time)
        // this is an essential perf. step so that we fetch the headers in bulk.
        // otherwise it takes a long time to fetch header info one at a time for each message
        if (!(emailStore instanceof MboxEmailStore)) {
            long startTimeMillis = System.currentTimeMillis();
            currentStatus = JSONUtils.getStatusJSON("Reading headers from " + folder.getName() + "...");
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.CONTENT_INFO);
            fp.add(UIDFolder.FetchProfileItem.UID); // important, otherwise reading UIDs takes a long time later
            fp.add("List-Post");
            for (int i = 0; i < nMessages; i++) {
                Message[] messages = new Message[]{folder.getMessage(i)};
                folder.fetch(messages, fp);
            }
            long endTimeMillis = System.currentTimeMillis();
            log.info("Done fetching headers: " + Util.commatize(endTimeMillis - startTimeMillis) + "ms");
        }
    }

    private Message[] removeMessagesAlreadyInArchive(Archive archive, Message[] messages) {
        // early out for the common case that we have an empty archive
        if (archive.getAllDocs().size() == 0)
            return messages;

        List<Message> resultList = new ArrayList<>();
        for (int i = 0; i < messages.length; i++) {
            //int idx = messages[i].getMessageNumber();
            Message m = messages[i];
            MimeMessage mm = (MimeMessage) m;
            try {
                EmailDocument ed = convertToEmailDocument(mm, "dummy"); // id doesn't really matter here
                if (archive.containsDoc(ed)) {
                    //get more info about the already present message (duplicate)
                    Document alreadypresent = archive.getAllUniqueDocsMap().get(ed);
                    archive.getDupMessageInfo().put(alreadypresent,new Tuple2(ed.folderName,ed.messageID));

                    stats.nMessagesAlreadyPresent++;
                    //dataErrors.add("Duplicate message: " + ed); // note: report.jsp depends on this exact string
                    continue;
                }
            } catch (Exception e) {
                Util.print_exception(e, log);
            }
            resultList.add(mm);
            messages[i] = null; // no harm explicitly nulling out messages
        }
        Message[] resultArray = resultList.toArray(new Message[0]);
        return resultArray;
    }

    /**
     * Make few post checks on the content and returns true if the message looks
     * ok
     */
    private boolean messageLooksOk(String content) {
        if (content == null)
            //let others handle it.
            return true;

        String[] lines = content.split("\n");
        int badlines = 0;
        if (lines.length > 50)
            for (String line : lines) {
                if (!line.contains(" "))
                    badlines++;
                else
                    badlines = 0;
                if (badlines > 50)
                    return false;
            }
        return true;
    }

    //keep track of the total time elapsed in fetching messages across batches
    private static long fetchStartTime = System.currentTimeMillis();

    /**
     * fetch given message idx's in given folder -- @performance critical
     *
     * @param offset - the original offset of the first message in the messages array, important to initialize
     *               for proper assignment of unique id or doc Id
     */
    //private void fetchUncachedMessages(String sanitizedFName, Folder folder, DocCache cache, List<Integer> msgIdxs) throws MessagingException, FileNotFoundException, IOException, GeneralSecurityException {
    private void fetchAndIndexMessages(Folder folder, Message[] messages, int offset, int totalMessages) {
        //mark the processing of new batch
        if (offset == 0)
            fetchStartTime = System.currentTimeMillis();

        currentStatus = JSONUtils.getStatusJSON((emailStore instanceof MboxEmailStore) ? "Parsing " + folder.getName() + " (can take a while)..." : "Reading " + folder.getName() + "...");

        // bulk fetch of all message headers
        int n = messages.length;

        // eliminate any messages the archive already has
        messages = removeMessagesAlreadyInArchive(archive, messages);

        log.info(n - messages.length + " message(s) already in the archive");

        ArrayList<EmailDocument> emails = new ArrayList<>();

        // for performance, we need to do bulk prefetches, instead of fetching 1 message at a time
        // prefetchedMessages will be a temp cache of prefetched messages
        int first_i_prefetched = -1, last_i_prefetched = -1;
        List<?> prefetchedMessages = null; // the type of this can be either list<string> if text only, otherwise list<mimemmessage>

        long highestUID = archive.getLastUIDForFolder(fetchedFolderInfo.accountKey, fetchedFolderInfo.longName);
        long lastAssignedUID = highestUID;
        boolean bodyTextOnly = !fetchConfig.downloadAttachments;
        try {
            archive.openForWrite();
            for (int i = 0; i < messages.length; i++) {
                // critical step: (thanks, yourkit!)
                // null out the ref to the previous message, otherwise it stays in memory, and the heap effectively needs to be as big as the size of all messages
                if (i > 0)
                    messages[i - 1] = null;

                if (isCancelled)
                    break;

                Message m = messages[i];
                MimeMessage mm = (MimeMessage) m;

                if (i >= last_i_prefetched) {
                    // critical perf. step: do a bulk imap prefetch
                    // the prefetch will fetch as many messages as possible up to a max buffer size, and return the messages prefetched
                    // last_i_prefetched tracks what is the last index into idxs that we have prefetched.
                    // when we run out of prefetched messages, we do another bulk prefetch

                    prefetchedMessages = do_imap_prefetch(messages, i, folder, bodyTextOnly);
                    if (prefetchedMessages != null) {
                        first_i_prefetched = i;
                        last_i_prefetched = i + prefetchedMessages.size();
                    }
                }

                int pctDone = ((i + offset) * 100) / totalMessages;
                long elapsedMillis = System.currentTimeMillis() - fetchStartTime;
                long unprocessedSecs = Util.getUnprocessedMessage(i + offset, totalMessages, elapsedMillis);
                int N_TEASERS = 50; // 50 ok here, because it takes a long time to fetch and process messages, so teaser computation is relatively not expensive
                int nTriesForThisMessage = 0;
                currentStatus = getStatusJSONWithTeasers("Reading " + Util.commatize(totalMessages) + " messages from " + folder.getName() + "...", pctDone, elapsedMillis / 1000, unprocessedSecs, emails, N_TEASERS);

                int messageNum = mm.getMessageNumber();

                try {
                    long unique_id;

                    // if we have uid, that's even better
                    // don't use uid's for mbox, it has a bug and always gives -1
                    // see http://james.apache.org/server/rfclist/imap4/rfc2060.txt for uid spec
                    if (folder instanceof UIDFolder && !(emailStore instanceof MboxEmailStore)) {
                        long uid = ((UIDFolder) folder).getUID(m);
                        unique_id = uid;
                    } else
                        unique_id = lastAssignedUID + 1 + i + offset; // +1 since i starts from 0 (but lastAssignedUID can be -1 -- is that safe? -sgh)

                    if (unique_id > highestUID)
                        highestUID = unique_id;

                    String unique_id_as_string = Long.toString(unique_id);

                    // well, we already converted to emaildoc above during removeMessagesAlreadyInArchive
                    // not a serious perf. concern now, but revisit if needed
                    EmailDocument ed = convertToEmailDocument(mm, unique_id_as_string); // this messageNum is mostly for debugging, it should not be used for equals etc.
                  /*  //if ed is null then continue;// when will it be null? when it finds an icon file interpreted as message. It does not contain any date, from-to etc and no body
                    right now folder listing will not show a file named with icon.
                    if(ed==null)
                    {
                        continue;
                    }*/
                    // need to check this again, because there might be duplicates such within the set we are currently processing.
                    if (archive.containsDoc(ed)) {
                        stats.nMessagesAlreadyPresent++;
                        //get more info about the already present message (duplicate)
                        Document alreadypresent = archive.getAllUniqueDocsMap().get(ed);
                        archive.getDupMessageInfo().put(alreadypresent,new Tuple2(ed.folderName,ed.messageID));


                        //dataErrors.add("Duplicate message: " + ed); // note: report.jsp depends on this specific string
                        continue;
                    }

                    MimeMessage originalMessage = mm; // this is the mm that has all the headers etc.
                    List<Blob> attachmentsList = new ArrayList<>();

                    // if we already have it prefetched, use the prefetched version
                    List<String> contents = null;

                    if (first_i_prefetched >= 0 && prefetchedMessages != null) {
                        if (!fetchConfig.downloadAttachments) {
                            // text only means the prefetchedMessages are stored directly as a list of strings
                            String content = (String) prefetchedMessages.get(i - first_i_prefetched); // note: this_mm only has the prefetched content, but not the headers
                            contents = new ArrayList<>();

                            try {
                                // a special for yahoo which routinely uses quoted-printable. content looks like  =0A0D.... = etc.
                                if (mm.isMimeType("multipart/alternative")) {
                                    Multipart mm_mp = (Multipart) mm.getContent();
                                    Part p0 = mm_mp.getBodyPart(0);
                                    if (p0 instanceof com.sun.mail.imap.IMAPBodyPart) {
                                        String encoding = ((com.sun.mail.imap.IMAPBodyPart) p0).getEncoding();
                                        if ("quoted-printable".equals(encoding)) {
                                            content = new String(Util.getBytesFromStream(javax.mail.internet.MimeUtility.decode(new java.io.ByteArrayInputStream(content.getBytes()), "quoted-printable")));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Util.print_exception("Error trying to parse encoding of multipart", e, log);
                            }

                            contents.add(content);
                        } else {
                            // subtle issue here: the contentType of the prefetchedMessage needs to be be set to the original_mm's content-type.
                            // this was found for cases where the original message is multipart-alternative with a text and html part.
                            // if we don't set prefetchedMessage's content type, it gets a mime type of text/plain and a body = the entire multipart including both parts.
                            // found on sgh's sent mail w/subject: "text to add in help" from  Fri, 7 Jun 2013
                            MimeMessage prefetchedMessage = (MimeMessage) prefetchedMessages.get(i - first_i_prefetched);
                            String contentTypeHeaders[] = originalMessage.getHeader("Content-Type");
                            String contentTypeHeader = null;
                            if (contentTypeHeaders != null && contentTypeHeaders.length == 1)
                                contentTypeHeader = contentTypeHeaders[0];

                            if (!Util.nullOrEmpty(contentTypeHeader)) // we do care about body structure, hang on to it
                                prefetchedMessage.setHeader("Content-Type", contentTypeHeader);
                            mm = prefetchedMessage;
                        }
                        prefetchedMessages.set(i - first_i_prefetched, null); // null out to save memory
                    }

                    if (contents == null)
                        contents = processMessagePart(ed,messageNum, originalMessage, mm, attachmentsList);

                    // if mm is not prefetched, it is the same as original_mm
                    // will also work, but will be slow as javamail accesses and fetches each mm separately, instead of using the bulk prefetched version
                    // even when prefetched, the processMessagePart is somewhat expensive because the attachments have to be extracted etc.

                    // we could overlap processMessagePart with do_imap_prefetch by prefetching in a separate thread, since prefetch is network limited.
                    // but profiling shows processMessagePart takes only 1/4th the time of do_imap_prefetch so overlapping would be a relatively small gain.
                    // not worth the effort right now.
                    ed.attachments = attachmentsList;
                    if (fetchConfig.downloadAttachments)
                        ed.attachmentsYetToBeDownloaded = false; // we've already downloaded our attachments

                    // concat all the contents parts
                    StringBuilder sb = new StringBuilder();
                    for (String s : contents) {
                        sb.append(s);
                        sb.append("\n");
                    }

                    String contentStr = sb.toString();
                    if (!messageLooksOk(contentStr)) {
                        dataErrors.add("Skipping message as it seems to have very long words: " + ed);
                        Set<String> label = new LinkedHashSet<>();
                        label.add(LabelManager.LABELID_PARSING_ERRS);
                        archive.getLabelManager().setLabels(ed.getUniqueId(),label);
                        // but we continue, don't skip the message entirely. See issue #214
                        //but just truncate the message..
                        contentStr="<MESSAGE TOO LONG: POSSIBLE PARSING ISSUE- Truncated>" + contentStr.substring(0,100);

                    }

                    if (contentStr.length() > Config.MAX_TEXT_SIZE_TO_ANNOTATE) {
                        dataErrors.add("Skipping message as it seems to be very long: " + contentStr.length() + " chars, while the max size message that will be annotated for display is " + Config.MAX_TEXT_SIZE_TO_ANNOTATE + " chars. Message = " + ed);
                        Set<String> label = new LinkedHashSet<>();
                        label.add(LabelManager.LABELID_PARSING_ERRS);
                        archive.getLabelManager().setLabels(ed.getUniqueId(),label);
                        // but we continue, don't skip the message entirely. See issue #111
                    }

                    contentStr = IndexUtils.normalizeNewlines(contentStr); // just get rid of \r's

                    archive.addDoc(ed, contentStr);

                    List<LinkInfo> linkList = new ArrayList<>();
                    // linkList might be used only for slant
                    IndexUtils.populateDocLinks(ed, contentStr, linkList, true);
                    ed.links = linkList;
                    stats.nMessagesAdded++;
                } catch (Exception ex) {
                    // sometimes we get unexpected folder closed, so try again
                    boolean retry = false;
                    if (ex instanceof javax.mail.FolderClosedException) {
                        log.warn("Oops, thread " + threadID + " got the folder closed in its face! " + ex.getMessage());

                        // sometimes we get this exception about folder closed
                        // retry up to 3 times, then give up
                        if (nTriesForThisMessage < 3) {
                            retry = true;
                            log.info("Re-opening email store; attempt #" + (nTriesForThisMessage + 1) + " for message " + i);
                            nTriesForThisMessage++;
                            messages = openFolderAndGetMessages();
                            fetchHeaders(messages);
                            --i; // adjust the message index n try again
                        }
                    }

                    if (!retry) {
                        // we sometimes see UnsupportedEncodingException with x-utf8utf8 mime type and ParseException
                        // nothing much can be done, just create a dummy doc and add it to the cache
                        nErrors++;
                        stats.nErrors++;
                        EmailDocument ed = new EmailDocument(Integer.toString(messageNum));
                        log.warn("Exception reading message from " + folder_name() + " Message #" + messageNum + " " + ex.getMessage() + "\n" + Util.stackTrace(ex));

                        ed.setErrorString(Util.stackTrace(ex));
                    }
                }
            }
        } catch (Throwable t) {
            Util.print_exception(t, log);
        } finally {
            //				if (cancelled && false) // TODO: disable for now as currently only indexes are rolled back and allDocs/blobs are not rolled back in sync yet
            //					archive.rollbackIndexWrites();
            //				else
            currentStatus = JSONUtils.getStatusJSON("Saving archive...");
            archive.close();
        }

        fetchedFolderInfo.lastSeenUID = highestUID;
        log.info("at end of fetch, folder info is " + fetchedFolderInfo);

        log.info("emailfetcher thread completed, archive has " + archive.getAllDocs().size() + " docs");
    }

    public FolderInfo getFetchedFolderInfo() {
        return fetchedFolderInfo;
    }

    private int openFolderAndGetMessageCount() throws MessagingException {
        folder = null;

        store = emailStore.connect();
        folder = emailStore.get_folder(store, folder_name());
        if (folder != null)
            return folder.getMessageCount();
        else
            return 0;
    }

    /**
     * Comment by @vihari
     * Not sure what uid id and folder are,I think this code should be more predictable
     * The params begin idx and end idx are used for both uid filtering and Mbox message indexing.
     * does not make sense
     */
    private Message[] openFolderAndGetMessages() throws MessagingException {
        if (folder == null)
            openFolderAndGetMessageCount();

        Message[] messages = null;
        if (folder == null)
            return messages;

        String descr = emailStore.getAccountID() + ":" + folder;
        boolean haveUID = false;
        int count = folder.getMessageCount();
        boolean use_uid_if_available = (begin_msg_index == 1 && end_msg_index == count + 1);
        log.info("use_uid_if_available is set to " + use_uid_if_available);

        if (fetchConfig.filter != null && fetchConfig.filter.isActive()) {
            log.info("Issuing server side filters for " + fetchConfig.filter);
            boolean useReceivedDateTerms = descr.contains("yahoo.com");
            messages = folder.search(fetchConfig.filter.convertToSearchTerm(useReceivedDateTerms));
        } else {
            // mbox provider claims to provide UIDFolder but the uids are bogus so we treat mboemailstore folders as not uidfolders
            boolean is_uid_folder = (folder instanceof UIDFolder) && !(emailStore instanceof MboxEmailStore);

            if (use_uid_if_available && is_uid_folder) {
                // for uidfolders, we want to update the last seen uid in the FolderInfo
                long uid = archive.getLastUIDForFolder(emailStore.getAccountID(), folder_name());
                if (uid > 0) {
                    messages = ((UIDFolder) folder).getMessagesByUID(uid + 1, UIDFolder.LASTUID);
                    log.info("Archive has already seen this folder: " + descr + " will only fetch messages from uid " + uid + " onwards, " + messages.length + " messages will be incrementally fetched");
                    haveUID = true;
                } else
                    log.info(descr + " is a UIDFolder but not seen before");
            } else
                log.info(descr + " is not a UIDFolder");

            if (!haveUID) {
                log.info("All " + count + " messages in " + descr + " will be fetched");
                //messages = folder.getMessages();

                if (begin_msg_index > 0 && end_msg_index > 0) {
                    // we have to use only specified messages
                    // if there are 8 messages, count = 8, end_msg_index will be 9
                    if (end_msg_index > count + 1)
                        log.warn("Warning: bad end_msg_index " + end_msg_index + " count = " + count); // use the full messages
                    else {
                        int nMessages = end_msg_index - begin_msg_index;
                        Message[] newMessages = new Message[nMessages];
                        for (int i = 0; i < end_msg_index - begin_msg_index; i++)
                            newMessages[i] = folder.getMessage(begin_msg_index + i);//messages[begin_msg_index - 1 + i]; // -1 cos messages array is indexed from 0, but begin_msg_index from 1
                        log.info("total # of messages: " + count + " reduced # of messages: " + newMessages.length);
                        messages = newMessages;
                    }
                }
            }
        }

        return messages;
    }

    /**
     * main fetch+index method
     * The assumptions that the heap is big enough to enough to fit all the messages i the folder is not scalable for larger archive.
     * Instead, we process each message individually.
     * fetchHeaders may be penalised due to multiple requests of fetch?
     * In order to make indexing of large archives possible, fetch of NON-MBOXEmailstrore formats is penalised. It is possible to avoid this by handling MBox and IMAP/POP formats differently.
     */
    public void run() {
        currentStatus = JSONUtils.getStatusJSON("Reading " + folder_name());

        isCancelled = false;
        Thread.currentThread().setName("EmailFetcher");
        nErrors = 0;
        //Message[] messages = null;
        // use_uid is set only if we are reading the whole folder. otherwise we won't use it, and we won't update the highest UID seen for the folder in the archive.
        try {
            //			long t1 = System.currentTimeMillis();
            int nMessages = openFolderAndGetMessageCount();
            log.info("Total number of messages: " + nMessages);

            if (emailStore instanceof MboxEmailStore) {
                // this is a special for mbox'es because we run out of memory if we try to openFolderAndGetMessages()
                // so we process in batches
                //TODO: Ideally, should cap on buffer size rather than on number of messages.
                final int BATCH = 10000;
                int nbatches = nMessages / BATCH;
                nMessagesProcessedSuccess = 0;
                long st = System.currentTimeMillis();
                int b;
                for (b = 0; b < nbatches + 1; b++) {
                    begin_msg_index = b * BATCH + 1;
                    end_msg_index = Math.min((b + 1) * BATCH, nMessages) + 1;
                    log.info("Fetching messages in index [" + begin_msg_index + ", " + end_msg_index + "] batch: " + b + "/" + nbatches + "\nTotal Messages: " + nMessages);
                    Message[] messages = openFolderAndGetMessages();
                    currentStatus = JSONUtils.getStatusJSON("");
                    if (isCancelled)
                        return;
//Check here fore nullablity (??) of message..
                    if (messages!=null && messages.length > 0) {
                        try {
                            if (fetchConfig.downloadMessages) {
                                log.info(nMessages + " messages will be fetched for indexing");
                                fetchAndIndexMessages(folder, messages, begin_msg_index, nMessages);
                            } else {
                                // this is for memory test screening mode.
                                // we create a dummy archive without any real contents
                                for (int i = 0; i < nMessages; i++) {
                                    String unique_id_as_string = Long.toString(i);

                                    // well, we already converted to emaildoc above during removeMessagesAlreadyInArchive
                                    // not a serious perf. concern now, but revisit if needed
                                    EmailDocument ed = convertToEmailDocument((MimeMessage) messages[i], unique_id_as_string); // this messageNum is mostly for debugging, it should not be used for equals etc.
                                    archive.addDocWithoutContents(ed);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Exception trying to fetch messages, results will be incomplete! " + e + "\n" + Util.stackTrace(e));
                        }
                    }
                    log.info("Fetch stats for this fetcher thread: " + stats);
                }
                log.info("Read #" + nMessages + " messages in #" + b + " batches of size: " + BATCH + " in " + (System.currentTimeMillis() - st) + "ms");
            } else {
                // IMAP etc are pretty efficient with lazily populating message objects, so unlike mbox, its ok to use openFolderAndGetMessages() on the entire folder.
                // remember to init the begin/end_msg_index before calling openFolderAndGetMessages
                begin_msg_index = 1;
                end_msg_index = nMessages + 1;
                nMessagesProcessedSuccess = 0;
                Message[] messages = openFolderAndGetMessages();

                long st = System.currentTimeMillis();
                currentStatus = JSONUtils.getStatusJSON("");
                if (isCancelled)
                    return;

                if (messages.length > 0) {
                    try {
                        fetchHeaders(messages); // always fetch headers
                        if (fetchConfig.downloadMessages) {
                            log.info(nMessages + " messages will be fetched for indexing");
                            //we process all the messages together here unlike the case of mstor
                            //hence the begin index is always 0
                            fetchAndIndexMessages(folder, messages, 0, messages.length);
                        } else {
                            // this is for memory test screening mode.
                            // we create a dummy archive without any real contents
                            for (int i = 0; i < nMessages && i < messages.length; i++) {
                                String unique_id_as_string = Long.toString(i);

                                // well, we already converted to emaildoc above during removeMessagesAlreadyInArchive
                                // not a serious perf. concern now, but revisit if needed
                                EmailDocument ed = convertToEmailDocument((MimeMessage) messages[i], unique_id_as_string); // this messageNum is mostly for debugging, it should not be used for equals etc.
                                archive.addDocWithoutContents(ed);
                            }
                        }
                    } catch (Exception e) {
                        Util.print_exception("Exception trying to fetch messages, results will be incomplete! ", e, log);
                    }
                }
                log.info("Read #" + nMessages + " messages in  in " + (System.currentTimeMillis() - st) + "ms");
            }
        } catch (Throwable t) {
            if (t instanceof OutOfMemoryError)
                this.mayHaveRunOutOfMemory = true;
                // this is important, because there could be an out of memory etc over here.

            // if it's not an mbox file at all, it will show up as begin and end msg_indx = 1.
            // so mild warning in that case.
            // however, try to give a big warning in case it is a real mbox file.
            if (begin_msg_index == 1 && end_msg_index == 1)
                log.warn ("An error parsing mbox file " + folder_name() + " (it may not be an mbox file at all)");
            else
                Util.aggressiveWarn(" A major error seems to have occurred! Processing of messages has been aborted for folder " + folder_name() + " messages [" + begin_msg_index + ", " + end_msg_index + ")", 5000, log);
            Util.print_exception(t, log);
        } finally {
            try {
                if (folder != null)
                    folder.close(false);
                if (store != null)
                    store.close();
            } catch (Exception e) {
                Util.print_exception(e);
            }
        }
    }

	/*
	 * code for handling other kinds of headers, e.g. to find location of the
	 * message -- not used right now, but may use in the future.
	 * public void processHeaders(MimeMessage m) throws Exception
	 * {
	 * Address[] froms = m.getFrom();
	 * if (froms == null)
	 * return;
	 * InternetAddress a = (InternetAddress) froms[0];
	 * ContactInfo ci = addressBook.getContactInfoForAddress(a);
	 * Enumeration<Header> e = (Enumeration<Header>) m.getAllHeaders();
	 * String lastReceivedHeader = null;
	 * while (e.hasMoreElements())
	 * {
	 * Header h = e.nextElement();
	 * String n = h.getName();
	 * String v = h.getValue();
	 * // log.info ("header: " + n + " = " + n);
	 * String s = n.toLowerCase();
	 * if ("x-mailer".equals(s) || "user-agent".equals(s))
	 * {
	 * log.warn (m.getFrom()[0] + " --> " + n + " " + v);
	 * ci.addMailer(v);
	 * }
	 * if ("x-originating-ip".equals(s) || "x-yahoo-post-ip".equals(s))
	 * {
	 * log.warn (m.getFrom()[0] + " --> " + n + " " + v);
	 * ci.addIPAddr(v);
	 * }
	 * if ("x-yahoo-profile".equals(s))
	 * log.warn (m.getFrom()[0] + " --> " + n + " " + v);
	 * if ("message-id".equals(s))
	 * {
	 * log.warn("messageID = " + v);
	 * ci.addMessageID(v);
	 * }
	 * if ("received".equals(s) || "x-received".equals(s))
	 * {
	 * lastReceivedHeader = v;
	 * }
	 * }
	 *
	 * // sometimes the headers have an extra ctrl-m at the end, strip it if
	 * this is the case.
	 * if (lastReceivedHeader != null && lastReceivedHeader.endsWith("\r"))
	 * lastReceivedHeader = lastReceivedHeader.substring(0,
	 * lastReceivedHeader.length()-1);
	 *
	 * ci.addLastReceivedHeader(lastReceivedHeader);
	 *
	 * String from = froms[0].toString();
	 *
	 * log.info (from + " lastReceived " + lastReceivedHeader);
	 * if (lastReceivedHeader == null)
	 * log.warn ("WARNING: " + from + " --> no received header!?");
	 * else
	 * {
	 * String ipAddrStr = processLastReceived(lastReceivedHeader);
	 * if (ipAddrStr != null)
	 * {
	 * byte[] ipAddrBytes = Util.parseIPAddress(ipAddrStr);
	 * if (ipAddrBytes != null)
	 * {
	 * // InetAddress ipAddr = InetAddress.getByAddress(ipAddrBytes);
	 * // log.info ("Received: " + locationService.lookupLocation(ipAddr));
	 * }
	 * }
	 * }
	 * }
	 */

    public String toString() {
        return Util.fieldsToString(this);
    }
}
