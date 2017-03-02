<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.ie.EntityFeature"%>
<%@page language="java" %>
<%@ page import="edu.stanford.muse.ie.AuthorityMapper" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@ page import="edu.stanford.muse.email.Contact" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="com.google.gson.Gson" %>
<%@ page import="edu.stanford.muse.webapp.*" %>
<%@ page import="java.util.*" %>

<html>
<head>
	<title>Processing: Assign authorities</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<link rel="stylesheet" href="js/fancyBox/source/jquery.fancybox.css" type="text/css" media="screen" />
    <link rel="stylesheet" href="css/sidebar.css">
	<jsp:include page="css/css.jsp"/>

    <script src="js/jquery.js"></script>
    <script src="js/jquery.mockjax.js" type="text/javascript"></script>
    <script src="js/jquery.autocomplete.js" type="text/javascript"></script>
    <script src="js/jquery.dataTables.min.js"></script>
    <script type='text/javascript' src='js/jquery.qtip-1.0.js'></script>
    <script type="text/javascript" src="js/fancyBox/source/jquery.fancybox.js"></script>

	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>

	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>

	<script src="js/utils.js" type="text/javascript"></script>	

	<style type="text/css">
      .js table {display: none; font-size: 14px; vertical-align: top;}
      .candidate { margin-top: 10px; margin-left:30px;}
      hr { margin-top: 0px; margin-bottom: 0px; opacity: 0.3;}

      .search {cursor:pointer;}
      .wikiPage{
      	width: 350px;
      	display: inline-block;
      }
      .fancybox-wrap {
      	 position: fixed !important;
  		 top: 100px !important;
	  }
	  tr.aa{
	 	padding-top: 5px;
		padding-bottom: 5px;
	}
	.fancybox-overlay{
		background-color: rgba(0, 0, 0, 0.57);
	}
    .autocomplete-suggestions {
        top: 210.2px !important;
    }

        .authority-ids {
            font-size: 12px;
            color: #aaa;
            margin-top: 10px;
        }
        .auth-record {margin-left: 10px; }
    </style>
    <!-- fancybox is failing to compute the right top value on this page.-->
    
    <script>
      qtipreinitialise = function(){
    	  $('[title][title!=]').qtip({
				hide:{ delay:'200', fixed:true },
				style: { width: "357px", padding: "10px", color: 'black', textAlign: 'left', border: {width: 5, radius: 5, color: '#0175bc'}, name: 'light'}
    	  });
      };    
      
	  $('html').addClass('js'); // see http://www.learningjquery.com/2008/10/1-way-to-avoid-the-flash-of-unstyled-content/
	  var save_authorities = function(e) {
		  obj = {};
		  obj2 = [];
		  names = [];
		  //also send all unchecked names
		  $('input[type="checkbox"]:checked').each(function(idx, inp) {
			  var $tr = $(inp).closest('tr');
			  var name = $('td.name', $tr).text();
			  var idS = $(inp).attr('data-ids');
			  var dbTypeS = $(inp).attr('data-dbtypes');
			  var ids = idS.split(":::");
			  var dbTypes = dbTypeS.split(":::");
			  obj[name] = [ids,dbTypes];
			  names.push(name);
		  });
		  $('input[type="checkbox"]:not(:checked)').each(function(idx,inp){
			  var $tr = $(inp).closest('tr');
			  var name = $('td.name', $tr).text();
			  if(names.indexOf(name)==-1)
			  	obj2.push(name);
		  });
		  
		  $('#stats').css('color', 'inherit');
		  $('#stats').html('Saving authority records... <img style="height:15px" src="images/spinner.gif"/>');

			$.ajax({type: 'POST',
				dataType: 'json',
				url: 'ajax/updateAuthorityRecords.jsp', 
				data: {authorities: JSON.stringify(obj), reverted: JSON.stringify(obj2) },
				cache: false,
				success: function (response, textStatus) {
						if (response && (response.status == 0)) {
							var status = 'Success! ' + response.message;
							$('#stats').text(status);
							$('#stats').css('color', 'green');
						}
						else {
							if (response)
								$('#stats').text('Error! Code ' + response.status + ', Message ' + response.error);
							else
								$('#stats').text('Error! No response from ePADD.');
							$('#stats').css('color', 'red');
						}
					$('img[src="images/spinner.gif').hide();
					$('.fa-spinner').hide();
				},
				error: function() { 
					epadd.alert ("Sorry, something went wrong. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report this to epadd_project@stanford.edu."); 
					$('img[src="images/spinner.gif').hide(); // hide the spinner otherwise it continues on the page even after this crash
					$('.fa-spinner').hide();
				}});
			return false; /* to suppress other button actions */
	 };

	  $(document).ready(function() {
		  $('.search').click(epadd.do_search);
		  renderedRows = [];
		  oTable = $('#table')
		  		.on( 'page.dt',   function () { qtipreinitialise(); } )
		  		.dataTable({
                      pagingType: 'simple',
                      //"aoColumnDefs": [{ "bVisible": false, "aTargets": [3] }],
                      "fnDrawCallback": function (oSettings) {
                          qtipreinitialise();
                          $('a.popupManual').fancybox({'hideOnContentClick': true});
                      },
                      "fnRowCallback": function (nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                          qtipreinitialise();
                          //This check is necessary as this event is triggered every time the row content is replaced.
                          //				  if(renderedRows.indexOf(nRow)>-1)
                          //					  return;
                          //				  renderedRows.push(nRow);
                      }
                  });

		  oTable.fnSort( [ [1,'desc'] ] );
		  $('#table').fadeIn();
		  
		  $('#save_button').click(save_authorities);
		  return false;
		});
		
		if(document.URL.indexOf("?")>-1){
			var paramStr = document.URL.split("?")[1];
			var params = paramStr.split("&");
			for(var i=0;i<params.length;i++){
				if(params[i].indexOf("type")>-1){
					type = params[i].split("=")[1];
					//remove any non word chars at the end.
					type = type.replace(/^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$/g,"");
					epadd.log("Found type: "+type);
				}
			}
		}
	</script>
