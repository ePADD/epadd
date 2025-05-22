/*
 * Copyright (C) 2012 The Stanford MobiSocial Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
    2022-09-13  Added finalizer to close the index files
*/
package edu.stanford.muse.index;

import edu.stanford.muse.Config;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.email.StatusProvider;
import edu.stanford.muse.lang.Languages;
import edu.stanford.muse.ner.NER;
import edu.stanford.muse.util.*;
import edu.stanford.muse.webapp.JSPHelper;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.automaton.RegExp;

import javax.mail.Header;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * this class is pretty closely tied with the summarizer (which generates cards  - Muse only.).
 * the function of the indexer is to perform indexing, and provide a query
 * interface to the index.
 *
 * This index has 2-levels: Docs and MultiDocs (that themselves contain entire
 * documents as subdocs) against each other. MultiDocs are docs for a particular month, a concept rarely used now.
 * The index can be looked up with specific terms, and returns subdocs that contain the term.
 *
 * Ideally, no method/field should be set to visibility higher than protected in this class
 * Any method/field in this class should be strictly accessible only through Archive class, not even from the same package.
 *
 * there are two reasons for delegations of methods here
 * 1. Some classes in the index package are so tightly using Indexer (For example Cluer, Lexicon, Summarizer), that to make them all go through archive is a lot of code changes
 * 2. The method really belongs to this Class.
 *
 * cluster numbers mentioned here are for MultiDocs -- (earlier used for months, now rarely used.)
 */
@Getter
public class Indexer implements StatusProvider, java.io.Serializable {
	static final Logger					log					= LogManager.getLogger(Indexer.class);
	private static final long	serialVersionUID	= 1L;

	/** these enums should move out of this class if Indexer is to be made protected because they are part of the API -sgh */
	public enum QueryType {
		FULL, ORIGINAL, CORRESPONDENTS, SUBJECT, REGEX, PRESET_REGEX, META
	}

	public enum SortBy{
		RELEVANCE, CHRONOLOGICAL_ORDER, RECENT_FIRST
	}

	// weight given to email subject; 2 means subject is given 2x weight
    static final int			DEFAULT_SUBJECT_WEIGHT			= 2;
	public static final int		MAX_MAILING_LIST_NAME_LENGTH	= 20;
	private static final String	LANGUAGE_FIELD_DELIMITER	= "|";								    // warning: make sure this is a single char, not a string because StringTokenizer constructor with "AB" will split on A or B
	public static final String	NAMES_FIELD_DELIMITER		= "\n";									// this String is not allowed within a name
	public static final Version LUCENE_VERSION				= Version.LUCENE_7_2_1;
	private static final String	INDEX_BASE_DIR_NAME			= Archive.INDEXES_SUBDIR;
	private static final String	INDEX_NAME_EMAILS			= "emails";
	private static final String	INDEX_NAME_ATTACHMENTS		= "attachments";
    //I dont see why the presetQueries cannot be static. As we read these from a file, there cannot be two set of preset queries for two (or more) archives in session
	static String[]		presetQueries				= null;

	private final Map<String, EmailDocument>				docIdToEmailDoc			= new LinkedHashMap<>();			// email docId -> EmailDoc
	private Map<String, Blob>						attachmentDocIdToBlob	= new LinkedHashMap<>();					// attachment's docid -> Blob
    private HashMap<String, Map<Integer, String>>	dirNameToDocIdMap		= new LinkedHashMap<>();	// just stores 2 maps, one for content and one for attachment Lucene doc ID -> docId

	private File baseDirFile;
	private File indexBaseDirFile;
	transient private Directory directory;
	transient private Directory	directory_blob;																// for attachments
	transient private Analyzer analyzer;
	transient private IndexSearcher isearcher;
	transient private IndexSearcher	isearcher_blob;
	transient private QueryParser parser, parserEntityFields, parserOriginal, parserSubject, parserCorrespondents, parserRegex, parserMeta;		// parserOriginal searches the original content (non quoted parts) of a message
	transient private IndexWriter iwriter;
	transient private IndexWriter iwriter_blob;
	transient Map<Integer,String> blobDocIds;
	public transient Map<Integer,String> contentDocIds;														// these are fieldCaches of ldoc -> docId for the docIds (for performance)

	transient private String baseDir = null;												// where the file-based directories should be stored (under "indexes" dir)

	// these are field type configs for all the fields to be stored in the email and attachment index.
	// most fields will use ft (stored and analyzed), and only the body fields will have a full_ft which is stored, analyzed (with stemming) and also keeps term vector offsets and positions for highlights
	// some fields like name_offsets absolutely don't need to be indexed and can be kept as storeOnly_ft
	final transient private static FieldType storeOnly_ft = new FieldTypeStoredOnly();;
	private static final transient FieldType ft = new FieldTypeStoredAnalyzed();
	public static final transient FieldType full_ft  = new FieldTypeStoredAnalysedTokenized();
	static transient FieldType unanalyzed_full_ft;													// unanalyzed_full_ft for regex search
	private static final CharArraySet MUSE_STOP_WORDS_SET = StopWords.getCharArraySet();

	private IndexOptions				io;
	private boolean				cancel							= false;
	final List<LinkInfo>	links							= new ArrayList<>();

	// Collection<String> dataErrors = new LinkedHashSet<String>();

	@Getter
	public static class IndexStats implements java.io.Serializable {
		private static final long	serialVersionUID		= 1L;
		// int nTokens = 0, nStopTokens = 0, nDictTokens = 0;
        //total # of messages indexed
		int				nDocuments				= 0;
        //#chars indexed
		long				indexedTextSize			= 0;
        //#chars indexed in original content
		long				indexedTextSizeOriginal	= 0;
		long				indexedTextLength_blob	= 0, nIndexedNames_blob = 0;	// attachments indexed

		// original => only in original content, i.e. not quoted parts of
		// messages
        //total # of occurances i.e. each name will be counted as many times as it occurs
		int				nNames					= 0, nOriginalNames = 0;
        //# of unique names in entire corpus, # of unique names in original content
		public int					nUniqueNames			= 0, nUniqueNamesOriginal = 0;
	}

	/** query options is a small class th	at holds a query, including its term and type.
	 * cluster, start/end date are used only by Muse frontend. */
    public static class QueryOptions{
        int cluster = -1, threshold = -1;
        QueryType qt = QueryType.FULL; // defauly
        //filter options
        Date startDate, endDate;
        SortBy sortBy = SortBy.CHRONOLOGICAL_ORDER;

        public void setCluster(int cluster){
            this.cluster = cluster;
        }
        public void setThreshold(int threshold){
            this.threshold = threshold;
        }
        public void setQueryType(QueryType qt){
            this.qt = qt;
        }
        public void setStartDate(Date d){
            this.startDate = d;
        }
        public void setEndDate(Date d){
            this.endDate = d;
        }
        public void setSortBy(SortBy sortBy){
            this.sortBy = sortBy;
        }
        int getThreshold(){return threshold;}
        int getCluster(){return cluster;}
        QueryType getQueryType(){return qt;}
        Date getStartDate(){return startDate;}
        Date getEndDate(){return endDate;}
        SortBy getSortBy(){return sortBy;}
    }

	IndexStats			stats					= new IndexStats();

	private List<MultiDoc>	docClusters;
	final List<Document>				docs					= new ArrayList<>();

	private Indexer(IndexOptions io) {
		clear();
		this.io = io;
	}

	private Indexer() throws IOException {
		this(null, null);
	}

	Indexer(String baseDir, IndexOptions io) throws IOException {
		this(io);
		this.baseDir = baseDir;
		//		analyzer = new StandardAnalyzer(MUSE_LUCENE_VERSION, MUSE_STOP_WORDS_SET);
		//		analyzer = new SnowballAnalyzer(MUSE_LUCENE_VERSION, "English", MUSE_STOP_WORDS_SET);
		analyzer = newAnalyzer();
		directory = createDirectory(INDEX_NAME_EMAILS);
		iwriter = openIndexWriter(directory);
		directory_blob = createDirectory(INDEX_NAME_ATTACHMENTS);
		iwriter_blob = openIndexWriter(directory_blob);
	}

	interface FilterFunctor {
		boolean filter(org.apache.lucene.document.Document doc); // can modify doc and return whether the result should be included
	}

    // Below two routines are for dropping fields from index on the given directory.
	// (Adapted from http://www.flax.co.uk/blog/2011/06/24/how-to-remove-a-stored-field-in-lucene/)

	// report on whether some of these fields exist in the given directory, and return result
	private static boolean indexHasFields() {
		return true; // return true pending migration of IndexReader to DirectoryReader

		//		if (Util.nullOrEmpty(fields))
		//			return false;
		//
		//		DirectoryReader reader = DirectoryReader.open(dir); // IndexReader.open(dir, true); // read-only=true
		//		// getFieldNames is no longer supported!
		//		Collection<String> fieldNames = reader.getFieldNames(FieldOption.ALL);
		//		reader.close();
		//		for (String field : fields) {
		//			if (fieldNames.contains(field)) {
		//				return true;
		//			}
		//		}
		//		return false;
	}

	/** returns whether indexAttachments succeeded */
	private synchronized boolean indexAttachments(EmailDocument e, BlobStore blobStore, Set<Blob> processedBlobSet, IndexStats stats) throws IOException
	{
		boolean result = true;
		// bail out if no attachments
		if (e.attachments == null)
			return true;

		final String DELIMITER = "\n";
		for (Blob b : e.attachments) {
			if (processedBlobSet != null && processedBlobSet.contains(b))
				continue; // skip if already processed (blob may be shared by multiple docs)

			/*int id_int = iwriter_blob.numDocs();
			String id = Integer.toString(++id_int);*/
			int id_int = blobStore.index(b);
			String id = Integer.toString(id_int);
			if (processedBlobSet != null)
				processedBlobSet.add(b);
			attachmentDocIdToBlob.put(id, b);

			org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document(); // not to be confused with edu.stanford.muse.index.Document
			Pair<String, String> content = blobStore.getContent(b);
			if (content == null) {
				// failed to process blob
				result = false;
				log.warn("Failed to fetch content from: "+ blobStore.get_URL_Normalized(b)+" content type: "+b.contentType+" size: "+b.getSize());
				continue; // but try to continue the process
			}

			// imp: for id, should use Field.Index.NOT_ANALYZED field should be http://vuknikolic.wordpress.com/2011/01/03/lucenes-field-options-store-and-index-aka-rtfm/
			// note: id for attachments index is just sequential numbers, 1, 2, 3. etc.
			// it is not the full unique id (<folder>-<num>) that the emails index has.
			//NOTE: docid, emaildocid and languages fields can be stored without position (hence ft as FieldType)
			//because user can not do a phrase query on these fields.
			doc.add(new Field("docId", id, ft));
            //Field type ft instead of StoredFiled so as to be able to search over this field
            doc.add(new Field("emailDocId", e.getUniqueId(), ft));
			String documentText = content.first + DELIMITER + content.second;

			// we'll store all languages detected in the doc as a field in the index
			Set<String> languages = Languages.getAllLanguages(documentText);
			String lang_str = Util.join(languages, LANGUAGE_FIELD_DELIMITER);
			doc.add(new Field("languages", lang_str, ft));

			if(edu.stanford.muse.Config.OPENNLP_NER) {
				Set<String> names = setNameFieldsOpenNLP(documentText, doc);

				String s = Util.join(names, NAMES_FIELD_DELIMITER); // just some connector for storing the field

				doc.add(new Field("names", s, full_ft));

				if(stats!=null)
					stats.nIndexedNames_blob += names.size();
			}

			// log.info ("blob metadata = " + content.first);
			//meta data does not contain the fileName
			doc.add(new Field("meta", content.first, full_ft));
			doc.add(new Field("fileName", blobStore.get_URL_Normalized(b), full_ft));

			//don't tokenize if the content.first is of type zip or gzip which means use ft instead of full_ft
            if(content.first.contains("text"))
                doc.add(new Field("body", content.second, full_ft));
            //else
            	//don't add body field if we can not search this field for the non-text type.
			//Earlier we had resorted to using "ft" (non-positional) indexing but two different type of indexing for the same field gave error
			//while performing phrase query.
            //    doc.add(new Field("body", content.second, ft));

			iwriter_blob.addDocument(doc);
			//log.info("Indexed attachment #" + id + " : text = '" + documentText + "' names = '" + s + "'");
			if (stats != null) {
				stats.indexedTextLength_blob += documentText.length();
			}
		}
		return result;
	}

