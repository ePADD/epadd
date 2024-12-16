package edu.stanford.muse.ie.variants;

import edu.stanford.muse.ie.NameTypes;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.ner.Entity;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;

import javax.print.Doc;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by hangal on 1/31/17. Entity mapper for an archive.
 */

class Summary_L1{
    double score;
    Set<Document> messages;
    Date startDate;
    Date endDate;
    public Set<Document> getDocs(){
        return messages;
    }
}
public class EntityBook implements Serializable {
    public static long serialVersionUID = 1L;
    public static Logger log = LogManager.getLogger(EntityBook.class);

    private static final String DELIMS = " .;,-#@&()";
    public final static String DELIMITER = "--";
    private final Short entityType;
    // for all entities in this mapper, canonicalized name -> MappedEntity object. to save space, only contains MappedEntity's for entities that actually have more than 1 name variant
    final Map<String, MappedEntity> nameToMappedEntity = new LinkedHashMap<>();
    final Map<MappedEntity,Summary_L1> summary_L1_entityCountMap = new LinkedHashMap<>();
    private JSONArray summary_JSON = null;
    /**
     * case independent, word order independent, case normalized
     */

    public EntityBook(Short entityType){
        this.entityType=entityType;
    }

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

    public  Integer getEntitiesCountMapModuloThreshold(double threshold) {
        // for external entities: ignore threshold
        List<Summary_L1> list = new ArrayList<>(summary_L1_entityCountMap.values());
        return list.size();
    }

    public void fillDocSetMap(Map<MappedEntity,Pair<Double,Set<Document>>> docsetmap){
        Set<MappedEntity> mappedEntities = new LinkedHashSet<>(this.nameToMappedEntity.values());
        mappedEntities.forEach(mappedEntity -> {
            //do a lucene search for all names present in mappedEntity.
            //How to get their scores?? May be- for one of the search result do exact search of that entity and get its score from that span.

        });
    }

    public void fillSummaryFields(Map<MappedEntity, Pair<Double,Set<Document>>> docsetmap,Archive archive){
        JSONArray resultArray = new JSONArray();
        final Integer[] count = {0};//trick to use count (modifiable variable) inside for each.
        summary_L1_entityCountMap.clear();
        docsetmap.entrySet().forEach(entry->{
            count[0]=count[0]+1;
                    Summary_L1 summary = new Summary_L1();
                    summary.score=entry.getValue().first;
                    summary.messages=entry.getValue().second;
                    //get date range
                    Collection<EmailDocument> emaildocs = summary.messages.stream().map(s->(EmailDocument)s).collect(Collectors.toList());
                    Pair<Date,Date> daterange = EmailUtils.getFirstLast(emaildocs,true);
            if(daterange==null) {
                daterange = new Pair<>(archive.collectionMetadata.firstDate,archive.collectionMetadata.lastDate);
            }
            if(daterange.first==null)
                daterange.first = archive.collectionMetadata.firstDate;
            if(daterange.second==null)
                daterange.second = archive.collectionMetadata.lastDate;

            summary.startDate=daterange.first;
                    summary.endDate=daterange.second;
                    summary_L1_entityCountMap.put(entry.getKey(),summary);

            String entity = entry.getKey().getDisplayName();
            JSONArray j = new JSONArray();
            Short etype = entityType;
            Set<String> altNamesSet = entry.getKey().getAltNames();
            String altNames = (altNamesSet == null) ? "" : "Alternate names: " + Util.join (altNamesSet, ";");
            j.put (0, Util.escapeHTML(entity));
            j.put (1, summary.score);
            j.put (2, summary.messages.size());
            j.put (3, altNames);
            if(summary.startDate!=null)
                j.put (4, new SimpleDateFormat("MM/dd/yyyy").format(summary.startDate));
            else
                j.put(4,summary.startDate);
            if(summary.endDate!=null)
                j.put (5, new SimpleDateFormat("MM/dd/yyyy").format(summary.endDate));
            else
                j.put(5,summary.endDate);
            //add entity type as well..
            j.put(6, NEType.getTypeForCode(entityType).getDisplayName());
            resultArray.put (count[0]-1,j);

        });
        summary_JSON = resultArray;
    }



/*

        //private List<MappedEntity> entityList = new LinkedList<>();
        *//**
         * initialize this mapper from the given text
         *//*
        public void initializeOld(String text, short type) {
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
        }*/
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


    /*
        The format for writing an EntityBook object is as following,
        For every unique mappedEntity object present in the map nameToMappedEntity
        call writeObjectToStream method of mappedEntity class
        write an entity delimiter "-----------------------------------" once done (for each mapped Entity)
         */
    public void writeObjectToStream(BufferedWriter out) throws IOException {
        //default writing order is display name.
        writeObjectToStream(out,true);

    }

    public void writeObjectToStream(BufferedWriter out,boolean alphaSort) throws IOException {
        //sort the map nameToMappedEntity base on the value of alphasort option.

        List<MappedEntity> entityList = new LinkedList<>();
        if (alphaSort) {
            //sorty by alphabetical order of display names.
            Set<MappedEntity> set = new LinkedHashSet<>(nameToMappedEntity.values());
            entityList = new ArrayList<>(set);
            Collections.sort(entityList, new Comparator<MappedEntity>() {
                @Override
                public int compare(MappedEntity o1, MappedEntity o2) {
                    return o1.getDisplayName().compareTo(o2.getDisplayName());
                }
            });

        } else {
            //sort by frequency
            entityList = summary_L1_entityCountMap.entrySet().stream().sorted(new Comparator<Map.Entry<MappedEntity, Summary_L1>>() {
                @Override
                public int compare(Map.Entry<MappedEntity, Summary_L1> o1, Map.Entry<MappedEntity, Summary_L1> o2) {
                    return Integer.compare(o2.getValue().messages.size(),o1.getValue().messages.size());//we need ordering from high to low.
                }
            }).sorted(new Comparator<Map.Entry<MappedEntity, Summary_L1>>() {
                @Override
                public int compare(Map.Entry<MappedEntity, Summary_L1> o1, Map.Entry<MappedEntity, Summary_L1> o2) {
                    return Double.compare(o2.getValue().score,o1.getValue().score);
                }
            }).map(e->e.getKey()).collect(Collectors.toList());
        }

        for(MappedEntity entity: entityList){
            entity.writeObjectToStream(out);
            out.write(DELIMITER);
            out.newLine();
        }
    }


    /*For reading and constructing nameToMappedEntity map open a BufferedReader and construct
    mappedEntity object iteratively (in between skipping a line for entity delimiter --------------------).
    Once done, iterate over this list of mappedEntity and create nameToMappedEntity map again
    by calling                             nameToMappedEntity.put(canonicalize(s), me);
    for every s in mappedEntity.altNames.
    */
    public static EntityBook readObjectFromStream(BufferedReader in,Short type) throws IOException {
        MappedEntity me = MappedEntity.readObjectFromStream(in);
        if(me==null)
            return new EntityBook(type);//return empty entitybook
        EntityBook ebook = new EntityBook(type);
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


    public JSONArray getInfoAsJSON() {
        return summary_JSON;

    }

    public Set<String> getAllEntities() {
        Set<MappedEntity> set = new LinkedHashSet<>(nameToMappedEntity.values());
        return set.stream().map(s->s.getDisplayName()).collect(Collectors.toSet());
    }
}
