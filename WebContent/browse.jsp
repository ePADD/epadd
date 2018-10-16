<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%
    JSPHelper.checkContainer(request); // do this early on so we are set up
    request.setCharacterEncoding("UTF-8");
%>
<%@page import="edu.stanford.muse.index.*"%>
<%@page import="edu.stanford.muse.util.DetailedFacetItem"%>
<%@page import="edu.stanford.muse.util.EmailUtils"%>
<%@page import="edu.stanford.muse.util.Pair"%>
<%@page import="edu.stanford.muse.webapp.EmailRenderer"%>
<%@page import="edu.stanford.muse.webapp.HTMLUtils"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig"%>

<%@ page import="java.util.*" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="edu.stanford.muse.LabelManager.Label" %>
<%@ page import="edu.stanford.muse.LabelManager.LabelManager" %>
<%@ page import="org.json.JSONArray" %>

<%@include file="getArchive.jspf" %>

<%
    String searchTerm = JSPHelper.convertRequestParamToUTF8(request.getParameter("term"));

    // compute the title of the page
    String title = request.getParameter("title");
    //<editor-fold desc="Derive title if the original title is not set" input="request" output="title"
    // name="search-title-derivation">
    {
        if (Util.nullOrEmpty(title)) {

            // good to give a meaningful title to the browser tab since a lot of them may be open
            // warning: remember to convert, otherwise will not work for i18n queries!
            String sentiments[] = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("lexiconCategory"));

            String[] persons = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("person"));
            String[] attachments = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("attachment"));

            int month = HTMLUtils.getIntParam(request, "month", -1);
            int year = HTMLUtils.getIntParam(request, "year", -1);
            int cluster = HTMLUtils.getIntParam(request, "timeCluster", -1);

            String sentimentSummary = "";
            if (sentiments != null && sentiments.length > 0)
                for (int i = 0; i < sentiments.length; i++) {
                    sentimentSummary += sentiments[i];
                    if (i < sentiments.length - 1)
                        sentimentSummary += " & ";
                }

            if (searchTerm != null)
                title = "Search: " + searchTerm;
            else if (cluster != -1)
                title = "Cluster " + cluster;
            else if (!Util.nullOrEmpty(sentimentSummary))
                title = sentimentSummary;
            else if (attachments != null && attachments.length > 0)
                title = attachments[0];
            else if (month >= 0 && year >= 0)
                title = month + "/" + year;
            else if (year >= 0)
                title = Integer.toString(year);
            else if (persons != null && persons.length > 0) {
                title = persons[0];
                if (persons.length > 1)
                    title += "+" + (persons.length - 1);
            } else
                title = "Browse";
            title = Util.escapeHTML(title);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Setup (load archive/prepare session) for public mode- if present" input="request"
    // output="">
    /*if (ModeConfig.isPublicMode()) {
        // this browse page is also used by Public mode where the following set up may be requried.
        String archiveId = request.getParameter("aId");
        Sessions.loadSharedArchiveAndPrepareSession(session, archiveId);
    }*/
    //</editor-fold>

    String docsetID;
    //<editor-fold desc="generate a random id for this dataset (the terms docset and dataset are used interchangeably)"
    // input="" output="String:datasetID">
    {
        docsetID = String.format("docset-%08x", EmailUtils.rng.nextInt());// "dataset-1";
    }
    //</editor-fold>

    String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);

