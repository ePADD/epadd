/*
 Copyright (C) 2012 The Stanford MobiSocial Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/



package edu.stanford.muse.wpmine;

// warning: do not introduce package dependencies other than java.* classes in this collection of utils
// utils that are specific to other libs should go in their own utils file
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.GZIPInputStream;


/** a general set of utils by Sudheendra... don't introduce dependencies in this file on ANY other libs
 * because it is used by multiple projects.
 * @author hangal
 */
public class Util
{
	public static boolean BLUR = true; // blurring of fnames

	public static void setBlur(boolean b) { BLUR = b; }
	
    // truncates given string to max len, adding ellipsis or padding if necessary
    public static String truncate (String s, int max_len)
    {
        if (s == null)
            s = "???";
        int len = s.length();
        if (len <= max_len)
        {
            for (int i = 0; i < max_len - len; i++)
                s = s + " ";
        }
        else
            s = s.substring (0, max_len-3) + "...";

        return s;
    }

public static void ASSERT (boolean b)
{
    if (!b)
    {
        System.err.println ("Assertion failed!\n");
        throw new RuntimeException();
    }
}

public static boolean nullOrEmpty (String x) {	return (x == null || "".equals(x)); }
public static<E> boolean nullOrEmpty (E[] a) {	return (a == null || a.length == 0); }
public static boolean nullOrEmpty (Collection c) {	return (c == null || c.size() == 0); }
public static boolean nullOrEmpty (Map m) {	return (m == null || m.size() == 0);}

/** replaces everything but the first and last letter of the input string s by '.'
 * useful for bluring potentially sensitive information that is needed in log files
 * like email folder names
 * e.g. SECRET is returned as S....T
 */
public static String blur(String s)
{
	if (!BLUR)
		return s;

	if (s == null || s.length() <= 1)
		return s;
	char c[] = s.toCharArray();
	for (int i = 1; i < s.length()-1; i++)
		c[i] = '.';
	return new String(c);
}

