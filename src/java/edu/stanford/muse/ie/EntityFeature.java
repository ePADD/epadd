package edu.stanford.muse.ie;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import edu.stanford.muse.index.ArchiveReaderWriter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.stanford.muse.Config;
import edu.stanford.muse.email.StatusProvider;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.index.IndexUtils;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.JSONUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.webapp.JSPHelper;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Data structure to represent mixtures of an entity mention in archive
 * TODO: move checkindex, index and indexExists to different class.
 */
public class EntityFeature implements StatusProvider, Serializable {
	/**
	 * Feature class to store co-occurring entities and (co-)occurring email
	 * addresses for an entity. This is required for expandname.jsp and
	 * sortsources.jsp.
	 */

	private static final long	serialVersionUID	= 1L;
	String				name;
	Map<String, Integer>	cooccuringEntities;
	Map<String, Integer>	emailAddresses;
	short				type;

	double				priorProbablity		= 0.0;
	int					freq				= 0;
	//acronyms may overlap with ORG or PLACE acronyms are all capital letters
	private static final short			PERSON				= 0;
    private static final short ORG = 1;
    private static final short PLACE = 2;
    public static short CORRESPONDENT = 3;
    private static final short ACRONYM = 4;

	private static final String		ID					= "id";
    private static final String TID = "tid";
    static final String NAME = "name";
    static final String EAS = "emailAddresses";
    static final String CES = "cooccurringEntities";
    static final String TYPE = "type";
    static final String PP = "priorProbablity";

	private boolean						cancel				= false;
	private String						status;
	private double						pctComplete			= 0.0;

	private static IndexReader			reader				= null;
	private static IndexSearcher		searcher			= null;

    private static final Log log						= LogFactory.getLog(EntityFeature.class);

    /**
	 * This constructor is initialized so that the status provider will be given
	 * some handle for status messages while checking for index
	 */
    EntityFeature() {
	}

	private EntityFeature(String name, short type) {
		this.name = name;
		this.cooccuringEntities = new HashMap<>();
		this.emailAddresses = new HashMap<>();
		this.type = type;
	}

	private EntityFeature(Document doc) {
		name = doc.get(NAME);
		try {
			type = Short.parseShort(doc.get(TYPE));
			priorProbablity = Double.parseDouble(doc.get(PP));
		} catch (Exception e) {
			System.err.println("Couldn't parse type:" + doc.get(TYPE) + ", pp: " + doc.get(PP));
			e.printStackTrace();
		}
		Gson gson = new Gson();
		String ce = doc.get(CES);
		String ea = doc.get(EAS);
		java.lang.reflect.Type mapT = new TypeToken<Map<String, Integer>>() {
		}.getType();
		try {
			this.cooccuringEntities = gson.fromJson(ce, mapT);
			this.emailAddresses = gson.fromJson(ea, mapT);
		} catch (Exception e) {
			e.printStackTrace();
			log.warn("Parsing emailaddresses: " + ea);
			log.warn("Parsing ce's: " + ce);
			log.warn("Exception while reading: context back for id: " + doc.get(ID));
		}
	}

	private void addCE(String entity) {
		if (this.cooccuringEntities.size() >= Config.MAX_ENTITY_FEATURES)
			return;
		entity = StringEscapeUtils.escapeJava(entity);
		if (!this.cooccuringEntities.containsKey(entity))
			this.cooccuringEntities.put(entity, 0);
		this.cooccuringEntities.put(entity, this.cooccuringEntities.get(entity) + 1);
	}

	private void addEA(String address) {
		if (this.emailAddresses.size() >= Config.MAX_ENTITY_FEATURES)
			return;
		address = StringEscapeUtils.escapeJava(address);
		if (!this.emailAddresses.containsKey(address))
			this.emailAddresses.put(address, 0);
		this.emailAddresses.put(address, this.emailAddresses.get(address) + 1);
	}

	/**
	 * Should be called once per document
	 */
    private void accountForThis() {
		this.freq++;
	}

	private void addAllCE(Collection<String> entities) {
		for (String entity : entities)
			addCE(entity);
	}

	private void addAllEA(Collection<String> addresses) {
		for (String address : addresses)
			addEA(address);
	}

