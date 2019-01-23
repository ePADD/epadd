<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="java.util.*"%>
<%@page import="edu.stanford.muse.index.*"%>
<%@page import="java.io.File" %>
<!DOCTYPE html>
<%@include file="getArchive.jspf" %>
<%
	// which lexicon? first check if url param is present, then check if url param is specified
	String lexiconName = request.getParameter("lexicon");

	Lexicon lex = null;
	if (!Util.nullOrEmpty(lexiconName))
		lex = archive.getLexicon(lexiconName);

	// read the default lexicon from the session if no explicit parameter for the lexicon name
	if (lex == null) {
		lex = new Lexicon(archive.baseDir+ File.separator + Archive.BAG_DATA_FOLDER + File.separatorChar + Archive.LEXICONS_SUBDIR, lexiconName);
		JSPHelper.log.info ("Creating new lexicon: " + lexiconName);
	}

	// get the final name for lex
	lexiconName = lex.name;
	boolean isRegex = Lexicon.REGEX_LEXICON_NAME.equals (lexiconName);

	JSPHelper.log.info ("lex lexiconName = " + lexiconName + " loaded lex's lexiconName = " + ((lex == null) ? "(lex is null)" : lex.name));

	// ok, now lexiconName and lex are set up
%>
<html>
<head>
	<title>Lexicons</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png"/>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css"/>
	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css">

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>
	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<%@include file="header.jspf"%>
<script>epadd.nav_mark_active('Browse');</script>

<style type="text/css">
	.lexiconCategory {
		width:fit-content;/*for chrome */
		width: -moz-max-content; /*for firefox*/
		/*width: intrinsic;           !* Safari/WebKit uses a non-standard name *!*/
	}
	.lexiconName {float:left}
	.test-category {cursor:pointer;
		float:right; margin-right: 10px}
	.delete-category {cursor:pointer;
		float:right; margin-right: 10px}
</style>

<% if (lex == null) { %>
	 Sorry, there is no lexicon named <%=lexiconName%>.
<%
	return;
} %>

<%writeProfileBlock(out, archive, "Lexicon: " + lexiconName);%>
<br/>

<!--sidebar content-->
<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>

<nav class="menu1" role="navigation">
	<h2>Lexicon Editing Tips</h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<p>To add a lexicon category, select the Add a Category button. To remove a category, simply remove all words in it.

	<p>New entries in a category should be separated with the | (pipe) character.

	<p>Multi-word phrases should be enclosed in double-quotes.

	<p>Note that words and phrases with hyphens, such as father-in-law, should be spelled without hyphens.
	<% if (!isRegex) { %>
		<p>Select Test for a given category to view the number of hits for each keyword in that category. <!-- test not available if in regex -->
	<% } %>
</nav>
<div align="center">
<div class="button_bar_on_lexicon">
	<div title="Add Category" class="buttons_on_datatable" id="add-category"><img class="button_image_on_datatable" src="images/add_lexicon.svg"></div>
	<div title="Download Lexicon" class="buttons_on_datatable" onclick="exportLexiconHandler();return false;"><img class="button_image_on_datatable" src="images/download.svg"></div>
	<div title="Delete Lexicon" class="buttons_on_datatable" onclick="deleteLexiconHandler();return false;"><img class="button_image_on_datatable" src="images/delete.svg"></div>
