/*
 Copyright (C) 2012 The Stanford MobiSocial Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package edu.stanford.muse.index;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.Config;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** A lexicon is a set of categories. Each category has a name and a set of words (essentially a query string, OR-separated with |). See <archive>/lexicons/*.lex.txt
 * in theory a lexicon can have different words for the same category in different languages. e.g. a lexicon may have the category
 * happy: joyful|elated|happy in the english lexicon and
 * happy: joyeux|gay in the french lexicon.
 *
 * Note on terminology: here, happy is called the caption, and the rest of the line (separated by :) is called the query
 *
 * of course, one could also write:
 * happy: joyful|elated|happy|joyeux|gay in a single file, but the original idea of separate files for each language was that the lexicon words could be developed by different people or teams)
 * However, in practice, we use a single language (English) and don't split the lexicon categories across multiple files.
 * Therefore this Lexicon1Lang, etc is over-complicated and may be simplified some day.
 */

public class Lexicon implements Serializable {
	
	private static Log log = LogFactory.getLog(Lexicon.class);
	public static final String LEXICON_SUFFIX = ".lex.txt";
	public static final String REGEX_LEXICON_NAME = "regex"; // this is a special lexicon name, to which regex search is applied
	public static final String SENSITIVE_LEXICON_NAME = "sensitive"; // this is a special lexicon name, to which regex search is applied

	private static final long serialVersionUID = 1377456163104266479L;//1L;

	public static Map<String, Lexicon> lexiconMap = new LinkedHashMap<>(); // directory of lexicons // not used but still need when deserialized old archives
	public String name;
	private Map<String, Lexicon1Lang> languageToLexicon = new LinkedHashMap<>();

	//field to store the summary object for this lexicon.
	transient         Map<String, Integer> L1_Summary_category_count = new LinkedHashMap<>();

	public Lexicon1Lang getLexiconForLanguage(String language){
		return languageToLexicon.get(language);
	}
	/** inner class that stores lexicon for 1 language */

	public class Lexicon1Lang implements Serializable {
		private static final long serialVersionUID = 70739504277864045L;//1L;

		/** there are 2 caption -> query maps. the expanded query is the actual query made to the index.
		 * the rawquery is what the user specified (in the <name>.<lang>.lex.txt file 
		 */
		public Map<String, String> captionToExpandedQuery = new LinkedHashMap<>(), captionToRawQuery = new LinkedHashMap<>();
		public Set<String> usedInOtherCaptions = new LinkedHashSet<>();
		public Lexicon1Lang() {}
		public Lexicon1Lang(String filename) throws IOException
		{
			captionToRawQuery = new LinkedHashMap<>();
			captionToExpandedQuery = new LinkedHashMap<>();
            InputStream is = new FileInputStream(filename);
			List<String> lines = Util.getLinesFromInputStream(is, false /* ignore comment lines = false, we'll strip comments here */);
			is.close();
			for (String line:lines)
			{
				int idx = line.indexOf('#'); // strip everything after the comment char
				if (idx >= 0)
					line = line.substring(0, idx);
				line = line.trim();
				if (line.length() == 0)
					continue; // ignore blank lines
				StringTokenizer st = new StringTokenizer (line, ":");
				if (st.countTokens() != 2)
				{
					log.warn ("line ignored: " + line);
					continue;
				}
				
				String caption = st.nextToken().trim();
				String query = st.nextToken().trim();
				String existingQuery = captionToRawQuery.get(caption);
				if (!Util.nullOrEmpty(existingQuery))
					query = existingQuery + "|" + query;
				captionToRawQuery.put(caption, query);				
			}
			expandQueries();
		}
		
		public void setRawQueryMap(Map<String, String> map)
		{
			captionToRawQuery = map;
			expandQueries();
		}
		
