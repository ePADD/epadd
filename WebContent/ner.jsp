<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page language="java" %>
<head>
    <title>ePADD</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png?v3">
    <link href='http://fonts.googleapis.com/css?family=Open+Sans' rel='stylesheet' type='text/css'>
    <script src="js/jquery.js"></script>

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>
	<style>
		.hfill {padding:10px;}
		.vfill { height:10px;}
	</style>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>

<jsp:include page="header.jspf"/>
<script type="text/javascript" src="js/statusUpdate.js"></script>
<%@include file="div_status.jspf"%>
<%@include file="getArchive.jspf" %>
<%writeProfileBlock(out, archive, "", "Named entity extraction");%>
<script>
	function start(){
		page = "ajax/nertrainandrecognise.jsp";
		//supplying the ready function to make it not redirect the other page and give us the handle of the response data.  
		fetch_page_with_progress(page, "status", document.getElementById('status'), document.getElementById('status_text'));	
	}
</script>
<body>
<div style='text-align:center'>
	<br/>
	<br/>
		We will now identify the entities in this archive using ePADD's Named Entity Recognition (NER) algorithm.
		This process can take several minutes.
	<br/>
	<br/>
	<p> <button onclick='start()' class='btn btn-cta'>Start <i class="icon-arrowbutton"></i></button>
</div>
</body>
