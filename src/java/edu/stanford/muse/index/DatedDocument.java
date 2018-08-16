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
package edu.stanford.muse.index;


import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.stanford.muse.email.CalendarUtil;
import edu.stanford.muse.util.Util;

/** class for any document that has a date associated with it.
 * documents can be compared/sorted on the basis of date.
 */
public class DatedDocument extends Document {
	private static final long serialVersionUID = 2208220822082208L;

	private static Log log = LogFactory.getLog(DatedDocument.class);

    public Date date;
    public boolean hackyDate; // indicates that we're not certain about the date
    
	DatedDocument() { /* dummy for serialization */ }

	public Date getDate() { return date; }

	DatedDocument(String id, String s, Date d)
	{
		super (id, s);
		this.date = d;
	}

	@Override
	public String toString() { return super.toString() + (hackyDate ? " guessed ":" ") + "date " + dateString(); }
	
	@Override
	public int compareTo(Document o)
	{
		DatedDocument other = (DatedDocument) o;
		if (this == other) return 0;

		int result = Util.compareToNullSafe(this.date,other.date);
		if(result!=0) {
//			//check if the application of Util.compareToNullSafe(other.date,this.date) is different sign than this one.
//			int tmp = Util.compareToNullSafe(other.date,this.date);
//			if(tmp<0 && result<0)
//				assert(Util.softAssert(true, "Some error as both can't be of same sign", JSPHelper.log));
//			if(tmp>0 && result>0)
//				assert(Util.softAssert(true, "Some error as both can't be of same sign", JSPHelper.log));
			return result;
		}
		/*if (this.date != other.date) {
			// check for null date and ordered it after
			if (this.date == null) return -1;
			if (other.date == null)	return 1;

			int result = date.compareTo(other.date);
			if (result != 0) return result;
		}*/

		//return this.toString().compareTo(other.toString()); // this would compare docId which we may not want to. 
		//return super.compareTo(other); // this would compare url which we may not want to.
		return Util.compareToNullSafe(description, other.description);
	}

	/** documents are equal if they have the same subject (description)
	 * and the exact same date
	 */
	@Override
	public boolean equals (Object o)
	{
		if (!(o instanceof Document))
			return false;

		DatedDocument other = (DatedDocument) o;

		if (!Util.equalsNullSafe(date, other.date))
			return false;

		// both dates are equal (both may be null)
		//return super.compareTo((Document) o) == 0; // this would compare url which we may not want to.
		return Util.equalsNullSafe(description, other.description);
	}

	@Override
	public int hashCode()
	{
		int result = 0;
		if (date != null)
			result = result ^ date.hashCode();
		if (description != null)
			result = result*37 ^ description.hashCode();
		return result;
	}

	public String dateString()
	{
		if (date != null)
		{
			Calendar c = new GregorianCalendar();
			c.setTime(date);
			return c.get(Calendar.DATE) + " " + CalendarUtil.getDisplayMonth(c) + " " + c.get(Calendar.YEAR);
		}
		else
			return "????-??-??";
	}

	/** invoke only from getHTMLForHeader, needs specific context of date etc. */
	private StringBuilder getHTMLForDate()
	{
		StringBuilder result = new StringBuilder();

		if (date != null)
		{
			Calendar c = new GregorianCalendar();
			c.setTime(date);
			result.append ("<tr><td width=\"7%\" align=\"right\" class=\"muted\">Date: </td><td>" + CalendarUtil.getDisplayMonth(c) + " " + c.get(Calendar.DATE) + ", " + c.get(Calendar.YEAR));
		}
		return result;
	}

	private StringBuilder getHTMLForSubject()
	{
		StringBuilder result = new StringBuilder();
		String x = this.description;
		if (x == null)
			x = "<None>";
		result.append ("<tr><td width=\"7%\" align=\"right\" class=\"muted\">Title: </td><td align=\"left\"><b>" + x + "</b>\n");
		result.append ("\n</td></tr>\n");
		return result;
	}

	public StringBuilder getHTMLForHeader() {
		StringBuilder result = new StringBuilder();
		// header table
		result.append ("<table class=\"docheader rounded\">\n");
		result.append (getHTMLForSubject());
		result.append (getHTMLForDate());
		result.append ("</table>");
		return result;
	}

	public static StringBuilder formatStringForMaxCharsPerLine(String contents, int CHARS_PER_LINE)
	{
		StringBuilder sb = new StringBuilder();

		// should be in one time setup
		final String punctuationChars = "., \t!)]";
		final Set<Character> punctuationCharSet = new LinkedHashSet<>();
		for (int i = 0; i < punctuationChars.length(); i++)
			punctuationCharSet.add(punctuationChars.charAt(i));

		LineNumberReader lnr = new LineNumberReader(new StringReader(contents));
		while (true)
		{
			String line = null;
			try { line = lnr.readLine(); } catch (IOException ioe) { sb.append ("Unexpected error while formatting line"); log.warn ("Unexpected io exception while formatting max chars per line: " + Util.stackTrace(ioe)); }
			if (line == null)
				break;

			if (line.length() < CHARS_PER_LINE)
			{
				// common case, short line, nothing more to be done.
				sb.append(line);
				sb.append("\n");
			//	log.info ("short line: \"" + line + "\"");
				continue;
			}

			// line is longer than CHARS_PER_LINE. try to break it up.
			String remainingLine = line;
			while (true)
			{
				if (remainingLine.length() <= CHARS_PER_LINE)
				{
					sb.append(remainingLine);
					break;
				}

				// line is too long, try to break it up, look backwards from CHARS_PER_LINE to see if there's some punctuation
				int i = CHARS_PER_LINE - 1;
				for (; i >= 0; i--)
				{
					if (punctuationCharSet.contains(remainingLine.charAt(i)))
						break;
				}

				// i is the index of the punct. char, or -1 if no punct.
				// emit the line, including the punct.
				if (i == -1)
				{
					// no break found, emit chars as is
					sb.append(remainingLine.substring(0, CHARS_PER_LINE));
			//		log.info ("no break: \"" + remainingLine.substring(0, CHARS_PER_LINE) + "\"");

					remainingLine = remainingLine.substring(CHARS_PER_LINE);
				}
				else
				{
					// break found
					sb.append(remainingLine.substring(0, i+1));
					sb.append("\n");
				//	log.info ("found line: \"" + remainingLine.substring(0, i+1) + "\"");
					if (remainingLine.length() <= (i+1))
						break; // nothing remains

					remainingLine = remainingLine.substring(i+1);
				}
			}
			sb.append("\n");
		//	log.info ("appended newline");
		}
		return sb;
	}

	/*
	public StringBuilder getHTMLForContents(Indexer indexer) throws IOException
	{
		String contents = "\n\nContents not downloaded.\n\n";
		try {
			if (indexer == null)
				contents = getContents();
			else
				contents = getContents();

			contents = contents.replace("\n", "<p>\n");
//				contents = indexer.getHTMLAnnotatedDocumentContents(this);
		}
		catch (Exception e) { log.warn(e + Util.stackTrace(e)); }
		return new StringBuilder(contents);
	}

	public String toString()
	{
		return super.toString() + " " + dateString();
	}
	*/
}
