<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page language="java" import="org.jsoup.Jsoup"%>
<%@page language="java" import="org.jsoup.nodes.Document"%>
<%@page language="java" import="org.jsoup.select.Elements"%>
<%@page language="java" import="org.jsoup.nodes.Element"%>
<%@page language="java" import="java.util.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@ page import="edu.stanford.muse.ie.*" %>
<%@ page import="edu.stanford.muse.ie.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.index.Archive" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!-- Re-sorts various dbpedia resolutions of a name based on context. 
	 context is a comma separated string of various entities that co-occurred with the entity being resolved.
	 input: various sources that need to be re-sorted in html format like this: (<div class='record'></div>) and context as described above.
	 output: a re-sorted html, sorted based in decreasing order of confidence. 
	 TODO: Its not clean to work on html content, change the i/o accordingly.	
-->

<%
    response.setContentType("application/x-javascript; charset=utf-8");
    String name = request.getParameter("entity");
    String type = request.getParameter("type");
    String rownum = request.getParameter("rownum");
    JSPHelper.log.info("Received request for: " + name + " and type: " + type);
    Archive archive = JSPHelper.getArchive(session);
    String ckbox = "<input type='checkbox'>";
    List<Pair<String, Entities.Score>> scores = new ArrayList<Pair<String, Entities.Score>>();
    if("person".equals(type)) {
        List<Pair<FASTRecord, Entities.Score>> scoreRecords = Entities.getFASTRecordsFor(name, FASTRecord.FASTDB.PERSON, true, archive);
        for (Pair<FASTRecord, Entities.Score> sr : scoreRecords) {
            Pair<String, String> links = sr.getFirst().getAllSources();
            String html = "<div class='record'><input type='checkbox' data-ids='" + links.second + "' data-dbTypes='" + links.first + "'> " + sr.getFirst().toHTMLString();
            scores.add(new Pair<String, Entities.Score>(html, sr.getSecond()));
        }
    }
    if("places".equals(type)) {
        List<Pair<FreebaseType, Entities.Score>> scoreRecords = Entities.getFreebaseRecordsFor(name, FreebaseType.Type.Location, true, archive);
        for(Pair<FreebaseType, Entities.Score> sr: scoreRecords) {
            Pair<String, String> links = sr.getFirst().getAllSources();
            String html = "<div class='record'><input type='checkbox' data-ids='" + links.second + "' data-dbTypes='" + links.first + "'> " + sr.getFirst().toHtmlString();
            scores.add(new Pair<String, Entities.Score>(html, sr.getSecond()));
        }
    }
    if("org".equals(type)) {
        List<Pair<FreebaseType, Entities.Score>> scoreRecords = Entities.getFreebaseRecordsFor(name, FreebaseType.Type.Organization, true, archive);
        for(Pair<FreebaseType, Entities.Score> sr: scoreRecords) {
            Pair<String, String> links = sr.getFirst().getAllSources();
            String html = "<div class='record'><input type='checkbox' data-ids='" + links.second + "' data-dbTypes='" + links.first + "'> " + sr.getFirst().toHtmlString();
            scores.add(new Pair<String, Entities.Score>(html, sr.getSecond()));
        }
    }

    //some stats to serve as titles
    String finalhtml = "";
    if (scores != null) {
        double maxScore = scores.get(0).getSecond().score;
        for (int j = 0; j < scores.size(); j++) {
            Pair<String, Entities.Score> elt = scores.get(j);
            finalhtml += elt.first;
            finalhtml += Util.getConfidenceBar(elt.getSecond().score/maxScore, elt.getSecond().scoredOn) + "<br><br></div>";
        }
    }
    finalhtml += "<span class='manual' id='manual_" + rownum + "'>";
    finalhtml += "<a class='popupManual' id='inline_" + rownum + "' title='' href='#manualassign' onclick='localStorage.setItem(\"entitynum\",\"" + rownum + "\");'>";
    finalhtml += "<i class=\"manual-authority fa fa-plus\" id='manual_" + rownum + "'/></i></a></span>\n";

    //TODO: There should be a better way to do this, improve this!
//    Elements manualelts = doc.select(".manual");
//    if (manualelts.size() > 0) {
//        Element elt = manualelts.get(0);
//        finalhtml += elt.outerHtml();
//    }

    response.getWriter().write(finalhtml + "<script>qtipreinitialise();</script>");
%>
