<%@page contentType="text/html; charset=UTF-8"%>
<%@ page import="edu.stanford.muse.Config" %>
<%@page language="java" import="java.util.Collection"%>
<%@page language="java" import="java.util.LinkedHashSet"%>
<%@ page import="edu.stanford.muse.index.*" %>
<%@ page import="org.json.JSONArray" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
	<title>Lexicon</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<jsp:include page="css/css.jsp"/>
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<link rel="stylesheet" href="css/sidebar.css">

	<script src="js/jquery.js"></script>
	<script src="js/jquery.dataTables.min.js"></script>

	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>

	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<script src="js/epadd.js"></script>
	
	<style type="text/css">
     /*.js #table  {display: none;}*/
      .search {cursor:pointer;}
    </style>
	<style>
		.modal-body {
			/* 100% = dialog height, 120px = header + footer */
			max-height: calc(100% - 120px);
			overflow-y: scroll;
		}
	</style>

	<%
		String lexiconname = request.getParameter("lexicon");
	%>

</head>
<body>
<%@include file="header.jspf"%>
<script>epadd.nav_mark_active('Browse');</script>

<%writeProfileBlock(out, false, archive, "Lexicon Hits: "+lexiconname, 900);%>

<!--sidebar content-->
<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>

<nav class="menu1" role="navigation">
	<h2>Lexicon Tips</h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<p>Lexicons are customizable saved searches containing categories of keywords.
	<p>Define the default lexicon using the config.properties file.
	<p>Selecting a lexicon category from this screen will display the messages containing keywords in that category.
	<p>Select a different lexicon using the Choose Lexicon dropdown.
	<p>Lexicons can be viewed in detail and edited by selecting the View/Edit Lexicon button.
	<p>Create a new lexicon by selecting the Create New Lexicon button.

</nav>
<!--/sidebar-->


<%
	Lexicon lex = null;
	// first look for url param for lexicon name if specified
	if (!Util.nullOrEmpty(lexiconname))
		lex = archive.getLexicon(lexiconname);

	if (lex == null)
	{
		// if not in session either, simply look for default
		lexiconname = Config.DEFAULT_LEXICON;
		lex = archive.getLexicon(lexiconname);
	}

	if (lex == null) {
		out.println ("<div style=\"text-align:center\">Sorry! No lexicon named " + Util.escapeHTML(lexiconname) + "</div>");
	}
	boolean isRegex = Lexicon.REGEX_LEXICON_NAME.equalsIgnoreCase (lexiconname);
	JSONArray map = lex.getCountsAsJSON(lexiconname,archive);
%>

	<br/>

<div style="margin:auto; width:900px">

	<div class="button_bar_on_datatable">
		<div title="Go to graph view" class="buttons_on_datatable" onclick="window.location = 'graph?archiveID=<%=archiveID%>&view=sentiments&lexicon=<%=lex.name%>';"><img class="button_image_on_datatable" src="images/graph.svg"></div>
		<%if(!ModeConfig.isDiscoveryMode()){%>
			<div title="Edit lexicon" class="buttons_on_datatable" id="edit-lexicon"><img class="button_image_on_datatable" src="images/edit_lexicon.svg"></div>
		<%}%>
	</div>

	<table id="table">
		<thead><th>Lexicon category</th><th>Messages</th></thead>
		<tbody>			
			<%
			for (int j=0; j<map.length();j++) {
				%>
				<tr><td class="search"><%=((JSONArray)map.get(j)).get(0) %></td><td><%=((JSONArray)map.get(j)).get(1)%></td></tr>
				<%
			}
			%>
		</tbody>
	</table>
</div>
<p>
<br/>

<script>
	$('html').addClass('js'); // see http://www.learningjquery.com/2008/10/1-way-to-avoid-the-flash-of-unstyled-content/
	$(document).ready (function() {
		window.is_regex = <%=isRegex%>;
		$('#edit-lexicon').click (function() { window.location='edit-lexicon?archiveID=<%=archiveID%>&lexicon=<%=lex.name%>';});
	});

	// we're using a simple datatables data source here, because this will be a small table
	$(document).ready(function() {
		function do_lexicon_search(e) {
			var cat = $(e.target).text();
			var lexname = '<%=lexiconname%>';
			if (window.is_regex)
				window.open('browse?adv-search=1&archiveID=<%=archiveID%>&sensitive=true&termBody=on&termSubject=on&termAttachments=on&lexiconCategory=' + cat + '&lexiconName=' + lexname); // sensitive=true is what enables regex highlighting
			else
				window.open('browse?adv-search=1&archiveID=<%=archiveID%>&lexiconCategory=' + cat + '&termAttachments=on&termBody=on&termSubject=on&lexiconName=' + lexname);
		}
		//if the paging is set, then the lexicon anchors in the subsequent pages are not hyperlinked. Lexicons typically do not need paging, so we list all categories in one page
		var oTable = $('#table').dataTable({paging:false, columnDefs: [{ className: "dt-right", "targets": 1}]});
		oTable.fnSort( [ [1,'desc'] ] );
		$('#table').show();

		// attach the click handlers
		$('.search').click(do_lexicon_search);
	} );
</script>


<jsp:include page="footer.jsp"/>
</body>
</html>
