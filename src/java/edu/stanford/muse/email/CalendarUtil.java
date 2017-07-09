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
package edu.stanford.muse.email;

import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/** bunch of utils for manipulating date ranges and splitting them
 up into intervals based on exchanges with contacts etc. */
public class CalendarUtil {
    public static Log log = LogFactory.getLog(CalendarUtil.class);

    private static String[] monthStrings = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
	//private static String[] monthStrings = new DateFormatSymbols().getMonths();
	private final static int nMonthPerYear = monthStrings.length;

	public static String getDisplayMonth(int month)
	{
		return monthStrings[month];
	}

	public static String getDisplayMonth(Calendar c)
	{
		// we don't want to be dependent on Calendar.getDisplayName() which is in java 1.6 only
		// return c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " " + c.get(Calendar.YEAR);
		// could also use a simpledataformat here.
		int month = c.get(Calendar.MONTH); // month is 0-based
		return monthStrings[month];
	}

	public static String getDisplayMonth(Date d)
	{
		Calendar c = new GregorianCalendar();
		c.setTime(d);
		return getDisplayMonth(c);
	}

	public static int getDiffInMonths(Date firstDate, Date lastDate)
	{
		Calendar cFirst = new GregorianCalendar(); cFirst.setTime(firstDate);
		Calendar cLast = new GregorianCalendar(); cLast.setTime(lastDate);
		int cFirst_year = cFirst.get(Calendar.YEAR);
		int cFirst_month = cFirst.get(Calendar.MONTH);
		int cLast_year = cLast.get(Calendar.YEAR);
		int cLast_month = cLast.get(Calendar.MONTH);
		return (cLast_year - cFirst_year) * (cLast.getMaximum(Calendar.MONTH)-cLast.getMinimum(Calendar.MONTH)+1) + (cLast_month - cFirst_month);
	}

	public static int[] getNextMonth(int year, int month)
	{
		month++;
		year += month/nMonthPerYear;
		month %= nMonthPerYear;
		int[] result = new int[]{ year, month };
		return result;
	}

	/** divides the given time range into intervals */
	public static List<Date> divideIntoIntervals(Date start, Date end, int nIntervals)
	{
		List<Date> result = new ArrayList<Date>();

		long startMillis = start.getTime();
		long endMillis = end.getTime();
		Util.ASSERT (endMillis >= startMillis);

		long gap = (endMillis - startMillis)/nIntervals;

		result.add(start);
		for (int i = 1; i < nIntervals; i++)
		{
			long millis = (startMillis + i * gap);
			result.add(new Date(millis));
		}
		result.add(end);

		return result;
	}

	public static Calendar startOfMonth(Date d)
	{
		Calendar c = new GregorianCalendar();
		c.setTime(d);
		int yy = c.get(Calendar.YEAR);
		int mm = c.get(Calendar.MONTH);
		// get new date with day of month set to 1
		c = new GregorianCalendar(yy, mm, 1);
		return c;
	}

	/** returns a list of monthly intervals for the first of each month
	 * the first entry in the list is the 1st of the month before start
	 * and the last entry is the 1st of the month after the end.
	 */
	public static List<Date> divideIntoMonthlyIntervals(Date start, Date end)
	{
		List<Date> result = new ArrayList<Date>();

		// result will always have at least 2 entries
		Calendar c = startOfMonth(start);
		result.add(c.getTime());
		do {
			int mm = c.get(Calendar.MONTH);
			int yy = c.get(Calendar.YEAR);
			mm++;
			// months are 0-based (jan = 0)
			if (mm >= 12)
			{
				mm = 0;
				yy++;
			}
			c = new GregorianCalendar(yy, mm, 1);
			result.add(c.getTime());
		} while (c.getTime().before(end)); // stop when we are beyond end

		return result;
	}

    public static int[] computeHistogram(List<Date> dates, List<Date> intervals) { return computeHistogram(dates, intervals, true); }

