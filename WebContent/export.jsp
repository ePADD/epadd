<%
/*    
    2022-10-28      Allow custom naming in Export to next ePADD module
*/
%>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.webapp.ModeConfig" %>
<%@ page import="java.util.Set" %>
<%@ page import="edu.stanford.muse.Config" %>
<%@ page import="java.util.Map" %>
<%@ page import="edu.stanford.muse.AddressBookManager.AddressBook" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@page language="java" import="edu.stanford.epadd.util.EmailConvert"%>
<%@ page import="edu.stanford.muse.index.EmailExporter" %>
<%@ page import="edu.stanford.muse.ner.model.NEType" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@page language="java" %>
<%	EmailConvert.activateLicense();%>

    <!DOCTYPE HTML>
<html>
<head>


    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">


    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
    <link href="css/selectpicker.css" rel="stylesheet" type="text/css" media="screen" />
    <jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="css/sidebar.css?v=1.1">
    <link rel="stylesheet" href="css/main.css?v=1.1">
    <link rel="stylesheet" href="css/bootstrap-dialog.css">

    <script src="js/jquery.js"></script>

    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="jqueryFileTree/jqueryFileTree.js"></script>
    <script src="js/jquery.autocomplete.js" type="text/javascript"></script>

    <script src="js/filepicker.js?v=1.1"></script>
    <script src="js/selectpicker.js?v=1.1"></script>
    <script src="js/modernizr.min.js"></script>
    <script src="js/sidebar.js?v=1.1"></script>

    <script src="js/muse.js"></script>
    <script src="js/epadd.js?v=1.1"></script>
    <script src="js/bootstrap-dialog.js"></script>

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
        .buttonIn {
                width: 640px;
                position: relative;
        }
        .folder-button {
                position: absolute;
                top: 10;
                border-radius: 5px;
                right: 6px;
                z-index: 2;
                border: none;
                top: 2px;
                height: 32px;
                cursor: pointer;
                color: white;
                background-color: #1e90ff;
                transform: translateX(2px);
        }
    </style>
</head>
<body style="background-color:white;">

<%@include file="header.jspf"%>
<title> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.head-export")%> </title>

<jsp:include page="div_filepicker.jspf"/>

<script>epadd.nav_mark_active('Export');</script>


<div class="nav-toggle1 sidebar-icon">
    <img src="images/sidebar.png" alt="sidebar">
</div>
<nav class="menu1" role="navigation">
    <h2><b><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "export.help.head")%></b></h2>
    <!--close button-->
    <a class="nav-toggle1 show-nav1" href="#">
        <img src="images/close.png" class="close" alt="close">
    </a>

    <div class="search-tips" style="display:block">

        <% if (ModeConfig.isAppraisalMode()) { %>
            <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "export.help.appraisal")%>

        <% } else if (ModeConfig.isProcessingMode()) { %>
            <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "help", "export.help.processing")%>

        <% } %>
    </div>
</nav>

