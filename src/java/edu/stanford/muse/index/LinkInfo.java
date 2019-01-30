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


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;

import java.io.Serializable;
import java.util.*;

public class LinkInfo implements Serializable {
	// private static Log log = LogFactory.getLog(LinkInfo.class);

private static final long serialVersionUID = 1L;

public String link;
	private String originalLink; // link is the canonicalized version, originalLink is the actual string.
private final Document doc; // document this linkinfo belongs to
public String tld; // set up later, while sorting...
int count;

public LinkInfo(String link, Document d)
{
	if (link != null)
	{
		this.originalLink = link;
		this.link = link.toLowerCase(); // canonicalize link, just in case.
	}
	this.doc = d;
}

public int hashCode()
{
	return this.link.hashCode() ^ this.doc.hashCode();
}

// same only if same link and same doc
public boolean equals (Object o)
{
	if (o == null || (!(o instanceof LinkInfo)))
		return false;

	LinkInfo other = (LinkInfo) o;
	return this.link.equals(other.link) && this.doc.equals(other.doc);
}

public String toString()
{
	return Util.fieldsToString(this);
}

public static Pair<Date, Date> getFirstLast(List<LinkInfo> list)
{
	List<DatedDocument> docs = new ArrayList<>();
	for (LinkInfo li: list)
		if (li.doc instanceof DatedDocument)
			docs.add((DatedDocument) li.doc);
	return EmailUtils.getFirstLast(docs);
}


public static String linksToJson(List<LinkInfo> links)
{
	if (Util.nullOrEmpty(links))
		return "[]";

	// create a map of url to link info's
	Map<String, List<LinkInfo>> map = new LinkedHashMap<>();
	for (LinkInfo li: links)
	{
		String url = li.link.trim().toLowerCase();
		List<LinkInfo> list = map.get(url);
		if (Util.nullOrEmpty(list))
		{
			list = new ArrayList<>();
			map.put(url, list);
		}
		list.add(li);
	}

	Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
	JsonArray arr = new JsonArray();
	for (String url: map.keySet())
	{
		// currently returning only the count, in future we can consider weights based on person with whom email was exchanged, date of exchange etc.
		JsonObject obj = new JsonObject();
		obj.addProperty("url", url);
		obj.addProperty("times", map.get(url).size());
		arr.add(obj);
	}
	// log.info ("json = " + json);
	return gson.toJson(arr);
}
}
