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

import edu.stanford.muse.Config;
import edu.stanford.muse.util.DictUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Triple;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.SimpleSessions;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**OpenNLP NER
 * @see edu.stanford.muse.ner.NER
 * @deprecated because it is not used any more
 */
@Deprecated public class NER {
	public static Log								log						= LogFactory.getLog(NER.class);
	public static boolean							REMOVE_I18N_CHARS		= false;

	public static final Map<String, LocationInfo>	locations				= new LinkedHashMap<String, LocationInfo>();
	public static final Map<String, Long>			populations				= new LinkedHashMap<String, Long>();
	public static final Set<String>					locationsToSuppress		= new LinkedHashSet<String>();
	public static Map<String, Integer>				defaultTokenTypeWeights	= new LinkedHashMap<String, Integer>();
	public static final int							MIN_NAME_LENGTH			= 2, MAX_NAME_LENGTH = 100;					// can't have a single char name. max # of chars in a name. above this we'll drop it. avoids NER returning garbage like sometimes it returns a string of 200K 'a's.

	static {
		//	readLocationNamesToSuppress();
		//			readLocationsFreebase();
		//	readLocationsWG();
		//	Indexer.log.info ("locations DB has " + locations.size() + " coordinates");
		try {
			NER.initialize();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		defaultTokenTypeWeights.put("PERSON", 10000); // 1000 FLAG DEBUG
		if (System.getProperty("muse.remove18n") != null)
		{
			REMOVE_I18N_CHARS = true;
			log.info("Remove i18n chars = true");
		}
	}

	public static void readLocationsFreebase() throws IOException
	{
		InputStream is = new GZIPInputStream(NER.class.getClassLoader().getResourceAsStream("locations.gz"));
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			StringTokenizer st = new StringTokenizer(line, "\t");
			if (st.countTokens() == 3)
			{
				String locationName = st.nextToken();
				String canonicalName = locationName.toLowerCase();
				String lat = st.nextToken();
				String longi = st.nextToken();
				locations.put(canonicalName, new LocationInfo(locationName, lat, longi));
			}
		}
		lnr.close();
	}

	public static void readLocationsWG()
	{
		InputStream is = null;
		try {
			is = new GZIPInputStream(NER.class.getClassLoader().getResourceAsStream("WG.locations.txt.gz"));
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
			while (true)
			{
				String line = lnr.readLine();
				if (line == null)
					break;
				StringTokenizer st = new StringTokenizer(line, "\t");
				if (st.countTokens() == 4)
				{
					String locationName = st.nextToken();
					String canonicalName = locationName.toLowerCase();
					if (locationsToSuppress.contains(canonicalName))
						continue;
					String lat = st.nextToken();
					String longi = st.nextToken();
					String pop = st.nextToken();
					long popl = Long.parseLong(pop);
					float latf = ((float) Integer.parseInt(lat)) / 100.0f;
					float longif = ((float) Integer.parseInt(longi)) / 100.0f;
					Long existingPop = populations.get(canonicalName);
					if (existingPop == null || popl > existingPop)
					{
						populations.put(canonicalName, popl);
						locations.put(canonicalName, new LocationInfo(locationName, Float.toString(latf), Float.toString(longif)));
					}
				}
			}
			if (is != null)
				is.close();
		} catch (Exception e) {
			log.warn("Unable to read World Gazetteer file, places info may be inaccurate");
			log.debug(Util.stackTrace(e));
		}
	}

	public static void readLocationNamesToSuppress()
	{
		String suppress_file = "suppress.locations.txt.gz";
		try {
			InputStream is = new GZIPInputStream(NER.class.getClassLoader().getResourceAsStream(suppress_file));
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
			while (true)
			{
				String line = lnr.readLine();
				if (line == null)
					break;
				StringTokenizer st = new StringTokenizer(line);
				if (st.hasMoreTokens())
				{
					String s = st.nextToken();
					if (!s.startsWith("#"))
						locationsToSuppress.add(s.toLowerCase());
				}
			}
			is.close();
		} catch (Exception e) {
			log.warn("Error: unable to read " + suppress_file);
			Util.print_exception(e);
		}
		log.info(locationsToSuppress.size() + " names to suppress as locations");
	}

