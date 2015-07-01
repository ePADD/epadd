<%@page import="java.net.URLEncoder"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.epadd.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.epadd.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="java.text.DecimalFormat"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="org.apache.commons.lang.StringEscapeUtils"%>

<!DOCTYPE html>
<html>
<head>
<title>Test Assign authorities and expand name</title>
</head>
<body>
	<script src="js/jquery.js" type="text/javascript"></script> 
	<script src="js/jquery/jquery.tools.min.js" type="text/javascript"></script>
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<script src="js/jquery.dataTables.min.js"></script>
	
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js" type="text/javascript"></script>
	<script src="js/epadd.js"></script>
	
	<!-- For tool-tips -->
	<script type='text/javascript' src='js/jquery.qtip-1.0.js'></script>
	<script type='text/javascript' src='js/utils.js'></script>
	<script type='text/javascript' src='js/testpageutils.js'></script>
	
<jsp:include page="header.jspf"/>
Use this to test expand name.
<%
String p = request.getParameter("page");
int numSamples = 80;
if(p.equals("expandname")){
	Archive archive = (Archive)request.getSession().getAttribute("archive");
	Indexer indexer = (Indexer) archive.indexer;
	Map<String, EmailDocument> docMap = indexer.getDocMap();
	//Entity string, docId it occurred in and type
	List<Triple<String,String,String>> allEntities = (List<Triple<String,String,String>>)session.getAttribute("allEntities");
	//new ArrayList<Triple<String,String,String>>();
	int k = 0;
	if(allEntities == null){
		allEntities = new ArrayList<Triple<String,String,String>>();
		for(String docId: docMap.keySet()){
			String etype = "person", otype = "organization", ptype = "location";
			EmailDocument ed = docMap.get(docId);
			List<String> persons = indexer.getEntitiesInDoc(ed, etype);
			List<String> orgs = indexer.getEntitiesInDoc(ed, otype);
			List<String> places = indexer.getEntitiesInDoc(ed, ptype);
			List<String> entities = new ArrayList<String>();
			entities.addAll(orgs);entities.addAll(places);entities.addAll(persons);
			int i=0;
			while(i++<10){
				int j = (int)(Math.random()*entities.size());
				if(j<entities.size()){
					String e = entities.get(j);
					String t = null;
					if(persons.contains(e))
						t = "person";
					else if(orgs.contains(e))
						t = "org";
					else if(places.contains(e))
						t = "places";
					if(t.equals("person")){
						//because there are no resolutions if its a multi-word person's name but only mapping to internal or external authority. 
						if(entities.get(j)!=null&&!entities.get(j).contains(" "))
							allEntities.add(new Triple<String,String,String>(entities.get(j),docId,t));
					}else
						allEntities.add(new Triple<String,String,String>(entities.get(j),docId,t));
				}
			}
		}
		session.setAttribute("allEntities", allEntities);
	}
	
	//now randomly sample.
	int i=0;
	out.println("<table style='background-color:rgb(185, 185, 185)'>");
	Set<Integer> considered = new HashSet<Integer>();
	while(i++<numSamples){
		int j = -1;
		while(true){
			j = (int)(Math.random()*allEntities.size());
			if(j>=allEntities.size()||j<0)
				continue;
			if(!considered.contains(j)){
				considered.add(j);
				break;
			}
		}
		String e = allEntities.get(j).first;
		String docId = allEntities.get(j).second;
		String t = allEntities.get(j).third;
		out.println("<tr>");
		out.println("<td style='width:30%'><a target='_blank' href=\"browse?docId="+URLEncoder.encode(docId)+"\">"+e+"</td>");
		out.println("<td style='width: 20%'>Position of the correct match: <input placeholder='1-5' type=\"text\" size=4></input></td>");
		out.println("<td style='width: 10%'>"+t+"</td>");
		out.println("<td style='width:40%'>Resolutions <br>"+
					"<div id='expand_"+j+"'><img src=\"images/spinner.gif\" style=\"height:15px\"/></div><br>"+
					"<script>expand(\"" + e + "\",\"" + StringEscapeUtils.escapeJava(docId) + "\",\"" + j + "\", true);</script></td>");
		out.println("</tr>");
	}
	out.println("</table>");
	out.println("<button onclick='collectStats(true)'>Evaluate</button>");
}
%>
</body>
</html>
