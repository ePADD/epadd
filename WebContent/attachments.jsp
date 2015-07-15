<%@ page contentType="text/html; charset=UTF-8"%>
<%
	JSPHelper.checkContainer(request); // do this early on so we are set up
	request.setCharacterEncoding("UTF-8");
%>
<%@page language="java" import="org.json.JSONArray"%>
<%@ page import="java.io.File" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.util.*" %>
<%@page language="java" import="edu.stanford.muse.datacache.BlobStore"%>
<%@page language="java" import="edu.stanford.muse.datacache.Blob"%>
<%@page language="java" import="edu.stanford.muse.datacache.FileBlobStore"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.Document"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@page language="java" import="edu.stanford.muse.util.Pair"%>
<%@page language="java" import="edu.stanford.muse.util.Util"%>
<%@page language="java" import="edu.stanford.muse.webapp.JSPHelper"%>
<%@include file="getArchive.jspf" %>
<% 	boolean selectDocType = "doc".equals(request.getParameter("type")); %>

<!DOCTYPE HTML>
<html lang="en">
<title><%=selectDocType ? "Document":"Other" %> Attachments</title>
<head>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	
	<script src="js/jquery.js"></script>
	<script src="js/jquery.dataTables.min.js"></script>
	<link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
	
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	
	<style type="text/css">
      .js #attachments {display: none;}
    </style>

<script type="text/javascript" charset="utf-8">
		$('html').addClass('js'); // see http://www.learningjquery.com/2008/10/1-way-to-avoid-the-flash-of-unstyled-content/
</script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
	AddressBook ab = archive.addressBook;
	String bestName = ab.getBestNameForSelf();
	JSONArray resultArray = new JSONArray();

	String userKey = "user";
	String rootDir = JSPHelper.getRootDir(request);
	String cacheDir = (String) JSPHelper.getSessionAttribute(session, "cacheDir");
	JSPHelper.log.info("Will read attachments from blobs subdirectory off cache dir " + cacheDir);
	
	Collection<Document> docs = JSPHelper.selectDocs(request, session, true /* only apply to filtered docs */, false);

	
	if (docs != null && docs.size() > 0)
	{
		String extra_mesg = null;
		// attachmentsForDocs
		String attachmentsStoreDir = cacheDir + File.separator + "blobs" + File.separator;
		BlobStore store = null;
		try
		{
			store = new FileBlobStore(attachmentsStoreDir);
		} catch (IOException ioe)
		{
			JSPHelper.log.error("Unable to initialize attachments store in directory: "
					+ attachmentsStoreDir + " :" + ioe);
			Util.print_exception(ioe, JSPHelper.log);
			String url = null;
		}

		Map<Blob, String> blobToSubject = new LinkedHashMap<Blob, String>();
		List<Pair<Blob, EmailDocument>> allAttachments = new ArrayList<Pair<Blob, EmailDocument>>();
		Collection<EmailDocument> eDocs = (Collection) docs;
        Set<Blob> allBlobsSet = new LinkedHashSet<Blob>();
        for (EmailDocument doc : eDocs)
        {
            List<Blob> a = doc.attachments;
            if (a != null) {
                for (Blob b : a) {
                    if (selectDocType) {
                        if (Util.is_doc_filename(b.filename)) {
                            allAttachments.add(new Pair<Blob, EmailDocument>(b, doc));
                            allBlobsSet.add(b);
                        }
                    } else {
                        if (!Util.is_doc_filename(b.filename) && !Util.is_image_filename(b.filename)) {
                            allAttachments.add(new Pair<Blob, EmailDocument>(b, doc));
                            allBlobsSet.add(b);
                        }
                    }
                }
            }
        }

        writeProfileBlock(out, bestName, "",  Util.pluralize(allAttachments.size(), (selectDocType ? "Document" : "Other") + " Attachment") + " (" + allBlobsSet.size() + " unique)");

		if (allAttachments.size() > 0) { %>
			<br/>
			<div style="margin:auto; width:1000px">
				<table id="attachments">
				<thead><th>Subject</th><th>Date</th><th>Size</th><th>Attachment name</th></thead>
				<tbody>

				<%
				int count = 0;
				BlobStore blobStore = archive.blobStore;
				for (Pair<Blob, EmailDocument> p: allAttachments) {
					EmailDocument ed = p.getSecond();
					String docId = ed.getUniqueId();
					Blob b = p.getFirst();
					String contentFileDataStoreURL = blobStore.get_URL(b);
					String blobURL = "serveAttachment.jsp?file=" + Util.URLtail(contentFileDataStoreURL);
					String messageURL = "browse?docId=" + docId;
					%>
						<!--
					<tr>
						<td class="link"><a href="<%=messageURL%>"><%=docId%></a></td>
						<td> <%=ed.dateString() %></td>
						<td><%=b.size/1000%>KB</td>
						<td class="link"><a href="<%=blobURL%>"><%=b.filename %></a></td>
					</tr>
					-->
				<%
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
<%	} %>

<br/>
<jsp:include page="footer.jsp"/>
<script>

$(document).ready(function() {
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
