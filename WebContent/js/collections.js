"use strict";



var isProcessingMode=false;
var _collectionDetails;
var browseType; //either collection or institution.
var institutionName;

/*This method renders the page when user clicks on 'Browse collections' tab on navbar. It renders a header with search bar and the
collection details in form of tiles. The collection information is stored in variable collectionDetails.
the first argument of this method. The headername to display is passed as the second argument.
redrawComplete is a boolean argument which controls if the header (header string + search box) should also get redrawn.
This is used to control the redrawing of collection tiles when user search something in the searchbox.
 */
var renderBrowseCollection = function(collectionDetails, headerstring, redrawComplete){
    //read from collectionDetails object array and set up collection tiles.
    //1. Iterate over every element of collectionDetails.
    //2. Construct the element to be added
    if(redrawComplete) {
        //3. append the element as a child under "collectionsInfo" div (this div is on the page collections.jsp)
        $('#collectionsInfo-header').empty();
        $('#collectionsInfo-details').empty();
        //Add div code as the first element to have search box and the header. Header name is passed as an argument here.
        var searchbox_header = '<div style="margin:auto;width:1100px;">\n' +
            '    <div class="container">\n' +
            '        <div class="row" style="height:10px;margin-left:-1px;">\n' +
            '            <div class="col-sm-4" ">\n' +
            '                <h1 class="text-left" style="margin-left:-30px;">' + headerstring + '</h1>\n' +
            '            </div>\n' +
            '            <div class="col-sm-8 inner-addon right-addon">\n' +
            '                <input name="search-collection" id="search-collection" type="text" class="form-control" placeholder="Search collection">\n' +
            '                <i class="glyphicon glyphicon-search form-control-feedback"></i>\n' +
            '            </div>\n' +
            '        </div>\n' +
            '    </div>\n' +
            '    <hr>\n' +
            '</div>';
        $('#collectionsInfo-header').append(searchbox_header);
    }else{
        $('#collectionsInfo-details').empty();
    }
    collectionDetails.forEach(function(collectionInfo){

        var shortDescription= collectionInfo.shortDescription;
        var shortTitle = collectionInfo.shortTitle;
        var dataDir =  collectionInfo.dataDir;
        var landingImageURL = collectionInfo.landingImageURL;
        var archivecard = $('<div></div>');
        archivecard = $(archivecard).addClass("archive-card").attr("data-dir",dataDir);
        var landingImage = $('<div></div>');
        landingImage = $(landingImage).addClass("landing-img")
			.css("background-color","#e0e4e6")
			.css("background-size","contain")
			.css("background-repeat","no-repeat")
			.css("background-position","center center")
			.css("background-image", 'url("' + landingImageURL + '")')

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
            $('<span></span>').addClass("inner-landing-image-text").text(shortDescription)
        );

        landingImageText = $(landingImageText).prepend(
            $('<div></div>').addClass("epadd-separator")
        ).prepend(
            $('<span></span>').css("font-size","20px").css("font-weight","600").css("color","#0175BC")
                .text(shortTitle)
        );

        //landingImageText = $(landingImageText)

        //add landingImage and landingImageText under archiveCard as children which is appended to collectionsInfo div. (If processing mode then add edit image button as well)
        if(isProcessingMode)
            $('#collectionsInfo-details').append(
                archivecard.append(landingImage.append(landingEditImage)).append(landingImageText)
            );
        else
            $('#collectionsInfo-details').append(
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

//Register method for search box..
$('body').on('keyup','#search-collection', function(e){

    var extractCollectionData = function(collection){
        //Return a canonical (string)representation of the collection information. This will include everything that will be
        //present in the collection metadata of a collection so that the filtering can be done easily on this data.
        return collection.shortDescription+"___"+collection.shortTitle;
    }
    // get the content of the #search-collection text box.
    var term = $(e.target).val();
    // if term is not already quoted, quote it now
    term = term.trim();
    if (!(term && term.length > 2 && term.charAt(0) == '"' && term.charAt(term.length-1) == '"')) {
        //filter collectionDetails json data for the content written in this text box..
        var modCollectionDetails = _collectionDetails.filter(collection=>(extractCollectionData(collection).toLowerCase().includes(term.toLowerCase())));
        //Render renderBoxCollection() with this modified data and headerstring variable..
        renderBrowseCollection(modCollectionDetails,"Browse Collections",false);
    }
});
/*This method renders the page when user clicks on 'Browse institutions' tab on navbar. It renders a header with search bar and the
institution details in form of a table.
redrawComplete is a boolean variable that controls if the search header should also get redrawn or not. This is used to
 */
var renderBrowseInstitutions = function(institutionDetails, redrawComplete){
    //clear the div.
    $('#collectionsInfo-header').empty();
    $('#collectionsInfo-details').empty();

    //Add the header string and the search bar first .
    var header_search = '<div style="margin:auto;width:1100px;">\n' +
        '    <div class="container">\n' +
        '        <div class="row" style="height: 10px;margin-left:-1px;">\n' +
        '            <div class="col-sm-4" >\n' +
        '                <h1 class="text-left" style="margin-left:-30px;">Browse Institutions</h1>\n' +
        '            </div>\n' +
        '            <div class="col-sm-8 inner-addon right-addon">\n' +
        '                <input name="search-institution" id="search-institution" type="text" class="form-control" placeholder="Search institutions">\n' +
        '                <i class="glyphicon glyphicon-search form-control-feedback"></i>\n' +
        '            </div>\n' +
        '        </div>\n' +
        '    </div>\n' +
        '    <hr>\n' +
        '</div>';
    $('#collectionsInfo-header').append(header_search);

    //Now add table along with headers.
    var table= $('<table></table>').attr("id","institution-table").css("margin","auto").css("width","1100px");
    $('#collectionsInfo-details').append(
        table
    );
    table = document.getElementById("institution-table");
    var row = table.createTHead().insertRow(0);
    row.insertCell(0).innerText = "";
    row.insertCell(1).innerText = "Institution name";
    row.insertCell(2).innerText = "No. of collections";


    var clickable_institution = function ( data, type, full, meta ) {
        return '<a target="_self" href="collections?browse-type=institution&institutionID=' + encodeURI(full["institution"]) + '">' + full["institution"] + '</a>';
    };
    var render_collection_count = function ( data, type, full, meta ) {
        return full["numCollections"];
    };



        var t = $('#institution-table').DataTable({
            data: institutionDetails,
            pagingType: 'simple',
            searching: false, paging: false, info: false,
            columns: [{data: 'id', defaultContent:''},
                { data: 'institution' },
                { data: 'numCollections' }
            ],
            columnDefs: [{targets: 0,searchable: false, orderable: false},
                {targets:1, searchable: false,orderable: false,render:clickable_institution},
                {targets:2, searchable: false, orderable: false,render: render_collection_count,className: "dt-right"}], // no width for col 0 here because it causes linewrap in data and size fields (attachment name can be fairly wide as well)
            //order:[[1, 'asc']], // col 1 (date), ascending
            //fnInitComplete: function() { $('#attachments').fadeIn();}
        });
        t.on( 'order.dt search.dt', function () {
            t.column(0, {search:'applied', order:'applied'}).nodes().each( function (cell, i) {
                cell.innerHTML = i+1;
                t.cell(cell).invalidate('dom');
            } );
        } ).draw();

}

/*This method renders 2 ui elements when user clicks on a particular institution to browse the collections corresponding to that institution.
The first one is "Back to browse institution page which, when click, leads to collectiosn.jsp?browsetype=institution.
The second one is a broad header presenting the information/logo about the institution.
 */
var renderInstitutionInfoHeader = function(institutionDetails){

    var logoURL="https://toppng.com/uploads/preview/stanford-university-logo-stanford-logo-11563198738mb4vawsk4c.png";
    var instName="Green Library";
    var instAddressLine1="Green Library";
    var instAddressLine2="Stanford California 94305-6004";
    var instPhone="(650) 725-1161";
    var instFax="(650) 723-8690";
    var instMail="library@stanford.edu";
    var instURL="www.library.stanford.edu";
    var instInformation= 'Materials from other libraries, including Green Library, ' +
        '    can now be picked up at the East Asia Library.  Additionally, patrons are now able to borrow materials ' +
        '    from other libraries through Interlibrary Loan. On Tuesday September 01, the Stanford Libraries Science and ' +
        '    Engineering Group hosted a one-hour information session for new graduate students in the STEM disciplines. ' +
        '    ';


    //Extract institution detail and put it in the div appropriately.
    var header_skelton= $('<div class="instinfo-container">\n' +
        '  <div class="row">\n' +
        '    <div class="col-sm-3 d-flex">\n' +
        ' <img alt="Qries" src="'+ logoURL +'"\n' +
        '     style="object-fit:scale-down;margin:auto;height:150px;padding-right:50px;">\n' +
        '     </div>\n' +
        '    <div class="col-sm-3">\n' +
        '      <span class="inst-heading"> '+ instName+'</span><br>\n' +
        '      <span class="inst-address">'+instAddressLine1+'</span><br>\n' +
        '      <span class="inst-address">'+instAddressLine2+'</span><br>\n' +
        '<b class=inst-address style="font-weight:bold;color:black;">Phone:</b> <span class="inst-address">'+instPhone+'</span><br>\n' +
        '     <b class=inst-address style="font-weight:bold;color:black;">Fax:</b>  <span class="inst-address">'+instFax+'</span><br>\n' +
        '      <b class=inst-address style="font-weight:bold;color:black;">Email:</b> <span class="inst-address">'+instMail+'</span><br>\n' +
        '      <a class="inst-address" href="www.library.stanford.edu">'+instURL+'</a><br>\n' +
        '    </div>\n' +
        '    <div class="col-sm-6  d-flex ">\n' +
        '      <span class="inst-heading">Information about the institution  <span><br>\n' +
        '  <p class="inst-address" style="text-align:justify;margin-right:10px">\n' +instInformation +
        '  </p>\n' +
        '  \n' +
        '  </div>\n' +
        '  </div>\n' +
        '</div>');


    //Add this div fragment as a child to institutionInfo
    $('#institutionInfo').empty();
    $('#institutionInfo').append(header_skelton);
    //add a new line after this.
    $('#institutionInfo').append('<br>');
    //Also add 'Back to Browse Institution arrow button' under div back-to-institution-browse

}

//Generic error handling function used in ajax calls.
var error = function(message){
    return function (jq, textStatus, errorThrown) {
        console.log(errorThrown);
        epadd.error(message+ " (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")");
    }
}

$(document).ready(function() {

	//Step 1: Do an ajax call to get the set of collections' metadata.
    //Before that fill in the data based on the input parameters of this call is to get collection details or institution details.
    //Also if it is a call to get the collection detail then if it is to bring institution specific collections or all collections.
    //This is handled by two JS variables which get set by the incoming parameters.- browseType, institutionName

    if(browseType=="collection"){
        // Case 1: "AllCollections": If browseType="collection" then get all collections. -- render as tile (using renderBrowseCollection method)
        $.ajax({
            type: 'POST', url: 'getCollectionDetails', datatype: 'json',  success: function (data, textStatus, jqxhr) {
                // fade_spinner_with_delay($spinner);
                _collectionDetails=data;
                renderBrowseCollection(data, "Browse collections",true);
            }, error : error("Error setting flags. Please try again, and if the error persists, report it to epadd_project@stanford.edu.")
        });
    }else if(browseType=="institution" && institutionName && institutionName.trim() ){
        // Case 2: "InstitutionSpecificCollectiosn": If browseType="institution" and institutionName=nonempty then get all collections for this particular institution. -- render both, institution information and the collection details
        // using renderBrowseCollectionByInstitution method)
        //First ajax call: For getting institution information
        $.ajax({
            type: 'POST', url: 'getInstitutionDetails?institutionName='+institutionName, datatype: 'json',  success: function (data, textStatus, jqxhr) {
                // fade_spinner_with_delay($spinner);
                renderInstitutionInfoHeader(data);
            }, error : error("Error setting flags. Please try again, and if the error persists, report it to epadd_project@stanford.edu.")
        });
        //Second ajax call: For getting collection information for that institution
        $.ajax({
            type: 'POST', url: 'getCollectionDetails?institutionName='+institutionName, datatype: 'json',  success: function (data, textStatus, jqxhr) {
                _collectionDetails=data;
                renderBrowseCollection(data, "Collection details",true);
            }, error : error("Error setting flags. Please try again, and if the error persists, report it to epadd_project@stanford.edu.")
        });
    }else if(browseType=="institution"){
        // Case 3: "AllInstitutions": If browseType="institution" and institutionName=null then get all institutions' details. - render as table (using renderBrowseInstitutions method)
        $.ajax({
            type: 'POST', url: 'getInstitutionDetails', datatype: 'json',  success: function (data, textStatus, jqxhr) {
                // fade_spinner_with_delay($spinner);
                renderBrowseInstitutions(data);
            }, error : error("Error setting flags. Please try again, and if the error persists, report it to epadd_project@stanford.edu.")
        });
    }



});
