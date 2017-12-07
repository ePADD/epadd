package edu.stanford.muse.ie;

import com.google.gson.Gson;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.util.DictUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.webapp.SimpleSessions;
import opennlp.tools.util.featuregen.FeatureGeneratorUtil;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.Version;

import java.io.File;
import java.util.*;

/**
 * A representative class for candidate entity. Just like a contact in
 * address book, will merge many different names and contains some other pre-processing information.
 *
 * This class is used nowhere and is for only experimentation purposes
 * This is an attempt to merge all the coreferring noun phrase mentions into clusters; The algorithm used here is naive and mostly adapted from a paper
 * TODO: Whats the title?
 */
public class Entity extends EntityFeature {
	private static final long	serialVersionUID	= -6930196836983330301L;
	Set<String>					names				= new HashSet<String>();
	//These are all the co-occurring entities, and those that are not used for closeness measure.
	Map<String, Integer>		allCE				= new HashMap<String, Integer>();
	Map<String, Boolean>		extra				= new HashMap<String, Boolean>();
	//default time quantization - day
	public Map<Date, Integer>	timeHistogram		= new HashMap<Date, Integer>();
	//in address book?
	boolean						inAddressBook		= false;
	//#times referred back in the replay of the message.
	int							numReferBack		= 0;
	//prior probability that denotes the entityness score of this phrase, prior is calculated based on some of the signals above.
	double						prior				= 0.0;
	double						entropyCE			= -1, entropyEA = -1, entropyTime = -1;
	Set<String>					people				= new HashSet<String>();
	boolean						namelike			= false;
	//number of times this sentence appeared in the start of sentence.
	int							sentstart			= 0;
	//case insensitive frequency
	int							cifreq				= 0;

	static String				ENTITIES			= "entities";
	static Set<String>			sws					= new HashSet<String>();
	static EnglishAnalyzer		en_an				= new EnglishAnalyzer(Version.LUCENE_CURRENT);
	static QueryParser			parser				= new QueryParser(Version.LUCENE_CURRENT, "some_field", en_an);

	static {
		String[] temp = edu.stanford.muse.util.Util.stopwords;
		for (String t : temp)
			sws.add(t);
	}

	public Entity(String name) {
		this.names = new HashSet<String>();
		names.add(name);
		this.cooccuringEntities = new HashMap<String, Integer>();
		this.emailAddresses = new HashMap<String, Integer>();
		this.allCE = new HashMap<String, Integer>();
	}

	public static <K> void putAll(Map<K, Integer> target, Map<K, Integer> source) {
		if (source == null)
			return;
		for (K src : source.keySet()) {
			if (!target.containsKey(src))
				target.put(src, 0);
			target.put(src, source.get(src) + target.get(src));
		}
	}

	public void merge(Entity e) {
		if (e == null)
			return;
		names.addAll(e.names);
		putAll(cooccuringEntities, e.cooccuringEntities);
		putAll(allCE, e.allCE);
		putAll(emailAddresses, e.emailAddresses);
		putAll(timeHistogram, e.timeHistogram);
		this.inAddressBook = this.inAddressBook || e.inAddressBook;
		this.numReferBack += e.numReferBack;
		this.freq += e.freq;
	}

	public static boolean isDictionaryWord(String word) {
		word = word.toLowerCase();
		return DictUtils.fullDictWords.contains(word) && !sws.contains(word);
	}

	//checks if an entity is good enough to score over it.
	public static boolean good(String e) {
		if (e == null)
			return false;
		if (e.matches("\\bI\\b"))
			return false;
		String[] words = e.split("\\s+");
		for (String w : words)
			if (isDictionaryWord(w))
				return false;
		return true;
	}

	@Override
	public Document getIndexerDoc() {
		Document doc = new Document();
		Field nameF = null, probF = null, typeF = null;

		if (name != null) {
			nameF = new StringField(NAME, name, Field.Store.YES);
			doc.add(nameF);
		}

		typeF = new StringField(TYPE, type + "", Field.Store.YES);
		doc.add(typeF);

		probF = new StringField(PP, this.priorProbablity + "", Field.Store.YES);
		doc.add(probF);

		Gson gson = new Gson();
		if (this.cooccuringEntities != null) {
			String cEs = gson.toJson(this.cooccuringEntities);
			doc.add(new StringField(CES, cEs, Field.Store.YES));
		}
		if (this.emailAddresses != null) {
			String eAs = gson.toJson(this.emailAddresses);
			doc.add(new StringField(EAS, eAs, Field.Store.YES));
		}
		if (this.allCE != null) {
			String entities = gson.toJson(this.allCE);
			doc.add(new StringField(ENTITIES, entities, Field.Store.YES));
		}
		return doc;
	}

