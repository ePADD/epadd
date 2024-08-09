<%@page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.xword.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.ie.ie.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@ page import="edu.stanford.muse.ie.TypeHierarchy" %>
<%@ page import="edu.stanford.muse.ie.NameInfo" %>
<%@ page import="edu.stanford.muse.ie.NameTypes" %>
<%@include file="../getArchive.jspf" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
<jsp:include page="../css/css.jsp"/>
<head>
<script src="../js/jquery.js"></script>
<script src="typeutils.js?v=1.1"></script>
<script src="js/muse.js"></script>
<link rel="icon" type="image/png" href="images/muse-favicon.png">

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Name Types</title>
</head>
<body>
<br/>
<p>

<%
	out.println("<div id='sumbit_area'><button onclick=\"submit()\">Evaluate</button></div>");
	Map<String, NameInfo> nameMap = NameTypes.computeNameMap(archive, (Collection<EmailDocument>) session.getAttribute("emailDocs"));
	Map<String, Collection<NameInfo>> typedHits = NameTypes.assignTypes(nameMap);
	Lexicon lex = (Lexicon) session.getAttribute("lexicon");
	if (lex == null)
	{
		lex = archive.getLexicon("general");
		session.setAttribute("lexicon", lex);	
	}
	NameTypes.computeInfo(nameMap, (Collection<EmailDocument>) session.getAttribute("emailDocs"), archive, lex);
	archive.setNameMap(nameMap);
	if (request.getParameter("save") != null)
		SimpleSessions.saveArchive(session);
	
	ArchiveCluer cluer = new ArchiveCluer(null, archive, null, lex);

TypeHierarchy th = new TypeHierarchy();

// first remove the notypes
Collection<NameInfo> untyped = typedHits.remove("notype");

int typeCount = 0;
int typeHitsCount = 0;
for (String type: typedHits.keySet())
	typeHitsCount += typedHits.get(type).size();

out.println ("<b>" + typeHitsCount + " entities in " + typedHits.size() + " type(s) </b><br/>");
for (String type: typedHits.keySet())
{
	th.recordTypeCount(type, typedHits.get(type).size());
}

out.println(th.toString(true));

for (String type: typedHits.keySet())
{
	if (type.equals("notype"))
		continue;
	typeCount++;	
	
	List<NameInfo> list1 = (List) typedHits.get(type);
	Collections.sort(list1);
	out.println ("<br/>");
	out.println ("<a name=\"" + type + "\">");
	out.println ("<b> Type #" + typeCount + ". " + type + " (" + Util.pluralize(list1.size(), "hit") + ")</b><br/>");
	String tc = type.replaceAll("\\|", "");
	out.println("<div class=\""+tc+"\">");
	for (NameInfo I: list1) 
	{
		//Clue clue = cluer.createClue(I.title, null);
		out.println (I.toHTML(true) + "<br/>");
	}
	out.println("</div>");
}
%>
<hr/>

<%
if (!Util.nullOrEmpty(untyped)) {
    out.println ("No type for " + Util.pluralize(untyped.size(), "name"));
    %>
    <a name="notypes"></a>
    <span onclick="$('#notypes').toggle(); toggleMosreAndLess(this);">More</span><br/>
    <div id="notypes" style="display:none;">
	    <% 
	    for (NameInfo I: untyped)
	    {
	    	String lookup = I.originalTerm;
	    	if (lookup == null)
	    		lookup = I.title;
// 			Clue clue = cluer.createClue(lookup, null);
// 	    	out.println (I.toHTML(false) + " " + clue + "<br/>");
	    }
	    %>
    </div>
    <%
}
%>

</body>
</html>