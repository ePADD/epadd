<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="org.json.JSONArray" %>
<%@include file="getArchive.jspf" %>

<!-- Input: Field name
     Renders a table containing the list of entities 
     corresponding to this field
-->
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
%>

<!DOCTYPE HTML>
<html>
<head>
	<title>Entitiess</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<script src="js/jquery.js"></script>
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<script src="js/jquery.dataTables.min.js"></script>
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<!-- Optional theme -->
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js?v=1.1"></script>
	<style type="text/css">
		.js #entities {display: none;}
	</style>

</head>
<body>
<%@include file="header.jspf"%>

    <%
	writeProfileBlock(out, archive, et + " entities");
			%>

<div style="text-align:center;display:inline-block;vertical-align:top;margin-left:170px">
	<button class="btn-default" onclick="window.location = 'graph?archiveID=<%=archiveID%>&view=entities&type=<%=type%>';"><i class="fa fa-bar-chart-o"></i> Go To Graph View</button>
</div>
<br/>


<div style="margin:auto; width:900px">

	<div id="spinner-div" style="text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>

<%
		out.flush();

//	Contact ownContact = ab.getContactForSelf();
//    List<Contact> allContacts = ab.sortedContacts((Collection) docs);
//    Map<Contact, Integer> contactInCount = new LinkedHashMap<Contact, Integer>(), contactOutCount = new LinkedHashMap<Contact, Integer>(), contactMentionCount = new LinkedHashMap<Contact, Integer>();
    int MAX_DEFAULT_RECORDS = 100000;
    int max = HTMLUtils.getIntParam(request, "max", MAX_DEFAULT_RECORDS);
    JSONArray entityinfo = archive.getEntityBookManager().getEntityBookForType(ct).getInfoAsJSON();//getEntitiesCountAsJSON(ct,max);
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

			var entities = <%=entityinfo.toString(4)%>;
			$('#entities').dataTable({
				data: entities,
				pagingType: 'simple',
				autoWidth: false,
				columnDefs: [{ className: "dt-right", "targets": [ 1 ] },{width: "630px", targets: 0},{targets: 0, render:click_to_search}],
				order:[[2, 'desc']], // col 2 (entity message count), descending
				fnInitComplete: function() { $('#spinner-div').hide(); $('#entities').fadeIn(); }
			});
		} );
	</script>

	<jsp:include page="footer.jsp"/>
</body>
</html>
<%!

%>