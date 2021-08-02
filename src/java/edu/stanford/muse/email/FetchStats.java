package edu.stanford.muse.email;

import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import lombok.ToString;

import java.io.Serializable;
import java.util.*;

/**
 * This is a (partial) archive stats object. it includes import stats, as well as the first/last message date, etc.
 */
@ToString
public class FetchStats implements Serializable {
    private final static long serialVersionUID = 1L;

    public long lastUpdate; /* time in UTC */
    public long fetchAndIndexTimeMillis;
    public String userKey;
    EmailFetcherStats importStats;
    public final List<Pair<String, FolderInfo>> selectedFolders = new ArrayList<>(); // selected folders and their counts
    private Filter messageFilter;
    public long firstMessageDate, lastMessageDate;
    private int spanInMonths;
    public int nMessagesInArchive;
    public Collection<String> dataErrors;
    public long spaceSavingFromDupMessageDetection;
    public long spaceSavingFromDupAttachmentDetection;
    public String archiveOwnerInput;
    public String primaryEmailInput;
    public String archiveTitleInput;
    public Set<String> emailSourcesInput;

    /** returns an HTML version of the stats; used in ePADD */
    public String toHTML() {
        // do not use html special chars here!
        Calendar c = new GregorianCalendar();
        c.setTimeZone(TimeZone.getDefault());
        c.setTime(new Date(lastUpdate));
        String s = "Import Date: " + Util.formatDateLong(c) + " " + c.getTimeZone().getDisplayName() + "<br/>\n";
        s += "Fetch and index time: " + Util.pluralize((int) fetchAndIndexTimeMillis / 1000, "second") + "<br/>\n";
        //Add information as specified in issue #254
        s += "Archive Owner: "+ archiveOwnerInput +"<br/>\n";
        s += "Primary Email Address: "+primaryEmailInput+"<br/>\n";
        s += "Archive Title: "+archiveTitleInput +"<br/>\n";
         if(!Util.nullOrEmpty(emailSourcesInput))
            s += "Email Sources: "+String.join(",",emailSourcesInput)+"<br/>\n";


        if (selectedFolders == null)
            s += "Selected folders: unavailable";
        else {
            s += "Selected folders: " + selectedFolders.size() + "<br/>";
            for (Pair<String, FolderInfo> p : selectedFolders)
                s += Util.escapeHTML(p.getFirst()) + " (" + Util.escapeHTML(p.getSecond().toString()) + ")" + "<br/>";
        }

        s += "selected_messages: " + Util.commatize(importStats.nTotalMessages) +  ", filtered: " + Util.commatize(importStats.nMessagesFiltered) + ", imported: " + Util.commatize(importStats.nMessagesAdded) +", duplicates: " + Util.commatize(importStats.nMessagesAlreadyPresent) + "<br/>\n";
        s += ((messageFilter == null) ? "No message filter" : ("message_filter: " + messageFilter)) + "<br/>\n";
        //	s += "Sent messages: " + nMessagesSent + " Received messages: " + nMessagesReceived + "<br/>\n";
        s += "Messages span: " + Util.formatDate(new Date(firstMessageDate)) + " to " + Util.formatDate(new Date(lastMessageDate)) + "<br/>\n";
        //space saving
        if(spaceSavingFromDupMessageDetection>0) {
            s += "Space saved from detected duplicate messages: "+Util.commatize(spaceSavingFromDupMessageDetection)+"KB" + "<br/>\n";

        }
        if(spaceSavingFromDupAttachmentDetection>0){
            s += "Space saved from detecting duplicate attachments: "+Util.commatize(spaceSavingFromDupAttachmentDetection)+"KB" + "<br/>\n";

        }
        return s;
    }
}