	//TODO: this method is not complete, dont use
	public double scoreWith(EntityFeature ef) {
		if (!ef.name.toLowerCase().contains(name))
			return 0;
		// csore is based on cooccuring entities and escore is based on email
		// addresses.
		double cScore = 0, eScore = 0;
		Set<String> targetCEs = ef.cooccuringEntities.keySet(), targetEAs = ef.emailAddresses.keySet();
		for (String targetCE : targetCEs) {
			if (this.cooccuringEntities.keySet().contains(targetCE))
				cScore++;
		}
		for (String targetEA : targetEAs) {
			if (this.emailAddresses.keySet().contains(targetEA))
				eScore++;
		}
		return (cScore * 0.5 + eScore * 0.5);
	}

	private List<Pair<EntityFeature, Double>> getClosestEntities(Archive archive) {
        Map<EntityFeature, Double> scoresU = new LinkedHashMap<>();
        Set<EntityFeature> efts = null;
        if(type!=EntityFeature.ACRONYM)
            efts = EntityFeature.getMatches(name.toLowerCase(),archive);
        else
            efts = EntityFeature.getAbbreviations(name.toLowerCase(),archive);
        JSPHelper.log.info("Found expansions: "+efts.size()+" for "+name);

        Set<String> ownAddr = archive.addressBook.getOwnAddrs();
        if(efts == null){
            JSPHelper.log.warn("Matching efs for: " + name + " are null.");
//            result.put("result", "Never seen this name before");
//            response.getWriter().write(result.toString(4));
            return null;
        }
        for (EntityFeature eft : efts) {
            Map<String, Integer> targetCE = eft.cooccuringEntities;
            Map<String, Integer> targetEA = eft.emailAddresses;
            double cScore, eScore;


            if(type != EntityFeature.ACRONYM){
                if (!eft.name.toLowerCase().matches(".*\\b" + name.toLowerCase() + "\\b.*")) {
                    scoresU.put(eft, 0.0);
                    continue;
                }
            }else{
                String acr = edu.stanford.muse.ie.Util.getAcronym(eft.name).toLowerCase();
                if(JSPHelper.log.isDebugEnabled())
                    JSPHelper.log.debug(name+", acr: "+acr);
                if(!acr.equals(name.toLowerCase()))
                    continue;
            }

            String lcname = eft.name.toLowerCase();
            //if(lcname.contains("at")||lcname.contains("and")||lcname.contains("works")||lcname.contains("dear")||lcname.contains("selected")||lcname.contains("journal")||
            if(!lcname.contains(" ")){
                scoresU.put(eft, 0.0);
                continue;
            }

            cScore = 0;
            eScore = 0;
            double targetEAMod = 0, targetCEMod = 0;
            for (String ea : targetEA.keySet())
                targetEAMod += Math.pow(targetEA.get(ea), 2);
            for (String ce : targetCE.keySet())
                targetCEMod += Math.pow(targetCE.get(ce), 2);
            targetEAMod = Math.sqrt(targetEAMod);
            targetCEMod = Math.sqrt(targetCEMod);

            for (String ea : this.emailAddresses.keySet()) {
                if (targetEA.containsKey(ea)) {
                    if ((targetEA.get(ea) > 0)&&(!ownAddr.contains(ea))){
                        if(JSPHelper.log.isDebugEnabled())
                            JSPHelper.log.debug("Scoring with ea: "+ea);
                        eScore += targetEA.get(ea);
                    }
                }
            }

            for (String ce : this.cooccuringEntities.keySet()) {
                if (targetCE.containsKey(ce)) {
                    if (targetCE.get(ce) > 0){
                        EntityFeature efce = EntityFeature.getExactMatches(ce,archive);
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
            if(JSPHelper.log.isDebugEnabled())
                JSPHelper.log.debug(eft.name + ", score: " + (score) + ", cScore: " + cScore + ", eScore: " + eScore);
        }
        return edu.stanford.muse.util.Util.sortMapByValue(scoresU);
	}

	public List<Pair<EntityFeature, Double>> getClosestNEntities(Archive archive, int N) {
		List<Pair<EntityFeature,Double>> scores = getClosestEntities(archive);
        return scores.subList(0, Math.min(N, scores.size()));
	}

	@Override
	public String toString() {
		return name;
	}

	Document getIndexerDoc() {
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
		return doc;
	}

	/** Cleans the mixtures directory */
    private void clean(Archive archive) {
		String iDir = getFeaturesDir(archive);
		File f = new File(iDir);
		if (f.exists()) {
			if (!f.delete())
				log.warn ("Warning, delete failed: " + f.getAbsolutePath());
		}
	}

	/**
	 * @return true if successful
	 */
    private boolean index(Map<String, EntityFeature> features, Archive archive) {
		IndexWriter w = null;
		try {
			String iDir = getFeaturesDir(archive);
			StandardAnalyzer analyzer = new StandardAnalyzer();
			Directory index = FSDirectory.open(new File(iDir).toPath());
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(OpenMode.CREATE);
			w = new IndexWriter(index, iwc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (w == null) {
			log.warn("Couldn't open index files... exiting.");
			return false;
		}

		int c = 0;
		for (String key : features.keySet()) {
			if (cancel) {
				clean(archive);
				return false;
			}
			EntityFeature ef = features.get(key);
			try {
				Document doc = ef.getIndexerDoc();
				doc.add(new StringField(ID, key, Field.Store.YES));
				// use this field to get partial matches.... is tokenised.
				doc.add(new TextField(TID, key, Field.Store.YES));
				w.addDocument(doc);
				if (c % 1000 == 0) {
					JSPHelper.log.info("Extracted and wrote doc for: " + c + " of " + features.keySet().size());
					status = "Indexed " + c + "/" + features.size() + " mixtures";
					pctComplete = ((double) c * 50) / ((double) features.size()) + 50;
				}
				c++;
			} catch (Exception e) {
				log.warn("Exception while writing/closing (to) index");
				e.printStackTrace();
				return false;
			}
		}
		try {
			w.close();
			log.warn("Done closing index files.");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static String getFeaturesDir(Archive archive) {
		return archive.baseDir + File.separator + Config.FEATURES_INDEX;
	}

	/**
	 * @return true if feature index exists, false otherwise. Does not actually
	 *         index.
	 */
	public static boolean indexExists(Archive archive) {
		String iDir = getFeaturesDir(archive);
		File f = new File(iDir);
		return f.exists();
	}

	public boolean checkIndex(Archive archive) {
		return checkIndex(archive, false);
	}

	/**
	 * @arg2 force creation of index irrespective of previous existence of the
	 *       index.
	 *       Checks and creates index if required.
	 * @return true if successful
	 */
    private boolean checkIndex(Archive archive, boolean force) {
		Boolean exists = indexExists(archive);
		int c1 = 0, c2 = 0, c3 = 0;
		int g1 = 0, g2 = 0, g3 = 0;
		int f1 = 0, f2 = 0, f3 = 0;
		boolean istatus = true;
		if (force || (!exists)) {
			Map<String, EntityFeature> features = new HashMap<>();
			Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
			int totalEntities = 0;
			log.info("No feature index found..., starting to process and index. This can take a while.");
			int di = 0;
			for (EmailDocument ed : docs) {
				if (cancel) {
					clean(archive);
					return false;
				}

				if (di % 1000 == 0) {
					JSPHelper.log.info("Done analysing documents: " + di + " of: " + docs.size());
					status = "Analyzed " + di + "/" + docs.size() + " email documents";
					pctComplete = ((double) di * 50) / (double) docs.size();
				}
				di++;

                List<Span> names;
                try {
                    names = Arrays.asList(archive.getAllNamesInDoc(ed, true));
                }catch (IOException ioe){
                    log.error("Problem accessing entities in "+ed.getUniqueId(), ioe);
                    continue;
                }
                List<String> entities = names.stream()
                        .filter(n -> n.type == NEType.Type.PERSON.getCode())
                        .map(n -> n.text).collect(Collectors.toList());
                List<String> places = names.stream()
                        .filter(n-> n.type == NEType.Type.PLACE.getCode())
                        .map(n -> n.text).collect(Collectors.toList());
                List<String> orgs = names.stream()
                        .filter(n-> n.type == NEType.Type.ORGANISATION.getCode())
                        .map(n -> n.text).collect(Collectors.toList());
				if (entities != null)
					c1 += entities.size();
				if (orgs != null)
					c2 += orgs.size();
				if (places != null)
					c3 += places.size();

				Map<String, String> goodNames = new HashMap<>();

				List<String> correspondents = ed.getAllNames();
				List<String> addresses = ed.getAllAddrs();
				if (correspondents != null)
					for (String c : correspondents) {
						if (c != null && c.contains(" ")) {
							String n = IndexUtils.canonicalizeEntity(c);//EmailUtils.normalizePersonNameForLookup(c);
							goodNames.put(n, "person");
						}
					}

				for (String e : entities) {
					if (e != null && e.contains(" ")) {
						String canonicalEntity = IndexUtils.canonicalizeEntity(e);
						if (canonicalEntity == null)
							continue;

						goodNames.put(canonicalEntity, "person");
						g1++;
					}
				}

				for (String o : orgs) {
					String canonicalEntity = IndexUtils.canonicalizeEntity(o);
					if (canonicalEntity == null)
						continue;

					goodNames.put(canonicalEntity, "org");
					g2++;
				}

				for (String p : places) {
					String canonicalEntity = IndexUtils.canonicalizeEntity(p);
					if (canonicalEntity == null)
						continue;
					goodNames.put(canonicalEntity, "places");
					g3++;
				}

				// O(goodNames.size())
				for (String gn : goodNames.keySet()) {
					if (features.get(gn) == null) {
						if (goodNames.get(gn).equals("person")) {
							features.put(gn, new EntityFeature(gn, EntityFeature.PERSON));
							f1++;
						}
						else if (goodNames.get(gn).equals("org")) {
							features.put(gn, new EntityFeature(gn, EntityFeature.ORG));
							f2++;
						}
						else if (goodNames.get(gn).equals("places")) {
							features.put(gn, new EntityFeature(gn, EntityFeature.PLACE));
							f3++;
						}
					}
					features.get(gn).accountForThis();
					features.get(gn).addAllCE(goodNames.keySet());
					if (addresses != null)
						features.get(gn).addAllEA(addresses);

					features.get(gn).priorProbablity = features.get(gn).priorProbablity + 1.0;
					totalEntities++;
				}
			}
			log.info("Found: " + c1 + " entities, " + c2 + " orgs and " + c3 + " places");
			log.info("Gn: " + g1 + " entities, " + g2 + " orgs and " + g3 + " places");
			log.info("Found goodfeatures: " + f1 + " entities, " + f2 + " orgs and " + f3 + " places");
			for (String key : features.keySet())
				features.get(key).priorProbablity = features.get(key).priorProbablity / (double) totalEntities;
			log.info("Done analysing docs. Starting to index.");
			istatus = index(features, archive);
		}
		return istatus;
	}

	private static EntityFeature getExactMatches(String name, Archive archive) {
		String iDir = getFeaturesDir(archive);
		if (searcher == null || reader == null)
			try {
				reader = DirectoryReader.open(FSDirectory.open(new File(iDir).toPath()));
				searcher = new IndexSearcher(reader);
			} catch (Exception e) {
				e.printStackTrace();
			}

		try {
			TermQuery q = new TermQuery(new Term(ID, name));
			TopDocs td = searcher.search(q, 10);
			if (td.scoreDocs.length > 1)
                log.warn("Warn: There are more than one record with name:" + name);
			if (td.scoreDocs.length > 0) {
				ScoreDoc sd = td.scoreDocs[0];
				return new EntityFeature(searcher.doc(sd.doc));
			} else {
				log.warn("Cannot find: " + name);
				return null;
			}
		} catch (IOException e) {
			log.warn("Couldn't find/read index files.");
			e.printStackTrace();
			return null;
		}
	}

	/** Not exact matches */
	private static Set<EntityFeature> getMatches(String name, Archive archive) {
		String iDir = getFeaturesDir(archive);
		if (reader == null | searcher == null)
			try {
				reader = DirectoryReader.open(FSDirectory.open(new File(iDir).toPath()));
				searcher = new IndexSearcher(reader);
			} catch (Exception e) {
				e.printStackTrace();
			}
		name = IndexUtils.canonicalizeEntity(name);
		if (name == null) {
			log.info("Name: " + name + ", on normalisation gave null. Returning!");
			return null;
		}
		IndexReader reader;
		IndexSearcher searcher;
		Set<EntityFeature> efs = new HashSet<>();
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(iDir).toPath()));
			searcher = new IndexSearcher(reader);
			BooleanQuery.Builder internal = new BooleanQuery.Builder();
			String[] names = name.split("\\s+");
			CharArraySet stopWords = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

			for (String w : names) {
				if (stopWords.contains(w))
					internal.add(new TermQuery(new Term(TID, w)),
							BooleanClause.Occur.SHOULD);
				else
					internal.add(new TermQuery(new Term(TID, w)),
							BooleanClause.Occur.MUST);
			}
			TopDocs td = searcher.search(internal.build(), Integer.MAX_VALUE);
			for (ScoreDoc sd : td.scoreDocs) {
				efs.add(new EntityFeature(searcher.doc(sd.doc)));
			}
			return efs;
		} catch (IOException e) {
			log.warn("Couldn't find/read index files.");
			e.printStackTrace();
			return null;
		}
	}

	private static Set<EntityFeature> getAbbreviations(String name, Archive archive) {
		String iDir = getFeaturesDir(archive);
		name = IndexUtils.canonicalizeEntity(name);
		if (name == null) {
			log.warn("Name: " + name + ", on normalisation gave null. Returning!");
			return null;
		}
		IndexReader reader = null;
		IndexSearcher searcher = null;
		Set<EntityFeature> efs = new HashSet<>();
		CharArraySet stopWordsSet = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
		String[] stopWords = new String[stopWordsSet.size()];
		Iterator it = stopWordsSet.iterator();
		int j = 0;
		while (it.hasNext()) {
			char[] stopWord = (char[]) it.next();
			stopWords[j++] = new String(stopWord);
		}

		String stopWordsList = "", regex = "";
		for (int i = 0; i < stopWords.length; i++) {
			String stopWord = stopWords[i];
			stopWordsList += stopWord;
			if (i < (stopWords.length - 1))
				stopWordsList += "|";
		}

		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(iDir).toPath()));
			searcher = new IndexSearcher(reader);
			String word = "[a-zA-Z0-9]", nonword = "[^a-zA-Z0-9]";
			for (int i = 0; i < name.length(); i++) {
				char ch = name.charAt(i);
				regex += ch + word + "+";
				if (i < (name.length() - 1))
					regex += nonword + "+(" + stopWordsList + ")?" + nonword + "*";
			}
			log.info("Searching for pattern: " + regex);
			RegexpQuery query = new RegexpQuery(new Term(NAME, regex));

			TopDocs td = searcher.search(query, Integer.MAX_VALUE);

			log.info("returned: " + td.scoreDocs.length);
			for (ScoreDoc sd : td.scoreDocs) {
				efs.add(new EntityFeature(searcher.doc(sd.doc)));
			}
			return efs;
		} catch (IOException e) {
			log.info("Couldn't find/read index files.");
			e.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		Archive archive = null;
		try {
			String aFile = System.getProperty("user.home") + File.separator + "epadd-appraisal" + File.separator + "user";
			archive = ArchiveReaderWriter.readArchiveIfPresent(aFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Set<EntityFeature> efts = EntityFeature.getMatches("florida", archive); //getAbbreviations("HPC", archive);
		long end_time = System.currentTimeMillis();
		System.err.println("Query completed in: " + (end_time - start_time));
		for (EntityFeature eft : efts)
            System.err.println(eft.cooccuringEntities.keySet());

        try {

            for (LeafReaderContext ctx : reader.leaves()) {
                LeafReader reader = ctx.reader();
                for (int i = 0; i < reader.maxDoc(); i++) {
                    Document doc = reader.document(i);
//                    for (IndexableField f: doc.getFields())
                    System.err.println(doc.get("name"));
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
	}

	@Override
	public String getStatusMessage() {
		return JSONUtils.getStatusJSON(status, (int) pctComplete, 0, 0);
	}

	@Override
	public void cancel() {
		cancel = true;
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}
}