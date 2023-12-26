<%@page contentType="text/html; charset=UTF-8"%>
<%!
    private boolean IsNormalized=false;
%><%
	JSPHelper.checkContainer(request); // do this early on so we are set up
	request.setCharacterEncoding("UTF-8");
%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.util.*" %>
<%@page import="edu.stanford.muse.datacache.BlobStore"%>
<%@page import="edu.stanford.muse.datacache.Blob"%>
<%@page import="edu.stanford.muse.util.Util"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@page import="java.util.stream.Collectors" %>
<%@page import="edu.stanford.muse.Config" %>
<%@page import="com.google.common.collect.Multimap" %>
<%@page import="edu.stanford.muse.index.*" %>
<%@include file="getArchive.jspf" %>

<!DOCTYPE HTML>
<html lang="en">
<head>
	<title>Other Attachments</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link href="css/selectpicker.css" rel="stylesheet" type="text/css" media="screen" />
	<jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="js/jquery-ui/jquery-ui.css">
    <link rel="stylesheet" href="js/jquery-ui/jquery-ui.theme.css">
    <script src="js/jquery.js"></script>
    <script src="js/jquery-ui/jquery-ui.js"></script>

	<script src="js/jquery.dataTables.min.js"></script>
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/modernizr.min.js"></script>
    <script src="js/selectpicker.js"></script>

	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	
	<style type="text/css">
      .js #attachments {display: none;}
    </style>

<script type="text/javascript" charset="utf-8">
		$('html').addClass('js'); // see http://www.learningjquery.com/2008/10/1-way-to-avoid-the-flash-of-unstyled-content/