	/** returns whether indexAttachments succeeded */
	private synchronized boolean indexAttachments(Collection<EmailDocument> docs, BlobStore blobStore) throws IOException
	{
		if (iwriter_blob == null) {
			//if (directory_blob == null) directory_blob = initializeDirectory(directory_blob, INDEX_NAME_ATTACHMENTS); // should already be valid
			iwriter_blob = openIndexWriter(directory_blob);
		}
		attachmentDocIdToBlob = new LinkedHashMap<>();
		Set<Blob> processedBlobSet = new LinkedHashSet<>();
		boolean result = true;
		for (EmailDocument e : docs) {
			result &= indexAttachments(e, blobStore, processedBlobSet, stats);
		}
		iwriter_blob.close();
		return result;
	}

	//Note that baseDir is the path to the bag (that contains data folder. Data folder in turn contains indexes,lexicons,sessions,blobs folders)
	static private Directory createDirectory(String baseDir, String name) throws IOException
	{
		//return new RAMDirectory();
		File f = new File (baseDir);
		f.mkdir();//just to make sure that the parent directory is created.
		String index_dir = baseDir + File.separator + INDEX_BASE_DIR_NAME;
		f = new File (index_dir);
		boolean b = f.mkdir(); // will not create parent basedir if not already exist. ignore the return val
		if (!f.exists() || !f.isDirectory()) {
			log.warn ("Unable to create directory: " + index_dir);
			return null;
		}
		return FSDirectory.open(new File(index_dir + File.separator + name).toPath());
	}

	private Directory createDirectory(String name) throws IOException
	{
		return createDirectory(this.baseDir, name);
	}

	void clear()
	{
		cancel = false;
		links.clear();
	}

	/*
	 * public Collection<String> getDataErrors() { return
	 * Collections.unmodifiableCollection(dataErrors); }
	 */

	/**
	 * quick method for extracting links only, without doing all the parsing and
	 * indexing - used by slant, etc.
	 */
	void extractLinks(Collection<edu.stanford.muse.index.Document> docs) {
		try {
			for (edu.stanford.muse.index.Document d : docs)
			{
				if (cancel)
					break;

				String contents = "";
				if (!io.ignoreDocumentBody)
				{
					contents = getContents(d, true /* original content only */);
				}

				List<LinkInfo> linksForThisDoc = new ArrayList<>();
				IndexUtils.populateDocLinks(d, contents, linksForThisDoc, io.includeQuotedMessages);
				linksForThisDoc = removeDuplicateLinkInfos(linksForThisDoc);
				d.links = linksForThisDoc; // not necessarily needed, right ?
											// should have been init'ed when we
											// first read the contents of this
											// Doc. but doesn't hurt.
				links.addAll(linksForThisDoc);
			}
		} catch (Exception e) {
			Util.print_exception(e);
		}
	}

	String computeStats()
	{
		return computeStats(true);
	}

    //This method cannot be moved to Archive because of stats object
	private String computeStats(boolean blur)
	{
		String result = "Index options: " + io.toString(blur) + "\n"; // + " " +
																		// currentJobDocsetSize
																		// +
																		// " post-filter docs and "
																		// +
		// docs.size() + " multi-docs\n" +
		// clusterStats + "\n";
		if (stats != null)
		{
			result += Util.fieldsToString(stats);
			// Util.commatize(stats.processedTextLength/1024) +
			// "K chars processed (" + stats.unoriginalTextLength/1024 +
			// "K unoriginal), " + stats.nProcessedNames + " names\n" +
			// Util.commatize(stats.processedTextLength_blob/1024) +
			// "K chars of attachments processed, " + stats.nProcessedNames_blob
			// + " names in attachments\n" +
			result += Util.commatize(InternTable.getSizeInChars()) + " chars in " + Util.commatize(InternTable.getNEntries()) + " entries in intern table\n";
		}
		Util.getMemoryStats();

		return result;
	}

	private int	currentJobDocsetSize, currentJobDocsProcessed, currentJobErrors;

	public void cancel()
	{
		cancel = true;
		log.warn("Indexer cancelled!");
	}

	public boolean isCancelled() {
		return cancel;
	}

	public String getStatusMessage()
	{
		if (currentJobDocsetSize == 0)
			return JSONUtils.getStatusJSON("Starting indexer...");

		// compute how much time remains
		long currentJobStartTimeMillis = 0L;
		long elapsedTimeMillis = System.currentTimeMillis() - currentJobStartTimeMillis;
		long unprocessedTimeSeconds = -1;

		// compute unprocessed message
		if (currentJobDocsProcessed != 0)
		{
			long unprocessedTimeMillis = (currentJobDocsetSize - currentJobDocsProcessed) * elapsedTimeMillis / currentJobDocsProcessed;
			unprocessedTimeSeconds = unprocessedTimeMillis / 1000;
		}

		int doneCount = currentJobDocsProcessed + currentJobErrors;
		String descriptor = (io.ignoreDocumentBody) ? "headers" : " messages";
		int pctComplete = (doneCount * 100) / currentJobDocsetSize;
		String processedMessage;
		if (pctComplete < 100)
			processedMessage = "Indexing " + Util.commatize(currentJobDocsetSize) + " " + descriptor;
		else
		{
			processedMessage = "Creating summaries...";
			unprocessedTimeSeconds = -1L;
		}
		return JSONUtils.getStatusJSON(processedMessage, pctComplete, elapsedTimeMillis / 1000, unprocessedTimeSeconds);
	}

	/** -1 => all clusters */
	private List<edu.stanford.muse.index.Document> getDocsInCluster(int clusterNum)
	{
		List<edu.stanford.muse.index.Document> result = new ArrayList<>();
		if (clusterNum < 0)
		{
			for (MultiDoc md : docClusters)
				result.addAll(md.docs);
		}
		else
		{
			MultiDoc clusterDocs = docClusters.get(clusterNum);
			result.addAll(clusterDocs.docs);
		}
		return result;
	}

	/**
	 * we're done indexing, optimize for storage, and prepare for queries, if
	 * applicable
	 */

	//	public String getHTMLAnnotatedDocumentContents(String contents, Date d, String docId, String[] searchTerms, Boolean isRegexSearch, Set<String> stemmedTermsToHighlight,
	//			Set<String> unstemmedTermsToHighlight, Map<String, Map<String, Short>> entitiesWithId) throws Exception
	//	{
	//		return Highlighter.getHTMLAnnotatedDocumentContents(contents, d, docId, searchTerms, isRegexSearch, stemmedTermsToHighlight, unstemmedTermsToHighlight,
	//				entitiesWithId, null, summarizer.importantTermsCanonical
    //              unstemmed because we are only using names
	//	}

	// remove any duplicate link URLs from the incoming LinkInfos
	private static List<LinkInfo> removeDuplicateLinkInfos(List<LinkInfo> input)
	{
		List<LinkInfo> result = new ArrayList<>();
		Set<String> seenURLs = new LinkedHashSet<>();
		for (LinkInfo li : input)
			if (!seenURLs.contains(li.link))
			{
				result.add(li);
				seenURLs.add(li.link);
			}
		return result;
	}

	/**
	 * main entry point for indexing. note: recomputeCards has to be called
	 * separately
	 */
	/*
	 * void processDocumentCollection(List<MultiDoc> mDocs, List<Document> docs,
	 * BlobStore blobStore) throws Exception { log.info ("Processing " +
	 * docs.size() + " documents"); try { indexDocumentCollection(mDocs, docs,
	 * blobStore); } catch (OutOfMemoryError oome) { log.error
	 * ("Sorry, out of memory, results may be incomplete!"); clear(); } }
	 *
	 * /** preprocessed and indexes the docs.
	 */
	/*
	 * private void indexDocumentCollection(List<MultiDoc> mDocs, List<Document>
	 * allDocs, BlobStore blobStore) throws Exception { this.clear();
	 * currentJobStartTimeMillis = System.currentTimeMillis();
	 * currentJobDocsetSize = allDocs.size(); currentJobDocsProcessed =
	 * currentJobErrors = 0;
	 *
	 * System.gc(); String stat1 = "Memory status before indexing " +
	 * allDocs.size() + " documents: " + Util.getMemoryStats(); log.info
	 * (stat1); docClusters = mDocs;
	 *
	 * if (io.do_NER) openNLPNER.printAllTypes();
	 *
	 * computeClusterStats(mDocs); log.info ("Indexing " + allDocs.size() +
	 * " documents in " + docClusters.size() + " clusters"); int clusterCount =
	 * -1; int docsIndexed = 0, multiDocsIndexed = 0; Posting.nPostingsAllocated
	 * = 0; docClusters = mDocs;
	 *
	 * try { for (MultiDoc md: docClusters) { clusterCount++; log.info
	 * ("-----------------------------"); log.info ("Indexing " + md.docs.size()
	 * + " documents in document cluster #" + clusterCount + ": " +
	 * md.description);
	 *
	 * for (Document d: md.docs) { if (cancel) throw new CancelledException();
	 *
	 * String contents = ""; if (!io.ignoreDocumentBody) { try { contents =
	 * d.getContents(); } catch (Exception e) { markDataError
	 * ("Exception trying to read " + d + ": " + e); } }
	 *
	 * if (contents.length() > MAX_DOCUMENT_SIZE) { markDataError
	 * ("Document too long, size " + Util.commatize(contents.length()) +
	 * " bytes, dropping it. Begins with: " + d + Util.ellipsize(contents, 80));
	 * contents = ""; }
	 *
	 * String subject = d.getSubjectWithoutTitle(); subject =
	 * EmailUtils.cleanupSubjectLine(subject);
	 *
	 * indexSubdoc(subject, contents, d, blobStore);
	 *
	 * docsIndexed++; currentJobDocsProcessed++; } // end cluster
	 *
	 * log.info ("Finished indexing multi doc " + md); if (md.docs.size() > 0)
	 * log.info ("Current stats:" + computeStats());
	 *
	 * multiDocsIndexed++; // IndexUtils.dumpDocument(clusterPrefix,
	 * clusterText); // i don't think we need to do this except for debugging
	 * System.out.toString("."); // goes to console, that's ok...
	 *
	 * if (md.docs.size() > 0) { String stat2 = ("Memory status after indexing "
	 * + docsIndexed + " of " + allDocs.size() + " documents in " +
	 * multiDocsIndexed + " (non-zero) multi-docs, total text length " +
	 * stats.processedTextLength + " chars, " + stats.nProcessedNames +
	 * " names. " + Util.getMemoryStats()); log.info (stat2); } } } catch
	 * (OutOfMemoryError oome) { String s =
	 * "REAL WARNING! SEVERE WARNING! Out of memory during indexing. Please retry with more memory!"
	 * + oome; s += "\n"; log.error (s); // option: heroically soldier on and
	 * try to work with partial results }
	 *
	 * // imp: do this at the end to save memory. doesn't save memory during
	 * indexing but saves mem later, when the index is being used. // esp.
	 * important for lens. openNLPNER.release_classifier(); // release memory for
	 * classifier log.info ("Memory status after releasing classifier: " +
	 * Util.getMemoryStats()); packIndex();
	 *
	 * return; }
	 */