	private static TokenizerME			tokenizer;
	private static NameFinderME			pFinder, lFinder, oFinder;
	private static SentenceDetectorME	sFinder;

	public synchronized static void initialize() throws ClassCastException, IOException, ClassNotFoundException
	{
		if (pFinder != null)
			return;
		long startTimeMillis = System.currentTimeMillis();
		log.info("Initializing NER models");

        try {
            InputStream pis = Config.getResourceAsStream("models/en-ner-person.bin");
            TokenNameFinderModel pmodel = new TokenNameFinderModel(pis);
            pFinder = new NameFinderME(pmodel);

            InputStream lis = Config.getResourceAsStream("models/en-ner-location.bin");
            TokenNameFinderModel lmodel = new TokenNameFinderModel(lis);
            lFinder = new NameFinderME(lmodel);

            InputStream ois = Config.getResourceAsStream("models/en-ner-organization.bin");
            TokenNameFinderModel omodel = new TokenNameFinderModel(ois);
            oFinder = new NameFinderME(omodel);
        }
        //dont bother about this, instead try not to use it
        catch(Exception e){
            Util.print_exception(e, log);
        }
        try {
            InputStream modelIn = Config.getResourceAsStream("models/en-sent.bin");
            SentenceModel model = new SentenceModel(modelIn);
            sFinder = new SentenceDetectorME(model);

            InputStream tokenStream = Config.getResourceAsStream("models/en-token.bin");
            TokenizerModel modelTokenizer = new TokenizerModel(tokenStream);
            tokenizer = new TokenizerME(modelTokenizer);
        }catch(Exception e){
            Util.print_exception(e);
        }

        long endTimeMillis = System.currentTimeMillis();
		log.info("Done initializing NER model in " + Util.commatize(endTimeMillis - startTimeMillis) + "ms");
	}

	public synchronized static void release_classifier()
	{
		pFinder = lFinder = oFinder = null;
		tokenizer = null;
	}

	public static Set<String>	allTypes	= new LinkedHashSet<String>();

	public synchronized static MyTokenizer parse(String documentText)
	{
		return parseAndGetOffsets(documentText).first;
	}

	/** replaced i18n chars above ascii 127, with spaces */
	private static String cleanI18NChars(String s)
	{
		char[] chars = s.toCharArray();
		int nCharsReplaced = 0;
		for (int i = 0; i < chars.length; i++)
			if (chars[i] < 0 || chars[i] > 127)
			{
				nCharsReplaced++;
				chars[i] = ' ';
			}
		if (nCharsReplaced > 0)
		{
			log.info("i18n char(s) replaced: " + nCharsReplaced);
			s = new String(chars);
		}
		return s;
	}

	private static boolean nameFilterPass(String n)
	{
		// if 50% or less of the letters are characters, its not a valid name
		// e.g. fail "p.s." or "ca 94305"

		int chars = 0;
		for (char c : n.toCharArray())
			if (Character.isLetter(c))
				chars++;
		return (chars * 2 > n.length());
	}

