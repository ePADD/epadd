<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page contentType="text/html; charset=UTF-8"%>
	
	<%!
	static Set<String> prefixesToDrop = new LinkedHashSet<String>();
	static Set<Character> charsToDrop = new LinkedHashSet<Character>();
	static {
		prefixesToDrop.add("dr.");
		prefixesToDrop.add("mr.");
		prefixesToDrop.add("ms.");
		prefixesToDrop.add("mrs.");
		prefixesToDrop.add("prof.");
		prefixesToDrop.add("shri");
		prefixesToDrop.add("shri.");
		prefixesToDrop.add("pt.");
		prefixesToDrop.add("smt.");
		prefixesToDrop.add("pandit");
		prefixesToDrop.add("hello");
		prefixesToDrop.add("hi");
		charsToDrop.add('{');
		charsToDrop.add('}');
		charsToDrop.add('"');
	}
	
	public static String normalize_name(String n)
	{
		if (n == null)
			return null;
		n = n.toLowerCase();
		
		// strip specials chars at the endes, e.g. Michael D. Ernst} => Michael D. Ernst
		while (n.length() > 0)
		{
			if (charsToDrop.contains(n.charAt(0)))
			{
				n = n.substring(1);
				continue;
			}
			else if ((charsToDrop.contains(n.charAt(n.length()-1))))
			{
				n = n.substring(0, n.length()-1);
				continue;
			}
			else
				break;
		}
		
		for (String prefix: prefixesToDrop)
		{
			if (n.startsWith(prefix))
			{
				n = n.substring(prefix.length(), n.length());
				break;
			}
		}
		return n.trim();
	}
	%>


<%@include file="../getArchive.jspf" %>
<html>
<head>
<Title>Sentiments</Title>
<jsp:include page="../css/css.jsp"/>
<link rel="icon" type="image/png" href="images/muse-favicon.png">
<script type="text/javascript" src="js/protovis.js"></script>
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

	Set<String> words = new LinkedHashSet<String>();
	words.add("i");
	words.add("me");
	words.add("my");
	words.add("mine");
	
	/*
	for (Document d: archive.getAllDocs()) {
		String text = archive.getContents(d, true);
		NER.printSentencesWithWords(text, words);
	}
	NER.printStats();
	*/
	/*
	Map<Integer, Integer>[] map = Sentiments.debugEmotions(driver.indexer, (List) list);

	for (int i = 0; i < Sentiments.emotionsData.length; i++)
	{
		out.println ("<hr/><b>Sentiment: " + Sentiments.emotionsData[i][0] + "</b>\n<p/>");
		List<Pair<Integer, Integer>> pairs = Util.sortMapByValue(map[i]);
		for (Pair<Integer, Integer> p: pairs)
		{
			out.println (p.getSecond() + " <a href=\"browse?noSaveDocsInSession=true&&sentiment=" + Sentiments.emotionsData[i][0] + "&docNum=" + p.getFirst() + "\">" + list.get(p.getFirst()) + "</a><br/>");
		}
	}
	*/
	int count = 0;
	int totalNames = 0;
	int multiWordsNames = 0;
	int contactTokenCounts = 0, emailAccountCounts = 0, inAddressBookCount = 0;
	int multiContactMessages = 0;
	Map<String, Integer> unidMap = new LinkedHashMap<String, Integer>();
	for (edu.stanford.muse.index.Document d: archive.getAllDocs())
	{
		String id = d.getUniqueId();
		String type = request.getParameter("type");
		List<String> names = null;
		if (Util.nullOrEmpty(type))
			names = archive.getNamesForDocId(id, Indexer.QueryType.ORIGINAL);
//		else
//			names = archive.getNamesOfTypeForDoc(type, id, Indexer.QueryType.FULL);
		
		EmailDocument ed = (EmailDocument) d;
		Collection<Contact> contacts = ed.getParticipatingContactsExceptOwn(addressBook);
		multiContactMessages += ((contacts.size() > 1) ? 1: 0);

		if (Util.nullOrEmpty(names))
			continue;
		Set<String> namesSet = new LinkedHashSet<String>(names);
		if (namesSet.size() != names.size())
			out.println ("<p>name duplicated!<p>");
		count++;
		totalNames += names.size();
		Set<String> nameTokens = new LinkedHashSet<String>(), emailAccountTokens = new LinkedHashSet<String>();
		for (Contact c: contacts)
		{
			Set<String> namesForThisContact = c.names;
			if (namesForThisContact != null)				
				for (String n: namesForThisContact)
					nameTokens.addAll(Util.tokenize(n.toLowerCase()));
			
			for (String n: c.emails)
			{
				int endIdxp1 = n.length();
				int x = n.indexOf("@");
				if (x >= 1)
					endIdxp1 = x;
				
				String emailAccount = n.substring(0, endIdxp1);
				emailAccountTokens.add(emailAccount.toLowerCase());
			}
		}

		out.println("<div><a href=\"../browse?docId=" + id + "\">" + id + "</a> (" + names.size() + ") ");
		for (String orig_n: names)
		{
			boolean identified = false;
			String n = normalize_name(orig_n);

			if (nameTokens.contains(n))
			{
				contactTokenCounts++;
				out.println ("<span style=\"color:green\">" +  Util.escapeHTML(orig_n) + "</span>");
				identified = true;
			}
			
			if (!identified && n.length() >= 2)
			{
				for (String emailAccount: emailAccountTokens)
					if (emailAccount.startsWith(n) || emailAccount.endsWith(n))
					{
						out.println ("<span style=\"color:red\">" +  Util.escapeHTML(orig_n) + " (" + emailAccount + ")</span>");
						identified = true;
						emailAccountCounts++;
						break;
					}
			}

			if (!identified && n.contains(" "))
			{
				Contact c = addressBook.lookupByName(n);
				if (c == null)
					c = addressBook.lookupByName(orig_n);
				if (c != null)
				{
					String bestName = c.pickBestName();
					if (bestName != null)
						bestName = bestName.trim();
					String annotation = orig_n.equals(bestName) ? "" : " (" + bestName + ")";
					out.println ("<span style=\"color:royalblue\">" +  Util.escapeHTML(orig_n) + annotation + "</span>");
					identified = true;
					inAddressBookCount++;
					break;
				}
			}
			
			if (!identified)
			{
				out.println (Util.escapeHTML(orig_n));
				Integer I = unidMap.get(n);
				if (I == null)
					unidMap.put(n, 1);
				else
					unidMap.put(n, I+1);
			}
			
			out.println (" | ");
			
			if (Util.tokenize(n).size() > 1)
				multiWordsNames++;
			
		}
		out.println ("</div>");		
	}
	out.println (count + " out of " + archive.getAllDocs().size() + " docs have at least one name<br/>");
	out.println (multiContactMessages + " messages have 2 or more contacts<br/>");
	out.println (totalNames + " names, " + multiWordsNames + " multiWord<br/>");
	out.println (" identified from name tokens in same message:" + contactTokenCounts + " email account:" + emailAccountCounts + " elsewhere in address book: " + inAddressBookCount);
	
	out.println ("<hr/>Unidentified: <p>");
	List<Pair<String, Integer>> pairs = Util.sortMapByValue(unidMap);
	for (Pair<String, Integer> p: pairs)
		out.println (p.getFirst() + ":" + p.getSecond() + "<br/>");

%>
<hr/>
</div> <!-- div_main -->
<jsp:include page="../footer.jsp"/>
</body>
</html>
