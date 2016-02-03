<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
	<title>Lexicon</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	
	<script src="js/jquery.js"></script>
	<script src="js/jquery.dataTables.min.js"></script>
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>
	
	<style type="text/css">
     .js #table  {display: none;}
      .search {cursor:pointer;}
    </style>
	<script type="text/javascript" charset="utf-8">
		// we're using a simple datatables data source here, because this will be a small table
		$('html').addClass('js'); // see http://www.learningjquery.com/2008/10/1-way-to-avoid-the-flash-of-unstyled-content/
		$(document).ready(function() {
		  //if the paging is set, then the lexicon anchors in the subsequent pages are not hyperlinked. Lexicons typically do not need paging,
		  var oTable = $('#table').dataTable({paging:false, columnDefs: [{ className: "dt-right", "targets": 1}]});
		  oTable.fnSort( [ [1,'desc'] ] );
		  $('#table').show();		  
		  $('.search').click(epadd.do_sentiment_search);
		} );
	</script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
	Indexer indexer = archive.indexer;
	AddressBook ab = archive.addressBook;
	String bestName = ab.getBestNameForSelf();
%>
<%writeProfileBlock(out, bestName, "", "Lexicon Hits");%>

<!--
<h1 title="title_tooltip">
	Email Archives of <%= bestName%><br/>
	<span style="font-size:60%">Lexicon Hits</span>
</h1>
-->

<div style="text-align:center">
	<button class="btn-default" onclick="window.location = 'graph?view=sentiments';"><i class="fa fa-bar-chart-o"></i> Go To Graph View</button>
	&nbsp;&nbsp;
	<button id="edit-lexicon" class="btn-default"><i class="fa fa-edit"></i> View/Edit Lexicon </button>
	&nbsp;&nbsp;
	<button id="create-lexicon" class="btn-default"><i class="fa fa-plus"></i> Create new lexicon</button>
</div>
<br/>

<%
	Set<DatedDocument> allDocs = new LinkedHashSet<DatedDocument> ((Collection) JSPHelper.selectDocs(request, session, true /* only apply to filtered docs */, false));

	Lexicon lex = null;
	// first look for url param for lexicon name if specified
	String name = request.getParameter("name");
	if (!Util.nullOrEmpty(name))
		lex = archive.getLexicon(name);
	else
	{
		// if no url param, look for lexicon in session
		lex = (Lexicon) JSPHelper.getSessionAttribute(session, "lexicon");
		if (lex != null)
		{
			name = lex.name;
			lex = archive.getLexicon(name); // re-read it from the disk. we may have come here just after updating it.
		}

		if (lex == null)
		{
			// if not in session either, simply look for default
			name = "general";
			lex = archive.getLexicon(name);
		}
	}

	if (lex == null) {
		out.println ("<div style=\"text-align:center\">Sorry! No lexicon named " + Util.escapeHTML(name) + "</div>");
	} else {
		session.setAttribute("lexicon", lex);
		Map<String, Integer> map = lex.getLexiconCounts(indexer, true);
		Collection<String> lexiconNames = archive.getAvailableLexicons();
		if (ModeConfig.isDeliveryMode()) {
			lexiconNames = new LinkedHashSet(lexiconNames); // we can't call remve on the collection directly, it throws an unsupported op.
			lexiconNames.remove("sensitive");
		}

		boolean onlyOneLexicon = (lexiconNames.size() == 1);
		// common case is only one lexicon, don't show load lexicon in this case.
		if (lexiconNames.size() > 1) { %>
			<script>function changeLexicon() {	window.location = 'lexicon?name=' +	$('#lexiconName').val(); }</script>

			<div style="text-align:center">
			<select id="lexiconName" onchange="changeLexicon()">
			<% for (String n: lexiconNames) { %>
				%> <option <%=name.equalsIgnoreCase(n) ? "selected":""%> value="<%=n.toLowerCase()%>"><%=Util.capitalizeFirstLetter(n)%></option>
			<% } %>
			</select>
			</div>
			<br/>
	<% } %>
			
</h1>
<div style="margin:auto; width:800px">
	<table id="table">
		<thead><th>Lexicon category</th><th>Messages</th></thead>
		<tbody>			
			<%
			for (String caption: map.keySet()) {
				%>
				<tr><td class="search"><%=caption %></td><td><%=map.get(caption)%></td></tr>
				<%
			}
			%>
		</tbody>
	</table>
</div>
<p>
<br/>
<% } // lex != null %>

	<script>
		$('#edit-lexicon').click (function() { window.location='edit-lexicon?lexicon=<%=lex.name%>';})
		$('#create-lexicon').click (function() {
			var lexiconName = prompt ('Enter the name of the new lexicon:');
			if (!lexiconName)
				return;
			window.location = 'edit-lexicon?lexicon=' + lexiconName;
		});
	</script>

	<jsp:include page="footer.jsp"/>
</body>
</html>
