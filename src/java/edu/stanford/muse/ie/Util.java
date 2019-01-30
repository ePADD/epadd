package edu.stanford.muse.ie;

/**
 * @author viharipiratla
 */
import edu.stanford.muse.ner.featuregen.FeatureUtils;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Span;
import opennlp.tools.util.featuregen.FeatureGeneratorUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains IE related util funtions and fields
 */
public class Util {
	private static final int			TIMEOUT		= 10000;

	/**
	 * @param context
	 *            from the personal archive
	 * @return score of the Wikipedia page and summary text of entities upon
	 *         which the Wikipedia page is scored.
	 * @bug revision: original name should not contribute to the score.
	 * @bug this method only looks for two consecutive words in a phrase, for
	 *       example for the phrase: (expansion of NHPRC) it only looks at
	 *       combinations: NH, HP, PR, RC, hence counting it 4 times instead of
	 *       once. The reason for a matching method like this is: it captured
	 *       more variant names and hence pushed the best matches further while
	 *       matching.
	 */
	public static Pair<String, Double> scoreWikiPage(String wikiLink, String[] context) {
		if (wikiLink == null)
			return null;
		System.err.println("Fetching page: " + wikiLink);
		System.err.println("Num terms in context: " + context.length);
		Document wikiDoc = null;
		try {
			wikiDoc = Jsoup.parse(new URL(wikiLink), TIMEOUT);
		} catch (Exception e) {
			System.err.println("Timeout or exception while fetching wiki page: " + wikiLink);
			e.printStackTrace();
			return null;
		}

		String topic = wikiLink.replaceAll("http://en.wikipedia.org/wiki/", "").replaceAll("_", "");
		String[] topicArr = topic.split(" ");
		List<String> topicWords = new ArrayList<>();
		for (String t : topicArr)
			topicWords.add(t.toLowerCase());

		String mainHtml = wikiDoc.html();
		//remove stuff in references, external links or anything after it.
		Elements tmpE = wikiDoc.select("#External_links");
		tmpE.addAll(wikiDoc.select("#References"));

		int scrapPos = Integer.MAX_VALUE;
		for (Element aTmpE : tmpE) {
			scrapPos = Math.min(scrapPos,
					mainHtml.indexOf(aTmpE.outerHtml()));
			System.err.println("pos of: " + aTmpE.html()
					+ " is "
					+ mainHtml.indexOf(aTmpE.outerHtml()));
		}

		double s = 0;
		String text = wikiDoc.text();
		Set<String> matches = new HashSet<>();
		Map<String, Set<String>> crosscheckWords = new HashMap<>();

		for (String c : context) {
			String[] words = c.split("\\s+");
			Set<String> tokens = new HashSet<>();
			//word.tolowercase does bad or good on performance (accuracy)?
			for (String word : words)
				tokens.add(word.toLowerCase());
			for (String token : tokens)
				crosscheckWords.put(token, tokens);
		}
		List<String> sws = Arrays.asList("but", "be", "with", "such", "then", "for", "no", "will", "not", "are", "and", "their", "if", "this", "on", "into", "a", "there", "in", "that", "they", "was", "it", "an", "the", "as", "at", "these", "to", "of" );

		//		//O(#words) in wikipage
		String[] textLines = text.split("\\.|\\n|<br>");
		for (String textLine : textLines) {
			String[] textTokens = textLine.split("\\s+");
			for (int ti = 0; ti < textTokens.length; ti++) {
				String t = textTokens[ti].toLowerCase();
				if (edu.stanford.muse.util.Util.nullOrEmpty(t) || topicWords.contains(t))
					continue;
				if (crosscheckWords.keySet().contains(t)) {
					String prevToken = ti > 0 ? textTokens[ti - 1].toLowerCase() : "";
					String nextToken = ti < (textTokens.length - 1) ? textTokens[ti + 1].toLowerCase() : "";

					if (crosscheckWords.get(t).contains(prevToken)) {
						//int idx = mainHtml.indexOf(prevToken + " " + t);
						//if (idx < scrapPos)
						if (filterEntity(prevToken + " " + t) && !sws.contains(prevToken) && !sws.contains(t))
							matches.add(EmailUtils.uncanonicaliseName(prevToken + " " + t));
					}
					else if (crosscheckWords.get(t).contains(nextToken) && !sws.contains(nextToken) && !sws.contains(t)) {
						//int idx = mainHtml.indexOf(t + " " + nextToken);
						//if (idx < scrapPos)
						if (!filterEntity(t + " " + nextToken))
							matches.add(EmailUtils.uncanonicaliseName(t + " " + nextToken));

					}
				}
			}
		}

		s = matches.size();
		//considering 1- or more matches to be a perfect score.
		int k = 0;
		String matchStr = "";
		for (String match : matches) {
			matchStr += match;
			if (k < matches.size() - 2)
				matchStr += ", ";
			else if (k == matches.size() - 2)
				matchStr += " and ";
			k++;
		}

		return new Pair<>(matchStr, s);
	}

