<%--
  Created by IntelliJ IDEA.
  User: chinmay
  Date: 9/6/17
  Time: 2:31 PM
  To change this template use File | Settings | File Templates.
--%>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@ page import="java.util.Collection" %>
<%@ page import="org.apache.commons.csv.CSVFormat" %>
<%@ page import="org.apache.commons.csv.CSVPrinter" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.mail.Address" %>
<%@ page import="javax.mail.internet.InternetAddress" %>

<%@include file="getArchive.jspf" %>
<html>
<head>
    <title>Export Headers</title>

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
<%@include file="header.jspf"%>
<script>epadd.nav_mark_active('Export');</script>
<% 	AddressBook addressBook = archive.addressBook;
    writeProfileBlock(out, archive, "Export archive");
%>
<div style="margin-left:170px">
    <div id="spinner-div" style="text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>

    <%
        String dir = request.getParameter ("dir");

        File f = new File(dir);
        String pathToFile;
        if (f.isDirectory())
            pathToFile = f.getAbsolutePath() + File.separator + "epadd-headers.csv";
        else
            pathToFile = f.getAbsolutePath();

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(pathToFile, "UTF-8");
        } catch (Exception e) {
            out.println ("Sorry, error opening header file: " + e + ". Please see the log file for more details.");
            Util.print_exception("Error opening header file: ", e, JSPHelper.log);
            return;
        }

        // String exportType = request.getParameter("exportType"); // currently not used

        try {
            char CSV_RECORD_SEP=',';
            //In order to separate different mail id's within a single field (like cc or bcc) we use the following separator.
            String EMAILID_SEP=";";
            //If a best name/mail id already contains EMAILID_SEP then we need to convert it to something else before proceeding.
            String EMAILID_SEP_CONVERSION="_";

            Collection<Document> docs = archive.getAllDocs();
            //Create csv file format configuration with record separator as new line and value delimiter as comma
            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n").withDelimiter(CSV_RECORD_SEP);
            //Create CSVPrinter with appropriate file writer and configuration object
            CSVPrinter csvFilePrinter = new CSVPrinter(pw,csvFileFormat);

            //Specify the header content of the csv file and print it to csv
            String [] fileHeader = {"Date","From_mailid","From_Bestname","To_mailids","To_Bestnames","cc_mailids","cc_bestnames",
            "bcc_mailids","bcc_bestnames"};
            csvFilePrinter.printRecord(fileHeader);

            //Iterate over each document of this archive and print sender/receiver/cc/bcc's and date for that document
            for(Document edoc : docs){
                if(edoc instanceof  EmailDocument){
                    EmailDocument doc=(EmailDocument)edoc;
                    //Transfer the headers only if the message is not marked as DNT(do not transfer) or TWR(Transfer with restrictions).
                    // @TODO-Export Take decision on exporting based on labels of this document set
                    if(true) {

                       List record = new ArrayList<String>();
                       //add date in the start..
                        record.add(doc.dateString());

                       //from address computation with best names
                        if (doc.from != null){
                            StringBuilder fromEmailIds = new StringBuilder();
                            StringBuilder fromBestNames = new StringBuilder();

                            for (Address a : doc.from)
                                if (a instanceof InternetAddress) {
                                    //get email id and its best name
                                    String email = ((InternetAddress) a).getAddress();
                                    String bestname = addressBook.getBestDisplayNameForEmail(email);
                                    //Can email or bestname be null? CHECK_ASSERTION
                                    //getBestDisplayNameForEmail can be empty but not null
                                    //convert EMAILID_SEP (if any) present in email or bestname to EMAILID_SEP_CONVERSION
                                    email = email.replaceAll(EMAILID_SEP,EMAILID_SEP_CONVERSION);
                                    bestname = bestname.replaceAll(EMAILID_SEP,EMAILID_SEP_CONVERSION);
                                    fromEmailIds.append(email + EMAILID_SEP);
                                    fromBestNames.append(bestname + EMAILID_SEP);
                                }
                            //remove last separator only if these string builders are non-empty.
                            if(fromEmailIds.length()!=0)
                                fromEmailIds.deleteCharAt(fromEmailIds.length()-1);
                            if(fromBestNames.length()!=0)
                                fromBestNames.deleteCharAt(fromBestNames.length()-1);
                            //add strings to records
                            record.add(fromEmailIds.toString());
                            record.add(fromBestNames.toString());
                        }

                        //To address computation with best names
                        if (doc.to != null) { // to can sometimes be null e.g. for mbox files have a "IMAP server data -- DO NOT DELETE" as the first message
                            StringBuilder toEmailIds = new StringBuilder();
                            StringBuilder toBestNames = new StringBuilder();

                            for (Address a : doc.to)
                                if (a instanceof InternetAddress) {
                                    //get email id and its best name
                                    String email = ((InternetAddress) a).getAddress();
                                    String bestname = addressBook.getBestDisplayNameForEmail(email);
                                    //Can email or bestname be null? CHECK_ASSERTION
                                    //getBestDisplayNameForEmail can be empty but not null
                                    //convert EMAILID_SEP (if any) present in email or bestname to EMAILID_SEP_CONVERSION
                                    email = email.replaceAll(EMAILID_SEP,EMAILID_SEP_CONVERSION);
                                    bestname = bestname.replaceAll(EMAILID_SEP,EMAILID_SEP_CONVERSION);
                                    toEmailIds.append(email + EMAILID_SEP);
                                    toBestNames.append(bestname + EMAILID_SEP);
                                }
                            //remove last separator only if these string builders are non-empty.
                            if(toEmailIds.length()!=0)
                                toEmailIds.deleteCharAt(toEmailIds.length()-1);
                            if(toBestNames.length()!=0)
                                toBestNames.deleteCharAt(toBestNames.length()-1);

                            record.add(toEmailIds.toString());
                            record.add(toBestNames.toString());
                        }

                        //cc address computation with best names
                        if (doc.cc != null) { // to can sometimes be null e.g. for mbox files have a "IMAP server data -- DO NOT DELETE" as the first message
                            StringBuilder ccEmailIds = new StringBuilder();
                            StringBuilder ccBestNames = new StringBuilder();

                            for (Address a : doc.cc)
                                if (a instanceof InternetAddress) {
                                    //get email id and its best name
                                    String email = ((InternetAddress) a).getAddress();
                                    String bestname = addressBook.getBestDisplayNameForEmail(email);
                                    //Can email or bestname be null? CHECK_ASSERTION
                                    //getBestDisplayNameForEmail can be empty but not null
                                    //convert EMAILID_SEP (if any) present in email or bestname to EMAILID_SEP_CONVERSION
                                    email = email.replaceAll(EMAILID_SEP,EMAILID_SEP_CONVERSION);
                                    bestname = bestname.replaceAll(EMAILID_SEP,EMAILID_SEP_CONVERSION);
                                    ccEmailIds.append(email + EMAILID_SEP);
                                    ccBestNames.append(bestname + EMAILID_SEP);
                                }
                            //remove last separator only if these string builders are non-empty.
                            if(ccEmailIds.length()!=0)
                                ccEmailIds.deleteCharAt(ccEmailIds.length()-1);
                            if(ccBestNames.length()!=0)
                                ccBestNames.deleteCharAt(ccBestNames.length()-1);

                            record.add(ccEmailIds.toString());
                            record.add(ccBestNames.toString());

                        }

                        //bcc address computation with best names
                        if (doc.bcc != null) { // to can sometimes be null e.g. for mbox files have a "IMAP server data -- DO NOT DELETE" as the first message
                            StringBuilder bccEmailIds = new StringBuilder();
                            StringBuilder bccBestNames = new StringBuilder();

                            for (Address a : doc.bcc)
                                if (a instanceof InternetAddress) {
                                    //get email id and its best name
                                    String email = ((InternetAddress) a).getAddress();
                                    String bestname = addressBook.getBestDisplayNameForEmail(email);
                                    //Can email or bestname be null? CHECK_ASSERTION
                                    //getBestDisplayNameForEmail can be empty but not null
                                    //convert EMAILID_SEP (if any) present in email or bestname to EMAILID_SEP_CONVERSION
                                    email = email.replaceAll(EMAILID_SEP,EMAILID_SEP_CONVERSION);
                                    bestname = bestname.replaceAll(EMAILID_SEP,EMAILID_SEP_CONVERSION);
                                    bccEmailIds.append(email + EMAILID_SEP);
                                    bccBestNames.append(bestname + EMAILID_SEP);
                                }

                            //remove last separator only if these string builders are non-empty.
                            if(bccEmailIds.length()!=0)
                                bccEmailIds.deleteCharAt(bccEmailIds.length()-1);
                            if(bccBestNames.length()!=0)
                                bccBestNames.deleteCharAt(bccBestNames.length()-1);

                            record.add(bccEmailIds.toString());
                            record.add(bccBestNames.toString());
                        }
                        csvFilePrinter.printRecord(record);

                    }
                }
            }
            pw.close();
            csvFilePrinter.close();
        } catch(Exception e){
            Util.print_exception ("Error exporting headers", e, JSPHelper.log);
            e.printStackTrace();
            out.println(e.getMessage());
            return;
        }
    %>

    <script>$('#spinner-div').hide();</script>
    <p>
    <p>
        Archive headers exported to <%=Util.escapeHTML(pathToFile)%><br/>
</div>
<p>
    <br/>
    <br/>
</body>
</html>
