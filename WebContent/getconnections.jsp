<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.lang.reflect.Type"%>
<%@page language="java" import="com.google.gson.Gson"%>
<%@page language="java" import="com.google.gson.reflect.TypeToken"%>
<%@page language="java" import="edu.stanford.epadd.WordGraph"%>
<html>
<head>
<title>Insert title here</title>
</head>
<body>
<!-- Reads edges and returns all connected nodes to nodes that match the input text. -->
<%
	String text = request.getParameter("text").toLowerCase();
	if(text!=null){
		Gson gson = new Gson();
		WordGraph wg = (WordGraph)request.getSession().getAttribute("wordgraph");
		if(wg==null){
			String home = System.getProperty("user.home");String sep = File.separator;
			FileReader fr = new FileReader(new File(home+sep+"sanbox"+sep+"edges.txt"));
			Type type = new TypeToken<List<Triple<String,String,Double>>>() {
			}.getType();
			List<Triple<String,String,Double>> edges = gson.fromJson(fr,type);
			wg = new WordGraph(edges);
			request.getSession().setAttribute("wordgraph", wg);	
		}
		Map<String, Set<String>> connections = new HashMap<String,Set<String>>();
		for(String node : wg.adj.keySet()){
			if(node.contains(text)){
				Set<String> conns = connections.get(node);
				if(conns==null)
					conns=wg.adj.get(node).keySet();	
				else	
					conns.addAll(wg.adj.get(node).keySet());
			}
		}
		response.getWriter().print(gson.toJson(connections));
		//wg.GetAllConnectedNodes(node);
	}
%>

</body>
</html>
