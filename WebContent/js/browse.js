
/**
 * Created by hangal on 6/2/17.
 */
/* JS code for handing interactive browse page.

/* The browse page has a certain number of messages on screen. these arrays store the states of each of the messages.*/
//var transferWithRestrictions = [], doNotTransfer = [], reviewed = [],
    var addToCart = [], messageIds = [], annotations = [];
var PAGE_ON_SCREEN = -1; // current page displayed on screen
var TOTAL_PAGES = 0; // global vars

/* update message state.
   e: event, toAll is a boolean indicating whether to apply that state to all messages.
 */

function apply(e, toAll) {
    var post_data = {};

    // set the post_data based on these vars which track the state of the flags of the currently displayed message
    var dnt = $('#doNotTransfer').hasClass('flag-enabled');
    var twr = $('#transferWithRestrictions').hasClass('flag-enabled');
    var rev = $('#reviewed').hasClass('flag-enabled');
    var atc = $('#addToCart').hasClass('flag-enabled');
    var ann = $('#annotation').val();
    post_data.setDoNotTransfer = dnt ? "1" : "0";
    post_data.setTransferWithRestrictions = twr ? "1" : "0";
    post_data.setReviewed = rev ? "1" : "0";
    post_data.setAddToCart = atc ? "1" : "0";
    post_data.setAnnotation = ann;

    function check_twr() {
        epadd.log('checking twr');
        check_flags(transferWithRestrictions, PAGE_ON_SCREEN, twr, 'setTransferWithRestrictions', 'transfer-with-restrictions', check_reviewed);
    }

    function check_reviewed() {
        epadd.log('checking reviewed');
        check_flags(reviewed, PAGE_ON_SCREEN, rev, 'setReviewed', 'reviewed', check_add_to_cart);
    }

    function check_add_to_cart() {
        epadd.log('checking add to cart');
        check_flags(addToCart, PAGE_ON_SCREEN, atc, 'setAddToCart', 'add-to-cart', check_annotations);
    }

// any messages in current dataset already have annotations?

    function check_annotations() {
        var anyAnnotations = false;
        $('.page').each(function (i, o) {
            if (i == PAGE_ON_SCREEN)
                return;
            if (annotations[i]) {
                anyAnnotations = true;
                return false;
            }
        });

        if (anyAnnotations) {
            epadd.log('showing overwrite/append modal');
            // summon the modal and assign click handlers to the buttons.
            $('#info-modal .modal-body').html('Some messages already have annotations. Append to existing annotations, or overwrite them?');
            $('#info-modal').modal();
            modal_shown = true;
            $('#overwrite-button').click(function () {
                epadd.log('overwrite button clicked');
                post_updates();
            });
            $('#append-button').click(function () {
                epadd.log('append button clicked');
                post_data.append = 1;
                post_updates();
            });
            //			$('#cancel-button').click(function() { /* do nothing */});
        }
        else
            post_updates();
    }

    // prompts if any conflicting flags, and if the users says no, then deletes the given prop_name from post_data, then calls continuation()
    // if the user says yes, or there is no conflict, it calls continuation()
    // except_page is excepted when warning for conflicting messages
    // this is to avoid the case that user clicks on a flag and changes it (which immediately applies it), then clicks apply all button
    function check_flags(flags_array, except_page, new_val, prop_name, description, continuation) {
        var trueCount = 0, falseCount = 0, totalCount = 0;
        $('.page').each(function (i, o) {
            totalCount++;
            if (i == except_page)
                return; // incr. neither true nor false count
            if (flags_array[i]) {
                trueCount++;
            } else {
                falseCount++
            }
        });

        var apply_continuation = false;
        if (trueCount > 0 && falseCount > 0) {
            var mesg = 'The ' + description + ' flag is set for ' + epadd.pluralize(trueCount, 'message') + ' and unset for ' + epadd.pluralize(falseCount, 'message') + '. '
                + (new_val ? 'Set' : 'Unset') + ' this flag for all ' + epadd.pluralize(totalCount, 'message') + '?';

            epadd.log('showing confirm modal for: overwrite/append modal: ' + mesg);

            $('#info-modal1 .modal-body').html(mesg);
            // make sure to unbind handlers before adding new ones, so that the same handler doesn't get called repeatedly (danger of this happening because the info-modal1 is the same element)
            $('#no-button').unbind().click(function () {
                apply_continuation = true;
                epadd.log('cancelling application of ' + description);
                alert(prop_name + ' = ' + post_data[prop_name]);
                +delete post_data[prop_name];
            });
            ; // unbind all prev. handlers
            $('#yes-button').unbind().click(function () {
                apply_continuation = true;
                epadd.log('continuing with application of ' + description);
            });
            // call continuation only if apply_continuation is set (i.e yes or no was selected), otherwise even dismissing the modal from its close (x) sets it off.
            $('#info-modal1').unbind('hidden.bs.modal').on('hidden.bs.modal', function () {
                if (apply_continuation) {
                    continuation();
                } else {
                    epadd.log(description + ' modal dismissed without yes or no.');
                }
            });
            $('#info-modal1').modal();
        }
        else
            continuation(); // no continuation, easy.
    }

    var url = 'ajax/applyLabelsAnnotations.jsp';
    // first check if applying to all, in case we may need to check if we append/overwrite
    var modal_shown = false;
    if (toAll) {
        epadd.log('checking do not transfer');
        check_flags(doNotTransfer, PAGE_ON_SCREEN, dnt, 'setDoNotTransfer', 'do-not-transfer', check_twr);
        // the continuation sequence will end up calling post_updates
    }
    else
        post_updates();

    function post_updates() {
        post_data.archiveID = archiveID;
        epadd.log('posting updates');
        if (toAll)
            post_data.datasetId = datasetName;
        else
            post_data.docId = messageIds[PAGE_ON_SCREEN];

        // we unbind to make sure multiple copies of the same handler don't get attached to the overwrite/append buttons.
        $('#append-button').unbind();
        $('#overwrite-button').unbind();
        // we need the spinner to be visible for at least 500ms so it will be clear to the user that the button press was accepted.
        // otherwise, the op is usually so quick that the user doesn't know whether anything happened.
        var fade_spinner_with_delay = function () {
            $spinner.delay(500).fadeOut();
        };
        var $spinner = $('.spinner', $(e.target));
        epadd.log($spinner);
        $spinner.show();

        // updates state to all pages in this set in-browser
        function update_pages_in_browser() {
            epadd.log('updating pages in browser');

            if (toAll) {
                $('.page').each(function (i, o) {
                    if (post_data.setDoNotTransfer) {
                        doNotTransfer[i] = dnt;
                    }
                    if (post_data.setTransferWithRestrictions) {
                        transferWithRestrictions[i] = twr;
                    }
                    if (post_data.setReviewed) {
                        reviewed[i] = rev;
                    }
                    if (post_data.setAddToCart) {
                        addToCart[i] = atc;
                    }
                    if (ann) {
                        annotations[i] = (post_data.append) ? ((annotations[i] ? annotations[i] : "") + " " + ann) : ann;
                    }
                });
                // also update the annotation on screen to the updated value of the current page
                if (annotations[PAGE_ON_SCREEN] != null && annotations[PAGE_ON_SCREEN].length > 0)
                    $('#annotation').val(annotations[PAGE_ON_SCREEN]);
            }
            else {
                doNotTransfer[PAGE_ON_SCREEN] = dnt;
                transferWithRestrictions[PAGE_ON_SCREEN] = twr;
                reviewed[PAGE_ON_SCREEN] = rev;
                addToCart[PAGE_ON_SCREEN] = atc;
                annotations[PAGE_ON_SCREEN] = ann;
            }
        }

        // now post updates to server
        epadd.log('hitting url ' + url);
        $.ajax({
            type: 'POST',
            url: url,
            datatype: 'json',
            data: post_data,
            success: function (data, textStatus, jqxhr) {
                fade_spinner_with_delay();
                update_pages_in_browser(); // update pages in browser only if server update successful, because we don't want browser and server out of sync
                epadd.log("Completed flags updated with status " + textStatus);
            },
            error: function (jq, textStatus, errorThrown) {
                fade_spinner_with_delay();
                $spinner.delay(500).fadeOut();
                var message = ("Error setting flags. Please try again, and if the error persists, report it to epadd_project@stanford.edu. (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")");
                epadd.log(message);
                epadd.alert(message);
            }
        });
        // these ajax reqs are completing normally, but report an error after completion... not sure why.
        return false;
    }
}

