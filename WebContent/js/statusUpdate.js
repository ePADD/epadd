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
function fetch_page_with_progress(page, spage, sdiv, sdiv_text, post_params, onready, redirect_page)
{
	// little helper class to hold details
	function Operation() {
		this.id = -1;
		this.done = false; // done will be set when op is complete or cancelled
		this.cancelled = false;
		this.page_xhr = new_xhr();
		this.status_xhr = new_xhr();
		this.status_div = null;
		this.status_text = null;
		this.status_page = null;
		this.currentTeaserId = null;

	}
	
	//function to fetch page, polling spage to receive status.
	//sdiv is shown when status messages are active
	//status messages are printed to the sdiv_text
	//when the page is ready, the server should send a <resultPage>link</resultPage> result
	//and the script will redirect to that page.

	// need to clean up this onready stuff
	// URL page is invoked with post_params
	// if it returns successfully, has no error and is not cancelled, we'll set window.location to redirect_page if defined, else to <response>.resultPage
	//IMP: Made changes in this function so that the result of this ajax call can be chained with other call. Especially in header.jspf when close is clicked then the save is performed followed by unloading the archive and then setting the current page as collection-detail page.
	function kick_off_page_fetch(page, post_params, onready, redirect_page)
	{
	var returnresult = function(resolve, reject){
			var page_xhr = currentOp.page_xhr;
		var status_div = currentOp.status_div;
		if (post_params == undefined)
			page_xhr.open('GET', page, true);
		else {
			page_xhr.open('POST', page, true);
		}
        poll_status();

		//	page_xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
		page_xhr.onreadystatechange = function () {
			if (this.readyState != 4)
				return false;
			if(this.status == 0)//to avoid the issue when the op was considered as completed but nothing returned in json because the system went to sleep and the network got disconnected.
				return false;
			currentOp.done = true;
			document.title = currentOp.orig_doc_title;

			// we'll aggressively update currentOp.status_div_text.innerHTML as late as possible, but before each return
			// so we don't see residue for the next op

			if (currentOp.cancelled) {
				// if op was cancelled, return quietly without changing the page
				$('.muse-overlay').hide();
				return resolve(page_xhr.response);
			}
			// make sure we set the status_div to hide
			// otherwise the window.onbeforeunload
			// will prompt the user for confirmation
			$(currentOp.status_div).hide();
			$('.muse-overlay').hide();

			currentOp.status_div_text.innerHTML = "<br/>&nbsp;<br/>"; // wipe out the status so we dont see residue for the next op

			// epadd.log ('op completed, status = ' + this.status);

			// check for errors first
			if (this.status != 200 && this.status != 0) {
				// sgh: not sure why status = 0 is checked above.
				// i think this is historical because earlier 0 might be returned for success. but i think it is not necessary now
				window.location = "error";
				epadd.log("Error: status " + this.status + " received from page: " + page);
				return reject(page_xhr.response);
			}

			// response is in json => key fields are status, error, cancelled, resultPage
			var responseText = this.responseText;
			responseText = muse.trim(responseText);

			var j;
			try {
				eval("j = " + responseText + ";");
			}
			catch (e) {
				epadd.log('error with status json response from page ' + page + ': ' + responseText);
			}

			epadd.log("Response text: " + responseText);
			epadd.log("j:" + j);
			if (!j) {
				epadd.error("Sorry, there was an error: (no JSON was returned). Please retry and if the error persists, report the problem to us. HTTP status: " + this.status);
				return;
			}

			// check if any errors
			if (j.error) {
				epadd.log("Error in getting status:" + responseText);
				window.location = "error";
				return;
			}

			// in case op is cancelled, we should get back cancelled = true
			if (j.cancelled)
				return;

			// ok, so the op was successful, have we been told what ready function to call or page to to redirect on the client side by the caller?
			if (onready) {
				onready(j);
				return;
			}

			if (redirect_page) {
				window.location = redirect_page;
				return;
			}

			// we haven't been told where to redirect by the client, so go by what the page says in its json
			resultPage = j.resultPage;

			if (resultPage != null) {
				window.location = resultPage; // redirect to result page
			}
			return resolve(page_xhr.response);

		};

		if (post_params == undefined)
			page_xhr.send(null);
		else {
			page_xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
			// browsers firebug complains that these params are unsafe...
			//page_xhr.setRequestHeader("Content-length", post_params.length);
			//page_xhr.setRequestHeader("Connection", "close");
			// if post_params is an object, turn it into a URL string. we are introducing a dependency on jquery, that's ok.
			if (typeof post_params == 'object')
				post_params = $.param(post_params);
			page_xhr.send(post_params);
		}
	}
		return new Promise(returnresult);

	}
	
	function new_xhr()
	{
		if (window.XMLHttpRequest) {
			return new XMLHttpRequest();
		}
		// IE
		else if (window.ActiveXObject) {
			return new ActiveXObject("Microsoft.XMLHTTP");
		}
	}
	function poll_status()
	{
		var status_xhr = currentOp.status_xhr;
		var status_div_txt = currentOp.status_xhr;
		status_xhr.open("GET", currentOp.status_page, true);
	
		// hack to make status_div visible inside the callback
		status_xhr.onreadystatechange=function() {
	
			if (status_xhr.readyState==4) {
				var POLL_MILLIS = MIN_POLL_MILLIS; // default
				
				if (!currentOp.done && !currentOp.cancelled) {
					//    debugger;
					if (this.status != 200 && this.status != 0)
					{
						// this mostly happens if the server crashed.
						// give an alert, because most likely window.location = error.jsp is not going to work either.
						var err = ("Sorry, looks like we can't get a response from the server. Please try restarting ePADD.");
						epadd.error (err);
						$('.muse-overlay').hide();
						window.location = "error.jsp";
						return;
					}
					var responseText = status_xhr.responseText;
					var cType = this.getResponseHeader("Content-Type");
					if ((cType != null) && cType.indexOf ('application/json') >= 0)
					{
						var success = false;
						responseText = muse.trim(responseText);
	
						// epadd.log ('responseText: ' + responseText);
						try {
							eval ("var j = " + responseText + ";");
						} catch (e) { epadd.log ("Sorry! error converting response text to json: " + responseText);}

						if (typeof(j) === 'undefined') {
							if (responseText)
								currentOp.status_div_text.innerHTML = responseText;
						}
						else
						{
							if (j.sp_not_ready) { // this means status provider is not ready
								// hide cancel button
								$('#cancel').hide();
							} else {
								// show cancel button
								$('#cancel').show();
							}
							if (j.error)
							{
								html = 'Error! ' + j.error;
								epadd.log (html);
							}
							else
							{
								shown = true;
								html = j.message ? j.message : ''; // prevent displaying undefined in case j.message is not defined.
							//	epadd.log ('status = ' + muse.dump_obj(j));
							    try { document.title = html; } catch (e) { epadd.log ('exception trying to set title to ' + html); } // set title so user can switch tabs and still keep an eye on the status of this one
								var $progress = $('.progress_bar');
								var total_width = $('.progress_bar_outer_box').width();
								epadd.log("pct: "+j.pctComplete);
								if (j.pctComplete && j.pctComplete >= 0)
								{
	
									var newWidth = parseInt(total_width*j.pctComplete/100);
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
								if ((j.secsElapsed && (j.secsElapsed < 0 || j.secsElapsed > 10))
								   && (j.secsRemaining && j.secsRemaining >= 0))
								{
									$('.time_remaining').html(muse.approximateTimeLeft(j.secsRemaining));
									POLL_MILLIS = (j.secsRemaining*1000)/100; // poll after 1% of time left
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
								var fn = getTeaserFn(j.teasers, 0);
								fn();					
							}
							currentOp.status_div_text.innerHTML = html;
						}
					}
					else // not json ?!?
						currentOp.status_div_text.innerHTML = responseText;
		
					setTimeout(poll_status, POLL_MILLIS);
				}
				else // current op done or cancelled
					currentOp.status_div_text.innerHTML = "";
			}
		};
		status_xhr.send(null);
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
		epadd.log ('cancelling current op ' + currentOp);
		// cancel requested. hide status box.
		currentOp.cancelled = true;
		currentOp.page_xhr.abort();
//	currentOp.status_xhr.abort(); // do we need this ?
		if (currentOp.teaserTimeoutId)
			clearTimeout (currentOp.teaserTimeoutId);

		// tell the server to abort, but we don't really wait for it to confirm... this does run the risk of cancel's getting lost...
		$.get('ajax/cancel.jsp');
		// restore ui
		$(currentOp.status_div).hide();
		$('.muse_overlay').hide();
		$('.teaser').hide();
		currentOp.status_div_text.innerHTML = "<br/>&nbsp;<br/>";
		document.title = currentOp.orig_doc_title;

//	$('#div_main').removeClass('faded');
//	$('body').css("background-color", "#e0e0e0"); // restore body margins when status window is closed

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

//	$('body').css("background-color", "#c0c0c0");  // fade body margins when status window is seen, a la hulu
	// insert overlay only if it doesn't already exist
	$('.muse-overlay').height($(document).height());
	$('.muse-overlay').width($(document).width());
	$('.muse-overlay').show();
	sdiv.style.left = (window.innerWidth/2)- 320+"px"; // the total width is 640px
	sdiv.style.top = (window.innerHeight/2) - 65+"px";

	return kick_off_page_fetch(page, post_params, onready, redirect_page);
	// end of this function. the rest are private helper functions


}

