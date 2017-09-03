<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="au.com.bytecode.opencsv.CSVReader" %>
<%@ page import="java.util.*" %>
<%@page language="java" import="com.google.gson.*"%>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.email.AddressBook" %>
<%@ page import="edu.stanford.muse.index.DataSet" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="edu.stanford.muse.index.SearchResult" %>
<%--
  Created by IntelliJ IDEA.
  User: vihari
  Date: 04/08/15
  Time: 15:04
  To change this template use File | Settings | File Templates.
--%>
<!--No use with archive on this page, but for the profile block -->
<%@include file="getArchive.jspf" %>
<%
/**
 * Reads a list of email addresses from CSV file, select docs from or to these email addresses and applies flags selected to these docs*/
%>
<html>
<head>
    <title>Bulk Redaction</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="css/epadd.css">

    <script src="js/jquery.js"></script>
    <script src="js/jquery.dataTables.min.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <script src="js/epadd.js"></script>
    <style>
        div.section {margin-top: 15px; margin-bottom: 25px;}
        div.section .btn-default { position: absolute; right: 50px;}
    </style>
</head>
<body>
    <jsp:include page="header.jspf"/>

    <% writeProfileBlock(out, archive, "Apply actions to multiple messages", "");%>

    <br/>
    <br/>
    <%

        String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
        // convert req. params to a multimap, so that the rest of the code doesn't have to deal with httprequest directly
        Multimap<String, String> params = JSPHelper.convertRequestToMap(request);
        SearchResult inputSet = new SearchResult(archive,params);
        String datasetName = String.format("docset-%08x", EmailUtils.rng.nextInt());// "dataset-1";

        SearchResult outputSet = SearchResult.selectDocsForBulkFlags(inputSet);

        Set<Document> matchedDocs = outputSet.getDocumentSet();
      //  request.setAttribute("selectDocs", matchedDocs);

        // create a dataset out of the matched docs
        DataSet dataset = new DataSet(matchedDocs, outputSet, datasetName);
        session.setAttribute(datasetName, dataset);

    %>

    <div style="width:900px;margin-left:170px; position:relative;">
            <div class="panel">
                <div class="panel-heading">
                    Apply actions to <%= Util.pluralize (matchedDocs.size(), "message")%>
                </div>
                <div class="section">
                    <i title="Do not transfer" id="dnt-flag" class="flag fa fa-ban"></i>
                    <button type="button" class="btn btn-default" style="margin-left:25px;margin-right:25px;" id="dnt-button">Apply to all <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
                </div>
                <div class="section">
                    <i title="Transfer with restrictions" id="twr-flag" class="flag fa fa-exclamation-triangle"></i>
                    <button type="button" class="btn btn-default" style="margin-left:25px;margin-right:25px;" id="twr-button">Apply to all <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
                </div>

                <div class="section">
                    <i title="Message Reviewed" id="reviewed-flag" class="flag fa fa-eye"></i>
                    <button type="button" class="btn btn-default" style="margin-left:25px;margin-right:25px;" id="reviewed-button">Apply to all <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
                </div>

                <div class="section">
                    <div style="display:inline;" id="annotation_div" style="z-index:1000;">
                        <input id="annotation" placeholder="Annotation" style="z-index:1000;width:20em;margin-left:25px"/>
                    </div>
                    <button type="button" class="btn btn-default" style="margin-left:25px;margin-right:25px;" id="annotation-button">Apply to all <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
                </div>
            </div>
    </div>

    <script>
        var fade_spinner_with_delay = function ($spinner) {
            $spinner.delay(500).fadeOut();
        };

        $("div.panel .btn").click(
            function(e) {

                var post_data = {};
                var $target = $(e.target);
                var $spinner = $('.spinner', $target.closest('.section')); // find my spinner
                $spinner.show();

                if ($target.attr('id') === 'dnt-button') {
                    post_data.setDoNotTransfer = $('#dnt-flag').hasClass ('flag-enabled') ? 1 : 0;
                }
                else if ($target.attr('id') === 'twr-button') {
                    post_data.setTransferWithRestrictions = $('#twr-flag').hasClass ('flag-enabled') ? 1 : 0;
                }
                else if ($target.attr('id') === 'reviewed-button') {
                    post_data.setReviewed = $('#reviewed-flag').hasClass ('flag-enabled') ? 1 : 0;
                }
                else if ($target.attr('id') == 'annotation-button') {
                    post_data.setAnnotation = $('#annotation').val();
                }

                post_data.datasetId = '<%=datasetName%>';
                post_data.archiveID = '<%=archiveID%>';
                <%--var allDocs = '<%=request.getParameter("allDocs")%>';--%>
                var url = "ajax/applyFlags.jsp";

                $.ajax({
                    type: 'POST',
                    url: url,
                    datatype: 'json',
//                    allDocs: allDocs,
                    data: post_data,
                    success: function (data, textStatus, jqxhr) {
                        fade_spinner_with_delay($spinner);
                        epadd.log("Completed flags updated with status " + textStatus);
                    },
                    error: function (jq, textStatus, errorThrown) {
                        fade_spinner_with_delay($spinner);
                        var message = ("Error setting flags. Please try again, and if the error persists, report it to epadd_project@stanford.edu. (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")");
                        epadd.log(message);
                        epadd.alert(message);
                    }
                });
            }
        );

        $('.flag').click (function(e) {
            var $target = $(e.target);
            $target.toggleClass('flag-enabled');
        });
    </script>
</body>
</html>
