<%@page contentType="text/html; charset=UTF-8"%>
<%
	JSPHelper.checkContainer(request); // do this early on so we are set up
  request.setCharacterEncoding("UTF-8");
%>
<%@ page import="org.json.*"%>
<%@ page import="java.util.*"%>
<%@ page import="edu.stanford.muse.index.*" %>
<%@ page import="edu.stanford.muse.email.CalendarUtil" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.email.AddressBook" %>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="edu.stanford.muse.util.*" %>
<%@ page import="edu.stanford.muse.email.Contact" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@include file="getArchive.jspf" %>
<%!

public String scriptForFacetsGraph(List<DetailedFacetItem> dfis, List<Date> intervals, Collection<EmailDocument> docs, int[] allMessagesHistogram, int w, int h)
{
	Collections.sort(dfis);
	JSONArray j = new JSONArray();
	int count = 0, MAX_COUNT = 20; // we can only show top 20 layers
	for (DetailedFacetItem dfi: dfis)
	{
		String folder = dfi.name;
		int[] hist = CalendarUtil.computeHistogram(EmailUtils.datesForDocs((Collection) dfi.docs), intervals, true /* ignore invalid dates */);
		try {
			JSONArray groupVolume = JSONUtils.arrayToJsonArray(hist);
			JSONObject o = new JSONObject();
			o.put("caption", dfi.name);
			o.put("full_caption", dfi.description);
			o.put("url", dfi.messagesURL);
			o.put("histogram", groupVolume);
			j.put(count, o);
		} catch (Exception e) { Util.print_exception(e, JSPHelper.log); }
		count++;
		if (count >= MAX_COUNT) 
			break;
	}
	String json = j.toString();
	
	String totalMessageVolume = JSONUtils.arrayToJson(allMessagesHistogram);
	Calendar c = new GregorianCalendar(); c.setTime(intervals.get(0)); 
	int start_mm = c.get(Calendar.MONTH), start_yy = c.get(Calendar.YEAR);
	
	return "<script type=\"text/javascript\">\n"
	+ "var jsonForData = " + json + ";\n"
	//+ "draw_stacked_graph(jsonForData, " + totalMessageVolume + ", " + 0 + ", " + w + ", " + h + ", " + start_yy + ", " + start_mm + ", pv.Colors.category20(), get_click_handler_people_or_folders(jsonForData), graphType);\n"
//	+ "draw_stacked_graph_d3('#div_graph', '#div_legend', jsonForData, " + totalMessageVolume + ", " + w + ", " + h + ", " + start_yy + ", " + start_mm + ", d3.scale.category20b(), get_click_handler_people_or_folders(jsonForData), graphType);\n"
	+ "draw_stacked_graph_d3('#div_graph', '#div_legend', jsonForData, " + totalMessageVolume + ", " + w + ", " + h + ", " + start_yy + ", " + start_mm + ", d3.scale.category20b(), null, graphType);\n"
	+ "</script>\n";
}

