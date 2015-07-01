<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.ModeConfig"%>
<!DOCTYPE HTML>
<html lang="en">
<head>
	<title>ePADD Settings</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>
	<script src="js/stacktrace.js"></script>
	<style>
		#advanced_options button {width:250px;}
	</style>
</head>
<body style="color:gray;">
<jsp:include page="header.jspf"/>
<p>
<script>
	epadd.select_link('#nav1', 'Settings');
</script>

	<div style="margin-left:5%">
	<p>
<p>

<% if (!ModeConfig.isDiscoveryMode()) { %>
	Click <a href="debug">here</a> to view the ePADD log which can be used for reporting a problem. <br>
<% } %>
	
<% if (!ModeConfig.isDiscoveryMode() && !ModeConfig.isDeliveryMode()) { %>
	<div style="font-size:small;margin-top:15px;text-transform:uppercase; ">
		Module:
		<select name="mode" id="mode-select">
			<option value="appraisal" <%=ModeConfig.isAppraisalMode() ? "selected":""%> >APPRAISAL</option>
			<option value="processing" <%=ModeConfig.isProcessingMode() ? "selected":""%> >PROCESSING</option>
			<option value="discovery" <%=ModeConfig.isDiscoveryMode() ? "selected":""%> >DISCOVERY</option>
			<option value="delivery" <%=ModeConfig.isDeliveryMode() ? "selected":""%> >DELIVERY</option>
		</select>
	</div>
<% } %>

<script>
	// we assume jq is always loaded onto any page that includes this header
	$('#mode-select').change(function() {
		var val = $('#mode-select').val();
		epadd.log ('changing mode to ' + val);
		$.ajax({
			url:'ajax/changeMode.jsp',
			type: 'POST',
			data: {mode:val},
			dataType: 'json',
			success: function() { if (val == 'appraisal') { window.location="./browse-top"; } else { window.location = './collections';}},
			error: function() { epadd.alert('Unable to change mode, sorry!'); }
		});
	});
</script>

	<br/>
	<%

	 Archive archive = (Archive)request.getSession().getAttribute("archive");
	 if (archive!=null) { %>
		<div id="advanced_options">

		<% if (!ModeConfig.isDiscoveryMode()) { %>
			<p><button class="btn btn-default" id='reset-reviewed' style='cursor:pointer'><i class="fa fa-eye"></i> Reset reviewed status</button>
		<% } %>

		<p><button class="btn-default" id="unload-archive"><i class="fa fa-eject"></i> Unload Archive</button>

				<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
					<p><button id="delete-archive" class="btn-default"><span class="spinner"><i class="fa fa-trash"></i></span> Delete Archive</button>
				<%	} %>

				<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
					<p><button onclick="window.location.href='ner'" class="btn btn-default"><i class="fa fa-tag"></i> Re-recognize entities</button>
					<%
					org.apache.lucene.document.Document doc = (archive.indexer).getDoc(archive.getAllDocs().get(0));
					if (doc.get(edu.stanford.muse.ner.NER.EPER) == null)
						out.println("ePADD has indexed the emails, but not yet identified entities in these emails.<br>");
		//		out.println("Recompute numbers that are displayed on the landing page by clicking <a id='recompute' style='cursor:pointer'>here</a> <img id='recompute-stat' src='images/spinner.gif' style='display:none'/>");
				} %>
				<% if (!ModeConfig.isDiscoveryMode()) { %>
					<p><button class="btn btn-default" id="recompute" style='cursor:pointer'><i class="fa fa-refresh"></i> Recompute Stats</button>
				<% } %>
			<%--<p><button class="btn btn-default" id="featuresIndex" style='cursor:pointer'><i class="fa fa-refresh"></i> Build cache for internal authority assignment</button>--%>
		</div>
	<% } /* archive != null */ %>

	<script>
		$("#recompute").click(function(e){
			// can consider making this is a get_page_with_progress, since it can take a while?
			var $spinner = $('.fa', $(e.target));
			$spinner.addClass('fa-spin');

			$.ajax({
				url: 'ajax/recomputestats.jsp',
				success: function(){$spinner.removeClass('fa-spin');},
				error: function(){$spinner.removeClass('fa-spin');epadd.alert('Unable to compute numbers, sorry!');}
			});
			$("#recompute-stat").css('display','block');
		});

		$("#featuresIndex").click(function(e){
			// can consider making this is a get_page_with_progress, since it can take a while?
			var $spinner = $('.fa', $(e.target));
			$spinner.addClass('fa-spin');

			$.ajax({
				url: 'ajax/checkFeaturesIndex.jsp',
				success: function(){$spinner.removeClass('fa-spin');},
				error: function(){$spinner.removeClass('fa-spin');epadd.alert('Unable to compute numbers, sorry!');}
			});
			$("#recompute-stat").css('display','block');
		});

		$("#reset-reviewed").click(function(e){
			var $spinner = $('.fa', $(e.target));
			$spinner.addClass('fa-spin');
			$.ajax({
				url: 'ajax/applyFlags.jsp?allDocs=1&setReviewed=0',
				success: function(){$spinner.removeClass('fa-spin');},
				error: function(){$spinner.removeClass('fa-spin');epadd.alert('Unable to reset reviewed flag, sorry!');}
			});
			$("#recompute-stat").css('display','block');
		});

		$("#unload-archive").click(epadd.unloadArchive);
		$("#delete-archive").click(epadd.deleteArchive);
	</script>
	<!--
	<h2>Experimental features </h2>
	<p>
	<button class="btn-default" onclick="window.location='finetypes.jsp';">Infer fine grained types</button>
	-->
<p>

</div>
</body>
</html>
