<%@page import="edu.stanford.muse.email.AddressBook"%>
<%@page import="edu.stanford.muse.util.Pair"%>
<%@page import="edu.stanford.muse.util.Util"%>
<%@page import="org.apache.lucene.analysis.util.CharArraySet"%>
<%@page import="org.apache.lucene.analysis.core.StopAnalyzer"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>	
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.epadd.util.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="opennlp.tools.sentdetect.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="opennlp.tools.util.featuregen.FeatureGeneratorUtil"%>
<%@page language="java" import="opennlp.tools.postag.POSTaggerME"%>
<%@page language="java" import="opennlp.tools.postag.POSModel"%>
<%@page language="java" import="opennlp.tools.tokenize.TokenizerME"%>
<%@page language="java" import="opennlp.tools.tokenize.TokenizerModel"%>
<!-- With email based smoothing.  -->
<!DOCTYPE html>
<html>
<%
	String[] entities;
	BufferedReader br = new BufferedReader(new FileReader(new File("cbooks.txt")));
	List<String> allentities = new ArrayList<String>();
	String line = null;
	while((line=br.readLine())!=null)
		allentities.add(line.trim());	
	entities = allentities.toArray(new String[allentities.size()]);
	
	//same docId, tId, time window, eA, anywhere
	//fdoc*wdoc+fthread*wthread+ftw*wtw+feA*weA+fAll*wAll
	double wdoc = 0.35,wthread = 0.25,wtw = 0.2, weA = 0.15, wAll = 0.05;
	
	SentenceDetectorME	sentenceDetector;
	InputStream SentStream = NLPUtils.class.getClassLoader().getResourceAsStream("models/en-sent.bin");
	SentenceModel model = null;
	POSTaggerME	tagger = null;
	TokenizerME	tokenizer = null;
	try {
		model = new SentenceModel(SentStream);
		InputStream modelIn = NLPUtils.class.getClassLoader().getResourceAsStream("models/en-pos-maxent.bin");
		// OpenNLP.class.getClassLoader().getResourceAsStream("en-pos-maxent.bin");
		POSModel posmodel = new POSModel(modelIn);
		tagger = new POSTaggerME(posmodel);
		InputStream tokenStream = NLPUtils.class.getClassLoader()
				.getResourceAsStream("models/en-token.bin");
		// OpenNLP.class.getClassLoader().getResourceAsStream("en-token.bin");
		TokenizerModel modelTokenizer = new TokenizerModel(tokenStream);
		tokenizer = new TokenizerME(modelTokenizer);
	} catch (Exception e) {
		e.printStackTrace();
		System.err.println("Exception in init'ing sentence model");
	}
	sentenceDetector = new SentenceDetectorME(model);
	
	Archive archive = JSPHelper.getArchive(session);
	//archive.assignThreadIds();
	AddressBook ab = archive.addressBook;
	List<Document> docs = archive.getAllDocs();
	Indexer indexer = archive.indexer;
	//P(kw|entity)
	Map<String,Double> kwgentity = new HashMap<String,Double>();

	//frequency of kws.
	Map<String,Double> freqs = new HashMap<String,Double>();
	CharArraySet stopWordsSet = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
	
	Map<String,Collection<EmailDocument>> processed = new HashMap<String,Collection<EmailDocument>>();
	int i=0;
	for(Document d: docs){
		EmailDocument ed = (EmailDocument)d;
		String content = indexer.getContents(ed, true);
		String fullcontent = indexer.getContents(ed, false);
		content = content.replaceAll("^>+.*","");
		content = content.replaceAll("\\n\\n", ". ");
		content = content.replaceAll("\\n"," ");
		if(i%100==0)
			System.err.println("Processed: "+i);
// 		if(i++>0)
// 			break;
		i++;
		
		String[] sentences = sentenceDetector.sentDetect(content);
		for(int s=0;s<sentences.length;s++){
			String sent = sentences[s];
			boolean bookFound = false;
			String sample = sent;

			boolean matches = false;
			for(String entity: entities){
				if(sent.contains(entity)){
					matches = true;
					break;
				}
			}
			Set<String> kws = new HashSet<String>();
			if(matches){
				String prevSent = s>0?sentences[s-1]:null;
				String nxtSent = s<(sentences.length)?sentences[s]:null;
				String[] nbdsents = new String[]{sent,prevSent,nxtSent};
				for(String nsent: nbdsents){
					if(nsent==null||nsent.length()>500)
						continue;
					String[] tokens = tokenizer.tokenize(nsent);
					String tags[];
					try {
						tags = tagger.tag(tokens);
					} catch (Exception e) {
						System.err.println("Error tagging message: " + e);
						tags = new String[] {};
					}
					//String[] tokens = nsent.split("\\s+");
					int ti =0 ;
					for(String token: tokens){
						String type = FeatureGeneratorUtil.tokenFeature(token);
						if(type.equals("lc")&&!stopWordsSet.contains(token)){
							if(tags[ti].equals("NN")||tags[ti].equals("NNS")||tags[ti].matches("VB[A-Z]")){
								//System.err.println(token);
								if(!kwgentity.containsKey(token))
									kwgentity.put(token, 0.0);
								kwgentity.put(token,kwgentity.get(token)+1.0);
								kws.add(token);
							}
						}
						ti++;
					}
				}
			}
			int j=0;
			for(String kw: kws){
				if(j++>100)
					break;
				int fdoc = edu.stanford.muse.ie.Util.countMatches(content, kw);
				int fthread = edu.stanford.muse.ie.Util.countMatches(fullcontent, kw);
				Calendar c = new GregorianCalendar();
				c.setTime(ed.getDate());
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH);
				//System.err.println("Year: "+year+", month: "+month);
				int ftw, feA, fAll;
				if(!processed.containsKey(kw)){
					Collection<EmailDocument> alldocs = indexer.luceneLookupDocs(kw ,Indexer.QueryType.FULL);
					Collection<EmailDocument> twdocs = IndexUtils.selectDocsByDateRange(alldocs, year, month);
					List<String> emails = ed.getAllNonOwnAddrs(ab.getOwnAddrs());
					String[] emailsA = emails.toArray(new String[emails.size()]);
					Collection<Document> cdocs = IndexUtils.selectDocsByPersons(ab, alldocs, emailsA);
					
					ftw = twdocs.size();
					feA = cdocs.size();
					fAll = alldocs.size();
					processed.put(kw,alldocs);
				}else{
					 Collection<EmailDocument> alldocs = processed.get(kw);
					 Collection<EmailDocument> twdocs = IndexUtils.selectDocsByDateRange(alldocs, year, month);
					 List<String> emails = ed.getAllNonOwnAddrs(ab.getOwnAddrs());
					 String[] emailsA = emails.toArray(new String[emails.size()]);
					 Collection<Document> cdocs = IndexUtils.selectDocsByPersons(ab, alldocs, emailsA);
					 ftw = twdocs.size();
					 feA = cdocs.size();
					 fAll = alldocs.size();
				}
				
				double score = fdoc*wdoc+fthread*wthread;//+ftw*wtw+feA*weA+fAll*wAll;		
				if(!freqs.containsKey(kw))
					freqs.put(kw,0.0);
				freqs.put(kw, freqs.get(kw)+score);
			}
		}
	}
	for(String kw: kwgentity.keySet())
		kwgentity.put(kw, kwgentity.get(kw)/freqs.get(kw));
	List<Pair<String,Double>> skw = Util.sortMapByValue(kwgentity);
	for(Pair<String,Double> kw: skw)
		out.println(kw.first+" : "+kw.second+"<br>");
%>

</html>