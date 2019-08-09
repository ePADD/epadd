<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%
    JSPHelper.checkContainer(request); // do this early on so we are set up
    request.setCharacterEncoding("UTF-8");
%>
<%@page import="edu.stanford.muse.index.*"%>
<%@page import="edu.stanford.muse.util.DetailedFacetItem"%>
<%@page import="edu.stanford.muse.util.EmailUtils"%>
<%@page import="edu.stanford.muse.util.Pair"%>
<%@page import="edu.stanford.muse.webapp.EmailRenderer"%>
<%@page import="edu.stanford.muse.webapp.HTMLUtils"%>
<%@page import="edu.stanford.muse.webapp.ModeConfig"%>

<%@ page import="java.util.*" %>
<%@ page import="com.google.common.collect.Multimap" %>
<%@ page import="edu.stanford.muse.LabelManager.Label" %>
<%@ page import="edu.stanford.muse.LabelManager.LabelManager" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>

<%--<%@include file="getArchive.jspf" %>--%>

<%
    String searchTerm = JSPHelper.convertRequestParamToUTF8(request.getParameter("term"));

    // compute the title of the page
    String title = request.getParameter("title");
    String sortBy = request.getParameter("sortBy");

    //<editor-fold desc="Derive title if the original title is not set" input="request" output="title"
    // name="search-title-derivation">
    {
        if (Util.nullOrEmpty(title)) {

            // good to give a meaningful title to the browser tab since a lot of them may be open
            // warning: remember to convert, otherwise will not work for i18n queries!
            String sentiments[] = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("lexiconCategory"));

            String[] persons = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("person"));
            String[] attachments = JSPHelper.convertRequestParamsToUTF8(request.getParameterValues("attachment"));

            int month = HTMLUtils.getIntParam(request, "month", -1);
            int year = HTMLUtils.getIntParam(request, "year", -1);
            int cluster = HTMLUtils.getIntParam(request, "timeCluster", -1);

            String sentimentSummary = "";
            if (sentiments != null && sentiments.length > 0)
                for (int i = 0; i < sentiments.length; i++) {
                    sentimentSummary += sentiments[i];
                    if (i < sentiments.length - 1)
                        sentimentSummary += " & ";
                }

            if (searchTerm != null)
                title = "Search: " + searchTerm;
            else if (cluster != -1)
                title = "Cluster " + cluster;
            else if (!Util.nullOrEmpty(sentimentSummary))
                title = sentimentSummary;
            else if (attachments != null && attachments.length > 0)
                title = attachments[0];
            else if (month >= 0 && year >= 0)
                title = month + "/" + year;
            else if (year >= 0)
                title = Integer.toString(year);
            else if (persons != null && persons.length > 0) {
                title = persons[0];
                if (persons.length > 1)
                    title += "+" + (persons.length - 1);
            } else
                title = "Browse";
            title = Util.escapeHTML(title);
        }
    }


    String docsetID = String.format("docset-%08x", EmailUtils.rng.nextInt());// "dataset-1";
%>
<!DOCTYPE HTML>
<html lang="en">
<head>
    <META http-equiv="Content-Type" content="text/html; charset=UTF-8">

    <title><%=title%></title>

    <link rel="icon" type="image/png" href="../images/epadd-favicon.png">

    <link rel="stylesheet" href="../bootstrap/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="../css/jquery.qtip.min.css">
    <%--<jsp:include page=".."/>--%>
    <link rel="stylesheet" href="../css/sidebar.css">
    <link rel="stylesheet" href="../css/main.css">
    <link href='https://fonts.googleapis.com/css?family=Open+Sans:400,600,400italic' rel='stylesheet' type='text/css'>
    <link rel="stylesheet" href="font-awesome-4.3.0/css/font-awesome.min.css">
    <link href='https://fonts.googleapis.com/css?family=Open+Sans' rel='stylesheet' type='text/css'>
    <link href='https://fonts.googleapis.com/css?family=Open+Sans:Semibold' rel='stylesheet' type='text/css'>

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <!-- Optional theme -->
    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap-theme.min.css">
    <link rel="stylesheet" type="text/css" href="css/pm-min.css"/>

    <script type="text/javascript" src="//ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js"></script>
    <script type="text/javascript" src="//maxcdn.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script>

    <script src="js/lib/jquery.backstretch.min.js"></script>

    <script src="https://apis.google.com/js/api:client.js"></script>
    <script src="https://apis.google.com/js/client:platform.js" async defer></script>

    <script src="../js/stacktrace.js" type="text/javascript"></script>
    <script src="../js/jquery.js" type="text/javascript"></script>

    <script type='text/javascript' src='../js/jquery.qtip.min.js'></script>
    <script type="text/javascript" src="../bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="../js/selectpicker.js"></script>
    <script src="../js/modernizr.min.js"></script>
    <script src="../js/sidebar.js"></script>

    <script src="../js/muse.js" type="text/javascript"></script>
    <script src="../js/epadd.js"></script>
    <%--<script src="../js/browse.js" type="text/javascript"></script>--%>
    <script type='text/javascript' src='../js/utils.js'></script>     <!-- For tool-tips -->

    <style>
        div.facets hr { width: 90%; }
        .navbar { margin-bottom: 0; } /* overriding bootstrap */
        .dropdown-header { font-weight: 600;color: black; font-size: 15px;}
         a.opt { color: black;  padding-left: 1.25em; }
        ul.dropdown-menu.inner li, .multi-select .dropdown-menu ul li { border-bottom: none; }
        svg { fill: blue; color: red; stroke: green; }
    </style>


