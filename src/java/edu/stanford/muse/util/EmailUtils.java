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
package edu.stanford.muse.util;

import com.google.common.collect.Multimap;
import edu.stanford.muse.AnnotationManager.AnnotationManager;
import edu.stanford.muse.Config;
import edu.stanford.muse.LabelManager.LabelManager;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.email.*;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.index.*;
import edu.stanford.muse.ie.variants.Variants;

import edu.stanford.muse.webapp.EmailRenderer;
import edu.stanford.muse.webapp.JSPHelper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTimeComparator;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

import static edu.stanford.muse.email.EmailFetcherThread.INVALID_DATE;

public class EmailUtils {
    public static final Logger log = LogManager.getLogger(EmailUtils.class);
    private static org.apache.commons.collections4.map.CaseInsensitiveMap<String, String> dbpedia = null;

    private enum MessageType {
        ONLY_PLAIN, ONLY_HTML, PLAIN_AND_HTML, PLAIN_AND_ATTACHMENT, HTML_AND_ATTACHMENT, PLAIN_AND_HTML_AND_ATTACHMENT
    }

    /**
     * Returns the part before @ in an email address, e.g. hangal@gmail.com => hangal.
     * Returns the full string if the input does not have @, or null if the input is null.
     */
    private static String getAccountNameFromEmailAddress(String email) {
        if (email == null)
            return null;
        int idx = email.indexOf("@");
        return (idx < 0) ? email : email.substring(0, idx);
    }

