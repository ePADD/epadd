package edu.stanford.muse.ner.EntityExtractionManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import groovy.lang.Tuple;
import org.apache.commons.collections4.Bag;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

class MultimapCollector {

    public static <K, V, A extends Multimap<K, V>> Collector<Map.Entry<K, V>, A, A> toMultimap(Supplier<A> supplier) {
        return Collector.of(supplier, (acc, entry) -> acc.put(entry.getKey(), entry.getValue()), (map1, map2) -> {
            map1.putAll(map2);
            return map1;
        });
    }
}
public class PersonEntityExtractor extends EntityExtractor {

    private boolean displayNeg = false;
    static Log log = LogFactory.getLog(PersonEntityExtractor.class);

    private static final String nPersonEntityExtractionRulesFile="PersonExtractionRules.pl";

    Set<String> getLogicRuleFiles(){
        Set<String> tmp = new LinkedHashSet<>();
        tmp.add(System.getProperty("user.home")+File.separator+nPersonEntityExtractionRulesFile);
        return tmp;
    }
    /*This method takes a set of facts (with CIC encapsulated in them), and extract entities from them.
        IT then fills the type of CIC's back into the set of facts and return them.
     */
    public DocFacts extractEntities(DocFacts input){
//        DocFacts res = Phase1_Resolution_From_AddressBook(input);
        DocFacts res = Phase1_Merging_FollowedBy_Resolution_From_AddressBook_(input);

        DocFacts res2 = Phase2_Resolution_From_DBPedia(res);
        return res2;

    }

    /*
    resolution form the names present in the addrssbook. Here we can look at the addressbook in the following order; Global addressbook (if single contactid is found) else the addressbook
    of all the mails present in the current cluster (if single contactid is found) else the addressbook of current message. But as first cut let us search only in the addressbook of
    all the mails present in the current cluster. This information is stored in maps DocFacts.nMessagesToSender and DocFacts.nMessagesToReceiver.

    This phase is broken into two rounds. In first round the names are resolved using addressbook. In second round the names are joined provided that they are separated by some well known
    separator (like ",") and both of them belong to same contact id. This is to merge the names of the form Gandhi, Mohandas Karamchand.
     */
    private DocFacts Phase1_Resolution_From_AddressBook(DocFacts input){
        //Round 1- resolution of CIC names in isolation from the addressbook of current cluster [Note: DocFacts object represent the information about one cluster]
        Map<String, CICFact> cicnameToTypeID = new LinkedHashMap<>();
        Set<String> seenset = new LinkedHashSet<>();
        input.nCICFacts.forEach(cicFact -> {
            //if in seen set then return.
            if(seenset.contains(cicFact.nCIC))
                return;
            //get contactID's for cicfact name.
            Set<Integer> contactids = input.getContactIDs(cicFact.nCIC);
            if(contactids.size()!=0){
                if(contactids.size()>1){
                    //means more than one contact ids found. Report it and continue (don't resolve this cic-- Handle in future)
                    if(displayNeg)
                        log.info("Name "+cicFact.nCIC+" appears in more than one contact. Skipping the resolution using addressbook");
                }else{
                    //means 1 contact id found. resolve it
                    log.info("Name "+cicFact.nCIC+" resolved using addressbook");
                    cicFact.nEntityID=contactids.iterator().next();
                    cicFact.nType= NEType.Type.PERSON;
                    cicnameToTypeID.put(cicFact.nCIC,cicFact);
                }
            }
            seenset.add(cicFact.nCIC);
        });

        //Round 2- joining/merging of resolved CIC's obtained after previous round.
        Set<CICFact> toBeRemoved = new LinkedHashSet<>();
        Set<CICFact> toBeAdded = new LinkedHashSet<>();
        input.nCICFacts.forEach(cicFact -> {
            //if cicFact.nCIC and cicFact.nNextCIC are of same contactID and inbetween is one of the seaprator like "," then merge them.
            if(cicnameToTypeID.get(cicFact.nCIC)!=null && cicnameToTypeID.get(cicFact.nNextCIC)!=null &&
                    cicnameToTypeID.get(cicFact.nCIC).nType==cicnameToTypeID.get(cicFact.nNextCIC).nType && //same contactid
                    cicnameToTypeID.get(cicFact.nCIC).nEntityID==cicnameToTypeID.get(cicFact.nNextCIC).nEntityID && //same contactid
                    cicnameToTypeID.get(cicFact.nCIC).nMID.equals(cicnameToTypeID.get(cicFact.nNextCIC).nMID) && //same messageid
                    cicFact.nInBetween.trim().equals(",")){
                        log.info("Merging "+cicFact.nCIC+" and "+cicFact.nNextCIC);
                        CICFact thiscic = cicFact;
                        CICFact nextcic = cicnameToTypeID.get(cicFact.nNextCIC);
                        //construct a CIC fact that needs to be added and that represents the joined CIC.
                        CICFact newlyadded = new CICFact();
                        newlyadded.nCIC=new String(thiscic.nCIC+", "+nextcic.nCIC);
                        newlyadded.nStart=thiscic.nStart;
                        newlyadded.nEnd=nextcic.nEnd;
                        newlyadded.nNextCIC=nextcic.nNextCIC;
                        newlyadded.nMID=new String(thiscic.nMID);
                        newlyadded.nInBetween=new String(nextcic.nInBetween);
                newlyadded.nEntityID=nextcic.nEntityID;
                newlyadded.nType=nextcic.nType;

                        //remove two CIC's and add newly created CIC. (in temp for now)
                        toBeRemoved.add(cicnameToTypeID.get(cicFact.nCIC));
                        toBeRemoved.add(cicnameToTypeID.get(cicFact.nNextCIC));
                        toBeAdded.add(newlyadded);
            }
        });
        //once we are done.. remove all CICFacts in the set toberemoved and add all CICFacts from the set toBeAdded.
        toBeRemoved.forEach(cicFact ->  input.removeCIC(cicFact));
        //add all CICFacts in the set tobeadded.
        toBeAdded.forEach(cicFact -> input.addCIC(cicFact));
        return input;
    }



