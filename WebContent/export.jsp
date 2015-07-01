<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="com.google.gson.Gson"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="java.util.ArrayList"%>
<%@page language="java" import="java.util.List"%>
<%@page language="java" %>
<%@page language="java" %>
<%@include file="getArchive.jspf" %>

<html>
<head>
	<title>Export</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
	<script src="jqueryFileTree/jqueryFileTree.js"></script>
		
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>
	<script src="js/filepicker.js"></script>
</head>

<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Export');</script>

<%
	AddressBook addressBook = archive.addressBook;
	String bestName = addressBook.getBestNameForSelf();
	writeProfileBlock(out, bestName, "", "Select folder for export");
%>

<script type="text/javascript">
</script>
<p>
<br/>
<br/>

<% /* all mboxes folders will be in account divs here */ %>
<div id="filepicker">
	<label class="col-sm-1 control-label">Export Folder</label> 
	<div class="col-sm-5"><input class="dir form-control" type="text" name="mboxDir1"/></div>
	<div class="col-sm-2"><button class="btn btn-default"><i class="fa fa-file"></i> <span>Browse</span></button></div>
	<br/>
	<br/>
	<br/>
	<br/>
	<div class="roots" style="display:none"></div>
	<div class="browseFolder"></div>
</div> <!--  end account -->
    <button class="btn btn-cta" style="margin-left:110px" onclick="submit(); return false;">Export <i class="icon-arrowbutton"></i> </button> 
<%
java.io.File[] rootFiles = java.io.File.listRoots(); 
List<String> roots = new ArrayList<String>();
for (java.io.File f: rootFiles)
	roots.add(f.toString());
String json = new Gson().toJson(roots);
%>
<script> 
	function submit() {
		window.location = 'export-complete?dir=' + $('#filepicker .dir').val();
	}
	var roots = <%=json%>;
	new FilePicker($('#filepicker'), roots);
</script>
<p>
<p>

</body>
</html>
