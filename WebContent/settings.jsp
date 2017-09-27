<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.webapp.ModeConfig"%>
<%@include file="getArchive.jspf" %>

<!DOCTYPE HTML>
<html lang="en">
<head>
    <title>ePADD Settings</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css">

	<script src="js/jquery.js"></script>

	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>

	<script src="js/epadd.js"></script>
	<script src="js/stacktrace.js"></script>
	<style>
		#advanced_options button {width:250px;}
	</style>
</head>
<body style="color:gray;">
<jsp:include page="header.jspf"/>
<jsp:include page="alert.jspf"/>

<p>
<script>
	epadd.select_link('#nav1', 'Settings');
</script>

	<div style="margin-left:5%">
	<p>
<p>

	<!--sidebar content-->
<div class="nav-toggle1 sidebar-icon">
	<img src="images/sidebar.png" alt="sidebar">
</div>

<nav class="menu1" role="navigation">
	<h2>Settings Tips</h2>
	<!--close button-->
	<a class="nav-toggle1 show-nav1" href="#">
		<img src="images/close.png" class="close" alt="close">
	</a>

	<!--phrase-->
	<div class="search-tips">
		<img src="images/pharse.png" alt="">
		<p>
			Text from Josh here.
		</p>
	</div>

	<!--requered-->
	<div class="search-tips">
		<img src="images/requered.png" alt="">
		<p>
			More text
		</p>
	</div>


</nav>
<!--/sidebar-->

	<br/>
	<%

		String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	 if (archive!=null) { %>
		<div id="advanced_options">

		<% if (!ModeConfig.isDiscoveryMode()) { %>
            <% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
			    <p><button onclick="window.location='set-images?archiveID=<%=archiveID%>';" class="btn-default" style='cursor:pointer' ><i class="fa fa-picture-o"></i> Set Images</button>
            <% } %>

			<p><button onclick="window.location='bulk-flags?archiveID=<%=archiveID%>&allDocs=1'" class="btn-default" style='cursor:pointer'><i class="fa fa-eye"></i> Set default actions</button>

			<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
				<p><button class="btn btn-default" style='cursor: pointer' onclick="window.location='bulk-upload?archiveID=<%=archiveID%>'"><i class="fa fa-upload"></i> Set bulk flags</button>
				<p><button onclick="window.location.href='ner?archiveID=<%=archiveID%>'" class="btn btn-default"><i class="fa fa-tag"></i> Re-recognize entities</button>
				<%
				/*
						int size = archive.getAllDocs().size();
						if (size > 0) {
							org.apache.lucene.document.Document doc = archive.getLuceneDoc(archive.getAllDocs().get(0).id);
							if (doc == null || doc.get(edu.stanford.muse.ner.NER.NAMES) == null)
								out.println("ePADD has indexed the email messages, but not yet identified entities in them.<br>");
						} else
							out.println ("There are no messages in this archive.<br/>");
							*/

				//		out.println("Recompute numbers that are displayed on the landing page by clicking <a id='recompute' style='cursor:pointer'>here</a> <img id='recompute-stat' src='images/spinner.gif' style='display:none'/>");
				} %>

    <!--                <p><button class="btn btn-default" id="recompute" style='cursor:pointer'><i class="fa fa-refresh"></i> Recompute Stats</button> -->

<!--                <p><button onclick="window.location='debug';" class="btn-default"  style='cursor:pointer'><i class="fa fa-flag"></i> Debug Log</button> -->
                <p><button class="btn-default" id="unload-archive"><i class="fa fa-eject"></i> Unload Archive</button>

				<% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
						<p><button id="delete-archive" class="btn-default"><span class="spinner"><i class="fa fa-trash"></i></span> Delete Archive</button>
						<!-- <p><button class="btn-default" onclick="window.location='export-mbox'"><i class="fa fa-save"></i> Export archive to mbox</button> -->
				<%	} %>

<!--                <p><button class="btn btn-default" id="recompute" style='cursor:pointer'><i class="fa fa-refresh"></i> Recompute Stats</button> -->

                <% if (!ModeConfig.isDeliveryMode()) { %>
                <div style="font-size:small;margin-top:15px;text-transform:uppercase; ">
                    <br/>
                    <div class="row">
                        <div class="form-group col-sm-3">
                            <span class="badge" style="background-color:rgb(191,19,19)">Advanced</span><br/>
                            <label for="mode-select">Select ePADD Module</label><br>

                            <select id="mode-select" name="attachmentFilesize" class="form-control selectpicker">
                                <option value="" selected disabled>Choose Module</option>
                                <option value="appraisal" <%=ModeConfig.isAppraisalMode() ? "selected" : ""%> >APPRAISAL</option>
                                <option value="processing" <%=ModeConfig.isProcessingMode() ? "selected" : ""%> >PROCESSING</option>
                                <option value="discovery" <%=ModeConfig.isDiscoveryMode() ? "selected" : ""%> >DISCOVERY</option>
                                <option value="delivery" <%=ModeConfig.isDeliveryMode() ? "selected" : ""%> >DELIVERY</option>
                            </select>
                        </div>
                    </div>
                    <% } %>

            </div>
	<% } /* archive != null */ %>

        <% } %>

	<script>
/*
		$("#recompute").click(function(e){
			// can consider making this is a get_page_with_progress, since it can take a while?
			var $spinner = $('.fa', $(e.target));
			$spinner.addClass('fa-spin');

			$.ajax({
				url: 'ajax/recomputestats.jsp',
                data: {
                    "archiveID": archiveID
                },
				success: function(){$spinner.removeClass('fa-spin');},
				error: function(){$spinner.removeClass('fa-spin');epadd.alert('Unable to compute numbers, sorry!');}
			});
			$("#recompute-stat").css('display','block');
		});
*/

/*
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
*/

		$("#unload-archive").click(epadd.unloadArchive('<%=archiveID%>'));
		$("#delete-archive").click(epadd.deleteArchive('<%=archiveID%>'));

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

</div>
</body>
</html>
