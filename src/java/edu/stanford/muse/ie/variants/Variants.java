package edu.stanford.muse.ie.variants;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.Config;
import edu.stanford.muse.util.DictUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/* class meant to handle variants. note this maps each word to one or more other words.
e.g. sandy could map to sandra or alexander
  * sandy -> sandra, or alexander
  * kate -> kathleen or katherine
  */
public class Variants {

    private final Multimap<String, String> map = HashMultimap.create();
    private static final Log log = LogFactory.getLog(Variants.class);

    public static Variants nameVariants;
    private static Set<String> stopWords;

    static {
        try {
            InputStream is = Config.getResourceAsStream("stop.words"); // can change this to stop.words.new or stop.words.full if we want to
            stopWords = DictUtils.readStreamAndInternStrings(new InputStreamReader(is, "UTF-8"));
            is.close();
        } catch (Exception ioe) {
            Util.print_exception("Error reading stop words file", ioe, log);
        }
    }

    static {
        try {
            nameVariants = new Variants("name-variants.txt");
        } catch (IOException ioe) {
            Util.print_exception("Error reading name variants", ioe, log);
        }
    }

    private Variants() throws IOException {
        this(Config.getResourceAsStream("name-variants.txt"));
    }

    private Variants(String filename) throws IOException {
        this(Config.getResourceAsStream(filename));
    }

    private Variants(InputStream is) throws IOException {
        List<String> lines = Util.getLinesFromInputStream(is, true);
        // each line is of the form: robert = bob = bobby (robert is canonical)
        for (String line : lines) {
            StringTokenizer st = new StringTokenizer(line, ", ="); //allow comma, equals, space and tabs
            if (!st.hasMoreTokens())
                continue;
            String canonical = st.nextToken().toLowerCase();
            while (st.hasMoreTokens()) {
                String s = st.nextToken().toLowerCase();
                Collection<String> v = map.get(s);
                if (!Util.nullOrEmpty(v))
                    log.warn("variant " + s + " mapping to " + canonical + ", when it is already mapped to " + v);
                map.put(s, canonical);
            }
        }

        int max_count = map.size();
        for (String start : map.keySet()) {
            String s = start;
            // better not have loops here!
            int count = 0;
            while (true) {
                Collection<String> mapped = map.get(s);
                if (Util.nullOrEmpty(mapped))
                    break;
                s = mapped.iterator().next();
                if (++count > max_count) {
                    log.warn("Variants loop detected starting from: " + start);
                    break;
                }
            }
        }
    }

    /**
     * get a single, canonical variant
     */
    public String getCanonicalVariant(String s) {
        String result = null;

        // only get the first string that s maps to
        while (s != null) {
            result = s;
            Collection<String> coll = map.get(s);
            if (Util.nullOrEmpty(coll))
                break;
            s = coll.iterator().next();
        }
        return result;
    }

    private Set<String> getVariants(String s) {
        Set<String> result = new LinkedHashSet<>();
        Set<String> seen = new LinkedHashSet<>(); // will track all strings we have seen

        Set<String> nextResult = new LinkedHashSet<>();
        nextResult.add(s);

        seen.addAll(nextResult);

        while (!Util.nullOrEmpty(nextResult)) {
            result = nextResult;
            seen.addAll(result);
            nextResult = new LinkedHashSet<>();
            for (String s1 : result) {
                Collection<String> coll = map.get(s1);
                if (!Util.nullOrEmpty(coll))
                    nextResult.addAll(coll);
            }
            nextResult.removeAll(seen); // remove the ones we have already seen, to avoid cycles
        }
        return result;
    }

    public boolean effectivelyEqual(String a, String b) {
        String ac = getCanonicalVariant(a.toLowerCase());
        String bc = getCanonicalVariant(a.toLowerCase());
        return ac.equals(bc);
    }

    public static void main1(String[] args) throws IOException {
        String sb = "Robert = Bob = bobby\n" + "bob = rob\n";
        Variants v = new Variants();
        Util.ASSERT(v.getCanonicalVariant("rob").equals("robert"));
    }

