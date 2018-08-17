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

import java.util.HashSet;
import java.util.Set;

import edu.stanford.muse.index.Document;

public class DetailedFacetItem implements Comparable<DetailedFacetItem>
{
	final public Set<Document> docs; // this has to be a set, to avoid double-counting
	final public String name;
	final public String description;
	final public String messagesURL;
	//final public String attachmentsURL;
	final private String paramName;
	final private String paramValue;

	public String getParamName() { return paramName; }
	public String getParamValue() { return paramValue; }

	public DetailedFacetItem(String name, String description, String paramName, String paramValue)
	{
		this(name, description, null, paramName, paramValue);
	}
	
	public DetailedFacetItem(String name, String description, Set<Document> docs, String paramName, String paramValue)
	{
		this.name = name; this.description = description;
		this.docs = docs == null ? new HashSet<>() : docs;
		this.paramName = paramName;
		this.paramValue = paramValue;
		this.messagesURL = paramName + "=" + Util.URLEncode(paramValue);
	}

	public void addDoc(Document d) 
	{
		if (d != null)
            docs.add(d);
	}
	
	public int totalCount()	{ return docs.size(); }
	
	// compare inconsistent with equals
	public int compareTo(DetailedFacetItem other)
	{
		int count = totalCount();
		int otherCount = other.totalCount();
		return (otherCount - count); // higher count comes first
	}
	
	@Override
	public int hashCode() { return name.hashCode() ^ description.hashCode(); }
	
	@Override
	public boolean equals(Object other) 
	{ 
		if (!(other instanceof DetailedFacetItem))
			return false;
		DetailedFacetItem d = (DetailedFacetItem) other;
		return (name + description).equals(d.name + d.description);
	}
}