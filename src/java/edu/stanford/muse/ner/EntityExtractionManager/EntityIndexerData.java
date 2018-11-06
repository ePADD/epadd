package edu.stanford.muse.ner.EntityExtractionManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import edu.stanford.muse.Config;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.index.EnglishNumberAnalyzer;
import edu.stanford.muse.index.Indexer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Starting version 7, the entity extraction is refactored. We propose the following;
 * 1. Keep all entities (obtained from DBPedia) in lucene for fast and fuzzy search.
 * 2. This class is the interface of querying lucene for stored entities. It is used during entity inference in the archive content.
 * 3. Main method in this class will be run only once to store the database of entities in lucene.
 * We use two fields in each doc to store the entity name and type.
 * When we infer rules, we use 'entity name' field to store rule [like (PERSON,of,PLACE)] and 'entity type' to store the resultant type like 'PLACE'
 */
public class EntityIndexerData {
    static Log log = LogFactory.getLog(Indexer.class);
    private static final long serialVersionUID = 1L;

    private enum fields {
        ENTITY_NAME, ENTITY_TYPE; //in future add a field for ENTITY_RULE as well.
    }
    public static final Version LUCENE_VERSION				= Version.LUCENE_7_2_1;
    private static final String	INDEX_BASE_DIR_NAME			= Archive.INDEXES_SUBDIR;
    private static final String	INDEX_NAME_DIR			= "DBPediaIndex";
    private static final String	INDEX_NAME_ATTACHMENTS		= "attachments";
    //I dont see why the presetQueries cannot be static. As we read these from a file, there cannot be two set of preset queries for two (or more) archives in session
    static String[]		presetQueries				= null;

    private Map<String, EmailDocument> docIdToEmailDoc			= new LinkedHashMap<>();			// docId -> EmailDoc
    private Map<String, Blob>						attachmentDocIdToBlob	= new LinkedHashMap<>();					// attachment's docid -> Blob
    private HashMap<String, Map<Integer, String>> dirNameToDocIdMap		= new LinkedHashMap<>();	// just stores 2 maps, one for content and one for attachment Lucene doc ID -> docId

    transient private Directory directory;
    transient private Directory	directory_blob;																// for attachments
    transient private Analyzer analyzer;
    transient private IndexSearcher isearcher;
    transient private IndexSearcher	isearcher_blob;
    transient private QueryParser parser, parserOriginal, parserSubject, parserCorrespondents, parserRegex, parserMeta;		// parserOriginal searches the original content (non quoted parts) of a message
    transient private IndexWriter iwriter;
    transient private IndexWriter iwriter_blob;
    transient Map<Integer,String> blobDocIds;
    transient Map<Integer,String> contentDocIds;														// these are fieldCaches of ldoc -> docId for the docIds (for performance)

    // field type ft: stored and indexed. storeOnly_ft: only stored, full_ft: stored, indexed and analyzed.
    //field ename: indexed, stored and analyzed. etype: stored only.
    //if we want to support regex then ename: indexed, stored and unanalyzed.
    transient private static FieldType storeOnly_ft;
    static transient FieldType full_ft;
    static {
        storeOnly_ft = new FieldType();
        storeOnly_ft.setStored(true);
        storeOnly_ft.freeze();

//        ft = new FieldType();
//        ft.setStored(true);
//        //@TODO: Check if from lucene 7.2 by default this field is indexed.
//        //ft.setIndexed(true);
//        //Since lucene 7.2, a field is indexed if its indexoptions is set anything else than None.
//        ft.setIndexOptions(org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS);
//        ft.setTokenized(false);
//        ft.freeze();

        full_ft = new FieldType();
        full_ft.setStored(true);
        //@TODO: Check if from lucene 7.2 by default this field is indexed.
        //full_ft.setIndexed(true);
        full_ft.setIndexOptions(org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        full_ft.freeze();

        //use this field for spanning regular expression, also tokenises text
//		unanalyzed_full_ft = new FieldType();
//		unanalyzed_full_ft.setStored(true);
//		unanalyzed_full_ft.setIndexed(true);
//		unanalyzed_full_ft.setTokenized(false);
//		unanalyzed_full_ft.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
//		unanalyzed_full_ft.freeze();
    }


    EntityIndexerData(){
        analyzer=newAnalyzer();
    }
    private Analyzer newAnalyzer() {
        // we can use LimitTokenCountAnalyzer to limit the #tokens

//        EnglishAnalyzer stemmingAnalyzer = new EnglishAnalyzer( );
//        EnglishNumberAnalyzer snAnalyzer = new EnglishNumberAnalyzer(MUSE_STOP_WORDS_SET);

        Map<String, Analyzer> map = new LinkedHashMap<>();

        KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);
        map.put("ename", standardAnalyzer);
        map.put("etype", standardAnalyzer);

        /////////// For supporting non-stemmed version-------/////
		/*
		//do not remove any stop words.
		map.put("body", standardAnalyzer);
		map.put("title", standardAnalyzer);
		map.put("body_original", standardAnalyzer);
		*/
        ////////////////////////////////////////////////////////////////
        return new PerFieldAnalyzerWrapper(standardAnalyzer, map);
    }

