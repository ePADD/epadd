<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page language="java" import="java.net.URL"%>
<%@page language="java" import="javax.servlet.http.HttpServlet"%>
<%@page language="java" import="javax.servlet.http.HttpServletRequest"%>
<%@page language="java" import="javax.servlet.http.HttpServletResponse"%>
<%@page language="java" import="org.jsoup.Jsoup"%>

<%
	String url = request.getParameter("url");
	try{
		response.getWriter().write(Jsoup.parse(new URL(url), 10000).html());
	}catch(Exception e){
		e.printStackTrace();
	}
%>