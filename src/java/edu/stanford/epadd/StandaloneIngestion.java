package edu.stanford.epadd;

import edu.stanford.muse.email.MboxEmailStore;
import edu.stanford.muse.email.MuseEmailFetcher;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import edu.stanford.muse.webapp.SimpleSessions;

import java.io.IOException;

abstract class testm{
    public abstract  void test();
}
public class StandaloneIngestion{

    private boolean IngestMbox(String name, String folderpath){

        MuseEmailFetcher mailFetcher = new MuseEmailFetcher();
        String errorMessage="";
        MboxEmailStore me = null;
        try {
            mailFetcher.addMboxAccount(name,folderpath, folderpath, false);
        } catch (IOException e) {
            errorMessage="File input output error : "+e.getMessage();
        }
        if(!Util.nullOrEmpty(errorMessage))
            return false;
        //setup fetchers
        mailFetcher.setupFetchers(-1);
        //now we are ready to get the mbox reading..
        int nAccounts = mailFetcher == null ? 0 : mailFetcher.getNAccounts();
        for (int accountIdx = 0; accountIdx < nAccounts; accountIdx++)
        {
            String accountName = mailFetcher.getDisplayName(accountIdx);
            String sanitizedAccountName = accountName.replace(".", "-");

            boolean success = true;
            String failMessage = "";

            boolean toConnectAndRead = !mailFetcher.folderInfosAvailable(accountIdx);

            if (toConnectAndRead)
            {
                // mbox can only be with "desktop" mode, which means its a fixed cache dir (~/.muse/user by default)
                // consider moving to its own directory under ~/.muse
                String mboxFolderCountsCacheDir = SimpleSessions.getDefaultCacheDir();
                JSPHelper.doLogging("getting folders and counts from fetcher #" + accountIdx);
                // refresh_folders() will update the status and the account's folders div
                mailFetcher.readFoldersInfos(accountIdx, mboxFolderCountsCacheDir);
            }
            System.out.println(mailFetcher.getFolderInfosAsJson(accountIdx));
        } // end of fetcher loop
        //Now the reading of folder count is done.. At least we can stop here and open the file to see the count folderwise.
        return true;
    }

    public static void main(String args[]){

        StandaloneIngestion si = new StandaloneIngestion();
        if(args.length!=2){
            System.out.println("Please pass two arguments: name of the mbox and the path of the folder ");
            return;
        }
        String name = args[0];
        String path=args[1];
        si.IngestMbox(name,path);
        return;
    }
}