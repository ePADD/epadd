package edu.stanford.muse.ner.model;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Multimap;
import edu.stanford.muse.Config;
import edu.stanford.muse.ner.dictionary.EnglishDictionary;
import edu.stanford.muse.ner.tokenize.CICTokenizer;
import edu.stanford.muse.ner.tokenize.Tokenizer;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.util.*;
import edu.stanford.muse.webapp.JSPHelper;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.StopAnalyzer;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
 * Created by Chinmay on 22 Nov 2020.
 * This class implements a simple Naive Bayes classifier to assign entity types to CIC words
 * This model gets trained on DBPedia instance types
 */

/**
 * We want to maximize the probability P(C/CIC) for a given CIC and entity type C
 * We represent Tok(CIC) as the set of all tokens present in that CIC [except stop words].
 * They together constitute the feature list of that CIC.
 * P(C/Tok(CIC))  is the probability that given CIC (represented by that feature set Tok(CIC)) is of type C.
 * P(C/Tok(CIC)) = [P(x1/C).P(x2/C)...P(xn/C).P(C)]/P(x1).P(x2)..P(xn)]
 * We chose that C for which P(x1/C).P(x2/C)...P(xn/C).P(C) is maximized where x1, x2,... xn are in Tok(CIC)
 *
 * We create two data structures to calculate P(C/Tok(CIC)) and then chose that C for which it is maximum.
 *
 * A map to hold P(x/C) for every C and for every x present in Tok(Word) where Word is a dbpedia entity.
 * A map to hold P(C) for every C
 */

public class NBModel implements NERModel, Serializable {
    public static String MODEL_FILENAME = "NBModel.txt";
    private static final String GAZETTE_FILE = "gazettes.ser.gz";
    public static final String RULES_DIRNAME = "rules";
    private static final long serialVersionUID = 1L;
    private static final Logger log =  LogManager.getLogger(NBModel.class);
    //public static final int MIN_NAME_LENGTH = 3, MAX_NAME_LENGTH = 100;
    private static FileWriter fdw = null;
    private static Tokenizer tokenizer = new CICTokenizer();
    private static String PROBABILITY_CLASS_SEPARATOR="##############################";
    private static final boolean DEBUG = false;
    static final short UNKNOWN_TYPE = -10;

    static final double MIN_PROB = 0.00001f;

    //Variables to hold P(x/C) and P(C).
    public Map<String, Map<NEType.Type,Float>> conditionalProb = new LinkedHashMap<>();
    public Map<NEType.Type, Float> classProb = new LinkedHashMap<>();


    private static final Random rand = new Random(1);

    //mixtures of the BMM model
    private Map<String, MU> mixtures = new LinkedHashMap<>();
    //Keep the ref. to the gazette lists it is trained on so that we can lookup these when extracting entities.
    private final Map<String,String> gazettes;

    private NBModel(Map<String, MU> mixtures, Map<String, String> gazettes) {
        this.mixtures = mixtures;
        this.gazettes = gazettes;
        {
            CharArraySet stopWordsSet = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
            stopWords = new LinkedHashSet<>();//new String[stopWordsSet.size()];
            Iterator it = stopWordsSet.iterator();
            int j = 0;
            while (it.hasNext()) {
                char[] stopWord = (char[]) it.next();
                stopWords.add(new String(stopWord));
            }
        }
    }

    Set<String> stopWords=null;

    @Override
    public void setTokenizer(Tokenizer tokenizer){
        NBModel.tokenizer = tokenizer;
    }

    //Input is a token and returns the best type assignment for token
    private static Short getType(String token, Map<String,MU> mixtures) {
        MU mu = mixtures.get(token);
        if (mu == null) {
            //log.warn("Token: "+token+" not initialised!!");
            return UNKNOWN_TYPE;
        }
        Short[] allTypes = NEType.getAllTypeCodes();
        Short bestType = allTypes[rand.nextInt(allTypes.length)];
        double bv = 0;

        //We don't consider OTHER as even a type
        for (Short type : allTypes) {
            if (!type.equals(NEType.Type.OTHER.getCode())) {
                double val = mu.getLikelihoodWithType(type);
                if (val > bv) {
                    bv = val;
                    bestType = type;
                }
            }
        }
        return bestType;
    }

