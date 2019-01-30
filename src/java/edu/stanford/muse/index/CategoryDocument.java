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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CategoryDocument is something that is associated with some sort of category like a section or time period */
public class CategoryDocument extends Document {
	private static final long serialVersionUID = 12208220822082208L;
    private final String category;

	public CategoryDocument(int num, String s, String category)
	{
		super (Integer.toString(num), s);
		this.category = category;
	}

	public static List<MultiDoc> clustersDocsByCategoryName(Collection<CategoryDocument> docs)
	{
		Map<String, MultiDoc> map = new LinkedHashMap<>();
		List<MultiDoc> result = new ArrayList<>();

		int count = 0;
		for (CategoryDocument d: docs)
		{
			String category = d.category;
			MultiDoc md = map.get(category);
			if (md == null)
			{
				md = new MultiDoc(Integer.toString(count++), category);
				map.put(category, md);
				result.add(md);
			}
			md.add(d);
		}
		return result;
	}
}
