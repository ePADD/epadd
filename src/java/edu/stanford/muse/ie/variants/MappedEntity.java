package edu.stanford.muse.ie.variants;

import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.util.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * small class that stores an entity with a preferred display name, and its alternate variants
 */
public class MappedEntity implements Serializable {

    private final Set<String> altNames = new LinkedHashSet<>(); // altNames does NOT contain displayName. altNames are not canonicalized
    private String displayName; // this is the preferred name that should be shown to the user when any of the alt names is present
    private short entityType;

    public short getEntityType(){
        return entityType;
    }
    public void setEntityType(short type){
        entityType = type;
    }
    public String getDisplayName(){
        return displayName;
    }


    public void setDisplayName(String name){
        displayName = name;
    }

    public Set<String> getAltNames(){
        return altNames;
    }

    public void addAltNames(String altName){
        altNames.add(altName);
    }
    // this may also point to auth record, etc
    public String toString() {
        return displayName + " (" + Util.pluralize(altNames.size(), "alternate name") + ")";
    }

    /*
        The format for writing a mappedEntity object is as following,
        Start with (###Names###)
        set of alt names (separated by newline)-- If a name is display name then put * in front of that
        then a separator (###EntityType###)
        entity type
         */
    public void writeObjectToStream(BufferedWriter out) throws IOException {
        //out.append("###Names###");
        //out.newLine();
        for(String name: this.getAltNames()){
            if(name.compareTo(this.getDisplayName())==0 && this.getAltNames().size()>1)
                out.append("*");
            out.append(name);
            out.newLine();
        }
        //out.append("###EntityType###");
        //out.newLine();
        //out.append(Integer.toString(this.getEntityType()));
        //out.newLine();
    }


    public static MappedEntity readObjectFromStream(BufferedReader in) throws IOException {
        String inp = in.readLine();
        in.mark(1000);

        if(inp==null)
            return null;
        MappedEntity tmp = new MappedEntity();
        int state  = 0;
        //0=name_reading,1=state_reading
        while(inp!=null){
            if(inp.trim().startsWith(EntityBook.DELIMITER)) {
                //reset to the marked position
                in.reset();
                break;
            }
            else if(inp.trim().startsWith("*")) {
                    //check if the line inp starts with *. If yes then set it as display name as well as altname

                    tmp.getAltNames().add(inp.trim().substring(1));
                    tmp.setDisplayName(inp.trim().substring(1));
            }else {
                    //else set it as only altnames
                    tmp.getAltNames().add(inp.trim());
            }
            //Util.softAssert(tmp.mailingListState,"Some serious issue in reading mailing list state from the contact object",log);
            in.mark(1000);//here readAheadLimit tells how many characters stream can read
            //without losing the mark. In this case we assume that one line of entitybook can not be more than 1000 characters.
            //which is a realistic assumption.
            inp = in.readLine();
        }
        //Display name must be set by now, if not then set the first name from altNames as display name.
        if(Util.nullOrEmpty(tmp.getDisplayName()))
            tmp.setDisplayName(tmp.getAltNames().iterator().next());
        //what about entity type?

        return tmp;
    }


}
