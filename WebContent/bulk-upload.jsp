<%@ page contentType="text/html;charset=UTF-8"%>
<%@include file="getArchive.jspf" %>
<html>
<head>
    <title>Bulk Upload</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <link href="jqueryFileTree/jqueryFileTree.css" rel="stylesheet" type="text/css" media="screen" />
    <jsp:include page="css/css.jsp"/>

    <script src="js/jquery.js"></script>
    <script src="jqueryFileTree/jqueryFileTree.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/filepicker.js" type="text/javascript"></script>

    <script src="js/epadd.js"></script>

    <style type="text/css">
        .go-button, .go-button:hover { background-color: #0075bb; color: #fff; }  /* saumya wants this always filled in, not just on hover */

        .btn-default { height: 37px; }
        label {  font-size: 14px; padding-bottom: 13px; font-weight: 400; color: #404040; } /* taken from form-group label in adv.search.scss */
        .faded { opacity: 0.5; }
        .one-line::after {  content:"";  display:block;  clear:both; }  /* clearfix needed, to take care of floats: http://stackoverflow.com/questions/211383/what-methods-of-clearfix-can-i-use */
        .picker-buttons { margin-top:40px; margin-left:-30px; } /* so that the browse button appears at the right edge of the input box */
        .form-group { margin-bottom: 25px;}
    </style>
</head>
<body>
    <jsp:include page="header.jspf"/>
    <jsp:include page="div_filepicker.jspf"/>

<% writeProfileBlock(out, archive, "Apply flags to multiple messages", "");
String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
%>

<br/>
<br/>
    <div id="all_fields" style="margin-left:170px; width:900px; padding: 10px">

        <div class="panel">
            <div class="panel-heading">List of correspondents</div>

            <div id="filepicker" class="one-line">
                <div class="form-group col-sm-8">
                    <label for="filePath">CSV file location</label>
                    <input id="filePath" class="dir form-control" type="text" name="name" value=""/>
                </div>
                <div class="form-group col-sm-4 picker-buttons">
                    <button id="upload-browse" class="btn-default browse-button">Browse</button>
                    <button onclick="submit()" id="upload-do" style="margin-left: 10px;" class="go-button faded btn-default">Upload</button>
                </div>
            </div>
        </div>

        <script>
            var fp = new FilePicker($('#filepicker'));
            var submit = function(){
                window.location = "bulk-flags?archiveID=<%=archiveID%>&filePath="+$("#filePath").val();
            }
        </script>
    </div>

</body>
</html>