    /** intervals must be sorted, start from before the earliest date, and end after the latest date */
	public static int[] computeHistogram(List<Date> dates, List<Date> intervals, boolean ignoreInvalidDates)
	{
		if (intervals == null || intervals.size() == 0)
			return new int[0];

		int nIntervals = intervals.size()-1;
		int[] counts = new int[nIntervals];

		if (ignoreInvalidDates) {
			int count = 0;
			List<Date> newDates = new ArrayList<Date>();
			for (Date d : dates)
				if (!EmailFetcherThread.INVALID_DATE.equals(d))
					newDates.add(d);
				else
					count++;
			dates = newDates;
			if (count > 0)
				log.info (count + " invalid date(s) ignored");
		}

		if (dates.size() == 0)
			return counts;

		Collections.sort(dates);

		Date firstDate = dates.get(0);
		Date lastDate = dates.get(dates.size()-1);
		// intervals are assumed to be already sorted
		if (firstDate.before (intervals.get(0)))
			throw new RuntimeException("INTERNAL ERROR: invalid dates, first date " + formatDateForDisplay(firstDate)  + " before intervals start " + formatDateForDisplay(intervals.get(0)) + ", aborting histogram computation");
		if (lastDate.after(intervals.get(intervals.size()-1)))
			throw new RuntimeException("INTERNAL ERROR: invalid dates, last date " + formatDateForDisplay(firstDate)  + " after intervals end " + formatDateForDisplay(intervals.get(intervals.size()-1)) + ", aborting histogram computation");

		int currentInterval = 0;
		int thisIntervalCount = 0; // running count which we are accumulating into the current interval
		// no need to track currentIntervalStart explicitly
		Date currentIntervalEnd = intervals.get(currentInterval+1);

		// we'll run down the sorted dates, counting dates in each interval
		for (Date currentDate: dates)
		{
			if (currentDate.after (currentIntervalEnd))
			{
				// we're done with current interval, commit its count
				counts[currentInterval] = thisIntervalCount;

				// find the next interval, skip over till current Date is before the interval end
				do {
					currentInterval++;
					currentIntervalEnd = intervals.get(currentInterval+1);
				} while (currentDate.after(currentIntervalEnd));
				// now current date is before the current interval end, so we have the new interval reflected in currentIntervalEnd
				// count currentDate in this interval
				thisIntervalCount = 1;
			}
			else
				thisIntervalCount++; // still not reached end of interval
		}
		// the last interval's end was not exceeded, so set up its count here
		counts[currentInterval] = thisIntervalCount;

		return counts;
	}

	/** computes activity chart as HTML */
	public static String computeActivityChartHTML(double[] normalizedHistogram)
	{
		StringBuilder sb = new StringBuilder();
		for (double d: normalizedHistogram)
		{
			int colorVal;
			if (d < 0.000001)
				colorVal = 0;
			else
			{
				colorVal = 64 + (int) (d * 192);
				if (colorVal > 255)
					colorVal = 255; // saturate in case of corner cases
			}

			String colorString = Integer.toHexString(colorVal);
			if (colorString.length() == 1)
				colorString = "0" + colorString; // add leading 0, otherwise stylesheet is not valid

			colorString += colorString;
			colorString += "00";
			// colorString += "0000"; // assuming colorString is going into red
			// toString a dummy space with the appropriate background color
			sb.append ("<x style=\"background-color:#" + colorString + "\">&nbsp;</x>");  // inactive unless above a miniscule percentage
		}
		return sb.toString();
	}

	public static String formatDateForDisplay(Date d)
	{
		if (d == null)
			return "<No Date>";
	    SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");
	    return formatter.format(d);
	}

	/** returns date object for the first day of the given month. m is 0 based */
	public static Date convertYYMMToDate(int y, int m, boolean beginning_of_day)
	{
		return convertYYMMDDToDate(y, m, 1, beginning_of_day);
	}

