<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.net.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>

<html>
<head>
<script src="js/jquery.js" type="text/javascript"></script>
<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
<link rel="icon" type="image/png" href="images/epadd-favicon.png">
<jsp:include page="css/css.jsp"/>
<title>ePADD Help</title>
<style>
	.title { text-align:center; border: solid 1px black; padding: 5px}
	.description { border: solid 1px black; padding: 5px;}
	.hfill { width:20px}
	.vfill { height:20px}
</style>
<script src="js/epadd.js" type="text/javascript"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.select_link('#nav1', 'Help');</script>

<p>
<br/>
<h1>ePADD Help</h1>
<p>
<p>
To be filled in...
<p>

 <jsp:include page="footer.jsp"/>
 
</body>
</html>
