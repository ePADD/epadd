<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page language="java" %>
<%@page language="java" %>
<%@page language="java" %>
<%@page language="java" %>

<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="edu.stanford.muse.index.IndexUtils"%>
<%@ page import="edu.stanford.muse.util.EmailUtils"%>
<%@ page import="edu.stanford.muse.util.Pair"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.HashSet"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Set"%>
<%@ page import="edu.stanford.muse.util.Span"%>
<%@ page import="edu.stanford.muse.ie.*"%>
<%/*Given a name; resolves it to multiple word name if the name is single word and also annotates the multiple word name with external(if there is an authorised database id) or internal(If there is a contact in addressbook with that name.)
	Input: name that is to be expanded/annotated; docId of the document in which name is to be expanded/annotated.
	Output: If expanded possible multiple word names with authority type annotations in decreasing order of confidence(Max: 5)
			If input is multiple word name then will be just annotated with the authority type.
*/
%>
<%
	response.setContentType("application/json; charset=utf-8");
//	InternalAuthorityAssigner authorities = (InternalAuthorityAssigner)request.getSession().getAttribute("authorities");
	JSONObject result = new JSONObject();
    Archive archive = JSPHelper.getArchive(request);
    if (archive == null) {
        JSONObject obj = new JSONObject();
        obj.put("status", 1);
        obj.put("error", "No archive in session");
        out.println (obj);
        JSPHelper.log.info(obj);
        return;
    }
	Set<String> ownNames = archive.addressBook.getOwnNamesSet();
	Set<String> ownAddr = archive.getAddressBook().getOwnAddrs();

	String internalRecordHtml = "<i title=\"In address book\" class=\"fa fa-envelope\"></i>";
	String externalRecordHtml = "<img title='External Authority' src='images/appbar.globe.wire.png' width=20px;/>";

	String name = request.getParameter("name");
	String docId = request.getParameter("docId");
	JSPHelper.log.info("DocId: " + docId + " ," + archive.getAllDocs().size());
	EmailDocument ed = archive.docForId(docId);
	if (ed == null) {
    	result.put("result", "Wrong docId!");
	   	response.getWriter().write(result.toString(4));
		JSPHelper.log.info("Wrong docId!");
		return;
	} else {
		String html = "<ol style='margin:0 0 0px'>";
		int num = 0;

		Span span = new Span(name, -1, -1);
        List<Pair<ProperNounLinker.EmailMention, Integer>> scores = ProperNounLinker.getNearestMatches(new ProperNounLinker.EmailMention(span,ed,new EmailHierarchy()), 4, archive);
		for (Pair<ProperNounLinker.EmailMention, Integer> score : scores) {
			String d = EmailUtils.uncanonicaliseName(score.first.entity.text);
		    double p = 1.0f/(1+score.second);
			String color = "#0175BC";//"rgb(" + new Double((1 - p) * 255).intValue() + "," + new Double(p * 255).intValue() + "," + "20)";
			String width = new Double(50 * p).intValue() + "px";
			String href = "browse?term=\"" + edu.stanford.muse.util.Util.escapeHTML(d) + "\"";
			String recordType = "";
			if (archive.addressBook.lookupByName(d) != null)
			    recordType += internalRecordHtml;


		//	if (AuthorisedAuthorities.cnameToDefiniteID != null && AuthorisedAuthorities.cnameToDefiniteID.get(IndexUtils.canonicalizeEntity(d)) != null)
		//	    recordType += externalRecordHtml;
			html += "<li><a href='" + href + "' style='color:black'>" + d + "</a>&nbsp"
			    + recordType
				+ "<div style='background-color:" + color + ";width: " + width + ";height:5px;'></div>" + "</li>";//new DecimalFormat("#.####").format(score.second)
			num++;
		}
		html += "</ol>";
		if (num == 0)
		    html = "No confident matches.";
		if(JSPHelper.log.isDebugEnabled())
		    JSPHelper.log.debug(html);
		result.put("result", html);
	}

	JSPHelper.log.info("Expand name: Response for: "+request.getParameter("name")+" is: "+result.toString(4));
	response.getWriter().write(result.toString(4));
%>