	/**
	 * The red-green bar that reflects the confidence.
	 * 
	 * @args score is relative and should be <=1, title of the div element.
	 */
	public static String getConfidenceBar(double s, String title) {
		int r = new Double(255 * (1 - s)).intValue(), g = new Double(255 * s).intValue();
		String color = "rgb(" + r + "," + g + ",20)";
		return "<div style='width: " + (new Double(100 * s).intValue() + 10) + "px; background-color:" + color + ";display: inline-block;height: 5px;' title='" + title + "'> </div>";
	}

	public static List<Pair<String, Pair<Double, Double>>> sortMapByValue(Map<String, Pair<Double, Double>> map) {
		List<Pair<String, Pair<Double, Double>>> l = new ArrayList<>();
		for (String str : map.keySet()) {
			l.add(new Pair<>(str, map.get(str)));
		}

		l.sort((x, y) -> {
			double d1 = x.getSecond().first, d2 = y
					.getSecond().first;
			if (d1 == d2) {
				d1 = x.getSecond().second;
				d2 = y.getSecond().second;
				if (d1 == d2)
					return 0;
				else
					return (d1 < d2 ? 1 : -1);
			}
			return (d1 < d2 ? 1 : -1);
		});
		return l;
	}

	//TODO: ideally kill phrases class should be doing all this.
	private static final Log						log								= LogFactory.getLog(Util.class);
	private static final Map<String, Set<String>>	files							= new LinkedHashMap<>();
	//order person, location, org
	private static final String[]					TYPE_SPECIFIC_COMMON_WORDS_FILE	= new String[] { edu.stanford.muse.Config.SETTINGS_DIR + File.separator + "per-kill.txt", edu.stanford.muse.Config.SETTINGS_DIR + File.separator + "loc-kill.txt", edu.stanford.muse.Config.SETTINGS_DIR + File.separator + "org-kill.txt" };

	private static Set<String> readFile(String fileName) {
        if (files.containsKey(fileName))
            return files.get(fileName);
        try {
			InputStream in = new FileInputStream(fileName);
            return readFile(in, fileName);
		} catch (Exception e) {
            files.put(fileName, new LinkedHashSet<>());
            log.info("Did not find the common words file: " + fileName);
			return new LinkedHashSet<>();
		}
	}

    private static Set<String> readFileFromResource(String fileName) {
        if (files.containsKey(fileName))
            return files.get(fileName);
        try {
            InputStream in = Util.class.getClassLoader().getResourceAsStream(fileName);
            return readFile(in, fileName);
        } catch (Exception e) {
            files.put(fileName, new LinkedHashSet<>());
            log.info("Did not find the common words file: " + fileName);
            return new LinkedHashSet<>();
        }
    }

