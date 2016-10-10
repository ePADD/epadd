<%@page language="java" contentType="text/plain;charset=UTF-8"%>
<%
	String exportType = request.getParameter("exportType");
	edu.stanford.muse.index.Archive archive = edu.stanford.muse.webapp.JSPHelper.getArchive(session);
    try{
		String csv = edu.stanford.muse.ie.AuthorisedAuthorities.exportRecords(archive,exportType);
        out.println(csv);
	}catch(Exception e){
		e.printStackTrace();
		out.println(e.getMessage());
    }
%>