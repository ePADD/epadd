<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig"%>
<%@page import="edu.stanford.muse.index.ArchiveReaderWriter"%>
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
<%writeProfileBlock(out, archive, "Settings");%>

<jsp:include page="alert.jspf"/>

<p>

    <div style="width:1100px; margin:auto">
	<%

        String archiveID = ArchiveReaderWriter.getArchiveIDForArchive(archive);
        if (archive != null) { %>
            <div id="advanced_options">

            <% if (!ModeConfig.isDiscoveryMode()) { %>
                    <p><button onclick="window.location='verify-bag?archiveID=<%=archiveID%>'" class="btn-default" style="cursor:pointer"><i class="fa fa-eye"></i>Verify bag checksum</button>

                <% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
                    <p><button onclick="window.location='set-images?archiveID=<%=archiveID%>';" class="btn-default" style='cursor:pointer' ><i class="fa fa-picture-o"></i> Set Images</button></p>
                <% }
            } /* archive != null */
        }
    %>

    </div>

</body>
</html>
