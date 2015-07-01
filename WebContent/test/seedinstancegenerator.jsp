<%@page import="edu.stanford.muse.util.Pair"%>
<%@page import="edu.stanford.muse.util.Util"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>	
<%@page language="java" import="edu.stanford.muse.ie.KnownClasses"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.util.regex.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="com.google.gson.Gson"%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<script src="../js/jquery.js"></script>
<script>
	function submit(){
		type = $("#type").val();
		if(type==="other")
			type = $("#className").val();
		synText = $("#syns").val();
		syns = synText.split(",");
		
		c = parseInt($("#cutoff").val());
		entities = [];
		$(".entity").each(function(i,d){
			s = $(d).attr("data-score");
			score = parseInt(s);
			if(score>c){
				entities.push($(d).text());
			}
		});
		$.ajax({
			type: "POST",
			url: "./nertraingenerator.jsp",
			data: {
				entities: entities,
				type: type,
				syns: syns
			},
			success: function(data,status){
				var newWindow = window.open();
				newWindow.document.write(data);
			},
			error: function(error){
				console.error("Error while sorting the entities");
			}
		});
	}
	
	function changesyns(){
		var type = $("#type").val();	
		syns = synMap[type];
		if(type==="other")
			$("#className").show();
		else
			$("#className").hide();
		
		var text = "";
		if(typeof(syns)!=="undefined")
			syns.map(function(d,i){
				text+=d;
				if(i<(syns.length-1)){
					text+=",";
				}
			});
		$("#syns").val(text);
	}
</script>
<%
	Gson gson = new Gson();
	KnownClasses kc = null;
	try{
		FileReader fr = new FileReader(new File(edu.stanford.muse.Config.SETTINGS_DIR+File.separator+"known_classes.json"));
		kc = gson.fromJson(fr, KnownClasses.class);
		fr.close();
	}catch(Exception e){
		e.printStackTrace();
	}
	if(kc == null||kc.syns == null)
		kc = new KnownClasses();
	else
		System.err.println("Read: "+kc.syns.size()+" from knwon_classes.json");
	
	String options = "<select id='type' onchange='changesyns()'><option value='invalid'>Select a type</option>";
	Map<String,String[]> synMap = kc.syns;
	for(String cz: synMap.keySet())
		options += "<option value='"+cz+"'>"+cz+"</option>";
		
	out.print("<script>synMap={");
	int i =0,m=synMap.size();
	for(String cz: synMap.keySet()){
		if(synMap.get(cz)!=null){
			out.print("\""+cz+"\":[");
			int j=0,n=synMap.get(cz).length;
			for(String syn: synMap.get(cz)){
				out.print("\""+syn+"\"");
				if(j++<(n-1))
					out.print(",");
				else
					out.print("]");
			}
		}
		if(i++<(m-1))
			out.print(",");
		else
			out.print("}");
	}
	out.println("</script>");
	out.println("<script type='text/javascript' src='../js/fancyBox/source/jquery.fancybox.js'></script>");			
	out.println(options+"<option value='other'>Other</option></select>");
	out.println("<input type='text' placeholder='Please name this class' id='className' style='display:none'/>");
	out.println("<input type='text' id='syns' size='180' placeholder='Enter variants of type class in comma seperated form'></input>");
	out.println("<input type='text' id='cutoff' placeholder='cutoff'/><button onclick='submit()'>Submit</button>");
	System.err.println("Received request seedinstancegenerator: processing...");
	String[] entities = request.getParameterValues("entities[]");
	//dbpedia links to entities
	String[] dLinks = request.getParameterValues("dLinks[]");
	//map from entity -> dbpedia resource
	Map<String,String> dLinkMap = new HashMap<String,String>();
	for(i=0;i<entities.length;i++)
		dLinkMap.put(entities[i],dLinks[i]);
	
	Archive archive = JSPHelper.getArchive(session);
	Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
	Indexer indexer = (Indexer) archive.indexer;
	String recInst = "(";
	int i1 = 0;
	for(String instance: entities){
		String cleaned = Util.cleanForRegex(instance);
		cleaned = cleaned.replaceAll("\\W+","\\\\W\\+");
		recInst += cleaned;
		if(i1<(entities.length-1))
			recInst+="|";	
		i1++;
	}
	recInst += ")";
	Map<String, Set<String>> contextMap = new HashMap<String,Set<String>>();
	Pattern p = Pattern.compile(recInst);
	i=0;
	for(EmailDocument ed: docs){
		if(i!=0&&i%1000==0)
			System.err.println("Processed(seedinstancegen): "+i);
		i++;
		
		String content = indexer.getContents(ed, false);
		content = content.replaceAll("^>+.*","");
		content = content.replaceAll("\\n\\n", ". ");
		content = content.replaceAll("\\n"," ");
		content.replaceAll(">+","");
		
		boolean found = false;
		Set<String> matchingEntities = new HashSet<String>();	
		for(String entity: entities)
			if(content.contains(entity)){
				found = true;
				matchingEntities.add(entity);
				//break;
			}
		if(!found)
			continue;
// 		Matcher m = p.matcher(content);
// 		while(m.find())	
// 			matchingEntities.add(m.group());
		
		List<String> names = indexer.getNamesForDocId(ed.getUniqueId(), Indexer.QueryType.ORIGINAL);
		for(String me: matchingEntities){
			if(!contextMap.containsKey(me))
				contextMap.put(me,new HashSet<String>());
			contextMap.get(me).addAll(names);
		}
	}
	
	Map<String,Double> ewithScores = new HashMap<String,Double>();
	Map<String,String> matches = new HashMap<String,String>();
	i=0;
	for(String entity: contextMap.keySet()){
		System.err.println("Scored: "+i+" pages");
		i++;
		String[] cc = contextMap.get(entity).toArray(new String[contextMap.get(entity).size()]);
		System.err.println("length of context: "+cc.length);
		String link = dLinkMap.get(entity);
		Pair<String,Double> score = edu.stanford.muse.ie.Util.scoreWikiPage(link, cc);
		if(score!=null)
			System.err.println("Link: "+link+", score: "+score.second);
		if(score == null)
			score = new Pair<String,Double>("Timed out",0.0);
		ewithScores.put(entity, score.second);
		matches.put(entity, score.first);
	}
	
	List<Pair<String,Double>> scores = Util.sortMapByValue(ewithScores);
	String html = "";
	double max = scores.get(0).second;
	for(Pair<String,Double> s: scores){
		String title = matches.get(s.first);
		String bar = edu.stanford.muse.ie.Util.getConfidenceBar(s.second / max, title);
		String dLink = dLinkMap.get(s.first);

		html += "<a class='entity' data-score='"+s.second+"' href=\""+dLink+"\" target=\"_blank\" title=\""+contextMap.get(s.first)+"\">"+s.first+"</a> score: "+s.second+"<br>";
		html += bar+"<br><br>";
	}
	response.getWriter().write(html);
%>