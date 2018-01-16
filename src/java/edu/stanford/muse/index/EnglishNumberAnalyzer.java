package edu.stanford.muse.index;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class EnglishNumberAnalyzer extends StopwordAnalyzerBase {
	private final CharArraySet stemExclusionSet;

	public static CharArraySet getDefaultStopSet() {
		return EnglishNumberAnalyzer.DefaultSetHolder.DEFAULT_STOP_SET;
	}

	public EnglishNumberAnalyzer() {
		this(EnglishNumberAnalyzer.DefaultSetHolder.DEFAULT_STOP_SET);
	}

	public EnglishNumberAnalyzer(CharArraySet stopwords) {
		this(stopwords, CharArraySet.EMPTY_SET);
	}

	public EnglishNumberAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet) {
		super(stopwords);
		this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
	}
/*
 * Creates a {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} which
 * tokenizes all the text in the reader that is set after the instantiation of this analyzer .
 *
 * @return A {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} built from an {@link StandardTokenizer} filtered with {@link StandardFilter}, {@link EnglishPossessiveFilter}, {@link LowerCaseFilter}, {@link StopFilter} , {@link SetKeywordMarkerFilter} if a stem exclusion set is
 *         provided and {@link PorterStemFilter}.
 **/

	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source = new StandardNumberTokenizer();
		TokenStream result = new StandardFilter(source);
//@TODO document what each of these filters do
		result = new EnglishPossessiveFilter(result);
		result = new LowerCaseFilter(result);
		result = new StopFilter(result, this.stopwords);
		if(!this.stemExclusionSet.isEmpty()) {
			result = new SetKeywordMarkerFilter((TokenStream)result, this.stemExclusionSet);
		}

		result = new PorterStemFilter((TokenStream)result);
		return new TokenStreamComponents(source, result);
	}

	protected TokenStream normalize(String fieldName, TokenStream in) {
		TokenStream result = new StandardFilter(in);
		result = new LowerCaseFilter(result);
		return result;
	}

	private static class DefaultSetHolder {
		static final CharArraySet DEFAULT_STOP_SET;

		private DefaultSetHolder() {
		}

		static {
			DEFAULT_STOP_SET = StandardAnalyzer.STOP_WORDS_SET;
		}
	}
}

/*
package edu.stanford.muse.index;

import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.util.Version;

*/
/**
 * This class is exactly like EnglishAnalyzer, in fact all the code is copied
 * from the EnglishAnanlyzer.
 * except that this uses StandardNumberTokenizer instead of StandardTokenizer to
 * emit consecutive numbers as one token..
 * Could n't extend EnglishAnanlyzer as it is defined final.
 * Why is there no analyzer routine that takes tokenize as input
 * Change the implementation, either extend or use derogation.
 *//*

public class EnglishNumberAnalyzer extends StopwordAnalyzerBase{
	private final CharArraySet	stemExclusionSet;

	*/
/**
	 * Returns an unmodifiable instance of the default stop words set.
	 * 
	 * @return default stop words set.
	 *//*

	public static CharArraySet getDefaultStopSet() {
		return DefaultSetHolder.DEFAULT_STOP_SET;
	}

	*/
/**
	 * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer
	 * class
	 * accesses the static final set the first time.;
	 *//*

	private static class DefaultSetHolder {
		static final CharArraySet	DEFAULT_STOP_SET	= StandardAnalyzer.STOP_WORDS_SET;
	}

	*/
/**
	 * Builds an analyzer with the default stop words: {@link #getDefaultStopSet}.
	 *//*

	public EnglishNumberAnalyzer() {
		this(DefaultSetHolder.DEFAULT_STOP_SET);
	}

	*/
/**
	 * Builds an analyzer with the given stop words.
	 * 
	 * @param matchVersion
	 *            lucene compatibility version
	 * @param stopwords
	 *            a stopword set
	 *//*

	public EnglishNumberAnalyzer(Version matchVersion, CharArraySet stopwords) {
		this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
	}

	*/
