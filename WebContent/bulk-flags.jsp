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
    <script src="js/jquery.js"></script>
    <link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
    <script src="jqueryFileTree/jqueryFileTree.js"></script>

    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <script src="js/jquery.dataTables.min.js"></script>
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <jsp:include page="css/css.jsp"/>
    <script src="js/epadd.js"></script>
    <script src="js/filepicker.js" type="text/javascript"></script>
    <style type="text/css">
        .js #entities {display: none;}
    </style>
</head>
<body>
<%	AddressBook addressBook = archive.addressBook;
    String bestName = addressBook.getBestNameForSelf();
    String datasetName = String.format("docset-%08x", EmailUtils.rng.nextInt());// "dataset-1";
%>
    <jsp:include page="header.jspf"/>

<<<<<<< HEAD
<% writeProfileBlock(out, bestName, "Apply flags to multiple messages", "");%>
=======
<% writeProfileBlock(out, bestName, "Bulk apply flags", "");%>
>>>>>>> a2eab7412b81eeb8688c7528533ec626f266efb1

<br/>
<br/>
    <script>
        submit = function(){
            window.location = "bulk-flags?filePath="+$("#filePath").val();
        }
    </script>
    <% String filePath = request.getParameter("filePath");
       String allDocsParam = request.getParameter("allDocs");
       boolean allDocs = allDocsParam!=null && allDocsParam.equals("1");

       out.println("<div style='text-align:center'>");
       if(allDocs || (filePath!=null && (new File(filePath).exists()))) {
           if (allDocs) {
               Set<Document> matches = archive.getAllDocsAsSet();
               request.setAttribute("selectDocs", matches);
               out.println(matches.size() + " messages matched<br><br/>");
           } else {
               //read the entries in the file
               CSVReader reader = new CSVReader(new FileReader(filePath));
               Set<String> eas = new LinkedHashSet<String>();
               String[] line;
               while ((line = reader.readNext()) != null) {
                   String eA = line[0].trim();
                   eas.add(eA);
               }
               try {
                   out.println("<div style=\"text-align:center;position:relative;top:30px\">");
                   out.println("Checking " + eas.size() + " email address(es). ");

                   Set<Document> matches = EmailUtils.getDocsForEAs(archive.getAllDocsAsSet(), eas);
                   request.setAttribute("selectDocs", matches);
                   out.println(matches.size() + " messages matched<br><br/>");

                   // create a dataset out of the matched docs
                   DataSet dataset = new DataSet(matches, archive, datasetName, null, null, null, null);
                   session.setAttribute(datasetName, dataset);
                   session.setAttribute("docs-" + datasetName, new ArrayList<Document>(matches));
               } catch (Exception e) {
                   Util.print_exception("Exception while fetching messages for: " + eas, e, JSPHelper.log);
               }
           }
    %>

            <div class="controls" style="position:relative;width:auto;display:inline-block">

<<<<<<< HEAD
                <div style="position:relative;padding:5px;">
                    <i title="Do not transfer" id="doNotTransfer" class="flag fa fa-ban"></i>
                    <i title="Transfer with restrictions" id="transferWithRestrictions" class="flag fa fa-exclamation-triangle"></i>
                    <i title="Message Reviewed" id="reviewed" class="flag fa fa-eye"></i>

                    <div style="display:inline;" id="annotation_div" style="z-index:1000;">
                        <input id="annotation" placeholder="Annotation" style="z-index:1000;width:20em;margin-left:25px"/>
                    </div>
                    <!--			<div style="display:inline-block;position:relative;top:10px"><input type="checkbox" id="applyToAll" style="margin-left:250px"/> Apply to all</div> -->
                    <button type="button" class="btn btn-default" style="margin-left:25px;margin-right:25px;" id="apply">Apply to all <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
                </div>

            </div>
<%
        out.println("</div>");
       }
       else if(filePath==null || !(new File(filePath)).exists()){%>

            <div id="filepicker" style="width:900px;padding-left:170px">

                <div class="div-input-field">
                    <div class="input-field-label"><i class="fa fa-folder-o"></i> CSV File</div>
                    <div class="input-field">
                        <input name="filePath" id="filePath" class="dir form-control" type="text" name="sourceDir"/> <br/>
                        <button onclick="return false;" class="btn-default"><i class="fa fa-file"></i>
                            <span>Browse</span>
                        </button>
                    </div>
                    <br/>
                    <div class="roots" style="display:none"></div>
                    <div class="browseFolder"></div>
                    <br/>
                </div>
            </div>

    <%
=======
        <div id="filepicker" style="width:900px;padding-left:170px">

            <div class="div-input-field">
                <div class="input-field-label"><i class="fa fa-folder-o"></i> CSV File</div>
                <div class="input-field">
                    <input name="filePath" id="filePath" class="dir form-control" type="text" name="sourceDir"/> <br/>
                    <button onclick="return false;" class="btn-default"><i class="fa fa-file"></i>
                        <span>Browse</span>
                    </button>
                </div>
                <br/>
                <div class="roots" style="display:none"></div>
                <div class="browseFolder"></div>
                <br/>
            </div>
        </div>

        <%
>>>>>>> a2eab7412b81eeb8688c7528533ec626f266efb1
            java.io.File[] rootFiles = java.io.File.listRoots();
            List<String> roots = new ArrayList<String>();
            for (java.io.File f: rootFiles)
                roots.add(f.toString());
            String json = new Gson().toJson(roots);
<<<<<<< HEAD
%>
=======
        %>
>>>>>>> a2eab7412b81eeb8688c7528533ec626f266efb1
        <script>
            var roots = <%=json%>;
            var fp = new FilePicker($('#filepicker'), roots);
        </script>

        <div style="text-align:center;position:relative;top:30px">
            <button onclick="submit()" class="btn btn-cta" id="gobutton">Submit <span class="spinner"><i class="icon-arrowbutton"></i></span> </button>
        </div>
<<<<<<< HEAD
    <%}
