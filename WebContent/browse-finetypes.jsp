<%@ page import="edu.stanford.muse.ner.featuregen.FeatureDictionary" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.json.JSONArray" %>
<%@include file="getArchive.jspf" %>

<%--
  Created by IntelliJ IDEA.
  User: vihari
  Date: 12/12/15
  Time: 22:20
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<head>
    <title>Entities</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <script src="js/jquery.js"></script>
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <script src="js/jquery.dataTables.min.js"></script>
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <jsp:include page="css/css.jsp"/>
    <script src="js/epadd.js"></script>
    <style type="text/css">
        .js #entities {display: none;}
    </style>

</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>


<%
    Map<Short, String> desc = new LinkedHashMap<>();
    desc.put(FeatureDictionary.PERSON, "Person");
    desc.put(FeatureDictionary.COMPANY, "Company");
    desc.put(FeatureDictionary.BUILDING, "Building");
    desc.put(FeatureDictionary.PLACE, "Place");
    desc.put(FeatureDictionary.RIVER, "River");
    desc.put(FeatureDictionary.ROAD, "Road");
    desc.put(FeatureDictionary.UNIVERSITY, "University");
    desc.put(FeatureDictionary.MOUNTAIN, "Mountain");
    desc.put(FeatureDictionary.AIRPORT, "Airport");
    desc.put(FeatureDictionary.ORGANISATION, "Organization");
    desc.put(FeatureDictionary.PERIODICAL_LITERATURE, "Periodical Literature");
    desc.put(FeatureDictionary.ISLAND, "Island");
    desc.put(FeatureDictionary.MUSEUM, "Museum");
    desc.put(FeatureDictionary.BRIDGE, "Bridge");
    desc.put(FeatureDictionary.AIRLINE, "Airline");
    desc.put(FeatureDictionary.GOVAGENCY, "Government Agency");
    desc.put(FeatureDictionary.HOSPITAL, "Hospital");
    desc.put(FeatureDictionary.AWARD, "Award");
    desc.put(FeatureDictionary.THEATRE, "Theatre");
    desc.put(FeatureDictionary.LIBRARY, "Library");
    desc.put(FeatureDictionary.LAWFIRM, "Law Firm");
    desc.put(FeatureDictionary.MONUMENT, "Monument");
    desc.put(FeatureDictionary.DISEASE, "Disease");
    desc.put(FeatureDictionary.EVENT, "Event");

    JSONArray resultArray = new JSONArray();
    int count = 0;
    for(Short type: FeatureDictionary.allTypes){
        JSONArray j = new JSONArray();
        if(FeatureDictionary.OTHER == type || desc.get(type)==null)
            continue;
        j.put(0, "<a href='finetypes?type="+type+"' target='_blank'>"+desc.get(type)+"</a>");
        j.put(1, archive.processingMetadata.entityCounts.get(type));
        resultArray.put(count++, j);
    }

%>
<div style="margin:auto; width:900px">
    <div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>
    <table id="entities">
        <thead><th>Semantic Type</th><th># entities</th></thead>
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
