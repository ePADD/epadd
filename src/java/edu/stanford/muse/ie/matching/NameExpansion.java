package edu.stanford.muse.ie.matching;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.email.AddressBook;
import edu.stanford.muse.email.Contact;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.index.Searcher;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.util.Util;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class NameExpansion {

    private static boolean matchAgainstEmailContent(Archive archive, EmailDocument ed, Matches matchResults, String messageType, float score) {
        Set<String> allNames = new LinkedHashSet();
        Stream.of(archive.getEntitiesInDoc(ed, false)).map(Span::getText).forEach(allNames::add);
        Stream.of(archive.getEntitiesInDoc(ed, true)).map(Span::getText).forEach(allNames::add);
        Iterator it = allNames.iterator();

        String name;
        StringMatchType matchType;
        do {
            if (!it.hasNext()) {
                return false;
            }

            name = (String) it.next();
            matchType = Matches.match(matchResults.getMatchString(), name);
        }
        while (matchType == null || !matchResults.addMatch(name, score, matchType, (messageType != null ? messageType + " " : "") + " (message ID: " + Util.hash(ed.getSignature()) + ")", false));

        return true;
    }

    /* Given the string s in emailDocument ed, returns a matches object with candidates matching s */
    public static Matches getMatches(String s, Archive archive, EmailDocument ed, int maxResults) {
        Matches matches = new Matches(s, maxResults);
        AddressBook ab = archive.addressBook;
        List<Contact> contactsExceptSelf = ed.getParticipatingContactsExceptOwn(archive.addressBook);
        List<Contact> contacts = new ArrayList(contactsExceptSelf);
        contacts.add(ab.getContactForSelf());

        // check if s matches any contacts on this message
        outer:
        for (Contact c: contacts) {
            if (c.names == null)
                continue;
            for (String name : c.names) {
                StringMatchType matchType = Matches.match(s, name);
                if (matchType != null) {
                    float score = 1.0F;
                    if (matches.addMatch(name, score, matchType, "Name of a contact on this message", true))
                        return matches;
                    continue outer;
                }
            }
        }

        // check if s matches anywhere else in this message
        if (matchAgainstEmailContent(archive, ed, matches, "Mentioned elsewhere in this message", 1.0F)) {
            return matches;
        }

        synchronized (archive) {
            if (ed.threadID == 0L) {
                archive.assignThreadIds();
            }
        }

        // check if s matches anywhere else in this thread
        List<EmailDocument> messagesInThread = (List) archive.docsWithThreadId(ed.threadID);
        for (EmailDocument messageInThread: messagesInThread) {
            if (matchAgainstEmailContent(archive, messageInThread, matches, "Mentioned in this thread", 0.9F)) {
                return matches;
            }
        }

        // check if s matches any other email with any of these correspondents
        for (Contact c: contactsExceptSelf) {
            if (c.emails != null) {
                String correspondentsSearchStr = String.join(";", c.emails);
                Set<EmailDocument> messagesWithSameCorrespondents = (Set) Searcher.filterForCorrespondents((Collection) archive.getAllDocs(), ab, correspondentsSearchStr, true, true, true, true);
                for (EmailDocument messageWithSameCorrespondents: messagesWithSameCorrespondents) {
                    if (matchAgainstEmailContent(archive, messageWithSameCorrespondents, matches, "Mentioned in other messages with these correspondents", 0.8F)) {
                        return matches;
                    }
                }
            }
        }

        // search for s anywhere in the archive
        Multimap<String, String> params = LinkedHashMultimap.create();
        params.put("termSubject", "on");
        params.put("termBody", "on");
        String term = s;
        if (s.contains(" ") && (!s.startsWith("\"") || !s.endsWith("\""))) {
            term = "\"" + s + "\"";
        }

        Pair<Set<Document>, Set<Blob>> p = Searcher.searchForTerm(archive, params, term);
        Set<EmailDocument> docsWithTerm = (Set) p.getFirst();
        for (EmailDocument docWithTerm: docsWithTerm) {
             if (matchAgainstEmailContent(archive, docWithTerm, matches, "Mentioned elsewhere in this archive", 0.7F))
                return matches;
        }

        return matches;
    }
}