	/* like assert, bit does not crash */
public static boolean softAssert (boolean b)
{
    warnIf (!b, "Soft assert failed!");
    return true;
}

/* like assert, bit does not crash */
public static boolean softAssert (boolean b, String message)
{
    warnIf (!b, "Soft assert failed! " + message);
    return true;
}

public static void warnIf (boolean b, String message)
{
    if (b)
    {
    	System.err.println ("REAL WARNING: " + message + "\n");
		// Thread.dumpStack();
	    breakpoint();
    }
}

public static void die(String reason)
{
	System.err.println(reason);
	ASSERT (false);
}

public static void breakpoint()
{
	// permanent breakpoint
}

public static String stackTrace(Throwable t)
{
	StringWriter sw = new StringWriter(0);
	PrintWriter pw = new PrintWriter(sw);
	t.printStackTrace(pw);
	pw.close();
	return sw.getBuffer().toString();
}

public static String stackTrace ()
{
    Throwable t = new Exception("Printing current stack trace");
    t.fillInStackTrace();
    return stackTrace(t);
}

public static void run_command (String[] cmd, String dir) throws IOException
{
	// introduced the envp array with the PATH= string after /opt/local/bin/convert started failing
	// while converting pdf to jpg. it would fail saying" sh: gs: command not found"

	File f = null;

	if (dir != null)
	{
		f = new File(dir);
		if (!f.exists())
			f = null;
	}

    Process p = Runtime.getRuntime().exec (cmd, new String[]{"PATH=/opt/local/bin:/bin:/sbin:/usr/bin:/opt/local/sbin"}, f);

    // printing the process's stderr on screen
    Reader r = new InputStreamReader(p.getErrorStream());
    while (true)
    {
        int i = r.read();
        if (i == -1)
            break;
        System.err.print ((char) i);
    }
    r.close();

    try { p.waitFor(); }
    catch (InterruptedException ie) { System.out.println ("Unable to complete command"); throw new RuntimeException(ie); }
}


public static long copy_stream_to_file(InputStream is, String filename) throws IOException
{
    int bufsize = 64 * 1024;
    long nBytes = 0;
    BufferedInputStream bis = null;
    BufferedOutputStream bos = null;
    try {
        bis = new BufferedInputStream(is, bufsize);
        bos = new BufferedOutputStream(new FileOutputStream(filename), bufsize);
        byte buf[] = new byte[bufsize];
        while (true)
        {
            int n = bis.read(buf);
            if (n <= 0)
                break;
            bos.write (buf, 0, n);
            nBytes += n;

        }
    } finally {
        if (bis != null) bis.close();
        if (bos != null) bos.close();
    }
    return nBytes;
}

public static void copy_file(String from_filename, String to_filename) throws IOException
{
    copy_stream_to_file(new FileInputStream(from_filename), to_filename);
}


public static List<String> getLinesFromInputStream(InputStream in, boolean ignoreCommentLines) throws IOException
{
	return getLinesFromReader(new InputStreamReader(in, "UTF-8"), ignoreCommentLines);
}

/** returns collection of lines from given file (UTF-8). 
 * trims spaces from the lines, 
 * ignores lines starting with # if ignoreCommentLines is true  */
public static List<String> getLinesFromReader(Reader reader, boolean ignoreCommentLines) throws IOException
{
	LineNumberReader lnr = new LineNumberReader(reader);
	List<String> result = new ArrayList<>();
	while (true)
	{
		String line = lnr.readLine();
		if (line == null)
			break;
		line = line.trim();
		
		if (ignoreCommentLines && (line.charAt(0) == '#'))
			continue;
		
		result.add(line);
	}
    return result;
}

/** escape some special xml chars. use at your own risk. */
public static String escapeXML (String str)
{
	if (str == null)
		return null;

	// these are the 5 special xml chars according to http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
	str = str.replace ("&", "&amp;");
	str = str.replace ("'", "&apos;");
	str = str.replace ("\"", "&quot;");
	str = str.replace ("<", "&lt;");
	str = str.replace (">", "&gt;");

	StringBuilder sb = new StringBuilder();

	// can speed this up if needed by checking if it's the common case of no special chars
	char ca[] = str.toCharArray();
	for (char c : ca)
	{
		if (c > 127)
			try {
				sb.append ("&#x" + String.format("%04x", (int) c) + ";");
			} catch (IllegalFormatConversionException ifce) {
				System.out.println ("REAL WARNING: illegal format conversion: " + ifce + " char = " + (int) c);
				// ignore it
			}
		else
			sb.append (c);
	}

	return sb.toString();
}

/** escapes the 5 special html chars - see http://www.w3schools.com/tags/ref_entities.asp */
public static String unescapeHTML (String str)
{
	if (str == null)
		return null;

	// these are the 5 special xml chars according to http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
	str = str.replace ("&amp;", "&");
	str = str.replace ("&apos;", "'");
	str = str.replace ("&quot;", "\"");
	str = str.replace ("&lt;", "<");
	str = str.replace ("&gt;", ">");
	str = str.replace ("\u0095", "."); // remove the damn bullets. // TODO: should use \d format instead to avoid the special char itself.
	return str;
}


/** escapes the 5 special html chars - see http://www.w3schools.com/tags/ref_entities.asp */
public static String escapeHTML (String str)
{
	if (str == null)
		return null;

	// these are the 5 special xml chars according to http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
	str = str.replace ("&", "&amp;");
	str = str.replace ("'", "&apos;");
	str = str.replace ("\"", "&quot;");
	str = str.replace ("<", "&lt;");
	str = str.replace (">", "&gt;");
	return str;
}

// TODO: should consider escaping the escape char as well. Otherwise, subject to \' -> \\' which leaves the quote unescaped. 
//
///** escapes single quote with backslash */
//public static String escapeSquote (String str)
//{
//	if (str == null)
//		return null;
//	str = str.replace ("\'", "\\\'");
//	return str;
//}
//
///** escapes double quote with backslash */
//public static String escapeDquote (String str)
//{
//	if (str == null)
//		return null;
//	str = str.replace ("\"", "\\\"");
//	return str;
//}

/** capitalizes just the first letter and returns the given string */
public static String capitalizeFirstLetter (String str)
{
	if (str == null)
		return null;
	if (str.length() == 0)
		return str;
	if (str.length() == 1)
		return Character.toString((Character.toUpperCase(str.charAt(0))));
	else
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
}


/** capitalizes just the first letter and returns the given string */
public static boolean allUppercase (String str)
{
	if (str == null)
		return true;
	for (char c: str.toCharArray())
		if (Character.isLowerCase(c))
			return false;
	return true;
}

public static String ellipsize(String s, int maxChars)
{
	if (s == null)
		return null;

	if (maxChars < 4)
		return (s.substring(0, maxChars));

	if (s.length() > maxChars)
		return s.substring(0, maxChars-3) + "...";
	else
		return s;
}

/** checks if the string has i18n chars, or is plain ascii */
public static boolean isI18N(String s)
{
	byte bytes[] = s.getBytes();
	for (byte b: bytes)
		if (b < 0)
			return true;
	return false;			
}

public static String padWidth(String s, int width)
{
	if (s == null)
		s = "";
	if (s.length() >= width)
		return s;
	StringBuilder sb = new StringBuilder(s);
	for (int i = 0; i < width - s.length(); i++)
		sb.append(" ");
	return sb.toString();
}

/** returns file's extension, and null if it has no extension */
public static String getExtension(String filename)
{
	if (filename == null)
		return null;
	
	int idx = filename.lastIndexOf(".");
	int MAX_EXTENSION_LENGTH = 6;
	if (idx > 0) // note: > not >= if filename starts with ., its not considered an extension
	{
		int ext_length = filename.length() - idx;
		if (ext_length > 0 && ext_length < MAX_EXTENSION_LENGTH)
			return filename.substring(idx+1);
	}
	return null;
}

private static void testGetExtension()
{
	ASSERT (getExtension(".cshrc") == null);
	ASSERT (getExtension("") == null);
	ASSERT (getExtension("no-dot") == null);
	ASSERT (getExtension("a.ppt").equals("ppt"));	
}

/** converts a very long name.doc -> a very lon...g.doc.
 * tries to fit s into maxChars, with a best effort to keep the extension */
public static String ellipsizeKeepingExtension(String s, int maxChars)
{
	if (s.length() <= maxChars)
		return s;

	int idx = s.lastIndexOf(".");
	if (idx <= 0)
		return ellipsize(s, maxChars); // no extension

	int MAX_EXTENSION_LENGTH = 6;
	if (s.length() - idx > MAX_EXTENSION_LENGTH)
		return ellipsize (s, maxChars); // unusually long "extension", don't what's happening, play it safe by ignoring it

	// keep everything from one char before the . till the end,
	String tail = s.substring(idx-1); // tail is [idx-1 to s.length]

	int maxCharsRemaining = maxChars - tail.length();
	return ellipsize(s.substring(0,idx-1), maxCharsRemaining) + tail;
}

/** blurs a filename but keeps the extension intact.
 * e.g. "secret.jpg" becomes "s....t.jpg"
 */
public static String blurKeepingExtension(String s)
{
	if (s == null)
		return null;
	
	int idx = s.lastIndexOf(".");
	if (idx <= 0)
		return blur(s); // no extension

	int MAX_EXTENSION_LENGTH = 6;
	if (s.length() - idx > MAX_EXTENSION_LENGTH)
		return blur (s); // unusually long "extension", don't what's happening, play it safe by ignoring it

	// tail is everything from one char before the . till the end,
	String tail = s.substring(idx); // tail is [idx-1 to s.length]

	// blur the part before tail and append the rest to it.
	return blur(s.substring(0,idx)) + tail;
}

public static List<String> tokenize(String s)
{
	List<String> result = new ArrayList<>();
	if (Util.nullOrEmpty(s))
		return result;
	
	StringTokenizer st = new StringTokenizer(s);
	while (st.hasMoreTokens())
		result.add(st.nextToken());
	return result;
}

public static List<String> tokenize(String s, String delims)
{
	List<String> result = new ArrayList<>();
	if (Util.nullOrEmpty(s))
		return result;
	
	StringTokenizer st = new StringTokenizer(s, delims);
	while (st.hasMoreTokens())
		result.add(st.nextToken());
	return result;
}

public static Collection<String> breakIntoParas(String input) throws IOException
{
	List<String> paras = new ArrayList<>();
	LineNumberReader lnr = new LineNumberReader(new StringReader(input));
	
	StringBuilder currentPara = new StringBuilder();
	
	while (true)
	{
		String line = lnr.readLine();
		if (line == null)
			break;
		line = line.trim();
		
		if (line.length() == 0)
		{
			// end para
			if (currentPara.length() > 0)
				paras.add(currentPara.toString());
			currentPara = new StringBuilder();
		}
		else
		{
			currentPara.append(line);
			currentPara.append("\n");
		}
	} 
	
	// add any residue
	if (currentPara.length() > 0)
		paras.add(currentPara.toString());
	return paras;
}

public static void testEllipsizeKeepingExtension()
{
	ASSERT (ellipsizeKeepingExtension("Olson Melvile Reading Group Description (Revised 08.23.01).doc", 15).equals("Olson M...).doc"));
	ASSERT (ellipsizeKeepingExtension("RobertCreeleyInterview.rtf", 15).equals("RobertC...w.rtf"));
	ASSERT (ellipsizeKeepingExtension("Article_Type1_c=Article_cid=1074381008529_call_pageid=1044442959412_col=1044442957278", 15).equals("Article_Type..."));
	ASSERT (ellipsizeKeepingExtension("Harold I. Cammer to ED,.doc", 15).equals("Harold ...,.doc"));
	ASSERT (ellipsizeKeepingExtension("ED to Harold I. Cammer.doc", 15).equals("ED to H...r.doc"));
	ASSERT (ellipsizeKeepingExtension("permission creeley.doc", 15).equals("permiss...y.doc"));
}

public static void main(String args[])
{
	testEllipsizeKeepingExtension();
	testGetExtension();
	System.out.println ("Tests passed ok");
}

/** safely splits a string into two around the first occurrence of c in s.
 * always returns an array of 2 strings.
 * if c does not occur in s, returns an empty string in the second place.
 */
public static String[] splitIntoTwo(String s, char c)
{
	int idx = s.indexOf(c);
	if (idx < 1)
		return new String[]{s, ""};

	String[] result = new String[2];
	
	result[0] = (idx > 0) ? s.substring(0, idx-1):""; // substring should have accepted args (0, -1) to give an empty string, but it doesn't.
	result[1] = s.substring(idx+1); // this will always work even if the last character is c, because "abc".substring(3) returns ""
	return result;
}

public static String commatize(long n)
{
	String result = "";
	do
	{
		if (result.length() > 0)
			result = "," + result;
		long trio = n%1000; // 3 digit number to be printed
		if (trio == n) // if this is the last trio, no lead of leading 0's, otherwise make sure to printf %03f
			result = String.format("%d", n%1000) + result;
		else
			result = String.format("%03d", n%1000) + result;

		n = n/1000;
	} while (n > 0);
	return result;
}

/** if num > 1, pluralizes the desc. will also commatize the num if needed. */
public static String pluralize(int x, String desc)
{
	return Util.commatize(x) + " " + desc + ((x > 1) ? "s":"");
}

public static String approximateTimeLeft(long sec)
{
	int h = (int) sec/3600;
	int m = (int) (sec%3600)/60;

	if (sec > 2 * 3600)
		return "About " + h + " hours left";
	if (h == 1)
		return "About an hour and " + m + " minutes ";
	if (sec > 120)
		return "About " + m + " minutes left";
	if (sec > 90)
		return "A minute and a bit ...";
	if (sec > 60)
		return "About a minute ...";
	if (sec > 30)
		return "Less than a minute ...";
	if (sec > 10)
		return "Less than half a minute ...";
	if (sec >= 2)
		return sec + " seconds left";

	return "Just a sec...";
}

/** returns yyyy-mm-dd format for given calendar object */
public static String formatDate(Calendar c)
{
	if (c == null)
		return "??-??";
	else
		return c.get(Calendar.YEAR) + "-" + String.format("%02d", (1+c.get(Calendar.MONTH))) + "-" + String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
}

/** returns yyyy-mm-dd format for given date object */
public static String formatDate(Date d)
{
	if (d == null)
		return "??-??";
	Calendar c = new GregorianCalendar();
	c.setTime(d);
	return formatDate(c);
}

public static String formatDateLong(Calendar d)
{
	if (d == null)
		return "??-??";
	else
		return d.get(Calendar.YEAR) + "-" + String.format("%02d", (1+d.get(Calendar.MONTH))) + "-" + String.format ("%02d", d.get(Calendar.DAY_OF_MONTH)) + "."
			 + String.format("%02d", d.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", d.get(Calendar.MINUTE)) + ":" +
			 String.format("%02d", d.get(Calendar.SECOND));
}

public static String formatDateLong(Date d)
{
	if (d == null)
		return "??-??";
	Calendar c = new GregorianCalendar();
	c.setTime(d);
	return formatDateLong(c);
}

// computes basename of s: if s is /a/b/c/hangal@cs.stanford.edu/Mail__Sent__Mail (/ is file.separatorchar, could be \ on windows)
// returns Mail__Sent__Mail, so that it can be hyperlinked from the top level html file
public static String baseName (String s)
{
	if (s == null)
		return null;
	String base = s;
	int idx = base.lastIndexOf(File.separatorChar);
	if (idx >= 0)
		base = base.substring(idx+1);
	return base;
}

/** complement of baseName */
public static String dirName (String s)
{
	String dir = "";
	int idx = s.lastIndexOf(File.separatorChar);
	if (idx >= 0)
		dir = s.substring(0, idx);
	return dir;
}

/** normalizes histogram in hist to produce ratios of each position to total sum */
public static double[] normalizeHistogram(int[] hist)
{
	int sum = 0;
	for (int i: hist)
		sum += i;
	return normalizeHistogramToBase(hist, sum);
}

/** returns histogram counts divided by given base */
public static double[] normalizeHistogramToBase(int[] hist, double base)
{
	double[] result = new double[hist.length];
	for (int i = 0; i < hist.length; i++)
		result[i] = (base == 0) ? 0.0 : ((double) hist[i])/base;

	return result;
}


	/** reads contents of a (text) file and returns them as a string */
public static String getFileContents(String filename) throws IOException
{
	BufferedReader br;
	if (filename.endsWith(".gz"))
		br = new BufferedReader(new InputStreamReader (new GZIPInputStream(new FileInputStream(filename)), "UTF-8"));
	else
		br = new BufferedReader(new InputStreamReader (new FileInputStream(filename), "UTF-8"));

	StringBuilder sb = new StringBuilder();
	// read all the lines one by one till eof
	while (true)
	{
		String x = br.readLine();
		if (x == null)
			break;

		sb.append(x);
		sb.append("\n");
	}
	return sb.toString();
}

// returns a list of dates representing intervals
// interval i is represented by [i]..[i+1] in the returned value
public static List<Date> getMonthlyIntervals(Date start, Date end)
{
	List<Date> intervals = new ArrayList<>();
	GregorianCalendar c = new GregorianCalendar();
	c.setTime(start);
	int startMonth = c.get(Calendar.MONTH);
	int startYear = c.get(Calendar.YEAR);
	int year = startYear;
	int month = startMonth;

	intervals.add(start);
	while (true)
	{
		month++;
		if (month == 12)
		{
			month = 0;
			year++;
		}

		c = new GregorianCalendar(year, month, 1, 0, 0, 0);
		intervals.add(c.getTime());
		if (c.getTime().after(end))
			break;
	}
	return intervals;
}


//returns a list of dates representing intervals
//interval i is represented by [i]..[i+1] in the returned value
public static List<Date> getYearlyIntervals(Date start, Date end)
{
	List<Date> intervals = new ArrayList<>();
	GregorianCalendar c = new GregorianCalendar();
	c.setTime(start);
	int startYear = c.get(Calendar.YEAR);
	int year = startYear;

	intervals.add(start);
	while (true)
	{
		year++;
		c = new GregorianCalendar(year, 0, 1, 0, 0, 0);
		intervals.add(c.getTime());
		if (c.getTime().after(end))
			break;
	}
	return intervals;
}

/* given jun2004-oct2008 with a window size of 12, and step size of 1, returns:
 * jun2004-jun2005
 * jul2004-jul2005
 * ...
 * nov2007-nov2008
 */
public static List<Pair<Calendar,Calendar>> getSlidingMonthlyIntervalsForward(Calendar start, Calendar end, int windowSizeInMonths, int stepSizeInMonths)
{
	List<Pair<Calendar,Calendar>> intervals = new ArrayList<>();

	if (start.after(end)) // error
	{
		softAssert(false);
		return intervals;
	}

	if (start == null || end == null)
		return intervals;

	Calendar windowStart = start;
	int windowStartMonth = start.get(Calendar.MONTH);
	int windowStartYear = start.get(Calendar.YEAR);

	while (true)
	{
		int windowEndMonth = windowStartMonth + windowSizeInMonths;
		int windowEndYear = windowStartYear;
		if (windowEndMonth >= 12)
		{
			windowEndYear += windowEndMonth/12;
			windowEndMonth = windowEndMonth%12;
		}

		Calendar windowEnd = new GregorianCalendar(windowEndYear, windowEndMonth, 1, 0, 0, 0);
		intervals.add(new Pair<>(windowStart, windowEnd));

		if (windowEnd.after(end))
			break;

		// step window start
		windowStartMonth += stepSizeInMonths;
		if (windowStartMonth >= 12)
		{
			windowStartYear += windowStartMonth/12;
			windowStartMonth = windowStartMonth%12;
		}

		windowStart = new GregorianCalendar(windowStartYear, windowStartMonth, 1, 0, 0, 0);
	}
	return intervals;
}

/** like forward, but er... backward.
 */
public static List<Pair<Date,Date>> getSlidingMonthlyIntervalsBackward(Date start, Date end, int windowSizeInMonths, int stepSizeInMonths)
{
	Util.die ("Unimplemented");
	return null;
	/*
	List<Pair<Date,Date>> intervals = new ArrayList<Pair<Date,Date>>();

	if (start.after(end)) // error
	{
		softAssert(false);
		return intervals;
	}

	if (start == null || end == null)
		return intervals;

	Date windowEnd = end;
	int windowEndMonth = end.get(Calendar.MONTH);
	int windowEndYear = end.get(Calendar.YEAR);

	while (true)
	{
		if (windowEnd.before(start))
			break;

		int windowStartMonth = windowEndMonth - windowSizeInMonths;
		int windowStartYear = windowEndYear;
		if (windowStartMonth < 0)
		{
			// e.g. if windowStartMonth is -5, we need to adjust year by 1 and month to 7. (windowStartMonth/12 is 0)
			// e.g. if windowStartMonth is -15, we need to adjust year by 2 and month to 9.

			int yearsToAdjust = 1+(windowStartMonth/12);
			windowStartYear -= yearsToAdjust;
			windowStartMonth += 12*yearsToAdjust;
		}

		Calendar windowStart = new GregorianCalendar(windowStartYear, windowStartMonth, 1, 0, 0, 0);
		intervals.add(new Pair<Date, Date>(windowStart, windowEnd));

		// step window start
		windowEndMonth -= stepSizeInMonths;
		if (windowEndMonth < 0)
		{
			// same logic as above
			int yearsToAdjust = 1+(windowEndMonth/12);
			windowEndYear -= yearsToAdjust;
			windowEndMonth += 12*yearsToAdjust;
		}

		windowEnd = new GregorianCalendar(windowEndYear, windowEndMonth, 1, 0, 0, 0);
	}
	return intervals;
	*/
}

// strip leading and trailing punctuation
public static String stripPunctuation(String s)
{
//	Util.ASSERT (!s.contains(" "));
//	Util.ASSERT (!s.contains("\t"));
	String punctuation = "\r\n\t~!@#$%^&*()_+`-={}|[]\\:\";'<>?,./";

	int start = 0, end = s.length()-1;
	for (; start < s.length(); start++)
	{
		char c = s.charAt(start);
		if (punctuation.indexOf(c) < 0)
			break;
	}

	// start is our starting index for non-punct
	// if no non-punct, start is s.length()
	for (; end >= start; end--)
	{
		char c = s.charAt(end);
		if (punctuation.indexOf(c) < 0)
			break;
	}

	// end is our ending point for non-punct
	// everything between start and end (inclusive) is non-punct
	if (start > end)
		return "";

	String strippedStr = s.substring(start, end+1);
	return strippedStr;
}

// strips brackets from (...) and [...] if there are any
public static String stripBrackets(String s)
{
	if (s.startsWith("[") && s.endsWith("]"))
		return s.substring(1, s.length()-1);
	if (s.startsWith("(") && s.endsWith(")"))
		return s.substring(1, s.length()-1);
	return s;
}

/** returns a string with elements of the given collection concatenated, separated by given separator */
public static<E> String join (Collection<E> c, String separator)
{
	if (c == null)
		return null;
	if (c.size() == 0)
		return "";
	int n = c.size(), count = 0;
	StringBuilder result = new StringBuilder();
	for (E e: c)
	{
		result.append(e);
		count++;
		if (count < n) // no separator at the end
			result.append(separator);
	}
	return result.toString();
}

/** returns a string with elements of the given array concatenated, separated by given separator */
public static<E> String join (E[] c, String separator)
{
	if (c == null)
		return null;
	if (c.length == 0)
		return "";
	int n = c.length, count = 0;
	StringBuilder result = new StringBuilder();
	for (E e: c)
	{
		result.append(e);
		count++;
		if (count < n) // no separator at the end
			result.append(separator);
	}
	return result.toString();
}

/** returns a string with elements of the given collection sorted and concatenated, separated by given separator */
public static<E extends Comparable<? super E>> String joinSort (Collection<E> c, String separator)
{
	if (c == null)
		return null;
	if (c.size() == 0)
		return "";
	int n = c.size(), count = 0;
	StringBuilder result = new StringBuilder();
	List<E> tmp = new ArrayList<>(c);
	Collections.sort(tmp);
	for (E e: tmp)
	{
		result.append(e);
		count++;
		if (count < n) // no separator at the end
			result.append(separator);
	}
	return result.toString();
}

// convert an email folder name to something sane for a file system
public static String sanitizeFolderName(String s)
{
	if (s == null)
		return null;

	//	clean up special chars in the folder name
	s = s.replace(":", "__");
	s = s.replace("/", "__");
	s = s.replace("\\", "__");
	s = s.replace(" ", "__");
	s = s.replace("[", "");
	s = s.replace("]", "");
	return s;
}

// parses a string and returns as byte array
public static byte[] parseIPAddress(String str)
{
	try {
		StringTokenizer st = new StringTokenizer(str, ".");
		List<Byte> list = new ArrayList<>();
		boolean invalidAddr = false;
		while (st.hasMoreTokens())
		{
			int x = Integer.parseInt(st.nextToken());
			if ((x & 0xffffff00) != 0)
			{
				invalidAddr = true;
				break;
			}
			list.add((byte) x);
		}

		if (list.size() != 4)
			invalidAddr = true;

		if (invalidAddr)
		{
			System.err.println ("String is not a valid IPv4 address " + str);
			return null;
		}

		byte[] result = new byte[list.size()];

		int i = 0;
		for (byte b: list)
			result[i++] = b;
		return result;
	} catch (Exception e) {
		System.err.println ("Error parsing " + str + " : " + e);
		return null;
	}
}

//Deletes all files and subdirectories under dir.
// Returns true if all deletions were successful.
// If a deletion fails, the method stops attempting to delete and returns false.
public static boolean deleteDir(File f)
{
    if (f.isDirectory())
    {
        String[] children = f.list();
		for (String aChildren : children) {
			boolean success = deleteDir(new File(f, aChildren));
			if (!success) {
				System.err.println("warning: failed to delete file " + f);
				return false;
			}
		}
    }

    // The directory is now empty so delete it
    return f.delete();
}

public static void deleteDir(String path)
{
	if (path == null)
		return;
	File f = new File(path);
	if (f.exists())
	{
		boolean success = deleteDir(f);
		warnIf(!success, "Unable to delete file: " + f.getPath());
	}
	else
		warnIf(true, "Sorry, can't delete path because it doesn't even exist: " + path);
}

public static class MyFilenameFilter implements FilenameFilter {
	private String prefix, suffix; // suffix is optional
	public MyFilenameFilter(String prefix) { this.prefix = prefix; }
	public MyFilenameFilter(String prefix, String suffix) { this.prefix = prefix; this.suffix = suffix; }
	public boolean accept (File dir, String name)
	{
//		String path = (dir.getAbsolutePath() + File.separator + name);
		if (prefix != null && !name.startsWith(prefix))
			return false;
		return !(suffix != null && !name.endsWith(suffix));
	}
}

public static Set<String> filesWithPrefixAndSuffix (String dir, String prefix, String suffix)
{
	Set<String> result = new LinkedHashSet<>();
	
	if (dir == null)
		return result;
	if (!new File(dir).exists())
		return result; // empty result

	File files[] = new File(dir).listFiles(new MyFilenameFilter(prefix, suffix));
	if (files != null)
		for (File f: files)
		{
			String name = f.getName();
			if (prefix != null)
				name = name.substring(prefix.length());
			if (suffix != null)
				name = name.substring(0, name.length()-suffix.length());
			result.add(name);
		}
	return result;
}

public static Set<String> filesWithSuffix (String dir, String suffix)
{
	return filesWithPrefixAndSuffix(dir, null, suffix);
}



///////////////////////////////////////////////////////////////////////////////////////////////
//public static void sortPairsBySecondElementInt(List<Pair<?,Integer>> input)
//{
//	Collections.sort (input, new Comparator<Pair<?,Integer>>() {
//		public int compare (Pair<?,Integer> p1, Pair<?,Integer> p2) {
//			int i1 = p1.getSecond();
//			int i2 = p2.getSecond();
//			return i2 - i1;
//		}
//	});
//}

//public static void sortPairsBySecondElementFloat(List<Pair<?,Float>> input)
//{
//	Collections.sort (input, new Comparator<Pair<?,Float>>() {
//		public int compare (Pair<?,Float> p1, Pair<?,Float> p2) {
////			int i1 = p1.getSecond();
////			int i2 = p2.getSecond();
////			return i2 - i1;
//			return p2.getSecond().compareTo(p1.getSecond());
//		}
//	});
//}

/** sorts in decreasing order of second element of pair */
public static<S,T extends Comparable<? super T>> void sortPairsBySecondElement(List<Pair<S,T>> input)
{
	input.sort((p1, p2) -> {
		T i1 = p1.getSecond();
		T i2 = p2.getSecond();
		return i2.compareTo(i1);
	});
}

/** sorts in decreasing order of second element of pair */
public static<S,T extends Comparable<? super T>> void sortPairsBySecondElementIncreasing(List<Pair<S,T>> input)
{
	input.sort((p1, p2) -> {
		T i1 = p1.getSecond();
		T i2 = p2.getSecond();
		return i1.compareTo(i2);
	});
}
public static void sortPairsByFirstElement(List<Pair<Integer, ?>> input)
{
	input.sort((p1, p2) -> {
		int i1 = p1.getFirst();
		int i2 = p2.getFirst();
		return i2 - i1;
	});
}


public static<T> List<T> permuteList(List<T> in, int seed)
{
	// create a copy of the input
	List<T> result = new ArrayList<>();
	result.addAll(in);
	
	Random R = new Random(seed);
	for (int permuteSize = in.size(); permuteSize > 1; permuteSize--)
	{
		int pos = Math.abs(R.nextInt()) % permuteSize;
		// pos is in teh range 0..permuteSize-1
		// interchange elements permuteSize-1 and pos
		T tmp = result.get(permuteSize-1);
		result.set(permuteSize-1, result.get(pos));
		result.set (pos, tmp);			
	}
	return result;
}

/** takes in a map K,V and returns a List of Pairs <K,V> sorted by (descending) value */
public static<K,V> List<Pair<K,V>> mapToListOfPairs(Map<K,V> map)
{
	List<Pair<K,V>> result = new ArrayList<>();
	for (Map.Entry<K,V> e: map.entrySet())
		result.add(new Pair<>(e.getKey(), e.getValue()));
	return result;
}

/** takes in a map K,V and returns a List of Pairs <K,V> sorted by (descending) value */
public static<K,V extends Comparable<? super V>> List<Pair<K,V>> sortMapByValue(Map<K,V> map)
{
	List<Pair<K,V>> result = new ArrayList<>();
	for (Map.Entry<K,V> e: map.entrySet())
		result.add(new Pair<>(e.getKey(), e.getValue()));
	Util.sortPairsBySecondElement(result);
	return result;
}

/** takes in a map K,V and returns a sorted LinkedHashMap, sorted by (descending) value */
public static<K,V extends Comparable<? super V>> Map<K,V> reorderMapByValue(Map<K,V> map)
{
	List<Pair<K,V>> resultPairs = new ArrayList<>();
	for (Map.Entry<K,V> e: map.entrySet())
		resultPairs.add(new Pair<>(e.getKey(), e.getValue()));
	Util.sortPairsBySecondElement(resultPairs);
	Map<K, V> result = new LinkedHashMap<>();
	for (Pair<K,V> p: resultPairs)
		result.put(p.getFirst(), p.getSecond());
	return result;
}

/** takes in a map K,List<V> and returns a new Map of Pairs <K,List<V>> sorted by (descending) size of the lists.
 * by sorting, we just mean that a linkedhashmap is returned which can be iterated over in sorted order. */
public static<K,V> Map<K,Collection<V>> sortMapByListSize(Map<K,Collection<V>> map)
{
	List<Pair<K,Integer>> counts = new ArrayList<>();
	for (Map.Entry<K,Collection<V>> e: map.entrySet())
		counts.add(new Pair<>(e.getKey(), e.getValue().size()));
	Util.sortPairsBySecondElement(counts);
	Map<K,Collection<V>> result = new LinkedHashMap<>();
	for (Pair<K,Integer> p: counts)
	{
		K k = p.getFirst();
		result.put(k, map.get(k));
	}
	return result;
}

/** takes in a map K,List<V> and adds value to key's list - effectively a multi-map. */
public static<K,V> void addTo(Map<K,Collection<V>> map, K key, V value)
{
	Collection<V> values = map.computeIfAbsent(key, k -> new ArrayList<>());
	values.add(value);
}

/** parses a day string in the format yyyymmdd */
private static Calendar parseDate(String s)
{
	int d, m, y;

	int x = 0;
	try {
		x = Integer.parseInt(s);
	} catch (NumberFormatException nfe) { System.err.println ("Invalid date: " + s); return new GregorianCalendar(); }

	if (x <= 9999) // only yyyy is given
		x = x* 10000 + 1 * 100 + 1; // adjust to yyyy-01-01
	else if (x <= 999999) // only yyyy-mm is given
		x = x * 100 + 1; // adjust to yyyy-mm-01

	y = x / 10000;
	m = (x % 10000)/100;
	d = (x % 100);

	Calendar c = new GregorianCalendar(y, m-1, d); // note month needs adjustment because GC is zero based
	return c;
}

/* parses a date string in format "start-end" and returns the start and end daes
 * e.g. 2004-20060723 is equiv to 2004-01-01 to 2006-07-23
 * string on each side of '-' can be yyyy or yyyymm or yyyymmdd
 * if no '-' is given, end date is assumed to be now
 * error checking not very robust
 */
public static Pair<Calendar, Calendar> parseDateInterval(String calendarString)
{
	Calendar endDate = null;
	String startDateString, endDateString = null;

	if (!calendarString.contains("-"))
	{
		endDate = new GregorianCalendar(); // current time, default
		startDateString = calendarString;
	}
	else
	{
		StringTokenizer st = new StringTokenizer(calendarString, "-");
		startDateString = st.nextToken();
		endDateString = st.nextToken();
	}

	Calendar startDate = Util.parseDate(startDateString);
	if (endDateString != null)
		endDate = parseDate(endDateString);

	return new Pair<>(startDate, endDate);
}

/** parses keyword strings a la google search in the given input string and returns the results.
 * always returns lowercase
 * currently just tokenizes the input, in future could be aware of " ... " operators for multi-word terms.
 */
public static List<String> parseKeywords(String keywords)
{
	List<String> result = new ArrayList<>();
	if (keywords == null)
		return result;
	StringTokenizer st = new StringTokenizer(keywords);
	while (st.hasMoreTokens())
		result.add(st.nextToken().toLowerCase());
	return result;
}

public static String getMemoryStats()
{
	Runtime r = Runtime.getRuntime();
	System.gc();
	int MB = 1024 * 1024;
	return r.freeMemory()/MB + " MB free, " + (r.totalMemory()/MB - r.freeMemory()/MB) + " MB used, "+ r.maxMemory()/MB + " MB max, " + r.totalMemory()/MB + " MB total";
}

public static int getMinFreq(int nDocs, float pct)
{
	int minCount = (int) ((nDocs * pct) / 100);
	if (minCount < 2)
		minCount = 2;
	if (minCount > 5)
		minCount = 5;
	return minCount;
}

/** converts an object to a string representation by printing all its fields (fields may be non-public
 * if running without security manager). expand=true expands collections
 */
public static String fieldsToString(Object o, boolean expand)
{
    if (o == null)
        return "null";

    StringBuilder result = new StringBuilder();
    try {
    	Class c = o.getClass();
    	result.append (c.getName() + ": ");
    	Field f[] = c.getDeclaredFields();
		for (Field aF : f) {
			boolean acc = aF.isAccessible();
			if (!acc)
				aF.setAccessible(true); // ok to do in absence of a security manager

			Class t = aF.getType();
			String name = aF.getName();
			if (t == double.class)
				result.append(name + "=" + aF.getDouble(o));
			else if (t == float.class)
				result.append(name + "=" + aF.getFloat(o));
			else if (t == int.class)
				result.append(name + "=" + aF.getInt(o));
			else if (t == long.class)
				result.append(name + "=" + aF.getLong(o));
			else if (t == char.class)
				result.append(name + "=" + aF.getChar(o) + "(" + Integer.toString(aF.getChar(o)) + ")");
			else if (t == short.class)
				result.append(name + "=" + aF.getShort(o));
			else if (t == byte.class)
				result.append(name + "=" + aF.getByte(o));
			else if (t == boolean.class)
				result.append(name + "=" + aF.getBoolean(o));
			else {
				// field is of object type
				Object val = aF.get(o); // o.f[i]'s type is t, value is val
				if (val == null)
					result.append(name + "=null");
				else {
					Class valClass = val.getClass();
					if (valClass.isArray()) {
						result.append(name + "=arr{size:" + Array.getLength(val) + " ");
						if (expand)
							for (int x = 0; x < Array.getLength(val); x++)
								result.append(Array.get(val, x) + " ");
						result.append("}");
					} else if (Map.class.isAssignableFrom(valClass)) // could also check t, but val.getClass is more specific
					{
						Map m = (Map) aF.get(o);
						result.append(name + "=map{size:" + m.keySet().size() + " ");
						if (expand)
							for (Object x : m.keySet())
								result.append(x + "->" + m.get(x) + " ");
						result.append("}");
					} else if (Collection.class.isAssignableFrom(valClass)) // could also check t, but val.getClass is more specific
					{
						Collection c1 = (Collection) aF.get(o);
						result.append(name + "=" + valClass.getName() + "{size:" + c1.size() + " ");
						if (expand)
							for (Object o1 : c1)
								result.append(o1 + " ");
						result.append("}");
					} else
						result.append(name + "=[" + val.toString() + "]");
				}
			}

			result.append(" ");

			if (!acc)
				aF.setAccessible(false);
		}

    } catch (Throwable e) {
    		System.err.println(e);
    }

    return result.toString();
}

public static String fieldsToString(Object o)
{
	return fieldsToString(o, false);
}

public static long getUnprocessedMessage(int done, int total, long elapsedMillis)
{
	// compute unprocessed message
//	String unprocessedMessage = "--:-- remaining";
	long unprocessedTimeSeconds = -1;
	if (done > 0) // if 0, no way of estimating time remaining
	{
	//	long unprocessedTimeMillis = (nTotalMessagesInAllFolders - processedCount) * elapsedTimeMillis/processedCount;
		int undone = total - done;
		// this is a best guess at uncached count. we don't know how many are cached in folders we haven't even looked at yet
		// we assume they are all uncached by subtracting from total messages only the provably cached messages so far.
		long unprocessedTimeMillis = -1;
		if (done > 0)
			unprocessedTimeMillis = (undone * elapsedMillis)/done;

		unprocessedTimeSeconds = unprocessedTimeMillis / 1000;

		/*
		long hours = unprocessedTimeSeconds / 3600;
		long x = unprocessedTimeSeconds % 3600;
		long mins = x / 60;
		long secs = x % 60;
		if (hours > 0)
			formatter.format("%dh:", hours);

		formatter.format( "%02dm:", mins);
		if (hours == 0 && mins == 0 && secs == 0)
			secs = 1; // its embarassing to show 00:00s and then make people wait (which happens sometimes), so always show at least 00:01 sec remaining
		formatter.format( "%02ds", secs);
		*/
	}

	return unprocessedTimeSeconds;
}

/* given 2 arrays of strings, returns their union */
public static String[] unionOfStringArrays(String x[], String y[])
{
	Set<String> set = new LinkedHashSet<>();
    Collections.addAll(set, x);
	Collections.addAll(set, y);
	String[] arr = new String[set.size()];
	set.toArray(arr);
	return arr;
}

/** given a string representing a path to a file, returns the url string for it.
 * we only substitute # (we know it causes trouble -- remember CHI session called EPIC #FAIL ? :-)
 * and '?' currently. Note. do not use URLEncoder.encode because that does other bad things like
 * replace each space with + */
public static String URLEncodeFilePath(String s)
{
	String s1 = s.replace("#", "%23");
	s1 = s1.replace("?", "%3F");
	return s1;
}

/** given any string, returns the url string for it which should be XSS-safe.
 */
public static String URLEncode(String s)
{
	try {
		return URLEncoder.encode(s, "UTF-8");
	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
		return null;
	}
}

public static String tail(String s, String separator)
{
	if (s == null)
		return null;
	int idx = s.lastIndexOf(separator);
	if (idx >= 0)
		return s.substring(idx+1);
	else
		return s;
}

/** returns the component of the url after the last / i.e. the name of the actual file in the URL.
 * returns null if the input is null.
 */
public static String URLtail(String url)
{
	return tail (url, "/");
}

/** returns the component of the url after the last / OR \
 */
public static String filePathTail(String filePath)
{
	String t = tail (filePath, "/");
	t = tail (t, "\\");
	return t;
}

/** if s begins with prefix, strips prefix and returns s. otherwise returns original s */
public static String stripFrom(String s, String prefix)
{
	if (s.startsWith(prefix))
		return s.substring(prefix.length());
	else
		return s;
}


/** if s begins with prefix, strips prefix and returns s. otherwise returns original s */
public static String userIdFromEmail(String email)
{
	int idx = email.indexOf("@");
	if (idx < 0)
		return email;
	else
		return email.substring(0, idx);
}


// remove given chars from beginning or end of given string
public static String removeCharsFromBeginOrEnd(String s, String tabooAtBeginOrEnd)
{
	// strip from the end
	while (s.length() > 0 && tabooAtBeginOrEnd.indexOf(s.charAt(s.length()-1)) >= 0)
		s = s.substring(0, s.length()-1);

	// strip from the beginning
	if (s.length() >= 1)
		while (tabooAtBeginOrEnd.indexOf(s.charAt(0)) >= 0)
		{
			if (s.length() == 0)
				break;
			s = s.substring(1);
		}
	return s;
}

public static List<String> stripCommonPrefix(List<String> list)
{
	if (list.size() == 0)
		return list;

	String commonPrefix = list.get(0);
	for (String s: list)
	{
		int matchLen = 0;
		for (; matchLen < commonPrefix.length(); matchLen++)
		{
			if (s.charAt(matchLen) != commonPrefix.charAt(matchLen))
				break;
		}

		commonPrefix = commonPrefix.substring(0, matchLen);
	}

	commonPrefix = commonPrefix.substring (0, commonPrefix.lastIndexOf(File.separatorChar)+1);
	if (commonPrefix.length() <= 1)
		return list;

	List<String> result = new ArrayList<>();
	for (String s: list)
		result.add(s.substring(commonPrefix.length()));

	return result;
}

public static String arrayToJson(int[] x)
{
	StringBuilder sb = new StringBuilder("[");
	for (int i: x)
	{
		if (sb.length() > 1) // not the first
			sb.append (", ");
		sb.append (i);
	}
	sb.append("]");
	return sb.toString();
}

/** remove everything in str before the last = */
public static String strippedEmailAddress(String str)
{
	int idx = str.lastIndexOf('=');
	if (idx < 0)
		return str;

	str = str.substring(idx);
	if (str.length() == 1)
		return str; // funny... str ends with '='. let's not mess with it.

	return str.substring(1);
}

	/** util method */
private static String bytesToHexString(byte[] bytes) 
{
	// http://stackoverflow.com/questions/332079
	// http://stackoverflow.com/questions/7166129
	StringBuilder sb = new StringBuilder();
	for (byte aByte : bytes) {
		String hex = Integer.toHexString(0xFF & aByte);
		if (hex.length() == 1)
			sb.append('0');
		sb.append(hex);
	}
	return sb.toString();
}

/** SHA-256 hash */
public static String hash(String s)
{
	MessageDigest digest=null;
	String hash = null;
	try {
		digest = MessageDigest.getInstance("SHA-256");
		digest.update(s.getBytes());
		hash = bytesToHexString(digest.digest());
	} catch (NoSuchAlgorithmException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	return hash;
}

public static String hash(String s, Map<String, String> map)
{
	MessageDigest digest=null;
	String hash = null;
	try {
		digest = MessageDigest.getInstance("SHA-256");
		digest.update(s.getBytes());
		hash = bytesToHexString(digest.digest());
	} catch (NoSuchAlgorithmException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	if (map != null)
		map.put (hash, s);
	return hash;
}

public static void writeObjectToFile(String filename, Serializable s) throws IOException
{
	ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename));
	oos.writeObject(s);
	oos.close();
}

public static Serializable readObjectFromFile(String filename) throws IOException, ClassNotFoundException
{
	ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename));
	Serializable s = (Serializable) ois.readObject();
	return s;
}

