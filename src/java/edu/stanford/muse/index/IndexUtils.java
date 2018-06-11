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
package edu.stanford.muse.index;

import edu.stanford.muse.AnnotationManager.AnnotationManager;
import edu.stanford.muse.datacache.Blob;
import edu.stanford.muse.datacache.BlobStore;
import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.email.CalendarUtil;
import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.LabelManager.Label;
import edu.stanford.muse.LabelManager.LabelManager;
import edu.stanford.muse.util.*;
import edu.stanford.muse.webapp.JSPHelper;
import edu.stanford.muse.webapp.ModeConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** useful utilities for indexing */
public class IndexUtils {
	private static Log	log	= LogFactory.getLog(IndexUtils.class);

	/** temporary method */
	public static boolean query(String s, String query)
	{
		List<String> tokens = Util.tokenize(query, "|");
		s = s.toLowerCase();
		for (String t : tokens)
		{
			t = t.trim();
			if (Util.nullOrEmpty(t))
				continue;
			if (s.contains(t))
				return true;
		}
		return false;
	}

	/** replaces all tokens in the given text that are not in any of the entities in the given doc.
     * all other tokens are replaced with REDACTION_CHAR.
     * token is defined as a consecutive sequence of letters or digits
     * Note: all other characters (incl. punctuation, special symbols) are blindly copied through
     * anything not captured in a token is considered non-sensitive and is passed through
     */
    public static String retainOnlyNames(String text, org.apache.lucene.document.Document doc) {
        StringBuilder result = new StringBuilder();
        Set<String> allowedTokens = new LinkedHashSet<>();

        // assemble all the allowed tokens (lower cased) from these 3 types of entities
        {
            List<String> allEntities = Arrays.asList(Archive.getAllNamesInLuceneDoc(doc,true)).stream().map(Span::getText).collect(Collectors.toList());

            for (String e : allEntities)
                allowedTokens.addAll(Util.tokenize(e.toLowerCase()));
            // names may sometimes still have punctuation; strip it. e.g. a name like "Rep. Duncan" should lead to the tokens "rep" and "duncan"
            allowedTokens = allowedTokens.stream().map(Util::stripPunctuation).collect(Collectors.toSet());
        }

        final char REDACTION_CHAR = '.';
        int idx = 0;

		boolean previousTokenAllowed = false;

        outer:
        while (true) {
            StringBuilder token = new StringBuilder();

            // go through all the chars one by one, either passing them through or assembling them in a token that can be looked up in allowedTokens

            {
                // skip until start of next token, passing through chars to result
                // the letter pointed to by idx has not yet been processed
                while (true) {
                    if (idx >= text.length())
                        break outer;

                    char ch = text.charAt(idx++);
                    if (Character.isLetter(ch) || Character.isDigit(ch)) {  // if other chars are judged sensitive in the future, this condition should be updated
                        token.append(ch);
                        break;
                    } else
                        result.append(ch);
                }
            }

            Character ch;
            {
                // now, idx is just past the start of a token (with the first letter stored in token),
                // keep reading letters until we find a non-letter, adding it to the token
                // the letter pointed to by idx has not yet been processed
                while (true) {
                    ch = null;
                    if (idx >= text.length())
                        break; // only break out of inner loop here, not the outer. this might be the last token, and token may have some residual content, so it has to be processed
                    ch = text.charAt(idx++);
                    if (!Character.isLetter(ch) && !Character.isDigit(ch))
                        break;

                    token.append(ch);
                }
            }
            // ch contains the first char beyond the token (if it is not null). If it is null, it means we have reached the end of the string


            // look up the token and allow it only if allowedTokens contains it
            // use lower case token for comparison, but when appending to result, use the original string with the original case
			// worried about "A" grade, we should disallow it although it could easily be a token in a name somewhere

			String lowerCaseToken = token.toString().toLowerCase(); // ctoken = canonicalized token
            boolean allowToken = allowedTokens.contains(lowerCaseToken);

			// however, if this token is a stop word, only allow if previous token was allowed because we don't want to start from a stop word.
            // note: this will still allow the stop word if it is at the beginning of a sentence, and the prev. sentence ended in an allowed token
			if (allowToken && DictUtils.isJoinWord(lowerCaseToken))
				allowToken = previousTokenAllowed;

			if (allowToken)
                result.append(token);
            else
                for (int j = 0; j < token.length(); j++)
                    result.append(REDACTION_CHAR);

			previousTokenAllowed = allowToken;

            if (ch != null)
                result.append(ch);
        }

        return result.toString();
    }

	public static class Window {
		public Date					start;
		public Date					end;
		public List<EmailDocument>	docs;

		public String toString() {
			return "[" + CalendarUtil.getDisplayMonth(start) + " - " + CalendarUtil.getDisplayMonth(end) + "), " + docs.size() + " messages";
		}
	}

	/** returns list of list of docs organized by a series of time windows */
	public static List<Window> docsBySlidingWindow(Collection<EmailDocument> allDocs, int windowSizeInMonths, int stepSizeInMonths)
	{
		List<Window> result = new ArrayList<>();
		if (allDocs.size() == 0)
			return result;

		// compute the begin and end date of the corpus
		Date first = null;
		Date last = null;
		for (EmailDocument ed : allDocs)
		{
			Date d = ed.date;
			if (d == null)
			{ // drop this ed
				log.warn("Warning: null date on email: " + ed.getHeader());
				continue;
			}
			if (first == null || d.before(first))
				first = d;
			if (last == null || d.after(last))
				last = d;
		}

		// compute the monthly intervals
		List<Pair<Date, Date>> intervals = Util.getSlidingMonthlyIntervalsBackward(first, last, windowSizeInMonths, stepSizeInMonths);

		int nIntervals = intervals.size();
		for (Pair<Date, Date> interval1 : intervals) {
			Window w = new Window();
			w.start = interval1.getFirst();
			w.end = interval1.getSecond();
			w.docs = new ArrayList<>();
			result.add(w);
		}

		// for each message, add it to all intervals it belongs to
		// can be made more efficient by first sorting allDocs by date etc
		// but may not be needed except for a very large # of intervals and
		// a large # of docs
		for (EmailDocument ed : allDocs)
		{
			Date d = ed.date;
			if (d == null)
				continue;

			// add ed to all intervals that c falls in
			for (int i = 0; i < nIntervals; i++)
			{
				Pair<Date, Date> interval = intervals.get(i);
				Date intervalStart = interval.getFirst();
				Date intervalEnd = interval.getSecond();
				if (d.equals(intervalStart) || d.equals(intervalEnd) ||
						(d.after(intervalStart) && d.before(intervalEnd)))
				{
					result.get(i).docs.add(ed);
					break;
				}
			}
		}

		return result;
	}

