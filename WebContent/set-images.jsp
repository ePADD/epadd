<%@ page import="java.io.File" %>
<%@ page import="edu.stanford.muse.index.Archive" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.index.ArchiveReaderWriter" %>
<%--<%@include file="getArchive.jspf" %>--%>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Set Images</title>
    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <jsp:include page="css/css.jsp"/>

    <script src="js/jquery.js"></script>
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>
    <script src="js/epadd.js?v=1.1" type="text/javascript"></script>
</head>
<body>
 <%
     String collectionID = request.getParameter("collection");
     Archive archive= JSPHelper.getArchive(request);

     //INV: here collectionID=null && archiveID="" can not be true. Exactly one should be true.
     //This page can be invoked in two different context. One: from the colleciton-detail page with collection
     //dir as an argument (in "collection") and two: from epadd-settings page with archiveID as an argument.
     //collectionID is empty if the mode is appraisal mode. Otherwise we get the basedir from archive and set collecitonid
     //as the name of the base directory.

 %>
 <%@include file="header.jspf"%>

    <script>epadd.nav_mark_active('Collections');</script>

    <div style="margin-left:170px">
        You can upload images that represent the collection here. Only PNG format files are supported.
        <p></p>
        <form id="uploadProfilePhotoForm" method="POST" enctype="multipart/form-data" >
            <%--adding a hidden input field to pass collectionID the server. This is a common pattern used to pass--%>
            <%--archiveID in all those forms where POST was used to invoke the server page.--%>
            <input type="hidden" value="<%=collectionID%>" name="collection"/>
                <input type="hidden" value="<%=archiveID%>" name="archiveID"/>

            Profile Photo: (Aspect ratio 1:1)<br/>
            <%
                String file = null;
                if(!Util.nullOrEmpty(collectionID))
                    file = collectionID + File.separator + Archive.BAG_DATA_FOLDER+ File.separator+ Archive.IMAGES_SUBDIR + File.separator + "profilePhoto.png";
                else if(!Util.nullOrEmpty(archiveID))
                    file = new File(archive.baseDir).getName() + File.separator + Archive.BAG_DATA_FOLDER+ File.separator+ Archive.IMAGES_SUBDIR + File.separator + "profilePhoto";
            %>

                <div class="profile-small-img" style="background-image:url('serveImage.jsp?file=<%=file%>')"></div>
                <%--<div class="profile-small-img" style=" background-size: contain;--%>
                        <%--background-repeat: no-repeat;--%>
                        <%--width: 50%;--%>
                        <%--height: 50%;--%>
                        <%--padding-top: 20%;  background-image:url('serveImage.jsp?file=<%=file%>')">--%>
            <input type="file" name="profilePhoto" id="profilePhoto" /> <br/><br/>

            Landing Page Photo: (Aspect ratio 4:3)<br/>
            <%
                if(!Util.nullOrEmpty(collectionID))
                    file = collectionID + File.separator + Archive.BAG_DATA_FOLDER+ File.separator+ Archive.IMAGES_SUBDIR + File.separator + "landingPhoto";
                else if(!Util.nullOrEmpty(archiveID))
                    file = new File(archive.baseDir).getName() + File.separator + Archive.BAG_DATA_FOLDER+ File.separator+  Archive.IMAGES_SUBDIR + File.separator + "landingPhoto";
            %>
            <div class="landing-img" style="background-image:url('serveImage.jsp?file=<%=file%>')"></div>
            <br/>
            <br/>
            <input type="file" name="landingPhoto" id="landingPhoto" />
            <br/><br/>

            <%
                if(!Util.nullOrEmpty(collectionID))
                    file = collectionID + File.separator + Archive.BAG_DATA_FOLDER+ File.separator+  Archive.IMAGES_SUBDIR + File.separator + "bannerImage";
                else if(!Util.nullOrEmpty(archiveID))
                    file = new File(archive.baseDir).getName() + File.separator + Archive.BAG_DATA_FOLDER+ File.separator+ Archive.IMAGES_SUBDIR + File.separator + "bannerImage";
            %>
            Banner Image: (Aspect ratio 2.5:1)<br/>
                <div class="banner-img" style=" background-size: contain;
                        background-repeat: no-repeat;
                        width: 50%;
                        height: 50%;
                        padding-top: 20%;  background-image:url('serveImage.jsp?file=<%=file%>')"> <!-- https://stackoverflow.com/questions/2643305/centering-a-background-image-using-css -->
<%--Height added in style otherwise the image was not being visible--%>
                </div>
            <br/>
            <br/>
            <input type="file" name="bannerImage" id="bannerImage" />
            <br/><br/>
            <button id="upload-btn" class="btn btn-cta" onclick="uploadPhotoHandler();return false;">Upload <i class="icon-arrowbutton"></i></button>
        </form>
    </div>
 <script>
     var uploadPhotoHandler=function() {
         event.preventDefault(); //prevent default action

         //collect archiveID,and addressbookfile field. If  empty return false;
         var profilePhotoFilePath = $('#profilePhoto').val();
         var landingPhotoFilePath = $('#landingPhoto').val();
         var bannerImageFilePath = $('#bannerImage').val();

         if (!profilePhotoFilePath && !landingPhotoFilePath && !bannerImageFilePath) {
             alert('Please provide the path of at least one image type!');
             return false;
         }



         var form = $('#uploadProfilePhotoForm')[0];

         // Create an FormData object
         var data = new FormData(form);
         //hide the modal.
         //$('#profilePhoto-upload-modal').modal('hide');
         //now send to the backend.. on it's success reload the same page. On failure display the error message.

         $.ajax({
             type: 'POST',
             enctype: 'multipart/form-data',
             processData: false,
             url: "ajax/upload-images.jsp",
             contentType: false,
             cache: false,
             data: data,
             success: function (data) {
                 //epadd.success('Profile photo uploaded and applied.', function () {
                 window.location.reload();
                 //});
             },
             error: function (jq, textStatus, errorThrown) {
                 epadd.error("Error uploading files, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown);
             }
         });
     }

    /* $('div.profile-pic-edit').click (function() {
         $('#profilePhoto-upload-modal').modal();
     });*/

 </script>
    <jsp:include page="footer.jsp"/>

    </body>
</html>