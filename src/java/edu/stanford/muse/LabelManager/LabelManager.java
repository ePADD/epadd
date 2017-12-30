package edu.stanford.muse.LabelManager;

import com.google.gson.Gson;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by chinmay on 21/12/17.
 */
public class LabelManager implements Serializable{

    private static Log log = LogFactory.getLog(LabelManager.class);
    private final static long serialVersionUID = 1L;
    public final static String LABELID_DNT="0";

    public enum LabType {
        RESTR_LAB, GEN_LAB
    }

    public enum RestrictionType {
        OTHER, RESTRICTED_UNTIL, RESTRICTED_FOR_YEARS
    }

    //Map from Document ID to set of Label ID's
    private Map<String,Set<String>> docToLabelMap= null;
    //Map from Label ID's to Label Information
    private Map<String,Label> labelInfoMap=null;

    public LabelManager(){
        docToLabelMap = new LinkedHashMap<>();
        labelInfoMap = new LinkedHashMap<>();
        InitialLabelSetup();
    }

    /** creates and returns a new label with the given details, assigning it a new, unused labelID */
    public Label createLabel(String labelName, String description, LabelManager.LabType type) {

        // look for the first integer that is not taken as a label id
        int id = 1;
        while (labelInfoMap.get (Integer.toString(id)) != null)
            id++;

        String newlabid = Integer.toString(id);
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
        Label dnt = new Label("Do not transfer",LabType.RESTR_LAB, LABELID_DNT,
                "Do not transfer this message",true);
        labelInfoMap.put(dnt.getLabelID(),dnt);

        //restricted
        Label twr = new Label("Restricted",LabType.RESTR_LAB,"1",
                "Transfer this message only after 2040",false);
        labelInfoMap.put(twr.getLabelID(),twr);

        // test
        Label gen = new Label("General",LabType.GEN_LAB,"2",
                "This is general label",false);
        labelInfoMap.put(gen.getLabelID(),gen);

        //reviewed
        Label reviewed = new Label("Reviewed",LabType.GEN_LAB,"3",
                "This message was reviewed",true);
        labelInfoMap.put(reviewed.getLabelID(),reviewed);
    }

    //set label for an email document
    public void setLabels(String docid, Set<String> labelIDs){
        Set<String> labset = docToLabelMap.getOrDefault(docid,null);
        if(labset==null){
            docToLabelMap.put(docid,new LinkedHashSet<>());
            labset = docToLabelMap.get(docid);
        }
        labset.addAll(labelIDs);
    }

    //remove label for an email document
    public void unsetLabels(String docid, Set<String> labelIDs){
        Set<String> labset = docToLabelMap.getOrDefault(docid,null);
        if(labset!=null)
            labset.removeAll(labelIDs);
    }

    //put only a set of labels on a document
    public void putOnlyTheseLabels(String docid, Set<String> labelIDs){
        Set<String> labset = docToLabelMap.getOrDefault(docid,null);
        if(labset==null)
        {
            docToLabelMap.put(docid,new LinkedHashSet<>());
            labset = docToLabelMap.get(docid);
        }
        labset.clear();
        labset.addAll(labelIDs);
    }


    public boolean isRestrictionLabel(String labid){
        Label lab = labelInfoMap.getOrDefault(labid,null);
        if(lab==null){
            Util.softAssert(true,"No label found with the given label id "+labid, log);
            return false;
        }else
            return lab.getType()==LabType.RESTR_LAB;

    }

    //get all label IDs for an email document ( any type)
    public Set<String> getLabelIDs(String docid){
        return docToLabelMap.getOrDefault(docid,new HashSet<>());
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
      Code for serialization of this object. Going forward, we will save this object in a human-readable format.
      For that, we need to resolve the issue of storing mailingList and dataErrors in a human-readable format.
       */
    public void serializeObjectToFile(String filename) throws IOException {
        Util.writeObjectToFile(filename,this);
    }
    /*
    Code for deserialization and re-initialization of transient fields of this Class.
     */
    public static LabelManager deserializeObjectFromFile(String filename) throws IOException, ClassNotFoundException {
        LabelManager labelManager = (LabelManager)Util.readObjectFromFile(filename);
        //No transient fields need to be filled. Just return this object.
        return labelManager;
    }

    /*
    Returns a new labelmanager to capture what all labels and docs are being exported from a module
     */
    public LabelManager getLabelManagerForExport(Set<String> docids, Archive.Export_Mode mode){
        LabelManager tmp = new LabelManager();
        tmp.labelInfoMap.putAll(labelInfoMap);
        tmp.docToLabelMap.putAll(docToLabelMap);
        if(mode== Archive.Export_Mode.EXPORT_APPRAISAL_TO_PROCESSING){
            //all labels are exported.. But in labelDocMap keep only those docs which are being exported.
            tmp.docToLabelMap.keySet().retainAll(docids);
        }else{
            //only non-restricted labels are exported[even if of date type].. In labelDocMap keep only those docs which are being exported.
            tmp.labelInfoMap = tmp.labelInfoMap.entrySet().stream().filter(entry->entry.getValue().getType()!=LabType.RESTR_LAB).collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
            tmp.docToLabelMap.keySet().retainAll(docids);
        }
        return tmp;
    }
}
