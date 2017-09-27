package edu.stanford.muse.ie;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.Config;
import edu.stanford.muse.email.AddressBook;
import edu.stanford.muse.email.Contact;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.index.Archive;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static edu.stanford.muse.ie.FASTIndexer.FIELD_NAME_FAST_ID;
import static edu.stanford.muse.ie.variants.EntityMapper.canonicalize;

/**
 * Authority mapper for correspondents only
 */
public class AuthorityMapper implements java.io.Serializable {
    public static Log log					= LogFactory.getLog(AuthorityMapper.class);

    private final static long serialVersionUID = 1L;
    public final static long INVALID_FAST_ID = -1L;

    /** small class meant to convey temp. results to the frontend. Not serialized. */
    public static class AuthorityInfo {
        public boolean isConfirmed;
        public int nMessages;
        public String name, tooltip, url, errorMessage;
        public AuthorityRecord confirmedAuthority;
        public List<AuthorityRecord> candidates;
    }

    /** important class - to store a confirmed (or candidate) auth records. might be extended to support geonames or other kinds of auth dbs in the future */
    public static class AuthorityRecord implements java.io.Serializable {
        public long fastId;
        public String lcshId, lcnafId, wikipediaId, viafId, localId;
        public String preferredLabel, altLabels;
        public String extent; /* e.g. 1940-2012, or b. 1223 */
        boolean isManuallyAssigned;
    }

    /** the core data in this class is:
     *  (1) cnameToAuthority (for confirmed authorities only)
        (2) cnameToFastIdCandidates -> holds candidates for names in an archive. Expensive to compute, so cached and serialized.
        (3) cnameToCount - counts of cname occurrences
     */
    private Map<String, AuthorityRecord> cnameToAuthority = new LinkedHashMap<>(); // these are confirmed authority records

    // to save memory, we store only the fast_id's not the entire auth record object. this may change when we start computing candidates using other databases. for now, the only db we use to map to a candidate is FAST.
    private Multimap<String, Long> cnameToFastIdCandidates = LinkedHashMultimap.create();

    private Map<String, Integer> cnameToCount = new LinkedHashMap<>(); // name to count of # of times it appears in the archive. applicable only for correspondents currently.

