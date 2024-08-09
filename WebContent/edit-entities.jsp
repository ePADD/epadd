<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="java.util.*"%>

<%@page import="edu.stanford.muse.ner.model.NEType" %>
<%@page import="java.util.stream.Collectors" %>
<%@page import="edu.stanford.muse.ie.variants.EntityBook" %>
<%@page import="edu.stanford.muse.util.Pair" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.io.BufferedWriter" %>
<%@page contentType="text/html; charset=UTF-8"%>
<%@include file="getArchive.jspf" %>
<%
	String sort = request.getParameter("sort");
	boolean alphaSort = ("alphabetical".equals(sort));
%>
<html>
<head>
	<title>Edit Entities</title>
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

<%	Map<Short, String> typeCodeToName = new LinkedHashMap<>();
	for(NEType.Type t: NEType.Type.values())
		typeCodeToName.put(t.getCode(), t.getDisplayName());

	Short type = Short.parseShort(request.getParameter("type"));
	//out.println("<h1>Type: "+ typeCodeToName.get(type)+"</h1>");

	writeProfileBlock(out, archive, "Edit entities    -    "+typeCodeToName.get(type));%>

<!--sidebar content-->
<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>

<nav class="menu1" role="navigation">
	<h2><b>Edit Entities</b></h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>
	<p>Merge entities by grouping them together using find, cut, and paste commands.

	<p>Unmerge entities by separating them using find, cut, and paste commands.

	<p>The top listed entity name in a set will be the name displayed in all ePADD interfaces, including search and browsing results and visualizations. You can use an entity name already listed, or supply a new entity name that does not appear in the email archive.

	<p>Assign entities to a different category by cutting and pasting them into a different entity category’s <b>Edit Entities</b> panel.
</nav>
<!--/sidebar-->

<div style="text-align:center;display:inline-block;vertical-align:top;margin-left:40%; width: 20%; margin-bottom: 20px;">
	<select title="Sort order" id="sort-order" class="form-control selectpicker">
		<option <%=!alphaSort?"selected":""%> value="volume">Sort by frequency</option>
		<option <%=alphaSort?"selected":""%> value="alpha">Sort alphabetically</option>
	</select>
</div>

<script>

	$(document).ready(function() {
		$('#sort-order').change(function () {
			var url = 'edit-entities?archiveID=<%=archiveID%>&type=<%=request.getParameter ("type")%>';
			if ('alpha' == this.value)
				window.location = url += '&sort=alphabetical';
			else
				window.location = url;
		});
	});
</script>

<%--<%



	// start building the string that goes into the text box
	StringBuilder textBoxVal = new StringBuilder();
	{
		for (String entityDisplayName : entityDisplayNames) {
			// for this entity, enter the separator first, then the display name, then alt names if any
			textBoxVal.append("-- \n");
			textBoxVal.append(Util.escapeHTML(entityDisplayName) + "\n");

			// get alt names if any
			Set<String> altNames = entityBook.getAltNamesForDisplayName(entityDisplayName, type);
			if (altNames != null)
				for (String altName : altNames) {
				//enter altName only if different from the entityDisplayName.
					if(!entityDisplayName.equals(altName))
						textBoxVal.append(Util.escapeHTML(altName) + "\n");

				}
		}
	}
%>--%>

<p>

<div style="text-align:center">
    <!--http://stackoverflow.com/questions/254712/disable-spell-checking-on-html-textfields-->
	<form method="post" action="browse-top">
		<input name="entityType" type="hidden" value="<%=type%>"/>
		<%--//adding a hidden input field to pass archiveID to the server. This is a common pattern used to pass
		//archiveID in all those forms where POST was used to invoke the server page.--%>
		<input type="hidden" value="<%=archiveID%>" name="archiveID"/>
		<textarea title="Entities" name="entityMerges" id="text" style="width:600px" rows="40" autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false">
		<%
			EntityBook entityBook = archive.getEntityBookManager().getEntityBookForType(type);
			StringWriter sw = new StringWriter();
			BufferedWriter bwriter = new BufferedWriter(sw);
			entityBook.writeObjectToStream (bwriter, alphaSort);
			bwriter.flush();
			out.print(sw.toString());
		%>
		</textarea>
		<br/>
		<br/>
		<button class="btn btn-cta" type="submit">Save <i class="icon-arrowbutton"></i> </button>
	</form>
<br/>

</div>
<p/>
<br/>
<jsp:include page="footer.jsp"/>
</body>
</html>
