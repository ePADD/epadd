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
<%@ page import="java.util.List"%>
<%@ page import="edu.stanford.muse.util.Span"%>
<%@ page import="edu.stanford.muse.ie.*"%>
<%@ page import="org.json.JSONArray"%>
<%/*Given a name; resolves it to multiple word name if the name is single word and also annotates the multiple word name with external(if there is an authorised database id) or internal(If there is a contact in addressbook with that name.)
	Input: name that is to be expanded/annotated; docId of the document in which name is to be expanded/annotated.
	Output: If expanded possible multiple word names with authority type annotations in decreasing order of confidence(Max: 5)
			If input is multiple word name then will be just annotated with the authority type.
*/
	response.setContentType("application/json; charset=utf-8");
	InternalAuthorityAssigner authorities = (InternalAuthorityAssigner)request.getSession().getAttribute("authorities");
	Archive archive = (Archive)request.getSession().getAttribute("archive");
	JSONObject result = new JSONObject();

	if (archive == null){
		result.put("result", "Session expired? Please reload");
		response.getWriter().write(result.toString(4));
		return;
	}

	String name = request.getParameter("name");
	String docId = request.getParameter("docId");
	EmailDocument ed = archive.docForId(docId);
	if (ed == null) {
    	result.put("result", "Invalid docId!");
	   	response.getWriter().write(result.toString(4));
		return;
	} else {
        JSONArray arr = new JSONArray();
		Span span = new Span(name, -1, -1);
        List<Pair<ProperNounLinker.EmailMention, Integer>> namesAndScores = ProperNounLinker.getNearestMatches(new ProperNounLinker.EmailMention(span,ed,new EmailHierarchy()), 4, archive);
        int max = 5, count = 0;

		for (Pair<ProperNounLinker.EmailMention, Integer> nameAndScore : namesAndScores) {

        	JSONObject o = new JSONObject();

			String d = EmailUtils.uncanonicaliseName(nameAndScore.first.entity.text);
            o.put ("candidate", d);
            o.put ("score", nameAndScore.second);
			String recordType = "";

			o.put ("inAddressBook", archive.addressBook.lookupByName(d) != null);

			o.put ("confirmedAuthority", AuthorisedAuthorities.cnameToDefiniteID != null && AuthorisedAuthorities.cnameToDefiniteID.get(IndexUtils.canonicalizeEntity(d)) != null);
			arr.put (count, o);
            if (++count > max)
                break;
		}
		result.put("candidates", arr);
	}

	JSPHelper.log.info("Expand name: Response for: "+request.getParameter("name")+" is: "+result.toString(4));
	response.getWriter().write(result.toString(4));
%>