<%@page contentType="text/html; charset=UTF-8"%>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="edu.stanford.muse.index.DatedDocument" %>
<%@ page import="edu.stanford.muse.index.Indexer" %>
<%@ page import="edu.stanford.muse.index.Lexicon" %>
<%@page language="java" import="java.util.Collection"%>
<%@page language="java" import="java.util.LinkedHashSet"%>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
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
     .js #table  {display: none;}
      .search {cursor:pointer;}
    </style>

	<%
		String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	%>
	<script type="text/javascript" charset="utf-8">
		// we're using a simple datatables data source here, because this will be a small table
		$('html').addClass('js'); // see http://www.learningjquery.com/2008/10/1-way-to-avoid-the-flash-of-unstyled-content/
		$(document).ready(function() {
			function do_lexicon_search(e) {
			  var cat = $(e.target).text();
			  if (window.is_regex)
                  window.open('browse?adv-search=1&archiveID=<%=archiveID%>&sensitive=true&lexiconCategory=' + cat + '&lexiconName=' + $('#lexiconName').val()); // sensitive=true is what enables regex highlighting
			  else
                  window.open('browse?adv-search=1&archiveID=<%=archiveID%>&lexiconCategory=' + cat + '&lexiconName=' + $('#lexiconName').val());
            }
            //if the paging is set, then the lexicon anchors in the subsequent pages are not hyperlinked. Lexicons typically do not need paging, so we list all categories in one page
			var oTable = $('#table').dataTable({paging:false, columnDefs: [{ className: "dt-right", "targets": 1}]});
			oTable.fnSort( [ [1,'desc'] ] );
			$('#table').show();

			// attach the click handlers
			$('.search').click(do_lexicon_search);
		} );
	</script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%writeProfileBlock(out, archive, "", "Lexicon Hits");%>

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

<div style="text-align:center">
	<button class="btn-default" onclick="window.location = 'graph?archiveID=<%=archiveID%>&view=sentiments';"><i class="fa fa-bar-chart-o"></i> Go To Graph View</button>
	&nbsp;&nbsp;
	<button id="edit-lexicon" class="btn-default"><i class="fa fa-edit"></i> View/Edit Lexicon </button>
	&nbsp;&nbsp;
	<button id="create-lexicon" class="btn-default"><i class="fa fa-plus"></i> Create new lexicon</button>
</div>
<br/>

<%
	Lexicon lex = null;
	// first look for url param for lexicon name if specified
	String lexiconname = request.getParameter("lexicon");
	if (!Util.nullOrEmpty(lexiconname))
		lex = archive.getLexicon(lexiconname);
	else
	{
        /*
		// if no url param, look for lexicon in session
		lex = (Lexicon) JSPHelper.getSessionAttribute(session, "lexicon");
		if (lex != null)
		{
			name = lex.name;
			lex = archive.getLexicon(name); // re-read it from the disk. we may have come here just after updating it.
		}
        */
		if (lex == null)
		{
			// if not in session either, simply look for default
			lexiconname = Config.DEFAULT_LEXICON;
			lex = archive.getLexicon(lexiconname);
		}
	}
    boolean isRegex = Lexicon.REGEX_LEXICON_NAME.equalsIgnoreCase (lexiconname);

	if (lex == null) {
		out.println ("<div style=\"text-align:center\">Sorry! No lexicon named " + Util.escapeHTML(lexiconname) + "</div>");
	} else {
//		session.setAttribute("lexicon", lex);
		Map<String, Integer> map = lex.getLexiconCounts(archive.indexer, !isRegex /* originalContent only */, isRegex);
		Collection<String> lexiconNames = archive.getAvailableLexicons();
		if (ModeConfig.isDeliveryMode()) {
			lexiconNames = new LinkedHashSet(lexiconNames); // we can't call remve on the collection directly, it throws an unsupported op.
			lexiconNames.remove(Lexicon.SENSITIVE_LEXICON_NAME);
		}

		boolean onlyOneLexicon = (lexiconNames.size() == 1);
		// common case is only one lexicon, don't show load lexicon in this case.
		if (!onlyOneLexicon) { %>
			<script>function changeLexicon() {	window.location = 'lexicon?archiveID=<%=archiveID%>&lexicon=' +	$('#lexiconName').val(); }</script>

			<div style="text-align:center">

					<div class="form-group" style="width:20%; margin-left:40%">
						<label for="lexiconName">Choose Lexicon</label>
						<select id="lexiconName" name="lexiconName" class="form-control selectpicker">
							<% for (String n: lexiconNames) { %>
							%> <option <%=lexiconname.equalsIgnoreCase(n) ? "selected":""%> value="<%=n.toLowerCase()%>"><%=Util.capitalizeFirstLetter(n)%></option>
							<% } %>
						</select>
					</div>

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
	<script>
		$(document).ready (function() {
            window.is_regex = <%=isRegex%>;
			$('#lexiconName').change (changeLexicon);
			$('#edit-lexicon').click (function() { window.location='edit-lexicon?archiveID=<%=archiveID%>&lexicon=<%=lex.name%>';});
			$('#create-lexicon').click (function() {
				var lexiconName = prompt ('Enter the name of the new lexicon:');
				if (!lexiconName)
					return;
				window.location = 'edit-lexicon?archiveID=<%=archiveID%>&lexicon=' + lexiconName;
			});
		});
	</script>

<% } // lex != null %> 
	<jsp:include page="footer.jsp"/>
</body>
</html>