/** renders labels on screen for the current message (but does not change any state in the backend) */
function refresh_labels_on_screen(labelIds) {
    if (!labelIds)
        return;

    // we have to set up the select picker so it will show the right things in the dropdown
    $('.label-selectpicker').selectpicker('deselectAll');
    $('.label-selectpicker').selectpicker ('val', labelIds); // e.g. setting val, [0, 1, 3] will set the selectpicker state to these 3

    // refresh the labels area
    $('.labels-area').html(''); // wipe out existing labels
    for (var i = 0; i < labelIds.length; i++) {
        var label_id = labelIds[i];
        var label = allLabels[label_id];
        if (!label)
            continue;

        var class_for_type; // this is one of system/general/restriction label
        {
            if (label.labType === 'RESTR_LAB' && label.isSysLabel)
                class_for_type = 'system-label';
            else if (label.labType === 'RESTR_LAB')
                class_for_type = 'restriction-label';
            else if (label.labType === 'GEN_LAB')
                class_for_type = 'general-label';
        }

        $('.labels-area').append(
            '<div '
            + ' data-label-id="' + label.labId + '" '
            + ' title="' + label.description + '" '
            + ' class="message-label ' + class_for_type + '" >'
            + label.labName
            + '</div>');
    }
}

/** labelIds is an array of label ids (e.g. [1,2,5]) which are to be applied to the current message */
function apply_labels(labelIds) {

    // post to the backend, and when successful, refresh the labels on screen
    $.ajax({
        url:'ajax/applyLabelsAnnotations.jsp',
        type: 'POST',
        data: {archiveID: archiveID,docId: $($pages[0]).attr('docId'), labelIDs: labelIds.join(),action:"override"}, // labels will go as CSVs: "0,1,2" or "id1,id2,id3"
        dataType: 'json',
        success: function() { refresh_labels_on_screen (labelIds); },
        error: function() {  refresh_labels_on_screen (labelIds);
        /* FLAG DEBUG epadd.alert('There was an error in saving labels, sorry!'); */
        }
    });
}

