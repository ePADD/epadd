<%--
  Created by IntelliJ IDEA.
  User: chinmay
  Date: 9/6/17
  Time: 2:31 PM
  To change this template use File | Settings | File Templates.
--%>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@ page import="java.util.Collection" %>
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
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Export');</script>
<% 	AddressBook addressBook = archive.addressBook;
    String bestName = addressBook.getBestNameForSelf().trim();
    writeProfileBlock(out, archive, "", "Export archive");
%>
<div style="margin-left:170px">
    <div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

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

        String exportType = request.getParameter("exportType"); // currently not used

        try {
            Collection<Document> docs = archive.getAllDocs();
            String towrite;
            for(Document doc : docs){
                if(doc instanceof  EmailDocument){
                    EmailDocument d=(EmailDocument)doc;
                    towrite = d.getToString()+","+d.getFromString()+","+d.getCcString()+","+d.getBccString()+","+d.getDate().toString();
                    pw.println(towrite);
                }
            }
            pw.close();
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
