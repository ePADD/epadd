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

import java.util.List;
import java.util.StringTokenizer;

/* frequency of a term in a document */
public class Posting implements Comparable<Posting>, java.io.Serializable {
	private final static long serialVersionUID = 1L;
	// traditional postings are <termid, docid, term freq>
	//public Document document;
	public String term; // this is the canonical term (may be stemmed etc)
	String originalTerm; // this is the actual un-normalized
						 // term which may be displayed back to the user.
						 // it may have original capitalization, spacing, join words etc.
//	float normalizedTF;
	int tf;
	float idf;
	private byte[] pos;

	public static int nPostingsAllocated = 0;

	public Posting()
	{
		Posting.nPostingsAllocated++;
	}

	public float score()
	{
		return tf * idf;
	}

	public int compareTo(Posting other)
	{
		float score1 = score();
		float score2 = other.score();
		if (score1 == score2) {
			if (tf == other.tf) {
				return originalTerm.compareTo(other.originalTerm); // ascending
			}
			return other.tf - tf; // descending
		}
		if (score1 > score2)
			return -1;
		else
			return 1;
	}

	/* returns true if this term is adjacent to the other in ANY position, with the last word overlapping.
	 * i.e. if the first term is "a b c" and the second term is "d e f"
	 * looks for a case where the starting of the second term is 2 pos away from the start of the first term
	 * assumes other is also from the same document */
	public boolean isAdjacentTo(Posting other)
	{
		// if we don't have positions, we have no evidence of 2 terms being adjacent
		if (pos == null || other.pos == null)
			return false;

		/// should never be called on a Posting that wasn't constructed with (true),
		// i.e. should always be called on those postings which have pos non-null.
		StringTokenizer st = new StringTokenizer(term);
		int nTokens = st.countTokens();

		// other's pos should be nTokens away from this's pos
		for (int i = 0; i < pos.length; i++)
			for (int j = 0; j < other.pos.length; j++)
				if (pos[i]+nTokens-1 == other.pos[j])
					return true;
		return false;
	}

	public void setPositions(List<Integer> list)
	{
		if (list == null)
		{
			pos = new byte[0];
			return;
		}
		pos = new byte[list.size()];
		int i = 0;
		for (Integer p : list)
			pos[i++] = (byte) (p%256);
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append (short_toString());
		if (pos != null)
			for (Byte b: pos)
				sb.append (b + " ");
		return sb.toString();
	}


	public String short_toString()
	{
		return ("Term: [" + term + "] score " + score() + " freq: " + tf + " idf: " + String.format("%.2f", idf) + " tf-idf: " + String.format("%.2f", (tf*idf)) + " ntf: " + tf + " pos: ");
	}


	public String POS_toString()
	{
//		return ("Term: [" + term + "] " + NLP.taggedString(term) + " score " + score() + " freq: " + tf + " idf: " + String.format("%.2f", idf) + " tf-idf: " + String.format("%.2f", (tf*idf)) + " ntf: " + tf + " pos: ");
		return ("Term: [" + term + "] " + /* NLP.taggedString(term) + */ " score " + score() + " freq: " + tf + " idf: " + String.format("%.2f", idf) + " tf-idf: " + String.format("%.2f", (tf*idf)) + " ntf: " + tf + " pos: ");
	}
}