    /**
     * get a list of possible names, like "First Last" from "First.Last@gmail.com" etc
     */
    public static List<String> parsePossibleNamesFromEmailAddress(String email) {
        List<String> result = new ArrayList<>();
        if (email == null)
            return result;
        String strippedEmail = getAccountNameFromEmailAddress(email);

        // handle addrs like mondy_dana%umich-mts.mailnet@mit-multics.arp, in this case strip out the part after %
        int idx = strippedEmail.indexOf("%");
        if (idx >= 0)
            strippedEmail = strippedEmail.substring(0, idx);

        // 2 sets of splitters, one containing just periods, other just underscores.
        // most people have periods, but at least Dell has underscores
        String[] splitters = new String[]{".", "_"};
        for (String splitter : splitters) {
            StringTokenizer st = new StringTokenizer(strippedEmail, splitter);
            int nTokens = st.countTokens();
            // allow only first.last or first.middle.last
            if (nTokens < 2 || nTokens > 3)
                continue;

            String possibleName = "";
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (Util.hasOnlyDigits(token))
                    return result; // abort immediately if only numbers, we don't want things like 70451.2444@compuserve.com
                possibleName += Util.capitalizeFirstLetter(token) + " "; // optionally we could upper case first letter of each token.
            }
            possibleName = possibleName.trim(); // remove trailing space
            result.add(possibleName);
        }
        return result;
    }

    private static class DBpediaTypes {
        //these are types identified from DBpedia that may contain some predictable tokens and omitting types with any possible tokens like TVShows and Bands
        //also omitting types that are not very different from other types like, Company and AutomobileEngine|Device
    }

    /**
     * best effort to toString something about the given message.
     * use only for diagnostics, not for user-visible messages.
     * treads defensively, this can be called to report on a badly formatted message.
     */
    public static String formatMessageHeader(MimeMessage m) {
        StringBuilder sb = new StringBuilder();
        sb.append("To: ");
        if (m == null) {
            log.warn("Trying to format null message!?");
            return "Null message";
        }
        try {
            Address[] tos = m.getAllRecipients();
            if (tos != null)
                for (Address a : tos)
                    sb.append(a.toString()).append(" ");
            sb.append("\n");
        } catch (Exception e) {
            Util.print_exception(e, log);
        }

        sb.append("From: ");
        try {
            Address[] froms = m.getFrom();
            if (froms != null)
                for (Address a : froms)
                    sb.append(a.toString()).append(" ");
            sb.append("\n");
        } catch (Exception e) {
            Util.print_exception(e, log);
        }

        try {
            sb.append("Subject: ").append(m.getSubject());
            sb.append("Message-ID: ").append(m.getMessageID());
        } catch (Exception e) {
            Util.print_exception(e, log);
        }

        return sb.toString();
    }

    // clean the dates
    public static void cleanDates(Collection<? extends Document> docs) {
        Date now = new Date();
        long time = now.getTime() + 1000 * 24 * 60 * 60L; // allow 1 day after current time
        Date cutoff = new Date(time);

        Date earliestDate = new Date();
        Date latestDate = null;

        // compute earliest and latest dates. ignore dates after cutoff
        for (Document d : docs) {
            if (d instanceof DatedDocument) {
                DatedDocument dd = (DatedDocument) d;
                if (dd.date == null || dd.date.after(cutoff))
                    continue;

                if (dd.date.before(earliestDate))
                    earliestDate = dd.date;

                if (latestDate == null)
                    latestDate = dd.date;
                else if (dd.date.after(latestDate))
                    latestDate = dd.date;
            }
        }

        for (Document d : docs) {
            if (d instanceof DatedDocument) {
                DatedDocument dd = (DatedDocument) d;
                if (dd.date == null)
                    dd.date = earliestDate;
                if (dd.date.after(cutoff)) {
                    log.warn("Warning: date is beyond current time " + CalendarUtil.formatDateForDisplay(dd.date) + ", changing to last reasonable date, " + CalendarUtil.formatDateForDisplay(latestDate));
                    dd.date = latestDate;
                }
            }
        }
    }

    // strips the Re: of subjects
    private static String normalizedSubject(String subject) {
        String originalSubject = subject.toLowerCase();
        String result = originalSubject;
        // Strip all variations of re: Re: RE: etc... by converting subject to lowercase
        // but don't lose case on original subject
        subject = subject.toLowerCase();

        if (subject.startsWith("re ") && subject.length() > "re ".length())
            result = originalSubject.substring("re ".length());
        if (subject.startsWith("re: ") && subject.length() > "re: ".length())
            result = originalSubject.substring("re: ".length());
        return result;
    }

    // removes re: fwd etc from a subject line
    public static String cleanupSubjectLine(String subject) {
        if (subject == null)
            return null;
        // first strip mailing list name if any, at the beginning of the subject,
        // otherwise terms like [prpl-devel] appear very frequently
        StringTokenizer st = new StringTokenizer(subject);
        String firstToken = "";

        // firstToken is empty or the first non [mailing-list] first token
        while (st.hasMoreTokens()) {
            firstToken = st.nextToken();
            String f = firstToken.toLowerCase();
            // these are the common patterns i've seen in subject lines
            if ("re".equals(f) || "re:".equals(f) || "fwd".equals(f) || "fwd:".equals(f) || "[fwd".equals(f) || "[fwd:".equals(f))
                continue;
            // strip mailing list stuff as well
            if (f.startsWith("[") && f.endsWith("]") && f.length() < Indexer.MAX_MAILING_LIST_NAME_LENGTH)
                continue;

            break;
        }

        // firstToken is empty or the first non [mailing-list], non re/fwd/etc token
        String result = firstToken + " ";
        while (st.hasMoreTokens())
            result += st.nextToken() + " ";

        return result;
    }

    /**
     * returns just the first message for each thread
     */
    public static List<EmailDocument> threadHeaders(List<EmailDocument> emails) {
        // map maps normalized subject to a list of threads that all have that thread
        Map<String, List<EmailThread>> map = new LinkedHashMap<>();

        for (EmailDocument email : emails) {
            String normalizedSubject = normalizedSubject(email.getSubjectWithoutTitle());

            List<EmailThread> threads = map.computeIfAbsent(normalizedSubject, k -> new ArrayList<>());

            EmailThread existingThread = null;
            for (EmailThread thread : threads) {
                if (thread.belongsToThisThread(email)) {
                    existingThread = thread;
                    break;
                }
            }

            if (existingThread != null)
                existingThread.addMessage(email);
            else
                threads.add(new EmailThread(email, normalizedSubject)); // new emailthread based on this message
        }

        System.out.println(map.keySet().size() + " unique subjects");

        List<EmailDocument> result = new ArrayList<>();

        for (String normalizedSubject : map.keySet()) {
            List<EmailThread> threads = map.get(normalizedSubject);
            System.out.print("Subject: " + normalizedSubject.replaceAll("\n", "") + ": ");
            System.out.print(threads.size() + " thread(s), : ");
            for (EmailThread et : threads) {
                System.out.print(et.size() + " ");
                result.add(et.emails.get(0));
            }
            System.out.println();
        }

        System.out.println(emails.size() + " emails reduced to " + result.size() + " threads");

        return result;
    }

    //	From - Tue Sep 29 11:38:30 2009
    private static final SimpleDateFormat sdf1 = new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy");
    // Date: Wed, 2 Apr 2003 11:53:17 -0800 (PST)
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss");
    public static Random rng = new Random(0);

    static {
        // if file separator is /, then newline must be \n, otherwise \r\n
        byte b[] = "/".equals(File.separator) ? new byte[]{(byte) 10} : new byte[]{(byte) 13, (byte) 10};
        Base64 base64encoder = new Base64(76, b);
    }

    private static String printHeaderToMbox(Archive archive, EmailDocument ed, PrintWriter mbox, LabelManager labelManager, AnnotationManager annotationManager) {
        /* http://www.ietf.org/rfc/rfc1521.txt is the official ref. */
        String frontier = "";
        Date d = ed.date != null ? ed.date : new Date();
        String s = sdf1.format(d);
        mbox.println("From - " + s);
        mbox.println("X-ePADD-Folder: " + ed.folderName);
        if (!Util.nullOrEmpty(annotationManager.getAnnotation(ed.getUniqueId()))) {
            String comment = annotationManager.getAnnotation(ed.getUniqueId());
            comment = comment.replaceAll("\n", " ");
            comment = comment.replaceAll("\r", " ");
            mbox.println("X-ePADD-Annotation: " + comment);
        }

        // print labels
        {
            String labelDescription = ""; // default, if there are no labels, we'll always output it.
            Set<String> labelsIDsForThisDoc = labelManager.getLabelIDs(ed.getUniqueId());

            if (!Util.nullOrEmpty(labelsIDsForThisDoc)) {
                Set<String> labelsForThisDoc = labelsIDsForThisDoc.stream().map(id -> labelManager.getLabel(id).getLabelName()).collect(Collectors.toSet());
                labelDescription = Util.join(labelsForThisDoc, ";");
            }
            mbox.println("X-ePADD-Labels: " + labelDescription);
        }
        List<String> headerList = archive.indexer.getOriginalHeaders(ed);
        for (String headerString : headerList) {
            mbox.println(headerString);
            if (headerString.toLowerCase().startsWith("content-type:")) {
                Pattern p = Pattern.compile("Type:.*;\\r?\\n?.*boundary=\\\"?([^\\\"]*)\\\"?");
                Matcher m = p.matcher(headerString);

                while (m.find()) {
                    //There can be more than one sequence of upper-case letter followed by lower-case
                    //e.g. DaVinci
                    int n = m.groupCount();
                    if (m.groupCount() > 0) {
                        frontier = m.group(1);
                    }
                }
            }
        }
        mbox.println();

        return frontier;
    }

    /**
     * this is an export for other tools to process the message text or names. Writes files called <n>.fill and <n>.names in the givne dir
     */
    public static void dumpMessagesAndNamesToDir(Archive archive, Collection<EmailDocument> docs, String dir) throws ClassCastException {
        File f = new File(dir);
        f.mkdirs();
        int i = 0;
        for (EmailDocument ed : docs) {
            try {
                String m = ed.description + "\n\n" + archive.getContents(ed, false /* full message */);
                PrintWriter pw = new PrintWriter(new FileOutputStream(dir + File.separatorChar + i + ".full"));
                pw.println(m);
                pw.close();

                Set<String> allEntities = new LinkedHashSet<>();
                allEntities.addAll(Arrays.stream(archive.getAllNamesInDoc(ed, true)).map(n -> n.text).collect(Collectors.toSet()));
                allEntities.addAll(Arrays.stream(archive.getAllNamesInDoc(ed, false)).map(n -> n.text).collect(Collectors.toSet()));

                pw = new PrintWriter(new FileOutputStream(dir + File.separatorChar + i + ".names"));

                String s = "";
                for (String e : allEntities) {
                    e = e.toLowerCase();
                    e = e.replaceAll(" ", "_");
                    s += e + " ";
                }
                pw.println(s);
                pw.close();
            } catch (Exception e) {
                log.warn("Unable to save contents of message: " + ed);
                Util.print_exception(e, log);
            }
            i++;
        }
    }

    /*
     * mbox files are generated in mbox format for email preservation need in PREMIS. Each of them would reflect the most up-to-dated email contents and attachments according to Lucene Documents.
     *
     */
    public static void printNormalizedMbox(Archive archive, EmailDocument ed, PrintWriter mbox, BlobStore blobStore, boolean stripQuoted) {

    }

    /*
     * header is printed in mbox format, then each of the given contents sequentially
     * if blobStore is null, attachments are not printed. could make this better by allowing text/html attachments.
     */
    public static void printToMbox(Archive archive, EmailDocument ed, PrintWriter mbox, BlobStore blobStore, boolean stripQuoted) {
        String contents;
        String topLevelFrontier = "";
        topLevelFrontier = printHeaderToMbox(archive, ed, mbox, archive.getLabelManager(), archive.getAnnotationManager());
        contents = archive.getContents(ed, stripQuoted);
        String htmlText = archive.indexer.getTextHtml(ed);
        List<Blob> attachments = null;
        if (ed != null) {
            attachments = ed.attachments;
        }
        boolean hasAttachments = !Util.nullOrEmpty(attachments) && blobStore != null;
        boolean hasHtmlPart = (htmlText != null && !htmlText.isEmpty());
        boolean hasHtmlAndPlain = (ed.hasPlainTextPart() && hasHtmlPart);
        MessageType messageType = getMessageType(ed, hasAttachments, hasHtmlPart);

        if (messageType != MessageType.ONLY_HTML && messageType != MessageType.ONLY_PLAIN) {
            mbox.println();
            mbox.println("This is a multi-part message in MIME format.");
            printStartFrontier(topLevelFrontier, mbox);
        }
        switch (messageType) {
            case ONLY_PLAIN:
                printMessagePart(contents, mbox);
                break;
            case ONLY_HTML:
                printMessagePart(htmlText, mbox);
                break;
            case PLAIN_AND_HTML:
                printPlainAndHtml(mbox, contents, htmlText, topLevelFrontier);
                break;
            case PLAIN_AND_ATTACHMENT:
                printPlainAndAttachments(mbox, contents, topLevelFrontier, blobStore, attachments);
                break;
            case HTML_AND_ATTACHMENT:
                printHtmlAndAttachments(mbox, htmlText, topLevelFrontier, blobStore, attachments);
                break;
            case PLAIN_AND_HTML_AND_ATTACHMENT:
                printPlainAndHtmlAndAttachments(mbox, contents, htmlText, topLevelFrontier, blobStore, attachments);
                break;
        }
        mbox.println();
    }

    private static MessageType getMessageType(EmailDocument ed, boolean hasAttachments, boolean hasHtmlPart) {
        if (!hasAttachments) {
            if (!hasHtmlPart) {
                return MessageType.ONLY_PLAIN;
            } else if (!ed.hasPlainTextPart()) {
                return MessageType.ONLY_HTML;
            } else {
                return MessageType.PLAIN_AND_HTML;
            }
        } else {
            if (!hasHtmlPart) {
                return MessageType.PLAIN_AND_ATTACHMENT;
            }
            if (!ed.hasPlainTextPart()) {
                return MessageType.HTML_AND_ATTACHMENT;
            } else {
                return MessageType.PLAIN_AND_HTML_AND_ATTACHMENT;
            }
        }
    }

    private static void printEndFrontier(String frontier, PrintWriter mbox) {
        mbox.println();
        mbox.println("--" + frontier + "--");
    }

    private static void printStartFrontier(String frontier, PrintWriter mbox) {
        mbox.println();
        mbox.println("--" + frontier);
    }

    private static void printPlainAndHtml(PrintWriter mbox, String content, String htmlText, String frontier) {
        // Header should contain content-type alternative
        printPlainTextString(mbox, content);
        printStartFrontier(frontier, mbox);
        printHtmlString(mbox, htmlText);
        printEndFrontier(frontier, mbox);
        mbox.println();
        //			mbox.println("--" + frontier + "--");
        // probably need to fix: other types of charset, encodings
    }

    private static void printPlainAndAttachments(PrintWriter mbox, String content, String frontier, BlobStore blobStore, List<Blob> attachments) {
        // Header should contain content-type alternative
        printPlainTextString(mbox, content);
        printAttachments(frontier, mbox, blobStore, attachments);
        printEndFrontier(frontier, mbox);
    }

    private static void printPlainAndHtmlAndAttachments(PrintWriter mbox, String content, String htmlText, String frontier, BlobStore blobStore, List<Blob> attachments) {
        String frontier2 = frontier + "_2nd";
        // Header should contain content-type mixed
        mbox.println("Content-Type: multipart/alternative; boundary=\"" + frontier2 + "\"");
        mbox.println();
        printStartFrontier(frontier2, mbox);
        printPlainTextString(mbox, content);
        printStartFrontier(frontier2, mbox);
        printHtmlString(mbox, htmlText);
        printEndFrontier(frontier2, mbox);
        printAttachments(frontier, mbox, blobStore, attachments);
        printEndFrontier(frontier, mbox);
    }

    private static void printHtmlAndAttachments(PrintWriter mbox, String content, String frontier, BlobStore blobStore, List<Blob> attachments) {
        // Header should contain content-type alternative
        printHtmlString(mbox, content);
        printAttachments(frontier, mbox, blobStore, attachments);
        printEndFrontier(frontier, mbox);
    }

    private static void printHtmlString(PrintWriter mbox, String htmlText) {
        mbox.println("Content-Type: text/html;charset=\"UTF-8\"");
        mbox.println("Content-Transfer-Encoding: quoted-printable\n");
        printEncoded(mbox, htmlText);
    }

    private static void printPlainTextString(PrintWriter mbox, String content) {
        mbox.println("Content-Type: text/plain; charset=\"UTF-8\"");
        mbox.println("Content-Transfer-Encoding: quoted-printable");
        mbox.println();
        printEncoded(mbox, content);
    }

    private static void printEncoded(PrintWriter mbox, String content) {
        String encoded = "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            OutputStream encodedOut = javax.mail.internet.MimeUtility.encode(baos, "quoted-printable");
            encodedOut.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | MessagingException e) {
            //.....
        }
        encoded = baos.toString();
        mbox.print(encoded);
    }

    private static void printAttachments(String frontier, PrintWriter mbox, BlobStore blobStore, List<Blob> attachments) {
        byte[] encodedBytes;
        for (Blob b : attachments) {
            printStartFrontier(frontier, mbox);
            mbox.println("Content-type: " + b.contentType);
            if (b.getContentTransferEncoding() != Blob.ContentTransferEncoding.MISSING) {
                if (b.getContentTransferEncoding() == Blob.ContentTransferEncoding.NA) {
                    mbox.println("Content-transfer-encoding: base64");
                } else {
                    mbox.println("Content-transfer-encoding: " + b.getContentTransferEncoding().toString());
                }
            }
            mbox.println("Content-Disposition: attachment;filename=\"" + blobStore.full_filename_normalized(b, false) + "\"\n");
            byte bytes[] = new byte[0];
            try {
                bytes = blobStore.getDataBytes(b);
                switch (b.getContentTransferEncoding()) {
                    //From https://www.w3.org/Protocols/rfc1341/5_Content-Transfer-Encoding.html:
                    // The values "8bit", "7bit", and "binary" all imply that NO encoding has been performed. However, they are potentially useful as indications ...
                    case BINARY:
                    case BIT8:
                    case BIT7:
                    case MISSING:
                    default:
                        encodedBytes = bytes;
                        break;

                    case BASE64:
                    case NA:
                        if (b.isRfc822()) {
                            //Sometimes rfc822 messages (emails as attachment) have encoding declared as base64
                            //although the text is human readable. We write the text as 8bit, so it is (hopefully) the
                            //same as in the original Mbox file. We keep the line 'Content-transfer-encoding: base64'
                            //just to keep the file in its original state.
                            encodedBytes = bytes;
                        } else {
                            encodedBytes = Base64.encodeBase64(bytes, true);
                        }
                        break;
                }

                for (byte by : encodedBytes)
                    mbox.print((char) by);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static void printMessagePart(String contents, PrintWriter mbox) {
        // content type is set in header already
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            OutputStream encodedOut = MimeUtility.encode(baos, "quoted-printable");
            encodedOut.write(contents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | MessagingException e) {
            e.printStackTrace();

        }
        String encoded = baos.toString();
        int i;
        mbox.println(encoded);
    }

    /**
     * gets the part before @ in the address
     */
    public static String getLoginFromEmailAddress(String emailAddress) {
        if (emailAddress == null)
            return null;
        int idx = emailAddress.indexOf("@");
        if (idx < 0)
            return emailAddress;
        return emailAddress.substring(0, idx);
    }

    private static final Pattern parensPattern = Pattern.compile("\\(.*\\)");
    private static final Pattern sqBracketsPattern = Pattern.compile("\\[.*\\]");

    /**
     * Cleans the given person name, by stripping whitespace at either end, normalizes spaces, so exactly 1 space between tokens.
     * The goal of this method is only to clean up messiness in person names read from mbox files.
     * It is mostly a simple, cosmetic (and early) cleanup for what is read from the mbox.
     * Note: this is more for display, not a normalization!
     * Note: May return null!
     * returns null if we judge it not a valid name or has a banned word/string or is a single word name
     * retains case of the input as is.
     * returns same case
     * earlier it used to return null if name only had a single word, but now it will just clean that word and return.
     */
    public static String cleanPersonName(String name) {
        if (name == null)
            return null;

        // be careful with case, we want name to remain in its original case

        // strip whitespace and quotes
        {
            name = name.trim();
            // a surprising number of names in email headers start and end with a single quote
            // if so, strip it.
            if (name.length() >= 2 && name.startsWith("'") && name.endsWith("'"))
                name = name.substring(1, name.length() - 1);
            if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\""))
                name = name.substring(1, name.length() - 1);
        }

        // if it has no alphabetical characters at all, return null
        {
            boolean allNonAlpha = true;
            for (char c : name.toCharArray()) {
                if (Character.isAlphabetic(c)) {
                    allNonAlpha = false;
                    break;
                }
            }

            // all non-alphabet? return nothing, because its likely a junk name like "(" or "((" (yes, we see plenty of those!)
            if (allNonAlpha)
                return null;
        }

        // Strip stuff inside parens, e.g. sometimes names are like: foo bar (at home) or foo bar [some Dept]
        // in either case, we want to keep just foo bar and strip the rest
        {
            Matcher m1 = parensPattern.matcher(name);
            name = m1.replaceAll("");
            Matcher m2 = sqBracketsPattern.matcher(name);
            name = m2.replaceAll("");
            // trim again if needed
            name = name.trim();
        }

        // check if an email addr accidentally got passed in, if so, don't do any of the rest
        // sometimes the mbox parser may read
        if (name.contains("@")) // an email addr, not a real name -- we dunno what's happening, just return it as is, just lowercasing it.
            return name.toLowerCase();

        // normalize spaces
        // return null if name has banned words - e.g. ben s's email has different people with the "name" (IPM Return requested)
        String result = "";
        for (String t : Util.tokenize(name)) {
            if (DictUtils.bannedWordsInPeopleNames.contains(t.toLowerCase())) {
                if (log.isDebugEnabled())
                    log.debug("Will not consider name (because it has a banned word): " + name);
                return null;
            }
            result += t + " ";
        }

        result = result.trim(); // very important, we've added a space after the last token above

        String lowerCaseName = name.toLowerCase();
        for (String bannedString : DictUtils.bannedStringsInPeopleNames)
            if (lowerCaseName.contains(bannedString)) {
                if (log.isDebugEnabled()) {
                    log.debug("Will not consider name due to banned string: " + name + " due to string: " + bannedString);
                }
                return null;
            }

    	/*
		if (Util.tokenize(name).size() < 2) {
			return null; // single word names should not be considered for merging
		}
		*/

        return result;
    }

    // removes dups from the input list
    public static List<String> removeMailingLists(List<String> in) {
        List<String> result = new ArrayList<>();
        for (String s : in) {
            s = s.toLowerCase();
            if (s.contains("@yahoogroups") || s.contains("@googlegroups") || s.contains("@lists.") || s.contains("@mailman") || s.contains("no-reply") || s.contains("do-not-reply")) {
                log.debug("Dropping mailing list or junk address: " + s);
                continue;
            }
            result.add(s);
        }
        return result;
    }

    /**
     * try to get the last name
     */
    private static String getLastName(String fullName) {
        StringTokenizer st = new StringTokenizer(fullName);
        String lastToken = null;
        int nRealTokens = 0;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if ("jr".equalsIgnoreCase(token))
                continue;
            // i don't actually know anyone with these suffixes, but in honor william gates III
            if ("i".equalsIgnoreCase(token) || "ii".equalsIgnoreCase(token) || "iii".equalsIgnoreCase(token) || "iv".equalsIgnoreCase(token) || "v".equalsIgnoreCase(token))
                continue;
            lastToken = token;
            nRealTokens++;
        }

        if (nRealTokens < 2)
            return null; // not two names
        if (lastToken.length() < 2)
            return null; // don't know what is happening
        return lastToken;
    }

    private final static Set<String> secondLevelDomains = new LinkedHashSet<>();
    private final static String[] secondLevelDomainsArray = new String[]{"ac", "co"};
    private final static String[] serviceProvidersArray = new String[]{"hotmail", "gmail", "yahoo", "live", "msn", "pacbell", "vsnl", "comcast", "rediffmail"};
    private final static Set<String> serviceProviders = new LinkedHashSet<>();

    static {
        Collections.addAll(secondLevelDomains, secondLevelDomainsArray);
        Collections.addAll(serviceProviders, serviceProvidersArray);
    }

    /**
     * try to get the last name
     */
    private static String getOrg(String email) {
        if (!email.contains("@"))
            return null;

        StringTokenizer st = new StringTokenizer(email, "@. !");
        List<String> tokens = new ArrayList<>();

        while (st.hasMoreTokens())
            tokens.add(st.nextToken());
        if (tokens.size() < 3)
            return null; // must have at least 3 tokens : a@b.org
        String org = tokens.get(tokens.size() - 2).toLowerCase();
        if (secondLevelDomains.contains(org))
            org = tokens.get(tokens.size() - 3);
        if (serviceProviders.contains(org))
            return null;

        return org;
    }

    public static Pair<Date, Date> getFirstLast(Collection<? extends DatedDocument> allDocs) {
        return getFirstLast(allDocs, true /* ignore invalid dates */);
    }

    // compute the begin and end date of the corpus
    public static Pair<Date, Date> getFirstLast(Collection<? extends DatedDocument> allDocs, boolean ignoreInvalidDates) {
        // compute the begin and end date of the corpus
        Date first = null;
        Date last = null;

        if (allDocs == null)
            return null;

        for (DatedDocument ed : allDocs) {
            Date d = ed.date;
            if (d == null) {
                // drop this $ed$
                EmailUtils.log.warn("Warning: null date on email: " + ed.getHeader());
                continue;
            }

            // ignore invalid date if asked
            if (ignoreInvalidDates)
                if (DateTimeComparator.getDateOnlyInstance().compare(d, INVALID_DATE) == 0 || ed.hackyDate)
                    continue;//Why did we use this comparator instead of equal of Date? Because even if dates were same cdate field was different hence January 1 1960 was considered as different in all of them.//Addding the support that on browse-top page the range should not consider the unreadable dates where ePADD by default puts something like 1 January 1960

            if (first == null || d.before(first))
                first = d;
            if (last == null || d.after(last))
                last = d;
        }

        return new Pair<>(first, last);
    }

    public static <T extends Comparable<? super T>> List<T> removeDupsAndSort(List<T> docs) {
        log.info("-----------------------Detecting duplicates-------------------");
        Set<T> set = new LinkedHashSet<>();
        set.addAll(docs);

        // maintain a map so when we find a duplicate, we can toString both the dup and the original
        Map<T, T> map = new LinkedHashMap<>();
        for (T ed : docs) {
            if (map.get(ed) != null)
                log.info("Duplicate messages:\n\t" + ed + "\n\t" + map.get(ed));
            map.put(ed, ed);
        }

        List<T> result = new ArrayList<>();
        result.addAll(set);
        Collections.sort(result);
        //		for (EmailDocument ed: result)
        //			System.out.println ("doc: " + ed);

        log.info("Removed duplicates from " + docs.size() + " messages: " + (docs.size() - result.size()) + " removed, " + result.size() + " left");

        return result;
    }

    public static List<LinkInfo> getLinksForDocs(Collection<? extends Document> ds) {
        // extract links from the given docs
        List<LinkInfo> links = new ArrayList<>();
        if (ds == null)
            return links;

        for (Document d : ds)
            if (d.links != null)
                links.addAll(d.links);
        return links;
    }

    public static int countAttachmentsInDocs(Collection<EmailDocument> docs) {
        int count = 0;
        if (docs == null)
            return 0;
        for (EmailDocument ed : docs)
            if (ed.attachments != null)
                count += ed.attachments.size();
        return count;
    }

	/*public static int countImageAttachmentsInDocs(Collection<EmailDocument> docs)
	{
		int count = 0;
		if (docs == null)
			return 0;
		for (EmailDocument ed : docs)
			if (ed.attachments != null)
				for (Blob b : ed.attachments)
					if (Util.is_image_filename(archive.getBlobStore().get_URL_Normalized(b);)) // consider looking at b.contentType as well
						count++;
		return count;
	}*/

	/*public static int countDocumentAttachmentsInDocs(Collection<EmailDocument> docs)
	{
		int count = 0;
		if (docs == null)
			return 0;
		for (EmailDocument ed : docs)
			if (ed.attachments != null)
				for (Blob b : ed.attachments)
					if (Util.is_doc_filename(archive.getBlobStore().get_URL_Normalized(b);)) // consider looking at b.contentType as well
						count++;
		return count;
	}*/

    public static int getMessageCount(List<Pair<String, Integer>> foldersAndCounts) {
        int totalMessageCount = 0;
        for (Pair<String, Integer> p : foldersAndCounts)
            if (!"[Gmail]/All Mail".equals(p.getFirst())) // don't count special case of gmail/all mail
                totalMessageCount += p.getSecond();
        return totalMessageCount;
    }

    /**
     * This method returns the start year and the end year for all the messages in docs. A related method is
     *
     * @param docs
     * @return
     */
    public static Pair<Integer, Integer> getStartEndYear(Collection<Document> docs) {
        List<Date> dates = EmailUtils.datesForDocs((Collection) docs);
        Calendar calendear = Calendar.getInstance();
        dates.sort(Date::compareTo);
        calendear.setTime(dates.get(0));//to get the starting year
        int startYear = calendear.get(Calendar.YEAR);
        calendear.setTime(dates.get(dates.size() - 1));//to get the last year
        int lastYear = calendear.get(Calendar.YEAR);
        return new Pair(startYear, lastYear);
    }

    /*
    Returns a list of pairs (in sorted order) listing for each year the number of attachments. There is a catch though to support putting the undated attachments at the end
    on attachment viewer window. If one of the year in the returned list corresponds to 1960 (undated messages) then put it at the end of the list (second returned argument of the
    following function).
     */
    public static Pair<Boolean, List<Pair<Integer, Integer>>> getStartEndYearOfAttachments(Multimap<String, String> params, Collection<Document> docs) {
        Pair<Boolean, Map<Integer, Integer>> yearWiseAttachments = EmailUtils.getYearWiseAttachments(docs, params);
        boolean isHacky = yearWiseAttachments.first;

        //Iterate over map and make the start year as the oldest year with non-zero attachments.
        //Similarly make the end year as the latest year with non-zero attachments.
        //get the years in sorted order.
        List<Integer> years = new ArrayList<>(yearWiseAttachments.second.keySet());
        Collections.sort(years);
        int startYear = -1;
        int endYear = -1;
        for (Integer year : years) {
            if (yearWiseAttachments.second.get(year) != 0) {

                startYear = year;
                break;
            }
        }
        //Now iterate in reverse order to remove years with zero attachments from the end.
        Collections.reverse(years);
        for (Integer year : years) {
            if (yearWiseAttachments.second.get(year) != 0) {
                endYear = year;
                break;
            }
        }
        List<Pair<Integer, Integer>> result = new LinkedList<>();
        for (int j = startYear; j <= endYear; j++) {
            if (isHacky && j == 1960) {
                //don't put it in result right now. Add it at the end after finishing this loop.
                continue;
            }
            if (yearWiseAttachments.second.get(j) != null && yearWiseAttachments.second.get(j) != 0)
                result.add(new Pair(j, yearWiseAttachments.second.get(j)));
        }
        //Now put the data for 1960 year at the end (only if such an year was present in the list earlier - which means isHacky is true)
        if (isHacky)
            result.add(new Pair(1960, yearWiseAttachments.second.get(1960)));
        return new Pair(isHacky, result);
    }

    /**
     * This method returns the Year wise attachment counts for all the emails in the set docs provided that the attachment type is of interest (as passed
     * by the front end in the query parameter params). If params is null then this information is passed for all the attachments present in docs.
     *
     * @param docs
     * @param params
     * @return two entries. Second variable is map of year and the count of attachments in that year and the first boolean variable denotes if in that map there is an year (1960)
     * which corresponds to the notion of hacky date (1 January 1960)
     */
    public static Pair<Boolean, Map<Integer, Integer>> getYearWiseAttachments(Collection<Document> docs, Multimap<String, String> params) {
        String archiveID = JSPHelper.getParam(params, "archiveID");
        Archive archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        boolean isHacky = false;
        Pattern pattern = null;
        try {
            pattern = Pattern.compile(EmailRenderer.EXCLUDED_EXT);
        } catch (Exception e) {
            Util.report_exception(e);
        }
        Set<String> attachmentTypes = null;
        if (params != null)
            attachmentTypes = IndexUtils.getAttachmentExtensionsOfInterest(params);
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (Document d : docs) {
            EmailDocument ed = (EmailDocument) d;
            //if ed has nonzero attachments then update that number for the year of this email.
            if (ed.attachments.size() != 0) {

                //get year of the email.
                Calendar calendear = Calendar.getInstance();
                calendear.setTime(ed.date);//to get the starting year
                int startYear = calendear.get(Calendar.YEAR);
                if (!result.containsKey(startYear)) {
                    result.put(startYear, 0);
                }
                //Don't put the count of all attachments but only those whose extension type is present in the set attachmentTypes (if it is non-null)
                List<Blob> attachments = ed.attachments;
                int count = 0;
                if (attachments != null)
                    for (Blob b : attachments) {
                        String ext = Util.getExtension(archive.getBlobStore().get_URL_Normalized(b));
                        if (ext == null)
                            ext = "Unidentified";
                        ext = ext.toLowerCase();
                        //Exclude any attachment whose extension is of the form EXCLUDED_EXT
                        //or whose extension is not of interest.. [because it was not passed in attachmentType or attachmentExtension query on input.]

                        if (pattern.matcher(ext).find()) {
                            continue;//don't consider any attachment that has extension of the form [0-9]+
                        }

                        if (attachmentTypes != null && !attachmentTypes.contains(ext))
                            continue;

                        count++;

                    }
                //add count to the map.
                int updatedval = result.get(startYear) + count;
                //if count is nonzero and this message has hacky date then set the variable isHacky to true.
                if (count != 0 && ed.hackyDate)
                    isHacky = true;
                result.put(startYear, updatedval);

            }

        }
        return new Pair(isHacky, result);
    }

    public static List<Date> datesForDocs(Collection<? extends DatedDocument> c) {
        List<Date> result = new ArrayList<>();
        for (DatedDocument d : c) {
            //Do not add hacky dates..
            if (!d.hackyDate)
                result.add(d.date);

        }
        return result;
    }

    public static void maskEmailDomain(Collection<EmailDocument> docs, AddressBook ab) {
        ab.maskEmailDomain();
        for (EmailDocument e : docs) {
            e.maskEmailDomain(ab);
        }
    }

    /**
     * given a set of emailAddress's, returns a map of email address -> docs containing it, from within the given docs.
     * return value also contains email addresses with 0 hits in the archive
     * emailAddress should all be lower case.
     */
    public static Map<String, Set<Document>> getDocsForEAs(Collection<Document> docs, Set<String> emailAddresses) {
        Map<String, Set<Document>> map = new LinkedHashMap<>();
        if (emailAddresses == null)
            return map;

        for (String email : emailAddresses)
            map.put(email, new LinkedHashSet<>());

        for (Document doc : docs) {
            if (!(doc instanceof EmailDocument))
                continue;
            EmailDocument ed = (EmailDocument) doc;
            List<String> docAddrs = ed.getAllAddrs();
            for (String addr : docAddrs) {
                String cAddr = addr.toLowerCase(); // canonical addr
                if (emailAddresses.contains(cAddr)) {
                    Set<Document> set = map.get(cAddr); // can't be null as we've already created a hash entry for each address
                    set.add(doc);
                }
            }
        }

        return map;
    }

    /**
     * little util method get an array of all own addrs, given 1 addr and some alternate ones.
     * alternateaddrs could have multiple addrs, separated by whitespace or commas
     * either of the inputs could be null
     */
    public static Set<String> parseAlternateEmailAddrs(String alternateAddrs) {
        Set<String> result = new LinkedHashSet<>();
        if (Util.nullOrEmpty(alternateAddrs))
            return result;

        StringTokenizer st = new StringTokenizer(alternateAddrs, "\t ,");
        while (st.hasMoreTokens())
            result.add(st.nextToken().toLowerCase());

        // log own addrs
        StringBuilder sb = new StringBuilder();
        for (String s : result)
            sb.append(s).append(" ");
        AddressBook.log.info(result.size() + " own email addresses: " + sb);
        return result;
    }

    /**
     * normalizeNewlines should already have been called on text
     */
    public static String getOriginalContent(String text) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader br = new BufferedReader(new StringReader(text));

        // stopper for the tokenize when we meet a line that needs to be ignored
        String stopper = " . ";

        // we'll maintain line and nextLine for lookahead. needed e.g. for the "on .... wrote:" detection below
        String originalLine;
        String nextLine = br.readLine();

        while (true) {
            originalLine = nextLine;
            if (originalLine == null)
                break;
            nextLine = br.readLine();

            /*
             * Yahoo replies look like this. they don't use the quote character.
             * --
             * This is another test. Another president's name is Barack Obama.
             * ________________________________
             * From: Sudheendra Hangal <hangal@cs.stanford.edu>
             * To: sd sdd <s_hangal@yahoo.com>
             * etc.
             * --
             * so lets eliminate everything after a line that goes ________________________________
             * i.e. at least 20 '_' chars, and the following line has a colon
             */

            // do all checking ops on effective line, but don't modify original line, because we want to use it verbatim in the result.
            // effectiveLine is taken before replacing everything after a >, because we may want to look for a marker like "forwarded message" after a > as well.
            String effectiveLine = originalLine.trim();

            // nuke everything after >. this is the only modification to the original line
            originalLine = originalLine.replaceAll("^\\s*>.*$", stopper); //

            // now a series of checks to stop copying of the input to result
            if (effectiveLine.length() > 20 && Util.hasOnlyOneChar(effectiveLine, '_') && nextLine != null && nextLine.indexOf(":") > 0)
                break;

            // eliminate everything after the line "On Wed, Jul 3, 2013 at 2:06 PM, Sudheendra Hangal <hangal@gmail.com> wrote:"
            // however, sometimes this line has wrapped around on two lines, so we use the nextLine lookahead if it's present
            if ((effectiveLine.startsWith("On ") && effectiveLine.endsWith("wrote:")) ||
                    (effectiveLine.startsWith("On ") && effectiveLine.length() < 80 && (nextLine != null && nextLine.trim().endsWith("wrote:")))) {
                break;
            }

            // look for forward separator
            String lowercaseLine = effectiveLine.toLowerCase();
            if (lowercaseLine.startsWith("Begin forwarded message:"))
                break;
            if (effectiveLine.startsWith("---") && effectiveLine.endsWith("---") && (lowercaseLine.contains("forwarded message") || lowercaseLine.contains("original message")))
                break;

            result.append(originalLine);

            // be careful, need to ensure that result is same as original text if nothing was stripped.
            // this is important because indexer will avoid redoing openNLPNER etc. if stripped content is exactly the same as the original.
            if (nextLine == null) {
                // we're at end of input, append \n only if original text had it.
                if (text.endsWith("\n"))
                    result.append("\n");
            } else
                result.append("\n");
        }

        return result.toString();
    }

    private static void testGetOriginalContent() throws IOException {
        String x = "abc\r\nrxyz\rde\n";
        System.out.println("s = " + x + "\n" + x.length());

        x = x.replaceAll("\r\n", "\n");
        System.out.println("s = " + x + "\n" + x.length());
        x = x.replaceAll("\r", "\n");
        System.out.println("s = " + x + "\n" + x.length());

        String s = "This is a test\nsecond line\n"; // testing with 4 trailing newlines, this was causing a problem
        String s1 = getOriginalContent(s);
        System.out.println("s = " + s);
        System.out.println("s1 = " + s1);
        System.out.println("equals = " + s.equals(s1));
        if (!s.equals(s1))
            throw new RuntimeException();
    }

    // text is an email message, returns a sanitized version of it
    // after stripping away signatures etc.
    public static String cleanupEmailMessage(String text) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader br = new BufferedReader(new StringReader(text));
        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            line = line.trim();
            if (line.equals("--")) // strip all lines including and after signature
                break;

            if (line.contains("Original message") || line.contains("Forwarded message")) {
                int idx1 = line.indexOf("-----");
                int idx2 = line.lastIndexOf("-----");
                if (idx1 >= 0 && idx2 >= 0 && idx1 != idx2)
                    break;
            }

            if (line.startsWith("On ") && line.endsWith("wrote:"))
                break;

            result.append(line).append("\n");
        }

        return result.toString();
    }

    public static List<String> emailAddrs(Address[] as) {
        List<String> result = new ArrayList<>();
        if (as == null)
            return result;

        for (Address a : as) {
            String email = ((InternetAddress) a).getAddress();
            if (!Util.nullOrEmpty(email))
                result.add(email.toLowerCase());
        }
        return result;
    }

    public static List<String> personalNames(Address[] as) {
        List<String> result = new ArrayList<>();
        if (as == null)
            return result;

        for (Address a : as) {
            String name = ((InternetAddress) a).getPersonal();
            if (!Util.nullOrEmpty(name))
                result.add(name.toLowerCase());
        }
        return result;
    }

    /**
     * thread a collection of emails
     */
    public static Collection<Collection<EmailDocument>> threadEmails(Collection<EmailDocument> docs) {
        Map<String, Collection<EmailDocument>> map = new LinkedHashMap<>();
        for (EmailDocument ed : docs) {
            // compute a canonical thread id, based on the cleaned up subject line (removing re: fwd: etc) and the description.
            // in the future, could consider date ranges too, e.g. don't consider messages more than N days apart as the same thread
            // even if they have the same subject and recipients
            // for gmail only -- there is a thread id directly in gmail which can potentially be used
            String canonicalSubject = cleanupSubjectLine(ed.description);
            // we prob. don't care about canonical case for subject

            List<String> addrs = emailAddrs(ed.to);
            addrs.addAll(emailAddrs(ed.cc));
            addrs.addAll(emailAddrs(ed.from));
            Collections.sort(addrs);
            String threadId = canonicalSubject + " " + Util.join(addrs, ",");
            // canonical id for the thread.
            Collection<EmailDocument> messagesForThisThread = map.computeIfAbsent(threadId, k -> new ArrayList<>());
            messagesForThisThread.add(ed);
        }
        return map.values();
    }


    /*
     * normalizes names, computing the (internal) name **for the purposes of lookup only**
     * it mangles the name, so it should never be displayed to the user!
     * e.g.s
     * Wetterwald Julien becomes julien wetterwald
     * Obama Barack becomes barack obama (all hail the chief!)
     * Barack H Obama also becomes barack obama
     *
     * (this is different from normalizePersonName, which actually changes the display name shown to the user. e.g. in that method
     * "'Seng Keat'" becomes "Seng Keat" (stripping the quotes) and "Teh, Seng Keat" becomes "Seng Keat Teh")
     *
     * it canonicalizes the name by the following rules:
     * (1) ordering all the words in the name alphabetically
     * (2) there is exactly one space between each word in the output, with no spaces at the end.
     * (3) if there are at least 2 names with multiple letters, then single letter initials are dropped.
     *
     * we assume these forms are really the same person, so they must be looked up consistently.
     * this is somewhat aggressive entity resolution. reconsider if it is found to relate unrelated people...
     */
    public static String normalizePersonNameForLookup(String name) {
        if (name == null)
            return null;

        String originalName = name;

        name = cleanPersonName(name);

        if (name == null)
            name = "";

        // remove all periods and commas
        // in future: consider removing all non-alpha, non-number chars.
        // but should we also remove valid quote chars in names like O'Melveny?
        // also be careful of foreign names
        name = name.replaceAll("\\.", " "); // make sure to escape the period, replaceAll's first param is a regex!
        name = name.replaceAll(",", " ");

        // gather all the words in the name into tokens and sort it
        List<String> tokens = Util.tokenize(name.toLowerCase());
        if (tokens.size() <= 1)
            return name.toLowerCase();//IMP: as all tokens which are being returned, are in lower case.

        if (tokens.size() > 2) {
            int nOneLetterTokens = 0, nMultiLetterTokens = 0;
            for (String t : tokens) {
                if (t.length() > 1)
                    nMultiLetterTokens++;
                else
                    nOneLetterTokens++;
            }

            // if we have at least 2 multi-letter names, then ignore initials
            if (nMultiLetterTokens >= 2 && nOneLetterTokens >= 1)
                // this is an initial, remove it.
                tokens.removeIf(t -> t.length() == 1);
        }

        Collections.sort(tokens);

        // enable variants
        tokens = tokens.stream().map(Variants.nameVariants::getCanonicalVariant).collect(Collectors.toList());

        // cat all the tokens, one space in between, no space at the end
        String cname = Util.join(tokens, " ");

        if (Util.nullOrEmpty(cname)) {
            // unlikely case, but can happen if input string was "" or just periods and commas
            // we don't know whats going in in that case, just be safe and return the original name
            // better to return the original name than cause merging and confusion with an empty normalized name
            return originalName;
        }

        return cname;
    }

    public static String uncanonicaliseName(String str) {
        if (str == null)
            return null;
        List<String> sws = Arrays.asList("but", "be", "with", "such", "then", "for", "no", "will", "not", "are", "and", "their", "if", "this", "on", "into", "a", "there", "in", "that", "they", "was", "it", "an", "the", "as", "at", "these", "to", "of");

        str = str.replaceAll("^\\W+|\\W+$", "");
        String res = "";
        String[] tokens = str.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (sws.contains(token.toLowerCase())) {
                res += token;
            } else {
                if (token.length() > 1)
                    res += token.toUpperCase().substring(0, 1) + token.substring(1);
                else
                    res += token.toUpperCase();
            }
            if (i < (tokens.length - 1))
                res += " ";
        }
        return res;
    }

    /**
     * normalizes email address: strips away blanks and quotes, lower cases. must be used before put/get into AddressBook.emailToContact.
     * we often see junk like 'hangal@cs.stanford.edu' (including the start and end quote characters), which this method strips away
     */
    public static String cleanEmailAddress(String e) {
        if (e == null)
            return null;

        e = e.trim().toLowerCase();
        while (e.startsWith("'") || e.startsWith("\"")) {
            e = e.substring(1);
            e = e.trim();
        }
        while (e.endsWith("'") || e.endsWith("\"")) {
            e = e.substring(0, e.length() - 1);
            e = e.trim();
        }

        return e;
    }

    /* returns a set of contact objects for all to/from/cc/bcc of the message */
    public static Set<Contact> getContactsForMessage(AddressBook ab, EmailDocument ed) {
        Set<InternetAddress> allAddressesInMessage = new LinkedHashSet<>(); // only lookup the fields (to/cc/bcc/from) that have been enabled

        // now check for mailing list state
        if (!Util.nullOrEmpty(ed.to)) {
            allAddressesInMessage.addAll((List) Arrays.asList(ed.to));
        }
        if (!Util.nullOrEmpty(ed.from)) {
            allAddressesInMessage.addAll((List) Arrays.asList(ed.from));
        }
        if (!Util.nullOrEmpty(ed.cc)) {
            allAddressesInMessage.addAll((List) Arrays.asList(ed.cc));
        }
        if (!Util.nullOrEmpty(ed.bcc)) {
            allAddressesInMessage.addAll((List) Arrays.asList(ed.bcc));
        }

        Set<Contact> contactsInMessage = new LinkedHashSet<>();
        for (InternetAddress a : allAddressesInMessage) {
            // try and find the contact for both the email address and the name, because sometimes (in extreme cases only) perhaps the email is not there, and we only have a name
            Contact c = ab.lookupByEmail(a.getAddress());
            if (c != null)
                contactsInMessage.add(c);
            else {
                // look up name contact only if the email lookup failed -- hopefully this is rare
                log.debug("Warning: email lookup failed for " + a);
                Collection<Contact> contacts = ab.lookupByName(a.getPersonal());
                if (!Util.nullOrEmpty(contacts))
                    contactsInMessage.addAll(contacts);
            }
        }
        return contactsInMessage;
    }

    public static String cleanEmailContent(String content) {
        String[] lines = content.split("\\n");
        String cont = "";
        for (String line : lines) {
            if (line.contains("wrote:")) {
                break;
            }
            cont += line + "\n";
        }

        content = cont;
        //only double line breaks are considered EOS.
        content = content.replaceAll("^>+.*", "");
        content = content.replaceAll("\\n\\n", ". ");
        content = content.replaceAll("\\n", " ");
        return content;
    }

    private static Map<String, String> sample(Map<String, String> full, double p) {
        Random rand = new Random();
        Map<String, String> sample = new LinkedHashMap<>();
        for (String e : full.keySet()) {
            if (rand.nextDouble() < p)
                sample.put(e, full.get(e));
        }
        return sample;
    }

    private static String cleanDBPediaRoad(String title) {
        String[] words = title.split(" ");
        String lw = words[words.length - 1];
        String ct = "";
        boolean hasNumber = false;
        for (Character c : lw.toCharArray())
            if (c >= '0' && c <= '9') {
                hasNumber = true;
                break;
            }
        if (words.length == 1 || !hasNumber)
            ct = title;
        else {
            for (int i = 0; i < words.length - 1; i++) {
                ct += words[i];
                if (i < words.length - 2)
                    ct += " ";
            }
        }
        return ct;
    }

    private static Map<String, String> readDBpedia(double p, String typesFile) {
        if (dbpedia != null) {
            if (p == 1)
                return dbpedia;
            else
                return new org.apache.commons.collections4.map.CaseInsensitiveMap<>(sample(dbpedia, p));
        }
        if (typesFile == null)
            typesFile = Config.DBPEDIA_INSTANCE_FILE;
        //dbpedia = new LinkedHashMap<>();
        //we want to be able to access elements in the map in a case-sensitive manner, this is a way to do that.
        dbpedia = new org.apache.commons.collections4.map.CaseInsensitiveMap<>();
        int d = 0, numPersons = 0, lines = 0;
        try {
            InputStream is = Config.getResourceAsStream(typesFile);
            if (is == null) {
                log.warn("DBpedia file resource could not be read!!");
                return dbpedia;
            }

            //true argument for BZip2CompressorInputStream so as to load the whole file content into memory
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new BZip2CompressorInputStream(is, true), StandardCharsets.UTF_8));
            while (true) {
                String line = lnr.readLine();
                if (line == null)
                    break;
                if (lines++ % 1000000 == 0)
                    log.info("Processed " + lines + " lines of approx. 3.02M in " + typesFile);

                if (line.contains("GivenName"))
                    continue;

                String[] words = line.split("\\s+");
                String r = words[0];

                /*
                 * The types file contains lines like this:
                 * National_Bureau_of_Asian_Research Organisation|Agent
                 * National_Bureau_of_Asian_Research__1 PersonFunction
                 * National_Bureau_of_Asian_Research__2 PersonFunction
                 * Which leads to classifying "National_Bureau_of_Asian_Research" as PersonFunction and not Org.
                 */
//                if (r.contains("__")) {
//                    d++;
//                    continue;
//                }
                //if it still contains this, is a bad title.
                if (r.equals("") || r.contains("__")) {
                    d++;
                    continue;
                }
                String type = words[1];
                //Royalty names, though tagged person are very weird, contains roman characters and suffixes like of_Poland e.t.c.
                if (type.equals("PersonFunction") || type.equals("Royalty|Person|Agent"))
                    continue;
                //in places there are things like: Shaikh_Ibrahim,_Iraq
                if (type.endsWith("Settlement|PopulatedPlace|Place"))
                    r = r.replaceAll(",_.*", "");

                //its very dangerous to remove things inside brackets as that may lead to terms like
                //University_(Metrorail_Station) MetroStation|Place e.t.c.
                //so keep them, or just skip this entry all together
                //We are not considering single word tokens any way, so its OK to remove things inside the brackets
                //removing stuff in brackets may cause trouble when blind matching entities
                //r = r.replaceAll("_\\(.*?\\)", "");
                String title = r.replaceAll("_", " ");

                String badSuffix = "|Agent";
                if (type.endsWith(badSuffix) && type.length() > badSuffix.length())
                    type = type.substring(0, type.length() - badSuffix.length());
                if (type.endsWith("|Person"))
                    numPersons++;
                type = type.intern(); // type strings are repeated very often, so intern

                if (type.equals("Road|RouteOfTransportation|Infrastructure|ArchitecturalStructure|Place")) {
                    //System.err.print("Cleaned: "+title);
                    title = cleanDBPediaRoad(title);
                    //System.err.println(" to "+title);
                }
                dbpedia.put(title, type);
            }
            lnr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("Read " + dbpedia.size() + " names from DBpedia, " + numPersons + " people name. dropped: " + d);

        return new org.apache.commons.collections4.map.CaseInsensitiveMap<>(sample(dbpedia, p));
    }

    public static Map<String, String> readDBpedia() {
        return readDBpedia(1.0, null);
    }

    private static void testLookupNormalizer() {
        Pair<String, String>[] tests = new Pair[]{
                new Pair<>("bernstein", "bernstein"),
                new Pair<>("charles, bernstein", "bernstein charles"),
                new Pair<>("James H McGill", "james mcgill"),
                new Pair<>("Wetterwald Julien", "julian wetterwald"), // variants maps julien to julian
                new Pair<>("Barack H. Obama", "barack obama"),
                new Pair<>("George H W, Bush", "bush george"),
                new Pair<>("bob creeley", "robert creeley"),

                //committee is a banned word in people names
                new Pair<>("Justice committee", ""),
                // undisclosed is a banned string in people names
                new Pair<>("some-undisclosed-recip", ""),
                new Pair<>("''''093- 'Wetterwald Jul-ien\"''''", "'''093- 'wetterwald jul-ien\"'''")
        };
        for (Pair<String, String> p : tests) {
            String np = normalizePersonNameForLookup(p.getFirst());
            if ((np == null && p.second != null) || (np != null && p.second == null) || (np != null && p.second != null && !np.equals(p.getSecond()))) {
                System.err.println("Test fail!! Expected: \"" + p.second + "\" found: \"" + np + "\" -- for: " + p.first);
            }
        }
        System.err.println("All tests done!");
    }

    public static void main(String[] args) {
        testLookupNormalizer();
    }
}
