<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page contentType="text/html; charset=UTF-8"%>

<%@include file="../getArchive.jspf" %>
<html>
<head>
<Title>Export</Title>
<jsp:include page="../css/css.jsp"/>
<link rel="icon" type="image/png" href="images/muse-favicon.png">
<script type="text/javascript" src="js/protovis.js?v=1.1"></script>
<script type="text/javascript" src="js/muse.js"></script>
</head>
<body>

<%
	JSPHelper.logRequest(request);

	if (archive == null)
	{
		if (!session.isNew())
			session.invalidate();
		%>
	    <script type="text/javascript">window.location="index.jsp";</script>
		<%
		System.err.println ("Error: session has timed out");
		return;
	}
	AddressBook addressBook = archive.addressBook;

	List<DatedDocument> list = new ArrayList<DatedDocument>((List) archive.getAllDocs());
	Collections.sort(list);
	
	int count = 0, sentCount = 0, recdCount = 0;
	int totalNames = 0;
	int multiWordsNames = 0;
	int multiContactMessages = 0;
	Map<String, Integer> unidMap = new LinkedHashMap<String, Integer>();
	String dir = "/tmp/creeley/";
	new File(dir).mkdirs();
	
	for (edu.stanford.muse.index.Document d: archive.getAllDocs())
	{
		int x = ((EmailDocument) d).sentOrReceived(addressBook); 
		boolean sent = (x & EmailDocument.SENT_MASK) != 0;
		String orig = archive.getContents(d, true);
		String full = archive.getContents(d, false);
		String suffix = (sent) ? "sent." + sentCount : "received." + recdCount;
		if (sent) { sentCount++; } else { recdCount++; }
		PrintStream ps = new PrintStream(new FileOutputStream(dir + "noquoted." + suffix));
		ps.println(orig);		
		ps.close();
		ps = new PrintStream(new FileOutputStream(dir + "withquoted." + suffix));
		ps.println(full);		
		ps.close();
	}

%>
<hr/>
</div> <!-- div_main -->
<jsp:include page="../footer.jsp"/>
</body>
</html>
