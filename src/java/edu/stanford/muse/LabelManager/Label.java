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

    // basic fields
    LabelManager.LabType labType; // restricted or general
    String labId;
    String labName;
    String description;

    boolean isSysLabel; // system labels cannot be edited. but they can be either general or restriction labels.

    public LabelManager.RestrictionType getRestrictionType() {
        return restrictionType;
    }

    // the following fields are for restriction labels only. they will be don't care for general labels.
    // by default, restrictionType is NONE
    // depending on restrictionType, one of the field restrictedUntilTime and restrictedForYears will be set.
    private LabelManager.RestrictionType restrictionType = LabelManager.RestrictionType.OTHER;

    public long getRestrictedUntilTime() {
        return restrictedUntilTime;
    }

    private long restrictedUntilTime; // date until restricted (UTC time)

    public int getRestrictedForYears() {
        return restrictedForYears;
    }

    private int restrictedForYears;

    // if set to non-null and non-empty, the restriction applies only to this text within the body of the message. Otherwise the whole message is restricted.
    // this is orthogonal to restriction time
    String restrictedText = null;

    public String getRestrictedText() {
        return restrictedText;
    }

    public Label(String name, LabelManager.LabType type, String labid, String description, boolean isSysLabel){
        this.labName = name;
        this.labType  = type;

        this.labId = labid;
        this.description = description;
        this.isSysLabel = isSysLabel;
    }
    public String getLabelName(){
        return labName;
    }

    public void setRestrictionDetails (LabelManager.RestrictionType restrictionType, long restrictedUntilTime, int restrictedForYears, String restrictedText) {
        this.restrictionType = restrictionType;
        this.restrictedForYears = restrictedForYears;
        this.restrictedUntilTime = restrictedUntilTime;
        this.restrictedText = restrictedText;
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