%>
<!DOCTYPE HTML>
<html lang="en">
<head>
    <META http-equiv="Content-Type" content="text/html; charset=UTF-8">

    <title><%=title%></title>

    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="css/jquery.qtip.min.css">
    <jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/main.css">

    <script src="js/stacktrace.js" type="text/javascript"></script>
    <script src="js/jquery.js" type="text/javascript"></script>

    <script type='text/javascript' src='js/jquery.qtip.min.js'></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/selectpicker.js"></script>
    <script src="js/modernizr.min.js"></script>
    <script src="js/sidebar.js"></script>

    <script src="js/muse.js" type="text/javascript"></script>
    <script src="js/epadd.js"></script>
    <script>var archiveID = '<%=archiveID%>';</script> <!-- make the archiveID available to browse.js -->
    <script>var docsetID = '<%=docsetID%>';</script> <!-- make the dataset name available to browse.js -->
    <script src="js/browse.js" type="text/javascript"></script>
    <script type='text/javascript' src='js/utils.js'></script>     <!-- For tool-tips -->

    <style>
        div.facets hr { width: 90%; }
        .navbar { margin-bottom: 0; } /* overriding bootstrap */
        .dropdown-header { font-weight: 600;color: black; font-size: 15px;}
         a.opt { color: black;  padding-left: 1.25em; }
        ul.dropdown-menu.inner li, .multi-select .dropdown-menu ul li { border-bottom: none; }
        svg { fill: blue; color: red; stroke: green; }
    </style>


</head>
<div id="annotation-modal" class="modal fade" style="z-index:9999">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title">Edit Annotation</h4>
            </div>
            <textarea title="annotation" name="annotationField" style="margin: 3%; width: 90%; border: solid 1px gray;" class="modal-body">

            </textarea>
            <div class="modal-footer">
                    <label class="radio-inline">
                        <input type="radio" name="overwrite-append-options" value="overwrite" checked>
                        <span class="text-radio">Overwrite</span>
                    </label>
                    <label class="radio-inline">
                        <input type="radio" name="overwrite-append-options" value="append">
                        <span class="text-radio">Append</span>
                    </label>
                <br><br>
                <button id='ok-button' type="button" class="btn btn-default" data-dismiss="modal">APPLY TO THIS MESSAGE</button>
                <button id='apply-all-button' type="button" class="btn btn-default" data-dismiss="modal">APPLY TO ALL MESSAGES</button>
            </div>
        </div><!-- /.modal-content -->
    </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<body > <!--  override margin because this page is framed. -->
<jsp:include page="header.jspf"/>
<%--<script>epadd.nav_mark_active('Browse');--%>