		private void expandQueries()
		{
			captionToExpandedQuery.clear(); // clear the expanded query map first, we don't want any residue from the previous state
			for (String caption: captionToRawQuery.keySet())
			{
				String query = captionToRawQuery.get(caption);
				List<String> orTerms = Util.tokenize(query, "|");
				String expandedQuery = "";
				for (int i = 0; i < orTerms.size(); i++)
				{
					String t = orTerms.get(i).trim();
					if (t.length() == 0)
						continue;
					if (t.startsWith("{") && t.endsWith("}"))
					{
						String c = t.substring(1, t.length()-1);
						String exp = captionToExpandedQuery.get(c); // note: expanded map, not rawmap, to allow multi-level expansion
						if (exp == null)
						{
							t = captionToRawQuery.get(c);
							if (t == null)
							{
								log.warn("ERROR: no prev. caption: " + c + " in query " + query);
								continue;
							}
						}
						else
							t = exp;

						usedInOtherCaptions.add(c);
					}
                    expandedQuery += t;
                    //there is no point adding or(|), as the query is treated just as a text string and is not handled specially in Indexer.lookupDocsAsId
                    //however, adding a non-word, non-special character will enable tokenization at that index and will be appended as many "or" terms.
                    if (i < orTerms.size()-1)
						expandedQuery += "|";
				}
				
				if (caption.length() > 0 && expandedQuery.length() > 0)
				{
					// if caption already exists, just add to it
					String existingQuery = captionToExpandedQuery.get(caption);
					if (!Util.nullOrEmpty(existingQuery))
						expandedQuery = existingQuery + "|" + expandedQuery;
					captionToExpandedQuery.put(caption, expandedQuery);
				}
			}
			
			// remove the non top-level captions
			for (String caption: usedInOtherCaptions)
				captionToExpandedQuery.remove(caption);
		}
		
		public void save (String filename) throws Exception
		{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
			for (String caption: captionToRawQuery.keySet())
				pw.println (caption + ":" + captionToRawQuery.get(caption));
			pw.close();
			log.info(captionToRawQuery.size() + " sentiment categories saved in " + filename);
		}




		/** main entry point: returns a category -> docs map for each (non-zero) category in the current captionToQueryMap. 
		 * @indexer must already have run 
		 * @docs results are restrictes to these docs. assumes all docs if docs is null or empty.
		 * @captions (null/none = all)
         *
         * vihari
         * This is a weird name for a method that returns documents with emotions instead of emotions.
		 */
		public Map<String, Collection<Document>> getEmotions (Indexer indexer, Collection<Document> docs, boolean originalContentOnly, String... captions)
		{
			Map<String, Collection<Document>> result = new LinkedHashMap<>();
			Set<Document> docs_set = Util.castOrCloneAsSet(docs);
//			for (String[] emotion: emotionsData)
			String[] selected_captions = captions.length > 0 ? captions : captionToExpandedQuery.keySet().toArray(new String[0]);
			for (String caption: selected_captions)
			{
				String query = captionToExpandedQuery.get(caption);
				if (query == null) {
					log.warn("Skipping unknown caption '" + caption + "'");
					continue;
				}

				// query is simply word1|word2|word3 etc for that sentiment
				// the -1 indicates that we want all docs in the indexer that match the query
				int threshold = 1;
                Indexer.QueryOptions options = new Indexer.QueryOptions();
                options.setThreshold(threshold);
                Indexer.QueryType qt = Indexer.QueryType.FULL;
                if (originalContentOnly)
                    qt = Indexer.QueryType.ORIGINAL;
                if (Lexicon.REGEX_LEXICON_NAME.equals (Lexicon.this.name))
                    qt = Indexer.QueryType.REGEX;

                options.setQueryType(qt);
				Collection<Document> docsForCaption = indexer.docsForQuery(query, options);
				/*
				log.info (docsForCaption.size() + " before");
				threshold = 2;
				docsForCaption = indexer.docsForQuery(query, -1, threshold);
				log.info (docsForCaption.size() + " after");
				*/
//				Set<Document> docs = indexer.docsWithPhraseThreshold(query, -1, 2); // in future, we may have a higher threshold for sentiment matching
				// if @param docs is present, retain only those docs that match, otherwise retain all
				if (!Util.nullOrEmpty(docs_set))
					//docsForCaption.retainAll(docs_set);
					docsForCaption = Util.listIntersection(docsForCaption, docs_set);

				// put it in the result only if at least 1 doc matches
				if (docsForCaption.size() > 0)
					result.put (caption, docsForCaption);
			}
			return result;
		}
		
