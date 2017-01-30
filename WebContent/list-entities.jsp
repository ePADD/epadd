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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!--
Browse page for enbtities based on fine types
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
        for(NEType.Type t: NEType.Type.values())
            desc.put(t.getCode(), t.getDisplayName());

        Short type = Short.parseShort(request.getParameter("type"));
        out.println("<h1>Type: "+desc.get(type)+"</h1>");
        Archive archive = JSPHelper.getArchive(session);
        Map<String,Entity> entities = new LinkedHashMap();
        double theta = 0.001;
        for(Document doc: archive.getAllDocs()){
            Span[] es = archive.getEntitiesInDoc(doc,true);
            Set<String> seenInThisDoc = new LinkedHashSet<>();

            for(Span sp: es) {
                String e = sp.getText();
                if(sp.type!=type || sp.typeScore<theta)
                    continue;
                if (seenInThisDoc.contains (e.toLowerCase().trim()))
                    continue;
                seenInThisDoc.add (e.toLowerCase().trim());

                if (!entities.containsKey(e))
                    entities.put(e, new Entity(e, sp.typeScore));
                else
                    entities.get(e).freq++;
            }
        }
        Map<Entity, Double> vals = new LinkedHashMap<>();
        for(Entity e: entities.values()) {
            vals.put(e, e.score);
            //System.err.println("Putting: "+e+", "+e.score);
        }
        List<Pair<Entity,Double>> lst = Util.sortMapByValue(vals);

        JSONArray resultArray = new JSONArray();
        String url = request.getRequestURL().toString();
        int count = 0;
	    for (Pair<Entity, Double> p: lst) {
	        count++;
            String entity = p.getFirst().entity;
            JSONArray j = new JSONArray();

            j.put (0, Util.escapeHTML(entity));
            j.put (1, (float)p.getFirst().score);
            j.put (2, p.getFirst().freq);

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
    <button class="btn btn-default" onclick="window.location='edit-entities?type=<%=request.getParameter ("type")%>'">Edit Entities</button>
</div>
    <script type="text/javascript">
        $(document).ready(function() {
            var click_to_search = function ( data, type, full, meta ) {
                // epadd.do_search will open search result in a new tab.
                // Note, we have to insert onclick into the rendered HTML,
                // we were earlier trying $('.search').click(epadd.do_search) - this does not work because only the few rows initially rendered to html match the $('.search') selector, not the others
                return '<span style="cursor:pointer" onclick="epadd.do_search(event)">' + data + '</span>';
            };

            var entities = <%=resultArray.toString(4)%>;
            $('#entities').dataTable({
                data: entities,
                pagingType: 'simple',
                columnDefs: [{ className: "dt-right", "targets": [ 1 ] },{width: "600px", targets: 0},{targets: 0, render:click_to_search},
                    {render:function(data,type,row){return Math.round(row[1]*1000)/1000}, targets:[1]}],
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