<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page import="edu.stanford.muse.AddressBookManager.Contact"%>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.webapp.HTMLUtils" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="org.json.JSONArray" %><%@ page import="org.json.JSONObject"%><%@ page import="java.io.File"%><%@ page import="edu.stanford.muse.index.Lexicon"%><%@ page import="edu.stanford.muse.index.ArchiveReaderWriter"%><%@ page import="java.io.PrintWriter"%><%@ page import="org.json.CDL"%><%@ page import="org.apache.commons.io.FileUtils"%><%@ page import="au.com.bytecode.opencsv.CSVWriter"%><%@ page import="java.util.*"%><%@ page import="java.io.IOException"%><%@ page import="java.io.FileWriter"%><%@ page import="edu.stanford.muse.ner.model.NEType"%>
<% 
	String query = request.getParameter("data");
	JSONObject result= new JSONObject();
	Archive archive = JSPHelper.getArchive(request);
	 if (archive == null) {
        result.put("error", "No archive in session");
        result.put("status", 1);
        out.println (result);
        JSPHelper.log.info(result);
        return;
    }

	query = query.toLowerCase();
	 String error="";
	 String downloadURL="";
     String appURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();

//////////////////////////////////////////////Download Lexicon///////////////////////////////////////////////////////////////////////////////
	 if(query.equals("lexicon")){
	    String lexiconname=request.getParameter("lexicon").toLowerCase();
	    String lexiconpath = archive.baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.LEXICONS_SUBDIR;
	    File files[] = new File(lexiconpath).listFiles(new Util.MyFilenameFilter(lexiconname, Lexicon.LEXICON_SUFFIX));
		File fname=null;
	    if (files != null)
		{
			for (File f : files)
			{
				fname = f;
				//Ideally one lexicon can match to more than one file (with different languages) but right now we are only supporting one
				//language here. @TODO
				break;
			}
		    //copy lexicon to temp and return a link to serveTemp with that file
	    	String destdir = Archive.TEMP_SUBDIR + File.separator;
			Util.copy_file(fname.getAbsolutePath(),destdir+File.separator+fname.getName());
			//prepare a URL with this file
    	    String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file=" + fname.getName() ;
            downloadURL = appURL + "/" +  contentURL;
		}else{
		    error="Lexicon file not found";
		}

//////////////////////////////////////Download addressbook/////////////////////////////////////////////////////////////////////////////////
    }else if(query.equals("addressbook")){
	    //copy addressbook to temp and return a link to serveTemp with that file
	    String addressbookpath = archive.baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.SESSIONS_SUBDIR + File.separator + Archive.ADDRESSBOOK_SUFFIX;
	    if(new File(addressbookpath).exists()){
	        String destdir = Archive.TEMP_SUBDIR + File.separator;

	        Util.copy_file(addressbookpath,destdir+File.separator+Archive.ADDRESSBOOK_SUFFIX);
			//prepare a URL with this file
    	    String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file=" + Archive.ADDRESSBOOK_SUFFIX ;
            downloadURL = appURL + "/" +  contentURL;
	    }else{
	        error="Addressbook not found";
	    }
//////////////////////////////////////Download label-info.json file//////////////////////////////////////////////////////////////////////
	}else if(query.equals("labels")){
	    //copy label-info.json to temp and return a link to serveTemp with that file
	  String labeinfopath= archive.baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.SESSIONS_SUBDIR + File.separator
	  + Archive.LABELMAPDIR + File.separator + "label-info.json";

	  if(new File(labeinfopath).exists()){
	        String destdir = Archive.TEMP_SUBDIR + File.separator;

	        Util.copy_file(labeinfopath,destdir+File.separator+"label-info.json");
			//prepare a URL with this file
    	    String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file=label-info.json" ;
            downloadURL = appURL + "/" +  contentURL;
	    }else{
	        error="Label description file not found";
	    }
    }//////////////////////////////////////Download confirmed correspondent file//////////////////////////////////////////////////////////////////////
	else if(query.equals("confirmedcorrespondents")){

    String pathToFile = Archive.TEMP_SUBDIR + File.separator + "epadd-authorities.csv";
    PrintWriter pw = null;
        try {
            pw = new PrintWriter(pathToFile, "UTF-8");
            String csv = archive.getCorrespondentAuthorityMapper().getAuthoritiesAsCSV();
            pw.println(csv);
            pw.close();
            String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file=epadd-authorities.csv" ;
            downloadURL = appURL + "/" +  contentURL;
        } catch(Exception e){
            //Util.print_exception ("Error exporting authorities", e, JSPHelper.log);
            e.printStackTrace();
           error="Error exporting confirmed correspondents";///
        }

    }//////////////////////////////////////Download unconfirmed correspondent file//////////////////////////////////////////////////////////////////////
    else if(query.equals("unconfirmedcorrespondents")){
	    JSONArray correspondents = archive.getAddressBook().getCountsAsJson((Collection)archive.getAllDocs(),true,ArchiveReaderWriter.getArchiveIDForArchive(archive));
	    //put all these data in a csv, copy to temp directory and return the url to download.
         String destfile = Archive.TEMP_SUBDIR + File.separator + "unconfirmedCorrespondents.csv";
         FileWriter fw = new FileWriter(destfile);
         List<String> line = new ArrayList<>();
         line.add ("Correspondent");
         line.add ("Number_of_messages_sent");
         line.add ("Number_of_messages_received");
         line.add ("Number_of_messages_mentioned");
         line.add ("Start_Date");
         line.add ("End_Date");

         CSVWriter csvwriter = new CSVWriter(fw, ',', '"',' ',"\n");

         csvwriter.writeNext(line.toArray(new String[line.size()]));

         for(int i=0;i<correspondents.length();i++){
             JSONArray l = correspondents.getJSONArray(i);
             // write the records
             line = new ArrayList<>();
             //Here indices 0 to 7 denote
             //displayname, #sent message,#received messages,#mentions, url,allnames and emails, #start datarange, #end date range
             //only choose 0,1,2,3,6,7
             int[] options = new int[]{0, 1, 2, 3, 6, 7};
             for(int j: options){
                 if(!l.isNull(j))
                    line.add(l.get(j).toString());
                 else
                     line.add("unknown");
                }
             JSPHelper.log.info(i);
                 csvwriter.writeNext(line.toArray(new String[line.size()]));
             }

         csvwriter.close();
         fw.close();
         String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file=unconfirmedCorrespondents.csv" ;
         downloadURL = appURL + "/" +  contentURL;
    }//////////////////////////////////////Download entity information file//////////////////////////////////////////////////////////////////////
	else if(query.equals("entities")){
        //get entitytype
        Short entitytype = Short.parseShort(request.getParameter("type"));
        Map<Short, String> entitytypesOptions= new LinkedHashMap<>();

        for(NEType.Type t: NEType.Type.values())
        {
            entitytypesOptions.put(t.getCode(),t.getDisplayName());
        }
        //put all these data in a csv, copy to temp directory and return the url to download.
         String destfile = Archive.TEMP_SUBDIR + File.separator + "entitiesInfo.csv";
         FileWriter fw = new FileWriter(destfile);
         List<String> line = new ArrayList<>();
         line.add("Entity Type");
         line.add ("EntityName");
         line.add("Score");
         line.add ("Number of messages");

         CSVWriter csvwriter = new CSVWriter(fw, ',', '"',' ',"\n");

         csvwriter.writeNext(line.toArray(new String[line.size()]));
         Set<Short> entityTypesShort= new LinkedHashSet<>();
         if(entitytype==Short.MAX_VALUE)//means all was set from the front end. - export page.
            entityTypesShort=entitytypesOptions.keySet();
         else
             entityTypesShort.add(entitytype);
         for(Short etype: entityTypesShort){
             JSONArray entityinfo = archive.getEntitiesInfoJSON(etype);
                for(int i=0;i<entityinfo.length();i++){
                    JSONArray l = entityinfo.getJSONArray(i);
                    // write the records
                    line = new ArrayList<>();
                    for(int j=0;j<l.length();j++){
                        if(j==0)
                            line.add(entitytypesOptions.get(etype));//add entity display name as first entry in the row.
                        if(!l.isNull(j))
                            line.add(l.get(j).toString());
                        else
                            line.add("unknown");
                        }
                    csvwriter.writeNext(line.toArray(new String[line.size()]));
                }
             }

         csvwriter.close();
         fw.close();
         String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file=entitiesInfo.csv" ;
         downloadURL = appURL + "/" +  contentURL;

    }
  if (!Util.nullOrEmpty(error)){
            result.put("status", 1);
            result.put ("error", error);
        } else {
            result.put ("status", 0);
            result.put ("downloadurl", downloadURL);
        }
out.println (result.toString(4));


%>