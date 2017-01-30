<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="edu.stanford.muse.ner.Entity" %>

<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="edu.stanford.muse.ie.variants.Variants" %>
<%@ page contentType="text/html; charset=UTF-8"%>
<%@include file="getArchive.jspf" %>
<%
	AddressBook addressBook = archive.addressBook;
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

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>

<%writeProfileBlock(out, archive, "Edit address book", "");%>
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
		});
	});
</script>

<%
	Map<Short, String> desc = new LinkedHashMap<>();
	for(NEType.Type t: NEType.Type.values())
		desc.put(t.getCode(), t.getDisplayName());

	Short type = Short.parseShort(request.getParameter("type"));
	out.println("<h1>Type: "+desc.get(type)+"</h1>");
	Map<String, Entity> nameToEntity = new LinkedHashMap();
	double theta = 0.001;
	for(Document doc: archive.getAllDocs()){
		Span[] spans = archive.getEntitiesInDoc (doc, true);
		Set<String> seenInThisDoc = new LinkedHashSet<>();

		for(Span span: spans) {
			String name = span.getText();
			if(span.type!=type || span.typeScore<theta)
				continue;
			if (seenInThisDoc.contains (name.toLowerCase().trim()))
				continue;
			seenInThisDoc.add (name.toLowerCase().trim());

			if (!nameToEntity.containsKey(name))
				nameToEntity.put(name, new Entity(name, span.typeScore));
			else
				nameToEntity.get(name).freq++;
		}
	}

	Map<String, Integer> nameToFreq = new LinkedHashMap<>();
	for(Entity e: nameToEntity.values()) {
		nameToFreq.put(e.entity, e.freq);
		//System.err.println("Putting: "+e+", "+e.score);
	}

	Set<String> entityNames = nameToFreq.entrySet().stream().map (e -> e.getKey()).collect (Collectors.toSet());
	Variants.EntityMap em = new Variants.EntityMap();
	em.setupEntityMapping (entityNames);

	Set<Set<String>> clusters = em.getClusters();

	StringBuilder textBoxVal = new StringBuilder();
	for (Set<String> set: clusters) {
		textBoxVal.append("-- \n");
		for (String s : set) {
			textBoxVal.append (s);
			textBoxVal.append ("\n");
		}
	}

	/*
	List<Pair<String,Integer>> list = (alphaSort) ? Util.sortMapByKey(nameToFreq) : Util.sortMapByValue(nameToFreq);

	for (Pair<String, Integer> p: list) {
		String n = p.getFirst().trim();
		textBoxVal.append ("-- Times: " + p.getSecond() + "\n" + Util.escapeHTML(n) + "\n");
	}
	*/

%>

<p>
<div style="text-align:center">
    <!--http://stackoverflow.com/questions/254712/disable-spell-checking-on-html-textfields-->
<textarea name="entities" id="text" style="width:600px" rows="40" autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false"><%=textBoxVal%>
</textarea>
<br/>

<button class="btn btn-cta" type="submit">Save <i class="icon-arrowbutton"></i> </button>
</div>
<p/>
<br/>
<jsp:include page="footer.jsp"/>
</body>
</html>
