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
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<%
Archive archive = JSPHelper.getArchive(session);
Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
Indexer indexer = archive.indexer;
AddressBook addressbook = archive.addressBook;
String[] instances = new String[]{"On the Road","The God of Small Things","He and I","Monday or Tuesday","DOOR WIDE OPEN","War and Peace","Coming Attractions","The Leopard","All Quiet on the Western Front","Dear Zoe","Paradise Lost","The Black Mountain","Master and Commander","Orpheus Emerged","In Cold Blood","Billy Budd","The Prelude","Leaves of Grass","The Tin Drum","The White Goddess","Against Interpretation","The Dalkey Archive","The Great Gatsby","Don Quixote","A People's History of the United States","THE GRAPES OF WRATH","The World According to Garp","The War Between the Tates","Gone With the Wind","The Scarlet Letter","Crime and Punishment","Illness As Metaphor","Animal Farm","The Europeans","I Ching","Off the Road","For the Union Dead","La Casa","The Joy Luck Club","Great Expectations","It's Halloween","Naked Lunch","Panel Discussions","Ethan Frome","The Holy","THE YAGE LETTERS","Life Studies","The New Media Reader","Visions of Cody","The Best Democracy Money Can Buy","Doors Open","World on Fire","The Natural","Snowball's Chance","DO NOT OPEN","Plan of Attack","Under the Volcano","The Good House","Uncle Tom's Cabin","The Trial of Henry Kissinger","The Town and the City","Doctor Sax","The Plague","Always Running","The Rings of Saturn","The Door in the Wall","A Tale of Two Cities","Invisible Man","Nice Work","The Idiot","Purple Hibiscus","A Fable","DISTANT STAR","House of Leaves","Reservation Blues","Father of Lies","The Keepers of Truth","Actual Air","Growing Up Absurd","All Watched Over By Machines of Loving Grace","The First Death","The Invisible Man","A Song Flung Up To Heaven","The Shack","The Catcher in the Rye","A Man","Being and Time","The Defense","A Good Scent From a Strange Mountain","The English Patient","FORTRESS BESIEGED","Fast Food Nation","On Liberty","The Cloud of Unknowing","The Bad Seed","Manna From Heaven","PUT OUT MORE FLAGS","CLASS WARFARE","The Psychopathology of Everyday Life","The Bookshop","Guinness World Records","Energy Victory","The Ants","The Road","The Subterraneans","Locus Solus","Death in Venice","Notes of a Native Son","When Worlds Collide","FEAR AND TREMBLING","The Lost Boy","The Book of Hours","The Book of Illusions","What Happened","An American Dream","Finnegans Wake","Light in August","The Caine Mutiny","The English Roses","Book of Sketches","The Glory","Empire Falls","Bully for Brontosaurus","Me Talk Pretty One Day","Cities in Flight","Jane Eyre","Sentimental Education","The Novel","This Side of Paradise","THE BEST LAID PLANS","File Under Popular","The Dissertation","Race Matters","Postern of Fate","According to Mark","A Coney Island of the Mind","The Other Hand","Indian Killer","The Bluest Eye","The Monk","The Singing","The Great Indian Novel","The Namesake","Little Men","Chronicle of a Death Foretold","Everyone Poops","The Politics of Anti-Semitism","The Auroras of Autumn","The Dispossessed","The Orchid Thief","The Holocaust Industry","HAYDUKE LIVES","Notable American Women","The Joy of Sex","Lord Jim","Colleges that Change Lives","Time to Come","Manchild in the Promised Land","THE POISONWOOD BIBLE","The Human Stain","The Elegant Universe","Zorba the Greek","Zen and the Art of Motorcycle Maintenance","Black Boy","WELCOME TO THE DESERT OF THE REAL","Logan's Run","The Ambassadors","The Trench","La Maravilla","The Reader","To the Lighthouse","Enduring Love","The Sand Pebbles","A Heartbreaking Work of Staggering Genius","GOING TOO FAR","Beyond Black","MANHATTAN NOCTURNE","Bush at War","Next Sunday","The Electronic Revolution","Dorothy Rabinowitz","With A Little Help","The Third Mind","CLOSE TO THE GROUND","Unfit for Command","The End of Nature","Silent Spring","Homage to Catalonia","The Alienist","Following the Equator","Red Harvest","JUST FOR FUN","The Time of Our Singing","Wittgenstein's Mistress","The First Casualty","The Fires of Spring","NO LOGO","The Seven Storey Mountain","The Handmaid's Tale","The Gadfly","Blood Meridian","Treasure Island","Book of Haikus","The Refugees","A Connecticut Yankee in King Arthur's Court","INVITATION TO A BEHEADING","The Remains of the Day","The Blessing","Dream Jungle","Where I Was From","Lies and the Lying Liars Who Tell Them","Four Quartets","The Return of the King","MAN WALKS INTO A ROOM","THE NEW YORK TRILOGY","THE MUSIC OF CHANCE","The Dharma Bums","A VOID","Castle to Castle","The Precipice","The Protocols of the Elders of Zion","Sitt Marie Rose","Adam Bede","A PERFECT SPY","The Good Soldier","Already Dead","War Is a Force That Gives Us Meaning","Native Son","The Plot Against America","Burning Bright","The Commodore","Doing It","The Myth of Sisyphus","New Atlantis","Soul Mountain","Canto General","The Twenty-Seventh City","Along Came a Spider","A Suitable Boy","American Psycho","The End of the Affair","All The King's Men","The Trial","ELECTIVE AFFINITIES","Peace is Possible","A Journey","In the Country of Last Things","Moon Palace","The Invention of Solitude","Bone Dance","Bridget Jones's Diary","Between Pacific Tides","The Farming of Bones","Last Exit to Brooklyn","On Writing","TROUT FISHING IN AMERICA","AFTER IMAGE","Memoirs of a Geisha","The Illustrated Man","Peace and War","Coming Through Slaughter","Mexico City Blues","Specimen Days","Jude the Obscure","The Electric Kool-Aid Acid Test","The Exorcist","Homicide: A Year on the Killing Streets","Magnificent Obsession","Moses and Monotheism","Irish Eyes","Story of O","The Manchurian Candidate","Ashes and Diamonds","Portnoy's Complaint","Tarzan of the Apes","The Running Man","Waiting for the Barbarians","The Portrait of a Lady","In the Pond","A Clockwork Orange","City of Night","The Shipping News","The Stand"};

