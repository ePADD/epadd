<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%
	JSPHelper.checkContainer(request); // do this early on so we are set up
  request.setCharacterEncoding("UTF-8");
%>
<%@page language="java" import="edu.stanford.muse.ie.InternalAuthorityAssigner"%>
<%@page language="java" import="edu.stanford.muse.util.Pair"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.datacache.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.util.Pair"%>
<%@page language="java" import="edu.stanford.muse.ie.InternalAuthorityAssigner"%>

<%@include file="getArchive.jspf" %>

<%

String title = request.getParameter("title");

// good to give a meaningful title to the browser tab since a lot of them may be open
String term = request.getParameter("term");
term = JSPHelper.convertRequestParamToUTF8(term);
String sentiments[] = request.getParameterValues("sentiment");
String[] persons = request.getParameterValues("person");
String[] attachments = request.getParameterValues("attachment");
int month = HTMLUtils.getIntParam(request, "month", -1);
int year = HTMLUtils.getIntParam(request, "year", -1);
int cluster = HTMLUtils.getIntParam(request, "timeCluster", -1);

String sentimentSummary = "";
if (sentiments != null && sentiments.length > 0)
	for (int i = 0; i < sentiments.length; i++)
	{
		sentimentSummary += sentiments[i];	
		if (i < sentiments.length-1)
	sentimentSummary += " & ";
	}
		
if (Util.nullOrEmpty(title))
{
	if (term != null)
		title = "Search: " + term;
	else if (cluster != -1)
		title = "Cluster " + cluster;
	else if (sentimentSummary != null)
		title = sentimentSummary;
	else if (attachments != null && attachments.length > 0)
		title = attachments[0];
	else if (month >= 0 && year >= 0)
		title = month + "/" + year;
	else if (year >= 0)
		title = Integer.toString(year);
	else if (persons != null && persons.length > 0)
	{
		title = persons[0];
		if (persons.length > 1)
	title += "+" + (persons.length-1);
	}
	else
		title = "Browse";
}
title = Util.escapeHTML(title);

boolean noFacets = request.getParameter("noFacets") != null;// || ModeConfig.isPublicMode();

if (ModeConfig.isPublicMode()) {
	// this browse page is also used by Public mode where the following set up may be requried. 
	String archiveId = request.getParameter("aId");
	Sessions.loadSharedArchiveAndPrepareSession(session, archiveId);
}
%>
<!DOCTYPE HTML>
<html lang="en">
<head>
	<META http-equiv="Content-Type" content="text/html; charset=UTF-8">

	<title><%=title%></title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js" type="text/javascript"></script> 

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/stacktrace.js" type="text/javascript"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js" type="text/javascript"></script>
	<script src="js/epadd.js"></script>
	
	<!-- For tool-tips -->
	<script type='text/javascript' src='js/jquery.qtip-1.0.js'></script>
	<script type='text/javascript' src='js/utils.js'></script>
<!-- <script src="js/jQueryRotateCompressed.2.1.js" type="text/javascript"></script> -->
<!-- <script src="js/protovis.js" type="text/javascript"></script> -->
<!-- <script src="js/proto_funcs.js" type="text/javascript"></script>  -->

	<style> div.facets hr { width: 90%; } </style>

</head>
<body > <!--  override margin because this page is framed. -->
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>
	<!--  required to include joh_plugin for some compilation issues -->
	<!--  important: include jog_plugin AFTER header.jsp, otherwise the extension is applied to a jquery ($) object that is overwritten when header.jsp is included! -->
	<script src="js/jog_plugin.js" type="text/javascript"></script>
<%
	String bestName = archive.addressBook.getBestNameForSelf();