	/**
	 * triple is a set of <entity, start char offset (inclusive), end char offset (not inclusive).
	 * see http://nlp.stanford.edu/nlp/javadoc/javanlp/edu/stanford/nlp/ie/AbstractSequenceClassifier.html#classifyToCharacterOffsets(java.lang.String)
	 */
	private synchronized static Pair<MyTokenizer, List<Triple<String, Integer, Integer>>> parseAndGetOffsets(String documentText)
	{
		try {
			NER.initialize();
		} catch (Exception e) {
			Util.print_exception(e, log);
		}

		if (documentText.indexOf("\u00A0") > 0)
			documentText = documentText.replaceAll("\\xA0", " "); // 0xA0 is seen often and generates a lot of annoying messages.
		// replace i18n chars with space, causes annoying NER messages + perhaps slows down NER?
		if (REMOVE_I18N_CHARS)
			documentText = cleanI18NChars(documentText);

		List<Pair<String, String>> namedEntities = new ArrayList<Pair<String, String>>(); // token-type pairs
		List<Triple<String, Integer, Integer>> allTriples = new ArrayList<Triple<String, Integer, Integer>>(); // string, start-end pairs

		Span sentenceSpans[] = sFinder.sentPosDetect(documentText); // do NER sentence by sentence -- much faster than doing the entire documentText at once

		for (Span sentenceSpan : sentenceSpans)
		{
			int sentenceStartOffset = sentenceSpan.getStart();
			String sentence = sentenceSpan.getCoveredText(documentText).toString();
			if (sentence.length() > 2000)
				continue; // that's not a reasonable sentence, could be a uuencoded-something.

			// convert sentence to tokens cos that's what the name finders need
			Span[] tokSpans = tokenizer.tokenizePos(sentence);
			String tokens[] = new String[tokSpans.length];
			for (int i = 0; i < tokSpans.length; i++)
				tokens[i] = tokSpans[i].getCoveredText(sentence).toString();

			// find the actual spans (in terms of tokens) that represent names
			Span[] pSpans = pFinder.find(tokens);
			Span[] lSpans = lFinder.find(tokens);
			Span[] oSpans = oFinder.find(tokens);
			List<Triple<String, Integer, Integer>> sentenceTriples = new ArrayList<Triple<String, Integer, Integer>>(); // string, start-end pairs

			for (Span span : pSpans)
				sentenceTriples.add(new Triple<String, Integer, Integer>("PERSON", span.getStart(), span.getEnd()));
			for (Span span : lSpans)
				sentenceTriples.add(new Triple<String, Integer, Integer>("LOCATION", span.getStart(), span.getEnd()));
			for (Span span : oSpans)
				sentenceTriples.add(new Triple<String, Integer, Integer>("ORGANIZATION", span.getStart(), span.getEnd()));

			for (Triple<String, Integer, Integer> t : sentenceTriples)
			{
				String type = t.first();
				if (type == null)
					type = "UNKNOWN"; // we see type = null sometimes #!@#$
				allTypes.add(type);
				int startTok = t.second();
				int endTok = t.third();

				String namedEntity = sentence.substring(tokSpans[startTok].getStart(), tokSpans[endTok - 1].getEnd());
				// we tend to see a lot of annoying [Hi Sam] or [Dear Caroline] phrases. surprising NER can't handle it already.

				if (namedEntity.toLowerCase().startsWith("hi "))
					namedEntity = namedEntity.substring("hi ".length()).trim();
				if (namedEntity.toLowerCase().startsWith("hello "))
					namedEntity = namedEntity.substring("hello ".length()).trim();
				if (namedEntity.toLowerCase().startsWith("dear "))
					namedEntity = namedEntity.substring("dear ".length()).trim();
				if (namedEntity.toLowerCase().startsWith("cheers "))
					namedEntity = namedEntity.substring("cheers ".length()).trim();
				if (namedEntity.toLowerCase().startsWith("thanks "))
					namedEntity = namedEntity.substring("thanks ".length()).trim();

				if (DictUtils.tabooNames.contains(namedEntity.toLowerCase()))
					continue;
				if (!nameFilterPass(namedEntity))
					continue;

				if (namedEntity.length() < MIN_NAME_LENGTH || namedEntity.length() > MAX_NAME_LENGTH) // drop it
					continue;
				namedEntities.add(new Pair<String, String>(namedEntity, type));
				if(log.isDebugEnabled())
                    log.debug(t.first() + " : [" + t.second() + ":" + t.third() + "] " + namedEntity);
			}

			// sentence triple offsets cannot be used directly ... have to be first converted to the right offset within the entire document by adding sentenceStartOffset
			for (Triple<String, Integer, Integer> t : sentenceTriples) {
				int startTok = t.second();
				int endTok = t.third();
				int start = tokSpans[startTok].getStart(), end = tokSpans[endTok - 1].getEnd();

				//allTriples.add(new Triple<String, Integer, Integer>(t.getFirst(), sentenceStartOffset + t.getSecond(), sentenceStartOffset + t.getThird()));
				allTriples.add(new Triple<String, Integer, Integer>(t.getFirst(), sentenceStartOffset + start, sentenceStartOffset + end));
			}
		}

		return new Pair<MyTokenizer, List<Triple<String, Integer, Integer>>>(new NERTokenizer(namedEntities), allTriples);
	}

