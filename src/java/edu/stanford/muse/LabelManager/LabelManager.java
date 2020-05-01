package edu.stanford.muse.LabelManager;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by chinmay on 21/12/17.
 */
public class LabelManager implements Serializable{

    private static final Logger log =  LogManager.getLogger(LabelManager.class);
    private final static long serialVersionUID = 1L;
    public final static String LABELID_DNT="0";
    private final static String LABELID_REVIEWED="1";
    public final static String LABELID_CFR="2";
    public final static String LABELID_NODATE="3";
    public final static String LABELID_POSS_BADDATE="4";
    public final static String LABELID_ATTCH_ERRS ="5";
    public final static String LABELID_PARSING_ERRS="6";
    public final static String LABELID_MISSING_CORRESPONDENT="7";

    private static final String JSONFILENAME="label-info.json";
    private static final String CSVFILENAME="docidmap.csv";

    public static final String ALL_EXPIRED="allexpired";
    public enum LabType {
        RESTRICTION, GENERAL
    }

    public enum RestrictionType {
        OTHER, RESTRICTED_UNTIL, RESTRICTED_FOR_YEARS
    }

    //Map from Document ID to set of Label ID's
    private Multimap<String,String> docToLabelID = null;
    //Map from Label ID's to Label Information
    private Map<String,Label> labelInfoMap=null;

    public LabelManager(){
        docToLabelID = LinkedHashMultimap.create();
        labelInfoMap = new LinkedHashMap<>();
        InitialLabelSetup();
    }

    /** creates and returns a new label with the given details, assigning it a new, unused labelID */
    public Label createLabel(String labelName, String description, LabelManager.LabType type) {

        String newlabid = Integer.toString(getNextLabelNumber());
        Label label = new Label(labelName,type,newlabid,description,false);
        labelInfoMap.put(newlabid, label);
        return label;
    }

    public String getLabelInfoMapAsJSONString(){
        return new Gson().toJson(labelInfoMap);
    }
    // Some test labels
    //By default we have a few in-built system labels.
    private void InitialLabelSetup(){
        // these are just for testing
        Label dnt = new Label(edu.stanford.muse.util.Messages.getMessage("messages","label-manager.restrict.no-transfer"),LabType.RESTRICTION, LABELID_DNT,
                edu.stanford.muse.util.Messages.getMessage("messages","label-manager.restrict.no-transfer.mess"),true);
        labelInfoMap.put(dnt.getLabelID(),dnt);

        /*//restricted
        Label twr = new Label("Restricted",LabType.RESTRICTION,"1",
                "Transfer this message only after 2040",false);
        labelInfoMap.put(twr.getLabelID(),twr);

        // test
        Label gen = new Label("General",LabType.GENERAL,"2",
                "This is general label",false);
        labelInfoMap.put(gen.getLabelID(),gen);
*/
        //reviewed
        Label reviewed = new Label(edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.reviewed"),LabType.GENERAL,LABELID_REVIEWED, edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.reviewed.mess"),false);
        labelInfoMap.put(reviewed.getLabelID(),reviewed);

