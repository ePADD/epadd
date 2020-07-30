<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@include file="getArchive.jspf" %>

<!DOCTYPE HTML>
<html>


<head>


	<meta name="viewport" content="width=device-width, initial-scale=1">

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css">
	<link rel="stylesheet" href="css/main.css">

<%-- jquery was present here earlier--%>
	<script src="js/jquery.js"></script>

	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>

	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	<style>
		td > div {
			padding: 5px;
		}

		.option {
			margin-right: 15px;
		}

		.underlined-header { border-bottom: solid 4px #0175bc; }
		.search-header { font-size: 100%; cursor: pointer; padding-bottom:5px;}
	</style>
</head>
<body>

<%-- The file header.jspf was present here --%>
<%@include file="header.jspf"%>
<title> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.head-search")%> </title>

<script>epadd.nav_mark_active('Search');</script>

<%writeProfileBlock(out, false, archive, edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.profile-search"), 900);%>

<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>
<nav class="menu1" role="navigation">
	<h2><b><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "search-query.help.head")%></b></h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<div class="search-tips" style="display:block">

		<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode() || ModeConfig.isDeliveryMode()) { %>
			<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "search-query.help.appraisal-processing-delivery")%>


		<% } else if (ModeConfig.isDiscoveryMode()) { %>
			<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "search-query.help.discovery")%>
		<% } %>
	</div>
</nav>

<br/>
<br/>

<div style="text-align:center; margin:auto; width:900px;">
	<div style="width:100%;margin-bottom:20px;">
		<a id="simple-search-header"  class="search-header" > <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.simple-title")%> </a>
		<a id="query-generator-header" class="search-header" style="margin-left:40px;"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.multi-entity-title")%> </a>
		<% if(!ModeConfig.isPublicMode()){%>
		<a id="term-search-header" class="search-header" style="margin-left:40px;"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.multi-term-title")%> </a>
		<a id="correspondent-list-search-header" class="search-header" style="margin-left:40px;"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.correspondent-title")%> </a>
		<%}%>
	</div>

	<div id="simple-search" style="text-align:center">
		<form method="get" action="browse">
	<%--		hidden input field to pass archiveID to the server. This is a common pattern used to pass
			//archiveID in all those forms where POST was used to invoke the server page.
	--%>
		<input type="hidden" value="<%=archiveID%>" class="form-control" name="archiveID"/>
			<input style="width:900px" name="term" placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.simple-search-description")%>"/>
			<div style="display:none">
				<input type="hidden" name="adv-search"/>
				<input type="checkbox" name="termBody" checked>
				<input type="checkbox" name="termSubject" checked>
				<input type="checkbox" name="termAttachments" checked>
				<input type="checkbox" name="termOriginalBody" checked>
			</div>
			<br/>
			<br/>


			<button class="btn btn-cta" style="margin-top: 5px" type="submit" name="Go"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.search")%> <i class="icon-arrowbutton"></i></button>
		</form>
	</div>
<p>

	<div style="display:none" id="term-search">
		<form method="post" action="query-generator" accept-charset="UTF-8">
			<input type="hidden" value="<%=archiveID%>" class="form-control" name="archiveID"/>
			<textarea placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.multi-term-search-description")%>" name="refTextTerms" id="refTextTerms" style="width:900px" rows="10"></textarea>
			<br/>
			<div style="text-align:center">
				<button class="btn btn-cta" style="margin-top: 5px" type="submit" name="Go"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.search")%> <i class="icon-arrowbutton"></i></button>
			</div>
		</form>
	</div>

	<div style="display:none" id="query-generator">
		<form method="post" action="query-generator" accept-charset="UTF-8">
			<input type="hidden" value="<%=archiveID%>" class="form-control" name="archiveID"/>
			<textarea placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.multi-entity-search-description")%>" name="refText" id="refText" style="width:900px" rows="10"></textarea>
			<br/>
			<div style="text-align:center">
				<button class="btn btn-cta" style="margin-top: 5px" type="submit" name="Go"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.search")%> <i class="icon-arrowbutton"></i></button>
			</div>
		</form>
	</div>

	<div style="display:none" id="correspondent-list-search">
		<form method="post" action="browse" accept-charset="UTF-8">
			<input type="hidden" value="<%=archiveID%>" class="form-control" name="archiveID"/>
			<textarea placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.correspondent-list-search-description")%>" name="correspondentList" id="correspondentList" style="width:900px" rows="10"></textarea>
			<br/>
			<div style="text-align:center">
				<button class="btn btn-cta" style="margin-top: 5px" type="submit" name="Go"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.search")%> <i class="icon-arrowbutton"></i></button>
			</div>
		</form>
	</div>

	<br/>
	<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.pre-advanced-text")%>  <a href="advanced-search?archiveID=<%=archiveID%>"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "search-query.advanced-search")%></a>.
	<br/>
	<br/>
	<br/>
	<br/>
	<br/>
	<br/>
	<br/>
	<br/>
	<br/>
	<br/>
	<br/>
	<br/>


</div>

<script>
	$(document).ready(function() {
		$('#simple-search-header').addClass('underlined-header');
		$('#simple-search-header').click(function() {
            $('.search-header').removeClass('underlined-header');
			$('#simple-search-header').addClass('underlined-header');

            $('#simple-search').show();
			$('#query-generator').hide();
			$('#term-search').hide();
            $('#correspondent-list-search').hide();
        });
		$('#query-generator-header').click(function() {
            $('.search-header').removeClass('underlined-header');
            $('#query-generator-header').addClass('underlined-header');

            $('#simple-search').hide();
			$('#term-search').hide();
			$('#query-generator').show();
            $('#correspondent-list-search').hide();
        });
        $('#term-search-header').click(function() {
            $('.search-header').removeClass('underlined-header');
            $('#term-search-header').addClass('underlined-header');

            $('#simple-search').hide();
            $('#term-search').show();
            $('#query-generator').hide();
            $('#correspondent-list-search').hide();
        });

        $('#correspondent-list-search-header').click(function() {
            $('.search-header').removeClass('underlined-header');
            $('#correspondent-list-search-header').addClass('underlined-header');

            $('#simple-search').hide();
            $('#term-search').hide();
            $('#query-generator').hide();
            $('#correspondent-list-search').show();
        });
	});

</script>

<p>
<jsp:include page="footer.jsp"/>
</body>
</html>
