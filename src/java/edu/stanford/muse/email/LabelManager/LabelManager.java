package edu.stanford.muse.email.LabelManager;

import edu.stanford.muse.ie.AuthorityMapper;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.util.Util;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by chinmay on 21/12/17.
 */
public class LabelManager implements Serializable{

    public enum LabType {
        SYSTEM_LAB, RESTR_LAB, GEN_LAB
    }

    //Map from Document ID to set of Label ID's
    private HashMap<String,Set<Integer>> docToLabelMap= null;
    //Map from Label ID's to Label Information
    public HashMap<Integer,Label> labelInfoMap=null;

    public LabelManager(){
        docToLabelMap = new LinkedHashMap<>();
        labelInfoMap = new LinkedHashMap<>();
        InitialLabelSetup();
    }

    //By default we have one in-built system label.
    private void InitialLabelSetup(){
        //do not transfer
        Label dnt = new Label("Do not transfer",LabType.SYSTEM_LAB,1);
        labelInfoMap.put(1,dnt);
        //transfer with restriction
        Label twr = new Label("With restriction",LabType.SYSTEM_LAB,2);
        labelInfoMap.put(2,twr);
        //reviewed
        Label reviewed = new Label("Reviewed",LabType.SYSTEM_LAB,3);
        labelInfoMap.put(3,reviewed);

    }
    //

    //set label for an email document
    public void setLabel(EmailDocument edoc, Integer labelID){
        Set<Integer> labset = docToLabelMap.getOrDefault(edoc.getUniqueId(),null);
        if(labset==null){
            docToLabelMap.put(edoc.getUniqueId(),new LinkedHashSet<>());
            labset = docToLabelMap.get(edoc.getUniqueId());
        }
        labset.add(labelID);
    }

    //get all labels for an email document and a given type
    public Set<Integer> getLabels(EmailDocument edoc, LabType type){
        Set<Integer> ss = new LinkedHashSet<>();
        ss.add(1);
        ss.add(2);

        return ss;

//        Set<Integer> result =
//                docToLabelMap.getOrDefault(edoc.getUniqueId(),new HashSet<>()).stream().filter(f->
//                        labelInfoMap.get(f)!=null && labelInfoMap.get(f).labType==type).collect(Collectors.toSet());
//        return result;
    }


    //get all labels for an email document ( any type)
    public Set<Integer> getLabels(EmailDocument edoc){
        return docToLabelMap.getOrDefault(edoc.getUniqueId(),new HashSet<>());
    }

    //get all labels of a given type
    public Set<Label> getAllLabels(LabType type){
        Set<Label> result = labelInfoMap.values().stream().filter(f->
                                                                f.labType == type).collect(Collectors.toSet());
        return  result;
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
