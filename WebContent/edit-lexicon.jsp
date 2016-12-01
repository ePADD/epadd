<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<!DOCTYPE html>
<%@include file="getArchive.jspf" %>
<% 
// params:
// lexicon=<lexicon name>. if no lexicon name, use "default"

	boolean freshLexicon = false; // track whether this is a newly created lexicon -- we suppress the "create new lexicon" option if so.

	// which lexicon? first check if url param is present, then check if url param is specified
	String lexiconName = request.getParameter("lexicon");

	Lexicon lex = null;
	if (!Util.nullOrEmpty(lexiconName))
		lex = archive.getLexicon(lexiconName);

	// read the default lexicon from the session if no explicit parameter for the lexicon name
	if (lex == null) {
		lex = new Lexicon(archive.baseDir, lexiconName);
		JSPHelper.log.info ("Creating new lexicon: " + lexiconName);
	}

	// get the final name for lex
	lexiconName = lex.name;

	JSPHelper.log.info ("lex lexiconName = " + lexiconName + " loaded lex's lexiconName = " + ((lex == null) ? "(lex is null)" : lex.name));

	// ok, now lexiconName and lex are set up
%>
<html>
<head>
	<title>Lexicons</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png"/>

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css"/>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<% if (lex == null) { %>
	Sorry, there is no lexicon named <%=lexiconName%>.
<%
	return;
} %>

<%writeProfileBlock(out, archive, "", "Lexicon: " + lexiconName);%>
<br/>

<div align="center">
	<p>

	<div id="categories">
		<%
		boolean noCategories = false;
		// can show expandedMap here, but then disable update/query option so that the expanded map doesn't get saved
		Map<String, String> captionToQueryMap = lex.getRawMapFor("english");
		if (captionToQueryMap != null && captionToQueryMap.size() > 0)
		{
			for (String sentiment: captionToQueryMap.keySet())
			{
				String query = captionToQueryMap.get(sentiment);
				int nRows = query.length()/120 + 1;
				%>
				<p>
				<div class="lexiconCategory">
					<b><%=sentiment%></b> (<a target="_blank" class="test-lexicon" href="#">Test</a>)<br/>
					<textarea style="padding:5px" cols="120" rows="<%=nRows%>" name="<%=sentiment%>" ><%=query%></textarea>
				</div>
				<%
			}
		}
		else
		{
			%>
			No categories in this lexicon. Please create some.
			<%
			noCategories = true;
		}
		%>
	</div> <!--  categories -->
	<p>
<br/>
	<button id="add-category" class="btn-default" class="tools-pushbutton" ><i class="fa fa-plus"></i> Add a category</button>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	<button class="btn-default" id="save-button" style="<%= (noCategories?"display:none":"")%>" class="tools-pushbutton" ><i class="fa fa-save"></i> Save Lexicon</button>

	<script type="text/javascript">
		$(document).ready(function() {
			function addCategory() {
				var name = prompt("Category name");
				if (name == null || name.length == 0)
					return;

				// target = _blank is important here, otherwise the user will navigate off the page, and may not have saved the new category!
				var html = '<br/><div class="lexiconCategory"> <b>' + name + '</b> (<a target="_blank" class="test-lexicon" href="#">Test</a>) <br/><textarea cols="120" rows="2" name="' + name + '" placeholder="Enter some words or phrases, separated by |"/></div>';
				$('#categories').append(html);
				$('#save-button').fadeIn(); // always show, otherwise it may be hidden if we started with 0 categories
				var areas = $('textarea');
				areas[areas.length - 1].focus(); // focus on the last (just added) textarea
			}

			$('#add-category').click(addCategory);
			$('#save-button').click(function () {
				var post_params = {};
				// read all the text areas and put them in post_params;
				$('#categories textarea').each(function (i, o) {
					post_params[$(o).attr('name')] = $(o).val();
				});
				post_params.lexicon = '<%=lexiconName%>';
				post_params.language = 'english';
				$('#save-button .fa').addClass('fa-spin');
				$.ajax({
					url: 'ajax/save-lexicon.jsp',
					type: 'POST',
					dataType: 'json',
					data: post_params,
					success: function (j) {
						$('#save-button .fa').removeClass('fa-spin');
						epadd.alert('Lexicon with ' + j.nCategories + ' categories saved.');
					},
					error: function (j) {
						$('#save-button .fa').removeClass('fa-spin');
						epadd.alert('Sorry! There was an error saving the lexicon. Please try again, and if the error persists, report it to epadd_project@stanford.edu.');
					}
				});
			});

			var test_lexicon = function(e) {
				var $lexicon = $(e.target).closest('.lexiconCategory');
				var lexiconTerms = $('textarea', $lexicon).val();
				if (!lexiconTerms)
					return;
				var lexiconTermsArr = lexiconTerms.split('|');

				var url = 'multi-search?';
				for (var i = 0; i < lexiconTermsArr.length; i++) {
					url += 'term=' + lexiconTermsArr[i] + '&'; // there will be a trailing &, that's ok
				}
				$(e.target).attr('href', url);
				return true;
			};
			$('.test-lexicon').live('click', test_lexicon); // .live is important because we want it to work with new categories added on the page
		});
	</script>
</div>
<br/>

<jsp:include page="footer.jsp"/>
</div>
</body>
</html>
