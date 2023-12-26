var idown;  // Keep it outside of the function, so it's initialized once.
var mbox_download_once = true;
var attachment_download_once = true;
var csv_donwnload_once = true;

var mbox_download_url = "";
var attachment_download_url = "";
var csv_download_url = "";

// Is it possible to download File Using Javascript/jQuery?
// https://stopbyte.com/t/is-it-possible-to-download-file-using-javascript-jquery/59/3
var downloadURL = function(url) {
  if (idown) {
    idown.attr('src',url);
  } else {
    idown = $('<iframe>', { id:'idown', src:url }).hide().appendTo('body');
  }
};

var downloadMessage = function() {

    // set up event handlers for download message, should be called only once
    function setup() {

        // when download mbox file link is clicked
        $('#a-download-mbox-file-link').click(function() {
           // post to the backend, and when successful, download mbox file content to client

           if (mbox_download_once) {
                $('#spinner-div').fadeIn();

                $.ajax({
                            url : 'ajax/downloadMboxFile.jsp',
                            type : 'POST',
                            data : {
                                archiveID : archiveID,
                                docsetID : docsetID,
                                noattach : noattach,
                                stripQuoted : stripQuoted,
                                headerOnly : headerOnly,
                                messageType : messageType
                            },
                            dataType : 'json',
                            success : function(response) {
                                if (response && response.status==0 ) {
                                    mbox_download_once = false;
                                    $('#spinner-div').fadeOut();

                                    mbox_download_url = response.url;
                                    $('#a-download-mbox-file-link').attr("href", mbox_download_url);
                                    downloadURL(mbox_download_url);  // Download immediately
                                }

                            },
                            error : function() {
                                epadd
                                        .error('There was an error downloading Mbox file. Please try again, and if the error persists, report it to epadd_project@stanford.edu.');
                            }
                        });
            } else
                downloadURL(mbox_download_url);  // Download immediately

            return false;
        });

        // when download attachments in a zip file link is clicked
        $('#a-download-attachment-zip-link').click(function() {
                   // post to the backend, and when successful, download attachments zip file content to client

                   if (attachment_download_once) {
                        $('#spinner-div').fadeIn();

                        $.ajax({
                                    url : 'ajax/downloadAttachmentZip.jsp',
                                    type : 'POST',
                                    data : {
                                        archiveID : archiveID,
                                        docsetID : docsetID,
                                    },
                                    dataType : 'json',
                                    success : function(response) {
                                        if (response && response.status==0 ) {
                                            attachment_download_once = false;
                                            $('#spinner-div').fadeOut();

                                            attachment_download_url = response.url;
                                            $('#a-download-attachment-zip-link').attr("href", attachment_download_url);
                                            downloadURL(attachment_download_url);  // Download immediately
                                        }

                                        else {
                                            if (response)
                                                console.log('Ajax success but response.status <>0 | Debug: response.status=' +response.status + " | response.url=" + response.url );
                                            else
                                                console.log('Ajax success but response is null!' );
                                        }
                                    },
                                    error : function() {
                                        epadd
                                                .error('There was an error downloading attachment zip file. Please try again, and if the error persists, report it to epadd_project@stanford.edu.');
                                    }
                                });
                    } else
                        downloadURL(attachment_download_url);  // Download immediately

                    return false;
        });

       $('#a-download-search-result-csv-link').click(function() {
                   // post to the backend, and when successful, download attachments zip file content to client

                   if (csv_donwnload_once) {
                        $('#spinner-div').fadeIn();

                        $.ajax({
                                    url : 'ajax/downloadSearchResultCSV.jsp',
                                    type : 'POST',
                                    data : {
                                        archiveID : archiveID,
                                        docsetID : docsetID,
                                        stripQuoted : stripQuoted
                                    },
                                    dataType : 'json',
                                    success : function(response) {
                                        if (response && response.status==0 ) {
                                            csv_donwnload_once = false;
                                            $('#spinner-div').fadeOut();

                                            csv_download_url = response.url;
                                            $('#a-download-search-result-csv-link').attr("href", csv_download_url);
                                            downloadURL(csv_download_url);  // Download immediately
                                        }

                                        else {
                                            if (response)
                                                console.log('Ajax success but response.status <>0 | Debug: response.status=' +response.status + " | response.url=" + response.url );
                                            else
                                                console.log('Ajax success but response is null!' );
                                        }
                                    },
                                    error : function() {
                                        epadd
                                                .error('There was an error downloading attachment zip file. Please try again, and if the error persists, report it to epadd_project@stanford.edu.');
                                    }
                                });
                    } else
                        downloadURL(csv_download_url);  // Download immediately

                    return false;
        });

    } // end Setup()

    return {
        setup : setup,
    // debug only
    };

}();


$(document).ready(function() {
    downloadMessage.setup();
});