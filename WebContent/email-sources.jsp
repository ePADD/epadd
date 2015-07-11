<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="com.google.gson.Gson"%>
<%@page language="java" import="edu.stanford.muse.email.AddressBook"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="java.util.ArrayList"%>
<%@page language="java" import="java.util.List"%>
<%@page language="java" import="java.util.Set"%>
<%@page language="java" %>
<!DOCTYPE HTML>
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Specify Email Sources</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>
	<link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
	<script src="jqueryFileTree/jqueryFileTree.js"></script>
	
	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
	
	<jsp:include page="css/css.jsp"/>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	<style>
		.div-input-field { display: inline-block; width: 400px; margin-left: 20px; line-height:10px; padding:20px;}
		.input-field {width:350px;}
		.input-field-label {font-size: 12px;}
	</style>
</head>
<body style="background-color:white;">
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Import');</script>

<%@include file="profile-block.jspf"%>

<%
Archive archive = (Archive) JSPHelper.getSessionAttribute(session, "archive");
String bestName = "";
String bestEmail = "";
if (archive != null) {
	AddressBook ab = archive.addressBook;
	bestName = ab.getBestNameForSelf();
	Set<String> addrs = ab.getOwnAddrs();
	if (addrs.size() > 0)
		bestEmail = addrs.iterator().next();
	writeProfileBlock(out, bestName, "", "Import email into this archive");
}
%>

<p>

<form method="post" class="form-horizontal">
<div id="all_fields" style="margin-left:170px; width:900px; padding: 10px">
	<section>
		<div class="panel">
			<div class="panel-heading">About this archive</div>
			<div class="div-input-field">
				<div class="input-field-label"><i class="fa fa-user"></i> Name of archive owner</div>
				<br/>
				<div class="input-field">
					<input class="form-control" type="text" name="name" value="<%=bestName%>"/>
				</div>
			</div>
			<div class="div-input-field">
				<div class="input-field-label"><i class="fa fa-envelope"></i> Primary email address</div>
				<br/>
				<div class="input-field">
					<input class="form-control" type="text" name="alternateEmailAddrs" value="<%=bestEmail%>"/>
				</div>
			</div>
		</div>
	</section>

	<section>
	<div id="servers" class="accounts panel">
		<% /* proper field names and ids will be assigned later, when the form is actually submitted */ %>
		<div class="panel-heading">Public Email Accounts (Gmail, Yahoo, Hotmail, Live.com, etc)</div>

		<div class="account">
			<input class="accountType" type="text" style="display:none" id="accountType0" name="accountType0" value="email"/>

			<div class="div-input-field">
				<div class="input-field-label"><i class="fa fa-envelope"></i> Email Address</div>
				<br/>
				<div class="input-field"><input class="form-control" type="text" name="loginName0"/></div>
			</div>

			<div class="div-input-field">
				<div class="input-field-label"><i class="fa fa-key"></i> Password <img class="spinner" id="spinner0" src="images/spinner3-black.gif" width="15" style="margin-left:10px;visibility:hidden"></div>
				<br/>
				<div class="input-field"><input class="form-control" type="password" name="password0"/></div>
			</div>
			<br/>
		</div>
		<br/>

		<button style="margin-left:40px" class="btn-default" onclick="add_server(); return false;"><i class="fa fa-plus"></i> <%=edu.stanford.muse.util.Messages.getMessage("messages", "appraisal.email-sources.another-public-imap")%></button>
		<br/>
		<br/>
	</div> <!--  end servers -->
