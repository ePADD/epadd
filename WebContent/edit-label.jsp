<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.LabelManager.Label" %>
<%@ page import="edu.stanford.muse.LabelManager.LabelManager" %>
<!DOCTYPE html>
<%@include file="getArchive.jspf" %>
<%
    // labelId = null or empty => tis is a new label

	String archiveID = SimpleSessions.getArchiveIDForArchive(archive);
	// which lexicon? first check if url param is present, then check if url param is specified
	String labelID = request.getParameter("labelID");
	String labelName = "", labelDescription = "", labelType = "";
    if (!Util.nullOrEmpty(labelID)) {
        Label label = archive.getLabelManager().getLabel(labelID);
        if (label != null) {
            labelName = label.getLabelName();
            labelDescription = label.getDescription();
            labelType = label.getType().toString();
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
            <input name="labelID" type="hidden" value="<%=(labelID == null) ? "": labelID  %>" class="form-control"/>
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
                    <input name="labelName" id="labelName" type="text" class="form-control" value="<%=labelName%>">
                </div>
            </div>

            <!--File Size-->
            <div class="form-group col-sm-6">
                <label for="labelType">Label type</label>
                <select id="labelType" name="labelType" class="form-control selectpicker">
                    <option value="" selected disabled>Choose label type</option>
                    <option value="<%=LabelManager.LabType.RESTR_LAB.toString()%>" <%=LabelManager.LabType.RESTR_LAB.toString().equals(labelType) ? "selected":""%> >Restriction label</option>
                    <option value="<%=LabelManager.LabType.GEN_LAB.toString()%>"  <%=LabelManager.LabType.GEN_LAB.toString().equals(labelType) ? "selected":""%> >General label</option>
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

        <div style="display:none" class="restriction-details row">
            <div class="form-group col-sm-6">
                <label for="restrictionType">Restriction type</label>
                <select id="restrictionType" name="restrictionType" class="form-control selectpicker">
                    <option value="" selected disabled>Restriction type</option>
                    <option value="1">Until date</option>
                    <option value="2">Years from date of message</option>
                </select>
            </div>

            <div class="form-group">
                <label for="restrictedUntil"><i class="fa fa-calendar"/> Restricted until</label>
                <input name="restrictedUntil" id="restrictedUntil" type="text" class="form-control" value="<%=label.restrictedUntil%>">
            </div>
            <div class="form-group">
                <label for="restrictedForYears"><i class="fa fa-calendar"/> Restricted for (years)</label>
                <input name="restrictedForYears" id="restrictedForYears" type="text" class="form-control" value="<%=label.restrictedForYears%>">
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
<%--Code base/template to submit this form data using ajax. The steps are as following;--%>
<%--1.Capture the form submit button so that the default action doesn't take place--%>
<%--2.Get all of the data from our form using jQuery--%>
<%--3.Submit using AJAX --%>
<%--4.Show errors if there are any--%>
<%--//https://scotch.io/tutorials/submitting-ajax-forms-with-jquery--%>
<script>
    $(document).ready(function() {
        // process the form
        $('form').submit(function(event) {
            // get the form data using jquery's method
            var formData = $('form').serialize();
            /*// there are many ways to get this data using jQuery (you can use the class or id also)
            var formData = {
                'name'              : $('input[name=name]').val(),
                'email'             : $('input[name=email]').val(),
                'superheroAlias'    : $('input[name=superheroAlias]').val()
            };*/

            // process the form
            $.ajax({
                type        : 'POST', // define the type of HTTP verb we want to use (POST for our form)
                url         : 'ajax/createEditLabels.jsp', // the url where we want to POST
                data        : formData, // our data object
                dataType    : 'json', // what type of data do we expect back from the server
                success: function(data) {epadd.alert('Labels updated!',function(){window.location='labels.jsp?archiveID=<%=archiveID%>'});},
                error: function(jq, textStatus, errorThrown) { var message = ("Error saving labels. (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")"); epadd.log (message); epadd.alert(message); }
            });

            // stop the form from submitting the normal way and refreshing the page
            event.preventDefault();
        });

    });


</script>
<jsp:include page="footer.jsp"/>
</body>
</html>