    private void addDoc(IndexWriter writer, String name, String type) throws IOException {
        // make a new, empty document
        Document doc = new Document();
        //create a field to store name of the entity
        Field entitynamefield = new Field("ename", name, full_ft);
        doc.add(entitynamefield);
        //create another field to store type.
        Field entitytypefield = new Field("etype", type, storeOnly_ft);
        doc.add(entitytypefield);

        writer.addDocument(doc);


    }

    public SetMultimap<String,String> findCandidateTypes(String entityname) throws ParseException, IOException {
        String indexPath =  Config.DEFAULT_SETTINGS_DIR+ File.separator + INDEX_NAME_DIR;
        SetMultimap<String,String> entityTypeRsult = HashMultimap.create();
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);
        String field = "ename";
        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(entityname);
        ScoreDoc[] result = searcher.search(query, 100).scoreDocs;
        for(int i=0; i<result.length;i++) {
            Document doc = searcher.doc(result[i].doc);
            String type =  doc.get("etype");
            entityTypeRsult.put(doc.get("ename"),type);
        }
        return entityTypeRsult;
    }


    public void indexInitialize(String entityListingFileName, boolean append){

         String indexPath =  Config.DEFAULT_SETTINGS_DIR+ File.separator + INDEX_NAME_DIR;

        try(Directory dir = FSDirectory.open(Paths.get(indexPath)); BufferedReader br = new BufferedReader(new FileReader(entityListingFileName))) {
            System.out.println("Indexing to directory '" + indexPath + "'...");
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            if (!append) {
                // Create a new index in the directory, removing any
                // previously indexed documents:
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            } else {
                // Add new documents to an existing index:
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            }

            // Optional: for better indexing performance, if you
            // are indexing many documents, increase the RAM
            // buffer.  But if you do this, increase the max heap
            // size to the JVM (eg add -Xmx512m or -Xmx1g):
            //
            // iwc.setRAMBufferSizeMB(256.0);

            IndexWriter writer = new IndexWriter(dir, iwc);
            //read entityListingFileName file one by one, in the format of name,type,count
            //for each one.. create a doc.. put name in the name field, type in the type field
            String line = null;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("\",","\":::");
                String[] splitted = line.split(":::");
                //first and second have surrounding quotes, remove them first.
                String entityname = splitted[0].substring(1,splitted[0].length()-1);
                String entitytype = splitted[1].substring(1,splitted[1].length()-1);
                addDoc(writer,entityname,entitytype);
                lineNum++;
                System.out.println("Done--"+line);
            }
            System.err.println("Indexed #" + lineNum + " from DBPedia entity file: " + entityListingFileName);
            br.close();
            writer.close();
            } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
            }

    }

    public static void main(String args[]){
        EntityIndexerData eidata  = new EntityIndexerData();
        //eidata.indexInitialize("/Users/tech/Projects/epadd-dev/epadd/DBPediaFacts.pl",false);
        try {
            SetMultimap<String,String> result = eidata.findCandidateTypes("Florida");
            Set<String> types = new LinkedHashSet<>(result.values());
            Map<String,Long> freq = result.entries().stream().collect(Collectors.groupingBy(obj->obj.getValue(),Collectors.counting()));
            result.entries().forEach( entry->System.out.println("Name is "+ entry.getKey()+ ", Type is "+entry.getValue()));
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}