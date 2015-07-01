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
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<script src="../js/jquery.js"></script>
<script>
	function submit(){
		ct = $("#cutoff").val();
		cutoff = parseInt(ct);
		entities = [];
		$(".entity").each(function(i,d){
			s = $(d).attr("data-score");
			score = parseInt(s);
			if(score>cutoff){
				entities.push($(d).text());
			}
		});
		$.ajax({
			type: "POST",
			url: "./nertraingenerator.jsp",
			data: {
				entities: entities,
			},
			success: function(data,status){
				alert(data);
			},
			error: function(error){
				console.error("Error while sorting the entities");
			}
		});
	}
</script>
<%
	out.println("<input type='text' id='cutoff'/><button onclick='submit()'>Submit</button>");
	String[] entities = request.getParameterValues("entities[]");
	//String[] entities = new String[]{"On the Road","The God of Small Things","He and I","Monday or Tuesday","DOOR WIDE OPEN","War and Peace","Coming Attractions","The Leopard","All Quiet on the Western Front","Dear Zoe","Paradise Lost","The Black Mountain","Master and Commander","Orpheus Emerged","In Cold Blood","Billy Budd","The Prelude","Leaves of Grass","The Tin Drum","The White Goddess","Against Interpretation","The Dalkey Archive","The Great Gatsby","Don Quixote","A People's History of the United States","THE GRAPES OF WRATH","The World According to Garp","The War Between the Tates","Gone With the Wind","The Scarlet Letter","Crime and Punishment","Illness As Metaphor","Animal Farm","The Europeans","I Ching","Off the Road","For the Union Dead","La Casa","The Joy Luck Club","Great Expectations","It's Halloween","Naked Lunch","Panel Discussions","Ethan Frome","The Holy","THE YAGE LETTERS","Life Studies","The New Media Reader","Visions of Cody","The Best Democracy Money Can Buy","Doors Open","World on Fire","The Natural","Snowball's Chance","DO NOT OPEN","Plan of Attack","Under the Volcano","The Good House","Uncle Tom's Cabin","The Trial of Henry Kissinger","The Town and the City","Doctor Sax","The Plague","Always Running","The Rings of Saturn","The Door in the Wall","A Tale of Two Cities","Invisible Man","Nice Work","The Idiot","Purple Hibiscus","A Fable","DISTANT STAR","House of Leaves","Reservation Blues","Father of Lies","The Keepers of Truth","Actual Air","Growing Up Absurd","All Watched Over By Machines of Loving Grace","The First Death","The Invisible Man","A Song Flung Up To Heaven","The Shack","The Catcher in the Rye","A Man","Being and Time","The Defense","A Good Scent From a Strange Mountain","The English Patient","FORTRESS BESIEGED","Fast Food Nation","On Liberty","The Cloud of Unknowing","The Bad Seed","Manna From Heaven","PUT OUT MORE FLAGS","CLASS WARFARE","The Psychopathology of Everyday Life","The Bookshop","Guinness World Records","Energy Victory","The Ants","The Road","The Subterraneans","Locus Solus","Death in Venice","Notes of a Native Son","When Worlds Collide","FEAR AND TREMBLING","The Lost Boy","The Book of Hours","The Book of Illusions","What Happened","An American Dream","Finnegans Wake","Light in August","The Caine Mutiny","The English Roses","Book of Sketches","The Glory","Empire Falls","Bully for Brontosaurus","Me Talk Pretty One Day","Cities in Flight","Jane Eyre","Sentimental Education","The Novel","This Side of Paradise","THE BEST LAID PLANS","File Under Popular","The Dissertation","Race Matters","Postern of Fate","According to Mark","A Coney Island of the Mind","The Other Hand","Indian Killer","The Bluest Eye","The Monk","The Singing","The Great Indian Novel","The Namesake","Little Men","Chronicle of a Death Foretold","Everyone Poops","The Politics of Anti-Semitism","The Auroras of Autumn","The Dispossessed","The Orchid Thief","The Holocaust Industry","HAYDUKE LIVES","Notable American Women","The Joy of Sex","Lord Jim","Colleges that Change Lives","Time to Come","Manchild in the Promised Land","THE POISONWOOD BIBLE","The Human Stain","The Elegant Universe","Zorba the Greek","Zen and the Art of Motorcycle Maintenance","Black Boy","WELCOME TO THE DESERT OF THE REAL","Logan's Run","The Ambassadors","The Trench","La Maravilla","The Reader","To the Lighthouse","Enduring Love","The Sand Pebbles","A Heartbreaking Work of Staggering Genius","GOING TOO FAR","Beyond Black","MANHATTAN NOCTURNE","Bush at War","Next Sunday","The Electronic Revolution","Dorothy Rabinowitz","With A Little Help","The Third Mind","CLOSE TO THE GROUND","Unfit for Command","The End of Nature","Silent Spring","Homage to Catalonia","The Alienist","Following the Equator","Red Harvest","JUST FOR FUN","The Time of Our Singing","Wittgenstein's Mistress","The First Casualty","The Fires of Spring","NO LOGO","The Seven Storey Mountain","The Handmaid's Tale","The Gadfly","Blood Meridian","Treasure Island","Book of Haikus","The Refugees","A Connecticut Yankee in King Arthur's Court","INVITATION TO A BEHEADING","The Remains of the Day","The Blessing","Dream Jungle","Where I Was From","Lies and the Lying Liars Who Tell Them","Four Quartets","The Return of the King","MAN WALKS INTO A ROOM","THE NEW YORK TRILOGY","THE MUSIC OF CHANCE","The Dharma Bums","A VOID","Castle to Castle","The Precipice","The Protocols of the Elders of Zion","Sitt Marie Rose","Adam Bede","A PERFECT SPY","The Good Soldier","Already Dead","War Is a Force That Gives Us Meaning","Native Son","The Plot Against America","Burning Bright","The Commodore","Doing It","The Myth of Sisyphus","New Atlantis","Soul Mountain","Canto General","The Twenty-Seventh City","Along Came a Spider","A Suitable Boy","American Psycho","The End of the Affair","All The King's Men","The Trial","ELECTIVE AFFINITIES","Peace is Possible","A Journey","In the Country of Last Things","Moon Palace","The Invention of Solitude","Bone Dance","Bridget Jones's Diary","Between Pacific Tides","The Farming of Bones","Last Exit to Brooklyn","On Writing","TROUT FISHING IN AMERICA","AFTER IMAGE","Memoirs of a Geisha","The Illustrated Man","Peace and War","Coming Through Slaughter","Mexico City Blues","Specimen Days","Jude the Obscure","The Electric Kool-Aid Acid Test","The Exorcist","Homicide: A Year on the Killing Streets","Magnificent Obsession","Moses and Monotheism","Irish Eyes","Story of O","The Manchurian Candidate","Ashes and Diamonds","Portnoy's Complaint","Tarzan of the Apes","The Running Man","Waiting for the Barbarians","The Portrait of a Lady","In the Pond","A Clockwork Orange","City of Night","The Shipping News","The Stand"};
	String[] vars = new String[]{"book","volume","record book","novel","Novel","published","rule book","Koran", "Quran", "al-Qur'an","Bible", "Christian Bible", "Book", "Good Book"}; 
	double SAME_EMAIL_SCORE=0.1,SAME_PARA_SCORE=0.4,SAME_SENTENCE_SCORE=0.7;
	Archive archive = JSPHelper.getArchive(session);
	Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
	Indexer indexer = (Indexer) archive.indexer;
	String recInst = "(";
	int i1 = 0;
	for(String instance: entities){
		String cleaned = Util.cleanForRegex(instance);
		recInst += cleaned;
		if(i1<(entities.length-1))
			recInst+="|";	
		i1++;
	}
	recInst += ")";
	
	Map<String,Double> conf = new HashMap<String,Double>();
	Map<String,Double> freqs = new HashMap<String,Double>();
	Pattern p = Pattern.compile(recInst);
	int i=0;
	for(EmailDocument ed: docs){
		if(i!=0&&i%10000==0)
			System.err.println("Processed: "+i);
		i++;
		
		String content = indexer.getContents(ed, false);
		boolean found = false,mention = false;
		for(String entity: entities)
			if(content.contains(entity)){
				found = true;
				break;
			}
		if(!found)
			continue;
		int allcount = 0;
		Set<String> allEntities = new HashSet<String>();
		//String[] paras = content.split("\\n\\n+");
		String[] allsents = content.split("\\. ");
		int paracount = 0,freq = 0;
		int STEP = 4;
		for(int j=0;j<allsents.length;j+=STEP){
			String para = "";
			for(int k=j;k<Math.min(allsents.length,j+STEP);k++){
				para+=allsents[k];
			}
			String[] sents = para.split("\\.");
			Set<String> paraEntities = new HashSet<String>();
			for(String sent: sents){
				Matcher m = p.matcher(sent);
				Set<String> matchingEntities = new HashSet<String>();	
				while(m.find())	{
					matchingEntities.add(m.group());
					String entity = m.group();
					if(!freqs.containsKey(entity))
						freqs.put(entity, 0.0);
					freqs.put(entity, freqs.get(entity)+1);
				}
			
				mention = false;
				for(String var: vars)
					if(sent.contains(var)){
						mention = true;
						break;
					}
				if(mention)
					for(String me: matchingEntities){
						if(!conf.containsKey(me))
							conf.put(me, 0.0);
						conf.put(me, conf.get(me)+SAME_SENTENCE_SCORE);
					}
			
				paraEntities.addAll(matchingEntities);
				paracount+=matchingEntities.size();
			}
			mention = false;
			freq = 0;
			for(String var: vars)
				if(para.contains(var)){
					mention = true;
					freq ++;
				}
			if(mention)
				for(String me: paraEntities){
					if(!conf.containsKey(me))
						conf.put(me, 0.0);
					conf.put(me, conf.get(me)+freq*SAME_PARA_SCORE);
				}
			allcount += paracount;
			allEntities.addAll(paraEntities);
		}
		mention = false;
		freq = 0;
		for(String var: vars)
			if(content.contains(var)){
				mention = true;
				freq++;
			}
// 		if(mention)
// 			for(String me: allEntities){
// 				if(!conf.containsKey(me))
// 					conf.put(me, 0.0);
// 				conf.put(me, conf.get(me)+freq*SAME_EMAIL_SCORE);
// 			}
	}
	
	Map<String,Double> nconf = new HashMap<String,Double>();
	for(String c: conf.keySet())
		nconf.put(c, conf.get(c)/freqs.get(c));
	
	List<Pair<String,Double>> sconf = Util.sortMapByValue(nconf);
	double max = sconf.get(0).second;
	for(Pair<String,Double> c: sconf){
		out.println("<div class='entity' data-score="+c.second+">"+c.first+"</div> : "+c.second+"<br>");
		String bar = edu.stanford.muse.ie.Util.getConfidenceBar(c.second / max, c.second + "");
		out.println(bar+"<br><br>");
	}
%>