    private static double getPrior(MU mu, Map<String,MU> mixtures){
        return mu.getNumSeenEffective()/mixtures.size();
    }

    private double getPrior(MU mu){
        return mu.getNumSeenEffective()/mixtures.size();
    }

    /**
     * Generalizes mixtures by replacing literal labels with its type such as Paris Weekly => $CITY Weekly
     * {robert:[L:NULL,R:creeley,T:PERSON,SW:NULL,DICT:FALSE],creeley:[L:robert,R:NULL,T:PERSON,SW:NULL,DICT:FALSE]}
     * ==>
     * {robert:[L:NULL,R:PERSON,T:PERSON,SW:NULL,DICT:FALSE],creeley:[L:PERSON,R:NULL,T:PERSON,SW:NULL,DICT:FALSE]}
     * */
    private static Map<String,List<String>> typeFeatures(Map<String,List<String>> features, Map<String,MU> mixtures){
        Map<String, List<String>> nwfs = new LinkedHashMap<>();
        features.keySet().forEach(k->{
            List<String> fs = new ArrayList<>();
            features.get(k).forEach(f->{
                if(f.startsWith("L:") && !f.equals("L:NULL"))
                    fs.add("L:"+getType(f.substring(2), mixtures));
                else if(f.startsWith("R:") && !f.equals("R:NULL"))
                    fs.add("R:"+getType(f.substring(2), mixtures));
                else
                    fs.add(f);
            });
            nwfs.put(k, fs);
        });
        return nwfs;
    }

