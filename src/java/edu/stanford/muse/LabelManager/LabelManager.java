package edu.stanford.muse.LabelManager;

import com.google.gson.Gson;
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

    public enum LabType {
        RESTR_LAB, GEN_LAB
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

    public String createLabel(String labelName, String description, boolean isRestricted){
        //get the labelID that can be assigned to this label.
        //For now, get the max of all labelIDs present so far and add 1 to that.
        Set<Integer> labids = labelInfoMap.keySet().stream().map(id->Integer.parseInt(id)).collect(Collectors.toSet());
        Integer m = labids.stream().mapToInt(v->v).max().orElseThrow(NoSuchElementException::new);
        m = m+1;//inc by 1 to get new id.
        String newlabid = m.toString();
        LabType labt;
        if(isRestricted)
            labt = LabType.RESTR_LAB;
        else
            labt = LabType.GEN_LAB;
        labelInfoMap.put(newlabid,new Label(labelName,labt,newlabid,description,false));
        return newlabid;
    }

    public boolean updateLabel(String labelID, String labelName, String description, boolean isRestricted) {
        //get Label object for given labelID
        Label obj = labelInfoMap.get(labelID);
        if (obj == null) {
            Util.softAssert(true, "Label id " + labelID + " is not a valid one. Nothing to update", log);
            return false;
        }
        if (isRestricted)
            obj.updateInfo(labelName, description, LabType.RESTR_LAB);
        else
            obj.updateInfo(labelName, description, LabType.GEN_LAB);

        return true;
    }

    public String getLabelInfoMapAsJSONString(){
        return new Gson().toJson(labelInfoMap);
    }
    // Some test labels
    //By default we have a few in-built system labels.
    private void InitialLabelSetup(){
        //do not transfer
        Label dnt = new Label("Do not transfer",LabType.RESTR_LAB, "0",
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
        Label reviewed = new Label("Reviewed",LabType.RESTR_LAB,"3",
                "This message was reviewed",false);
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

}
