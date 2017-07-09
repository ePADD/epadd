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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** intern table, not shared with RoW. not thread safe */
public class InternTable {
	// normally i always use LinkedHashMap, but this one appears to be memory critical.
	// not sure if it saves too much, but probably around 10% ?
	private final static Map<String,String> internTable = new HashMap<String, String>(10000,0.75f);
	private static int internTableChars = 0;
	public static String intern(String s)
	{
		if (Trace.isEnabled)
			Trace.trace("interning", s);

		String x = internTable.get(s);
		if (x != null)
			return x;

		if (Trace.isEnabled)
			Trace.trace("new term in intern table", s);

		internTable.put (s, s);
		internTableChars += s.length();
		return s;
	}

	public static int getSizeInChars()
	{
		return internTableChars;
	}

	public static int getNEntries()
	{
		return internTable.size();
	}

	public static void clear()
	{
		internTable.clear();
		internTableChars = 0;
	}

	public static void dump(String filename) throws IOException
	{
		List<String> list = new ArrayList<String>(internTable.keySet());
		//Collections.sort(list);
		PrintWriter pw = new PrintWriter(new FileOutputStream(filename));
		for (String s: list)
			pw.println (s);
		pw.close();
	}
}