    public static Date convertYYMMDDToDate(int y, int m, int d, boolean beginning_of_day)
    {
        // if m is out of range, its equiv to 0
        if (m < 0 || m > 11)
            m = 0;
        if (d<0 || d>30)
            d = 0;
        GregorianCalendar c = new GregorianCalendar();
        c.set(Calendar.YEAR, y);
        c.set(Calendar.MONTH, m);
        c.set(Calendar.DATE, d);
        if (beginning_of_day)
        {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
        }
        else
        {
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.SECOND, 59);
        }
        return c.getTime();
    }
	
    public static Pair<Date, Date> getDateRange(int startY, int startM, int endY, int endM){
		return getDateRange(startY, startM, -1, endY, endM, -1);
    }

	/** cool method to convert a pair of <yy, mm, dd> specs per Gregorian calendar to a date range.
	 * Note: startM, endM are 0-based, startD, endD are 1-based.
	 * startY/endY must be valid (>= 0), otherwise a null is returned for start / end dates.
	 * Note: all months are 0-based, but days-of-month start from 1.
	 * startM/endM/startD/endD can be invalid (< 0, or also > 11 in the case of months) in which case they are treated as "*".
	 * i.e. put to their min values for start, or to max values for end.
	 * if startM is invalid, startD is ignored and also considered invalid. likewise for endM/endD.
	 * no handling of time zone, default TZ is assumed.
	 * */
	public static Pair<Date, Date> getDateRange(int startY, int startM, int startD, int endY, int endM, int endD)
    {
		Date startDate = null;
		// Calendar.JANUARY is 0, and Calendar.DECEMBER is 11
		if (startY >= 0) {
			// check startM
			if (startM < Calendar.JANUARY || startM > Calendar.DECEMBER) {
				// invalid startM, assign M and D to Jan 1
				startM = Calendar.JANUARY;
				startD = 1;
			} else {
				if (startD <= 0) // invalid startD
					startD = 1;
			}
			startDate = convertYYMMDDToDate(startY, startM, startD, true);
		}

		// endM/endD will be set to be BEYOND the end of the desired day/month/year.
		// e.g. if the end y/m/d params are 2001/-1/<whatever>, we want end y/m/d to correspond to 2002 1st Jan
		// and we'll compute endDate back to EOD 2001 31st Dec.
		// if the end y/m/d params are 2001/5/-1, we want y/m/d to become correspond to 2001, 1st June and we'll set endDate back to
		// EOD on 2001, 31st May.
		Date endDate = null;

		if (endY >= 0) {
			if (endM < Calendar.JANUARY || endM > Calendar.DECEMBER) {
				// invalid endM (and endD), therefore set to end
				endM = Calendar.JANUARY;
				endD = 1;
				endY++;
			} else {
				if (endD <= 0) {
					// no date provided, so just bump month.
					endD = 1;
					endM++;
					if (endM > Calendar.DECEMBER) {
						// obviously account for rollovers. so 2001/11/-1 sets end y/m/d to 2002/0/1
						endY++;
						endM = Calendar.JANUARY;
					}
				} else {
					endD++;
					// bump day, but need to check if its more than the allowed days for that month. misery!
					// http://stackoverflow.com/questions/8940438/number-of-days-in-particular-month-of-particular-year
					Calendar tmp = new GregorianCalendar(endY, endM, 1);
					int maxDays = tmp.getActualMaximum(Calendar.DAY_OF_MONTH);
					if (endD > maxDays) {
						endD = 1;
						// bump month
						endM++;
						if (endM > Calendar.DECEMBER) {
							// obviously account for rollovers. so 2001/11/-1 sets end y/m/d to 2002/0/1
							endY++;
							endM = Calendar.JANUARY;
						}
					}
				}
			}
			Date beyond_end = convertYYMMDDToDate(endY, endM, endD, true);
			endDate = new Date(beyond_end.getTime()-1001L);
		}

		log.info ("date range: " + startDate + "-" + endDate);
        return new Pair<Date, Date>(startDate, endDate);
    }

	/** the quarter beginning just before this d */
	public static Date quarterBeginning(Date d)
	{
		Calendar c = new GregorianCalendar();		
		c.setTime(d);
		
		int m = c.get(Calendar.MONTH);
		int qrtrBegin = 3 * (m/3);
		c.set(Calendar.MONTH, qrtrBegin);
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}
	
	public static void main (String args[])
	{
		// tests for get date range

		Pair<Date, Date> p = getDateRange(2010, 10, 2011, 5); // 2010 nov through june 2011
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2010, 10, 2011, -1); // 2010 Nov through end 2011
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2010, -1, 2011, -1); // all of 2010 and 2011
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2010, -1, 2010, -1); // all of 2010
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2010, -1, -1, 2010, -1, -1); // all of 2010
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2007, -1, -1, 2007, 1, -1); // begin 2007 through end Feb (28th)
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2008, -1, -1, 2008, 1, -1); // begin 2008 through end Feb (29th)
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2010, 10, -1, 2011, 5, -1); // begin 2010 through end June 2011
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2010, -1, 5, 2011, -1, 5); //begin 2010 through end of 2011
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2010, -1, 5, 2011, 11, 25); //begin 2010 through end of 25th dec 2011
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2010, -1, 5, 2011, 11, -1); // begin 2010 through end of 2011
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(2010, -1, 5, 2011, 11, 31); //begin 2010 through end of 2011
		System.out.println(p.getFirst() + " - " + p.getSecond());

		p = getDateRange(-1, 1, 1, -1, 1, 1); // invalid years, returns null, null;
		System.out.println(p.getFirst() + " - " + p.getSecond());

		Date d = new Date();
		for (int i = 0; i < 100; i++)
		{
			d = quarterBeginning(d);
			System.out.println (formatDateForDisplay(d));
			d.setTime(d.getTime() - 1L);
		}
	}
}
