package edu.stanford.muse.ner.tokenize;

import edu.stanford.muse.ner.dictionary.EnglishDictionary;
import edu.stanford.muse.util.*;
import opennlp.tools.util.Span;
import opennlp.tools.util.featuregen.FeatureGeneratorUtil;

import com.google.common.collect.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CIC Pattern based tokenizer
 * This is a utility class to segment pseudo proper nouns from text and emit them
 * Tokenizer is often called in an NER or some other text-processing pipeline so be extra careful about the efficiency
 *
 * TODO: CIC tokenizer fails when the sentence tokenizer fails, it is required to make the sentence tokenizer handle at least a few common abbreviations (such as Col. Mt. Inc. Corp. etc.) to make the application look less stupid; OpenNLP splits on some of the periods falsely
 * TODO: split tokens like P.V. Krishnamoorthi -> P. V. Krishnamoorthi
 * TODO: canonicalize and tokenize words such that stop words irrespective of their capitalised form are recognized, for example: "In American Culture", "IN SPANISH", "A NEW FEDERAL POLICY", "THE PROVOST"
 */
public class CICTokenizer implements Tokenizer, Serializable {
    private static Log log						= LogFactory.getLog(CICTokenizer.class);

    private static Pattern	entityPattern;
    private static Pattern multipleStopWordPattern;
    //NOTE: All the string lists below namely commonStartWords, commonEndWords, badSubstrings are case-insensitive
    //strips these words from the emitted token if they are seen in the start
    //Checks and strips if a phrase contains {[common start word]+" "}
    private static String[] commonStartWords = new String[]{
            "hey","the","a","an","hello","dear","hi","met","from","quote","from:","and","my","our","regarding","quoting","on behalf of","behalf of","sorry","but",
            //include all stop words
            "and","for","a","the","to","at", "in", "of",
            "on","with","ok","your","I","to:","thanks", "let","does", "sure", "thank","you","about","&","yes","if","by","why","said","even","am","respected","although","as"
    };
    //Checks and strips if a phrase contains {" "+[common end word]}
    private static String[] commonEndWords = new String[]{
            "I", "and","for","a","the","to","at", "in", "of"
    };
    //Emitted tokens containing these sub-strings will be dropped
    //checked for {contains " "+[bad substring]+" "} or starts with {[bad substring]+" "} or ends with {" "+[bad substring]}
    private static String[] badSubstrings = new String[]{
            "Not ", "I'm", "I'll","I am","n't","I've"," Have ","I'd", "You've", "We've", "They've",
            //stuff specifically related to emails
            "Email","To","From","Date","Subject", "begin pgp"
    };

    //de often appears in personal names like "Alain de Lille", "Christine de Pizan", "Ellen van Langen"
    //https://en.wikipedia.org/wiki/Portuguese_name#The_particle_.27de.27
    //how useful is "on" in the stop words list
    //Consecutive capital words are allowed to be separated by these words, so this list is more restrictive than general stop words list
    private static List<String> stopWords =  Arrays.asList(
            "and","for","a","the","at", "in", "of",
            //based on occurrence frequency of more than 100 in English DBpedia personal names list of 2014
            "de", "van","von","da","ibn","mac","bin","del","dos","di","la","du","ben","no","ap","le","bint","do", "den"/*John den Braber*/
    );
    private static final long serialVersionUID = 1L;

    static {
        try {
            initPattern();
        } catch (Exception e) {
            Util.print_exception("Exception in init pattern", e, log);
        }
    }

    private static void initPattern() {
        //This def. of a word that can appear in person or non-person names
        String nameP = "[A-Z][A-Za-z0-9'\\-.]*";
        //these are the chars that are allowed to appear between words in the chunk
        //comma is a terrible character to allow, it sometimes crawls in the full list an entity is part of.
        String allowedCharsOther = "\\s&'";
        //allowedCharsPerson = "\\s";

        StringBuilder sp = new StringBuilder();
        int i = 0;
        for (String stopWord : stopWords) {
            sp.append(stopWord);
            if (i++ < (stopWords.size() - 1))
                sp.append("|");
        }
        String stopWordsPattern = "(" + sp.toString() + ")";

        //defines the number of occurrences of allowed chars between words
        String recur = "{1,3}";
        //the person name pattern or entity pattern can match more than one consecutive stop word or multiple appearances of [-.'] which is undesired
        //Hence we do another level of tokenisation with the pattern below
        multipleStopWordPattern = Pattern.compile("(\\s|^)("+stopWordsPattern+"["+allowedCharsOther+"]"+recur+"){2,}|(['-.]{2,})|'s(\\s|$)");

        //[\"'_:\\s]*
        //number of times special chars between words can recur
        String nps = "(" + nameP + "([" + allowedCharsOther + "]" + recur + "(" + nameP + "[" + allowedCharsOther + "]" + recur + "|(" + stopWordsPattern + "[" + allowedCharsOther + "]" + recur + "))*" + nameP + ")?)";
        entityPattern = Pattern.compile(nps);
        log.info("EP: " + nps);
        //allow comma only once after the first word
        //nps = "(" + nameP + "([" + allowedCharsPerson + ",]" + recur + "(" + nameP + "[" + allowedCharsPerson + "]" + recur + ")*" + nameP + ")?)";
        //personNamePattern = Pattern.compile(nps);
    }