	private Analyzer newAnalyzer() {
        // we can use LimitTokenCountAnalyzer to limit the #tokens

        EnglishAnalyzer stemmingAnalyzer = new EnglishAnalyzer(MUSE_STOP_WORDS_SET);
        EnglishNumberAnalyzer snAnalyzer = new EnglishNumberAnalyzer(MUSE_STOP_WORDS_SET);

        // these are the 3 fields for stemming, everything else uses StandardAnalyzer
        Map<String, Analyzer> map = new LinkedHashMap<>();
        map.put("body", snAnalyzer);
        map.put("title", snAnalyzer);
        map.put("body_original", stemmingAnalyzer);

		KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
		StandardAnalyzer standardAnalyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);

		/////////// For supporting non-stemmed version-------/////
		/*
		//do not remove any stop words.
		map.put("body", standardAnalyzer);
		map.put("title", standardAnalyzer);
		map.put("body_original", standardAnalyzer);
		*/
		////////////////////////////////////////////////////////////////

        // actually these do not need any real analyzer, they are just stored opaquely
        map.put("docId", keywordAnalyzer);
        map.put("names_offsets", keywordAnalyzer);
        //body redacted contains only  names and a lot of dots, hence it requires special handling.
//        if(ModeConfig.isPublicMode()) {
//            map.put("body", new Analyzer() {
//                @Override
//                protected TokenStreamComponents createComponents(final String fieldName,
//                                                                 final Reader reader) {
//                    Version matchVersion = Indexer.LUCENE_VERSION;
//                    final CICTokenizer source = new StandardNumberTokenizer(matchVersion, reader);
//                    TokenStream result = new LowerCaseFilter(matchVersion, source);
//                    return new TokenStreamComponents(source, result);
//                }
//            });
//        }


		return new PerFieldAnalyzerWrapper(standardAnalyzer, map);
	}

	// set create = false to append to existing index.
	private IndexWriter openIndexWriter(Directory dir) throws IOException
	{
		//IndexWriterConfig config = new IndexWriterConfig(MUSE_LUCENE_VERSION, null);
		//IndexWriter writer = new IndexWriter(dir, null, IndexWriter.MaxFieldLength.UNLIMITED);
		IndexWriterConfig iwc = new IndexWriterConfig( analyzer);
		iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		return new IndexWriter(dir, iwc); // , new IndexWriter.MaxFieldLength(250000));
	}

	void setBaseDir(String baseDir)
	{
		if (this.baseDir != null && !this.baseDir.equals(baseDir)) {
			close();
		}
		this.baseDir = baseDir;
	}

	/**
	 * sets up directory
	 *
	 * @throws IOException
	 */
	private synchronized void setupDirectory() throws IOException
	{
		directory = initializeDirectory(directory, INDEX_NAME_EMAILS);
		directory_blob = initializeDirectory(directory_blob, INDEX_NAME_ATTACHMENTS);

		if (analyzer == null)
			analyzer = newAnalyzer();
	}

	/**
	 * sets up indexer just for reading... if needed for writing only, call
	 * setupForWrite. if need both read & write, call both.
	 */
	synchronized void setupForRead()
	{
		log.info("setting up index for read only access");
		long startTime = System.currentTimeMillis();

		//closeHandles();
		try {
			setupDirectory();

			String[] defaultSearchFields, defaultSearchFieldsOriginal;
			String[] defaultSearchFieldSubject = new String[] { "title" }; // for subject only search
			String[] defaultSearchFieldCorrespondents;
			String[] defaultSearchFieldEntities = new String[] {"names","en_names_title"};//For searching in the entity field of the document.
            //body field should be there, as the content of the attachment lies in this field, should also include meta field?
            //why the search over en-names and en-names-original when body/body_original is included in the search fields?
            defaultSearchFields = new String[] { "body", "title", "to_names", "from_names", "cc_names", "bcc_names", "to_emails", "from_emails", "cc_emails", "bcc_emails" };
            defaultSearchFieldsOriginal = new String[] { "body_original", "title" }; // we want to leave title there because we want to always hit the title -- discussed with Peter June 27 2015
            defaultSearchFieldCorrespondents = new String[] { "to_names", "from_names", "cc_names", "bcc_names", "to_emails", "from_emails", "cc_emails", "bcc_emails" };
            // names field added above after email discussion with Sit 6/11/2013. problem is that we're not using the Lucene EnglishPossessiveFilter, so
            // openNLPNER will extract the name Stanford University in a sentence like:
            // "This is Stanford University's website."
            // but when the user clicks on the name "Stanford University" in say monthly cards, we
            // will not match the message with this sentence because of the apostrophe.

			//for searching an attchment with fileName
			String[] metaSearchFields = new String[]{"fileName"};
			// Parse a simple query that searches for "text":
			if (parser == null) {
				//parser = new QueryParser(MUSE_LUCENE_VERSION, defaultSearchField, analyzer);
				parser = new MultiFieldQueryParser(defaultSearchFields, analyzer);
				//parserEntityFields = new MultiFieldQueryParser(defaultSearchFieldEntities);
				parserOriginal = new MultiFieldQueryParser( defaultSearchFieldsOriginal, analyzer);
				parserSubject = new MultiFieldQueryParser(defaultSearchFieldSubject, analyzer);
				parserCorrespondents = new MultiFieldQueryParser(defaultSearchFieldCorrespondents, analyzer);
				parserMeta = new MultiFieldQueryParser(metaSearchFields, new KeywordAnalyzer());
			}

			/**
			 * Bunch of gotchas here
			 * Its a bad idea to store lucene internal docIds, as no assumptions about the internal docIds should be made;
			 * not even that they are serial. When searching, lucene may ignore logically deleted docs.
			 * Lucene does not handle deleted docs, and having these docs in search may bring down the search performance by 50%
			 * Deleted docs are cleaned only during merging of indices.*/
            int numContentDocs = 0, numContentDeletedDocs = 0, numAttachmentDocs = 0, numAttachmentDeletedDocs = 0;
			if (DirectoryReader.indexExists(directory)) {
				DirectoryReader ireader = DirectoryReader.open(directory);
				if(ireader.numDeletedDocs()>0)
                    log.warn ("!!!!!!!\nIndex reader has " + ireader.numDocs() + " doc(s) of which " + ireader.numDeletedDocs() + " are deleted)\n!!!!!!!!!!");
				isearcher = new IndexSearcher(ireader);
				contentDocIds = new LinkedHashMap<>();
                numContentDocs = ireader.numDocs();
                numContentDeletedDocs = ireader.numDeletedDocs();

                Bits liveDocs = MultiFields.getLiveDocs(ireader);
                Set<String> fieldsToLoad = new HashSet<>();
                fieldsToLoad.add("docId");
                /*for(int i=0;i<ireader.maxDoc();i++){
					org.apache.lucene.document.Document doc = ireader.document(i,fieldsToLoad);
                    if(liveDocs!=null && !liveDocs.get(i))
                        continue;

                    if(doc == null || doc.get("docId") == null)
						continue;
					contentDocIds.put(i, doc.get("docId"));
				}*/
				log.info("Loaded: "+contentDocIds.size()+" content docs");
			}

			if (DirectoryReader.indexExists(directory_blob))
			{
				IndexReader ireader_blob = DirectoryReader.open(directory_blob);
				isearcher_blob = new IndexSearcher(ireader_blob); // read-only=true
				blobDocIds = new LinkedHashMap<>();

                numAttachmentDocs = ireader_blob.numDocs();
                numAttachmentDeletedDocs = ireader_blob.numDeletedDocs();

                Bits liveDocs = MultiFields.getLiveDocs(ireader_blob);
                Set<String> fieldsToLoad = new HashSet<>();
				fieldsToLoad.add("docId");
				/*for(int i=0;i<ireader_blob.maxDoc();i++){
					org.apache.lucene.document.Document doc = ireader_blob.document(i,fieldsToLoad);
                    if(liveDocs!=null && !liveDocs.get(i))
                        continue;

					if(doc == null || doc.get("docId") == null)
						continue;
					blobDocIds.put(i,doc.get("docId"));
                }*/
				log.info("Loaded: "+blobDocIds.size()+" attachment docs");
            }

            log.warn("Number of content docs: "+numContentDocs+", number deleted: "+numContentDeletedDocs);
            log.warn("Number of attachment docs: " + numAttachmentDocs + ", number deleted: " + numAttachmentDeletedDocs);

			if (dirNameToDocIdMap == null)
				dirNameToDocIdMap = new LinkedHashMap<>();
		} catch (Exception e) {
			Util.print_exception(e, log);
		}
		log.info ("Setting up index for read took " + (System.currentTimeMillis() - startTime) + " ms");
	}

	int nDocsInIndex()
	{
		return iwriter.maxDoc();
	}

	/**
	 * preferably use this method to open an existing index.... set it up only
	 * for writing. if need read also, call setupForRead too.
	 * but packIndex MUST be called to close the writer, otherwise lucene will
	 * throw write lock errors
	 */
	void setupForWrite() throws IOException
	{
		log.info("setting up index for write access");
		setupDirectory();

		if (iwriter == null)
			iwriter = openIndexWriter(directory);
		if (iwriter_blob == null)
			iwriter_blob = openIndexWriter(directory_blob);

		// invalidate content/blobs -> docIdMap map as Lucene doc ID may change after writes (?)
		dirNameToDocIdMap = new LinkedHashMap<>();
	}

	public void commitAfterAddingDocs() throws IOException {
		iwriter.commit();
		iwriter.close();
		iwriter = null;
		iwriter_blob.commit();
		iwriter_blob.close();
		iwriter_blob=null;
		isearcher = null;
		isearcher_blob = null;
	}
	private synchronized Directory initializeDirectory(Directory dir, String name) throws IOException
	{
		if (dir == null)
			dir = createDirectory(name);
/*
@TODO This was commented out when porting to lucene 7.2 because no such API was present. Look carefully if that
is what we want.
		if (dir.getLockFactory() == null)
			dir.setLockFactory(new SingleInstanceLockFactory());
*/
		return dir;
	}

