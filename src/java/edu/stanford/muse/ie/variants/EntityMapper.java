package edu.stanford.muse.ie.variants;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.ner.Entity;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.util.Util;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Created by hangal on 1/31/17. Entity mapper for an archive.
 */
public class EntityMapper implements Serializable {
    public static long serialVersionUID = 1L;

    public static final String DELIMS = " .;,-#@&()";

    /**
     * small class that stores an entity with a preferred display name, and its alternate variants
     */
    public class MappedEntity implements Serializable {

        private Set<String> altNames = new LinkedHashSet<>(); // altNames does NOT contain displayName. altNames are not canonicalized
        private String displayName; // this is the preferred name that should be shown to the user when any of the alt names is present

        // this may also point to auth record, etc
        public String toString() {
            return displayName + " (" + Util.pluralize(altNames.size(), "alternate name") + ")";
        }
    }

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

    /**
     * small inner class that stores all entity mappings for 1 type
     */
    class EntityMapperForType implements java.io.Serializable {
        private final static String DELIMITER = "--";

        // for all entities in this mapper, canonicalized name -> MappedEntity object. to save space, only contains MappedEntity's for entities that actually have more than 1 name variant
        Multimap<String, MappedEntity> cNameToMappedEntity = LinkedHashMultimap.create();

        /**
         * initialize this mapper from the given text
         */
        public void initialize(String text) {
            cNameToMappedEntity.clear();

            List<String> lines = Util.tokenize(text, "\r\n");
            List<String> linesForEntity = new ArrayList<String>();

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

                        for (String s : linesForEntity) {
                            if (Util.nullOrEmpty(me.displayName)) {
                                me.displayName = s;
                            } else {
                                me.altNames.add(s);
                            }
                            cNameToMappedEntity.put(canonicalize(s), me);
                        }
                    }
                    linesForEntity.clear();
                }
            }
        }

        public void initializeFromFile(String filename) throws IOException {
            String text = Util.readFile(filename);
            initialize(text);
        }

        public void writeToFile() {

        }
    }

    /**
     * there is only entityMapper for every fine grained type
     */
    Map<Short, EntityMapperForType> typeToEntityMapper = new LinkedHashMap<>();

    /* get the preferred display name for the given string */
    public String getDisplayName(String s, short type) {
        EntityMapperForType emft = typeToEntityMapper.get(type);
        if (emft == null)
            return s;
        String canonicalized = canonicalize(s);
        Collection<MappedEntity> mes = emft.cNameToMappedEntity.get(canonicalized);
        return (mes == null || mes.size() == 0) ? s : mes.iterator().next().displayName; // return the first iterator's display name only
    }

    public void initialize(short type, String text) {
        EntityMapperForType emft = new EntityMapperForType();
        emft.initialize(text);
        typeToEntityMapper.put(type, emft);
    }

    /* given this display name for a type, look up its alt names */
    public Set<String> getAltNamesForDisplayName(String displayName, short type) {
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

        return null;
    }

    public Map<String, Integer> getDisplayNameToFreq(Archive archive, short type) {
        Map<String, Entity> displayNameToEntity = new LinkedHashMap();
        double theta = 0.001;
        EntityMapper entityMapper = archive.getEntityMapper();

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
                if (entityMapper != null)
                    displayName = entityMapper.getDisplayName(name, span.type);

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
}
