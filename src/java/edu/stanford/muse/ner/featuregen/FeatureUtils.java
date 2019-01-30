package edu.stanford.muse.ner.featuregen;

import edu.stanford.muse.ner.dictionary.EnglishDictionary;
import edu.stanford.muse.ner.model.NEType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class FeatureUtils implements Serializable {
    static Log log = LogFactory.getLog(FeatureUtils.class);
    //DBpedia type mapping of the types this can handle
    private static final Pattern endClean = Pattern.compile("^\\W+|\\W+$");
    //this is an enhanced list that is different from EnglishDictionary.stopWords
    public static final List<String> sws = Arrays.asList("and","for","to","in","at","on","the","of", "a", "an", "is", "from",
            "de", "van","von","da","ibn","mac","bin","del","dos","di","la","du","ben","ap","le","bint","do");
    private static final List<String> symbols = Arrays.asList("&","-",",");
    static final boolean DEBUG = false;

    private static final long serialVersionUID = 1L;

    private static final Random rand = new Random();
    static{
        rand.setSeed(5);
    }

    /**
     * Johnson & Johnson => [johnson &, & johnson]
     * Johnson and Johnson => [johnson and, johnson]
     * and the => []
     * Johnson Co. => [johnson, co.]
     * M. Arthi => [m, arthi]
     * */
    public static String[] getPatts(String phrase){
        List<String> patts = new ArrayList<>();
        String[] words = phrase.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            //do not emit stop words
            //canonicalize the word
            word = word.toLowerCase();
            if(sws.contains(word) || symbols.contains(word)){
                continue;
            }
            //some DBpedia entries end in ',' as in Austin,_Texas
            String t = word;
            //if the token is of the form [A-Z]. don't remove the trailing period since such tokens are invaluable in recognising some names, for ex:
            if(t.length()!=2 || t.charAt(1)!='.')
                t = endClean.matcher(t).replaceAll("");
            //sws.contains(words[i-1].toLowerCase()) only if the previous token is a symbol, we make an amalgamation
            //sws before a token should not be considered "Bank of Holland", should not have a feature "of Holland", instead "Holland" makes more sense
            if(i>0 && symbols.contains(words[i-1].toLowerCase()))
                t = words[i-1].toLowerCase()+" "+t;

            //for example in Roberston Stephens & Co -- we don't want to see "stephens &" but only "& co"
            if(i<(words.length-1) && (sws.contains(words[i+1].toLowerCase())))//||(symbols.contains(words[i+1].toLowerCase()))))
                t += " "+words[i+1].toLowerCase();

            //emit all the words or patterns
            if (t != null)
                patts.add(t);
        }
        return patts.toArray(new String[patts.size()]);
    }

    /**
     * A generative function, that takes the phrase and generates mixtures.
     * requires mixtures as a parameter, because some of the mixtures depend on this
     * Given an input phrase returns the context of each of the words in the phrase
     * Mr. Robert Creeley, PERSON => {mr.:[L:NULL,R:robert,R:creeley,T:PERSON,DICT:false,SW:false],
     *                                robert:[L:mr.,R:Creeley,T:PERSON,SW:false,DICT:false],
     *                                creeley:[L:mr.,L:robert,R:NULL,T:PERSON,SW:false,DICT:false]}
     * */
    private static Map<String,List<String>> generateFeatures(String phrase, Short type){
        Map<String, List<String>> mixtureFeatures = new LinkedHashMap<>();
        String[] patts = getPatts(phrase);
        String[] words = phrase.split("\\W+");
        String sw = "NULL";

        if(patts.length == 0)
            return mixtureFeatures;

        //scrapped position label (i.e. in the beginning, middle, end) feature for these reasons:
        //1. It is un-normalized, the possible labels are not equally likely
        //2. The left and right mixtures already hold the position info. very tightly
        for(int pi = 0; pi<patts.length; pi++){
            if(sws.contains(patts[pi].toLowerCase()))
                continue;
            for(int wi=0;wi<words.length;wi++){
                String word = words[wi];
                //Generally entries contain only one stop word per phrase, so not bothering which one
                //index>0 check to avoid considering 'A' and 'The' in the beginning
                if(wi>0 && sws.contains(word.toLowerCase()) && !(wi<words.length-1 && patts[pi].equals(word.toLowerCase()+" "+words[wi+1].toLowerCase())) && !(words[wi-1].toLowerCase()+" "+word.toLowerCase()).equals(patts[pi])) {
                    sw = word;
                    break;
                }
            }

            List<String> features = new ArrayList<>();

            for(int pj=0;pj<pi;pj++)
                features.add("L:" + patts[pj]);
            if(pi==0)
                features.add("L:NULL");

            for(int pj=pi+1;pj<patts.length;pj++)
                features.add("R:" + patts[pj]);
            if(pi+1 == patts.length)
                features.add("R:NULL");

            features.add("SW:" + sw);
            features.add("T:" + type);
            //boolean containsAdj = false, containsAdv = false, containsVerb = false, containsPrep = false, containsPronoun = false;
            boolean containsDict = false;
            for(String word: words) {
                word  = word.toLowerCase();
                //consider all the other words, other than this word
                if(!sws.contains(word) && !patts[pi].equals(word) && !patts[pi].contains(" "+word) && !patts[pi].contains(word+" ")) {
                    if(EnglishDictionary.getDict().contains(word)) {
                        containsDict = true;
                        break;
                    }
                }
            }
            if(containsDict)
                features.add("DICT:Y");
            else
                features.add("DICT:N");

            mixtureFeatures.put(patts[pi].toLowerCase(), features);
        }
        return mixtureFeatures;
    }

    /**If the phrase is of OTHER type, then consider no chunks and emit mixtures for every word*/
    public static Map<String,List<String>> generateFeatures2(String phrase, Short type){
        Map<String,List<String>> features = new LinkedHashMap<>();
        if(type == NEType.Type.OTHER.getCode()){
            String[] words = getPatts(phrase);
            for(String w: words){
                Map<String,List<String>> map = generateFeatures(w, type);
                for(String m: map.keySet())
                    features.put(m, map.get(m));
            }
            return features;
        }
        features = generateFeatures(phrase, type);
        Map<String,List<String>> ffeatures = new LinkedHashMap<>();
        for(String f: features.keySet())
            if(f.length()>1)
                ffeatures.put(f, features.get(f));
        return ffeatures;
    }

    public static void main(String[] args) {
        String[] test = new String[]{"Settlement|PopulatedPlace|Place","Town|Settlement|PopulatedPlace|Place","Road|RouteOfTransportation|Infrastructure|ArchitecturalStructure|Place",
        "Village|Settlement|PopulatedPlace|Place","Building|ArchitecturalStructure|Place"};
        for(String t: test) {
            NEType.Type type = NEType.parseDBpediaType(t);
            System.out.println(t + " - " + type.getCode() + " - " + NEType.getCoarseType(type));
        }
        for(NEType.Type t: NEType.getAllTypes())
            System.out.println(t+" -- "+NEType.getCoarseType(t));
        System.out.println(generateFeatures("Fulton Street", NEType.Type.PLACE.getCode()));
    }
}
