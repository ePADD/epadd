<%@page import="edu.stanford.muse.email.AddressBook"%>
<%@page import="opennlp.tools.cmdline.parser.*"%>
<%@page import="opennlp.tools.postag.*"%>
<%@page import="opennlp.tools.parser.*"%>
<%@page import="opennlp.tools.tokenize.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>	
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="opennlp.tools.sentdetect.*"%>
<%@page language="java" import="edu.stanford.muse.ie.Authority"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%
  	Archive archive = JSPHelper.getArchive(session);
	Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
	AddressBook ab = archive.addressBook;
	Indexer indexer = archive.indexer;
	Set<String> ownAddr = ab.getOwnAddrs();	
	//data-structure that contains freqs of communication between contacts.
	Map<Integer, Map<Integer,Integer>> freqs = new HashMap<Integer,Map<Integer,Integer>>();
	//entity freqs, entity -> <contact id, frequency>
	Map<String, Map<Integer,Integer>> efreqs = new HashMap<String,Map<Integer,Integer>>();
	//entity -> noun/verb freqs
	Map<String,Map<String,Integer>> nvFreqs = new HashMap<String,Map<String,Integer>>();
	
	List<String> interestedTags = Arrays.asList("NN","NNP","VBG");
	int i = 0;
	//MaltParserService service =  new MaltParserService();
	//String classpath = System.getProperty("java.class.path")+System.getProperty("path.separator")+".";
	//service.initializeParserModel("-c engmalt.linear-1.7 -m parse -w . -lfi parser.log");

	InputStream modelIn = null;
	POSTaggerME tagger = null;
	try {
	  modelIn = NLPUtils.class.getClassLoader().getResourceAsStream("models/en-pos-maxent.bin");
	  POSModel model = new POSModel(modelIn);
	  tagger = new POSTaggerME(model);
	}
	catch (IOException e) {
	  // Model loading failed, handle the error
	  e.printStackTrace();
	}
	finally {
	  if (modelIn != null) {
	    try {
	      modelIn.close();
	    }
	    catch (IOException e) {
	    }
	  }
	}	
	
	modelIn = NLPUtils.class.getClassLoader().getResourceAsStream("models/en-token.bin");
	TokenizerModel tmodel = null;
	try {
	  tmodel = new TokenizerModel(modelIn);
	}
	catch (IOException e) {
	  e.printStackTrace();
	}
	finally {
	  if (modelIn != null) {
	    try {
	      modelIn.close();
	    }
	    catch (IOException e) {
	    }
	  }
	}
	
	Tokenizer tokenizer = new TokenizerME(tmodel);
	SentenceDetectorME	sentenceDetector;
	InputStream SentStream = NLPUtils.class.getClassLoader().getResourceAsStream("models/en-sent.bin");
	SentenceModel model = null;
	try {
		model = new SentenceModel(SentStream);
	} catch (Exception e) {
		e.printStackTrace();
		System.err.println("Exception in init'ing sentence model");
	}
	sentenceDetector = new SentenceDetectorME(model);
	
	for (EmailDocument ed : docs) {
		List<String> addrs = ed.getAllAddrs();
		Set<Integer> cids = new HashSet<Integer>();
		for (String addr : addrs) {
			if(ownAddr.contains(addr))
				continue;	
			
			Contact c = ab.lookupByEmail(addr);
			if (c != null) {
				int cid = ab.getContactId(c);
				cids.add(cid);
			}
		}
		int min_cid = Integer.MAX_VALUE;
		for(int cid: cids)
			min_cid = Math.min(min_cid, cid);
		for(int cid: cids){
			if(cid == min_cid)
				continue;
			Map<Integer,Integer> t = freqs.get(min_cid);
			if(t == null)
				t = new HashMap<Integer,Integer>();
			if(!t.containsKey(cid))
				t.put(cid, 0);
			t.put(cid, t.get(cid)+1);
			freqs.put(min_cid, t);
		}
		
		List<String> entities = indexer.getNamesFromPatternForDocId(ed.getUniqueId(), Indexer.QueryType.ORIGINAL);
		Map<Integer,Integer> temp = new HashMap<Integer,Integer>();
		for(int cid: cids){
			if(!temp.containsKey(cid))
				temp.put(cid,0);
			temp.put(cid,temp.get(cid)+1);
		}
		for(String entity: entities)
			efreqs.put(entity, temp);
		
		String contents = indexer.getContents(ed, false);
 		String[] sents = sentenceDetector.sentDetect(contents);
// 		DependencyStructure graph = service.parse(sents);
		
	//	System.out.println(graph);
		for(String sent: sents){
			boolean inter = false;
			Set<String> ce = new HashSet<String>();
			for(String entity: entities){		
				if(sent.contains(entity)){
					inter = true;
					ce.add(entity);
				}
			}
			//do pos tagging and consider all verbs and nouns
			if(inter){
				String[] tokens = tokenizer.tokenize(sent);
				String[] tags = tagger.tag(tokens);
				Set<String> nvs = new HashSet<String>();
				for(int j=0;j<tags.length;j++){
					String tag = tags[j];
					if(interestedTags.contains(tag))
						nvs.add(stemmer.stem(tokens[j]));
				}
				
				for(String e: ce){
					Map<String,Integer> nvfreq = nvFreqs.get(e);
					if(nvfreq==null)
						nvfreq = new HashMap<String,Integer>();
					for(String nv: nvs){
						if(!nvfreq.containsKey(nv))
							nvfreq.put(nv, 0);
						nvfreq.put(nv, nvfreq.get(nv)+1);
					}
					nvFreqs.put(e, nvfreq);
				}
			}
		}
	
		System.err.println("Done: "+(++i)+"/"+docs.size());
		if(i>1000)
			break;
	}
	//service.terminateParserModel();
	
	Map<String,Double> u = new HashMap<String,Double>();
	for(String entity: efreqs.keySet()){
		//compute the mention vector
		List<Double> rvec = new ArrayList<Double>();
		for(int corr: freqs.keySet()){
			double m = 0;
			//group vector
			Map<Integer,Integer> gv = freqs.get(corr);
			Map<Integer,Integer> mentions = efreqs.get(entity);
			for(int corr2: gv.keySet())
				if(mentions.containsKey(corr2))
					m += gv.get(corr2)*mentions.get(corr2);
			rvec.add(m);
		}
		Double[] r = rvec.toArray(new Double[rvec.size()]);
		double x = 0, numG = 1;
		for(double rx: r){
			x += rx*rx;
			if(rx>3)
				numG++;
		}
		x = Math.sqrt(x);
		x /= numG;
		
		u.put(entity, x);
	}
	//for(String e:efreqs.keySet())
	//	u.put(e, (double)efreqs.get(e).size());

	List<Pair<String,Double>> pairs = Util.sortMapByValue(u);
	for(Pair<String,Double> p: pairs){
		Map<String,Integer> nvs = nvFreqs.get(p.first);
		if(nvs==null||nvs.keySet()==null)
			continue;
		String temp = "";
		for(String nv: nvs.keySet())
			temp += nv+" : "+nvs.get(nv)+"    ";
		out.println(p.first+" : "+p.second+" Real: "+efreqs.get(p.first).size()+"temp: "+temp+"<br><hr>");
	}
%>