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
package edu.stanford.muse.lang;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.stanford.muse.index.InternTable;
import edu.stanford.muse.util.Util;

public class Languages {
    public static Log log = LogFactory.getLog(Languages.class);

	/** Google translate supports: 
	 * English, Afrikaans, Albanian, Arabic, Armenian, Azerbaijani, Basque, Belarusian, Bengali, 
	 * Bulgarian, Catalan, Chinese, Croatian, Czech, Danish, Dutch, Esperanto, Estonian, Filipino, 
	 * Finnish, French, Galician, Georgian, German, Greek, Gujarati, Haitian Creole, Hebrew, Hindi, 
	 * Hungarian, Icelandic, Indonesian, Irish, Italian, Japanese, Kannada, Korean, Latin, Latvian, 
	 * Lithuanian, Macedonian, Malay, Maltese, Norwegian, Persian, Polish, Portuguese, Romanian, 
	 * Russian, Serbian, Slovak, Slovenian, Spanish, Swahili, Swedish, Tamil, Telugu, Thai, Turkish, 
	 * Ukrainian, Urdu, Vietnamese, Welsh, Yiddish
	 */
	
	/* http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes.
	 * http://answers.oreilly.com/topic/215-how-to-use-unicode-code-points-properties-blocks-and-scripts-in-regular-expressions
	 * http://www.oracle.com/technetwork/java/javase/locales-137662.html#util-text
	 */
	static String[][] script_languages = new String[][] {
		{"Armenian", "Armenian"},
		{"Hebrew", "Hebrew"},
		{"Arabic", "Arabic"},
	//	{"Syriac", ""},
//		{"Thaana", ""}, // maldives
		{"Devanagari", "Hindi,Marathi"},
		{"Bengali", "Bengali"},
		{"Gurmukhi", "Punjabi"},
		{"Gujarati", "Gujarati"},
		{"Oriya", "Oriya"},
		{"Tamil", "Tamil"},
		{"Telugu", "Telugu"},
		{"Kannada", "Kannada"},
		{"Malayalam", "Malayalam"},
//		{"Sinhala", "Sinhala"},
		{"Thai", "Thai"},
	//	{"Lao", "lo"},
	//	{"Tibetan", "bo"},
	//	{"Myanmar", "my"},
		{"Georgian", "Georgian"},
//		{"Ethiopic", "Ethiopic"},
//		{"Cherokee", "Cherokee"},
//		{"Unified_Canadian_Aboriginal_Syllabics", "??"},
//		{"Ogham", "Irish"},
//		{"Runic", "??"}, // some german languages
//		{"Tagalog", "tl"},
//		{"Buhid", "??"},
//		{"Tagbanwa", "tl"}, // related to tagalog
//		{"Khmer", "km"},
//		{"Mongolian", "mn"},
//		{"Limbu", "li"},
//		{"Tai_Le", "??"}, // tai nua language
//		{"Khmer_Symbols", "km"},
		{"Greek_Extended", "Greek"},
		{"CJK_Radicals_Supplement", "Chinese"},
		{"Kangxi_Radicals", "Chinese"},
		{"CJK_Symbols_and_Punctuation", "Chinese"},
		{"Kanbun", "Chinese"},
		{"Bopomofo_Extended", "Chinese"},
		{"Enclosed_CJK_Letters_and_Months", "Chinese"},
		{"CJK_Compatibility", "Chinese"},
		{"CJK_Unified_Ideographs_Extension_A", "Chinese"},
		{"Yijing_Hexagram_Symbols", "Chinese"},
		{"CJK_Unified_Ideographs", "Chinese"},
		{"Yi_Syllables", "Chinese"},
		{"Yi_Radicals", "Chinese"},
		{"CJK_Compatibility_Ideographs", "Chinese"},
		{"CJK_Compatibility_Forms", "Chinese"},
		{"Hanunoo", "Japanese"},
		{"Hiragana", "Japanese"},
		{"Katakana", "Japanese"},
		{"Bopomofo", "Japanese"},
		{"Katakana_Phonetic_Extensions", "Japanese"},
		{"Hangul_Compatibility_Jamo", "Korean"},
		{"Hangul_Syllables", "Korean"},
		{"Hangul_Jamo", "Korean"},
	};

	/** little helper class */
	static class LangInfo {
		Pattern pattern;
		String language;
		public LangInfo(Pattern p, String language) {
			this.pattern = p;
			this.language = language;
		}
	}
	
	public static List<LangInfo> allPatterns = new ArrayList<>();
	public static Set<String> allScripts = new LinkedHashSet<>();
	public static Set<String> allLanguages = new LinkedHashSet<>();
	
	static { init(); }
	
	public static void init()
	{		
		allPatterns.clear();
		allLanguages.clear();
		allScripts.clear();
		allLanguages.add("English");
		allScripts.add("Roman");
		
		for (String[] lang: script_languages)
		{
			String scriptName = lang[0];
			String langString = lang[1];
			List<String> languages = new ArrayList<>();
			
			// tokenize langString by ,
			StringTokenizer st = new StringTokenizer(langString,",");
			while (st.hasMoreTokens())
			{
				String language = st.nextToken();
				language = language.trim();
				if (language.length() == 0)
					continue;
				languages.add(language);
			}
			
			// build up pattern array or all pattern to languages
			for (String language: languages)
			{
				Pattern p = Pattern.compile(".*\\p{In" + scriptName + "}.*", Pattern.DOTALL); // DOTALL allows matching across lines which is what we want to detect chars in a given script
				allPatterns.add(new LangInfo(p, language));
				allScripts.add(scriptName);
				allLanguages.add(language);
			}
		}
		
		log.info(allScripts.size() + " scripts, " + allLanguages.size() + " languages, " + allPatterns.size() + " patterns");
	}	
	
	/** returns all languages for a bunch of text, based on the script used */
	public static Set<String> getAllLanguages(String text)
	{
		// can make it more efficient by allowing p to point to multiple languages (e.g. Devanagari)
		Set<String> result = new LinkedHashSet<>();
		for (LangInfo p: allPatterns)
		{
			if (p.pattern.matcher(text).matches())
				result.add(InternTable.intern(p.language.toLowerCase()));
		}
		result.add(InternTable.intern("english")); // always adding english, currently
		return result;
	}
	
	public static void main (String args[])
	{
		String s = "ಕನ್ನಡ ಟೆಸ್ಟ್ ગુજરાતી ટેસ્ટ दी टेस्ट தமிழ் டெஸ்ட் 可憐  同 情  憐 憫 關愛 愛 護 憐惜 悲 憫 sadness:悲哀|傷心|痛苦|愁苦|哀愁|悲觀 happy:歡喜|高興|快樂|喜悅|開心  興奮 樂觀";
		System.out.println (Util.join(getAllLanguages(s), "|")); 
	}
}