// 2022-09-13
        @Override
        protected void finalize() {
            try {
                close();
            } catch (Exception e) {
                Util.print_exception("Indexer.finalize.close", e, log);
            }
        }

	/*
	 * properly close index writers.
	 * also null out directories that may not be serializable
	 * (e.g., RAMDirectory is, but FSDirectory isn't)
	 */
	void close()
	{
		log.info("Closing indexer handles");
		//closeHandles();
		analyzer = null;
		parser = parserOriginal = parserSubject = parserCorrespondents = null;
		try {
			if (isearcher != null)
				isearcher.getIndexReader().close();
		} catch (IOException e) {
			Util.print_exception(e, log);
		}
		isearcher = null;
		try {
			if (isearcher_blob != null)
				isearcher_blob.getIndexReader().close();
		} catch (IOException e) {
			Util.print_exception(e, log);
		}
		isearcher_blob = null;
		try {
			if (iwriter != null)
				iwriter.close();
		} catch (Exception e) {
			Util.print_exception(e, log);
		}
		iwriter = null;
		try {
			if (iwriter_blob != null)
				iwriter_blob.close();
		} catch (Exception e) {
			Util.print_exception(e, log);
		}
		iwriter_blob = null;

		if (directory != null && !(directory instanceof Serializable)) {
			try {
				directory.close();
			} catch (Exception e) {
				Util.print_exception(e, log);
			}
			try {
				directory_blob.close();
			} catch (Exception e) {
				Util.print_exception(e, log);
			}
			directory = null;
			directory_blob = null;
		}
	}

	/***
	 * Deletes and cleans up index docs and residual files and directories
	 * Unsure why this hasn't existed until now, but using primarily for cleaning up unit tests
	 * Explicitly closing each level to make sure basePath directory isn't shared with something else
	 * @throws IOException
	 */
	public void deleteAndCleanupFiles() throws IOException {
		if (iwriter == null || iwriter_blob == null) {
			setupForWrite();
		}
		analyzer.close();
		iwriter.deleteAll();
		iwriter.commit();
		iwriter_blob.deleteAll();
		iwriter_blob.commit();
		Arrays.stream(directory.listAll()).forEach(f -> {
			try {
				directory.deleteFile(f);
			} catch (Exception e) {
				System.out.println("Unable to delete" + f);
			}
		});
		Arrays.stream(directory_blob.listAll()).forEach(f -> {
			try {
				directory_blob.deleteFile(f);
			} catch (Exception e) {
				System.out.println("Unable to delete" + f);
			}
		});
		close();
		File baseDirFile = new File(baseDir);
		File indexesFile = new File(baseDir + File.separator + INDEX_BASE_DIR_NAME);
		File attachmentFile = new File(baseDir + File.separator + INDEX_BASE_DIR_NAME + File.separator + INDEX_NAME_ATTACHMENTS);
		File emailsFile = new File(baseDir + File.separator + INDEX_BASE_DIR_NAME + File.separator + INDEX_NAME_EMAILS);

		emailsFile.delete();
		attachmentFile.delete();
		indexesFile.delete();
		baseDirFile.delete();
	}

	private Map<String, EmailDocument> getDocMap() {
		return docIdToEmailDoc;
	}

    // quote chars (> ) interfere with name recognition, so remove them from the text.
	// however be careful that the char positions don't change since openNLPNER returns us offsets into the string.
	private static String prepareFullBodyForNameExtraction(String text)
	{
		// first search & replace the longer strings
		text = text.replaceAll("\n> > ", "     ");
		text = text.replaceAll("\n> ", "   ");
		return text;
	}

	/**
	 * returns all names from text. also adds fields of the name category to the
	 * doc, with the value being strings of the category, separated by
	 * NAMES_FIELD_DELIMITER.
	 * e.g. name category could be "person", with value
	 * "Monica<NAMES_FIELD_DELIMITER>David"
	 */
	private Set<String> setNameFieldsOpenNLP(String text, org.apache.lucene.document.Document doc)
	{
		text = prepareFullBodyForNameExtraction(text);
		Pair<Map<String, List<String>>, List<Triple<String, Integer, Integer>>> mapAndOffsets = edu.stanford.muse.index.openNLPNER.categoryToNamesMap(text);

		List<Triple<String, Integer, Integer>> offsets = mapAndOffsets.second;
		storeNameOffsets(doc, offsets);

		Map<String, List<String>> categoryToNamesMap = mapAndOffsets.first;
		// we'll get people, places, orgs in this map

		Set<String> allNames = new LinkedHashSet<>();
		for (String category : categoryToNamesMap.keySet())
		{
			Set<String> namesForThisCategory = Util.scrubNames(categoryToNamesMap.get(category));
			String s = Util.join(namesForThisCategory, NAMES_FIELD_DELIMITER);
			doc.add(new Field(category.toLowerCase(), s, full_ft));
			allNames.addAll(namesForThisCategory);
		}

		return allNames;
	}

	private void storeNameOffsets(org.apache.lucene.document.Document doc, List<Triple<String, Integer, Integer>> offsets)
	{
		try {
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bs);
			oos.writeObject(offsets);
			oos.close();
			bs.close();
			doc.add(new Field("names_offsets", bs.toByteArray(), storeOnly_ft));
		} catch (IOException e) {
			log.warn("Failed to serialize names_offsets");
			e.printStackTrace();
		}
	}


	private void storeHeaders(org.apache.lucene.document.Document doc, List<Header> headers)
	{
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		List<String> headerString = new ArrayList<>();
		for (Header h : headers)
		{
			headerString.add(h.getName() + ": " + h.getValue());
			if (h.getName().equals("Message-ID"))
				doc.add(new Field("Message-ID", h.getValue(), storeOnly_ft));
		}
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bs);
			oos.writeObject(headerString);
			oos.close();
			bs.close();

			doc.add(new Field("headers_original", bs.toByteArray(), storeOnly_ft));
		} catch (IOException e) {
			log.warn("Failed to serialize headers");
			e.printStackTrace();
		}

		java.io.ObjectInputStream ois;
		try {
			ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bs.toByteArray()));
			java.util.List<String> sp = (java.util.List<String>) ois.readObject();


		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void storeTextHtmlPart(org.apache.lucene.document.Document doc, String textHtmlPart)
	{
		doc.add(new Field("text_html_part", textHtmlPart, storeOnly_ft));
	}

	/**
	 * internal method, core method for adding a doc to the index.
	 * adds body, title, people, places, orgs. assumes the index, iwriter etc
	 * are already set up and open.
	 * does openNLPNER and returns the list of all names
	 *
	 * @param stats
	 */
	private synchronized void add1DocToIndex(String title, String content, List<Header> headerList, Document d, IndexStats stats, String textHtmlPart) throws Exception
	{

		org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document(); // not to be confused with edu.stanford.muse.index.Document

		String id = d.getUniqueId();
		//		softAssertDocIdNotPresent(id);

		// imp: for id, should use Field.Index.NOT_ANALYZED field should be http://vuknikolic.wordpress.com/2011/01/03/lucenes-field-options-store-and-index-aka-rtfm/
		doc.add(new Field("docId", id, ft));

		// we'll store all languages detected in the doc as a field in the index
		Set<String> languages = Languages.getAllLanguages(content);
		d.languages = languages;
		String lang_str = Util.join(languages, LANGUAGE_FIELD_DELIMITER);
		doc.add(new Field("languages", lang_str, ft));

		doc.add(new Field("title", title, full_ft));

		// extract names from both title (weighted by subjectWeight) and body. put a fullstop after title to help openNLPNER.
		StringBuilder effectiveSubject = new StringBuilder(); // put body first so that the extracted openNLPNER offsets can be used without further adjustment at a later phase.
		for (int i = 0; i < io.subjectWeight; i++)
		{
			effectiveSubject.append(". ");
			effectiveSubject.append(title);
		}

		// there are multiple body fields:
		// body_raw is the original as-is text, that will be used for rendering the message
		if (d instanceof EmailDocument)
		{
			EmailDocument ed = (EmailDocument) d;

			// Store the to, from, cc, bcc fields as string
			String to_emails = Util.join(EmailUtils.emailAddrs(ed.to), " ");
			doc.add(new Field("to_emails", to_emails, full_ft));

			String to_names = Util.join(EmailUtils.personalNames(ed.to), " ");
			doc.add(new Field("to_names", to_names, full_ft));

			String from_emails = Util.join(EmailUtils.emailAddrs(ed.from), " ");
			doc.add(new Field("from_emails", from_emails, full_ft));

			String from_names = Util.join(EmailUtils.personalNames(ed.from), " ");
			doc.add(new Field("from_names", from_names, full_ft));

			String cc_emails = Util.join(EmailUtils.emailAddrs(ed.cc), " ");
			doc.add(new Field("cc_emails", cc_emails, full_ft));

			String cc_names = Util.join(EmailUtils.personalNames(ed.cc), " ");
			doc.add(new Field("cc_names", cc_names, full_ft));

			String bcc_emails = Util.join(EmailUtils.emailAddrs(ed.bcc), " ");
			doc.add(new Field("bcc_emails", bcc_emails, full_ft));

			String bcc_names = Util.join(EmailUtils.personalNames(ed.bcc), " ");
			doc.add(new Field("bcc_names", bcc_names, full_ft));

			//IMP: Field body sometimes wrongly include the attachment binary resulting in large size of this term. This further results in throwing an
			//exception when adding this document in the index. Therefore truncate the size of body upto 32766.
			int len = content.length()>32766?32766:content.length();
			content = content.substring(0,len);
			if(len!=content.length()){
			    //mark dataerror to record this truncation.@TODO
            }

			storeHeaders(doc, headerList);
			storeTextHtmlPart(doc, textHtmlPart);
			// Index original headers. Store as full_ft.
			//doc.add(new Field("headers_original", header, full_ft)); // save raw before preprocessed
			doc.add(new Field("body", content, full_ft)); // save raw before preprocessed
		}

		// the body field is what is used for searching. almost the same as body_raw, but in some cases, we expect
		// body != body_raw, e.g. if we have special logic to strip signatures, boilerplate text etc.
		// at the moment, body == body_raw, so we don't bother to store it.
		// doc.add(new Field("body", body, Field.Store.YES, Field.Index.ANALYZED));

		// body original is the original content in the message. (i.e. non-quoted, non-forwarded, etc.)
		String bodyOriginal = EmailUtils.getOriginalContent(content);
		doc.add(new Field("body_original", bodyOriginal, full_ft));
		int originalTextLength = bodyOriginal.length();
		Set<String> namesOriginal;

		int ns = 0;

		if(edu.stanford.muse.Config.OPENNLP_NER) {
            String textForNameExtraction = content + ". " + effectiveSubject; // Sit says put body first so
			// that the extracted openNLPNER offsets can be used without further adjustment for epadd redaction
            Set<String> allNames = setNameFieldsOpenNLP(textForNameExtraction, doc);

			String names = Util.join(allNames, NAMES_FIELD_DELIMITER);

			// should the names/names_orig filed be analyzed? We don't want them
			// stemmed, after all. However, we do want to match case-insensitive
			// NAMES_FIELD_DELIMITER is just some connector for storing the field
			doc.add(new Field("names", names, full_ft));

			// just reuse names for the common case of body = bodyOriginal
			if (bodyOriginal.equals(content))
				namesOriginal = allNames;
			else {
				String originalTextForNameExtraction = bodyOriginal + ". " + effectiveSubject;
				namesOriginal = Archive. extractNamesOpenNLP(originalTextForNameExtraction);
			}
			if(stats!=null) {
				stats.nNames += allNames.size();
				stats.nOriginalNames += namesOriginal.size();
			}
			ns = allNames.size();

			String namesOriginalString = Util.join(namesOriginal, NAMES_FIELD_DELIMITER);
			doc.add(new Field("names_original", namesOriginalString, full_ft));
		}

		try {
			iwriter.addDocument(doc);
		}catch(IllegalArgumentException exception){
			exception.printStackTrace(); //There were few instances where add Document failed. This catch is to detect those cases which will otherwise ripple to ner recognition phase
			//because the document will not be found in lucene index if addDocument failed.

		}
		// why not maintain lucene doc id map instead of email doc id -> doc
		// how to get lucene doc id?
		docIdToEmailDoc.put(id, (EmailDocument) d);
		if (stats != null) {
			stats.nDocuments++;
			stats.indexedTextSize += title.length() + content.length();
			stats.indexedTextSizeOriginal += originalTextLength;
		}
	}

	void updateDocument(org.apache.lucene.document.Document doc) {
		try {
			iwriter.updateDocument(new Term("docId", doc.get("docId")), doc);
		} catch (Exception e) {
			//e.printStackTrace();
			Util.print_exception("Exception while updating document: " + doc.get("docId"), e, log);
		}
	}

	/* note sync. because we want only one writer to be going at it, at one time */
	synchronized void indexSubdoc(String title, String documentText, List<Header> headers, edu.stanford.muse.index.Document d, BlobStore blobStore, String textHtmlPart)
	{
		if (d == null)
			return;

		if (documentText == null)
			documentText = "";

		try {
			add1DocToIndex(title, documentText, headers, d, stats, textHtmlPart);
			if (blobStore != null && d instanceof EmailDocument && io.indexAttachments)
				indexAttachments((EmailDocument) d, blobStore, null, stats);
		} catch (Throwable e) {
  			Util.print_exception(e, log);
			// also catch Errors. Sometimes, we might have a ClassNotFoundError (which is not covered by Exception)
			if (e instanceof OutOfMemoryError) {
				Util.print_exception("Out of memory error!", e, log);
				throw ((OutOfMemoryError) e);
			}
		}

		if (d instanceof EmailDocument)
			if (d.links != null)
				links.addAll(d.links);
	}

	private List<org.apache.lucene.document.Document> getAllDocs(Boolean attachmentType) throws IOException
	{
		List<org.apache.lucene.document.Document> result = new ArrayList<>();

		// read all the docs in the leaf readers. omit deleted docs.
		DirectoryReader r;
		if(attachmentType)
			r = DirectoryReader.open(directory_blob);
		else
			r = DirectoryReader.open(directory);

		List<LeafReaderContext> atomicReaderContexts = r.leaves();
		for (LeafReaderContext atomicReaderContext : atomicReaderContexts)
		{
			LeafReader atomicReader = atomicReaderContext.reader();
			Bits bits = atomicReader.getLiveDocs();
			for (int i = 0; i < atomicReader.maxDoc(); i++)
				if (bits!=null && bits.get(i))
					result.add(r.document(i));
				else if(bits == null)
					result.add(r.document(i));
		}
		r.close();
		return result;
	}

    public List<org.apache.lucene.document.Document> getAllDocsWithFields(Boolean attachmentType) throws IOException
    {
        List<org.apache.lucene.document.Document> result = new ArrayList<>();

        // read all the docs in the leaf readers. omit deleted docs.
        DirectoryReader r;
        if(attachmentType)
            r = DirectoryReader.open(directory_blob);
        else
            r = DirectoryReader.open(directory);

        Bits liveDocs = MultiFields.getLiveDocs(r);
//IMP: The fields of email index and attachment index are different. We need to extract appropriate field otherwis we will be missing some fields
        //after exporting the archive index.

        String fieldsArray[] = null;
        if(attachmentType)
            fieldsArray = new String[]{"body","meta","fileName","docId","emailDocId","languages"};
        else
            fieldsArray = new String[]{"body","headers_original","text_html_part", "body_original","docId","title","to_emails","from_emails","cc_emails","bcc_emails","to_names","from_names",
                    "cc_names","bcc_names","languages","names","names_original","en_names_title"};

        Set<String> fieldsToLoad = new HashSet<String>();

        for(String s: fieldsArray){
            fieldsToLoad.add(s);
        }

        for(int i=0;i<r.maxDoc();i++){
            org.apache.lucene.document.Document doc = r.document(i,fieldsToLoad);
            if(liveDocs!=null && !liveDocs.get(i))
                continue;

            if(doc == null || doc.get("docId") == null)
                continue;
            result.add(doc);
            //contentDocIds.put(i, doc.get("docId"));
        }
        r.close();
        return result;
    }

    public Map<String,Span[]> getAllEntitiesInDocs(int maxdocs) throws IOException
	{
        Map<String,Span[]> result = new ConcurrentHashMap<>();
		Query query = new MatchAllDocsQuery();
		TopDocs hits = null;
		try {
			hits = isearcher.search(query, Integer.max(Config.MAX_DOCS_PER_QUERY, maxdocs));
		}
		catch (Exception e)
		{
			log.error("Error in getAllEntitiesInDocs: " + e);
			return result;
		}
        ScoreDoc[] scoreDocs = hits.scoreDocs;

        //Tried to use parallel streams for speedup.
        Arrays.asList(scoreDocs).parallelStream().forEach(hit ->{
            int ldocId = hit.doc; // this is the lucene doc id, we need to map it to our doc id.

            String docId; // this will be our doc id
            org.apache.lucene.document.Document ldoc = null;
            try {
                ldoc = isearcher.doc(ldocId);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            //Now get emaildoc id stored in this doc.
            if(ldoc == null || ldoc.get("docId") == null)
                return;
            docId = ldoc.get("docId");
            //Now get entities stored in fields NER.NAMES and NER.NAMES_TITLE
            String val = ldoc.get(NER.NAMES);
			if (val == null)
			{
				val = "";
			}
				String[] plainSpans = val.split(Indexer.NAMES_FIELD_DELIMITER);
				// remove kill phrases here itself

				Span[] bodyentities = Arrays.stream(plainSpans).map(Span::parse).filter(s -> s != null /*&& !KillPhrases.isKillPhrase(s.getText())*/).toArray(Span[]::new);

				val = ldoc.get(NER.NAMES_TITLE);
			if (val == null)
			{
				val = "";
			}
				//Split the value and convert to span and then add to result map.

				plainSpans = val.split(Indexer.NAMES_FIELD_DELIMITER);

				Span[] titleentities = Arrays.stream(plainSpans).map(Span::parse).filter(s -> s != null /*&& !KillPhrases.isKillPhrase(s.getText())*/).toArray(Span[]::new);

				Span[] allspans = ArrayUtils.addAll(bodyentities, titleentities);

				result.put(docId, allspans);
        });

		return result;
	}


	public EmailDocument docForId(String id) {
		return docIdToEmailDoc.get(id);
	}

    /**
     * @returns an empty set if the none of the docs are instance of EmailDocument*/
	Collection<EmailDocument> convertToED(Collection<Document> docs) {
        if(docs == null)
            return null;
        Set<EmailDocument> eds = new LinkedHashSet<>();
        for(Document doc: docs) {
            if(doc instanceof EmailDocument)
                eds.add((EmailDocument) doc);
        }
        return eds;
    }

	Collection<edu.stanford.muse.index.Document> docsForQuery(String term, QueryOptions options)
    {
        log.info("Options for docs selection: "+term+", "+options.getEndDate()+","+options.getSortBy()+", "+options.getStartDate());
        QueryType qt = options.getQueryType();
        int cluster = options.getCluster();
        int threshold = options.getThreshold();
		//doesn't need term for preset regex

		Set<edu.stanford.muse.index.Document> docs_in_cluster = null;
		if (cluster != -1)
			docs_in_cluster = new LinkedHashSet<>((Collection) getDocsInCluster(cluster));

        //should retain the lucene returned order
		List<edu.stanford.muse.index.Document> result = new ArrayList<>();
		try {
            //System.err.println("Looking up:"+term);
            long st = System.currentTimeMillis();
			Collection<String> hitDocIds = lookupAsDocIds(term, threshold, isearcher, qt);
            //System.err.println("took: "+(System.currentTimeMillis()-st)+"ms and found: "+hitDocIds.size());
			log.info("Looking up term: "+term +" took: "+(System.currentTimeMillis()-st));
            st = System.currentTimeMillis();
            for (String d : hitDocIds)
			{
				EmailDocument ed = docIdToEmailDoc.get(d);
				if (ed != null) {
					if (cluster == -1 || docs_in_cluster.contains(ed))
						// if for a specific cluster, obtain only the docs in the cluster
						result.add(ed);
				} else {
					log.warn("Index hit for doc id " + d + " but doc does not exist!");
				}
			}
            //System.err.println("Iterating over the docs took: "+(System.currentTimeMillis()-st));
		} catch (Exception e) {
			Util.print_exception(e);
		}


        //sort
        SortBy sb = options.getSortBy();
        if(sb == null || sb == SortBy.CHRONOLOGICAL_ORDER)
            Collections.sort(result);
        else if(sb == SortBy.RECENT_FIRST) {
            Collections.sort(result);
            Collections.reverse(result);
        }
        //already in the relevance order

		return result;
	}

	Set<Blob> blobsForQuery(String term)
	{
		Set<Blob> result = new LinkedHashSet<>();

		if (Util.nullOrEmpty(term))
			return result;

		try {
			Collection<String> hitDocIds = lookupAsDocIds(term, 1, isearcher_blob, QueryType.FULL);
			for (String d : hitDocIds)
			{
				Blob b = attachmentDocIdToBlob.get(d);
				if (b != null) {
					result.add(b);
				} else {
					log.warn("Index hit for blob id " + d + " but blob does not exist in map!");
				}
			}
		} catch (Exception e) {
			Util.print_exception(e);
		}

		return result;
	}

    int countHitsForQuery(String q, QueryType qt) {
    	/*
		if (Util.nullOrEmpty(q)) {
            if(qt!=QueryType.PRESET_REGEX)
                return 0;
            //either we have to count the number of hits for every query in the preset file and take union to find the total number of hits or this
            QueryOptions options = new QueryOptions();
            options.setQueryType(qt);
            Collection<Document> docs = docsForQuery(q, options);
            return docs.size();
        }
        */
		try {
			QueryParser parserToUse;
			switch (qt) {
				case ORIGINAL:
					parserToUse = parserOriginal;
					break;
				case SUBJECT:
					parserToUse = parserSubject;
					break;
				case CORRESPONDENTS:
					parserToUse = parserCorrespondents;
					break;
				default:
					parserToUse = this.parser;
			}
			Query query = parserToUse.parse(q);
			return (int) isearcher.search(query, Config.MAX_DOCS_PER_QUERY,Sort.RELEVANCE).totalHits;
		} catch (Exception e) {
			Util.print_exception(e);
		}

		return 0;
	}

	private List<String> getNamesForLuceneDoc(org.apache.lucene.document.Document doc, QueryType qt)
	{
		List<String> result = new ArrayList<>();
		if (doc == null)
		{
			log.warn("trying to get names from null doc");
			return result;
		}
		// ...and read its names
		String names = (qt == QueryType.ORIGINAL) ? doc.get("names_original") : doc.get("names");
		if (!Util.nullOrEmpty(names)) {
			StringTokenizer names_st = new StringTokenizer(names, Indexer.NAMES_FIELD_DELIMITER);
			while (names_st.hasMoreTokens())
				result.add(names_st.nextToken());
		}
		return result;
	}

    private Collection<String> luceneLookupAsDocIds (String q, int threshold, IndexSearcher searcher, QueryType qt) throws IOException, ParseException {
        Pair<Collection<String>,Integer> p = luceneLookupAsDocIdsWithTotalHits(q, threshold, searcher, qt, Config.MAX_DOCS_PER_QUERY);
        return p.first;
    }

    List<String> getNamesForDocId(String id, Indexer.QueryType qt) throws IOException{
        // get this doc from the index first...
        org.apache.lucene.document.Document docForThisId = getLDoc(id);
        return getNamesForLuceneDoc(docForThisId, qt);
    }

    protected Integer getNumHits(String q, boolean isAttachments, QueryType qt) throws IOException, ParseException {
        Pair<Collection<String>,Integer> p;
        if (!isAttachments)
            p = luceneLookupAsDocIdsWithTotalHits(q, 1, isearcher, qt, 1);
        else
            p = luceneLookupAsDocIdsWithTotalHits(q, 1, isearcher_blob, qt, 1);
        return p.second;
    }


    /*
    Moves edoc from src to dest index. Here src is source indexer and the current object (Indexer) is where
    the doc is going to be moved.
    The associated attachments also need to be moved from srcBlobStore to destBlobStore.
    Don't forget to call pack()[BlobStore] after calling this method for every new document that has been added.
     */
    public void moveDocAndAttachmentsToThisIndex(Indexer srcindexer, EmailDocument edoc, BlobStore srcBlobStore, BlobStore destBlobStore) throws IOException {
		//prepare writers if not done by the caller
		if (iwriter == null)
			iwriter = openIndexWriter(directory);
		if (iwriter_blob == null) {
			//if (directory_blob == null) directory_blob = initializeDirectory(directory_blob, INDEX_NAME_ATTACHMENTS); // should already be valid
			iwriter_blob = openIndexWriter(directory_blob);
		}
		//get lucenedocid of doc wrt src Indexer first without attachment then with attachment.
		org.apache.lucene.document.Document dsrc = srcindexer.getLDoc(edoc.getUniqueId());
		if(dsrc==null)
			return;//This should not happen.. @TODO debug it if it happens.
		LinkedList<org.apache.lucene.document.Document> dattachments = new LinkedList<>();
		//create a new lucenedoc for adding to iwriter.
		org.apache.lucene.document.Document newdoc = new org.apache.lucene.document.Document();

		//copy fields from dsrc to newdoc
		dsrc.getFields().forEach(newdoc::add);
		//add to map docIDtoEmailDoc
		docIdToEmailDoc.put(edoc.getUniqueId(), edoc);

		for(Blob b: edoc.attachments){
		///NOTE: For now we are adding blob irrespective of whether it was present in destblobstore or not.
				//get blobid for this blob
			String id = Integer.toString(srcBlobStore.index(b));
			//get lucendoc for this docid.
			org.apache.lucene.document.Document attachmentsrc = srcindexer.getLDocAttachment(id);
			if(attachmentsrc==null)
				return;//This should not happen.. @TODO debug it if it happens.
			//create new doc and add to list of dattachments (for adding it to index at the end atomically)
			org.apache.lucene.document.Document newattachmentdoc = new org.apache.lucene.document.Document();
			dattachments.add(newattachmentdoc);

			//copy all fields from attachmentsrc to newattachmentdoc except docid that will be the number
			//obtained after adding blob to destblobstore and then getting it's index.
			attachmentsrc.getFields().stream().forEach(field->{
				if(!"docId".equals(field.name())){
					newattachmentdoc.add(field);
				}
			});
			String urlstring = srcBlobStore.get_URL_Normalized(b);
			URL url = new URL(urlstring);
			destBlobStore.add(b,url.openStream());
			String newid = Integer.toString(destBlobStore.index(b));
			newattachmentdoc.add(new Field("docId", newid, ft));
			attachmentDocIdToBlob.put(newid,b);
		}

		//add newdoc to iwriter and all docattachments present in dattachments list to iwriter_blob.
		iwriter.addDocument(newdoc);
		for(org.apache.lucene.document.Document d: dattachments){
			iwriter_blob.addDocument(d);
		}


	}


	/**
	 * returns collection of docIds of the Lucene docs that hit, at least
	 * threshold times.
	 * warning! only looks up body field, no others
     * Caution: This code is not to be touched, unless something is being optimised
     * Introducing something here can seriously affect the search times.
	 */
	private Pair<Collection<String>,Integer> luceneLookupAsDocIdsWithTotalHits(String q, int threshold, IndexSearcher searcher, QueryType qt, int lt) throws IOException, ParseException {
		Collection<String> result = new ArrayList<>();

		//	String escaped_q = escapeRegex(q); // to mimic built-in regex support
		//TODO: There should also be a general query type that takes any query with field param, i.e. without parser
		Query query;
		if (qt == QueryType.ORIGINAL)
			query = parserOriginal.parse(q);
		else if (qt == QueryType.SUBJECT)
			query = parserSubject.parse(q);
		else if (qt == QueryType.CORRESPONDENTS)
			query = parserCorrespondents.parse(q);
		else if (qt == QueryType.REGEX)
		{

			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			/**
			 * Note: this is not a spanning (i.e. doesn't search over more than
			 * one token) regexp, for spanning regexp use: body_unanlyzed and
			 * title_unanlyzed fields instead
			 */
			Query query1 = new RegexpQuery(new Term("body", q), RegExp.ALL);
			Query query2 = new RegexpQuery(new Term("title", q), RegExp.ALL);
			builder.add(query1,org.apache.lucene.search.BooleanClause.Occur.SHOULD);
			builder.add(query2, org.apache.lucene.search.BooleanClause.Occur.SHOULD);
			query = builder.build();
		} else /* if (qt == QueryType.PRESET_REGEX) {
			query = new BooleanQuery();
			if(presetQueries != null) {
				for (String pq : presetQueries) {
					Query query1 = new RegexpQuery(new Term("body", pq), RegExp.ALL);
					Query query2 = new RegexpQuery(new Term("title", pq), RegExp.ALL);
					((BooleanQuery) query).add(query1, org.apache.lucene.search.BooleanClause.Occur.SHOULD);
					((BooleanQuery) query).add(query2, org.apache.lucene.search.BooleanClause.Occur.SHOULD);
				}
				log.info("Doing a preset regex search");
			}else{
				log.warn("Preset queries is not initialised");
			}
		} else */ if (qt == QueryType.META) {
            query = parserMeta.parse(q);
		} else
			query = parser.parse(q);

		//		query = convertRegex(query);
        long st = System.currentTimeMillis();
		int totalHits = 0;
		ScoreDoc[] hits = null;
		if(query!=null) {
			TopDocs tds = searcher.search(query,  lt, Sort.RELEVANCE);
			log.info("Took: " + (System.currentTimeMillis() - st) + "ms for query:" + query);
			hits = tds.scoreDocs;
			totalHits = (int)tds.totalHits;
		}else{
			log.error("Query is null!!");
		}
		// this logging causes a 50% overhead on the query -- maybe enable it only for debugging
		// log.info (hits.length + " hits for query " + Util.ellipsize(q, 30) + " => " + Util.ellipsize(escaped_q, 30) + " = " + Util.ellipsize(query.toString(), 30) + " :");

		// Iterate through the results:

		// TODO: not very pretty code here to determine dir_name which selects the cache to use
		Util.softAssert(searcher == isearcher || searcher == isearcher_blob,log);
		String dir_name = searcher == isearcher ? INDEX_NAME_EMAILS : INDEX_NAME_ATTACHMENTS;

		Map<Integer, String> map = dirNameToDocIdMap.get(dir_name);
		if (map == null) {
			map = new LinkedHashMap<>();
			dirNameToDocIdMap.put(dir_name, map);
			log.info("Adding new entry for dir name to docIdMap");
		} else {
			log.info("Existing entry for dir name to docIdMap");
		}

		int n_added = 0;
		log.info("Found: " + hits.length + " hits for query: " + q);
		for (ScoreDoc hit : hits) {
			int ldocId = hit.doc; // this is the lucene doc id, we need to map it to our doc id.

			String docId; // this will be our doc id
			org.apache.lucene.document.Document ldoc = searcher.doc(ldocId);
			docId = ldoc.get("docId");
			// try to use the new fieldcache id's
			// if this works, we can get rid of the dirNameToDocIdMap
			/*try {
				docId = (searcher == isearcher) ? contentDocIds.get(ldocId) : blobDocIds.get(ldocId);
			} catch (Exception e) {
				Util.print_exception(e, log);
				continue;
			}*/

			if (threshold <= 1) {
				// common case: threshold is 1.
				result.add(docId);
				n_added++;
			} else {
				// more expensive, do it only if threshold is > 1
				Explanation expl = searcher.explain(query, hit.doc);
				Explanation[] details = expl.getDetails();
				// NB: a catch here is that details.length doesn't reflect the actual # of hits for the query.
				// sometimes, for a single hit, there are 2 entries, a ComplexExplanation and an Explanation.
				// not sure why, but is somewhat corroborated by the code:
				// http://massapi.com/class/ex/Explanation.html
				// showing a single hit creating both a C.E and an E.
				// a more robust approach might be to look for the summary to end with product of: , sum of: etc.
				// e.g. http://www.gossamer-threads.com/lists/lucene/java-dev/49706
				// but for now, we'll count only the number of ComplexExplanation and check if its above threshold
				//				log.info("doc id " + hits[i].toString() + " #details = " + details.length);

				// HORRIBLE HACK! - because we don't know a better way to find the threshold
				outer:
				for (Explanation detail : details) {
					// log.info(detail.getClass().getName());

//					if (detail instanceof ComplexExplanation) {
//						ComplexExplanation ce = (ComplexExplanation) detail;
						String s = detail.toString();
						int total_tf = 0;
						while (true) {
							int idx = s.indexOf("tf(termFreq(");
							if (idx < 0)
								break outer;
							s = s.substring(idx);
							idx = s.indexOf("=");
							if (idx < 0)
								break outer;
							s = s.substring(idx + 1);
							int idx1 = s.indexOf(")");
							if (idx < 0)
								break outer;
							String num_str = s.substring(0, idx1);
							int num = 0;
							try {
								num = Integer.parseInt(num_str);
							} catch (Exception e) {
								log.warn("ERROR parsing complex expl: " + num_str);
							}
							total_tf += num;
							if (total_tf >= threshold) {
								result.add(docId);
								n_added++;
								break outer;
							}
						}
					}
				}
			}
		//}
        log.info(n_added + " docs added to docIdMap cache");
		return new Pair<>(result, totalHits);
	}

	/** returns collection of docId's that hit, at least threshold times */
	private Collection<String> lookupAsDocIds(String q, int threshold, IndexSearcher searcher, QueryType qt) throws IOException, ParseException {
		// get as documents, then convert to ids
		return luceneLookupAsDocIds(q, threshold, searcher, qt);
	}

	/** returns collection of EmailDocs that hit */
	protected Set<EmailDocument> lookupDocs(String q, QueryType qt) throws IOException, ParseException {
		Collection<String> docIds = luceneLookupAsDocIds(q, 1, isearcher, qt);
		Set<EmailDocument> result = new LinkedHashSet<>();
		for (String docId : docIds)
		{
			EmailDocument ed = docIdToEmailDoc.get(docId);
			if (ed != null)
			{
				//				if (ed.languages == null)
				//				{
				//					// extract languages for the doc from the index and store it in the document, so we don't have to compute it again
				//					String lang = getLuceneDoc(docId).getFieldable("languages").stringValue();
				//					StringTokenizer st = new StringTokenizer(lang, ",");
				//					Set<String> langs = new LinkedHashSet<String>();
				//					while (st.hasMoreTokens())
				//						langs.add(st.nextToken());
				//					if (langs.size() == 0) // shouldn't happen, just defensive
				//						langs.add("english");
				//					ed.languages = langs;
				//				}
				result.add(ed);
			}
			else
				log.warn("Inconsistent Index! docId " + docId + " is a hit in the index, but is not in the doc map!");
		}
		return result;
	}

    //@return pair of content of the attachment and status of retrieval
	Pair<String,String> getContentsOfAttachment(String fileName){
		try {
            fileName = "\""+fileName+"\"";
			Collection<String> docIds = luceneLookupAsDocIds(fileName, 1, isearcher_blob, QueryType.META);
			if(docIds == null) {
				log.error("lookup for " + fileName + " returned null");
				return new Pair<>(null, "Lookup failed!");
			}
			if(docIds.size()>1)
				log.warn("More than one attachment with the same attachment name: "+fileName);
			if(docIds.size() == 0) {
				log.warn("No docIds found for fileName: " + fileName);
                return new Pair<>(null, "No attachment found with that name");
			}

			String docId = docIds.iterator().next();
			org.apache.lucene.document.Document ldoc = getLDocAttachment(docId);
			if(ldoc == null) {
				log.error("Cannot find lucene doc for docId: "+docId);
				return new Pair<>(null, "Internal error! Cannot find lucene doc for docId: " + docId);
            } else {
                log.info("Found doc for attachment: "+fileName);
			}
			return new Pair<>(ldoc.get("body"), "success");
		}catch(Exception e){
			log.info("Exception while trying to fetch content of "+fileName, e);
			return new Pair<>(null, e.getMessage());
		}
	}

	// since we may need to rebuild the index in a new directory, the analyzer needs to have been initialized apriori
	private synchronized Directory copyDirectoryExcludeFields(boolean attachmentType, String out_basedir, String out_name, String... fields_to_be_removed) throws IOException
	{
		List<org.apache.lucene.document.Document> allDocsLive = getAllDocsWithFields(attachmentType);
		//IndexReader reader = DirectoryReader.open(dir); // IndexReader.open(dir, true); // read-only=true

		Directory newDir = createDirectory(out_basedir, out_name);
		IndexWriter writer = openIndexWriter(newDir);
		//log.info("Removing field(s) " + Util.join(fields_to_be_removed, ", ") + " from index.");

		for(org.apache.lucene.document.Document doc: allDocsLive){
			for (String field : fields_to_be_removed)
				doc.removeFields(field);
			writer.addDocument(doc);
		}
		/*for (int i = 0; i < reader.maxDoc(); i++) {
			org.apache.lucene.document.Document doc = reader.document(i);

		}*/

		writer.close();
		//reader.close();

		return newDir;
	}

	// since we may need to rebuild the index in a new directory, the analyzer needs to have been initialized apriori
	private synchronized Directory copyDirectoryWithDocFilter(boolean attachmentType, String out_basedir, String out_name, FilterFunctor filter_func) throws IOException
	{
		long startTime = System.currentTimeMillis();
		Directory newDir = createDirectory(out_basedir, out_name);
		IndexWriter writer = openIndexWriter(newDir);
		List<org.apache.lucene.document.Document> result = new ArrayList<>();

		// read all the docs in the leaf readers. omit deleted docs.
		DirectoryReader r;
		if(attachmentType)
			r = DirectoryReader.open(directory_blob);
		else
			r = DirectoryReader.open(directory);

		Bits liveDocs = MultiFields.getLiveDocs(r);
//IMP: The fields of email index and attachment index are different. We need to extract appropriate field otherwis we will be missing some fields
		//after exporting the archive index.

		String fieldsArray[] = null;
		if(attachmentType)
			fieldsArray = new String[]{"body","meta","fileName","docId","emailDocId","languages"};
		else
			fieldsArray = new String[]{"body","headers_original","text_html_part", "body_original","docId","title","to_emails","from_emails","cc_emails","bcc_emails","to_names","from_names",
					"cc_names","bcc_names","languages","names","names_original","en_names_title"};

		Set<String> fieldsToLoad = new HashSet<String>();

		for(String s: fieldsArray){
			fieldsToLoad.add(s);
		}
		//https://stackoverflow.com/questions/2311845/is-it-possible-to-iterate-through-documents-stored-in-lucene-index
		int count = 0;
		for(int i=0;i<r.maxDoc();i++){
			org.apache.lucene.document.Document doc = r.document(i,fieldsToLoad);
			if(liveDocs!=null && !liveDocs.get(i))
				continue;

			if(doc == null || doc.get("docId") == null)
				continue;
			if (filter_func == null || filter_func.filter(doc))
			{
				writer.addDocument(doc);
				count++;
			}
			//contentDocIds.put(i, doc.get("docId"));
		}
		r.close();
		writer.close();

		/*List<org.apache.lucene.document.Document> alldocsLive = getAllDocsWithFields(attachmentType);
//		IndexReader reader = DirectoryReader.open(dir); // IndexReader.open(dir, true); // read-only=true


		//log.info("Removing field(s) " + Util.join(fields_to_be_removed, ", ") + " from index.");


		for(org.apache.lucene.document.Document doc: alldocsLive){
			if (filter_func == null || filter_func.filter(doc))
			{
				writer.addDocument(doc);
				count++;
			}
		}
		for (int i = 0; i < reader.numDocs(); i++) {
		for (int i = 0; i < reader.maxDoc(); i++) {

			org.apache.lucene.document.Document doc = reader.document(i);
			//add only if it is not deleted...

		}

		writer.close();
		//reader.close();
		*/
		log.info ("CopyDirectoryWithtDocFilter to dir:" + out_basedir + " name: " + baseDir + " time: " + (System.currentTimeMillis() - startTime) + " ms docs: " + count);
		return newDir;
	}

    private synchronized Directory removeFieldsFromDirectory(Directory dir, boolean attachmentType, String... fields_to_be_removed) throws IOException
	{
		if (!indexHasFields())
			return dir;

		boolean is_file_based = dir instanceof FSDirectory;

		String tmp_name = ".tmp";
		FSDirectory fsdir = null;
		if (is_file_based) {
			fsdir = (FSDirectory) dir;
			tmp_name = fsdir.getDirectory().getFileName() + tmp_name;
		}

		Directory newDir = copyDirectoryExcludeFields(attachmentType, baseDir, tmp_name, fields_to_be_removed);

		if (is_file_based) {
			// delete the original dir and rename tmp
			File org_file = fsdir.getDirectory().toFile();
			File tmp_file = ((FSDirectory) newDir).getDirectory().toFile();
            FileUtils.deleteDirectory(org_file);
            boolean res = tmp_file.renameTo(org_file);
		    if(!res)
                log.warn("Rename of "+tmp_file.getName()+" failed, things may not work as expected!!");
        }
        return fsdir;
	}

	// since we may need to rebuild the index in a new directory, the analyzer needs to have been initialized apriori
	private synchronized void removeFieldsFromDirectory(String... fields_to_be_removed) throws IOException
	{
		//directory = removeFieldsFromDirectory(directory, fields_to_be_removed);
		//updated the signature so that now along with directory attachment type (false for email index and true for attachment index) is passed
		//this additional variable is used for subsequent method call in removeFieldsFromDirectory method.
		directory = removeFieldsFromDirectory(directory,false, fields_to_be_removed);
		directory_blob = removeFieldsFromDirectory(directory_blob,true, fields_to_be_removed);
		//directory_blob = removeFieldsFromDirectory(directory_blob, fields_to_be_removed);

	}

	//Method not used till v5 but if used in future, it should be updated (signature) along the lines of previous method.
	private synchronized void copyDirectoryExcludeFields(String out_dir, String... fields_to_be_removed) throws IOException
	{
		//directory = copyDirectoryExcludeFields(directory, out_dir, INDEX_NAME_EMAILS, fields_to_be_removed);
		//directory_blob = copyDirectoryExcludeFields(directory_blob, out_dir, INDEX_NAME_ATTACHMENTS, fields_to_be_removed);
		directory = copyDirectoryExcludeFields(false, out_dir, INDEX_NAME_EMAILS, fields_to_be_removed);
		directory_blob = copyDirectoryExcludeFields(true, out_dir, INDEX_NAME_ATTACHMENTS, fields_to_be_removed);
	}

	synchronized void copyDirectoryWithDocFilter(String out_dir, FilterFunctor emailFilter, FilterFunctor attachmentFilter) throws IOException
	{
		//directory = copyDirectoryWithDocFilter(directory, out_dir, INDEX_NAME_EMAILS, emailFilter);
		Directory tmpdirecotry = copyDirectoryWithDocFilter(false, out_dir, INDEX_NAME_EMAILS, emailFilter);
        //the docIds of the attachment docs are not the same as email docs, hence the same filter won't work.
        //by supplying a null filter, we are not filtering attachments at all, is this the right thing to do? Because this may retain attachment doc(s) corresponding to a removed email doc
		//directory_blob = copyDirectoryWithDocFilter(directory_blob, out_dir, INDEX_NAME_ATTACHMENTS, attachmentFilter);
		Directory tmpdirecotry_blob = copyDirectoryWithDocFilter(true, out_dir, INDEX_NAME_ATTACHMENTS, attachmentFilter);

		directory = tmpdirecotry;
		directory_blob = tmpdirecotry_blob;
	}

	// CAUTION: permanently change the index!
	private synchronized int removeEmailDocs(Collection<? extends edu.stanford.muse.index.Document> docs) throws IOException
	{
		if (iwriter != null) {
			throw new IOException("iwriter is not null. prepareForSerialization() should be called first.");
		}

		if (isearcher != null) {
			isearcher.getIndexReader().close();
			isearcher = null;
		}

		stats = null; // stats no longer valid

		int count = docIdToEmailDoc.size();

		IndexWriterConfig cfg = new IndexWriterConfig( analyzer);
		IndexWriter writer = new IndexWriter(directory, cfg);
		//IndexWriter writer = new IndexWriter(directory, analyzer, false, new IndexWriter.MaxFieldLength(250000));
		assert (writer.numDocs() == docIdToEmailDoc.size());

		for (edu.stanford.muse.index.Document d : docs) {
			String id = d.getUniqueId();
			EmailDocument ed = docIdToEmailDoc.get(id);
			assert (d == ed);
			docIdToEmailDoc.remove(id);
			writer.deleteDocuments(new TermQuery(new Term("docId", id)));
			log.info("Removed doc " + id + " from index");
		}

		writer.commit();

		assert (writer.numDocs() == docIdToEmailDoc.size());

		writer.close();

		count -= docIdToEmailDoc.size(); // number of removed docs
		assert (count == docs.size());
		return count;
	}

	private synchronized void rollbackWrites() throws IOException
	{
		if (iwriter != null) {
			iwriter.rollback();
			iwriter = null;
		}
		if (iwriter_blob != null) {
			iwriter_blob.rollback();
			iwriter_blob = null;
		}
	}

	org.apache.lucene.document.Document getDoc(edu.stanford.muse.index.Document d) throws IOException {
		return getLDoc(d.getUniqueId());
	}

	private void softAssertDocIdNotPresent(String docId) throws IOException
	{
		TermQuery q = new TermQuery(new Term("docId", docId));
		TopDocs td = isearcher.search(q, 1); // there must be only 1 doc with this id anyway
        Util.softAssert(td.totalHits == 0, "Oboy! docId: " + docId + " already present in the index, don't try to add it again!",log);
	}

	// look up the doc from doc id assigned to it
	// Use this method only if docID exist and you want to get the corresponding lucene doc.
	private org.apache.lucene.document.Document getLDoc(String docId, Boolean attachment, Set<String> fieldsToLoad) throws IOException
	{
		IndexSearcher searcher;
		if(!attachment) {
			if (isearcher == null) {
				DirectoryReader ireader = DirectoryReader.open(directory);
				isearcher = new IndexSearcher(ireader);
			}
			searcher = isearcher;
		}
		else {
			if (isearcher_blob == null) {
				DirectoryReader ireader = DirectoryReader.open(directory_blob);
				isearcher_blob = new IndexSearcher(ireader);
			}
			searcher = isearcher_blob;
		}

		TermQuery q = new TermQuery(new Term("docId", docId));
		TopDocs td = searcher.search(q, 1); // there must be only 1 doc with this id anyway
		Util.softAssert(td.totalHits <= 1, "docId = " + docId + " is not unique. Found: "+td.totalHits+" hits!, Signature is " + EmailDocument.uniqueIdToSignature.get(docId)+"----\n",log);
		ScoreDoc[] sd = td.scoreDocs;
		if (sd.length != 1)
		{
			// something went wrong... report it and ignore this doc
			Util.warnIf(true, "lookup failed for id " + docId +": " + sd.length + " documents found for this id, Signature is " + EmailDocument.uniqueIdToSignature.get(docId)+"---\n",log);
			return null;
		}

		org.apache.lucene.document.Document resultdoc;
        if(fieldsToLoad!=null) {
			resultdoc = searcher.doc(sd[0].doc, fieldsToLoad);
			if(resultdoc==null) {
				log.warn("Lucene could not find the document for docid = " + docId + "and field = "+fieldsToLoad.toString());
			}
        }
        else{
			resultdoc = searcher.doc(sd[0].doc);
			if(resultdoc==null) {
				log.warn("Lucene could not find the document for docid = " + docId);
			}
		}
		return  resultdoc;
	}

	private org.apache.lucene.document.Document getLDocAttachment(String docId) throws IOException{
		return getLDoc(docId, true, null);
	}

	org.apache.lucene.document.Document getLDoc(String docId) throws IOException{
		return getLDoc(docId, false, null);
	}

    private org.apache.lucene.document.Document getLDocAttachment(String docId, Set<String> fieldsToLoad) throws IOException{
        return getLDoc(docId, true, fieldsToLoad);
    }

    org.apache.lucene.document.Document getLDoc(String docId, Set<String> fieldsToLoad) throws IOException{
        return getLDoc(docId, false, fieldsToLoad);
    }

	String getContents(edu.stanford.muse.index.Document d, boolean originalContentOnly)
	{
		org.apache.lucene.document.Document doc;
		try {
			doc = getDoc(d);
		} catch (IOException e) {
			log.warn("Unable to obtain document " + d.getUniqueId() + " from index");
			e.printStackTrace();
			return null;
		}

		return getContents(doc, originalContentOnly);
	}

    String getTitle(org.apache.lucene.document.Document doc) {
        if(doc == null)
            return null;
        return doc.get("title");
    }

	String getPreservedContents(edu.stanford.muse.index.Document d)
	{
		org.apache.lucene.document.Document doc;
		try {
			doc = getDoc(d);
		} catch (IOException e) {
			log.warn("Unable to obtain document " + d.getUniqueId() + " from index");
			e.printStackTrace();
			return null;
		}

		return getPreservedContents(doc);
	}

	String getPreservedContents(org.apache.lucene.document.Document doc) {
		String contents;
		try {
			JSPHelper.doLogging("getPreservedContents: get body-preserved");
			contents = doc.get("body-preserved");

			if (contents == null) {
				contents = doc.get("body");
				JSPHelper.doLogging("getPreservedContents: get body!!!");
			}

		} catch (Exception e) {
			log.warn("Exception " + e + " trying to read field 'body_preserved/body': " + Util.ellipsize(Util.stackTrace(e), 350));
			//@TODO
			Util.print_exception("Exception Trying to read field body_preserved/body",e,log);
			contents = null;
		}

		return contents;
	}


    //@TODO
	String getContents(org.apache.lucene.document.Document doc, boolean originalContentOnly) {
        String contents;
        try {
            if (originalContentOnly)
                contents = doc.get("body_original");
            else
                contents = doc.get("body");
        } catch (Exception e) {
            log.warn("Exception " + e + " trying to read field 'body/body_original': " + Util.ellipsize(Util.stackTrace(e), 350));
            //@TODO
			Util.print_exception("Exception Trying to read field body/body_original",e,log);
            contents = null;
        }


        if (contents == null) { // fall back to 'names' (or in public mode)
            try {
                List<String> names = getNamesForLuceneDoc(doc, originalContentOnly ? QueryType.ORIGINAL : QueryType.FULL);
                contents = Util.joinSort(Util.scrubNames(names), "\n"); // it seems <br> will get automatically added downstream
            } catch (Exception e) {
                log.warn("Exception " + e + " trying to read extracted names of " + this.toString());
                contents = null;
            }

            if (contents == null)
                contents = "\n\nContents not available.\n\n";
        }

        return contents;
    }
	public String getTextHtml(edu.stanford.muse.index.Document d) {
		org.apache.lucene.document.Document doc;
		try {
			doc = getDoc(d);
		} catch (IOException e) {
			log.warn("Unable to obtain document " + d.getUniqueId() + " from index");
			e.printStackTrace();
			return "";
		}
		return doc.get("text_html_part");
	}

	public List<String> getOriginalHeaders(edu.stanford.muse.index.Document d)
	{
		org.apache.lucene.document.Document doc;
		java.util.List<String> headerList = new ArrayList<String>();
		try {
			doc = getDoc(d);
		} catch (IOException e) {
			log.warn("Unable to obtain document " + d.getUniqueId() + " from index");
			e.printStackTrace();
			return headerList;
		}
		BytesRef bin1ref = doc.getBinaryValue("headers_original");
		if (bin1ref == null)
		{
			//No headers_original in Lucene. Maybe the original Mbox file has
			//been created with an old version of ePADD?
			return new ArrayList<String>();
		}
		byte[] bin1bytes = bin1ref.bytes;

		java.io.ObjectInputStream ois;
		try {
			ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bin1bytes));
			headerList = (java.util.List<String>) ois.readObject();
		} catch (ClassNotFoundException | IOException e) {
			log.warn("Unable to get original headers for " + d.getUniqueId());
			e.printStackTrace();
			return headerList;
		}
		return headerList;
	}

	String getHeaders(edu.stanford.muse.index.Document d)
	{
		org.apache.lucene.document.Document doc;
		try {
			doc = getDoc(d);
		} catch (IOException e) {
			log.warn("Unable to obtain document " + d.getUniqueId() + " from index");
			e.printStackTrace();
			return null;
		}

		return getHeaders(doc);
	}

	String getHeaders(org.apache.lucene.document.Document doc) {
        String headers;
        try {
			headers = doc.get("headers_original");
        } catch (Exception e) {
            log.warn("Exception " + e + " trying to read field 'headers_original': " + Util.ellipsize(Util.stackTrace(e), 350));
			Util.print_exception("Exception Trying to read field headers_original",e,log);
            headers = null;
        }


        if (headers == null) { // fall back to 'names' (or in public mode)
            try {
                List<String> names = getNamesForLuceneDoc(doc, QueryType.ORIGINAL);
                headers = Util.joinSort(Util.scrubNames(names), "\n"); // it seems <br> will get automatically added downstream
            } catch (Exception e) {
                log.warn("Exception " + e + " trying to read extracted names of " + this.toString());
                headers = null;
            }

            if (headers == null)
                headers = "\n\nHeaders not available.\n\n";
        }

        return headers;
    }

    protected org.apache.lucene.document.Document getLDoc(Integer ldocId, Set<String> fieldsToLoad) {
        try {
            if (isearcher == null) {
                DirectoryReader ireader = DirectoryReader.open(directory);
                isearcher = new IndexSearcher(ireader);
            }
            return isearcher.doc(ldocId, fieldsToLoad);
        } catch(IOException e){
            Util.print_exception(e, log);
            return null;
        }
    }

	public static void main(String args[]) {
		//		String content = "A FEDERAL JUDGE DECLINED on Monday to grant to the University of South Florida a declaratory judgment that the university's plan"
		//				+ " to fire Sami Al-Arian, a tenured professor with alleged links to terrorism, did not violate his First Amendment right to free speech";
		//		Set<String> pnames = getNamesFromPatterns(content, true);
		//		Set<String> names = getNamesFromPatterns(content, false);
		//		System.err.println("Pnames: " + pnames);
		//		System.err.println("Names: " + names);
		//System.err.println(Indexer.getNamesFromPatterns("There is a story in today's New Zealand Herald about a German and French peace-keeping force that would work as UN Peacekeepers, presumably to protect IRAQ after the US invasion.... Mark", false));
		//		EmailUtils.test("NEA and the Massachusetts Cultural Council",
		//				"Dartmouth College and Harvard Law School",
		//				"Cornell University and Boston College",
		//				"The National Park Service",
		//				"Adams President Natural Resources Defense Council",
		//				"Tougher Disguises Press",
		//				"The Chronicle of Higher Education for Tuesday",
		//				"Electronic Poetry Center",
		//				"CIA",
		//				"CNN",
		//				"FBI",
		//				"Meridian Books",
		//				"Apple",
		//				"House Press",
		//				"Microsoft");
		//		EmailUtils.test("China", "NYState", "Orlando", "Elmwood", "Mykonos", "Raliegh",
		//				"Dresden", "Northampton", "IRAQ", "New Zealand", "English in Dresden");
		//testQueries();
		//		try {
		//			String aFile = System.getProperty("user.home") + File.separator + "epadd-appraisal" + File.separator + "user";
		//			String type = "3classes";
		//
		//			String BASE_DIR = System.getProperty("user.home") + File.separator + "epadd-appraisal" + File.separator + "user" + File.separator + "models" + File.separator;
		//			String path = BASE_DIR + File.separator + type + "_" + edu.stanford.muse.ModeConfig.SVM_MODEL_FILE;
		//			String wfsPath = BASE_DIR + File.separator + type + "_" + edu.stanford.muse.ModeConfig.WORD_FEATURES;
		//			Archive archive = SimpleSessions.readArchiveIfPresent(aFile);
		//			WordFeatures.trainCustomModel(archive, path, wfsPath);
		//		} catch (Exception e) {
		//			e.printStackTrace();
		//		}
        String userDir = System.getProperty("user.home") + File.separator + "epadd-appraisal" + File.separator + "user";
        Archive archive = ArchiveReaderWriter.readArchiveIfPresent(userDir);
        String queries[] = new String[]{"(teaching|learning|research|university|college|school|education|fellowship|professor|graduate)",
                                        "(book|chapter|article|draft|submission|review|festschrift|poetry|prose|writing)",
                                        "(award|prize|medal|fellowship|certificate)",
                                        "(father|dad|dada|daddy|papa|pappa|pop|\"old man\"|mother|mama|mamma|mom|momma|mommy|mammy|mum|mummy|\"father in law\"|\"mother in law\"|\"brother in law\"|\"sister in law\"|stepfather|stepmother|husband|wife|son|boy|daughter|girl|sister|brother|cousin|grandfather|grandmother|grandson|granddaughter|genealogy|reunion)"
        };
        for(String q: queries){
            long st = System.currentTimeMillis();
            Collection<Document> docs = archive.indexer.docsForQuery(q,new QueryOptions());
            long et = System.currentTimeMillis();
            System.err.println("Took: "+(et-st)+"ms to search for: "+q+"\nFound: #"+docs.size());
        }
	}

	public String toString()
	{
		return computeStats();
	}
}
