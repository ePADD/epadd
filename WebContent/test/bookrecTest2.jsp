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

<%@page language="java" import="opennlp.tools.tokenize.TokenizerME"%>
<%@page language="java" import="opennlp.tools.tokenize.TokenizerModel"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<%
Archive archive = JSPHelper.getArchive(session);
archive.assignThreadIds();
Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
Indexer indexer = (Indexer) archive.indexer;
AddressBook addressbook = archive.addressBook;
SentenceDetectorME	sentenceDetector;
InputStream SentStream = NLPUtils.class.getClassLoader().getResourceAsStream("models/en-sent.bin");
SentenceModel model = null;
TokenizerME	tokenizer = null;
try {
	model = new SentenceModel(SentStream);
	InputStream tokenStream = NLPUtils.class.getClassLoader()
			.getResourceAsStream("models/en-token.bin");
	
	TokenizerModel modelTokenizer = new TokenizerModel(tokenStream);
	tokenizer = new TokenizerME(modelTokenizer);
} catch (Exception e) {
	e.printStackTrace();
	System.err.println("Exception in init'ing sentence model");
}
sentenceDetector = new SentenceDetectorME(model);

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
String stopWordsPattern = "[a-z]+";
//for places.
//String stopWordsPattern = "(of|at)";

//generaly after colon contains some stuff unnecessary like: ALABANZA: New & Selected Poems Barnes & Noble, 4 Astor Place (Lafayette & B'way) free
String nameP = "[A-Z]+[a-z':]*";
String allowedChars = "[\\s\\-\\&\\.]";
//dont write pattern as (nameP|stopWordsPattern||\\s) as that has a large effect on performance. 
String bookPattern1 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+)*"+nameP+")?";
String bookPattern2 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+)*[\"'_:\\-\\s]*[,-;])?";
String bookPattern = "("+bookPattern1+"|"+bookPattern2+")";
String bookListPattern = "("+bookPattern+"|"+"\\W)+";

// String moviePattern1 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+)*"+nameP+")?";
// String moviePattern2 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+)*[,-;])?";
// String moviePattern = "("+moviePattern1+"|"+moviePattern2+")";
// String movieListPattern = "("+moviePattern+"|"+"\\W)+";
// String infectionPattern1 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+){0,1}"+nameP+")?";
// String infectionPattern2 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+){0,1}[,-;])?";
// String infectionPattern = "("+infectionPattern1+"|"+infectionPattern2+")";
// String infectionListPattern = "("+infectionPattern+"|"+"\\W)+";

// String tripPattern1 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+){0,2}"+nameP+")?";
// String tripPattern2 = "[\"'_:\\-\\s]*"+nameP+"("+allowedChars+"+("+nameP+allowedChars+"+|"+stopWordsPattern+allowedChars+"+){0,2}([,-;]|and))?";
// String tripPattern = "("+tripPattern1+"|"+tripPattern2+")";
// String tripListPattern = "("+tripPattern+"|"+"\\W)+";

//book extraction pattern
String[] extractionPatternStrings = new String[]{
		"books\\W+such\\W+as\\W+("+bookListPattern+")",
		"book\\W+called\\W+("+bookPattern+")",
		"books\\W+like\\W+("+bookListPattern+")",
		//The pattern below pulls in some noise though is also useful sometimes.
		"book, ("+bookPattern+")",
		"book: ("+bookPattern+")",
		"book--("+bookPattern+")",
		"books\\W+including\\W+("+bookListPattern+")",
		"such\\W+books\\W+as\\W("+bookListPattern+")",
		"includes\\W+such\\W+titles\\W+as\\W+("+bookListPattern+")",
	};
String[] seedPatterns = new String[]{"books such as","book called","books like","book, ","book: ","book--","books including","such books as","includes such titles as"};

// String[] extractionPatternStrings = new String[]{
// // 	"movies\\W+including\\W+("+movieListPattern+")",
// // 	"movies\\W+such\\W+as\\W+("+movieListPattern+")",
// // 	"movies\\W+like\\W+("+movieListPattern+")",
// // 	"such\\W+movies\\W+as\\W+("+movieListPattern+")",
// // 	"movie\\W+called\\W+("+moviePattern+")",
// // 	"movie\\W+named\\W+("+moviePattern+")",
// // 	"movie\\W+like\\W+("+moviePattern+")",
// // 	"("+moviePattern+"), a\\W+film\\W+by",
//  	"("+movieListPattern+")",
//  	"("+moviePattern+")"
// };

//for places been to
// String[] extractionOutPatternStrings = new String[]{
// 	//"week\\W+in\\W("+tripListPattern+")",
// 	//"week\\W+in\\W("+tripPattern+")",	
// 	//"time\\W+in\\W+("+tripListPattern+")",
// 	//"time\\W+in\\W+("+tripPattern+")",
// 	//"month\\W+in\\W+("+tripListPattern+")",
// 	//"month\\W+in\\W+("+tripPattern+")",
// 	"trips\\W+to\\W+("+tripListPattern+")",
// 	"trips\\W+to\\W+("+tripPattern+")",
// 	"trip\\W+to\\W+("+tripListPattern+")",
// 	"trip\\W+to\\W+("+tripPattern+")",
//  	"trip\\W+to\\W+the\\W+("+tripPattern+")"
//  	//"("+tripListPattern+")\\W+trip",
//  	//"trips\\W+up\\W+to\\W+("+tripListPattern+")",
//  	//"trip\\W+up\\W+to\\W+("+tripPattern+")"
// };
// String[] extractionInPatternStrings = new String[]{
// 	"Have\\W+a\\W+nice\\W+trip\\W+to\\W+("+tripListPattern+")"
//};

//for diseases
// String[] extractionPatternStrings = new String[]{
// 	"infected\\W+with\\W+("+infectionPattern+")",
// 	"diseases\\W+like\\W+("+infectionListPattern+")",
// 	"disease\\W+like\\W+("+infectionPattern+")",
// 	"diseases\\W+such\\W+as\\W+("+infectionPattern+"|"+infectionListPattern+")",
// 	"("+infectionPattern+")\\W+result\\W+of\\W+infection",
// 	"infections\\W+like\\W+("+infectionListPattern+")",
// 	"infection\\W+like\\W+("+infectionPattern+")",
// 	"symptoms\\W+of\\W+("+infectionPattern+")",
// 	"diagonosed\\W+with\\W+("+infectionPattern+")",
// 	""
// };

// NELL Seed extraction patterns for books: "books , including _" 	"books , such as _" 	"books like _" 	"books such as _" 	"such books as _" 

//noun synonyms of book picked from wordnet
String[] syns = new String[]{"book","volume","record","record book","script","playscript", "ledger", "leger", "account book", "book of account","rule book","Koran", "Quran", "al-Qur'an","Bible", "Christian Bible", "Book", "Good Book", "Holy Scripture", "Holy Writ", "Scripture, Word of God", "Word"};
//syns of places
//String[] syns = new String[]{"place","trip","tour"};
//syns of diseases
//String[] syns = new String[]{"disease","infect","physiological condition","sick"};
// Pattern[] extractionInPattern = new Pattern[extractionInPatternStrings.length];
// Pattern[] extractionOutPattern = new Pattern[extractionOutPatternStrings.length];
// for(int i=0;i<extractionInPatternStrings.length;i++)
// 	extractionInPattern[i] = Pattern.compile(extractionInPatternStrings[i]);
// for(int i=0;i<extractionOutPatternStrings.length;i++)
// 	extractionOutPattern[i] = Pattern.compile(extractionOutPatternStrings[i]);
Pattern[] extractionPattern = new Pattern[extractionPatternStrings.length];
for(int i=0;i<extractionPatternStrings.length;i++)
	extractionPattern[i] = Pattern.compile(extractionPatternStrings[i]);

Set<String> allBooks = new HashSet<String>();
//some more extraction patterns
Map<String,Double> allpatterns = new HashMap<String,Double>();
Map<String,Set<String>> allpatternInstances = new HashMap<String,Set<String>>();
// Set<Pattern> newPatterns = new HashSet<Pattern>();
// for(Pattern ep: extractionPattern)
// 	newPatterns.add(ep);

//train sequences writer.
PrintStream tsw = null;
//additional context writer
PrintStream acw = null;
String TRAIN_FILE = "en-ner-books.train",AC_FILE="en-ner-books-ac.train";
try{
	tsw = new PrintStream(new File(TRAIN_FILE));
	acw = new PrintStream(new File(AC_FILE));
}catch(Exception e){
	e.printStackTrace();
}

//NameFinderME needs atleast 15K sentences to work reasonably. https://opennlp.apache.org/documentation/1.5.3/manual/opennlp.html#tools.namefind.training
//Not sure how they came up this number but for personal corpus the constraint is expected to be a little loose.
//2 from sent: i so much love the humor through this book: At a dinner in Kuala Lumpur where I was the guest together with a sewerage expert it's so beautiful to read/see where the poet has been placed, who with. 
//3 from: So here are the flights I would like to propose that they book: Feb 19 Delta # 2032 Depart Buffalo 12:50 PM Arrive Atlanta 3:05 PM Delta # 961 Depart Atlanta 4:10 PM Arrive Birmingham 4:05 PM Return Feb 21 Delta # 1582 Depart Birmingham 4:55 PM Arrive Atlanta 7:00PM Delta # 462 Depart Atlanta 8:10 Arrive Buffalo 10:18 PM Do you concur? 
//4 from: I got your new book, Bob! 	
//5 from: Her fourth book, Malvas orquídeas del mar, is forthcoming from Editorial Tsé-Tsé in Buenos Aires.
String[] killPhrases = new String[]{"Alan's","Given"," ","Ellie","David Antin","Fall","Charles Olson's","Denny Smith","Passage","Charles Olson's "," If I","Franz","Walt Whitman","  If I","Thirty","Carole","These","Howe's"," I","I","Selected Poems ","At a dinner in Kuala Lumpur where I","Feb","\"The","Bob","Malvas","Charles Olson's","If I Were ... I'm","I felt confident I'd","ISBN","I felt I had visited with Arnold","Mark Jarman","the","The","Poem for the Day","Alan Golding","If I","Leslie Fiedler","HAPPY ENDING coming out and I","Since","WE","--How EXCITING","Mom","America","America's","I'd","CD","WE","Kin","If I","\"It","Four","Given","Something","John","G","lt","Lo","If","I'll","I'm","This","It","Here's","Washington","Ellie","Family","Given","Ashbery","Cars","Selected Poems","If I","Kin","Four","Given","To","For","Most","Common","Una","I've","Album","Cuba","April","Now I","Sacrifice"};
//String[] killPhrases = new String[]{"January","Febraury","March","April","May","June","July","August","September","October","November","December","NYC","New York","nyc","I"};
//add killphrases and rerun
//String[] killPhrases = new String[]{};

List<String> killList = new ArrayList<String>();
for(String kp: killPhrases)
	killList.add(kp);

String[] fpp = new String[]{"I","We","Us","Our","Me","My"};
int numIter=0,maxIter = 1,numPositiveSamples=0,numNegativeSamples = 0;
do{
	int i=0,gs=0;j=0;
	Set<String> books = new HashSet<String>();
	//System.err.println("Number of patterns: "+newPatterns.size());
	for(EmailDocument ed: docs){
		if(i>0&&i%100000==0)
			System.err.println("Processed: "+i);
		i++;
//  		if(i++>5000)
//  			break;	
		String content = indexer.getContents(ed, true);
		if(content.length()>10000)
			continue;
		int x = ed.sentOrReceived(addressbook);
		//NO reply and other stuff.
		content = content.replaceAll("^>+.*","");
		content = content.replaceAll("\\n\\n", ". ");
		content = content.replaceAll("\\n"," ");
		
		//System.err.println("Detecting sentence");
		String[] sentences = sentenceDetector.sentDetect(content);
		Pattern[] newPatterns = extractionPattern;
// 		if ((x & EmailDocument.SENT_MASK) != 0)
// 			newPatterns = extractionOutPattern;
// 		else
// 			newPatterns = extractionInPattern;
		int STEP = 4;
		for(int si=0;si<sentences.length;si++){
			String s = sentences[si];
			boolean bookFound = false;
			//if(s.contains(" books ")||s.contains(" book ")||s.contains(" book: ")||s.contains(" book: ")||s.contains(" book--")){
			if(true){
			//if(s.contains("trip")||s.contains("time")||s.contains("month")||s.contains("week")){
			//if(s.contains("movie")||s.contains("film")){
				gs++;
				int idx = s.indexOf("movie");
				if(idx==-1) idx=s.indexOf("film");
				String subs = s;//s.substring(idx);
				for(Pattern ep: newPatterns){
					//System.err.println("Detecting pattern in: "+subs);
					Matcher m = ep.matcher(subs);
					while(m.find()){
						if(m.groupCount()>1){						
							//may contain books like: Art Attack: The Midnight Politics of a Guerilla Artist was published by Harper 
							//out.println("Book/books: "+m.group(1)+"\n<br>"+s+"\n<br>");
							String book = m.group(1);
							books.add(m.group(1));
						}
					}
				}
			}
			j++;
		}
		i++;
	}
	
	Set<String> cbooks = new HashSet<String>();
	for(String book: books){
		String[] abs = book.split(",\\s+");
		for(String ab: abs){
			ab = ab.replaceAll("^[\\W_]+|[\\W_]+$", "");	
			if(!Util.nullOrEmpty(ab)&&!killList.contains(ab)){
				allBooks.add(ab);
				cbooks.add(ab);
			}
		}
	}
	out.println("#"+allBooks.size()+" books found in iteration #"+numIter+"<br>");
	
	Set<String> dict = new HashSet<String>();
	//FileReader fr = new FileReader(new File("/Users/viharipiratla/repos/epadd/src/resources/words"));
// 	BufferedReader br = new BufferedReader(fr);
// 	String line = null;
// 	while((line=br.readLine())!=null)
// 		dict.add(line.trim().toLowerCase());
	
	i=0;
	Pattern bookListP = Pattern.compile(bookListPattern);
	for(EmailDocument ed: docs){
		if(i>0&i%10000==0)
			System.err.println("Processed: "+i);
		i++;
// 		if(i++>5000)
// 			break;
		
		String content = indexer.getContents(ed, true);
		content = content.replaceAll("^>+.*","");
		content = content.replaceAll("\\n\\n", ". ");
		content = content.replaceAll("\\n"," ");
		content = content.replaceAll(">+","");
		
		String[] sentences = sentenceDetector.sentDetect(content);
		for(String sent: sentences){
			boolean bookFound = false;
			String sample = sent;
			Set<String> matchingBooks = new HashSet<String>();
			for(String book: cbooks){
				if(sent.contains(book)){
					//multi-word or not in dictionary
					if(book.split(" ").length>1){//||!dict.contains(book.toLowerCase())){
						matchingBooks.add(book);
					}
				}
			}

			//for addition context generation
// 			Set<String> entityTokens = new HashSet<String>();
// 			Pattern p = Pattern.compile(bookPattern);
// 			Matcher m = p.matcher(sent);
// 			while(m.find()){
// 				String[] tokens =  tokenizer.tokenize(m.group());
// 				for(String token: tokens)
// 					entityTokens.add(token);
// 			}	
			
			for(String mb: matchingBooks){	
				boolean contained = false;
				for(String ob: matchingBooks)
					if(ob.contains(mb)&&!mb.equals(ob))
						contained = true;
				//replace with the biggest title known
				if(contained)
					continue;
				
				try{
					mb = mb.replaceAll("\\(","\\\\(");
					sample = sample.replaceAll(mb, " <START:book> "+mb+" <END> ");
					bookFound = true;
					numPositiveSamples++;
				}catch(Exception e){
					e.printStackTrace();
				}
			}
// 			String acontext = "docId: "+ed.getUniqueId()+" ::: ";
// 			acontext += "tId: "+ed.threadID+" ::: ";

// 			//not interested in mails with huge receving list
// 			if(ed.getAllNonOwnAddrs(addressbook.getOwnAddrs()).size()<5){
// 				for(String name: ed.getAllNonOwnAddrs(addressbook.getOwnAddrs()))
// 					acontext += "person: "+name+" ::: ";
// 			}
// 			Date d = ed.getDate();
// 			String time = d.getMonth()+"-"+d.getYear();
// 			acontext += "time: "+time;
			
			
// // 			//context in tokenised
//  			String tcontext = "",SEP = " ---- ";
// 			String[] tokens = tokenizer.tokenize(sent);
// 			for(int ti = 0;ti<tokens.length;ti++){
// 				String prevToken = tokens[Math.max(0,ti-1)];
// 				String nextToken = tokens[Math.min(tokens.length-1,ti+1)];
// 				String currToken = tokens[ti];
// 				if(entityTokens.contains(prevToken)&&entityTokens.contains(currToken)&&!prevToken.equals(currToken))
// 					tcontext += acontext;
// 				else if(entityTokens.contains(nextToken)&&entityTokens.contains(currToken)&&!nextToken.equals(currToken))
// 					tcontext += acontext;
// 				else
// 					tcontext += "null";
// 				if(ti<(tokens.length-1))
// 					tcontext += SEP;
// 			}
			
			if(bookFound){
				sample = sample.replaceAll("<END>  ","<END> ");
				sample = sample.replaceAll("  <START:book>"," <START:book>");
				tsw.println(sample);
				//acw.println(tcontext);
			}
			//Write as a negative sample only if none of the other syns of book are absent.
			if(!bookFound){
				Boolean bookRelated = false;
				for(String syn: syns){
					if(content.contains(syn)){
						bookRelated = true;
						break;
					}
				}
				if(!bookRelated){
					tsw.println(sent);
					//acw.println(tcontext);
					numNegativeSamples++;
				}
			}
		}
		//an empty line to mark the end of an article.
		tsw.println();
		//acw.println();
	}
	
	int i1=0;
	String recBookPattern = "";
	for(String ab: cbooks){
		String cleaned = Util.cleanForRegex(ab);
		cleaned = cleaned.replaceAll("\\W+","\\\\W\\+");
		recBookPattern += "("+cleaned+")";
		if(i1<(cbooks.size()-1))
			recBookPattern+="|";	
		i1++;
	}
	
	//Now check to see how many sentences these occur in.
	//FileWriter fw = new FileWriter(new File("en-ner-book.train"));
// 	int numOccur = 0;
// 	Map<String,Integer> freqs = new HashMap<String,Integer>();
// 	Map<String,Integer> patterns = new HashMap<String,Integer>();
// 	Map<String,Set<String>> patternInstances = new HashMap<String,Set<String>>();
// 	i=0;
// 	for(EmailDocument ed: docs){
// 		if(i>0&&i%10000==0)
// 			System.err.println("Processed(2): "+i);
// 		i++;
// 		String content = indexer.getContents(ed, false);
// 		content = content.replaceAll("\\n+","");
// 		//Dont want to rely on sentence detector
// 		String[] sentences = content.split("\\. ");
// 		String pattern = "(\\w+\\W+\\w+\\W+(\\w+)\\W+("+recBookPattern+")+)";
// 		Pattern bookP = Pattern.compile(pattern);
	
// 		for(String book: cbooks){
// 			if(content.contains(book)){
// 				for(String sent: sentences){
// 					if(!Util.nullOrEmpty(book)&&!book.equals("I")&&sent.contains(book)){
// 						if(!sent.matches(".*?"+pattern+".*"))
// 							continue;
// 						if(freqs.containsKey(book))
// 							freqs.put(book,freqs.get(book)+1);
// 						else freqs.put(book,1);
// 						numOccur++;
// 						book = book.replaceAll("^\\(+|\\)+$","");
// 						Matcher m = bookP.matcher(sent);
// 						while(m.find())
// 							if(m.groupCount() > 3 && m.group(2)!=null && m.group(3)!=null){
// 								String p = m.group(1);
// 								p = p.replaceAll("^("+stopWordsList+")","");
// 								p = p.replaceAll(">+","");
// 								p = p.replaceAll("^\\W+|\\W+$","");
// 								try{
// 									p = p.replaceAll(m.group(3), "*");
// 									//System.err.println(book+", "+p+", "+m.group(3)+" ::: "+sent);
// 									if(!patternInstances.containsKey(p))
// 										patternInstances.put(p,new HashSet<String>());
									
// 									patternInstances.get(p).add(m.group(3));
// 									patterns.put(p,patternInstances.get(p).size());
// 									break;
// 								} catch(Exception e){
// 									//System.err.println("Pattern syntax exception with:"+book+", "+p+", "+m.group(2)+" ::: "+sent);
// 									//e.printStackTrace();
// 									break;
// 								}
// 							}		
// 					}
// 				}
// 			}
// 		}
// 	}

// 	List<Pair<String,Integer>> pairs = Util.sortMapByValue(freqs);
// 	List<Pair<String,Integer>> sortedPatterns = Util.sortMapByValue(patterns);
// 	Indexer li = indexer;
	
// 	Map<String,Double> patternScores = new HashMap<String,Double>(); 
// 	for(Pair<String,Integer> p: sortedPatterns){
// 		out.println(p.first+" : "+p.second+" ::: "+patternInstances.get(p.first)+"\n<br>");
// 		if(p.second>1){
// 			String phrase = p.first.replaceAll("\\s\\*+", "");
// 			phrase = phrase.replaceAll("\"","");
// 			phrase = Util.cleanForRegex(phrase);
// 			phrase = ".*"+phrase+".*";
// 			System.err.println("Looking up for: "+phrase);
// 			Collection<EmailDocument> eds = li.lookupDocs(phrase ,Indexer.QueryType.REGEX);
// 			String pattern = "("+p.first+"\\W+("+bookListPattern+"|"+bookPattern+")+)";
// 			Pattern bookP = Pattern.compile(pattern);
// 			Pattern patternP = Pattern.compile(p.first);
// 			int bs = 0, total = 0;
// 			for(EmailDocument ed: eds){
// 				String content = indexer.getContents(ed, false);
// 				content = content.replaceAll("\\n+"," ");
// 				Matcher m = bookP.matcher(content);
// 				Matcher pm = patternP.matcher(content);
// 				while(pm.find())
// 					total++;
// 				while(m.find())
// 					if(m.groupCount()>2)
// 						bs++;			
// 			}
// 			patternScores.put(p.first,(double)bs/(double)total);
// 		}else
// 			//because its a sorted list
// 			break;
// 	}
	
// 	List<Pair<String,Double>> impPatterns = Util.sortMapByValue(patternScores);
// 	int prevNumPatterns = allpatterns.size();
// 	newPatterns = new HashSet<Pattern>();
// 	System.err.println("Important patterns:");
// 	for(Pair<String,Double> impPattern: impPatterns)
// 		System.err.println(impPattern.first+" : "+impPattern.second);
	
// 	for(Pair<String,Double> impPattern: impPatterns){
// 		//This is tolerance to error.
// 		if(impPattern.second<0.8)
// 			break;
		
// 		if(allpatterns.size()==0){
// 			Boolean subsetP = false;
// 			for(String ep: seedPatterns){
// 				if(impPattern.first.contains(ep)){
// 					subsetP = true;
// 					System.err.println(ep+" is subset of: "+impPattern);
// 					break;
// 				}
// 			}
// 			if(!subsetP){
// 				allpatterns.put(impPattern.first, impPattern.second);
// 				allpatternInstances.put(impPattern.first, patternInstances.get(impPattern.first));
// 				String pat = impPattern.first.replaceAll("\\W+", "\\\\W\\+");
// 				pat = pat.replaceAll("\\*","");
// 				pat = pat + "("+bookPattern+"|"+bookListPattern+")";
// 				System.err.println("Adding pattern: "+pat);
// 				newPatterns.add(Pattern.compile(pat));
// 			}		
// 		}
// 		else{
// 			boolean subsetP = false;
// 			for(String ep: allpatterns.keySet())
// 				if(impPattern.first.contains(ep)&&!impPattern.first.equals(ep)){
// 					subsetP = true;
// 					System.err.println(ep+" is subset of: "+impPattern);
// 					break;
// 				}
// 			if(!subsetP){
// 				String gp = impPattern.first;
// 				if(!allpatterns.containsKey(gp)){
// 					allpatterns.put(gp, 0.0);
// 					String pat = gp.replaceAll("\\W+", "\\\\W\\+");
// 					pat = pat.replaceAll("\\*","");
// 					pat = pat + "("+bookPattern+"|"+bookListPattern+")";
// 					System.err.println("Adding pattern: "+pat);
// 					newPatterns.add(Pattern.compile(pat));
// 				}else
// 					System.err.println("Already known pattern: "+gp);
// 				if(!allpatternInstances.containsKey(gp))
// 					allpatternInstances.put(gp, new HashSet<String>());
// 				allpatterns.put(gp, allpatterns.get(gp)+1);
// 				allpatternInstances.get(gp).addAll(patternInstances.get(gp));
// 			}
// 		}
// 	}
	
// 	if((numIter==maxIter)||allpatterns.size()==prevNumPatterns){
// 		for(String book: allBooks)
// 			out.println(book+"<br>");
		
// 		out.println("Learned extraction patterns<br>");
// 		List<Pair<String,Double>> sallp = Util.sortMapByValue(allpatterns);
// 		for(Pair<String,Double> p: sallp)
// 			out.println(p.first+" : "+p.second+" ::: "+allpatternInstances.get(p.first)+"<br>");
// 	}
// 	if(allpatterns.size()==prevNumPatterns){
// 		System.err.println("No new patterns added in iteration:#"+numIter+", breaking...");
// 		break;
// 	}
// 	System.err.println("#"+(allpatterns.size()-prevNumPatterns)+" added in iteration: "+numIter);
// 	System.err.println("Iter:"+numIter+" of "+maxIter+" done.");
}while(++numIter<maxIter);
String [] nerBooks = new String[]{"After Happily Ever After","Turn the Wheel","The Arts","FAMOUS LAST WORDS","TOMORROW IS NOW","Water Music","Uncertain Poetries","The American Evasion of Philosophy","Fuck You","Cincinnati","Battery Light","Extraordinary Measures","The Fire Within","House of Leaves","For Fear of the Jews","Alan Semerdjian","The Love","Prison","An End to Innocence","Common Zens","Life and Death","The Arts Paper","LETTERS TO J. D. SALINGER","Common Review","The Torturers","The Listening","Winter Light","A Love Supreme","Endorsed by Jack Chapeau","Leslie Fiedler","HAPPY SUMMER","IF I WERE WRITING","MY WAY","A","O","David Antin","The Bubble of American Supremacy","A Secret Location","Wave Forms","The Mind","Dan Jordan","BLACK MOUNTAIN COLLEGE","O Throat","Lackawanna Lives","IF I WERE WRITING THIS","Life & Death","Life & Time","Flash Fire","Nixon Off the Record","Love and Death","The Holocaust in American Life","Surely I","Portraits and Elegies","The Flower","Water Sickness","Ring of Bone","Maybe","THE SECTION","Memory I","Life","Leslie Fiedler Symposium","Memory Cards & Adoption Papers","O Messenger","Jumping the Line","Past Midnight","THE WRITER","IF I WERE WRITING THIS. a","John","Artist and Model","A Prayer for America","Open Slowly","Blue","Nickel City","For I","A Hard Day","Third Light Poem","Rattapallax Press","A Glyph","After Midnight","Native Sons","The Island","Ears on Fire","Uncertain Grace","HAPPY CHRISTMAS","Her Wild American Self","Round Midnight","A Speech","Which Way","Frozen Spring","For Cid","The American","The Hat","The Golden Book of Words","A Bookshop Idyll","The Love Book","A Girl","For You","Your","aly","Blind Eye","Happy Birthday","The Keep","Alan","RECLAIMING LAWRENCE","Dear God","The Emily XYZ Songbook","Western Capital Rhapsodies","Kenneth Patchen Howe","The Door","No Way","Fuck With Love","RECLAIMING","The Way","Dan Kaplan","Visiting Langston","Calder Boyars","In the Dream Hole","AS USUAL","Million Poems Journal","American Dynasty","The Poet","Ears","The Bookshop","The Bondswoman","reading Golding","Animal Farm","Leslie Scalapino","The Browser","The Dial","The Poem That Changed America","A Dialogue on Love","A Child","Pathways","The Midnight","The Rescue","The Rum Diary","I 'm","Black Mountain","Open Secret","Plan of Attack","Blair","The Taliban","File Under Popular","A Master","O Ocean","The Gesture","The Arts at Black Mountain College","David Bottoms","Learning to Draw","Presentation Piece","David Dunn","WC Williams","Common","MY MENTOR","Welcome Overboard","Water","Adina","Not That","The Road","The Charm","Love and Understanding","A Piece","The Last Nazi","For W.C.W","The Gift","Punishment","A World of Difference","Shahrnush launched into her","The Acquaduct","Monster Fashion","Water Up Ahead","Fuck You-Aloha-I Love You","The American Dream","LIP SERVICE","The Cabbage","The Pink Church","For Will","Bach","Electric Light","The Address Book","Marc Sloan","AS EVER","Travels With My Ape","For Richer","Table of Forms","IF","Sleeping Where I Fall","O Bob","The Garden","Oh God","God Save The","Penis","O God","Kenneth Sherwood","Eleni Sikelianos","Common Dreams","Lolita","Loose Sugar and Cascadia","If","William Golding","Alan Rowlin","The Last Beatnik","Postmodernism","The Europe of Trusts or Pierce-Arrow","Nixon","Short Fuse","The Business","The Writer","The Ball","Chapter","Can You Relax in My House","The Potato","Kenneth Lay","Fuck","Mirrors","Brown will appear in","IF EVER","The Sandman","The American Century","Wolves","Blue Water Grill","Fuck Off","World Hotel","O Patriarchy","A Poet","Alan Golding","The Bather","The Toast","BLACK MOUNTAIN","Juniper","Digital Poetics","Poetry Sanctuary","The CD","Screw Europe","Leslie Fieldler","Ring of Gold","THE HISPANIC CONDITION","Rise Up","The Fool","Kenneth Koch","A Pact","HAPPY HOLIDAYS","The Business of Fancydancing","The Body","For Joel","Hazmat","When","W+Golding","Love","Nixon said","The American Obsession","Calder and Boyars","The Bell","Visiting Instructor","Work Sonnets","The","Dictionary of Silence","Love and Theft","Happy Birthday President Bush","The Fire","Just in Time","Fuck you Adam","The Sound","MY LAST","The Cloud of Knowable Things","Lackwanna Lives","Sia","For Kenneth","The Pearl Fishers","Darkness Visible","The Fugitive","Dancing In Odessa","Loose Joints","The Capeman","Alan Halsey","Letters to Salinger","God Save My Queen","Being Busted","Blue Mark","The Science of Science and Reflexivity","Saving Lives and Saving Dollars","Ring Fallujah","Sex","O Bomb","David Ball","The Untraceable","Test Of Poetry","Alan Golding Publisher","Kenneth","Dancing in","Ring Cycle","The Wedge","Mind","Carol","GEEZER LOVE","A Whitman","Jacob","Fuck This","The Crafty Reader","Just in Time_","The Last Samurai","FAMOUS AMERICANS","Love Defined","If I Were Writing This","Alan Foley","Nice Hat","Range Finder","If I Told Him","HAPPY BIRTHDAY","9/11","FAMOUS AGAIN","The God","O Books","I CRY LIKE A BABY","For Fun","THE POETRY ANTHOLOGY","If I Were","Kenneth Patchen","The Eye","Circus Magazine","If I Were Writing This.pdf","The Language","Kenneth Goldsmith","Juniper Festival Amherst","Franks wrote that Feith","Fuck Dan Quayle","If I Were Writing ThisThanks","David","Fuck Faas","Surely","The Land of Milk & Honey","Ballads","pbglawson@hotmail.com","Christening the Dancer","Poetry Speaks published by Source Books","Grave Concerns","Penis Poems","The Danubians","Florsheim","Heart Print","Love and Death in the American Novel","The Poetics of Chance","Life","For Love","Dancing in Odessa","The Successor","The Dark","Can Poetry Matter","Everybody","Rituals of Truce and the Other Israeli","published Charm","Lies","For Graham","Tyrant","The Body of","The Towers","Ring of Fire","Ovarian Twists","Everybody Loves Nothing","The Saint","David Fox","O Kenneth","On the Cave You Live In","Rattapallax","The Party","The Women","How the South Finally Won the Civil War and Controls the Political Future of the U.S","I AM THAT HERO","Joe Elliot","ARCTIC DREAMS","Creating Fiction","JAZZ TEXT","A VIOLENT ACT","IF I WERE","HAPPY ENDING","The Twist","The Arts Paper/Protest","Four from Milwaukee","Pacific","The Fire Continues","Fuck you","BAY OF SOULS","Just","Marcella","Darkness","The End","Minnis","Digital Essays","Back Through Interruption","Land of Jihad Tour","For Creeley","Juniper Fuse","Disorderly Conduct","THE CHARM","Bouncing Ball","Moving My Mother","Politically Correct Gardening","Leslie","If I","FROZEN SPRING","Common Sense","Franks","Interlocking Lives","Saturday Night","Digital Resistance","Mariana Ruiz-Firmat","David Young","The Duplications","Circus Nerves","The Random House Guide to Writing and","Uncertain Allegiance","Travels","Ovarian Twist","WHEN YOUR PARENT DRINKS TOO MUCH","For Anya","No More Strangers","IF EVER THERE","The Whip","A Tally","The God of Neglect","A Day Book","A Citizen","O Polybius","The Weather","The Mechanic","By God","Hi Hat","Fading Light","Anatman","Illness","Olson","Nice","O Cristal","Winning To Lose","Disordered Ideas","Many Loves","Warp Spasm","Routine Disruptions","A Poetry Evening","Hazard","The President","The Heart","Hot","A Map of the Mind","When Law Goes Pop","If Cheney","Winter","Midnight Poet","The Fire Within Us","The Idiot","Eigner","The Beat Generation","If I Scratch","Harwood","For","The God of Small Things","IF YOU","Just in","The Life & Death","RATTAPALLAX SPONSORS","The Dark Months of May","A Hero","Non","A Story","Saturday Night Live","God","The Guardian"};
//String[] nerBooks = new String[]{"Key","Pinsky?s Jersey","Los Angeles","West","Providence May","his Jersey","Europe","Italy","France","over","Key in","North","Cambridge","Providence","June","Texas","Tucson","Jersey","South Waldoboro","Paris","Maine","New Jersey","North Jersey","Waldoboro","Key West I","Los","waldoboro","Women","Providence Bridge","Bridge","Boston","South","Los Angeles Times","South Jersey","Key West February","FRANCE","another","Florence","Farnsworth","February","398 Jersey","Rome","May","Maine June","Key Bridge","Los Angeles WBEZ","South America","New","Woodstock","Angeles","the","PROVIDENCE","in","LA","America","usually Jersey","Key West","WALDOBORO","398"};
int found = 0;
for(String book: allBooks)
	out.println(book+"<br>");

out.println("<br><br>Novel books with NER<br>");
for(String nb: nerBooks){
	boolean f = false;
	for(String book: allBooks){
		if(nb.equalsIgnoreCase(book)){
			f = true;
			break;
		}
	}
	if(!f){
		out.println(nb+"<br>");
		found++;
	}
}
out.println("#"+found+" books novel with NER recognised books");

tsw.close();
System.err.println("Wrote #"+numPositiveSamples+" positive samples and #"+numNegativeSamples+" total: #"+(numPositiveSamples+numNegativeSamples)+" to "+new File(TRAIN_FILE).getAbsolutePath());

//System.err.println("Total num of docs: "+i);
//System.err.println("Total num of Sentences: "+j);
//System.err.println("Total num of Good sentences: "+gs);
%>