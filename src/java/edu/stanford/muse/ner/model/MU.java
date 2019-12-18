package edu.stanford.muse.ner.model;

import edu.stanford.muse.ner.featuregen.FeatureUtils;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Features for a word that corresponds to a mixture
 * left and right labels, LABEL is one of: LOC, ORG,PER, OTHER, NEW, stop words, special chars
 * Change log
 * 1. Changed all the MU values from double to float to reduce the model size and we are not interested (actually undesired) to have very small values in MU*/
public class MU implements Serializable {
    private static final Logger log =  LogManager.getLogger(MU.class);
    static final long serialVersionUID = 1L;
    private static final String[] POSITION_LABELS = new String[]{"S","B","I","E"};
    private static final String[] WORD_LABELS = new String[NEType.getAllTypes().length+1];
    private static final String[] TYPE_LABELS = new String[NEType.getAllTypes().length];
    private static final String[] BOOLEAN_VARIABLES = new String[]{"Y","N"};
    private static final String[] DICT_LABELS = BOOLEAN_VARIABLES;
    static{
        NEType.Type[] allTypes = NEType.getAllTypes();
        for(int i=0;i< allTypes.length;i++)
            WORD_LABELS[i] = allTypes[i].getCode()+"";
        for(int i=0;i< allTypes.length;i++)
            TYPE_LABELS[i] = allTypes[i].getCode()+"";
        WORD_LABELS[WORD_LABELS.length-1] = "NULL";
    }

    public String id;
    //static int NUM_WORDLENGTH_LABELS = 10;
    //feature and the value, for example: <"LEFT: and",200>
    //indicates if the values are final or if they have to be learned
    private Map<String,Float> muVectorPositive;
    public float numMixture;
    //total number of times, this mixture is considered
    public float numSeen;

    private MU(String id){
        initialize(id, null);
    }

    public MU(String id, Map<String, Float> initialParams) {
        initialize(id, initialParams);
    }
    //Smooth param alpha is chosen based on alpha*35(ie. number of types) = an evidence number you can trust.
    //with 0.2 it is 0.2*35=7; If the token has appeared at least seven times, I can start believing
    private static final float SMOOTH_PARAM = 0.2f;

    public static double getMaxEntProb(){
        return (1.0/ MU.WORD_LABELS.length)*(1.0/ MU.WORD_LABELS.length)*(1.0/ MU.TYPE_LABELS.length)*(1.0/ MU.DICT_LABELS.length)*(1.0/(FeatureUtils.sws.size()+1));
    }

    //Since we depend on tags of the neighbouring tokens in a big way, we initialize so that the mixture likelihood with type is more precise.
    //and the likelihood with all the other types to be equally likely
    //alpha is the parameter related to dirichlet prior, though the param is called alpha it is treated like alpha-1; See paper for more details
    private void initialize(String id, Map<String, Float> initialParams) {
        muVectorPositive = new LinkedHashMap<>();
        this.numMixture = 0;
        this.numSeen = 0;
        if (initialParams != null) {
            initialParams.entrySet().forEach(e->muVectorPositive.put(e.getKey(),e.getValue()));
            numMixture = (float)initialParams.entrySet().stream().filter(e->e.getKey().startsWith("T:")).mapToDouble(Map.Entry::getValue).sum();
            numSeen = numMixture;
        }
        this.id = id;
    }

    //returns smoothed P(type/this-mixture)
    private float getLikelihoodWithType(String typeLabel){
        float p1, p2;

        for(String tl: TYPE_LABELS) {
            if(("T:"+tl).equals(typeLabel)) {

                if(muVectorPositive.containsKey(typeLabel)) {
                    p1 = muVectorPositive.get(typeLabel);
                    p2 = numMixture;
                    return (p1 + SMOOTH_PARAM) / (p2 + NEType.getAllTypes().length*SMOOTH_PARAM);
                }
                //its possible that a mixture has never seen certain types
                else
                    return (SMOOTH_PARAM)/(numMixture + NEType.getAllTypes().length*SMOOTH_PARAM);
            }
        }
        log.warn("!!!FATAL: Unknown type label: " + typeLabel + "!!!");
        if(log.isDebugEnabled()) {
            log.debug("Expected one of: ");
            for (String tl : TYPE_LABELS)
                log.debug(tl);
        }
        return 0;
    }

