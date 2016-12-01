<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="com.google.gson.Gson"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="java.util.ArrayList"%>
<%@page language="java" import="java.util.List"%>
<%@include file="getArchive.jspf" %>
<html>
<head>
	<title>Export</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
	<script src="jqueryFileTree/jqueryFileTree.js"></script>
		
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>
	<script src="js/filepicker.js"></script>
</head>

<body>
	<jsp:include page="header.jspf"/>
	<script>epadd.nav_mark_active('Export');</script>
	<%
		AddressBook ab = archive.addressBook;
		writeProfileBlock(out, archive, "", "Export messages");
	%>
	<p>
	<br/>
	<br/>

	<div id="filepicker" style="width:900px;padding-left:170px">
		<div class="div-input-field">
			<div class="input-field-label"><i class="fa fa-folder-o"></i> Export folder</div>
			<div class="input-field">
				<input id="dir" class="dir form-control" type="text" name="dir"/><br/>
				<button onclick="return false;" class="btn-default"><i class="fa fa-file"></i>
					<span>Browse</span>
				</button>
			</div>
			<div class="roots" style="display:none"></div>
			<div class="browseFolder"></div>
			<br/>
			<div style="text-align:center">
				<button type="button" class="btn btn-cta" onclick="submit(); return false;">
					Export <i class="icon-arrowbutton"></i>
				</button>
			</div>
		</div>
	</div>

	<p>
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
		function submit() {
			window.location = 'export-complete-processing?dir=' + $('#filepicker .dir').val();
		}
		var roots = <%=json%>;
		new FilePicker($('#filepicker'), roots);
	</script>
	<p>
	<p>
	<jsp:include page="footer.jsp"/>
</body>
</html>
