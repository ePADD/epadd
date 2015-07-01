<%@ page contentType="text/html; charset=UTF-8"%>
<%
	JSPHelper.checkContainer(request); // do this early on so we are set up
	request.setCharacterEncoding("UTF-8");
%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.datacache.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="java.lang.*"%>
<%@page language="java" import="java.net.*"%>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html lang="en">
<title>Image Attachments</title>
<head>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">
	<script src="js/jquery.js"></script>
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script src="js/epadd.js"></script>
	<script type="text/javascript" src="js/muse.js"></script>
</head>  
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
	String userKey = "user";
	String rootDir = JSPHelper.getRootDir(request);
	String cacheDir = (String) JSPHelper.getSessionAttribute(session, "cacheDir");
	JSPHelper.log.info("Will read attachments from blobs subdirectory off cache dir " + cacheDir);
	
	Collection<Document> docs = JSPHelper.selectDocs(request, session, true /* only apply to filtered docs */, false);

	if (docs != null && docs.size() > 0) {
		String extra_mesg = "";

		// attachmentsForDocs
		String attachmentsStoreDir = cacheDir + File.separator
				+ "blobs" + File.separator;
		BlobStore store = null;
		try {
			store = new FileBlobStore(attachmentsStoreDir);
		} catch (IOException ioe) {
			JSPHelper.log.error("Unable to initialize attachments store in directory: "
					+ attachmentsStoreDir + " :" + ioe);
			Util.print_exception(ioe, JSPHelper.log);
			String url = null;
		}

		Map<Blob, String> blobToSubject = new LinkedHashMap<Blob, String>();
		List<Blob> allAttachments = new ArrayList<Blob>();
		Collection<EmailDocument> eDocs = (Collection) docs;
		for (EmailDocument doc : eDocs) {
			List<Blob> a = doc.attachments;
			if (a != null)
				allAttachments.addAll(a);
			if (!(Util.nullOrEmpty(doc.description)))
				for (Blob b: a)
					blobToSubject.put(b, doc.description);
		}
		
		// create a dataset object to view
		int i = new Random().nextInt();
		String randomPrefix = String.format("%08x", i);
		JSPHelper.log.info("Root dir for blobset top level page is " + rootDir);
		System.err.println("Root dir for blobset top level page is " + rootDir);
		BlobSet bs = new BlobSet(rootDir, allAttachments, store);

		String appURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
		int nEntriesForPiclens = bs.generate_top_level_page(randomPrefix, appURL, extra_mesg);
		// attachmentsForDocs
		
		String piclensRSSFilename = userKey + "/" + randomPrefix + ".photos.rss";
		String faviconPlusCSS = "<link rel=\"icon\" type=\"image/png\" href=\"images/muse-favicon.png\">\n<link href=\"css/muse.css\" rel=\"stylesheet\" type=\"text/css\"/>"; 

		// This generates the html page
		// don't include the wall if no attachments on

		/*
		if (nEntriesForPiclens > 0) {
			out.println("<span class=\"db-hint\">"
					+ Util.commatize(bs.getStats().n_total_pics)
					+ " attachments, "
					+ Util.commatize(bs.getStats().n_unique_pics) + " unique, "
					+ Util.commatize(bs.getStats().unique_data_size / 1024)
					+ " KB.");
			out.println("</span><br/>");
		}	
			*/
	AddressBook ab = archive.addressBook;
	String bestName = ab.getBestNameForSelf();
	Contact ownContact = ab.getContactForSelf();
	List<Contact> allContacts = ab.sortedContacts((Collection) docs);
	Map<Contact, Integer> contactInCount = new LinkedHashMap<Contact, Integer>(), contactOutCount = new LinkedHashMap<Contact, Integer>(), contactMentionCount = new LinkedHashMap<Contact, Integer>();
	String url = request.getRequestURI();
	System.out.println (url);
%>
<%writeProfileBlock(out, bestName, "", nEntriesForPiclens + " attachments");%>
<br/>
<br/>

<div style="text-align: center">
<% if (nEntriesForPiclens > 0) { %>
	<object id="o" classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000" width="1200" height="720">
		<param name="movie" value="cooliris/cooliris.swf" />
		<param name="flashvars" value="feed=<%= piclensRSSFilename %>" />
		<param name="allowFullScreen" value="true" />
		<param name="allowScriptAccess" value="n" />
	<embed type="application/x-shockwave-flash" src="cooliris/cooliris.swf" width="1200" height="720" flashvars="feed=<%= piclensRSSFilename %>" allowFullScreen="true" allowfullscreen="true" allowScriptAccess="never" allowscriptaccess="never"></embed>
	</object>
	<br/>
</div>
<% }
} else {
%>
		No image attachments.
<%	} %>
<jsp:include page="footer.jsp"/>

</body>
</html>