<div class="nav-toggle1 sidebar-icon">
<img src="images/sidebar.png" alt="sidebar">
</div>
<nav class="menu1" role="navigation">
    <h2>Browsing Messages</h2>
    <!--close button-->
    <a class="nav-toggle1 show-nav1" href="#">
        <img src="images/close.png" class="close" alt="close">
    </a>

    <div class="search-tips" style="display:block">

        <% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
            Navigate between messages using the left and right arrow keys on your keyboard. Hold down the arrow key to move more quickly.
            <br/>
            <br/>

            Use the search facets on the left of the message pane to further refine your search results.
            <br/>
            <br/>

            Click on a correspondent name or address in the message headers, or an entity in the message subject or body, to jump to a set of all related messages.
            <br/>
            <br/>

            Create annotations to add description to a single message or set of messages. All annotations you add are searchable via Advanced Search. Annotations will export between the Appraisal, Processing, and Delivery modules. Annotations will not export outside of ePADD, including from the Delivery module.
            <br/>
            <br/>

            If you do not want annotations to export between modules, you must clear them by first navigating to the set of messages with that annotation using Advanced Search, then selecting the annotation, then overwriting that annotation with a blank annotation, then selecting “Apply to all messages.”
            <br/>
            <br/>

            Use general labels (created via the Labels option on the dashboard) to add a brief description to a set of messages, or to mark messages as already reviewed.
            <br/>
            <br/>

            General labels will export between the Appraisal, Processing, and Delivery modules. If you do not want general labels to export between these modules, you must clear them by first navigating to the set of messages with that label from the Labels screen, selecting “Label All,” then selecting “Unset for All” for the given label.
            <br/>
            <br/>

            Use restriction labels (created via the Labels option on the dashboard) to flag messages for restriction, including for a certain period from the current date, or from the date of creation.
            <br/>
            <br/>

            Restricted messages (and associated restriction labels), except for messages assigned the “Do not Transfer” restriction label, will export from the Appraisal module to the Processing module. Restricted messages will not export to the Discovery or Delivery modules, unless they are also assigned the “Cleared for release” label within the Appraisal or Processing modules.
            <br/>
            <br/>

            All labels are searchable via Advanced Search.
            <br/>
            <br/>

            Default labels applied to all messages can be set from the Dashboard, under the More option.
            <br/>
            <br/>

            [Icon 1] - Download all messages in a set of results.
            <br/>
            <br/>


            [Icon 2] - Annotate this message.
            <br/>
            <br/>

            [Icon 3] - Show thread view.
            <br/>
            <br/>

            [Icon 4] - Copy the message ID for the selected message. The message ID is a unique identifier within the collection that can be used to return to a particular message in the future. To return to a particular message in the future, enter the collection, then paste the link associated with the message ID into your web browser’s address bar. You can also paste the message ID into the appropriate field in Advanced Search.
            <br/>
            <br/>

            [Icon 5] - Show message attachments.
            <br/>
            <br/>

        <% } else if (ModeConfig.isDiscoveryMode()) { %>

            Messages in this view are redacted to protect privacy and copyright. To request access to the full text of any messages, please contact the host institution.
            <br/>
            <br/>

            Navigate between messages using the left and right arrow keys on your keyboard. Hold down the arrow key to move more quickly.
            <br/>
            <br/>

            Use the search facets on the left of the message pane to further refine your search results.
            <br/>
            <br/>

            Click on a correspondent name or address in the message headers, or an entity in the message subject or body, to jump to a set of all related messages.
            <br/>
            <br/>

            The message ID is a unique identifier within the collection that can be used to return to a particular message in the future. To return to a particular message in the future, enter the collection, then paste the link associated with the message ID. You can also paste the message ID into the appropriate field in Advanced Search.
            <br/>
            <br/>

        <% } else if (ModeConfig.isDeliveryMode()) { %>
            Navigate between messages using the left and right arrow keys on your keyboard. Hold down the arrow key to move more quickly.
            <br/>
            <br/>

            Use the search facets on the left of the message pane to further refine your search results.
            <br/>
            <br/>

            Click on a correspondent name or address in the message headers, or an entity in the message subject or body, to jump to a set of all related messages.
            <br/>
            <br/>

            Create annotations to add description to a single message or set of messages. All annotations you add are searchable via Advanced Search. If exported messages have been annotated, the annotations will not export.
            <br/>
            <br/>

            Use labels (created via the Labels option on the dashboard) to add a brief description to a set of messages, or mark messages as already reviewed.
            <br/>
            <br/>

            Labels will not export.
            <br/>
            <br/>

            All labels are searchable via Advanced Search.
            <br/>
            <br/>

            Set default labels for all messages from the Dashboard, under the More option.
            <br/>
            <br/>

            [Icon 1] - Download all messages in a set of results.
            <br/>
            <br/>

            [Icon 2] - Annotate this message.
            <br/>
            <br/>

            [Icon 3] - Show thread view.
            <br/>
            <br/>

            [Icon 4] - Copy the message ID for the selected message. The message ID is a unique identifier within the collection that can be used to return to a particular message in the future. To return to a particular message in the future, enter the collection, then paste the link associated with the message ID. You can also paste the message ID into the appropriate field in Advanced Search.
            <br/>
            <br/>

            [Icon 5] - Show message attachments.
            <br/>
            <br/>
        <% } %>
    </div>
</nav>