<%
    Archive archive = JSPHelper.getArchive(request);
    String bestName = "";
    String bestEmail = "";
    if (archive != null) {
        AddressBook ab = archive.addressBook;
// 2022-10-28        
        bestName = ab.getBestNameForSelf();
        Set<String> addrs = ab.getOwnAddrs();
        if (addrs.size() > 0)
            bestEmail = addrs.iterator().next();
        writeProfileBlock(out, archive, edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.profile-export"));
    }
    if (!ModeConfig.isProcessingMode() && !ModeConfig.isAppraisalMode()) {
%>
Error: Export is only available in processing or appraisal modes!
<%
        return;
    }
    int messagesToExport = 0, annotatedMessages = 0, restrictedMessages = 0, messagesNotToExport = 0;
    /*
	@TODO-Export Take decision on exporting based on labels of this document set
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
    }*/
%>

<p>

<div id="all_fields" style="margin:auto; width:1100px; padding: 10px">
 
    <section>
            <div class="panel" id="export-preservation">
                <div class="panel-heading">Export to Preservation<br>
                    Emailchemy license: <span id="license" style="color:orange">Retrieving status ...</span><br/>
                </div>
                <div class="one-line">
                    <div class="form-group col-sm-8">
                        <select id="export-preservation-options" name="export-preservation-options" class="form-control selectpicker">
                            <option value="" selected disabled><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.select")%></option>
                            <% if (ModeConfig.isAppraisalMode()) { %>
                                <option value = <%=EmailExporter.EXPORT_APPRAISED_MBOX%>>Export appraised emails in Mbox format</option>
                                <option value = <%=EmailExporter.EXPORT_APPRAISED_EML%>>Export appraised emails in EML format</option>

                            <% } else if (ModeConfig.isProcessingMode()) { %>
                                <option value = <%=EmailExporter.EXPORT_PROCESSED_MBOX%>>Export processed emails in Mbox format</option>
                                <option value = <%=EmailExporter.EXPORT_PROCESSED_EML%>>Export processed emails in EML format</option>
                            <% } %>
                        </select>
                    </div>
                    <div class="form-group col-sm-4">
                        <button id="export-mbox-do" class="go-button  btn-default"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.export-button")%></button>
                    </div>
                </div>
                <br/>
            </div>
        </section>

    <section>
        <div class="panel">
            <div class="panel-heading"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.next-module")%></div>
            <% if (ModeConfig.isProcessingMode()) { %>
            <div class="one-line">
                <fieldset name="message-filter" id="message-filter" class="comman-radio">
                     <label class="radio-inline">
                        <input name="message-filter" value="non-restricted" type="radio" checked>
                        <span class="text-radio"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.mess-filter-non-restricted")%></span>
                    </label>
                    <label class="radio-inline">
                        <input name="message-filter" value="permissive" type="radio">
                        <span class="text-radio"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.mess-filter-permissive")%></span>
                    </label>
                </fieldset>
            </div>
            <br/>
            <% } %>
            <div class="one-line" id="export-next">
                <div class="form-group col-sm-8">
                    <label for="export-next-file"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.specify-location")%></label>
                    <div class="buttonIn">
                    <input id="export-next-file" class="dir form-control" type="text" name="name" value=""/>
<%--						<button id="expor2t-next-create" class="btn-default folder-button" onclick=createFolder(document.getElementById('export-next-file'));>New Folder</button>--%>
                    </div>
                </div>
                <div class="form-group col-sm-4 picker-buttons">
                    <button id="export-next-browse" class="btn-default browse-button"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.browse-button")%></button>
                </div>
<%--                <div class="form-group col-sm-8">--%>
<%--                    <label><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.export-archive")%></label>--%>
<%--                    <input id="export-next-dir" class="form-control archive_name" type="text" name="archive_name" value="ePADD archive of <%=bestName%>" />--%>
<%--                </div>    --%>
                <div class="form-group col-sm-4 picker-buttons">    
                    <button id="export-next-do" style="margin-left: 10px;" class="go-button faded btn-default"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.export-button")%></button>
                </div>
            </div>
        </div>
    </section>

    <%--</div>--%>
    <%--</section>--%>

    <section>
        <div class="panel">
            <div class="panel-heading"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.export-attachments")%> </div>



            <div class="one-line">
                <div class="checkbox-inline" style="padding:0px 0px 0px 15px">
                    <label>
                        <input type="checkbox" name="unprocessedOption" checked>
                        <span class="label-text"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.unrecog-by-apache")%></span>
                    </label>
                </div>
            </div>

            <%
                Map<String,String> attachmentTypeOptions= Config.attachmentTypeToExtensions;
            %>
            <div class="one-line">
                <div class="advanced-search form-group col-sm-6" style="padding:0px 0px 0px 15px">
                    <label for="attachmentType"> <%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.file-type")%> </label>
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
                    <label for="attachmentExtension"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.other-extension")%></label>
                    <input id="attachmentExtension" name="attachmentExtension" id="attachmentExtension" type="text" class="form-control">
                </div>

                <br/>

            </div>

            <div class="one-line" id="export-attach">
                <div class="form-group col-sm-8">
                    <label for="export-attach-file"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.specify-location")%></label>
                    <div class="buttonIn">
                    <input id="export-attach-file" class="dir form-control" type="text" name="name" value=""/>
<%--						<button id="export-attach-create" class="btn-default folder-button" onclick=createFolder(document.getElementById('export-attach-file'));>New Folder</button>--%>
                    </div>
                </div>
                <div class="form-group col-sm-4 picker-buttons">
                    <button id="export-attach-browse" class="btn-default browse-button"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.browse-button")%></button>
                    <button id="export-attach-do" style="margin-left: 10px;" class="go-button faded btn-default"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.export-button")%></button>
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
// 2022-09-09           window.location = baseUrl + '?archiveID=<%=archiveID%>&dir=' + dir + '&type=' + type + '&ext=' + ext + '&unprocessedonly=' + $('input[name="unprocessedOption"]').prop('checked');
						window.location = encodeURI(baseUrl + '?archiveID=<%=archiveID%>&dir=' + dir + '&type=' + type + '&ext=' + ext + '&unprocessedonly=' + $('input[name="unprocessedOption"]').prop('checked'));
                });
            </script>

            <br/>
        </div>
    </section>

    <% if (ModeConfig.isProcessingMode()) { %>

    <section>
        <div class="panel" id="export-headers">
            <div class="panel-heading"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.headers-file")%></div>

            <div class="one-line">
                <div class="form-group col-sm-8">
                    <label for="export-headers-file"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.headers-file-specify-location")%></label>
                    <input id="export-headers-file" class="dir form-control" type="text" name="name" value=""/>
                </div>
                <div class="form-group col-sm-4 picker-buttons">
                    <button id="export-headers-browse" class="btn-default browse-button"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.headers-file-browse")%></button>
                    <button id="export-headers-do" style="margin-left: 10px;" class="go-button faded btn-default"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.headers-file-export")%></button>
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
                window.location = baseUrl + '?archiveID=<%=archiveID%>&exportType=csv&dir=' + dir;
        });
    </script>
    <% } %>

    <% if (ModeConfig.isProcessingMode() || ModeConfig.isAppraisalMode()) { %>
    <section>
        <div class="panel" id="export-mbox">
            <div class="panel-heading"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.export-message")%></div>
<%--            <% if (ModeConfig.isProcessingMode()) {%>--%>
<%--                <div class="one-line">--%>
<%--                    <div class="form-group col-sm-8">--%>
<%--                    <label for="export-mbox"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.email-store")%></label>--%>
<%--                    <select id="export-mbox-copy-options" name="export-mbox-copy-options" class="form-control selectpicker">--%>
<%--                        <option value="" selected disabled><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.select")%></option>--%>
<%--                            <option value = "redacted"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.email-store-redacted")%></option>--%>
<%--                        <option value = "preserved"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.email-store-preserved")%></option>--%>
<%--                    </select>--%>
<%--                    </div>--%>
<%--                </div>--%>
<%--            <br/>--%>
<%--                        <% } else { %>--%>
<%--                        <input type="hidden" id="export-mbox-copy-options" name="export-mbox-copy-options" value="redacted"/>--%>
<%--                        <% } %>--%>

                        Emailchemy license:<br>
<span id="licenseDownload"
      style="color:orange;margin-left: 40px">Retrieving status ...</span><br/>
<span id="licenseDownload2"
                              style="margin-left: 40px"></span><br/><br/>
<label for=downloadformat>Download format</label>
<fieldset id="downloadformat" class="comman-radio">
    <label class="radio-inline">
        <input type="radio" id="mbox" name="formatradiobutton">
        <span class="text-radio">MBOX</span>
    </label>

    <label class="radio-inline">
        <input type="radio" id="eml" name="formatradiobutton">
        <span class="text-radio">EML</span>
    </label>
</fieldset>


            <div class="one-line">
                <div class="form-group col-sm-8">
                    <label for="export-mbox"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.mess-exported")%></label>
                    <select id="export-mbox-options" name="export-mbox-options" class="form-control selectpicker">
                        <option value="" selected disabled><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.select")%></option>
                        <option value = "all"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.mess-all")%></option>
                        <option value = "restricted"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.mess-restricted")%></option>
                        <option value = "non-restricted"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.mess-non-restricted")%></option>
                            <option value = "permissive"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.mess-permissive")%></option>
                    </select>
                </div>
                <div class="one-line">
                    <div class="checkbox-inline" style="padding:0px 0px 0px 15px">
                        <label>
                            <input type="checkbox" id="only-headers-option" name="only-headers-option">
                            <span class="label-text"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.header-only")%></span>
                        </label>
                    </div>
                </div>
                <div class="form-group col-sm-4">
        <button id="export-mbox-do"
                class="go-button  btn-default"><%=edu.stanford.muse.util.Messages.getMessage(archiveID, "messages", "export.export-button")%>
        </button>
                </div>
            </div>


            <br/>
        </div>
    </section>


    <script>
        $('#export-preservation .go-button').click (function(e) {
            var exportoptions= $('#export-preservation-options').val();
            if(!exportoptions){
                alert("Please select at least one option!");
                return false;
            }
            var post_params={archiveID:archiveID, exportableAssets:exportoptions};
            var params = epadd.convertParamsToAmpersandSep(post_params);
            fetch_page_with_progress("ajax/async/setExportableAssets.jsp", "status", document.getElementById('status'), document.getElementById('status_text'), params);
        });

        $('#export-mbox .go-button').click (function(e) {
            var exportoptions= $('#export-mbox-options').val();
            var isEml = $('#eml').prop('checked');

            var format;
            if (isEml)
            {
                format = "to-eml";
            }
            else
            {
                format = "to-mbox";
            }
 
            // var copyType = $('#export-mbox-copy-options').val();
            // if(!exportoptions || !copyType){
            //     alert("Please select at least one option!");
            //     return false;
            // }
            // Disabled the option to choose between redacted and preserved as currently the HTML parts
            // of emails are always unredacted.
            var copyType = "preserved";
            /* var onlyHeadersOption= $('#only-headers-option').val(); */
            var onlyHeadersOption=  ($('#only-headers-option').is(':checked'))? 'on': 'off';

            var post_params={archiveID:archiveID, data:format, type:exportoptions, onlyHeaders:onlyHeadersOption, copyType:copyType};
            var params = epadd.convertParamsToAmpersandSep(post_params);
            var premisData = {eventType: "transfer to processing", eventDetailInformation: "Exported to " + post_params.dir};

             fetch_page_with_progress("ajax/downloadData.jsp", "status", document.getElementById('status'), document.getElementById('status_text'), params, null, null, premisData);
        });
    </script>
    <% } %>

    <% if (ModeConfig.isProcessingMode()) {
        Map<Short, String> entitytypes= new LinkedHashMap<>();
        entitytypes.put(Short.MAX_VALUE,"All");
        for(NEType.Type t: NEType.Type.values())
            entitytypes.put(t.getCode(), t.getDisplayName());
    %>
    <section>
        <div class="panel" id="export-entities">
            <div class="panel-heading">Export entities</div>
            <div class="one-line">
                <div class="form-group col-sm-8">
                    <select id="entityType" name="entityType" class="form-control selectpicker">
                        <option value="" selected disabled>Select</option>
                        <%for (Short s : entitytypes.keySet()){
                        %>
                        <option value = "<%=s%>"><%=entitytypes.get(s)%></option>
                        <%}%>
                    </select>
                </div>
                <div class="form-group col-sm-4">
                    <button id="export-entities-do"  class="go-button btn-default">Export</button>
                </div>
            </div>


            <br/>
        </div>
    </section>

    <script>
        $('#export-entities .go-button').click (function(e) {
            var entityType=$('#entityType').val();
            var option;
            var msg;
            if(!entityType){
                alert("Please select an entity type");
                return false;
            }
            var post_params={archiveID:archiveID, data: "entities",type:entityType};
            var params = epadd.convertParamsToAmpersandSep(post_params);
            fetch_page_with_progress("ajax/downloadData.jsp", "status", document.getElementById('status'), document.getElementById('status_text'), params);
            /*$.ajax({
                type: 'POST',
                url: "ajax/downloadData.jsp",
                data: {archiveID: archiveID, data: "entities",type:entityType},
                dataType: 'json',
                success: function (data) {
                    epadd.alert('List of selected entities will be downloaded in your download folder!', function () {
                        window.location=data.downloadurl;
                    });
                },
                error: function (jq, textStatus, errorThrown) {
                    var message = ("Error Exporting file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
                    epadd.log(message);
                    epadd.alert(message);
                }
            });*/
        });
    </script>

    <section>
        <div class="panel" id="export-auth">
            <div class="panel-heading">Export correspondents</div>
            <div class="one-line">
                <div class="form-group col-sm-8">
                    <select id="correspondentType" name="correspondentType" class="form-control selectpicker">
                        <option value="" selected disabled>Select</option>
                        <option value="confirmed">Confirmed Correspondents</option>
                        <option value="unconfirmed">Unconfirmed Correspondents</option>
                    </select>
                </div>
                <div class="form-group col-sm-4">
                    <button id="export-auth-do"  class="go-button btn-default">Export</button>
                </div>
            </div>


            <br/>
        </div>
    </section>

    <script>
        $('#export-auth .go-button').click (function(e) {
            var correspondentType=$('#correspondentType').val();
            if(!correspondentType)
            {
                alert("Please select a correspondent type");
                return false;
            }
            var option;
            if(correspondentType=="confirmed")
                option="confirmedCorrespondents";
            else
                option="unconfirmedCorrespondents";
            $.ajax({
                type: 'POST',
                url: "ajax/downloadData.jsp",
                data: {archiveID: archiveID, data: option},
                dataType: 'json',
                success: function (data) {
                    epadd.info_confirm_continue('The file correspondents.csv will be downloaded to your browser\’s download folder.', function () {
                        window.location=data.downloadurl;
                    });
                },
                error: function (jq, textStatus, errorThrown) {
                    epadd.error("Error exporting file, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
                }
            });
        });
    </script>


    <section>
        <div class="panel" id="export-messages-text">
            <div class="panel-heading">Original text of all non-restricted messages as individual TXT files</div>
            <div class="one-line">
                <div class="form-group col-sm-8">
                    <div class="form-group col-sm-4">
                        <button id="export-messages-text-do"  class="go-button btn-default">Export</button>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <script>
        $('#export-messages-text .go-button').click (function(e) {
            var post_params={archiveID:archiveID, data: "originaltextasfiles"};
            var params = epadd.convertParamsToAmpersandSep(post_params);
            fetch_page_with_progress("ajax/downloadData.jsp", "status", document.getElementById('status'), document.getElementById('status_text'), params);
        });
    </script>
    <% } %>


</div> <!--  all fields -->

<p>

    <script type="text/javascript">
        $(document).ready(function() {
            new FilePicker($('#export-next'));
            //new FilePicker($('#export-mbox'));
            new FilePicker($('#export-attach'));
//            new FilePicker($('#export-auth'));
            new FilePicker($('#export-headers'));

           setInterval(fetchLicenseStatus, 3000);

        });
		
        function fetchLicenseStatus() {
            $.ajax({
                    type: "POST",
                    url: "ajax/licenseStatus",
                    success: function (result) {
                        console.log("RESULT " + result);
                        document.getElementById('license').innerHTML = result;
                        document.getElementById('licenseDownload').innerHTML = result;

                        if (result.includes("License active")) {
                            document.getElementById('license').setAttribute("style", "color:green;");
                            document.getElementById('license').innerHTML += "- Conversion for export to EML fully working"

                            document.getElementById('licenseDownload').setAttribute("style", "color:green; margin-left: 40px;");

                            document.getElementById('licenseDownload2').setAttribute("style", "color:green; margin-left: 40px;");
                            document.getElementById('licenseDownload2').innerHTML = "Conversion for downloading EML files fully working"
                        } else {
                            document.getElementById('license').setAttribute("style", "color:red");
                            document.getElementById('license').innerHTML += "- Conversion for export to EML working in demo mode"

                            document.getElementById('licenseDownload').setAttribute("style", "color:red; margin-left: 40px;");

                            document.getElementById('licenseDownload2').setAttribute("style", "color:red; margin-left: 40px;");
                            document.getElementById('licenseDownload2').innerHTML = "Conversion for downloading EML files working in demo mode"
                        }
                    }
                }
            )
        }

        function createFolder(obj) {
                    BootstrapDialog.show({
                        title: 'Create Folder',
                        message: 'New Folder: <input type="text" class="form-control" value="' + obj.value +'">',
                        data: {
                          'folder': obj.value
                        },
                        buttons: [
                          {
                            id: 'btn-ok',   
//                            icon: 'glyphicon glyphicon-check',       
                            label: 'OK',
                            cssClass: 'btn-default', 
                            autospin: false,
                            action: function(dialogRef){    
                                $.ajax({
                                    type: "POST",
                                    url: "ajax/newFolder",
                                    data: { folder: dialogRef.getModalBody().find('input').val() },
                                    success: function(response) {
//                                        alert(response['reason']);
                                        if (response['result'] === "ok") {
                                            BootstrapDialog.show({message:response['reason'], type: BootstrapDialog.TYPE_SUCCESS});
                                            obj.value = dialogRef.getModalBody().find('input').val().replaceAll('\\', '/');
                                            dialogRef.close();
                                        } else {
                                            BootstrapDialog.show({message:response['reason'], type: BootstrapDialog.TYPE_WARNING});
                                        }
                                    },
                                    error: function() {
//                                        alert(response['reason']);
                                        BootstrapDialog.show({message:response['reason'], type: BootstrapDialog.TYPE_WARNING});
                                    }
                                });                                
                            }
                          },
                          {
                            id: 'btn-cancel',   
//                            icon: 'glyphicon glyphicon-check',       
                            label: 'Cancel',
                            cssClass: 'btn-default', 
                            autospin: false,
                            action: function(dialogRef){    
                                dialogRef.close();
                            }    
                          }
                        ]
                    });                    
        }    
		
        $('#export-next .go-button').click (function(e) {
            var $button = $(e.target);
            if ($button.hasClass('faded'))
                return false; // do nothing;
            var baseUrl = '<%=ModeConfig.isProcessingMode() ? "ajax/async/export-from-processing.jsp":"ajax/async/export-from-appraisal.jsp"%>';
            var dir = $('.dir', $('#export-next')).val();
            var archive_name = $('.archive_name').val();
            if (dir && dir.length > 0) {
                if(<%=ModeConfig.isAppraisalMode()%>) {
                    var promptmethod = function (dir) {
                        return function () {
                            var msg = "ePADD archive exported to: " + dir + "\n." + " Please zip this folder and submit it to the archives.";
                            epadd.success(msg);
                        }
                    }(dir);
                }else{
                    var promptmethod = function (dir) {
                        return function () {
                            var msg1 = "Full ePADD archive exported to: "+dir+"/-Delivery. Please copy this folder under &lt;HOME&gt;/<%=Util.filePathTail(Config.REPO_DIR_DELIVERY)%> and start ePADD in delivery mode.";
                            var msg2 = "Public ePADD archive (containing only named entities) exported to: "+dir+"/-Discovery. Please copy this folder under &lt;HOME&gt;/<%=Util.filePathTail(Config.REPO_DIR_DISCOVERY)%> and start ePADD in discovery mode.";
                            epadd.success(msg1+"\n" +msg2);
                        }
                    }(dir);
                }
                //window.location = baseUrl + '?archiveID=<%=archiveID%>&dir=' + dir;
// 2022-10-28
//                var post_params = {archiveID: archiveID, dir: dir};

                var messFilter = $("input[name='message-filter']:checked").val();

                //var permLabelOnly = ($('#permissionLabelOnly').is(':checked'))? 'on' : 'off';
                var permLabelOnly = ("permissive" ===messFilter)? 'on' : 'off';

                var post_params = {
                    archiveID: archiveID,
                    dir: dir,
                    permLabelOnly: permLabelOnly
                };

                var params = epadd.convertParamsToAmpersandSep(post_params);
                var premisData = {eventType: "transfer to processing", eventDetailInformation: "Exported to " + post_params.dir};
                fetch_page_with_progress(baseUrl, "status", document.getElementById('status'), document.getElementById('status_text'), params,promptmethod, null, premisData);
            }else{
                epadd.error("Please enter the directory path where the exported archive will be stored!");
            }
        });
        var autocomplete_params = {
            serviceUrl: 'ajax/attachmentAutoComplete.jsp?extensions=1&archiveID=<%=archiveID%>',
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
