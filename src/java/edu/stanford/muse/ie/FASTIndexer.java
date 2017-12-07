package edu.stanford.muse.ie;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class takes a .fast nt file and converts it to a lucene index that can be searched for.
 * It's a standalone program, i.e. doesn't need to be included in epadd.war
 */
public class FASTIndexer {
    private static Log log = LogFactory.getLog(FASTIndexer.class);
    public static final String FIELD_NAME_LABELS = "labels";
    public static final String FIELD_NAME_FAST_ID = "fastId";
    public static final String FIELD_NAME_WIKIPEDIA_ID = "wikipediaId";
    public static final String FIELD_NAME_VIAF_ID = "viafId";
    public static final String FIELD_NAME_LCNAF_ID = "lcnafId";
    public static final String FIELD_NAME_LCSH_ID = "lcshId";
    public static final String FIELD_NAME_EXTENT = "extent";
    private static final String LABEL_SEPARATOR = " ; "; // the labels field will have primary name, followed by alt names, all separated with a ";"
    private static PrintStream out = System.out, err = System.err;

    public static void main (String args[]) throws IOException, ParseException {
        if (args.length != 2) {
            out.println("usage java FASTIndexer <.nt file> <output directory");
            return;
        }

       // index (args[0], args[1]);
        test (args[1]);
    }

    // example line:
    // <http://id.worldcat.org/fast/348231> <http://www.w3.org/2004/02/skos/core#prefLabel> "Obama, Barack" .
    // Note: it ends with space and period.
    private static Pattern triplePattern = Pattern.compile("([^\\s]*)\\s+([^\\s]*)\\s+(.*) \\.");
    private static IndexWriter indexWriter;

    public static void index(String fastNTFile, String outputDir) throws IOException {

        File outputFile = new File (outputDir);
        if (!outputFile.exists() && !outputFile.isFile()) {
            outputFile.mkdirs();
        }

        StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_47, new CharArraySet(Version.LUCENE_47, new ArrayList<String>(), true /* ignore case */)); // empty chararrayset, so effectively no stop words
        Directory index = FSDirectory.open(outputFile);
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47, analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        indexWriter = new IndexWriter(index, iwc);

