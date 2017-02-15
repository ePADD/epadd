<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="edu.stanford.muse.ner.Entity" %>

<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="edu.stanford.muse.ie.variants.EntityMapper" %>
<%@ page import="java.util.stream.Stream" %>
<%@ page contentType="text/html; charset=UTF-8"%>
<%@include file="getArchive.jspf" %>
<%
	Collection<EmailDocument> allDocs = (Collection<EmailDocument>) JSPHelper.getSessionAttribute(session, "emailDocs");
	if (allDocs == null)
		allDocs = (Collection) archive.getAllDocs();
	String sort = request.getParameter("sort");
	boolean alphaSort = ("alphabetical".equals(sort));
%>
<html>
<head>
	<title>Edit Entities</title>
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
	<h2>Edit Entities</h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<!--phrase-->
	<div class="search-tips">
		<img src="images/pharse.png" alt="">
		<p>
			Text from Josh here.
		</p>
	</div>

	<!--requered-->
	<div class="search-tips">
		<img src="images/requered.png" alt="">
		<p>
			More text
		</p>
	</div>


</nav>
<!--/sidebar-->

<div style="text-align:center;display:inline-block;vertical-align:top;margin-left:170px;margin-top:20px;">
	<select id="sort-order">
		<option <%=!alphaSort?"selected":""%> value="volume">Sort by frequency</option>
		<option <%=alphaSort?"selected":""%> value="alpha">Sort alphabetically</option>
	</select>
</div>

<script>
	$(document).ready(function() {
		$('#sort-order').change(function (e) {
			var url = 'edit-entities?type=<%=request.getParameter ("type")%>';
			if ('alpha' == this.value)
				window.location = url += '&sort=alphabetical';
			else
				window.location = url;
		});
	});
</script>

<%
	Map<Short, String> typeCodeToName = new LinkedHashMap<>();
	for(NEType.Type t: NEType.Type.values())
		typeCodeToName.put(t.getCode(), t.getDisplayName());

	Short type = Short.parseShort(request.getParameter("type"));
	out.println("<h1>Type: "+ typeCodeToName.get(type)+"</h1>");

	EntityMapper entityMapper = archive.getEntityMapper();
	Map<String, Integer> displayNameToFreq = entityMapper.getDisplayNameToFreq(archive, type);

	// get pairs of <displayname, freq> in alpha order of display name, or the order of Freq
	List<String> entityDisplayNames;
	if (alphaSort) {
		entityDisplayNames = new ArrayList<> (displayNameToFreq.keySet());
		Collections.sort (entityDisplayNames);
	} else {
		entityDisplayNames = Util.sortMapByValue(displayNameToFreq).stream().map(p -> p.getFirst()).collect (Collectors.toList());
	}

	// start building the string that goes into the text box
	StringBuilder textBoxVal = new StringBuilder();
	{
		for (String entityDisplayName : entityDisplayNames) {
			// for this entity, enter the separator first, then the display name, then alt names if any
			textBoxVal.append("-- \n");
			textBoxVal.append(Util.escapeHTML(entityDisplayName) + "\n");

			// get alt names if any
			Set<String> altNames = entityMapper.getAltNamesForDisplayName(entityDisplayName, type);
			if (altNames != null)
				for (String altName : altNames)
					textBoxVal.append(Util.escapeHTML(altName) + "\n");
		}
	}
%>

<p>

<div style="text-align:center">
    <!--http://stackoverflow.com/questions/254712/disable-spell-checking-on-html-textfields-->
	<form method="post" action="browse-top">
		<input name="entityType" type="hidden" value="<%=type%>"/>

		<textarea name="entityMerges" id="text" style="width:600px" rows="40" autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false"><%=textBoxVal%>
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
