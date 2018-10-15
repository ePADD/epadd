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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<head>
    <title>Entities</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="css/sidebar.css">
    <link rel="stylesheet" href="css/main.css">

    <script src="js/jquery.js"></script>
    <script src="js/jquery.dataTables.min.js"></script>

    <!-- Optional theme -->
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/modernizr.min.js"></script>
    <script src="js/sidebar.js"></script>

    <script src="js/epadd.js"></script>
    <style type="text/css">
        .js #entities {display: none;}
    </style>

</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>


<div class="nav-toggle1 sidebar-icon">
    <img src="images/sidebar.png" alt="sidebar">
</div>
<nav class="menu1" role="navigation">
    <h2><b>Browsing Entity Types</b></h2>
    <!--close button-->
    <a class="nav-toggle1 show-nav1" href="#">
        <img src="images/close.png" class="close" alt="close">
    </a>

    <div class="search-tips" style="display:block">
        ePADD recognizes fine-grained entities within the text message subjects and bodies.
        <br/><br/>

        This screen lists a selection of entity types bootstrapped from DBpedia, along with the number of entities associated with each type.
        <br/><br/>

        Select an entity type to view a list of all associated entities.
        <br/><br/>

    </div>
</nav>

<%writeProfileBlock(out, archive, "Entity types");%>

<%
    Map<Short, String> desc = new LinkedHashMap<>();
    for(NEType.Type t: NEType.Type.values())
        desc.put(t.getCode(), t.getDisplayName());

    String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
    JSONArray resultArray = new JSONArray();
    int count = 0;
    for(Short type: desc.keySet()){
        JSONArray j = new JSONArray();
//        if(NEType.Type.OTHER.getCode() == type || NEType.Type.PERSON.getCode() == type || desc.get(type)==null)
        if(NEType.Type.OTHER.getCode() == type || desc.get(type)==null)
            continue;
        j.put(0, "<a href='list-entities?type="+type+"&archiveID="+archiveID+"' target='_blank'>"+desc.get(type)+"</a>");
        j.put(1, archive.collectionMetadata.entityCounts.getOrDefault(type,0));
        resultArray.put(count++, j);
    }

%>
<div style="margin:auto; width:900px">
    <div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>
    <table id="entities">
        <thead><th>Entity Type</th><th># entities</th></thead>
        <tbody>
        </tbody>
    </table>
</div>

<script type="text/javascript">
    $(document).ready(function() {

        var entities = <%=resultArray.toString(4)%>;
        $('#entities').dataTable({
            data: entities,
            //pagingType: 'simple',
            paging: false,
            columnDefs: [{ className: "dt-right", "targets": [ 1 ] },{width: "600px", targets: 0},{targets: 0}],
            order:[[1, 'desc']], // col 1 (entity message count), descending
            fnInitComplete: function() { $('#spinner-div').hide(); $('#entities').fadeIn(); }
        });
    } );
</script>

<jsp:include page="footer.jsp"/>
</body>
</html>