//	writeProfileBlock(out, bestName, "Search", "");
	// archive is null if we haven't been able to read an existing dataset
	
	AddressBook addressBook = archive.addressBook;
	// nofilter is used in some cases like browse with a specific docids where we don't want to apply to docs in current filter, but to all docs in archive
	 
	JSPHelper.log.info("Initialising InternalAuthorityAssigner");
	InternalAuthorityAssigner assignauthorities = (InternalAuthorityAssigner)request.getSession().getAttribute("authorities");
	if (assignauthorities==null){
		assignauthorities = InternalAuthorityAssigner.load(archive);
		request.getSession().setAttribute("authorities", assignauthorities);
	}
	 
	 Pair<Set<Document>,Set<Blob>> search_result = JSPHelper.selectDocsWithHighlightAttachments(request, session, false /* onlyFilteredDocs */, false);
	 List<Document> docs = new ArrayList<Document>(search_result.first);
	 Collections.sort(docs);//order by time
	 Set<Blob> highlightAttachments = search_result.second;
	 Lexicon lexicon = (Lexicon) JSPHelper.getSessionAttribute(session, "lexicon");
	 if (lexicon == null)
	 {
		lexicon = archive.getLexicon("default");
		session.setAttribute("lexicon", lexicon);	
	 }
	 
	 if (Util.nullOrEmpty(docs)) { %>
		 <div style="margin-top:2em;font-size:200%;text-align:center;">No matching messages.</div>
	 <%} else { %>
 <div class="browsepage" style="min-width:1220px">

 <%
	 Map<String, Collection<DetailedFacetItem>> facets = IndexUtils.computeDetailedFacets(docs, archive, lexicon);
	 //returns sorted collection
	 //Collection<DetailedFacetItem> locSubjects = TopicsSearcher.getMentions(docs,archive);
	 //if(locSubjects!=null)
		//facets.put("mentions", locSubjects);

	 boolean jogDisabled = true;

	 // now docs is the selected docs

	 String jog_contents_class = "";
	 jog_contents_class = "message";
		String origQueryString = request.getQueryString();
		if (origQueryString == null)
			origQueryString = "";

	 String datasetName = String.format("docset-%08x", EmailUtils.rng.nextInt());// "dataset-1";
	 int nAttachments = EmailUtils.countAttachmentsInDocs((Collection) docs);
 %>

<div style="display:inline-block;vertical-align:top;width:160px;padding-left:5px">
<div class="facets" style="min-width:10em;text-align:left;margin-bottom:0px;">
<%
	if (!Util.nullOrEmpty(term)) { 
		out.println("<b>Search</b><br/>\n");
		String displayTerm = Util.ellipsize(term, 15);

		out.println("<span title=\"" + Util.escapeHTML(term) + "\" class=\"facet nojog selected-facet rounded\" style=\"padding-left:2px;padding-right:2px\">" + Util.escapeHTML(displayTerm));
		out.println (" <span class=\"facet-count\">(" + docs.size() + ")</span>");
		out.println ("</span><br/>\n");
		out.println("<br/>\n");
	}

	for (String facet: facets.keySet())
	{
		List<DetailedFacetItem> items = new ArrayList<DetailedFacetItem>(facets.get(facet));
		if (items.size() == 0)
			continue; // don't show facet if it has no items.

		// the facet items could still all have 0 count, in which case, skip this facet
		boolean nonzero = false;
		for (DetailedFacetItem f: items)
			if (f.totalCount() > 0)
			{
				nonzero = true;
				continue;
			}
		if (!nonzero)
			continue;

		String facetTitle = Util.escapeHTML(facet);
		if ("sentiments".equals(facetTitle))
			facetTitle = "lexicon";
		if ("people".equals(facetTitle))
			facetTitle = "correspondents";

		facetTitle = Util.capitalizeFirstLetter(facetTitle);
		out.println("<b>" + facetTitle + "</b><br/>\n");
		Collections.sort(items);

		// generate html for each facet. selected and unselected facets separately
		List<String> htmlForSelectedFacets = new ArrayList<String>();
		List<String> htmlForUnSelectedFacets = new ArrayList<String>();

		// random idea: BUI (Blinds user interface. provide blinds-like controls (pull a chain down/to-the-side to reveal information))
		for (DetailedFacetItem f: items)
		{
			if (f.totalCount() == 0)
				continue;
	    	// find the facet url in the facet params
			int facetParamIdx = Util.indexOfUrlParam(origQueryString, f.messagesURL); // TODO: do we need to worry about origQueryString.replaceAll("%20", " ")?
			boolean facetAlreadySelected = facetParamIdx >= 0;
			if (Util.nullOrEmpty(f.name))
			{
				JSPHelper.log.info ("Warning; empty title!"); /* happened once */
				continue;
			}
			String name = Util.ellipsize(f.name, 15);
			String url = request.getRequestURI();
			// f.url is the part that is to be added to the current url
			if (!Util.nullOrEmpty(origQueryString))
			{
			    if (!facetAlreadySelected)
			    	url += "?" + origQueryString + "&" + f.messagesURL;
			    else
			    	// facet filter already selected, so unselect it
					url += "?" + Util.excludeUrlParam(origQueryString, f.messagesURL);
			}
			else
			{
				// no existing params ... not sure if this can happen (might some day if we want to browse all messages in session)
				url += '?' + f.messagesURL;
			}

			String c = facetAlreadySelected ? " selected-facet rounded" : "";

			String html = "<span class=\"facet nojog" + c + "\" style=\"padding-left:2px;padding-right:2px\" onclick=\"javascript:self.location.href='" + url + "';\" title=\"" + Util.escapeHTML(f.description) + "\">" + Util.escapeHTML(name)
						+ " <span class=\"facet-count\">(" + f.totalCount() + ")</span>"
						+ "</span><br/>\n";
			if (facetAlreadySelected)
				htmlForSelectedFacets.add(html);
			else
				htmlForUnSelectedFacets.add(html);
		}
		
		// prioritize selected over unselected facets
		List<String> htmlForAllFacets = new ArrayList<String>();
		htmlForAllFacets.addAll(htmlForSelectedFacets);
		htmlForAllFacets.addAll(htmlForUnSelectedFacets);

		int N_INITIAL_FACETS = 5;
		// print the first 5 items, rest hidden under a More link
		int count = 0;
		for (String html: htmlForAllFacets)
		{
			out.println (html);
			if (++count == N_INITIAL_FACETS && htmlForAllFacets.size() > N_INITIAL_FACETS)
				out.println("<div style=\"display:none;margin:0px;\">\n");
		}
		
		if (count > N_INITIAL_FACETS)
		{
			out.println("</div>");
			out.println("<div class=\"clickableLink\" style=\"text-align:right;padding-right:10px;font-size:80%\" onclick=\"muse.reveal(this)\">More</div>\n");
		}
		out.println ("<br/>");
		
	} // String facet
	%>
</div> <!--  .facets -->
</div> <!-- 160px block -->

<%

%>

<div style="display:inline-block;vertical-align:top;">
	<div class="browse_message_area rounded shadow;position:relative" style="width:1020px;min-height:400px">
		<div class="controls" style="position:relative;width:100%;">

			<div style="position:relative;float:left;padding:5px;">
				<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
				<i title="Do not transfer" id="doNotTransfer" class="flag fa fa-ban"></i>
				<i title="Transfer with restrictions" id="transferWithRestrictions" class="flag fa fa-exclamation-triangle"></i>
				<% } %>
				<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode() || ModeConfig.isDeliveryMode()) { %>
				<i title="Message Reviewed" id="reviewed" class="flag fa fa-eye"></i>
				<% } %>
				<% if (ModeConfig.isDeliveryMode()) { %>
				<i title="Add to Cart" id="addToCart" class="flag fa fa-shopping-cart"></i>
				<% } %>

				<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode() || ModeConfig.isDeliveryMode()) { %>
				<div style="display:inline;" id="annotation_div" style="z-index:1000;">
					<input id="annotation" placeholder="Annotation" style="z-index:1000;width:20em;margin-left:25px"/>
				</div>
				<% } %>
				<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode() || ModeConfig.isDeliveryMode()) { %>
				<!--			<div style="display:inline-block;position:relative;top:10px"><input type="checkbox" id="applyToAll" style="margin-left:250px"/> Apply to all</div> -->
				<button type="button" class="btn btn-default" style="margin-left:25px;margin-right:25px;" id="apply">Apply <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
				<%
					// show apply to all only if > 1 doc
					if (docs.size() > 1) { %>
				<button type="button" class="btn btn-default" id="applyToAll" style="margin-right:25px">Apply to all <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
				<% } %>
				<% } %>
			</div>

			<div style="float:right;position:relative;top:8px">
				<div>
						<div style="display:inline;vertical-align:top;font-size:20px; position:relative; top:8px; margin-right:10px" id="pageNumbering"></div>
						<ul class="pagination">
							<li class="button">
								<a id="page_back" style="border-right:0" href="#0" class="icon-peginationarrow"></a>
								<a id="page_forward" href="#0" class="icon-circlearrow"></a>
							</li>
						</ul>
				</div>
				<!--
				<img src="images/back_enabled.png" id="back_arrow"/>
				<img src="images/forward_enabled.png" id="forward_arrow"/>
				-->
				<!--
					 <div class="pagination">
						 <li><a id="back_arrow" style="border-right:0" href="#0" class="icon-peginationarrow"></a></li>
						 <li> <div style="display:inline;" id="pageNumbering"></div></li>
						 <li><a id="forward_arrow" href="#0" class="icon-circlearrow"></a></li>
					 </div>
					 -->
			</div>
			<div style="clear:both"></div>
		</div> <!-- controls -->

		<!--  to fix: these image margins and paddings are held together with ducttape cos the orig. images are not a consistent size -->
