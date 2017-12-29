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
package edu.stanford.muse.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.stanford.muse.email.CalendarUtil;

/** a sloppy date parser that can handle natural lang. ways of specifying a date, like jan 10, sep 29, etc. */
public class SloppyDates {
    private static Log log = LogFactory.getLog(SloppyDates.class);

	private static String[] monthNames = {"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"};
	private static Triple<Integer, Integer, Integer> parseDate(String s)
	{
		// separate into month and date
		// "jan10", "10jan", "jan 10" "10 jan" should all work
		s = s.toLowerCase();
		s = s.trim();
		StringBuilder sb = new StringBuilder();
		// detect when string changes from alpha to num or vice versa and ensure a whitespace there
		boolean prevCharDigit = false, prevCharLetter = false;
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (Character.isWhitespace(c))
			{
				sb.append (c);
				prevCharDigit = prevCharLetter = false;
				continue;
			}
			// treat apostrophe like a space
			if (c == '\'')
			{
				sb.append (' ');
				prevCharDigit = prevCharLetter = false;
				continue;
			}

			if (Character.isLetter(c))
			{
				if (prevCharDigit)
					sb.append (' ');
				sb.append (c);
				prevCharLetter = true;
				prevCharDigit = false;
			}
			else if (Character.isDigit(c))
			{
				if (prevCharLetter)
					sb.append (' ');
				sb.append (c);
				prevCharDigit = true;
				prevCharLetter = false;
			}
			else
				throw new RuntimeException();
		}

		String newS = sb.toString();
		log.info ("string " + s + " parsed to " + newS);
		StringTokenizer st = new StringTokenizer(newS);

		int nTokens = st.countTokens();
		if (nTokens == 0 || nTokens > 3)
			return new Triple<Integer, Integer, Integer>(-1, -1, -1);

		int mm = -1, dd = -1, yy = -1;
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			boolean isNumber = true;
			int num = -1;
			try { num = Integer.parseInt(token); } catch (NumberFormatException nfe) { isNumber = false; }
			if (isNumber && num < 0)
				return new Triple<Integer, Integer, Integer>(-1, -1, -1);
			if (isNumber)
			{
				if (dd == -1 && num > 0 && num <= 31)
					dd = num;
				else if (yy == -1)
				{
					yy = num;
					if (yy < 100)
					{
						yy = (yy > 12) ? (1900 + yy) : (2000 + yy);
					}
					if (yy < 1900 || yy > 2015)
						return new Triple<Integer, Integer, Integer>(-1, -1, -1);
				}
				else
					return new Triple<Integer, Integer, Integer>(-1, -1, -1);
			}
			else
			{
				int x = SloppyDates.uniquePrefixIdx(token, monthNames);
				if (x >= 0 && mm == -1)
					mm = x;
				else
					return new Triple<Integer, Integer, Integer>(-1, -1, -1);
			}
		}
		return new Triple<Integer, Integer, Integer>(dd, mm, yy);
	}

	/** checks if s is a unique prefix for any of the given terms.
	 * returns -1 if s is not a prefix for any term.
	 * returns -2 if s is a prefix for more than one term.
	 */
	private static int uniquePrefixIdx(String s, String[] terms)
	{
		int match = -1;
		for (int i = 0; i < terms.length; i++)
		{
			String term = terms[i];
			if (term.startsWith(s))
			{
				if (match != -1)
					return -2;
				else
					match = i;
			}
		}
		return match;
	}

	public static List<DateRangeSpec> parseDateSpec(String dateSpec)
	{
		List<DateRangeSpec> result = new ArrayList<DateRangeSpec>();
 		StringTokenizer st = new StringTokenizer(dateSpec, ".,;");
 		while (st.hasMoreTokens())
 		{
 			String token = st.nextToken();
 			result.add (new DateRangeSpec(token));
 		}
 		return result;
	}

	public static class DateRangeSpec {
		boolean specificDate;
		Triple<Integer, Integer, Integer> startDate, endDate;

		public DateRangeSpec (String spec)
		{
 			StringTokenizer st1 = new StringTokenizer(spec, "-");
 			if (st1.countTokens() == 1)
 				setRange(parseDate(st1.nextToken()));
 			else
 				setRange(parseDate(st1.nextToken()), parseDate(st1.nextToken()));
		}

		public void setRange(Triple<Integer, Integer, Integer> t)
		{
			this.startDate = t;
			this.endDate = t;
		}

		public void setRange(Triple<Integer, Integer, Integer> startDate, Triple<Integer, Integer, Integer> endDate)
		{
			this.startDate = startDate;
			this.endDate = endDate;
		}

		public boolean satisfies(Date d)
		{
			Calendar c = new GregorianCalendar();
			c.setTime(d);
			int c_dd = c.get(Calendar.DATE);
			int c_mm = c.get(Calendar.MONTH);
			int c_yy = c.get(Calendar.YEAR);

			// for start, end: mm, dd, yy = -1 => wildcard for that field.
			int start_dd = this.startDate.getFirst();
			int start_mm = this.startDate.getSecond();
			int start_yy = this.startDate.getThird();
			int end_dd = this.endDate.getFirst();
			int end_mm = this.endDate.getSecond();
			int end_yy = this.endDate.getThird();

			// if any of the start/end's are wildcards, just set the calendar to the same value
			if (start_yy == -1)
				c_yy = end_yy;
			else if (end_yy == -1)
				c_yy = start_yy;
			if (start_mm == -1)
				c_mm = end_mm;
			else if (end_mm == -1)
				c_mm = start_mm;
			if (start_dd == -1)
				c_dd = end_dd;
			else if (end_dd == -1)
				c_dd = start_dd;

			// convert all 3 dates to decimal yyyymmdd
			int startDate = (start_yy+1) * 10000 + (start_mm+1) * 100 + (start_dd);
			int endDate = (end_yy+1) * 10000 + (end_mm+1) * 100 + (end_dd);
			int cDate = (c_yy+1) * 10000 + (c_mm+1) * 100 + (c_dd);

			// now ready to compare
			return (startDate <= cDate && cDate <= endDate);
		}
	}

	public static void main (String args[])
	{
		Triple<Integer, Integer, Integer> t;
		t = parseDate(" JAN 10");
		System.out.println (t.getFirst() + " " + t.getSecond() + " " + t.getThird());
		t = parseDate(" jul10");
		System.out.println (t.getFirst() + " " + t.getSecond() + " " + t.getThird());
		t = parseDate("10May");
		System.out.println (t.getFirst() + " " + t.getSecond() + " " + t.getThird());
		t = parseDate("10 seP");
		System.out.println (t.getFirst() + " " + t.getSecond() + " " + t.getThird());
		t = parseDate("sep 11 2001");
		System.out.println (t.getFirst() + " " + t.getSecond() + " " + t.getThird());
		t = parseDate("dec 31 1999");
		System.out.println (t.getFirst() + " " + t.getSecond() + " " + t.getThird());
		t = parseDate("dec 31");
		System.out.println (t.getFirst() + " " + t.getSecond() + " " + t.getThird());
		t = parseDate("dec 31 2009");
		System.out.println (t.getFirst() + " " + t.getSecond() + " " + t.getThird());
		t = parseDate("dec 31'09");
		System.out.println (t.getFirst() + " " + t.getSecond() + " " + t.getThird());
	}
}