	private static String getSafeText(String documentText)
	{
		// workaround for "Warning: calling makeObjectBankFromString with an existing file name! This will open the file instead."
		// if "documentText" happens to match an existing filename, makeObjectBankFromString, which is used by classifyToCharacterOffsets, can misbehave.
		if ((new File(documentText)).exists()) {
			if (!(".".equals(documentText) || "..".equals(documentText))) { // do not warn on "." and ".." files which always exist
				log.warn("Muse should be launched from an empty directory to workaround a caveat of name extraction"); // would still have problem with "/home" or "\windows\system\" etc.
			}
			//return new NERTokenizer(tokensList);
			do {
				documentText += " % ? * / \\ | > < \" :"; // a number of invalid chars that should not be in filename
			} while ((new File(documentText)).exists());
		}

		return documentText;
	}

	public static void printAllTypes()
	{
		Indexer.log.info(allTypes.size() + " NER types");
		for (String s : allTypes)
			Indexer.log.info(s);
	}

	public static List<Pair<String, Float>> namesFromURL(String url) throws ClassCastException, IOException, ClassNotFoundException
	{
		return namesFromURL(url, false);
	}

	/** returns a list of <name, #occurrences> */
	public static List<Pair<String, Float>> namesFromURL(String url, boolean removeCommonNames) throws ClassCastException, IOException, ClassNotFoundException
	{
		// only http conns allowed currently
		Indexer.log.info(url);
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setInstanceFollowRedirects(true);
		conn.setRequestProperty("User-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:6.0.2) Gecko/20100101 Firefox/6.0.2");
		conn.connect();
		Indexer.log.info("url for extracting names:" + conn.getURL());
		byte[] b = Util.getBytesFromStream(conn.getInputStream());
		String text = new String(b, "UTF-8");
		text = Util.unescapeHTML(text);
		org.jsoup.nodes.Document doc = Jsoup.parse(text);
		text = doc.body().text();
		return namesFromText(text, removeCommonNames);
	}

	public static List<Pair<String, Float>> namesFromArchive(String url, boolean removeCommonNames) throws ClassCastException, IOException, ClassNotFoundException
	{
		// only http conns allowed currently
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

		conn.setInstanceFollowRedirects(true);
		conn.setRequestProperty("User-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:6.0.2) Gecko/20100101 Firefox/6.0.2");
		conn.connect();

		byte[] b = Util.getBytesFromStream(conn.getInputStream());
		String text = new String(b, "UTF-8");
		text = Util.unescapeHTML(text);
		org.jsoup.nodes.Document doc = Jsoup.parse(text);
		text = doc.body().text();
		return namesFromText(text, removeCommonNames);
	}

	public static List<Pair<String, Float>> namesFromURLs(String urls)
	{
		return namesFromURLs(urls, false);
	}

	/** like namesFromURLs, just takes multiple \s separated urls */
	public static List<Pair<String, Float>> namesFromURLs(String urls, boolean removeCommonNames)
	{
		List<Pair<String, Float>> result = new ArrayList<Pair<String, Float>>();
		StringTokenizer st = new StringTokenizer(urls);
		while (st.hasMoreTokens())
		{
			try {
				String url = st.nextToken();
				result.addAll(namesFromURL(url, removeCommonNames));
			} catch (Exception e) {
				Util.print_exception(e);
			}
		}
		return result;
	}