public String scriptForSentimentsGraph(Map<String, Collection<Document>> map, List<Date> intervals, int[] allMessagesHistogram, int w, int h, int normalizer, HttpSession session)
{
	String totalMessageVolume = JSONUtils.arrayToJson(allMessagesHistogram);

	// normalizer is the max # of documents in a single intervals

	List<Set<DatedDocument>> sentimentDocs = new ArrayList<Set<DatedDocument>>();
	StringBuilder json = new StringBuilder("[");
	for (String caption: map.keySet())
	{
		Set<DatedDocument> docs = new LinkedHashSet<DatedDocument>((Collection) map.get(caption)); // map.get returns a list, we have to cast it to set
		int[] hist = CalendarUtil.computeHistogram(EmailUtils.datesForDocs(docs), intervals, true /* ignore invalid dates */);
		String sentimentVolume = JSONUtils.arrayToJson(hist);
		if (json.length() > 2)
			json.append (",");
		json.append ("{caption: \"" + caption + "\", url: \"sentiment=" + caption + "\", histogram: " + sentimentVolume + "}");
	}
	json.append("]");
	Calendar c = new GregorianCalendar(); c.setTime(intervals.get(0)); int start_mm = c.get(Calendar.MONTH); int start_yy = c.get(Calendar.YEAR);
	return "<script type=\"text/javascript\">\n"
	+ "var jsonForData = " + json + ";\n"
	//+ "draw_stacked_graph(jsonForData, " + totalMessageVolume + ", " + 0 + ", " + w + ", " + h + ", " + start_yy + ", " + start_mm + ", pv.Colors.category19(), get_click_handler_sentiments(jsonForData), graphType);\n"
//	+ "draw_stacked_graph_d3('#div_graph', '#div_legend', jsonForData, " + totalMessageVolume + ", " + w + ", " + h + ", " + start_yy + ", " + start_mm + ", d3.scale.category20b(), get_click_handler_sentiments(jsonForData), graphType);\n"
	+ "draw_stacked_graph_d3('#div_graph', '#div_legend', jsonForData, " + totalMessageVolume + ", " + w + ", " + h + ", " + start_yy + ", " + start_mm + ", d3.scale.category20b(), null, graphType);\n"
	+ "</script>\n";
}

%>
<%

	if (archive == null)
	{
		if (!session.isNew())
			session.invalidate();
	%>
	    <script type="text/javascript">window.location="index.jsp";</script>
	<%
		System.err.println ("Error: session has timed out, archive is null.");
		return;
	}
	
	Collection<DatedDocument> allDocs = (Collection) JSPHelper.selectDocs(request, session, true /* only apply to filtered docs */, false);

	AddressBook addressBook = archive.addressBook;
	Lexicon lex = (Lexicon) JSPHelper.getSessionAttribute(session, "lexicon");
	String name = request.getParameter("lexicon");

	Pair<Date, Date> p = EmailUtils.getFirstLast(allDocs, true /* ignore invalid dates */);
	Date globalStart = p.getFirst();
	Date globalEnd = p.getSecond();
	List<Date> intervals = null;
	int nIntervals = 0;
	if (globalStart != null && globalEnd != null) {
		intervals = CalendarUtil.divideIntoMonthlyIntervals(globalStart, globalEnd);
		nIntervals = intervals.size() - 1;
	}
	boolean doSentiments = false, doPeople = false, doEntities = false;
	String view = request.getParameter("view");
	String type = request.getParameter("type");
	String heading = "", tableURL = "";
	if ("sentiments".equals(view)) {
		doSentiments = true;
		JSPHelper.log.info("req lex name = " + name + " session lex name = " + ((lex == null) ? "(lex is null)" : lex.name));
		// resolve lexicon based on name in request and existing lex in session.
		// name overrides lex
		if (!Util.nullOrEmpty(name)) {
			if (lex == null || !lex.name.equals(name)) {
				lex = archive.getLexicon(name);
				session.setAttribute("lexicon", lex);
			}
			// else do nothing, the right lex is already loaded
		} else {
			if (lex == null) {
				// nothing in session, no request param... probably shouldn't happen
				name = "default";
				lex = archive.getLexicon(name);
				session.setAttribute("lexicon", lex);
			} else
				name = lex.name;
		}
		heading = "Lexicon Graph";
		tableURL = "lexicon";
	} else if ("people".equals(view)) {
		heading = "Top correspondents graph";
		doPeople = true;
		tableURL = "correspondents";
	} else if ("entities".equals(view)) {
		doEntities = true;
		heading = "Top entities graph (type: " + Util.capitalizeFirstLetter(type) + ")";
		tableURL = "entities?type=" + type;
	}
	%>
<html>
<head>
	<title>Graph</title>	
	<script type="text/javascript" src="js/jquery.js"></script>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png"/>

	<script src="js/jquery.js"></script>
	
	<link href="css/d3_funcs.css" rel="stylesheet" type="text/css"/>
	<script type="text/javascript" src="js/d3.v2.min.js"></script>
	<script type="text/javascript" src="js/d3_funcs.js"></script>
	
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css"/>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js"></script>

	<style>
	.brush .extent {
	  stroke: black;
	  stroke-width: 2;
	  fill-opacity: .5;
	}
	</style>
