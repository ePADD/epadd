<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>
<%@ page import="au.com.bytecode.opencsv.CSVWriter"%>

<%@page language="java" import="java.util.*"%>
<%@page language="java" import="java.io.*"%>
<%@page language="java" import="java.text.SimpleDateFormat"%>
<%@page language="java" import="java.util.stream.Collectors"%>
<%@page language="java" import="edu.stanford.muse.AnnotationManager.AnnotationManager"%>
<%@page language="java" import="edu.stanford.muse.LabelManager.LabelManager"%>
<%@page language="java" import="edu.stanford.muse.util.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>

<%@include file="../getArchive.jspf" %>

<%
    JSONObject result = new JSONObject();
    String linkURL = "";
    int status = 0;

    String docsetID = request.getParameter("docsetID");
    String archiveID = request.getParameter("archiveID");
    String stripquoted = request.getParameter("stripQuoted");

    String dir = Archive.TEMP_SUBDIR + File.separator;
    new File(dir).mkdir();//make sure it exists
    //create mbox file in localtmpdir and put it as a link on appURL.

    //String dir = request.getParameter ("dir");
    File f = new File(dir);

    String csvFilename = "epadd-export-" + docsetID + "-only-headers.csv";

    String pathToCSVFile = f.getAbsolutePath() + File.separator + csvFilename;

    CSVWriter csvwriter;
    FileWriter fw;

    try {
        fw = new FileWriter(pathToCSVFile);

        //remark: CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd)
        csvwriter = new CSVWriter(fw, ',', '"','"',"\n");

    } catch (Exception e){
        out.println ("Sorry, error opening csv file: " + e + ". Please see the log file for more details.");
        Util.print_exception("Error opening csv file: ", e, JSPHelper.log);
        return;
    }

    //check if request contains docsetID then work only on those messages which are in docset
    //else export all messages of mbox.
    Collection<Document> selectedDocs;

    if(!Util.nullOrEmpty(docsetID)){
        DataSet docset = (DataSet) session.getAttribute(docsetID);
        selectedDocs = docset.getDocs();
    }else {
        selectedDocs = new LinkedHashSet<>(archive.getAllDocs());
    }
    JSPHelper.log.info ("export csv has " + selectedDocs.size() + " docs");

    //Print csv headers information for search result csv format
    List<String> line = new ArrayList<>();
    line.add ("From -");
    line.add ("Date:");
    line.add ("From:");
    line.add ("To:");
    line.add ("Cc:");
    line.add ("Bcc:");
    line.add ("Subject:");
    line.add ("Message-ID:");
    line.add ("X-ePADD-Folder:");
    line.add ("X-ePADD-Annotation:");
    line.add ("X-ePADD-Labals:");

    try {
        csvwriter.writeNext(line.toArray(new String[line.size()]));

    } catch (Exception e){
        out.println ("Sorry, error writing csv header: " + e + ". Please see the log file for more details.");
        Util.print_exception("Error writing csv header: ", e, JSPHelper.log);
        return;
    }

    int numOfMessageWritten = 0;
    final int N_MAX_MESSAGE_TO_FLUSHED = 4*1024;    // experimental

    //Prepare csv data rows

    // Try to periodically flush output stream to remedy the mbox file output crash. Experimental
    boolean stripQuoted = "on".equals(stripquoted);

    //line = new ArrayList<>();

    final SimpleDateFormat	sdf1	= new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy");
    final SimpleDateFormat	sdf2	= new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss");
    Date d = new Date();
    String s = "";
    String comment = "";
    String cc = "";
    String bcc = "";
    String labelDescription = "";
    Set<String> labelsIDsForThisDoc;
    Set<String> labelsForThisDoc;
    LabelManager labelManager = archive.getLabelManager();
    AnnotationManager annotationManager = archive.getAnnotationManager();

    // Transverse the document and print out one by one
    for (Document doc: selectedDocs) {
        EmailDocument ed = (EmailDocument) doc;
        line = new ArrayList<>();

        d = (ed.date != null) ? ed.date : new Date();
        s = sdf1.format(d);
        line.add (s);                               //From -

        line.add (sdf2.format(d) + " +0000 GMT");   // watch out, this might not be the right date format // Date:
        line.add (ed.getFromString());             // From:
        line.add (ed.getToString());                // To:

	    cc = ed.getCcString();
    	if (Util.nullOrEmpty(cc))
    		cc = "";
        line.add (cc);                           // Cc:

    	bcc = ed.getBccString();
    	if (Util.nullOrEmpty(bcc))
    		bcc = "";
        line.add (bcc);                          // Bcc:

        line.add (ed.description);                  // Subject:
        line.add (ed.messageID);                    // Message-ID:
        line.add (ed.folderName);                   // X-ePADD-Folder:

        if (!Util.nullOrEmpty(annotationManager.getAnnotation(ed.getUniqueId())))
        {
            comment = annotationManager.getAnnotation(ed.getUniqueId());
            comment = comment.replaceAll("\n", " ");
            comment = comment.replaceAll("\r", " ");
	    }

        line.add (comment);                         // X-ePADD-Annotation:

    	// print labels
    	labelDescription = ""; // default, if there are no labels, we'll always output it.
    	labelsIDsForThisDoc = labelManager.getLabelIDs(ed.getUniqueId());

    	if (!Util.nullOrEmpty(labelsIDsForThisDoc)) {
    		labelsForThisDoc = labelsIDsForThisDoc.stream().map(id -> labelManager.getLabel(id).getLabelName()).collect(Collectors.toSet());
    		labelDescription = Util.join(labelsForThisDoc, ";");
    	}

        line.add (labelDescription);            // X-ePADD-Labels:

        //Print this line
        try {
            csvwriter.writeNext(line.toArray(new String[line.size()]));
            numOfMessageWritten++;

        } catch (Exception e){
               out.println ("Sorry, error writing csv file: " + e + ". Please see the log file for more details.");
               Util.print_exception("Error writing csv file: ", e, JSPHelper.log);
               return;
        }
        if (numOfMessageWritten == N_MAX_MESSAGE_TO_FLUSHED) { // experimental
                        numOfMessageWritten = 0;
                        //pwOnlyHeaders.flush();
                        csvwriter.flush();
        }

    } // End for loop

    try {
        csvwriter.close();
        fw.close();
    } catch (IOException e){
            out.println ("Sorry, error close csv file: " + e + ". Please see the log file for more details.");
            Util.print_exception("Error close csv file: ", e, JSPHelper.log);
            return;
    }

    String appURLOnlyHeaders = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    String contentURLOnlyHeaders = "serveTemp.jsp?archiveID="+archiveID+"&file=" + csvFilename ;
    linkURL = appURLOnlyHeaders + "/" + contentURLOnlyHeaders;

    result.put("status", status);
    result.put("url", linkURL);
    out.println(result.toString());
%>