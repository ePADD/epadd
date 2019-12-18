package edu.stanford.muse.ie.variants;

import com.google.common.collect.*;
import edu.stanford.muse.util.DictUtils;
import edu.stanford.muse.util.Util;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/** Util program to generate tokens that are candidates to be variants of each other from Wikipedia surface text.
 * prints token pairs that occur at least THRESHOLD times.
 * Needs stop words file and SurfaceForms_LRD-WAT.nofilter.tsv in ~/data and writes out to variants.txt in the same dir */
class GenerateVariants {
    private static final Logger log =  LogManager.getLogger(GenerateVariants.class);
    private static   String BASE_DIR = System.getProperty("user.home") + File.separator + "data";
    private static   int THRESHOLD = 10; // min. #times a token pair should appear in order to be output
    private static   String TSV_FILE = "SurfaceForms_LRD-WAT.nofilter.tsv.gz", STOP_WORDS_FILE = "stop.words.full";
    private static   int WORD_FREQ_SUPPORT = 8;
    private static  Set<String> stopWords;
    private static Map<String, Integer> wordToFreq = new LinkedHashMap<>();

    static class Variant implements Comparable<Variant>, Serializable {
        String main, alias;
        float score1, score2;
        int freq1, freq2;
        String type1, type2;

        public int hashCode() { return main.hashCode() ^ alias.hashCode(); }
        public boolean equals(Object o) {
            Variant other = (Variant) o;
            return (other.main.equals(this.main) && other.alias.equals(this.alias));
        }

        public int compareTo(Variant other) {
            float thisScore = score1 * score2;
            float otherScore = other.score1 * other.score2;
            if (thisScore == otherScore)
                return ((this.main + "-" + this.alias).compareTo(other.main + "-" + other.alias));
            else
                return (thisScore > otherScore) ? -1 : 1;
        }

        public String toString() {
           return (score1 * score2) + " " + main + " = " + alias + " [" + type1 + " = " + type2 + "] " + " freq1: " + freq1 + " freq2: " + freq2;
        }
    }

    static {
        try {
            InputStream is = new FileInputStream(BASE_DIR + File.separator + STOP_WORDS_FILE);
            stopWords = DictUtils.readStreamAndInternStrings(new InputStreamReader(is, StandardCharsets.UTF_8));
            is.close();
        } catch (IOException ioe) {
            Util.print_exception("Error reading stop words file", ioe, log);
        }
    }

    // remove unnecessary punctuation
    private static String withoutPunctuation(String s) {
        // replace Cancer_(constellation) with just Cancer by stripping the _(...type...) part of the Wikipedia title, if its present;
        s = s.replaceAll("_\\(.*\\)", "");

        // replace leading and trailing brackets, underscores, commas (e.g. "stanford," parsed from "stanford, california") with a blank
        return s.replaceAll("\\(.*\\)", "").replaceAll("_", " ").replaceAll("\\(", " ").replaceAll("\\)", " ").replaceAll(",", " ").replaceAll("\\.", "");
    }

    /**
     * returns true if s's chars are not all alpha num, period, dash or comma
     */
    private static boolean hasFunnyChars(String s) {
        for (char ch : s.toCharArray()) {
            if (!Character.isAlphabetic(ch) && !Character.isDigit(ch) && !Character.isSpaceChar(ch) && ch != '.' && ch != '-' && ch != ',')
                return true;
        }
        return false;
    }

