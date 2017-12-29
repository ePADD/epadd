<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%
    out.println (JSPHelper.createOrEditLabels(request));
%>
