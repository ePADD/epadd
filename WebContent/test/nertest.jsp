<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="java.util.regex.*"%>
<%@page import="org.apache.lucene.analysis.util.CharArraySet"%>
<%@page import="org.apache.lucene.analysis.core.StopAnalyzer"%>
<%@page import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>	
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.epadd.util.*"%>
<%@page language="java" import="edu.stanford.epadd.ie.*"%>
<%@page language="java" import="edu.stanford.epadd.ie.WordFeatures.WordFeatureVector"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>

<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>

<%@page language="java" import="opennlp.tools.namefind.NameFinderME"%>
<%@page language="java" import="opennlp.tools.namefind.TokenNameFinderModel"%>
<%@page language="java" import="opennlp.tools.sentdetect.*"%>
<%@page language="java" import="opennlp.tools.tokenize.TokenizerME"%>
<%@page language="java" import="opennlp.tools.tokenize.TokenizerModel"%>
<%@page language="java" import="opennlp.tools.util.Span"%>

<%@page language="java" import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="edu.stanford.muse.util.Util"%>
<%@page import="libsvm.*"%>
<style>
.class1{
	color: rgb(197, 16, 16);
}
.class2{
	background-color: green;
}
.class3{
	background-color:yellow;
}
class4{
	background-color:sky-blue;
}
class5{
	color: blue;
}
</style>
<% 
Archive archive = JSPHelper.getArchive(session);
List<Document> docs = archive.getAllDocs();
Indexer indexer = archive.indexer;
AddressBook ab = archive.addressBook;
Set<String> cnames = EmailUtils.getNames(ab.allContacts());

String[] types = new String[]{"person-walstreet"};
String[] modelFiles = new String[types.length];
String[] preTags = new String[]{"<span class=\"class3\">","<span class=\"class4\">","<span class=\"class5\">"};
String[] postTags = new String[]{"</span>","</span>","</span>"};
svm_model svmModel = svm.svm_load_model(new BufferedReader(new InputStreamReader(EmailUtils.class.getClassLoader().getResourceAsStream("person_svm.model"), "UTF-8")));
int nr_class=svm.svm_get_nr_class(svmModel);
double[] prob_estimates = new double[nr_class];

List<Contact> contacts = archive.addressBook.allContacts();

Map<String, String> dbpedia = EmailUtils.readDBpedia();
System.err.println("Generating features.");
WordFeatures wfs = edu.stanford.epadd.ie.EmailUtils.generateFeatures(cnames,dbpedia);
System.err.println("Done generating features.");

List<Set<String>> recnames = new ArrayList<Set<String>>();
for(int x=0;x<3;x++)
 recnames.add(new HashSet<String>());

int[] freqs = new int[]{0,0};

String modeldir = edu.stanford.epadd.Config.BASE_DIR;
for(int i=0;i<types.length;i++){
	modelFiles[i] = modeldir+"models/en-ner-"+types[i]+".bin";
	System.err.println("Model file: "+modelFiles[i]);
}

