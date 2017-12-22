package edu.stanford.muse.email.LabelManager;

import java.io.Serializable;

/**
 * Created by chinmay on 21/12/17.
 */
public class Label implements Serializable{
    String labName;
    Integer labId;
    LabelManager.LabType labType;
    public Label(String name, LabelManager.LabType type,int labid){
        this.labName = name;
        this.labType  = type;
        this.labId = labid;
    }
}