<script>
    $('body').on('click','#normalizationInfo',function(e){
        // get the attribute's values - originalURL and originalName.
        var origianlURL = $(e.target).data('originalurl');
        var originalName = $(e.target).data('originalname');
        var msg='';
        //prepare the message in html based on these two values.
        if(origianlURL){
            //if originalURL is not null - This file was converted during the preservation process. Click here to download the original file.
            msg="This file was converted during the preservation process. Its original name was "+originalName+". Click <a href="+origianlURL+">here </a> to download the original file";
        }else {
            //if originalURL is null and original Name is not null- This file was renamed during the preservation process. The name of the original file was -
            msg="This file name was cleaned up during the preservation process. The original file name was "+originalName;
        }
        $('#normalization-description').html (msg);
        $('#normalization-info-modal').modal('show');

    });
</script>

<!--  important: include jog_plugin AFTER header.jsp, otherwise the extension is applied to
a jquery ($) object that is overwritten when header.jsp is included! -->
<script src="js/jog_plugin.js" type="text/javascript"></script>

<%

    Collection<Document> docs;
    SearchResult outputSet;
    //<editor-fold desc="Search archive based on request parameters and return the result" input="archive;request"
    // output="collection:docs;collection:attachments(highlightAttachments) name="search-archive">
    {
        // convert req. params to a multimap, so that the rest of the code doesn't have to deal with httprequest directly
        Multimap<String, String> params = JSPHelper.convertRequestToMap(request);
        SearchResult inputSet = new SearchResult(archive,params);
        Pair<Collection<Document>,SearchResult> search_result = SearchResult.selectDocsAndBlobs(inputSet);
        //Pair<Collection<Document>, Collection<Blob>> search_result
        docs = search_result.first;
        //Collections.sort(docs);//order by time
        outputSet = search_result.second;
    }
    //</editor-fold>
    %>
<script>
    TOTAL_PAGES = <%=docs.size()%>;
