/*
 * Copyright (c) 2013-2019 Amuse Labs Pvt Ltd
 */

package edu.stanford.muse.email;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import edu.stanford.muse.email.EmailStore;
import edu.stanford.muse.email.FolderInfo;
import edu.stanford.muse.email.GmailAuth.AuthenticatedUserInfo;
import edu.stanford.muse.email.MuseEmailFetcher;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Created by Chinmay
 * GmailStore is emailStore extended object which connects and fetch messages using Java REST APIs
 *                  *  specified in developer console google. We can use it by instantiating museEmailFetcher with this store. However, I realized that gmail can
 *                  *  also be accessed using IMAP interaface and so we ended up using IMAPPopEmailStore (which is already an emailStore extended class) to connect
 *                  *  and get gmail data. However It is good to keep this class and maybe use it for comparing the gmail fetching via IMAP and via Rest/Java API end points.
 */
public class GmailStore extends EmailStore implements Serializable {

    private final static long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(MboxEmailStore.class);

    private static final String CACHE_FILENAME = ".muse.dir";

    private String nextPageToken=null;

    AuthenticatedUserInfo authenticatedUserInfo;
    Gmail gmailService;
    Map<String, FolderInfo> folderInfoMap;
    String userId;

    public GmailStore(AuthenticatedUserInfo authenticatedUserInfo) {
        //super(authenticatedUserInfo.getDisplayName(), authenticatedUserInfo.getAuthUserId());
        super("Gmail", authenticatedUserInfo.getAuthUserId());
        //set userid as "me". Can we set it to anyone? Not sure..
        userId="me";
        this.folderInfoMap = new LinkedHashMap<>();
        this.authenticatedUserInfo = authenticatedUserInfo;
        String accessToken = authenticatedUserInfo.getAccessToken();
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        this.gmailService = new Gmail.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName("MailMiner").build();
    }


    /**
     * @param userId
     * @return a list of folder info objects. One corresponding to each label present in the mail box.
     * @throws IOException
     */
    public List<FolderInfo> getFoldersAndCount(String userId) throws IOException {
        doneReadingFolderCounts=false;
        List<Label> labels = listLabels(userId);
        List<FolderInfo> result = new LinkedList<>();
        for (Label label : labels) {
            //get number of messages for this label..
            Label linfo = getLabelInfo(userId, label.getId());
            Integer sizeTotal = linfo.getMessagesTotal();
            Integer sizeUnread = linfo.getMessagesUnread();
            FolderInfo folderInfo = new FolderInfo(authenticatedUserInfo.getUserName(), label.getName().toLowerCase(), label.getName().toLowerCase(), sizeTotal);
            result.add(folderInfo);
            folderInfoMap.put(label.getName().toLowerCase(), folderInfo);
        }
        doneReadingFolderCounts=true;
        return result;
    }