    public static void setStopWords(List<String> stopWords){
        CICTokenizer.stopWords = stopWords;
        initPattern();
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public List<Triple<String, Integer, Integer>> tokenize(String content) {
        List<Triple<String, Integer, Integer>> matches = new ArrayList<>();
        if (content == null)
            return matches;

        if (entityPattern == null) {
            initPattern();
        }
        Pattern namePattern = entityPattern;

        //we need a proper sentence splitter, as some of the names can contain period.
        String[] lines = content.split("\\n");
        //don't change the length of the content, so that the offsets are not messed up.
        content = "";
        for (String line : lines) {
            //for very short lines, new line is used as a sentence breaker.
            if (line.length() < 40)
                content += line + "%";
            else
                content += line + " ";
        }

        Span[] sentenceSpans = NLPUtils.tokenizeSentenceAsSpan(content);
        assert sentenceSpans != null;

        for (Span sentenceSpan : sentenceSpans) {
            int sentenceStartOffset = sentenceSpan.getStart();
            String sent = sentenceSpan.getCoveredText(content).toString();
            //TODO: sometimes these long sentences are actually long list of names, which we cannot afford to lose.
            //Is there an easy way to test with the sentence is good or bad quickly and easy way to tokenize it further if it is good?
            //Sometimes there can be junk in the content such as a byte code or a randomly long string of characters,
            //we don't want to process such sentences and feed the many CIC tokens it would generate to the entity recogniser
            if (sent.length() >= 2000)
                continue;

            Matcher m = namePattern.matcher(sent);
            while (m.find()) {
                if (m.groupCount() > 0) {
                    String name = m.group(1);
                    int start = m.start(1) + sentenceStartOffset, end = m.end(1) + sentenceStartOffset;
                    //if the length is less than 3, accept only if it is all capitals.
                    if (name.length() < 3) {
                        String tt = FeatureGeneratorUtil.tokenFeature(name);
                        if (tt.equals("ac")) {
                            //this list contains many single-word bad names like Jan, Feb, Mon, Tue, etc.
                            if (DictUtils.tabooNames.contains(name.toLowerCase())) {
                                continue;
                            }
                            matches.add(new Triple<>(name, start, end));
                        }
                    } else {
                        //further cleaning to remove "'s" pattern
                        //@TODO: Can these "'s" be put to a good use? Right now, we are just tokenizing on them
                        String[] tokens = clean(name);
                        outer:
                        for (String token : tokens) {
                            int s = name.indexOf(token);
                            if (s < 0) {
                                log.error("Did not find " + token + " extracted and cleaned from " + name);
                                continue;
                            }
                            String lc = token.toLowerCase();
                            for (String bs : badSubstrings) {
                                String lbs = bs.toLowerCase();
                                if (lc.equals(lbs) || lc.contains(" " + lbs + " ") || lc.startsWith(lbs + " ") || lc.endsWith(" " + lbs))
                                    continue outer;
                            }
                            //this list contains many single word bad names like Jan, Feb, Mon, Tue, etc.
                            if (DictUtils.tabooNames.contains(token.toLowerCase())) {
                                continue;
                            }
                            String ct = canonicalize(token);
                            matches.add(new Triple<>(ct, start + name.indexOf(token), start + name.indexOf(token) + token.length()));
                        }
                    }
                }
            }
        }

        ///Code added to dump the extracted CIC information in a file..
        //dumpCICInformation(content,matches);
        return matches;
    }

    private void dumpCICInformation(String content, List<Triple<String,Integer,Integer>> ciclist){
        //for the time being assume the filename as "CICInfo.txt"
        //open file in append format (or create if not exists)
        //dump content
        //put separator (###)
        //put each elemnt in the triple in comma separated format
        // when done put separator (---)

        File f = new File("/home/chinmay/CICWords.txt");
        if(!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e) {

            }
        try(FileWriter fw = new FileWriter("/home/chinmay/CICWords.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(content);
            out.println("########################################################");
            //dump cic list in comma separated value.
            ciclist.forEach(triple-> out.println(triple.first+","+triple.second+","+triple.third));
            out.println("--------------------------------------------------------");
        } catch (IOException e) {
           log.warn("Unable to dump the CIC information in CICWords.txt file");
        }


    }

    /**
     * <ul>
     *     <li>cleans more than one extra space in the phrase</li>
     * </ul>
     * */
    public static String canonicalize(String phrase){
        if(phrase.contains("  "))
            phrase = phrase.replaceAll("\\s{2,}"," ");
        return phrase;
    }

    /**
     * Ensures the sanity of the entity chunk, does the following checks:
     * <ul>
     *  <li>tokenizes on multiple stop words or more than one occurrence of {',-,.} (chars that are allowed in an entity word) or quote-s ie. 's, see multipleStopWordPattern</li>
     *  <li>ensures that the token does not end in space or period or hyphen</li>
     *  <li>If the chunk starts the sentence, then removes articles or other common words that start the sentence</li>
     *  <li>Drop the phrase if it is a member of English Dictionary</li>
     *  <li>Tokenize further on tokens that never had the history of being a noun.</li>
     * </ul>
     * TODO: Tokenizing on likely non-noun phrase(the last rule above) is a little aggressive.
     * Shares in Slough -> Shares in
     * @param phrase is the string that is to be cleaned
     * @return the tokenized, cleaned and filtered sub-chunks in the phrase passed.
     * */
    private static String[] clean(String phrase){
        List<String> tokenL = new ArrayList<>();
        Matcher m = multipleStopWordPattern.matcher(phrase);
        int end = 0;
        while(m.find()){
            tokenL.add(phrase.substring(end, m.start()));
            end = m.end();
        }
        if(end!=phrase.length())
            tokenL.add(phrase.substring(end));
        //we have all the split tokens, will have to filter now
        List<String> nts = new ArrayList<>();
        for (String t : tokenL) {
            t = t.replaceAll("^\\W+|\\W+$", "");

            //if the chunk is the first word then, double check the capitalisation
            if (DictUtils.fullDictWords.contains(t.toLowerCase())) {
                continue;
            }
            //remove common start words
            boolean hasCSW = false;
            do {
                String lc = t.toLowerCase();
                for (String cw : commonStartWords)
                    if (lc.startsWith(cw.toLowerCase() + " ")) {
                        t = t.substring(cw.length() + 1);
                        hasCSW = true;
                        break;
                    }
                if (!hasCSW) break;
                hasCSW = false;
            } while (true);

            boolean hasCEW = false;
            do {
                String lc = t.toLowerCase();
                for (String cw : commonEndWords)
                    if (lc.endsWith(" " + cw.toLowerCase())) {
                        t = t.substring(0, t.length() - cw.length() - 1);
                        hasCEW = true;
                        break;
                    }
                if (!hasCEW) break;
                hasCEW = false;
            } while (true);

            String[] words = t.split("\\s+");
            String segment = "";
            int currOff = 0;
            for (String word : words) {
                String temp = t.substring(currOff);
                String pad = t.substring(currOff, currOff + temp.indexOf(word));
                currOff += (pad + word).length();

                String lc = word.toLowerCase();
                if (!stopWords.contains(lc)) {
                    Multimap<String, Pair<String, Integer>> tdict = EnglishDictionary.getTagDictionary();

                    int freq = 0, nounCount = 0;
                    if (tdict != null)
                        for (Pair<String, Integer> p : tdict.get(lc)) {
                            freq += p.getSecond();
                            String tag = p.getFirst();
                            if ("NN".equals(tag) || "NNS".equals(tag) || "NNP".equals(tag) || "NNPS".equals(tag))
                                nounCount += p.getSecond();
                        }

                    //We miss on probable tokens (ADJ) like Iraqi, Turkish because they was mostly JJ.
                    // which I think is OK, else no complaints over CONLL testa
                    //Royal Meteorological Institute is tokenized on the middle word
                    //Also tokenizes on Limited
                    if (tdict != null && (((float) nounCount / freq) < 0.01)) {
                        if (segment.length() > 0)
                            nts.add(segment);
                        segment = "";
                    } else {
                        //don't add padding when the segment is empty
                        if (segment.length() > 0)
                            segment += pad;
                        segment += word;
                    }
                } else {
                    if (segment.length() > 0)
                        segment += pad;
                    segment += word;
                }
            }
            if (segment.length() > 0)
                nts.add(segment);
        }
        //System.out.println(phrase+" -> "+nts.stream().reduce("",String::concat));
        return nts.toArray(new String[0]);
    }
}
