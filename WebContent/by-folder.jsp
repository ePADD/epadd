<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page import="edu.stanford.muse.index.EmailDocument"%>
<%@page import="edu.stanford.muse.util.Pair" %>
<%@page import="edu.stanford.muse.util.Util" %>
<%@page import="org.json.JSONArray" %>
<%@page import="java.util.Collection" %>
<%@page import="java.util.LinkedHashMap" %>
<%@page import="java.util.Map" %>
<%@include file="getArchive.jspf" %>
<!DOCTYPE HTML>
<html>

<head>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <script src="js/jquery.js"></script>
    <script src="js/jquery.dataTables.min.js"></script>
    <link href="css/jquery.dataTables.css" rel="stylesheet" type="text/css"/>
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <jsp:include page="css/css.jsp"/>
    <script src="js/muse.js"></script>
    <script src="js/epadd.js?v=1.1"></script>

    <style type="text/css">
        .js #folders {display: none;}
    </style>

    <script type="text/javascript" charset="utf-8">

    </script>
</head>
<body>
<%@include file="header.jspf"%>
<title><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "by-folder.head-folders")%></title>

<script>epadd.nav_mark_active('Browse');</script>

<%
    AddressBook ab = archive.addressBook;
%>

<%writeProfileBlock(out, archive, edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "by-folder.manage-folders") );%>

<br/>
<br/>

<div style="margin:auto; width:1100px">
    <table id="folders" style="display:none">
        <thead><tr><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "by-folder.source")%></th><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "by-folder.folder")%></th><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "by-folder.messages-from-owner")%></th><th><%=edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "by-folder.total")%></th></tr></thead>
        <tbody>
        </tbody>
    </table>
    <div id="spinner-div" style="text-align:center; position:fixed; left:50%; top:50%"><img style="height:20px" src="images/spinner.gif"/></div>

    <%
        out.flush(); // make sure spinner is seen
        Collection<EmailDocument> docs = (Collection) archive.getAllDocs();
        JSONArray resultArray = new JSONArray();
        Map<String, String> folderToSource = new LinkedHashMap<>();
        Map<String, Integer> folderToInCount = new LinkedHashMap<>(), folderToOutCount = new LinkedHashMap<>(), folderToTotalCount =  new LinkedHashMap<>();
        for (EmailDocument ed: docs) {

            String folderName = ed.folderName;
            Pair<Boolean, Boolean> p = ab.isSentOrReceived(ed.getToCCBCC(), ed.from);
            // p: first sent, second received
            if (p.getFirst()) { //means message is sent from owner.
                Integer I = folderToOutCount.get(folderName);
                folderToOutCount.put(folderName, (I == null) ? 1 : I + 1);
            }
            if (p.getSecond()) {//means the message is received by owner (to/cc/bcc)
                Integer I = folderToInCount.get(folderName);
                folderToInCount.put(folderName, (I == null) ? 1 : I + 1);
            }

            Integer I = folderToTotalCount.get(folderName);
            folderToTotalCount.put (folderName, (I == null) ? 1 : I+1);
            if (ed.emailSource != null)
                folderToSource.put(folderName, ed.emailSource);
        }

        int count = 0;
        for (String folderName: folderToTotalCount.keySet()) {
            String source = folderToSource.get(folderName);
            if (source == null)
                source = "";

            JSONArray j = new JSONArray();
            j.put(0, Util.escapeHTML(source));
            j.put(1, Util.escapeHTML(folderName));
            //j.put(2, folderToInCount.get(folder));
            j.put(2, folderToOutCount.getOrDefault(folderName,0));
            j.put(3, folderToTotalCount.getOrDefault(folderName,0));
            j.put(4,  folderName);

            resultArray.put(count++, j);
        }
    %>
    <script>
        var folders = <%=resultArray.toString(4)%>;
        // get the href of the first a under the row of this checkbox, this is the browse url, e.g.
        $(document).ready(function() {
            var clickable_message = function ( data, type, full, meta ) {
                //return '<a target="_blank" title="' + full[1] + '" href="/epadd/browse?archiveID=<%=archiveID%>&folder=' + encodeURIComponent(full[1]) + '">' + data + '</a>'; // full[4] has the URL, full[5] has the title tooltip
                return '<a target="_blank" title="' + encodeURIComponent(full[1]) + '" href="browse?archiveID=<%=archiveID%>&folder=' + encodeURIComponent(full[4]) + '">' + data + '</a>';
                // full[4] has the URL, full[5] has the title tooltip
            };

            $('#folders').dataTable({
                data: folders,
                pagingType: 'simple',
                order:[[3, 'desc']], // col 12 (outgoing message count), descending
                autoWidth: false,
                columnDefs: [{width: "550px", targets: 1}, { className: "dt-right", "targets": [ 2,3 ] },{width: "50%", targets: 1},{targets: 1, render:clickable_message}], /* col 0: click to search, cols 4 and 5 are to be rendered as checkboxes */
                fnInitComplete: function() { $('#spinner-div').hide(); $('#folders').fadeIn(); }
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