// currently called before the new page has been rendered
var page_change_callback = function(oldPage, currentPage) {

    var $annotation = $('#annotation');

    if (annotations[currentPage] != null && annotations[currentPage].length > 0)
    {
        $annotation.val(annotations[currentPage]);
        $('#annotation_div').show();
    }
    else
    {
        $annotation.val('');
        $annotation.attr('placeholder', 'Add an annotation');
    }
    PAGE_ON_SCREEN = currentPage;
    $('#pageNumbering').html(((TOTAL_PAGES == 0) ? 0 : currentPage+1) + '/' + TOTAL_PAGES);
    var labelIds = labelsOnPage[currentPage];
    if (labelIds)
        refresh_labels_on_screen(labelIds);
};

var labelsOnPage = [];

$('body').ready(function() {

    $pages = $('.page');
    PAGE_ON_SCREEN = 0;
    TOTAL_PAGES = $pages.length;
    for (var i = 0; i < TOTAL_PAGES; i++)
    {
        annotations[i] = $pages[i].getAttribute('comment');

        // doNotTransfer[i] = ($pages[i].getAttribute('doNotTransfer') != null);
        // transferWithRestrictions[i] = ($pages[i].getAttribute('transferWithRestrictions') != null);
        // reviewed[i] = ($pages[i].getAttribute('reviewed') != null);
        //get system labels and put them in sys labels array
      //  syslabels[i] = ($pages[i].getAttribute('syslabels'));
        //get restriction labels and put them in restr labels array
       // restrlabels[i] = ($pages[i].getAttribute('restrlabels'));
        //get general labels and put them in genlabels array
       // genlabels[i] = ($pages[i].getAttribute('genlabels'));
        labelsOnPage[i] = $pages[i].getAttribute('labels').split(",");

//        addToCart[i] = ($pages[i].getAttribute('addToCart') != null);
        messageIds[i] = $pages[i].getAttribute('docID');
    }
    currentPage = 0;

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //Big gotcha here: Be very careful what method is passed as logger into this jog method.
    //if for some reason, the logger fails or actually does a post operation; this thing pushes
    // it to retry making the whole thing (the entire browser and the system) to slow down
    //TODO: JOG plugin should not be this aggressive with the logger
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    var x = $(document).jog({
//        paging_info: {url: 'ajax/jogPageIn.jsp?datasetId=' + datasetName + '&debug=<%=request.getParameter("debug")%>', window_size_back: 30, window_size_fwd: 50}, // disabling request.getParameter("debug") after we moved this js from browse.jsp to browse.js
        paging_info: {url: 'ajax/jogPageIn.jsp?archiveID='+archiveID+'&datasetId=' + datasetName, window_size_back: 30, window_size_fwd: 50},
        page_change_callback: page_change_callback,
        logger: epadd.log,
        width: 180,
        disabled: 'true',
        dynamic: false
    });

    $('#page_forward').click(x.forward_func);
    $('#page_back').click(x.back_func);
    // toggle each flag upon click
    $('.flag').click (function(e) {
        var $target = $(e.target);
        $target.toggleClass('flag-enabled');
        apply(e, false);
    });

    $('#apply').click(function(e) { apply(e, false);});
    $('#applyToAll').click(function(e) { apply(e, true);});
    $('#annotation_div').focusout(function (e) { apply(e, false); });

    $('.label-selectpicker').on('change', function(){
        var labelIds = $('.label-selectpicker').selectpicker('val');
        if(labelIds) {
            labelsOnPage[currentPage]=labelIds;
            apply_labels(labelIds);
        }
    });
});

// on page unload, release dataset to free memory
$(window).unload(function() {
    epadd.log ('releasing dataset ' + datasetName);
    $.get('ajax/releaseDataset.jsp?datasetId=' + datasetName);
});

//syslabels[1].split(",").map((x)=>labelMap[x].labName)
