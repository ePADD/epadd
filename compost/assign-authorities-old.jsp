<%@page language="java" import="edu.stanford.muse.ie.EntityFeature"%>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" %>
<%@page language="java" %>
<%@include file="../WebContent/getArchive.jspf" %>

<html>
<head>
	<title>Processing: Assign authorities</title>
	<link rel="icon" type="image/png" href="../WebContent/images/epadd-favicon.png">
	<script src="../../muse/WebContent/js/jquery.js"></script>
	<script src="../../muse/WebContent/js/jquery.dataTables.min.js"></script>

	<link rel="stylesheet" href="../WebContent/bootstrap/dist/css/bootstrap.min.css">
	<link href="../../muse/WebContent/css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	<link rel="stylesheet" href="../WebContent/js/fancyBox/source/jquery.fancybox.css" type="text/css" media="screen" />
	<jsp:include page="../../muse/WebContent/css/css.jsp"/>
	<link rel="stylesheet" href="../WebContent/css/sidebar.css">
	<link href="../WebContent/css/suggester.css" rel="stylesheet" />


	<script type="text/javascript" src="../WebContent/bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="../WebContent/js/jquery.mockjax.js" type="text/javascript"></script>
	<script src="../WebContent/js/jquery.autocomplete.js" type="text/javascript"></script>
	<script src="../WebContent/js/modernizr.min.js"></script>
	<script src="../WebContent/js/sidebar.js"></script>

	<script src="../../muse/WebContent/js/muse.js"></script>
	<script src="../WebContent/js/epadd.js"></script>
	

	<script src="../WebContent/js/utils.js" type="text/javascript"></script>
	<script type='text/javascript' src='../WebContent/js/jquery.qtip-1.0.js'></script>
	<script type="text/javascript" src="../WebContent/js/fancyBox/source/jquery.fancybox.js"></script>

	<style type="text/css">
      .js #table {display: none;}
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
		  $('#stats').html('Saving authority records... <img style="height:15px" src="../../muse/WebContent/images/spinner.gif"/>');

			$.ajax({type: 'POST',
				dataType: 'json',
				url: 'ajax/updateAuthorityRecords.jsp-',
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

      //@arg randId - a randum id that identifies an element in the row
      sort = function(randId) {
          rowNo = $("#"+randId).closest("tr").index();
          epadd.log("Row no: "+rowNo);
          //because we get a 0 indexed row number
          rowNo++;
          selector = "tr:eq(" + rowNo + ") td:eq(2)";
          dbsHtml = $(selector);
          ck = $(selector).find("input[type='checkbox']")[0];
          var url = document.URL;
          name = $("tr:eq(" + rowNo + ") td:eq(0)").text();
          if (typeof(s) !== "undefined") s = s.substr(1, s.length - 2);
          context = $(selector).attr("data-context");//aData[3];
          if (!$(selector).find(".loading"))
              $(selector).append("<img style='height:15px' class='loading' src='../../muse/WebContent/images/spinner.gif'/>");
          //dont place the call when the context is empty.
          //Never pass html as a parameter, I have learnt it the hardest way possible
          //for instance I was passing html in the last column of a row as a param to an ajax call,
          //the html that is being passed as parameter spilled all over and gave me a very cryptic
          // error message: (TypeError: 'click' called on an object that does not implement interface HTMLElement)
          //which took me one good day to figure out what the problem was
          if ((typeof(ck) !== "undefined") && (!ck.checked)) {
              $(selector).find(".loading").fadeIn();
              epadd.log("rn: " + rowNo);
              $.ajax({
                  type: "POST",
                  url: "./ajax/sortsources.jsp",
                  contentType: "application/x-www-form-urlencoded;charset=utf-8",
                  dataType: "text",
                  data: {
                      "entity": name,
                      "type": type,
                      "rowno": rowNo
                  },
                  success: function (data, status, jqXHR) {
                      selector = "tr:eq(" + rowNo + ") td:eq(2)";
                      console.log("Received response: " + data + " for rowNo: " + rowNo);
                      epadd.log(data);
                      $(selector).html(data);
                      $(selector).find(".loading").hide();
                  },
                  beforeSend: function (jqXHR, settings) {
                      jqXHR.rowNo = settings.rowNo;
                  },
                  error: function (jqXHR, exception, error) {
                      epadd.log("Error while sorting: #" + rowNo + " row. Exception: " + exception + " Error:" + error);
                      $("tr:eq(" + rowNo + ") td:eq(2)").find(".loading").hide();
                      return null;
                  }
              });
          }
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
<%
	String type = request.getParameter("type");
	if (!"correspondent".equals(type))
		type = "all";
%>
<jsp:include page="../WebContent/header.jspf"/>
<script>epadd.nav_mark_active('Authorities');</script>
<script type="text/javascript" src="../../muse/WebContent/js/statusUpdate.js"></script>
<%@include file="../WebContent/div_status.jspf"%>

<!--sidebar content-->
<!--
<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>

<nav class="menu1" role="navigation">
	<h2>Assign authorities</h2>
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<div class="search-tips">
		<img src="images/pharse.png" alt="">
		<p>
			Text from Josh here.
		</p>
	</div>

	<div class="search-tips">
		<img src="images/requered.png" alt="">
		<p>
			More text
		</p>
	</div>


</nav>
-->
<!--/sidebar-->

<script>
	bi=0;
	ei=<%=edu.stanford.muse.Config.MAX_TRY_TO_RESOLVE_NAMES %>;
	
	getNext = function(numEntries){
		var temp = bi;
		bi = ei;
		ei = temp+numEntries;
		fetchTableEntries(bi,ei);
	};
	
	getNext = function(){
		numEntries = parseInt($("#numLoad").val());
		bi = ei;
		ei = bi+numEntries;
		fetchTableEntries(bi,ei);
	};
	
	createIndex = function(){
		params = "";
		page = "ajax/checkFeaturesIndex.jsp";
		epadd.log(page+params);
		
		//supplying the ready function to make it not redirect to the other page and give us the handle of the response data.  
		fetch_page_with_progress(page, "status", document.getElementById('status'), document.getElementById('status_text'), params, null, "assign-authorities-old.jsp?type="+type);
	};

	showing = 0;
	//beginIndex and endIndex of teh entries to be fetched.
	fetchTableEntries = function(beginIndex,endIndex){
		ready = function(j){
			var t = $('#table').DataTable();
			status = j["status"];
            if(status === "0") {
                var info = j["info"];
                epadd.log(info);
                $("#table").html("<span style='color:red'>"+info+"</span>");
                return;
            }
            total = j["total"];
			ei = j["endIndex"];
			j = j["data"];
			showing += (endIndex-beginIndex);

			for(var i=0;i<j.length;i++){
				contexts = j[i]["contexts"];
				classes = j[i]["classes"];
				values = j[i]["values"];
				rows = "";
				//link the name to browse page (search)
				//name is in the index 0
				values[0] = '<a target="_blank" href=\'browse?term="'+values[0]+'"\'>'+values[0]+'</a>';
				epadd.log("Value: "+values[0]);
				for(var k=0;k<contexts.length;k++){
					row = $("<td>").append(values[k]);
					$(row).attr("data-context",contexts[k]);
					$(row).attr("class",classes[k]);
				    rows += row[0].outerHTML;
				    if(k<(contexts.length-1))
				    	rows+=",";
				}
				jRow = $('<tr>').append(rows);
				t.row.add(jRow).draw();
			}
			html = "";
			featureExists = <%=EntityFeature.indexExists(archive)%>;
			if(featureExists==false)
				html += "<button class='btn-default' onclick='createIndex()'><i class=\"fa fa-tags\"></i> Enable Disambiguation</button><br/> (Initializing this feature may take around 30-60 minutes. Once it completes, authority options will be shown in decreasing order of probability.)<br>";
			html += "Found "+showing+" possible authority matches in "+total+" entities. You can find " + "<select id='numLoad'><option value='"+(total-showing)+"'>All</option><option value='100'>Next 100</option><option value='10'>Next 10</option></select>"+
				" possible matches by clicking <button onclick='getNext()' class='btn-default'>here</button><br/><br/>";
				
			$("#extra").html(html);			

		};
		params = encodeURI("type="+type+"&db="+"freebase"+"&test="+"false"+"&bi="+bi+"&ei="+ei);
		page = "ajax/getTableEntries.jsp";
		epadd.log(page+params);
		
		//supplying the ready function to make it not redirect the other page and give us the handle of the response data.  
		fetch_page_with_progress(page, "status", document.getElementById('status'), document.getElementById('status_text'), params, ready, null);	
	};
    fetchTableEntries(bi,ei);
</script>

<%
	AddressBook ab = archive.addressBook;
	String testStr = request.getParameter("test");
	Boolean test = false;
	try{
		test = Boolean.parseBoolean(testStr);
	}catch(Exception e){
		e.printStackTrace();
	}

	writeProfileBlock(out, archive, "", "Assign authority records");
	out.println ("<br/>");

	Integer numEntities = null;
	type = request.getParameter("type");
	if(type == null)
		type = "correspondent";
%>

	<div id='extra' style='text-align:center;'></div>
	<div style="margin:auto; width:1000px">
	<table id="table" class="authorities-table">
		<thead><tr><th><%="correspondent".equals(type) ? "Correspondent":"Entity"%></th><th>Messages</th><th>Possible matches</th>
	<%
		if(test)
			out.println("<th>Accuracy</th>");
			
		out.println("</tr></thead><tbody>");
		out.flush();
		//KillPhrases kill = new KillPhrases();
	%>
	</tbody>
	</table>
	<br/>
	<br/>
	
	<div style="width:100%;text-align:center">
		<button class='btn btn-cta' id="save_button">Save <i class="icon-arrowbutton"></i></button>
	</div>
	
	<div id="stats"></div>

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
	
 <jsp:include page="../../muse/WebContent/footer.jsp"/>
 
</body>
</html>
