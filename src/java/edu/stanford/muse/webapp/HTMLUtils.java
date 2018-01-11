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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import edu.stanford.muse.util.Util;

public class HTMLUtils {
	
	public String getDebugString(HttpServletRequest request, HttpSession session, String webAppRoot) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		sb.append ("Session attributes\n");
	    java.util.Enumeration<String> keys = session.getAttributeNames();

	    while (keys.hasMoreElements())
	    {
	      String key = keys.nextElement();
	      sb.append(key + ": " + JSPHelper.getSessionAttribute(session, key) + "\n");
	    }

	    Thread.currentThread().setName("testContactGroups");
	    
	    String tmpDir = System.getProperty ("java.io.tmpdir");

	    // concat debug file
	    String debugFile = tmpDir + File.separatorChar + "debug.txt"; // should be in sync with debug filename in jettylauncher
		File f = new File(debugFile);
	    if (f.exists() && f.canRead())
	    	sb.append(Util.getFileContents(debugFile));
	    else
	    	sb.append ("No debug log");
	    return sb.toString();
	}
	
	/** reads an integer param from the request.
	 * if the value of the param cannot be parsed as an integer, returns the defaultValue
	 */
	public static int getIntParam (HttpServletRequest request, String paramName, int defaultValue)
	{
		String val = request.getParameter(paramName);
		if (val == null)
			return defaultValue;

		int result = defaultValue;
		try { result = Integer.parseInt(val); }
		catch (NumberFormatException nfe) { /* do nothing */ }
		return result;
	}

	public static int getIntParam (Map<String, String> map, String paramName, int defaultValue)
	{
		String val = map.get(paramName);
		if (val == null)
			return defaultValue;

		int result = defaultValue;
		try { result = Integer.parseInt(val); }
		catch (NumberFormatException nfe) { /* do nothing */ }
		return result;
	}
	
	/** reads an integer param from the request.
	 * if the value of the param cannot be parsed as an integer, returns the defaultValue
	 */
	public static long getLongParam (HttpServletRequest request, String paramName, long defaultValue)
	{
		String val = request.getParameter(paramName);
		if (val == null)
			return defaultValue;

		long result = defaultValue;
		try { result = Long.parseLong(val); }
		catch (NumberFormatException nfe) { /* do nothing */ }
		return result;
	}
	
	/* like requestParamMap but returns String -> String instead of String -> String[]. assumes no duplicate params */
	public static Map<String, String> getRequestParamMap(HttpServletRequest request)
	{
		Map<String, String> result = new LinkedHashMap<>();
		Map<String, String[]> paramMap = request.getParameterMap();
		for (String key: paramMap.keySet())
			result.put(key, paramMap.get(key)[0]);
		return result;
	}

	/** reads an integer param from the request.
	 * if the value of the param cannot be parsed as an integer, returns the defaultValue
	 */
	public static int getIntAttr (HttpSession session, String paramName, int defaultValue)
	{
		String val = (String) JSPHelper.getSessionAttribute(session, paramName);
		if (val == null)
			return defaultValue;

		int result = defaultValue;
		try { result = Integer.parseInt(val); }
		catch (NumberFormatException nfe) { /* do nothing */ }
		return result;
	}
	/** reads a float param from the request.
	 * if the value of the param cannot be parsed, returns the defaultValue
	 */
	public static float getFloatParam (HttpServletRequest request, String paramName, float defaultValue)
	{
		String val = request.getParameter(paramName);
		if (val == null)
			return defaultValue;
		float result = defaultValue;
		try { result = Float.parseFloat(val); }
		catch (NumberFormatException nfe) { /* do nothing */ }
		return result;
	}
	
	public static boolean runningOnTablet (HttpServletRequest request)
	{
	    String ua = request.getHeader("User-agent");
	    ua = ua.toLowerCase();
	    return ua.contains("iphone") || ua.contains("ipad") || ua.contains("android");
	}

	/** extract text from html, including a newline after all div's/br's or hr's. note -- this should really not be in the webapp package
	// since its used by email fetcher (which can be used without the webapp frontend).
	 */
	public static void extractTextFromHTML(org.jsoup.nodes.Node n, StringBuilder result)
	{
		if (n instanceof TextNode)
			result.append (((TextNode) n).text());
		else if (n instanceof Element)
		{
			Element e = (Element) n;
			for (org.jsoup.nodes.Node c: e.childNodes())
				extractTextFromHTML(c, result);
			if ("div".equals(e.tagName()))
				result.append ("\n");
			if ("br".equals(e.tagName()))
				result.append ("\n");
			if ("hr".equals(e.tagName()))
				result.append ("\n");
		}
	}

	// should always be used instead of http://localhost:9099/muse etc
	public static String getRootURL(HttpServletRequest request) {
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
	}
	
	public static void main (String args[])
	{
		List<String> l = new ArrayList<>();
		l.add("/foo/xx/bar");
		l.add("/foo/xx/xxx");
		l.add("/foo/xx/yyy");
		List<String> x = Util.stripCommonPrefix(l);
		for (String s: x)
			System.out.println (s);
	}
}
