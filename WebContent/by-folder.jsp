<%@page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.EmailDocument"%>
<%@ page import="edu.stanford.muse.util.Pair" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>
<head>
    <title>Folders</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <script src="js/jquery.js"></script>
    <script src="js/jquery.dataTables.min.js"></script>
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <jsp:include page="css/css.jsp"/>
    <script src="js/muse.js"></script>
    <script src="js/epadd.js"></script>

    <style type="text/css">
        .js #people {display: none;}
    </style>

    <script type="text/javascript" charset="utf-8">

    </script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%
    AddressBook ab = archive.addressBook;
%>

<%writeProfileBlock(out, archive, "", "Email folders");%>

<br/>
<br/>

<div style="margin:auto; width:900px">
    <table id="people" style="display:none">
        <thead><th>Name</th><th>Incoming messages</th><th>Outgoing messages</th><th>Total</th></thead>
        <tbody>
        </tbody>
    </table>
    <div id="spinner-div" style="text-align:center"><i class="fa fa-spin fa-spinner"></i></div>

    <%
        out.flush(); // make sure spinner is seen
        Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
        JSONArray resultArray = new JSONArray();

        Map<String, Integer> folderToInCount = new LinkedHashMap<>(), folderToOutCount = new LinkedHashMap<>(), folderToTotalCount =  new LinkedHashMap<>();
        for (EmailDocument ed: docs) {
            String folder = ed.folderName;
            Pair<Boolean, Boolean> p = ab.isSentOrReceived(ed.getToCCBCC(), ed.from);
            // p: first sent, second received
            if (p.getFirst()) {
                Integer I = folderToOutCount.get(folder);
                folderToOutCount.put(folder, (I == null) ? 1 : I + 1);
            }
            if (p.getSecond()) {
                Integer I = folderToInCount.get(folder);
                folderToInCount.put(folder, (I == null) ? 1 : I + 1);
            }

            Integer I = folderToTotalCount.get(folder);
            folderToTotalCount.put (folder, (I == null) ? 1 : I+1);
        }

        int count = 0;
        for (String folder: folderToTotalCount.keySet()) {
            JSONArray j = new JSONArray();
            j.put(0, Util.escapeHTML(folder));
            j.put(1, folderToInCount.get(folder));
            j.put(2, folderToOutCount.get(folder));
            j.put(3, folderToTotalCount.get(folder));
            j.put(4, "/epadd/browse?folder=" + folder);
            j.put(5, Util.escapeHTML(folder));
            resultArray.put(count++, j);
        }
    %>
    <script>
        var correspondents = <%=resultArray.toString(4)%>;
        // get the href of the first a under the row of this checkbox, this is the browse url, e.g.
        $(document).ready(function() {
            var clickable_message = function ( data, type, full, meta ) {
                return '<a target="_blank" title="' + full[5] + '" href="' + full[4] + '">' + data + '</a>'; // full[4] has the URL, full[5] has the title tooltip
            };

            $('#people').dataTable({
                data: correspondents,
                pagingType: 'simple',
                order:[[3, 'desc']], // col 12 (outgoing message count), descending
                columnDefs: [{width: "550px", targets: 0}, { className: "dt-right", "targets": [ 1,2,3 ] },{width: "50%", targets: 0},{targets: 0, render:clickable_message}], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
                fnInitComplete: function() { $('#spinner-div').hide(); $('#people').fadeIn(); }
            });
        } );


    </script>

    <div style="clear:both"></div>
</div>
<p>
    <br/>
    <jsp:include page="footer.jsp"/>

</body>
</html>