        //clearForRelease
        Label readyForRelease = new Label(edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.clear-for-release"),LabType.GENERAL,LABELID_CFR,
                edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.clear-for-release.mess"),true);
         labelInfoMap.put(readyForRelease.getLabelID(),readyForRelease);
        //label for messages with no date
        Label nodate = new Label(edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.no-date"),LabType.GENERAL,LABELID_NODATE,
                edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.no-date.mess"),true);
        labelInfoMap.put(nodate.getLabelID(),nodate);
        //label for possibly bad date messages.
        Label possiblybaddate = new Label(edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.bad-date"),LabType.GENERAL,LABELID_POSS_BADDATE,
                edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.bad-date.mess"),true);
        labelInfoMap.put(possiblybaddate.getLabelID(),possiblybaddate);
        //label for messages with attachments without filename
        Label attachmentNoName = new Label(edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.attach-error"),LabType.GENERAL, LABELID_ATTCH_ERRS,
                edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.attach-error.mess"),true);
        labelInfoMap.put(attachmentNoName.getLabelID(),attachmentNoName);
        //label for othe errors -parsing the messages.
        Label otherparsingErrors = new Label(edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.error-while-parsing"),LabType.GENERAL,LABELID_PARSING_ERRS,
                edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.error-while-parsing.mess"),true);
        labelInfoMap.put(otherparsingErrors.getLabelID(),otherparsingErrors);
        Label missingCorrespondentErrors = new Label(edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.error-in-corres"),LabType.GENERAL,LABELID_MISSING_CORRESPONDENT,
                edu.stanford.muse.util.Messages.getMessage("messages","label-manager.general.error-in-corres.mess"),true);
        labelInfoMap.put(missingCorrespondentErrors.getLabelID(),missingCorrespondentErrors);
    }

    //set label for an email document
    public void setLabels(String docid, Set<String> labelIDs){
        labelIDs.forEach(labelid-> docToLabelID.put(docid,labelid));

    }

    //remove label; only if this is not applied to any message. otherwise return status as 1 and error message.
    public Pair<Integer,String> removeLabel(String labid){
        String name = getLabel(labid).labName;
        if(docToLabelID.containsValue(labid)){
            return new Pair(1,edu.stanford.muse.util.Messages.getMessage("messages","label-manager.label-mess.start") + name + edu.stanford.muse.util.Messages.getMessage("messages","label-manager.label-mess.later") );
        }else{
            //means this label isnot present in doctOlABelidmap so just remove it from labelinfo
            labelInfoMap.remove(labid);
            return new Pair(0, edu.stanford.muse.util.Messages.getMessage("messages","label-manager.remove-label") +name);
        }

    }

    //remove label for an email document
    public void unsetLabels(String docid, Set<String> labelIDs){
        labelIDs.forEach(labelid-> docToLabelID.remove(docid,labelid));
    }

    //put only a set of labels on a document
    public void putOnlyTheseLabels(String docid, Set<String> labelIDs){
        docToLabelID.removeAll(docid);
        labelIDs.forEach(labelid-> docToLabelID.put(docid,labelid));
    }


    public boolean isRestrictionLabel(String labid){
        Label lab = labelInfoMap.getOrDefault(labid,null);
        if(lab==null){
            Util.softAssert(true,"No label found with the given label id "+labid, log);
            return false;
        }else
            return lab.getType()==LabType.RESTRICTION;

    }

    public Set<String> getRelativeTimedRestrictionLabels()
    {
        Set<Label> restrictionlabs = getAllLabels(LabelManager.LabType.RESTRICTION);
        Set<String> relativeTimedRestrictions = new LinkedHashSet<>();
        restrictionlabs.forEach(lab-> {
            if(lab.getRestrictionType().equals(LabelManager.RestrictionType.RESTRICTED_FOR_YEARS))
                relativeTimedRestrictions.add(lab.getLabelID());
        });
        return relativeTimedRestrictions;
    }

    //get all label IDs for an email document ( any type)
    public Set<String> getLabelIDs(String docid){
        return new LinkedHashSet<>(docToLabelID.get(docid));
    }

    //get all labels of a given type
    public Set<Label> getAllLabels(LabType type){
        Set<Label> result = labelInfoMap.values().stream().filter(f->
                                                                f.labType == type).collect(Collectors.toSet());
        return  result;
    }

    //get all possible labels
    public Set<Label> getAllLabels(){
        Set<Label> result = new LinkedHashSet<>(labelInfoMap.values());
        return  result;
    }

    //get all possible labels
    public Label getLabel(String labelID){
        return labelInfoMap.get(labelID);
    }

    /*
    argument is the directory where json and csv files will be generated. Label meta data will be stored
    in a json file whereas the mapping of docids to labelids will be stored as csv.
     */
    public void writeObjectToStream(String dirname,Map<String,String> docidToSignature){
        // writing labelinfo map to json format
        FileWriter writer = null;
        try {
            String str = dirname+ File.separator+JSONFILENAME;
            writer = new FileWriter(str);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(labelInfoMap,writer);
            writer.close();
        } catch (IOException e) {
            log.warn("Unable to write labelinfo file");
        }finally {
            try {
                writer.close();
            } catch (IOException e) {

            }
        }


        //writing docToLabelIDmap to csv
        try{
        FileWriter fw = new FileWriter(dirname+ File.separator+CSVFILENAME);
            CSVWriter csvwriter = new CSVWriter(fw, ',', '"',' ',"\n");

        // write the header line: "DocID,LabelID,signature ".
        List<String> line = new ArrayList<>();
        line.add ("DocID");
        line.add ("LabelID");
        //line.add("Signature");
        csvwriter.writeNext(line.toArray(new String[line.size()]));

        // write the records
        for(String docid: docToLabelID.keySet()){
            for(String labid: docToLabelID.get(docid)) {
                line = new ArrayList<>();
                line.add(docid);
                line.add(labid);
                String sig = docidToSignature.getOrDefault(docid,"ERROR! No document found in archive for this unique id");
                String d = sig.replace("\n","").replace("\"","").replace(","," ");
                                //get the signature of this doc from docidToSignature map
                //line.add(d);
                csvwriter.writeNext(line.toArray(new String[line.size()]));
            }
           }
            csvwriter.close();
            fw.close();
        } catch (IOException e) {
            log.warn("Unable to write docid to label map in csv file");
            return;
        }

    }

    public static LabelManager readObjectFromStream(String dirname){
        // reading labelinfo map from json format
        LabelManager lm = new LabelManager();
        FileReader reader = null;
        try {
            String str = dirname+ File.separator+JSONFILENAME;
            reader = new FileReader(str);
            Type type = new TypeToken<Map<String, Label>>(){}.getType();
            lm.labelInfoMap = new Gson().fromJson(reader,type);
            reader.close();

        } catch (IOException e) {
            log.warn("Unable to read labelinfo file");
        }finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.warn("Unable to close labelinfo file");
            }
        }
        /// reading docToLabelIDmap from csv
        try{
            FileReader fr = new FileReader(dirname+ File.separator+CSVFILENAME);
            CSVReader csvreader = new CSVReader(fr, ',', '"', ' ');

            // read line by line, except the first line which is header
            String[] record = null;
            record = csvreader.readNext();//skip the first line.
            while ((record = csvreader.readNext()) != null) {
                lm.docToLabelID.put(record[0],record[1]);//skip record 2 for the time being..
            }

            csvreader.close();
            fr.close();
        } catch (IOException e) {
            log.warn("Unable to read docid to label map from csv file");

        }

        return lm;
    }


    /*
    Returns a new labelmanager to capture what all labels and docs are being exported from a module
     */
    public LabelManager getLabelManagerForExport(Set<String> docids, Archive.Export_Mode mode){
        LabelManager tmp = new LabelManager();
        tmp.labelInfoMap.putAll(labelInfoMap);
        tmp.docToLabelID.putAll(docToLabelID);
        if(mode== Archive.Export_Mode.EXPORT_APPRAISAL_TO_PROCESSING){
            //all labels are exported.. But in labelDocMap keep only those docs which are being exported.
            tmp.docToLabelID.keySet().retainAll(docids);
        }else{
            //only non-restricted labels are exported[even if of date type].. In labelDocMap keep only those docs which are being exported.
            tmp.labelInfoMap = tmp.labelInfoMap.entrySet().stream().filter(entry->entry.getValue().getType()!=LabType.RESTRICTION).collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
            //Following requirement #260 on github don't export system labels.
            tmp.labelInfoMap = tmp.labelInfoMap.entrySet().stream().filter(entry->!entry.getValue().isSysLabel()).collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
            tmp.docToLabelID.keySet().retainAll(docids);
            //alsow, following #260, label 'Reviewed' to be removed if exported to discovery module.
            if(mode== Archive.Export_Mode.EXPORT_PROCESSING_TO_DISCOVERY){
                tmp.labelInfoMap = tmp.labelInfoMap.entrySet().stream().filter(entry->!entry.getKey().equalsIgnoreCase(LABELID_REVIEWED)).collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
            }
            //also, following #260, retain only those labels from docidmap for which there is  info in labelInfoMap-- for example if reviewed is removed from labelinfomap
            //then remove it from the doc map as well. Same for restriction types as well. Although there is a catch. When exporting from processing to delivery, don't remove
            //reviewed label but remove this label from all documents (if applied).
            Collection<String> labelidsleft = new LinkedHashSet<>(tmp.labelInfoMap.keySet());
            //Contd.. To achieve this, remove LabelID-Reviewed from labelidsleft collection if mode is export_processing_to_delivery.
            if(mode == Archive.Export_Mode.EXPORT_PROCESSING_TO_DELIVERY){
                labelidsleft.remove(LABELID_REVIEWED);
            }

            //Now remove all those lables in docToLabelID map which don't appear in labelidsleft.

            Multimap<String,String> tmpdocToLabelID = LinkedHashMultimap.create();
            for(String docid: tmp.docToLabelID.keySet()){
                    Collection<String> labids = new LinkedHashSet<>(tmp.docToLabelID.get(docid));
                    //retain only those labids which are still left in labelinfomap
                    labids.retainAll(labelidsleft);
                    if(labids.size()>0)
                    {
                        //if there are non-zero such labels only then add this document's info in docToLabelIDMap.
                        for(String labid: labids){
                            tmpdocToLabelID.put(docid,labid);
                        }
                    }
            }
            tmp.docToLabelID = tmpdocToLabelID;
        }
        return tmp;
    }

    private int getNextLabelNumber(){
        // look for the first integer that is not taken as a label id
        int id = 1;
        while (labelInfoMap.get (Integer.toString(id)) != null)
            id++;

        return id;
    }


    public Set<String> getGenRestrictions(){
        Set<Label> allRestrictions = getAllLabels(LabelManager.LabType.RESTRICTION);
        Set<String> genRestriction = new HashSet<>();
        allRestrictions.forEach(label -> {
            if (label.getRestrictionType() == LabelManager.RestrictionType.OTHER)
                genRestriction.add(label.getLabelID());
            });
        return genRestriction;
    }

    public Set<String> getTimedRestrictions(){
        Set<Label> allRestrictions = getAllLabels(LabelManager.LabType.RESTRICTION);
        Set<String> timedRestriction = new HashSet<>();
        allRestrictions.forEach(label -> {
            if (label.getRestrictionType() != LabelManager.RestrictionType.OTHER)
                timedRestriction.add(label.getLabelID());
        });
        return timedRestriction;

    }
    public class MergeResult{
        public final Set<Label> newLabels = new LinkedHashSet<>();
        public final List<Pair<Label,Label>> labelsWithNameClash = new LinkedList<>();
    }
    /* Merging of two label managers. Here the implicit assumptions is that the docs on which these labels were
    applied are of same type (i.e. emaildocument here). This merging is just a set union of labels' data structure.
    But before merging, the labelID's in one of the label manager are renamed to make sure
     */
    public MergeResult merge(LabelManager other){
        MergeResult result = new MergeResult();
        //System labels should not be redefined in other. We assume that they will always have fixed semantics
        //across different label managers.
        //a map that holds the mapping of old labelID to newlabel ID before copying them from other to this
        //lael manager.
        Map<String,String> oldToNewLabelID = new LinkedHashMap<>();
        //Also note that, if two labels have same name across two different label managers we will add some
        //distinguishing prefix before them (say LM1 and LM2, or only accession2 in the src label manager)
        String renamedLM_Name="Accession2.";
        Map<String,Label> labnameToLabelMap = new LinkedHashMap<>();
        getAllLabels().forEach(label-> labnameToLabelMap.put(label.getLabelName(),label));
        //Step 1. Copy labels from other to this while renaming label ID's (except DNT/System label)
        for(Label lab: other.getAllLabels()){
            String labid = lab.getLabelID();
            if(lab.isSysLabel)
                continue;
            String newlabid;
            if(labelInfoMap.containsKey(labid)){
                //rename labid to newlabid
                Integer i = getNextLabelNumber();
                newlabid = Integer.toString(i);
            }else
                newlabid = labid;

            //if name of this label clashes with an already existing label of this label manager then add
            //a prefix before that name.
            String labname;
            if(labnameToLabelMap.keySet().contains(lab.getLabelName())) {

                Label inCollection = labnameToLabelMap.get(lab.getLabelName());
                Pair<Label,Label> clashed = new Pair<>(inCollection,lab);
                result.labelsWithNameClash.add(clashed);
                labname = renamedLM_Name + lab.getLabelName();
            }
            else {
                labname = lab.getLabelName();
                result.newLabels.add(lab);
            }
            //create a new label with thisnew label id and other details same as label 'lab'.
            Label newlab = new Label(labname, lab.labType, newlabid, lab.description, lab.isSysLabel);
            newlab.setRestrictionDetails(lab.getRestrictionType(),lab.getRestrictedUntilTime(),lab.getRestrictedForYears(),lab.getLabelAppliesToMessageText());
            //add to labelInfoMap.
            labelInfoMap.put(newlabid,newlab);
            //add mapping of oldlabid to newlabid
            oldToNewLabelID.put(labid,newlabid);
        }
        //now iterate over docLabelInfoMap of other and add those documents in this object's docLabelInfoMap
        //if docid is same then do the set union after renaming old id's to new one (taken from oldToNewLabelIdMap)
        //if docid is new then simply copy them

        ////make sure that system labels' mapping remain same,///////////////////////////////////
        //i.e. if labelid 0 is system label it will continue to map to 0 in oldToNewLabelIDmap
        Set<String> syslabelids = new LinkedHashSet<>();
        getAllLabels().forEach(label-> {
            if(label.isSysLabel)
                syslabelids.add(label.getLabelID());
        });
        syslabelids.forEach(lid->oldToNewLabelID.put(lid,lid));
        /////////////////////////////////////////////////////////////////////

        for(String docid: other.docToLabelID.keySet()){
            //get newlabid's generated in above for loop
            Set<String> newlabels = other.docToLabelID.get(docid).stream().map(labid->oldToNewLabelID.get(labid)).collect(Collectors.toSet());
            newlabels.forEach(labelid-> docToLabelID.put(docid,labelid));
        }

        return result;
    }

    /*
    Returns true if any label is applied to any message. False otherwise
     */
    public boolean isAnyLabel(){

        if(docToLabelID.size()==0)
            return false;
        else
            return  true;
    }
}