	/** returns a list of <name, #occurrences> */
	public static List<Pair<String, Float>> namesFromText(String text) throws ClassCastException, IOException, ClassNotFoundException
	{
		return namesFromText(text, false);
	}

	public static List<Pair<String, Float>> namesFromText(String text, boolean removeCommonNames) throws ClassCastException, IOException, ClassNotFoundException
	{
		Map<String, Integer> defaultTokenTypeWeights = new LinkedHashMap<String, Integer>();
		defaultTokenTypeWeights.put("PERSON", 100); // 1000 FLAG DEBUG
		return namesFromText(text, removeCommonNames, defaultTokenTypeWeights);
	}

	public static List<Pair<String, Float>> namesFromText(String text, boolean removeCommonNames, Map<String, Integer> tokenTypeWeights) throws ClassCastException, IOException, ClassNotFoundException
	{
		return namesFromText(text, removeCommonNames, tokenTypeWeights, false, 0);
	}

	/**
	 * return value.first: a list of <category> -> list of names in that category.
	 * */
	public static Pair<Map<String, List<String>>, List<Triple<String, Integer, Integer>>> categoryToNamesMap(String text)
	{
		Pair<MyTokenizer, List<Triple<String, Integer, Integer>>> ner_result = NER.parseAndGetOffsets(text);
		NERTokenizer t = (NERTokenizer) ner_result.first;
		Map<String, List<String>> map = new LinkedHashMap<String, List<String>>();

		// filter tokens first, map them out by category
		while (t.hasMoreTokens())
		{
			Pair<String, String> tokenAndType = t.nextTokenAndType();
			String token = tokenAndType.getFirst();
			String type = tokenAndType.getSecond();
			List<String> list = map.get(type);
			if (list == null)
			{
				list = new ArrayList<String>();
				map.put(type, list);
			}
			list.add(token);
		}
		return new Pair<Map<String, List<String>>, List<Triple<String, Integer, Integer>>>(map, ner_result.second);
	}

