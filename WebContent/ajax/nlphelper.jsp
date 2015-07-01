<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page language="java" import="edu.stanford.epadd.util.*"%>
<%@page language="java" import="com.google.gson.Gson"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%
	JSPHelper.setPageUncacheable(response);
	response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
	//response.setHeader("Access-Control-Allow-Origin", "http://xenon.stanford.edu");
	response.setHeader("Access-Control-Allow-Credentials", "true");
	response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
	response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
	String text = request.getParameter("text");
	String action = request.getParameter("action");
	System.err.println("Received request for: "+text+" action: "+action);
	try{
		Gson gson = new Gson();
		if(text!=null){
			text = text.replaceAll("\\?", "");
			if(action.equals("sentenceTokenize")){
				String json = "";
				String[] sents = NLPUtils.SentenceTokenizer(text);  
				if(sents!=null)
					json = gson.toJson(sents);	
				System.err.println(json);
				response.getWriter().write(json);
			}
		}
	}catch(Exception e){
		e.printStackTrace();
	}
%>
