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

import java.util.ArrayList;
import java.util.List;

/** a doc with a bunch of sub-docs */
public class MultiDoc extends Document {
    public enum ClusteringType{
        MONTHLY, YEARLY, NONE
	}

	private final static long serialVersionUID = 1L;

	public List<Document> docs = new ArrayList<>();
//	public Indexer perDocIndexer;

	public List<Document> getDocs() {
		return docs;
	}

	public MultiDoc (int num, String s)
	{
		this(Integer.toString(num), s);
	}
	
	public MultiDoc (String id, String s)
	{
		super (id, s);
	}

	public void add(Document d)
	{
		docs.add(d);
	}

	public String toString()
	{
		return super.toString() + " with " + docs.size() + " docs";
	}
}
