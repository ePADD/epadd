<%@ page contentType="text/html; charset=UTF-8"%>
<%@page language="java" import="edu.stanford.muse.email.MuseEmailFetcher"%>
<%@page language="java" import="edu.stanford.muse.index.Archive"%>
<%@page language="java" import="edu.stanford.muse.webapp.Accounts"%>
<%@ page import="edu.stanford.muse.webapp.Sessions" %>
<%
 	JSPHelper.checkContainer(request); // do this early on so we are set up
 request.setCharacterEncoding("UTF-8");
 MuseEmailFetcher m = (MuseEmailFetcher) JSPHelper.getSessionAttribute(session, "museEmailFetcher");
 if (m == null)
 {
 	response.sendRedirect("index?noredirect=");
 	return;
 }
 		
 Accounts.updateUserInfo(request);
 // re-read accounts again only if we don't already have them in this session.
 // later we might want to provide a way for users to refresh the list of folders.
 	
 // we are already logged into all accounts at the point this is called
 // we may not have finished reading the folders though.
 	session.setMaxInactiveInterval(-1);
     // never let session expire

       response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
       response.setHeader("Pragma","no-cache"); //HTTP 1.0
       response.setDateHeader ("Expires", 0); //prevent caching at the proxy server
 	  // remove existing status provider if any because sometimes status provider
 	  // for prev. op prints stale status till new one is put in the session
       if (JSPHelper.getSessionAttribute(session, "statusProvider") != null)
 		  session.removeAttribute("statusProvider");
       session.removeAttribute("loginErrorMessage");

 	  // UI display of folders
       int numFoldersPerRow = 2; // 4;
 %>
<!DOCTYPE HTML>
<html lang="en">
<head>
	<title>Choose Folders</title>

	<link rel="icon" type="image/png" href="images/epadd-favicon.png">

	<script src="js/jquery.js"></script>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

	<jsp:include page="css/css.jsp"/>
	<script type="text/javascript" src="js/stacktrace.js"></script>
	<script src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
	<script type="text/javascript" src="js/showFolders.js"></script>

	<script type="text/javascript">
	     var numFoldersPerRow = <%=numFoldersPerRow%>;
	</script>
</head>
<body style="background-color:white">

<jsp:include page="header.jspf"/>
<%@include file="div_status.jspf"%>
<script>epadd.nav_mark_active('Import');</script>

<%@include file="profile-block.jspf"%>

<script type="text/javascript" src="js/statusUpdate.js"></script>

<%
	Archive archive = (Archive) JSPHelper.getSessionAttribute(session, "archive");
	if (archive != null) {
		writeProfileBlock(out, archive, "", "Add email from folders");
	}
%>


<br/> <!--  some space at the top -->

<div id="div_main" style="min-width:400px" class="folderspage">
<form id="folders">
	<div style="text-align:center;width:90%;margin:auto" >

<%
	int nAccounts = m == null ? 0 : m.getNAccounts();
	JSPHelper.log.info ("Muse has " + nAccounts + " accounts");
	// we could parallelize the fetching of folders and counts for each fetcher at some point
	// need to worry about displaying multiple statuses (statii ?) from each fetcher
	// and aborting the others if one fails etc.
	for (int accountIdx = 0; accountIdx < nAccounts; accountIdx++)
	{
		String accountName = m.getDisplayName(accountIdx);
		// 2 divs for each fetcher: the account header and a div containing the actual folders
		%> <div>
			    <div class="account panel" style="padding:25px;border:solid 1px rgba(160,160,160,0.5)" id="<%=accountName%>">
    				<h1>
						<i class="fa fa-folder-o"></i> <span class="accountHeader"><%=accountName%></span>
					</h1>

    				<div id="accountBody-<%=accountIdx%>" style="width:100%" class="accountBody folders-div">
		    	    	<table align="center">
	    				<tbody class="foldersTable" style="text-align:left">
							<!-- ajax will fill in stuff here -->
	    				</tbody>
	    				</table>
    				</div>
					<br/>
					<div class="select_all_folders" style="display:none;text-align:center">
						<button id="selectall<%=accountIdx%>" type="button" class="btn-default select_all_button" style="display:none"><i class="fa fa-files-o"></i> Select all folders</button>
					</div>
				</div>
   			</div>
   			<%=	accountIdx < nAccounts-1?"<br/>":""%>
		<%
	}

	
	%>

 		<div style="margin-top:25px">

			<div id="div_controls" class="toolbox">
				<div style="float:right">
				<% String checked = "checked"; // (request.getParameter("downloadAttachments") != null) ? "checked" : "";
				%>
				<INPUT TYPE="hidden" ID="downloadAttachments" NAME="downloadAttachments" <%=checked%>/>
				</div>
							<section><div id="date-range" style="display:none"><h2><i class="fa fa-calendar"></i> Date Range</h2>

							<p>
								From: <input name="from" id="from" size="12"
									placeholder="YYYYMMDD" /> To: <input name="to" id="to"
									size="12" placeholder="YYYYMMDD" />
							<p><%=edu.stanford.muse.util.Messages.getMessage("messages", "appraisal.folders.date-range")%>
							</div> </section>

							<div style="float:right">
				<button  class="btn btn-cta" id="go-button" style="display:none" onclick="epadd.submitFolders();return false;">Continue <i class="icon-arrowbutton"></i> </button>
				</div>
				
				<br/>				
			</div> <!--  div_controls -->

	<div>
	</div>
	</div>
</form>
</div> <!-- div_main -->

<div style="height:50px"></div>


	<%
	for (int accountIdx = 0; accountIdx < nAccounts; accountIdx++)
	{
		String accountName = m.getDisplayName(accountIdx);
		String sanitizedAccountName = accountName.replace(".", "-");

		boolean success = true;
		String failMessage = "";

    	boolean toConnectAndRead = !m.folderInfosAvailable(accountIdx);
        %>
        <script type="text/javascript">display_folders('<%=accountName%>', <%=accountIdx%>, true);</script>
		<%
        out.flush();

    	if (toConnectAndRead)
  	    {
    		// mbox can only be with "desktop" mode, which means its a fixed cache dir (~/.muse/user by default)
    		// consider moving to its own directory under ~/.muse
    		String mboxFolderCountsCacheDir = Sessions.CACHE_DIR; 
    		JSPHelper.log.info ("getting folders and counts from fetcher #" + accountIdx);
           	// refresh_folders() will update the status and the account's folders div
            m.readFoldersInfos(accountIdx, mboxFolderCountsCacheDir);
  	    }
	} // end of fetcher loop

    %>

<script type="text/javascript">
// when the page is ready, fade out the spinner and fade in the folders
$(document).ready(function() {
    $('#div_main').fadeIn("slow");
    updateAllFolderSelections(); // some folders may be in selected state when page is refreshed
    var newdiv = document.createElement('div');
    newdiv.setAttribute('id', 'folders-completed');
    var div_main = document.getElementById('div_main');
    div_main.appendChild(newdiv);

	// fade in the select all buttons, and assign their action
	$('.select_all_button').click(toggle_select_all_folders).fadeIn();
});
</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