    /**
     * @param userId
     * @param lastManyQuarters - a number that denotes how many previous quarters should be searched for messages
     * @param sentonly         - a boolean if set means check only in sent folder.
     * @return list of messages satisfying given query
     * @throws IOException
     */
    public List<Message> listMessagesQuarterWise(String userId,
                                                 int lastManyQuarters, boolean sentonly) throws IOException {

        String query = "";
        if (sentonly)
            query = query + " is:sent ";


        //get current date..
        LocalDate currentdate = LocalDate.now(); // Or whatever you want
        //multiply lastmanyquarters by 4
        int prevmonths = lastManyQuarters * 4;
        LocalDate prevdate = currentdate.minusMonths(prevmonths);
        String startdate = prevdate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));//simpleDateFormat.format(prevdate);
        String enddate = currentdate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        query = query + "after:" + startdate + " before:" + enddate;
        return listMessagesMatchingQuery(userId, query);
    }

    /**
     * @param userId
     * @param start    start date of mail message used for query
     * @param end      end date of maile messages used for query
     * @param sentonly boolean if set searches these messages only in sent folder.
     * @return list of messages satisfying given query.
     * @throws IOException
     */
    public List<Message> listMessagesDateRange(String userId,
                                               Date start, Date end, boolean sentonly) throws IOException {

        String query = "";
        String pattern = "yyyy-mm-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String startdate = simpleDateFormat.format(start);
        String enddate = simpleDateFormat.format(end);
        if (sentonly)
            query = query + " is:sent ";
        query = query + "after:" + startdate + " before:" + enddate;
        return listMessagesMatchingQuery(userId, query);
    }

    /**
     * @param userId
     * @param query  return only those messages which satisfy this query
     * @return An estimate of number of messages satisfying given query.
     * @throws IOException
     */
    public long getMessageCountEstimate(String userId,
                                        String query) throws IOException {
        //https://developers.google.com/gmail/api/v1/reference/users/messages/list
        ListMessagesResponse response = gmailService.users().messages().list(userId).setQ(query).setFields("messages,nextPageToken,resultSizeEstimate").execute();
        return response.getResultSizeEstimate();

    }


    /**
     * @param userId
     * @param query  return only those messages which satisfy this query
     * @return list of messages satisfying given query
     * @throws IOException
     */
    public List<Message> listMessagesMatchingQuery(String userId,
                                                   String query) throws IOException {
        //https://developers.google.com/gmail/api/v1/reference/users/messages/list
        ListMessagesResponse response = gmailService.users().messages().list(userId).setQ(query).setFields("messages,nextPageToken,resultSizeEstimate").execute();
        response.getResultSizeEstimate();
        List<Message> messages = new ArrayList<Message>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                response.getResultSizeEstimate();
                String pageToken = response.getNextPageToken();
                response = gmailService.users().messages().list(userId).setQ(query)
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }


        return messages;
    }


    /**
     * @param userId
     * @param query  return only those messages which satisfy this query
     * @return An iterator which will then be used to return a list of message id's corresponding to this user/query.
     * @throws IOException
     */
    public GmailMessageEnumerator IteratorMessagesMatchingQuery(String userId,
                                                   String query) throws IOException {
        //https://developers.google.com/gmail/api/v1/reference/users/messages/list
        ListMessagesResponse response = gmailService.users().messages().list(userId).setQ(query).setFields("messages,nextPageToken,resultSizeEstimate").execute();

        return new GmailMessageEnumerator(gmailService,userId,query,response);
    }

    /**
     * Returns a JSONArray whose each element represent a succinct representation of the message specified by msgids given in the parameter msgs.
     *
     * @param userId
     * @param msgs
     * @return
     * @throws IOException
     */
    public List<javax.mail.Message> getMessagesForIds(String userId, List<Message> msgs) throws IOException, MessagingException {
        //https://developers.google.com/gmail/api/v1/reference/users/messages/get
        Message response;
        List<javax.mail.Message> messages = new LinkedList<javax.mail.Message>();
        for (Message msg : msgs) {
            JSONObject elem = new JSONObject();
            String msgid = msg.getId();
            response = gmailService.users().messages().get(userId, msgid).setFormat("raw").execute();
            Base64 base64Url = new Base64(true);
            byte[] emailBytes = base64Url.decodeBase64(response.getRaw());

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            javax.mail.Message email = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

            ((LinkedList<javax.mail.Message>) messages).push(email);
            /*   elem.put("id", msgid);
            elem.put("data", email.getContent().toString());
            result.add(elem);*/
        }

        return messages;

    }


    static void prepareArchive(List<MimeMessage> msgs) {
        //prepare empty archive directory.

        //call fetchand index.. or a similar method in EmailThreadFetcher.java file of muse library.
        MuseEmailFetcher mef = new MuseEmailFetcher();
        //set email store as gmailstore.
        //mef.emailStores.add(new GmailStore());
        mef.setupFetchers(-1);

    }

    /**
     * Returns a list of labels in the given account.
     *
     * @param userId
     * @return list of labels.
     * @throws IOException
     */

    public List<Label> listLabels(String userId) throws IOException {
        //https://developers.google.com/gmail/api/v1/reference/users/labels/list
        ListLabelsResponse response = gmailService.users().labels().list(userId).execute();
        List<Label> labels = response.getLabels();
        return labels;
    }

    public Label getLabelInfo(String userId, String labelId) throws IOException {
        Label response = gmailService.users().labels().get(userId, labelId).execute();
        return response;

    }


    @Override
    public String getAccountID() {
        return authenticatedUserInfo.getUserName();
    }

    @Override
    public void computeFoldersAndCounts(String cacheDir) throws MessagingException {
        //if folderInfomap is already filled then simply store this data in cache.

        //else call getFoldersAndCount method that will fill folderInfoMap and then store this data in cache.

        if(folderInfoMap.isEmpty()) {
            try {
                doneReadingFolderCounts =false;
                getFoldersAndCount(userId);
                doneReadingFolderCounts=true;
            } catch (IOException e) {
                doneReadingFolderCounts=true;
                e.printStackTrace();
                log.warn("Unable to get folder information and count!! Please check.");
                return;
            }
        }
        this.folderInfos = new ArrayList<>();
        // Here folderInfoMap object is set. Use it to fill in folderInfos list variable in super class (EmailStore).
        this.folderInfos.addAll(folderInfoMap.values());

    }

    @Override
    protected Pair<Folder, Integer> openFolder(Store store, String fname) throws MessagingException {
       return new Pair(null,folderInfoMap.get(fname).messageCount);
        //return null;
    }

    @Override
    protected Folder openFolderWithoutCount(Store store, String fname) throws MessagingException {
        return null;
    }

    @Override
    public Store connect() throws MessagingException {
        return null;
    }


}