/**
	 * Builds an analyzer with the given stop words. If a non-empty stem
	 * exclusion set is
	 * provided this analyzer will add a {@link SetKeywordMarkerFilter} before
	 * stemming.
	 * 
	 * @param matchVersion
	 *            lucene compatibility version
	 * @param stopwords
	 *            a stopword set
	 * @param stemExclusionSet
	 *            a set of terms not to be stemmed
	 *//*

	public EnglishNumberAnalyzer(Version matchVersion, CharArraySet stopwords, CharArraySet stemExclusionSet) {
		super(stopwords);
		this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(
				stemExclusionSet));
	}

	*/
/**
	 * Creates a {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} which
	 * tokenizes all the text in the provided {@link Reader}.
	 * 
	 * @return A {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} built from an {@link StandardTokenizer} filtered with {@link StandardFilter}, {@link EnglishPossessiveFilter}, {@link LowerCaseFilter}, {@link StopFilter} , {@link SetKeywordMarkerFilter} if a stem exclusion set is
	 *         provided and {@link PorterStemFilter}.
	 *//*

	@Override
	protected TokenStreamComponents createComponents(String fieldName,
			Reader reader) {
        //TODO: document what each of the filter, does.
		final Tokenizer source = new StandardNumberTokenizer( reader);
		TokenStream result = new StandardFilter(source);
		// prior to this we get the classic behavior, standardfilter does it for us.
		result = new EnglishPossessiveFilter(result);
		result = new LowerCaseFilter(result);
		result = new StopFilter(result, stopwords);
		if (!stemExclusionSet.isEmpty())
			result = new SetKeywordMarkerFilter(result, stemExclusionSet);
		result = new PorterStemFilter(result);
		return new TokenStreamComponents(source, result);
	}

	public static void main(String[] args) {
		Analyzer analyzer = new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(final String fieldName,
					final Reader reader) {
                Version matchVersion = Indexer.LUCENE_VERSION;
                final Tokenizer source = new StandardNumberTokenizer(reader);
                TokenStream result = new LowerCaseFilter( source);
                return new TokenStreamComponents(source, result);
			}
		};
        //Cheat sheet on how different analyzers work
        //Standard Analyzer tokenizes phrase like: "W.S. Merwin" into "w.s" and "merwin"
        //Simple Analyzer "w", "s", "merwin"
        //WhiteSpaceAnalyzer: "W.S." and "Merwin"
        //EnglishAnalyzer "w." and "merwin" (because of stop words filter)
        // of all Simple Analyzer has more predictable behavior
        //also want the stop words to be highlighted
        //new SimpleAnalyzer(lv);
        String text = ".... .........,\n" +
              "\n" +
              "   ....... .. ... ... ............ .... ... ... ..../Buffalo's ........\n" +
              "\n" +
              "   .... ........ .. ........ .. ....... ... ....... ..... .... . ..... ...\n" +
              ".......... ..... .. ............ .. ....... ... ....... ....... .. .. . ....\n" +
              "..... ..... ........... .. ... .......; .. .... ... ....... . ..... ... ......\n" +
              "... ........... .. ... .... ...., ......... .. ... ..... ... ....: .....-..\n" +
              "\n" +
              "   .. ... .... .. ........... .... .... ...., ...... ... .......... ......\n" +
              "\n" +
              "   .. ... .... .. . ... ...... ... ...... .. .. .... .... ... .. ... ... .......\n" +
              ".... ......., .... .... ..... .. .........\n" +
              "\n" +
              "   ... ... ...., Harvey\n" +
              "\n" +
              "--------------------------------------------------------------------------\n" +
              "Harvey Axlerod,\n" +
              ".... ....., .....-............-....\n" +
              ".......@...........\n" +
              "--------------------------------------------------------------------------\n" +
              ".. ........... .... .... ....:\n" +
              "\n" +
              ".. .... .. ..... ..: ........@....................\n" +
              "\n" +
              ".. .. ... .... .. ... ....., ..... ... ....:\n" +
              "\n" +
              "..... .....-............-....\n" +
              ">>";
        //String text = "Phillip Randolph, A.L. Lewis-, etc";
        analyzer = new EnglishNumberAnalyzer(Version.LUCENE_7_2_1);
		try {
			TokenStream stream = analyzer.tokenStream(null, new StringReader(text));
			CharTermAttribute cattr = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			while (stream.incrementToken()) {
				System.out.println(cattr.toString());
			}
			stream.end();
			stream.close();
			analyzer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}*/
