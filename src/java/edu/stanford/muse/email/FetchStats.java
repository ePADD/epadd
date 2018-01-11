package edu.stanford.muse.email;

import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;

import java.io.Serializable;
import java.util.*;

/**
 * This is a (partial) archive stats object. it includes import stats, as well as the first/last message date, etc.
 */
public class FetchStats implements Serializable {
    private final static long serialVersionUID = 1L;

    public long lastUpdate;
    public long fetchAndIndexTimeMillis;
    public String userKey;
    EmailFetcherStats importStats;
    public List<Pair<String, FolderInfo>> selectedFolders = new ArrayList<>(); // selected folders and their counts
    private Filter messageFilter;
    public long firstMessageDate, lastMessageDate;
    private int spanInMonths;
    public int nMessagesInArchive;
    public Collection<String> dataErrors;

    public String toString() {
        // do not use html special chars here!
        Calendar c = new GregorianCalendar();
        c.setTime(new Date(lastUpdate));
        String s = "Import date: " + Util.formatDateLong(c) + "\n";

        s += "message_filter: " + messageFilter + "\n";

//        s += "selected_messages: " + importStats.nTotalMessages +  " filtered: " + importStats.nMessagesFiltered + " imported: " + importStats.nMessagesAdded +" duplicates: " + importStats.nMessagesAlreadyPresent + "\n";
        s += "imported: " + Util.commatize(importStats.nMessagesAdded) + " duplicates: " + Util.commatize(importStats.nMessagesAlreadyPresent) + "\n";
        //	s += "sent_messages: " + nMessagesSent + " received_messages: " + nMessagesReceived + "\n";
        s += "first_date: " + Util.formatDate(new Date(firstMessageDate)) + " last_date: " + Util.formatDate(new Date(lastMessageDate)) + " span_in_months: " + spanInMonths + "\n";
        s += "fetch_time_in_secs: " + fetchAndIndexTimeMillis / 1000 + "\n";
//		s += "user_key: " + userKey + "\n";
        //		s += "total_folders: " + nFolders + "\n";
        if (selectedFolders == null)
            s += "selected_folders: null";
        else {
            s += "selected_folders: " + selectedFolders.size();
            for (Pair<String, FolderInfo> p : selectedFolders)
                s += " - " + p.getFirst() + " (" + p.getSecond() + ")";
        }
        s += "\n";
        return s;
    }

    /** returns an HTML version of the stats; used in ePADD */
    public String toHTML() {
        // do not use html special chars here!
        Calendar c = new GregorianCalendar();
        c.setTime(new Date(lastUpdate));
        String s = "Fetch Date: " + Util.formatDateLong(c) + "<br/>\n";
        s += "Fetch and index time: " + Util.pluralize((int) fetchAndIndexTimeMillis / 1000, "second") + "<br/>\n";
        if (selectedFolders == null)
            s += "Selected folders: unavailable";
        else {
            s += "Selected folders: " + selectedFolders.size() + "<br/>";
            for (Pair<String, FolderInfo> p : selectedFolders)
                s += Util.escapeHTML(p.getFirst()) + " (" + Util.escapeHTML(p.getSecond().toString()) + ")" + "<br/>";
        }

        s += "selected_messages: " + importStats.nTotalMessages +  " filtered: " + importStats.nMessagesFiltered + " imported: " + importStats.nMessagesAdded +" dups: " + importStats.nMessagesAlreadyPresent + "<br/>\n";
        s += ((messageFilter == null) ? "No message filter" : ("message_filter: " + messageFilter)) + "<br/>\n";
        //	s += "Sent messages: " + nMessagesSent + " Received messages: " + nMessagesReceived + "<br/>\n";
        s += "Messages span: " + Util.formatDate(new Date(firstMessageDate)) + " to " + Util.formatDate(new Date(lastMessageDate)) + "<br/>\n";

        return s;
    }

    public static void main(String args[]) {
        FetchStats as = new FetchStats();
        System.out.println(Util.fieldsToString(as));
    }
}
