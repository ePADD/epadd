package edu.stanford.muse.email;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

public class GmailMessageEnumerator implements Enumeration<List<Message>> {
    private  ListMessagesResponse listMessagesResponse;
    private String userId;
    private Gmail gmailService;
    private String query;

    public GmailMessageEnumerator(Gmail gmailService, String userId, String query, ListMessagesResponse listMessagesResponse) {
        this.gmailService = gmailService;
        this.userId = userId;
        this.query = query;
        this.listMessagesResponse = listMessagesResponse;
    }

    @Override
    public boolean hasMoreElements() {
        if (listMessagesResponse == null || listMessagesResponse.getMessages() == null)
            return false;
        else
            return true;
    }

    /**
     * listMessageResponse is the object that should be returned (messages contained in this). Before returning, advance it to the next pointing listMessageResponse object (if any) or
     * point it to null (if none)
     * @return
     */
    @Override
    public List<Message> nextElement()
    {
        ListMessagesResponse toreturn;
        if (listMessagesResponse != null && listMessagesResponse.getMessages() != null) {
            toreturn = listMessagesResponse;
            //now advance it to the next if present. else set to null..
            if (listMessagesResponse.getNextPageToken() != null) {
//                response.getResultSizeEstimate();
                String pageToken = listMessagesResponse.getNextPageToken();
                try {
                    listMessagesResponse = gmailService.users().messages().list(userId).setQ(query)
                            .setPageToken(pageToken).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    listMessagesResponse = null;
                }
            } else
                listMessagesResponse = null;
        }else
            return  null; //nothing to return. mark the end of reading date.

        //Now return from the pointer that was pointing to listMessageResponse in the start of this method.
        return toreturn.getMessages();
    }

}
