package edu.stanford.muse.ie;

import com.google.gson.Gson;
import edu.stanford.muse.email.Contact;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.index.IndexUtils;
import edu.stanford.muse.index.Indexer;
import edu.stanford.muse.ner.tokenize.CICTokenizer;
import edu.stanford.muse.ner.tokenize.Tokenizer;
import edu.stanford.muse.util.DictUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.SimpleSessions;
import opennlp.tools.util.featuregen.FeatureGeneratorUtil;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.Version;

import javax.mail.Address;
import java.io.File;
import java.io.FileWriter;
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

	/**
	 * entityListings -- entities that appear in listings togather with this entity.
	 * start - appearing at the strt of the sentence?
	 */
	public void addAllEntities(Collection<String> entities, Set<String> entityListings, boolean start) {
		if (start)
			this.sentstart++;

		for (String e : entities) {
			if (!allCE.containsKey(e))
				allCE.put(e, 0);
			allCE.put(e, allCE.get(e) + 1);
			if (entityListings.contains(e))
				extra.put(e, true);
			else
				extra.put(e, false);

		}
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
	 * email document in which this entity is found and the email content
	 */
	public void accountForThis(EmailDocument ed, String content, Set<String> history) {
		this.freq++;
		//System.err.println("Accounting for: " + content);

		Address[] from = ed.from;
		if (from != null)
			for (Address fa : from) {
				this.people.add(fa.toString());
			}

		int numRefer = 0;
		for (String h : history)
			for (String name : names)
				if (h.contains(name)) {
					numRefer++;
					break;
				}
		this.numReferBack = numRefer;
		Date d = ed.date;
		if (!timeHistogram.containsKey(d))
			timeHistogram.put(d, 0);
		this.timeHistogram.put(d, this.timeHistogram.get(d) + 1);
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

	public static <K> double getEntropy(Map<K, Integer> map) {
		if (map == null)
			return 0;
		double entropy = 0;
		int tc = 0;
		for (K ce : map.keySet())
			tc += map.get(ce);
		//-sigma(p(x)*log(p(x)))
		for (K ce : map.keySet()) {
			double p = (double) map.get(ce) / (double) tc;
			//is just 0 if p=0
			if (p > 0)
				entropy += p * Math.log(p);
		}
		return -entropy;
	}

	public static void computePriors(Map<String, Entity> features, Archive archive, int numDocs) {
		System.err.println("Computing priors");
		int numContacts = archive.addressBook.allContacts().size();
		List<Contact> contacts = archive.addressBook.allContacts();
		int numEmails = archive.getAllDocs().size();
		//Indexer li = archive.indexer;
		//all name like words.
		Set<String> nws = new HashSet<String>();
		for (Contact c : contacts) {
			Set<String> names = c.names;
			for (String name : names) {
				if (name == null)
					continue;
				String[] words = name.split("\\s+");
				if (words.length < 1)
					continue;
				//just get the first and last name.
				//need to be careful not to add junk like I, The, At, American, North, Summers etcetra
				String[] iws = new String[] { words[0], words[words.length - 1] };
				for (String word : iws) {
					if (DictUtils.fullDictWords.contains(word.toLowerCase()) || word.equals("I") || sws.contains(word.toLowerCase()))
						continue;
					nws.add(word);
				}
			}
		}
		double fmean = 0, fvar = 0;
		for (Entity e : features.values())
			fmean += e.freq;
		fmean /= features.size();
		for (Entity e : features.values())
			fvar += Math.pow(e.freq - fmean, 2);
		fvar /= features.size();
		fvar = Math.sqrt(fvar);

		for (String str : features.keySet()) {
			Entity e = features.get(str);
			if (e.names == null)
				continue;
			//allcapitals
			//			boolean ac = false;
			//			for (String name : e.names) {
			//				String tc = FeatureGeneratorUtil.tokenFeature(name);
			//				if (tc.equals("ac")) {
			//					ac = true;
			//					break;
			//				}
			//			}
			//adressbook score
			boolean namelike = false;
			//			String dec = null;
			outer: for (String name : e.names) {
				String[] words = name.split("\\s+");
				for (String word : words) {
					if (nws.contains(word)) {
						namelike = true;
						//dec = word;
						break outer;
					}
				}
			}
			double score = 0;
			//this may give false scores based on lame matches such as Research, Pragram etcetra
			if (namelike) {
				e.namelike = true;
				score += 1;
				//System.err.println(str + "\t is name like due to " + dec);
			}
			score += (double) e.people.size() / (double) numContacts;
			score += (double) e.numReferBack / (e.freq);
			//			double p = Math.exp(-Math.pow(e.freq - fmean, 2) / 2 * fvar * fvar);
			//			score += p;
			//			System.err.println("P: " + p + ", mean: " + fmean + ", fvar: " + fvar + ",freq: " + e.freq);
			score += (double) e.freq / numDocs;
			//give entropy formulation score to time histogram, score based on least entropy.
			//ent = - (sigma(f(x)*log(f(x)/w(x)))) where w is width.
			//score += 0;

			//TODO: I hate just coming up with numbers to multiply and add --- come up with more logical form of expressions.
			//			if (ac)
			//				score *= 5;

			e.entropyCE = getEntropy(e.allCE) * (1 - ((double) e.allCE.size() / (double) features.size())) + 1;
			e.entropyEA = getEntropy(e.emailAddresses) * (1 - ((double) e.emailAddresses.size() / (double) numContacts)) + 1;
			//gives a higher entropy if mentioned more times, need to nulltify that.
			e.entropyTime = getEntropy(e.timeHistogram) * (1 - ((double) e.freq / (double) numEmails)) + 1;
			//			try {
			//				Collection<EmailDocument> docs = li.lookupDocs("\"" + str + "\"");
			//				e.cifreq = docs.size();
			//				if (e.cifreq > 0)
			//					score *= ((double) 1) / e.cifreq;
			//			} catch (Exception exc) {
			//				;
			//			}
			//if an entity always appeared in the start of a sentence, then prior (score) = 0; 
			//score *= (1 - ((double) e.sentstart / e.freq));
			e.prior = score;
		}
	}

	/**
	 * Features to rank and number of iterations.
	 */
	public static void rank(Map<String, Entity> features, int numIters) {
		for (int iter = 0; iter < numIters; iter++) {
			Map<String, Double> wordPriors = new HashMap<String, Double>();
			//ok, two iterations here, one to fetch all the single word tokens and then to score them
			{
				Map<String, Set<String>> temp = new HashMap<String, Set<String>>();
				for (String str : features.keySet()) {
					String[] tokens = str.split("\\s+");
					for (String token : tokens) {
						token = token.replaceAll("^\\W+|\\W+$", "");
						if (!temp.containsKey(token))
							temp.put(token, new HashSet<String>());
						temp.get(token).add(str);
					}
				}
				for (String token : temp.keySet()) {
					double p = 0;
					//List<Double> tempS = new ArrayList<Double>();
					for (String str : temp.get(token)) {
						p += features.get(str).prior / (double) str.split("\\s+").length;
						//tempS.add(features.get(str).prior);
					}
					p /= temp.get(token).size();
					wordPriors.put(token, p);
				}
			}

			Map<String, Double> scores = new HashMap<String, Double>();
			for (String str : features.keySet()) {
				Entity e = features.get(str);
				double p = 0;
				if (e.allCE != null)
					for (String ce : e.allCE.keySet()) {
						if (!features.containsKey(ce))
							continue;
						Entity cE = features.get(ce);
						int cooccurFreq = e.allCE.get(ce);
						if (e.freq == 0 || cE.freq == 0)
							continue;
						//						if ((cE.freq + e.freq - cooccurFreq) == 0)
						//							System.err.println("OOPS" + cE.freq + ", " + e.freq + ", " + cooccurFreq);
						double score = 1.0 / (double) (cE.freq + e.freq - cooccurFreq);

						score *= cooccurFreq;
						//P(E/e) = score, prefer scoring with entities of low entropy
						double beliefScore = (score) / (cE.entropyCE + cE.entropyEA + e.entropyTime);
						//TODO: add also the entity listing score.
						//appeared in entity listing together
						//						if (e.extra.get(ce))
						//							beliefScore = 0.8 * 1 + 0.2 * beliefScore;
						//						if (cE.prior < 1 && e.freq < 2)
						//							beliefScore = 0;
						p += beliefScore * cE.prior;
					}
				p /= e.allCE.size();
				double entropy = e.entropyCE + e.entropyEA + e.entropyTime;
				if (entropy > 0)
					p *= e.prior / entropy;
				else
					p *= e.prior;

				//compute score based on words.
				//double wp = 0;
				String[] tokens = str.split("\\s+");
				for (String token : tokens) {
					token = token.replaceAll("^\\W+|\\W+$", "");
					//wp += wordPriors.get(token);
				}
				//wp /= tokens.length;
				//System.err.println("Word score for: " + str + " is " + wp);
				scores.put(str, p);//+ wp);
			}

			for (String str : scores.keySet()) {
				if (!features.containsKey(str))
					continue;
				features.get(str).prior = scores.get(str);
			}
		}
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

	public static boolean checkIndex1(Archive archive, boolean force) {
		Boolean exists = indexExists(archive);
		int c1 = 0, c2 = 0, c3 = 0;
		int g1 = 0, g2 = 0, g3 = 0;
		int f1 = 0, f2 = 0, f3 = 0;
		boolean istatus = true;
		if (force || (!exists)) {
			Indexer indexer = archive.indexer;
			Map<String, Entity> features = new HashMap<String, Entity>();
			Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
			int totalEntities = 0;
			System.err.println("No feature index found..., starting to process and index. This can take a while.");
			int di = 0;
			archive.assignThreadIds();
            Tokenizer tokenizer = new CICTokenizer();
			for (EmailDocument ed : docs) {
				if (di % 100 == 0) {
					System.err.println("Done analysing documents: " + di + " of: " + docs.size());
				}
				di++;
				if (di > 2000)
					break;

				String content = archive.getContents(ed, true);
				Set<String> entities = null;
				try {
					entities = tokenizer.tokenizeWithoutOffsets(content);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (entities == null)
					continue;

				c1 += entities.size();

				Map<String, String> goodNames = new HashMap<String, String>();

				List<String> correspondents = ed.getAllNames();
				List<String> addresses = ed.getAllAddrs();
				if (correspondents != null)
					for (String c : correspondents) {
						if (good(c) && c.contains(" ")) {
							String n = IndexUtils.canonicalizeEntity(c);//EmailUtils.normalizePersonNameForLookup(c);
							goodNames.put(n, "person");
						}
					}

				for (String e : entities) {
					if (good(e) && e.contains(" ")) {
						String canonicalEntity = IndexUtils.canonicalizeEntity(e);
						if (canonicalEntity == null)
							continue;
						goodNames.put(canonicalEntity, "person");
						g1++;
					}
				}

				List<edu.stanford.muse.index.Document> prevDocs = archive.docsWithThreadId(ed.threadID);
				Set<String> history = new HashSet<String>();
				Date cdate = ed.getDate();
				//this is taking long.
				for (edu.stanford.muse.index.Document prevDoc : prevDocs) {
					EmailDocument pd = (EmailDocument) prevDoc;
					Date pdate = pd.getDate();
					if (pdate.before(cdate))
						history.add(archive.getContents(pd, true));
				}

				Set<Set<String>> entityListings = new HashSet<Set<String>>();
				//entityListings = getEntityListings(content);
				String[] sents = content.split("\\. ");
				// O(goodNames.size())
				for (String gn : entities) {
					Set<String> el = new HashSet<String>();
					for (Set<String> list : entityListings)
						if (list.contains(gn))
							el = list;

					boolean sentstart = false;
					for (String sent : sents)
						if (sent.indexOf("gn") == 0)
							sentstart = true;

					if (features.get(gn) == null)
						features.put(gn, new Entity(gn));

					features.get(gn).accountForThis(ed, content, history);
					features.get(gn).addAllCE(goodNames.keySet());
					features.get(gn).addAllEntities(entities, el, sentstart);
					if (addresses != null)
						features.get(gn).addAllEA(addresses);

					features.get(gn).priorProbablity = features.get(gn).priorProbablity + 1.0;
					totalEntities++;
				}
			}

			System.err.println("Found: " + c1 + " entities, " + c2 + " orgs and " + c3 + " places");
			System.err.println("Gn: " + g1 + " entities, " + g2 + " orgs and " + g3 + " places");
			System.err.println("Found goodfeatures: " + f1 + " entities, " + f2 + " orgs and " + f3 + " places");
			System.err.println("Done analysing docs. Starting to index.");
			for (String key : features.keySet())
				features.get(key).priorProbablity = features.get(key).priorProbablity / (double) totalEntities;
			System.err.println("Starting to merge entities");
			features = clean(features);
			//features = merge(features);
			computePriors(features, archive, di);
			//now the main algo
			//			Entity e = features.get("Seaview");
			//			System.out.println("That occur with With");
			//			for (String str : e.allCE.keySet())
			//				System.out.println(str + " : " + e.allCE.get(str));
			rank(features, 3);
			Map<Entity, Double> scores = new HashMap<Entity, Double>();
			for (Entity es : features.values())
				scores.put(es, es.prior);
			List<Pair<Entity, Double>> fs = edu.stanford.muse.util.Util.sortMapByValue(scores);
			for (Pair<Entity, Double> se : fs) {
				Entity e = se.first;
				//System.out.println("-------------------");
				String names = "", ce = "";
				for (String en : e.names)
					names += en + "\t";
				//List<Pair<String, Integer>> temp = edu.stanford.muse.util.Util.sortMapByValue(e.allCE);
				//				for (Pair<String, Integer> c : temp)
				//					ce += c.first + ":" + c.second + ":::";
				//rank of entities with lower freq is not reliable.
				if (e.freq > 1) {
					System.out.println(names + "\tScore: " + se.second + "\tFreq: " + e.freq + "\t#cooccur: " + e.allCE.size() + "\tentropyCE: " + e.entropyCE + "\tentropyEA:" + e.entropyEA + "\tNamelike: " + e.namelike + "\tCIFreq: " + e.cifreq + "\tentropyTime:" + e.entropyTime);// + "\tCE:" + ce);
					System.out.println("-------------------");
				}
			}

			//these are freq>1's
			List<Pair<Entity, Double>> temp = new ArrayList<Pair<Entity, Double>>();
			for (Pair<Entity, Double> p : fs)
				if (p.first.freq > 1)
					temp.add(p);

			//generate data for testing.
			Random randnum = new Random();
			String html = "";
			html += "<script src='jquery.js'></script>";
			html += "<script src='utils.js'></script>";
			html += "<body data-total='" + temp.size() + "'>";
			for (int i = 0; i < 1000; i++) {
				int idx = randnum.nextInt(temp.size());
				int rank = idx + 1;
				Pair<Entity, Double> p = temp.get(idx);
				Entity e = p.first;
				String str = "";
				for (String en : e.names)
					str += en + "\t";
				html += "<input type='checkbox' data-rank=" + rank + " data-score=" + p.second + "/> Rank: " + rank + ", " + str + ", score: " + p.second + ", namelike:" + p.first.namelike + "<br>";
			}
			html += "<button onclick='compute()'>Evaluate</button>";
			html += "</body>";

			try {
				FileWriter fw = new FileWriter(new File("/Users/viharipiratla/sandbox/entity.html"));
				fw.write(html);
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			//istatus = index1(features);
		}
		return istatus;
	}

	public static Map<String, Integer> getEntitiesIn(Set<edu.stanford.muse.index.Document> cdocs, Archive archive) {
		Map<String, Integer> ents = new HashMap<String, Integer>();
        Tokenizer tokenizer = new CICTokenizer();
		for (edu.stanford.muse.index.Document cdoc : cdocs) {
			String content = archive.getContents(cdoc, false);
			try {
				Set<String> es = tokenizer.tokenizeWithoutOffsets(content);
				for (String e : es) {
					if (!ents.containsKey(e))
						ents.put(e, 0);
					ents.put(e, ents.get(e) + 1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ents;
	}

	public static void test(Archive archive) {
		Indexer indexer = archive.indexer;

		//Laura.Cerruti@ucpress.edu
		Contact c = archive.addressBook.lookupByEmail("Laura.Cerruti@ucpress.edu");
		Set<String> names = c.names;
		Map<String, Integer> ces;
		Set<edu.stanford.muse.index.Document> docs = new HashSet<edu.stanford.muse.index.Document>();
		for (String name : names) {
			try {
				System.err.println("Looking up: " + name);
				//docs.addAll(ldocs);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ces = getEntitiesIn(docs, archive);
		List<Pair<String, Integer>> sces = Util.sortMapByValue(ces);
		for (Pair<String, Integer> sce : sces)
			System.err.println(sce.first + " : " + sce.second);
		System.err.println("--------------");

		int[] contact_ids = new int[] { 69 };
		Set<edu.stanford.muse.index.Document> cdocs = IndexUtils.selectDocsByAllPersons(archive.addressBook, (Collection) archive.getAllDocs(), null, contact_ids);

		Map<String, Integer> ents = getEntitiesIn(cdocs, archive);
		List<Pair<String, Integer>> sens = Util.sortMapByValue(ents);
		for (Pair<String, Integer> se : sens)
			System.err.println(se.first + " : " + se.second);

	}

	public static void main(String[] args) {
		try {
			Archive archive = SimpleSessions.readArchiveIfPresent(System.getProperty("user.home") + File.separator + "epadd-appraisal" + File.separator + "user-creeley");

			//			Indexer li = archive.indexer;
			//			String q = "\"Holly Tavel\"";
			//			Collection<EmailDocument> docs = li.lookupDocs(q);
			//			System.out.println("hits for: " + q + " = " + docs.size());
			test(archive);
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
