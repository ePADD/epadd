<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="org.json.JSONArray" %><%@ page import="org.json.JSONObject"%><%@ page import="org.json.CDL"%><%@ page import="org.apache.commons.io.FileUtils"%><%@ page import="au.com.bytecode.opencsv.CSVWriter"%><%@ page import="java.util.*"%><%@ page import="edu.stanford.muse.ner.model.NEType"%><%@ page import="edu.stanford.muse.index.*"%><%@ page import="java.io.*"%><%@ page import="java.util.zip.GZIPOutputStream"%><%@ page import="java.util.zip.ZipOutputStream"%><%@ page import="java.util.zip.ZipEntry"%><%@ page import="edu.stanford.muse.util.EmailUtils"%><%@ page import="edu.stanford.muse.webapp.ModeConfig"%><%@ page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
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
	    File files[] = new File(lexiconpath).listFiles(new Util.MyFilenameFilter(null, Lexicon.LEXICON_SUFFIX));
		File fname=null;
	    if (files != null && files.length!=0)
		{
			for (File f : files)
			{
			    if(Lexicon.lexiconNameFromFilename(f.getName()).toLowerCase().equals(lexiconname))
				    {
				        fname = f;
				        //Ideally one lexicon can match to more than one file (with different languages) but right now we are only supporting one
				        //language here. @TODO
				        break;
				    }

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

	        Util.copy_file(addressbookpath,destdir+File.separator+Archive.ADDRESSBOOK_SUFFIX+".txt");
			//prepare a URL with this file
    	    String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file=" + Archive.ADDRESSBOOK_SUFFIX+".txt" ;
            downloadURL = appURL + "/" +  contentURL;
	    }else{
	        error="Addressbook not found";
	    }
//////////////////////////////////////Download label-info.json file//////////////////////////////////////////////////////////////////////
	}else if(query.equals("labels")){
        //save the label manager first so that the file gets updated with any new change in the labels.
        ArchiveReaderWriter.saveLabelManager(archive,Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
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

    }if(query.equals("originaltextasfiles")){

    String pathToDirectory = Archive.TEMP_SUBDIR + File.separator + "messages-as-text-files";
    Util.deleteDir(pathToDirectory,JSPHelper.log);
    new File(pathToDirectory).mkdir();//create the direcotry.
    PrintWriter pw = null;
        try {
            List<Document> docs = archive.getDocsForExport(Archive.Export_Mode.EXPORT_PROCESSING_TO_DELIVERY);
            Set<Document> docset = new LinkedHashSet<>(docs);//convert to set to remove possible duplicates.
            int i=1;
            for(Document doc: docset){
                EmailDocument edoc = (EmailDocument)doc;
                String filename = pathToDirectory+File.separator+"message-"+Integer.toString(i)+".txt";
                edoc.exportToFile(filename,archive);
                i++;
            }
            //now zip pathToDirectory directory and create a zip file in TMP only
            String zipfile = Archive.TEMP_SUBDIR+ File.separator + "all-messages-as-text.zip";
            Util.deleteAllFilesWithSuffix(Archive.TEMP_SUBDIR,"zip",JSPHelper.log);
            Util.zipDirectory(pathToDirectory, zipfile);

            //return it's URL to download
            String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file=all-messages-as-text.zip" ;
            downloadURL = appURL + "/" +  contentURL;
        } catch(Exception e){
            //Util.print_exception ("Error exporting authorities", e, JSPHelper.log);
            e.printStackTrace();
           error="Error exporting confirmed correspondents";///
        }

    }else if(query.equals("to-mbox")){

        String type=request.getParameter("type");
        Set<Document> docset = null;
        String fnameprefix=null;
        Archive.Export_Mode mode=null;
        if(ModeConfig.isAppraisalMode())
            mode = Archive.Export_Mode.EXPORT_APPRAISAL_TO_PROCESSING;
        else if(ModeConfig.isProcessingMode())
            mode =Archive.Export_Mode.EXPORT_PROCESSING_TO_DELIVERY;//to discovery is also same so no difference.
        if(type.equals("all")){
            docset=archive.getAllDocsAsSet();
            fnameprefix="all-messages";
        }else if(type.equals("non-restricted")){
            docset = new LinkedHashSet<>(archive.getDocsForExport(Archive.Export_Mode.EXPORT_PROCESSING_TO_DELIVERY));
            fnameprefix="non-restricted-messages";
        }else if(type.equals("restricted")){
            docset = new LinkedHashSet<>(archive.getDocsForExport(Archive.Export_Mode.EXPORT_PROCESSING_TO_DELIVERY));
            Set<Document> alldocs = new LinkedHashSet<>(archive.getAllDocsAsSet());
            alldocs.removeAll(docset);//now alldocs contain those messages which are not exported,i.e. are restricted.
            docset=alldocs;
            fnameprefix="restricted-messages";
        }


    String pathToFile = Archive.TEMP_SUBDIR + File.separator + fnameprefix+".mbox";
        Util.deleteAllFilesWithSuffix(Archive.TEMP_SUBDIR,"mbox",JSPHelper.log);
    PrintWriter pw = null;
    try {
        pw = new PrintWriter(pathToFile, "UTF-8");
        boolean stripQuoted = true;
        for (Document ed: docset)
            EmailUtils.printToMbox(archive, (EmailDocument) ed, pw,archive.getBlobStore(), stripQuoted);

        pw.close();

         //return it's URL to download
        String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file="+fnameprefix+".mbox" ;
        downloadURL = appURL + "/" +  contentURL;
        } catch(Exception e){
            //Util.print_exception ("Error exporting authorities", e, JSPHelper.log);
           e.printStackTrace();
           error="Error exporting confirmed correspondents";///
        }

    }//////////////////////////////////////Download unconfirmed correspondent file//////////////////////////////////////////////////////////////////////
    else if(query.equals("unconfirmedcorrespondents")){
	    JSONArray correspondents = AddressBook.getCountsAsJson((Collection)archive.getAllDocs(),true,ArchiveReaderWriter.getArchiveIDForArchive(archive));
	    //put all these data in a csv, copy to temp directory and return the url to download.
         String destfile = Archive.TEMP_SUBDIR + File.separator + "unconfirmedCorrespondents.csv";
         Util.deleteAllFilesWithSuffix(Archive.TEMP_SUBDIR,"csv",JSPHelper.log);
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
                 if (l.isNull(j))
                     line.add("unknown");
                 else if(!l.isNull(j))
                    line.add(l.get(j).toString());

                }
             //JSPHelper.log.info(i);
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
          String fnameprefix=null;
         if(entitytype==Short.MAX_VALUE)//means all was set from the front end. - export page.
            {
                  fnameprefix="all";
            }
         else{
                   fnameprefix=entitytypesOptions.get(entitytype);
            }
         String destfile = Archive.TEMP_SUBDIR + File.separator + fnameprefix+"_entitiesInfo.csv";
         Util.deleteAllFilesWithSuffix(Archive.TEMP_SUBDIR,"csv",JSPHelper.log);
         FileWriter fw = new FileWriter(destfile);
         CSVWriter csvwriter = new CSVWriter(fw, ',', '"',' ',"\n");
        //write header info
        //put all these data in a csv, copy to temp directory and return the url to download.
         List<String> line = new ArrayList<>();

         line.add ("EntityName");
         line.add("Score");
         line.add ("Number of messages");
         line.add ("Start Date");
         line.add ("End Date");
          line.add("Entity Type");
         csvwriter.writeNext(line.toArray(new String[line.size()]));

             JSONArray entityinfo = archive.getEntitiesInfoJSON(entitytype);
//  j.put (0, Util.escapeHTML(entity));
//            j.put (1, (float)p.getFirst().score);
//            j.put (2, p.getFirst().freq);
//            j.put (3, altNames);
//            j.put (4, daterange.get(entity).first);
//            j.put (5, daterange.get(entity).second);

                for(int i=0;i<entityinfo.length();i++){
                    JSONArray l = entityinfo.getJSONArray(i);
                    // write the records
                    line = new ArrayList<>();
                    for(int j=0;j<l.length();j++){
                        if(j==3)
                            continue;
                        if(!l.isNull(j))
                            line.add(l.get(j).toString());
                        else
                            line.add("unknown");
                        }
                    csvwriter.writeNext(line.toArray(new String[line.size()]));
                }

         csvwriter.close();
         fw.close();
         String contentURL = "serveTemp.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&file="+fnameprefix+"_entitiesInfo.csv" ;
         downloadURL = appURL + "/" +  contentURL;

    }
  if (!Util.nullOrEmpty(error)){
            result.put("status", 1);
            result.put ("error", error);
        } else {
            result.put ("status", 0);
            result.put ("downloadurl", downloadURL);
            result.put("resultPage",downloadURL);
            result.put("responseText","Preparing file for download!");
        }
out.println (result.toString(4));


%>