	public static void dumpDocument(String prefix, String bodyText) throws IOException
	{
		// dump contents
		PrintWriter pw1 = new PrintWriter(new FileOutputStream(prefix + ".txt"));
		pw1.println(bodyText);
		pw1.close();
	}

	// read all the headers
	public static List<Document> findAllDocs(String prefix) throws ClassNotFoundException, IOException
	{
		List<Document> allDocs = new ArrayList<>();

		// weird: sometimes we get a double-slash or double-backslash which kills the matching...
		// better canonicalize first, which calling new File() and then getAbsolutePath does
		prefix = new File(prefix).getAbsolutePath();
		String dir = ".";
		int x = prefix.lastIndexOf(File.separator);
		if (x >= 0)
			dir = prefix.substring(0, x);
		File dirFile = new File(dir);
		// select valid header files
		File files[] = dirFile.listFiles(new Util.MyFilenameFilter(prefix, ".header"));
		for (File f : files)
		{
			try {
				Document d = null;
				ObjectInputStream headerOIS = new ObjectInputStream(new FileInputStream(f));
				d = (Document) headerOIS.readObject();
				headerOIS.close();
				allDocs.add(d);
			} catch (Exception e) {
				Util.print_exception(e, log);
			}
		}

		System.out.println(allDocs.size() + " documents found with prefix " + prefix);
		return allDocs;
	}

	// read all the headers
	//	public static List<Document> findAllDocsInJar(String prefix) throws ClassNotFoundException, IOException
	//	{
	//		String dir = Util.dirName(prefix);
	//		String file = Util.baseName(prefix);
	//		JarDocCache cache = new JarDocCache(dir);
	//		List<Document> allDocs = new ArrayList<Document>(cache.getAllHeaders(file).values());
	//		return allDocs;
	//	}

	static String capTo2Tokens(String s)
	{
		StringTokenizer st = new StringTokenizer(s);
		int nTokens = st.countTokens();
		if (nTokens <= 2)
			return s;
		else
			return st.nextToken() + " " + st.nextToken();
	}

	/**
	 * splits the input string into words e.g.
	 * if the input is "a b c", return a list containing "a" "b" and "c"
	 * if input is "a" a list containing just "a" is returned.
	 * input must have at least one token.
	 * 
	 * @param s
	 * @return
	 */
	public static List<String> splitIntoWords(String s)
	{
		List<String> result = new ArrayList<>();
		if (s == null)
			return result;

		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens())
		{
			String term = st.nextToken();
			if (term.startsWith("\""))
			{
				term = term.substring(1); // skip the leading "
				while (st.hasMoreTokens())
				{
					term += " " + st.nextToken();
					if (term.endsWith("\""))
					{
						term = term.substring(0, term.length() - 1); // skip the trailing "
						break;
					}
				}
			}
			result.add(term);
		}

