package edu.stanford.muse.ie.variants;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.ner.Entity;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.awt.image.ImageWatched;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by hangal on 1/31/17. Entity mapper for an archive.
 */
public class EntityBook implements Serializable {
    public static long serialVersionUID = 1L;
    public static Log log = LogFactory.getLog(EntityBook.class);

    public static final String DELIMS = " .;,-#@&()";

    /**
     * case independent, word order independent, case normalized
     */
    public static String canonicalize(String s) {
        if (s == null)
            return s;

        // remove stop words, even before lower casing?
        s = s.toLowerCase();
        s = Util.canonicalizeSpaces(s);
        List<String> tokens = Util.tokenize(s, DELIMS);

        if (Util.nullOrEmpty(tokens))
            return "";

        Collections.sort(tokens);
        return Util.join(tokens, " ");
    }

        private final static String DELIMITER = "--";

        // for all entities in this mapper, canonicalized name -> MappedEntity object. to save space, only contains MappedEntity's for entities that actually have more than 1 name variant
        Multimap<String, MappedEntity> nameToMappedEntity = LinkedHashMultimap.create();

        //private List<MappedEntity> entityList = new LinkedList<>();
        /**
         * initialize this mapper from the given text
         */
        public void initialize(String text, short type) {
            //Dont' remove everything just remove the entries for type. Bug #307
            //nameToMappedEntity.clear();
Set<MappedEntity> toRemove = new LinkedHashSet<>();
nameToMappedEntity.values().forEach(me->{
    if(me.getEntityType()==type)
        toRemove.add(me);
});
//now remove from nameToMappedEntity.
            nameToMappedEntity.values().removeAll(toRemove);
            List<String> lines = Util.tokenize(text, "\r\n");
            List<String> linesForEntity = new ArrayList<>();

            for (int i = 0; i <= lines.size(); i++) {
                boolean endOfInput = (i == lines.size());
                boolean endOfEntity = endOfInput; // if end of input, definitely end of person. otherwise, could still be end of person if the line starts with PERSON_DELIMITER
                if (!endOfInput) {
                    String line = lines.get(i).trim();
                    if (line.startsWith(DELIMITER)) {
                        endOfEntity = true;
                    } else {
                        if (!Util.nullOrEmpty(line)) // && !line.startsWith("#")) -- some strange address in jeb bush start with # (!)
                            linesForEntity.add(line);
                    }
                }

                if (endOfEntity) {
                    // end of a contact, process linesForContact. if only 1 line in contact, no need to do anything, we won't enter it into this mapper
                    if (linesForEntity.size() > 1) {
                        MappedEntity me = new MappedEntity();
                        me.setEntityType(type);
                        for (String s : linesForEntity) {
                            if (Util.nullOrEmpty(me.getDisplayName())) {
                                me.setDisplayName( s);
                            }
                                me.addAltNames(s);//alwasy add a name as altname.. Display name is added separately as display name..

                            nameToMappedEntity.put(canonicalize(s), me);
                        }
                    }
                    linesForEntity.clear();
                }
            }
        }
/*
        public void initializeFromFile(String filename) throws IOException {
            String text = Util.readFile(filename);
            initialize(text,);
        }

        public void writeToFile() {

        }*/

    /**
     * there is only entityMapper for every fine grained type
     */

    public Collection<MappedEntity> getEntitiesForName(String name){
    //work on a transient data.. multimap of name to mappedentity.
        //get canonical name of name.
        String cname = canonicalize(name);
        return nameToMappedEntity.get(cname);
    }

    public MappedEntity getEntityForNameAndType(String name, short type){

        Collection<MappedEntity> entities = getEntitiesForName(name);
        //filter on entities where type is same same type.
        Set<MappedEntity> filtered = entities.stream().filter(f->f.getEntityType()==type).collect(Collectors.toSet());

        //ideally this set should be of one element. If not then log it as a message/warning.
        Util.warnIf(filtered.size()>1,"More than one entities of the same type found. Please check for entity name as "+name+" and type as "+type,log);
        //return the first element of this filtered list if any.
        if(filtered.size()==0)
            return null;

        return filtered.iterator().next();
    }

