<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page import="edu.stanford.muse.AddressBookManager.Contact"%>
<%@page import="edu.stanford.muse.util.Pair"%>
<%@page import="edu.stanford.muse.util.Util"%>

<%@page language="java" import="edu.stanford.muse.index.*"%>	
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.epadd.util.*"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@include file="../getArchive.jspf" %>

<!DOCTYPE html>
<html>
<head>
<title>Insert title here</title>
<script>
function dodrop(event) {
	alert ('event = ' + event.dataTransfer.files[0]);
}
</script>
</head>
<body>
<div id="output" style="min-height: 200px; white-space: pre; border: 1px solid black;"
     ondragenter="document.getElementById('output').textContent = ''; event.stopPropagation(); event.preventDefault();"
     ondragover="event.stopPropagation(); event.preventDefault();"
     ondrop="event.stopPropagation(); event.preventDefault();
     dodrop(event);">
     DROP FILES HERE FROM FINDER OR EXPLORER
</div>
</body>
<%
//Archive archive = JSPHelper.getArchive(session);
Indexer indexer = (Indexer) archive.indexer;

String q = "/r..bert/";
Collection<EmailDocument> docs = indexer.lookupDocs(q, Indexer.QueryType.FULL);
System.out.println("hits for: " + q + " = " + docs.size());

q = "[0-9]{4}[ \\-]*[0-9]{4}[ \\-]*[0-9]{4}[ \\-]*[0-9]{4,6}";
docs = indexer.lookupDocs(q, Indexer.QueryType.REGEX);
out.println("hits for: " + q + " = " + docs.size());

// 	Archive archive = JSPHelper.getArchive(session);
// 	AddressBook ab = archive.addressBook;
// 	Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
// 	Indexer indexer = archive.indexer;
// 	out.println(ab.allContacts().size());
// 	int numSent = 0;
// 	int min = 2050,max = -1;
// 	for(EmailDocument ed: docs){
// 		int x = ed.sentOrReceived(ab);
// 		if ((x & EmailDocument.SENT_MASK) != 0)
// 			numSent++;
// 		int year = ed.getDate().getYear()+1900;
// 		if(year == 2014)
// 			continue;
// 		min = year<min?year:min;
// 		max = year>max?year:max;
// 	}
// 	System.err.println("Numsent: "+numSent+", total: "+docs.size());
// 	System.err.println("Min: "+min+", max: "+max);
// 	String in = NLPUtils.class.getClassLoader().getResource("words").getPath();
// 	System.err.println(in); 
%>
</html>
