package edu.stanford.epadd;

/*
Class to capture institution specific information that needs to be displayed to the user in public-discovery mode.
 */
public class RepositoryInfo {
    public String institutionName;
    public String repositoryName;
    public int numberOfCollections;
    public int numberOfMessages;

    public RepositoryInfo(String repositoryName, String institutionName){
        this.institutionName=new String(institutionName);
        this.repositoryName=new String(repositoryName);
        this.numberOfCollections=0;
        this.numberOfMessages=0;


    }
}
