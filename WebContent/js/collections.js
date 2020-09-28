"use strict";



var isProcessingMode=false;


var loadCollectionTiles = function(collectionDetails){
    //read from collectionDetails object array and set up collection tiles.
    //1. Iterate over every element of collectionDetails.
    //2. Construct the element to be added
    //3. append the element as a child under "collectionsInfo" div (this div is on the page collections.jsp)

    collectionDetails.forEach(function(collectionInfo){

        var archivecard = $('<div></div>');
        archivecard = $(archivecard).addClass("archive-card").attr("data-dir",collectionInfo.dataDir);
        var landingImage = $('<div></div>');
        landingImage = $(landingImage).addClass("landing-img")
			.css("background-color","#e0e4e6")
			.css("background-size","contain")
			.css("background-repeat","no-repeat")
			.css("background-position","center center")
			.css("background-image", 'url("' + collectionInfo.landingImageURL + '")')

		var landingEditImage = $('<div></div>');
        landingEditImage = $(landingEditImage).addClass("landing-photo-edit")
			.css("text-align","right")
			.css("top","10px")
            .css("right","10px")
            .css("position","relative");

			landingEditImage.append(
                $('<img></img').attr("src","images/edit_summary.svg")
			)
        //     <div class="landing-img-text">
        //     <span style="font-size:20px;font-weight:600;color:#0175BC"><%=Util.nullOrEmpty(cm.shortTitle) ? edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.no-title") : Util.escapeHTML(cm.shortTitle)%></span>
        //     <div class="epadd-separator"></div>
        // <%=Util.nullOrEmpty(cm.shortDescription) ? edu.stanford.muse.util.Messages.getMessage(archiveID,"messages", "collection.no-description") : Util.escapeHTML(cm.shortDescription)%>
        //     </div>
        var landingImageText = $('<div></div>').addClass("landing-img-text").append(
            $('<span></span>').addClass("inner-landing-image-text").text(collectionInfo.shortDescription)
        );

        landingImageText = $(landingImageText).prepend(
            $('<div></div>').addClass("epadd-separator")
        ).prepend(
            $('<span></span>').css("font-size","20px").css("font-weight","600").css("color","#0175BC")
                .text(collectionInfo.shortTitle)
        );

        //landingImageText = $(landingImageText)


        //add landingImage and landingImageText under archiveCard as children which is appended to collectionsInfo div. (If processing mode then add edit image button as well)
        if(isProcessingMode)
            $('#collectionsInfo').append(
                archivecard.append(landingImage.append(landingEditImage)).append(landingImageText)
            );
        else
            $('#collectionsInfo').append(
                archivecard.append(landingImage).append(landingImageText)
            );

        if(isProcessingMode){//Have this function only if it is processing mode.
            $('.upload-btn').click(function (e) {
                //collect archiveID,and addressbookfile field. If  empty return false;
                var filePath = $('#landingPhoto').val();
                if (!filePath) {
                    alert(invalidPathMessage);
                    return false;
                }

                var form = $('#uploadLandingPhotoForm')[0];

                // Create an FormData object
                var data = new FormData(form);

                //hide the modal.
                $('#landingPhoto-upload-modal').modal('hide');
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
                        if (data && data.status === 0) {
                            window.location.reload();
                        } else {
                            epadd.error(uploadImageErrorMessage);
                        }
                    },
                    error: function (jq, textStatus, errorThrown) {
                        epadd.error(uploadImageErrorMessage, 'status =  '+ textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown );
                    }
                });
            });

            $('.landing-photo-edit').click(function (e) {
                var collectionID = $(e.target).closest('.archive-card').attr('data-dir');
                $('input[name="collectionID"]').val(collectionID);
                $('#landingPhoto-upload-modal').modal();
                return false;
            });
        }
    });
    //Code to invoke the collection detail page when clicking on a collection card.
    $('.archive-card').click(function(e) {
        var dir = $(e.target).closest('.archive-card').attr('data-dir');
        window.location = 'collection-detail?collection=' + encodeURIComponent(dir); // worried about single quotes in dir
    });
};

$(document).ready(function() {

	//Step 1: Do an ajax call to get the set of collections' metadata.

    $.ajax({
        type: 'POST',
        url: 'getCollectionDetails',
        datatype: 'json',
//                    allDocs: allDocs,
//         data: post_data,
        success: function (data, textStatus, jqxhr) {
            // fade_spinner_with_delay($spinner);
			console.log(data);
            //Step 2: For each entry in the array of metadata modify dom to add tiles..

            loadCollectionTiles(data);
            epadd.log("Completed flags updated with status " + textStatus);
        },
        error: function (jq, textStatus, errorThrown) {
        	console.log(errorThrown);
            epadd.error("Error setting flags. Please try again, and if the error persists, report it to epadd_project@stanford.edu. (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")");
        }
    });




});