</div>
</div>
<!--/sidebar-->
<div align="center">
	<p>

	<div id="categories">
		<%
		boolean noCategories = false;
		// can show expandedMap here, but then disable update/query option so that the expanded map doesn't get saved
		Map<String, String> captionToQueryMap = lex.getRawMapFor(lex.getLexiconLanguage());
		if (captionToQueryMap != null && captionToQueryMap.size() > 0)
		{
			for (String sentiment: captionToQueryMap.keySet())
			{
				String query = captionToQueryMap.get(sentiment);
				int nRows = query.length()/120 + 1;
				%>
				<p>
				<div class="lexiconCategory">
					<b class="lexiconName"><%=sentiment%></b>
					<% if (!isRegex) { %>
	<div  title="Test category" class="test-category" onclick="test_category(event);"><img style="height:20px" src="images/test.svg"></div>
	<div  title="Delete category" class="delete-category" onclick="delete_category(event);"><img style="height:20px" src="images/delete.svg"></div>
					<% } %>
                    <br/>
					<textarea style="width:1100px;height:<%=(nRows+1)*20%>" name="<%=sentiment%>" ><%=query%></textarea>
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
<br/>

<button class="btn-default" id="save-button" style="<%= (noCategories?"display:none":"")%>" class="tools-pushbutton" ><i class="fa fa-save"></i> Save</button>

	<script type="text/javascript">
        var exportLexiconHandler=function(){
            var x='<%=lexiconName%>'
            $.ajax({
                type: 'POST',
                url: "ajax/downloadData.jsp",
                data: {archiveID: archiveID, data: "lexicon", lexicon:'<%=lexiconName%>'},
                dataType: 'json',
                success: function (data) {
                    epadd.info_confirm_continue('The file <%=lexiconName%>.english.lex.txt will be downloaded to your browser\'s download folder.', function () {
                        window.location=data.downloadurl;
                    });
                },
                error: function (jq, textStatus, errorThrown) {
                    epadd.error("Error exporting file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
                }
            });
        }

        var deleteLexiconHandler = function() {
            var post_params = {};
            post_params.archiveID = '<%=archiveID%>';
            post_params.lexicon = '<%=lexiconName%>';
            epadd.warn_confirm_continue('Do you want to delete lexicon ' + post_params.lexicon + '? This action cannot be undone.', function() {
                $.ajax({
                    url: 'ajax/delete-lexicon.jsp',
                    type: 'POST',
                    dataType: 'json',
                    data: post_params,
                    success: function (j) {
                        epadd.success('Lexicon \'' + post_params.lexicon + '\' deleted.', function () {
                                window.location = 'lexicon-top?archiveID=' + post_params.archiveID;
                        });
                    },
                    error: function (j) {
//                        $('#save-button .fa').removeClass('fa-spin');
                        epadd.error('Sorry, there was an error removing the lexicon. Please try again, and if the error persists, report it to epadd_project@stanford.edu.');
                    }
                });
            });
        }

        var test_category = function(e) {
            //$(e.target).closest('.lexiconCategory').children().first().text()
            var $lexicon = $(e.target).closest('.lexiconCategory');
            var lexiconTerms = $('textarea', $lexicon).val();
            if (!lexiconTerms)
                return;
            var lexiconTermsArr = lexiconTerms.split('|');

            var url = 'multi-search?archiveID=<%=archiveID%>&';
            for (var i = 0; i < lexiconTermsArr.length; i++) {
                url += 'term=' + lexiconTermsArr[i] + '&'; // there will be a trailing &, that's ok
            }
            window.location=url;
            //$(e.target).attr('href', url);
            return false;
        };

        var delete_category = function(e) {
            epadd.warn_confirm_continue('Do you want to delete this category? This action cannot be undone.', function() {
                var $lexicon = $(e.target).closest('.lexiconCategory');
                $($lexicon).attr('onclick', "");//remove event handler otherwise the other one is getting called
                $($lexicon).remove();
            });
            return false;
        };

        $(document).ready(function() {
			function addCategory() {
				var name = prompt("Category name");
				if (name == null || name.length == 0)
					return;

				// target = _blank is important here, otherwise the user will navigate off the page, and may not have saved the new category!
				var html = '<br/><div class="lexiconCategory" > <b class="lexiconName">' + name + '</b>';
				var placeholder;

                <% if (!isRegex) { %>
	    			html += '<div  title="Test category" class="test-category" onclick="test_category(event);"><img style="height:20px" src="images/test.svg"></div>';
                	html += '<div  title="Delete category" class="delete-category" onclick="delete_category(event);"><img style="height:20px" src="images/delete.svg"></div>';

                placeholder = 'Enter some words or phrases, separated by |';
    	    	<% } else { %>
				    placeholder = 'Enter a regular expression';
    	    	<% } %>
                html += '<br/><textarea style="width:1100px;" cols="120" rows="2" name="' + name + '" placeholder="' + placeholder + '"/></div>';

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
				post_params.archiveID = '<%=archiveID%>';
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
						epadd.success('Lexicon with ' + j.nCategories + ' categories saved.');
					},
					error: function (j) {
						$('#save-button .fa').removeClass('fa-spin');
						epadd.error('Sorry, there was an error saving the lexicon. Please try again, and if the error persists, report it to epadd_project@stanford.edu.');
					}
				});
			});


			$('.test-lexicon').live('click', test_category); // .live is important because we want it to work with new categories added on the page
		});
	</script>
</div>
<br/>

<jsp:include page="footer.jsp"/>
</div>
</body>
</html>
