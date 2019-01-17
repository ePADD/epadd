<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@ page import="edu.stanford.muse.util.Util" %>
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
<%@include file="header.jspf"%>
<%@include file="div_status.jspf"%>

<%--<script>epadd.nav_mark_active('Export');</script>--%>
<% 	AddressBook addressBook = archive.addressBook;
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
	writeProfileBlock(out, archive, "Verify checksum of this archive");
%>
<div style="margin-left:170px">

<%
	if(!checkDone) {
		out.println("<p> ePADD relies upon the Bag-it specification to ensure the authenticity"+
				" of the archive for preservation purposes. To check that the archive has not been altered outside of "+
				"the program, you can verify the checksum. Please note that this process might take a while for large archives.</p>");
	}else{
	    if(success){
	        out.println("<br><p> The bag is verified. The content of this archive was not modified.</p>");
		}else
		{
		    String msg  = request.getParameter("errmsg");
			out.println("<br><p>Bag verification failed. This archive may have been corrupted!</p>");
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
        var params = epadd.convertParamsToAmpersandSep(enterparams);
        fetch_page_with_progress ('ajax/async/verify-bag-checksum.jsp', "status", document.getElementById('status'), document.getElementById('status_text'), params); /* load_archive_and_call(function() { window.location = "browse-top"} */;
    });
    <%}%>

</script>
</div>
<p>
<br/>
<br/>
</body>
</html>
