package edu.stanford.muse.epaddpremis;

//We need this for the two different types of 'object' we have in the Premis file:
//<premis:object xsi:type="premis:intellectualEntity"> and <premis:object xsi:type="premis:file">
//We use an XmlElement of type ObjectBaseClass and then either assign an object of type File or an object of type
//IntellectualEntity. The marshaller will then add xsi:type="premis:intellectualEntity" or xsi:type="premis:file"
//and the unmarshaller knows what to do with these types.

import java.io.Serializable;

public class ObjectBaseClass implements Serializable {
    private static final long serialVersionUID = -1125433511681045553L;
}