    public float getLikelihoodWithType(Short typeCode){
        return getLikelihoodWithType("T:"+typeCode);
    }

    //gets number of symbols in the dimension represented by this feature
    private static int getNumberOfSymbols(String f){
        if(f.startsWith("L:")||f.startsWith("R:"))
            return WORD_LABELS.length;
        if(f.startsWith("T:"))
            return TYPE_LABELS.length;
        for(String str: POSITION_LABELS)
            if(f.endsWith(str))
                return POSITION_LABELS.length;
        if(f.startsWith("SW:"))
            return FeatureUtils.sws.size()+1;
        if(f.startsWith("DICT:")||f.startsWith("ADJ:")||f.startsWith("ADV:")||f.startsWith("PREP:")||f.startsWith("V:")||f.startsWith("PN:")||f.startsWith("POS:"))
            return 2;
        log.error("!!!REALLY FATAL!!! Unknown feature: " + f);
        return 0;
    }

    //mixtures also include the type of the phrase
    //returns P(mixtures/this-mixture)
    public double getLikelihood(List<String> features) {
        double p = 1.0;

        int numLeft = 0, numRight = 0;
        for (String f: features) {
            if(f.startsWith("L:"))
                numLeft++;
            else if(f.startsWith("R:"))
                numRight++;
        }
        //numLeft and numRight will always be greater than 0
        for (String f : features) {
            int v = getNumberOfSymbols(f);
            double val;
            Float freq = muVectorPositive.get(f);
            val = ((freq==null?0:freq) + SMOOTH_PARAM) / (numMixture + v*SMOOTH_PARAM);

            if (Double.isNaN(val)) {
                log.warn("Found a NaN here: " + f + " " + muVectorPositive.get(f) + ", " + numMixture + ", " + val);
                log.warn(toString());
            }

            if(f.startsWith("L:"))
                val = Math.pow(val, 1.0f/numLeft);
            else if(f.startsWith("R:"))
                val = Math.pow(val, 1.0f/numRight);

            p *= val;
        }
        return p;
    }

    //where N is the total number of observations, for normalization
    public float getNumSeenEffective(){
        if(numSeen == 0) {
            //two symbols here SEEN and UNSEEN, hence the smoothing; the prior here makes no sense, but code never reaches here...
            //log.warn("FATAL!!! Number of times this mixture is seen is zero, that can't be true!!!");
            return 1.0f;
        }
        return (numMixture);///(numSeen+alpha_pi);
    }

    /**Maximization step in EM update,
     * @param resp - responsibility of this mixture in explaining the type and mixtures
     * @param features - set of all *relevant* mixtures to this mixture*/
    public void add(Float resp, List<String> features, Map<String,Float> alpha) {
        //if learn is set to false, ignore all the observations
        if (Float.isNaN(resp))
            log.warn("Responsibility is NaN for: " + features);
        numMixture += resp;
        numSeen += 1;

        String type="NULL";
        for (String f: features)
            if(f.startsWith("T:")) {
                type = f.substring(f.indexOf(":") + 1);
                break;
            }
        int numLeft = 0, numRight = 0;
        for (String f: features) {
            if(f.startsWith("L:"))
                numLeft++;
            else if(f.startsWith("R:"))
                numRight++;
        }
        Map<String,Float> alpha_0 = new LinkedHashMap<>();
        for(String val: alpha.keySet()) {
            String dim = val.substring(0, val.indexOf(':'));
            if(!alpha_0.containsKey(dim))
                alpha_0.put(dim, 0f);
            alpha_0.put(dim, alpha_0.get(dim) + alpha.get(val));
        }

        for (String f : features) {
            if(f.equals("L:"+SequenceModel.UNKNOWN_TYPE)) f = "L:"+type;
            if(f.equals("R:"+SequenceModel.UNKNOWN_TYPE)) f = "R:"+type;
            float fraction = 1;
            if(f.startsWith("L:")) fraction = 1.0f/numLeft;
            if(f.startsWith("R:")) fraction = 1.0f/numRight;
            if (!muVectorPositive.containsKey(f)) {
                muVectorPositive.put(f, 0.0f);
            }
            String dim = f.substring(0,f.indexOf(':'));
            float alpha_k = alpha.getOrDefault(f,0f);
            float alpha_k0 = alpha_0.getOrDefault(dim,0f);
            assert alpha_k0>=alpha_k;
            //for left and right semantic type: we are supposed to add to numMixture numLeft or numRight times.
            // Instead we correct for that by multiplying the numerator with 1/numLeft or 1/numRight
            muVectorPositive.put(f, muVectorPositive.get(f) + (fraction*(1+alpha_k)/(1+alpha_k0))*resp);
        }
    }

