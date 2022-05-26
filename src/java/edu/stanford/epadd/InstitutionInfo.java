package edu.stanford.epadd;

/*
Class to capture institution specific information that needs to be displayed to the user in public-discovery mode.
 */
public class InstitutionInfo {
    public String institutionName;
    public int numberOfCollections;

    public InstitutionInfo(String institutionName){
        this.institutionName= institutionName;
        this.numberOfCollections=0;


    }
}
