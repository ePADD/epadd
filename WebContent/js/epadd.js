"use strict";
var epadd = {};

var escapeHTML = function(s) {
	// verified that all the 5 characters "'&<> are ok being embedded inside a / /g
    s = s.replace(/"/g, "&quot;");
    s = s.replace(/'/g, "&apos;");
    s = s.replace(/&/g, "&amp;");
    s = s.replace(/</g, "&lt;");
    s = s.replace(/>/g, "&gt;");
    return s;
}

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

// s is the message, f is the function to be called when the modal is dismissed
epadd.alert = function(s, f) {
	$('#alert-modal .modal-body').html(s);
	if (f) {
		$('#alert-modal').on('hidden.bs.modal', f);
	}
	$('#alert-modal').modal();
}

epadd.pluralize = function(count, description)
{
	if (count == 1)
		return count + ' ' + description;
	else
		return count + ' ' + description + 's';
};

// do a search on the text of the element that was clicked
epadd.do_search = function(e,archiveID) {
	var term = $(e.target).text();
	// if term is not already quoted, quote it now
	term = term.trim();
	if (!(term && term.length > 2 && term.charAt(0) == '"' && term.charAt(term.length-1) == '"')) {
		// not in quotes. encode URI and then add quotes
		term = '"' + encodeURIComponent(term) + '"'; // remember to encodeURIComponent, otherwise it fails on names with &. Also don't use encodeURI because it doesn't escape &
	}
	// otherwise let term be as is.

	window.open ('browse?archiveID='+archiveID+'&adv-search=1&term=' + term + '&termBody=on&termSubject=on'); // since this function is used from entities page, we don't want to match attachments (?)
//	window.open ('browse?adv-search=1&term=' + term + '&termBody=on&termSubject=on&termAttachments=on');
};

// do a search on the text of the element that was clicked
epadd.do_search_incl_attachments = function(e,archiveID) {
    var term = $(e.target).text();
    // if term is not already quoted, quote it now
    if (!(term && term.length > 2 && term.charAt(0) == '"' && term.charAt(term.length-1) == '"')) {
        // not in quotes. encode URI and then add quotes
        term = '"' + encodeURIComponent(term) + '"'; // remember to encodeURIComponent, otherwise it fails on names with &. Also don't use encodeURI because it doesn't escape &
    }
    // otherwise let term be as is.

    //window.open ('browse?adv-search=1&term=' + term + '&termBody=on&termSubject=on&term'); // since this function is used from entities page, we don't want to match attachments (?)
	window.open ('browse?archiveID='+archiveID+'&adv-search=1&term=' + term + '&termBody=on&termSubject=on&termAttachments=on');
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
		var $emailSource = $($('input', $mbox_accounts[i])[2]); // second input field is the email source
		$emailSource.attr('name', 'emailSource' + count);
		$emailSource.attr('id', 'emailSource' + count);
		count++;
	}
};

/** takes the input fields and does logins, showing spinners while logging is on. returns false if an obvious error occurred. */

epadd.do_logins = function() {

	epadd.fix_login_account_numbers();

	// squirrel away the input field values in local storage
	$('input[type="text"]').each(function(){
		var field = 'email-source:' + $(this).attr('name');
		if (!field)
			return;
		var value = $(this).val();
		localStorage.setItem(field, value);
	});

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
		var is_valid_login_account = (login && login.length > 0) && (pw && pw.length > 0);
		var is_valid_mbox_account = (mbox && mbox.length > 0);
		is_valid_account[i] = is_valid_login_account || is_valid_mbox_account;
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

/** needs a stats field to update stats in. if successive, will navigate the browser to /collections */
epadd.import_accession = function(e, post_params) {
    $('#gobutton').fadeOut(); // avoid a second submit

	var $spinner = $('.fa-spinner', $(e.target));
	$spinner.show();
	$spinner.addClass('fa-spin');
	$('#spinner-div').fadeIn();

	// close the file picker first in case its open
	if (window.fp)
		fp.close();
	
	$.ajax({type: 'POST',
	dataType: 'json',
	url: 'ajax/importAccession.jsp',
	data: post_params,
	cache: false,
	success: function (response) {
		$spinner.removeClass('fa-spin');
		$('#spinner-div').fadeOut();
		$('#gobutton').fadeIn();

		if (response ) {
            if (response.status == 0) {
                var status = 'Success! ' + response.message;
                epadd.log('loading archive status: ' + status);
                window.location = './browse-top?archiveID=' + response.archiveID;
            } else if (response.status == 1) {
                windos.location = "./mergeReport?archiveID=" + response.archiveID;
            } else {
                epadd.alert('Error! Code ' + response.status + ', Message: ' + response.error);
            }
        }   else
                    epadd.alert('Error! No response from ePADD.');
	},
	error: function() {
		$('#spinner-div').fadeOut();
		$('#gobutton').fadeIn();
		epadd.alert ("Sorry, something went wrong. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report it to epadd_project@stanford.edu.");
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
//It is advisable to return an ajax object on which any subsequent promise/wait/then can be invoked to chain ajax requests together.
//Currently only following two functions return such objects because we needed to implement the functionality of closing an archive by
//stitching together the functionality of saveArchive and unloadArchive.
epadd.unloadArchive = function(archiveID) {
	return $.ajax({
			type: 'POST',
			url: "ajax/kill-session.jsp",
			dataType: "json",
        	data: {
                "archiveID": archiveID
            },
			success: function(data) {epadd.alert('Archive unloaded successfully!');},
			error: function(jq, textStatus, errorThrown) { var message = ("Error unloading archive. (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")"); epadd.log (message); epadd.alert(message); }
	});
};



epadd.saveArchive= function(archiveID,prompt) {
    if(prompt==undefined){
        prompt=true;
    }
    return $.ajax({
        type: 'POST',
        url: "ajax/save-archive.jsp",
        dataType: "json",
        data: {
            "archiveID": archiveID
        },
        success: function(data) { if(prompt){epadd.alert('Archive saved successfully!')};},
        error: function(jq, textStatus, errorThrown) { var message = ("Error saving archive. (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")"); epadd.log (message); epadd.alert(message); }
    });
};

/*
epadd.saveAndCloseArchive= function() {
    $.ajax({
        type: 'POST',
        url: "ajax/save-archive.jsp",
        dataType: "json",
        success: function(data) { epadd.alert(date+'Archive saved successfully! Now closing the archive',epadd.unloadArchive());},
        error: function(jq, textStatus, errorThrown) { var message = ("Error saving and closing archive. (Details: status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace() + ")"); epadd.log (message); epadd.alert(message); }
    });
};
*/


epadd.deleteArchive = function(archiveID) {
	var c = confirm ('Delete the archive? This action cannot be undone!');
	if (!c)
		return;
	/*var $spinner = $('.spinner', $(e.target));
	$spinner.addClass('fa-spin'); // revisit this -- fa-spin did not work for loadArchive() after new fa version
	*/
	$.ajax({
		type: 'POST',
		url: "ajax/delete-archive.jsp",
		dataType: "json",
        data: {
            "archiveID": archiveID
        },
		success: function(data) { $spinner.removeClass('fa-spin'); epadd.alert('Archive deleted successfully!', function() { window.location.reload(); });},
		error: function(jq, textStatus, errorThrown) { $spinner.removeClass('fa-spin'); var message = ("Error deleting archive, status = " + textStatus + ' json = ' + jq.responseText + ' errorThrown = ' + errorThrown + "\n" + printStackTrace()); epadd.log (message); epadd.alert(message); }
	});
};