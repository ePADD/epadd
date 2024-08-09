<%@page contentType="text/html; charset=UTF-8"%>
<!--
	jsp to fetch pages from startPage to endPage (inclusive) for the given dataset
	is used in an ajax call inside jog.js, never rendered directly
 -->

<%@page import="edu.stanford.muse.webapp.*"%>
<%@page import="edu.stanford.muse.index.*"%>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="java.util.*" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="edu.stanford.muse.email.EmailFetcherThread" %>
<%@page contentType="text/html; charset=UTF-8"%>

<%
	String datasetId = request.getParameter("datasetId");
	DataSet dataset = (DataSet) JSPHelper.getSessionAttribute(session, datasetId);
	//get query params which were stored in dataset object
	Multimap<String,String> queryparams = dataset.getQueryParams();
	boolean error = (dataset == null);

	String archiveID = request.getParameter("archiveID");

	int startPage = HTMLUtils.getIntParam(request, "startPage", -1);
	int endPage = HTMLUtils.getIntParam(request, "endPage", -1);
	boolean debug = HTMLUtils.getIntParam(request,"debug", 0) != 0;
	JSPHelper.doLogging("jogpage: " + debug + ", " + request.getParameter("debug"));

	JSPHelper.doLogging("Fetching attachment pages (years) [" + startPage + ".." + endPage + "] for dataset " + datasetId);

	// output format needed: an external div, which contains individual page divs
	out.println ("<div>");

	//here each page corresponds to a year. Let us say that the range of years for dataset is from 1954 to 1978 then on first page the attachments for
	//year 1954 will be displayed. On the second page the attachments for 1955 will be displayed.
    Pair<Boolean,List<Pair<Integer,Integer>>> yearwiseAttachments = EmailUtils.getStartEndYearOfAttachments(queryparams, dataset.getDocs());
    //extract the variable that denotes if this list of years and attachments has a year corresponding to hacky date (1January 1960)
    boolean isHacky=yearwiseAttachments.first;

	for (int i = startPage; i <= endPage; i++ ) // note: start and end page inclusive
	{
	    //TO get the year corresponding to ith page.. look for i th key in yearwiseAttachments map. This will cause jog plugin to skip over all those years which had
		//non zero attachments.
		int year = yearwiseAttachments.second.get(i).first;
		out.println ("<div class=\"page\" pageId=\"" + year + "\" display=\"none\">");
		if (error)
			out.println ("Sorry, error reading document " + year + ". Session may have timed out. Please log out and try again. (Doc set:" + datasetId + ")");
		else
		{
			try {
				out.println ("<script>$('.qtip').remove()</script>");
				out.println (dataset.getPageForAttachments(year, isHacky, archiveID,queryparams));
				out.println ("\n");
                out.println ("<script src=\"js/epadd.js?v=1.1\"></script>"); // @chinmay, do we need this? epadd.js should already be included
				out.println ("<script>initialiseqtip()</script>");
			} catch (Exception e) {
				out.println ("Sorry... exception reading page content for attachments: " + e);
			}
		}
		out.println ("</div>");

	}
	out.println ("</div>");
%>
