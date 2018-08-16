<%@ page import="org.json.JSONArray" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="java.util.*" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="getArchive.jspf" %>

<!--
Browse page for entities based on fine types
-->
<html>
<head>
    <title>Entities</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <jsp:include page="css/css.jsp"/>
    <link href="css/epadd.css" rel="stylesheet" type="text/css"/>

    <script src="js/jquery.js"></script>
    <script src="js/jquery.dataTables.min.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="js/statusUpdate.js"></script>
    <script src="js/muse.js"></script>
    <script src="js/epadd.js"></script>
    <style type="text/css">
        .js #entities {display: none;}
    </style>
</head>
<body>
    <%@include file="div_status.jspf"%>
    <jsp:include page="header.jspf"/>
    <script>epadd.nav_mark_active('Browse');</script>

<div style="margin:auto; width:900px">
    <div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>
    <%
        Map<Short, String> desc = new LinkedHashMap<>();
        String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
        for(NEType.Type t: NEType.Type.values())
            desc.put(t.getCode(), t.getDisplayName());

        Short type = Short.parseShort(request.getParameter("type"));
        out.println("<h1>Type: "+desc.get(type)+"</h1>");
        JSONArray resultArray = archive.getEntitiesInfoJSON(type);
    %>

<table id="entities" style="display:none">
    <thead><th>Entity</th><th>Score</th><th>Messages</th></thead>
    <tbody>
    </tbody>
</table>

    <br/>
    <br/>
    <br/>

<div style="text-align:center">
    <button class="btn btn-default" onclick="window.location='edit-entities?archiveID=<%=archiveID%>&type=<%=type%>'">Edit Entities</button>
</div>
    <script type="text/javascript">
            $(document).ready(function() {
                var archiveID='<%=archiveID%>';
            var click_to_search = function ( data, type, full, meta ) {
                // epadd.do_search will open search result in a new tab.
                // Note, we have to insert onclick into the rendered HTML,
                // we were earlier trying $('.search').click(epadd.do_search) - this does not work because only the few rows initially rendered to html match the $('.search') selector, not the others
                return '<span title="' + full[3] + '" style="cursor:pointer" onclick="epadd.do_search(event,archiveID)">' + data + '</span>';
            };

            var entities = <%=resultArray.toString(4)%>;
            $('#entities').dataTable({
                data: entities,
                pagingType: 'simple',
                columnDefs: [
                        { className: "dt-right", targets: 1},
                        { width: "600px", targets: 0},
                        { render:click_to_search, targets: 0},
                        { render: function(data,type,row){return Math.round(row[1]*1000)/1000}, targets:1}],
                order:[[1, 'desc'], [2, 'desc']], // col 1 (entity message count), descending
                fnInitComplete: function() { $('#spinner-div').hide(); $('#entities').fadeIn(); }
            });
        } );
    </script>

    <br/>
    <br/>
    <br/>
    <jsp:include page="footer.jsp"/>
</body>
<%!

%>