	public Entity getExactMatch(String str, Map<String, Entity> features) {
		for (String en : features.keySet())
			if (str.equals(en.toLowerCase()))
				return features.get(en);
		return null;
	}

	public List<Pair<Entity, Double>> scoreMatches(Map<String, Entity> candidates, Map<String, Entity> features) {
		//String c = "Elisa Berslein";
		//System.err.println("Features contain: " + c + "\t" + features.containsKey(c));
		Collection<Entity> efts = candidates.values();
		EntityFeature ef = this;
		//System.err.println("Size of EA's: " + ef.emailAddresses.size() + " CE's: " + ef.cooccuringEntities.size());
		List<Pair<Entity, Double>> scores = new ArrayList<Pair<Entity, Double>>();

		/////
		Map<Entity, Double> scoresU = new HashMap<Entity, Double>();
		Map<String, Integer> CE = ef.cooccuringEntities, EA = ef.emailAddresses;
		//P(CE) and P(EA);
		double EAMod = 0, CEMod = 0;
		for (String ea : EA.keySet())
			EAMod += Math.pow(EA.get(ea), 2);
		for (String ce : CE.keySet())
			CEMod += Math.pow(CE.get(ce), 2);
		EAMod = Math.sqrt(EAMod);
		CEMod = Math.sqrt(CEMod);

		for (Entity eft : efts) {
			//String allnames = "";
			Map<String, Integer> targetCE = eft.cooccuringEntities;

			Map<String, Integer> targetEA = eft.emailAddresses;
			double cScore = 0, eScore = 0;

			double targetEAMod = 0, targetCEMod = 0;
			for (String ea : targetEA.keySet())
				targetEAMod += Math.pow(targetEA.get(ea), 2);
			for (String ce : targetCE.keySet())
				targetCEMod += Math.pow(targetCE.get(ce), 2);
			targetEAMod = Math.sqrt(targetEAMod);
			targetCEMod = Math.sqrt(targetCEMod);

			for (String ea : EA.keySet()) {
				if (targetEA.containsKey(ea)) {
					if ((targetEA.get(ea) > 0)) {//&& (!ownAddr.contains(ea))) {
						//	System.err.println("Scoring with ea: " + ea);
						eScore += targetEA.get(ea);
					}
				}
			}

			for (String ce : CE.keySet()) {
				if (targetCE.containsKey(ce)) {
					if (targetCE.get(ce) > 0) {
						Entity efce = getExactMatch(ce, features);
						if (efce == null) {
							continue;
						}
						cScore += (targetCE.get(ce) / efce.priorProbablity);
					}
				}
			}
			cScore /= (targetCEMod);
			eScore /= (targetEAMod);

			// csore is based on cooccuring entities and escore is based on email
			// addresses.
			double score = 0.5 * cScore + 0.5 * eScore;
			//this is more important than one would imagine
			score *= eft.priorProbablity;
			if (score != 1)
				scoresU.put(eft, score);
			//String thisnames = "";
			//			for (String n : this.names)
			//				thisnames += n + " ";
			//System.err.println(allnames + ", score: " + (score) + ", cScore: " + cScore + ", eScore: " + eScore + " on scoring with " + thisnames);
		}
		scores = edu.stanford.muse.util.Util.sortMapByValue(scoresU);
		if (scores != null && scores.size() > 0) {
			double max_score = scores.get(0).second;
			if (max_score > 0)
				for (Pair<Entity, Double> e : scores)
					e.second /= max_score;
		}
		return scores;
	}

	/**
	 * Should also be able to handle phrases like: NY Times
	 */
	public static String getAcronym(String phrase) {
		String[] words = phrase.split("\\s+");
		String acr = "";
		for (String word : words) {
			for (int i = 0; i < word.length(); i++) {
				char c = word.charAt(i);
				if (c >= 'A' && c <= 'Z')
					acr += c;
				else
					break;
			}
		}
		return acr;
	}

