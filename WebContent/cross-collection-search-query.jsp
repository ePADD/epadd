<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
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
</head>
<body>
<jsp:include page="header.jspf"/>
<br/>
<br/>

<div style="text-align:center; margin:auto; width:600px;">
	<div style="width:100%;margin-bottom:20px;">
		Cross-collection search
	</div>

	<div id="cross-collection-search" style="text-align:center">
		<form method="get" action="cross-collection-search">

			<input name="term" size="80" placeholder="search query"/>
			<br/>
			<br/>

			<button class="btn btn-cta" style="margin-top: 5px" type="submit" name="Go">Search <i class="icon-arrowbutton"></i></button>

		</form>
	</div>
	<p>
</div>

<script>
    $(document).ready(function() {

    });

</script>

<p>
	<jsp:include page="footer.jsp"/>
</body>
</html>
