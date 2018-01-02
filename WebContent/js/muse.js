/**
 * 
 */

var muse = {};

muse.ASSERT = function(p)
{
	if (!p)
	{
		try {
			var trace = printStackTrace();
			alert ('Assertion failed: stack trace is\n' +  trace.join('\n'));
		} catch (e) { alert ("printStackTrace not found; need to include stacktrace.js");}
		throw "ASSERTION FAILED";
	}
};

muse.log = function(s) { epadd.log(s); };

muse.trim = function(s) {
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

muse.pluralize = function(count, description)
{
	if (count == 1)
		return count + ' ' + description;
	else
		return count + ' ' + description + 's';
};

muse.reveal = function(elem, show_less) 
{ 
	if (typeof(show_less) == 'undefined')
		show_less = 'true'; // true by default
	if ($(elem).text() == 'More')
	{
		// sometimes we want to only do the "more" without an option for "less"
		if (show_less)
			$(elem).text('Less'); 
		else
			$(elem).text('');
		$(elem).prev().show();
	}
	else
	{
		$(elem).text('More'); 
		$(elem).prev().hide();
	}
	return false; //  usually we don't want the click to ripple through
};

// print all members directly in o (and not its supertypes)
// note: does not actually print anything, just returns a string
// print_supertype_fields is off by default
muse.dump_obj = function (o, print_supertype_fields)
{
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
				s += f + "=" + (f.match(/.*password.*/) ? '***' : o[f]) + ' '; // otherwise write out the value					
		} catch (e) {
			epadd.log ('exception trying to dump object field ' + f + ':' + e);
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

muse.approximateTimeLeft = function(sec)
{
	var h = parseInt(sec/3600);
	var m = parseInt((sec%3600)/60);
	
	if (sec > 2 * 3600)
		return "About " + h + " hours left";
	if (h == 1 && m > 5)
		return "About an hour and " + muse.pluralize(m, 'minute');
	if (h == 1)
		return "About an hour";
	if (sec > 120)
		return "About " + m + " minutes left";
	if (sec > 90)
		return "A minute and a bit...";
	if (sec > 60)
		return "About a minute...";
	if (sec > 30)
		return "Less than a minute...";
	if (sec > 10)
		return "Less than half a minute...";	
	return "About 10 seconds...";
};

/** collects all input fields on the page, and makes an object out of them with the field names as property names */
muse.collect_input_fields = function() {
	var result = {};
	$('input,select,textarea').each (function() {  // select field is needed for accounttype
		if ($(this).attr('type') == 'button') { return; } // ignore buttons (usually #gobutton)
		if ($(this).attr('type') == 'checkbox') {
			if ($(this).is(':checked'))
			{
				result[this.name] = 'on';
				epadd.log ('checkbox ' + this.name + ' is on');
			}
			else
				epadd.log ('checkbox ignored');
		}
		else {
			result[this.name] = this.value;
		}
	});
	
	return result;
};

muse.ellipsize = function(s, maxChars)
{
	if (s == null)
		return null;

	if (maxChars < 0)
		return '';
	
	if (maxChars < 4)
		return (s.substring(0, maxChars));

	if (s.length > maxChars)
		return s.substring(0, maxChars-3) + "...";
	else
		return s;
};
