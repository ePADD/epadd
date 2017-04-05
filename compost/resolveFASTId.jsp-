<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html>
<%@page language="java" import="edu.stanford.muse.ie.FASTEntities"%>
<%@page language="java" import="edu.stanford.muse.ie.FASTPerson"%>
<%
	try{
		String id = request.getParameter("id");
		FASTPerson fp = FASTEntities.get(id);
		out.println(fp.toHTMLString());
	}catch(Exception e){
		e.printStackTrace();
	}
%>