	/**
	 * returns a list of <name, weighted #occurrences>.
	 * positional_boost can be 0, in which case no weight bias based on position
	 */
	public static List<Pair<String, Float>> namesFromText(String text, boolean removeCommonNames, Map<String, Integer> tokenTypeWeights, boolean normalizeByLength, int max_positional_boost) throws ClassCastException, IOException, ClassNotFoundException
	{
		NERTokenizer t = (NERTokenizer) NER.parse(text);
		Map<String, Float> termFreqMap = new LinkedHashMap<String, Float>();
		List<Pair<String, String>> tokenList = new ArrayList<Pair<String, String>>();

		// filter tokens first
		while (t.hasMoreTokens())
		{
			Pair<String, String> tokenAndType = t.nextTokenAndType();
			String token = tokenAndType.getFirst().trim().toLowerCase();
			// drop dictionary words
			if (DictUtils.fullDictWords != null && DictUtils.fullDictWords.contains(token))
				continue;
			if (removeCommonNames && DictUtils.topNames != null && DictUtils.topNames.contains(token))
				continue;
			// drop "terms" with | -- often used as a separator on the web, and totally confuses our indexer's phrase lookup
			if (token.indexOf("|") >= 0)
				continue;
			tokenList.add(tokenAndType);
		}

		// initialize weights
		for (Pair<String, String> token : tokenList)
		{
			String normalizedTerm = token.getFirst().toLowerCase();
			termFreqMap.put(normalizedTerm, 0.0f);
		}

		int n_tokens = tokenList.size();

		int current_token_count = 0;
		// note: tokenList could have lower and upper case
		for (Pair<String, String> token : tokenList)
		{
			// if normalizing by length, each occurrence of a token has a weight of 1/tokenList.size()
			float basic_weight = (normalizeByLength) ? 1.0f / tokenList.size() : 1.0f;

			float positional_boost = 1.0f;
			if (max_positional_boost > 0)
			{
				// higher boost based on earlier position in doc
				positional_boost = (n_tokens - current_token_count) * max_positional_boost * 1.0f / n_tokens;
				current_token_count++;
			}

			// compute boost based on token type
			float token_type_boost = 1.0f;
			if (tokenTypeWeights != null)
			{
				String tokenType = token.getSecond();
				Integer X = tokenTypeWeights.get(tokenType);
				if (X != null)
					token_type_boost = (float) X;

				// tokens of length < 3 chars should not count
				String x = token.getFirst();
				if (x.length() <= 3)
					token_type_boost = 0.0f;
				else
				{
					int nTokens = new StringTokenizer(x).countTokens();
					// boost terms that have a space in them, 'cos they're less likely to be ambiguous
					if (nTokens > 1)
						token_type_boost *= nTokens;
				}
			}
			//@vihari: I dont think this is important
			positional_boost = 1.0f;
			float weight = basic_weight * positional_boost * token_type_boost;

			// multiplier should be between 1 and positional_boost
			String normalizedTerm = token.getFirst().toLowerCase();
			Float F = termFreqMap.get(normalizedTerm);
			termFreqMap.put(normalizedTerm, F + weight);
		}

		// reconstruct orgTermFreqMap from termFreqMap using the original terms instead of normalized terms
		Map<String, Float> orgTermFreqMap = new LinkedHashMap<String, Float>();
		for (Pair<String, String> token : tokenList)
		{
			String term = token.getFirst();
			String normalizedTerm = term.toLowerCase();
			if (termFreqMap.containsKey(normalizedTerm)) {
				orgTermFreqMap.put(term, termFreqMap.get(normalizedTerm));
				termFreqMap.remove(normalizedTerm);
			}
		}
		assert (termFreqMap.isEmpty());

		/*
		 * for(Pair<String,Float> entity: list){
		 * if(!namesAdded.contains(entity.getFirst())){
		 * names.add(entity);
		 * namesAdded.add(entity.getFirst());
		 * }
		 * }
		 */
		/*
		 * log.info("Names recognised:");
		 * for(Pair<String,Float> entity: names)
		 * log.info(entity.getFirst());
		 */
		//	for (Pair<String, Integer> p: list)
		//		log.info (p.getFirst() + " (" + p.getSecond() + " occurrence(s))");
		return Util.sortMapByValue(orgTermFreqMap);
	}

	// classifier.classifyToCharacterOffsets() is compute intensive
	public static String retainOnlyNames(String text)
	{
		try {
			NER.initialize();
		} catch (Exception e) {
			Util.print_exception(e, log);
		}

		String nerText = getSafeText(text);
		return retainOnlyNames(text, parseAndGetOffsets(nerText).getSecond());
	}

	public static String retainOnlyNames(String text, List<Triple<String, Integer, Integer>> offsets)
	{
		if (offsets == null)
			return retainOnlyNames(text); // be forgiving

		int len = text.length();
		offsets.add(new Triple<String, Integer, Integer>(null, len, len)); // sentinel
		StringBuilder result = new StringBuilder();
		int prev_name_end_pos = 0; // pos of first char after previous name
		for (Triple<String, Integer, Integer> t : offsets)
		{
			int begin_pos = t.second();
			int end_pos = t.third();
			if (begin_pos > len || end_pos > len) {
				// TODO: this is unclean. currently happens because we concat body & title together when we previously generated these offsets but now we only have body.
				begin_pos = end_pos = len;
			}

			if (prev_name_end_pos >= begin_pos) // something strange, but possible - the same string can be recognized as multiple named entity types
				continue;
			String filler = text.substring(prev_name_end_pos, begin_pos);
			//filler = filler.replaceAll("\\w", "."); // CRITICAL: \w only matches (redacts) english language
			filler = filler.replaceAll("[^\\p{Punct}\\s]", ".");
			result.append(filler);
			result.append(text.substring(begin_pos, end_pos));
			prev_name_end_pos = end_pos;
		}

		return result.toString();
	}