NameFinderME[] finders = new NameFinderME[modelFiles.length];
Map<String,Set<String>> entities = new HashMap<String,Set<String>>(); 
SentenceDetectorME	sentenceDetector;
InputStream SentStream = NLPUtils.class.getClassLoader().getResourceAsStream("models/en-sent.bin");
SentenceModel model = null;
TokenizerME	tokenizer = null;
try {
	int i=0;
	for(String modelFile: modelFiles){
		//InputStream pis = NLPUtils.class.getClassLoader().getResourceAsStream(modelFiles[i]);
		TokenNameFinderModel nmodel = new TokenNameFinderModel(new File(modelFiles[i]));
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

Random randnum = new Random();
int NUM = 100;
for(int k=0;k<NUM;k++){
	EmailDocument ed = (EmailDocument)docs.get(randnum.nextInt(docs.size()));
	int x = ed.sentOrReceived(ab);
	
	String content = indexer.getContents(ed, false);
	content = content.replaceAll("^>+.*","");
	content = content.replaceAll("\\n\\n", ". ");
	content = content.replaceAll("\\n"," ");
	content.replaceAll(">+","");
	content = content.replaceAll("\"", "").replaceAll(">|<"," ");
		
	String[] sents = sentenceDetector.sentDetect(content);
	l++;
	if(l>0&&l%1000==0){
		System.err.println("Processed: "+l);
		System.err.println("#"+numNames+" found");
		for(String t: types)
			if(entities.containsKey(t))
				System.err.println("type:"+t+"\t"+"#"+entities.get(t).size());
	}
	int numRecognised = 0;
	
	for (int i = 0; i < sents.length; i++) {
		String text = sents[i];
		Set<String> names = Indexer.getNamesFromPatterns(text, true);
		boolean contains = false;
		for(String name: names){
			if(cnames.contains(name)){
				recnames.get(0).add(name);
				text = text.replaceAll(name,"<span class=\"class1\">"+StringEscapeUtils.escapeHtml(name)+"</span>");
				contains = true;
			}
		}
		if(contains)
			out.println(text+"<br>");
		
		contains = false;
		text = sents[i];
		for(String name: names){
			WordFeatureVector wfv = new WordFeatureVector(WordFeature.compute(name, wfs),wfs);
			svm_node[] svm_x = wfv.getSVMNode();
			double v = svm.svm_predict(svmModel, svm_x);
			if(v>0){
				System.err.println("Postive phrase: "+name+" found with probs "+prob_estimates[0]+", "+prob_estimates[1]);
				recnames.get(0).add(name);
				text = text.replaceAll(name,"<span class=\"class2\">"+StringEscapeUtils.escapeHtml(name)+"</span>");
				contains = true;
			}
		}
		if(contains)
			out.println(text+"<br>");
		
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
			text = sents[i];
			NameFinderME finder = finders[fi];
			Span[] bSpans = finder.find(tokens);
			Set<String> cb = new HashSet<String>();
			
			outer:
			for(Span span: bSpans){
				String name = "",pname="";
				int start =-1,end=-1;
				for(int m=span.getStart();m<span.getEnd();m++){
					int s = tokSpans[m].getStart(), e = tokSpans[m].getEnd();
					if(s<0||s>=text.length())
						continue;
					if(e<0||e>=text.length())
						continue;
					name += text.substring(
							tokSpans[m].getStart(),
							tokSpans[m].getEnd());
					pname += text.substring(
							tokSpans[m].getStart(),
							tokSpans[m].getEnd());
					if(m<(span.getEnd()-1)){
						pname+=" ";
					}
				}
				start = span.getStart();
				end = span.getEnd();
				
				//name = Util.cleanForRegex(name);
				//System.err.println("Replacing: "+name);
				cb.add(name);
				//candidate name
				String cname = name,cpname=pname;
				String lastcname = name,lastcpname = pname;
				boolean clean = false;
				
				if(!entities.containsKey(types[fi]))
					entities.put(types[fi], new HashSet<String>());
				
				entities.get(types[fi]).add(pname);
				pname = pname.replaceAll("\"","");
	
				recnames.get(fi+1).add(pname);
				try{
					String preTag = preTags[fi];
					String postTag = postTags[fi];
					text = text.replaceAll(pname, preTag+pname+postTag);
					break;
				}catch(Exception e){
					System.err.println("Exception while replacing pattern");
				}
			}
			numNames+=bSpans.length;
			if(bSpans.length>0)
				out.println("<br>"+text+"<br>");
			freqs[fi]+=bSpans.length;
		}
	}
	out.println("<br>----------------------------------------------<br>");
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
out.println("Toatal: "+numNames+" found"+" unique names: #"+tu);//" found by ner: "+gnerNames.size());
for(int fi=0;fi<types.length;fi++)
	out.println("Found: "+freqs[fi]+" entities of type: "+types[fi]);

FileWriter fw = new FileWriter(new File("AB.txt"));
for(String str: recnames.get(0))
	fw.write(str+"\n");
fw.close();
fw = new FileWriter(new File("ner.txt"));
for(String str: recnames.get(1))
	fw.write(str+"\n");
fw.close();
fw = new FileWriter(new File("ner-walstreet.txt"));
for(String str: recnames.get(2))
	fw.write(str+"\n");
fw.close();
%>