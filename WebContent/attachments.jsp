<%@ page contentType="text/html; charset=UTF-8"%>
<%
	JSPHelper.checkContainer(request); // do this early on so we are set up
	request.setCharacterEncoding("UTF-8");
%>
<%@page language="java" import="org.json.JSONArray"%>
<%@ page import="java.util.*" %>
<%@page language="java" import="edu.stanford.muse.datacache.BlobStore"%>
<%@page language="java" import="edu.stanford.muse.datacache.Blob"%>
<%@page language="java" %>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.Document"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@page language="java" import="edu.stanford.muse.util.Pair"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="edu.stanford.muse.index.Searcher" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="edu.stanford.muse.Config" %>
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
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
	AddressBook ab = archive.addressBook;
	String bestName = ab.getBestNameForSelf();
	JSONArray resultArray = new JSONArray();

	String cacheDir = (String) JSPHelper.getSessionAttribute(session, "cacheDir");
	JSPHelper.log.info("Will read attachments from blobs subdirectory off cache dir " + cacheDir);
    List<Pair<Blob, EmailDocument>> allAttachmentsPairsList = Searcher.selectBlobs (archive, request);
    Set<Blob> allAttachments = new LinkedHashSet<>();
    for (Pair<Blob, EmailDocument> p: allAttachmentsPairsList)
        allAttachments.add (p.getFirst());
    allAttachments = allAttachments.stream().filter (b -> !b.is_image()).collect (Collectors.toSet());

    writeProfileBlock(out, archive, "",  Util.pluralize(allAttachmentsPairsList.size(), "Non-image attachment") + " (" + allAttachments.size() + " unique)");
%>

<div id="all_fields" style="margin:auto;width:1000px; padding: 10px">

    <%
        Map<String,String> attachmentTypeOptions= Config.attachmentTypeToExtensions;
    %>

    <!-- filter form submits back to the same page -->
    <form action="attachments" method="get">
        <section>
            <div class="panel">
                <div class="panel-heading">Filter attachments</div>

                <div class="one-line">
                    <div class="form-group col-sm-6">
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

                    <!--File Size-->
                    <div class="form-group col-sm-6">
                        <label for="attachmentFilesize">File Size</label>
                        <select id="attachmentFilesize" name="attachmentFilesize" class="form-control selectpicker">
                            <option value="" selected disabled>Choose File Size</option>
                            <option value="1">&lt; 5KB</option>
                            <option value="2">5-20KB</option>
                            <option value="3">20-100KB</option>
                            <option value="4">100KB-2MB</option>
                            <option value="5">&gt; 2MB</option>
                            <option value="6">Any</option>
                        </select>
                    </div>
                </div>

                <div class="one-line">
                    <!--Time Range-->
                    <div class="form-group col-sm-6">
                        <label for="time-range">Time Range</label>
                        <div id="time-range" class="date-input-group">
                            <input type = "text" value="<%=request.getParameter ("startDate")==null?"": request.getParameter("startDate")%>" id="startDate" name="startDate" class="form-control" placeholder="YYYY - MM - DD">
                            <label for="endDate">To</label>
                            <input type = "text" value="<%=request.getParameter ("endDate")==null?"": request.getParameter("endDate")%>" id="endDate" name="endDate"  class="form-control" placeholder="YYYY - MM - DD">
                        </div>
                    </div>

                    <div class="form-group col-sm-6">
                        <button type="submit" class="btn-default filter-button">Filter</button>
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

    <% if (allAttachmentsPairsList.size() > 0) { %>
        <br/>
        <div style="margin:auto; width:1000px">
            <table id="attachments">
            <thead><th>Subject</th><th>Date</th><th>Size</th><th>Attachment name</th></thead>
            <tbody>

            <%
            int count = 0;
            BlobStore blobStore = archive.blobStore;
            for (Pair<Blob, EmailDocument> p: allAttachmentsPairsList) {
                EmailDocument ed = p.getSecond();
                String docId = ed.getUniqueId();
                Blob b = p.getFirst();
                String contentFileDataStoreURL = blobStore.get_URL(b);
                String blobURL = "serveAttachment.jsp?file=" + Util.URLtail(contentFileDataStoreURL);
                String messageURL = "browse?docId=" + docId;
                String subject = !Util.nullOrEmpty(ed.description) ? ed.description : "NONE";

                JSONArray j = new JSONArray();
                j.put (0, Util.escapeHTML(subject));
                j.put (1, ed.dateString());
                j.put (2, b.size);
                j.put (3, Util.escapeHTML(Util.ellipsize(b.filename, 50)));

                // urls for doc and blob go to the extra json fields, #4 and #5. #6 contains the full filename, shown on hover, since [3] is ellipsized.
                j.put (4, messageURL);
                j.put (5, blobURL);
                j.put (6, Util.escapeHTML(b.filename));

                resultArray.put (count++, j);
            }

            %>
            </tbody>
            </table>
        </div>
    <% } %>

<br/>
<jsp:include page="footer.jsp"/>
<script>

$(document).ready(function() {
    //installing input tags as datepickers
    //setting min date as 1 jan 1960. The format is year,month,date. Month is 0 based and all other are 1 based
    $('#startDate').datepicker({
        minDate: new Date(1960, 1 - 1, 1),
        dateFormat: "yy-mm-dd"
    });
    $('#endDate').datepicker({
            minDate: new Date(1960, 1 - 1, 1),
            dateFormat: "yy-mm-dd"
    });

    var clickable_message = function ( data, type, full, meta ) {
		return '<a target="_blank" href="' + full[4] + '">' + data + '</a>';
	};
	var clickable_attachment = function ( data, type, full, meta ) {
		return '<a title="' + full[6] + '" target="_blank" href="' + full[5] + '">' + data + '</a>';
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

    $.fn.dataTableExt.oSort['sort-kb-desc']  = function(x,y) { return -1 * $.fn.dataTableExt.oSort['sort-kb-asc'](x,y); }

    var attachments = <%=resultArray.toString(4)%>;
	$('#attachments').dataTable({
		data: attachments,
		pagingType: 'simple',
		columnDefs: [{targets: 0,render:clickable_message},
            {targets:3,render:clickable_attachment},
            {targets:1,width:'180px',className: "dt-right"},
            {targets:2,render:sortable_size,width:'100px',type:'sort-kb',className: "dt-right"}], // no width for col 0 here because it causes linewrap in data and size fields (attachment name can be fairly wide as well)
		order:[[1, 'asc']], // col 1 (date), ascending
		fnInitComplete: function() { $('#attachments').fadeIn();}
	});
});
</script>
</body>
</html>
