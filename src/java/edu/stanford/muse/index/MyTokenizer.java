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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.muse.util.DictUtils;
import edu.stanford.muse.util.Util;


/** like a stringtokenizer but embeds special handling of sentences by
 * offering a prevTokenEndsSentence() call that checks if the last token returned by nextToken() was terminated by one of ?.;!
 * @author hangal
 *
 */
public class MyTokenizer {

	private int ptr; // index into stringChars, in the parked position points either to stringChars.length or to the first non-delim char of a new token
	private int startPtr = -1; // index into stringChars, points to the idx of the last token returned

	private boolean prevTokenEndsSentence = false;
	private char[] stringChars;

	private static final Set<Character> delimSet = new LinkedHashSet<Character>();
	private static final String delims = " \r\n\t~#^&*_+`=|\\:\"<>/,?.!;()";

	private static final String emailAddressSpecialCharsAllowed = ".-_"; // chars allowed in email addresses, in addition to alphanumerics
	private static final String emailAddressDelimSet = " ()[]\r\n\t~#^&*`|\\:\"<>/,?!;"; // chars that end an email address

	private static final Set<Character> httpLinkDelimSet = new LinkedHashSet<Character>();
	// don't expect these to occur in http links, but double check in case of trouble.
	// may need to re-evaluate status of "." because it is sometimes allowed in http links like .do
	private static final String httpLinkDelims = " \r\n\t^*`|\\\"<>!;";

	private static final Set<Character> sentenceEndSet = new LinkedHashSet<Character>();
	private static final String sentenceEndChars = "?.!;"; // include comma ?

	static {
		char[] delimsCharArray = delims.toCharArray();
		for (char c: delimsCharArray)
			delimSet.add(c);

		char[] httpLinkDelimsCharArray = httpLinkDelims.toCharArray();
		for (char c: httpLinkDelimsCharArray)
			httpLinkDelimSet.add(c);

		char[] sentenceEndArray = sentenceEndChars.toCharArray();
		for (char c: sentenceEndArray)
			sentenceEndSet.add(c);
	}

	private void skipToFirstNonDelim()
	{
		while (ptr < stringChars.length)
		{
			if (sentenceEndSet.contains(stringChars[ptr]))
				prevTokenEndsSentence = true;
			if (!delimSet.contains(stringChars[ptr]))
				break;
			ptr++;
		}
	}

	/* sets ptr to the next delim */
	private void skipToFirstDelim()
	{
		int temp_ptr = ptr;
		boolean nextTokenIsEmailAddr = false;
		int MAX_EMAIL_ADDR_LEN = 100; // reasonable email addresses should be < 100 chars

		// check if current token may be an email address, by seeing if '@' appears before any non-email-addr char (dot and underscore are allowed)
		// otherwise, its annoying when email addresses break up into multiple "sentences"
		int possibleEmailAddrLen = -1;
		while (temp_ptr < stringChars.length)
		{
			char c = stringChars[temp_ptr];
			possibleEmailAddrLen++;
			temp_ptr++;
			if (possibleEmailAddrLen > MAX_EMAIL_ADDR_LEN)
			{
				nextTokenIsEmailAddr = false;
				break;
			}

			// email address has to have a '@'
			if (c == '@')
			{
				// @ not allowed at the beginning of the string for a valid email address
				if (possibleEmailAddrLen == 0)
				{
					nextTokenIsEmailAddr = false;
					break;
				}
				else
				{
					nextTokenIsEmailAddr = true;
					continue;
				}
			}

			// we'll look forward past letters, digits, special chars allowed in email
			if (Character.isLetterOrDigit(c) || emailAddressSpecialCharsAllowed.indexOf(c) >= 0)
				continue;

			// and stop when we come to a char that's not allowed in an email address
			if (emailAddressDelimSet.indexOf(c) >= 0)
 				break;
		}

		if (!nextTokenIsEmailAddr)
		{
			// the common case: go on till the next normal delim
			while (ptr < stringChars.length)
			{
				if (delimSet.contains(stringChars[ptr]))
					break;
				ptr++;
			}
		}
		else
		{
			// to be really sure its an email address, we could check that it ends in a TLD, but haven't seen the need for it yet
			// the email addr spans, [ptr, ptr+possibleEmailAddrLen-1], so ptr+possibleEmailAddrLen is the next delimiter
			ptr += possibleEmailAddrLen;
		}
	}

