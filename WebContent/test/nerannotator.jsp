<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="java.util.regex.*"%>
<%@page import="org.apache.lucene.analysis.util.CharArraySet"%>
<%@page import="org.apache.lucene.analysis.core.StopAnalyzer"%>
<%@page import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>	
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.epadd.util.*"%>
<%@page language="java" import="edu.stanford.epadd.ie.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>

<%@page language="java" import="opennlp.tools.namefind.NameFinderME"%>
<%@page language="java" import="opennlp.tools.namefind.TokenNameFinderModel"%>
<%@page language="java" import="opennlp.tools.sentdetect.*"%>
<%@page language="java" import="opennlp.tools.tokenize.TokenizerME"%>
<%@page language="java" import="opennlp.tools.tokenize.TokenizerModel"%>
<%@page language="java" import="opennlp.tools.util.Span"%>
<%@page import="edu.stanford.muse.util.Util"%>
<%@ page import="edu.stanford.muse.ie.KnownClasses" %>

<% 
Archive archive = JSPHelper.getArchive(session);
Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
Indexer indexer = archive.indexer;
AddressBook ab = archive.addressBook;
NameFinderME bFinder = null;
String BOOK="book",UNIV="university",MUSIC_ARTIST="musical_artist",HOTEL="hotel",MUSEUM="museum",COMPANY="company",AWARD="award",MOVIE="movie", PEOPLE = "people"; 
SentenceDetectorME	sentenceDetector;
InputStream SentStream = NLPUtils.class.getClassLoader().getResourceAsStream("models/en-sent.bin");
SentenceModel model = null;
TokenizerME	tokenizer = null;
CharArraySet stopWordsSet = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
String[] stopWords = new String[stopWordsSet.size()];
Iterator it = stopWordsSet.iterator();
int j = 0;
while(it.hasNext()){
	char[] stopWord = (char[]) it.next();
    stopWords[j++] = new String (stopWord);	
}

String stopWordsList = "";
for(int i=0;i<stopWords.length;i++){
	String stopWord = stopWords[i];
	stopWordsList+=stopWord;
	if(i<(stopWords.length-1))
		stopWordsList+="|";
}
stopWordsList += "";

KnownClasses kc = new KnownClasses();
String rt = request.getParameter("type");
String rc = request.getParameter("color");
String[] types,colors;
if(rt==null||rc==null){
	types = new String[]{KnownClasses.BOOK,KnownClasses.UNIV,KnownClasses.MUSEUM,KnownClasses.COMPANY,KnownClasses.AWARD,KnownClasses.MOVIE}; 
 	colors = new String[]{"green","red","deepskyblue","orange","violet","fuchsia"};
}else{
	types = new String[]{rt}; 
 	colors = new String[]{rc}; 
}

String[] modelFiles = new String[types.length];
String modeldir = archive.baseDir+File.separator;
for(int i=0;i<types.length;i++)
	modelFiles[i] = modeldir+"models/en-ner-"+types[i]+".bin";

//modelFiles = new String[]{"models/en-ner-book.bin","models/en-ner-university.bin","models/en-ner-museum.bin","models/en-ner-company.bin","models/en-ner-award.bin","models/en-ner-movies.bin"};
//String[] types = new String[]{PEOPLE};
//String[] colors = new String[]{"red"};
//Map<String,Pattern> patterns = new HashMap<String,Pattern>();
//Map<String,Pattern> listPatterns = new HashMap<String,Pattern>();
Map<String,String> stopWordsPattern = new HashMap<String,String>();

for(String type: types){
	String nameP = "[A-Z]+[a-z':]*";
	String allowedChars = "[\\s\\-\\&\\.]";
	String swPattern = "("+stopWordsList+")";//"[a-z]+";
	if(type.equals(BOOK)||type.equals(MOVIE))
		swPattern = "(but|be|with|such|then|for|no|will|not|are|and|their|if|this|on|into|a|there|in|that|they|was|it|an|the|as|at|these|to|of)";
	//for universities
	else if(type.equals(UNIV)||type.equals(MUSEUM))
		swPattern = "(for|and|a|the|of)";
	else if(type.equals(MUSIC_ARTIST))
		swPattern = "";
	else if(type.equals(AWARD))
		swPattern = "(of|and|a|an|on|in)";
	else if(type.equals(COMPANY))
		swPattern = "(of|and|a|an|on)";
	else if(type.equals(HOTEL))
		swPattern = "(of)";
	else
		swPattern = "(but|be|with|such|then|for|no|will|not|are|and|their|if|this|on|into|a|there|in|that|they|was|it|an|the|as|at|these|to|of)";
				
	String bookPattern1 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+)*"+nameP+")?";
	String bookPattern2 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+)*[\"'_:\\-\\s]*[,-;])?";
	String bookPatternString = "("+bookPattern1+"|"+bookPattern2+")";
	//String bookListPatternString = "("+bookPatternString+"|"+"\\W)+";
	//Pattern bookPattern = Pattern.compile(bookPatternString);
	//Pattern bookListPattern = Pattern.compile(bookListPatternString);
	//patterns.put(type,bookPattern);
	//listPatterns.put(type,bookListPattern);
	stopWordsPattern.put(type,swPattern);
}

