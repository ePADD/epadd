"use strict";
var epadd = {};

epadd.post_message = function(mesg)
{
	$.ajax({
		url: 'ajax/muselog.jsp',
		type: 'POST',
		data: {'message': mesg.toString()},
		retryLimit: 3,
		async: true
	});
	//$.post('/ajax/muselog.jsp', {'message': mesg.toString()});
};

epadd.log = function(mesg, log_on_server)
{
//	alert ('logging ' + mesg);
	if (typeof console != 'undefined' && epadd.log)
		console.log(mesg);

	// log on server only if log_on_server is undefined or true
	if (typeof log_on_server == 'undefined' || log_on_server)
		epadd.post_message(mesg); // post JS message to server

};

epadd.alert = function(s) { $('#alert-modal .modal-body').html(s); $('#alert-modal').modal(); }

// do a search on the text of the element that was clicked
epadd.do_search = function(e) {
	var term = $(e.target).text();
	window.open ('browse?term=\"' + term + '\"');
};

//do a search on the text of the element that was clicked
epadd.do_sentiment_search = function(e) {
	var term = $(e.target).text();
	window.open ('browse?sentiment=' + term);
};

// fixes field names in account divs in prep for collect_input_fields
// the form field names must be numbered incrementally, e.g. loginName0, password0, mboxDir1, etc.
epadd.fix_login_account_numbers = function() {
	var count = 0;
	var $server_accounts = $('#servers .account');
	for (var i = 0; i < $server_accounts.length; i++)
	{
		// set account type field to 'email' and the name and id to accountType<n>
		var $accountType = $('.accountType', $server_accounts[i]);
		$accountType.val('email');
		$accountType.attr('name', 'accountType' + count);
		$accountType.attr('id', 'accountType' + count);

		// now set the names of the login/password fields
		var $loginName = $($('input', $server_accounts[i])[1]); // second input field is login
		$loginName.attr('name', 'loginName' + count);
		$loginName.attr('id', 'loginName' + count);

		var $password = $($('input', $server_accounts[i])[2]); // third input field is password
		$password.attr('name', 'password' + count);
		$password.attr('id', 'password' + count);

		var $spinner = $($('.spinner', $server_accounts[i])[0]); // 4th input field is password
		$spinner.attr('id', 'spinner' + count);

		count++;
	}
	
	var $private_server_accounts = $('#private_servers .account');
	for (var i = 0; i < $private_server_accounts.length; i++)
	{
		// set account type field to 'email' and the name and id to accountType<n>
		var $accountType = $('.accountType', $private_server_accounts[i]);
		$accountType.val('imap');
		$accountType.attr('name', 'accountType' + count);
		$accountType.attr('id', 'accountType' + count);

		// now set the names of the login/password fields
		var $imapserver = $($('input', $private_server_accounts[i])[1]); // second input field is server
		$imapserver.attr('name', 'server' + count);
		$imapserver.attr('id', 'server' + count);

		// now set the names of the login/password fields
		var $loginName = $($('input', $private_server_accounts[i])[2]); // third input field is login
		$loginName.attr('name', 'loginName' + count);
		$loginName.attr('id', 'loginName' + count);

		var $password = $($('input', $private_server_accounts[i])[3]); // 4th input field is password
		$password.attr('name', 'password' + count);
		$password.attr('id', 'password' + count);

		var $spinner = $($('.spinner', $private_server_accounts[i])[0]); // 4th input field is password
		$spinner.attr('id', 'spinner' + count);
		count++;
	}
	
	var $mbox_accounts = $('#mboxes .account');
	for (var i = 0; i < $mbox_accounts.length; i++)
	{
		var $accountType = $('.accountType', $mbox_accounts[i]);
		$accountType.val('mbox');
		$accountType.attr('name', 'accountType' + count);
		$accountType.attr('id', 'accountType' + count);
		
		var $dir = $($('input', $mbox_accounts[i])[1]); // second input field is the mbox dir
		$dir.attr('name', 'mboxDir' + count);
		$dir.attr('id', 'mboxDir' + count);
		count++;
	}
};

