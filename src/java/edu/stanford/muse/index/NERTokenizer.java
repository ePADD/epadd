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

import edu.stanford.muse.util.Pair;

/** little container class that can enumerate the tokens identified by NER */
public class NERTokenizer extends MyTokenizer
{
	private List<Pair<String,String>> nerTokens;
	public NERTokenizer(List<Pair<String,String>> tokens)
	{
		nerTokens = new ArrayList<Pair<String,String>>(tokens);
	}

	private int tok = 0;
	public boolean hasMoreTokens()
	{
		return tok < nerTokens.size();
	}

    public String nextToken()
	{
	    return nerTokens.get(tok++).getFirst();
	}

    public Pair<String,String> nextTokenAndType()
	{
	    return nerTokens.get(tok++);
	}

	public boolean prevTokenEndsSentence() { return false; }

}