</head>

<body > <!--  override margin because this page is framed. -->
<%--<%@include file="../header.jspf"%>--%>
<div id="gSignInWrapper">
        <button id="customBtn">Google Login</button>
</div>
<script>
    var googleInitSignIn = function() {
        gapi.load('auth2', function(){
            // Retrieve the singleton for the GoogleAuth library and set up the client.
            auth2 = gapi.auth2.init({
                client_id: '893886069594-hsbcr0elt6u0vgr2qa7qei7boop3jgu2.apps.googleusercontent.com',
                // client_id: '606753072852-c2aoi9qu516lvo05gscdb017062a83hm.apps.googleusercontent.com',
                cookiepolicy: 'single_host_origin',
                // Request scopes in addition to 'profile' and 'email'
                //scope: 'additional_scope'
                // scope: 'email profile https://www.googleapis.com/auth/gmail.readonly'
                scope: 'email profile https://www.googleapis.com/auth/gmail.readonly https://mail.google.com/'
            });
            attachSignin(document.getElementById('customBtn'));
        });
    };

    function attachSignin(element) {
        auth2.attachClickHandler(element, {},
            function(googleUser) {
                //https://stackoverflow.com/questions/52883909/google-api-accessing-gmail-from-java-with-id-token-from-javascript
                //authenticate(AUTH_METHOD.GOOGLE, googleUser.getAuthResponse().id_token); // The user has successfully singled in, pass the id token at the server
                authenticate(googleUser.getAuthResponse().id_token, googleUser.getAuthResponse().access_token); // The user has successfully singled in, pass the id token at the server
            }, function(error) {
                console.log("Google login failed");
                // LOG("Google login failed:" + JSON.stringify(error, undefined, 2));
            });
    }
    function authenticate(idToken,accessToken) {
        // validate username and password
        var url = 'ajax/gmail-auth/authenticate.jsp';
var username='';
var password='';
        $.ajax({
            type: 'POST',
            url: url,
            contentType: "application/x-www-form-urlencoded; charset=UTF-8",
            dataType: "json",
            data: {idToken: idToken, username:username, passwd:password, accessToken: accessToken},
            success: function(response) {
                $('#info-modal .modal-title #spinner').remove();
                if (response && response.status === 0) {
                    console.log("login successfull!!");
                    window.location='gmail-top'
                    //location.pathname = location.pathname.replace(/(.*)\/[^/]*/, "$1/"+ 'dashboard');
                }
                else {
                    console.log("Login failed")
                    //LOG("Showing error");
                    $('#info-modal').css("color", "black");
                    $('#info-modal .modal-title').html('Error');
                    $('#info-modal .modal-body').html((response.error ? response.error : "") + '<br/>Contact puzzlemaster@amuselabs.com if this error persists.');
                    $('#info-modal').modal();
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                $('#info-modal .modal-title #spinner').remove();
                $('#info-modal .modal-title').html('Error');
                $('#info-modal .modal-body').html ('Sorry, there was an error in connecting to the server.');
                $('#info-modal').modal();
                //LOG_ON_SERVER("Error in saving the puzzle:" + textStatus + ", errorThrown:" + errorThrown);
            }
        });
    }
    $(document).ready(function() {
        //$.backstretch("images/login_bg_img.png");
        googleInitSignIn(); // Google auth login setup
    });
</script> <!-- make the dataset name available to browse.js -->

<%--<script>epadd.nav_mark_active('Browse');--%>

</div> <!--  browsepage -->
<br/>
<br/>

<div style="clear:both"></div>
<%--
<jsp:include page="../footer.jsp"/>
--%>
</body>
</html>