        try {
            LineNumberReader lnr;
            lnr = new LineNumberReader(new BufferedReader(new InputStreamReader(new FileInputStream(fastNTFile))));
            // sometimes lines in loc subject files starts like:
            int lineNum = 0;
            Multimap<String, String> predicateToObject = LinkedHashMultimap.create();

            long currentFastId = -1L; // invalid fast id

            // important assumption we are making about parsing the lines in this file:
            // all the entries with the same fastid as the predicate are next to each other
            // subjects without a "http://id.worldcat.org/fast/" start are ignored
            while (true) {
                String line = lnr.readLine();
                if (line == null)
                    break;

                lineNum++;
                if (lineNum % 100000 == 0)
                    System.out.println("Line number: " + lineNum);

                // sample line: <http://id.worldcat.org/fast/15615> <http://schema.org/name> "Boss, David" .

                Matcher m = triplePattern.matcher(line);

                if (!m.find() || m.groupCount() != 3) {
                    err.println("WARNING: This is not an nt file! line#" + lineNum + ": " + line);
                    continue;
                }

                String subject = m.group(1), predicate = m.group(2), object = m.group(3);
                subject = Util.convertSlashUToUnicode (subject);
                if (!subject.startsWith ("<http://id.worldcat.org/fast/"))
                    continue;

                // sometimes we see lines like this, skip them:
                //  <http://id.worldcat.org/fast/void/0.1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdfs.org/ns/void#Dataset> .
                if (subject.startsWith ("<http://id.worldcat.org/fast/void"))
                    continue;

                String fastIdStr = Util.baseName (subject); // "15615>"
                fastIdStr = fastIdStr.substring(0, fastIdStr.length()-1); // "15615" (strip the trailing > char)

                long fastId;
                try { fastId = Long.parseLong (fastIdStr); }
                catch (Exception e) { err.println ("WARNING: Unable to parse fast id on line " + line); continue; }

                predicate = Util.convertSlashUToUnicode (predicate);
                object = Util.convertSlashUToUnicode (object);

                if (currentFastId != fastId) {
                    // done with all the lines for one fast id, process it now
                    processFastEntity (currentFastId, predicateToObject);

                    // start a new fast id
                    currentFastId = fastId;
                    predicateToObject.clear();
                }
                predicateToObject.put (predicate, object);
            }
            lnr.close();

            // just cleanup at the last line
            if (predicateToObject.size() > 0)
                processFastEntity (currentFastId, predicateToObject);

            indexWriter.close();
        } catch (Exception e){
            Util.print_exception("Error parsing FAST file", e, log);
        }
    }

    private static final Pattern prefNameAndExtentMatcher = Pattern.compile ("(.*), *([0-9\\-]*)"); // to match pref names like: Cooper, Dr. (Thomas), 1759-1839
    private static final Pattern prefNameAndExtentMatcher2 = Pattern.compile ("(.*), (active.*)"); // to match Theoktistos, the Stoudite, active 14th century

    private static Pair<String, String> breakIntoNameAndExtent(String nameLabel) {

        String name = nameLabel, extent = null; // by default assume no extent

        Matcher m = prefNameAndExtentMatcher.matcher (nameLabel);
        if (m.matches ()) {
            name = m.group (1);
            extent = m.group (2);
        } else {
            Matcher m2 = prefNameAndExtentMatcher2.matcher (nameLabel);
            if (m2.matches ()) {
                // to handle prefLabel = Theoktistos, the Stoudite, active 14th century
                name = m2.group (1);
                extent = m2.group (2);
            }
        }

        return new Pair<>(name, extent);
    }

    /** assembles a fast entity, given all the pred->objs for subject with the given fastid */
    private static void processFastEntity(long fastId, Multimap<String, String> predToObject) throws IOException {

        String wikipediaId = "?", viafId = "?", lcshId = "?", lcnafId = "?";
        String type = "?";
        String prefLabel = "?";
        List<String> altLabels = new ArrayList<>();
        String extent = null;

        for (String pred: predToObject.keySet()) {
            Collection<String> objs = predToObject.get (pred);

            {
                // fast lines connecting to wikipedia look like this: <http://id.worldcat.org/fast/348231> <http://xmlns.com/foaf/0.1/focus> <http://en.wikipedia.org/wiki/Barack_Obama> .
                if (pred.startsWith ("<http://xmlns.com/foaf") && pred.endsWith ("focus>")) {
                    if (objs.size() > 1) {
                        // Multiple focus predicates can happen occasionally, e.g.
                        // WARNING: Multiple focus predicates for the same fast id: 52519
                        // <http://id.worldcat.org/fast/52519> <http://xmlns.com/foaf/0.1/focus> <http://en.wikipedia.org/wiki/Erich_Raeder_pre_Grand_Admiral> .
                        // <http://id.worldcat.org/fast/52519> <http://xmlns.com/foaf/0.1/focus> <http://en.wikipedia.org/wiki/Erich_Raeder> .
                        // seems to happen for a small # of id's (~50), so ignoring

                        err.println("WARNING: Multiple focus predicates for the same fast id: " + fastId);
                        continue;
                    }
                    String wikipediaObj = objs.iterator().next();
                    if (!wikipediaObj.startsWith ("<http://en.wikipedia.org/wiki/")) {
                        err.println ("WARNING: wikipediaObj is unexpected: " + wikipediaObj);
                        continue;
                    }

                    wikipediaId = Util.baseName(wikipediaObj);
                    wikipediaId = wikipediaId.substring (0, wikipediaId.length()-1);
                }
            }

            {
                // lines connecting to viaf and lcnaf and lcsh look like this:
                // <http://id.worldcat.org/fast/348231> <http://schema.org/sameAs> <https://viaf.org/viaf/52010985> .
                // <http://id.worldcat.org/fast/348231> <http://schema.org/sameAs> <http://id.loc.gov/authorities/names/n94112934> .
                // <http://id.worldcat.org/fast/369807> <http://schema.org/sameAs> <http://id.loc.gov/authorities/subjects/sh96000006> .
                if (fastId == 348231) {
                    out.println ("found obama");
                }
                if (pred.equals("<http://schema.org/sameAs>")) {
                    for (String obj : objs) {
                        if (obj.startsWith("<http://id.loc.gov/authorities/names/")) {
                            lcnafId = Util.baseName(obj);
                            lcnafId = lcnafId.substring(0, lcnafId.length() - 1);
                        } else if (obj.contains("//viaf.org/viaf/")) {
                            viafId = Util.baseName(obj);
                            viafId = viafId.substring(0, viafId.length() - 1);
                        } else if (obj.startsWith ("<http://id.loc.gov/authorities/subjects/")) {
                            lcshId = Util.baseName(obj);
                            lcshId = lcshId.substring(0, lcshId.length() - 1);
                        } else {
                           err.println("WARNING: unknown sameAs directive: " + obj + " fastId = " + fastId);
                        }
                    }
                }
            }

            {
                // lines specifying the type of this fast id look like this:
                // <http://id.worldcat.org/fast/348231> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://schema.org/Person> .
                if (pred.equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
                    if (objs.size() > 1) {
                        err.println ("WARNING: multiple types for fast id " + fastId + ": " + Util.join (objs, ";"));
                        continue;
                    }

                    type = objs.iterator().next();
                    type = Util.baseName(type);
                    type = type.substring (0, type.length()-1);
                }
            }

            {
                //  lines specifying the preferred label of this fast id look like this:
                // <http://id.worldcat.org/fast/348231> <http://www.w3.org/2004/02/skos/core#prefLabel> "Obama, Barack" .

                if (pred.equals("<http://www.w3.org/2004/02/skos/core#prefLabel>")) {
                    if (objs.size() > 1) {
                        err.println("WARNING: multiple preferred labels for fast id " + fastId + ": " + Util.join(objs, ";"));
                        continue;
                    }

                    prefLabel = Util.stripDoubleQuotes(objs.iterator().next());
                    Pair<String, String> p = breakIntoNameAndExtent(prefLabel);
                    prefLabel = p.getFirst();
                    if (!Util.nullOrEmpty(p.getSecond()))
                        extent = p.getSecond();

                    // more patterns needed for pref names we're not able to parse correctly:
                    // Hasselborn, Martha, 1600 or 1601-1696
                    // David, Ernest, b. 1838
                }
            }

            {
                //  lines specifying the alt labels of this fast id look like this:
                // <http://id.worldcat.org/fast/348231> <http://www.w3.org/2004/02/skos/core#altLabel> "Obama, Barack Hussein, II" .
                if (pred.equals("<http://www.w3.org/2004/02/skos/core#altLabel>")) {

                    for (String obj: objs) {
                        String altLabel = Util.stripDoubleQuotes(obj);
                        Pair<String, String> p = breakIntoNameAndExtent(altLabel);
                        altLabel = p.getFirst();
                        if (extent == null && !Util.nullOrEmpty(p.getSecond()))
                            extent = p.getSecond(); // if already set (by a prefLabel), ignore it. a dependency here on seeing the prefLabel line before the altLabel lines
                        altLabels.add(altLabel);
                    }
                }
            }
        }

        // we'll ignore anything non-Person
        if (!"Person".equals (type))
            return;

        String alt = (altLabels.size() == 0) ? "" : ((altLabels.size() == 1) ? "alt: " + altLabels.get(0) : altLabels.size() + "alt: " + Util.join (altLabels, ";"));
        out.println ("fast id: " + fastId + " pref name " + prefLabel + " "
                + (extent != null ? "" : "Extent: " + extent)
                + " " + alt + " viaf: " + viafId + " lcsh id " + lcshId + " lcnaf id " + lcnafId + " wiki " + wikipediaId);


        if (Util.nullOrEmpty(prefLabel)) {
            err.println ("WARNING: prefLabel = null or empty for fast id " + fastId);
            return;
        }
        if (fastId < 0) {
            err.println("WARNING: fast Id is not valid: " + fastId);
            return;
        }

        String labels = prefLabel;
        if (!Util.nullOrEmpty(altLabels)) {
            String altLabelString = Util.join (altLabels, LABEL_SEPARATOR);
            labels += LABEL_SEPARATOR + altLabelString;
        }

        // Put these entries into Lucene.
        // Important: the names are textfields, while the ids are stringfields (since they are matched exactly, so no tokenization)
        {
            Document luceneDoc = new Document();
            luceneDoc.add(new TextField(FIELD_NAME_LABELS, labels, Field.Store.YES));

            if (fastId >= 0)
                luceneDoc.add(new LongField(FIELD_NAME_FAST_ID, fastId, Field.Store.YES));

            if (!Util.nullOrEmpty(wikipediaId))
                luceneDoc.add(new StringField(FIELD_NAME_WIKIPEDIA_ID, wikipediaId, Field.Store.YES));

            if (!Util.nullOrEmpty(viafId))
                luceneDoc.add(new StringField(FIELD_NAME_VIAF_ID, viafId, Field.Store.YES));

            if (!Util.nullOrEmpty(lcnafId))
                luceneDoc.add(new StringField(FIELD_NAME_LCNAF_ID, lcnafId, Field.Store.YES));

            if (!Util.nullOrEmpty(lcshId))
                luceneDoc.add(new StringField(FIELD_NAME_LCSH_ID, lcshId, Field.Store.YES));

            if (!Util.nullOrEmpty(extent))
                luceneDoc.add(new StringField(FIELD_NAME_EXTENT, extent, Field.Store.YES));

            indexWriter.addDocument(luceneDoc);
        }
    }

    private static void queryFast(String dir, String name, int nExpectedHits) throws IOException, ParseException {
        IndexReader indexReader = DirectoryReader.open(FSDirectory.open (new File(dir)));
        StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_47, new CharArraySet(Version.LUCENE_47, new ArrayList<String>(), true /* ignore case */)); // empty chararrayset, so effectively no stop words
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        QueryParser parser = new QueryParser(Version.LUCENE_47, FIELD_NAME_LABELS, analyzer);

        Query query = parser.parse("\"" + name + "\"");
        TopDocs docs = indexSearcher.search (query, null, 10000);

        // note a quoted
        out.println ("searching for " + name);
        long startTimeMillis = System.currentTimeMillis();
        int i = 0;
        for (ScoreDoc scoreDoc: docs.scoreDocs) {
            out.println ("---- #" + (++i));
            Document d = indexSearcher.doc(scoreDoc.doc);
            for (IndexableField ifield: d.getFields()) {
                out.println (ifield.name() + "=" + ifield.stringValue());
            }
        }
        long endTimeMillis = System.currentTimeMillis();
        out.println ("time taken = " + (endTimeMillis - startTimeMillis) + "ms");

        if (docs.scoreDocs.length == nExpectedHits)
            out.println ("Good, got expected result for " + name);
        else {
            err.println("ERROR: " + docs.scoreDocs.length + " docs for " + name + ", expected " + nExpectedHits);
            throw new RuntimeException("test failed");
        }
        indexReader.close();
    }

    private static void test(String dir) throws IOException, ParseException {
        queryFast (dir, "Barack Obama", 1);
        queryFast (dir, "Barak Obama", 1);
        queryFast (dir, "barack", 3);
        queryFast (dir, "Gandhi", 24);
        queryFast (dir, "Gandhi Mohandas", 1);
        queryFast (dir, "Junk somename", 0);
    }
}
