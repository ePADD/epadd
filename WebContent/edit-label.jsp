<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="java.util.*"%>
<%@ page import="edu.stanford.muse.email.LabelManager.Label" %>
<%@ page import="edu.stanford.muse.email.LabelManager.LabelManager" %>
<!DOCTYPE html>
<%@include file="getArchive.jspf" %>
<%
    // labelId = null or empty => tis is a new label

	String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	// which lexicon? first check if url param is present, then check if url param is specified
	String labelID = request.getParameter("labelID");
	String labelName = "", labelDescription = "";
    if (!Util.nullOrEmpty(labelID)) {
        Label label = archive.getLabelManager().getLabel(labelID);
        if (label != null) {
            labelName = label.getLabelName();
            labelDescription = label.getDescription();
        }
    }
%>
<html>
<head>
	<title>Edit Label</title>
	<link rel="icon" type="image/png" href="images/epadd-favicon.png"/>

	<link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css"/>
	<jsp:include page="css/css.jsp"/>
    <link rel="stylesheet" href="css/main.css">

	<script src="js/jquery.js"></script>
	<script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/selectpicker.js"></script>
	<script src="js/modernizr.min.js"></script>
	<script src="js/sidebar.js"></script>
	<script type="text/javascript" src="js/muse.js"></script>
	<script src="js/epadd.js"></script>
</head>
<body>
<jsp:include page="header.jspf"/>
<script>epadd.nav_mark_active('Browse');</script>

<%writeProfileBlock(out, archive, "", "Label: " + labelName);%>
<br/>

<!-- when posted, this form goes back to labels screen -->
<form action="labels" method="post">
    <div class="container">
    <!--row-->
    <div class="row">
        <!--form-wraper-->
        <div class="form-wraper clearfix panel">
            <input name="labelID" type="hidden" value="<%=labelID%>" class="form-control"/>
            <input name="archiveID" type="hidden" value="<%=archiveID%>" class="form-control"/>

        <div class="row">
            <h4><%=(Util.nullOrEmpty(labelID) ? "New label" : "Edit Label Id: " + labelID)%></h4>
            <br/>
            <br/>
            <!--File Name-->
            <div class="margin-btm col-sm-6">

                <!--input box-->
                <div class="form-group">
                    <label for="labelName">Label name</label>
                    <input name="labelName" id="labelName" type="text" class="form-control">
                </div>
            </div>

            <!--File Size-->
            <div class="form-group col-sm-6">
                <label for="labelType">Label type</label>
                <select id="labelType" name="labelType" class="form-control selectpicker">
                    <option value="" selected disabled>Choose label type</option>
                    <option value="Restricted">Restriction label</option>
                    <option value="General">General label</option>
                </select>
            </div>
        </div>

        <div class="row">
            <!--Type-->
            <div class="form-group col-sm-12">

                <!--input box-->
                <div class="form-group">
                    <label for="labelDescription">Label description</label>
                    <input name="labelDescription" id="labelDescription" type="text" class="form-control" value="<%=Util.escapeHTML(labelDescription)%>">
                </div>

            </div>

        </div>
    </div>
        <div style="text-align:center">
            <button class="btn btn-cta" type="submit" id="save-button">Save <i class="icon-arrowbutton"></i> </button>
        </div>
    </div>
</div>
</form>
<br/>

<jsp:include page="footer.jsp"/>
</div>
</body>
</html>