<!-- <span id="jog_status1" class="showMessageFilter rounded" style="float:left;opacity:0.5;margin-left:30px;margin-top:10px;">&nbsp;0/0&nbsp;</span>  -->	
	<div style="font-size:12pt;opacity:0.5;margin-left:30px;margin-right:30px;margin-top:10px;">
<!--	Unique Identifier: <span id="jog_docId" title="Unique ID for this message"></span> -->
	</div>
	<div style="clear:both"></div>
	<div id="jog_contents" style="position:relative" class="<%=jog_contents_class%>">
		<div style="text-align:center"><h2>Loading <%=Util.commatize(docs.size())%> messages <img style="height:20px" src="images/spinner.gif"/></h2></div>
	</div>
	</div>	
</div>

<% 	out.flush();

	// has indexer indexed these docs ? if so, we can use the clusters in the indexer.
	// but it may not have, in which case, we just split up the docs into monthly intervals.
	Set<String> selectedPrefixes = lexicon == null ? null : lexicon.wordsForSentiments(archive.indexer, docs, request.getParameterValues("sentiment"));
	if (selectedPrefixes == null)
		selectedPrefixes = new LinkedHashSet<String>();
    else{
        //add quotes or else, stop words will be removed and highlights single words
        Set<String> tmp = new HashSet<String> ();
        for(String sp: selectedPrefixes)
            tmp.add('"'+sp+'"');
        selectedPrefixes = tmp;
    }
	String searchType = request.getParameter("searchType");
	// warning: remember to convert, otherwise will not work for i18n queries!
	String[] searchTerms = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("term"));
	Set<String> highlightTermsUnstemmed = new LinkedHashSet<String>(); 
	if (searchTerms != null && searchTerms.length > 0)
		for (String s: searchTerms) {
			selectedPrefixes.addAll(IndexUtils.getAllWordsInQuery(s));
			// note: we add the term to unstemmed terms as well -- no harm. this is being introduced to fix a query param we had like term=K&L Gates and this term wasn't being highlighted on the page earlier, because it didn't match modulo stemming 
			// if the query param has quotes, strip 'em
			// along with phrase in quotes thre may be other terms, this method does not handle that.
			if (s.startsWith("\"") && s.endsWith("\""))
				s = s.substring(1, s.length()-1);
			highlightTermsUnstemmed.addAll(IndexUtils.getAllWordsInQuery(s));
		}

    String[] contactIds = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("contact"));
	Set<Integer> highlightContactIds = new LinkedHashSet<Integer>();
    if(contactIds!=null && contactIds.length>0)
        for(String cis: contactIds) {
            try {
                int ci = Integer.parseInt(cis);
                highlightContactIds.add(ci);
            }catch(Exception e){
                JSPHelper.log.warn(cis+" is not a contact id");
            }
        }
	// now if filter is in effect, we highlight the filter word too
	NewFilter filter = (NewFilter) JSPHelper.getSessionAttribute(session, "currentFilter");
	if (filter != null && filter.isRegexSearch()) {
		highlightTermsUnstemmed.add(filter.get("term"));
	}
	Boolean isRegexSearch = false;
	if(searchType!=null && searchType.equals("regex"))
		isRegexSearch = true;

	Pair<DataSet, String> pair = null;
	try {
		pair = EmailRenderer.pagesForDocuments(docs, archive, datasetName, highlightContactIds, selectedPrefixes, highlightTermsUnstemmed, highlightAttachments);
	}catch(Exception e){
		e.printStackTrace();
	}
		DataSet browseSet = pair.getFirst();
	String html = pair.getSecond();
	
	//this is better than changing the aguements of the avbove function.
	browseSet.sensitive = "true".equals(request.getParameter("sensitive"));
	//browseSet.isRegexSearch = isRegexSearch;

	// entryPct says how far (what percentage) into the selected pages we want to enter
	int entryPage = IndexUtils.getDocIdxWithClosestDate((Collection) docs, HTMLUtils.getIntParam(request, "startMonth", -1), HTMLUtils.getIntParam(request, "startYear", -1));
	if (entryPage < 0) {
		// if initdocid is set, look for a doc with that id to set the entry page
		String docId = request.getParameter("initDocId");
		int idx = 0;
		for (Document d: docs)
		{
			if (d.getUniqueId().equals(docId))
				break;
			idx++;		
		}
		if (idx < docs.size()) // means it was found
			entryPage = idx;
		else
			entryPage = 0;
	}
	out.println ("<script type=\"text/javascript\">var entryPage = " + entryPage + ";</script>\n");

	session.setAttribute (datasetName, browseSet);
	session.setAttribute ("docs-" + datasetName, new ArrayList<Document>(docs));
	out.println (html);
	JSPHelper.log.info ("Browsing " + browseSet.size() + " pages in dataset " + datasetName);
	