    /**
     * Returns a probabilistic measure for
     * @param phrase to be a noun
     * @param nonNoun if true, then returns P(~noun/phrase) = 1-P(noun/phrase)
     * */
    private static double getNounLikelihood(String phrase, boolean nonNoun) {
        phrase = phrase.replaceAll("^\\W+|\\W+$", "");
        if (phrase.length() == 0) {
            if (nonNoun)
                return 1;
            else
                return 1.0 / Double.MAX_VALUE;
        }

        String[] tokens = phrase.split("\\s+");
        double p = 1;
        for (String token : tokens) {
            String orig = token;
            token = token.toLowerCase();
            List<String> noise = Arrays.asList("P.M", "P.M.", "A.M.", "today", "saturday", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december", "thanks");
            if (noise.contains(token)) {
                if (nonNoun)
                    p *= 1;
                else
                    p *= 1.0 / Double.MAX_VALUE;
                continue;
            }
            //Map<String,Pair<Integer,Integer>> map = EnglishDictionary.getDictStats();
            //Pair<Integer,Integer> pair = map.get(token);
            Multimap<String, Pair<String, Integer>> map = EnglishDictionary.getTagDictionary();//getDictStats();
            Collection<Pair<String, Integer>> pairs = map.get(token);

            if (pairs == null) {
                //log.warn("Dictionary does not contain: " + token);
                if (orig.length() == 0) {
                    if (nonNoun)
                        p *= 1;
                    else
                        p *= 1.0 / Double.MAX_VALUE;
                }
                if (orig.charAt(0) == token.charAt(0)) {
                    if (nonNoun)
                        p *= 1;
                    else
                        p *= 1.0 / Double.MAX_VALUE;
                } else {
                    if (nonNoun)
                        p *= 1.0 / Double.MAX_VALUE;
                    else
                        p *= 1.0;
                }
                continue;
            }
            //double v = (double) pair.getFirst() / (double) pair.getSecond();
            double v = pairs.stream().filter(pair->pair.first.startsWith("NN")||pair.first.startsWith("JJ")).mapToDouble(pair->pair.second).sum();
            v /= pairs.stream().mapToDouble(pair->pair.second).sum();
            //if (v > 0.25) {
            if(v > 0.25) {
                if (nonNoun)
                    return 1.0 / Double.MAX_VALUE;
                else
                    return 1.0;
            } else {
                if (token.charAt(0) == orig.charAt(0)) {
                    if (nonNoun)
                        return 1;
                    else
                        return 1.0 / Double.MAX_VALUE;
                } else {
                    if (nonNoun)
                        return 1.0 / Double.MAX_VALUE;
                    else
                        return 1.0;
                }
            }
        }
        return p;
    }

    private String lookup(String phrase) {
        //if the phrase is from CIC Tokenizer, it won't start with an article
        //enough with the confusion between [New York Times, The New York Times], [Giant Magellan Telescope, The Giant Magellan Telescope]
        Set<String> vars = new LinkedHashSet<>();
        vars.add(phrase);
        vars.add("The "+phrase);
        String type;
        for(String var: vars) {
            type = gazettes.get(var.toLowerCase());
            if(type!=null) {
                log.debug("Found a match for: "+phrase+" -- "+type);
                return type;
            }
        }
        return null;
    }



    public Span[] find (String content){
        if (content == null)
        {
            log.warn("content null in NBModel.find()");
            return null;
        }
        List<Span> spans = new ArrayList<>();

        opennlp.tools.util.Span[] sentSpans = NLPUtils.tokenizeSentenceAsSpan(content);
        assert sentSpans!=null;
        for(opennlp.tools.util.Span sentSpan: sentSpans) {
            String sent = sentSpan.getCoveredText(content).toString();
            int sstart = sentSpan.getStart();

            List<Triple<String, Integer, Integer>> toks = tokenizer.tokenize(sent);
            for (Triple<String, Integer, Integer> t : toks) {
                //this should never happen
                if(t==null || t.first == null)
                    continue;

                Map<String,Pair<Short,Double>> entities = seqLabel(t.getFirst());
                for(String e: entities.keySet()){
                    Pair<Short,Double> p = entities.get(e);
                    //A new type is assigned to some words, which is of value -2
                    if(p.first<0)
                        continue;

                    if(!p.first.equals(NEType.Type.OTHER.getCode()) && p.second>0) {
                        Span chunk = new Span(e, sstart + t.second + t.first.indexOf(e), sstart + t.second + t.first.indexOf(e) + e.length());
                        chunk.setType(p.first, new Float(p.second));
                        spans.add(chunk);
                    }
                }
            }
        }
        return spans.toArray(new Span[spans.size()]);
    }
    private Map<String, Pair<Short, Double>> seqLabel(String phrase) {
        Map<String, Pair<Short, Double>> segments = new LinkedHashMap<>();
        {
            String dbpediaType = lookup(phrase);
            NEType.Type type = NEType.parseDBpediaType(dbpediaType);

            if (dbpediaType != null && (phrase.contains(" ") || dbpediaType.endsWith("Country|PopulatedPlace|Place"))) {
                segments.put(phrase, new Pair<>(type.getCode(), 1.0));
                return segments;
            }
        }

        //This step of uncanonicalizing phrases helps merging things that have different capitalization and in lookup
        phrase = EmailUtils.uncanonicaliseName(phrase);

        if (phrase == null || phrase.length() == 0)
            return new LinkedHashMap<>();
        phrase = phrase.replaceAll("^\\W+|\\W+^", "");

        String[] tokens = phrase.split("\\s+");

        /**
         * In TW's sub-archive with ~65K entities scoring more than 0.001. The stats on frequency of #tokens per word is as follows
         * Freq  #tokens
         * 36520 2
         * 15062 3
         * 5900  4
         * 2645  5
         * 2190  1
         * 1301  6
         * 721   7
         * 18    8
         * 9     9
         * 2     10
         * 1     11
         * Total: 64,369 -- hence the cutoff below
         */
        if (tokens.length > 9) {
            return new LinkedHashMap<>();
        }

        StringBuilder modifiedPhrase = new StringBuilder();
        //We need to work on Phrase not on individual tokens. But we still iterate over tokens to remove those tokens which are very small.
        //and build a modifiedPhrase by concatenating the remaining tokens.
        for (String token : tokens) {
            //For each token B1
            if (token.length() != 2 || token.charAt(1) != '.')
                token = token.replaceAll("^\\W+|\\W+$", "");
            token = token.toLowerCase();
            if (token.length() < 2) {
                //System.out.println("Skipping: "+token+" due to very small size");
                continue;
            }
            modifiedPhrase.append(token);
            modifiedPhrase.append(" ");
        }
        String modifiedPhraseStr = modifiedPhrase.toString().trim();
        Pair<NEType.Type, Double> candidate = new Pair(NEType.Type.OTHER, 0D);

        //Now we iterate over all types to see the type for which the likelihood is maximum.
        for (NEType.Type candType : NEType.getAllTypes()) {
            double likelihood = getConditionalProb(modifiedPhraseStr, candType);
            if (likelihood - candidate.second > 1e-25) {
                //means likelihood is greater than old value of likelihood.
                candidate.first = candType;
                candidate.second = likelihood;
            }
        }

        //At the end put candidate information in segment map.
        segments.put(phrase, new Pair(candidate.first.getCode(), candidate.second));
        return segments;
    }

    /*
    This method computes P() [ probablity of a phrase containing tokesn B1, B2, etc.
     */
    private double getConditionalProb(String phrase, NEType.Type type){
        double likelihood=1D;

        String[] tokens = phrase.split("\\s+");

        //Get the Probability(Token/type) by looking at the map conditionalProb
        for(String origtoken: tokens) {
            String token = origtoken.toLowerCase().trim();
            if (conditionalProb.get(token) != null) {
                if (conditionalProb.get(token).get(type) != null) {
                   /* if(type==NEType.Type.PERSON && conditionalProb.get(token).get(type)==1.0D ){
                        likelihood=1D;
                        return likelihood;
                    }*/
                    likelihood = likelihood *(conditionalProb.get(token).get(type));
                } else {
                    //means the token is present but the probability of this token wrt the given type is not present.
                    likelihood = likelihood * MIN_PROB;
                }
            } else {
                //means no information about this token, just add a minimum amount.
                likelihood = likelihood * MIN_PROB;
            }
        }
        //once done multiply by the probability of this class by reading from classProb
        likelihood=likelihood *(classProb.get(type));

        return likelihood;
    }


    private static synchronized Map<String,String>  loadGazette(String modelDirName){
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new GZIPInputStream(Config.getResourceAsStream(modelDirName + File.separator + GAZETTE_FILE)));
            Map<String,String> model = (Map<String, String>) ois.readObject();
            ois.close();
            return model;
        } catch (Exception e) {
            Util.print_exception("Exception while trying to load gazette from: " + modelDirName, e, log);
            return null;
        }
    }

    public static synchronized NBModel loadModelFromRules(String rulesFileName) {
        Map<String, String> gazette = loadGazette(NBModel.RULES_DIRNAME);

        NBModel nbModel = new NBModel(null,gazette);
        String rulesFile = Config.DEFAULT_SETTINGS_DIR+File.separator+rulesFileName;
        if (!new File(rulesFile).exists()) {
            log.fatal("The supplied file: " + rulesFile + " does not exist!\n" +
                    "Cannot continue, exiting....");
            return null;
        }
        //READ the content of the file line by line. The structure of the file is as following
        /*
        CICToken,EntityType,P(CICToken/EntityType)
        ...
        ..
        #########
        EntityType, P(EntityType)
        ...
        ...
         */
        int type=1;//Variable to keep track of which part of the file we are reading. The first part is for CICToken/EntityType probability
        //and the second type (after PROBABILITY_CLASS_SEPARATOR is encountered in the file) is for P(EntityType).
        try(LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(rulesFile)))) {
            while (true) {
                String line = lnr.readLine();
                if (line == null)
                    break;
                else if(line.trim().equals(PROBABILITY_CLASS_SEPARATOR)){
                    //Means now onwards we are going to read P(EntityType) and store it in classProb variable of NBModel
                    type=2;
                    continue;
                }

                line = line.trim();
                if(type==1){
                    //The line is of the form CICToekn,EntityType,float
                    String info[] = line.split(",");
                    String cictoken = info[0].trim();
                    NEType.Type etype = NEType.getTypeForDisplayCode(info[1].trim());
                    //System.out.println(cictoken);
                    //System.out.println(info[2]) ;
                    float prob = Float.parseFloat(info[2].trim());
                    nbModel.setConditionalProbability(cictoken,etype,prob);
                }else{
                    //The line is of the form EntityType,float
                    String info[] = line.split(",");
                    NEType.Type etype = NEType.getTypeForDisplayCode(info[0].trim());
                    float prob = Float.parseFloat(info[1].trim());
                    nbModel.setEntityProbability(etype,prob);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return nbModel;
    }

    public void setEntityProbability(NEType.Type etype, float prob) {
        classProb.put(etype,prob);
    }

    public void setConditionalProbability(String cictoken, NEType.Type etype, float prob) {
        if(!conditionalProb.containsKey(cictoken)){
            Map<NEType.Type, Float> typeinfo = new LinkedHashMap<>();
            typeinfo.put(etype,prob);
            conditionalProb.put(cictoken,typeinfo);
        }else if(!conditionalProb.get(cictoken).containsKey(etype)){
            conditionalProb.get(cictoken).put(etype,prob);
        }else
            conditionalProb.get(cictoken).put(etype,prob);
    }


    //returns token -> {redirect (can be the same as token), page length of the page it redirects to}
    private static Map<String,Map<String,Integer>> getTokenTypePriors(){
        Map<String,Map<String,Integer>> pageLengths = new LinkedHashMap<>();
        log.info("Parsing token types...");
        try{
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(Config.getResourceAsStream("TokenTypes.txt")));
            String line;
            while((line=lnr.readLine())!=null){
                String[] fields = line.split("\\t");
                if(fields.length!=4){
                    log.warn("Line --"+line+"-- has an unexpected pattern!");
                    continue;
                }
                int pageLen = Integer.parseInt(fields[3]);
                String redirect = fields[2];
                //if the page is not a redirect, then itself is the title
                if(fields[2] == null || fields[2].equals("null"))
                    redirect = fields[1];
                String lc = fields[0].toLowerCase();
                if(!pageLengths.containsKey(lc))
                    pageLengths.put(lc, new LinkedHashMap<>());
                pageLengths.get(lc).put(redirect, pageLen);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return pageLengths;
    }

    private Span[] findEntitiesFromCICFile(String filename){
        //get all triples from CIC file..
        List<Triple<String,Integer,Integer>> ciclist = readCICFromFile(filename);
        List<Span> spans = new ArrayList<>();
        int sstart=0;//just added to keep the code in loop same as of find() method above. This was used to calculate the position of the span
        //with respect to the whole message. In future we can add message id and line number in CIC info as well.
        for (Triple<String, Integer, Integer> t : ciclist) {
                //this should never happen
                if(t==null || t.first == null)
                    continue;

                Map<String,Pair<Short,Double>> entities = seqLabel(t.getFirst());
                for(String e: entities.keySet()){
                    Pair<Short,Double> p = entities.get(e);
                    //A new type is assigned to some words, which is of value -2
                    if(p.first<0)
                        continue;

                    if(!p.first.equals(NEType.Type.OTHER.getCode()) && p.second>0) {
                        Span chunk = new Span(e, sstart + t.second + t.first.indexOf(e), sstart + t.second + t.first.indexOf(e) + e.length());
                        chunk.setType(p.first, new Float(p.second));
                        spans.add(chunk);
                    }
                }
            }

        return spans.toArray(new Span[spans.size()]);
    }

    private  List<Triple<String, Integer, Integer>> readCICFromFile(String filename){

        List<Triple<String,Integer,Integer>> result = new LinkedList<>();
        //read line.. if contains '###########' then change state to reading..
                      //else if contains '------------------' then change state to notreading
                      //else if state is reading then put int the list of triples..
                      //else if state is notreading then continue;
        int state=0;//0 means not reading, 1 means reading.
        String sampleLine=null;
        try(BufferedReader sampleBuffer = new BufferedReader(new FileReader(filename))) {
            while ((sampleLine = sampleBuffer.readLine()) != null) {
                if(sampleLine.contains("############"))
                    state=1;
                else if(sampleLine.contains("----------------"))
                    state=0;
                else if (state==1) {//means it is a triple of the form Jeb Bush,33,41
                    //split it on ',' and put them as triple
                    String[] res = sampleLine.trim().split(",");
                    //size of res should be 3..
                    result.add(new Triple<>(res[0],Integer.parseInt(res[1].trim()),Integer.parseInt(res[2].trim())));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Use this routine to read an external gazette list, the resource is expected to be a plain text file
     * the lines in the file should be two fields separated by ' ' (space), the first field should be the title and second: its type.
     * The type of the resource should follow the style of DBpedia types in our generated instance file, see NEType.dbpediaTypesMap for more info.
     * for example, a type annotation for building should look like "Building|ArchitecturalStructure|Place"
     * The spaces in the title, ie. the first entry should be replaced by '_'
     */
    private static Map<String,String> readEntityList(String resourcePath) {
        Map<String,String> content = new LinkedHashMap<>();
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(Config.getResourceAsStream(resourcePath)));
        }
        //Not a big deal if one entity listing is missed
        catch(Exception e){
            Util.print_exception("Could not load resource:"+resourcePath, e, log);
            return content;
        }

        String line;
        try {
            while ((line = br.readLine()) != null){
                line = line.trim();
                String[] fs = line.split(" ");
                String title = fs[0];
                title = title.replaceAll("_"," ");
                content.put(title, fs[1]);
            }
        } catch(IOException e){
            log.warn("Could not open and read the resource from "+resourcePath, e);
        }
        log.info("Read "+content.size()+" entries from "+resourcePath);
        log.debug("The top 10 entries");
        int i=0;
        for(Map.Entry<String,String> e: content.entrySet()) {
            log.debug(e.getKey() + " -- " + e.getValue());
            if(i++>=10)
                break;
        }
        return content;
    }

    public static void train() {
        //Map to contain a mapping of CICToken, EntityType -> Count (number of entities containing CIC token out of total entities of type entitytype)
        Map<String, Map<NEType.Type,Integer>> conditionalCount = new LinkedHashMap<>();
        //Map to contain a mapping of EntityType -> Count (number of total entities of type entity type)
        Map<NEType.Type, Integer> categoryTypeCount = new LinkedHashMap<>();
        //Variable to hold total number of entities over which this NB classifier gets trained.
        int totalEntities = 0 ;
        NBModel nbmodel = new NBModel(null,null);
        //Read file instances*.txt line by line.
        try (
                LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(Config.SETTINGS_DIR + File.separator + "instance_types_2014-04.en.txt")));
                Writer nbModelWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Config.SETTINGS_DIR + File.separator + "NBModel.txt")));
                ) {
            String line;
            while ((line = lnr.readLine()) != null) {
                totalEntities++;
                //Break the line into two.. separated by whitespace.
                String[] strs = line.split(" ");
                String entity_name = strs[0], entity_type = strs[1];
                //Now break the entityname on underscore, each one will constitute a token to be used for feature representation of a CIC
                String[] entity_name_tokens = entity_name.split("_");
                //Get the most general type
                NEType.Type entity_type_ = NEType.parseDBpediaType(entity_type);
                //Increment the count of such entity type by 1.
                if(!categoryTypeCount.containsKey(entity_type_))
                    categoryTypeCount.put(entity_type_,0);
                categoryTypeCount.put(entity_type_, categoryTypeCount.get(entity_type_)+1);

                System.out.println(line + "\n");
                for(String entity_name_token: entity_name_tokens){
                    System.out.println(entity_name_token+"####");
                }
                System.out.println("\n");
                System.out.println("Type is: "+ entity_type_.getDisplayName());
                System.out.println("\n");
                //For every entity_name_token check if it is a stop word. If not then increment the value of [token][type] by 1 (if not present then set to 1).
                //Also, do it for only entity types of interest.
                for(String entity_name_token: entity_name_tokens){
                    //Clean comma from entity_name_token if present
                    entity_name_token = entity_name_token.replaceAll(",","");
                    if(!nbmodel.stopWords.contains(entity_name_token.toLowerCase())){ //Proceed only for non-stop words present here.
                        if(!conditionalCount.containsKey(entity_name_token.toLowerCase())) {
                            Map<NEType.Type, Integer> entityCnt = new LinkedHashMap<>();
                            entityCnt.put(entity_type_,0);
                            conditionalCount.put(entity_name_token.toLowerCase(), entityCnt);
                        }else if(!conditionalCount.get(entity_name_token.toLowerCase()).containsKey(entity_type_)){
                            conditionalCount.get(entity_name_token.toLowerCase()).put(entity_type_,0);
                        }
                        //Now increase the count by 1.
                        int newcnt = conditionalCount.get(entity_name_token.toLowerCase()).get(entity_type_)+1;
                        conditionalCount.get(entity_name_token.toLowerCase()).put(entity_type_,newcnt);
                    }
                }
            }

            //If for a token there exists non-zero counts only for one entity type then it means we only found the evidence for that token
            //to be of one type only. We can bump up the probability of that token being of that only type. This will give sufficient weightage
            //to a CIC that contains this especial token. One example to support such bumping up the probability is person types.


            //While loop complete.. Now calculate P(x/C) and P(C) by iterating over maps condtionalCount and categoryTypeCount.
            for(String cicToken: conditionalCount.keySet()){
               /* Here I was checking a hack to bump up the probability of a phrase being person type if it doesn't appear in other types a lot.
               //Check if conditionalCount.get(cicToken).keySet() is of size 1 and that too is Person type.
                //If yes then set the count in conditionalCount.get(cicToken).get(entityType) to categoryTypeCount.get(entityType)
                if( conditionalCount.get(cicToken).keySet().contains(NEType.Type.PERSON) && conditionalCount.get(cicToken).keySet().size()<3){
                    NEType.Type entityType = NEType.Type.PERSON;
                    Integer entityTypeCount = categoryTypeCount.get(entityType);
                    conditionalCount.get(cicToken).put(entityType,entityTypeCount);
                }*/
                //Now calculate P(x/C) and P(C).
                for (NEType.Type entityType: conditionalCount.get(cicToken).keySet()){
                    float entityTypeCount = categoryTypeCount.get(entityType);
                    float cicTokenConditionalCount = conditionalCount.get(cicToken).get(entityType);
                    //Dumpe a line cictoken, entityType, P(cictoken/entityType)=cictokenConditionaCount/entityTypeCount in the file.
                    nbModelWriter.write(cicToken+","+entityType.getDisplayName()+","+(cicTokenConditionalCount/entityTypeCount)+"\n");
                }
            }
            nbModelWriter.write(PROBABILITY_CLASS_SEPARATOR+"\n");
            //Once we are done with putting individual probabilities now we will put P(C) for every class type C
            for(NEType.Type entityType: categoryTypeCount.keySet()){
                float entityCnt = categoryTypeCount.get(entityType);
                nbModelWriter.write(entityType.getDisplayName()+","+entityCnt/totalEntities+"\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        train();
        //NBModel nbModel = NBModel.loadModelFromRules("NBModel.txt");
    }
    public static void mainn(String[] args) {
        Options options = new Options();

        Option input = new Option("i", "input", true, "input file path");
        input.setRequired(false);
        options.addOption(input);

        Option output = new Option("o", "output", true, "output file path");
        output.setRequired(false);
        options.addOption(output);

//        Option test = new Option("t", "test", false, "run test");
//        test.setRequired(false);
//        options.addOption(test);

        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd=null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String inputFilePath = cmd.getOptionValue("input");
        boolean isTest = Boolean.parseBoolean(cmd.getOptionValue("test"));
        if(isTest)
            train();
        else
        {
            //output option must be given if the control reaches here.
            String outputfilePath = cmd.getOptionValue("output");
            if (fdw == null) {
                try {
                    fdw = new FileWriter(new File(System.getProperty("user.home") + File.separator + "epadd-settings" + File.separator + "cache" + File.separator + "mixtures.dump"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.err.println("Loading model...");
            NBModel nerModel;
            //log.info(Util.getMemoryStats());
            {
                nerModel = NBModel.loadModelFromRules("rules");
                if (nerModel == null)
                    train();//nerModel = train();
                Span[] entities = nerModel.findEntitiesFromCICFile(inputFilePath);
                     //write entities in a csv file specified by the output path.. filename should be entities_seq_model.csv
                    try{
                        FileWriter fw = new FileWriter(outputfilePath);
                        CSVWriter csvwriter = new CSVWriter(fw, ',', '"',' ',"\n");

                        // write the header line: "DocID,annotation".
                        List<String> line = new ArrayList<>();
                        line.add ("String");
                        line.add ("type");
                        line.add ("Score");
                        csvwriter.writeNext(line.toArray(new String[line.size()]));

                        // write the records
                        for(Span s: entities){
                            String name = s.getText();
                            String type = NEType.getTypeForCode(s.type).getDisplayName();
                            float score = s.typeScore;
                                line = new ArrayList<>();
                                line.add(name);
                                line.add(type);
                                line.add(new Float(score).toString());
                                csvwriter.writeNext(line.toArray(new String[line.size()]));
                        }
                        csvwriter.close();
                        fw.close();
                    } catch (IOException e) {
                        JSPHelper.doLoggingWarnings("Unable to write docid to annotation map in csv file");
                        return;
                    }
            }
        }
    }
}