    private static Map<String, String> readDbpedia(String file) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(file), true), "UTF-8"));
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
            String entity = withoutPunctuation(tokens.get(0));
            result.put(entity, tokens.get(1).intern()); // intern the types cos there are only a few of them
        }
        return result;
    }

    private static Pattern p1 = Pattern.compile ("_\\(.*\\)"), p2 = Pattern.compile ("\\(.*\\)"), p3 = Pattern.compile ("[_,\\.\\(\\)]");

    // remove unnecessary punctuation
    private static String withoutPunctuation(String s) {
        // replace Cancer_(constellation) with just Cancer by stripping the _(...type...) part of the Wikipedia title, if its present;
        s = p1.matcher(s).replaceAll("");
        s = p2.matcher(s).replaceAll("");
        s = p3.matcher(s).replaceAll(" ");
        return s;
        // replace leading and trailing brackets, underscores, commas (e.g. "stanford," parsed from "stanford, california") with a blank
    }

    public static void main (String args[]) throws IOException {
        Map<String, String> entityToType = readDbpedia(System.getProperty("user.home") + File.separator + "epadd-settings" + File.separator + "instance_types_2015-04.en.txt.bz2");
        EntityMap em = new EntityMap();

        Pattern p = Pattern.compile("_");
        Set<String> entities = entityToType.keySet().stream().map(s -> p.matcher(s).replaceAll(" ")).collect(Collectors.toSet());
        em.setupEntityMapping(entities);

        PrintStream out = System.out;

        String[] tests = new String[]{"gavaskar", "sunil gavaskar", "tendulkar", "creeley"};
        for (String s: tests) {
            int i = 0;
            Map<String, Integer> map = em.lookupAsWeightedMap(s);
            out.println ("Results for " + s);
            for (String k: map.keySet())
                out.println (++i + ". " + k + " " + map.get(k));
        }
    }

    public static class EntityMap {
        Multimap<String, String> cEntityToDEntity = HashMultimap.create();
        Multimap<String, String> tokenToCEntity = HashMultimap.create();
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
            tokens.removeAll(stopWords);
            tokens = tokens.stream().map(Variants.nameVariants::getCanonicalVariant).collect(Collectors.toList());

            if (Util.nullOrEmpty(tokens))
                return "";

            Collections.sort(tokens);
            return Util.join(tokens, " ");
        }

        public static String abbreviate(String s) {
            List<String> tokens = Util.tokenize(s, DELIMS);
            StringBuilder result = new StringBuilder();
            for (String token : tokens)
                result.append(token.charAt(0));  // may not work for indic scripts, etc.
            return result.toString();
        }

        public void setupEntityMapping(Set<String> allEntities) {
            int count = 0;
            for (String dEntity : allEntities) {
                dEntity = dEntity.trim();
                if (++count % 100000 == 0) {
                    log.info ("Setting up entity map: Processed " + count + " of " + allEntities.size());
                    log.info (Util.getMemoryStats());
                }
                String cEntity = canonicalize(dEntity);
                if (Util.nullOrEmpty(cEntity))
                    continue;

                Collection<String> existingDentities = cEntityToDEntity.get(cEntity);
                if (!Util.nullOrEmpty(existingDentities)) {
                    // only print if case invariant difference between existing dentities and the new dentity
                    Set<String> set = existingDentities.stream().map(String::toLowerCase).collect(Collectors.toSet());
                    set.remove(dEntity.toLowerCase());
                    if (!Util.nullOrEmpty(set))
                        log.warn ("Centity = " + cEntity + " dEntity = " + dEntity + " existingDentities = " + Util.join (set, ", "));
                }

                cEntityToDEntity.put(cEntity, dEntity);

                for (String token : Util.tokenize(cEntity)) {
                    tokenToCEntity.put(token, cEntity);
                }
                tokenToCEntity.put(abbreviate(cEntity), cEntity);
            }
        }

        public Set<Set<String>> getClusters() {
            Set<Set<String>> clusters = new LinkedHashSet<>();
            for (String cEntity: cEntityToDEntity.keys()) {
                Set<String> cluster = new LinkedHashSet<>(cEntityToDEntity.get(cEntity));
                clusters.add (cluster);
            }
            return clusters;
        }

        /* returns map of centities -> weight that the string s maps to */
        public Map<String, Integer> lookupAsWeightedMap(String s) {
            Map<String, Integer> result = new LinkedHashMap<>(); // cEntityToScore
            s = canonicalize(s);
            if (Util.nullOrEmpty(s))
                return result;

            List<String> tokens = Util.tokenize(s);
            Set<String> variantTokens = new LinkedHashSet<>(tokens);
            for (String t : tokens) {
                Collection<String> tokenVariants = nameVariants.getVariants(t);
                variantTokens.addAll(tokenVariants);
            }

            variantTokens.removeAll(tokens);
            Set<String> lookupTokens = new LinkedHashSet<>(tokens);
            lookupTokens.addAll(variantTokens);

            for (String lt : lookupTokens) {
                Collection<String> centities = tokenToCEntity.get(lt);
                if (centities != null) {
                    int increment = (tokens.contains(lt)) ? 2 : 1; // more weight if its an original token, instead of via a variant

                    for (String centity : centities) {
                        result.merge(centity, increment, (a, b) -> a + b);
                    }
                }
            }
            return result;
        }

        public Set<String> lookup(String s) {
            Map<String, Integer> centityToScore = lookupAsWeightedMap(s);
            List<Pair<String, Integer>> pairs = Util.sortMapByValue(centityToScore);

            Set<String> result = new LinkedHashSet<>(), resultLowerCase = new LinkedHashSet<>(); // maintain this because we don't want to ever return 2 entries that are different only in case

            for (Pair<String, Integer> p : pairs) {
                String cEntity = p.getFirst();
                Collection<String> dEntitiesForCEntity = cEntityToDEntity.get(cEntity);
                for (String dEntity : dEntitiesForCEntity) {

                    if (resultLowerCase.contains(dEntity.toLowerCase()))
                        continue;

                    result.add(dEntity);
                    resultLowerCase.add(dEntity.toLowerCase());
                }
            }
            return result;
        }
    }
}