%>
</div> <!--  browsepage -->
<br/>
<script type="text/javascript">

var transferWithRestrictions = [], doNotTransfer = [], reviewed = [], addToCart = [], messageIds = [], annotations = [];


function apply(e, toAll) {
	var post_data = {};

	// set the post_data based on these vars which track the state of the flags of the currently displayed message
	var dnt = $('#doNotTransfer').hasClass('flag-enabled');
	var twr = $('#transferWithRestrictions').hasClass('flag-enabled');
	var rev = $('#reviewed').hasClass('flag-enabled');
	var atc = $('#addToCart').hasClass('flag-enabled');
	var ann = $('#annotation').val();
	post_data.setDoNotTransfer = dnt ? "1" : "0";
	post_data.setTransferWithRestrictions = twr ? "1" : "0";
	post_data.setReviewed =  rev ? "1" : "0";
	post_data.setAddToCart =  atc ? "1" : "0";
	post_data.setAnnotation = ann;

	function check_twr() { epadd.log ('checking twr'); check_flags (transferWithRestrictions, twr, 'setTransferWithRestrictions', 'transfer-with-restrictions', check_reviewed); }
	function check_reviewed() { epadd.log ('checking reviewed'); check_flags (reviewed, rev, 'setReviewed', 'reviewed', check_add_to_cart); }
	function check_add_to_cart() { epadd.log ('checking add to cart'); check_flags (addToCart, atc, 'setAddToCart', 'add-to-cart', check_annotations); }
// any messages in current dataset already have annotations?

	function check_annotations() {
		var anyAnnotations = false;
		$('.page').each(function (i, o) { if (annotations[i]) {	anyAnnotations = true;	return false;};});

		if (anyAnnotations) {
			epadd.log('showing overwrite/append modal');
			// summon the modal and assign click handlers to the buttons.
			$('#info-modal .modal-body').html('Some messages already have annotations. Append to existing annotations, or overwrite them?');
			$('#info-modal').modal();
			modal_shown = true;
			$('#overwrite-button').click(function () {
				epadd.log('overwrite button clicked');
				post_updates();
			});
			$('#append-button').click(function () {
				epadd.log('append button clicked');
				post_data.append = 1;
				post_updates();
			});
			//			$('#cancel-button').click(function() { /* do nothing */});
		}
		else
			post_updates();
	}

	// prompts if any conflicting flags, and if the users says no, then deletes the given prop_name from post_data, then calls continuation()
	// if the user says yes, or there is no conflict, it calls continuation()
	function check_flags(flags_array, new_val, prop_name, description, continuation) {
		var trueCount = 0, falseCount = 0, totalCount = 0;
		$('.page').each (function(i, o) { totalCount++; if (flags_array[i]) { trueCount++;} else { falseCount++}});
		var apply_continuation = false;
		if (trueCount > 0 && falseCount > 0) {
			var mesg = 'The ' + description + ' flag is set for ' + epadd.pluralize(trueCount, 'message') + ' and unset for ' + epadd.pluralize(falseCount, 'message') + '. '
			+ (new_val ? 'Set' : 'Unset') + ' this flag for all ' + epadd.pluralize(totalCount, 'message') + '?';

			epadd.log('showing confirm modal for: overwrite/append modal: ' + mesg);

			$('#info-modal1 .modal-body').html(mesg);
			// make sure to unbind handlers before adding new ones, so that the same handler doesn't get called repeatedly (danger of this happening because the info-modal1 is the same element)
			$('#no-button').unbind().click(function() { apply_continuation = true; epadd.log('cancelling application of ' + description); alert(prop_name + ' = ' + post_data[prop_name]); + delete post_data[prop_name]; });; // unbind all prev. handlers
			$('#yes-button').unbind().click(function() { apply_continuation = true; epadd.log('continuing with application of ' + description);});
			// call continuation only if apply_continuation is set (i.e yes or no was selected), otherwise even dismissing the modal from its close (x) sets it off.
			$('#info-modal1').unbind('hidden.bs.modal').on('hidden.bs.modal', function() { if (apply_continuation) { continuation(); } else { epadd.log(description + ' modal dismissed without yes or no.');}});
			$('#info-modal1').modal();
		}
		else
			continuation(); // no continuation, easy.
	}

	var url = 'ajax/applyFlags.jsp';
	// first check if applying to all, in case we may need to check if we append/overwrite
	var modal_shown = false;
	if (toAll) {
		epadd.log ('checking do not transfer');
		check_flags (doNotTransfer, dnt, 'setDoNotTransfer', 'do-not-transfer', check_twr);
		// the continuation sequence will end up calling post_updates
	}
	else
		post_updates();

	function post_updates() {
		epadd.log ('posting updates');
		if (toAll)
			post_data.datasetId = '<%=datasetName%>';
		else
			post_data.docId = messageIds[PAGE_ON_SCREEN];

		// we unbind to make sure multiple copies of the same handler don't get attached to the overwrite/append buttons.
		$('#append-button').unbind();
		$('#overwrite-button').unbind();
		// we need the spinner to be visible for at least 500ms so it will be clear to the user that the button press was accepted.
		// otherwise, the op is usually so quick that the user doesn't know whether anything happened.
		var fade_spinner_with_delay = function () {
			$spinner.delay(500).fadeOut();
		};
		var $spinner = $('.spinner', $(e.target));
		epadd.log($spinner);
		$spinner.show();

		// updates state to all pages in this set in-browser
		function update_pages_in_browser() {
			epadd.log ('updating pages in browser');

			if (toAll) {
				$('.page').each(function (i, o) {
					if (post_data.setDoNotTransfer) { doNotTransfer[i] = dnt; }
					if (post_data.setTransferWithRestrictions) { transferWithRestrictions[i] = twr; }
					if (post_data.setReviewed) { reviewed[i] = rev; }
					if (post_data.setAddToCart) { addToCart[i] = atc; }
					if (ann) {
						annotations[i] = (post_data.append) ? ((annotations[i] ? annotations[i] : "") + " " + ann) : ann;
					}
				});
				// also update the annotation on screen to the updated value of the current page
				if (annotations[PAGE_ON_SCREEN] != null && annotations[PAGE_ON_SCREEN].length > 0)
					$('#annotation').val(annotations[PAGE_ON_SCREEN]);
			}
			else {
				doNotTransfer[PAGE_ON_SCREEN] = dnt;
				transferWithRestrictions[PAGE_ON_SCREEN] = twr;
				reviewed[PAGE_ON_SCREEN] = rev;
				addToCart[PAGE_ON_SCREEN] = atc;
				annotations[PAGE_ON_SCREEN] = ann;
			}
		}

		// now post updates to server
		epadd.log('hitting url ' + url);
		$.ajax({
			type: 'POST',
			url: url,
			datatype: 'json',
			data: post_data,
			success: function (data, textStatus, jqxhr) {
				fade_spinner_with_delay();
				update_pages_in_browser(); // update pages in browser only if server update successful, because we don't want browser and server out of sync
				epadd.log("Completed flags updated with status " + textStatus);
			},
			error: function (jq, textStatus, errorThrown) {
				fade_spinner_with_delay();
				$spinner.delay(500).fadeOut();
				var message = ("Error setting flags. Please try again, and if the error persists, report it to epadd_project@stanford.edu. (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")");
				epadd.log(message);
				epadd.alert(message);
			}
		});
		// these ajax reqs are completing normally, but report an error after completion... not sure why.
		return false;
	}
}