    public double difference(MU mu){
        if(this.muVectorPositive == null)
            return 0.0;
        double d = 0;
        for(String str: muVectorPositive.keySet()){
            double v1 = 0, v2 = 0;
            if(numMixture>0)
                v1 = muVectorPositive.get(str)/numMixture;
            if(mu.muVectorPositive.containsKey(str) && mu.numMixture>0)
                v2 = mu.muVectorPositive.get(str)/mu.numMixture;
            d += Math.pow(v1-v2,2);
        }
        double res = Math.sqrt(d);
        if(Double.isNaN(res)) {
            System.err.println("============================");
            for(String str: muVectorPositive.keySet()){
                if(mu.muVectorPositive.get(str)==null){
                    //that is strange, should not happen through the way this method is being used
                    continue;
                }
                System.err.println((muVectorPositive.get(str)/numMixture));
                System.err.println((mu.muVectorPositive.get(str)/mu.numMixture));
            }
            System.err.println(numMixture + "  " + mu.numMixture);
        }
        return res;
    }

    @Override
    public String toString(){
        String str = "";
        String p[] = new String[]{"L:","R:","T:","SW:","DICT:"};
        String[][] labels = new String[][]{WORD_LABELS,WORD_LABELS,TYPE_LABELS, FeatureUtils.sws.toArray(new String[FeatureUtils.sws.size()]),DICT_LABELS};
        str += "ID: " + id + "\n";
        for(int i=0;i<labels.length;i++) {
            Map<String,Float> some = new LinkedHashMap<>();
            for(int l=0;l<labels[i].length;l++) {
                String d = p[i] + labels[i][l];
                String dim = p[i].substring(0,p[i].length()-1);

                Float v = muVectorPositive.get(d);
                some.put(d, (((v==null)?0:v)) / (numMixture));
            }
            List<Pair<String,Float>> smap;
            smap = Util.sortMapByValue(some);
            for(Pair<String,Float> pair: smap)
                str += pair.getFirst()+":"+pair.getSecond()+"-";
            str += "\n";
        }
        str += "NM:"+numMixture+", NS:"+numSeen+"\n";
        return str;
    }

    public String prettyPrint(){
        String p[] = new String[]{"L:","R:","T:","SW:","DICT:"};
        List<String> swLabels = new ArrayList<>();
        swLabels.addAll(FeatureUtils.sws);
        swLabels.add("NULL");
        String[][] labels = new String[][]{WORD_LABELS,WORD_LABELS,TYPE_LABELS, swLabels.toArray(new String[swLabels.size()]), DICT_LABELS};
        StringBuilder sb = new StringBuilder();
        sb.append("Token: " + EmailUtils.uncanonicaliseName(id) + "\n");
        sb.append("ID: " + id + "\n");
        for(int i=0;i<labels.length;i++) {
            Map<String,Float> some = new LinkedHashMap<>();
            for(int l=0;l<labels[i].length;l++) {
                String k = p[i] + labels[i][l];
                String d;
                if(i==0 || i==1 || i==2)
                    d = p[i].replaceAll(":","") + "[" + (labels[i][l].equals("NULL")?"EMPTY": NEType.getTypeForCode(Short.parseShort(labels[i][l]))) + "]";
                else
                    d = p[i].replaceAll(":","") + "[" + labels[i][l] + "]";

                if(muVectorPositive.get(k) != null) {
                    some.put(d, (muVectorPositive.get(k)) / (numMixture));
                }
                else
                    some.put(d, 0.0f);
            }
            List<Pair<String,Float>> smap;
            smap = Util.sortMapByValue(some);
            int numF = 0;
            for(Pair<String,Float> pair: smap) {
                if(numF>5 || pair.getSecond()<=0)
                    break;

                sb.append(pair.getFirst() + ":" + new DecimalFormat("#.####").format(pair.getSecond()) + ":::");
                numF++;
            }
            sb.append("\n");
        }
        sb.append("Active Evidence: "+numMixture+"\n");
        sb.append("Evidence: "+numSeen+"\n");
        return sb.toString();
    }

