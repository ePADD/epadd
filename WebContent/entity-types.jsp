<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@include file="getArchive.jspf" %>

<%--
  Created by IntelliJ IDEA.
  User: vihari
  Date: 12/12/15
  Time: 22:20
--%>
<!DOCTYPE HTML>
<html>

<script src="js/jquery.js"></script>

<head>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/main.css">

    <%-- jquery was present here earlier --%>
    <script src="js/jquery.dataTables.min.js"></script>

    <!-- Optional theme -->
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/modernizr.min.js"></script>
    <script src="js/sidebar.js"></script>

    <script src="js/muse.js"></script>
    <script src="js/epadd.js"></script>
    <style type="text/css">
        /*.js #entities {display: none;}*/
    </style>

</head>
<body>
<%-- The file header.jspf was present here --%>
<%@include file="header.jspf"%>
<title><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "entity-types.head-entities")%></title>


<div class="nav-toggle1 sidebar-icon">
    <img src="images/sidebar.png" alt="sidebar">
</div>
<nav class="menu1" role="navigation">
    <h2><b><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "entity-types.help.head")%></b></h2>
    <!--close button-->
    <a class="nav-toggle1 show-nav1" href="#">
        <img src="images/close.png" class="close" alt="close">
    </a>

    <div class="search-tips" style="display:block">
       <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "entity-types.help")%>

    </div>
</nav>

<%writeProfileBlock(out, archive, edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "entity-types.head-entities") );%>

<%
    Map<Short, String> desc = new LinkedHashMap<>();
    for(NEType.Type t: NEType.Type.values())
        desc.put(t.getCode(), t.getDisplayName());

    JSONArray resultArray = new JSONArray();
    int count = 0;
    for(Short type: desc.keySet()){
        JSONArray j = new JSONArray();
//        if(NEType.Type.OTHER.getCode() == type || NEType.Type.PERSON.getCode() == type || desc.get(type)==null)
        if(NEType.Type.OTHER.getCode() == type || desc.get(type)==null)
            continue;
        j.put(0, "<a href='list-entities?type="+type+"&archiveID="+archiveID+"' target='_blank'>"+Util.escapeHTML(desc.get(type))+"</a>");
        j.put(1, archive.collectionMetadata.entityCounts.getOrDefault(type,0));
        resultArray.put(count++, j);
    }

%>
<div style="margin:auto; width:1100px;">
    <div class="button_bar_on_datatable">
        <div title="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "entity-types.download-in-csv-file")%>" class="buttons_on_datatable" onclick="exportAllEntitiesHandler()"><img class="button_image_on_datatable" src="images/download.svg"></div>
       <%--This second div added just for formatting purposes.. Therefore made it hidden. If we remove this div then the alignment of download buttons goes missing.
       This is because of the way in which class buttons_on_datatable has been defined. It requires at least two divs to be formatted nicely. Fix is later.--%>
        <div title="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "entity-types.download-as-csv")%>" class="buttons_on_datatable" style="display:none" onclick="exportCorrespondentHandler()"><img class="button_image_on_datatable" src="images/download.svg"></div>
    </div>
    <table id="entities" style="display:none;">
        <thead><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "entity-types.entity-type")%></th><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "entity-types.entities")%></th></thead>
        <tbody>
        </tbody>
    </table>
</div>
<div id="spinner-div" style="text-align:center;"> <img style="height:20px" src="images/spinner.gif"/></div>


<script type="text/javascript">
    var exportAllEntitiesHandler=function() {
        var entityType=<%=Short.MAX_VALUE%>; //Look the 'entites' section of downloadData.jsp to check that 'all' entity type is denoted by passing this value from the front end.
        var option;
        var msg;

        var post_params={archiveID:archiveID, data: "entities",type:entityType};
        var params = epadd.convertParamsToAmpersandSep(post_params);
        fetch_page_with_progress("ajax/downloadData.jsp", "status", document.getElementById('status'), document.getElementById('status_text'), params);

        /*$.ajax({
            type: 'POST',
            url: "ajax/downloadData.jsp",
            data: {archiveID: archiveID, data: "entities",type:entityType},
            dataType: 'json',
            success: function (data) {
                epadd.alert('List of selected entities will be downloaded in your download folder!', function () {
                    window.location=data.downloadurl;
                });
            },
            error: function (jq, textStatus, errorThrown) {
                var message = ("Error Exporting file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
                epadd.log(message);
                epadd.alert(message);
            }
        });*/
    };
    $(document).ready(function() {

        var entities = <%=resultArray.toString(4)%>;
        $('#entities').dataTable({
            data: entities,
            pagingType: 'simple',
            //paging: true,
            columnDefs: [{ className: "dt-right", "targets": [ 1 ] },{width: "400px", targets: 0}],
            order:[[1, 'desc']], // col 1 (entity message count), descending
            fnInitComplete: function() { $('#spinner-div').hide(); $('#entities').fadeIn(); }
        });
    } );
</script>

<jsp:include page="footer.jsp"/>
</body>
</html>