var PAGE_ON_SCREEN = -1, TOTAL_PAGES = 0; // these are global vars

function update_controls_on_screen(currentPage) {
	// if b, $elem has flag-enabled class added to it, otherwise flag-enabled class is removed
	function set_class (b, $elem) {
		if (b)
			$elem.addClass('flag-enabled');
		else
			$elem.removeClass('flag-enabled');
	}
	$('#pageNumbering').html(((TOTAL_PAGES == 0) ? 0 : currentPage+1) + '/' + TOTAL_PAGES);
	set_class(doNotTransfer[currentPage], $('#doNotTransfer'));
	set_class(transferWithRestrictions[currentPage], $('#transferWithRestrictions'));
	set_class(reviewed[currentPage], $('#reviewed'));
	set_class(addToCart[currentPage], $('#addToCart'));

	// color the arrows
	/*
	 $('#page_back').attr('src', (currentPage == 0) ? 'images/back_disabled.png' : 'images/back_enabled.png');
	 $('#page_forward').attr('src', (currentPage == TOTAL_PAGES-1) ? 'images/forward_disabled.png' : 'images/forward_enabled.png');
	 */

}

// toggle each flag upon click
$('.flag').click (function(e) {
	var $target = $(e.target);
	$target.toggleClass('flag-enabled');
});

