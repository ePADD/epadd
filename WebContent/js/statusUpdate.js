// "use strict";
var currentOp;
// ideally currentOp should be hidden, right now it can't be, because cancelOp needs it. so only one status div per page is possible.
var MIN_POLL_MILLIS = 2000, MAX_POLL_MILLIS = 10000;
var TEASER_REFRESH_MILLIS = 500;
var CANCEL_SELECTOR = '#cancel'; // ideally should be passed in as a param, currently fixed. It means nothing else on the page that includes div_status can have a #cancel element

// page: URL to hit that kicks off a long-running operation. We'll ajax fetch this page, and expect a json response with fields {error: String, cancelled: boolean, resultPage: String}.
// status_page: while page is running, we'll poll this URL, expecting current status as json
// status_div to show/hide
// status_div_text = place to update status (part of status_div)
// onready (optional) = function called over event: onready statechange
// if redirect_page is specified, on success, it redirects to that page, instead of whatever the page's response is
// need to clean up this onready stuff
function fetch_page_with_progress(page, spage, sdiv, sdiv_text, post_params, onready, redirect_page, premisData)
{
	// little helper class to hold details
	function Operation() {
        //add operation ID
        this.id = Math.round(Math.random()*10000000+1);
        //add to post_params with name as opID
		//post_params.opID = this.id;
		this.done = false; // done will be set when op is complete or cancelled
		this.cancelled = false;
		this.status_div = null;
		this.status_text = null;
		this.status_page = null;
		this.currentTeaserId = null;

	}

	function onSuccess(response,onready,redirect_page){
	    if (premisData) {
            if (response.cancelled)
            {
                premisData.outcome = "Cancelled by user";
            }
            else
            {
                premisData.outcome = "success";

            }
                $.ajax({
                    "type": "POST",
                    "url": "ajax/premis.jsp",
                    "data": {archiveID: archiveID, premisdata: JSON.stringify(premisData)},
                });
        }
	    console.log("ON SUCCESS");
		if(response.status===1){
            $(currentOp.status_div).hide();
            $('.muse-overlay').hide();
            currentOp.done = true;
            currentOp.status_div_text.innerHTML = "<br/>&nbsp;<br/>"; // wipe out the status so we dont see residue for the next op

            epadd.error(response.error);
            return;
        }
            currentOp.done = true;
            document.title = currentOp.orig_doc_title;

            $(currentOp.status_div).hide();
            $('.muse-overlay').hide();

            currentOp.status_div_text.innerHTML = "<br/>&nbsp;<br/>"; // wipe out the status so we dont see residue for the next op

            if (response.error) {
                epadd.log("Error in getting status:" + responseText);
                window.location = "error";
                return;
            }

            // in case op is cancelled, we should get back cancelled = true
            if (response.cancelled) {
                $('.muse-overlay').hide();
                $(currentOp.status_div).hide()
                return;
            }

            // ok, so the op was successful, have we been told what ready function to call or page to to redirect on the client side by the caller?
            if (onready) {
                onready(response);
                return;
            }

            if (redirect_page) {
                window.location = redirect_page;
                return;
            }

            // we haven't been told where to redirect by the client, so go by what the page says in its json
            resultPage = response.resultPage;

            if (resultPage != null) {
                window.location = resultPage; // redirect to result page
            }
        }

    function kick_off_page_fetch(page, post_params, onready, redirect_page)
    {
		//start ajax call to page.
		//on completion of this call, invoke method onSuccess defined above.
		//on error log the error for more debugging information.
        poll_status(onready,redirect_page);

        if (post_params == undefined)
            req_type='GET';
        else
            req_type='POST';

        post_params= post_params+'&opID='+currentOp.id;
		return $.ajax({type: req_type,
            dataType: 'json',
            url: page,
            data: post_params,
            cache: false,
            success: function (response) {
                    	epadd.log("Execution started successfully!!!");
                },
            error: function(xhr,status,error) {
                epadd.error ("Sorry, there was an error while importing the accession. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report it to epadd_project@stanford.edu.");
            }});

    }

	function 	poll_status(onready, redirect_page)
	{
		//also send the opID with each status request so that the backend returns the appropriate status.
		var params={opID:currentOp.id}

		var onStatusReceive = function(response){
            if (response.sp_not_ready) { // this means status provider is not ready
                // hide cancel button
                $('#cancel').hide();
            } else {
                // show cancel button
                $('#cancel').show();
            }
            if (response.error)
            {
                html = 'Error! ' + response.error;
                epadd.log (html);
            }
            else
            {
                shown = true;
                html = response.message ? response.message : ''; // prevent displaying undefined in case j.message is not defined.
                //	epadd.log ('status = ' + muse.dump_obj(j));
                try { document.title = html; } catch (e) { epadd.log ('exception trying to set title to ' + html); } // set title so user can switch tabs and still keep an eye on the status of this one
                var $progress = $('.progress_bar');
                var total_width = $('.progress_bar_outer_box').width();
                epadd.log("pct: "+response.pctComplete);
                if (response.pctComplete && response.pctComplete >= 0)
                {

                    var newWidth = parseInt(total_width*response.pctComplete/100);
                    // animate it over a second, because we are only polling every cpl of seconds...
                    // if w happens to be going backwards, don't animate.
                    // sometimes this happens when we've moved to the next operation
                    var going_backwards = false;
                    var w = '<>';
                    try {
                        w = $progress.width(); // e.g. w = "324px"
                        going_backwards = w > newWidth;
                        if (going_backwards)
                            epadd.log ('progress bar going backwards to ' + newWidth + ' from ' + w);
                    } catch (e) { epadd.log('exception trying to get progress bar width: ' + e + ' w=' + w); }

                    // epadd.log ('prog width = ' + $progress.css('width'));
                    if (going_backwards)
                    {
                        $progress.css('width', newWidth);
                    }
                    else
                        $progress.animate({'width': newWidth}, {duration: MIN_POLL_MILLIS/2});
                }
                else
                {
                    $progress.css('width', 4); // a small width
                }

                // limit needless polling when we're far away from completion...
                // poll after 1% of time left, subject to MIN/MAX Poll millis
                if ((response.secsElapsed && (response.secsElapsed < 0 || response.secsElapsed > 10))
                    && (response.secsRemaining && response.secsRemaining >= 0))
                {
                    $('.time_remaining').html(muse.approximateTimeLeft(response.secsRemaining));
                    POLL_MILLIS = (response.secsRemaining*1000)/100; // poll after 1% of time left
                    if (POLL_MILLIS < MIN_POLL_MILLIS)
                        POLL_MILLIS = MIN_POLL_MILLIS;
                    if (POLL_MILLIS > MAX_POLL_MILLIS)
                        POLL_MILLIS = MAX_POLL_MILLIS;
                    $('.time_remaining').fadeIn('slow');
                }
                else
                {
                    $('.time_remaining').fadeOut('slow');
                    $('.time_remaining').html('');
                }

                if (currentOp.teaserTimeoutId)
                    clearTimeout(currentOp.teaserTimeoutId);
                var fn = getTeaserFn(response.teasers, 0);
                fn();
            }
            currentOp.status_div_text.innerHTML = html;
		}

        return $.ajax({type: 'GET',
            dataType: 'json',
            url: currentOp.status_page,
            data: params,
            cache: false,
            success: function (onready,redirect_page) {
                return function(response){
                    if (response) {
                            var POLL_MILLIS = MIN_POLL_MILLIS; // default
                            //check if it status update message or operation completed message.
							//if status update message then update the status (by calling  and fire this method (poll_status) again after some fixed time.
							if(response.resType === "done") {
                                //if operation completed message then hide the progress bar and call onSuccess method defined above.
                                currentOp.status_div_text.innerHTML = "";
                                onSuccess(response, onready, redirect_page);
                            }else {
                                onStatusReceive(response);
                                setTimeout(function(){
                                    poll_status(onready,redirect_page)}, POLL_MILLIS
                                );
                            }
                    }
                    else
                        epadd.error('Sorry, there was an error while querying for the status of an operation. Improper response received from the ePADD server.');
                }}(onready,redirect_page),
            error: function(xhr,status,error) {
                var err = ("There was an error on the server. status: " + status + " error: " + error);
                if (xhr.responseText && xhr.responseText != '')
                {
                    err += (" Response : " + xhr.responseText);
                }

                console.log(err);
                epadd.error (err);
                $('.muse-overlay').hide();
                //window.location = "error.jsp";
            }
        });
	}


	
	function getTeaserFn(teasers, index)
	{
		if (typeof teasers === 'undefined' || teasers === null)
			return function() { $('.teaser').text(''); };
		//	teasers = ['abc', 'def', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10'];
		return function() {
			if (index >= teasers.length) {
				// don't wrap around, just stop
				$('.teaser').text('');
				return;
			}
			{
				$('.teaser').show();
				var charBudget = 60 - currentOp.status_div_text.innerHTML.length;
				if (teasers[index] != 'null') {
					$('.teaser').text(muse.ellipsize(teasers[index], charBudget));
				}
				// epadd.log ('setting teaser timeout');
				currentOp.teaserTimeoutId = setTimeout(getTeaserFn(teasers, index+1), TEASER_REFRESH_MILLIS);
			}
		};
	}

    /*
	$(window).resize(function() {
		$('.muse-overlay').height($(document).height());
		$('.muse-overlay').width($(document).width());
		sdiv.style.left = (window.innerWidth/2)- 320+"px"; // the total width is 640px
		sdiv.style.top = (window.innerHeight/2) - 65+"px";
	});
    */
	function cancelCurrentOp()
	{
        $(CANCEL_SELECTOR).unbind();
		epadd.log ('cancelling current op ' + currentOp);
		if (currentOp.teaserTimeoutId)
			clearTimeout (currentOp.teaserTimeoutId);

		// tell the server to abort, but we don't really wait for it to confirm... this does run the risk of cancel's getting lost...
        var params={opID:currentOp.id}
        return $.ajax({type: 'GET',
            dataType: 'json',
            url: 'ajax/cancel.jsp',
            data: params,
            cache: false,
            success: function(response){
				if(response.status===1){
					epadd.warn(response.errorMessage);
				}else{
					epadd.log("Initiated operation cancelling request to the backend!!")
				}
			},
            error: function(xhr,status,error) {
                epadd.error ("Sorry, there was an error while cancelling this operation. Report it to epadd_project@stanford.edu.");
            }});
	}

	// check if an op is already running (in this tab)... in no case should we fire 2 ops from the same page
	// (actually a better way is to check if a status object is in the session -- we shouldn't fire 2 ops from different tab either)
	if (currentOp && !currentOp.done) {
		return;
	}

	$(CANCEL_SELECTOR).click(cancelCurrentOp);
	currentOp = new Operation();
	currentOp.status_page = spage;//"ajax/getStatus.jsp";//spage;
	currentOp.status_div = sdiv;
	currentOp.status_div_text = sdiv_text;
	currentOp.orig_doc_title = document.title;

	sdiv_text.innerHTML = 'Starting up...';
	// display status box in the middle of the window
	$(currentOp.status_div).show();

	// insert overlay only if it doesn't already exist
	$('.muse-overlay').height($(document).height());
	$('.muse-overlay').width($(document).width());
	$('.muse-overlay').show();
	sdiv.style.left = (window.innerWidth/2)- 320+"px"; // the total width is 640px
	sdiv.style.top = (window.innerHeight/2) - 65+"px";

	return kick_off_page_fetch(page, post_params, onready, redirect_page);
	// end of this function. the rest are private helper functions


}