    private static Map<String, String> readDbpedia(String file) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(file), true), StandardCharsets.UTF_8));
        System.out.println ("Reading Dbpedia");

        int count = 0;
        while (true) {
            if (++count % 100000 == 0)
                System.out.println(count + " lines");

            String line = lnr.readLine();
            // line: Cancer  Malignancy      1.5029          1       8082.0  44.0
            if (line == null)
                break;

            List<String> tokens = Util.tokenize(line);
            result.put(tokens.get(0).toLowerCase(), tokens.get(1).intern()); // intern the types cos there are only a few of them
        }
        return result;
    }

    private static Map<String, Integer> computeWordToFreq() throws IOException {

        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream((new FileInputStream(BASE_DIR + File.separator + TSV_FILE)))));
        int count = 0;
        Set<String> allTokens = new LinkedHashSet<>();
        int maxTitleTokens = 0, maxVariantTokens = 0;
        Multiset<String> set = LinkedHashMultiset.create();

        while (true) {
            if (++count % 100000 == 0)
                System.out.println(count + " lines");

            String line = lnr.readLine();
            // line: Cancer  Malignancy      1.5029          1       8082.0  44.0
            if (line == null)
                break;

            List<String> vals = Util.tokenize(line, "\t");
            if (vals.size() < 2) // sanity check
                continue;

            String title = vals.get(0), variant = vals.get(1);
            // title: Cancer, variant: Malignancy

            title = withoutPunctuation(title);
            variant = withoutPunctuation(variant);

            // allow only some chars, skip lines with funny chars like:
            // !Action_Pact!   !Action Pact!   5.2295  L0      7       11.0    7.0
            // %22A%22_Device  A Device        5.4704  L1      8       12.0    8.0
            if (hasFunnyChars(title) || hasFunnyChars(variant))
                continue;

            // tokenize and remove stop tokens because they are low-signal
            List<String> titleTokens = Util.tokenize(title.toLowerCase());
            List<String> variantTokens = Util.tokenize(variant.toLowerCase());

            titleTokens = removeNeedlessWords(titleTokens);
            variantTokens = removeNeedlessWords(variantTokens);

            allTokens.addAll(titleTokens);
            allTokens.addAll(variantTokens);

            maxTitleTokens = Math.max(maxTitleTokens, titleTokens.size());
            maxVariantTokens = Math.max(maxVariantTokens, titleTokens.size());
            set.addAll(titleTokens);
            set.addAll(variantTokens);
        }

        System.out.println("total # tokens = " + allTokens.size());
        System.out.println("max title = " + maxTitleTokens + " max variant = " + maxVariantTokens);

        wordToFreq = new LinkedHashMap<>();
        for (String s : set.elementSet()) {
            if (s == null)
                continue;

            int bucket = set.count(s);
            if (bucket >= WORD_FREQ_SUPPORT) {
                wordToFreq.put(s, bucket);
            }
        }

        Util.writeObjectToFile("wordToFreq.ser", (Serializable) wordToFreq);
        PrintWriter pw = new PrintWriter(new File("wordToFreq.txt"));
        for (String s: wordToFreq.keySet())
            pw.println (s + " " + wordToFreq.get(s));
        pw.close();

        return wordToFreq;
    }

    private static void generateVariantWeights() throws IOException, ClassNotFoundException {

        if (!new File("wordToFreq.ser").exists())
            computeWordToFreq();
        wordToFreq = (Map) Util.readObjectFromFile("wordToFreq.ser");

        Map<String, Map<String, Float>> variantToTitleToWeight = new HashMap<>();

        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream((new FileInputStream(BASE_DIR + File.separator + TSV_FILE)))));
        int count = 0;
        Set<String> wordsOfInterest = wordToFreq.keySet();

        while (true) {
            if (++count % 100000 == 0)
                System.out.println(count + " lines");

            String line = lnr.readLine();
            // line: Cancer  Malignancy      1.5029          1       8082.0  44.0
            if (line == null)
                break;

            List<String> vals = Util.tokenize(line, "\t");
            if (vals.size() < 2) // sanity check
                continue;

            String title = vals.get(0), variant = vals.get(1);
            // title: Cancer, variant: Malignancy

            title = withoutPunctuation(title);
            variant = withoutPunctuation(variant);

            // tokenize and remove stop tokens because they are low-signal
            Set<String> titleTokens = new LinkedHashSet<>(removeNeedlessWords(Util.tokenize(title.toLowerCase())));
            Set<String> variantTokens = new LinkedHashSet<>(removeNeedlessWords(Util.tokenize(variant.toLowerCase())));

            titleTokens.retainAll(wordsOfInterest);
            variantTokens.retainAll(wordsOfInterest);

            // now remove common words between variant and title
            Set<String> v0TokensCopy = new HashSet<>(titleTokens);
            titleTokens.removeAll(variantTokens);
            variantTokens.removeAll(v0TokensCopy);

            // filter out tokens of 1 letter or 1 letter + "." (we see a lot of tokens like "v.")
            titleTokens = titleTokens.stream().filter(t -> t.length() > 2 || (t.length() == 2 && !t.endsWith("."))).collect(Collectors.toSet());
            variantTokens = variantTokens.stream().filter(t -> t.length() > 2 || (t.length() == 2 && !t.endsWith("."))).collect(Collectors.toSet());

            outer:
            while (true) {
                for (Iterator<String> itTitle = titleTokens.iterator(); itTitle.hasNext(); ) {
                    String titleToken = itTitle.next();
                    for (Iterator<String> itVariant = variantTokens.iterator(); itVariant.hasNext(); ) {
                        String variantToken = itVariant.next();
                        if (((titleToken.startsWith(variantToken) || titleToken.endsWith(variantToken)) && titleToken.length() < variantToken.length() + 3) ||
                                ((variantToken.startsWith(titleToken) || variantToken.endsWith(titleToken)) && variantToken.length() < titleToken.length() + 3)) {
                            itTitle.remove();
                            itVariant.remove();
                            continue outer;
                        }
                    }
                }
                break;
            }

            // now map every token in title to every token in variant
            float weight = 1.0f/(1+titleTokens.size()); // prob. that any given variant maps to any given token (simplistic)

            for (String titleToken : titleTokens)
                for (String variantToken : variantTokens) {
                    Map<String, Float> titleToWeight = variantToTitleToWeight.computeIfAbsent(variantToken, k -> new HashMap<>());

                    titleToWeight.merge(titleToken, weight, (a, b) -> a + b);
                }
        }


        // eliminate all pairs with weight < 1.0f
        {
            Collection<String> variants = new ArrayList<>(variantToTitleToWeight.keySet()); // create a copy of keys because we may delete this key inside the loop
            for (String variant : variants) {
                Map<String, Float> titleToWeight = variantToTitleToWeight.get(variant);

                // create a copy of title keys because we may delete this key inside the loop
                Collection<String> titles = new ArrayList<>(titleToWeight.keySet());
                for (String title : titles)
                    if (titleToWeight.get(title) < 1.0f)
                        titleToWeight.remove(title);

                // if this variant now has no titles, remove it from the map entirely
                if (titleToWeight.size() == 0)
                    variantToTitleToWeight.remove(variant);
            }
        }

        Util.writeObjectToFile("variantToTitleToWeight.ser", (Serializable) variantToTitleToWeight);
        PrintWriter pw = new PrintWriter(new File("variantToTitleToWeight.txt"));
        for (String variant: variantToTitleToWeight.keySet())
            for (String title: variantToTitleToWeight.get(variant).keySet())
                pw.println ( variantToTitleToWeight.get(variant).get(title) + " " + variant + " " + title);
        pw.close();
    }

    private static void computeVariants() throws IOException, ClassNotFoundException {
        if (!new File("variantToTitleToWeight.ser").exists())
            generateVariantWeights();

        Map<String, Map<String, Float>> variantToTitleToWeight = (Map<String, Map<String, Float>>) Util.readObjectFromFile("variantToTitleToWeight.ser");

        Map<String, String> entityToType = readDbpedia(System.getProperty("user.home") + File.separator + "epadd-settings" + File.separator + "instance_types_2015-04.en.txt.bz2");

        if (wordToFreq == null || wordToFreq.size() == 0) {
            if (!new File("wordToFreq.ser").exists())
                computeWordToFreq();
            wordToFreq = (Map) Util.readObjectFromFile("wordToFreq.ser");
        }

        Collection<Variant> symmetricVariants = new HashSet<>(), unsymmetricVariants = new HashSet<>();

        for (String v: variantToTitleToWeight.keySet()) {
            Map<String, Float> map = variantToTitleToWeight.get(v);

            for (String t: map.keySet()) {
                Util.ASSERT(!t.equals(v));

                /*
                if (t.startsWith(v) || v.startsWith(t) || t.endsWith(v) || v.endsWith(t))
                    continue;
                */
                Float score1 = map.get(t);

                Map<String, Float> map2 = variantToTitleToWeight.get(t);
                Float score2 = 0f;
                if (map2 != null) {
                    if (map2.get(v) != null)
                        score2 = map2.get(v);
                }

                String main = t, alias = v;
                float score = score1 * score2;
                Integer It = wordToFreq.get(main);
                Integer Iv = wordToFreq.get(alias);
                {
                    if (It == null) { System.out.println ("null for " + main); }
                    if (Iv == null) { System.out.println ("null for " + alias); }
                    if (It != null && Iv != null && It < Iv ) {
                        continue;
//                        String tmp = main; main = alias; alias = tmp;
 //                       float f = score1; score1 = score2; score2 = f;
                    } // first one should have more weight
                }

                Variant V = new Variant();
                V.main = main; V.alias = alias;
                V.score1 = score1; V.score2 = score2;
                V.freq1 = wordToFreq.get(main); V.freq2 = wordToFreq.get(alias);
                V.type1 = entityToType.get(main); V.type2 = entityToType.get(alias);

                if (score > 300)
                    symmetricVariants.add (V);
                else if (score1 > 10 || score2 > 10)
                    unsymmetricVariants.add (V);

            }
        }
        symmetricVariants = new ArrayList<>(symmetricVariants);
        Collections.sort((List) symmetricVariants);
        unsymmetricVariants = new ArrayList<>(unsymmetricVariants);
        Collections.sort((List) unsymmetricVariants);

        Util.writeObjectToFile("symmetric-variants.ser", (Serializable) symmetricVariants);
        Util.writeObjectToFile("unsymmetric-variants.ser", (Serializable) unsymmetricVariants);
    }

    private static void printVariants() throws IOException, ClassNotFoundException {
        if (!new File("symmetric-variants.ser").exists() || !new File("unsymmetric-variants.ser").exists()) {
            computeVariants();
        }
        Collection<Variant> symmetricVariants = (Collection) Util.readObjectFromFile("symmetric-variants.ser");
        Collection<Variant> unsymmetricVariants = (Collection) Util.readObjectFromFile("unsymmetric-variants.ser");

        System.out.println ("Writing symmetric and unsymmetric variants");
        PrintWriter pw = new PrintWriter(new File("symmetric-variants.txt"));
        for (Variant v: symmetricVariants)
            pw.println (v);
        pw.close();

        pw = new PrintWriter(new File("unsymmetric-variants.txt"));
        for (Variant v: unsymmetricVariants)
            pw.println (v);
        pw.close();
    }

    /** nulls out stop words, and words of length < 1 char */
    private static List<String> nullOutNeedlessWords (List<String> tokens) {
        for (int i = 0; i < tokens.size(); i++)
        {
            String token = tokens.get(i);
            if (stopWords.contains(token) || token.length() < 2)
                tokens.set (i, null);
        }
        return tokens; // return input parameter, just to allow chaining
    }

    /** nulls out stop words, and words of length < 1 char */
    private static List<String> removeNeedlessWords (List<String> tokens) {
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (stopWords.contains(token) || token.length() < 2)
                continue;
            result.add(token);
        }
        return result; // return input parameter, just to allow chaining
    }


    public static void main(String args[]) throws IOException, ClassNotFoundException {
      //  generateVariantWeights();

        printVariants();
    }
}