	private void skipToEndOfHttpLink()
	{
		while (ptr < stringChars.length)
		{
			if (httpLinkDelimSet.contains(stringChars[ptr]))
				break;
			ptr++;
		}
	}

	private void verifyParked()
	{
		if (ptr < stringChars.length)
			Util.ASSERT(!delimSet.contains(stringChars[ptr]));
	}

	MyTokenizer() { }

	public MyTokenizer (String s)
	{
		stringChars = s.toCharArray();
		skipToFirstNonDelim();
		verifyParked();
	}

	public boolean hasMoreTokens()
	{
		verifyParked();
		prevTokenEndsSentence = false;
		return ptr < stringChars.length;
	}

	int getCurrentTokenPointer()
	{
		return ptr;
	}

	int getTokenStartPointer()
	{
		return startPtr;
	}

	/* checks whether this string at the current idx has content = s */
	private boolean stringAtCurrentPositionIs(String s)
	{
		int idx = ptr;

		// the next s.length() chars in stringChars should be the same as in s to return true
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (idx+i >= stringChars.length)
				return false;
			if (c != stringChars[idx+i])
				return false;
		}

		return true;
	}

	public String nextToken()
	{
		startPtr = ptr;
		if (stringAtCurrentPositionIs("http:") || stringAtCurrentPositionIs("https:"))
		{
			// we come here only when generating annotated html doc contents.
			// during regular parsing, the links have already been stripped out
			skipToEndOfHttpLink();
		}
		else
		{
			skipToFirstDelim();
		}
		int endPtr = ptr;

		String token = new String(stringChars, startPtr, endPtr-startPtr);
		// token is from startPtr to endPtr-1
		if (ptr < stringChars.length)
		{
			char delim = stringChars[ptr];
			prevTokenEndsSentence = (delim == '?' || delim == '.' || delim == '!' || delim == ';' || delim == ',');
		}
		else
		{
			prevTokenEndsSentence = true; // actually it ends doc
		}
		skipToFirstNonDelim();
		verifyParked();
		return token;
	}

	/** returns null if we run out of tokens. returns "" if we might have more tokens, we just don't have one
	 * right now. */
	public String nextNonJoinWordToken()
	{
		while (hasMoreTokens())
		{
			String s = nextToken();
			if ("".equals(s))
				return "";
			if (!DictUtils.isJoinWord(s))
				return s;

			// special cases: these are considered non-delimiters, but
			// should not be independent tokens on their own
			if ("{".equals(s) || "}".equals(s) || "-".equals(s))
				return s;

			if (prevTokenEndsSentence)
				return "";
		}
		return "";
	}

	public boolean prevTokenEndsSentence() { return prevTokenEndsSentence; }

	static String cleanupDisplayTerm(String s)
	{
		// try and balance parantheses. want to eliminate ugly terms like (abc or xyz] which are often seen.

		// simple check: if the string only has opening ( and no closing ) or vice versa,
		// then it is unbalanced. same thing for the three kinds of braces
		// full balancing check requires a little more logic, we'll put it in when necessary.
		if ((s.indexOf("(") >= 0) ^ (s.indexOf(")") >= 0))
			s = Util.removeCharsFromBeginOrEnd(s, "()");
		if ((s.indexOf("[") >= 0) ^ (s.indexOf("]") >= 0))
			s = Util.removeCharsFromBeginOrEnd(s, "[]");
		if ((s.indexOf("{") >= 0) ^ (s.indexOf("}") >= 0))
			s = Util.removeCharsFromBeginOrEnd(s, "{}");

		// eliminate runs of length 3 or more of stopChars
		// esp. important for ---- and _____ which are used as line separators
		char[] stopChars = "-_".toCharArray();
		for (char stopChar: stopChars)
		{
			String regex = "";
			for (int i = 0; i < 3; i++)
				regex += stopChar;
			regex += stopChar + "*";
			// find the pattern and repalce with ""
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(s);
			s = m.replaceAll("");
		}

		return s;
	}
}
