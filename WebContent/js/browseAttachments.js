// @Chinmay, this seems to have a lot of repeated code with browseMessages.js, can we simplify/factor out?

// global vars, used by every module below

var docIDs = []; //Not used now for attachment page. this is required for posting the docid of the message to which the label/annotation change should be applied.
var PAGE_ON_SCREEN = -1; // current page displayed on screen
var TOTAL_PAGES = 0;
var START_YEAR=0;
// interacts with #page_forward, #page_back, and #pageNumbering on screen
var Navigation = function(){

    var jog;
    // currently called before the new page has been rendered, private method
    var page_change_callback = function(oldPage, currentPage) {
        // todo : add loadAttachmentTiles and loadAttacmentList here
        PAGE_ON_SCREEN = currentPage;
        $('#pageNumbering').html(((TOTAL_PAGES === 0) ? 0 : START_YEAR+currentPage));
    };

    var setupEvents = function() {
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //Big gotcha here: Be very careful what method is passed as logger into this jog method.
        //if for some reason, the logger fails or actually does a post operation; this thing pushes
        // it to retry making the whole thing (the entire browser and the system) to slow down
        //TODO: JOG plugin should not be this aggressive with the logger
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        jog = $(document).jog({
            paging_info: {
                url: 'ajax/jogPageInAttachments.jsp?archiveID=' + archiveID + '&datasetId=' + docsetID,
                window_size_back: 1,
                window_size_fwd: 1
            },
            page_change_callback: page_change_callback,
            page_load_callback: attachment_page_loaded_callback,
            logger: epadd.log,
            width: 180,
            /* enable this to enable jog dial
            disabled: false,
            dynamic: true
            */
            disabled: true,
            dynamic: false
        });

        // forward/back nav
        $('#page_forward').click(jog.forward);
        $('#page_back').click(jog.backward);
    };

    // annoying, have to declare these connector funcs, because jog is defined only in setupEvents, so can't directly use those funcs in the Navigation's interface.
    function disableCursorKeys() { jog.disableCursorKeys();}
    function enableCursorKeys() { jog.enableCursorKeys();}

    return {
        setupEvents: setupEvents,
        disableCursorKeys: disableCursorKeys,
        enableCursorKeys: enableCursorKeys
    };
}();


var attachment_page_loaded_callback = function(){
    loadAttachmentTiles(); //invoke method to setup tiles with attachmentDetails data.
    loadAttachmentList(); //invoke method to setup datatable with attachmentDetails data.
    if(isListView){
        showListView();
    } else{
       showTileView();
    }

}
function showTileView(){
    //hide list view and show tile view.
    $('#attachment-list').hide();
    $('#attachment-tiles').show();
    isListView=false;
    setSelected('#tileviewimg');
    unsetSelected('#listviewimg');
}

function showListView(){
    //hide tile view and show list view.
    $('#attachment-tiles').hide();
    $('#attachment-list').show();
    isListView=true;
    setSelected('#listviewimg');
    unsetSelected('#tileviewimg');
}

var setSelected = function(id){
    $(id).css("filter","invert(42%) sepia(93%) saturate(1352%) hue-rotate(190deg) brightness(80%) contrast(200%)")
}
var unsetSelected = function(id){
    $(id).css("filter","")
}

// interacts with #annotation-modal and .annotation-area on screen
// posts to applyLabelsAnnotations.jsp on updates
$(document).ready(function() {

    // allow facets panel to run the full height of the screen on the left
    $('div.facets').css('min-height', window.innerHeight - 50);

    PAGE_ON_SCREEN = 0;
    Navigation.setupEvents(); // important -- this has to be after labels and annotations setup to render the first page correctly

    // on page unload, release dataset to free memory
    $(window).unload(function () {
        epadd.log('releasing dataset ' + docsetID);
        $.get('ajax/releaseDataset.jsp?docsetID=' + docsetID);
    });
});
