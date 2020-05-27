<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig"%>
<%@page import="edu.stanford.muse.index.ArchiveReaderWriter"%>
<%@include file="getArchive.jspf" %>

<!DOCTYPE HTML>
<html lang="en">
    <script src="js/jquery.js"></script>
    <%@include file="header.jspf"%>

<head>
    <title><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.head-epadd-settings")%></title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">

	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css">

	<%-- Jscript was included here --%>

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
    <%-- Header.jsp was included here earlier --%>
<%writeProfileBlock(out, archive, edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.manage-settings"));%>

<jsp:include page="alert.jspf"/>
<div id="spinner-div" style="text-align:center;display:none"> <img style="height:20px" src="images/spinner.gif"/></div>

<p>

    <div style="width:1100px; margin:auto">
	<%
        if (archive != null) { %>
            <div id="advanced_options">

            <% if (!ModeConfig.isDiscoveryMode()) { %>
<p><button onclick="window.location='verify-bag?archiveID=<%=archiveID%>'" class="btn-default" style="cursor:pointer"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.verify-bag-checksum")%></button></p>
<p>    <button id="debugaddressbook" onclick="window.location='debugAddressBook?archiveID=<%=archiveID%>'"  class="btn-default" style="cursor:pointer"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.debug-address-book")%></button></p>

<section>
    <div class="panel" id="generate-thumbnails">
        <div class="panel-heading">Generate Thumbnails for Attachments</div>

        <div class="one-line">
            <div class="form-group col-sm-4">
                <%--<label for="trustedaddrsForComputation">Trusted emails addresses</label>--%>
                <input type="text" placeholder="Path to soffice executable (from LibreOffice) /Applications/LibreOffice.app/Contents/MacOS/soffice" id="libreofficpath"></input>
            </div>
            <div class="form-group col-sm-5">
                <%--<label for="outgoingthreshold">Outgoing messages threshold</label>--%>
                <input type="text" placeholder="Path to convert executable (from ImageMagick) /usr/local/bin/convert" id="convertpath"></input>
            </div>
            <div class="form-group col-sm-2 picker-buttons">
                <button id="createThumbnails" onclick="createThumbnailsAddressHandler();return false;" class="btn-default" style="cursor:pointer">Create Thumbnails</button>
            </div>
        </div>
        <br/>
        <br/>
        <div class="form-group col-sm-4">
            <input type="text" id="thumbnail-gen-result" placeholder="RESULT" readonly></input>
        </div>
        <br/>
        <br/>
    </div>
</section>

<section>
    <div class="panel" id="export-headers">
        <div class="panel-heading"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.trusted-address-computation")%></div>

        <div class="one-line">
            <div class="form-group col-sm-4">
                <%--<label for="trustedaddrsForComputation">Trusted emails addresses</label>--%>
                <input type="text" placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.trusted-email-addresses")%>" id="trustedaddrsForComputation"></input>
            </div>
            <div class="form-group col-sm-5">
                <%--<label for="outgoingthreshold">Outgoing messages threshold</label>--%>
                <input type="text" placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.min-outgoing-messages")%>" id="outgoingthreshold"></input>
            </div>
            <div class="form-group col-sm-2 picker-buttons">
                <button id="moreComputation" onclick="computeMoreTrustedAddressHandler();return false;" class="btn-default" style="cursor:pointer"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.get-trusted-addresses-button")%></button>
            </div>
        </div>
        <br/>
        <br/>
            <div class="form-group col-sm-4">
                <input type="text" id="result-more-trusted" placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.result")%>" readonly></input>
            </div>
        <br/>
        <br/>
    </div>
</section>

<section>
    <div class="panel" id="recomputation">
        <div class="panel-heading"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.address-book-recomputation")%></div>

<div class="one-line" >
    <input type="text" placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.trusted-email-addresses")%>" id="trustedaddrs"></input>
<button id="recomputebutton" onclick="recomputeAddressBookHandler();return false;" class="btn-default" style="cursor:pointer"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.recompute-address-book-button")%></button>
</div>
    </div>
</section>

<section>
    <div class="panel" id="ownersetting">
        <div class="panel-heading"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.set-owners-contact")%></div>

<div class="one-line" >
    <input type="text" placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.owner-email-id")%>" id="ownermailid"></input>
    <button id="owenersetting" onclick="setOwnerMailHandler();return false;" class="btn-default" style="cursor:pointer"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.set-as-owner")%></button>
</div>
    </div>
</section>
        <script>
            var setOwnerMailHandler = function(){
                var archiveID='<%=archiveID%>';
                //get owners address. In case of more than one separate by ;.
                // If empty then prompt user to provide at least one  address.
                var ownersaddress = $('#ownermailid').val();
                ownersaddress = ownersaddress.trim();
                if (!ownersaddress) {
                    epadd.error("Please provide at least one owner's email address!");
                    return;
                }
                //else perform an ajax call.
                //on succesful execution of the call redirect to browse-top page.
                // var $spinner = $('#spinner-div');
                // $spinner.show();
                // $spinner.addClass('fa-spin');
                // $('#spinner-div').fadeIn();
                // $('#recomputebutton').fadeOut();
                var data = {'archiveID': archiveID,'ownersaddress':ownersaddress};
                var params = epadd.convertParamsToAmpersandSep(data)
                var promptmethod = function(j){
                    epadd.info("Successfully set the owner's addresses",function(){
                        window.location = './browse-top?archiveID=' +archiveID;
                    })                }
                fetch_page_with_progress("ajax/async/setOwnersAddress.jsp", "status", document.getElementById('status'), document.getElementById('status_text'), params,promptmethod);

               /* $.ajax({type: 'POST',
                    dataType: 'json',
                    url: 'ajax/setOwnersAddress.jsp',
                    data: data,
                    success: function (response) {
                        $spinner.removeClass('fa-spin');
                        // $('#spinner-div').fadeOut();
                        // $('#recomputebutton').fadeIn();

                        if (response) {
                            if (response.status === 0) {
                                epadd.info("Successfully set the owner's addresses",function(){
                                    window.location = './browse-top?archiveID=' +archiveID;
                                })
                            } else{

                                epadd.error('Error setting the owner\'s addresses ' + response.status + ', Message: ' + response.error, function(){
                                    //window.location = './browse-top?archiveID=' +archiveID;
                                });
                            }
                        }
                        else{
                            epadd.error('Error setting the owner\'s addresses. Improper response received!', function(){
                                //window.location = './browse-top?archiveID=' +archiveID;
                            });
                        }
                    },
                    error: function(jq, textStatus, errorThrown) {
                        // $('#spinner-div').fadeOut();
                        // $('#recomputebutton').fadeIn();
                        epadd.error('Sorry, there was an error while setting the owner\'s address. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report it to epadd_project@stanford.edu.', function(){
                            window.location = './browse-top?archiveID=' +archiveID;
                        });
                    }
                });*/
            };

            var createThumbnailsAddressHandler = function(){
                var archiveID='<%=archiveID%>';
                //path to soffice program (from Libreoffice)
                var sofficepath = $('#libreofficpath').val();
                //path to convert program (from ImageMagick)
                var convertpath = $('#convertpath').val();
                //if any of them is empty then return with warning.
                if (!sofficepath || !convertpath) {
                    epadd.error("Please provide path to 'soffice' and 'convert' executables which are needed to create thumbnails.");
                    return;
                }
                //pass to backend three params, archiveID, path to convert program, path to soffice program.
                var $spinner = $('#spinner-div');
                $spinner.show();
                $spinner.addClass('fa-spin');
                $('#spinner-div').fadeIn();
                $('#createThumbnails').fadeOut();
                var data = {'archiveID': archiveID,'sofficepath':sofficepath,'convertpath':convertpath};


                $.ajax({type: 'POST',
                    dataType: 'json',
                    url: 'ajax/createThumbnails.jsp',
                    data: data,
                    success: function (response) {
                        $spinner.removeClass('fa-spin');
                        $('#spinner-div').fadeOut();
                        $('#createThumbnails').fadeIn();

                        if (response) {
                            if (response.status === 0) {
                                epadd.info("Successfully created the thumbnails for all attachments.",function(){
                                    $('#thumbnail-gen-result').val(response.result)
                                })
                            } else{

                                epadd.error('Error creating thumbnails:  ' + response.status + ', Message: ' + response.error);
                            }
                        }
                        else{
                            epadd.error('Error creating thumbnails. Improper response received!');
                        }
                    },
                    error: function(jq, textStatus, errorThrown) {
                        $('#spinner-div').fadeOut();
                        $('#createThumbnails').fadeIn();
                        epadd.error('Sorry, there was an error while creating the thumbnails for attachments. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report it to epadd_project@stanford.edu.');
                    }
                });
                //On success return the number that was
            }


    var computeMoreTrustedAddressHandler = function(){
        var archiveID='<%=archiveID%>';
        //get list of trusted addresses. If empty then prompt user to provide at least one trusted address.
        var trustedaddrs = $('#trustedaddrsForComputation').val();
        trustedaddrs = trustedaddrs.trim();
        if (!trustedaddrs) {
            epadd.error("Please provide at least one trusted email address for Addressbook construction!");
            return;
        }
        //get offset count, if nothing provided pass -1. On the server side it will assume infinity (or a very large value)
        var outcountoffset = $('#outgoingthreshold').val();
        if(isNaN(parseInt(outcountoffset)))
            outcountoffset=-1;
        else
            outcountoffset=parseInt(outcountoffset);

        //after ajax call returns set the result in the text box $('#result-more-trusted').val(RESULT)
//else perform an ajax call.
        //on succesful execution of the call redirect to browse-top page.
        var $spinner = $('#spinner-div');
        $spinner.show();
        $spinner.addClass('fa-spin');
        $('#spinner-div').fadeIn();
        $('#recomputebutton').fadeOut();
        var data = {'archiveID': archiveID,'trustedaddrs':trustedaddrs,'outoffset':outcountoffset};


        $.ajax({type: 'POST',
            dataType: 'json',
            url: 'ajax/computeTrustedAddresses.jsp',
            data: data,
            success: function (response) {
                $spinner.removeClass('fa-spin');
                $('#spinner-div').fadeOut();
                $('#recomputebutton').fadeIn();

                if (response) {
                    if (response.status === 0) {
                        epadd.info("Successfully computed the trusted addresses",function(){
                            $('#result-more-trusted').val(response.result)
                        })
                    } else{

                        epadd.error('Error recomputing the trusted addresses ' + response.status + ', Message: ' + response.error);
                    }
                }
                else{
                    epadd.error('Error recomputing the trusted addresses. Improper response received!');
                }
            },
            error: function(jq, textStatus, errorThrown) {
                $('#spinner-div').fadeOut();
                $('#recomputebutton').fadeIn();
                epadd.error('Sorry, there was an error while computing the trusted addresses. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report it to epadd_project@stanford.edu.');
            }
        });
    };
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
        var params = epadd.convertParamsToAmpersandSep(data)
        var promptmethod = function(j){
            epadd.info("Successfully recomputed the address book.",function(){
                window.location = './browse-top?archiveID=' +archiveID;
            })                }
        fetch_page_with_progress("ajax/async/recomputeAddressbook.jsp", "status", document.getElementById('status'), document.getElementById('status_text'), params,promptmethod);


        /*$.ajax({type: 'POST',
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
                epadd.error('Sorry, there was an error while recomputing the addressbook. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report it to epadd_project@stanford.edu.', function(){
                    window.location = './browse-top?archiveID=' +archiveID;
                });
            }
        });*/


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