//String[] modelFiles = new String[]{"models/en-ner-people.bin"};
NameFinderME[] finders = new NameFinderME[modelFiles.length];
Map<String,Set<String>> entities = new HashMap<String,Set<String>>(); 
try {
	int i=0;
	for(String modelFile: modelFiles){
		//InputStream pis = NLPUtils.class.getClassLoader().getResourceAsStream(modelFiles[i]);
		System.err.println("Loading: "+modelFiles[i]);
		TokenNameFinderModel nmodel = new TokenNameFinderModel(new FileInputStream(modelFiles[i]));
		finders[i] = new NameFinderME(nmodel);
		i++;
	}
	model = new SentenceModel(SentStream);
	InputStream tokenStream = NLPUtils.class.getClassLoader()
			.getResourceAsStream("models/en-token.bin");
	TokenizerModel modelTokenizer = new TokenizerModel(tokenStream);
	tokenizer = new TokenizerME(modelTokenizer);
} catch (Exception e) {
	e.printStackTrace();
}
sentenceDetector = new SentenceDetectorME(model);

int l =0,numNames = 0;
List<Integer> cmup = new ArrayList<Integer>(), nerup = new ArrayList<Integer>();
for(EmailDocument ed: docs){	
	String content = indexer.getContents(ed, false);
	content = content.replaceAll("^>+.*","");
	content = content.replaceAll("\\n\\n", ". ");
	content = content.replaceAll("\\n"," ");
	content.replaceAll(">+","");
	String[] sents = sentenceDetector.sentDetect(content);
// 	 	if(l++>1000)
// 	 		break;
	l++;
	if(l>0&&l%1000==0){
		System.err.println("Processed: "+l);
		System.err.println("#"+numNames+" found");
		for(String t: entities.keySet())
			System.err.println("type:"+t+"\t"+"#"+entities.get(t).size());
	}
	int numRecognised = 0;
	//List<String> names = indexer.getEntitiesInDoc(ed, "person");
	//gnerNames.addAll(names);
	
	for (int i = 0; i < sents.length; i++) {
		String text = sents[i];
		Span[] tokSpans = tokenizer.tokenizePos(text);
		// Sometimes there are illformed long sentences in the text that
		// give hard time to NLP.
		if (tokSpans.length > 1288)
			continue;

		String tokens[] = new String[tokSpans.length];
		for (int t = 0; t < tokSpans.length; t++) {
			tokens[t] = text.substring(
					Math.max(0, tokSpans[t].getStart()),
					Math.min(text.length(), tokSpans[t].getEnd()));
		}
		for(int fi=0; fi<finders.length;fi++){
			NameFinderME finder = finders[fi];
			Span[] bSpans = finder.find(tokens);
			Set<String> cb = new HashSet<String>();
			
			for(Span span: bSpans){
				String name = "",pname="";
				int start =-1,end=-1;
				for(int m=span.getStart();m<span.getEnd();m++){
					name += text.substring(
							tokSpans[m].getStart(),
							tokSpans[m].getEnd());
					pname += text.substring(
							tokSpans[m].getStart(),
							tokSpans[m].getEnd());
					if(m<(span.getEnd()-1)){
						name+="\\W+";
						pname+=" ";
					}
				}
				start = span.getStart();
				end = span.getEnd();
				String type = span.getType();
				
				cb.add(name);
				//candidate name
				String cname = name,cpname=pname;
				String lastcname = name,lastcpname = pname;
				boolean clean = false;
				for(int nt = end;nt<tokens.length;nt++){
					String token = tokens[nt];
					char fc = token.charAt(0);
					//System.err.println("fc: "+fc+", token: "+token+", name: "+name);
					
					if(stopWordsPattern.get(type)!=null&&stopWordsPattern.get(type).contains(token)){
						cname += "\\W+"+token;
						cpname += " "+token;
						clean = false;
					}
					else if(fc>='A'&&fc<='Z'){
						cname += "\\W+"+token;
						cpname += " "+token;
						clean = true;
					}
					else{
						boolean allowedT = false;
						if(token.equals("'")||token.equals(":")||token.equals("&")){
							allowedT = true;
							clean = false;
						}
						if(!allowedT)
							break;
					}
					if(clean){
						lastcname = cname;
						lastcpname = pname;
					}
				}
				if(clean){
					name = cname;
					pname = cpname;
				}else{
					name = lastcname;
					pname = lastcpname;
				}
				
				if(!entities.containsKey(type))
					entities.put(type, new HashSet<String>());
				
				entities.get(type).add(pname);
				text = text.replaceAll("\"", "").replaceAll(">|<"," ");
				name = name.replaceAll("\"", "");
				pname = pname.replaceAll("\"","");
				//out.println(name+"::: &nbsp&nbsp");
				try{
					String color = "yellow";
					if(kc.colors.containsKey(type))
						color = kc.colors.get(type);
					text = text.replaceAll(name,"<span style=\"color:"+color+"\"> {"+type+" : "+pname+"}</span>");
					break;
				}catch(Exception e){
					System.err.println("Exception while replacing pattern");
				}
			}
			numNames+=bSpans.length;
			if(bSpans.length>0){
				out.println(text+"<br>");
			}
		}
	}
}
int tu = 0;
for(String t: entities.keySet()){
	PrintWriter pw = new PrintWriter(new File(t+".txt"));
	if(entities.get(t)!=null){
		for(String e: entities.get(t))
			pw.println(e);
		tu += entities.get(t).size();
	}
	pw.close();
}
// for(String t: gnerNames){
// 	PrintWriter pw = new PrintWriter(new File("nerpeople.txt"));
// 	for(String e: gnerNames)
// 		pw.println(e);
// 	pw.close();
// }
out.println("Toatal: "+numNames+" found"+" unique names: #"+tu);//" found by ner: "+gnerNames.size());
%>