	public static int countNames(String text) {
		List<Triple<String, Integer, Integer>> triples = parseAndGetOffsets(text).getSecond();
		return triples.size();
	}

	static void trainForLowerCase()
	{
		countNames("rishabh has so far not taken to drama, so it was wonderful to see him involved and actively participating");
		countNames("rishabh has so far not taken to drama, so it was wonderful to see him involved and actively participating");
		countNames("rishabh has so far not taken to drama, so it was wonderful to see him involved and actively participating");
		countNames("rishabh has so far not taken to drama, so it was wonderful to see him involved and actively participating");
		countNames("rishabh has so far not taken to drama, so it was wonderful to see him involved and actively participating");
		countNames("rishabh has so far not taken to drama, so it was wonderful to see him involved and actively participating");
		countNames("rishabh has so far not taken to drama, so it was wonderful to see him involved and actively participating");
		countNames("rishabh has so far not taken to drama, so it was wonderful to see him involved and actively participating");
	}

	public static void main2(String args[]) throws IOException, ClassNotFoundException {
		String text = "Rishabh has so far not taken to drama, so it was wonderful to see him involved and actively participating. ";
		boolean normalizeByLength = false;

		System.err.println("Started reading");
		long start_time = System.currentTimeMillis();
		List<String> names = new ArrayList<String>();
		HashSet<String> namesAdded = new HashSet<String>();
		long end_time = System.currentTimeMillis();
		for (String name : names) {
			if (!namesAdded.contains(name)) {
				System.out.println(name);
				namesAdded.add(name);
			}
		}
		System.err.println("Reading done in " + (end_time - start_time));
		start_time = System.currentTimeMillis();

		List<Pair<String, Float>> entities = namesFromText(text, true, NER.defaultTokenTypeWeights, normalizeByLength, 1);
		end_time = System.currentTimeMillis();
		System.err.println("Done name recognition in: " + (end_time - start_time));

		for (Pair<String, Float> name : entities) {
			System.out.println(name.getFirst() + " : " + name.getSecond());
		}
	}

	public static void main1(String args[]) throws IOException {
		String s = Util.readFile("/tmp/fb.txt");
		System.out.println("Finished reading file");
		long m = System.currentTimeMillis();
		System.out.println(countNames(s) + " names");
		System.out.println(System.currentTimeMillis() - m + "ms");
	}

	static int	count	= 0, hitCount = 0;

	public static void printSentencesWithWords(String text, Set<String> wordsToSearch) throws IOException
	{
		//		InputStream SentStream = Config.getResourceAsStream("models/en-sent.bin");
		//		SentenceModel model = sFinder; // new SentenceModel(SentStream);
		//		SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
		String[] splitTexts = sFinder.sentDetect(text); // sentenceDetector.sentDetect(text);

		for (int i = 0; i < splitTexts.length; i++) {
			String orig_s = splitTexts[i];
			if (orig_s == null)
				continue;
			count++;
			String s = orig_s.toLowerCase();
			List<String> tokens = Util.tokenize(s);
			for (String tok : tokens)
				if (wordsToSearch.contains(tok))
				{
					hitCount++;
					System.out.println(hitCount + ". " + orig_s.replaceAll("\n", " -> "));
					break;
				}
		}
	}

	public static void printStats() {
		System.out.println("count = " + count + " hitcount = " + hitCount);
	}