</script>
<%
    Map<String, Collection<DetailedFacetItem>> facets;
    //<editor-fold desc="Create facets(categories) based on the search data" input="docs;archive"
    // output="facets" name="search-create-facets">
    {
        facets = IndexUtils.computeDetailedFacets(docs, archive);
    }
    //</editor-fold>

    String origQueryString = request.getQueryString();
    //<editor-fold desc="Massaging the query string (containing all search parameter)" input="request"
    // output="string:origQueryString" name="search-query-string-massaging" >
    {
        if (origQueryString == null)
            origQueryString = "";

        // make sure adv-search=1 is present in the query string since we've now switched over to the new searcher
        if (!origQueryString.contains("adv-search=")) {
            if (origQueryString.length() > 0)
                origQueryString += "&";
            origQueryString += "adv-search=1";
        }

        // remove all the either's because they are not needed, and could mask a real facet selection coming in below
        origQueryString = Util.excludeUrlParam(origQueryString, "direction=either");
        origQueryString = Util.excludeUrlParam(origQueryString, "mailingListState=either");
        origQueryString = Util.excludeUrlParam(origQueryString, "attachmentExtension=");
        origQueryString = Util.excludeUrlParam(origQueryString, "entity=");
        origQueryString = Util.excludeUrlParam(origQueryString, "correspondent=");
        origQueryString = Util.excludeUrlParam(origQueryString, "attachmentFilename=");
        origQueryString = Util.excludeUrlParam(origQueryString, "attachmentExtension=");
        origQueryString = Util.excludeUrlParam(origQueryString, "annotation=");
        origQueryString = Util.excludeUrlParam(origQueryString, "folder=");
        // entity=&correspondent=&correspondentTo=on&correspondentFrom=on&correspondentCc=on&correspondentBcc=on&attachmentFilename=&attachmentExtension=&annotation=&startDate=&endDate=&folder=&lexiconName=general&lexiconCategory=Award&attachmentExtension=gif
    }
    //</editor-fold>


    if (Util.nullOrEmpty(docs)) { %>
<div style="margin-top:2em;font-size:200%;text-align:center;">No matching messages.</div>
<%} else { %>
<div class="browsepage" style="min-width:1220px">

    <!-- 160px block on the left for facets -->
    <div style="display:inline-block;vertical-align:top;width:150px;">
        <div class="facets" style="min-width:10em;text-align:left;margin-bottom:0px;">
            <%
                if (!Util.nullOrEmpty(searchTerm)) {
                    out.println("<div class=\"facetTitle\">Search</div>\n");
                    String displayTerm = Util.ellipsize(searchTerm, 16);

                    out.println("<span title=\"" + Util.escapeHTML(searchTerm) + "\" class=\"facet nojog selected-facet rounded\" style=\"padding-left:2px;padding-right:2px\">" + Util.escapeHTML(displayTerm));
                    out.println (" <span class=\"facet-count\">(" + docs.size() + ")</span>");
                    out.println ("</span><br/>\n");
                }
                //<editor-fold desc="Facet-rendering" input="facets" output="html:out">
                for (String facet: facets.keySet())
                {
                    List<DetailedFacetItem> items = new ArrayList<>(facets.get(facet));
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
                    if ("correspondent".equals(facetTitle))
                        facetTitle = "correspondents";

                    facetTitle = Util.capitalizeFirstLetter(facetTitle);
                    out.println("<div class=\"facetTitle\">" + facetTitle + "</div>\n");
                    Collections.sort(items);

                    // generate html for each facet. selected and unselected facets separately
                    List<String> htmlForSelectedFacets = new ArrayList<>();
                    List<String> htmlForUnSelectedFacets = new ArrayList<>();

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
                        String name = Util.ellipsize(f.name, 16);
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
                            // no existing params ... not sure if this can happen (might some day if we want
                            // to browse all messages in session)
                            url += '?' + f.messagesURL;
                        }

                        String c = facetAlreadySelected ? " selected-facet rounded" : "";

                        url = url + "&archiveID="+ ArchiveReaderWriter.getArchiveIDForArchive(archive);
                        String html = "<span class=\"facet nojog" + c + "\" style=\"padding-left:2px;padding-right:2px\" onclick=\"javascript:self.location.href='" + url + "';\" title=\"" + Util.escapeHTML(f.description) + "\">" + Util.escapeHTML(name)
                                + " <span class=\"facet-count\">(" + f.totalCount() + ")</span>"
                                + "</span><br/>\n";
                        if (facetAlreadySelected)
                            htmlForSelectedFacets.add(html);
                        else
                            htmlForUnSelectedFacets.add(html);
                    }

                    // prioritize selected over unselected facets
                    List<String> htmlForAllFacets = new ArrayList<>();
                    htmlForAllFacets.addAll(htmlForSelectedFacets);
                    htmlForAllFacets.addAll(htmlForUnSelectedFacets);

                    int N_INITIAL_FACETS = 5;
                    // toString the first 5 items, rest hidden under a More link
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
                        out.println("<div class=\"clickableLink\" style=\"text-align:left;cursor:pointer;\" onclick=\"muse.reveal(this)\">More...</div>\n");
                    }
                    out.flush();
                } // String facet
                //</editor-fold>
            %>
        </div> <!--  .facets -->
    </div> <!-- 160px block -->

    <!-- 1020px block for the actual message body -->
    <%
        //parameterizing the class name so that any future modification is easier
        String jog_contents_class = "message";
        String collectionName = "", repositoryName = "", institutionName = "", collectionID = "";
        if (!ModeConfig.isAppraisalMode()) {
            Archive.CollectionMetadata cm = archive.collectionMetadata;
            collectionName = Util.escapeHTML(cm.collectionTitle != null ? cm.collectionTitle : "");
            repositoryName = Util.escapeHTML(cm.repository != null ? cm.repository : "");
            institutionName = Util.escapeHTML(cm.institution != null ? cm.institution : "");
            collectionID = Util.escapeHTML(cm.collectionID != null ? cm.collectionID : "");
        }

        String json = archive.getLabelManager().getLabelInfoMapAsJSONString();
        %>
        <script>
            allLabels = JSON.parse('<%=json%>');
        </script>
    <div style="display:inline-block;vertical-align:top;">
        <div class="browse_message_area rounded shadow" style="width:1020px;min-height:600px">
            <div class="controls" style="position:relative;width:100%; border: 1px solid #D4D4D4;">
                <div style="float:left;padding:5px">

                    <div class="form-group label-picker" style="display:inline-block">
                        <select data-selected-text-format="static" name="labelIDs" id="labelIDs" class="label-selectpicker form-control multi-select selectpicker" title="labels" multiple>
                            <option data-label-class="__dummy" data-label-id="__dummy" data-label="__dummy" value="__dummy">Dummy</option>

                            <optgroup label="Restriction Labels">
                                <%
                                    Set<Label> restrlabels = archive.getLabelManager().getAllLabels(LabelManager.LabType.RESTRICTION);
                                    //get general labels
                                    Set<Label> genlabels = archive.getLabelManager().getAllLabels(LabelManager.LabType.GENERAL);
                                    for (Label opt : restrlabels){
                                %>
                                <option value = "<%=opt.getLabelID()%>"><%=opt.getLabelName()%></option>
                                <%}%>
                            </optgroup>
                            <optgroup label="General Labels">
                                <%
                                    for (Label opt : genlabels){
                                %>
                                <option value = "<%=opt.getLabelID()%>"><%=opt.getLabelName()%></option>
                                <%}%>
                            </optgroup>
                        </select>
                    </div>
                    <div class="form-group label-picker" style="display:inline-block;margin-left:20px">

                        <select data-selected-text-format="static" name="sortBy" id="sortBy" class="sortby-selectpicker form-control selectpicker" title="Sort by">
                        <%--<select id="sortBy" class="form-control selectpicker" name="sortBy">--%>
                            <option value="" selected disabled>Sort by</option>
                            <option value="" selected disabled>(Not yet implemented)</option>
                            <option value="relevance">Most relevant</option>
                            <option value="recent">Newest first</option>
                            <option value="chronological">Oldest first</option>
                        </select>
                    </div>
                </div>

                <div style="user-select: none; float:right;position:relative;top:8px; padding-right: 10px;">
                    <div style="display:inline;vertical-align:top;font-size:16px; position:relative;" >

                        <div style="display:inline; border-right: solid 1px #d4d4d4; padding-right: 10px; margin-right: 20px; position: relative; top: 4px; cursor: pointer;">
                            <img title="Download messages as mbox" src="images/labels.svg" onclick="window.location='bulk-labels?archiveID=<%=archiveID%>&docsetID=<%=docsetID%>';">
                        </div>

                        <div style="display:inline; border-right: solid 1px #d4d4d4; padding-right: 10px; margin-right: 20px; position: relative; top: 4px; cursor: pointer;">
                            <img title="Download messages as mbox" src="images/download.svg" onclick="window.location='export-mbox?archiveID=<%=archiveID%>&docsetID=<%=docsetID%>';">
                        </div>

                        <div id="page_back" class="nav-arrow"><span style="position: relative; top:3px"> <img title="Previous message" src="images/prev.svg"/></span></div>
                        <div style="position: relative; top:4px; display:inline-block; padding: 0px 5px">
                            <div style="display:inline; position:relative; " id="pageNumbering"></div> of
                            <div style="display:inline; position:relative; " id="totalPages"><%=docs.size()%></div>
                        </div>
                        <div id="page_forward" class="nav-arrow"> <img title="Next message"  src="images/next.svg"/></div>
                    </div>
                </div>
                <div style="clear:both"></div>
            </div> <!-- controls -->

            <div class="labels-area">
                <!-- will be filled in by render_labels() in JS -->
            </div>

            <br/>

            <div id="position:relative">
                <div class="message-menu">
                    <a href="#" class="annotation-link" title="Message annotation"><img style="padding: 0px 27px; border-right: solid 1px #ccc;" src="images/add_annotation.svg"/></a>
                    <a href="#" class="id-link" title="Get message ID"><img style="padding: 0px 27px; border-right: solid 1px #ccc;" src="images/message_id.svg"/></a>
                    <a href="#" class="thread-link" target="_blank" title="Open thread"><img style="padding: 0px 27px; border-right: solid 1px #ccc;" src="images/thread_view.svg"/></a>
                    <a href="#" class="attach-link" title="Scroll down to attachments"><span style="padding: 0px 5px 0px 27px;">0</span><img src="images/attachments.svg"/></a>
                </div>

                <script>
                    // handlers for message-menu icons
                    $('a.id-link').click(function() {
                        // open the id-modal modal if id link is clicked
                        $id_modal = $('#id-modal');
                        // jam the link from the data-href attribute of the a link (which has been set in page_change_callback) into the .message-link-textarea
                        $('.modal-body .message-link-textarea', $id_modal).val($(this).attr('data-href'));
                        $id_modal.modal();
                        return false;
                    });
                    // add ok and cancel handlers here

                    $(document).on('click', '#copy-message-link-button', function (event) {

                        /* copies embed code to paste buffer */
                        function copy_handler() {
                            // should be called only when ID modal is shown
                            // right now, we copy the entire modal-body as the only thing it contains is the link.
                            var copyArea = $('#id-modal textarea.message-link-textarea')[0];
                            copyArea.select();
                            try {
                                var successful = document.execCommand('copy');
                                // alert ("Copy success = " + successful);
                            } catch (err) {
                                alert ('Sorry, unable to copy the link. Error: ' + err);
                            }
                        }

                        event.preventDefault();
                        copy_handler();
                    });


                    $("a.attach-link").click(function() {
                        // scroll down to attachments area of message if attach-link is clicked
                        // https://stackoverflow.com/questions/6677035/jquery-scroll-to-element
                        $([document.documentElement, document.body]).animate({
                            scrollTop: $(".attachments").offset().top
                        }, 1000);
                        return false;
                    });
                </script>

                <div class="annotation" title="Click to edit annotation">
                    <div class="annotation-header">
                        <span>Annotation</span>
                        <img title="Edit annotation" id="edit-annotation-icon" style="margin-left: 78px; cursor:pointer" src="images/edit_annotation.svg"/>
                        <img title="Close annotation" id="close-annotation-icon" style="margin-left: 25px; cursor: pointer" src="images/close.svg"/></div>
                    <div class="annotation-area">
                        <!-- will be filled in by JS -->
                    </div>
                </div>

                <script>
                    $("#edit-annotation-icon").click(function() {
                            $('#annotation-modal').modal();
                            $('.annotation-area').css('filter','blur(2px)')
                        });
                        $("#close-annotation-icon").click(function() {
                            $('.annotation').hide();
                        });

                </script>


                <div id="jog_contents" style="position:relative; border: 1px solid #D4D4D4;min-height:500px;" class="<%=jog_contents_class%>">
                    <div style="margin-top:150px;text-align:center"><br/><br/><h2>Loading <%=Util.commatize(docs.size())%> messages <img style="height:20px" src="images/spinner.gif"/></h2></div>
                </div>
            </div>

        </div>
    </div>
    <%

        // now if filter is in effect, we highlight the filter word too
        //as it is a common highlighting information valid for all result documents we add it to
        //commonHLInfo object of outputset by calling appropriate API.
        /*NewFilter filter = (NewFilter) JSPHelper.getSessionAttribute(session, "currentFilter");
        if (filter != null && filter.isRegexSearch()) {
            outputSet.addCommonHLInfoTerm(filter.get("searchTerm"));
        }
        */
        //IMP: github issue #171. Actual sorting of the result is being done by SearchResult class in
        //main entry method, selectDocsAndBlobs. The list of documents returned from that method
        //retains the order of the sorting. Earlier this set was not being passed for html rendering
        //and the documents were taken from the SearchResult class. Note that the order of documents
        //in searchResult class is different from the ordering obtained by 'sortBy' parameters. Hence
        //we need to pass that ordered set to getHTML* method below. This fixed the issue.
        Pair<DataSet, JSONArray> pair = null;
        try {
            String sortBy = request.getParameter("sortBy");

            // note: we currently do not support clustering for "recent" type, only for the chronological type.
            // might be easy to fix if needed in the future.
            if ("recent".equals(sortBy) || "relevance".equals(sortBy) || "chronological".equals(sortBy))
                pair = EmailRenderer.pagesForDocuments(docs,outputSet, docsetID, MultiDoc.ClusteringType.NONE);
            else {
                // this path should not be used as it sorts the docs in some order
                // (old code meant to handle clustered docs, so that tab can jump from cluster to cluster. not used now)
                pair = EmailRenderer.pagesForDocuments(docs,outputSet, docsetID);
            }
        }catch(Exception e){
            Util.print_exception("Error while making a dataset out of docs", e, JSPHelper.log);
        }

        DataSet browseSet = pair.getFirst();
        JSONArray jsonObjectsForMessages = pair.getSecond(); // this has message labels
        %>
        <script>
            window.messageMetadata = <%=jsonObjectsForMessages.toString()%>;
        </script>
        <%
        // entryPct says how far (what percentage) into the selected pages we want to enter
        int entryPage = IndexUtils.getDocIdxWithClosestDate((Collection) docs,
                HTMLUtils.getIntParam(request, "startMonth", -1), HTMLUtils.getIntParam(request, "startYear", -1));
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
        String labelMap = archive.getLabelManager().getLabelInfoMapAsJSONString();
        out.println("<script type=\"text/javascript\">var labelMap = "+labelMap+";var numMessages= "+browseSet.size()+";</script>\n");
        session.setAttribute (docsetID, browseSet);

        JSPHelper.log.info ("Browsing " + browseSet.size() + " pages in dataset " + docsetID);

    %>