</head>
<body>
<%@include file="getArchive.jspf" %>

<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Authorities');</script>

<script type="text/javascript" src="js/statusUpdate.js"></script>
<%@include file="div_status.jspf"%>

<%
    String type = request.getParameter("type");
    if (!"correspondent".equals(type))
        type = "all";
    AuthorityMapper authorityMapper = archive.getAuthorityMapper();
    authorityMapper.setupCounts(archive);
    AddressBook addressBook = archive.getAddressBook();

    List<Contact> contacts = addressBook.sortedContacts(new LinkedHashSet<>((List) archive.getAllDocs()));

    int rowType = edu.stanford.muse.webapp.HTMLUtils.getIntParam (request, "rowType", 1);

    List<AuthorityMapper.AuthorityInfo> rows = new ArrayList<>();
    for (Contact c: contacts) {
        String name = c.pickBestName();
        AuthorityMapper.AuthorityInfo info = authorityMapper.getAuthorityInfo(name);

        boolean showRow = false;
        switch (rowType) {
            case 1:
                showRow = (!info.isConfirmed && !Util.nullOrEmpty (info.candidates));
                break;
            case 2:
                showRow = !info.isConfirmed;
                break;
            case 3:
                showRow = true;
                break;
            case 4:
                showRow = info.isConfirmed;
                break;
            case 5:
                showRow = info.confirmedAuthority != null && info.confirmedAuthority.fastId != AuthorityMapper.INVALID_FAST_ID;
                break;
            case 6:
                showRow = info.confirmedAuthority != null && info.confirmedAuthority.fastId == AuthorityMapper.INVALID_FAST_ID;
                break;
        }

        if (showRow)
            rows.add (info);
    }

%>

