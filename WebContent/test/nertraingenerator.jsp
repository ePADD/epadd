<%@page import="edu.stanford.muse.email.AddressBook"%>
<%@page import="edu.stanford.muse.email.Contact"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.epadd.util.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="opennlp.tools.sentdetect.*"%>
<%@page language="java" import="opennlp.tools.util.featuregen.FeatureGeneratorUtil"%>
<%@page language="java" import="java.io.*"%>

<%@page language="java" import="opennlp.tools.util.TrainingParameters"%>
<%@page language="java" import="opennlp.tools.util.featuregen.*"%>
<%@page language="java" import="opennlp.tools.util.*"%>
<%@page language="java" import="opennlp.tools.namefind.*"%>
<%@page language="java" import="opennlp.tools.cmdline.*"%>
<%@page language="java" import="java.nio.charset.Charset"%>
<%@page language="java" import="opennlp.tools.util.PlainTextByLineStream"%>

<%@page language="java" import="com.google.gson.Gson"%>
<%@page language="java" import="edu.stanford.muse.ie.KnownClasses"%>

<!DOCTYPE html>
<%
	System.err.println("Request to write training file... received.");
	String[] entities = request.getParameterValues("entities[]");
	String[] syns = request.getParameterValues("syns[]");
	String type = request.getParameter("type");
	System.err.println("syns: "+syns+", type: "+type);
	
	String status = "Starting to process";
	KnownClasses kc = null;
	Gson gson = new Gson();
	try{
		FileReader fr = new FileReader(new File(edu.stanford.muse.Config.SETTINGS_DIR+File.separator+"known_classes.json"));
		kc = gson.fromJson(fr, KnownClasses.class);
		fr.close();
	}catch(Exception e){
		System.err.println("Cannot read from: "+edu.stanford.muse.Config.SETTINGS_DIR+File.separator+"known_classes.json");
		e.printStackTrace();
	}
	if(kc==null)
		kc = new KnownClasses();
	kc.syns.put(type, syns);
	String json = gson.toJson(kc);

	FileWriter fw = new FileWriter(new File(edu.stanford.muse.Config.SETTINGS_DIR+File.separator + "known_classes.json"));
	System.err.println("Writing to: "+kc.syns.size() + ", " + edu.stanford.muse.Config.SETTINGS_DIR + File.separator+"known_classes.json");
	fw.write(json);
	fw.close();
	
