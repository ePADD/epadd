<%@page import="edu.stanford.muse.util.Pair"%>
<%@page import="java.util.*"%>
<%@page import="edu.stanford.epadd.misc.*"%>
<%@page import="com.google.gson.Gson"%>
<%@ page import="edu.stanford.muse.util.Network" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>

<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<% 
	int TIMEOUT = 1000;
	String prApi = "http://en.wikipedia.org/w/api.php?action=query&format=json&colimit=max&prop=pageimages%7Ccoordinates&pithumbsize=180&pilimit=50&generator=geosearch&ggscoord=",
			suApi = "&ggsradius=1000&ggsnamespace=0&ggslimit=10";
	String lat = request.getParameter("latitude");
	String lon = request.getParameter("longitude");
	System.err.println("Querying: "+(prApi+lat+"|"+lon+suApi));
	java.net.URL aUrl = new java.net.URL(prApi+lat+"|"+lon+suApi);
	response.setCharacterEncoding("UTF-8");
	
	try {
		String json = Network.getContentFromURL(aUrl);
		if (json != null){
			Gson gson = new Gson();
			if(json.equals("{\"limits\":{\"coordinates\":500}}"))
				response.getWriter().write("Nothing found here!");
			WikiGeoApi geolocs = gson.fromJson(json, WikiGeoApi.class);
			List<Pair<String,Double>> hits = WikiGeoApi.getInfo(geolocs, lat,lon);
			if(hits!=null){
				String titles = "";
				if(hits.size()>0){
					for(Pair<String,Double> hit: hits){
						JSPHelper.log.info(hit.getSecond());
						titles += hit.getFirst()+"&nbsp"+hit.getSecond() +"kms<br>";
					}
				}
				response.getWriter().write(titles);	
				System.err.println(":::");
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
		try {
			response.getWriter().write("error");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		JSPHelper.log.info("Exception: " + e);
		return;
	}
	
%>