</script>

    <style>
        .date-input-group {  display: flex; }
        .date-input-group .form-control { width:45%;}
        .date-input-group label {  padding: 5px 18px;  font-size: 14px;  }

        .filter-button { margin-top:38px; margin-right:17px; position:absolute; right: 0px;}
        .btn-default { height: 37px; }
         label {  font-size: 14px; padding-bottom: 13px; font-weight: 400; color: #404040; } /* taken from form-group label in adv.search.scss */
        .one-line::after {  content:"";  display:block;  clear:both; }  /* clearfix needed, to take care of floats: http://stackoverflow.com/questions/211383/what-methods-of-clearfix-can-i-use */
        .form-group { margin-bottom: 25px;}
    </style>
</head>
<body>
<%@include file="header.jspf"%>
<script>epadd.nav_mark_active('Browse');</script>

<%
	JSONArray resultArray = new JSONArray();

	String cacheDir = (String) JSPHelper.getSessionAttribute(session, "cacheDir");
	JSPHelper.log.info("Will read attachments from blobs subdirectory off cache dir " + cacheDir);
    // convert req. params to a multimap, so that the rest of the code doesn't have to deal with httprequest directly
    Multimap<String, String> params = JSPHelper.convertRequestToMap(request);
    SearchResult inputSet = new SearchResult(archive,params);
    SearchResult resultSet = SearchResult.selectBlobs(inputSet);
    Set<Document> docset = resultSet.getDocumentSet();
    List<Blob> allAttachments = new LinkedList<>();
    for (Document doc: docset){
        EmailDocument edoc = (EmailDocument)doc;
        final BlobStore blobstore = archive.getBlobStore();
        //get all attachments of edoc which satisifed the given filter.
        List<Blob> tmp = resultSet.getAttachmentHighlightInformation(edoc).stream().filter(b-> !blobstore.is_image(b)).collect(Collectors.toList());
        allAttachments.addAll(tmp);
    }

    Set<Blob> uniqueAttachments = new LinkedHashSet<>(allAttachments);

    writeProfileBlock(out, archive,  Util.pluralize(allAttachments.size(), "non-image attachment") +
            " (" + uniqueAttachments.size() + " unique)");
%>

<div id="all_fields" style="margin:auto;width:1100px; padding: 10px 0px">

    <%
        Map<String,String> attachmentTypeOptions= Config.attachmentTypeToExtensions;
    %>

    <!-- filter form submits back to the same page -->
    <form action="attachments" method="get">
        <!-- add archiveID as hidden argument -->
        <input type="hidden" value="<%=archiveID%>" name="archiveID"/>

        <section>
            <div class="panel">
                <%--<div class="panel-heading">Filter attachments</div>--%>

                <div class="one-line">
                    <div class="form-group col-sm-5">
                        <%--<label for="attachmentType">Type</label>--%>
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

                    <!--File Size-->
                    <div class="form-group col-sm-2">
                        <%--<label for="attachmentFilesize">File Size</label>--%>
                        <select title="Attachment file size" id="attachmentFilesize" name="attachmentFilesize" class="form-control selectpicker empty">
                            <option value="" selected disabled>File Size</option>
                            <option value="1">&lt; 5KB</option>
                            <option value="2">5-20KB</option>
                            <option value="3">20-100KB</option>
                            <option value="4">100KB-2MB</option>
                            <option value="5">&gt; 2MB</option>
                            <option value="6">Any</option>
                        </select>
                    </div>
                     <!--Time Range-->
                    <div class="form-group col-sm-4">
                        <%--<label for="time-range">Time Range</label>--%>
                        <div id="time-range" class="date-input-group">
                            <input type = "text" value="<%=request.getParameter ("startDate")==null?"": request.getParameter("startDate")%>" id="startDate" name="startDate" class="form-control" placeholder="From" readonly="true" style="cursor: pointer;background:white;">
                            <%--<label for="endDate"></label>--%>
                            <input type = "text" value="<%=request.getParameter ("endDate")==null?"": request.getParameter("endDate")%>" id="endDate" name="endDate"  class="form-control" placeholder="To" readonly="true" style="cursor: pointer;background:white;">
                        </div>
                    </div>

                    <div class="form-group col-sm-1">
                        <button type="submit" class="btn-default">Filter</button>
                    </div>
                </div>
            </div>
        </section>
    </form>
    <!-- end filter form -->

    <!-- initialize filter fields per request params -->
    <% {
        String vals[] = request.getParameterValues ("attachmentFilesize");
        if (vals != null) {
            String jsValString = "[";
            for (String v: vals) { jsValString += "'" + v + "',"; }
            if (vals.length > 0) {
                jsValString = jsValString.substring(0, jsValString.length() - 1);
                jsValString += "]";
            }
    %>
    <script>
        // need a little delay for this to take effect
        $(document).ready (function() { setTimeout (function() { $('#attachmentFilesize').selectpicker ('val', <%=jsValString%>); }, 1000); } );
    </script>
    <% }
    }
    %>

    <% {
        String vals[] = request.getParameterValues ("attachmentType");
        if (vals != null) {
            String jsValString = "[";
            for (String v: vals) { jsValString += "'" + v + "',"; }
            if (vals.length > 0) {
                jsValString = jsValString.substring(0, jsValString.length() - 1);
                jsValString += "]";
            }
    %>
    <script>
        // need a little delay for this to take effect
        $(document).ready (function() { setTimeout (function() { $('#attachmentType').selectpicker ('val', <%=jsValString%>); }, 1000); } );
    </script>
    <% }
    }
    %>

</div>

    <% if (docset.size() > 0) {


            int count = 0;
            BlobStore blobStore = archive.blobStore;
            for (Document  doc: docset) {
                EmailDocument ed = (EmailDocument)doc;
                String docId = ed.getUniqueId();
                //get the set of attachments matching in this document against search query.
                List<Blob> blobs = resultSet.getAttachmentHighlightInformation(ed);
                for( Blob b : blobs) {
                    String contentFileDataStoreURL = blobStore.get_URL_Normalized(b);
                    String blobURL = "serveAttachment.jsp?archiveID="+archiveID+"&file=" + Util.URLtail(contentFileDataStoreURL);
                    String messageURL = "browse?archiveID="+archiveID+"&docId=" + docId;
                    String subject = !Util.nullOrEmpty(ed.description) ? ed.description : "NONE";
                    String displayFileName = archive.getBlobStore().full_filename_normalized(b,false);

                    JSONArray j = new JSONArray();
                    j.put(0, Util.escapeHTML(subject));
                    j.put(1, ed.dateString());
                    j.put(2, b.size);
                    j.put(3, Util.escapeHTML(Util.ellipsize(displayFileName, 50)));
                    boolean isNormalized = archive.getBlobStore().isNormalized(b);
                    boolean isCleanedName = archive.getBlobStore().isCleaned(b);
                    String cleanupurl = archive.getBlobStore().get_URL_Cleanedup(b);

                    // urls for doc and blob go to the extra json fields, #4 and #5. #6 contains the full filename, shown on hover, since [3] is ellipsized.
                    j.put(4, messageURL);
                    j.put(5, blobURL);
                    j.put(6, Util.escapeHTML(displayFileName));
                    String msg="";
                    if(isNormalized || isCleanedName){
                        String completeurl_cleanup ="serveAttachment.jsp?archiveID="+archiveID+"&file=" + Util.URLtail(cleanupurl);

                        if(isNormalized){
                            msg="This file was converted during the preservation process. Its original name was "+blobStore.full_filename_original(b,false)+". Click <a href="+completeurl_cleanup+">here </a> to download the original file";
                        }
                        else if(isCleanedName){
                            msg="This file name was cleaned up during the preservation process. The original file name was "+blobStore.full_filename_original(b,false);
                        }
                        j.put(7,msg);
                        IsNormalized=true;
                    }
                    resultArray.put(count++, j);
                }
            }

            %>
            <br/>
            <div style="margin:auto; width:1100px">
            <table id="attachments">
            <% if(IsNormalized) {%>
                <thead><tr><th>Subject</th><th>Date</th><th>Size</th><th>Attachment name</th><th>More Infomration</th></tr></thead>
            <%} else {%>
                <thead><tr><th>Subject</th><th>Date</th><th>Size</th><th>Attachment name</th></tr></thead>
                <%}%>
                <tbody>

            </tbody>
            </table>
        </div>
    <% } %>

<br/>
<jsp:include page="footer.jsp"/>
<script>
    $('body').on('click','#normalizationInfo',function(e){
        // get the attribute's values - originalURL and originalName.
        var message = $(e.target).data('normalization-info');
        $('#normalization-description').html (message);
        $('#normalization-info-modal').modal('show');

    });

$(document).ready(function() {
    //installing input tags as datepickers
    //setting min date as 1 jan 1960. The format is year,month,date. Month is 0 based and all other are 1 based
    $('#startDate').datepicker({
        minDate: new Date(1960, 1 - 1, 1),
        dateFormat: "yy-mm-dd",
        changeMonth: true,
        changeYear: true,
        yearRange: "1930:2030"
    });
    $('#endDate').datepicker({
        minDate: new Date(1960, 1 - 1, 1),
        dateFormat: "yy-mm-dd",
        changeMonth: true,
        changeYear: true,
        yearRange: "1930:2030"
    });

    var clickable_message = function ( data, type, full, meta ) {
		return '<a target="_blank" href="' + full[4] + '">' + data + '</a>';
	};
	var clickable_attachment = function ( data, type, full, meta ) {
//	    var moreinfo = "<span class=\"glyphicon glyphicon-info-sign\" id=\"normalizationInfo\" </span>";
        return '<a title="' + full[6] + '" target="_blank" href="' + full[5] + '">' + data + '</a>' ;
	};
	var clickable_normalization_info = function(data,type,full,meta){
	    if(full[7]){
            return "<span class=\"glyphicon glyphicon-info-sign\" id=\"normalizationInfo\" data-normalization-info=\""+full[7]+"\"</span>";
        }else
            return '';
    };
    var sortable_size = function(data, type, full, meta) {
        return Math.floor(full[2]/1024) + " KB";
    };

    $.fn.dataTableExt.oSort['sort-kb-asc']  = function(x,y) {
        console.log('x =' + x + ' y = ' + y);
        x = parseInt(x.substring(0, x.indexOf(' KB')));
        y = parseInt(y.substring(0, y.indexOf(' KB')));
        console.log('x =' + x + ' y = ' + y);
        return ((x < y) ? -1 : ((x > y) ?  1 : 0));
    };

    $.fn.dataTableExt.oSort['sort-kb-desc']  = function(x,y) { return -1 * $.fn.dataTableExt.oSort['sort-kb-asc'](x,y); };

    var attachments = <%=resultArray.toString(5)%>;
	$('#attachments').dataTable({
		data: attachments,
		pagingType: 'simple',
		autoWidth: false,
		columnDefs: [{targets: 0,render:clickable_message},
            {targets:3,render:clickable_attachment},
            <%if(IsNormalized){%>
            {targets:4, render:clickable_normalization_info},
            <% } %>
            {targets:1,width:'180px',className: "dt-right"},
            {targets:2,render:sortable_size,width:'100px',type:'sort-kb',className: "dt-right"}], // no width for col 0 here because it causes linewrap in data and size fields (attachment name can be fairly wide as well)
		order:[[1, 'asc']], // col 1 (date), ascending
		fnInitComplete: function() { $('#attachments').fadeIn();}
	});
});
</script>
<div>
    <div id="normalization-info-modal" class="info-modal modal fade" style="z-index:99999">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <%--<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>--%>
                    <%--<h4 class="modal-title">Confirm</h4>--%>
                </div>
                <div class="modal-body">
                    <span id="normalization-description"></span>
                </div>
                <div class="modal-footer">
                    <%--<button id='append-button' type="button" class="btn btn-default" data-dismiss="modal">Append</button>--%>
                    <%--<button id='overwrite-button' type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>--%>
                    <%--<button id='cancel-button' type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>--%>
                </div>
            </div><!-- /.modal-content -->
        </div><!-- /.modal-dialog -->
    </div><!-- /.modal -->
</div>

</body>
</html>