// 	String[] entities;
// 	BufferedReader br = new BufferedReader(new FileReader(new File("cbooks.txt")));
// 	List<String> allentities = new ArrayList<String>();
// 	String line = null;
// 	while((line=br.readLine())!=null)
// 		allentities.add(line.trim());	
// 	entities = allentities.toArray(new String[allentities.size()]);
	
	Archive archive = JSPHelper.getArchive(session);
	AddressBook ab = archive.addressBook;
	Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
	Indexer indexer = archive.indexer;

	if(syns == null){
		if(type.equals("book"))
			syns = new String[]{"library","book","volume","record book","novel","published","rule book", "al-Qur'an","bible", "christian bible", "book", "good book","author","ebook","reading","poem","poetry"}; 
		else if(type.equals("university"))
			syns = new String[]{"academic","college","degree","institution","university","graduate","school","department", "library", "student","dept", "professor", "teacher", "scholar","scholarship"}; 
		else if(type.equals("musical_artist"))
			syns = new String[]{"music","drummer","player","jazz","artist","concert","performances","record"};
		else if(type.equals("museum"))
			syns = new String[]{"museum","archive","library","art","historical","history","gallery"};
		else if(type.equals("hotel"))
			syns = new String[]{"hotel","food","suite","check-in","check in","dinner","lunch","breakfast","coffee","visit","gather","reserve","room","reservation"};
		else if(type.equals("company"))
			syns = new String[]{"credit card","creditcard","visa","payment","seller","market","customer","shipping","builder","contract","publish","logo","incorporated","trademark","product"};
		else if(type.equals("movies"))
			syns = new String[]{"film","movie","motion picture","screening","actor","actress","director","play","theater","screen","stars"};
		//This set looks so bad, no idea if it works.
		else if(type.equals("govt"))
			syns = new String[]{"union","government","administration","agency","agencies","association","public","volunteer"};
		else if(type.equals("award"))
			syns = new String[]{"prize","award","congratulations","winning","nomination","nominate","won"};
		else if(type.equals("people"))
			syns = null;
		//dont use this
		else if(type.equals("places"))
			syns = new String[]{"trip","ride","tour","station","street","visit","travel","meeting"," at "};
		else{
			System.err.println("Sorry unrecognised type: "+type);
			response.getWriter().write("Unrecognised type: "+type);
			return;
		}
	}
	
	PrintStream tsw = null,acw = null;
	String TRAIN_FILE = "en-ner-";//AC_FILE = "en-ner-books-ac.val";
	TRAIN_FILE += type+".train";
	try{
		tsw = new PrintStream(new File(TRAIN_FILE),"UTF-8");
		//acw = new PrintStream(new File(AC_FILE));
	}catch(Exception e){
		e.printStackTrace();
	}
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
	
	Set<String> singleWordNames = new HashSet<String>();
	String synString = "";
	//also add the entities in the address book
	if(type.equals("people")){
		Set<String> expandedEntities = new HashSet<String>();
		for(String e: entities)
			expandedEntities.add(e);
		int ci = 0;
		List<Contact> contacts = ab.sortedContacts((Collection) archive.getAllDocs());
		double threshold = contacts.size()*0.4;
		for(Contact c: contacts){
			if(ci>threshold)
				break;
			
			if(c.names!=null){
				boolean good = false;
				for(String ea: c.emails){
					if(ea.contains(".org")){
						continue;
					}else good = true;
				}
				boolean bad = false;
				for(String ea: c.emails)
					if(ea.contains("list")||ea.contains("trust")||ea.contains("editorial")||ea.contains("new_york")){
						bad = true;
						break;
					}
				
				if(!good||bad)
					continue;
				
				Set<String> names = c.names;
				
				for(String name: names){
					if(name==null)
						continue;
					String lname = name.toLowerCase();
					if(lname.contains("listing")||lname.contains("trust")||lname.contains("editorial")||(name.split("\\s+").length>3)||name.split("\\s+").length<2)
						continue;
					if(name.matches(".*?[0-9]+.*")||name.matches(""))
						break;
					
					String[] tokens = name.split("\\s+");
					for(String token: tokens)
						if(!token.matches("\\W+\\w\\W+"))
							singleWordNames.add(token);
					expandedEntities.add(name);
				}
			}
			ci++;
		}
		int prevSize = entities.length;
		entities = expandedEntities.toArray(new String[expandedEntities.size()]);
		System.err.println("Entities expanded from: "+prevSize+" to "+entities.length);
		syns = singleWordNames.toArray(new String[singleWordNames.size()]);
		for(String sw: singleWordNames)
			synString += sw+" ";
	}
	
	int numPositiveSamples = 0,numNegativeSamples = 0;
	int i=0;
	String entityString = "";
	for(String entity: entities)
		entityString += entity+":::";
	
	status = "Preparing the training file";
	for(EmailDocument ed: docs){
		status = "Processed: "+i+"/"+docs.size();
		String content = indexer.getContents(ed, true);
		content = content.replaceAll("^>+.*","");
		content = content.replaceAll("\\n\\n", ". ");
		content = content.replaceAll("\\n"," ");

		i++;
		if(i%1000==0)
			System.err.println("Processed: "+i);
// 		if(i>1500)
// 			break;
		String[] sentences = sentenceDetector.sentDetect(content);
		for(String sent: sentences){
			//these sentences give hard time to NLP while training.
			if(sent.length()>1500)
				break;
			//System.err.println("Starting to find matches");
			boolean bookFound = false;
			String sample = sent;
			Set<String> matchingBooks = new HashSet<String>();
			if(type.equals("people")){
				String[] sentTokens = sent.split("\\s+");
				for(int sti=0;sti<(sentTokens.length-1);sti++){
					String class1 = FeatureGeneratorUtil.tokenFeature(sentTokens[sti]);
					String class2 = FeatureGeneratorUtil.tokenFeature(sentTokens[sti+1]);
					if(class1.equals("ic")&&class2.equals("ic")){
						//try to expand the sentence
						String check = sentTokens[sti]+" "+sentTokens[sti+1];
						check = check.replaceAll("'s","");
						check = check.replaceAll("^\\W+|\\W+$","");
						if(entityString.contains(check)){
							if((sti+2)<(sentTokens.length)&&(FeatureGeneratorUtil.tokenFeature(sentTokens[sti+2]).equals("ic"))){
								String check1 = check+" "+sentTokens[sti+2];
								check1 = check1.replaceAll("^\\W+|\\W+$","");
								check1 = check1.replaceAll("'s","");
								if(entityString.contains(check1))
									matchingBooks.add(check1);
								else
									matchingBooks.add(check);
							}else{
								matchingBooks.add(check);
							}
						}
					}
				}	
			}else{
				for(String entity: entities){
					if(sent.contains(entity)){
						//multi-word or not in dictionary
						if(entity.split(" ").length>1){
							matchingBooks.add(entity);
						}
					}
				}
			}
			//System.err.println("Done finding matches");
			
			//System.err.println("Starting to replace: "+matchingBooks.size());
			for(String mb: matchingBooks){
				boolean contained = false;
				for(String ob: matchingBooks)
					if(ob.contains(mb)&&!mb.equals(ob))
						contained = true;
				//replace with the biggest title known
				if(contained)
					continue;
				
				try{
					sample = sample.replaceAll(mb, " <START:"+type+"> "+mb+" <END> ");
					bookFound = true;
					numPositiveSamples++;
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			//System.err.println("Replaced: "+matchingBooks.size());
// 			String acontext = "docId: "+ed.getUniqueId()+" ::: ";
// 			acontext += "tId: "+ed.threadID+" ::: ";

// 			//not interested in mails with huge receving list
// 			if(ed.getAllNonOwnAddrs(ab.getOwnAddrs()).size()<5){
// 				for(String name: ed.getAllNonOwnAddrs(ab.getOwnAddrs()))
// 					acontext += "person: "+name+" ::: ";
// 			}
// 			Date d = ed.getDate();
// 			String time = d.getMonth()+"-"+d.getYear();
// 			acontext += "time: "+time;
			
			if(bookFound){
				sample = sample.replaceAll("<END>  ","<END> ");
				sample = sample.replaceAll("  <START:"+type+">"," <START:"+type+">");
				tsw.println(sample);
				//acw.println(acontext);
			}
			//Write as a negative sample only if none of the other syns of book are absent.
			if(!bookFound){
				Boolean bookRelated = false;
				if(type.equals("people")){
					String[] sentTokens = sent.split("\\s+");
					//synset can be very large in this case, hence doing other way round.
					for(String st: sentTokens){
						st = st.replaceAll("'s","");
						st = st.replaceAll("^\\W+|\\W+$","");
						if(synString.contains(st)){
							bookRelated = true;
							//break;
// 							try{
// 								sent = sent.replaceAll(st,"<START:"+type+"> "+st+" <END> ");
// 							catch(Error e){
// 								continue;
// 							}
						}
					}
					sent = sent.replaceAll("<END>  ","<END> ");
					sent = sent.replaceAll("  <START:"+type+">"," <START:"+type+">");
					tsw.println(sent);
				}
				else{
					String lc = sent.toLowerCase();
					for(String syn: syns){
						if(lc.contains(syn)){
							bookRelated = true;
							break;
						}
					}		
				}
				if(!bookRelated){
					tsw.println(sent);
					//acw.println(acontext);
					numNegativeSamples++;
				}
			}
		}
	}
	System.err.println("Wrote: #"+numPositiveSamples+" positive samples and #"+numNegativeSamples+" to "+new File(TRAIN_FILE).getAbsolutePath());
	
	//------------training NER start--------------------
	AdaptiveFeatureGenerator featureGenerator = new CachedFeatureGenerator(
			new AdaptiveFeatureGenerator[] {
					new WindowFeatureGenerator(new TokenFeatureGenerator(), 2, 2),
					new WindowFeatureGenerator(new TokenClassFeatureGenerator(true), 2, 2),
					new OutcomePriorFeatureGenerator(),
					new PreviousMapFeatureGenerator(),
					new BigramNameFeatureGenerator(),
					new SentenceFeatureGenerator(true, false)
					//new RefFeatureGenerator()
			});
	
	String modeldir = archive.baseDir + File.separator + "models";
	if(!new File(modeldir).exists())
 		new File(modeldir).mkdir();
	
	String /*TRAIN_FILE = "en-ner-" + type + ".train"*/ modelFile = modeldir+File.separator+"en-ner-" + type + ".bin"; //AC_FILE = "en-ner-books-ac.train";
	String VAL_FILE = "en-ner-books.eval", VAL_AC_FILE = "en-ner-books-ac.val";
	TrainingParameters params = TrainingParameters.defaultParams();
	params.put(TrainingParameters.ITERATIONS_PARAM, Integer.toString(50));
	TokenNameFinderModel nermodel = null;
	ObjectStream<NameSample> sampleStream = null, acStream = null;
	status = "Wrote the ner training file";
	
	try {
		Charset charset = Charset.forName("UTF-8");
		ObjectStream<String> lineStream =
				new PlainTextByLineStream(new FileInputStream(TRAIN_FILE), charset);
			//			ObjectStream<String> lineStream2 =
			//					new PlainTextByLineStream(new FileInputStream(AC_FILE), charset);
		sampleStream = new NameSampleDataStream(lineStream);//, lineStream2);
		nermodel = NameFinderME.train("en", type, sampleStream, params, featureGenerator, null);
		CmdLineUtil.writeModel("name finder", new File(modelFile), nermodel);
	} catch (Exception e) {
		e.printStackTrace();
	} finally {
		try {
			sampleStream.close();
		} catch (IOException e) {
			
		}
	}
	String uris[] = request.getRequestURL().toString().split("/");
	String uri = "";
	for(int u=0;u<uris.length;u++)
		if(u<(uris.length-1))
			uri = uris[u]; 
	String url = uri+"/bookMETest.jsp?type="+type+"&color=red";
	url = "http://localhost:8080/epadd/"+url;
	response.getWriter().write("<html>Wrote: #"+numPositiveSamples+" positive samples and #"+numNegativeSamples+" to "+new File(TRAIN_FILE).getAbsolutePath()+". Trained the model: "+modelFile+", you can now browse the results at: <a href='"+url+"'>"+url+"</a></html>");
%>