		/** NOTA = None of the above. like getEmotions but also returns a category called None for docs that don't match any sentiment.
		 */
		public Map<String, Collection<Document>> getEmotionsWithNOTA (Indexer indexer, Collection<Document> docs, boolean originalContentOnly)
		{
			Collection<Document> allDocs;
			if (docs == null)
				allDocs = indexer.docs;
			else
				allDocs = docs;
			
			Map<String, Collection<Document>> map = getEmotions (indexer, docs, originalContentOnly);

			// collects docs with any sentiments
			Set<Document> docsWithAnySentiment = new LinkedHashSet<>();
			for (String sentiment: map.keySet())
				docsWithAnySentiment.addAll(map.get(sentiment));

			// docsWithNoSentiment = allDocs - docsWithAnySentiment
			Set<Document> docsWithNoSentiment = new LinkedHashSet<>();
			for (Document d: allDocs)
				if (!docsWithAnySentiment.contains(d))
					docsWithNoSentiment.add(d);
			map.put("None", docsWithNoSentiment);
			return map;
		}
		
		/** returns docs that don't match any sentiment. */
		public Set<Document> getDocsWithNoEmotions (Indexer indexer, Set<Document> docs, boolean originalContentOnly)
		{
			Collection<Document> allDocs;
			if (docs == null)
				allDocs = indexer.docs;
			else
				allDocs = docs;
			
			Map<String, Collection<Document>> map = getEmotions (indexer, docs, originalContentOnly);

			// collects docs with any sentiments
			Set<Document> docsWithAnySentiment = new LinkedHashSet<>();
			for (String sentiment: map.keySet())
				docsWithAnySentiment.addAll(map.get(sentiment));

			// docsWithNoSentiment = allDocs - docsWithAnySentiment
			Set<Document> docsWithNoSentiment = new LinkedHashSet<>();
			for (Document d: allDocs)
				if (!docsWithAnySentiment.contains(d))
					docsWithNoSentiment.add(d);
			return docsWithNoSentiment;
		}

		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append ("Raw lexicon:\n\n");
			for (String s: captionToRawQuery.keySet())
				sb.append(s + ":" + captionToRawQuery.get(s) + "\n");
			sb.append ("\n-----------------------------------------------------------------------------\n");
			sb.append ("Expanded lexicon:\n\n");
			for (String s: captionToExpandedQuery.keySet())
				sb.append(s + ":" + captionToExpandedQuery.get(s) + "\n");
			return sb.toString();
		}
	}

	// avoid file traversal vuln's, don't allow / or \ in lexicon names;
	public static String sanitizeLexiconName(String lex)
	{
		return lex.replaceAll("/", "__").replaceAll("\\\\", "__"); // the pattern itself needs 2 backslashes since \ is a regex escape char
	}

	public static String lexiconNameFromFilename(String filename)
	{
		return filename.replaceAll("\\.[^\\.]+\\" + LEXICON_SUFFIX, "");
	}

	public Lexicon (String dir, String name) throws IOException
	{
		name = sanitizeLexiconName(name);
		this.name = name;
		Set<String> languages = Util.filesWithPrefixAndSuffix(dir, name + ".", LEXICON_SUFFIX);
		for (String language: languages)
		{
			Lexicon1Lang lex = new Lexicon1Lang(dir + File.separator + name + "." + language + LEXICON_SUFFIX);  // LEXICON_SUFFIX already has a .
			language = language.toLowerCase();
			languageToLexicon.put(language, lex);
		}
	}

	public void removeLexicon(Archive archive,String dir){
		String name = this.name ;
		Set<String> languages = Util.filesWithPrefixAndSuffix(dir, name + ".", LEXICON_SUFFIX);
		//remove all languages for the given lexicon??
		for (String language: languages)
		{
			String filepath = dir + File.separator + name + "." + language + LEXICON_SUFFIX;  // LEXICON_SUFFIX already has a .
			try {
				Files.deleteIfExists(new File(filepath).toPath());
			} catch (IOException e) {
				e.printStackTrace();
				log.debug(e.getMessage());
			}
		}
		//update the bag metadata as well..
		archive.updateFileInBag(dir+File.separator,archive.baseDir);

	}
	public Set<String> getAvailableLanguages()	{ return Collections.unmodifiableSet(languageToLexicon.keySet()); }
	
	// identify all the langs in the docs, and the corresponding lexicons
	public Collection<Lexicon1Lang> getRelevantLexicon1Langs(Collection<Document> docs)
	{
		if (docs == null)
			return languageToLexicon.values(); // just return all lexicons
		
		Set<String> languages = IndexUtils.allLanguagesInDocs(docs);
		Set<Lexicon1Lang> lexicons = new LinkedHashSet<>();
		for (String lang: languages)				
		{
			Lexicon1Lang lex = languageToLexicon.get(lang);
			if (lex != null)
				lexicons.add(lex); // this lexicon doesn't know about this language
			else
				log.warn ("Warning: no support for " + lang + " in lexicon " + name);
		}
		return lexicons;
	}

	/*
	This method fills the summary object for all lexicons of the given archive.
	 */
	public static void fillL1_Summary_all(Archive archive, boolean originalContentOnly){

		for(String lexiconName: archive.getAvailableLexicons()){
			Lexicon lexicon = archive.getLexicon(lexiconName);
			lexicon.fillL1_Summary(lexiconName,archive,originalContentOnly);
		}

	}

	/*
	This method fills the summary object for a particular lexicon
	 */
	public void fillL1_Summary(String lexiconName, Archive archive, boolean originalContentOnly){
		//clear summary objects
		L1_Summary_category_count.clear();
	//call searchresult from here...
		List<Document> docs = archive.indexer.docs;
		Collection<Lexicon1Lang> lexicons  = getRelevantLexicon1Langs(docs);

		// aggregate results for each lang into result

		for (Lexicon1Lang lex: lexicons)
		{
			Set<String> categories = ((LinkedHashMap) lex.captionToRawQuery).keySet();
			for(String category: categories){
				Multimap<String,String> queryparams = LinkedListMultimap.create();
				queryparams.put("lexiconName",lexiconName);
				queryparams.put("lexiconCategory",category);
				if(originalContentOnly)
					queryparams.put("termOriginalBody","on");
				else
					queryparams.put("termBody","on");
				queryparams.put("termSubject","on");
				queryparams.put("termAttachments","on");
				SearchResult input = new SearchResult(archive,queryparams);
				SearchResult output = SearchResult.filterForLexicons(input);
				L1_Summary_category_count.put(category,output.getDocumentSet().size());
			}
		}

	}
	/*
	This method constructs the JSON object from the summary data and return it.
	 */

	public  JSONArray getCountsAsJSON(String lexiconName, Archive archive){

		JSONArray finalArray = new JSONArray();
		AtomicInteger  count = new AtomicInteger(0);
		L1_Summary_category_count.forEach((category,cnt)-> {

			JSONArray j = new JSONArray();
			j.put(0, Util.escapeHTML(category));
			j.put(1, cnt);
			finalArray.put(count.getAndIncrement(), j);
		});

		return finalArray;

    }

	/** Core sentiment detection method. doNota = none of the above 
	 * @param captions (null/none = all) */
	public Map<String, Collection<Document>> getEmotions (Indexer indexer, Collection<Document> docs, boolean doNota, boolean originalContentOnly, String... captions)
	{	
		Collection<Lexicon1Lang> lexicons  = getRelevantLexicon1Langs(docs);
		Map<String, Collection<Document>> result = new LinkedHashMap<>();
		Set<Document> docs_set = Util.castOrCloneAsSet(docs);
		// aggregate results for each lang into result
		for (Lexicon1Lang lex: lexicons)
		{
			Map<String, Collection<Document>> resultsForThisLang = (doNota? lex.getEmotionsWithNOTA(indexer, docs_set, originalContentOnly) : lex.getEmotions(indexer, docs_set, originalContentOnly, captions));
			if (resultsForThisLang == null)
				continue;

			for (String caption: resultsForThisLang.keySet())
			{
				Collection<Document> resultDocsThisLang = resultsForThisLang.get(caption);
				Collection<Document> resultDocs = result.get(caption);
				// if caption doesn't exist already, create a new entry, or else add to the existing set of docs that match this caption
				if (resultDocs == null)
					result.put(caption, resultDocsThisLang);
				else
					resultDocs.addAll(resultDocsThisLang);
			}				
		}
		// TODO: the result can be cached at server to avoid redundant computation (by concurrent users, which are few for now)
		return result;
	}
	
	private Set<Document> getDocsWithAnyEmotions(Indexer indexer, Collection<Document> docs, boolean originalContentOnly)
	{
		Set<Document> result = new LinkedHashSet<>();
		// return all docs that have at least one sentiment
		Map<String, Collection<Document>> map = getEmotions(indexer, docs, originalContentOnly, false);
		for (Collection<Document> values: map.values())
			result.addAll(values);
		return result;
	}

	private Set<Document> getDocsWithNoEmotions(Indexer indexer, Collection<Document> docs, boolean originalContentOnly)
	{
		Set<Document> result = new LinkedHashSet<>(docs);
		result.removeAll(getDocsWithAnyEmotions(indexer, docs, originalContentOnly));
		return result;
	}
	
	/** returns docs with ALL given sentiments.
	 * special cases: sentiments can be an array of length 1 and be "None", in which case all documents with no sentiments are returned.
	 * special cases: sentiments can be an array of length 1 and be "all", in which case all documents with any sentiments are returned. 
	 * @param captions */
	public Collection<Document> getDocsWithSentiments (String sentiments[], Indexer indexer, Collection<Document> docs, int cluster, boolean originalContentOnly, String... captions)
	{
		Collection<Document> result = null;
		// note: multiple sentiments are possible, they are ANDED
		if (sentiments == null || sentiments.length == 0)
			return result;

		Set<Document> docs_set = Util.castOrCloneAsSet(docs);
		if (sentiments.length == 1 && "all".equalsIgnoreCase(sentiments[0]))
			return getDocsWithAnyEmotions(indexer, docs_set, originalContentOnly);

		// note: we'll pass in null for docs, and intersect with the given set of docs later
		// otherwise we'd just be doing it again and again for each category and lexer
		Map<String, Collection<Document>> map = getEmotions(indexer, null, false, originalContentOnly, captions);
        for (String sentiment : sentiments) {
            Collection<Document> temp1 = ("None".equalsIgnoreCase(sentiment)) ? getDocsWithNoEmotions(indexer, docs_set, originalContentOnly) : map.get(sentiment);
            if (temp1 == null) {    // no matches, just return
                result = new LinkedHashSet<>();
                return result;
            }
            if (result == null)
                result = temp1;
            else
                result.retainAll(temp1);
        }
		//result.retainAll(docs);
		return Util.setIntersection(result, docs_set);
	}
	
	/** gets map for all languages */
    private Map<String, String> getCaptionToQueryMap(Collection<Document> docs)
	{
		// identify all the langs in the docs, and the corresponding lexicons
		Set<String> languages = IndexUtils.allLanguagesInDocs(docs);
		Set<Lexicon1Lang> lexicons = new LinkedHashSet<>();
		for (String lang: languages)				
		{
			Lexicon1Lang lex = languageToLexicon.get(lang);
			if (lex != null)
				lexicons.add(lex);
			// this lexicon doesn't know about this language
			else
				log.warn ("Warning: no support for " + lang + " in lexicon " + name);
		}
		
		Map<String, String> result = new LinkedHashMap<>();
		// aggregate results for each lang into result
		for (Lexicon1Lang lex: lexicons)
		{
			Map<String, String> resultsForThisLang = lex.captionToExpandedQuery;
			
			for (String caption: resultsForThisLang.keySet())
			{
				String queryThisLang = resultsForThisLang.get(caption);
                // if caption doesn't exist already, create a new entry, or else add to the existing set of docs that match this caption
                result.merge(caption, queryThisLang, (a, b) -> a + "|" + b);
			}				
		}
		return result;
	}
	
	/** returns whether it succeeded 
	 * @throws Exception */
	public boolean save(String dir, String language,Archive archive) throws Exception
	{
		language = language.toLowerCase();
		Lexicon1Lang langLex = languageToLexicon.get(language);
		if (langLex == null)
			return false;
		langLex.save(dir + File.separator + name + "." + language + LEXICON_SUFFIX); // LEXICON_SUFFIX already has a .
		//update the bag metadata as well..
		archive.updateFileInBag(dir,archive.baseDir);
		//update the summary of lexicons here..
		return true;
	}

	/** updates the map for a given language 
	 * @throws IOException 
	 * @throws FileNotFoundException */
	public boolean update(String language, Map<String, String> map) {
		language = language.toLowerCase();
        Lexicon1Lang langLex = languageToLexicon.computeIfAbsent(language, k -> new Lexicon1Lang());
        langLex.setRawQueryMap(map);
		return true;
	}

	public Map<String, String> getRawMapFor(String language)
	{
		language = language.toLowerCase();
		Lexicon1Lang lex = languageToLexicon.get(language);
		if (lex == null)
			return null;
		
		return Collections.unmodifiableMap(lex.captionToRawQuery);
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append ("Lexicon " + name + " with " + Util.pluralize(languageToLexicon.size(), "language") + "\n");
		int count = 0;
		for (String language: languageToLexicon.keySet())
		{
			sb.append ("Language #" + count++ + ": " + language + "\n");
			sb.append(languageToLexicon.get(language));
			sb.append ("\n-----------------------------------------------------------------------------\n");
		}
		return sb.toString();
	}

	private Set<String> wordsForSentiment (Map<String, String> captionToQueryMap, String sentiment)
	{
		if (sentiment == null)
			return null;
		Set<String> set = new LinkedHashSet<>();
		String query = captionToQueryMap.get(sentiment);
		if (query == null)
			return set;
		StringTokenizer st = new StringTokenizer(query, "|");
		while (st.hasMoreTokens())
			set.add(st.nextToken().trim().toLowerCase());
		return set;
	}

	/* returns set of terms for any of the given sentiments -- usually used for highlighting */
	public Set<String> wordsForSentiments (Indexer indexer, Collection<Document> docs, String sentiments[])
	{
		Map<String, String> captionToQueryMap = getCaptionToQueryMap(docs);
		
		if (sentiments == null)
			return null;
		Set<String> result = new LinkedHashSet<>();
		for (String sentiment: sentiments)
		{
			Set<String> set = wordsForSentiment(captionToQueryMap, sentiment);
			if (set == null)
				continue;
			result.addAll(set);
		}
		return result;
	}

	public static void main(String args[]) throws IOException
	{
		Lexicon lex = new Lexicon("/tmp", "default");
		System.out.println(lex);
	}
}
