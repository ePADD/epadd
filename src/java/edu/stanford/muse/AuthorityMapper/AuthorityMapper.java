package edu.stanford.muse.AuthorityMapper;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.Config;
//import edu.stanford.muse.AddressBookManager.AddressBook;
//import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.ie.FASTIndexer;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
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
import static edu.stanford.muse.ie.variants.EntityBook.canonicalize;

/**
 * Authority mapper for correspondents only
 * Update: Chinmay Modified this class to make it agnostic to correspondents or entities. Essentially
 * this class should be inherited to specialize the use of authority mapper either for the correspondent
 * or for the entities.
 */
public class AuthorityMapper implements java.io.Serializable {
    public static Log log					= LogFactory.getLog(AuthorityMapper.class);

    private final static long serialVersionUID = 1L;
    public final static long INVALID_FAST_ID = -1L;



    /** important class - to store a confirmed (or candidate) auth records. might be extended to support geonames or other kinds of auth dbs in the future */
    public static class AuthorityRecord implements java.io.Serializable {
        public long fastId;
        public String lcshId, lcnafId, wikipediaId, viafId, localId;
        public String preferredLabel, altLabels;
        public String extent; /* e.g. 1940-2012, or b. 1223 */
        public boolean isManuallyAssigned;
    }

    /** the core data in this class is:
     *  (1) cnameToAuthority (for confirmed authorities only)
        (2) cnameToFastIdCandidates -> holds candidates for names in an archive. Expensive to compute, so cached and serialized.
        (3) cnameToCount - counts of cname occurrences
     */
    protected Map<String, AuthorityRecord> cnameToAuthority = new LinkedHashMap<>(); // these are confirmed authority records

    // to save memory, we store only the fast_id's not the entire auth record object. this may change when we start computing candidates using other databases. for now, the only db we use to map to a candidate is FAST.
    protected Multimap<String, Long> cnameToFastIdCandidates = LinkedHashMultimap.create();

    protected Map<String, Integer> cnameToCount = new LinkedHashMap<>(); // name to count of # of times it appears in the archive. applicable only for correspondents currently.

    transient private IndexSearcher indexSearcher;
    transient private QueryParser parser;
    transient private IndexReader indexReader;


    /** returns a string with the confirmed authorities in a CSV format. */
    public String getAuthoritiesAsCSV () throws IOException {

        StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw, ',', '"', '\n');

        // write the header line: "name, fast, viaf, " etc.
        List<String> line = new ArrayList<>();
        line.add("Canonical Name");
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
            for(String canonname : cnameToAuthority.keySet()){
                AuthorityRecord auth = cnameToAuthority.get(canonname);
                // note: the cname itself is not exported.
                line = new ArrayList<>();
                line.add(canonname);

                if(auth.preferredLabel.equals("?"))
                    line.add("unknown");
                else
                    line.add(auth.preferredLabel);

                if(auth.fastId==0 || auth.fastId==-1)
                    line.add("unknown");
                else
                    line.add(Long.toString(auth.fastId));


                if(auth.viafId==null || auth.viafId.equals("?"))
                    line.add("unknown");
                else
                    line.add(auth.viafId);


                if(auth.wikipediaId== null || auth.wikipediaId.equals("?"))
                    line.add("unknown");
                else
                    line.add(auth.wikipediaId);

                if(auth.lcshId==null || auth.lcshId.equals("?"))
                    line.add("unknown");
                else
                    line.add(auth.lcshId);

                if(auth.lcnafId==null || auth.lcnafId.equals("?"))
                    line.add("unknown");
                else
                    line.add(auth.lcnafId);

                if(auth.localId==null || auth.localId.equals("?"))
                    line.add("unknown");
                else
                    line.add(auth.localId);

                if(auth.extent==null || auth.extent.equals("?"))
                    line.add("unknown");
                else
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
    public AuthorityRecord getAuthRecordForFASTId (long fastId) throws ParseException, IOException {
        AuthorityRecord result = new AuthorityRecord();
        String labelSeparator = " ; ";
        result.fastId = fastId;
        Query query =  LongPoint.newRangeQuery(FIELD_NAME_FAST_ID, fastId, fastId); // don't do a string query, must do a numeric range query

        if (indexSearcher == null)
            this.openFastIndex();

        TopDocs docs = indexSearcher.search (query, 10000, Sort.RELEVANCE);

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

    public  List<Document> lookupNameInFastIndex (String name) throws IOException, ParseException {
        List<Document> result = new ArrayList<>();

        // be careful, double quotes inside the name can mess things up and result in spurious hits.
        // e.g. we got name as the string:Karl "Fritz" Mueller
        // since we're going to give the whole string embedded in double quotes as a query to Lucene, the query got converted to "Karl "Fritz" Mueller", matching over 3000 records with Fritz
        // so we simple replace any double quotes with nothing
        name = name.replaceAll ("\"", "");

        Query query = parser.parse("\"" + name + "\"");
        TopDocs docs = indexSearcher.search (query, 10000,Sort.RELEVANCE);

        for (ScoreDoc scoreDoc: docs.scoreDocs) {
            Document d = indexSearcher.doc(scoreDoc.doc);
            result.add (d);
        }
        return result;
    }

    public void openFastIndex () throws IOException {
        String dir = Config.FAST_INDEX_DIR;
        indexReader = DirectoryReader.open(FSDirectory.open (new File(dir).toPath()));
        StandardAnalyzer analyzer = new StandardAnalyzer(new CharArraySet(new ArrayList<String>(), true /* ignore case */));
        indexSearcher = new IndexSearcher(indexReader);
        parser = new QueryParser(FASTIndexer.FIELD_NAME_LABELS, analyzer);
    }

    private void closeFastIndex () throws IOException {
        if (indexReader != null)
            indexReader.close();
    }




}