<form>
    <div style="text-align:center">
        <div class="form-group" style="width:20%; margin-left:40%">
            <label for="rowType"></label>
            <select id="rowType" name="rowType" class="form-control selectpicker">
                <option  <%=(rowType == 1 ? "selected":"")%> value=1>Show unconfirmed rows with candidates</option>
                <option  <%=(rowType == 2 ? "selected":"")%> value=2>Show all unconfirmed rows</option>
                <option  <%=(rowType == 3 ? "selected":"")%> value=3>Show all rows</option>
                <option  <%=(rowType == 4 ? "selected":"")%> value=4>Show all confirmed rows</option>
                <option  <%=(rowType == 5 ? "selected":"")%> value=5>Show rows with confirmed authorities</option>
                <option  <%=(rowType == 6 ? "selected":"")%> value=6>Show rows confirmed to have no authorities</option>
            </select>
        </div>
    </div>
</form>

<script>
    $('#rowType').change (function() { window.location = 'assign-authorities?rowType=' + $('#rowType').val();});
</script>

<br/>

<div style="margin:auto; min-width:1000px;max-width:1200px;padding:50px;">
    <table id="people" style="display:none">
        <thead><th>Name</th><th>Messages</th><th>Authorities</th></thead>
        <tbody>
        </tbody>
    </table>
</div>

<script>
    var tableEntries = <%=new Gson().toJson(rows)%>;
    // get the href of the first a under the row of this checkbox, this is the browse url, e.g.
    $(document).ready(function() {
        var clickable_message = function (data, type, full, meta) {
            return '<a target="_blank" title="' + (full.title ? full.title : '') + '" href="' + full.url + '">' + full.name + '</a>';
        };

        var render_nMessages = function(data, type, full, meta) {
            return full.nMessages;
        };

        var render_candidate = function(candidate, name) {
            var result = '<div class="candidate" data-fast-id="' + candidate.fastId + '" data-name="' + name + '">';
            {
                if (candidate.altLabels)
                    result += '<span title="' + candidate.altLabels + '">' + candidate.preferredLabel + '</span>';
                else
                    result += candidate.preferredLabel;

                result += '<br/>';
            }
            {
                result += '<div class="authority-ids">';
                if (candidate.fastId) {
//                    result += '<a href="http://id.worldcat.org/fast/' + candidate.fastId + '">FAST: ' + candidate.fastId + '</a>';
                    result += '<span class="auth-record"><i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="http://id.worldcat.org/fast/' + candidate.fastId + '">FAST</a></span>';
                }
                result += ' ';
                if (candidate.viafId && candidate.viafId != '?') {
//                    result += ' <a href="https://viaf.org/viaf/' + candidate.viafId + '">VIAF: ' + candidate.viafId + '</a>';
                    result += ' <span class="auth-record"> <i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="https://viaf.org/viaf/' + candidate.viafId + '">VIAF</a> </span>';
                }
                if (candidate.wikipediaId && candidate.wikipediaId != '?') {
                    result += ' <span class="auth-record"><i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="https://en.wikipedia.org/wiki/' + candidate.wikipediaId + '">Wikipedia: ' + candidate.wikipediaId + '</a></span>';
                }
                if (candidate.lcnafId && candidate.lcnafId != '?') {
 //                   result += ' <a href="http://id.loc.gov/authorities/names/' + candidate.lcnafId + '">LCNAF: ' + candidate.lcnafId + '</a>';
                    result += ' <span class="auth-record"><i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="http://id.loc.gov/authorities/names/' + candidate.lcnafId + '">LCNAF</a></span>';
                }
                if (candidate.lcshId && candidate.lcshId != '?') {
//                    result += ' <a href="http://id.loc.gov/authorities/subject/' + candidate.lcshId + '">LCNAF: ' + candidate.lcshId + '</a>';
                    result += ' <span class="auth-record"><i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="http://id.loc.gov/authorities/subject/' + candidate.lcshId + '">LCSH</a></span>';
                }
                result += '</div><br/>';
            }
            result += '</div>';
            return result;
        };

        var render_candidates = function(data, type, full, meta) {
            var result = '';
            if (full.candidates) {
                for (var i = 0; i < full.candidates.length; i++) {
                    var candidate = full.candidates[i];
                    var result_for_this_candidate = render_candidate (candidate, full.name);
                    result += result_for_this_candidate;
                    if (i < full.candidates.length-1)
                        result += '<hr/>';
                }
            }

            return result;
        }

        $('#people').dataTable({
            data: tableEntries,
            // pagingType: 'simple',
            order:[[1, 'desc']], // col 12 (outgoing message count), descending
            columnDefs: [
                    {width: "300px", targets: 0},
                    {className: "dt-right", "targets": 1 },
                    {targets: 0, render:clickable_message},
                    {targets: 1, render: render_nMessages},
                    {targets: 2, render: render_candidates}], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
            fnInitComplete: function() { $('#spinner-div').hide(); $('#people').fadeIn(); }
        });
    } );