public static void close(Closeable resource) {
    if (resource != null) {
        try {
            resource.close();
        } catch (IOException e) {
            // Do your thing with the exception. Print it, log it or mail it.
            e.printStackTrace();
        }
    }
}

public static<E> boolean hasRedundantElements(Collection<E> c)
{
	Map<E,E> m = new LinkedHashMap<>();
	for (E e : c) {
		if (m.containsKey(e)) {
			E e1 = m.get(e);
			//assert(e == e1); // that would just fail
			assert(e.equals(e1));
			return true;
		}
		m.put(e, e);
	}
	Set<E> s = new LinkedHashSet<>(c);
	assert(s.size() <= c.size());
	return s.size() != c.size();
}

public static File createTempDirectory() throws IOException
{
    final File temp = File.createTempFile("muse_", "_contents");

    if(!(temp.delete()))
    {
        throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
    }

    if(!(temp.mkdir()))
    {
        throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
    }

    return (temp);
}

public static String maskEmailDomain(String s)
{
    return s.replaceAll("\\b([A-Za-z0-9][A-Za-z0-9\\-_\\.]*)@[A-Za-z0-9][A-Za-z\\-0-9_]*(\\.[A-Za-z0-9][A-Za-z\\-0-9_]*)*\\.[A-Za-z]{2,4}\\b", "$1@...");
}

