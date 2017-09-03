<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.ner.Entity" %>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="edu.stanford.muse.util.Span" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="java.util.*" %>
<%@ page import="edu.stanford.muse.ie.variants.EntityMapper" %>
<%@ page import="java.util.stream.Collectors" %>
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
        String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
        for(NEType.Type t: NEType.Type.values())
            desc.put(t.getCode(), t.getDisplayName());

        Short type = Short.parseShort(request.getParameter("type"));
        out.println("<h1>Type: "+desc.get(type)+"</h1>");
        Map<String,Entity> entities = new LinkedHashMap();
        double theta = 0.001;
        EntityMapper entityMapper = archive.getEntityMapper();

        for (Document doc: archive.getAllDocs()){
//            Span[] es = archive.getEntitiesInDoc(doc,true);
            Span[] es1 = archive.getEntitiesInDoc(doc,true);
            Span[] es2 =  archive.getEntitiesInDoc(doc,false);
            Set<Span> ss = Arrays.stream(es1).collect(Collectors.toSet());
            Set<Span> ss1 = Arrays.stream(es2).collect(Collectors.toSet());
            ss.addAll(ss1);
            Set<String> seenInThisDoc = new LinkedHashSet<>();

            for (Span span: ss) {
                if (span.type != type || span.typeScore<theta)
                    continue;

                String name = span.getText();
                String displayName = name;

                //  map the name to its display name. if no mapping, we should get the same name back as its displayName
                if (entityMapper != null)
                    displayName = entityMapper.getDisplayName(name, span.type);

                displayName = displayName.trim();

                if (seenInThisDoc.contains(displayName.toLowerCase()))
                    continue; // count an entity in a doc only once

                seenInThisDoc.add (displayName.toLowerCase());

                //fixed: Here entities map was keeping the keys without lowercase conversion as a result
                // two entities which are same but differ only in their display name case (lower/upper) were
                // being identified as separate entities. As a result the count in listing page was shown differently from the
                //count on the processing metadata page (by a difference of 30 or so). This fix was done after
                //the fix to handle a large difference in person entities count. For that refer to JSPHelper.java
                //file fetchAndIndex method.
                if (!entities.containsKey(displayName.toLowerCase()))
                    entities.put(displayName.toLowerCase(), new Entity(displayName, span.typeScore));
                else
                    entities.get(displayName.toLowerCase()).freq++;
            }
        }

        Map<Entity, Double> vals = new LinkedHashMap<>();
        for(Entity e: entities.values()) {
            vals.put(e, e.score);
            //System.err.println("Putting: "+e+", "+e.score);
        }
        List<Pair<Entity,Double>> lst = Util.sortMapByValue(vals);

        JSONArray resultArray = new JSONArray();
         int count = 0;
	    for (Pair<Entity, Double> p: lst) {
	        count++;
            String entity = p.getFirst().entity;
            JSONArray j = new JSONArray();

            Set<String> altNamesSet = entityMapper.getAltNamesForDisplayName(entity, type);
            String altNames = (altNamesSet == null) ? "" : "Alternate names: " + Util.join (altNamesSet, ";");
            j.put (0, Util.escapeHTML(entity));
            j.put (1, (float)p.getFirst().score);
            j.put (2, p.getFirst().freq);
            j.put (3, altNames);

            resultArray.put (count-1, j);
        }
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
            var click_to_search = function ( data, type, full, meta ) {
                // epadd.do_search will open search result in a new tab.
                // Note, we have to insert onclick into the rendered HTML,
                // we were earlier trying $('.search').click(epadd.do_search) - this does not work because only the few rows initially rendered to html match the $('.search') selector, not the others
                return '<span title="' + full[3] + '" style="cursor:pointer" onclick="epadd.do_search(event,<%=archiveID%>)">' + data + '</span>';
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