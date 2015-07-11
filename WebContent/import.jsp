<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.net.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="com.google.gson.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.epadd.Config.*"%>

<html>
<head>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	
	<script src="js/jquery.js"></script>
	<link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
	<script src="jqueryFileTree/jqueryFileTree.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<jsp:include page="css/css.jsp"/>
	<title>Import accession</title>
	<script src="js/epadd.js" type="text/javascript"></script>
	<script src="js/filepicker.js" type="text/javascript"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Add');</script>

<p>

<section>
	<div id="filepicker" style="width:900px;padding-left:170px">

		<div class="div-input-field">
			<div class="input-field-label"><i class="fa fa-folder-o"></i> Accession folder</div>
			<div class="input-field">
				<input id="sourceDir" class="dir form-control" type="text" name="sourceDir"/> <br/>
				<button onclick="return false;" class="btn-default"><i class="fa fa-file"></i>
					<span>Browse</span>
				</button>
			</div>
			<br/>
			<div class="roots" style="display:none"></div>
			<div class="browseFolder"></div>
			<br/>
		</div>
	</div>
	<br/>
	<br/>
	
	<%
	java.io.File[] rootFiles = java.io.File.listRoots(); 
	List<String> roots = new ArrayList<String>();
	for (java.io.File f: rootFiles)
		roots.add(f.toString());
	String json = new Gson().toJson(roots);
	%>
	<script> 
		var roots = <%=json%>;
		var fp = new FilePicker($('#filepicker'), roots);
	</script>
	<div style="text-align: center;">
		<button class="btn btn-cta" id="gobutton">Import accession <span class="spinner"><i class="icon-arrowbutton"></i></span> </button>
	</div>

	<script type="text/javascript">
		$('#gobutton').click(function(e) {
			epadd.load_archive(e, $('#sourceDir').val());
			return false;
		});
	</script>
</section>

 <jsp:include page="footer.jsp"/>
 
</body>
</html>
