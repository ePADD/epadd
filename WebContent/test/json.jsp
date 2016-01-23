<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="java.util.*" %>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.ner.NER" %>
<%@ page import="javax.mail.Address" %>
<%@ page import="edu.stanford.muse.email.Contact" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="edu.stanford.muse.ner.model.SequenceModel" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="javax.mail.internet.InternetAddress" %>
<%@ page import="edu.stanford.muse.ner.featuregen.FeatureDictionary" %>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="static edu.stanford.muse.webapp.SimpleSessions.*" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>

<% Archive archive = prepareAndLoadDefaultArchive(request); // if we don't have an archive and are running in desktop mode, try to load archive from given cachedir or from the default dir %>
<%!

    // this JSP dumps email headers as JSON. meant to be used for viz tools etc.

    private static String displayForEmail(Archive archive, String email) {
        if (email == null)
            return "";

        Contact c = archive.addressBook.lookupByEmail(email);
        if (c == archive.addressBook.getContactForSelf())
            return  archive.addressBook.getBestNameForSelf(); // this might not be the same as the best name picked for this contact, because it could be a longer name

        if (c != null) {
            return c.pickBestName();
        }
        return "";
    }

    private static JSONArray jsonForCollection(Archive archive, Address[] as) {
        JSONArray result = new JSONArray();
        if (as != null) {
            for (Address a : as) {
                InternetAddress ia = (InternetAddress) a;
                result.put(displayForEmail(archive, ia.getAddress()));
            }
        }
        return result;
    }

    private static String[] getEntities(Archive archive, EmailDocument ed, String type) {
        List<String> entities = new ArrayList<>();
        double cutoff = 0.01;

        Short ct = FeatureDictionary.PERSON;
        if(edu.stanford.muse.ner.NER.EPER.equals(type)||"person".equals(type)) {
            ct = FeatureDictionary.PERSON;
        }
        else if(edu.stanford.muse.ner.NER.ELOC.equals(type)||"place".equals(type)) {
            ct = FeatureDictionary.PLACE;
        }
        else if(edu.stanford.muse.ner.NER.EORG.equals(type)||"organisation".equals(type)) {
            ct = FeatureDictionary.ORGANISATION;
        }

        try {
            Map<Short, Map<String, Double>> es = NER.getEntities(archive.getDoc(ed), true);
            List<Short> mtypes = Arrays.asList(SequenceModel.mappings.get(ct));
            for (Short mt : mtypes) {
                for (String e : es.get(mt).keySet()) {
                    double s = es.get(mt).get(e);
                    if (s < cutoff)
                        continue;
                    entities.add(e);
                }
            }
        } catch (Exception e) { Util.print_exception(e, JSPHelper.log);}
        entities = edu.stanford.muse.ie.Util.filterEntities(entities, type);
        return entities.toArray(new String[entities.size()]);
    }
%>

<%
    JSONArray messages = new JSONArray();
    List<Document> docs = archive.getAllDocs();

    // sort by date
    Collections.sort(docs);

    for (Document doc : docs) {
        EmailDocument ed = (EmailDocument) doc;
        JSONObject j = new JSONObject();

        j.put("from", jsonForCollection(archive, ed.from));
        j.put("to", jsonForCollection(archive, ed.to));
        j.put("cc", jsonForCollection(archive, ed.cc));
        j.put("bcc", jsonForCollection(archive, ed.bcc));
        j.put("date", ed.date.getTime());
        j.put("persons", getEntities(archive, ed, "person"));
        j.put("locations", getEntities(archive, ed, "place"));
        j.put("orgs", getEntities(archive, ed, "organisation"));

        messages.put(j);
    }

    out.println (messages.toString(4)); // .replaceAll("\n", "<br/>"));
%>