</div> <!--  browsepage -->
<br/>
<% } %> <!--  Util.nullOrEmpty(docs)  -->
<br/>

<!-- a couple of confirm modals that can be invoked when necessary -->
<div>
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

    <div id="id-modal" class="modal fade" style="z-index:9999">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title">Link to this message</h4>
                </div>
                <div class="modal-body" style="overflow-wrap:break-word"> <!-- overflow-wrap needed because ID URL could be very long -->
                    <textarea style="width:100%; height:120px;" class="message-link-textarea">No link</textarea>
                </div>
                <div class="modal-footer">
                    <button id='cancel-button-cl' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                    <button id='copy-message-link-button' type="button" class="btn btn-default" data-dismiss="modal">Copy</button>
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
</div>
<%--Modal for showing more information about an attachment (in case it was normalized/cleanedup during preservation support)--%>
<div>
    <div id="normalization-info-modal" class="modal fade" style="z-index:9999">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <%--<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>--%>
                    <%--<h4 class="modal-title">Confirm</h4>--%>
                </div>
                <div class="modal-body">
                    <span id="normalization-description"></span>
                </div>
                <div class="modal-footer">
                    <%--<button id='append-button' type="button" class="btn btn-default" data-dismiss="modal">Append</button>--%>
                    <%--<button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>--%>
                    <%--<button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>--%>
                </div>
            </div><!-- /.modal-content -->
        </div><!-- /.modal-dialog -->
    </div><!-- /.modal -->
</div>
<script>


//    });

</script>

<div style="clear:both"></div>
<jsp:include page="footer.jsp"/>
</body>
</html>
