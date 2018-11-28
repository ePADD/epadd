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
        #advanced_options input {width:250px;}
	</style>
</head>
<body style="color:gray;">
<%@include file="header.jspf"%>
<%writeProfileBlock(out, archive, "Settings");%>

<jsp:include page="alert.jspf"/>

<p>

    <div style="width:1100px; margin:auto">
	<%
        if (archive != null) { %>
            <div id="advanced_options">

            <% if (!ModeConfig.isDiscoveryMode()) { %>
<p><button onclick="window.location='verify-bag?archiveID=<%=archiveID%>'" class="btn-default" style="cursor:pointer">Verify bag checksum</button></p>
<p>    <button id="debugaddressbook" onclick="window.location='debugAddressBook?archiveID=<%=archiveID%>'"  class="btn-default" style="cursor:pointer">Debug address book</button></p>
<div class="one-line" id="export-next">
    <input type="text" placeholder="';' separated trusted email addresses" id="trustedaddrs"></input>
<button id="recomputebutton" onclick="recomputeAddressBookHandler();return false;" class="btn-default" style="cursor:pointer">Recompute Address Book</button>
</div>
<script>
    var recomputeAddressBookHandler = function(){
        var archiveID='<%=archiveID%>';
        //get list of trusted addresses. If empty then prompt user to provide at least one trusted address.
        var trustedaddrs = $('#trustedaddrs').val();
        trustedaddrs = trustedaddrs.trim();
        if (!trustedaddrs) {
            epadd.error("Please provide at least one trusted email address for Addressbook construction!");
            return;
        }
        //else perform an ajax call.
        //on succesful execution of the call redirect to browse-top page.
        var $spinner = $('#spinner-div');
        $spinner.show();
        $spinner.addClass('fa-spin');
        $('#spinner-div').fadeIn();
        $('#recomputebutton').fadeOut();
        var data = {'archiveID': archiveID,'trustedaddrs':trustedaddrs};


        $.ajax({type: 'POST',
            dataType: 'json',
            url: 'ajax/recomputeAddressbook.jsp',
            data: data,
            success: function (response) {
                $spinner.removeClass('fa-spin');
                $('#spinner-div').fadeOut();
                $('#recomputebutton').fadeIn();

                if (response) {
                    if (response.status === 0) {
                        epadd.info("Successfully recomputed the addressbook",function(){
                          window.location = './browse-top?archiveID=' +archiveID;
                        })
                    } else{

                        epadd.error('Error recomputing the addressbook ' + response.status + ', Message: ' + response.error, function(){
                            window.location = './browse-top?archiveID=' +archiveID;
                        });
                    }
                }
                else{
                    epadd.error('Error recomputing the addressbook. Improper response received!', function(){
                        window.location = './browse-top?archiveID=' +archiveID;
                    });
                }
            },
            error: function(jq, textStatus, errorThrown) {
                $('#spinner-div').fadeOut();
                $('#recomputebutton').fadeIn();
                epadd.error('Sorry, there was an error while importing the accession. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report it to epadd_project@stanford.edu.', function(){
                    window.location = './browse-top?archiveID=' +archiveID;
                });
            }
        });


    };
</script>
                <% if (ModeConfig.isAppraisalMode() || ModeConfig.isProcessingMode()) { %>
                    <%--NO LONGER NEEDED THIS FUNCTIONALITY HERE<p><button onclick="window.location='set-images?archiveID=<%=archiveID%>';" class="btn-default" style='cursor:pointer' ><i class="fa fa-picture-o"></i> Set Images</button></p>--%>
                <% }
            } /* archive != null */
        }
    %>

    </div>

</body>
</html>
