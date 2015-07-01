//==UserScript==
//@name muse-lens
//@exclude  http://localhost*
//@exclude  http://www.google.com/*
//@exclude  http://*.google.com/*
//==/UserScript==

/**
 Collect good testcases/difficult pages here.
 http://sec.gov/Archives/edgar/data/1326801/000119312512034517/d287954ds1.htm -- Facebook SEC S-1, very large page.
 http://icones.pro/en/?s=folder ...
 http://amazon.com (uses an old version of jquery, 1.2.6, so tests out noconflict issues
 http://nytimes.com
 http://cs.stanford.edu -- there is some funniness on this page with jquery claiming that nodeType 3 (txt nodes) are not visible. 
*/

(function() { // anon. wrapper function
	
try{throw'';}catch(e){}
var RUNNING_IN_PLUGIN = (typeof chrome != 'undefined' && chrome && chrome.extension);

if (window.top != window.self) // don't run on iframes window
    return;

// warning: be careful of using jquery .closest because some sites like Amazon inject jquery 1.2.6 on the page after us, and it didn't have .closest()
// instead use findParentWithSelector(node, selector)
// see: http://stackoverflow.com/questions/6105544/jquery-closest-works-on-one-page-but-not-another

if (typeof window.MUSE_URL == 'undefined')
	window.MUSE_URL = "http://localhost:9099/muse";

var LOG;
if (typeof GM_log == 'undefined') {
	LOG = function (mesg) { 
	if (typeof console != 'undefined')
		console.log(mesg); 
	};
}
else
	LOG = GM_log;

if (typeof unsafeWindow == 'undefined')
	unsafeWindow = window;

LOG ('Running lens on ' + document.URL + (RUNNING_IN_PLUGIN ? ' (in plugin)' : ''));

// Add jQuery to the page
inject_jquery = function() {
	if (RUNNING_IN_PLUGIN)	{
		// directly call main, no need for injecting jq
	    $_ = $;
	    main();
	    return;
	}

	var saved$ = null; // will save the original jq if page already has it
	
	function wait_for_prettyphoto_and_call_main() {
		if (typeof unsafeWindow.jQuery.fn.prettyPhoto == 'undefined') {
			window.setTimeout(wait_for_prettyphoto_and_call_main, 100); // jq not loaded, try again in 100ms
		} else {
			LOG ('ok, pretty photo loaded up ' + unsafeWindow.jQuery.prettyPhoto.version);
			
			// now, make our version of jq invisible and restore the original version if it exists
			$_ = unsafeWindow.jQuery.noConflict(true); // $_ is our version of jq, regardless of whether page had its own.	
			if (saved$ != null) {
				LOG ('restoring original version of jquery on page: ' + saved$().jquery);
				unsafeWindow.$ = unsafeWindow.jQuery = saved$; // restore saved
			} else {
				LOG ('cool, no previous version of jquery on page');
			}
			if (document.URL.indexOf("youtube") >= 0) { 
				prefetchYoutube();
				window.setTimeout(main,3300); // 3500);
			} else
				window.setTimeout(main,300);
		}
	}
	
    // function that polls and waits for jq to show up before dispatching to main
	function wait_for_jquery_and_call_main() {
		if (typeof unsafeWindow.jQuery == 'undefined') {
			window.setTimeout(wait_for_jquery_and_call_main, 100); // jq not loaded, try again in 100ms
		} else {
			// we have to install prettyPhoto also now
			LOG ('new jquery loaded, version is ' + unsafeWindow.jQuery().jquery + '. now injecting pretty photo');
			function inject_prettyphoto() {
				var GM_Head = document.getElementsByTagName('head')[0] || document.documentElement || document.body;
				var GM_JQPP = document.createElement('script');
				GM_JQPP.src = window.MUSE_URL + '/js/jquery.prettyPhoto.js';
				GM_JQPP.type = 'text/javascript';
				GM_JQPP.async = true;
				GM_Head.insertBefore(GM_JQPP, GM_Head.lastChild);
				var myStylesLocation = window.MUSE_URL + '/css/prettyPhoto.css';
				unsafeWindow.jQuery('<link rel="stylesheet" type="text/css" href="' + myStylesLocation + '" >').appendTo("head");           
			}
			// we have to wait now for prettyphoto also, because we want it to be associated with our version of jq
			$ = jQuery = unsafeWindow.jQuery;
			inject_prettyphoto();
			wait_for_prettyphoto_and_call_main();
		}	
	}
	
	function inject_jq_script() {
		LOG ("injecting jq");
        var jq_scr = document.createElement('script');
//        jq_scr.src = window.MUSE_URL + '/js/jquery/jquery.js';
        jq_scr.src = window.MUSE_URL + '/js/jquery.js';
        jq_scr.type = 'text/javascript';
        jq_scr.async = true;
	    var heads = document.getElementsByTagName('head');
        if (heads.length > 0)
            head = heads[0];
        else
            head = document.documentElement || document.body;
		head.insertBefore(jq_scr, head.firstChild);
	}

	if (typeof unsafeWindow.jQuery == 'undefined') {
		inject_jq_script();
	} else {
		// see http://blog.nemikor.com/2009/10/03/using-multiple-versions-of-jquery/ for noconflict explanation
		saved$ = unsafeWindow.jQuery.noConflict(true); // first save away the original jq if page already has it			
		LOG ('saving away original version of jquery on page: ' + saved$().jquery);
		inject_jq_script();
	}
	
	LOG ("Waiting for jquery in URL:" + document.URL);
	wait_for_jquery_and_call_main();
};

init = function() {
	if ((document.URL.indexOf(".js") == document.URL.length-3) || (document.URL.indexOf(".css") == document.URL.length-4)) {
		LOG ("skipping injecting jq into " + document.URL);
		return;
	}
	inject_jquery();
};


function inject_html(html) {
	var div = document.createElement('div');
	div.innerHTML = html;
	document.body.insertBefore(div, document.body.firstChild);
}

// most of the logic should be inside main
main = function(evt) {
	if ($_('#muse-status').length == 0)
	{
		var message = 'Reading page...';
		var BG_COLOR = '#0C3569';
	    var muse_status_container_style = 'position:fixed; left:0px; top:0px;width:100%; z-index:100000002;text-align:center;'; // -30 so we can animate it downward
		var muse_status_style = 'position:relative;top:10px;display:inline;padding:10px;background-color:' + BG_COLOR + ';color: #ffffff;';
		muse_status_style += 'font-family: \'Gill Sans\',\'Gill Sans MT\',Arial,sans-serif; font-size:15px; ';
		muse_status_style += 'border-radius:3px; border-bottom:solid 1px black; border-left: solid 1px black; border-right:solid 1px black;';
		muse_status_style += 'box-shadow: 1px 1px 1px #000; -webkit-box-shadow: 1px 1px 1px #000; -moz-box-shadow: 1px 1px 1px #000;';
		inject_html ('<div id="muse-status-container" style="' + muse_status_container_style + '"><div id="muse-status" style="' + muse_status_style + '">' + message + '</div></div>');

		//#0C3569
//		inject_html ('<div id="muse-status" style="' + muse_status_style + '">' + content + '</div>');
	}
	if ($_('.muse-highlight').length > 0)
	{
		$_('#muse-status').html('Already hilited!');
		window.setTimeout(function() { $_('#muse-status').fadeOut('slow');}, 3000);
		return;
	}

	LOG ('starting main with evt ' + evt);
    // if called due to an event, we want to be sure its due to the refresh button at the top
	if (typeof evt !== 'undefined' && evt != null && evt.clientY > 50)
		return;

    var UPPERCASE = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

    var elementlist= [];
    var n_hilights = 0;
    var startTime = new Date().getTime();
	var textNodesParsedWithoutBreak = 0;
	var IS_LARGE_PAGE = false;
	
	// fills in 'arr', an array of text nodes, arr can be passed in as null by external caller.
	// also fills in a null for div, header, li endings if insert_delimiters is true 
	// caller should watch out for nulls in arr
	function addTextNodes(node, arr, insert_delimiters, ignore_invisible) {
		// do not use jquery inside this method, it may not have been loaded!
		if ((document.URL.indexOf('google') || document.URL.indexOf('gmail') >= 0) && node.id == 'gb')
			return null; // ignore google bar because it causes spurious hits w/the name of the user.
		if (document.URL.indexOf('facebook') >= 0 && node.id == 'pageNav')
			return null; // ignore fb bar because it causes spurious hits w/the name of the user.

		// don't want to highlight our own stuff
		if (node.className == 'muse-details')
			return arr; 
		if (node.id == '#muse-status')
			return arr; 
		
		if (!arr)
			arr = [];
		if (node.nodeType == 7 || node.nodeType == 2) // comment node, attribute node
			return arr;

		if (typeof node.tagName !== 'undefined')
			if (node.tagName.toUpperCase() == 'SCRIPT' || node.tagName.toUpperCase() == 'NOSCRIPT')
				return arr; // don't look inside scripts, and even noscripts

		if (textNodesParsedWithoutBreak > 10)
		{
			LOG ('taking a break from finding text nodes');
			textNodesParsedWithoutBreak = 0;
			window.setTimeout (function() { addTextNodes(node, arr, insert_delimiters, ignore_invisible); }, 1); 
			return;
		}

      // ignore content that is not displayed in the page
		try {
			if (node.nodeType != 3) // don't do vis check for nodeType 3 (text nodes), as it fails unnecessarily on cs.stanford.edu
                if (ignore_invisible && !$_(node).is(':visible')) 
                {
                	LOG ('ignoring text because not visible: ' + ellipsize($_(node).text(), 1000));
                	if (arr)
                		LOG (arr.length + ' text nodes already identified');
                	dump_obj(node);
				    return arr;
                }
		} catch(e) { }

		
		var whitespace = /^\s*$/;

		if (node.nodeType == 3 && !whitespace.test(node.nodeValue))
		{
			// skip "Ads by Google" specifically
			if ("Ads by Google" != node.nodeValue)
			{
				arr.push(node);
				if (arr.length % 100 == 0)
					LOG ("# text nodes: " + arr.length);
			}
		}
		else 
		{
            // LOG ('looking for children # ' + node.childNodes.length); 
			for (var i = 0, len = node.childNodes.length; i < len; ++i)
				addTextNodes(node.childNodes[i], arr, insert_delimiters, ignore_invisible);

			if (insert_delimiters && typeof node.tagName !== 'undefined')
			{
				if (typeof arr != 'undefined' && arr && arr.length > 0 && arr[arr.length-1] != null)
				{
					var lastText = arr[arr.length-1].data;

					// see if the lastText ends with a sentence delimiter, if it doesn't insert one after some tags
					if ('!?.'.indexOf(lastText[lastText.length-1]) < 0)
					{
						var tag = node.tagName.toUpperCase();
						if (tag == 'H1' || tag == 'H2' || tag == 'H3' || tag == 'H4' || tag == 'H5' || tag == 'H6' || tag == 'DIV' || tag == 'P' || tag == 'LI' || tag == 'TD' || tag == 'B' || tag == 'I') // perhaps we should just check all tags that are not span.
						{
							// push an artificial stop after these tags.
							// LOG ('Pushing a stop after tag ' + tag);
							arr.push(null);
							if (arr.length % 100 == 0)
								LOG ("# text nodes: " + arr.length);
						}
					}
				}
			}
		}
	    return arr;
	}
	
	// replacements for GM_ functions from http://userscripts.org/posts/search?page=13&q=GM_addStyle
	if (typeof GM_xmlhttpRequest === "undefined") {
		GM_xmlhttpRequest = function(/* object */ details) {
			details.method = details.method.toUpperCase() || "GET";
			
			if(!details.url) {
				throw("GM_xmlhttpRequest requires an URL.");
				return;
			}
			
			// build XMLHttpRequest object
			var oXhr, aAjaxes = [];
			if (typeof ActiveXObject !== "undefined") {
				var oCls = ActiveXObject;
				aAjaxes[aAjaxes.length] = {cls:oCls, arg:"Microsoft.XMLHTTP"};
				aAjaxes[aAjaxes.length] = {cls:oCls, arg:"Msxml2.XMLHTTP"};
				aAjaxes[aAjaxes.length] = {cls:oCls, arg:"Msxml2.XMLHTTP.3.0"};
			}
			if (typeof XMLHttpRequest !== "undefined")
				 aAjaxes[aAjaxes.length] = {cls:XMLHttpRequest, arg:undefined};
		
			for (var i=aAjaxes.length; i--; )
				try {
					oXhr = new aAjaxes[i].cls(aAjaxes[i].arg);
					if (oXhr) 
						break;
				} catch(e) {}
			
			// run it
			if (oXhr) {
				if ("onreadystatechange" in details)
					oXhr.onreadystatechange = function() { details.onreadystatechange(oXhr); };
				if ("onload" in details)
					oXhr.onload = function() { details.onload(oXhr); };
				if ("onerror" in details)
					oXhr.onerror = function() { details.onerror(oXhr); };
				
				oXhr.open(details.method, details.url, true);
				
				if("headers" in details)
					for(var header in details.headers)
						oXhr.setRequestHeader(header, details.headers[header]);
				
				if("data" in details)
					oXhr.send(details.data);
				else
					oXhr.send();
			} else
				throw ("This Browser is not supported, please upgrade.");
		};
	}
	if (typeof GM_addStyle === "undefined") {
		function GM_addStyle(/* String */ styles) {
			var oStyle = document.createElement("style");
			oStyle.setAttribute("type", "text\/css");
			oStyle.appendChild(document.createTextNode(styles));
			document.getElementsByTagName("head")[0].appendChild(oStyle);
		}
	}

	// trim whitespace from either end of s
	var trim = function(s) {
		if (typeof (s) !== 'string')
			return s;

		// trim leading
		while (true) {
			if (s.length == 0)
				break;
			var c = s.charAt(0);
			if (c !== '\n' && c !== '\t' && c !== ' ')
				break;
			s = s.substring(1);
		}
		

		// trim trailing
		while (true) {
			if (s.length == 0)
				break;
			var c = s.charAt(s.length-1);
			if (c !== '\n' && c !== '\t' && c !== ' ')
				break;
			s = s.substring(0,s.length-1);
		}
		return s;
	};

	// main function
	var textNodesOnPage; // global var
	var hits = null; // global var
	
	var inject_styles = function() {
		styles =  '.muse-navbar {padding-top: 3px; position: fixed; top: 0pt; right:10px;z-index:10000; text-transform:uppercase;font-family:"Gill Sans",Calibri,Helvetica,Arial,Times;font-size:10pt;font-weight:normal} \
				   .muse-navbar a span{-moz-border-radius: 4px; background-color: #0C3569; opacity: 0.9;} \
				 	.muse-navbar a span {color:white;font-size:14pt; font-weight:normal; padding: 5px 5px 5px 5px; text-decoration:none;} \
					.muse-navbar a span:hover {color:yellow; text-decoration:none;}';

		var border_spec = 'border-top: 1px solid lightblue; border-right: 1px solid lightblue; border-left: 1px solid lightblue; box-shadow: 2px 2px 3px #000;';

		/* padding-right:20px to ensure space for settings wheel */
		styles += '#calloutparent { padding-right:20px; max-height:60px; line-height:20px; background: none repeat scroll 0 0 #0C3569; opacity: 0.9; color: #fff; box-shadow: 2px 2px 3px #000; padding-top: 5px; text-align:left;margin-left:2%;margin-right:2%;-moz-border-radius: 6px 6px 0px 0px;position: fixed; bottom: 0;left: 0;right: 0;z-index: 10000000;width: 96%; ' + border_spec + '}';
		styles += '.termMenu { background-color: black; -moz-border-radius: 6px 6px 0px 0px;border-top: 1px solid #fff; position:absolute;top:-22px;min-width:70px; border-top: 1px solid white; border-left: 1px solid white; border-right: 1px solid white;}';
		styles += '#callout { float: left;text-align: left; padding: 0px 5px; width:100%; margin-bottom: 4px} \
				  #callout li {background:transparent; display: inline;padding: 0 3px;} \
				  #callout, #callout a, .term {color: white; text-transform:uppercase;font-family:"Gill Sans",Calibri,Helvetica,Arial,Times;font-size:10pt;font-weight:normal;} \
				  .term:hover {text-decoration:underline}';

		styles += '.muse-highlight { background-color: yellow; color: black; cursor:hand; cursor:pointer;} \
				   .muse-soft-highlight {  background-color: lightyellow; color: black; cursor: hand;cursor:pointer;} \
				   .muse-NER-name { border-bottom: 1px red dotted; }';
		
		styles += '#callout-menu { position:absolute; top:5px; right:0px}';
		styles += '.menuitem { width:100% ; cursor:pointer;cursor:hand; font-family:"Gill Sans",Calibri,Helvetica,Arial,Times;font-size:12pt; color:#999; padding:4px 8px;} .menuitem:hover {color:white;}';

		styles += '#settings_dropdown {  top:-105px; right:-4px; margin: 0px;  text-align:right; line-height:25px; position: absolute; padding-left: 10px; padding-right: 10px; display: none;' + border_spec + '}';
		styles += '#settings_dropdown .inner { position:absolute; right: -4px; background-color:#0C3569; }';
		GM_addStyle(styles);
	};

	inject_styles();
	// wipe these out if they already existed
	var p = document.getElementById('calloutparent');
	if (p != null && p.parentNode != null)
		p.parentNode.removeChild(p);
	p = document.getElementsByClassName('muse-details');
	if (p != null && p.length > 0 && p[0].parentNode != null)
		p[0].parentNode.removeChild(p[0]);

	var muse_url = MUSE_URL + "/ajax/leadsAsJson.jsp";
	LOG ('Finding text on page...');
	var ignore_invisible = true;
	if (document.URL.indexOf('nytimes.com') >= 0)
		ignore_invisible = false; // ignore invisible text, but on nytimes.com the main headline is sometimes invisible (according to jq)
	if (document.URL.indexOf('Registration%20Statement') >= 0)
		ignore_invisible = false; // fb sec s1
	
	textNodesOnPage = addTextNodes(document.body, null, true, ignore_invisible);
    LOG (textNodesOnPage.length + ' text nodes found, elapsed time on page = ' + (new Date().getTime()-startTime) + 'ms');

    // compile text nodes into a single string
    var textOnPage = '';
	for (var i = 0; i < textNodesOnPage.length; i++)
	{
		if (textNodesOnPage[i] == null)
			textOnPage += '. ';  // introduce period for null node because its end of a html element which a sentence cannot span
		else
			textOnPage += ' ' + textNodesOnPage[i].data;
	}
	
	textOnPage = textOnPage.replace (/&/g, ' '); // replace ampersands, they interrupt the refText param!
	textOnPage = textOnPage.replace (/  /g, ' '); // rationalize spaces
    LOG ('encoding URIs, elapsed time on page ' + (new Date().getTime()-startTime) + 'ms');

    IS_LARGE_PAGE = textOnPage.length > (100 * 1024); // large pages are those that are > 100K currently. we won't do name red-underlining on those because its too expensive
    if (IS_LARGE_PAGE)
    	LOG ('THIS IS A LARGE PAGE');
    
	// LOG ('text on page = ' + textOnPage);
	LOG ('text on page length = ' + textOnPage.length + ' chars');
	var encoded_page = encodeURI(textOnPage);
	var encoded_url = encodeURI(document.URL);	
	LOG ("Looking up index for " + muse_url);
//		LOG ('encoded page = ' + encoded_page);
	$_('#muse-status').html('Looking up terms...');
	$_('#muse-status').show();
    LOG (' firing ajax, elapsed time so far ' + (new Date().getTime()-startTime) + 'ms');
    
	$_.ajax({
			url: muse_url, 
			type: 'POST',
			data: 'refText=' + encoded_page +"&refURL="+encoded_url, 
			dataType: 'text',
			beforeSend: function(xhr) { xhr.withCredentials = true;  },
			xhrFields: { withCredentials: true},
			crossDomain: true,
			success: function(response) {
			    LOG ('received muse response, elapsed time on page = ' + (new Date().getTime()-startTime) + 'ms');
				muse_response = trim(response);
				var hits = null;
				try { 
					hits = eval('(' + muse_response + ')'); 
					if (hits.error) 
						report_error(hits.error);
					else
						decoratePage(hits); 
				} catch (e) { report_error("Internal error: Wrong json: " + muse_response); return; }

				decoratePage(muse_response);
			},
			error: function(response) {
				report_error('Muse is off. (<a href="http://mobisocial.stanford.edu/muse/muse.jnlp">Launch it</a>)');
			}
		});

	function inject_prettyphoto() {
		var GM_Head = document.getElementsByTagName('head')[0] || document.documentElement || document.body;
		var GM_JQPP = document.createElement('script');
		GM_JQPP.src = window.MUSE_URL + '/js/jquery.prettyPhoto.js';
		GM_JQPP.type = 'text/javascript';
		GM_JQPP.async = true;
		GM_Head.insertBefore(GM_JQPP, GM_Head.lastChild);
		var myStylesLocation = window.MUSE_URL + '/css/prettyPhoto.css';
		$_('<link rel="stylesheet" type="text/css" href="' + myStylesLocation + '" >').appendTo("head");           
	}

	// utility function to print s, subject to a maxChars limit
	function ellipsize(s, maxChars) {
		if (s == null)
			return null;
	
		if (maxChars < 4)
			return (s.substring(0, maxChars));
	
		if (s.length > maxChars)
			return s.substring(0, maxChars-3) + "..."; // (" + s.length + " chars)";
		else
			return s;
	}

// start pretty photo, but wait for it to be come available first
function start_prettyphoto() {
	if (typeof $_.prettyPhoto == 'undefined')
		window.setTimeout (start_prettyphoto, 100);
	else
    {
        // muse-details is the div with the popup html.
//		$_('.muse-highlight').prettyPhoto({theme: 'dark_rounded', opacity: "0.3"});

        // muse-clicker is the div that is meant just to issue clicks. not displayed on page, but click() fired through jquery. 
        // upon click, it always shows the same div, which is the muse-details div. this div is populated appropriately before the click is fired.
        var $clickdiv = $_('<a id="muse-clicker" title="Message Details" href="#muse-details" alt="ALT"></a>'); // funnily, the title for this clicker needs to be set, otherwise pretty photo shows an undefined at bottom left
        $_('body').append($clickdiv);
        var $div = $_('<div style="font-size:11px; max-height:250px;display:none" class="muse-details" title="Message Details" style="align:left;display:none" id="muse-details"/>');
        var $child = $_('<div id="muse-pointers" style="color:white;font-size:11px" title="Message Details" alt="alt"/>');
        $div.append($child);
        $_('body').append($div);
LOG ('pretty photo started');
		$_('#muse-clicker').prettyPhoto({theme: 'light_rounded', opacity: "0.3"});
LOG ('pretty photo clicker started');
    }
}

// highlight contents
// decoratedWithoutBreak is an internal counter counting how many nodes we've decorated since the last break ("timeout")
// after some number of calls, we take a break, to allow the browser to become more responsive.
var decoratedWithoutBreak = 0;

/* hit_pats is a precomputed versions of hits[i].text */
function decorateTextNode(node, hits, hit_pats, anchors_entered) {
	
    if (!node.parentNode)
        return;
    if (node.parentNode.className == 'muse-NER-name' || node.parentNode.className == 'muse-highlight')
    	return;

    if (decoratedWithoutBreak > 100)
    {
         LOG ('taking a break from decorating nodes');
         decoratedWithoutBreak = 0;
         window.setTimeout (function() { decorateTextNode(node, hits, anchors_entered); }, 1);
         return;
    }

    // TEMPORARILY DISABLED decoratedWithoutBreak BECAUSE CALLER HAS TO BE AWARE OF WHEN REAL COMPLETION OF DECORATE NODES IS
    // decoratedWithoutBreak++;

	// node has to be a text node
    try {
	// ignore whitespace nodes
	if (/^\s*$/.test(node.nodeValue))
		return;

	var newNodes = []; // nodes we might create when we split this node
	
	// these two are purely for logging
	var nodeTextLength = node.data.length;
	var nodeTextSample = ellipsize(node.data, 100);

	// nodeText is canonicalized to uppercase, so it can be compared with hit_pats, which is also uppercase
	var nodeText = node.data.toUpperCase();
    
    // check for hits in this node
    var n_pats_hit = 0;
    var decorate_node_start_millis = new Date().getTime();
    
    for (var hit = 0; hit < hits.results.length; hit++)
    {
        var pat = hit_pats[hit];
        if (!pat)
        	continue;

        var pos = nodeText.indexOf(pat);
        if (pos < 0)
            continue; // not found

        n_pats_hit++;
        // skip if prev or next letters are alpha's, we want only complete words
        var prev_letter = '.', next_letter = '.';
        if (pos > 0)
            prev_letter = nodeText.charAt(pos-1);
        if (pos + pat.length < nodeText.length)
            next_letter = nodeText.charAt(pos + pat.length);
        if (UPPERCASE.indexOf(prev_letter) >= 0 || UPPERCASE.indexOf(next_letter) >= 0)
            continue;
        
	    // ok, we have a proper name in this node
	    n_hilights++;
	    
	    // create a <span> decorator node for it -- preferable to a nodes 'cos it's less likely to have existing page styles associated with it.
        var anchor = null;
        var decorator = document.createElement('span');
        
        var hilite_class = sketchy_hit(hits.results[hit]) ? "muse-soft-highlight":"muse-highlight";
        
        // assign it muse-highlight or NER-highlight based on whether it was a real hit or not
        if (hits.results[hit].indexScore > 0)
        {
            if (hilite_class == 'muse-highlight')
            {
                var name = pat.replace (/ /g, '_') + "_anchor";
                name = name.toLowerCase(); // canonicalize
                // only enter an anchor for the first occurrence on a page
                if (typeof anchors_entered[name] == 'undefined' || !anchors_entered[name]) {
	            	anchor = document.createElement('a');
	            	anchor.setAttribute('name', name);
	            	anchor.setAttribute('href', '#');
	            	anchors_entered[name] = true;
                }
            }

            var people_str = '';
            var people = hits.results[hit].people;
            if (typeof people !== 'undefined')
            	for (var i = 0; i < people.length; i++)
            		people_str += people[i].person + ' ';
            var title = '';
            if (hits.results[hit].type)
            	title += hits.results[hit].type + ' ';
            title += 'Associated with: ' + people_str + ' (Score: ' + parseInt(hits.results[hit].score*100)/100.0 + ' Page score: ' + parseInt(hits.results[hit].pageScore*100)/100.0 + ' Index score: ' + parseInt(hits.results[hit].indexScore*100)/100.0 + ' for ' + hits.results[hit].text + ')'; // set it otherwise "undefined" shows up in bottom left corner
            decorator.setAttribute('title',  title);
            decorator.setAttribute('alt', 'ALT');
	        // decorator.setAttribute('onclick', 'return HIT_CLICKED(event, ' + hit + ');');
            decorator.addEventListener("click", function(h) { return function(e) { return HIT_CLICKED(e, h); }; }(hits.results[hit]));
	        decorator.className = hilite_class;
        }
        else
        {
	        decorator.className = 'muse-NER-name'; // not a real hit, for debug only
        }
        // make sure existing css rules on span's don't cause extra margins, padding, etc.
        // e.g. of site that causes trouble without this: the timeswire div on nytimes.com
        $_(decorator).css({display: 'inline', padding: '0px', margin: '0px', 'float': 'none'});

        if (pos > 0)
        {
        	// hit not at the beginning. 
        	 // update nodeText to the portion before the hit
        	var originalNodeLength = nodeText.length;
        	middlebit = node.splitText(pos);
        	endNode = middlebit.splitText(pat.length);
	        var middleclone = middlebit.cloneNode(true);
	        decorator.appendChild(middleclone);
	        nodeText = node.data.toUpperCase();
	        middlebit.parentNode.replaceChild(decorator, middlebit);
        	// add endNode to newnodes if it has any content, we'll process it later
	        if (pos + pat.length < originalNodeLength)
	        {
	        	// LOG ('pushing end node ' + endNode.nodeValue);
	        	newNodes.push(endNode);
	        }
	        
          	jparent=$_(node);
		    while(jparent[0].nodeType!=1)
		    	jparent=jparent.parent();
		    if(! jparent.is(":visible"))
		    {
		    	jparent=jparent.clone();
		    	$_("#hidden_content").append(jparent);
		    	jparent.show();
		    }
		    elementlist.push(jparent);
        }
        else
        {
        	// hit at the beginning. nodeText = remaining portion after this hit.
        	middlebit = node;
        	node = middlebit.splitText(pat.length);
	        var middleclone = middlebit.cloneNode(true);
	        decorator.appendChild(middleclone);
	        middlebit.parentNode.replaceChild(decorator, middlebit);
	        nodeText = node.data.toUpperCase();
	       // var parentel= middlebit.parentNode;
          	jparent=$_(node);
		    while(jparent[0].nodeType!=1)
		    	jparent=jparent.parent();
		    if(! jparent.is(":visible"))
		    {
		    	jparent=jparent.clone();
		    	$_("#hidden_content").append(jparent);
		    	jparent.show();
		    }
		    elementlist.push(jparent);
        }
//        if (decorator.parentNode.tagName == 'A')
 //       	$_(decorator.parentNode).click(false); // disable click handler on parent if its a link, otherwise it puts the page in a state that we can't do ajax calls for getting hit details
        // http://stackoverflow.com/questions/4589964/jquery-disable-click
        if (anchor != null)
        	decorator.parentNode.insertBefore(anchor, decorator);
        
      //  LOG ('nodes twiddled, Node text is now: ' + node.data);
	} // end for
    LOG((new Date().getTime() - decorate_node_start_millis) + 'ms for ' + n_pats_hit + ' pattern hits in node with text length ' + nodeTextLength + ': ' + nodeTextSample);

    if (newNodes.length > 0)
    	LOG (newNodes.length + " new nodes after decoration done");
    // this node done, handle any news nodes created on the way
    for (var x = 0; x < newNodes.length; x++)
    	decorateTextNode(newNodes[x], hits, hit_pats, anchors_entered); 
    } catch(e) { 
    	LOG("Exception decorating text node: " + e); 
    }
}

// complicated way of getting an event target that is browser-portable
// http://www.quirksmode.org/js/events_properties.html
function getTarget(e) {
    var targ;
    if (!e) e = window.event;
    if (e.target) targ = e.target;
    else if (e.srcElement) targ = e.srcElement;
    if (targ.nodeType == 3) // defeat Safari bug
        targ = targ.parentNode;
    return targ;
}

function voteDown(o) {
    var value = o.getAttribute('value');
	$_(findParentWithSelector(o, 'li')).hide(); // hide this list item from the callout
	var cancel_url = window.MUSE_URL + '/ajax/downvote.jsp';

    // remove existing highlights on current page
    $jqs = $_('.muse-highlight');
    $jqs = $jqs.filter(function() { return $_(this).text().toLowerCase() === value;});
    $jqs.removeClass('muse-highlight');
    $jqs = $_('.soft-highlight');
    $jqs = $jqs.filter(function() { return $_(this).text().toLowerCase() === value;});
    $jqs.removeClass('muse-soft-highlight');
	
    var encoded_url = encodeURI(document.URL);	
	$_.ajax(
			{url: cancel_url, 
			type: 'POST',
			data: 'term=' + value +"&url="+encoded_url, 
			beforeSend: function(xhr) { xhr.withCredentials = true;  },
			xhrFields: { withCredentials: true},
			crossDomain: true,
			success : function(response) { LOG ('voted down term: ' + value); },
			error: function(response) { LOG ('WARNING: error voting down term: ' + value); }
			});
}

function voteUp(o) {
    var value = o.getAttribute('value');
	var scoring_url = window.MUSE_URL + '/ajax/upvote.jsp';
	$_(findParentWithSelector(o, '.termMenu')).hide(); // make the term menu go away upon click.
	
    var encoded_url=encodeURI(document.URL);	
    
	$_.ajax(
			{url: scoring_url, 
			type: 'POST',
			data: 'term=' + value +"&url=" + encoded_url + "&totalcount=" + $_(".musecheckbox").length, 
			beforeSend: function(xhr) { xhr.withCredentials = true;  },
			xhrFields: { withCredentials: true},
			crossDomain: true,
			success : function(response) { LOG ('voted up term: ' + value); },
			error: function(response) { LOG ('WARNING: error voting down term: ' + value); }
			});
}

// is this a hit we're not really confident of? 
function sketchy_hit (h) {	return h.score < 5.0; }

// a replacement for .closest because we can't rely on all sites having a version of jquery that supports it
var findParentWithSelector = function(node, selector) {
	while (node != null)
	{
		if ($_(node).is(selector))
			return node;
		node = node.parentNode;
	}
	//LOG ('sorry failed to find node');
	return null;
};

//a useful debug function: get a string printing all members directly in o
//note: does not actually print anything, just returns a string
//print_supertype_fields is off by default
unsafeWindow.dump_obj = function (o, print_supertype_fields) {
	if (typeof(o) === 'undefined')
		return 'undefined';
	if (typeof(print_supertype_fields) === 'undefined')
		print_supertype_fields = false;

	var functions = [];

	var s = 'typeof=' + typeof(o) + ' ';
	if (o.constructor.name) // often the constructor name is empty because it is an anonymous function; print it only if non-empty
		s += ' constructor=' + o.constructor.name + ' ';
	for (var f in o)
	{
		try {
			if (!print_supertype_fields && !o.hasOwnProperty(f)) // only print properties directly attached this object, not fields in its ancestors
				continue;
			if (typeof(o[f]) === 'function')
				functions.push (f); // just write out "function" for functions
			else if (typeof(o[f]) === 'string') {
				s += f + "=" + Util.ellipsize(o[f]) + ' '; // otherwise write out the value
			}
			else  {
				s += f + "=" + o[f] + ' '; // otherwise write out the value
			}
		} catch (e) {
			LOG ('exception trying to dump object field ' + f + ':' + e);
		}
	}
	
	if (functions.length > 0)
		s += functions.length + ' function(s): {';
	for (var i = 0; i < functions.length; i++)
	{
		s += functions[i];
		if (i < functions.length-1)
			s += ' ';
	}
	if (functions.length > 0)
		s += '}';
	
	return s;
};

var handleTermMenu = function(event, visible) {
    try { 
    	$li = $_(findParentWithSelector(event.target, 'LI'));
    	var $menu = $_('.termMenu', $li);
    	// LOG ('handletermmenu for ' + event.target + ' visible = ' + visible + " " + $menu.length);
    	window.setTimeout(function() { $menu.css ('display', visible ? 'inline':'none'); }, 100);
	} catch (e) { LOG('exception with event ' + $_(event.target).html() + ' ' + e); }
};

// when this is called, hit_details.messages must be populated
var open_popup = function(hit_details) 
{
    var popup = '';
    // show up to 6 messages
    for (var m = 0; m < hit_details.messages.length && m < 6; m++) {
    	var mesg = hit_details.messages[m];
    	var plus_mesg = (mesg.to.length <= 1) ? '': ' (+' + (mesg.to.length-1) + ')';
        var title = (typeof mesg.contents != 'undefined') ? mesg.contents : '';
        title = title.replace(/"/g, "&quot;");
        title = title.replace(/\n/g, " ");
        popup += "<p style=\"color:white;\" title=\"" + title + "\">" + mesg.date + " | "
        	   + "From: " + ((mesg.from.length > 0) ? mesg.from[0].email : '???')
        	   + " | To: " + ((mesg.to.length > 0) ? mesg.to[0].email + plus_mesg : '???')
               + "<br/>Subject: " + ellipsize(hit_details.messages[m].subject, 46);
        	   + "</p>";
    }
    popup += '<p><a href=\'' + hit_details.url + '\' target="_">View ' + hit_details.nMessages + ' message' + (hit_details.nMessages > 1 ? 's':'') + '</a><hr/>';

    $_('#muse-pointers').html(popup);
    LOG ('pop html is ' + popup);
    $_('#muse-details').attr('title', hit_details.text);  // this title is not showing up, but if we don't have this, it says "undefined"...
    $_('#muse-details').attr('alt', hit_details.text);  // this title is not showing up, but if we don't have this, it says "undefined"...
    $_('#muse-pointers').attr('title', hit_details.text); 
    $_('#muse-clicker').attr('title', '&nbsp;&nbsp;' + hit_details.text);
    $_('#muse-clicker').click();
    LOG ("click sent to open muse hits: " + $_('#muse-clicker').length);
};

var HIT_CLICKED = function(e, hit_details) {
    if (e.shiftKey)
        return true;
    
    LOG ("showing messages in popup, hit_details = " + hit_details);
    // if hit_details.messages are available, just go with that
    if (typeof hit_details.messages != 'undefined' && hit_details.messages.length > 0)
    	open_popup(hit_details);
    else
    {
    	var jqXHR = $_.ajax({
			url: window.MUSE_URL + '/ajax/getHitTermDetails.jsp', 
			type: 'POST',
			data: 'term=' + hit_details.text +"&pageScore="+hit_details.pageScore, 
			dataType: 'text',
			beforeSend: function(xhr) { xhr.withCredentials = true;  },
			xhrFields: { withCredentials: true},
			crossDomain: true,
			success: function(response) {
			    var muse_response = trim(response);
			    var new_details = null;
				try { new_details = eval('(' + muse_response + ')'); } 
				catch (e) { LOG('error in evaluating muse response for term details'); return; }
				hit_details.messages = new_details.result.messages;
				hit_details.nMessages = new_details.result.nMessages;
		    	open_popup(hit_details);
			},
			error: function(jqXHR, textStatus, errorThrown) {
				alert ('getting term details failed : '  + textStatus);
				LOG(dump_obj(jqXHR));
				LOG(errorThrown);
			}
		});
		LOG(dump_obj(jqXHR));
    }
    
//    if ($_(e).stopPropagation)
//   {
//  	LOG ('stopping propagation');
// 	    $_(e).stopPropagation();
//   }
    if (e.stopPropagation)
    {
    	LOG ('stopping event propagation');
    	e.stopPropagation();
    }
    if (e.preventDefault)
    {
		LOG ('preventing event default');
    	e.preventDefault();
    }
    
    return false;
};

var muse_show_filter = function() {
	$_.ajaxSetup({beforeSend: function(xhr) { xhr.withCredentials = true;  },
	xhrFields: { withCredentials: true},
	crossDomain: true});
	
	$('.muse-overlay').height($(document).height());		
    $('.muse-overlay').width($(document).width());		
    $('.muse-overlay').show();		

    $_('#filter-div').css ('left', (window.innerWidth/2)- 100+"px"); // the total width is 200px
    $_('#filter-div').css ('top', (window.innerHeight/2)- 75+"px"); // the total width is 200px
    $_('#filter-div').css ('width', '15px');
    $_('#filter-div').css ('height', '15px');
    $_('#filter-div').html('<img id="saveSessionSpinner" style="position:relative;top:0px;left:0px" width="15" src="images/spinner.gif"/>');
	$_('#filter-div').show();

//	$_('#filter-div').load(window.MUSE_URL + '/filter.jsp?muse_url=' + window.MUSE_URL + '/', function() {
	$_('#filter-div').load(window.MUSE_URL + '/filter.jsp', function() {
		$_.ajaxSetup({});
		$_('[id^=facet-name-]').hover(function (){
		    $_(this).css("text-decoration", "underline");
	    },function(){
	    	$_(this).css("text-decoration", "none");
	    });

		$_('document').keydown(function(event) {
			alert (event.which);
		});
	});
};

function create_callout(hits) {
	function html_for_terms(hits)
	{
		var n_hits = 0;
		var callout_content = '<ul style="margin:0px;padding:0px">';
		for (var i = 0; i < hits.results.length; i++) {
			// term did not hit
			if (hits.results[i].indexScore == 0)
				continue;

			// if the term appears in the document's url, kill it... e.g. stanford on stanforddaily.com
			if (document.URL.toUpperCase().indexOf(hits.results[i].text.toUpperCase()) >= 0) 
				continue;

			// no entry in callout for sketchy hits
			if (sketchy_hit(hits.results[i]))
				continue;

			n_hits++;
			var title = '';
			if (hits.results[i].type)
				title += 'Type ' + hits.results[i] + ' ';
			title += 'Score on page: ' + hits.results[i].pageScore + '; in archive: ' + hits.results[i].nMessages; 
			
			// callout_content may get large, therefore avoid doing += on it directly, do chota-mota += on x instead
			var x = '<li style="display:inline;position:relative" class="term">';
			x += '<span title="' + title + '"> ';
			x += '<span style="display:none;" class="termMenu">';
//			callout_content += '<span title="' + title + '">' + hits.results[i].text + '</span>\n';
			x += '<img style="display:inline;width:30px;margin-left:2px;margin-right:2px;" class="voteDown" width="30px" value="' + hits.results[i].text + '" src="' + window.MUSE_URL + '/images/thumbs-down.png"/>';
			x += '<img style="display:inline;width:30px;margin-left:2px;margin-right:2px;" class="voteUp" width="30px" value="' + hits.results[i].text + '" src="' + window.MUSE_URL + '/images/thumbs-up.png"/>';
			x += '</span>\n'; // termMenu

			x += '</span>\n';
			var term = hits.results[i].text;
			var name = term.replace (/ /g, '_').toLowerCase() + "_anchor";
			// no need to lowercase too, as term is already lower case

			x += '<a href="#' + name + '">' + term + '</a>\n';
//			callout_content += '<span title="' + title + '">' + hits.results[i].text + '</span>\n';
			x += '</li>\n';
			
			callout_content += x;
		}
		// callout_content += '</ul>';
		callout_content += '</ul>';

		LOG (n_hits + " item(s) in callout");	
		return {callout_content:callout_content, n_items:n_hits};
	}
	
	var callout_start_millis = new Date().getTime();
	// start by hiding whatever existed
	$_("#callout").hide();

	// add top level div and menu
	var $newdiv1 = $_('<div id="calloutparent"/>');
	var $filter = $_('<div class="muse-overlay"></div><div id="filter-div" style="position:fixed;left;0px;top:0px;background-color:white;z-index:10000;display:none"></div>');

	var $menu = $_('<div id="callout-menu"><img width="15" src="' + window.MUSE_URL + '/images/gears-icon-gold.png"/></div>');
	var $submenu = $_('<div id="settings_dropdown" style="display:none" class="dropdown-menu"><div class="inner">'
			+ '<span id="settings_menuitem" title="Click to open lens settings" class="menuitem">Settings</span><br/>'
			+ '<span id="filter_menuitem" title="Click to open lens settings" class="menuitem">Filter</span><br/>'
			+ '<span id="resize_callout_menuitem" title="Click to resize" class="menuitem">Resize</span><br/>'
			+ '<span id="help_menuitem" title="Open help" class="menuitem">Help</span><br/></div></div>');
	$menu.append($submenu);
	$newdiv1.append($filter);
	$newdiv1.append($menu);

	var $newdivchild = $_('<div id="callout"/>');
	$newdiv1.append($newdivchild);
	
	var x = html_for_terms(hits);
	var callout_content = x.callout_content;
	
//	show callout_content and context button and inject prettyphoto and css only if there are actual hits
	if (x.n_items > 0) {
		$_('#context_button').click(function(evt) {
			if ($_('#calloutparent').is(':visible')) {
				$_('#calloutparent').fadeOut();
				$_('#context_button span').html('Show');
			} else {
				$_('#calloutparent').fadeIn();
				$_('#context_button span').html('Hide');    			
			}
		}); 

		$_('body').append($newdiv1);

		$_("#callout").html(callout_content);
		$_("#callout").fadeIn('slow');
		$_("#calloutparent").css("max-height", 20 * hits.callout_lines);

		LOG ('# terms in callout ' + $_('#calloutparent li.term').length);
		
		// show vote meny on hover
		$_('#calloutparent li.term').hover(function(e) { handleTermMenu(e, true); }, function(e) { handleTermMenu(e, false); });

		// settings menu
		var muse_do_settings = function() {
			$_('#settings_dropdown').hide();
			window.open(window.MUSE_URL + '/lens-settings.jsp'); 
		};
		var muse_do_resize_callout = function() { 
			$_('#settings_dropdown').hide();
			var x = prompt('Maximum number of rows in the listing at the bottom'); 
			if (!x)
				return;
			LOG ('setting max height of callout to ' + 20*x + ' px');
			if (x >= 1)
				$_("#calloutparent").css("max-height", 20*x);
		};
		var muse_do_help = function() { window.open(window.MUSE_URL + '/help#lens'); };
		var muse_toggle_settings_menu = function() { $_('#settings_dropdown').slideToggle('fast'); };

		$_('#callout-menu').click(muse_toggle_settings_menu);
		$_('#settings_menuitem').click(muse_do_settings);
		$_('#filter_menuitem').click(muse_show_filter);
		$_('#resize_callout_menuitem').click(muse_do_resize_callout);
		$_('#help_menuitem').click(muse_do_help);
		var now = new Date().getTime();
		LOG ('callout computed in ' + (now - callout_start_millis) + 'ms, elapsed time on page ' + (now - startTime) + 'ms');
//		set up handlers for the up/down votes
		var ups = document.getElementsByClassName('voteUp');
		for (var i = 0; i < ups.length; i++)
			ups[i].addEventListener("click", function(e){ voteUp(getTarget(e));}, false);
		var downs = document.getElementsByClassName('voteDown');
		for (var i = 0; i < downs.length; i++)
			downs[i].addEventListener("click", function(e){ voteDown(getTarget(e));}, false);
	}
}


function report_error(error_message) {
    var $newdiv1 = $_('<div id="calloutparent"/>');
    var $newdivchild = $_('<div id="callout"/>');
    $newdiv1.append($newdivchild);
    $_('body').append($newdiv1);
    $_("#callout").html (error_message);
    $_("#callout").fadeIn('slow');
    $_("#calloutparent").fadeIn('slow');
	$_('#muse-status').fadeOut('slow');

    window.setTimeout (function() { $_('#calloutparent').fadeOut('slow'); }, 5000); // fade out in 5 seconds
}

function decoratePage(hits) {
	LOG ('decorating pages: ' + muse_response);
	
	if (!hits || typeof hits.results == 'undefined') {
        if (typeof hits.displayError !== 'undefined' && hits.displayError && hits.displayError.length > 0) {
		    var $newdiv1 = $_('<div id="calloutparent"/>');
		    var $newdivchild = $_('<div id="callout"/>');
		    $newdiv1.append($newdivchild);
	        $_('body').append($newdiv1);
		    $_("#callout").html (hits.displayError); 
		    $_("#callout").fadeIn('slow');
		    $_("#calloutparent").fadeIn('slow');
			$_("#muse-status").fadeOut('slow');
			window.setTimeout(function() { $_('#calloutparent').fadeOut('slow');}, 5000); // fade out in 5 secs
			report_error('muse returned an error: ' + hits.displayError);
        }
		return; // do nothing
	}

	var decorate_start_millis = new Date().getTime();
	LOG (hits.results.length + ' names on page');

    // sort results so that longer phrases are before shorter ones. 
    // ensures that superstrings are hilited in preference to substrings
    // e.g. Texas Rangers should be before Texas, so the whole phrase gets hilited
	
	// But, this doesn't always work well. e.g. on large pages with a lot of terms in the callout, unimportant but long names tend to appear first.
	//LOG ('sorting hits by text length');
    //var hit_results = hits.results;
    //hit_results.sort (function(a, b) { return b.text.length - a.text.length;});	

	// disabling callouts for epadd
//	create_callout(hits);

	var decorate_text_nodes_start_millis = new Date().getTime();
	var textNodes = textNodesOnPage; // as an optimization, we'll reuse the nodes we found while extracting the text
	LOG ('decorating ' + textNodes.length + ' text nodes on page with ' + hits.results.length + ' hits');
	
	// hit_pats[i] is just a pre-computed uppercase array of hits[i].text so we don't have to do it each time
    var hit_pats = [];
    for (var hit = 0; hit < hits.results.length; hit++)
    {
    	hit_pats[hit] = hits.results[hit].text.toUpperCase();
        if (document.URL.toUpperCase().indexOf(hit_pats[hit]) >= 0) // if the term appears in the document's url, kill it... e.g. stanford on stanforddaily.com, india on timesofindia.com
        	hit_pats[hit] = null; 
        if (IS_LARGE_PAGE && hits.results[hit].nMessages == 0)
    		hit_pats[hit] = null; // nuke out lookup for large pages if there are no message hits. we'll lose the red underlining, but that's ok.  
    }

	var anchors_entered = [];
	for (var x = 0; x < textNodes.length; x++)
		if (textNodes[x] != null)
		{
			try {
				decorateTextNode(textNodes[x], hits, hit_pats, anchors_entered);
			} catch(err) { alert(err); }
		}
	LOG (n_hilights + " names hilited");
	var now_millis = new Date().getTime();
	LOG ('time to decorate text nodes = ' + (now_millis-decorate_text_nodes_start_millis) + 'ms');
	
    // ensure margin space for callout at the bottom. otherwise if margin is 0, a link right at the bottom of the page becomes unreachable.
    if ($_('body').css('margin-bottom') == '0px')
        $_('body').css('margin-bottom', '60px'); 

    $_('#muse-status').fadeOut('slow');

	LOG ('total time to decorate page = ' + (now_millis-decorate_start_millis) + 'ms');
	LOG ('total elapsed time on page = ' + (now_millis-startTime) + 'ms');
	start_prettyphoto();
}

}; // end main

init();

})(); // end of anonymous wrapper function 

/*
	function select_menuitems($jelm) {
//		LOG("number of anchor tags:"+ $('a',$jelm).length);
	        try{
			//LOG(" select_contentNodes");
			// if($jelm[0].nodeType!=1)
			//	select_contentNodes($jelm.parent());
			
			if($jelm[0].nodeType==1)
			{
				var offset = $jelm.offset();
				if((($jelm.height() < 200 && $jelm.width()< 1000)||($jelm.height() < 1000 && $jelm.width()< 200))&& offset.left<300 && offset.top<600)
				{
					$jelm_parent=$jelm.parent();
					
					
					//LOG("number of anchor tags:"+ $jelm[0].getElementsByTagName('a').length);
					if($_('a',$jelm).length<=25 && $_('a',$jelm).length > 6)
					{
					       //&& offset.left<600 && offset.top<600
						LOG($jelm.html());
						
						menuitems.push($jelm);	
					}
					if($_('a',$jelm).length>50)
					{
						$jelm.children().each(function(i) { 
		    					select_menuitems($_(this));
						});
					}	
				}
				else
				{
					$jelm.children().each(function(i) { 
		    				select_menuitems($_(this));
					});
				}
				
				
			}
			else
			{
				$jelm.children().each(function(i) { 
		    				select_menuitems($_(this));
				});
			}
			
		
	        }
	        catch(err)
		{
			LOG("error**************"+err);
		}	
	}

 */
