package edu.stanford.muse.ner.EntityExtractionManager;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.ner.tokenize.CICTokenizer;
import edu.stanford.muse.ner.tokenize.Tokenizer;
import edu.stanford.muse.util.NLPUtils;
import edu.stanford.muse.util.Triple;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class DocFacts {
    final static String nCICFactsFileName="CICFacts.pl";
    final static String nCICMetaFactsFileName="CICMetaFacts.pl";




    Set<Document> nDocset=null;
    Archive nArchive=null;
    Set<CICFact> nCICFacts=null;
    //message id, threadid - map
    //message id, toCorrespondentid- multimap
    //messageid, fromid - map
    Map<String,Long> nMessageToThread=null;
    Multimap<String,Integer> nMessageToReceiver =null;
    Map<String,Integer> nMessageToSender=null;

    public DocFacts(Set<Document> docs,Archive archive){
        this.nDocset=docs;
        this.nArchive=archive;
        this.nCICFacts=new LinkedHashSet<>();
        this.nMessageToReceiver = LinkedListMultimap.create();
        this.nMessageToSender=new LinkedHashMap<>();
        this.nMessageToThread=new LinkedHashMap<>();
        File f = new File(System.getProperty("user.home")+ File.separator+nCICFactsFileName);
        if(f.exists())
            f.delete();
        f = new File(System.getProperty("user.home")+ File.separator+nCICMetaFactsFileName);
        if(f.exists())
            f.delete();


    }



    /*
    This method extracts CIC from the given set of documents and store them in appropriate fields of this class.
     */
    public void extractAndStoreCIC(Document doc){
        //Step 3.1 - get Content of ed
        EmailDocument ed = (EmailDocument)doc;
        Tokenizer tokenizer = new CICTokenizer();

        org.apache.lucene.document.Document ldoc = null;
        try {
            ldoc = nArchive.getLuceneDoc(doc.getUniqueId());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(ldoc==null)
            return;
        String originalContent = nArchive.getContents(ldoc, true);
        String content = nArchive.getContents(ldoc, false);
        String title = nArchive.getTitle(ldoc); //for now just concentrate on content, not on title -we can handle it later
        //original content is substring of content;


        //Step 3.2 - get Set of lines from the content
        opennlp.tools.util.Span[] sentSpans = NLPUtils.tokenizeSentenceAsSpan(content);
        assert sentSpans!=null;
        for(opennlp.tools.util.Span sentSpan: sentSpans) {
            String line = sentSpan.getCoveredText(content).toString();

            //Step 3.2.1 - For each line, get list of CIC's from the tokenizer and add all of them to dfacts (in one shot)
            List<Triple<String, Integer, Integer>> toks = tokenizer.tokenize(line);
            storeCIC(toks, line, doc);
        }
    }

    /*
    helper method  (private) to restructure a list of CIC's into something that will be stored in this object.
     */
    private void storeCIC(List<Triple<String, Integer, Integer>> ciclist,String line,Document doc){
        Set<Triple<String,Integer,Integer>> cicset = new LinkedHashSet<Triple<String,Integer,Integer>>();
        cicset.addAll(ciclist);//to remove the duplicate CIC's as it happens sometime.

        ciclist.clear();
        //remove a few unimportant stuff and put it back to ciclist
        ciclist = cicset.stream().filter(cictriple->!cictriple.first.equals("Original Message")).collect(Collectors.toList());
        String messageID=doc.getUniqueId();

        for(int i =0; i<ciclist.size();i++){
            Triple<String,Integer,Integer> thisitem = ciclist.get(i);
            CICFact cicFact = new CICFact();
            //what to dump depends upon if there is a next CIC available or not.
            if(i+1==ciclist.size()){
                //means this was last CIC, just dump it with empty CIC as the next one..
                //CIC(str,start,end,nextstr,mid,inbetween)
                cicFact.nCIC=new String(thisitem.first);
                cicFact.nStart=thisitem.second;
                cicFact.nEnd=thisitem.third;
                cicFact.nNextCIC="";
                cicFact.nMID=new String(messageID);
                cicFact.nInBetween="";line.substring(thisitem.third);//as there is no next CIC, everything in between this CIC and the
                //last character is put in this field.
            }else{
                //get the next CIC, get the inbetween substring between these two and then dump the information of this one.
                Triple<String,Integer,Integer> nextitem= ciclist.get(i+1);
                Integer nextstart = nextitem.getSecond();
                //find the substring between thisitem.end to nextstart in content string
                String inbetween = null;
                if(thisitem.getThird()>nextstart) {
                    //out.println("/* Some issue with finding CIC. Here the end of previous CIC is less than the start of the next one */");
                    inbetween = "";
                }
                else
                    inbetween = line.substring(thisitem.getThird(),nextstart);
                //if inbetween contains " then remove them, somehow escaping didn't work with prolog
                inbetween = inbetween.replace("\\","\\\\"); //INTERESTING: Here order is imp what if we put it after the second replacement? " becomes \" and \" becomes \\"
                inbetween = inbetween.replace("\"","\\\"");
                inbetween = inbetween.replace("'","\\'");

                cicFact.nCIC=new String(thisitem.first);
                cicFact.nStart=thisitem.second;
                cicFact.nEnd=thisitem.third;
                cicFact.nNextCIC=new String(nextitem.first);
                cicFact.nMID=new String(messageID);
                cicFact.nInBetween=inbetween;
                //now dump CIC(str,start,end,nextstr,mid,inbetween)
            }
            //add this information in CICFacts collection object of this class.
            nCICFacts.add(cicFact);

        }
    }

    /*
      This method extracts and store metadata corresponding to the messages in the given set and store them in
      appropriate fields of this class.
     */
    public void extractAndStoreMetaData(Document doc){
        //message id, threadid - map
        //message id, toCorrespondentid- multimap
        //messageid, fromid - map
        EmailDocument edoc = (EmailDocument)doc;
        long tid = edoc.threadID;
        String mid = edoc.getUniqueId();
        Contact fromContact = nArchive.getAddressBook().lookupByEmail(edoc.getFromEmailAddress());
        int fromID = fromContact!=null?nArchive.getAddressBook().getContactId(fromContact):-1;
        Set<Contact> toCCBCCContacts = edoc.getToCCBCCContacts(nArchive.getAddressBook());
        Set<Integer> toCCBCCContactsID = toCCBCCContacts.stream().map(f -> nArchive.getAddressBook().getContactId(f)).collect(Collectors.toSet());
        //add facts loc(messageid,threadid), from(messageid,fromid), to(messageid,toCCBCCContactsID) to the dump facts file..
        nMessageToThread.put(mid,tid);
        if(fromID!=-1)
            nMessageToSender.put(mid,fromID);
        toCCBCCContactsID.stream().forEach(id-> nMessageToReceiver.put(mid,id));

    }


    /*
    This method updates the type of the CIC stored in this object. If messageID is null then the type is applied to
    all CIC (present in any message of this set).
     */
    public void updateResolvedType(String CIC, int start, int end, String nextCIC, String inBetween, NEType.Type type, String messageID){

        //Step 1. Check if CIC and messageid is present in nCICFacts..
        CICFact cfact = new CICFact();
        cfact.nCIC=new String(CIC);
        cfact.nStart=start;
        cfact.nEnd=end;
        cfact.nNextCIC=new String(nextCIC);
        cfact.nMID = new String(messageID);
        cfact.nInBetween = new String(inBetween);

        if(nCICFacts.contains(cfact)){
            //If yes then just update the type -- assert that no type should have been assigned to it earlier. In case of conflict how to record that?
            nCICFacts.remove(cfact);
            cfact.nType = type;
            nCICFacts.add(cfact);
        }else{
            //Step 2. If not then add the CIC with these information and set the type accordingly.
            cfact.nType = type;
            nCICFacts.add(cfact);
        }
    }

    /*
    This method removes a given CIC. It is used when some CIC is explicitly merged with other CIC
     */
    public void removeCIC(String CIC, int start, int end, String nextCIC, String inBetween, String messageID){

       //remove this CIC from nCICFacts.
        CICFact cfact = new CICFact();
        cfact.nCIC=new String(CIC);
        cfact.nStart=start;
        cfact.nEnd=end;
        cfact.nNextCIC=new String(nextCIC);
        cfact.nMID = new String(messageID);
        cfact.nInBetween = new String(inBetween);

        nCICFacts.remove(cfact);

    }

    /*
     return all those CIC which have resolved type. This will be needed at the end when this docFacts object has passed through all phases of
     entityextraction (Person/Place etc) and will return set of all resolved types..
     */

    public Map<String,Set<CICFact>> getResolvedCICs(){
        Map<String,Set<CICFact>> result = new LinkedHashMap<>();
        nCICFacts.forEach(fact->{
            if(fact.nType!=null) {
             Set<CICFact> tmp = result.get(fact.nMID);
             if(tmp==null)
             {
                 tmp=new LinkedHashSet<>();
                 result.put(fact.nMID,tmp);
             }
             tmp.add(fact);

            }
        });
        return result;
    }

    /*
    This method dumps the facts extracted (like CIC, correspondents, date, threadid etc) in different prolog files as facts.
     */
    public void dumpFacts(){

        Set<String> alreadyDumpedSet = new LinkedHashSet<>();
        File f = new File(System.getProperty("user.home")+ File.separator+nCICFactsFileName);
        if(!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        try(FileWriter fw = new FileWriter(System.getProperty("user.home")+ File.separator+nCICFactsFileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)) {
            //Step 1. Dump by reading nCICFacts collection. Separately dump resolved and unresolved CICs with different fact names

            //Step 1.1. Iterate over CIC's and dump all resolved CIC's separately as resolved predicate.

            nCICFacts.forEach(fact->{
                if(fact.nType==null) {
                 if(!alreadyDumpedSet.contains(fact.nCIC)) {
                    alreadyDumpedSet.add(fact.nCIC);
                     out.println("cic(\"" + fact.nCIC + "\"," + fact.nStart + "," + fact.nEnd + ",\"" + fact.nNextCIC + "\",\"" + fact.nMID + "\",\"" + fact.nInBetween + "\").");

                 }
                } else{
                    out.println("resolved(\""+fact.nCIC+"\","+ fact.nStart + "," + fact.nEnd + ",\""+ fact.nNextCIC + "\",\""+fact.nMID + "\",\""+fact.nInBetween+"\",\""+fact.nType.name()+"\").");

                }
            });
        }catch(IOException e){
            e.printStackTrace();
        }

        //Step 2. Dump by reading maps/multimaps of threadid, correspondents

        //Step 3. Dump by reading all correspondent ids and archive's addressbook

        f = new File(System.getProperty("user.home")+ File.separator+nCICMetaFactsFileName);
        if(!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        try(FileWriter fw = new FileWriter(System.getProperty("user.home")+ File.separator+nCICMetaFactsFileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)) {

            this.nMessageToThread.forEach((k,v)->{
                out.println("loc(\""+k+"\","+Long.toString(v)+").");
            });
            this.nMessageToSender.forEach((k,v)->{
                out.println("from(\""+k+"\","+Integer.toString(v)+").");
            });
            this.nMessageToReceiver.entries().forEach(entry->{
                out.println("to(\""+entry.getKey()+"\","+Integer.toString(entry.getValue())+").");
            });

            //dump correspondent name and id using addressbook.
            Set<Integer> correspondentIDs= new LinkedHashSet<>();
            correspondentIDs.addAll(this.nMessageToReceiver.values());
            correspondentIDs.addAll(this.nMessageToSender.values());

            correspondentIDs.forEach(cid->{
                Contact contact = nArchive.getAddressBook().getContact(cid);
                if(contact!=null){
                    contact.getNames().forEach(name->{
                        out.println("contact("+cid+",\""+name+"\").");
                    });
                }
            });

        }catch(IOException e){
            e.printStackTrace();
        }




    }

}
