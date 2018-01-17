
// global vars, used by every module below

var docIDs = [];
var PAGE_ON_SCREEN = -1; // current page displayed on screen
var TOTAL_PAGES = 0;
var $pages;

// interacts with #page_forward, #page_back, and #pageNumbering on screen
var Navigation = function(){

    // currently called before the new page has been rendered, private method
    var page_change_callback = function(oldPage, currentPage) {

        PAGE_ON_SCREEN = currentPage;
        $('#pageNumbering').html(((TOTAL_PAGES === 0) ? 0 : currentPage+1) + '/' + TOTAL_PAGES);
        Labels.refreshLabels();
        Annotations.refreshAnnotation();
    };

    var setupEvents = function() {
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //Big gotcha here: Be very careful what method is passed as logger into this jog method.
        //if for some reason, the logger fails or actually does a post operation; this thing pushes
        // it to retry making the whole thing (the entire browser and the system) to slow down
        //TODO: JOG plugin should not be this aggressive with the logger
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        var x = $(document).jog({
            paging_info: {
                url: 'ajax/jogPageIn.jsp?archiveID=' + archiveID + '&datasetId=' + docsetID,
                window_size_back: 30,
                window_size_fwd: 50
            },
            page_change_callback: page_change_callback,
            logger: epadd.log,
            width: 180,
            disabled: 'true',
            dynamic: false
        });

        // forward/back nav
        $('#page_forward').click(x.forward_func);
        $('#page_back').click(x.back_func);
    };

    return { setupEvents: setupEvents};
}();


// interacts with .label-selectpicker and .labels-area on screen
var Labels = function() {
    var labelsOnPage = []; // private to this module

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

            var class_for_label; // this is one of system/general/restriction label
            {
                if (label.labType === 'RESTRICTION')
                    class_for_label = 'restriction-label';
                else if (label.labType === 'GENERAL')
                    class_for_label = 'general-label';

                if (label.isSysLabel)
                    class_for_label += ' system-label';
            }

            // restriction + system labels will have both system-label and restr. label applied and will be colored red
            // non-system restriction labels will be colored orange
            // general labels will be colored blue
            $('.labels-area').append(
                '<div '
                + ' data-label-id="' + label.labId + '" '
                + ' title="' + escapeHTML(label.description) + '" '
                + ' class="message-label ' + class_for_label + '" >'
                + escapeHTML(label.labName)
                + '</div>');
        }
    }

    /** private method labelIds is an array of label ids (e.g. [1,2,5]) which are to be applied to the current message */
    function apply_labels(labelIds) {

        // post to the backend, and when successful, refresh the labels on screen
        $.ajax({
            url: 'ajax/applyLabelsAnnotations.jsp',
            type: 'POST',
            data: {archiveID: archiveID, docId: docIDs[PAGE_ON_SCREEN], labelIDs: labelIds.join(), action: "override"}, // labels will go as CSVs: "0,1,2" or "id1,id2,id3"
            dataType: 'json',
            success: function () {
                refresh_labels_on_screen(labelIds);
            },
            error: function () {
                epadd.alert('There was an error in saving the annotations! Please try again.');
            }
        });
    }

    var refreshLabels = function () {
        var labelIds = labelsOnPage[PAGE_ON_SCREEN];
        if (labelIds)
            refresh_labels_on_screen(labelIds);
    };

    var setup = function () {
        for (var i = 0; i < TOTAL_PAGES; i++) {
            labelsOnPage[i] = $pages[i].getAttribute('labels').split(",");
        }
        // set up label handling
        $('.label-selectpicker').on('change', function () {
            var labelIds = $('.label-selectpicker').selectpicker('val');
            if (labelIds) {
                labelsOnPage[PAGE_ON_SCREEN] = labelIds;
                apply_labels(labelIds);
            }
        });
    };

    return {
        setup: setup,
        refreshLabels: refreshLabels
    };
}();

// interacts with #annotation-modal and .annotation-area on screen
// posts to applyLabelsAnnotations.jsp on updates
var Annotations = function() {

    var annotations = [];

    // set up event handlers for annotations, should be called only once
    function setup() {

        for (var i = 0; i < TOTAL_PAGES; i++) {
            annotations[i] = $pages[i].getAttribute('comment');
        }

        // things to do when annotation modal is shown
        function annotation_modal_shown() {
            $('#annotation-modal .modal-body').text(annotations[PAGE_ON_SCREEN]).focus();
        }

        // things to do when annotation modal is dismissed
        function annotation_modal_dismissed () {
            var annotation = $('#annotation-modal .modal-body').val(); // .val() gets the value of a text area. assume: no html in annotations
            annotations[PAGE_ON_SCREEN] = annotation;
            // post to the backend, and when successful, refresh the labels on screen
            $.ajax({
                url: 'ajax/applyLabelsAnnotations.jsp',
                type: 'POST',
                data: {
                    archiveID: archiveID,
                    docId: docIDs[PAGE_ON_SCREEN],
                    annotation: annotation,
                    action: "override"
                }, // labels will go as CSVs: "0,1,2" or "id1,id2,id3"
                dataType: 'json',
                success: function (response) {
                    $('.annotation-area').text(annotation);
                },
                error: function () { epadd.alert('There was an error in saving the annotation! Please try again.');}
            });
        }

        // set up handlers for when annotation modal is shown/dismissed
        $('#annotation-modal').on('shown.bs.modal', annotation_modal_shown).on('hidden.bs.modal', annotation_modal_dismissed);

        // when annotation is clicked, invoke modal
        $('.annotation-area').click(function () {
            // show the modal
            $('#annotation-modal').modal();
        });
    }

    // copies annotation from .annotation-area on screen to the current page's
    function refreshAnnotation() {
        var $annotation = $('.annotation-area');
        $annotation.show();
        if (annotations[PAGE_ON_SCREEN] && annotations[PAGE_ON_SCREEN].length > 0)
            $annotation.text(annotations[PAGE_ON_SCREEN]);
        else
            $annotation.text('No annotation');
    }

    return {
        setup: setup,
        refreshAnnotation: refreshAnnotation
    };
}();

$(document).ready(function() {

    // global vars setup
    {
        $pages = $('.page'); // all page frames for the entire dataset are pre-loaded in the page (although the HTML inside them is paged in lazily)
        PAGE_ON_SCREEN = 0;
        TOTAL_PAGES = $pages.length;
        for (var i = 0; i < TOTAL_PAGES; i++) {
            docIDs[i] = $pages[i].getAttribute('docID');
        }
    }

    Labels.setup();
    Annotations.setup();
    Navigation.setupEvents(); // important -- this has to be after labels and annotations setup to render the first page correctly

    // on page unload, release dataset to free memory
    $(window).unload(function () {
        epadd.log('releasing dataset ' + docsetID);
        $.get('ajax/releaseDataset.jsp?docsetID=' + docsetID);
    });
});
