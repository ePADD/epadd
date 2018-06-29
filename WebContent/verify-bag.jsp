<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.Config"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.Document"%>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.webapp.SimpleSessions" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileWriter" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@include file="getArchive.jspf" %>
<html>
<head>
	<script src="js/epadd.js" type="text/javascript"></script>
	<script type="text/javascript" src="js/statusUpdate.js"></script>
	<title>Verify archive bag</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<%@include file="div_status.jspf"%>

<%--<script>epadd.nav_mark_active('Export');</script>--%>
<% 	AddressBook addressBook = archive.addressBook;
String archiveID= ArchiveReaderWriter.getArchiveIDForArchive(archive);
boolean checkDone=false;
boolean success=false;
if(!Util.nullOrEmpty(request.getParameter("checkDone"))){
    checkDone=true;
    if(request.getParameter("result").equals("success"))
        success=true;
    else
        success=false;
}
	String bestName = addressBook.getBestNameForSelf().trim();
	writeProfileBlock(out, archive, "", "Verify checksum of this archive");
%>
<div style="margin-left:170px">

<%
	if(!checkDone) {
		out.println("<p> Starting from version 6,  ePADD archive follows Bag-it specification for preservation support. If you received this archive from " +
				" an untrusted source then you should verify the checksum of archive bag's content. Please note that this might be a time consuming process " +
				" depending upon the size of the archive. So be prepared before verifying the integrity of this archive!!</p>");
	}else{
	    if(success){
	        out.println("<br><p> Checksum is verified. It implies that the content of this archive was not modified in between.</p>");
		}else
		{
			out.println("<br><p> Checksum can not be verified. It implies that the content of this archive was modified from outside of ePADD.</p>");
		}
	}
%>
	<%--<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>--%>
	<% out.flush(); %>

<%--<script>$('#spinner-div').hide();</script>--%>
	<%if(!checkDone){%>
	<button id="button-verify-checksum" class="btn-default ">Verify checksum</button>
<script>
    $('#button-verify-checksum').click (function(e) {
        var enterparams = {archiveID: '<%=archiveID%>'};
        fetch_page_with_progress ('ajax/verify-bag-checksum.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), enterparams); /* load_archive_and_call(function() { window.location = "browse-top"} */;
    });
    <%}%>

</script>
</div>
<p>
<br/>
<br/>
</body>
</html>