    private static Set<String> readFile(InputStream in, String fileName){
        if (files.containsKey(fileName))
            return files.get(fileName);
        Set<String> words = new LinkedHashSet<>();
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase();
                words.add(line);
            }
            br.close();
            log.info("Read "+words.size()+" from "+fileName);
            files.put(fileName, words);
            return words;
        }catch(Exception e){
            files.put(fileName, words);
            return words;
        }
    }


    private static boolean filterEntity(String e){
        return filterEntity(e, (short)-1);
    }

    private static boolean filterEntity(Span s){
        return filterEntity(s.getText(),s.type);
    }

	/**
	 * Filters any entity that does not look like one
	 * returns true if the entity looks OK and false otherwise, type can be null if it is of unknown type
     */
	private static boolean filterEntity(String e, short type) {
		if (e == null)
			return false;

		int ti = 0;
		if (NEType.Type.PERSON.getCode() == type)
			ti = 0;
		else if (NEType.Type.PLACE.getCode() == type)
			ti = 1;
		else if (NEType.Type.ORGANISATION.getCode() == type)
			ti = 2;
		Set<String> cws = readFileFromResource("dict.words.full");
		Set<String> tcws = new HashSet<>();
		if (ti <= 2 && ti >= 0)
			tcws = readFile(TYPE_SPECIFIC_COMMON_WORDS_FILE[ti]);

		boolean inDict = false;
		if (cws.contains(e.toLowerCase()) || tcws.contains(e.toLowerCase()))
			inDict = true;
        String lc = e.toLowerCase();
        boolean allDict = false;
        List<String> noise = Arrays.asList("jan","feb","mar","apr","jul","aug","oct","nov","dec","sun","mon","tue","wed","thur","fri","sat","thu");
        {
            String[] words = lc.split("\\s+");
            int numDict = 0;
            for(String word: words) {
                if (noise.contains(word)) {
                    inDict = true;
                    break;
                }
                if(cws.contains(word.toLowerCase()))
                    numDict++;
            }
            if(numDict == words.length)
                allDict = true;
            if(allDict)
                return false;
        }

		//for orgs reject the entity if it is all caps or is in common words dictionary
		if (ti == 2) {
			if (inDict)
				return false;
			String[] words = e.split("\\s+");
			//check for any known patterns
			//drop it if contains I
			boolean containsKP = false;
			for (String word : words)
				if (word.equals("I") || word.equals("In") || word.equals("August") || word.equals("December") || word.equals("January")
						|| (word.equals("February") || word.equals("March") || word.equals("April") || word.equals("May")
								|| word.equals("June") || word.equals("July") || word.equals("September") || word.equals("October") || word.equals("November")
								|| word.equals("As") || word.equals("On") || word.equals("will") || word.equals("if")) || word.equals("At")
						|| word.equals("But") || word.equals("By") || word.equals("If") || word.equals("as") | word.equals("When") || word.equals("With"))
					containsKP = true;
			if (e.contains("City of") || e.contains("As a") || e.contains("As an")|| e.toLowerCase().contains("original message"))
				containsKP = true;

			if (containsKP)
				return false;

			boolean ac = true;
			for (String word : words) {
				String wc = FeatureGeneratorUtil.tokenFeature(word);
				if (!wc.equals("ac")) {
					ac = false;
					break;
				}
			}
			if (ac || inDict)
				return false;
		}

		return !inDict;
	}

	public static Span[] filterEntities(Span[] entities) {
		return Stream.of(entities).filter(Util::filterEntity).toArray(Span[]::new);
	}

	public static Span[] filterEntitiesByScore(Span[] entities, double threshold) {
		return Stream.of(entities).filter(e -> e.typeScore > threshold).toArray(Span[]::new);
	}

	public static void main(String[] args) {
//		String[] context = new String[] { "ivan allen college", "art history/fine arts", "general accounting office", "asia minor", "barbara bono", "aauw legal advocacy fund progress", "behar", "house", "ny", "department of labor", "sex equity awards", "dos", "andreas baader", "center", "pakistan", "hillary clinton", "natural sciences", "suzanne laychock", "new look", "barbara lazarus", "rainer werner fassbinder", "graduate student symposium on gender state university of new york", "peter cloos", "asm international", "electronic packaging", "national humanities center", "pratt institute", "science &amp; tech stubborn equation keeps women", "hirschhorn museum", "greece every", "aauw", "gao", "rolling stone", "irewg distinguished women speakers series &amp; modern languages", "brooklyn", "buffalo state college", "red army faction", "america", "john dingell editor", "bosmat alon", "allen college", "medical strategies", "allen hall", "department of classics", "block", "center for the arts cas spring lecture series alexis deveaux", "william egginton", "atlantic", "institute for research and education", "daemen college", "spring", "new mexico", "center for the study of diversity", "block de behar", "dawn bracely", "harold levy", "carolyn maloney", "student union student activism fair", "india", "elaine proctor britain/south africa", "buffalo", "arcade film &amp; arts centre", "mechanical &amp; aerospace engineering wed", "deborah chung at", "gross domestic product", "state college", "earth", "cleveland", "national academy of engineering", "senate campaign", "lisa block de behar", "lgbt alliance", "senate", "stories", "technology international hall of fame", "film fest", "irewg networking lunch", "western reserve university", "main st.", "block de", "arcade film &amp; arts centre int", "south africa", "hillary rodham clinton", "quick", "school of social work", "center for the arts irewg annual distinguished faculty lecture susan cole", "associated press", "kent state university", "surface mount technology association", "irewg", "h-technologies group", "center for the arts zodiaque dance company", "aauw legal advocacy fund", "pie", "women", "rekha menon", "quick list/aauw sex equity awards", "shea", "center for the arts" };
//		String wikiUrl = "http://en.wikipedia.org/wiki/Washington,_D.C.";
//		//String[] context = new String[] { "joseph m. conte", "bruce jackson", "barbara bono", "stanford", "ed dorn", "search", "assistant marshall dear joseph-- i", "research institute on addictions phone", "neil mcgillicuddy", "angelica huston", "center for tomorrow", "buffalo buffalo", "hollywood", "christina ricci", "student union", "new york state health insurance program", "robert creeley", "february", "harvard", "sarah campbell", "albright-knox art gallery", "chair department of english suny", "wisconsin", "department of english suny", "new mexico", "bob creeley", "suny", "nyperl", "search meeting", "professional staff senate", "gene jarrett", "gallery", "washington street", "roger livesey", "vincent gallo", "anna kedzierski", "dundon", " liz", "joseph conte", "robert duncan", "bono sent", "ub", "barbara christy", "arcade", "red cross campus blood drives", "medical examination", "ezra pound", "mafac", "ben gazarra", "mexico", "stahura", " glenda", "thursday", "loss pequeño glazier", "anna maria kedzierski assistant", "senate", "hershini bhana", "care insurance program", "assistant marshall dear colleagues", "new york state public employee", "classics", "st louis", "cas", "betty stone", "ub blood drive committee", "second call", "department commencement", "arcade film and arts center", "employee assistance program", "chris salem", "rd", "ub medical school", "al gelpi", "england", "stanford univ.", "research university", "new york state", "public high school", "confidential", "service center road", "employee assistance program ub blood drive committee list", "national science foundation", "kristin herman", "new york times photo archives", "ub english department list", "maine", "paramount", "occupational &amp; environmental safety phone", "agency", "university online directory", "retiree long-term care insurance plan", "new york", "jonathan skinner", "marilyn kramer", "assistant marshall", "civil service", "assistant marshalls", "richard lobaugh", "you.", "university commencement", "tom raworth" };
//		//String wikiUrl = "http://en.wikipedia.org/wiki/index.html?curid=3985";
//		Pair<String, Double> res = Util.scoreWikiPage(wikiUrl, context);
//		System.err.println("The score is: " + res.second + "<br>");
//		System.err.println(Util.getConfidenceBar(res.second, res.first));
        System.err.println(filterEntity("On Feb"));
	}

    /**
     * Should also be able to handle phrases like: NY Times and UC Santa Barbara
     * @param considerStopWords also considers stop words in the phrase as part of acronym
     * */
    public static String getAcronym(String phrase, boolean considerStopWords) {
        String[] words = phrase.split("\\s+");
        String acr = "";
        for (String word : words) {
            if(FeatureUtils.sws.contains(word) && considerStopWords) {
                acr += word.charAt(0);
                continue;
            }
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if ((c >= 'A' && c <= 'Z') || c=='.')
                    acr += c;
                else
                    break;
            }
        }
        return acr;
    }

    public static String getAcronym(String phrase){
        return getAcronym(phrase, false);
    }
}
