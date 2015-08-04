<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="au.com.bytecode.opencsv.CSVReader" %>
<%@ page import="java.util.*" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%--
  Created by IntelliJ IDEA.
  User: vihari
  Date: 04/08/15
  Time: 15:04
  To change this template use File | Settings | File Templates.
--%>
<!-- No use with archive on this page, but for the profile block -->
<%@include file="getArchive.jspf" %>

<html>
<head>
    <title>Bulk Redaction</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <script src="js/jquery.js"></script>
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <script src="js/jquery.dataTables.min.js"></script>
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <jsp:include page="css/css.jsp"/>
    <script src="js/epadd.js"></script>

</head>
<body>
    <% String filePath = request.getParameter("filePath");
        System.err.println(filePath);

        if(filePath==null || !(new File(filePath)).exists()){%>
    <div style="align:center">
        Please provide the path of file with bulk entries
        <input type="text" placeholder="CSV file path" id="filePath"/>
        <button class="btn-default" onclick="submit()">Submit</button>
    </div>
    <%}else {
        //read the entries in the file
        CSVReader reader = new CSVReader(new FileReader(filePath));
        Set<String> eas = new LinkedHashSet<String>();
        String[] line;
        while ((line = reader.readNext()) != null) {
            String eA = line[0].trim();
            eas.add(eA);
        }
        out.println(eas.size() + " email addresses matched<br>");
        try {

            Set<Document> matches = EmailUtils.getDocsForEAs(archive.getAllDocsAsSet(), eas);
            request.setAttribute("selectDocs", matches);
            //edu.stanford.muse.util.Pair<Integer, Map<String,Integer>> matches = archive.getNumMatchingDocs(eas);
            out.println(matches.size() + " messages matched<br>");
        }catch(Exception e){
            e.printStackTrace();
        }
        %>
        <div style="display:inline-block;vertical-align:top;">
            <div class="browse_message_area rounded shadow;position:relative" style="width:1020px;min-height:400px">
                <div class="controls" style="position:relative;width:100%;">

                    <div style="position:relative;float:left;padding:5px;">
                        <i title="Do not transfer" id="doNotTransfer" class="flag fa fa-ban"></i>
                        <i title="Transfer with restrictions" id="transferWithRestrictions" class="flag fa fa-exclamation-triangle"></i>
                        <i title="Message Reviewed" id="reviewed" class="flag fa fa-eye"></i>

                        <div style="display:inline;" id="annotation_div" style="z-index:1000;">
                            <input id="annotation" placeholder="Annotation" style="z-index:1000;width:20em;margin-left:25px"/>
                        </div>
                        <!--			<div style="display:inline-block;position:relative;top:10px"><input type="checkbox" id="applyToAll" style="margin-left:250px"/> Apply to all</div> -->
                        <button type="button" class="btn btn-default" style="margin-left:25px;margin-right:25px;" id="apply">Apply <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
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
            </div>
        </div>
        <%
            }
    %>
    <script>
        $('.select_all_button').click(
                function(){
                    $("input.ea").attr("checked",true);
                }
        );
        $(".unselect_all_button").clck(
                function(){
                    $("input.ea").attr("checked",false);
                }
        );
        var $spinner = $('.spinner', $(e.target));
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
        )
    </script>
</body>
</html>
