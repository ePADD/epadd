<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Search</title>
	
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>
	
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
<style>
	td > div {
		padding: 5px;
	}
	.option { margin-right:15px;}
</style>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Search');</script>

<%
AddressBook ab = archive.addressBook;
String bestName = ab.getBestNameForSelf();
String title = "Email Archive " + (!Util.nullOrEmpty(bestName) ? ("of " + bestName) : "SUMMARY");
%>
<%writeProfileBlock(out, bestName, "", "Search");%>
<br/>
<br/>

<div style="text-align:center; margin:auto; width:600px;">
<div style="text-align:left; padding:5px">
<form method="get" action="browse">
<input name="term" size="80" placeholder="search query"/>
    <input name="start_date" size="8" placeholder="mm/yyyy"/>
    <input name="end_date" size="8" placeholder="mm/yyyy"/>
<br/>
<br/>
	<!--
<input type="radio" name="searchType" value="correspondents"/> 
<span class="option" title="<%=edu.stanford.muse.util.Messages.getMessage("messages", "search.correspondents.help")%>">
	Correspondents
</span>
&nbsp;
-->
<input type="radio" name="searchType" value="subject" /> 
<span class="option"title="<%=edu.stanford.muse.util.Messages.getMessage("messages", "search.subject.help")%>">
	Subject
</span>
<!--  <input type="radio" name="searchType" value="attachments"/> Attachments  -->
&nbsp;
<input type="radio" name="searchType" value="original"/>
<span class="option" title="<%=edu.stanford.muse.util.Messages.getMessage("messages", "search.original.help")%>">
	Original Text
</span>
&nbsp;
<input type="radio" name="searchType" value="All" checked/> 
<span class="option" title="<%=edu.stanford.muse.util.Messages.getMessage("messages", "search.all.help")%>">
	All
</span>
<!-- &nbsp;
<input type="radio" name="searchType" value="regex"/> Regex
 -->
 
<p style="margin-top:10px">

Email direction:
<select name="direction" id="direction">
	<option value="in">Incoming</option>
	<option value="out">Outgoing</option>
	<option value="both" selected>Both</option>
</select>
    &nbsp&nbsp;
    Sort by:
    <select name="sort_by" id="sort_by">
        <option value="relevance">Relevance</option>
        <option value="chronological">Oldest First</option>
        <option value="recent">Recent First</option>
    </select>

<p style="text-align:center">
	<button class="btn btn-cta" class="" onclick="handle_click()">Search <i class="icon-arrowbutton"></i> </button>
</p>
</form>
<hr style="border-style:dashed"/>
<script>
function handle_click() { 
	var option = $("input[name=searchType]:checked").val();
	if ('original' == option) {
		$('#originalContentOnly').val('true');
	}
	if ('subject' == option) {
		$('#subjectOnly').val('true');
	}
	if ('correspondents' == option) {
		$('#correspondents').val('true');
	}
	return true;
}
</script>

<p>

Query Generator
<form id="folders" method="post" action="query-generator" accept-charset="UTF-8">
<textarea name="refText" id="refText" cols="80" rows="20"></textarea>
<br/>
<div style="text-align:center">
<button class="btn btn-cta" style="margin-top: 5px" type="submit" name="Go">Search <i class="icon-arrowbutton"></i> </button>
</div>
</form>

</div>
</div>
<p>
<jsp:include page="footer.jsp"/>
</body>
</html>
