<%@ page import="org.json.JSONArray" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="java.util.*" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="getArchive.jspf" %>

<!--
Browse page for entities based on fine types
-->
<%
    Map<Short, String> desc = new LinkedHashMap<>();
    for(NEType.Type t: NEType.Type.values())
        desc.put(t.getCode(), t.getDisplayName());

    Short type = Short.parseShort(request.getParameter("type"));
    String entityType = desc.get(type);

%>
<!DOCTYPE HTML>
<html>


<head>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/main.css">

    <script src="js/jquery.js"></script>
    <script src="js/jquery.dataTables.min.js"></script>
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
<%@include file="header.jspf"%>
<title><%=entityType%> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "list-entities.head-entities")%> </title>
    <script>epadd.nav_mark_active('Browse');</script>
    <%writeProfileBlock(out, archive, edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "list-entities.manage-list") + entityType + " " + edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "list-entities.manage-entities") );%>


    <div class="nav-toggle1 sidebar-icon">
        <img src="images/sidebar.png" alt="sidebar">
    </div>
    <nav class="menu1" role="navigation">
        <h2><b> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "list-entities.help.head")%> </b></h2>
        <!--close button-->
        <a class="nav-toggle1 show-nav1" href="#">
            <img src="images/close.png" class="close" alt="close">
        </a>

        <div class="search-tips" style="display:block">
            <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "list-entities.help")%>

        </div>
    </nav>

    <div style="margin:auto; width:1100px">

        <div id="spinner-div" style="text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>
        <%
            //out.println("<h1>Entity Type: " + entityType + "</h1></br></br>");
            out.flush();
            //JSONArray resultArray = archive.getEntitiesInfoJSON(type);
            JSONArray resultArray = archive.getEntityBookManager().getEntityBookForType(type).getInfoAsJSON();

        %>

        <div class="button_bar_on_datatable">
                <div title=' <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "list-entities.download-all") + entityType + edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "list-entities.entities-as-csv")%>' class="buttons_on_datatable" onclick="exportEntityHandler()"><img class="button_image_on_datatable" src="images/download.svg"></div>
            <%if(!ModeConfig.isDiscoveryMode()){%>
                <div title="Edit entities" class="buttons_on_datatable" onclick="window.location='edit-entities?archiveID=<%=archiveID%>&type=<%=type%>'"><img class="button_image_on_datatable" src="images/edit_lexicon.svg"></div>
            <%}%>
        </div>

        <table id="entities" style="display:none">
            <thead><th> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "list-entities.entity")%> </th><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "list-entities.score")%></th><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "list-entities.messages")%></th></thead>
            <tbody>
            </tbody>
        </table>

        <br/>
        <br/>
        <br/>
        <br/>
        <br/>
        <br/>
    </div>
<%--<div style="text-align:center">
    <button class="btn btn-default" onclick="window.location='edit-entities?archiveID=<%=archiveID%>&type=<%=type%>'">Edit Entities</button>
</div>--%>
    <script type="text/javascript">
        var exportEntityHandler=function() {
            var entityType=<%=type%>;
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
                var archiveID='<%=archiveID%>';
            var click_to_search = function ( data, type, full, meta ) {
                // epadd.do_search will open search result in a new tab.
                // Note, we have to insert onclick into the rendered HTML,
                // we were earlier trying $('.search').click(epadd.do_search) - this does not work because only the few rows initially rendered to html match the $('.search') selector, not the others
//                return '<span title="' + full[3] + '" style="cursor:pointer" onclick="epadd.do_entity_search(event,archiveID)">' + data + '</span>';
                return '<a target="_blank" title="' + escapeHTML(full[3]) + '"  onclick=\"epadd.do_entity_search(event,archiveID)\"">' + escapeHTML(data) + '</a>'; // full[4] has the URL, full[5] has the title tooltip
            };

            var entities = <%=resultArray.toString(4)%>;
            $('#entities').dataTable({
                data: entities,
                pagingType: 'simple',
                autoWidth: false,
                columnDefs: [
                        { className: "dt-right", targets: 1},
                        { className: "dt-right", targets: 2},
                    { width: "600px", targets: 0},
                        { render:click_to_search, targets: 0},
                        { render: function(data,type,row){return Math.round(row[1]*1000)/1000}, targets:1}],
                order:[[1, 'desc'], [2, 'desc']], // col 1 (entity message count), descending
                fnInitComplete: function() { $('#spinner-div').hide(); $('#entities').fadeIn(); }
            });


        } );
    </script>


    <jsp:include page="footer.jsp"/>
</body>
</html>
