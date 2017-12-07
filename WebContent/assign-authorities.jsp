<%@page language="java" import="edu.stanford.muse.email.AddressBookManager.AddressBook"%>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" %>
<%@ page import="edu.stanford.muse.email.CorrespondentAuthorityMapper" %>
<%@ page import="edu.stanford.muse.email.AddressBookManager.Contact" %>
<%@ page import="com.google.gson.Gson" %>
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
      
	  $('html').addClass('js'); // see http://www.learningjquery.com/2008/10/1-way-to-avoid-the-flash-of-unstyled-content/

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
		  
//		  $('#save_button').click(save_authorities);
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
    String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
    CorrespondentAuthorityMapper cAuthorityMapper = archive.getCorrespondentAuthorityMapper();
    AddressBook addressBook = archive.getAddressBook();

    // get all the contacts
    List<Contact> contacts = addressBook.sortedContacts(new LinkedHashSet<>((List) archive.getAllDocs()));

    int rowType = edu.stanford.muse.webapp.HTMLUtils.getIntParam (request, "rowType", 1);

    // filter the contacts based on rowType
    List<CorrespondentAuthorityMapper.AuthorityInfo> rows = new ArrayList<>();
    for (Contact c: contacts) {
        String name = c.pickBestName();
        CorrespondentAuthorityMapper.AuthorityInfo info = cAuthorityMapper.getCorrespondentAuthorityInfo(archiveID,addressBook, name);

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
                showRow = info.confirmedAuthority != null && info.confirmedAuthority.fastId != CorrespondentAuthorityMapper.INVALID_FAST_ID;
                break;
            case 6:
                showRow = info.confirmedAuthority != null && info.confirmedAuthority.fastId == CorrespondentAuthorityMapper.INVALID_FAST_ID;
                break;
        }

        if (showRow)
            rows.add (info);
    }

%>

<script>
    var INVALID_FAST_ID = <%=CorrespondentAuthorityMapper.INVALID_FAST_ID%>; // Java to JS
</script>
<form>
    <div style="text-align:center">
        <div class="form-group" style="width:20%; margin-left:40%">
            <label for="rowType"></label>
            <select id="rowType" name="rowType" class="form-control selectpicker">
                <option  <%=(rowType == 1 ? "selected":"")%> value=1>Show unconfirmed rows with candidates</option>
                <option  <%=(rowType == 2 ? "selected":"")%> value=2>Show unconfirmed rows</option>
                <option  <%=(rowType == 3 ? "selected":"")%> value=3>Show all rows</option>
                <option  <%=(rowType == 4 ? "selected":"")%> value=4>Show confirmed rows</option>
                <option  <%=(rowType == 5 ? "selected":"")%> value=5>Show rows with confirmed authorities</option>
                <option  <%=(rowType == 6 ? "selected":"")%> value=6>Show rows confirmed to have no authorities</option>
            </select>
        </div>
    </div>
</form>

