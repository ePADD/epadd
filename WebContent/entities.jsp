<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@page language="java" import="edu.stanford.muse.index.IndexUtils"%>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="edu.stanford.muse.util.Span" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="java.util.*" %>
<%@include file="getArchive.jspf" %>

<!-- Input: Field name
     Renders a table containing the list of entities 
     corresponding to this field
-->
<!DOCTYPE HTML>
<html>
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
    String type=request.getParameter("type");
	String et = "";
    Short ct = NEType.Type.PERSON.getCode();
	if("en_person".equals(type)||"person".equals(type)) {
        et = "Person";
        ct = NEType.Type.PERSON.getCode();
    }
	else if("en_loc".equals(type)||"place".equals(type)) {
        et = "Location";
        ct = NEType.Type.PLACE.getCode();
    }
	else if("en_org".equals(type)||"organisation".equals(type)) {
        et = "Organisation";
        ct = NEType.Type.ORGANISATION.getCode();
    }
	writeProfileBlock(out, archive, et + " entities", "");
			%>

<div style="text-align:center;display:inline-block;vertical-align:top;margin-left:170px">
	<button class="btn-default" onclick="window.location = 'graph?view=entities&type=<%=type%>';"><i class="fa fa-bar-chart-o"></i> Go To Graph View</button>
</div>
<br/>


<div style="margin:auto; width:900px">

	<div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

<%
		out.flush();

	Map<String, Integer> counts = new LinkedHashMap<>();
    Map<String, String> canonicalToOriginal = new LinkedHashMap<>();

    double cutoff = 0.001;
    Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
    for (EmailDocument ed: docs) {
        Span[] es = archive.getEntitiesInDoc(ed,true);
        List<Span> est = new ArrayList<>();
        for(Span e: es)
            if(NEType.getCoarseType(e.type).getCode() == ct)
                est.add(e);

        Span[] fes = edu.stanford.muse.ie.Util.filterEntitiesByScore(est.toArray(new Span[est.size()]),cutoff);
        //filter the entities to remove obvious junk
        fes = edu.stanford.muse.ie.Util.filterEntities(fes);
	    // note that entities could have repetitions.
	    // so we create a *set* of entities, but after canonicalization.
	    // canonical to original just uses an arbitrary (first) occurrence of the entity
        Set<String> canonicalEntities = new LinkedHashSet<String>();
        for (Span esp: fes) {
            String e = esp.getText();
            String canonicalEntity = IndexUtils.canonicalizeEntity(e);
            if (canonicalToOriginal.get(canonicalEntity) == null)
                canonicalToOriginal.put(canonicalEntity, e);
            canonicalEntities.add(canonicalEntity);
        }

        for (String ce: canonicalEntities)
        {
            Integer I = counts.get(ce);
            counts.put(ce, (I == null) ? 1 : I+1);
        }
    }

//	Contact ownContact = ab.getContactForSelf();
//    List<Contact> allContacts = ab.sortedContacts((Collection) docs);
//    Map<Contact, Integer> contactInCount = new LinkedHashMap<Contact, Integer>(), contactOutCount = new LinkedHashMap<Contact, Integer>(), contactMentionCount = new LinkedHashMap<Contact, Integer>();
%>
<%
    List<Pair<String, Integer>> pairs = Util.sortMapByValue(counts);
    int MAX_DEFAULT_RECORDS = 100000;
    int max = HTMLUtils.getIntParam(request, "max", MAX_DEFAULT_RECORDS);
    int count = 0;
    JSONArray resultArray = new JSONArray();
	for (Pair<String, Integer> p: pairs) {
        if (++count > max)
            break;
        String entity = p.getFirst();
        String entityToPrint = canonicalToOriginal.get(entity);
        JSONArray j = new JSONArray();
        j.put (0, Util.escapeHTML(entityToPrint));
        j.put (1, counts.get(entity));

        resultArray.put (count-1, j);
    }
%>
<table id="entities" style="display:none">
	<thead><th>Entity</th><th>Messages</th></thead>
	<tbody>
	</tbody>
</table>
</div>
<p>
<br/>

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
				columnDefs: [{ className: "dt-right", "targets": [ 1 ] },{width: "630px", targets: 0},{targets: 0, render:click_to_search}],
				order:[[1, 'desc']], // col 1 (entity message count), descending
				fnInitComplete: function() { $('#spinner-div').hide(); $('#entities').fadeIn(); }
			});
		} );
	</script>

	<jsp:include page="footer.jsp"/>
</body>
</html>
