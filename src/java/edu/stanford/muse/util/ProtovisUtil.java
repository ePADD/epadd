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
import java.util.Collection;
import java.util.Date;
import java.util.List;

import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.email.CalendarUtil;
import edu.stanford.muse.index.DatedDocument;

/* simple class, most methods have been removed from it. the remaining ones also can be moved to some util class */
public class ProtovisUtil {

	public static int normalizingMax(Collection<DatedDocument> docs,  AddressBook addressBook, List<Date> intervals)
	{
		List<Date> dates = new ArrayList<>();
		for (DatedDocument dd: docs) {
			if(!dd.hackyDate)
				dates.add(dd.date);
		}

		return findNormalizingMax(dates, intervals);
	}


	private static int findNormalizingMax(List<Date> dates, List<Date> intervals)
	{
		if (dates == null)
			return Integer.MIN_VALUE;
		int[] histogram = CalendarUtil.computeHistogram(dates, intervals);
		int max = Integer.MIN_VALUE;
		for (int x : histogram)
			if (x > max)
				max = x;
		return max;
	}
}