    /**To parse what is printed with prettyPrint routine*/
    public static MU parseFromText(String[] lines){
        if(lines == null || lines.length<8){
            log.warn("unexpected number of lines in the parse text!!! Please make sure the model is compatible with the version of software.\n" +
                    "Expected exactly 8 lines "+(lines==null?("passed null object"):", found: "+lines.length+" lines")+"\n"+
                    "Cannot continue... returning null");
            return null;
        }

        String id = lines[1].substring(4);
        MU mu = new MU(id);
        //System.out.println("Parsing...");
        //Stream.of(lines).forEach(System.out::println);
        String p[] = new String[]{"L:","R:","T:","SW:","DICT:"};
        IntStream.range(0, p.length).forEach(i->{
            //continue if the line is empty
            if (lines[2+i].length() > 0) {
                String[] fields = lines[2 + i].split(":::");
                Stream.of(fields).forEach(f -> {
                    String[] ov = f.split(":");
                    if (ov.length == 2) {
                        String o = ov[0];
                        o = o.replace("[", ":");
                        o = o.replace("]", "");
                        if (o.endsWith(":EMPTY"))
                            o = o.replace(":EMPTY", ":NULL");

                        if(i<3){
                            String[] fs = o.split(":");
                            if(fs.length!=2)
                                System.out.println("Bad line: "+lines[2+i]+" {"+o+","+f+"}");
                            if(!fs[1].equals("NULL")) {
                                short type_code = NEType.Type.valueOf(fs[1]).getCode();
                                o = fs[0] + ":" + type_code;
                            }
                        }

                        try {
                            float v = Float.parseFloat(ov[1]);
                            mu.muVectorPositive.put(o, v);
                        }catch(NumberFormatException ne){
                            log.warn("Unrecognizable line: " + lines[2+i] + " found while parsing.");
                        }
                    }
                    else
                        log.warn("Suspicious field: "+f+" while parsing line: "+lines[2 + i]);
                });
            } else{
                mu.muVectorPositive.put(p[i]+"NULL", 1f);
            }
        });
        mu.numMixture = Float.parseFloat(lines[2 + p.length].substring("Active Evidence: ".length()));
        mu.numSeen = Float.parseFloat(lines[2 + p.length + 1].substring("Evidence: ".length()));

        //make sure the features are normalized
        IntStream.range(0, p.length).forEach(i->{
            float tot = mu.muVectorPositive.entrySet().stream().filter(e->e.getKey().startsWith(p[i])).map(Map.Entry::getValue).reduce(Float::sum).orElse(1E-8f);
            if(tot>0)
                mu.muVectorPositive.entrySet().stream().filter(e->e.getKey().startsWith(p[i])).forEach(e->{
                    mu.muVectorPositive.put(e.getKey(), (e.getValue()*mu.numMixture)/tot);
                });
            //This is the case where all the features of certain type are set to 0; for example: affiliation with any type to the left is set to 0, possibly deliberate
            else{
                List<String> rkeys = mu.muVectorPositive.keySet().stream().filter(k -> k.startsWith(p[i])).collect(Collectors.toList());
                rkeys.stream().forEach(k -> mu.muVectorPositive.remove(k));
                if(i==0||i==1||i==3)
                    mu.muVectorPositive.put(p[i]+"NULL",1f);
                else if(i==2) {
                    log.error("Mixture with ID: "+mu.id+" is not associated with any type, generally the case of bad model editing!!!\n" +
                            "Since the problem is only with one of many mixtures, this is not fatal, but may affect performance... continuing");
                    mu.muVectorPositive.put(p[i]+NEType.Type.PERSON.getCode(), 1f);
                }
                else if(i==4)
                    mu.muVectorPositive.put(p[i]+"N",1f);
            }
        });
        return mu;
    }
}
