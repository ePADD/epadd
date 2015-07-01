<%@page import="com.adobe.xmp.impl.Utils"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="java.util.regex.*"%>
<%@page import="org.apache.lucene.analysis.util.CharArraySet"%>
<%@page import="org.apache.lucene.analysis.core.StopAnalyzer"%>
<%@page import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>	
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.epadd.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
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
<head>
<script src="../js/jquery.js" type="text/javascript"></script> 
<script>
compute = function(){
	mP = 0,mN = 0;
	vP = 0,vN = 0;
	numP = 0,numN = 0;
	total = $("body").attr("data-total");
	$("input").each(function(i,d){
		rank = $(d).attr("data-rank");
		norm = (parseInt(rank)/total);
		if($(d).attr("checked")){
			mP += norm;
			numP += 1;
		}else{
			mN += norm;
			numN += 1;
		}
	});
	mP /= numP;
	mN /= numN;
  	
	$("input").each(function(i,d){
                rank = $(d).attr("data-rank");
		norm = (parseInt(rank)/total);
	 	if($(d).attr("checked")){
			vP += Math.pow((norm-mP),2);
	     	}else{
			vN += Math.pow((norm-mN),2);
                }
        });	
	vP = Math.sqrt(vP/numP);
	vN = Math.sqrt(vN/numN);
	alert(mP+":::Var:"+vP+", "+mN+":::Var:"+vN);
}	
</script>
</head>
<%
	String patt = Indexer.initPattern(true);
	Archive archive = JSPHelper.getArchive(session);
	Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
	List<Contact> contacts = archive.addressBook.allContacts();
	//contacts = edu.stanford.epadd.ie.EmailUtils.getPeople(archive);
	Set<String> names = edu.stanford.epadd.ie.EmailUtils.getNames(contacts);
	names.addAll(edu.stanford.epadd.ie.EmailUtils.getDBpediaNames());
	
	//Now generate feature vectors for the names.
	WordFeatures wfs = edu.stanford.epadd.ie.EmailUtils.generateFeatures(names);

	Set<String> cnames = new HashSet<String>();
	int di = 0;
	for(EmailDocument doc: docs){
		Set<String> allNames = archive.indexer.getNamesFromPatternForDocId(doc.getUniqueId(),Indexer.QueryType.FULL);
		if(allNames!=null)
			for(String an: allNames){
				String cn = edu.stanford.epadd.ie.EmailUtils.clean(an);
				if(cn!=null)
					cnames.add(cn);
			}
		if(di++%1000==0)
			System.err.println(di);
	}
	
// 	String[] tests = new String[]{"Robert Creeley","Google","GE","Some Co.","Vihari Piratla","Vihari","Share to Spread","Narayan Swamy"};
	
	Map<String,Double> scores = new HashMap<String,Double>();
	Map<String,String> temp = new HashMap<String,String>();
	for(String cname: cnames){
		//global feature, how many other names have this as sub-string 
// 		String[] words = cname.split("\\s+");
		
// 		boolean substr = false;
// 		outer:for(String word: words)
// 			for(String cn1: cnames)
// 				if((cn1.contains(word+" ")||cn1.contains(" "+word))&&!cname.equals(cn1)){
// 					substr = true;
// 					break outer;
// 				}
// 		if(substr)
// 			scores.put(cname, 2*wfs.closeness(cname));
// 		else
		scores.put(cname, wfs.closeness(cname));
	}
	//WordFeatures.trainModel(archive, wfs, "person", 500);
	List<Pair<String,Double>> ss = Util.sortMapByValue(scores);
// 	for(Pair<String,Double> p: ss)
// 		out.println(p.first+" : "+p.second+"<br>");
	
	Random randnum = new Random();
	String html = "";
	html += "<body data-total='" + ss.size() + "'>";
	for (int i = 0; i < 100; i++) {
		int idx = (int) randnum.nextInt(ss.size());
		int rank = idx + 1;
		double t = (double)rank/(double)ss.size();
		System.err.println(t);
// 		if(t<0.50){
// 			i--;
// 			continue;
// 		}
		Pair<String, Double> p = ss.get(idx);
		String str = p.first;
		html += "<input type='checkbox' data-rank=" + rank + " data-score=" + p.second + "/> Rank: " + rank + ", " + str + ", score: " + p.second + "<br>";
		html += wfs.explainCloseness(p.first);
	}
	html += "<button onclick='compute()'>Evaluate</button>";
	html += "</body>";

	out.println(html);
	//out.println(wfs.explainCloseness("Original Message")+"<br>");
%>