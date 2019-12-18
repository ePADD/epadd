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
package edu.stanford.muse.webapp;

import edu.stanford.muse.util.DetailedFacetItem;
import edu.stanford.muse.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/** called NewFilter only because we have an old class called Filter, that is mostly not used */
public class NewFilter {
	private final Map<String, String[]> map = new LinkedHashMap<>();
    private static final Logger log =  LogManager.getLogger(NewFilter.class);
	
	/** in future, could create Filter from a list of detailed facet items to make it more robust
	// currently, we create a filter from the given request */
	public static NewFilter createFilterFromRequest(HttpServletRequest request)
	{
		NewFilter f = new NewFilter();
		// copy over the request params into the filter's map
		for (Object p: request.getParameterMap().keySet())
			f.map.put ((String) p, request.getParameterValues((String) p));
		return f;
	}

	/** checks if this filter contains the given facet.
	 * currently compares using url of f. but could be made more robust in future */
	public boolean containsFacet(DetailedFacetItem f) 
	{
		try {
			// get key, val from messagesURL. should be pretty robust, ok to fall through to exception in case the format is not as expected
			String key = f.getParamName();
			String val = f.getParamValue();
		
			// check if any of the values in the map == val
			String[] mapVals = map.get(key);
			if (mapVals == null)
				return false;
			
			for (String v: mapVals)
				if (v.equals(val))
					return true;
			return false;
		} catch (Exception e) {
			Util.print_exception(e, log);
			return false;
		}
	}
	
	/** gets value for given key directly from the map. only returns first value for that key */
    private String get(String key) {
		String[] values = map.get(key);
		if (values == null || values.length == 0)
			return null;
		else
			return values[0];
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String s: map.keySet())
			sb.append (s + "->" + map.get(s) + " ");
		return sb.toString();
	}

	// must be kept in sync with JSPHelper.isRegexSearch()
	public boolean isRegexSearch() {
		return "on".equals(get("unindexed"));
	}
}
