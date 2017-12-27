<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page language="java" import="java.io.*"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@ page contentType="text/html; charset=UTF-8"%>
<%@include file="getArchive.jspf" %>
<%
	AddressBook addressBook = archive.addressBook;
	String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	String sort = request.getParameter("sort");
	boolean alphaSort = ("alphabetical".equals(sort));
%>
<html>
<head>
	<title>Edit Correspondents</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
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
<jsp:include page="header.jspf"/>

<%writeProfileBlock(out, archive, "Edit address book", "");%>

<!--sidebar content-->
<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>

<nav class="menu1" role="navigation">
	<h2>Edit Correspondents</h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<!--phrase-->
	<div class="search-tips">
		<p>
			This screen allows the user to view and edit the email address(es) and name(s) associated with each correspondent
		<p>

			The archive owner should always be the first individual listed.
		<p>
			The first name listed within each group of names or addresses is the name ePADD will display in all search and browsing results and visualizations. Manually change this display name by adding a new name at the top of this list

		<p>
			If names or addresses are incorrectly associated with the wrong individual, you can manually cut and paste them underneath a different entry

		<p>
			Mailing list addresses can be manually identified and added under the heading “-- [ML]” If this heading does not exist simply create it.

		</p>
	</div>
</nav>
<!--/sidebar-->

<br/>
<br/>
<div style="text-align:center;margin-left:40%;width:20%;">
	<div class="form-group">
		<label for="sort-order">Sort order</label>
		<select id="sort-order" name="sort-order" class="form-control selectpicker">
			<option <%=!alphaSort ? "selected" : ""%> value="volume">Sort by email volume</option>
			<option <%=alphaSort ? "selected" : ""%> value="alpha">Sort alphabetically</option>
		</select>
	</div>

</div>

<script>
	$(document).ready(function() {
		$('#sort-order').change(function () {
			if ('alpha' === this.value)
				window.location = 'edit-correspondents?archiveID=<%=archiveID%>&sort=alphabetical';
			else
				window.location = 'edit-correspondents?archiveID=<%=archiveID%>';
		});
	});
</script>

<p>
<div style="text-align:center">
<form method="post" action="browse-top">
	<!-- adding a hidden input field to pass archiveID to the server. This is a common pattern used to pass
	//archiveID in all those forms where POST was used to invoke the server page. -->
	<input type="hidden" value="<%=archiveID%>" class="form-control" name="archiveID"/>

    <!--http://stackoverflow.com/questions/254712/disable-spell-checking-on-html-textfields-->
<textarea title="Address book update" name="addressBookUpdate" id="text" style="width:600px" rows="40" autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false">
	<%
	BufferedWriter bwriter = new BufferedWriter(new StringWriter());
	addressBook.writeObjectToStream(bwriter,alphaSort);
	out.print(bwriter.toString());
	%>
</textarea>
<br/>

<button class="btn btn-cta" type="submit">Save <i class="icon-arrowbutton"></i> </button>
</form>
</div>
<br/>
<jsp:include page="footer.jsp"/>
</body>
</html>
