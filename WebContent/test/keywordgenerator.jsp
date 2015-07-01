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
<%@page language="java" import="java.util.regex.*"%>
<%@page language="java" import="opennlp.tools.sentdetect.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="opennlp.tools.util.featuregen.FeatureGeneratorUtil"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>

</body>
<%
	String[] entities;
	BufferedReader br = new BufferedReader(new FileReader(new File("cbooks.txt")));
	List<String> allentities = new ArrayList<String>();
	String line = null;
	while((line=br.readLine())!=null)
		allentities.add(line.trim());	
	entities = allentities.toArray(new String[allentities.size()]);
	
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
	
	Archive archive = JSPHelper.getArchive(session);
	AddressBook ab = archive.addressBook;
	Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
	Indexer indexer = archive.indexer;
	//P(kw|entity)
	Map<String,Double> kwgentity = new HashMap<String,Double>();
	CharArraySet stopWordsSet = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

	int i=0;
	for(EmailDocument ed: docs){
		String content = indexer.getContents(ed, true);
		content = content.replaceAll("^>+.*","");
		content = content.replaceAll("\\n\\n", ". ");
		content = content.replaceAll("\\n"," ");
		if(i%100000==0)
			System.err.println("Processed: "+i);
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
			if(matches){
				String prevSent = s>0?sentences[s-1]:null;
				String nxtSent = s<(sentences.length)?sentences[s]:null;
				String[] nbdsents = new String[]{sent,prevSent,nxtSent};
				for(String nsent: nbdsents){
					if(nsent==null)
						continue;
					String[] tokens = nsent.split("\\s+");
					for(String token: tokens){
						String type = FeatureGeneratorUtil.tokenFeature(token);
						if(type.equals("lc")&&!stopWordsSet.contains(token)){
							if(!kwgentity.containsKey(token))
								kwgentity.put(token, 0.0);
							kwgentity.put(token,kwgentity.get(token)+1.0);
						}
					}
				}
			}
		}
	}
	
	System.err.println("Searching for: "+kwgentity.size());
	//normalisation
	i=0;
	for(String kw: kwgentity.keySet()){
		if(i%100==0)
			System.err.println("Processed: "+i+" of: "+kwgentity.size());	
		i++;
		String phrase = ".*"+kw+".*";
		Collection<EmailDocument> eds = indexer.luceneLookupDocs(kw ,Indexer.QueryType.FULL);
		kwgentity.put(kw, kwgentity.get(kw)/eds.size());
	}
	List<Pair<String,Double>> skw = Util.sortMapByValue(kwgentity);
	for(Pair<String,Double> kw: skw)
		out.println(kw.first+" : "+kw.second+"<br>");
%>

</html>