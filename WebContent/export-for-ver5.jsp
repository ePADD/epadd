<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="edu.stanford.muse.datacache.*"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="edu.stanford.muse.email.Contact" %>
<%@ page import="edu.stanford.muse.email.MailingList" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="com.google.common.collect.LinkedListMultimap" %>
<%@ page import="au.com.bytecode.opencsv.CSVWriter" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
    <title>Export</title>

    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <script src="js/jquery.js"></script>

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <jsp:include page="css/css.jsp"/>
    <script src="js/muse.js"></script>
    <script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Export');</script>
<% 	AddressBook addressBook = archive.addressBook;
String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	String bestName = addressBook.getBestNameForSelf().trim();
	writeProfileBlock(out, archive, "", "Export archive");
%>
<div style="margin-left:170px">
<div id="spinner-div" style="display:none;text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

<%
    // attachmentsForDocs

    String tmpdir = archive.baseDir + File.separator + "tmp";
    String metadatadir= tmpdir + File.separator + "ver5-metadata";

    new File(tmpdir).mkdir();//make sure it exists

    File metadata = new File(metadatadir);
    if(metadata.exists())
        Util.deleteDir(metadata,JSPHelper.log);
    new File(metadatadir).mkdir();
    //create mbox file in localtmpdir and put it as a link on appURL.

    //There are three files (directories) that we want to create for porting this version of archive to the next version.
    //1. Addressbook as addressbook file
    //2. labelmanager directory-- it will have two file.. one labelinfo
    //3. annotation csv file containing a mapping of uniqueid and the annotation of that document.
    //We will dump all these files/directories in tmp directory and pass the zip version of this tmp directory to the client.

    ///////////Addressbook exporting////////////////////////////////////////////////////////////
    %>
    <%!
        private static String dumpForContact(Contact c, String description) {
        StringBuilder sb = new StringBuilder();
        String mailingListOutput = (c.mailingListState & (MailingList.SUPER_DEFINITE | MailingList.USER_ASSIGNED)) != 0 ? MailingList.MAILING_LIST_MARKER : "";
        sb.append ("-- " + mailingListOutput + " " + description + "\n");

        // extra defensive. c.names is already supposed to be a set, but sometimes got an extra blank at the end.
        Set<String> uniqueNames = new LinkedHashSet<String>();
        for (String s: c.names)
        if (!Util.nullOrEmpty(s))
        uniqueNames.add(s);
        // uniqueNames.add(s.trim());

        Set<String> uniqueEmails = new LinkedHashSet<String>();
        for (String s: c.emails)
        if (!Util.nullOrEmpty(s))
        uniqueEmails.add(s);

        for (String s: uniqueNames)
        {
        sb.append (Util.escapeHTML(s) + "\n");
        }
        for (String s: uniqueEmails)
        sb.append (Util.escapeHTML(s) + "\n");
        //sb.append("\n");
        return sb.toString();
        }
    %>
    <%
        StringBuilder addressbookop = new StringBuilder();
        // always toString first contact as self
        Contact self = addressBook.getContactForSelf();
        if (self != null)
            addressbookop.append(dumpForContact(self, "Archive owner"));

        List<Contact> contacts = addressBook.sortedContacts((Collection) archive.getAllDocs());
        for (Contact c: contacts)
            if (c != self)
                addressbookop.append(dumpForContact(c, ""));

//    String dir = request.getParameter ("dir");
        File f = new File(metadatadir+File.separator+"AddressBook");
        //write addressbookop to file f.
        FileWriter fw = new FileWriter(f);
        fw.write(addressbookop.toString());


        fw.flush();
        fw.close();
    %>


    <%
    ///////////labelmanager exporting//////////////////////////////////////////////////////////
        //no need to create label-info.json file as it does not depend upon the archive.. just create it statically once and copy it to LabelMapper directory
        String labeldir = metadatadir + File.separator + "LabelMapper";
        new File(labeldir).mkdir();//make sure it exists
        //Map from Document ID to set of Label ID's
        Multimap<String,String> docToLabelID = LinkedListMultimap.create();

        //we are assuming the following labelid's for default labels..
        //0- do not transfer
        //1- Reviewed
        //3- transfer with restriction (because 2 is a system label - cleared for release)
        for(Document doc: archive.getAllDocsAsSet()){
            EmailDocument edoc = (EmailDocument)doc;
            if(edoc.reviewed){
                docToLabelID.put(Util.hash(edoc.getSignature()),"1");
            }
            if(edoc.doNotTransfer){
                docToLabelID.put(Util.hash(edoc.getSignature()),"0");
            }
            if(edoc.transferWithRestrictions){
                docToLabelID.put(Util.hash(edoc.getSignature()),"3");
            }
        }

        //export docToLabelID map as csv file and put it in dir+"docidmap.csv"
