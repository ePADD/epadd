<%@ page language="java" contentType="application/json;charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.ie.AuthorisedAuthorities"%>
<%@page language="java" import="edu.stanford.muse.ie.EntityFeature"%>
<%@page language="java" import="edu.stanford.muse.ie.InternalAuthorityAssigner"%>
<%@page language="java" %>

<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="edu.stanford.muse.index.IndexUtils"%>
<%@ page import="edu.stanford.muse.index.Indexer"%>
<%@ page import="edu.stanford.muse.util.EmailUtils"%>
<%@ page import="edu.stanford.muse.util.Pair"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.HashSet"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Set"%>
<%@ page import="java.util.regex.Pattern"%>
<%@ page import="edu.stanford.muse.ner.NER"%><%@ page import="edu.stanford.muse.util.Span"%><%@ page import="java.util.ArrayList"%><%@ page import="edu.stanford.muse.ner.featuregen.FeatureDictionary"%>
<%/*Given a name; resolves it to multiple word name if the name is single word and also anotates the multiple word name with external(if there is an authorised database id) or internal(If there is a contact in addressbook with that name.)
	Input: name that is to be expanded/annotated; docId of the document in which name is to be expanded/annotated. 
	Output: If expanded possible multiple word names with authority type annotations in decreasing order of confidence(Max: 5)
			If input is multiple word name then will be just annotated with the authority type.
*/
%>
<%
	response.setContentType("application/json; charset=utf-8");
	InternalAuthorityAssigner authorities = (InternalAuthorityAssigner)request.getSession().getAttribute("authorities");
	Archive archive = (Archive)request.getSession().getAttribute("archive");
	JSONObject result = new JSONObject();
	if (archive == null){
		result.put("result", "Session expired? Please reload");
		response.getWriter().write(result.toString(4));
		return;
	}
	Set<String> ownNames = archive.addressBook.getOwnNamesSet();
	Set<String> ownAddr = archive.getAddressBook().getOwnAddrs();

	String internalRecordHtml = "<i title=\"In address book\" class=\"fa fa-envelope\"></i>";
	String externalRecordHtml = "<img title='External Authority' src='images/appbar.globe.wire.png' width=20px;/>";

	if(authorities!=null && archive != null){
		Indexer indexer = archive.indexer;
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
            Span[] spans = archive.getEntitiesInDoc(ed, true);
            Set<String> persons = new HashSet<>(), places = new HashSet<>(), orgs = new HashSet<>();
			for(Span sp: spans){
			    if(sp == null) continue;
			    short ct = FeatureDictionary.getCoarseType(sp.type);
			    if(ct == FeatureDictionary.PERSON)
			        persons.add(sp.getText());
			    else if(ct == FeatureDictionary.PLACE)
			        places.add(sp.getText());
			    else
			        orgs.add(sp.getText());
			}
			Pattern pat = Pattern.compile("[A-Z]+");
			
			short et = -1;
			if(persons.contains(name)) et = EntityFeature.PERSON;
			else if(pat.matcher(name).matches()) et = EntityFeature.ACRONYM;
			else if(orgs.contains(name)) et = EntityFeature.ORG;
			else if(places.contains(name)) et = EntityFeature.PLACE;

			JSPHelper.log.info("Type of name: "+name+" -> " + et);
			//we only try to expand these types
			if(et == EntityFeature.PERSON || et==EntityFeature.ACRONYM ||
			    et == EntityFeature.ORG || et == EntityFeature.PLACE) {
				if(!name.contains(" ")) {
					Set<String> entities = persons;
					EntityFeature ef = new EntityFeature(name, et);
					List<String> names = ed.getAllNames();
					List<String> addresses = ed.getAllAddrs();
		
					Set<String> goodNames = new HashSet<String>();
		
					if (entities != null) {
						for (String entity : entities) {
							if (entity.contains(" "))
								goodNames.add(IndexUtils.canonicalizeEntity(entity));
						}
					}
					if (names != null)
						for (String n : names)
							if (n != null)
								goodNames.add(IndexUtils.canonicalizeEntity(n));
		
					ef.addAllCE(goodNames);
					if (addresses != null)
						ef.addAllEA(addresses);
					JSPHelper.log.info("Size of EA's: " + ef.emailAddresses.size() + " CE's: " + ef.cooccuringEntities.size());

					List<Pair<EntityFeature, Double>> scores = ef.getClosestNEntities(archive, 5);
		            if(scores == null){
		                result.put("result", "Never seen this name before");
                        response.getWriter().write(result.toString(4));
		            }

					String html = "<body><ol style='margin:0 0 0px'>";
					int num = 0;
					double maxScore = -1;
					if(scores.size()>0)
						maxScore = scores.get(0).second;
					for (Pair<EntityFeature, Double> score : scores) {
						if(true){
							//de-normalise entity name.
							Set<String> possiblenames = new HashSet<String>();
							possiblenames.add(EmailUtils.uncanonicaliseName(score.first.name));
							//choose the longest to display
							String d = "";
							for (int j = 0; j < 1000; j++)
								d += "1";
							if (possiblenames.size()>0) {
								for (String str : possiblenames)
									if (str != null && str.length() < d.length())
										d = str;
							} else
								JSPHelper.log.warn("No original string found for normalised name: " + score.first.name);
		
							double p = score.second / maxScore;
							String color = "#0175BC";//"rgb(" + new Double((1 - p) * 255).intValue() + "," + new Double(p * 255).intValue() + "," + "20)";
							String width = new Double(50 * p).intValue() + "px";
							String href = "browse?term=\"" + edu.stanford.muse.util.Util.escapeHTML(d) + "\"";
							String recordType = "";
							if (archive.addressBook.lookupByName(d) != null)
								recordType += internalRecordHtml;
		
							if (AuthorisedAuthorities.cnameToDefiniteID != null && AuthorisedAuthorities.cnameToDefiniteID.get(IndexUtils.canonicalizeEntity(d)) != null)
								recordType += externalRecordHtml;
		
							html += "<li><a href='" + href + "' style='color:black'>" + d + "</a>&nbsp"
									+ recordType
									+ "<div style='background-color:" + color + ";width: " + width + ";height:5px;'></div>" + "</li>";//new DecimalFormat("#.####").format(score.second)
							num++;
						}
					}
					html += "</ol></body>";
					if (num == 0)
						html = "No confident matches.";
					if(num == 1)
						html = "";
                    if(JSPHelper.log.isDebugEnabled())
					    JSPHelper.log.debug(html);
					result.put("result", html);
				}
				else{
					String html = name;
					String d = IndexUtils.canonicalizeEntity(name);
					if (archive.addressBook.lookupByName(d) != null)
						html += "&nbsp"+internalRecordHtml;
					if (AuthorisedAuthorities.cnameToDefiniteID != null && AuthorisedAuthorities.cnameToDefiniteID.get(IndexUtils.canonicalizeEntity(d)) != null)
						html += "&nbsp"+externalRecordHtml;

					result.put("result","");
				}
			}
		}
	} else {
		JSPHelper.log.warn("No authorities file!");
		result.put("result", "No authorities file loaded.");
	}

	JSPHelper.log.info("Expand name: Response for: "+request.getParameter("name")+" is: "+result.toString(4));
	response.getWriter().write(result.toString(4));
%>