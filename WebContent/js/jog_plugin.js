// terminology: a section has documents, documents may consist of multiple pages
// pdf's: conf. sections are sections, each paper/PDF is one
// emails: sections are emails a multi-doc, i.e. all emails in a month (or year). each email is a doc with just 1 page
// only "selected pages" are shown

// global vars: currentPage, totalPages
// page contents are currently read with $(settings.page_selector)[selectedPageNum[page]].innerHTML;

// "use strict";

(function( $ ) {
  var PDF_ICON_WIDTH = 30; // in pixels

  $.fn.jog = function(options) {
	  
	// debug func: print all members directly in o (and not its supertypes)
	  var dump_obj = function (o) {
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
				  else
				  {
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

	function LOG(s, p) {
		if (settings.logger) {
			//if (typeof p == 'undefined')
			//	p = true;
			settings.logger(s, p);
		}
	}
	// LOG ('jog settings = ' + dump_obj(settings));

      // inject jog_div
      var settings = {
          page_selector: '.page',
          document_selector: '.document',
          section_selector: '.section',
          // sound_selector
          jog_content_frame_selector: '#jog_contents',
          reel_prefix  : 'images/sleekknob',
          dynamic	   : false,
          width		   : 250,
          page_change_callback: null,
          page_load_callback: null,
          paging_info: null,
          logger: null,
          text_css: 'background-color: black; color: white; font-size: 20pt;border-radius:5px',
          show_count: true,
          disabled: false /* sometimes its just disabled */
      };
      $.extend(settings, options);

      var jog_div = '<div id="jog_div" style="position:fixed;display:none;top:0px;left:0px;opacity:0.9;z-index:1000000000">'
    			+ '<table style="background-color:transparent;border:0px;"><tr><td>	<img id="jog_img" src="' + settings.reel_prefix + '0.png' + '" width="' + settings.width + '"></img>	</td>';
        if (settings.show_count)
            jog_div += '<td valign="middle">&nbsp;&nbsp 	<span id="jog_text" style="' + settings.text_css + '">&nbsp;</span><br/>	</td>';
        jog_div += '</tr></table></div>';
    	$('body').append(jog_div);


	LOG ('done appending div');

////////////////////jog stuff //////////////////////

function Jog(settings, height, width, trackWidth, forward_handler, backward_handler)  {
	this.jogTrackWidth = trackWidth;
	this.x = 0;
	this.y = 0;
	this.enabled = false;
	this.height = height;
	this.width = width;
	this.forward = this.forward_func = forward_handler; // forward/backward_func for backward compat.
	this.backward = this.backward_func = backward_handler;
	this.cursorKeysDisabled = false;

	if (!settings.disabled) {
		// var x = this.createMouseDownHandler();
 //       $('#jog_img').mousedown(x); // same click handler on jog itself needed also to dismiss

        var y = this.createClickHandler();
		// $(settings.jog_content_frame_selector).click(x);
		$(settings.jog_content_frame_selector).click(y);
		$('#jog_img').click(y); // same click handler on jog itself needed also
		
        var z = this.createMouseMoveHandler();
        // for ios, touchmove is fired instead of mousemove. thankfully, not both
		$('#jog_img').mousemove(z);		
		$('#jog_img').bind('touchmove', z); 

		LOG ('mouse move setup jog width is ' + width);
	}
	else
		LOG ('jog dial disabled!!');
}

Jog.prototype.showJog = function (mouseX, mouseY, x, y)  // mouseX/Y is the actual mouse pos, for computing current compartment. x,y is jog center
{
	if (settings.disabled) {
		LOG ('jog dial is disabled!');
		return; // just never show it
	} else {
		LOG ('jog dial is enabled!');
	}
	
	var top = y-(this.height/2);
	var left = x-(this.width/2);
	LOG ('jog center is ' + x + ',' + y + ' left, top=' + left + ',' + top + ' h=' + this.height + ' w=' + this.width);

    var c = this.getCompartment(mouseX, mouseY); 
	c = (c+7)%8;

	
	// we explicitly change the image
	var img = settings.reel_prefix + c + '.png';
	$('#jog_img').attr('src', img);
	
	/* could use css3 rotation
	var a = this.getAngle(mouseX, mouseY, x, y);
	LOG ('angle = ' + a);
	
 	// could try CSS3 on supported browsers:
	a = parseInt(a);
	$('#jog_img').css('-moz-transform', 'rotate(' + a + 'deg)');
	$('#jog_img').css('-webkit-transform', 'rotate(' + a + 'deg)');
	$('#jog_img').css('-ms-transform', 'rotate(' + a + 'deg)');
	$('#jog_img').css('-transform', 'rotate(' + a + 'deg)');
	*/

	$('#jog_div').css('top', top); 
	$('#jog_div').css('left', left);

	$('#jog_div').fadeIn('fast');
	this.x = x;
	this.y = y;
	this.enabled = true;
	this.prevCompartment = -1;
	this.prevX = x;
	this.prevY = y;
};

Jog.prototype.hideJog = function ()
{
	$('#jog_div').fadeOut('fast');
	this.enabled = false;
};

Jog.prototype.createMouseMoveHandler = function () {
	var myJog = this;
	return function(event) {
        // this code tries to track the jog to the cursor location
        function moveJogDynamic()
        {
            function initEpoch(x, y, timeStamp) {
                var epoch = myJog.epoch;
                if (epoch == null)
                    myJog.epoch = epoch = {};
                epoch.timeStamp = timeStamp;
                epoch.startX = event.lastX = x;
                epoch.startY = event.lastY = y;
                epoch.distanceFromStart = epoch.distanceTravelled = 0;
                epoch.minX = epoch.minY = 10000;
                epoch.maxX = epoch.maxY = -1;

            }

            var epoch = myJog.epoch;
            if (typeof epoch == 'undefined' || (event.timeStamp - epoch.timeStamp > 1000))
            {
                initEpoch(event);
            } else {
                // LOG ('timestamp delta = ' + (event.timeStamp - epoch.timeStamp));
                var deltaX = Math.abs(currentX - epoch.startX);
                var deltaY = Math.abs(currentY - epoch.startY);
                epoch.distanceFromStart = Math.sqrt (deltaX * deltaX + deltaY * deltaY);

                var deltaX = Math.abs(currentX - epoch.lastX);
                var deltaY = Math.abs(currentY - epoch.lastY);
                var distanceTravelled = Math.sqrt (deltaX * deltaX + deltaY * deltaY);
                epoch.distanceTravelled += distanceTravelled;

                if (epoch.minX > currentX)
                    epoch.minX = currentX;
                if (epoch.minY > currentY)
                    epoch.minY = currentY;
                if (epoch.maxX < currentX)
                    epoch.maxX = currentX;
                if (epoch.maxY < currentY)
                    epoch.maxY = currentY;
                epoch.lastX = currentX;
                epoch.lastY = currentY;
                if (epoch.distanceFromStart < 40 && epoch.distanceTravelled > 100 && epoch.distanceTravelled > 5 * epoch.distanceFromStart)
                {
                    // need to readjust
                    newCenterX = (epoch.maxX + epoch.minX)/2;
                    newCenterY = (epoch.maxY + epoch.minY)/2;
                    LOG ("OK... RECENTERING! + " + newCenterX + "," + newCenterY);
                    initEpoch(currentX, currentY, event.timeStamp);
                    $('#jog_div').animate({'top': newCenterY-(myJog.height/2)}, {duration:100}); // -50 to make the mouse start at the ready-to-go point on the jog
                    $('#jog_div').animate({'left': newCenterX-(myJog.width/2)}, {duration:100});

                    // alert ('adjusting center to ' + newCenterX + ", " + newCenterY);
                }
            }
        }

		// jogInactivityTimer.reset();
		if (settings.dynamic)
			moveJogDynamic();

		var currentX, currentY;
		if (event.type === 'mousemove') {
			currentX = event.clientX; currentY = event.clientY; // clientX/Y are offset w.r.t. client window. http://www.gtalbot.org/DHTMLSection/PositioningEventOffsetXY.html
		}
		else {
			// must be a touchmove
			currentX = event.originalEvent.targetTouches[0].pageX;
			currentY = event.originalEvent.targetTouches[0].pageY;
			LOG ('event.type = ' + event.type + ' mousemove handler called with current X ' + currentX + ' current Y = ' + currentY);
		}

	    myJog.currentCompartment = myJog.getCompartment(currentX, currentY);
		var img = settings.reel_prefix + ((myJog.currentCompartment+7)%8) + '.png';
		$('#jog_img').attr('src', img);
	    /*
	    var a = myJog.getAngle(currentX, currentY, myJog.x, myJog.y);
	    a = parseInt(-a+90); // 0 angle is 90 deg for the image rot
		LOG ('angle = ' + a);

		// making a too smooth doesn't necessarily feel right
		// add a little bit of discretization to a
		// a = parseInt(a/22)*22;
		
		$('#jog_img').css('-moz-transform', 'rotate(' + a + 'deg)');
		$('#jog_img').css('-webkit-transform', 'rotate(' + a + 'deg)');
		$('#jog_img').css('-transform', 'rotate(' + a + 'deg)');
		*/

	    if (myJog.currentCompartment == myJog.prevCompartment)
	    	return;
//	    LOG('new compartment: ' + myJog.currentCompartment + ' prev = ' + myJog.prevCompartment);

	    if (myJog.prevCompartment == -1)
	    {
	    	myJog.prevCompartment = myJog.currentCompartment;
	    	return;
	    }

//	    LOG('ok, going to move');
	    if (myJog.currentCompartment == ((myJog.prevCompartment+1) % 8)) {
	    	LOG ('moving forward: compartment ' + myJog.prevCompartment + ' -> ' + myJog.currentCompartment);
	    	doPageForward(); // myJog.forward_handler();
	    } else if (myJog.currentCompartment == ((myJog.prevCompartment+7) % 8) ) {
	    	LOG ('moving backward: compartment ' + myJog.prevCompartment + ' -> ' + myJog.currentCompartment);
	    	doPageBackward(); // myJog.backward_handler();
		}
	    myJog.prevCompartment = myJog.currentCompartment;	
        event.preventDefault();
        if (event.stopPropagation)
            event.stopPropagation();
        return false;
	};
};

Jog.prototype.getCompartment = function(x, y) {
    var onLeft = false, lower = false, closerToYAxis = false;
    if (x < this.x)
        onLeft = true;
    if (y > this.y)
        lower = true;

    var correctedX = this.x;
    var correctedY = this.y;
    var xdiff = Math.abs(x - correctedX);
    var ydiff = Math.abs(y - correctedY);

  //  LOG('x = ' + x + ' this.x = ' + this.x + ' y = ' + y + ' this.y = ' + this.y + ' lower = ' + lower + ' onleft = ' + onLeft);
  //  LOG('xdiff = ' + xdiff + ' ydiff = ' + ydiff);
  //  LOG('lower = ' + lower + ' onLeft = ' + onLeft + ' closer to y = ' + closerToYAxis);

    if (ydiff > xdiff)
    	closerToYAxis = true;

    // direction encoding: NNE = 0, and hten clockwise till NNW = 7.
    var result = (closerToYAxis) ? 0 : 1;
    if (lower)
    	result = 3 - result;
	if (onLeft)
		result = 7 - result;
	
	/*
	LOG ('result compartment = ' + result);
	$('#jog_div').css('top', (lower?y+50:y-50)); // -50 to make the mouse start at the ready-to-go point on the jog
	$('#jog_div').css('left', (onLeft?x+50:x-50));

	this.x = x;
	this.y = y;
	 */
    currentCompartment = result;
	return result;
};

Jog.prototype.getAngle = function(x, y, ox, oy) {
	LOG ('getting angle for (' + x + ',' + y + ') wrt to (' + ox + ',' + oy + ')');
	var denom = (x - ox);
	var numer = (oy -y); // y axis is upside down when considering pixels (y - oy);
	if (denom === 0.0)
		denom = 0.001;

	var x = numer/denom;
	var pos = true, m90 = false, neg = false;
	if (Math.abs(x) > 1)
	{
		x = 1/x;
		m90 = true; // have to 90 - result
	}
	if (x < 0)
	{
		x = Math.abs(x);
		neg = true; // have to 180 - result
	}

	// arctan(t) = 1 - 1/3t^3 + 1/5t^5 etc c.f. http://www.mathstat.concordia.ca/faculty/rhall/mc/arctan.pdfhttp://www.mathstat.concordia.ca/faculty/rhall/mc/arctan.pdf
	var sum = 0;
	for (var i = 1; i < 40; i+=2)
	{
		var incr = (1.0/i) * Math.pow (x, i);
		if (pos) sum += incr; else sum -= incr;
		pos = !pos;
	}

	var degrees = sum * 180/3.14;
	if (m90)
		degrees = 90 - degrees;
	if (neg)
		degrees = 180 - degrees;
	if (numer < 0)
		degrees = degrees + 180;
	return degrees;
};

// not used currently: boundary condition adjustments
Jog.prototype.adjustPos = function(o)
{
    // TOFIX:
    var W = 1000; // topPage.tb.width;
    var H = 600; // topPage.tb.height;
    //Boundary condition adjustments
    if(o.x + (o.width / 2) > W)
    {
        o.x  = W - (o.width / 2) - 10;
    }
    if(o.x - (o.width / 2) < 0)
    {
        o.x  = (o.width / 2) + 10;
    }
    if(o.y + (o.height / 2) > H)
    {
        o.y  = H - (o.height / 2) - 10;
    }
    if(o.y - (o.height / 2) < 0)
    {
        o.y  = (o.height / 2) + 10;
    }
};

Jog.prototype.enableJog = function(mouseX, mouseY)
{
    this.x = mouseX;
    this.y = mouseY + (this.width / 2) - (this.jogTrackWidth / 2) - 30;
    this.enabled = true;
    this.showJog(mouseX, mouseY, this.x, this.y);
};

LOG ('setting click handler');

Jog.prototype.getEventTarget = function(e) {  
    e = e || window.event;
    return e.target || e.srcElement;  
};

Jog.prototype.createMouseDownHandler = function() {
	var myJog = this;

	return function(evt)
	{
		// LOG ('recorded down: ' + evt.clientX + ',' + evt.clientY);
		myJog.mouseDownX = evt.clientX;
		myJog.mouseDownY = evt.clientY;
	};
};

Jog.prototype.createClickHandler = function() {
    
	LOG ('click for jog contents');
	var myJog = this;
	return function (evt)
	{
		// ignore if it selected text
		if (getSelection().toString()) { return; }
		
	LOG ('at click ' + dump_obj(evt));
        var currentX = evt.clientX, currentY = evt.clientY;

		/*
		if (typeof myJog.mouseDownX !== 'undefined') {
			var manhattan_dist = Math.abs(evt.clientX - myJog.mouseDownX) + Math.abs(evt.clientY - myJog.mouseDownY);
			LOG ('man dist = ' + manhattan_dist);
			if (manhattan_dist > 15)
				return; // do nothing, it was not a single point click
		}
		 */
		var target = myJog.getEventTarget(evt);
		//alert (target.tagName.toLowerCase());
		// nojog is a class that should be assigned when we don't want the jog to popup. e.g. the More button for long messages in Muse.
		// also disabled on tagName=img, because the only reason for images is attachments.
		// if (target.tagName.toLowerCase() == 'a' || (target.id !== 'jog_img' && target.tagName.toLowerCase() == 'img') || target.className == 'nojog') 
        if (target.onclick != null || target.tagName.toLowerCase() == 'a' || target.tagName.toLowerCase() == 'input' || (target.id !== 'jog_img' && target.tagName.toLowerCase() == 'img') || ($(target).closest('.nojog').length >= 1) || $(target).is('.nojog'))
		{
            LOG ('not registering click because the target already takes click or it is an a or span ' + target.tagName + ' ' + target.className);
			return; // do nothing if the original click was on an 'a'
		}
		// note, sometimes we have spans for search terms or sentiments, that also act as links. should suppress jogs for those also
		// could disable when editing_comment - currently comment's textarea returns false onclick to stop event propagation
//		LOG ('handleclick called');
		if (myJog.enabled) 
		{
			LOG ('Jog dismissed');
			myJog.hideJog();	
		} else {
			LOG ('Jog summoned at ' + currentX + ', ' + currentY);
			myJog.enableJog(currentX, currentY);
		}
		return true;
	};
};

Jog.prototype.enableCursorKeys = function() { this.cursorKeysDisabled = false; }
Jog.prototype.disableCursorKeys = function() { this.cursorKeysDisabled = true;}

	function keypress_handle(event) {
        function scrollSection(increment)
        {
            LOG ('scroll section by ' + increment);
            if (sectionEndPages == null || sectionEndPages.length == 0)
            {
                // we don't have sections. just scroll to first or last page.
                if (increment < 0)
                    setCurrentPage(0);
                else
                    setCurrentPage(pages.length-1);
                showCurrentPage();
                return;
            }

            var section = whichRange(currentPage, sectionEndPages);
            var nextSection = section + increment;
            if (nextSection >= 0 && nextSection < nSections) {
                LOG ('scrolling to section ' + nextSection);
                if (nextSection == 0)
                    setCurrentPage(0);
                else
                    setCurrentPage(sectionEndPages[nextSection-1]+1); // first page of this section is last page of prev. section + 1
            } else {
                if (nextSection < 0)
                    setCurrentPage(0); // just go back to page 0
                else if (nextSection >= nSections)
                    setCurrentPage(pages.length-1); // just go to the last page.
                else
                    LOG ('not possible to scroll to section ' + nextSection);
            }

            showCurrentPage();
        }

        var code;
		var handled = false;
		if (event.keyCode)
		  code = event.keyCode; // keyCode is apparently IE specific
		else
		  code = event.charCode;

		if (event.jog_ignore)
		{
			LOG ('key event ignored by jog');
			return true; // don't handle if we've been told to ignore
		}
		
		if (code == 37 && !jog.cursorKeysDisabled) // left arrow
		{
		  doPageBackward();
		  handled = true;
		} else if (code == 39 && !jog.cursorKeysDisabled) // right arrow
		{
		  doPageForward();
		  handled = true;
		} else if (code == 9) // tab
		{
		  handled = true;
		  if (!event.shiftKey)
			  scrollSection(1);
		  else
			  scrollSection(-1);
		} 

		// see http://www.quirksmode.org/js/events_order.html for event bubbling
		if (handled)
		    return false; // we handled the key, do not propagate it further
		else 
			return true;
	} // end of keypress
	
// returns which section/doc the given page belongs to, given the ending page nums of each section/doc
function whichRange(page, endPageNums)
{
	if (endPageNums == null || endPageNums.length == 0)
		return -1;

	var startPage = 0;
	var endPage = endPageNums[0];
	for (var i = 0; i < endPageNums.length; i++)
	{
		endPage = endPageNums[i];
		if (page >= startPage && page <= endPage)
			return i;
		startPage = endPage+1; // for next iteration
	}
	LOG ('unable to find ' + page + ' in range [0..' + endPage + ']');
	return -1;
}

// update section name for the current page
function updateSectionName(page)
{
	currentSectionName = "Unnamed section";
	var section = whichRange(page, sectionEndPages);
	if (section >= 0)
		currentSectionName = sectionNames[section];
	LOG (' updating to section ' + section + ' name: ' + currentSectionName);
	$('#sectionName').html('&nbsp;' + currentSectionName + '&nbsp;');
}

// sections have names and pages under them
// computers sectionEndPages and sectionNames and nSections
function setupSections()
{
	LOG ('setting up sections');

	sectionEndPages = []; // end page # of sections, inclusive.
	sectionNames = [];
	nSections = 0;
	N_COLS_IN_FRAME = (window.innerWidth > 1600) ? 2 : 1;
	N_ROWS_IN_FRAME = 1;
	N_PAGES_IN_FRAME = N_COLS_IN_FRAME * N_ROWS_IN_FRAME;

	// OVERRIDE
	N_COLS_IN_FRAME = N_PAGES_IN_FRAME = N_ROWS_IN_FRAME = 1;

	// note: we don't support the case where there are no sections, but some pages are not in any section
	var sections = $(settings.section_selector);
	var pagesSoFar = 0;
	if (sections != null)
	{
		sections.each(function (idx) {
			sectionNames[idx] = $(this).attr('name');
			var sectionPages = $(this).find(settings.page_selector);
			if (sectionPages != null)
				pagesSoFar += sectionPages.length;
//			LOG ('section ' + idx + ': #pages = ' + sectionPages.length + ' total pages so far ' + pagesSoFar);
//			LOG ('Pages: ' + sectionPages.length + ' pages in section ' + idx);
			sectionEndPages[idx] = pagesSoFar-1;
		});
	}

	nSections = sectionEndPages.length;
	LOG (nSections + ' sections found with a total of ' + pagesSoFar + ' pages');
}

function setupDocuments()
{
	LOG ('setting up documents');
	docEndPages = []; // end page # of documents, inclusive.
	nDocs = 0;

	var docs = $(settings.document_selector);
	var pagesSoFar = 0;
	if (docs != null)
	{
		for (var i = 0; i < docs.length; i++)
		{
			var t = docs[i];
			var docPages = $(t).find (settings.page_selector);
			if (docPages != null)
				pagesSoFar += docPages.length;
			docEndPages[i] = pagesSoFar-1;
		}
		// .each seems to hit "script stack space quota is exhausted */
		/*
		docs.each(function (idx) {
			var docPages = $(this).find(settings.page_selector);
			if (docPages != null)
			{
				pagesSoFar += docPages.length;
//				LOG ('doc ' + idx + ': #pages = ' + docPages.length + ' total pages so far ' + pagesSoFar);
			}
			docEndPages[idx] = pagesSoFar-1;
		});
		*/
	}
	else
	{
		// no document class, so just assign 1 doc per page
		LOG ('no doc tags, assigning 1 doc per page');
		var pages = $(settings.page_selector);
		if (pages != null)
		{
			for (var i = 0; i < pages.length; i++)
				docEndPages[i] = i;
			/*
			pages.each(function (idx) {
				docEndPages[idx] = idx;
			});
			*/
			pagesSoFar = pages.length;
		}
	}

	nDocs = docEndPages.length;
	LOG (nDocs + ' documents found with a total of ' + pagesSoFar + ' pages');
}


/*
function scrollDocument(increment)
{
	if (docEndPages == null || docEndPages.length == 0)
		return;
	var doc = whichRange(currentPage, docEndPages);
	var nextDoc = doc + increment;
	if (nextDoc >= 0 && nextDoc < nDocs)
	{
		LOG ('scrolling to document ' + nextDoc);
		if (nextDoc == 0)
			setCurrentPage(0);
		else
			setCurrentPage(docEndPages[nextDoc-1]+1); // first page of this doc is last page of prev. doc + 1
	}
	else
		LOG ('not possible to scroll to section ' + nextSection);

	showCurrentPage();
}
*/


function setCurrentPage(num)
{
	var oldPage = currentPage;
	currentPage = num;	
	if (settings.page_change_callback != null)
		settings.page_change_callback.apply(this, [oldPage, currentPage]);
}

function showCurrentPage()
{
	LOG ('showing current page = ' + currentPage, false); // don't log on server

	// normally we'll update page window after displaying the page, but if we dont have the current page,
	// we need to update page window right now
	var page_window_updated_in_this_call = false;

	if (selectedPageNum.length == 0)
		return;

    if (settings.show_count)
	    $('#jog_text').html('&nbsp;' + (currentPage+1) + ' of ' + (selectedPageNum.length) + '&nbsp;');
	$('#jog_status1').html('&nbsp;' + (currentPage+1) + ' of ' + (selectedPageNum.length) + '&nbsp;');

	var docId = $($('.page').get(currentPage)).attr('docId');
//	$('#jog_docId').html('<a href="browse?initDocId=' + docId + '">' + docId + '</a>');
	// $('#jog_docId').html(docId);

	// LOG ('absolute page num = ' + selectedPageNum[currentPage]);

	var docNum = (typeof docEndPages == 'undefined') ? currentPage : whichRange(currentPage, docEndPages); // kinda inefficient to find range for every page in frame
	
	// table does not span 100% of div_jog_contents if in acrobatics mode
	var style = 'position:relative;left:0px;'; // 'style="width:100%"';
	if (typeof acrobatics !== 'undefined')
		style += 'min-width:' + 820*N_COLS_IN_FRAME + 'px;';

	var frameContents = '\n<div style="' + style + '">\n</div>';
//	frameContents += '<td class="pageHeader" colspan="' + N_COLS_IN_FRAME + '">' + 	computePageHeader(docNum);
//	frameContents += '</td>\n';
//	frameContents += '</tr>\n<tr>';

    if (typeof N_PAGES_IN_FRAME == 'undefined')
        N_PAGES_IN_FRAME = 1;
    if (typeof N_COLS_IN_FRAME == 'undefined')
        N_COLS_IN_FRAME = 1;

	for (var i = 0; i < N_PAGES_IN_FRAME; i++)
	{
		var page = currentPage + i;
		if (page < totalPages)
		{
			if (pages[currentPage] == null)
			{
				LOG ('page ' + currentPage + " not available", false);
				update_page_window();
				page_window_updated_in_this_call = true;

				// should show a quick status message here indicating that the page is loading
//				for (var i = 0; i < N_PAGES_IN_FRAME; i++)
//				{
//					var page = currentPage + i;
//					frameContents += "Loading page " + i;
//				}
				// we'll check again after 200 ms
				window.setTimeout(showCurrentPage, 200);
				return;
			}

// LOG ('ok we have page ' + currentPage  + ' ' + pages[currentPage].length + ' chars');
			var bStyle = '';
			if (page < totalPages-1 && i < N_PAGES_IN_FRAME-1)
				bStyle = 'border-right: solid #888 2px;';
			frameContents += '<td class="pageFrame" style="vertical-align:top;min-width:800px; max-width:800px; min-height:1000px;overflow:hidden;' + bStyle + '">';
			frameContents += pages[page];
			frameContents += '</td>\n';
			
			if ((i+1) % N_COLS_IN_FRAME == 0)
				frameContents += "</tr>\n<tr>\n";
		}
		// LOG ('page ' + page + ' is ' + pages[page]);
	}
	frameContents += '</tr></table>';
//	LOG ('frame contents: ' + frameContents);
	$(settings.jog_content_frame_selector).html(frameContents);
	// LOG ('setting up contents for page ' + currentPage);
	
	if (!page_window_updated_in_this_call)
		update_page_window();

	// trying out lightbox, remove it if it becomes too much of a hassle
	// downside is that it only works for images
	// re-investigate if we have attachments with image previews that link to download of actual file (like for PDFs)
	var temp = $(".attachments img.img");
	if (temp.length > 0)
		temp.lightBox();
	/*
	$(".attachments img").fancybox({
		'transitionIn'	:	'elastic',
		'transitionOut'	:	'elastic',
		'speedIn'		:	300, 
	//	'modal'			: true,
		'speedOut'		:	200, 
		'overlayShow'	:	true,
		'showCloseButton' : true,
		'hideOnOverlayClick': true
	});
	*/
	// updatePageHeader();
	if (settings.page_load_callback != null)
		settings.page_load_callback.apply(this, [currentPage]);
}

function doPageForward()
{
	if (currentPage + N_PAGES_IN_FRAME < selectedPageNum.length)
	{
		setCurrentPage(currentPage + N_PAGES_IN_FRAME);
	    showCurrentPage();
        if (typeof settings.sound_selector !== 'undefined')
            try { LOG ('playing sound!' + settings.sound_selector); $(settings.sound_selector)[0].play(); } catch (e) { LOG ('Exception playing click sound: ' + e); }
	}
}

function doPageBackward()
{
	if ((currentPage-N_PAGES_IN_FRAME) >= 0)
	{
		setCurrentPage(currentPage - N_PAGES_IN_FRAME);
	    showCurrentPage();
        if (typeof settings.sound_selector !== 'undefined')
            try { $(settings.sound_selector)[0].play(); } catch (e) { LOG ('Exception playing click sound: ' + e); }
	}
}

////////////////  paging ////////////////////
function handle_ajax_error(event, XMLHttpRequest, ajaxOptions, thrownError)
{
	// little fishy here -- browser refresh on releaseDataset hits a strange error on FF
	// alert ('ajax error from ' + ajaxOptions.url + " " + dump_obj(event) + " ajaxOptions = " + dump_obj(ajaxOptions) + dump_obj(thrownError));
	url = ajaxOptions["url"];
	LOG("url: "+url, false);
	if(typeof (url)!=undefined && url.indexOf('muselog.jsp')>=0) {
		LOG("Sorry: Ajax error: event = " + dump_obj(event) + " ajaxOptions = " + dump_obj(ajaxOptions) + " thrownError = " + dump_obj(thrownError), false);
		LOG("epadd log", false);
	}
	else
		LOG ("Sorry: Ajax error: event = " + dump_obj(event) + " ajaxOptions = " + dump_obj(ajaxOptions) + " thrownError = " + dump_obj(thrownError));
}

function setup_paging()
{
	LOG ('setting up paging');
	currentWindow = {};
	windowSize = {};
	currentWindow.start = currentWindow.end = -1;
	// these many pages will be stored in browser (not including current page)
	if (settings.paging_info != null)
	{
		windowSize.back = settings.paging_info.window_size_back;
		windowSize.fwd = settings.paging_info.window_size_fwd;
	}
	else
	{
		// no windows
		windowSize.back = totalPages;
		windowSize.fwd = totalPages;
	}
	timeOfLastWindowMove = 0; // we last moved the window in 1970...
}

// fetch startPage to endPage, both inclusive
function page_in(startPage, endPage, urlParam)
{
	if (!settings.paging_info || !settings.paging_info.url)
    {
        LOG ('no paging info available');
		return;
    }
	
	// false param to not log on server since this is latency critical
	LOG ("Paging in [" + startPage + ".." + endPage + "]", false);

	if (startPage > endPage)
	{
		LOG ("ERROR: attempting to page in [" + startPage + ".." + endPage + "]");
		return;
	}

	if (!urlParam) urlParam = '';

//	ajax request to get lowPage to highPage
// response pages must be in sequence with pageId attribute set
	var url = settings.paging_info.url + "&startPage=" + startPage + "&endPage=" + endPage + urlParam;
	$.ajax({
		url: url,
		success: function (response, t, xhr) {
			// using $(settings.page_selector, response) leads to stack overflow in ffox :-(, so working around by creating a div and stashing the response in it and using DOM query methods
			var p = document.createElement("div");
			p.innerHTML = response;
			var recvdPages = p.getElementsByClassName('page');

			// false param to not log on server since this is latency critical
			LOG("received response for pages [" + startPage + ".." + endPage + "], response length is " + response.length + " has " + recvdPages.length + " pages", false);
			for (var i = 0; i < recvdPages.length; i++) {
				var recvdPageId = recvdPages[i].getAttribute("pageId");
				var expectedPageId = startPage + i;
				if (recvdPageId != expectedPageId)
					LOG("Warning: pageId expected " + expectedPageId + " recvd " + recvdPageId);
				pages[startPage + i] = recvdPages[i].innerHTML;
			}
		},
		error: function(response) {
			epadd.error("Sorry, there was an error accessing " + url);
		}
	});
}

$.fn.jog_page_reload = function(page, inFull) {
	page_out(page, page);
	page_in(page, page, inFull);
	showCurrentPage();
};

// start, end could be < 0, in which case the page is ignored. start should always be <= end
function page_out(start, end)
{
	// sometimes start, end could be < 0
	for (var i = start; i <= end; i++) // note: start and end page inclusive (same as jogPageIn.jsp)
		if (i >= 0)
			pages[i] = null;
}

// initial window: [-1,-1]
function update_page_window() {
	var currentTime = new Date().getTime();

	// don't do anything if we moved window < 500 ms ago
	if ((currentTime - timeOfLastWindowMove) < 500)
		return;
	timeOfLastWindowMove = currentTime;

	var totalPages = pages.length;

	// compute position of new window, based on current page
	var newWindow = {};
	newWindow.start = currentPage - windowSize.back;
	if (newWindow.start <= 0)
		newWindow.start = 0;

	newWindow.end = currentPage + windowSize.fwd;
	if (newWindow.end >= totalPages)
		newWindow.end = totalPages-1;

	// if window didn't change, return, can happen e.g. if user moved fwd a page and then back a page
	if (currentWindow.start == newWindow.start && currentWindow.end == newWindow.end)
		return;

	// false param to not log on server since this is latency critical
	LOG ('current page: ' + currentPage + " of " + totalPages + " current window: [" + currentWindow.start + ".." + currentWindow.end + "] moving to new window: [" + newWindow.start + ".." + newWindow.end + "]", false);

	if (newWindow.start < currentWindow.start)
		page_in (newWindow.start, currentWindow.start-1);
	else if (newWindow.start > currentWindow.start)
		page_out (currentWindow.start, newWindow.start-1);

	if (currentWindow.end < 0) // initial condition
		page_in (newWindow.start, newWindow.end);
	else if (newWindow.end > currentWindow.end)
		page_in (currentWindow.end+1, newWindow.end);
	else if (newWindow.end < currentWindow.end)
		page_out (newWindow.end+1, currentWindow.end);

	currentWindow.start = newWindow.start;
	currentWindow.end = newWindow.end;
}

////////////////////////// actual Jog function starts here
      var currentPage = -1, currentCompartment = -1;

      if (typeof settings.pages == 'undefined') {
          LOG ('starting jog with settings.page_selector ' + settings.page_selector + " jog_contents_selector "+ settings.jog_content_frame_selector + ' settings.paging_info ' + settings.paging_info);
          totalPages = $(settings.page_selector).length;
          if (totalPages == 0)
          {
              if (TOTAL_PAGES)
                  totalPages = TOTAL_PAGES;
              else {
                  $(settings.jog_content_frame_selector).html('Zero documents.');
                  return;
              }
          }
          LOG (totalPages + ' pages found');

          pages = [];
          pageImagesFetched = [];
          for (var i = 0; i < totalPages; i++)
          {
              pages[i] = null;
              pageImagesFetched[i] = false;
          }

          // populate any pages that already exist
          var pageReadyCount = 0;
          $(settings.page_selector).each(function() {
              var id = $(this).attr('pageId');
              var html = $(this).html();
              if (html && html.length > 0)
              {
                  LOG ('we already have page ' + id + ' ' + html.length + ' chars');
                  pageReadyCount++;
                  pages[id] = html;
              }
          });
          LOG(pageReadyCount + ' pages already present');
          setupSections();
          setupDocuments();
      } else {
          pages = settings.pages;
          LOG ('using pre-provided pages: '  + pages.length);
          totalPages = pages.length;
          if (typeof settings.sectionEndPages != 'undefined') {
              sectionEndPages = settings.sectionEndPages;
          } else {
              sectionEndPages = [];
              // create sectionEndPages ourselves at intervals of 10
              var last_pushed = -1;
              for (var x = 10; x < pages.length; x+=10) {
                  sectionEndPages.push(x);
                  last_pushed = x;
              }
              if (last_pushed != pages.length) {
                  sectionEndPages.push(pages.length);
              }
          }
          nSections = sectionEndPages.length;
      }

      selectedPageNum = []; // array of indices of selected pages into main page array
      for (var i = 0; i < totalPages; i++)
          selectedPageNum[i] = i;

      LOG (pages.length + ' pages set up');

      $(document).ajaxError(handle_ajax_error);
      setup_paging();
      LOG ('jog disabled = ' + settings.disabled);
      jog = new Jog(settings, settings.width, settings.width, 20, doPageForward, doPageBackward);
      $(document).keydown(keypress_handle);
//	$(settings.jog_content_frame_selector).keydown(keypress_handle);
      // TOFIX: entryPage is a global
      if (typeof(entryPage) != 'undefined')
          setCurrentPage(entryPage);
      else
          setCurrentPage(0);

      showCurrentPage();

	return jog;

	};
})( jQuery );