		return result;
	}

	/**
	 * splits the input string into pairs of words e.g.
	 * if the input is "a b c", return a list containing "a b" and "b c".
	 * if input is "a" a list containing just "a" is returned.
	 * if input is "a b", a list containing just "a b" is returned.
	 * input must have at least one token.
	 * 
	 * @param s
	 * @return
	 */
	public static List<String> splitIntoPairWords(String s)
	{
		List<String> result = new ArrayList<>();
		if (s == null)
			return result;

		StringTokenizer st = new StringTokenizer(s);
		if (!st.hasMoreTokens())
			return result;

		String s1 = st.nextToken();
		if (!st.hasMoreTokens())
		{
			result.add(s1);
			return result;
		}

		while (st.hasMoreTokens())
		{
			String s2 = st.nextToken();
			if (DictUtils.isJoinWord(s2))
				continue;
			result.add(s1 + " " + s2);
			s1 = s2;
		}

		return result;
	}

	/**
	 * splits the input search query string into indiv. words.
	 * e.g. a b|"c d"|e returns a list of length 4: a, b, "c d", e
	 * 
	 * @return
	 */
	public static List<String> getAllWordsInQuery(String s)
	{
		List<String> result = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(s, "|");
		while (st.hasMoreTokens())
		{
			//			StringTokenizer st1 = new StringTokenizer(st.nextToken());
			//			while (st1.hasMoreTokens())
			{
				//				String word = st1.nextToken().trim().toLowerCase(); // canonicalize and add
				String word = st.nextToken();
				word = word.trim();
				if (word.length() > 0)
					result.add(word);
			}
		}
		return result;
	}

	/**
	 * returns docs with ALL the given email/names. looks for all aliases of the
	 * person's name, not just the given one
	 */
	public static Set<Document> selectDocsByPersons(AddressBook ab, Collection<EmailDocument> docs, String[] emailOrNames)
	{
		return selectDocsByPersons(ab, docs, emailOrNames, null);
	}

	public static List<Document> selectDocsByPersonsAsList(AddressBook ab, Collection<EmailDocument> docs, String[] emailOrNames)
	{
		return new ArrayList<>(selectDocsByPersons(ab, docs, emailOrNames, null));
	}

	/**
	 * returns docs with any/all the given email/names. looks for all aliases of
	 * the person's name, not just the given one.
	 * runs through all docs times # given emailOrNames.
	 */
	private static Set<Document> selectDocsByPersons(AddressBook ab, Collection<EmailDocument> docs, String[] emailOrNames, int[] contactIds)
	{
		Set<Contact> contactSet = new LinkedHashSet<>();
		if (emailOrNames != null) {
			for (String e : emailOrNames) {
				Collection<Contact> contacts = ab.lookupByEmailOrName(e);
				if (!Util.nullOrEmpty(contacts ))
					contactSet.addAll(contacts);
				else
					log.info("Unknown email/name " + e);
			}
		}

		if (contactIds != null) {
			for (int id : contactIds) {
				Contact c = ab.getContact(id);
				if (c == null)
					log.info("Unknown contact ID " + id);
				else
					contactSet.add(c);
			}
		}

		if (contactSet.isEmpty())
			return new LinkedHashSet<>(); // return an empty list

		// consider map impl in future where we can go directly from the names to the messages
		// currently each call to selectDocsByPerson will go through all docs
		Set<Document> docsForThisPerson = IndexUtils.selectDocsByContact(ab, docs, contactSet);

		for (Document d : docsForThisPerson)
		{
			if (!d.equals(d))
				log.error("doc not itself!");
			//	if (!docsForThisPerson.contains(d))
			//		log.error ("Email doc is not good!");
			//	log.info (d);
		}

		if (log.isDebugEnabled())
		{
			StringBuilder sb = new StringBuilder();
			for (String s : emailOrNames)
				sb.append(s + " ");
			log.debug(docsForThisPerson.size() + " docs with person: [" + sb + "]");
		}

		return docsForThisPerson;
	}

	/*
	 * returns index of doc with date closest to the date startYear/startMonth/1
	 * startMonth is 0-based
	 * returns -1 if no docs or invalid start month/year
	 */
	public static int getDocIdxWithClosestDate(Collection<? extends DatedDocument> docs, int startMonth, int startYear)
	{
		if (docs.size() == 0)
			return -1;

		if (startMonth < 0 || startYear < 0)
			return -1;

		long givenDate = new GregorianCalendar(startYear, startMonth, 1).getTime().getTime();

		Long smallestDiff = Long.MAX_VALUE;
		int bestIdx = -1, idx = 0;
		for (DatedDocument d : docs)
		{
			long dateDiff = Math.abs(d.date.getTime() - givenDate);
			if (dateDiff < smallestDiff)
			{
				bestIdx = idx;
				smallestDiff = dateDiff;
			}
			idx++;
		}
		return bestIdx;
	}

	/*
	 * public static Map<String, Collection<FacetItem>>
	 * computeFacets(Collection<Document> docs, AddressBook addressBook,
	 * GroupAssigner groupAssigner, Indexer indexer)
	 * {
	 * Map<String, Collection<FacetItem>> facetMap = new LinkedHashMap<String,
	 * Collection<FacetItem>>();
	 * 
	 * // sentiments
	 * if (indexer != null)
	 * {
	 * List<FacetItem> sentimentItems = new ArrayList<FacetItem>();
	 * 
	 * // rather brute-force, compute docs for all sentiments and then
	 * intersect...
	 * // a better way might be to process the selected messages and see which
	 * sentiments they reflect
	 * for (String sentiment: Sentiments.captionToQueryMap.keySet())
	 * {
	 * String query = Sentiments.captionToQueryMap.get(sentiment);
	 * List<Document> docsForTerm = new
	 * ArrayList<Document>(indexer.docsWithPhrase(query, -1));
	 * docsForTerm.retainAll(docs);
	 * String url = "sentiment=" + sentiment;
	 * sentimentItems.add(new FacetItem(sentiment,
	 * Sentiments.captionToQueryMap.get(sentiment), docsForTerm.size(), url));
	 * }
	 * facetMap.put("Sentiments", sentimentItems);
	 * }
	 * 
	 * if (addressBook != null)
	 * {
	 * // groups
	 * if (groupAssigner != null)
	 * {
	 * Map<SimilarGroup<String>, FacetItem> groupMap = new
	 * LinkedHashMap<SimilarGroup<String>, FacetItem>();
	 * for (Document d: docs)
	 * {
	 * if (!(d instanceof EmailDocument))
	 * continue;
	 * EmailDocument ed = (EmailDocument) d;
	 * SimilarGroup<String> g = groupAssigner.getClosestGroup(ed);
	 * if (g == null)
	 * continue;
	 * FacetItem f = groupMap.get(g);
	 * if (f == null)
	 * {
	 * String url = "groupIdx=" + groupAssigner.getClosestGroupIdx(ed);
	 * groupMap.put(g, new FacetItem(g.name, g.elementsToString(), 1, url));
	 * }
	 * else
	 * f.count++;
	 * }
	 * 
	 * facetMap.put("Groups", groupMap.values());
	 * }
	 * 
	 * // people
	 * Map<Contact, FacetItem> peopleMap = new LinkedHashMap<Contact,
	 * FacetItem>();
	 * for (Document d: docs)
	 * {
	 * if (!(d instanceof EmailDocument))
	 * continue;
	 * EmailDocument ed = (EmailDocument) d;
	 * List<Contact> people = ed.getParticipatingContactsExceptOwn(addressBook);
	 * for (Contact c: people)
	 * {
	 * String s = c.pickBestName();
	 * FacetItem f = peopleMap.get(c);
	 * if (f == null)
	 * {
	 * String url = "person=" + c.canonicalEmail;
	 * peopleMap.put(c, new FacetItem(s, c.toTooltip(), 1, url));
	 * }
	 * else
	 * f.count++;
	 * }
	 * }
	 * facetMap.put("People", peopleMap.values());
	 * }
	 * 
	 * // can do time also... locations?
	 * 
	 * return facetMap;
	 * }
	 */

	public static Map<Contact, DetailedFacetItem> partitionDocsByPerson(Collection<? extends Document> docs, AddressBook ab)
	{
		Map<Contact, DetailedFacetItem> result = new LinkedHashMap<>();
		Map<Contact, Pair<String, String>> tooltip_cache = new LinkedHashMap<>();
		for (Document d : docs)
		{
			if (!(d instanceof EmailDocument))
				continue;
			EmailDocument ed = (EmailDocument) d;
			List<Contact> people = ed.getParticipatingContactsExceptOwn(ab);
			for (Contact c : people)
			{
				String s = null;
				String tooltip = null;
				Pair<String, String> p = tooltip_cache.get(c);
				if (p != null) {
					s = p.getFirst();
					tooltip = p.getSecond();
				} else {
					s = c.pickBestName();
					tooltip = c.toTooltip();
					if (ModeConfig.isPublicMode()) {
						s = Util.maskEmailDomain(s);
						tooltip = Util.maskEmailDomain(tooltip);
					}
					tooltip_cache.put(c, new Pair<>(s, tooltip));
				}
				DetailedFacetItem f = result.get(c);
				if (f == null)
				{
					//String url = "person=" + c.canonicalEmail;
					//String url = "contact=" + ab.getContactId(c);
					f = new DetailedFacetItem(s, tooltip, "contact", Integer.toString(ab.getContactId(c)));
					result.put(c, f);
				}
				f.addDoc(ed);
			}
		}
		return result;
	}

	public static Map<String, DetailedFacetItem> partitionDocsByFolder(Collection<? extends Document> docs)
	{
		Map<String, DetailedFacetItem> folderNameMap = new LinkedHashMap<>();
		for (Document d : docs)
		{
			if (!(d instanceof EmailDocument))
				continue;
			EmailDocument ed = (EmailDocument) d;
			String s = ed.folderName;
			if (s == null)
				continue;
			DetailedFacetItem f = folderNameMap.computeIfAbsent(s, s1 -> new DetailedFacetItem(Util.filePathTail(s1), s1, "folder", s1));
			f.addDoc(ed);
		}
		return folderNameMap;
	}

	private static Map<String, DetailedFacetItem> partitionDocsByDirection(Collection<? extends Document> docs, AddressBook ab)
	{
		Map<String, DetailedFacetItem> result = new LinkedHashMap<>();
		DetailedFacetItem f_in = new DetailedFacetItem("Received", "Incoming messages", "direction", "in");
		DetailedFacetItem f_out = new DetailedFacetItem("Sent", "Outgoing messages", "direction", "out");

		for (Document d : docs)
		{
			if (!(d instanceof EmailDocument))
				continue;
			EmailDocument ed = (EmailDocument) d;
			int sent_or_received = ed.sentOrReceived(ab);

			// if sent_or_received = 0 => neither received nor sent. so it must be implicitly received.
			if (sent_or_received == 0 || (sent_or_received & EmailDocument.RECEIVED_MASK) != 0)
				f_in.addDoc(ed);
			if ((sent_or_received & EmailDocument.SENT_MASK) != 0)
				f_out.addDoc(ed);
		}

		if (f_in.totalCount() > 0)
			result.put("in", f_in);
		if (f_out.totalCount() > 0)
			result.put("out", f_out);

		return result;
	}

	/** Partition documents by the presence/absence of annotation text */
	private static Map<String, DetailedFacetItem> partitionDocsByAnnotationPresence(Collection<? extends Document> docs,Archive archive) {

		Map<String, Set<Document>> tagToDocs = new LinkedHashMap<>();
		Map<String, DetailedFacetItem> result = new LinkedHashMap<>();
		Set<Document> annotatedDocs = new LinkedHashSet<>();
		Set<Document> unannotatedDocs = new LinkedHashSet<>();
		AnnotationManager annotationManager = archive.getAnnotationManager();

		for (Document d : docs) {
			if (!Util.nullOrEmpty(annotationManager.getAnnotation(d.getUniqueId())))
				annotatedDocs.add(d);
			else
				unannotatedDocs.add(d);
		}

		if (unannotatedDocs.size() > 0 ){
			result.put("notannotated",new DetailedFacetItem("Not annotated","Documents with no annotation","isannotated","false"));
			unannotatedDocs.forEach(doc->result.get("notannotated").addDoc(doc));
		}
		if (annotatedDocs.size()>0){
			result.put("annotated",new DetailedFacetItem("Annotated","Documents with annotation","isannotated","true"));
			annotatedDocs.forEach(doc->result.get("annotated").addDoc(doc));
		}

		return result;


	}

	//Does this facet make sense in appraisal mode? Because in appraisal mode there is only one accession
	//at a time.
	public static Map<String, DetailedFacetItem> partitionDocsByAccessionID(Collection<? extends Document> docs, Archive archive)
	{

		Map<String, DetailedFacetItem> result = new LinkedHashMap<>();
		if(archive.collectionMetadata.accessionMetadatas==null)
			return result;
		Map<String, DetailedFacetItem> tmpresult = new LinkedHashMap<>();//a temp result to hold accessionID
		// specific facets. Once fully constructed we will convert accessionID to accessionName
		Set<String> accessionIDs= new LinkedHashSet<>();

		archive.collectionMetadata.accessionMetadatas.forEach(am->accessionIDs.add(am.id));
		archive.collectionMetadata.accessionMetadatas.forEach(f->
				tmpresult.put(f.id,new DetailedFacetItem(f.id,
						f.notes,"accessionIDs",f.id))
		);
		//now iterate over documents and add them to appropriate facet depending upon the accessionID of that document.

		Map<String,String> docIDToAccessionID = archive.getDocIDToAccessionID();
		for (Document d : docs) {
			if (!(d instanceof EmailDocument))
				continue;
			EmailDocument ed = (EmailDocument) d;
			Set<String> labs = archive.getLabelIDs(ed);
			String accID;
			if(docIDToAccessionID.containsKey(ed.getUniqueId()))
				accID = docIDToAccessionID.get(ed.getUniqueId());
			else
				accID = archive.baseAccessionID;
			//now add document to appropriate facet item.
			//INV: tmpresult.get(accID)!=null
			if(tmpresult.get(accID)!=null)
			tmpresult.get(accID).addDoc(ed);
		}
		//converting tmpresult to result and also take care of 'no doc in a facet' case
		tmpresult.keySet().forEach(accid-> {
					if (tmpresult.get(accid)!=null && tmpresult.get(accid).totalCount() > 0)
						result.put(accid, tmpresult.get(accid));
				}
		);
		return result;
	}


	/** Partition documents by label types ******************/
	private static Map<String, DetailedFacetItem> partitionDocsByLabelTypes(Collection<? extends Document> docs, Archive archive, LabelManager.LabType type) {
		Map<String, DetailedFacetItem> result = new LinkedHashMap<>();

		Map<String, DetailedFacetItem> tmpresult = new LinkedHashMap<>();//a temp result to hold labelID specific facets. Once fully constructed we will convert labelID to labelName
		//create as many detailed facetitems as there are number of labels of that type.
		Set<Label> labels= archive.getLabelManager().getAllLabels(type);
		labels.forEach(f->
			tmpresult.put(f.getLabelID(),new DetailedFacetItem(f.getLabelName(),
					f.getDescription(),"labelIDs",f.getLabelID()))
		);
		//now iterate over documents and add them to appropriate facet depending upon the labelID of that document.

		for (Document d : docs) {
			if (!(d instanceof EmailDocument))
				continue;
			EmailDocument ed = (EmailDocument) d;
			Set<String> labs = archive.getLabelIDs(ed);
			labs.forEach(f->{
				//add to tmpresult
				//null check here ensures that we add only those docs which have at least one label of interest
				//here note that the labels of interest (of a given type) are already put in tmpresult earlier.
					if(tmpresult.get(f)!=null)
						tmpresult.get(f).addDoc(ed);
			}); //add document to the facet associated with corresponding label ID
		}
		//converting tmpresult to result and also take care of 'no doc in a facet' case
		tmpresult.keySet().forEach(labid-> {
					if (tmpresult.get(labid).totalCount() > 0)
						result.put(labid, tmpresult.get(labid));
				}
		);
		return result;
	}
	/** note: attachment types are lower-cased */
	private static Map<String, DetailedFacetItem> partitionDocsByAttachmentType(Archive archive, Collection<? extends Document> docs)
	{
		Map<String, DetailedFacetItem> result = new LinkedHashMap<>();

		for (Document d : docs)
		{
			if (!(d instanceof EmailDocument))
				continue;
			EmailDocument ed = (EmailDocument) d;
			List<Blob> attachments = ed.attachments;
			if (attachments != null)
				for (Blob b : attachments)
				{
					String ext = Util.getExtension(archive.getBlobStore().get_URL_Normalized(b));
					if (ext == null)
						ext = "none";
					ext = ext.toLowerCase();
					DetailedFacetItem dfi = result.get(ext);
					if (dfi == null)
					{
						dfi = new DetailedFacetItem(ext, ext + " attachments", "attachmentExtension", ext);
						result.put(ext, dfi);
					}
					dfi.addDoc(ed);
				}
		}
		return result;
	}

	/** version that stores actual dates instead of just counts for each facet */
	public static Map<String, Collection<DetailedFacetItem>> computeDetailedFacets(Collection<Document> docs, Archive archive)
	{
		AddressBook addressBook = archive.addressBook;

		Map<String, Collection<DetailedFacetItem>> facetMap = new LinkedHashMap<>();

		// Note: order is important here -- the facets will be displayed in the order they are inserted in facetMap
		// current order: sentiments, groups, people, direction, folders
		/* disabling sentiment facets
		if (indexer != null)
		{
			List<DetailedFacetItem> sentimentItems = new ArrayList<DetailedFacetItem>();
			Set<Document> docSet = new LinkedHashSet<Document>(docs);

			// rather brute-force, compute docs for all sentiments and then intersect...
			// a better way might be to process the selected messages and see which sentiments they reflect
			Map<String, String> captionToQueryMap;
			if (lexicon != null && !ModeConfig.isPublicMode())
				captionToQueryMap = lexicon.getCaptionToQueryMap(docs);
			else
				captionToQueryMap = new LinkedHashMap<>();

			for (String sentiment : captionToQueryMap.keySet())
			{
				String query = captionToQueryMap.get(sentiment);
                Indexer.QueryOptions options = new Indexer.QueryOptions();
                //options.setQueryType(Indexer.QueryType.ORIGINAL);
				options.setSortBy(Indexer.SortBy.RELEVANCE); // to avoid unnecessary sorting
				Collection<Document> docsForTerm = indexer.docsForQuery(query, options);
				docsForTerm.retainAll(docSet);
				sentimentItems.add(new DetailedFacetItem(sentiment, captionToQueryMap.get(sentiment), new ArrayList<Document>(docsForTerm), "sentiment", sentiment));
			}
			facetMap.put("sentiments", sentimentItems);
		}
		*/

		if (addressBook != null)
		{
			// people
			Map<Contact, DetailedFacetItem> peopleMap = partitionDocsByPerson(docs, addressBook);
			facetMap.put("correspondent", peopleMap.values());

			// direction
			Map<String, DetailedFacetItem> directionMap = partitionDocsByDirection(docs, addressBook);
			if  (directionMap.size() > 1)
				facetMap.put("direction", directionMap.values());

			/*
			--No longer need this code as restriction, reviewed etc. are handled by labels--
			// flags -- provide them only if they have at least 2 types in these docs. if all docs have the same value for a particular flag, no point showing it.
			Map<String, DetailedFacetItem> doNotTransferMap = partitionDocsByDoNotTransfer(docs);
			if  (doNotTransferMap.size() > 1)
			facetMap.put("transfer", doNotTransferMap.values());

			Map<String, DetailedFacetItem> transferWithRestrictionsMap = partitionDocsByTransferWithRestrictions(docs);
			if  (transferWithRestrictionsMap.size() > 1)
				facetMap.put("restrictions", transferWithRestrictionsMap.values());
			Map<String, DetailedFacetItem> reviewedMap = partitionDocsByReviewed(docs);
			if  (reviewedMap.size() > 1)
				facetMap.put("reviewed", reviewedMap.values());
			*/
			//facet for restriction labels
			Map<String, DetailedFacetItem> restrlabels  = partitionDocsByLabelTypes(docs,archive, LabelManager.LabType.RESTRICTION);
			facetMap.put("Restriction Labels",restrlabels.values());

			//facet for general labels
			Map<String, DetailedFacetItem> genlabels = partitionDocsByLabelTypes(docs,archive, LabelManager.LabType.GENERAL);
			facetMap.put("General Labels",genlabels.values());
			//////////////////////////////////////////////////////////////////////////////////
			//facet for accession IDs- only in modes other than appraisal
			if(!ModeConfig.isAppraisalMode()) {
				Map<String, DetailedFacetItem> accIDs = partitionDocsByAccessionID(docs, archive);
				facetMap.put("Accessions", accIDs.values());
			}

			Map<String, DetailedFacetItem> annotationPresenceMap = partitionDocsByAnnotationPresence(docs,archive);
			facetMap.put("Annotations", annotationPresenceMap.values());
			// attachments
			if (!ModeConfig.isPublicMode())
			{
				Map<String, DetailedFacetItem> attachmentTypesMap = partitionDocsByAttachmentType(archive,docs);
				facetMap.put("attachment type", attachmentTypesMap.values());
			}
		}

		if (!ModeConfig.isPublicMode())
		{
			Map<String, DetailedFacetItem> folderNameMap = partitionDocsByFolder(docs);
			if  (folderNameMap.size() > 0)
				facetMap.put("folders", folderNameMap.values());
		}

		// sort so that in each topic, the heaviest facets are first
		for (String s : facetMap.keySet())
		{
			Collection<DetailedFacetItem> detailedFacets = facetMap.get(s);
			List<DetailedFacetItem> list = new ArrayList<>(detailedFacets);
			Collections.sort(list);
			facetMap.put(s, list);
		}

		return facetMap;
	}

	private static Pair<Date, Date> getDateRange(Collection<? extends DatedDocument> docs)
	{
		Date first = null, last = null;
		for (DatedDocument dd : docs)
		{
			if (dd.date == null)
				continue; // should not happen
			if (first == null)
			{
				first = last = dd.date;
				continue;
			}
			if (dd.date.before(first))
				first = dd.date;
			if (dd.date.after(last))
				last = dd.date;
		}
		return new Pair<>(first, last);
	}

	public static String getDateRangeAsString(Collection<? extends DatedDocument> docs)
	{
		Pair<Date, Date> p = getDateRange(docs);
		Date first = p.getFirst();
		Date last = p.getSecond();

		String result = "";
		if (first == null)
			result += "??";
		else
		{
			Calendar c = new GregorianCalendar();
			c.setTime(first);
			result += CalendarUtil.getDisplayMonth(c) + " " + c.get(Calendar.DAY_OF_MONTH) + ", " + c.get(Calendar.YEAR);
		}
		result += " to ";
		if (last == null)
			result += "??";
		else
		{
			Calendar c = new GregorianCalendar();
			c.setTime(last);
			result += CalendarUtil.getDisplayMonth(c) + " " + c.get(Calendar.DAY_OF_MONTH) + ", " + c.get(Calendar.YEAR);
		}
		return result;
	}

    public static <D extends DatedDocument> List<D> selectDocsByDateRange(Collection<D> c, int year, int month)
    {
        return selectDocsByDateRange(c, year, month, -1);
    }

    // date, month is 1-based NOT 0-based
	// if month is < 0, it is ignored
	public static <D extends DatedDocument> List<D> selectDocsByDateRange(Collection<D> c, int year, int month, int date)
	{
        //Calendar date is not 0 indexed: https://docs.oracle.com/javase/7/docs/api/java/util/Calendar.html#DATE
		--month; // adjust month to be 0 based because that's what calendar gives us
		boolean invalid_month = month < 0 || month > 11;
        boolean invalid_date = date<1 || date>31;
		List<D> result = new ArrayList<>();
		for (D d : c)
		{
			Calendar cal = new GregorianCalendar();
			cal.setTime(d.date);
			int doc_year = cal.get(Calendar.YEAR);
			int doc_month = cal.get(Calendar.MONTH);
            int doc_date = cal.get(Calendar.DATE);
			if (year == doc_year && (invalid_month || month == doc_month) && (invalid_date || date == doc_date))
				result.add(d);
		}
		return result;
	}

	/*
	 * return docs in given date range (inclusive), sorted by date
	 * month is 1-based NOT 0-based. Date is 1 based.
	 * see calendarUtil.getDateRange specs for handling of the y/m/d fields.
	 * if month is < 0, it is ignored, i.e. effectively 1 for the start year and
	 * 12 for the end year
	 * returns docs with [startDate, endDate] both inclusive
	 */
	public static List<DatedDocument> selectDocsByDateRange(Collection<DatedDocument> c, int startY, int startM, int startD, int endY, int endM, int endD)
	{
		Pair<Date, Date> p = CalendarUtil.getDateRange(startY, startM - 1, startD, endY, endM - 1, endD);
		Date startDate = p.getFirst(), endDate = p.getSecond();

		List<DatedDocument> result = new ArrayList<>();
		for (DatedDocument d : c)
		{
            //we want docs with the same date (year, month, date) or after start date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			if (!startDate.after(d.date) && !endDate.before(d.date))
				result.add(d);
		}
		// Collections.sort(result);
		return result;
	}

	/**
	 * picks docs from given docs with indices from the given nums.
	 * format for each num is:
	 * // 2-10 or 14 i.e. a single page# or a comma separate range.
	 * special case: "all" selects all docs
	 */
	public static List<Document> getDocNumbers(List<Document> docs, String nums[])
	{
		List<Document> result = new ArrayList<>();

		if (docs == null || nums == null)
			return result;

		if (nums.length == 1 && "all".equalsIgnoreCase(nums[0]))
		{
			result.addAll(docs);
			return result;
		}

		// format for s is:
		// 2-10 or 14 i.e. a single page# or a comma separate range
		for (String s : nums)
		{
			int startIdx = 0, endIdx = -1;

			try {
				if (s.contains("-"))
				{ // page range
					StringTokenizer pageST = new StringTokenizer(s, "-");
					startIdx = Integer.parseInt(pageST.nextToken());
					endIdx = Integer.parseInt(pageST.nextToken());
				}
				else
					startIdx = endIdx = Integer.parseInt(s);
			} catch (Exception e) {
				JSPHelper.log.error("Bad doc# string in query: " + s);
				continue;
			}

			for (int idx = startIdx; idx <= endIdx; idx++)
			{
				if (idx < 0 || idx >= docs.size())
				{
					JSPHelper.log.error("Bad doc# " + idx + " # docs = " + docs.size() + " doc num spec = " + s);
					continue;
				}
				result.add(docs.get(idx));
			}
		}
		return result;
	}

	/**
	 * returns set of all blobs that have an attachment that ends in ANY one of
	 * the given tails
	 */
	public static Set<Blob> getBlobsForAttachments(Collection<? extends Document> docs, String[] attachmentTails, BlobStore attachmentsStore)
	{
		Set<Blob> result = new LinkedHashSet<>();
		if (attachmentTails == null)
			return result; // empty results
		if (attachmentsStore == null)
		{
			JSPHelper.log.error("No attachments store!");
			return result;
		}

		Set<String> neededAttachmentTails = new LinkedHashSet<>();
		Collections.addAll(neededAttachmentTails, attachmentTails);
		for (Document d : docs)
		{
			if (!(d instanceof EmailDocument))
				continue;

			EmailDocument ed = (EmailDocument) d;
			if (ed.attachments == null)
				continue;
			for (Blob b : ed.attachments)
			{
				String url = attachmentsStore.getRelativeURL(b);
				String urlTail = Util.URLtail(url);
				if (neededAttachmentTails.contains(urlTail))
				{
					result.add(b);
				}
			}
		}
		return result;

	}

	/**
	 * returns set of all blobs whose type match ANY of the given extensions.
	 * ("none" is a valid type) and matches attachments that don't have an
	 * extension.
	 */
	/*public static Set<Blob> getBlobsForAttachmentTypes(Collection<? extends Document> docs, String[] attachmentTypes)
	{
		Set<Blob> result = new LinkedHashSet<>();
		// convert to a set for fast lookup
		Set<String> attachmentTypesSet = new LinkedHashSet<>();
		Collections.addAll(attachmentTypesSet, attachmentTypes);

		for (Document d : docs)
		{
			if (!(d instanceof EmailDocument))
				continue;

			EmailDocument ed = (EmailDocument) d;
			if (ed.attachments == null)
				continue;
			for (Blob b : ed.attachments)
			{
				String ext = Util.getExtension(archive.getBlobStore().get_URL_Normalized(b););
				if (ext == null)
					ext = "none";
				ext = ext.toLowerCase();
				if (attachmentTypesSet.contains(ext))
				{
					result.add(b);
				}
			}
		}
		return result;
	}
*/
	/** sort by site-alpha, so e.g. all amazon links show up together */
	public static void sortLinks(List<String> linksList)
	{
		linksList.sort((li1, li2) -> {
			String site1 = Util.getTLD(li1);
			String site2 = Util.getTLD(li2);
			return site1.compareTo(site2);
		});
	}

	public static String stripExtraSpaces(String name){
		String str = "";
		String[] words = name.split("\\s+");
		for(int wi=0;wi<words.length;wi++) {
			str += words[wi];
			if(wi<words.length-1)
				str += " ";
		}
		return str;
	}

	/**
	 * all suffixes of prefixes or all prefixes of suffixes.
	 * */
	public static Set<String> computeAllSubstrings(Set<String> set) {
		Set<String> substrs = new HashSet<>();
		for (String s : set) {
			substrs.addAll(computeAllPrefixes(computeAllSuffixes(s)));
		}
		return substrs;
	}

	private static List<String> computeAllSubstrings(Set<String> set, boolean sort) {
		Set<String> substrs = new HashSet<>();
		for (String s : set) {
			substrs.addAll(computeAllPrefixes(computeAllSuffixes(s)));
		}
		//sort
		Map<String, Integer> substrlen = new LinkedHashMap<>();
		for(String substr: substrs)
			substrlen.put(substr, substr.length());
		List<Pair<String,Integer>> ssubstrslen = Util.sortMapByValue(substrlen);
		List<String> ssubstrs = new ArrayList<>();
		for(Pair<String,Integer> p: ssubstrslen)
			ssubstrs.add(p.getFirst());
		return ssubstrs;
	}

	public static Set<String> computeAllSubstrings(String s)
	{
        s = s.replaceAll("^\\W+|\\W+$","");
		Set<String> set = new LinkedHashSet<>();
		set.add(s);
		return computeAllSubstrings(set);
	}

	/**@param sort in descending order of length*/
	private static List<String> computeAllSubstrings(String s, boolean sort)
	{
        s = s.replaceAll("^\\W+|\\W+$","");
        Set<String> set = new LinkedHashSet<>();
		set.add(s);
		return computeAllSubstrings(set, sort);
	}

	private static Set<String> computeAllPrefixes(Set<String> set)
	{
		Set<String> result = new LinkedHashSet<>();
		for (String s : set)
		{
			String prefix = "";
			StringTokenizer st = new StringTokenizer(s);
			while (st.hasMoreTokens())
			{
				if (prefix.length() > 0)
					prefix += " ";
				prefix += st.nextToken();//.toLowerCase();
				result.add(prefix);
			}
		}
		return result;
	}

	public static Set<String> computeAllPrefixes(String s)
	{
		Set<String> set = new LinkedHashSet<>();
		set.add(s);
		return computeAllPrefixes(set);
	}

	private static Set<String> computeAllSuffixes(Set<String> set)
	{
		Set<String> result = new HashSet<>();
		for (String s : set) {
			String suffix = "";
			String[] words = s.split("\\s+");
			for (int i = words.length - 1; i >= 0; i--) {
				if (suffix.length() > 0)
					suffix = " " + suffix;
				suffix = words[i] + suffix;
				result.add(suffix);
			}
		}
		return result;
	}

	private static Set<String> computeAllSuffixes(String s)
	{
		Set<String> set = new LinkedHashSet<>();
		set.add(s);
		return computeAllSuffixes(set);
	}

	/** experimental method, not used actively */
	//	public static Map<Integer, Collection<Collection<String>>> nameCooccurrenceInParas(Collection<EmailDocument> docs) throws IOException, GeneralSecurityException, ClassCastException, ClassNotFoundException
	//	{
	//		Pattern p = Pattern.compile("[1-9][0-9]*|\\d+[\\.]\\d+"); // don't want #s starting with 0, don't want numbers like 1.
	//		// numMap not used currently
	//		Map<String, List<String>> numMap = new LinkedHashMap<String, List<String>>();
	//
	//		Map<Integer, Collection<Collection<String>>> namesMap = new LinkedHashMap<Integer, Collection<Collection<String>>>();
	//		
	//		for (EmailDocument ed: docs)
	//		{
	//			System.out.println ("D" + ed.docId);
	//		
	//			String contents = "";
	//			try { ed.getContents(); }
	//			catch (ReadContentsException e) { Util.print_exception(e, log); }
	//
	//			Matcher m = p.matcher(contents);
	//			List<String> nums = new ArrayList<String>();
	//			while (m.find())
	//			{
	//				String num = m.group();
	//				
	//				try { 
	//					Integer.parseInt(num);  // just to check for exception
	//					nums.add(num);
	//				} catch (Exception e) {
	//					try { 
	//						Double.parseDouble(num); // just to check for exception
	//						nums.add(num);
	//					}
	//					catch (Exception e1) { Util.report_exception(e1); }
	//				}			
	//			}
	//	
	//			numMap.put(ed.docID, nums);
	//			Collection<String> paras = Util.breakIntoParas(contents);
	//			Collection<Collection<String>> docNames = new ArrayList<Collection<String>>();
	//	
	//			for (String para: paras)
	//			{
	//				List<Pair<String, Float>> names = NER.namesFromText(para);
	//				List<String> paraNames = new ArrayList<String>();
	//				for (Pair<String, ?> pair: names)
	//				{
	//					String name = pair.getFirst();
	//					name = name.replace("\n", " ").trim().toLowerCase().intern();
	//					paraNames.add(name);
	//				}
	//				docNames.add(paraNames);
	//			}
	//			namesMap.put(ed.getUniqueId(), docNames);
	//		}
	//		
	//		return namesMap;
	//	}

	public static Set<String> readCanonicalOwnNames(AddressBook ab)
	{
		// convert own names to canonical form
		Set<String> canonicalOwnNames = new LinkedHashSet<>();
		Set<String> ownNames = (ab != null) ? ab.getOwnNamesSet() : null;
		if (ownNames == null)
			return canonicalOwnNames;

		for (String s : ownNames)
			if (s != null)
				canonicalOwnNames.add(s.toLowerCase());
		return canonicalOwnNames;
	}

	/** returns all languages in a set of docs */
	public static Set<String> allLanguagesInDocs(Collection<? extends Document> docs)
	{
		Set<String> result = new LinkedHashSet<>();
		for (Document d : docs)
			if (d.languages != null)
				result.addAll(d.languages);
		if (result.size() == 0)
			result.add("english");
		return result;
	}

	public static List<Document> selectDocsByRegex(Archive archive, Collection<Document> allDocs, String term)
	{
		List<Document> result = new ArrayList<>();
		Pattern pattern = null;
		try {
			pattern = Pattern.compile(term);
		} catch (Exception e) {
			Util.report_exception(e);
			return result;
		}

		for (Document d : allDocs) {
			if (!Util.nullOrEmpty(d.description)) {
				if (pattern.matcher(d.description).find()) { // d.getHeader() will get message ID which may false match SSN patterns etc.
					result.add(d);
					continue;
				}
			}
			String text = archive.getContents(d, false /* full message */);
			if (pattern.matcher(text).find())
				result.add(d);
		}
		return result;
	}

	private static Set<Document> selectDocsByContact(AddressBook ab, Collection<EmailDocument> docs, Set<String> contact_names, Set<String> contact_emails)
	{
		Set<Document> result = new LinkedHashSet<>();

		// look up ci for given name
		// look up emails for ci
		for (EmailDocument ed : docs)
		{
			// assemble all names and emails for this messages
			List<String> allEmailsAndNames = ed.getAllAddrs();
			allEmailsAndNames.addAll(ed.getAllNames());

			// and match against the given ci
			for (String s : allEmailsAndNames)
			{
				if (contact_names.contains(s) || contact_emails.contains(s))
				{
					result.add(ed);
					break;
				}
			}
		}
		return result;
	}

	private static Set<Document> selectDocsByContact(AddressBook ab, Collection<EmailDocument> docs, Set<Contact> cset)
	{
		if (cset == null)
			return new LinkedHashSet<>();

		Set<String> cnames = new LinkedHashSet<>();
		Set<String> cemails = new LinkedHashSet<>();
		for (Contact c : cset) {
			cnames.addAll(c.getNames());
			cemails.addAll(c.getEmails());
		}
		return selectDocsByContact(ab, docs, cnames, cemails);
	}

	public static String canonicalizeEntity(String e)
	{
		if (e == null)
			return e;
		e = e.replaceAll("\\s\\s", " ");
		return e.trim().toLowerCase();
	}

	public static void main(String args[])
	{
		//System.out.println(query("this is a test", "testing|is|match"));
		List<String> substrs = computeAllSubstrings("Some. thing here", true);
		for(String substr: substrs)
			System.err.print(substr+" ::: ");
		System.err.println();
	}

	static final boolean	PHRASES_CAN_SPAN_EMPTY_LINE	= false;	// false by default, no external controls for this.

	// if false, empty line is treated as a sentence separator

	/**
	 * NEEDS REVIEW.
	 * removes http links etc, adds them in linkList if it is not null. removes
	 * quoted parts of message
	 */
	public static void populateDocLinks(Document d, String text, List<LinkInfo> linkList, boolean inclQM) throws IOException
	{
		BufferedReader br = new BufferedReader(new StringReader(text));

		while (true)
		{
			String line = br.readLine();
			if (line == null)
				break;

			line = line.trim();

			// strip links
			if (line.toLowerCase().contains("http:"))
			{
				StringTokenizer st = new StringTokenizer(line, " \r\n\t<>\""); // tokenize based on things likely to identify starting of link, http://...
				while (st.hasMoreTokens())
				{
					String s = st.nextToken();
					s = Util.stripPunctuation(s);
					if (s.toLowerCase().startsWith("http:"))
					{
						if (linkList != null && d != null)
							linkList.add(new LinkInfo(s, d));
					}
				}
			}
		}
	}

	// normalizes newlines by getting rid of \r's
	public static String normalizeNewlines(String s)
	{
		if (s == null)
			return null;
		String result = s.replaceAll("\r\n", "\n"); // note: no double escapes needed, \r is directly used as a char by java, not an esc. sequence
		result = result.replaceAll("\r", "\n");
		return result;
	}
}
