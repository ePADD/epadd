<%@page contentType="text/html; charset=UTF-8"%>
<!--
	jsp to fetch pages from startPage to endPage (inclusive) for the given dataset
	is used in an ajax call inside jog.js, never rendered directly
 -->

<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page language="java" import="edu.stanford.muse.util.Pair"%>
<%@page contentType="text/html; charset=UTF-8"%>

<%
	String datasetId = request.getParameter("datasetId");
	DataSet dataset = (DataSet) JSPHelper.getSessionAttribute(session, datasetId);
	boolean error = (dataset == null);

	String archiveID = request.getParameter("archiveID");

	int startPage = HTMLUtils.getIntParam(request, "startPage", -1);
	int endPage = HTMLUtils.getIntParam(request, "endPage", -1);
	boolean inFull = HTMLUtils.getIntParam(request, "inFull", 0) != 0;
	boolean debug = HTMLUtils.getIntParam(request,"debug", 0) != 0;
	boolean isPreserve = (request.getParameter("preserveMode") != null) && (request.getParameter("preserveMode").equals("1")); // in preservation mode

	Pair <String, Boolean> result = null;

	JSPHelper.doLogging("jogpage: " + debug + ", " + request.getParameter("debug"));

	JSPHelper.doLogging("Fetching pages [" + startPage + ".." + endPage + "] for dataset " + datasetId + " in preserve mode: "+ isPreserve);

// output format needed: an external div, which contains individual page divs
	out.println ("<div>");

	boolean IA_links = "true".equals(JSPHelper.getSessionAttribute(session, "IA_links"));
	for (int i = startPage; i <= endPage; i++) // note: start and end page inclusive
	{
		out.println ("<div class=\"page\" pageId=\"" + i + "\" display=\"none\">");
		if (error)
			out.println ("Sorry, error reading document " + i + ". Session may have timed out. Please log out and try again. (Doc set:" + datasetId + ")");
		else
		{
			try {
				out.println ("<script>$('.qtip').remove()</script>");
				result = dataset.getPageForMessages(i, IA_links, inFull, debug,archiveID, isPreserve);
				out.println (result.first);
				String redacted = result.second.booleanValue()== true? "true" : "false" ;
				out.println ("\n");
                out.println ("<script src=\"js/epadd.js\"></script>");
				out.println ("<script>initialiseqtip(); toggleEditImage(" + redacted + "," + isPreserve + ")</script>");
			} catch (Exception e) {
				out.println ("Sorry... exception reading page content: " + e);
			}
		}
		out.println ("</div>");
	}
	out.println ("</div>");
%>
