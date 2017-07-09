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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.stanford.muse.util.Util;

public class NLP {
    private static Log log = LogFactory.getLog(NLP.class);
    /*
	private static MaxentTagger tagger;

	static {
		try {
			tagger = new MaxentTagger("jar:left3words-wsj-0-18.tagger");
			TTags ttags = tagger.getTags();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ttags.getSize(); i++)
			{
				String tag = ttags.getTag(i);
				sb.append (tag + " ");
				// sb.append(ttags.isClosed(tag) ? "(closed) ": "");
			}
			log.info("Initialized tagger, available tags: " + sb);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
	}
	public static String taggedString(String s)
	{
		StringBuilder result = new StringBuilder();
	    List<ArrayList<? extends HasWord>> sentences = tagger.tokenizeText(new BufferedReader(new StringReader(s)));
	    for (ArrayList<? extends HasWord> sentence : sentences)
	    {
		      ArrayList<TaggedWord> tSentence = tagger.tagSentence(sentence);
		//      result.append(Sentence.listToString(tSentence, false));
	    }

	  //  return result.toString();
	    return s + (isTechnicalTerm(s) ? " (TT)": "");
	}
*/

	public static boolean isTechnicalTerm(String s)
	{
		return false;
		/*
	    List<HasWord> sentence = Sentence.toWordList(s);
	    List<TaggedWord> tagged = tagger.tagSentence(sentence);
	    List<ArrayList<? extends HasWord>> sentences = tagger.tokenizeText(new BufferedReader(new StringReader(s)));

	    StringBuilder sb = new StringBuilder();

	    for (List<? extends HasWord> sent: sentences)
	    {
		    ArrayList<TaggedWord> tSentence = tagger.tagSentence(sent);
		    for (TaggedWord tw: tSentence)
		    {
		    	String tag = tw.tag();
		    	if (TAG_NOUNS.contains(tag))
					sb.append (CODE_NOUN);
				else if (TAG_ADJECTIVE.equals(tag))
					sb.append (CODE_ADJECTIVE);
				else if (TAG_CD.equals(tag))
					sb.append (CODE_CD);
				else
					sb.append (CODE_OTHER);
		    }
	    }
	    String tagCodes = sb.toString();
	    return TECH_TERM_PAT.matcher(tagCodes).matches();
	    */
	}

	private static final String          TECH_TERM =                     "((J|N)+(C|N)|N)";
	private static final Pattern          TECH_TERM_PAT =                     Pattern.compile("((J|N)+(C|N)|N)");
	private static final String COMPOUND_TECH_TERM = "((J|N)*NO(J|N)*(C|N)|(J|N)+(C|N)|N)";

	private static final Set<String> TAG_NOUNS = new LinkedHashSet<String>( Arrays.asList( "NN", "NNS", "NNP", "NNPS" ) );
	private static final String TAG_ADJECTIVE = "JJ";
	private static final String TAG_CD = "CD";

	private static final String CODE_of = "O";
	private static final String CODE_NOUN = "N";
	private static final String CODE_ADJECTIVE = "J";
	private static final String CODE_OTHER = "X";
	private static final String CODE_CD = "C";

	/*
	public static void tag(String s)
	{
	    List<ArrayList<? extends HasWord>> sentences = tagger.tokenizeText(new BufferedReader(new StringReader(s)));
	    for (ArrayList<? extends HasWord> sentence : sentences)
	    {
	      ArrayList<TaggedWord> tSentence = tagger.tagSentence(sentence);
	      for (TaggedWord tw: tSentence)
	      {
	    	  String str = tw.tag();
	    	  if ("NN".equals(str))
	    		  System.out.println ("NN: " + tw);
	      }
	  //    System.out.println(Sentence.listToString(tSentence, false));
	    }
	}
*/
	  public static void main(String[] args) throws Exception {
		  /*
		  String s = "I'll  send  you  something as  soon as I  have it done.";
		    List<HasWord> sentence = Sentence.toWordList(s);
		    System.out.println ("sentence has " + sentence.size() + " words");
		    List<TaggedWord> tagged = tagger.tagSentence(sentence);
		    for (TaggedWord tw: tagged)
		    {
		    	System.out.println (tw);
		    }

		    List<ArrayList<? extends HasWord>> sentences = tagger.tokenizeText(new BufferedReader(new StringReader(s)));
		    for (List<? extends HasWord> sent: sentences)
		    {
			      ArrayList<TaggedWord> tSentence = tagger.tagSentence(sent);
			      System.out.println(Sentence.listToString(tSentence, false));
		    }

	    if (args.length != 1) {
	      System.err.println("usage: java NLP fileToTag");
	      return;
	    }
	    tag(Util.getFileContents(args[0]));
	    	  */

	  }

	/**
	 * Returns a union as Set
	 */
	public static<E> Set<E> setUnion(Collection<E> s1, Collection<E> s2)
	{
	    //if (s1 == null || s2 == null) return null; // let's trigger exception as caller may want null to represent "all"
	    return Util.setUnion(Util.castOrCloneAsSet(s1), Util.castOrCloneAsSet(s2));
	}

	/**
	 * Returns an intersection as Set, treating null as universal. Returns null if both inputs are null.
	 */
	public static<E> Set<E> setIntersectionNullIsUniversal(Collection<E> s1, Collection<E> s2)
	{
	    if (s1 == null) return Util.castOrCloneAsSet(s2);
	    if (s2 == null) return Util.castOrCloneAsSet(s1);
	    return Util.setIntersection(s1, s2);
	}

	/**
	 * Returns a union as Set, treating null as empty. Returns null if both inputs are null.
	 */
	public static<E> Set<E> setUnionNullIsEmpty(Collection<E> s1, Collection<E> s2)
	{
	    if (s1 == null) return Util.castOrCloneAsSet(s2);
	    if (s2 == null) return Util.castOrCloneAsSet(s1);
	    return Util.setUnion(s1, s2);
	}

}