/** takes the input fields and does logins, showing spinners while logging is on. returns false if an obvious error occurred. */
epadd.do_logins = function() {
	epadd.fix_login_account_numbers();
	epadd.log('doing login (go button pressed) for ' + $('#loginName0').val());
	var post_params = muse.collect_input_fields();
	var is_valid_account = [];
	$('.account .input-field').removeClass('has-error'); // remove all input fields marked with an error

	// this does the actual logins after sanity checking, renumbering accounts, etc.
	function doActualLogins() {
		for (var i = 0; i < $accounts.length; i++) {
			if (!is_valid_account[i])
				continue;

			post_params.accountIdx = i;
			post_params.incremental = 'true'; // don't think this is needed any more
			var n_errors = 0, resp_received = 0;
			epadd.log('logging into account # ' + accountIdx);
			var sent_folder_found = true; // will be set to false for any a/c for which we don't find a valid sent folder

			// do login for this account, all these lookups will be fired in parallel
			// note that all post_params are posted for each a/c... somewhat inefficient, but not enough to matter
			$($('.fa-key')[i]).addClass('fa-spin'); // start spinning the password key for the i'th

			$.ajax({
				url: 'ajax/doLogin.jsp',
				type: 'POST',
				data: post_params,
				dataType: 'json',
				success: (function (x, idx) {
					return function (j) {
						epadd.log('received success resp for login ' + idx + ':' + muse.dump_obj(j));
						$($('.fa-key')[x]).removeClass('fa-spin'); // start spinning the password key for the i'th
						resp_received++;
						if (j.status != 0) {
							epadd.log('error message: ' + j.errorMessage);
							epadd.alert('Error # ' + j.status + ": " + j.errorMessage); // $.jGrowl(j.errorMessage);
							n_errors++;
							$('.input-field', $('.account')[x]).addClass('has-error');
						}

						// if any errors, we'd have growled, we just don't move to the next page
						if (resp_received == n_valid_accounts && n_errors == 0)
							epadd.all_logins_complete(sent_folder_found);
					};
				})(i, accountIdx),

				error: (function (x, idx) {
					return function (j) {
						$($('.fa-key')[x]).removeClass('fa-spin'); // start spinning the password key for the i'th
//						$('.input-field', $('.account')[x]).addClass('has-error'); // maybe no need to mark error here. this is an ajax error, not a user error.
						epadd.alert("There was an AJAX error accessing account #" + x);
					}
				})(i, accountIdx)
			});
			accountIdx++;
		}
	}

	// epadd.log ("params = " + muse.dump_obj(post_params));
	// do not print post_params as it may have a passwd
	var $accounts = $('.accountType');
	epadd.log('#accounts = ' + $accounts.length);
	var accountIdx = 0;

	// compute is_valid_accounts for each account on the screen
	// also count the # valid accounts and alert if no valid login info
	var n_valid_accounts = 0;
	epadd.log('is_valid_account=' + is_valid_account);
	for (var i = 0; i < $accounts.length; i++) {
		// its a valid a/c if its got a login and passwd
		var $login = $('#loginName' + i);
		var login = $login.val();
		var pw = $('#password' + i).val();
		var mbox = $('#mboxDir' + i).val();
		// ignore if has login name and password fields, but neither is filled in
		is_valid_account[i] = (!((login == null || login.length == 0) && (pw == null || pw.length == 0))) || (mbox && mbox.length > 0);
		if (!is_valid_account[i]) {
			epadd.log('account #' + i + ' is not valid');
			continue;
		}
		n_valid_accounts++;
	}

	epadd.log('n_valid_accounts = ' + n_valid_accounts + ' out of ' + $accounts.length);

	if (n_valid_accounts === 0) {
		epadd.alert('Please enter some login information');
		return;
	}

	// first clear the existing accounts in the fetcher if any, then do logins given on this screen
	$.ajax({
		url: 'ajax/clearAccounts.jsp',
		type: 'POST',
		dataType: 'json',
		success: function (result) {
			epadd.log('cleared any existing accounts');
			doActualLogins();
		},
		error: function (result) {
			epadd.alert("Warning: error clearing existing accounts, You may see some stale accounts.");
		}
	});

	return true;
}



epadd.all_logins_complete = function(sent_folder_found) {
	epadd.log ('all login responses received');
	epadd.log ('redirecting to folders');
	window.location = 'folders';
};