</head>

<body style="margin:0% 2%"> <!--  override the default of 1% 5% because we need more width on this page -->
<jsp:include page="header.jspf"/>

<%
	String sentiment = request.getParameter("sentiment");
	if (sentiment == null)
		sentiment = "";
	String graphType = request.getParameter("graphType");
	if (graphType == null)
		graphType = "curvy";
	if (allDocs.size() < 500)
		graphType = "boxy"; // force boxy if < 100, because curvy doesn't look good
%>

<script type="text/javascript">
	var graphType = '<%=graphType%>';
</script>

<p>
<%
	String bestName = addressBook.getBestNameForSelf();
	writeProfileBlock(out, bestName, "", heading);
%>

<div style="text-align:center;display:inline-block;vertical-align:top;margin-left:170px">
	<button class="btn-default" onclick="window.location='<%=tableURL%>'"><i class="fa fa-table"></i> Go to Table View</button>
</div>

<div align="center">	
<div style="font-size:14px">
<br/>
<hr style="margin-top:-6px">

<%
 if (doSentiments) {
		Collection<String> lexiconNames = archive.getAvailableLexicons();
		 if (ModeConfig.isDeliveryMode()) {
			 lexiconNames = new LinkedHashSet(lexiconNames); // we can't call remve on the collection directly, it throws an unsupported op.
			 lexiconNames.remove("sensitive");
		 }

	 boolean onlyOneLexicon = (lexiconNames.size() == 1);
			if (lexiconNames.size() > 1)
			{
	%>
		<script>function changeLexicon() {	window.location = 'graph?view=sentiments&lexicon=' +	$('#lexiconName').val(); }</script>
		Lexicon <select id="lexiconName" onchange="changeLexicon()">
		<%
			// common case, only one lexicon, don't show load lexicon
			for (String n: lexiconNames)
			{
		%> <option <%=name.equalsIgnoreCase(n) ? "selected":""%> value="<%=n.toLowerCase()%>"><%=Util.capitalizeFirstLetter(n)%></option>
		<%
			}
		%>
		</select>
	<%
			}
	%>
<%
	}
	%>

</div>
</div>

<!-- make div "stackedgraph-container" fixed aspect ratio - see http://stackoverflow.com/questions/9219005/proportionally-scale-a-div-with-css-based-on-max-width-similar-to-img-scaling -->
<div class="stackedgraph-container" align="center" style="position:relative;min-width:640px;width:95%;padding-top:32%;padding-bottom:5%;height:0;margin:0 auto">
<div style="position:absolute;top:0;bottom:0;width:100%">

<div id="div_graph" style="width:80%;float:left"></div>
<div id="div_legend" style="width:19%;float:left;margin-left:1%;overflow:auto"></div>
<div style="clear:both"></div>

