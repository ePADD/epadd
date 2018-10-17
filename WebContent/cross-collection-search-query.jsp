<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<!DOCTYPE HTML>
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Search</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script  src="js/jquery-1.12.1.min.js"></script>
	<script src="js/jquery.autocomplete.js" type="text/javascript"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<%@include file="header.jspf"%>
<br/>
<br/>

<div style="text-align:center; margin:auto; width:600px;">
	<div style="width:100%;margin-bottom:20px;">
		Cross-collection entity search
	</div>

	<div id="cross-collection-search" style="text-align:center">
		<form method="get" action="cross-collection-search">

			<input id="xcoll-search" name="term" size="80" placeholder="search query"/>
			<br/>
			<br/>

			<button class="btn btn-cta" style="margin-top: 5px" type="submit" name="Go">Search <i class="icon-arrowbutton"></i></button>

		</form>
	</div>
	<p>
</div>

<script>
    $(document).ready(function() {
        var autocomplete_params = {
            serviceUrl: 'ajax/xcollSearchAutoComplete.jsp',
            onSearchError: function (query, jqXHR, textStatus, errorThrown) {epadd.log(textStatus+" error: "+errorThrown);},
            preventBadQueries: false,
            showNoSuggestionNotice: true,
            preserveInput: true,
            ajaxSettings: {
                "timeout":5000, /* 5000 instead of 3000 because xcoll search is likely to be slow */
                dataType: "json"
            },
            dataType: "text",
            //100ms
            deferRequestsBy: 100,
            onSelect: function(suggestion) {
                var existingvalue = $(this).val();
                var idx = existingvalue.lastIndexOf(';');
                if (idx <= 0)
                    $(this).val(suggestion.name);
                else
                    $(this).val (existingvalue.substring (0, idx+1) + ' ' + suggestion.name); // take everything up to the last ";" and replace after that
            },
            onHint: function (hint) {
                $('#autocomplete-ajax-x').val(hint);
            },
            onInvalidateSelection: function() {
                epadd.log('You selected: none');
            }
        };
        $('#xcoll-search').autocomplete(autocomplete_params);
    });

</script>

<p>
	<jsp:include page="footer.jsp"/>
</body>
</html>
