<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Collection"%><%@ page import="java.util.Set"%><%@ page import="java.util.LinkedHashSet"%><%@ page import="edu.stanford.muse.index.EmailDocument"%><%@ page import="java.util.Arrays"%><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%><%@ page import="edu.stanford.muse.ResultCacheManager.ResultCache"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%>
<%
    Archive archive = JSPHelper.getArchive(request);
    	JSONObject obj = new JSONObject();

    if (archive == null) {
        obj.put("status", 1);
        obj.put("error", "No archive in session");
        out.println (obj);
        JSPHelper.log.info(obj);
        return;
    }
    String trustedaddrstring = request.getParameter("trustedaddrs");
    //split trustedaddrs on semicolon
    String[] trustedaddrs = trustedaddrstring.split(";");
    Set<String> trustedaddrset=new LinkedHashSet<>();
    Arrays.stream(trustedaddrs).forEach(s->trustedaddrset.add(s));

    //inovke recomputeaddressbook method of class EmailDocument.java
    AddressBook newaddressbook = EmailDocument.recomputeAddressBook(archive,trustedaddrset);
    if(newaddressbook==null){
        obj.put("status", 1);
        obj.put("error", "Unable to recompute the addressbook!");
        out.println (obj);
        JSPHelper.log.info(obj);
    }else{
        //set this as current addressbook., also invalidate and recompute the cache.
        archive.setAddressBook(newaddressbook);
        Archive.cacheManager.cacheCorrespondentListing(ArchiveReaderWriter.getArchiveIDForArchive(archive));
        obj.put("status", 0);
        obj.put("message","Addressbook reconstructed successfully!");
        out.println (obj);
        JSPHelper.log.info(obj);

    }


%>