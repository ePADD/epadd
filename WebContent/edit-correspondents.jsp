<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page import="java.io.*"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%@page contentType="text/html; charset=UTF-8"%>
<%@include file="getArchive.jspf" %>
<%
	AddressBook addressBook = archive.addressBook;
	String sort = request.getParameter("sort");
	boolean alphaSort = ("alphabetical".equals(sort));
	boolean aliasCountSort = ("aliasCount".equals(sort));

%>
<html>
<head>
	<title>Edit Correspondents</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css?v=1.1">

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js?v=1.1"></script>

	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js?v=1.1"></script>
</head>
<body>
<%@include file="header.jspf"%>

<%writeProfileBlock(out, archive, "Edit address book");%>

<!--sidebar content-->
<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>

<nav class="menu1" role="navigation">
	<h2>Editing Correspondents</h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<!--phrase-->
	<div class="search-tips">
		This screen lists all names and email addresses associated with each correspondent. Under a given correspondent’s name, all the variants of their name are listed first, followed by all their email addresses.
		<br/><br/>

		The archive owner is always listed first, but you can sort the rest of the correspondents alphabetically or by volume.
		<br/><br/>

		The top listed name for each entry will be the name displayed in all ePADD search and browsing results, and visualizations. Change the displayed name by moving a name to the top of the list. You can also add a new name that does not appear in the archive.
		<br/><br/>

		Correspondents are separated with a single line containing "--" (2 hyphens).
		<br/><br/>

		Merge correspondents by grouping them together using find, cut, and paste commands.
		<br/><br/>

		Unmerge correspondents by separating them using find, cut, and paste commands. To add names and email addresses under a new correspondent that ePADD has not yet identified, add a new correspondent name and paste the names and email addresses under the new name.
		<br/><br/>

		ePADD enables you to view or ignore messages originating from a mailing list using Advanced Search criteria, which may be helpful for certain searches such as screening for sensitive information. You can designate messages as originating from a mailing list by typing -- ML after the correspondent name.
		<br/><br/>
	</div>
</nav>
<!--/sidebar-->

<br/>
<br/>
<div style="text-align:center;margin-left:40%;width:20%;">
	<div class="form-group">
		<select title="Sort order" id="sort-order" name="sort-order" class="form-control selectpicker">
			<option <%=!alphaSort && !aliasCountSort ? "selected" : ""%> value="volume">Sort by email volume</option>
			<option <%=alphaSort ? "selected" : ""%> value="alpha">Sort alphabetically</option>
			<option <%=aliasCountSort ? "selected" : ""%> value="aliasCount">Sort by number of aliases</option>
		</select>
	</div>

</div>

<script>
	$(document).ready(function() {
		$('#sort-order').change(function () {
			if ('alpha' === this.value)
				window.location = 'edit-correspondents?archiveID=<%=archiveID%>&sort=alphabetical';
            else if ('aliasCount' === this.value)
                window.location = 'edit-correspondents?archiveID=<%=archiveID%>&sort=aliasCount';
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
		StringWriter sw = new StringWriter();
		BufferedWriter bwriter = new BufferedWriter(sw);
		addressBook.writeObjectToStream (bwriter, alphaSort, aliasCountSort);
		bwriter.flush();
		out.print(sw.toString());
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