//writing docToLabelIDmap to csv
        try{
            fw = new FileWriter(labeldir+ File.separator+"docidmap.csv");
            CSVWriter csvwriter = new CSVWriter(fw, ',', '"', '\n');

            // write the header line: "DocID,LabelID ".
            List<String> line = new ArrayList<>();
            line.add ("DocID");
            line.add ("LabelID");
            csvwriter.writeNext(line.toArray(new String[line.size()]));

            // write the records
            for(String docid: docToLabelID.keySet()){
                for(String labid: docToLabelID.get(docid)) {
                    line = new ArrayList<>();
                    line.add(docid);
                    line.add(labid);
                    csvwriter.writeNext(line.toArray(new String[line.size()]));
                }
            }
            csvwriter.close();
            fw.close();
        } catch (IOException e) {
            JSPHelper.log.warn("Unable to write docid to label map in csv file");
            return;
        }

        //now copy label-info.json file from classloader to this LabelMapper directory

        // copy lexicons over to the muse dir
        // unfortunately, hard-coded because we are loading as a ClassLoader resource and not as a file, so we can't use Util.filesWithSuffix()
        // we have a different set of lexicons for epadd and muse which will be set up in LEXICONS by the time we reach here
            try( InputStream is = EmailUtils.class.getClassLoader().getResourceAsStream("label-info.json")) {
                if (is == null) {
                    JSPHelper.log.warn("label-info.json file needed for ePADD version 5 porting not found");

                }
                else {
                    JSPHelper.log.info("copying label-info.json to " + labeldir);
                    Util.copy_stream_to_file(is, labeldir + File.separator + "label-info.json");
                }
                is.close();
                } catch (Exception e) {
                Util.print_exception(e, JSPHelper.log);
            }

    %>



    <%
        //////////annotation exporting////////////////////////////////////////////////////////////
        Map<String,String> docToAnnotationMap = new LinkedHashMap<>();
        for(Document doc: archive.getAllDocsAsSet()){
        EmailDocument edoc = (EmailDocument)doc;
        if(!Util.nullOrEmpty(edoc.description))
            docToAnnotationMap.put(Util.hash(edoc.getSignature()),edoc.comment);
        }

        //export as csv file.. Annotations.csv
        try{
            fw = new FileWriter(metadatadir+ File.separator+"Annotations.csv");
            CSVWriter csvwriter = new CSVWriter(fw, ',', '"', '\n');

            // write the header line: "DocID,annotation".
            List<String> line = new ArrayList<>();
            line.add ("DocID");
            line.add ("annotation");
            csvwriter.writeNext(line.toArray(new String[line.size()]));

            // write the records
            for(String docid: docToAnnotationMap.keySet()){
                String annotation= docToAnnotationMap.get(docid); {
                    line = new ArrayList<>();
                    line.add(docid);
                    line.add(annotation);
                    csvwriter.writeNext(line.toArray(new String[line.size()]));
                }
            }
            csvwriter.close();
            fw.close();
        } catch (IOException e) {
            JSPHelper.log.warn("Unable to write annotation to label map in csv file");
            return;
        }



    %>

<%    /////////zipping dir directory////////////////////////////////////////////////////////////

    String pathofoutputzip = tmpdir + File.separator+ "ver5-exported-metadata.zip";//Util.nullOrEmpty(docsetID) ? "epadd-export-all.mbox" : "epadd-export-" + docsetID + ".mbox";
    String dirtozip = new File(metadatadir).getAbsolutePath();

    Util.createZip(dirtozip,pathofoutputzip);
    String appURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    String contentURL = "serveTemp.jsp?archiveID="+archiveID+"&file=" + "ver5-exported-metadata.zip";
    String linkURL = appURL + "/" +  contentURL;

%>

    <br/>

    <a href =<%=linkURL%>>Download metadata file for ePADD version 5</a>
    <p></p>
    This file is in zip format. Unzip it and copy it's content to the corresponding archive's session subfolder in ePADD version 5.<br/>
    <%--In the LabelMapper subfolder add one more file named as 'label-info.json". This file can be downloaded separately from github or contact pchan3@stanford.edu to get it--%>
    <br/>
    <br/>
</div>
<jsp:include page="footer.jsp"/>
</body>
</html>
