<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="au.com.bytecode.opencsv.CSVReader" %>
<%@ page import="java.util.*" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
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
    <script>
        submit = function(){
            window.location = "bulkredaction.jsp?filePath="+$("#filePath").val();
        }
    </script>
    <% String filePath = request.getParameter("filePath");

        if(filePath==null || !(new File(filePath)).exists()){%>
    <div style="text-align:center;position:relative;top:30px">
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
        out.println("<div style=\"text-align:center;position:relative;top:30px\">");
        out.println("Read " + eas.size() + " email address(es) from the file<br>");
        try {

            Set<Document> matches = EmailUtils.getDocsForEAs(archive.getAllDocsAsSet(), eas);
            request.setAttribute("selectDocs", matches);
            out.println(matches.size() + " messages matched<br>");
        }catch(Exception e){
            Util.print_exception("Exception while fetching messages for: "+eas,e, JSPHelper.log);
        }
        %>
        <div class="controls" style="position:relative;width:100%;">

            <div style="position:relative;padding:5px;">
                <i title="Do not transfer" id="doNotTransfer" class="flag fa fa-ban"></i>
                <i title="Transfer with restrictions" id="transferWithRestrictions" class="flag fa fa-exclamation-triangle"></i>
                <i title="Message Reviewed" id="reviewed" class="flag fa fa-eye"></i>

                <div style="display:inline;" id="annotation_div" style="z-index:1000;">
                    <input id="annotation" placeholder="Annotation" style="z-index:1000;width:20em;margin-left:25px"/>
                </div>
                        <!--			<div style="display:inline-block;position:relative;top:10px"><input type="checkbox" id="applyToAll" style="margin-left:250px"/> Apply to all</div> -->
                <button type="button" class="btn btn-default" style="margin-left:25px;margin-right:25px;" id="apply">Apply <img class="spinner" style="height:14px;display:none" src="images/spinner.gif"></button>
            </div>

        </div>
        <%
        out.println("</div>");
            }
    %>
    <script>
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
    </script>
</body>
</html>
