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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class Trace {
	public static final boolean isEnabled = false;
    private static final Log log = LogFactory.getLog("MuseTrace");
    private static final Set<String> traceTerms = new LinkedHashSet<String>();
    static {
//    	traceTerms.add("mahish");
    }

    public static void trace(Object message, Object o)
    {
    	if (traceTerms.size() == 0)
    		return;
    	String s = o.toString();
    	for (String traceTerm: traceTerms)
    		if (s.toString().toLowerCase().indexOf(traceTerm) >= 0)
    			log.info (message + ": " + s);
    }

    public static void trace(Object message1, Object message2, Object o)
    {
    	if (traceTerms.size() == 0)
    		return;
    	String s = o.toString();
    	for (String traceTerm: traceTerms)
    		if (s.toString().toLowerCase().indexOf(traceTerm) >= 0)
    			log.info (message1 + ": " + message2 + ": " + s);
    }

    public static void trace(Object message1, Object message2, Object message3, Object o)
    {
    	if (traceTerms.size() == 0)
    		return;
    	String s = o.toString();
    	for (String traceTerm: traceTerms)
    		if (s.toString().toLowerCase().indexOf(traceTerm) >= 0)
    			log.info (message1 + ": " + message2 + ": " + message3 + ": "+ s);
    }
}