=======
    <%}else {
        //read the entries in the file
        CSVReader reader = new CSVReader(new FileReader(filePath));
        Set<String> eas = new LinkedHashSet<String>();
        String[] line;
        int count = 0;
        while ((line = reader.readNext()) != null) {
            String eA = line[0].trim().toLowerCase();
            eas.add(eA);
        }

        JSONArray resultArray = new JSONArray();
        JSPHelper.log.info("Checking " + eas.size() + " email address(es). ");
        out.println("<div style=\"text-align:center;position:relative;top:30px\">");
        try {
            Map<String, Set<Document>> matchesMap = EmailUtils.getDocsForEAs(archive.getAllDocsAsSet(), eas);
            Set<Document> allMatches = new LinkedHashSet<Document>();

            // set up json results array for data table
            // create one set of all docs that matched any email address
            for (String s: matchesMap.keySet()) {
                JSONArray j = new JSONArray();
                j.put (0, Util.escapeHTML(s));
                int matchCount = 0;
                Set<Document> set = matchesMap.get(s);
                if (set != null) {
                    matchCount = set.size();
                    allMatches.addAll(set);
                }
                j.put (1, matchCount);
	            resultArray.put (count++, j);
	        }


            // create a dataset out of the matched docs, needed so flags can be applied to all these docs if required by the user.
            DataSet dataset = new DataSet(allMatches, archive, datasetName, null, null, null, null);
            session.setAttribute (datasetName, dataset);
            session.setAttribute ("docs-" + datasetName, new ArrayList<Document>(allMatches));
           %>
        <div class="controls" style="position:relative;width:auto;display:inline-block">
            <div style="position:relative;padding:5px;">
                <i title="Do not transfer" id="doNotTransfer" class="flag fa fa-ban"></i>
                <i title="Transfer with restrictions" id="transferWithRestrictions" class="flag fa fa-exclamation-triangle"></i>
                <i title="Message Reviewed" id="reviewed" class="flag fa fa-eye"></i>

                <div style="display:inline;" id="annotation_div" style="z-index:1000;">
                    <input id="annotation" placeholder="Annotation" style="z-index:1000;width:20em;margin-left:25px"/>
                </div>
                <!--			<div style="display:inline-block;position:relative;top:10px"><input type="checkbox" id="applyToAll" style="margin-left:250px"/> Apply to all</div> -->
                <button type="button" class="btn btn-default" style="margin-left:25px;margin-right:25px;" id="apply">Apply to all <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
            </div>
        </div>
        <br/>
        <br/>
        <div style="margin:auto; width:900px">
            <table id="counts" style="display:none">
                <thead><th>Email address</th><th>Messages</th></thead>
                <tbody>
                </tbody>
            </table>
            <br/>
            <%=allMatches.size()%> message(s) matched.
        </div>
    <%
    } catch(Exception e){
        Util.print_exception("Exception while fetching messages for: " + eas, e, JSPHelper.log);
    }
    %>

             <script type="text/javascript">
                $(document).ready(function() {
                            var click_to_search = function (data, type, full, meta) {
                                // epadd.do_search will open search result in a new tab.
                                // Note, we have to insert onclick into the rendered HTML,
                                // we were earlier trying $('.search').click(epadd.do_search) - this does not work because only the few rows initially rendered to html match the $('.search') selector, not the others
                                return '<span style="cursor:pointer" onclick="epadd.do_search(event)">' + data + '</span>';
                            };

                            var counts = <%=resultArray.toString(4)%>;
                            $('#counts').dataTable({
                                data: counts,
                                pagingType: 'simple',
                                columnDefs: [{className: "dt-right", "targets": [1]}, {
                                    width: "630px",
                                    targets: 0
                                }, {targets: 0, render: click_to_search}],
                                order: [[1, 'desc']], // col 1 (entity message count), descending
                                fnInitComplete: function () { $('#spinner-div').hide();$('#counts').fadeIn();}
                            });
                        });

                $('.select_all_button').click(function(){$("input.ea").attr("checked",true);});
                $(".unselect_all_button").click(function(){$("input.ea").attr("checked",false);});

                var $spinner = $('.spinner');
                var fade_spinner_with_delay = function () {
                    $spinner.delay(500).fadeOut();
                };
                $("#apply").click(
                        function(){
                            post_data = {};

                            var dnt = $('#doNotTransfer').hasClass('flag-enabled');
                            var twr = $('#transferWithRestrictions').hasClass('flag-enabled');
                            var rev = $('#reviewed').hasClass('flag-enabled');
                            var ann = $('#annotation').val();
                            post_data.setDoNotTransfer = dnt ? "1" : "0";
                            post_data.setTransferWithRestrictions = twr ? "1" : "0";
                            post_data.setReviewed = rev ? "1" : "0";
                            post_data.setAnnotation = ann;
                            post_data.datasetId = '<%=datasetName%>';

                            url = "ajax/applyFlags.jsp";
                            $spinner.show();
                            $.ajax({
                                type: 'POST',
                                url: url,
                                datatype: 'json',
                                data: post_data,
                                success: function (data, textStatus, jqxhr) {
                                    fade_spinner_with_delay();
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
                        }
                );
                $('.flag').click (function(e) {
                    var $target = $(e.target);
                    $target.toggleClass('flag-enabled');
                });
                // on page unload, release dataset to free memory
                $(window).unload(function() {
                    epadd.log ('releasing dataset <%=datasetName%>');
                    $.get('ajax/releaseDataset.jsp?datasetId=<%=datasetName%>');
                });
                </script>


        <%
        out.println("</div>");
            }
>>>>>>> a2eab7412b81eeb8688c7528533ec626f266efb1
    %>

    <script>
<<<<<<< HEAD
        $('.select_all_button').click(
                function(){
                    $("input.ea").attr("checked",true);
                }
        );
        $(".unselect_all_button").click(
                function(){
                    $("input.ea").attr("checked",false);
                }
        );
        var $spinner = $('.spinner');
        var fade_spinner_with_delay = function () {
            $spinner.delay(500).fadeOut();
        };
        $("#apply").click(
                function(){
                    post_data = {};

                    var dnt = $('#doNotTransfer').hasClass('flag-enabled');
                    var twr = $('#transferWithRestrictions').hasClass('flag-enabled');
                    var rev = $('#reviewed').hasClass('flag-enabled');
                    var ann = $('#annotation').val();
                    post_data.setDoNotTransfer = dnt ? "1" : "0";
                    post_data.setTransferWithRestrictions = twr ? "1" : "0";
                    post_data.setReviewed = rev ? "1" : "0";
                    post_data.setAnnotation = ann;
                    post_data.datasetId = '<%=datasetName%>';
                    allDocs = '<%=request.getParameter("allDocs")%>';

                    url = "ajax/applyFlags.jsp";
                    $spinner.show();
                    $.ajax({
                        type: 'POST',
                        url: url,
                        datatype: 'json',
                        allDocs: allDocs,
                        data: post_data,
                        success: function (data, textStatus, jqxhr) {
                            fade_spinner_with_delay();
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
                }
        );
        $('.flag').click (function(e) {
            var $target = $(e.target);
            $target.toggleClass('flag-enabled');
        });
=======

>>>>>>> a2eab7412b81eeb8688c7528533ec626f266efb1
    </script>
</body>
</html>