<%
	String graph_script = "";
	boolean trackNOTA = request.getParameter("nota") != null;

	boolean graph_is_empty = false;
	String empty_graph_message = null;

	if (doSentiments)
	{
		int normalizer = ProtovisUtil.normalizingMax(allDocs, addressBook, intervals);
		int[] allMessagesHistogram = CalendarUtil.computeHistogram(EmailUtils.datesForDocs(allDocs), intervals, true /* ignore invalid dates */);
		graph_is_empty = true;
		if (!Util.nullOrEmpty(allDocs))
		{
			Map<String, Collection<Document>> map = (Map) lex.getEmotions(archive.indexer, (Collection) allDocs, trackNOTA, request.getParameter("originalContentOnly") != null); // too heavyweight -- we just want to find if the damn graph is empty...
			for (String key: map.keySet())
			{
				Collection<Document> set = map.get(key);
				if (set != null && set.size() > 0)
				{
					graph_is_empty = false;
					break;
				}
			}
			graph_script = scriptForSentimentsGraph(map, intervals, allMessagesHistogram, 1000, 450, normalizer, session);
		}
	
		if (graph_is_empty)
			empty_graph_message = "No lexicon hits in these " + allDocs.size() + " messages";
	}
	else if (doPeople)
	{
		int normalizer = ProtovisUtil.normalizingMax(allDocs, addressBook, intervals);
		int[] allMessagesHistogram = CalendarUtil.computeHistogram(EmailUtils.datesForDocs(allDocs), intervals, true /* ignore invalid dates */);
		Map<Contact, DetailedFacetItem> folders = IndexUtils.partitionDocsByPerson((Collection) allDocs, addressBook);
		List<DetailedFacetItem> list = new ArrayList<DetailedFacetItem>(folders.values());
		graph_script = scriptForFacetsGraph(list, intervals, (Collection) allDocs, allMessagesHistogram, 1000, 450);
	}
	else if (doEntities)
	{
		int normalizer = ProtovisUtil.normalizingMax(allDocs, addressBook, intervals);
		int[] allMessagesHistogram = CalendarUtil.computeHistogram(EmailUtils.datesForDocs(allDocs), intervals, true /* ignore invalid dates */);

		Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
		Map<String, String> canonicalToOriginal = new LinkedHashMap<String, String>();
		Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
		for (EmailDocument ed: docs) {
			List<String> entities = archive.getEntitiesInDoc(ed, type);
			Set<String> set = new LinkedHashSet<String>(entities);

			for (String e: set) {
				String canonicalEntity = IndexUtils.canonicalizeEntity(e);
				if (canonicalToOriginal.get(canonicalEntity) == null)
					canonicalToOriginal.put(canonicalEntity, e);
				Integer I = counts.get(canonicalEntity);
				counts.put(canonicalEntity, (I == null) ? 1 : I+1);
			}
		}
		
		List<Pair<String, Integer>> pairs = Util.sortMapByValue(counts);
		int n = HTMLUtils.getIntParam(request, "n", 10);
		int count = 0;
		List<String> topEntities = new ArrayList<String>();
		for (Pair<String, Integer> pair: pairs) {
			topEntities.add(pair.getFirst());
			if (++count > n)
				break;
		}
		
		Map<String, Set<Document>> map = new LinkedHashMap<String, Set<Document>>();
		/*
		for (String e: topEntities)
		{
			String displayForEntity = canonicalToOriginal.get(e);
			if (Util.nullOrEmpty(displayForEntity))
				continue;
			Set<Document> set = archive.indexer.docsForQuery(e, -1, Indexer.QueryType.FULL);
			/* note: #docs here could mismatch with counts because a name might hit a doc, but not be recognized as a name in it */
			/*
			if (Util.nullOrEmpty(set))
				continue;
				
			map.put(e, set);
		}
		*/

		// now create the actual list of docs for each of the top entities
		for (EmailDocument ed: docs) {
			List<String> entities = archive.getEntitiesInDoc(ed, type);
			Set<String> set = new LinkedHashSet<String>(entities);

			for (String e: set) {
				String canonicalEntity = IndexUtils.canonicalizeEntity(e);
				if (!topEntities.contains(canonicalEntity))
					continue;
				
				Set<Document> docSet = map.get(canonicalEntity);
				if (docSet == null)
				{
					docSet = new LinkedHashSet<Document>();
					map.put(canonicalEntity, docSet);
				}
				docSet.add(ed);
			}
		}

		// now uncanonicalize the top terms in the map to form newmap
		Map<String, Collection<Document>> newMap = new LinkedHashMap<String, Collection<Document>>();
		for (String entity: topEntities)
		{
			String originalEntity = canonicalToOriginal.get(entity);
			if (Util.nullOrEmpty(originalEntity)) // shouldn't really happeb
				originalEntity = entity;
			newMap.put(originalEntity, map.get(entity));
		}
		
		graph_script = scriptForSentimentsGraph(newMap, intervals, allMessagesHistogram, 1000, 450, normalizer, session);
	}

	if (graph_is_empty)
		out.println ("<br/><br/>" + empty_graph_message);
	else {
		out.println (graph_script);
%>

</div>
</div>
<% } %>
<jsp:include page="footer.jsp"/>
</body>
</html>
