<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.index.Document"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="java.util.Set" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="java.util.Map" %>
<%@page language="java" %>
<!DOCTYPE HTML>
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>ePADD export</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">


	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
    <link href="css/selectpicker.css" rel="stylesheet" type="text/css" media="screen" />
	<jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="jqueryFileTree/jqueryFileTree.js"></script>
    <script src="js/jquery.autocomplete.js" type="text/javascript"></script>

    <script src="js/filepicker.js"></script>
    <script src="js/selectpicker.js"></script>

	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	<style>
		.mini-box { height: 105px; vertical-align:top; background-color: #f5f5f8; display: inline-block; width:200px; padding:20px; margin-right:22px;}

        .mini-box .review-messages  {  display: none; margin-top: 10px;}

        .mini-box-icon { color: #f2c22f; display: inline-block; width: 35px; vertical-align:top; font-size: 175%;}
        .mini-box-description .number { font-size: 22px; margin-bottom:5px; }
        .mini-box-description { font-size: 14px;  display: inline-block; width: 100px; vertical-align:top; }

        .mini-box:hover { background-color: #0075bb; color: white; box-shadow: 1px 1px 5px 0px rgba(0,0,0,0.75);}
        .mini-box:hover .mini-box-description, .mini-box:hover .mini-box-icon { display: none; }
        .mini-box:hover .review-messages  {  display: block;  transition: ease-in-out 0.0s;  cursor:pointer;  }
        i.icon-browsetoparrow { font-weight: 600; font-size: 125%;}

        .go-button, .go-button:hover { background-color: #0075bb; color: #fff; }  /* saumya wants this always filled in, not just on hover */

        .btn-default { height: 37px; }
         label {  font-size: 14px; padding-bottom: 13px; font-weight: 400; color: #404040; } /* taken from form-group label in adv.search.scss */
        .faded { opacity: 0.5; }
        .one-line::after {  content:"";  display:block;  clear:both; }  /* clearfix needed, to take care of floats: http://stackoverflow.com/questions/211383/what-methods-of-clearfix-can-i-use */
        .picker-buttons { margin-top:40px; margin-left:-30px; } /* so that the browse button appears at the right edge of the input box */
        .form-group { margin-bottom: 25px;}
        .review-messages { vertical-align:center; text-align:center;}

    </style>
</head>
<body style="background-color:white;">
<jsp:include page="header.jspf"/>
<jsp:include page="div_filepicker.jspf"/>

<script>epadd.nav_mark_active('Export');</script>

<%@include file="profile-block.jspf"%>

<%
Archive archive = (Archive) JSPHelper.getSessionAttribute(session, "archive");
String bestName = "";
String bestEmail = "";
if (archive != null) {
	AddressBook ab = archive.addressBook;
	Set<String> addrs = ab.getOwnAddrs();
	if (addrs.size() > 0)
		bestEmail = addrs.iterator().next();
	writeProfileBlock(out, archive, "", "Export archive");
}

if (!ModeConfig.isProcessingMode() && !ModeConfig.isAppraisalMode()) {
%>
    Error: Export is only available in processing or appraisal modes!
<%
    return;
}

    int messagesToExport = 0, annotatedMessages = 0, restrictedMessages = 0, messagesNotToExport = 0;
    for (Document d: archive.getAllDocs()) {
         EmailDocument ed = (EmailDocument) d;
         if (ed.doNotTransfer)
             messagesNotToExport++;
         if (ed.transferWithRestrictions)
             restrictedMessages++;
         if (!ed.doNotTransfer && !ed.transferWithRestrictions)
              messagesToExport++;
         if (!Util.nullOrEmpty(ed.comment))
             annotatedMessages++;
    }
%>

<p>

<div id="all_fields" style="margin-left:170px; width:900px; padding: 10px">
	<b>Review messages</b>
    <br/>
	<br/>
	<div onclick="window.location='export-review?type=transfer'" class="mini-box">
        <div class="review-messages">
            <i class="icon-browsetoparrow"></i><br/>View
        </div>


        <div class="mini-box-icon"><i class="fa fa-envelope-o"></i></div>
        <div class="mini-box-description">
            <div class="number"><%=Util.commatize(messagesToExport)%></div>
            Unrestricted messages
        </div>
    </div>

	<div onclick="window.location='export-review?type=annotated'" class="mini-box">
        <div class="review-messages">
            <i class="icon-browsetoparrow"></i><br/>View
        </div>


        <div class="mini-box-icon"><i class="fa fa-comment-o"></i></div>
        <div class="mini-box-description">
            <div class="number"><%=Util.commatize(annotatedMessages)%></div>
            Annotated messages
        </div>
	</div>

	<div onclick="window.location='export-review?type=transferWithRestrictions'" class="mini-box">
        <div class="review-messages">
            <i class="icon-browsetoparrow"></i><br/>View
        </div>


        <div class="mini-box-icon"><i class="fa fa-exclamation-triangle"></i></div>
        <div class="mini-box-description">
            <div class="number"><%=Util.commatize(restrictedMessages)%></div>
            Restricted messages
        </div>
	</div>

	<div onclick="window.location='export-review?type=doNotTransfer'" class="mini-box" style="margin-right:0px">
        <div class="review-messages">
            <i class="icon-browsetoparrow"></i><br/>View
        </div>


        <div class="mini-box-icon"><i class="fa fa-ban"></i></div>
        <div class="mini-box-description">
            <div class="number"><%=Util.commatize(messagesNotToExport)%></div>
            Messages not to export
        </div>
	</div>

	<br/>
	<br/>

	<section>
		<div class="panel">
			<div class="panel-heading">Export messages and attachments</div>

            <div class="one-line" id="export-next">
                <div class="form-group col-sm-8">
                    <label for="export-next-file">Export to next ePADD module</label>
                    <input id="export-next-file" class="dir form-control" type="text" name="name" value=""/>
                </div>
                <div class="form-group col-sm-4 picker-buttons">
                    <button id="export-next-browse" class="btn-default browse-button">Browse</button>
                    <button id="export-next-do" style="margin-left: 10px;" class="go-button faded btn-default">Export</button>
                </div>
            </div>

            <% if (ModeConfig.isProcessingMode()) { %>
                <div class="one-line" id="export-mbox">
                    <div class="form-group col-sm-8" >
                        <label for="export-mbox-file">Export to mbox</label>
                        <input id="export-mbox-file" class="dir form-control" type="text" name="name" value=""/>
                    </div>
                    <div class="form-group col-sm-4 picker-buttons">
                        <button id="export-mbox-browse" class="btn-default browse-button">Browse</button>
                        <button id="export-mbox-do" style="margin-left: 10px;" class="go-button faded btn-default">Export</button>
                    </div>
                </div>

                <script>
                $('#export-mbox .go-button').click (function(e) {
                    var $button = $(e.target);
                    if ($button.hasClass('faded'))
                        return false; // do nothing;
                    var baseUrl = 'export-mbox';
                    var dir = $('.dir', $('#export-mbox')).val();
                    if (dir && dir.length > 0)
                        window.location = baseUrl + '?dir=' + dir;
                });
                </script>
            <% } %>

		</div>
	</section>

    <section>
        <div class="panel">
            <div class="panel-heading">Export attachments</div>


            <div class="checkbox-inline">
                <label>
                    <input type="checkbox" name="unprocessedOption" checked>
                    <span class="label-text">Unprocessed only</span>
                </label>
            </div>

            <%
                Map<String,String> attachmentTypeOptions= Config.attachmentTypeToExtensions;
            %>
            <div class="one-line">
                <div class="advanced-search form-group col-sm-6" style="padding:0px 0px 0px 15px">
                    <label for="attachmentType">Type</label>
                    <select name="attachmentType" id="attachmentType" class="form-control multi-select selectpicker" title="Select" multiple>
                        <option value="" selected disabled>Select</option>
                        <%
                            for (Map.Entry<String,String> opt : attachmentTypeOptions.entrySet()){
                                 if(opt.getKey().toLowerCase().equals(opt.getValue().toLowerCase())){
                        %>
                                    <option value = "<%=opt.getValue()%>"><%=opt.getKey()%></option>
                        <%
                                }else{
                        %>

                                    <option value = "<%=opt.getValue()%>"><%=opt.getKey()+" ("+opt.getValue()+")"%></option>
                        <%} }%>
                    </select>
                </div>

                <!--Extension-->
                <div class="form-group col-sm-6">
                    <label for="attachmentExtension">Other extension</label>
                    <input id="attachmentExtension" name="attachmentExtension" id="attachmentExtension" type="text" class="form-control">
                </div>

                <br/>

            </div>

            <div class="one-line" id="export-attach">
                <div class="form-group col-sm-8">
                    <label for="export-attach-file">Specify location</label>
                    <input id="export-attach-file" class="dir form-control" type="text" name="name" value=""/>
                </div>
                <div class="form-group col-sm-4 picker-buttons">
                    <button id="export-attach-browse" class="btn-default browse-button">Browse</button>
                    <button id="export-attach-do" style="margin-left: 10px;" class="go-button faded btn-default">Export</button>
                </div>
            </div>

            <script>
                $('#export-attach .go-button').click (function(e) {
                    var $button = $(e.target);
                    if ($button.hasClass('faded'))
                        return false; // do nothing;

                    var ext = $('#attachmentExtension').val();
                    var type = $('#attachmentType').val();

                    /* //Check if type contains 'all' if it then add all options as type
                    if(type.indexOf('all')!=-1){
                        var allOptions = new Array;
                        //following loop gets all options present in attachmentType selection box except 'all'
                        $('#attachmentType option').each(function(){
                            if($(this).val()!='all')
                                allOptions.push($(this).val());
                        });
                        //assign options to type variable declared outside this if c`ondition
                        type = allOptions.slice()

                    }*/


                 //Now we also pass an option to denote if only unprocessed attachments need to be exported.
                    var baseUrl = 'export-attachments';
                    var dir = $('.dir', $('#export-attach')).val();
                    if (dir && dir.length > 0)
                        window.location = baseUrl + '?dir=' + dir + '&type=' + type + '&ext=' + ext + '&unprocessedonly=' + $('input[name="unprocessedOption"]').prop('checked');
                });
            </script>

            <br/>
        </div>
    </section>

    <% if (ModeConfig.isProcessingMode()) { %>

    <section>
        <div class="panel" id="export-headers">
            <div class="panel-heading">Export headers (CSV)</div>

            <div class="one-line">
                <div class="form-group col-sm-8">
                    <label for="export-headers-file">Specify location</label>
                    <input id="export-headers-file" class="dir form-control" type="text" name="name" value=""/>
                </div>
                <div class="form-group col-sm-4 picker-buttons">
                    <button id="export-headers-browse" class="btn-default browse-button">Browse</button>
                    <button id="export-headers-do" style="margin-left: 10px;" class="go-button faded btn-default">Export</button>
                </div>
            </div>


            <br/>
        </div>
    </section>

    <script>
        $('#export-headers .go-button').click (function(e) {
            var $button = $(e.target);
            if ($button.hasClass('faded'))
                return false; // do nothing;
            var baseUrl = 'export-headers';
            var dir = $('.dir', $button.closest('.panel')).val();
            if (dir && dir.length > 0)
                window.location = baseUrl + '?exportType=csv&dir=' + dir;
        });
    </script>
    <% } %>


    <% if (ModeConfig.isProcessingMode()) { %>
    <section>
            <div class="panel" id="export-auth">
                <div class="panel-heading">Export authorities (CSV)</div>

                <div class="one-line">
                    <div class="form-group col-sm-8">
                        <label for="export-auth-file">Specify location</label>
                        <input id="export-auth-file" class="dir form-control" type="text" name="name" value=""/>
                    </div>
                    <div class="form-group col-sm-4 picker-buttons">
                        <button id="export-auth-browse" class="btn-default browse-button">Browse</button>
                        <button id="export-auth-do" style="margin-left: 10px;" class="go-button faded btn-default">Export</button>
                    </div>
                </div>


                <br/>
            </div>
    </section>

    <script>
        $('#export-auth .go-button').click (function(e) {
            var $button = $(e.target);
            if ($button.hasClass('faded'))
                return false; // do nothing;
            var baseUrl = 'export-authorities';
            var dir = $('.dir', $button.closest('.panel')).val();
            if (dir && dir.length > 0)
                window.location = baseUrl + '?exportType=csv&dir=' + dir;
        });
        </script>
    <% } %>


</div> <!--  all fields -->

<p>

	<script type="text/javascript">
		$(document).ready(function() {
            new FilePicker($('#export-next'));
            new FilePicker($('#export-mbox'));
            new FilePicker($('#export-attach'));
            new FilePicker($('#export-auth'));
            new FilePicker($('#export-headers'));
		});

        $('#export-next .go-button').click (function(e) {
            var $button = $(e.target);
            if ($button.hasClass('faded'))
                return false; // do nothing;
            var baseUrl = '<%=ModeConfig.isProcessingMode() ? "export-complete-processing":"export-complete"%>';
            var dir = $('.dir', $('#export-next')).val();
            if (dir && dir.length > 0)
                window.location = baseUrl + '?dir=' + dir;
        });



        var autocomplete_params = {
            serviceUrl: 'ajax/attachmentAutoComplete.jsp?extensions=1',
            onSearchError: function (query, jqXHR, textStatus, errorThrown) {epadd.log(textStatus+" error: "+errorThrown);},
            preventBadQueries: false,
            showNoSuggestionNotice: true,
            preserveInput: true,
            ajaxSettings: {
                "timeout":3000,
                dataType: "json"
            },
            dataType: "text",
            //100ms
            deferRequestsBy: 100,
            onSelect: function(suggestion) {
                var existingvalue = $(this).val();
                var idx = existingvalue.lastIndexOf(';');
                if (idx <= 0)
                    $(this).val(suggestion.name);
                else
                    $(this).val (existingvalue.substring (0, idx+1) + ' ' + suggestion.name); // take everything up to the last ";" and replace after that
            },
            onHint: function (hint) {
                $('#autocomplete-ajax-x').val(hint);
            },
            onInvalidateSelection: function() {
                epadd.log('You selected: none');
            }
        };

        var attachmentExtAutoCompleteParams = $.extend({}, autocomplete_params);
        $('#attachmentExtension').autocomplete(attachmentExtAutoCompleteParams);

    </script>

</body>
</html>