</script>


	<div style="display:none">
		<div id="manualassign" style='width:400px;'>
			<div class="header">Assign authority records</div>
			<br/>
			<table style='width:100%;border-collapse: separate;border-spacing: 5px;'>
				<tr class='aa'>
					<td>Name </td>
					<td>
						<input type='text' name='name' id='manual_name' size="40" placeholder='Start typing a name'/>
						<input type="text" name='name' class='suggest' id="autocomplete-ajax-x" disabled="disabled" style="color: #CCC; position: absolute; background: transparent; z-index: 1;display:block"/>
					 </td>
				</tr>
				<tr class='aa'><td title="Faceted Application of Subject Terminology - OCLC">FAST </td><td><input type='text' id='fast' size="20" placeholder='Ex: 363849'/></td></tr>
				<tr class='aa'><td title="Virtual International Authority File">VIAF </td>   <td><input type='text' id='viaf' size="20" placeholder='Ex: 307436254'/></td></tr>
				<tr class='aa'><td title="DBpedia">DBpedia </td>   <td><input type='text' id='dbpedia' size="20" placeholder='Ex: George_W._Bush'/></td></tr>
				<tr class='aa'><td title="LoC Subject Headings">LoC-subject </td>   <td><input type='text' id='locSubject' size="20" placeholder='Ex: sh85098119'/></td></tr>
				<tr class='aa'><td title="LoC Named Authority File">LCNAF    </td>   <td><input type='text' id='locName' size="20" placeholder='Ex: n2014212161'/></td></tr>
				<tr class='aa'><td title="Freebase">Freebase    </td>   <td><input type='text' id='freebase' size="20" placeholder='Ex: /m/03cpmgr'/></td></tr>
			</table>
			<br>
			<div style='text-align:center'>
				<button id="assignauthority" class="btn btn-default" onclick='manual_assign()'>
					<i class="fa fa-tag"></i> Assign Authority <i style="display:none" class="fa fa-spinner fa-spin manual-spinner"></i>
				</button>
			</div>
		</div>
	</div>

	<br/>
    <script>    
    	//by default query parameter is initialised weith the text content and be sent.
    	//if needs to send more data use params field
	    $('#manual_name').autocomplete({
		    serviceUrl: 'ajax/getFASTMatches.jsp',
			onSearchError: function (query, jqXHR, textStatus, errorThrown) {epadd.log(textStatus+" error: "+errorThrown);},
			preventBadQueries: false,
			showNoSuggestionNotice: true,
			preserveInput: true,
			ajaxSettings: {
				"timeout":3000,
				dataType: "json"
			},
			dataType: "text",
			params: {"type": type},
			//100ms
			deferRequestsBy: 100,
			onSelect: function(suggestion) {
		        $("#fast").val(suggestion.fastID);
				$("#manual_name").val(suggestion.name);
		    },
		    onHint: function (hint) {
		       $('#autocomplete-ajax-x').val(hint);
		    },
		    onInvalidateSelection: function() {
		       epadd.log('You selected: none');
		    }
		 });
	
		qtipreinitialise();
	</script>
	
 <jsp:include page="footer.jsp"/>
 
</body>
</html>
