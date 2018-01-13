package edu.stanford.muse.LabelManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

/**
 * Created by chinmay on 21/12/17.
 */
public class Label implements Serializable{

    /** note: these field names are used in JS as well. do not change casually */

    private static Log log = LogFactory.getLog(Label.class);
    private final static long serialVersionUID = 1L;

    // basic fields
    LabelManager.LabType labType; // restricted or general
    String labId;
    String labName;
    String description;

    boolean isSysLabel; // system labels cannot be edited. but they can be either general or restriction labels.


    // the following fields are for restriction labels only. they will be don't care for general labels.
    // by default, restrictionType is NONE
    // depending on restrictionType, one of the field restrictedUntilTime and restrictedForYears will be set.
    //RestrictionType.OTHER means any other non-actionable restriction label.
    private LabelManager.RestrictionType restrictionType = LabelManager.RestrictionType.OTHER;

    private long restrictedUntilTime; // date until restricted (UTC time)


    private int restrictedForYears;
    // if set to non-null and non-empty, the restriction applies only to this text within the body of the message. Otherwise the whole message is restricted.
    // this is orthogonal to restriction time
    String labelAppliesToMessageText = null;


    /*This constructor should only be used by GSon to convert this class to json format.
    * Should not be invoked by the user code.*/
    private Label(){

    }
    public Label(String name, LabelManager.LabType type, String labid, String description, boolean isSysLabel){
        this.labName = name;
        this.labType  = type;

        this.labId = labid;
        this.description = description;
        this.isSysLabel = isSysLabel;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Name: "+this.getLabelName()+"\n");
        sb.append("ID: "+this.getLabelID()+"\n");
        sb.append("Description: "+this.getDescription()+"\n");
        sb.append("Label Type: "+this.getType()+"\n");
        if(this.getType()== LabelManager.LabType.RESTR_LAB)
        {
            sb.append("RestrictionType: "+this.getRestrictionType()+"\n");
            if(this.getRestrictionType()== LabelManager.RestrictionType.RESTRICTED_FOR_YEARS)
                sb.append("Restricted for years: "+this.restrictedForYears);
            else if (this.getRestrictionType()== LabelManager.RestrictionType.RESTRICTED_UNTIL)
                sb.append("Restricted until: "+this.restrictedUntilTime);
        }

        return sb.toString();

    }
    public void update (String name, String description, LabelManager.LabType type) {
        this.labName = name;
        this.description = description;
        this.labType = type;
    }

    public String getLabelName(){
        return labName;
    }

    public void setRestrictionDetails (LabelManager.RestrictionType restrictionType, long restrictedUntilTime, int restrictedForYears, String labelAppliesToMessageText) {
        this.restrictionType = restrictionType;
        this.restrictedForYears = restrictedForYears;
        this.restrictedUntilTime = restrictedUntilTime;
        this.labelAppliesToMessageText = labelAppliesToMessageText;
    }

    public LabelManager.RestrictionType getRestrictionType(){
        return this.restrictionType;
    }

    public int getRestrictedForYears(){
        return this.restrictedForYears;
    }
    public long getRestrictedUntilTime(){
        return this.restrictedUntilTime;
    }
    public String getLabelAppliesToMessageText(){
        return this.labelAppliesToMessageText;
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

    public boolean equals(Label other){
        if(other==null)
            return false;

        if(this.labType.equals(other.labType) && this.labId.equals(other.labId) && this.labName.equals(other.labName)&&
                this.description.equals(other.description) && this.isSysLabel==other.isSysLabel &&
                this.restrictionType == other.restrictionType && this.restrictedUntilTime==other.restrictedUntilTime &&
                this.restrictedForYears==other.restrictedForYears){
            return this.labelAppliesToMessageText == null ? other.labelAppliesToMessageText == null : this.labelAppliesToMessageText.equals(other.labelAppliesToMessageText);
        }else
            return false;
    }

    public int hashCode(){
        return this.labName.hashCode()+this.labType.hashCode()+this.description.hashCode();
    }
}