epadd.submitFolders = function()
{
	// throws an exception if at least one folder is not selected
	function getSelectedFolderParams() {
	    var checked = $('input.folder:checked');
	    var urlParams = "";
	    if (checked.length == 0)
	    {
	        epadd.alert ("Please select at least one folder.");
	        throw "Error";
	    }

	    for (var i=0; i < checked.length; i++)
	    {
	         var store = checked[i].getAttribute("STORE");
	         urlParams += encodeURI("folder=" + store + '^-^' + checked[i].name + "&");
	    }

	    // convert from, to fields to dateRange field needed by doFetchAndIndex
	    if ($('#from').val().length > 0 && $('#to').val().length > 0)
	    	urlParams += '&dateRange=' + $('#from').val() + '-' + $('#to').val();
	    
	    epadd.log ('urlparams = ' + urlParams);
	    return urlParams;
	}
	try {
		var post_params = getSelectedFolderParams() + '&period=Monthly&downloadAttachments=true';
		// need to check muse.mode here for page to redirect to actually!
		var page = "ajax/doFetchAndIndex.jsp";
		fetch_page_with_progress(page, "status", document.getElementById('status'), document.getElementById('status_text'), post_params);
	} catch(err) { }
};


if ($) {   // from http://stackoverflow.com/questions/11165618/jquery-selector-td-with-specific-text
	$.expr[':'].containsexactly = function(obj, index, meta, stack) 
	{  
	    return $(obj).text() === meta[3];
	};  
}

// removes link formatting for any link with the given text within the top_selector
epadd.select_link = function(top_selector, text) {
	var $x = $(top_selector + " a:containsexactly('" + text + "')");
	$x.addClass('selected');
};

// removes link formatting for any link with the given text within the top_selector
epadd.nav_mark_active = function(text) {
	var $x = $('.navbar-nav li ' + " a:containsexactly('" + text + "')").closest('li');
	$x.addClass('nav-active');
};

/** needs a stats field to update stats in */
epadd.load_archive = function(e, dir) {
	var $spinner = $('.spinner', $(e.target));
	$spinner.addClass('fa-spin');
	// close the file picker first in case its open
	if (window.fp)
		fp.close();
	
	$('#stats').css('color', 'inherit');
	$.ajax({type: 'POST',
	dataType: 'json',
	url: 'ajax/importArchive.jsp', 
	data: {dir: dir},
	cache: false,
	success: function (response, textStatus) {
		$spinner.removeClass('fa-spin');
		if (response && (response.status == 0)) {
				var status = 'Success! ' + response.message;
				epadd.log ('loading archive status: ' + status);
				window.location = './collections';
			}
			else {
				if (response)
					epadd.alert('Error! Code ' + response.status + ', Message: ' + response.error);
				else
					epadd.alert('Error! No response from ePADD.');
				$spinner.removeClass('fa-spin');
			}
	},
	error: function() { 
		epadd.alert ("Sorry, something went wrong. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report it to epadd_project@stanford.edu.");
		$spinner.removeClass('fa-spin');
	}});
};

epadd.clearCache = function() {
	$('#clearCacheSpinner').show();
	$.ajax({url: "clearCache",
		dataType: "json",
		success: function(data) {
			$('#clearCacheSpinner').hide('slow');
			if (data.status == 0)
				$.jGrowl('Archive cleared.');
			else
				$.jGrowl('Problems clearing archive.');
		}
	});
};

epadd.unloadArchive = function() {
	$.ajax({
			type: 'POST',
			url: "ajax/kill-session.jsp",
			dataType: "json",
			success: function(data) { epadd.alert('Archive unloaded successfully!'); window.location.reload();},
			error: function(jq, textStatus, errorThrown) { var message = ("Error unloading archive. (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")"); epadd.log (message); epadd.alert(message); }
	});
};


epadd.deleteArchive = function(e) {
	var c = confirm ('Delete the archive? This action cannot be undone!');
	if (!c)
		return;
	var $spinner = $('.spinner', $(e.target));
	$spinner.addClass('fa-spin');

	$.ajax({
		type: 'POST',
		url: "ajax/delete-archive.jsp",
		dataType: "json",
		success: function(data) { $spinner.removeClass('fa-spin'); epadd.alert('Archive deleted successfully!'); window.location.reload();},
		error: function(jq, textStatus, errorThrown) { $spinner.removeClass('fa-spin'); var message = ("Error deleting archive, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace()); epadd.log (message); epadd.alert(message); }
	});
};