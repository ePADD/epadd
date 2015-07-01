<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
 <%@page language="java" import="java.io.*"%>
 <%@page language="java" import="java.util.*"%>
 <%@page language="java" import="java.util.zip.*"%>
 <%@page language="java" import="edu.stanford.muse.util.*"%>
 <%@page language="java" import="edu.stanford.epadd.util.*"%>
 
<!DOCTYPE html>
<html>
<head>
<title>Insert title here</title>
</head>
<body>

</body>
<%
String BOOK="book",UNIV="univ",MUSIC_ARTIST="musical_artist",HOTEL="hotel",MUSEUM="museum",COMPANY="company",AWARD="award",MOVIE="movie", PEOPLE = "people"; 
String[] types = new String[]{BOOK,UNIV,MUSEUM,COMPANY,AWARD,MOVIE}; 
String[] dbpediaTypes = new String[]{"book","university","museum","company","award","film"};
String	typesFile = "instance_types_en.nt1.gz";
//very inefficient
Map<String,Pair<Integer,Integer>> stats = new HashMap<String,Pair<Integer,Integer>>();
for(int i=0;i<types.length;i++){
	String type = types[i];
	//expected dbpedia type
	String etype = dbpediaTypes[i];
	BufferedReader br = new BufferedReader(new FileReader(new File(type+".txt")));
	PrintWriter pw1 = new PrintWriter(new File(type+"-linkable.txt"));
	PrintWriter pw2 = new PrintWriter(new File(type+"-nonlinkable.txt"));
	Set<String> linkable = new HashSet<String>();
	String line = null;
	int nlinkable = 0,nnonlinkable = 0;
	Set<String> rsrcs = new HashSet<String>();
	while((line=br.readLine())!=null){
		String rsrc = line.replaceAll("^\\W+|\\W+$","");
		rsrc = rsrc.replaceAll("\\s+","_").toLowerCase();
		rsrcs.add(rsrc);
	}
	LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream(edu.stanford.epadd.util.NLPUtils.class.getClassLoader().getResourceAsStream(typesFile)), "UTF-8"));
	lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream(edu.stanford.epadd.util.NLPUtils.class.getClassLoader().getResourceAsStream(typesFile)), "UTF-8"));
	while (true)
	{
		String l = lnr.readLine();
		if (l == null)
			break;
		StringTokenizer st = new StringTokenizer(l);
		String r = st.nextToken().toLowerCase().trim();
		String t = st.nextToken().toLowerCase();
		if(rsrcs.contains(r) && t.contains(etype)){
			linkable.add(r);
			nlinkable++;
			pw1.println(r);
		}
	}
	System.err.println("Done "+i+"/"+types.length);
	for(String rsrc: rsrcs)
		if(!linkable.contains(rsrc))
			pw2.println(rsrc);
	pw1.close();
	pw2.close();
	br.close();
}
%>
</html>
