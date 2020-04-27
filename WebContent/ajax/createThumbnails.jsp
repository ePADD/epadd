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
    //read outgoing offset count.
    String outthreshold = request.getParameter("outoffset");
    int outthresholdcount = Integer.parseInt(outthreshold);
    String result = "";
    if(outthresholdcount==-1){
        outthresholdcount=archive.getAllDocsAsSet().size()+1;//to denote 'infinity'
        result = trustedaddrstring;//no new trusted address gets added because the outcount is set as infinity
    }else{
    //inovke recomputeaddressbook method of class EmailDocument.java
    Set<String> trustedAddresses = EmailDocument.computeMoreTrustedAddresses(archive,trustedaddrset,outthresholdcount);
    if(trustedAddresses==null)
        result="";
    else
        result = String.join(";",trustedAddresses);
    }
    if(Util.nullOrEmpty(result) ){
        obj.put("status", 1);
        obj.put("error", "Unable to recompute the addressbook!");
        out.println (obj);
        JSPHelper.log.info(obj);
    }else{
//        archive.setAddressBook(newaddressbook);
  //      archive.recreateCorrespondentAuthorityMapper(); // we have to recreate auth mappings since they may have changed
    //    ArchiveReaderWriter.saveAddressBook(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
      //  ArchiveReaderWriter.saveCorrespondentAuthorityMapper(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);

        //Archive.cacheManager.cacheCorrespondentListing(ArchiveReaderWriter.getArchiveIDForArchive(archive));
        obj.put("status", 0);
        obj.put("result",result);
        obj.put("message","Trusted addresses computed successfully!");
        out.println (obj);
        JSPHelper.log.info(obj);

    }


%>