$('#apply').click(function(e) { apply(e, false);});
$('#applyToAll').click(function(e) { apply(e, true);});

// currently called before the new page has been rendered
var callback = function(oldPage, currentPage) {

	update_controls_on_screen(currentPage);

	var $annotation = $('#annotation');

	if (annotations[currentPage] != null && annotations[currentPage].length > 0)
	{
		$annotation.val(annotations[currentPage]);
		$('#annotation_div').show();
	}
	else
	{
		$annotation.val('');
		$annotation.attr('placeholder', '<%=edu.stanford.muse.util.Messages.getMessage("messages", "annotation.label")%>');
	}
	PAGE_ON_SCREEN = currentPage;
};

//called after the new page has been rendered
var load_callback = function(currentPage) {
	if (parent.jog_onload)
		parent.jog_onload(document);
};

$('body').ready(function() {
	$pages = $('.page');

	PAGE_ON_SCREEN = 0;
	TOTAL_PAGES = $pages.length;
	for (var i = 0; i < TOTAL_PAGES; i++)
	{
		annotations[i] = $pages[i] . getAttribute('comment'); // pages[i] is not HttpSession
		doNotTransfer[i] = ($pages[i] . getAttribute('doNotTransfer') != null); // pages[i] is not HttpSession
		transferWithRestrictions[i] = ($pages[i] . getAttribute('transferWithRestrictions') != null); // pages[i] is not HttpSession
		addToCart[i] = ($pages[i] . getAttribute('addToCart') != null); // pages[i] is not HttpSession
		reviewed[i] = ($pages[i] . getAttribute('reviewed') != null);
		messageIds[i] = $pages[i] . getAttribute('docID');
	}
	update_controls_on_screen(PAGE_ON_SCREEN);

	//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	//Big gotcha here: Be very careful what method is passed as logger into this jog method.
	//if for some reason, the logger fails or actually does a post operation; this thing pushes
	// it to retry making the whole thing (the entire browser and the system) to slow down
	//TODO: JOG plugin should not be this aggressive with the logger
	//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	var x = $(document).jog({
		paging_info: {url: 'ajax/jogPageIn.jsp?datasetId=<%=datasetName%>&debug=<%=request.getParameter("debug")%>', window_size_back: 30, window_size_fwd: 50},
					page_change_callback: callback,
	//				page_load_callback: load_callback,
					logger: epadd.log,
					width: 180,
					disabled: 'true',
					dynamic: false
	});
		
	$('#page_forward').click(x.forward_func);
	$('#page_back').click(x.back_func);
	
});

// on page unload, release dataset to free memory 
$(window).unload(function() {
	epadd.log ('releasing dataset <%=datasetName%>');
	$.get('ajax/releaseDataset.jsp?datasetId=<%=datasetName%>');
});

</script>
<% } %> <!--  Util.nullOrEmpty(docs)  -->
<br/>

<div id="info-modal" class="modal fade" style="z-index:9999">
<div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title">Confirm</h4>
      </div>
      <div class="modal-body">
       </div>
      <div class="modal-footer">
        <button id='append-button' type="button" class="btn btn-default" data-dismiss="modal">Append</button>
        <button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>
        <button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div id="info-modal1" class="modal fade" style="z-index:9999">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
				<h4 class="modal-title">Confirm</h4>
			</div>
			<div class="modal-body">
			</div>
			<div class="modal-footer">
				<button id='no-button' type="button" class="btn btn-default" data-dismiss="modal">Leave flags unchanged</button>
				<button id='yes-button' type="button" class="btn btn-default" data-dismiss="modal">Continue</button>
			</div>
		</div><!-- /.modal-content -->
	</div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div style="clear:both"></div>
<jsp:include page="footer.jsp"/>
</body>
</html>
