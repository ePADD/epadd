
function get_account_div(accountName, accountIdx)
{
	// if querying by name, necessary to escape the accountIdx as it may have periods.
	// see http://docs.jquery.com/Frequently_Asked_Questions#How_do_I_select_an_item_using_class_or_ID.3F
	var accounts = $('.account');
	if (accounts.length == 0)
		throw("ERROR");
	var accountDiv = $(accounts[accountIdx]);
	return accountDiv;
}

function render_folders_box(accountName, accountIdx, accountStatus)
{
	var accountDiv = get_account_div(accountName, accountIdx);
	var accountHeader = $('.accountHeader', accountDiv);
	var accountBody = $('.foldersTable', accountDiv);

	var nFoldersAlreadyShown = 0;
	var prev_folders = $('.folder', accountDiv);
	if (prev_folders)
		nFoldersAlreadyShown = prev_folders.length;

	var folderInfos = accountStatus.folderInfos;
	if (!folderInfos) //  || !folderInfos.length)
		return; // we're not ready yet, just return quietly

	// store the status of checked folders in the existing doc, and initialize them again later
    // we assume that new folderinfos only get added at the end, but the previous remain in the same position
	var checked = [];
	for (var i = 0; i < prev_folders.length; i++)
		checked[i] = prev_folders[i].checked;

	// $('.nFolders', accountDiv).html(folderInfos.length);

	epadd.log ('we already have ' + nFoldersAlreadyShown + ' folders, new status has ' + folderInfos.length);
	epadd.log (accountStatus);

	var html = '';
//	for (var i = nFoldersAlreadyShown; i < folderInfos.length; i++)
	var totalMessages = 0, totalFileSize = 0;
	for (var i = 0; i < folderInfos.length; i++)
	{
		if (i == 0)
			html += '<tr>';

		var folderInfo = folderInfos[i];
		// if the folder was already selected (checked[i], give it a selected-folder class so it'll be highlighted
		var classStr = (checked[i]) ? 'folderEntry selected-folder' : 'folderEntry';
		
		var hovertext = folderInfo.displayName;
		if (folderInfo.fileSize && folderInfo.fileSize > 0) {
			if (folderInfo.fileSize > 1024)
				hovertext += ' (Size: ' + Math.floor(folderInfo.fileSize/1024) + " KB)";
			else
                hovertext += ' (' + folderInfo.fileSize + " bytes)";
		}

	    html += '<td title="' + hovertext + '" class=\"' + classStr + '\">\n'; // .folderEntry does the same as min-width:225.

	    var checkedStr = (checked[i]) ? 'CHECKED' : '';
	    html += '<INPUT class="folder" onclick=\"updateFolderSelection(this)" TYPE=CHECKBOX STORE="' + accountName + '" NAME="' + folderInfo.longName + '"' + checkedStr + '/>';
	    html += '&nbsp;' + folderInfo.shortName;
//	    html += '&nbsp;' + muse.ellipsize(folderInfo.shortName, 15);
	    if (folderInfo.messageCount >= 0)
		    html += ' (' + folderInfo.messageCount + ')';
		else
		    html += ' (<img width="15" src="images/spinner.gif"/>)';

	    html += '</td>\n';

	    if ((i+1)%numFoldersPerRow === 0) // numFoldersPerRow defined in /folders
		    html += '\n</tr>\n<tr>';

	    totalMessages += folderInfo.messageCount;
	    if (folderInfo.fileSize && folderInfo.fileSize > 0)
		    totalFileSize += folderInfo.fileSize;
	}

	if ('' !== html)
	{
	//	console.muse.log ('html being added is ' + html);
	//	$(accountBody).append(html);
		accountBody.html(html);
	}

	var message = accountName + ' (' + folderInfos.length + " folder(s), " + totalMessages + " message(s)";
	if (totalFileSize > 0) {
		message += ", " + Math.floor (totalFileSize/1024) + " KB";
	}
	message += ')';
    if (!accountStatus.doneReadingFolderCounts) {
		accountHeader.html('Scanning... ' + message);
    } else {
        accountHeader.html(message);
        $('#go-button').fadeIn();
        $('#date-range').fadeIn();
        if (folderInfos.length > 1)
            $('.select_all_folders').fadeIn();
    }
}

// displays folders and counts for the given account
// first is true only when called from the caller, not when this fn resched's itself
function display_folders(accountName, accountIdx, first)
{
	if (first)
	{
		var accountDiv = get_account_div(accountName, accountIdx);
		var accountHeader = $('.accountHeader', accountDiv);
		accountHeader.html("Scanning " + accountName);
		epadd.log ('Starting to read account ' + accountIdx + ' ' + accountName);
	}

	epadd.log ('Starting to getFolderInfos... for acct#' + accountIdx + ' ' + accountName);

	// note: .ajax instead of .get because it allows us to disable caching
	// refreshing folders while reading them wasn't working earlier inspite if cache control header from getFolderInfos.jsp
	// TODO: maybe should detect if any folders have already been loaded into archive and have them initially checked.
	//       but should also give some indicator that a sync is needed, i.e., if the (server) folder has been updated since it was previously loaded.
	$.ajax({
		type:'GET',
		url: 'ajax/getFolderInfos.jsp?account=' + accountIdx, 
		cache: false,
		success: function (response, textStatus) {
				// response is a json object
				var accountStatus =  response;
				epadd.log ('updating folders box for accountIdx ' + accountIdx + ': '  + accountName + ' http req. status = ' + textStatus);
				render_folders_box(accountName, accountIdx, accountStatus);
				// if we're not done, schedule refresh_folders again after some time
				if (typeof accountStatus.doneReadingFolderCounts == 'undefined' || !accountStatus.doneReadingFolderCounts)
					setTimeout(function() { display_folders(accountName, accountIdx, false); }, 1000);
			},
	error: function() { 
			alert ("Sorry, something went wrong. The ePADD program has either quit, or there was an internal error. Please retry and if the error persists, report this to the ePADD development team."); 
			$('img[src="images/spinner.gif').hide(); // hide the spinner otherwise it continues on the page even after this crash
		}
	});
}

function toggle_select_all_folders(e) {
	var $elem = $(e.target).closest('button');
	$account = $elem.closest('.account'); // which account we're in for this select all button
	var text = $elem.text();
	epadd.log('current text = ' + text);
	epadd.log($("input.folder").length);
	var UNSELECT_TEXT = 'Unselect all folders', SELECT_TEXT = 'Select all folders'; // text for buttons
	if (text.indexOf(UNSELECT_TEXT) >= 0)
	{
		$elem.text(SELECT_TEXT);
		$("input.folder", $account).attr("checked", false);
		$("input.folder", $account).closest('.folderEntry').removeClass('selected-folder');
	}
	else
	{
		$elem.text(UNSELECT_TEXT);
		$("input.folder", $account).attr("checked", true);
		$("input.folder", $account).closest('.folderEntry').addClass('selected-folder');
	}
	return false;
}

function updateFolderSelection(o)
{
	if (o.checked == true)
		$(o).closest('.folderEntry').addClass('selected-folder');
	else
		$(o).closest('.folderEntry').removeClass('selected-folder');
}

function updateAllFolderSelections(o)
{
	// warning: this code not really tested
	// repaint selected folder indication for all folders
    var unchecked = $('input.folder:not(:checked)');
    unchecked.each(function (index, elem) {
    	$(elem).closest('.folderEntry').removeClass('selected-folder');
    });
    var checked = $('input.folder:checked');
    checked.each(function (index, elem) {
    	$(elem).closest('td.folderEntry').addClass('selected-folder');
    });
}