    public static void testOpenNLP() {

        try {
            String s = Util.readFile("/tmp/in");
			/*
			List<Pair<String,Float>> pairs = NER.namesFromText(s);
			for (Pair<String,Float> p: pairs) {
				System.out.println (p);
			}
			System.out.println ("-----");
			*/

            InputStream pis = Config.getResourceAsStream("en-ner-person.bin");
            TokenNameFinderModel pmodel = new TokenNameFinderModel(pis);
            InputStream lis = Config.getResourceAsStream("en-ner-location.bin");
            TokenNameFinderModel lmodel = new TokenNameFinderModel(lis);
            InputStream ois = Config.getResourceAsStream("en-ner-organization.bin");
            TokenNameFinderModel omodel = new TokenNameFinderModel(ois);
            InputStream tokenStream = Config.getResourceAsStream("en-token.bin");
            TokenizerModel modelTokenizer = new TokenizerModel(tokenStream);
            TokenizerME tokenizer = new TokenizerME(modelTokenizer);
            Span[] tokSpans = tokenizer.tokenizePos(s); // Util.tokenize(s).toArray(new String[0]);

            String tokens[] = new String[tokSpans.length];
            for (int i = 0; i < tokSpans.length; i++)
                tokens[i] = s.substring(tokSpans[i].getStart(), tokSpans[i].getEnd());

            NameFinderME pFinder = new NameFinderME(pmodel);
            Span[] pSpans = pFinder.find(tokens);
            NameFinderME lFinder = new NameFinderME(lmodel);
            Span[] lSpans = lFinder.find(tokens);
            NameFinderME oFinder = new NameFinderME(omodel);
            Span[] oSpans = oFinder.find(tokens);
            System.out.println("Names found:");
            for (Span span : pSpans) {
                for (int i = span.getStart(); i < span.getEnd(); i++)
                    System.out.print(tokens[i] + " ");
                System.out.println();
            }

            System.out.println("Locations found:");
            for (Span span : lSpans) {
                for (int i = span.getStart(); i < span.getEnd(); i++)
                    System.out.print(tokens[i] + " ");
                System.out.println();
            }

            System.out.println("Orgs found:");
            for (Span span : oSpans) {
                for (int i = span.getStart(); i < span.getEnd(); i++)
                    System.out.print(tokens[i] + " ");
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public static void main(String[] args) {
		try {
			String text = "Sherlock Holmes and Dr. Watson are good friends and solved many cases together. Moriarty is their arch-rival and a professor. The book was written by Canon Doyle";
			Pair<MyTokenizer, List<Triple<String, Integer, Integer>>> o = NER.parseAndGetOffsets(text);
			for (Triple<String, Integer, Integer> t : o.getSecond())
				System.err.println(t);

			String userDir = System.getProperty("user.home") + File.separator + "epadd-appraisal" + File.separator + "user";
			userDir = Config.SETTINGS_DIR + File.separator + "ePADD archive of Unknown";
			Archive archive = SimpleSessions.readArchiveIfPresent(userDir);
			//List<Document> docs = archive.getAllDocs();
			String[] dIds = new String[] { "/Users/viharipiratla/epadd-settings/palin.mbox-1090" };//"/home/hangal/data/creeley/1/Mail[447610]-310" };//"/home/hangal/data/creeley/1/MAIL[96410]-990", "/home/hangal/data/creeley/1/LERNER.000-0" };
			//"/home/hangal/data/creeley/1/Mail[447842]-206", "/home/hangal/data/creeley/1/YAU.003-0", " /home/hangal/data/creeley/1/YAU.003-0" };//, "/home/hangal/data/creeley/1/INBOX[325606]-6", "/home/hangal/data/creeley/1/INBOX[325606]-10" };
			//Random randnum = new Random();
			Indexer li = archive.indexer;
			for (String dId : dIds) {
				Document doc = archive.docForId(dId);
				org.apache.lucene.document.Document ldoc = li.getDoc(doc);
				System.err.println(ldoc.getBinaryValue("names_offsets"));
				//System.err.println(ldoc.get("body_redacted"));
				//System.err.println("\n---------------------------\n");
				//System.err.println(NER.retainOnlyNames(li.getContents(doc, false), Indexer.getNamesOffsets(ldoc)));
				//	System.err.println(NER.retainOnlyNames(li.getContents(doc, false)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
