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

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
	<jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="jqueryFileTree/jqueryFileTree.js"></script>
	<script src="js/filepicker.js"></script>
	<script src="js/epadd.js"></script>
</head>

<body>
	<jsp:include page="header.jspf"/>
	<jsp:include page="div_filepicker.jspf"/>
	<script>epadd.nav_mark_active('Export');</script>
	<%
		String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
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
				<button class="btn-default browse-button"><i class="fa fa-file"></i>
					<span>Browse</span>
				</button>
			</div>
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
	<script>
		function submit() {
			window.location = 'export-complete-processing?archiveID=<%=archiveID%>&dir=' + $('#filepicker .dir').val();
		}
		new FilePicker($('#filepicker'));
	</script>
	<p>
	<p>
	<jsp:include page="footer.jsp"/>
</body>
</html>
