<%@ page import="java.util.*" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.ner.Entity" %>
<%@ page import="edu.stanford.muse.ner.NER" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="edu.stanford.muse.ner.featuregen.FeatureDictionary" %>
<%@ page import="edu.stanford.muse.util.Span" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!--	
    Use this page to train and recognise fine grainied entity types, 
    this is part of experimental module and not properly tested 
    across many archives
-->

<link href="css/epadd.css" rel="stylesheet" type="text/css"/>
<script src="js/jquery.js"></script>
<script src="js/muse.js"></script>
<script src="js/epadd.js"></script>
<script type="text/javascript" src="js/statusUpdate.js"></script>
<%@include file="div_status.jspf"%>
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

<div style="margin:auto; width:900px">
    <div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>
    <%
        Map<Short, String> desc = new LinkedHashMap<>();
        desc.put(FeatureDictionary.PERSON,"PERSON");desc.put(FeatureDictionary.COMPANY,"COMPANY");desc.put(FeatureDictionary.BUILDING,"BUILDING");
        desc.put(FeatureDictionary.PLACE,"PLACE");desc.put(FeatureDictionary.RIVER,"RIVER");desc.put(FeatureDictionary.ROAD,"ROAD");
        desc.put(FeatureDictionary.UNIVERSITY,"UNIVERSITY");desc.put(FeatureDictionary.MOUNTAIN,"MOUNTAIN");desc.put(FeatureDictionary.AIRPORT,"AIRPORT");
        desc.put(FeatureDictionary.ORGANISATION,"ORGANISATION");desc.put(FeatureDictionary.PERIODICAL_LITERATURE,"PERIODICAL_LITERATURE");
        desc.put(FeatureDictionary.ISLAND,"ISLAND");desc.put(FeatureDictionary.MUSEUM,"MUSEUM");desc.put(FeatureDictionary.BRIDGE,"BRIDGE");
        desc.put(FeatureDictionary.AIRLINE,"AIRLINE");desc.put(FeatureDictionary.GOVAGENCY,"GOVAGENCY");desc.put(FeatureDictionary.HOSPITAL,"HOSPITAL");
        desc.put(FeatureDictionary.AWARD,"AWARD");desc.put(FeatureDictionary.THEATRE,"THEATRE");desc.put(FeatureDictionary.LEGISTLATURE,"LEGISTLATURE");
        desc.put(FeatureDictionary.LIBRARY,"LIBRARY");desc.put(FeatureDictionary.LAWFIRM,"LAWFIRM");desc.put(FeatureDictionary.MONUMENT,"MONUMENT");
        desc.put(FeatureDictionary.DISEASE,"DISEASE");desc.put(FeatureDictionary.EVENT,"EVENT");
        Short type = Short.parseShort(request.getParameter("type"));
        out.println("<h1>Type: "+desc.get(type)+"</h1>");
        Archive archive = JSPHelper.getArchive(session);
        Map<String,Entity> entities = new LinkedHashMap();
        double theta = 0.001;
        for(Document doc: archive.getAllDocs()){
            Span[] es = NER.getEntities(archive.getLuceneDoc(doc),true);
            for(Span e: es) {
                if(e==null || e.type!=type)
                    continue;
                double s = e.typeScore;
                if(s<theta)
                    continue;
                if (!entities.containsKey(e.text))
                    entities.put(e.text, new Entity(e.text, s));
                else
                    entities.get(e.text).freq++;
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
        url = url.substring(0,url.indexOf("/finetypes"));
        int count = 0;
	    for (Pair<Entity, Double> p: lst) {
	        count++;
//            if (++count > max)
//                break;
            String entity = p.getFirst().entity;
            JSONArray j = new JSONArray();
            j.put (0, "<a target='_blank' href='"+url+"/browse?term=\""+entity+"\"'>"+entity+"</a>");
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
    <script type="text/javascript">
        $(document).ready(function() {

            var entities = <%=resultArray.toString(4)%>;
            $('#entities').dataTable({
                data: entities,
                pagingType: 'simple',
                columnDefs: [{ className: "dt-right", "targets": [ 1 ] },{width: "600px", targets: 0},{targets: 0},
                    {render:function(data,type,row){return Math.round(row[1]*1000)/1000}, targets:[1]}],
                order:[[1, 'desc'], [2, 'desc']], // col 1 (entity message count), descending
                fnInitComplete: function() { $('#spinner-div').hide(); $('#entities').fadeIn(); }
            });
        } );
    </script>

    <jsp:include page="footer.jsp"/>
</body>