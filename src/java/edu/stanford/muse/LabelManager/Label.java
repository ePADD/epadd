package edu.stanford.muse.LabelManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

/**
 * Created by chinmay on 21/12/17.
 */
public class Label implements Serializable {

    /** note: these field names are used in JS as well. do not change casually */

    private static Log log = LogFactory.getLog(Label.class);
    private final static long serialVersionUID = 1L;
    String labName;
    String labId;
    String description;
    LabelManager.LabType labType;
    boolean isSysLabel;
    public Label(String name, LabelManager.LabType type,String labid,String description,boolean isSysLabel){
        this.labName = name;
        this.labType  = type;
        this.labId = labid;
        this.description = description;
        this.isSysLabel = isSysLabel;
    }
    public String getLabelName(){
        return labName;
    }

    public boolean isSysLabel(){ return isSysLabel;}
    public String getLabelID(){
        return labId;
    }

    public String getDescription(){
        return description;
    }

    public LabelManager.LabType getType () { return this.labType; }
    public void updateInfo(String labelName, String description, LabelManager.LabType type){
        this.labName = labelName;
        this.labType = type;
        this.description = description;
    }
}