</section>

	<section>
	<p/>
	<div id="private_servers" class="accounts panel">
		<div class="panel-heading">Private Email IMAP Accounts (Google Apps, university account, corporate account, etc.)<br/>
		</div>
		<p></p>

		<% /* proper field names and ids will be assigned later, when the form is actually submitted */ %>
		<div class="account">
			<input class="accountType" type="text" style="display:none" id="accountType1" name="accountType1" value="email"/>

			<div class="div-input-field">
				<div class="input-field-label"><i class="fa fa-server"></i> IMAP Server</div>
				<br/>
				<div class="input-field"><input class="form-control" type="text" name="server1"/></div>
			</div>
			<br/>

			<div class="div-input-field">
				<div class="input-field-label"><i class="fa fa-envelope"></i> Email Address</div>
				<br/>
				<div class="input-field"><input class="form-control" type="text" name="loginName1"/></div>
			</div>

			<div class="div-input-field">
				<div class="input-field-label"><i class="fa fa-key"></i> Password <img class="spinner" id="spinner1" src="images/spinner3-black.gif" width="15" style="margin-left:10px;visibility:hidden"><br/>
				</div>
				<br/>
				<div class="input-field"><input class="form-control" type="password" name="password1"/></div>
			</div>
			<br/>

		</div>
		<br/>
		<button style="margin-left:40px" class="btn-default" onclick="add_private_server(); return false;"><i class="fa fa-plus"></i> <%=edu.stanford.muse.util.Messages.getMessage("messages", "appraisal.email-sources.another-private-imap")%></button>
		<br/>
		<br/>

	</div> <!--  end servers -->
</section>

	<%
		java.io.File[] rootFiles = java.io.File.listRoots();
		List<String> roots = new ArrayList<String>();
		for (java.io.File f: rootFiles)
			roots.add(f.toString());
		String json = new Gson().toJson(roots);
	%>
	<script> var roots = <%=json%>;</script>
	<script src="js/filepicker.js"></script>


	<section>
		<div id="mboxes" class="accounts panel">
			<div class="panel-heading">Mbox files<br/>
			</div>

			<% /* all mboxes folders will be in account divs here */ %>
			<div class="account">
				<input class="accountType" type="text" style="display:none" name="accountType1" value="mbox"/>
				<div class="div-input-field">
					<div class="input-field-label"><i class="fa fa-folder-o"></i> Folder or file location</div>
					<br/>
					<div class="input-field">
						<input class="dir form-control" type="text" name="mboxDir1"/> <br/>
						<button onclick="return false;" class="btn-default"><i class="fa fa-file"></i>
							<span>Browse</span>
						</button>
					</div>
					<br/>
					<div class="roots" style="display:none"></div>
					<div class="browseFolder"></div>
					<br/>
				</div>
				<br/>
			</div> <!--  end account -->
			<br/>
			<button  style="margin-left:40px" class="btn-default" onclick="add_mboxdir(); return false;"><i class="fa fa-plus"></i> <%=edu.stanford.muse.util.Messages.getMessage("messages", "appraisal.email-sources.another-mbox")%></button>
			<br/>
			<br/>
		</div>
	</section>
	<div style="text-align:center;margin-top:20px">
		<button class="btn btn-cta" id="gobutton" onclick="epadd.do_logins(); return false">Continue <i class="icon-arrowbutton"></i> </button>
	</div>
</div> <!--  all fields -->


</form>

<p>
<jsp:include page="footer.jsp"/>

	<script>
		function add_server() {
			// clone the first account
			var $logins = $('#servers .account');
			var $clone = $($logins[0]).clone();
			$clone.insertAfter($logins[$logins.length-1]);
			$('input', $clone).val(''); // clear the fields so we don't carry over what the cloned fields had
		}

		function add_private_server() {
			// clone the first account
			var $logins = $('#private_servers .account');
			var $clone = $($logins[0]).clone();
			$clone.insertAfter($logins[$logins.length-1]);
			$('input', $clone).val(''); // clear the fields so we don't carry over what the cloned fields had
			$('<br/>').insertAfter($logins[$logins.length-1]);
		}
		var fps = []; // array of file pickers, could have multiple open at the same time, in theory.
		var $account0 = $($('#mboxes .account')[0]);
		fps.push(new FilePicker($account0, roots));

		function add_mboxdir() {
			// first close all accounts, in case they have been expanded etc.
			for (var i = 0; i < fps.length; i++) {
				fps[i].close();
			}
			// clone the last account
			var $a = $('#mboxes .account');
			var $lasta = $($a[$a.length-1]);
			var $clone = $lasta.clone();
			$('input', $clone).val(''); // clear the fields so we don't carry over the values of the original fields
			$clone.insertAfter($lasta);
			$('<br/>').insertAfter($lasta);

			fps.push(new FilePicker($clone, roots));
			return false;
		}

	</script>

</body>
</html>