<script>
    $('#rowType').change (function() { window.location = 'assign-authorities?archiveID=<%=archiveID%>&rowType=' + $('#rowType').val();});
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
        var clickable_message = function (data, type, full) {
            return '<a target="_blank" title="' + (full.tooltip ? full.tooltip : '') + '" href="' + full.url + '">' + full.name + '</a>';
        };

        var render_nMessages = function(data, type, full, meta) {
            return full.nMessages;
        };

        var render_candidate = function(full, authRecord, name) {
            // get the name first (preferredLabel, with alt labels on hover
            var result = '<div class="candidate" data-fast-id="' + authRecord.fastId + '" data-name="' + name + '">';
            {
                result += '<input class="candidate-checkbox" type="checkbox" ' + (full.isConfirmed ? 'checked':'') + '/> ';
                if (authRecord.altLabels)
                    result += '<span title="' + authRecord.altLabels + '">' + authRecord.preferredLabel + '</span>';
                else
                    result += authRecord.preferredLabel;

                if (authRecord.extent)
                    result += ' (' + authRecord.extent + ')';

                result += '<br/>';
            }

            // get the line with the actual authority ids
            {
                result += '<div class="authority-ids">';
                if (authRecord.fastId && authRecord.fastId != INVALID_FAST_ID) {
//                    result += '<a href="http://id.worldcat.org/fast/' + candidate.fastId + '">FAST: ' + candidate.fastId + '</a>';
                    result += '<span class="auth-record"><i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="http://id.worldcat.org/fast/' + authRecord.fastId + '">FAST</a></span>';
                }
                result += ' ';
                if (authRecord.viafId && authRecord.viafId != '?') {
//                    result += ' <a href="https://viaf.org/viaf/' + candidate.viafId + '">VIAF: ' + candidate.viafId + '</a>';
                    result += ' <span class="auth-record"> <i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="https://viaf.org/viaf/' + authRecord.viafId + '">VIAF</a> </span>';
                }
                if (authRecord.wikipediaId && authRecord.wikipediaId != '?') {
                    result += ' <span class="auth-record"><i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="https://en.wikipedia.org/wiki/' + authRecord.wikipediaId + '">Wikipedia: ' + authRecord.wikipediaId + '</a></span>';
                }
                if (authRecord.lcnafId && authRecord.lcnafId != '?') {
 //                   result += ' <a href="http://id.loc.gov/authorities/names/' + candidate.lcnafId + '">LCNAF: ' + candidate.lcnafId + '</a>';
                    result += ' <span class="auth-record"><i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="http://id.loc.gov/authorities/names/' + authRecord.lcnafId + '">LCNAF</a></span>';
                }
                if (authRecord.lcshId && authRecord.lcshId != '?') {
//                    result += ' <a href="http://id.loc.gov/authorities/subject/' + candidate.lcshId + '">LCNAF: ' + candidate.lcshId + '</a>';
                    result += ' <span class="auth-record"><i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="http://id.loc.gov/authorities/subject/' + authRecord.lcshId + '">LCSH</a></span>';
                }
                if (authRecord.localId && authRecord.localId != '?') {
//                    result += ' <a href="http://id.loc.gov/authorities/subject/' + candidate.lcshId + '">LCNAF: ' + candidate.lcshId + '</a>';
                    result += ' <span class="auth-record"><i class="fa fa-check" aria-hidden="true"></i> <a target="_blank" href="#">Local ID: ' + authRecord.localId + '</a></span>';
                }
                result += '</div><br/>';
            }
            result += '</div>';
            return result;
        };

        var render_manual_assign_option = function (record) {
            return '<div class="candidate" data-name="' + record.name + '"><a class="manual-assign-link" href="#"><i class="fa fa-plus"/> Assign an authority</a></div>';
        };

        var render_candidates_col = function(data, type, full, meta) {
            var result = '<div data-name="' + full.name + '" class="all-candidates">';
            if (full.isConfirmed && full.confirmedAuthority) {
                if (full.confirmedAuthority.fastId != INVALID_FAST_ID)
                    result += render_candidate (full, full.confirmedAuthority, full.name);
                else
                    result += render_candidate (full, {fastId: INVALID_FAST_ID, preferredLabel: 'No authority record'}, full.name);
            }
            else if (full.candidates) {
                for (var i = 0; i < full.candidates.length; i++) {
                    var candidate = full.candidates[i];
                    var result_for_this_candidate = render_candidate (full, candidate, full.name);
                    result += result_for_this_candidate;
                    result += '<hr/>';
                }
                // render the no-auth-record option
                result += render_candidate (full, {fastId: INVALID_FAST_ID, preferredLabel: 'No authority record'}, full.name);
                result += '<hr/>';
                result += render_manual_assign_option (full);
            }
            result += '</div>';
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
                    {targets: 2, render: render_candidates_col}], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
            fnInitComplete: function() { $('#spinner-div').hide(); $('#people').fadeIn(); }
        });

        $('.candidate-checkbox').change(function(e) {
            var $div_cand = $(e.target).closest('div.candidate');
            var fastId = $div_cand.attr('data-fast-id');
            var name = $div_cand.attr('data-name');
            var data = {name: name, fastId: fastId};
            if (!$(e.target).is(':checked')) {
                // must be unchecked
                data.unset = false;
            };
            data.archiveID = '<%=archiveID%>';

            $.ajax({
                url: 'ajax/confirm-authority.jsp',
                data: data,
                success: function () {
                    $spinner.removeClass('fa-spin');
                },
                error: function () {
                    $spinner.removeClass('fa-spin');
                    epadd.alert('Unable to save fast ID, sorry!');
                }
            });
        });

        $('.manual-assign-link').click(function(e) {
            var $div_cand = $(e.target).closest('div.candidate');
            var name = $div_cand.attr('data-name');
            $('#manual-assign-name').html (name);
            $('#manual-assign-modal').modal('show');
        });

        $('#manual-assign-submit').click (function() {
            var data = {name: $('#manual-assign-name').text(), fastId: $('#fastId').val(), viafId: $('#viafId').val(), wikipediaId: $('#wikipediaId').val(), lcnafId: $('#lcnafId').val(), lcshId: $('#lcshId').val(),
                localId: $('#localId').val(), isManualAssign: true, archiveID:'<%=archiveID%>'}

            $.ajax({
                url: 'ajax/confirm-authority.jsp',
                data: data,
                success: function () {
                  //  $spinner.removeClass('fa-spin');
                    $('#manual-assign-modal').modal('hide');

                },
                error: function () {
                   // $spinner.removeClass('fa-spin');
                    epadd.alert('Unable to save authority, sorry!');
                    $('#manual-assign-modal').modal('hide');
                }
            });
        });


    } );


