<%
//    2022-11-03  allow to add event manually to PREMIS metadata
%>    
<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig"%>
<%@page import="edu.stanford.muse.index.ArchiveReaderWriter"%>
<%@include file="getArchive.jspf" %>

<!DOCTYPE HTML>
<html lang="en">

<head>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">

	<jsp:include page="css/css.jsp"/>
	<link rel="stylesheet" href="css/sidebar.css?v=1.1">
	<link rel="stylesheet" href="css/bootstrap-datetimepicker.min.css">
	<%-- Jscript was included here --%>

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js?v=1.1"></script>

	<script src="js/epadd.js?v=1.1"></script>
	<script src="js/stacktrace.js?v=1.1"></script>
    <script type="text/javascript" src="js/moment.min.js"></script>
    <script type="text/javascript" src="js/moment-with-locales.min.js"></script>
    <script type="text/javascript" src="js/bootstrap-datetimepicker.min.js"></script>
	<style>
		#advanced_options button {width:250px;}
        #advanced_options input {width:250px;}
	</style>
</head>
<body style="color:gray;">
    <%-- Header.jsp was included here earlier --%>
    <%@include file="header.jspf"%>
    <title><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.head-epadd-settings")%></title>

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
    <div class="panel" id="ownersetting">
        <div class="panel-heading"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.set-owners-contact")%></div>

<div class="one-line" >
    <input type="text" placeholder="<%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.owner-email-id")%>" id="ownermailid"></input>
    <button id="owenersetting" onclick="setOwnerMailHandler();return false;" class="btn-default" style="cursor:pointer"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "settings.set-as-owner")%></button>
</div>
    </div>
</section>

<section>
    <div class="panel" id="premispanel">
        <div class="panel-heading">PREMIS Metadata</div>
        
        <div class="form-group col-sm-12">
            Event
            <select id="premisevent" class="form-control selectpicker" style="margin: 0 100px; width:600px">
                <option value="Ingestion">Ingestion</option>
                <option value="Identifier assignment">Identifier assignment</option>
                <option value="Fixity check">Fixity check</option>
                <option value="Message digest calculation">Message digest calculation</option>
                <option value="Quarantine">Quarantine</option>
                <option value="Unquarantine">Unquarantine</option>
                <option value="Unpacking">Unpacking</option>
                <option value="Name cleanup">Name cleanup</option>
                <option value="Virus check">Virus check</option>
                <option value="Format identification">Format identification</option>
                <option value="Validation">Validation</option>
                <option value="Normalization">Normalization</option>
                <option value="Transcription">Transcription</option>
                <option value="Creation">Creation</option>
                <option value="Other">Other</option>
            </select>
        </div>        

        <div class="form-group col-sm-12"> 
            Detail
            <input type="text" class="form-control" placeholder="" id="premisdetail" style="margin: 0 100px; width:600px"></input>
        </div>
        
    <div class="container form-group col-sm-6" >
       Date/Time
            <div class='input-group date' id='premisdatetime' style="margin: 0 100px;">
               <input type='text' class="form-control"/>
               <span class="input-group-addon">
                   <i class="glyphicon glyphicon-calendar"></i>
               </span>
            </div>
      <script type="text/javascript">
         $(function () {
             $('#premisdatetime').datetimepicker({
                 format: 'YYYY-MM-DD HH:mm',
                 defaultDate: new Date()
             });
         });
      </script>
    </div>        
        
        <div class="form-group col-sm-6"></div>
        <div align="right">     
            <button id="premisbutton" onclick="add2Premis(archiveID);" class="btn-default" style="cursor:pointer">Add</button>
        </div>
        
    </div>
</section>

        <script>
// 2022-11-03            
        function add2Premis(archive1) {
           if (!$('#premisdetail').val()) {
                BootstrapDialog.show({
                    message: 'Detail is empty', 
                    type: BootstrapDialog.TYPE_WARNING, 
                    buttons: [{label: 'OK', action:function(dialogItself) { dialogItself.close();} }] 
                });
               return false;
           }
           if ( $("#premisdatetime").data('DateTimePicker').date()) {
               var a1 = moment($("#premisdatetime").data('DateTimePicker').date());
               if (!a1.isValid()) {
                    BootstrapDialog.show({
                        message: 'Date/Time is invalid', 
                        type: BootstrapDialog.TYPE_WARNING, 
                        buttons: [{label: 'OK', action:function(dialogItself) { dialogItself.close();} }] 
                    });
                   return false;
               }    
            } else {
                BootstrapDialog.show({
                    message: 'Date/Time is empty', 
                    type: BootstrapDialog.TYPE_WARNING, 
                    buttons: [{label: 'OK', action:function(dialogItself) { dialogItself.close();} }] 
                });
                return false;
            }

            $.ajax({
                type: "POST",
                url: "ajax/add2Premis",
                data: { 
                       archive: archive1,
                       premisevent: $('#premisevent').val(),
                       premisdetail: $('#premisdetail').val(),
                       premisdatetime: moment($("#premisdatetime").data('DateTimePicker').date()).format('YYYY-MM-DD HH:mm:ss')
                },
                success: function(response) {
                                        if (response['result'] === "ok") {
                                            BootstrapDialog.show({
                                                message:response['reason'],  
                                                type: BootstrapDialog.TYPE_SUCCESS, 
                                                buttons: [{label: 'OK', action:function(dialogItself) { dialogItself.close();} }] 
                                            });
                                        } else {
                                            BootstrapDialog.show({
                                                message:response['reason'], 
                                                type: BootstrapDialog.TYPE_WARNING, 
                                                buttons: [{label: 'OK', action:function(dialogItself) { dialogItself.close();} }] 
                                            });
                                        }
                                    },
                                    error: function(response) {
                                        BootstrapDialog.show({message:response['reason'], type: BootstrapDialog.TYPE_WARNING});
                                    }
            });                                
            
            return true;
        }
		
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
