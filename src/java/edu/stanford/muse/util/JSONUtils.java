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


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.AddressBookManager.Contact;

public class JSONUtils {
	private static Log log = LogFactory.getLog(JSONUtils.class);

	public static String jsonForIntList(List<Integer> list) throws JSONException
	{
		JSONObject result = new JSONObject();
		JSONArray entries = new JSONArray();

		if (list == null)
		{
			log.warn ("json for int list: null");
			return "";
		}
		for (int i = 0; i < list.size(); i++)
			entries.put(i, list.get(i));
		result.put("indices", entries);
		return result.toString();
	}

	public static String jsonForNewNewAlgResults(AddressBook ab, String resultsFile) throws IOException, JSONException
	{
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(resultsFile)));
		JSONObject result = new JSONObject();

		JSONArray groups = new JSONArray();
		int groupNum = 0;
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			line = line.trim();
			// line: group 8, freq 49: seojiwon@gmail.com sseong@stanford.edu debangsu@cs.stanford.edu

			// ignore lines without a ':'
			int idx = line.indexOf(":");
			if (idx == -1)
				continue;

			String vars = line.substring(0, idx);
			// vars: freq=5 foo bar somevar=someval
			// we'll pick up all tokens with a = in them and assume they mean key=value
			StringTokenizer varsSt = new StringTokenizer(vars);

			JSONObject group = new JSONObject();
			while (varsSt.hasMoreTokens())
			{
				String str = varsSt.nextToken();
				// str: freq=5
				int x = str.indexOf("=");
				if (x >= 0)
				{
					String key = str.substring(0, x);
					String value = "";
					// we should handle the case of key= (empty string)
					if (x < str.length()-1)
						value = str.substring(x+1);
					group.put (key, value);
				}
			}

			String groupsStr = line.substring(idx+1);
			// groupsStr: seojiwon@gmail.com sseong@stanford.edu debangsu@cs.stanford.edu

			StringTokenizer st = new StringTokenizer(groupsStr);

			JSONArray groupMembers = new JSONArray();
			int i = 0;

			while (st.hasMoreTokens())
			{
				String s = st.nextToken();
				Contact ci = ab.lookupByEmail(s);
				if (ci == null)
				{
					System.out.println ("WARNING: no contact info for email address: " + s);
					continue;
				}
				groupMembers.put(i++, ci.toJson());
			}
			group.put("members", groupMembers);
			groups.put(groupNum, group);
			groupNum++;
		}

		result.put ("groups", groups);
		return result.toString();
	}

	public static float getScoreForHits(List<JSONObject> lensHits) throws JSONException
	{
		float score = 0.0f;
		for (int i = 0; i < lensHits.size(); i++)
		{
			JSONObject o = lensHits.get(i);
			int timesOnPage = o.getInt("timesOnPage");
			int nMessages = o.getInt("nMessages");
			score += timesOnPage * nMessages;
		}
		return score;
	}

	private static JSONObject jsonForAddressBook(AddressBook ab) throws JSONException
	{
		JSONObject result = new JSONObject();
		JSONArray entries = new JSONArray();
		List<Contact> allContacts = ab.allContacts();

		// ankita wants the first contact to always be me

		// ** CHANGED CODE BY ANKITA **

		int i = 1;

//		Set<String> myAddresses = ab.getOwnAddrsSet();
		Contact me = ab.getContactForSelf();
		Set<String> myAddresses = me.getEmails();

		for (Contact c: allContacts)
		{
			if (myAddresses.equals(c.getEmails())) entries.put(0, c.toJson());
			else entries.put(i++, c.toJson());
		}
//		entries.put(0, myAddresses);

		/*
		 * OLD CODE:
		int i = 0;
		Contact me = ab.getOwnContact();
		if (me != null)
		{
			entries.put (0, me.toJson());  // ORIGINAL:	entries.put(0, me);
			i++;
		}

		for (Contact c: allContacts)
		{
			if (me == c)
				continue;
			entries.put(i++, c.toJson());
		}
		*/

		result.put("entries", entries);
		return result;
	}