	//should also get phrases smaller in length and also those of same size.
	//check for ancronym if the name is all caps or if there are more than one word in the string
	static Map<String, Entity> getMatches(String name, Map<String, Entity> features) {
		Map<String, Entity> res = new HashMap<String, Entity>();
		if (name == null)
			return res;
		String tc = FeatureGeneratorUtil.tokenFeature(name);
		boolean checkAcronym = false;
		if (tc.equals("ac"))
			checkAcronym = true;
		String[] words = name.split("\\s+");
		if (words.length > 1)
			checkAcronym = true;

		String acronym = getAcronym(name);
		if (checkAcronym) {
			for (String str : features.keySet()) {
				String acr = getAcronym(str);
				if (acronym.equals(acr))
					res.put(str, features.get(str));
			}
		}
		return res;
	}

	public static Map<String, Entity> merge(Map<String, Entity> features) {
		Set<Entity> considered = new HashSet<Entity>();
		double THRESHOLD = 0.5;
		Map<String, Entity> fmerged = new HashMap<String, Entity>();
		int i = 0;
		for (String name : features.keySet()) {
			Map<String, Entity> matches = getMatches(name, features);
			Entity e = features.get(name);
			List<Pair<Entity, Double>> scores = e.scoreMatches(matches, features);
			if (considered.contains(e))
				continue;

			for (Pair<Entity, Double> me : scores) {
				double d = me.second;
				if (d > THRESHOLD) {
					e.merge(me.first);
					considered.add(me.first);
				}
			}

			fmerged.put(name, features.get(name));
			System.err.println("Done merging: " + (++i) + "/" + features.size());
			//System.out.println(name);
		}
		return fmerged;
	}

	/** Cleans a name recognised from pattern */
	public static String clean(String name) {
		if (name == null)
			return name;
		String[] frags = name.split(" and ");
		//actually both the fragments should be considered.
		String cn = frags[0];
		cn = cn.replaceAll("(^\\W+|\\W+$)", "");
		//'s
		cn = cn.replaceAll("'s$", "");
		return cn;
	}

	//call this b4 merging.
	public static Map<String, Entity> clean(Map<String, Entity> features) {
		Map<String, Entity> nf = new HashMap<String, Entity>();
		for (String str : features.keySet()) {
			String ns = clean(str);
			Set<String> names = new HashSet<String>();
			names.add(ns);
			Entity e = features.get(str);
			e.names = names;
			nf.put(ns, e);
		}
		return (nf);
	}

	public static void main(String[] args) {
		try {
			Archive archive = SimpleSessions.readArchiveIfPresent(System.getProperty("user.home") + File.separator + "epadd-appraisal" + File.separator + "user-creeley");

			//			Indexer li = archive.indexer;
			//			String q = "\"Holly Tavel\"";
			//			Collection<EmailDocument> docs = li.lookupDocs(q);
			//			System.out.println("hits for: " + q + " = " + docs.size());
			//checkIndex1(archive, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String content = "Abraham Lincoln, Somnath\nChandra Bose, Helooiuh, Hhayi JJh, Jfhg. \nThanks, \nBob";
		//		Set<Set<String>> els = getEntityListings(content);
		//		for (Set<String> el : els)
		//			System.err.println(el);
		//		String[] strs = content.split("[\\s,]+");
		//		for (String str : strs)
		//			System.err.println(str + ";;;");
		//		String[] names = new String[] { "Program Solicitation: NSF",
		//				"Program Notice:",
		//				"NIA: NIA Pilot Research Grant Program",
		//				"Program Sorting Code:",
		//				"DOE: Inventions and Innovation Program",
		//				"Research Enhancement Support Program",
		//				"ASEE: Fellowship Program",
		//				"Young Investigator Grant Program",
		//				"Sponsored Research Infrastructure Program" };
		//		Map<String, Entity> features = new HashMap<String, Entity>();
		//		for (String name : names)
		//			features.put(name, new Entity(name));
		//		merge(features);
		//System.err.println("Acr: " + getAcronym("New York Times"));
	}
}