String recInst = "(";
int i1 = 0;
for(String instance: instances){
	String cleaned = Util.cleanForRegex(instance);
	cleaned = cleaned.replaceAll("\\W+","\\\\W\\+");
	recInst += cleaned;
	if(i1<(instances.length-1))
		recInst+="|";	
	i1++;
}
recInst += ")";
Pattern recInstSuffixPattern = Pattern.compile(recInst+"((\\W+\\w+){0,3})");
Pattern recInstPrefixPattern = Pattern.compile("((\\w+\\W+){0,3})"+recInst);

Map<String,Integer> suffixPatts = new HashMap<String,Integer>();
Map<String,Integer> prefixPatts = new HashMap<String,Integer>();

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
int i=0;
for(EmailDocument ed: docs){
	if(i!=0&&i%10000==0)
		System.err.println("Processed: "+i);
	i++;
	String content = indexer.getContents(ed, false);
	boolean found = false;
	for(String inst: instances)
		if(content.contains(inst)){
			found = true;
			break;
		}
	if(!found)
		continue;
	Matcher ms = recInstSuffixPattern.matcher(content);
	Matcher mp = recInstPrefixPattern.matcher(content);
	while(ms.find())
		if(ms.groupCount()>1){
			String patt = ms.group(2);
			patt = patt.replaceAll("^("+stopWordsList+")","");
			if(!suffixPatts.containsKey(patt))
				suffixPatts.put(patt,0);
			suffixPatts.put(patt,suffixPatts.get(patt)+1);
		}
	
	while(mp.find()){
		if(mp.groupCount()>1){
			String patt = mp.group(1);
			patt = patt.replaceAll("("+stopWordsList+")$","");
			if(!prefixPatts.containsKey(patt))
				prefixPatts.put(patt,0);
			prefixPatts.put(patt,prefixPatts.get(patt)+1);
		}
	}
}
List<Pair<String,Integer>> sps = Util.sortMapByValue(suffixPatts);
List<Pair<String,Integer>> pps = Util.sortMapByValue(prefixPatts);

out.println("Suffix paterns<br>");
for(Pair<String,Integer> sp: sps){
	out.println(sp.first+" : "+sp.second+"<br>");
}

out.println("Prefix patterns<br>");
for(Pair<String,Integer> pp: pps){
	out.println(pp.first+" : "+pp.second+"<br>");
}
%>