public static<E extends Comparable<? super E>> int compareToNullSafe(E a, E b)
{
    if (a == b) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    return a.compareTo(b);
}

public static<E> boolean equalsNullSafe(E a, E b)
{
    if (a == null)
        return b == null;
    else
        return a.equals(b);
}

/**
 * Return list1 - list2. require that the elements must be sortable.
 * @param list1
 * @param list2
 * @return
 */
public static<E extends Comparable<? super E>> List<E> getRemoveAll(List<E> list1, Collection<E> list2)
{
	Set<E> set1 = new LinkedHashSet<>(list1);
	set1.removeAll(list2);
	return new ArrayList<>(set1);
}

/** Return int[] from String[] */
public static int[] toIntArray(String[] arr)
{
	int[] result = null;
	if (arr != null) {
		result = new int[arr.length];
		for (int i = 0; i < arr.length; i++)
			result[i] = Integer.parseInt(arr[i]);
	}
	return result;
}

public static int maxTokenLength(String s)
{
	if (s == null)
		return 0;
	
	int max = 0;
	StringTokenizer st = new StringTokenizer(s);
	while (st.hasMoreTokens())
	{
		int tokenLength = st.nextToken().length();
		if (tokenLength > max)
			max = tokenLength;
	}
	return max;
}

public static int nLetterChars(String s)
{
	int count = 0;
	for (char c: s.toCharArray())
		if (Character.isLetter(c))
			count++;
	return count;
}

