<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.index.Archive"%>
<%@ page import="edu.stanford.muse.util.EmailUtils" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="java.util.Collection"%><%@ page import="java.util.Set"%><%@ page import="java.util.LinkedHashSet"%><%@ page import="edu.stanford.muse.index.EmailDocument"%><%@ page import="java.util.Arrays"%><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%><%@ page import="edu.stanford.muse.ResultCacheManager.ResultCache"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="java.io.File"%><%@ page import="edu.stanford.muse.index.Document"%><%@ page import="edu.stanford.muse.datacache.Blob"%>
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
    String result = "",error="";
    int thumbgenerated=0;//to track number of successfully generated thumbnails.

    String sofficepath = request.getParameter("sofficepath");
    String convertpath = request.getParameter("convertpath");
    //Check if we have executables on these paths. If not then return immediately with error message.
    if(!new File(sofficepath).exists()){
        error="Path to 'soffice' is not valid. Please check again!";
    }
    if(!new File(convertpath).exists()){
        error="Path to 'convert' is not valid. Please check again!";
    }
    if(Util.nullOrEmpty(error)){
        //Iterate over all documents in archive and for each document iterate over all attachments and call the method to generate thumnbnail of that attachment.
        //Also keep track of number of created thumbnails.
        archive.getBlobStore().setExecutablePath("convert",convertpath);
        archive.getBlobStore().setExecutablePath("soffice",sofficepath);
        for(Blob b: archive.getBlobStore().uniqueBlobs){
            if(archive.getBlobStore().generate_thumbnail(b))
                thumbgenerated++;
        }
        obj.put("status", 0);
        obj.put("result", "Generated thumbnails for "+thumbgenerated+" attachments successfully!");
        out.println (obj);
        //JSPHelper.log.info(obj);
    }else{
        //means we had error. Build the response object accordingly.
        obj.put("status", 1);
        obj.put("error", error);
        out.println (obj);
       // JSPHelper.log.info(obj);
    }

%>