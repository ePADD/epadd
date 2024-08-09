<%@page contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="com.google.gson.Gson"%>
<%@page language="java" import="edu.stanford.muse.AddressBookManager.AddressBook"%>
<%@page language="java" import="java.util.ArrayList"%>
<%@page language="java" import="java.util.List"%>
<%@include file="../WebContent/getArchive.jspf" %>

<html>
<head>
	<title>Export</title>
	<link rel="icon" type="image/png" href="../WebContent/images/epadd-favicon.png">


	<link href="../WebContent/jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
    <link rel="stylesheet" href="../WebContent/bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="../WebContent/css/css.jsp"/>

    <script src="../WebContent/js/jquery.js"></script>
    <script type="text/javascript" src="../WebContent/bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="../WebContent/jqueryFileTree/jqueryFileTree.js"></script>
	<script src="../WebContent/js/epadd.js?v=1.1"></script>
	<script src="../WebContent/js/filepicker.js?v=1.1"></script>
</head>

<body>
    <jsp:include page="../WebContent/header.jspf"/>
    <jsp:include page="../WebContent/div_filepicker.jspf"/>

    <%
        writeProfileBlock(out, archive, "Select archive folder to import actions from");
    %>

    <p>
    <br/>
    <br/>

    <% /* all mboxes folders will be in account divs here */ %>
    <div id="filepicker" style="width:900px;padding-left:170px">
        <div class="div-input-field">
            <div class="input-field-label"><i class="fa fa-folder-o"></i> Folder of v1 archive</div>
                <input id="dir" class="dir form-control" type="text" name="dir"/><br/>
                <button class="btn-default browse-button"><i class="fa fa-file"></i>
                    <span>Browse</span>
                </button>
            <br/>
            <br/>

            <div style="text-align:center">
                <button type="button" class="btn btn-cta" onclick="submit(); return false;">
                    Transfer <i class="icon-arrowbutton"></i>
                </button>
            </div>

        </div>
    </div>

    <script>
        function submit() {
            window.location = 'do-transfer-actions.jsp?dir=' + $('#filepicker .dir').val();
            return false;
        }
        new FilePicker($('#filepicker'));
    </script>
    <p>
    <p>

</body>
</html>