/** replaces sequences of space chars with one space */
public static String canonicalizeSpaces(String s)
{
	// includes replacement for 0xA0 (nbsp), which is not handled by \s alone
	// http://stackoverflow.com/questions/1702601/unidentified-whitespace-character-in-java
	if (s == null)
		return null;
	return s.replaceAll("[\\s\\xA0]+", " ");
}

/**
 * Returns the input cast as Set (modifiable) if it is indeed one,
 * or clone it as a Set. Returns null if the input is null.
 */
public static<E> Set<E> castOrCloneAsSet(Collection<E> c)
{
    return (c == null || c instanceof LinkedHashSet) ? (Set<E>) c : new LinkedHashSet<>(c);
}


public static List<String> scrubNames(Collection<String> list)
{
	Set<String> set = new LinkedHashSet<>();
	for (String s : list) {
		s = s.replaceAll("[\\r\\n\\a]+", " ")		// newlines
			 .replaceAll("\\s+", " ")				// whitespaces compaction
			 .replaceAll("</?[A-Za-z]+[^>]*>", "")	// HTML tags
			 ;
		if (set.contains(s)) continue; // could also do case insensitive
		set.add(s);
	}
	return new ArrayList<>(set);
}

// both arguments have to agree on being or not being URL escaped (probably have to be escaped since we assume "&" is the delimiter)
public static int indexOfUrlParam(String allParams, String param)
{
	allParams += "&"; // sentinel
	param += "&"; // to prevent prefix matching (e.g., param = "foo=12" should not match allParams = "foo=123")
	return allParams.toLowerCase().indexOf(param.toLowerCase());
}

public static String excludeUrlParam(String allParams, String param)
{
	int startIdx = indexOfUrlParam(allParams, param);
	if (startIdx < 0)
	{
		//JSPHelper.doConsoleLoggingWarnings ("unexpected! facet already selected but not in params: " + allParams);
		return allParams;
	}

	int endIdx = startIdx + param.length();
	if (startIdx > 0 && allParams.charAt(startIdx-1) == '&')
		startIdx--; // exclude preceding & also if present
	if (endIdx < allParams.length() && startIdx == 0 && allParams.charAt(endIdx) == '&') // should not need to check == '&'
		endIdx++; // exclude following & if that becomes the first param (should be harmless to leave it there anyway)

	// splice out [startIdx, endIdx) ; notice the exclusive upper end
	String newParams = allParams.substring(0, startIdx);
	newParams += allParams.substring(endIdx);
	return newParams;
}

public static boolean isWindowsPlatform()
{
	return (System.getProperty("os.name").toLowerCase().contains("windows"));
}

public static String devNullPath()
{
	return isWindowsPlatform() ? "NUL" : "/dev/null"; 
}

}