    private DocFacts Phase1_Merging_FollowedBy_Resolution_From_AddressBook_(DocFacts input) {
        //Round 1- For every CIC of the form A , B combine them and see if that can be resolved using addressbook of the current cluster.
        // If yes, remove two CIC's and add one. Keep on iterating until no
        //more CIC's to be added [Why are we repeating? Because one merged CIC can next be merged with anotehr one... That is more sophisticated approach.. To start with just iterate
        //once.
        Set<CICFact> toBeAdded = new LinkedHashSet<>();
        Set<CICFact> toBeRemoved = new LinkedHashSet<>();
        {
            Map<Pair<String, String>, Integer> seenAndMerged = new LinkedHashMap();
            Set<Pair<String, String>> seenAndUnmerged = new LinkedHashSet<>();
            input.nCICFacts.forEach(cicFact -> {
                if (cicFact.nInBetween.trim().equals(",")) {
                    //if seen and already merged
                    if (seenAndMerged.get(new Pair(cicFact.nCIC, cicFact.nNextCIC)) != null) {
                        Integer contactid = seenAndMerged.get(new Pair(cicFact.nCIC, cicFact.nNextCIC));
                        CICFact nextcic = input.getNextCIC(cicFact);
                        if (nextcic != null) {
                            toBeRemoved.add(cicFact);
                            toBeRemoved.add(nextcic);
                            //construct a CIC fact that needs to be added and that represents the joined CIC.
                            CICFact newlyadded = new CICFact();
                            newlyadded.nCIC = new String(cicFact.nCIC + ", " + nextcic.nCIC);
                            newlyadded.nStart = cicFact.nStart;
                            newlyadded.nEnd = nextcic.nEnd;
                            newlyadded.nNextCIC = nextcic.nNextCIC;
                            newlyadded.nMID = new String(cicFact.nMID);
                            newlyadded.nInBetween = new String(nextcic.nInBetween);
                            newlyadded.nEntityID = contactid;
                            newlyadded.nType = NEType.Type.PERSON;

                            toBeAdded.add(newlyadded);
                        }

                    } else if (seenAndUnmerged.contains(new Pair(cicFact.nCIC, cicFact.nNextCIC))) {
                        //if seen and decided to not merge.. Use this information. No need to recalculate the same decision again.
                        return;//continue to next cic.
                    } else {
                        //means we need to check it. We haven't seen it earlier.
                        String potentialContact = cicFact.nCIC + ", " + cicFact.nNextCIC;
                        Set<Integer> contactids = input.getContactIDs(potentialContact);
                        if (contactids.size() == 1) {
                            log.info("Resolved the combined name \"" + potentialContact + "\" using addressbook!");
                            Integer contactid = contactids.iterator().next();
                            //remove cicFact.nCIC and cicFact.nNextCIC from the set and add the combined CIC

                            CICFact nextcic = input.getNextCIC(cicFact);
                            if (nextcic != null) {
                                toBeRemoved.add(cicFact);
                                toBeRemoved.add(nextcic);
                                //construct a CIC fact that needs to be added and that represents the joined CIC.
                                CICFact newlyadded = new CICFact();
                                newlyadded.nCIC = new String(cicFact.nCIC + ", " + nextcic.nCIC);
                                newlyadded.nStart = cicFact.nStart;
                                newlyadded.nEnd = nextcic.nEnd;
                                newlyadded.nNextCIC = nextcic.nNextCIC;
                                newlyadded.nMID = new String(cicFact.nMID);
                                newlyadded.nInBetween = new String(nextcic.nInBetween);
                                newlyadded.nEntityID = contactid;
                                newlyadded.nType = NEType.Type.PERSON;

                                toBeAdded.add(newlyadded);
                                seenAndMerged.put(new Pair(cicFact.nCIC, cicFact.nNextCIC), contactid);
                            }//what if we couldn't find next cic.. That is important!!! Ideally it shouldn't arise.
                        } else {
                            if(displayNeg)
                                log.info("Aha!! \"" + potentialContact + "\" Does not look like a contact!");
                            //add to seenAndUnmerged set so as to reduce burder of cacluating again.
                            seenAndUnmerged.add(new Pair(cicFact.nCIC, cicFact.nNextCIC));
                        }

                    }
                }
            });
            log.info("SUMMARY-----"+ seenAndMerged.size()+" Unique CIC's merged by looking at the address book");
        }
        //once we are done.. remove all CICFacts in the set toberemoved and add all CICFacts from the set toBeAdded.
        toBeRemoved.forEach(cicFact ->  input.removeCIC(cicFact));
        //add all CICFacts in the set tobeadded.
        toBeAdded.forEach(cicFact -> input.addCIC(cicFact));

        //Round 2- resolution of CIC names in isolation from the addressbook of current cluster [Note: DocFacts object represent the information about one cluster]
        {
            Map<String, Integer> seenAndResolved = new LinkedHashMap<>();
            Set<String> seensAndUnresolved = new LinkedHashSet<>();
            input.nCICFacts.forEach(cicFact -> {
                //if resolved then return.
                if (cicFact.nType != null)
                    return;
                if (seenAndResolved.get(cicFact.nCIC) != null) {
                    //if seen and resolved then utilize this information.
                    Integer contactid = seenAndResolved.get(cicFact.nCIC);
                    cicFact.nEntityID = contactid;
                    cicFact.nType = NEType.Type.PERSON;
                } else if (seensAndUnresolved.contains(cicFact.nCIC)) {
                    //if seen and unresolved.. just continue;
                    return;
                } else {
                    //means not seen yet. Process now.
                    //get contactID's for cicfact name.
                    Set<Integer> contactids = input.getContactIDs(cicFact.nCIC);
                    if (contactids.size() != 0) {
                        if (contactids.size() > 1) {
                            //means more than one contact ids found. Report it and continue (don't resolve this cic-- Handle in future)
                            if(displayNeg)
                                log.info("Name " + cicFact.nCIC + " appears in more than one contact. Skipping the resolution using addressbook");
                            seensAndUnresolved.add(cicFact.nCIC);
                        } else {
                            //means 1 contact id found. resolve it
                            log.info("Name " + cicFact.nCIC + " resolved using addressbook");
                            cicFact.nEntityID = contactids.iterator().next();
                            cicFact.nType = NEType.Type.PERSON;
                            seenAndResolved.put(cicFact.nCIC,cicFact.nEntityID);
                        }
                    }else{
                        seensAndUnresolved.add(cicFact.nCIC);
                    }
                }
            });
            log.info("SUMMARY-----"+ seenAndResolved.size()+"  CIC's resolved by looking at the address book");

        }


        return input;
    }
    /*
    resolution from the names extracted from DBPedia. We can have following two strategies to start with;
    1. exact match as person
    2. after tokenization, if each token matches to person then the combined one is person too.
    -- These rules can grow to become more sophisticated.
     */
    private DocFacts Phase2_Resolution_From_DBPedia(DocFacts input){

        //For every CIC that is not resolved yet, search the name in DBPedia index to check if the CIC is a person. Or rather break it into tokens and then search for each one of them
        //being person. That is the current strategy employed.
        EntityIndexerData eidata = new EntityIndexerData();
        Set<String> seenAndResolved = new LinkedHashSet<>();
        Set<String> seenAndUnresolved = new LinkedHashSet<>();
        input.nCICFacts.forEach(cicFact -> {
            if(cicFact.nType!=null)
                return;//if already resolved then continue;

            if(seenAndResolved.contains(cicFact.nCIC)){
                cicFact.nType= NEType.Type.PERSON;
            }else if(seenAndUnresolved.contains(cicFact.nCIC)){
                return;
            }else{
                //strategy 1- get in full from dbpedia (exact search). if found (any only one entry then done)
                try {
                    Multimap<String,String> types = eidata.findCandidateTypes("\""+cicFact.nCIC+"\"");
                    if(types.size()==1){
                        //found exact match
                        String type = types.values().iterator().next();
                        //For now we are interested in only finding person type entities.
                        if(type.equals("person")){
                            seenAndResolved.add(cicFact.nCIC);
                            cicFact.nType= NEType.Type.PERSON;
                            log.info("Found exact match in dbpedia for "+cicFact.nCIC);
                            log.info("----- The type is PERSON");
                        }else{
                            seenAndUnresolved.add(cicFact.nCIC);
                        }

                    }else if(types.size()==0 || types.size()>1){
                      /*//found more than one type match, log them and pass that to an aggregator for aggregation. use that aggregate value as type.
                      log.info("Found more than one candidate match in dbpedia for "+cicFact.nCIC);
                      types.entries().forEach(entry->{ log.info("----- The type is "+entry.getKey()+":::"+ entry.getValue());});
                  }else{*/
                        //means found no match or many matches for this in dbpedia.-- now apply strategy 2.
                        //tokenize and then search individually. Then apply some heuristic to infer the type of all.
                        //step 1. tokenize cicFact.nCIC
                        StringBuilder logmessage = new StringBuilder();
                        logmessage.append("Trying to infer the type of "+cicFact.nCIC+"\n");
                        List<String> tokens = Util.tokenize(cicFact.nCIC);
                        String tokentypesofar=null;
                        //step 2. iterate over all tokens if stop word then dont' query else query and filter results which are in the same position.
                        for(int pos=1; pos<=tokens.size();pos++){
                            String token = tokens.get(pos-1);
                            boolean undecidedtoken = false;
                            if( !StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(token.toLowerCase())){
                                //search dbpedia for this token
                                Multimap<String,String> result = eidata.findCandidateTypes(token);
                                //filter result to only those entitynames where token appear at pos.
                                final int position=pos;
                                Multimap<String,String> filteredres = result.entries().stream().filter(entry->{
                                    String ename= entry.getKey();
                                    List<String> tok = Util.tokenize(ename);//tokenize.. and return true only if token appears at position po of this tokenized list.
                                    if(tok.size()>position-1 && tok.get(position-1).trim().toLowerCase().equals(token.trim().toLowerCase()))
                                        return true;
                                    else
                                        return false;
                                }).collect(MultimapCollector.toMultimap(HashMultimap::create));
                                if(filteredres.size()==0){
                                    //means no type detected for this token. Skip it.
                                    tokentypesofar=null;
                                    undecidedtoken=true;
                                }else {
                                    //step 3. aggregate those satisfying the position constraints and find the type based on some heuristic aggregation. that becomes the type of this token.
                                    Map<String, Long> freq = filteredres.entries().stream().collect(Collectors.groupingBy(obj -> obj.getValue(), Collectors.counting()));

                                    //get that type which has the maximum frequency.
                                    Map.Entry<String, Long> max = freq.entrySet().stream().max((e1, e2) -> Long.compare(e1.getValue(), e2.getValue())).get();
                                    //make sure that it is clear majority (no tie).
                                    List<Long> values = freq.values().stream().collect(Collectors.toList());
                                    //max.value() should not appear twice in the list of values.
                                    int temp = values.size();
                                    values.removeIf(val -> val == max.getValue());
                                    if (values.size() != temp - 1) {
                                        //means more than one occurrences of the same frequency. So we can not infer the type of this token correctly. Skip it.
                                        tokentypesofar = null;
                                        undecidedtoken = true;
                                    } else {
                                        logmessage.append("Inferred the type of toekn " + token + "as " + max.getKey() + " with occurrences of " + max.getValue()+"\n");
                                        if (tokentypesofar != null && !tokentypesofar.equals(max.getKey())) {
                                            tokentypesofar = null;
                                            undecidedtoken = true;
                                        } else if(!max.getKey().equals("person")){
                                            //if the inferred type is not person then also quit.. Because we don't know if this heuristic makes sense for non-person types..
                                            tokentypesofar = null;
                                            undecidedtoken = true;
                                        } else {
                                            tokentypesofar = max.getKey();
                                        }
                                    }
                                }
                                if(undecidedtoken)
                                    break;
                            }
                        }
                        //step. If the type of each token is Person then the type of the original one is person - H1 (similar things can be thought about other types like Place university et.)
                        if(tokentypesofar!=null){
                            //means a consensus was reached for each token of the query item. Declare it as a type .

                            log.info(logmessage.toString()+"----- INFERRED:::: The type of " +  cicFact.nCIC+ " is "+ tokentypesofar);
                            cicFact.nType= NEType.Type.PERSON;
                            seenAndResolved.add(cicFact.nCIC);
                        }else{
                            if(displayNeg)
                            log.info(logmessage.toString()+"----- Couldn't infer the type of  " +  cicFact.nCIC);
                            seenAndUnresolved.add(cicFact.nCIC);
                        }

                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        log.info("SUMMARY-----"+ seenAndResolved.size() +" CIC's resolved by looking at DBPedia and applying max-count heuristic!!");
        return input;
    }



}