    // lucene querying stuff
    transient private StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_47, new CharArraySet(Version.LUCENE_47, new ArrayList<String>(), true /* ignore case */));
    transient private IndexSearcher indexSearcher;
    transient private QueryParser parser;
    transient private IndexReader indexReader;

    /* creates a mapper. first checks if it already exists in the archive dir. otherwise creates a new one, and initializes it for the archive (might take a while to generate candidates) */
    public static AuthorityMapper createAuthorityMapper (Archive archive) throws IOException, ParseException, ClassNotFoundException {

        String filename = archive.baseDir + File.separator + Config.AUTHORITIES_FILENAME;
        File authMapperFile = new File (filename);
        AuthorityMapper mapper = null;

        try {
            if (authMapperFile.exists() && authMapperFile.canRead()) {
                mapper = (AuthorityMapper) Util.readObjectFromFile(filename);
            }
        } catch (Exception e) {
            Util.print_exception ("Error reading authority mapper file: " + filename, e, log);
        }

        if (mapper == null)
            mapper = new AuthorityMapper();

        // get ready for queries
        mapper.openFastIndex ();

        // compute candidates if we don't have them yet. may need some way to force recomputation in the future, even if already computed.
        if (mapper.cnameToFastIdCandidates.isEmpty() || mapper.cnameToCount.isEmpty()) {
            // this can take a while.... might need to make a progress bar available for large archives
            mapper.setupCandidatesAndCounts(archive);
        }
        return mapper;
    }

    /** this should be called during creation time, or any time the cnameToFastIdCandidates has to be recomputed */
    public void setupCandidatesAndCounts(Archive archive) throws IOException, ParseException {
        AddressBook ab = archive.getAddressBook();

        List<Contact> contacts = ab.allContacts();
        for (Contact c : contacts) {
            try {
                Set<String> names = c.names;
                if (Util.nullOrEmpty(names))
                    continue;

                String contactName = c.pickBestName();
                String cname = canonicalize(contactName);
                List<String> cnameTokens = Util.tokenize(cname);
                if (cnameTokens.size() < 2)
                    continue; // only match when 2 or more words are present in the name

                if (cnameToAuthority.get(cname) != null) {
                    continue;
                } else {
                    for (String name : names) {
                        List<String> nameTokens = Util.tokenize(name);
                        if (nameTokens.size() < 2)
                            continue; // only match when 2 or more words are present in the name

                        List<Document> hits = lookupNameInFastIndex(name);
                        if (hits.size() > 20)
                            log.warn ("Warning: many (" + hits.size() + ") hits for authority name=" + name + " (associated with contact " + contactName + ")");
                        for (Document d : hits) {
                            Long fastId = Long.parseLong(d.get(FIELD_NAME_FAST_ID));
                            cnameToFastIdCandidates.put(cname, fastId);
                        }
                    }
                }
            } catch (Exception e) {
                Util.print_exception("Error parsing contact for authorities: " + c, e, log);
            }
        }

        List<Pair<Contact, Integer>> pairs = ab.sortedContactsAndCounts((Collection) archive.getAllDocs());
        for (Pair<Contact, Integer> p : pairs) {
            Contact c = p.getFirst();
            String name = c.pickBestName();
            String cname = canonicalize(name);
            if (Util.nullOrEmpty(cname))
                continue;

            cnameToCount.put(cname, p.getSecond());
        }
    }

    /** returns a string with the confirmed authorities in a CSV format. */
    public String getAuthoritiesAsCSV () throws IOException {

        StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw, ',', '"', '\n');

        // write the header line: "name, fast, viaf, " etc.
        List<String> line = new ArrayList<>();
        line.add ("Name");
        line.add ("FAST Id");
        line.add ("VIAF Id");
        line.add ("Wikipedia Id");
        line.add ("LoC Subject Headings Id");
        line.add ("LoC Named Authority File Id");
        line.add ("Local Id");
        line.add ("Extent");
        line.add ("Is manually assigned");

        writer.writeNext(line.toArray(new String[line.size()]));

        // write the records
        if (cnameToAuthority != null) {
            for (AuthorityRecord auth : cnameToAuthority.values()) {
                // note: the cname itself is not exported.
                line = new ArrayList<>();
                line.add(auth.preferredLabel);
                line.add(Long.toString(auth.fastId));
                line.add(auth.viafId);
                line.add(auth.wikipediaId);
                line.add(auth.lcshId);
                line.add(auth.lcnafId);
                line.add(auth.localId);
                line.add(auth.extent);
                line.add(auth.isManuallyAssigned ? "Y" : "N");

                writer.writeNext(line.toArray(new String[line.size()]));
            }
        }
        writer.close();
        String csv = sw.toString();
        return csv;
    }


    /** populates the other ids, given the fast Id */
    private AuthorityRecord getAuthRecordForFASTId (long fastId) throws ParseException, IOException {
        AuthorityRecord result = new AuthorityRecord();
        String labelSeparator = " ; ";
        result.fastId = fastId;
        Query query = NumericRangeQuery.newLongRange(FIELD_NAME_FAST_ID, fastId, fastId, true, true); // don't do a string query, must do a numeric range query

        if (indexSearcher == null)
            this.openFastIndex();

        TopDocs docs = indexSearcher.search (query, null, 10000);

        // there should be only 1 result
        for (ScoreDoc scoreDoc: docs.scoreDocs) {
            Document d = indexSearcher.doc(scoreDoc.doc);
            result.viafId = d.get(FASTIndexer.FIELD_NAME_VIAF_ID);
            result.lcshId = d.get(FASTIndexer.FIELD_NAME_LCSH_ID);
            result.lcnafId = d.get(FASTIndexer.FIELD_NAME_LCNAF_ID);
            result.wikipediaId = d.get(FASTIndexer.FIELD_NAME_WIKIPEDIA_ID);
            result.localId = null; // Local Id won't be in the fast index
            result.extent = d.get(FASTIndexer.FIELD_NAME_EXTENT);

            String labels = d.get(FASTIndexer.FIELD_NAME_LABELS); // this has prefLabel followed by altLabels, all separated with labelSeparator
            if (!Util.nullOrEmpty(labels)) {
                String splitLabels[] = labels.split (labelSeparator, 2);
                if (!Util.nullOrEmpty(splitLabels[0]))
                    result.preferredLabel = splitLabels[0];
                if (splitLabels.length > 1 && !Util.nullOrEmpty(splitLabels[1]))
                    result.altLabels = splitLabels[1];
            }
        }
        return result;
    }

    /** returns an authorityInfo object representing info needed by the front-end. Use only for rendering the authorities table. */
    public AuthorityInfo getAuthorityInfo (String archiveID,AddressBook ab, String name) throws IOException, ParseException {
        String cname = canonicalize (name);
        AuthorityInfo result = new AuthorityInfo();
        result.isConfirmed = false;
        result.name = name;

        Integer nMessages = (cnameToCount != null) ? cnameToCount.get(cname) : null;
        result.nMessages = (nMessages == null) ? 0 : nMessages;

        Contact c = ab.lookupByName(name);
        if (c == null) {
            result.errorMessage = "Name not in address book: " + name;
            return result;
        }

        result.url = "browse?archiveID="+archiveID+"&adv-search=1&contact=" + ab.getContactId(c);
        result.tooltip = c.toTooltip();
        result.nMessages = nMessages;

        AuthorityRecord authRecord = cnameToAuthority.get(cname);
        if (authRecord != null) {
            result.isConfirmed = true;
            result.confirmedAuthority = authRecord;
        }

        List<AuthorityRecord> candidates = new ArrayList<>();
        Collection<Long> fastIds = cnameToFastIdCandidates.get(cname);
        if (fastIds != null)
            for (Long id : fastIds)
                candidates.add (getAuthRecordForFASTId(id));

        result.candidates = candidates;
        return result;
    }

    /** confirms an auth record */
    public AuthorityRecord setAuthRecord (String name, long fastId, String viafId, String wikipediaId, String lcnafId, String lcshId, String localId, boolean isManuallyAssigned) throws IOException, ParseException {
        AuthorityRecord ar;

        if (isManuallyAssigned) {
            ar = new AuthorityRecord();
            ar.preferredLabel = name;
            ar.fastId = fastId;
            ar.viafId = viafId;
            ar.wikipediaId = wikipediaId;
            ar.lcnafId = lcnafId;
            ar.lcshId = lcshId;
            ar.localId = localId;
            ar.isManuallyAssigned = isManuallyAssigned;
        } else {
            if (fastId != INVALID_FAST_ID)
                ar = getAuthRecordForFASTId(fastId);
            else {
                ar = new AuthorityRecord();
                ar.preferredLabel = name;
                ar.fastId = INVALID_FAST_ID;
            }
        }

        if (ar != null) {
            String cname = canonicalize(name);
            cnameToAuthority.put(cname, ar);
        }

        return ar;
    }

    public void unsetAuthRecord (String name) {
        String cname = canonicalize(name);
        cnameToAuthority.remove (cname);
    }

    private List<Document> lookupNameInFastIndex (String name) throws IOException, ParseException {
        List<Document> result = new ArrayList<>();

        // be careful, double quotes inside the name can mess things up and result in spurious hits.
        // e.g. we got name as the string:Karl "Fritz" Mueller
        // since we're going to give the whole string embedded in double quotes as a query to Lucene, the query got converted to "Karl "Fritz" Mueller", matching over 3000 records with Fritz
        // so we simple replace any double quotes with nothing
        name = name.replaceAll ("\"", "");

        Query query = parser.parse("\"" + name + "\"");
        TopDocs docs = indexSearcher.search (query, null, 10000);

        for (ScoreDoc scoreDoc: docs.scoreDocs) {
            Document d = indexSearcher.doc(scoreDoc.doc);
            result.add (d);
        }
        return result;
    }

    private void openFastIndex () throws IOException {
        String dir = Config.FAST_INDEX_DIR;
        indexReader = DirectoryReader.open(FSDirectory.open (new File(dir)));
        analyzer = new StandardAnalyzer(Version.LUCENE_47, new CharArraySet(Version.LUCENE_47, new ArrayList<String>(), true /* ignore case */)); // empty chararrayset, so effectively no stop words
        indexSearcher = new IndexSearcher(indexReader);
        parser = new QueryParser(Version.LUCENE_47, FASTIndexer.FIELD_NAME_LABELS, analyzer);
    }

    private void closeFastIndex () throws IOException {
        if (indexReader != null)
            indexReader.close();
    }
}
