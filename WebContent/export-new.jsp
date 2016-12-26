<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="com.google.gson.Gson"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="java.util.ArrayList"%>
<%@page language="java" import="java.util.List"%>
<%@page language="java" import="java.util.Set"%>
<%@ page import="edu.stanford.muse.index.Document" %>
<%@ page import="edu.stanford.muse.index.EmailDocument" %>
<%@page language="java" %>
<!DOCTYPE HTML>
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Specify Email Sources</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png">


	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
	<jsp:include page="css/css.jsp"/>

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	<script src="jqueryFileTree/jqueryFileTree.js"></script>
	<script src="js/filepicker.js"></script>

	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	<style>
		.input-field {padding-left: 20px; width:70%; display:inline-block;}
		.input-field-label {padding-left: 20px; font-size: 12px;}
		.mini-box { background-color: #f5f5f8; display: inline-block; width:200px; padding:20px; margin-right:22px;}
        .mini-box:hover { background-color: #0075bb; color: #fff; cursor: pointer; }
        .mini-box .number { font-size: 200%; margin-bottom:10px; }
        .panel { padding: 10px 0px;}
        .faded { opacity: 0.5; }
	</style>
</head>
<body style="background-color:white;">
<jsp:include page="header.jspf"/>
<jsp:include page="div_filepicker.jspf"/>

<script>epadd.nav_mark_active('Export');</script>

<%@include file="profile-block.jspf"%>

<%
Archive archive = (Archive) JSPHelper.getSessionAttribute(session, "archive");
String bestName = "";
String bestEmail = "";
if (archive != null) {
	AddressBook ab = archive.addressBook;
	Set<String> addrs = ab.getOwnAddrs();
	if (addrs.size() > 0)
		bestEmail = addrs.iterator().next();
	writeProfileBlock(out, archive, "", "Export archive");
}

    int messagesToExport = 0, annotatedMessages = 0, restrictedMessages = 0, messagesNotToExport = 0;
    for (Document d: archive.getAllDocs()) {
         EmailDocument ed = (EmailDocument) d;
         if (ed.doNotTransfer)
             messagesNotToExport++;
         if (ed.transferWithRestrictions)
             restrictedMessages++;
         if (!ed.doNotTransfer && !ed.transferWithRestrictions)
              messagesToExport++;
         if (!Util.nullOrEmpty(ed.comment))
             annotatedMessages++;
    }
%>

<p>

<div id="all_fields" style="margin-left:170px; width:900px; padding: 10px">
	<b>Review messages</b>
    <br/>
	<br/>
	<div onclick="window.location='export-review?type=transfer'" class="mini-box">
        <div class="number"><%=Util.commatize(messagesToExport)%></div>
		Unrestricted messages
	</div>
	<div onclick="window.location='export-review?type=annotated'" class="mini-box">
        <div class="number"><%=Util.commatize(annotatedMessages)%></div>
        Annotated messages
	</div>
	<div onclick="window.location='export-review?type=transferWithRestrictions'" class="mini-box">
        <div class="number"><%=Util.commatize(restrictedMessages)%></div>
        Restricted messages
	</div>
	<div onclick="window.location='export-review?type=doNotTransfer'" class="mini-box" style="margin-right:0px">
        <div class="number"><%=Util.commatize(messagesNotToExport)%></div>
		Messages not to export
	</div>

	<br/>
	<br/>

	<section>
		<div class="panel">
			<div class="panel-heading">Export messages and attachments</div>

			<div class="input-field-label">Export to next ePADD module</div>
			<div class="one-line" id="export-next">
				<div class="input-field">
					<input class="form-control" type="text" name="name" value="<%=bestName%>"/>
				</div>
				<div style="display:inline-block">
					<button id="export-next-browse" class="btn-default browse-button">Browse</button>
					<button id="export-next-do" style="margin-left: 10px;opacity:0.5;" class="disabled btn-default">Export</button>
				</div>
			</div>
            <br/>
			<div class="input-field-label">Export to mbox</div>
			<div class="one-line" id="export-mbox">
				<div class="input-field">
					<input class="form-control" type="text" name="name" value="<%=bestName%>"/>
				</div>
				<div style="display:inline-block">
					<button id="export-mbox-browse" class="btn-default browse-button">Browse</button>
					<button id="export-mbox-do" style="margin-left: 10px;opacity:0.5;" class="disabled btn-default">Export</button>
				</div>
			</div>
			<br/>
		</div>
	</section>

    <section>
        <div class="panel" id="export-attach">
            <div class="panel-heading">Export attachments</div>

            <div class="input-field-label">Specify location</div>
            <div class="one-line">
                <div class="input-field">
                    <input class="form-control" type="text" name="name" value="<%=bestName%>"/>
                </div>
                <div style="display:inline-block">
                    <button id="export-attach-browse" class="btn-default browse-button">Browse</button>
                    <button id="export-attach-do" style="margin-left: 10px;" class="faded btn-default">Export</button>
                </div>
            </div>


            <br/>
        </div>
    </section>

    <section>
        <div class="panel" id="export-auth">
            <div class="panel-heading">Export authorities (CSV)</div>

            <div class="input-field-label">Specify location</div>
            <div class="one-line">
                <div class="input-field">
                    <input class="form-control" type="text" name="name" value="<%=bestName%>"/>
                </div>
                <div style="display:inline-block">
                    <button id="export-auth-browse" class="btn-default browse-button">Browse</button>
                    <button id="export-auth-do" style="margin-left: 10px;" class="faded btn-default">Export</button>
                </div>
            </div>


            <br/>
        </div>
    </section>

</div> <!--  all fields -->


<p>

	<script type="text/javascript">
		$(document).ready(function() {
            new FilePicker($('#export-next'));
            new FilePicker($('#export-mbox'));
            new FilePicker($('#export-attach'));
            new FilePicker($('#export-auth'));

			$('input[type="text"]').each(function () {
				var field = 'email-source:' + $(this).attr('name');
				if (!field)
					return;
				var value = localStorage.getItem(field);
				$(this).val(value);
			});
		});

	</script>

</body>
</html>
