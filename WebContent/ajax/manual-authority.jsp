<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@page import="java.util.*"%>
<%@page import="edu.stanford.muse.util.*"%>
<%@page import="edu.stanford.muse.ie.FASTSearcher"%>
<%@page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="edu.stanford.muse.ie.Authority" %>
<%@ page import="edu.stanford.muse.ie.FASTRecord" %>
<!DOCTYPE html>
<!--
	 @author: viharipiratla
	 Manually assign an entity to an archive Id. Archives handled are FAST, Viaf, LCNAF, Loc-Subject, Dbpedia. 
	 Input: dbId and dbType(one of fast,viaf,dbpedia,locName.locSubject) 
	 Output: descriptive html for the input id.-->
<%
String[] idTypes = request.getParameterValues("idTypes[]");
String[] ids = request.getParameterValues("ids[]");
	if (idTypes == null || ids == null)
		JSPHelper.log.info("Received request with one of the parameters null: "+idTypes+", "+ids);
else{
	String fast_id = null;
		for (int i = 0; i < ids.length;i++)
		JSPHelper.log.info("id: "+ids[i]+", idType: "+idTypes[i]);
	
	FASTRecord record = null;
	for(int i=0;i<ids.length;i++)
	{
		String id = ids[i], idType = idTypes[i];
		JSPHelper.log.info("Request for: "+id+", "+idType);
		//the reason for repeating code is to move some computation out of the loop.
		if (idType.equals("fast")) {
			Set<FASTRecord> frs = FASTSearcher.lookupId(id, FASTRecord.FASTDB.ALL);
			Iterator<FASTRecord> it = frs.iterator();
			if (it.hasNext())
				record = it.next();
			else
				JSPHelper.log.info("Cannot find: "+id+" in FAST entities");
		} else {
			Set<FASTRecord> frs = FASTSearcher.getRecordsByDB(id, idType, FASTRecord.FASTDB.ALL);
			Iterator<FASTRecord> it = frs.iterator();
			if (it.hasNext())
			{
				record = it.next();
				if (it.hasNext())
					JSPHelper.log.info("Found more than one record for dbId: " + id + " type:" + idType);
			}
			else
				JSPHelper.log.info("Cannot find any records with links to dbId: " + id + " type:" + idType);
		}

		JSPHelper.log.info("id: "+id+", idType: "+idType+", fast-id: "+fast_id);
	}
	
	if (record == null){
		Authority auth = new Authority("some",ids,idTypes);
		String html = auth.toHTMLString();
		JSPHelper.log.info("Writing: " + html);
		response.getWriter().write(html);	
		JSPHelper.log.info("fast id is null");
	}
	//we give fast ids special handling
	//Note: this may erase other ids assigned with the fast id
	else
	{
		//FASTPerson fp = FASTEntities.fastIDToFASTPerson.get(fast_id);
		Pair<String, String> s = record.getAllSources();
		JSPHelper.log.info("Sources is null for: "+record.getNames());
		String html = "<div class='record'><input data-ids='" + s.second + "' data-dbTypes='"+s.first+"' type='checkbox' checked> " + record.toHTMLString() + " </div>";
		response.getWriter().write(html);
		JSPHelper.log.info("fast id is: "+fast_id);
	}
}
%>