</script>


<!-- Modal -->
<div class="modal fade" id="manual-assign-modal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <!-- Modal Header -->
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">
                    <span aria-hidden="true">&times;</span>
                    <span class="sr-only">Close</span>
                </button>
                <h4 class="modal-title" id="myModalLabel">Assign authority record for <span id="manual-assign-name"></span></h4>
            </div>

            <!-- Modal Body -->
            <div class="modal-body">

                <form role="form">
                    <div class="form-group">
                        <label for="fastId">FAST Id</label>
                        <input type="text" class="form-control" id="fastId" name="fastId" placeholder="e.g., 61561"/>
                    </div>

                    <div class="form-group">
                        <label for="viafId">VIAF Id</label>
                        <input type="text" class="form-control" id="viafId" name="viafId" placeholder="e.g., 66552944"/>
                    </div>

                    <div class="form-group">
                        <label for="fastId">Wikipedia Id</label>
                        <input type="text" class="form-control" id="wikipediaId" name="wikipediaId" placeholder="e.g., Thomas_Edison"/>
                    </div>

                    <div class="form-group">
                        <label for="fastId">LoC Named Authority File Id</label>
                        <input type="text" class="form-control" id="lcnafId" name="lcnafId" placeholder="e.g., n80126308"/>
                    </div>

                    <div class="form-group">
                        <label for="fastId">LoC Subject Headings Id</label>
                        <input type="text" class="form-control" id="lcshId" name="lcshId" placeholder="e.g., sh95009459"/>
                    </div>

                    <div class="form-group">
                        <label for="fastId">Local Id</label>
                        <input type="text" class="form-control" id="localId" name="localId" placeholder=""/>
                    </div>


                </form>
            </div>

            <!-- Modal Footer -->
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                <button id="manual-assign-submit" type="button" class="btn btn-default">Save</button>
            </div>
        </div>
    </div>
</div>



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
 <jsp:include page="footer.jsp"/>
 
</body>
</html>
