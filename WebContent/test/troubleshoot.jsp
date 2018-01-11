<%@page import="edu.stanford.muse.ie.EntityFeature"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="../getArchive.jspf" %>

<!DOCTYPE html>
<!--This JSP is not used anywhere-->
<html>
<head>
<title>ePADD troubleshoot page</title>
<link rel="icon" type="image/png" href="images/epadd-favicon.png?v3">
</head>
<body style='text-align:center'>
<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
<link rel="stylesheet" href="bootstrap/dist/css/bootstrap-theme.min.css">
<jsp:include page="../css/css.jsp"/>
<jsp:include page="../header.jspf"/>
<script type="text/javascript" src="js/statusUpdate.js"></script>
<%@include file="../div_status.jspf"%>

<script src="js/jquery.js"></script>
<script src="js/epadd.js"></script>
	<style>
		.hfill {padding:10px;}
		.vfill { height:10px;}
	</style>
<!-- <script>epadd.select_link('#nav1', 'Troubleshoot');</script> -->
<script>
	process = function(id){
		ready = function(){
			if (this.readyState != 4)
				return;

			currentOp.done = true;
			if (currentOp.cancelled)
			{
			    $('.muse-overlay').hide();		
				return;
			}
			$(currentOp.status_div).hide();
			$('.muse-overlay').hide();

			currentOp.status_div_text.innerHTML = "<br/>&nbsp;<br/>"; 
			if (this.status != 200 && this.status != 0)
			{
				window.location = "error";
				epadd.log ("Error: status " + this.status + " received from page: " + page);
				return;
			}
			
			$(".loading").remove();
			window.location.reload();

		};
		params = "";
		page = "ajax/checkFeaturesIndex.jsp";
		epadd.log(page+params);
	
		fetch_page_with_progress(page, "status", document.getElementById('status'), document.getElementById('status_text'), params, ready);
		$("#"+id).append("<img style='height:15px' class='loading' src='images/spinner.gif'/>");
	};
</script>
<h2>Troubleshoot ePADD</h2>
<hr>
<h3>Features</h3>
<%
	boolean findex = EntityFeature.indexExists(archive);
	if(findex)
		out.println("<div id='t1'>Features index exists. Click here to recreate the features index <button class='btn-default' onclick='process(\"t1\")'>Recreate</button></div>");
	else
		out.println("<div id='t2'>Features index is not created. Please click here to create the features index <button class='btn-default' onclick='process(\"t2\")'>Create</button></div>");
%>
<jsp:include page="../footer.jsp"/>
</body>
</html>