    /* get the preferred display name for the given string */
    public String getDisplayName(String s, short type) {
        MappedEntity m = getEntityForNameAndType(s,type);
        if(m==null)
            return s;
        else
            return m.getDisplayName();
    }
/*
    public void initialize(short type, String text) {
        initialize(text);
        typeToEntityMapper.put(type, emft);
    }*/

    /* given this display name for a type, look up its alt names */
    public Set<String> getAltNamesForDisplayName(String displayName, short type) {
        MappedEntity m = getEntityForNameAndType(displayName,type);
        if(m==null)
            return null;
        else
            return m.getAltNames();
/*
        String cname = canonicalize(displayName);
        EntityMapperForType em = typeToEntityMapper.get(type);
        if (em == null)
            return null;

        Collection<MappedEntity> mes = em.cNameToMappedEntity.get(cname);
        if (mes == null || Util.nullOrEmpty(mes))
            return null;

        MappedEntity me = mes.iterator().next();
        if (Util.nullOrEmpty(me.displayName))
            return null;

        if (me.displayName.equals(displayName))
            return me.altNames;

        return null;*/
    }

    public Map<String, Integer> getDisplayNameToFreq(Archive archive, short type) {
        Map<String, Entity> displayNameToEntity = new LinkedHashMap();
        double theta = 0.001;
        EntityBook entityBook = archive.getEntityBook();

        for (Document doc : archive.getAllDocs())

        {
            Span[] spans = archive.getEntitiesInDoc(doc, true);
            Set<String> seenInThisDoc = new LinkedHashSet<>();

            for (Span span : spans) {
                // bail out if not of entity type that we're looking for, or not enough confidence
                if (span.type != type || span.typeScore < theta)
                    continue;

                String name = span.getText();

                String displayName = name;

                //  map the name to its display name. if no mapping, we should get the same name back as its displayName
                if (entityBook != null)
                    displayName = entityBook.getDisplayName(name, span.type);

                displayName = displayName.trim();

                if (seenInThisDoc.contains(displayName))
                    continue; // count an entity in a doc only once

                seenInThisDoc.add(displayName);

                if (!displayNameToEntity.containsKey(displayName))
                    displayNameToEntity.put(displayName, new Entity(displayName, span.typeScore));
                else
                    displayNameToEntity.get(displayName).freq++;
            }
        }

        // convert from displayNameToEntity to displayNameToFreq
        Map<String, Integer> displayNameToFreq = new LinkedHashMap<>();
        for (Entity e : displayNameToEntity.values())
            displayNameToFreq.put(e.entity, e.freq);

        return displayNameToFreq;
    }

    /*
        The format for writing an EntityBook object is as following,
        For every unique mappedEntity object present in the map nameToMappedEntity
        call writeObjectToStream method of mappedEntity class
        write an entity delimiter "-----------------------------------" once done (for each mapped Entity)
         */
    public void writeObjectToStream(BufferedWriter out) throws IOException {
        Set<MappedEntity> entityList = new LinkedHashSet<>(nameToMappedEntity.values());
        for(MappedEntity entity: entityList){
            entity.writeObjectToStream(out);
            out.write("-------------------------------------------");
            out.newLine();
        }
    }

    /*For reading and constructing nameToMappedEntity map open a BufferedReader and construct
    mappedEntity object iteratively (in between skipping a line for entity delimiter --------------------).
    Once done, iterate over this list of mappedEntity and create nameToMappedEntity map again
    by calling                             nameToMappedEntity.put(canonicalize(s), me);
    for every s in mappedEntity.altNames.
    */
    public static EntityBook readObjectFromStream(BufferedReader in) throws IOException {
        MappedEntity me = MappedEntity.readObjectFromStream(in);
        if(me==null)
            return null;
        EntityBook ebook = new EntityBook();
        while(me!=null){
            for(String name: me.getAltNames()){
                ebook.nameToMappedEntity.put(canonicalize(name),me);
            }
            //now skip one line that represents entity delimiter.
            in.readLine();
            me = MappedEntity.readObjectFromStream(in);
        }
        return ebook;

    }
}