/*	public static List<String> getArrayElements(org.json.simple.JSONArray arr)
	{
		List<String> result = new ArrayList<String>();
		if (arr == null)
			return result;
		for (int i = 0; i < arr.size(); i++)
		{
        	org.json.simple.JSONObject o = (org.json.simple.JSONObject) arr.get(i);
        	if (o == null)
        	{
        		System.out.println ("Warning: ignoring ARR " + i);
        		continue;
        	}
        	String s = (String) o.get("smtp");
        	if (s == null)
        	{
        		System.out.println ("Warning: ignoring ARR SMTP " + i);
        		continue;
        	}
        	result.add(s);
		}

		return result;
	}*/

	private static Address[] convertStringsToInternetAddrs(List<String> list) throws AddressException
	{
		Address[] result = new Address[list.size()];
		for (int i = 0; i < list.size(); i++)
			result[i] = new InternetAddress(list.get(i));
		return result;
	}

	public static List<String> getArrayElements(org.json.simple.JSONArray arr)
	{
		List<String> result = new ArrayList<String>();
		if (arr == null)
			return result;
		for (int i = 0; i < arr.size(); i++)
		{
        	String s = (String) arr.get(i);
        	result.add(s);
		}

		return result;
	}
	
	private static class Message implements Serializable {
		List<String> to, cc, bcc;
		String from;
	}

	public static String getStatusJSON(String message, int pctComplete, long secsElapsed, long secsRemaining)
	{
		JSONObject json = new JSONObject();
		try {
			json.put("pctComplete", pctComplete);
			json.put("message", message);
			json.put("secsElapsed", secsElapsed);
			json.put("secsRemaining", secsRemaining);
		} catch (JSONException jsone) {
			try {
				json.put("error", jsone.toString());
			} catch (Exception e) { Util.report_exception(e); }
		}
		return json.toString();
	}

	public static String getStatusJSONWithTeaser(String message, int pctComplete, long secsElapsed, long secsRemaining, List<String> teasers)
	{
		JSONObject json = new JSONObject();
		try {
			json.put("pctComplete", pctComplete);
			json.put("message", message);
			json.put("secsElapsed", secsElapsed);
			json.put("secsRemaining", secsRemaining);
			if (!Util.nullOrEmpty(teasers))
			{
				JSONArray arr = new JSONArray();
				for (int i = 0; i < teasers.size(); i++)
					arr.put(i, teasers.get(i));
				json.put("teasers", arr);
			}
		} catch (JSONException jsone) {
			try {
				json.put("error", jsone.toString());
			} catch (Exception e) { Util.report_exception(e); }
		}
		return json.toString();
	}
	
	public static String getStatusJSON(String message)
	{
		return getStatusJSON(message, -1, -1, -1);
	}
	
	public static Pair<String, List<Pair<String,Integer>>> getDBpediaForPerson (String name)
	{
		name = name.trim();
		name = name.replaceAll(" ", "_");
		try {
			String url = "http://dbpedia.org/data/" + name + ".jsod";
			byte[] b = Util.getBytesFromStream(new URL(url).openStream());
	//		String contents = new String(CryptoUtils.decryptBytes(b), "UTF-8");
			String contents = new String(b, "UTF-8");
			org.json.simple.JSONObject o = (org.json.simple.JSONObject) org.json.simple.JSONValue.parse(contents);
			org.json.simple.JSONObject o1 = (org.json.simple.JSONObject) o.get("d");
			org.json.simple.JSONArray o2 = (org.json.simple.JSONArray) o1.get("results");
			org.json.simple.JSONObject o3 = (org.json.simple.JSONObject) o2.get(0);
			org.json.simple.JSONObject o4 = (org.json.simple.JSONObject) o3.get("http://dbpedia.org/ontology/thumbnail");
			org.json.simple.JSONObject o5 = (org.json.simple.JSONObject) o4.get("__deferred");
			String o6 = (String) o5.get("uri");
		
			org.json.simple.JSONObject o7 = (org.json.simple.JSONObject) o3.get("http://xmlns.com/foaf/0.1/page");
			org.json.simple.JSONObject o8 = (org.json.simple.JSONObject) o7.get("__deferred");
	//		String wp_page = (String) o8.get("uri");
			
//			String wp_contents = doc.body().text();
		//	List<Pair<String,Integer>> list = NER.namesFromURL(wp_page);
			return new Pair<String, List<Pair<String,Integer>>> (o6, null);
//			return new Pair<String, List<Pair<String,Integer>>> (o6, list);
		} catch (Exception e) { 
			Util.print_exception(e);
		}
		return null;
	}

	private static Pair<JSONArray, Map<String, String>> hashJsonArray(JSONArray arr, Map<String, String> reverseMap) throws JSONException
	{
		JSONArray newArr = new JSONArray();
		for	(int k = 0; k < arr.length(); k++)
		{
			Object elem = arr.get(k);
			if (elem instanceof String)
				newArr.put(k, Util.hash ((String) elem, reverseMap));
			else if (elem instanceof JSONArray)
				newArr.put(k, hashJsonArray((JSONArray) elem, reverseMap).getFirst());
			else if (elem instanceof JSONObject)
				newArr.put(k, hashJsonObject((JSONObject) elem, reverseMap).getFirst());
			else 
				newArr.put(k, elem);
		}	
		return new Pair<JSONArray, Map<String, String>> (newArr, reverseMap);
	}
	
	private static Pair<JSONObject, Map<String, String>> hashJsonObject(JSONObject obj, Map<String, String> reverseMap) throws JSONException
	{
		JSONObject result = new JSONObject();
		
		if (reverseMap == null)
			reverseMap = new LinkedHashMap<String, String>();
		for (Iterator<String> it = obj.keys(); it.hasNext(); )
		{
			String key = it.next();
			String key_hash = key.startsWith("_") ? key : Util.hash(key, reverseMap);
			Object value = obj.get(key);

			// flatten any arrays
			if (value instanceof JSONArray)
			{
				JSONArray hashA = hashJsonArray((JSONArray) value, reverseMap).getFirst();
				result.put (key_hash, hashA);
			}
			else if (value instanceof JSONObject)
			{
				JSONObject val = ((JSONObject) value);
				JSONObject hashO = hashJsonObject(val, reverseMap).getFirst();
				result.put (key_hash, hashO);
			}
			else if (value instanceof String)
			{
				String strValue = (String) value;
				result.put(key_hash, Util.hash(strValue, reverseMap));
			}
			else
				result.put(Util.hash(key, reverseMap), value);
		}
		return new Pair<JSONObject, Map<String, String>> (result, reverseMap);
	}
	
	private static List<Pair<String, String>> convertToTuples(String prefix, Object value) throws JSONException
	{
		List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();

		// flatten any arrays
		if (value instanceof JSONArray)
			result.addAll(convertToTuples(prefix, (JSONArray) value));
		else if (value instanceof JSONObject)
			result.addAll(convertToTuples(prefix, (JSONObject) value));
		else if (value instanceof String)
			result.add(new Pair<String, String>(prefix, (String) value));
		else if (value instanceof Integer)
			result.add(new Pair<String, String>(prefix, Integer.toString((Integer) value)));
		
		return result;
	}
	
	private static List<Pair<String, String>> convertToTuples(String prefix, JSONArray arr) throws JSONException
	{
		List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();

		for (int i = 0; i < arr.length(); i++)
		{
			Object o = arr.get(i);
			result.addAll(convertToTuples (prefix + "[" + i + "]", o));
		}
		return result;
	}
	
	private static List<Pair<String, String>> convertToTuples(String prefix, JSONObject obj) throws JSONException
	{
		List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
		
		for (Iterator<String> it = obj.keys(); it.hasNext(); )
		{
			String key = it.next();
			String full_key = prefix.length() == 0 ? key : prefix + "." + key;
			Object value = obj.get(key);
			result.addAll(convertToTuples(full_key, value));
		}
		return result;
	}

	public static List<Pair<String, String>> convertToTuples(String json) throws JSONException
	{
		JSONObject jo = new JSONObject(json);
		return convertToTuples("", jo);
	}

	public static JSONArray arrayToJsonArray(int[] x) throws JSONException
	{
		JSONArray result = new JSONArray();
		for (int i = 0; i < x.length; i++)
			result.put(i, x[i]);
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
}
