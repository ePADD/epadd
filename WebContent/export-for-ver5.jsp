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

    String dir = archive.baseDir + File.separator + "tmp" + File.separator + "ver5-metadata";
    new File(dir).mkdir();//make sure it exists
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
        sb.append("\n");
        return sb.toString();
        }
    %>
    <%
        StringBuilder addressbookop = new StringBuilder();
        // always toString first contact as self
        Contact self = addressBook.getContactForSelf();
        if (self != null)
            addressbookop.append(dumpForContact(self, "Archive owner"));

        // build up a map of best name -> contact, sort it by best name and toString contacts in the resulting order
        List<Contact> allContacts = addressBook.allContacts();
        Map<String, Contact> canonicalBestNameToContact = new LinkedHashMap<String, Contact>();
        for (Contact c: allContacts)
        {
        if (c == self)
        continue;
        String bestEmail = c.pickBestName();
        if (bestEmail == null)
        continue;
        canonicalBestNameToContact.put(bestEmail.toLowerCase(), c);
        }

        List<Pair<String, Contact>> pairs = Util.mapToListOfPairs(canonicalBestNameToContact);
        Util.sortPairsByFirstElement(pairs);

        for (Pair<String, Contact> p: pairs)
        {
        Contact c = p.getSecond();
        if (c != self)
        addressbookop.append(dumpForContact(c, c.pickBestName()));
        }


//    String dir = request.getParameter ("dir");
        File f = new File(dir+File.separator+"AddressBook");
        //write addressbookop to file f.
        FileWriter fw = new FileWriter(f);
        fw.write(addressbookop.toString());


        fw.flush();
        fw.close();
    %>


    <%
    ///////////labelmanager exporting//////////////////////////////////////////////////////////
        //no need to create label-info.json file as it does not depend upon the archive.. just create it statically once and copy it to LabelMapper directory
        String labeldir = dir + File.separator + "LabelMapper";
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
                docToLabelID.put(edoc.getUniqueId(),"1");
            }
            if(edoc.doNotTransfer){
                docToLabelID.put(edoc.getUniqueId(),"0");
            }
            if(edoc.transferWithRestrictions){
                docToLabelID.put(edoc.getUniqueId(),"3");
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

    %>



    <%
        //////////annotation exporting////////////////////////////////////////////////////////////
        Map<String,String> docToAnnotationMap = new LinkedHashMap<>();
        for(Document doc: archive.getAllDocsAsSet()){
        EmailDocument edoc = (EmailDocument)doc;
        if(!Util.nullOrEmpty(edoc.description))
            docToAnnotationMap.put(edoc.getUniqueId(),edoc.description);
        }

        //export as csv file.. annotations.csv
        try{
            fw = new FileWriter(dir+ File.separator+"annotations.csv");
            CSVWriter csvwriter = new CSVWriter(fw, ',', '"', '\n');

            // write the header line: "DocID,annotation".
            List<String> line = new ArrayList<>();
            line.add ("DocID");
            line.add ("annotation");
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



    %>
    /////////zipping dir directory////////////////////////////////////////////////////////////

    String fname = "ver5-exported-metadata.zip";//Util.nullOrEmpty(docsetID) ? "epadd-export-all.mbox" : "epadd-export-" + docsetID + ".mbox";


    String pathToFile = dir.getAbsolutePath() + File.separator + fname;
    PrintWriter pw = null;
    try {
        pw = new PrintWriter(pathToFile, "UTF-8");
    } catch (Exception e) {
        out.println ("Sorry, error opening mbox file: " + e + ". Please see the log file for more details.");
        Util.print_exception("Error opening mbox file: ", e, JSPHelper.log);
        return;
    }

    //check if request contains docsetID then work only on those messages which are in docset
    //else export all messages of mbox.
    Collection<Document> selectedDocs;

    if(!Util.nullOrEmpty(docsetID)){
        DataSet docset = (DataSet) session.getAttribute(docsetID);
        selectedDocs = docset.getDocs();
    }else {
        selectedDocs = archive.getAllDocs().stream().collect(Collectors.toSet());
    }
    JSPHelper.log.info ("export mbox has " + selectedDocs.size() + " docs");


// either we do tags (+ or -) from selectedTags
    // or we do all docs from allDocs
    String cacheDir = archive.baseDir;
    String attachmentsStoreDir = cacheDir + File.separator + "blobs" + File.separator;
    BlobStore bs = null;
    try {
        bs = new BlobStore(attachmentsStoreDir);
        JSPHelper.log.info ("Good, found attachments store in dir " + attachmentsStoreDir);
    } catch (IOException ioe) {
        JSPHelper.log.error("Unable to initialize attachments store in directory: " + attachmentsStoreDir + " :" + ioe);
    }

    /*
    String rootDir = JSPHelper.getRootDir(request);
    new File(rootDir).mkdirs();
    String userKey = JSPHelper.getUserKey(session);

    String name = request.getParameter("name");
    if (Util.nullOrEmpty(name))
        name = String.format("%08x", EmailUtils.rng.nextInt());
    String filename = name + ".mbox.txt";

    String path = rootDir + File.separator + filename;
    PrintWriter pw = new PrintWriter (path);
    */

    String noAttach = request.getParameter("noattach");
    boolean noAttachments = "on".equals(noAttach);
    boolean stripQuoted = "on".equals(request.getParameter("stripQuoted"));
    for (Document ed: selectedDocs)
        EmailUtils.printToMbox(archive, (EmailDocument) ed, pw, noAttachments ? null: bs, stripQuoted);
    pw.close();
    String appURL = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    String contentURL = "serveTemp.jsp?archiveID="+archiveID+"&file=" + fname ;
    String linkURL = appURL + "/" +  contentURL;

%>

    <br/>

    <a href =<%=linkURL%>>Download mbox file</a>
    <p></p>
    This file is in mbox format, and can be accessed with many email clients (e.g. <a href="http://www.mozillamessaging.com/">Thunderbird</a>.)
    It can also be viewed with a text editor.<br/>
    On Mac OS X, Linux, and other flavors of Unix, you can usually open a terminal window and type the command: <br/>
    <i>mail -f &lt;saved file&gt;</i>.
    <p>
        This mbox file may also have extra headers like X-ePADD-Folder, X-ePADD-Labels and X-ePADD-Annotation.
    </p>
    <br/>
</div>
<jsp:include page="footer.jsp"/>
</body>
</html>
