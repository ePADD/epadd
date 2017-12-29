package edu.stanford.muse.index;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerImpl;
import org.apache.lucene.analysis.standard.StandardTokenizerInterface;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

final class StandardNumberTokenizer extends Tokenizer {
	private StandardTokenizerImpl	scanner;

	private static final int			HOST			= 5;
	private static final int			NUM				= 6;

	private static final int			ACRONYM_DEP		= 8;


	/** String token types that correspond to token type int constants */
	private static final String[]	TOKEN_TYPES		= new String[] {
													"<ALPHANUM>",
													"<APOSTROPHE>",
													"<ACRONYM>",
													"<COMPANY>",
													"<EMAIL>",
													"<HOST>",
													"<NUM>",
													"<CJ>",
													"<ACRONYM_DEP>",
													"<SOUTHEAST_ASIAN>",
													"<IDEOGRAPHIC>",
													"<HIRAGANA>",
													"<KATAKANA>",
													"<HANGUL>"
													};

	private int						skippedPositions;

	private static final int maxTokenLength	= StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;

	public StandardNumberTokenizer(Version matchVersion, Reader input) {
		super(input);
		init(matchVersion);
	}

	public StandardNumberTokenizer(Version matchVersion, AttributeFactory factory, Reader input) {
		super(factory, input);
		init(matchVersion);
	}

	private final void init(Version matchVersion) {
		if (matchVersion.onOrAfter(Version.LUCENE_47))
			this.scanner = new StandardTokenizerImpl(input);
	}

	// this tokenize generates three attributes:
	// term offset, positionIncrement and type
	private final CharTermAttribute				termAtt		= addAttribute(CharTermAttribute.class);
	private final OffsetAttribute				offsetAtt	= addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute	posIncrAtt	= addAttribute(PositionIncrementAttribute.class);
	private final TypeAttribute					typeAtt		= addAttribute(TypeAttribute.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.analysis.TokenStream#next()
	 */
	@Override
	public final boolean incrementToken() throws IOException {
		clearAttributes();
		skippedPositions = 0;

		while (true) {
			int tokenType = scanner.getNextToken();
			if (tokenType == StandardTokenizerInterface.YYEOF) {
				return false;
			}

			if (scanner.yylength() <= maxTokenLength) {
				posIncrAtt.setPositionIncrement(skippedPositions + 1);
				scanner.getText(termAtt);
				final int start = scanner.yychar();
				offsetAtt.setOffset(correctOffset(start), correctOffset(start + termAtt.length()));
				// This 'if' should be removed in the next release. For now, it converts
				// invalid acronyms to HOST. When removed, only the 'else' part should
				// remain.
				if (tokenType == StandardNumberTokenizer.ACRONYM_DEP) {
					typeAtt.setType(StandardNumberTokenizer.TOKEN_TYPES[StandardNumberTokenizer.HOST]);
					termAtt.setLength(termAtt.length() - 1); // remove extra '.'
				}
				else if (tokenType == StandardNumberTokenizer.NUM) {
					String term = "";
					int end = start + termAtt.length();
					while (true) {
						int type = scanner.getNextToken();
						if (type != NUM) {
                            scanner.yypushback(scanner.yylength());
							break;
						} else {
							term += "-" + scanner.yytext();
							//extra 1 for the the character that separates the words
							end = end + scanner.yylength() + 1;
						}
					}
					offsetAtt.setOffset(correctOffset(start), correctOffset(end));
					termAtt.append(term);
					typeAtt.setType(StandardNumberTokenizer.TOKEN_TYPES[tokenType]);
				}
				else{
					typeAtt.setType(StandardNumberTokenizer.TOKEN_TYPES[tokenType]);
				}
                return true;
			} else
				// When we skip a too-long term, we still increment the
				// position increment
				skippedPositions++;
		}
	}

	@Override
	public final void end() throws IOException {
		super.end();
		// set final offset
		int finalOffset = correctOffset(scanner.yychar() + scanner.yylength());
		offsetAtt.setOffset(finalOffset, finalOffset);
		// adjust any skipped tokens
		posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement() + skippedPositions);
	}

	@Override
	public void close() throws IOException {
		super.close();
		scanner.yyreset(input);
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		scanner.yyreset(input);
		skippedPositions = 0;
	}

	public static void main(String[] args) {
		try {
            StringReader input = new StringReader("Passport number: k4190175 ");
            StandardNumberTokenizer t = new StandardNumberTokenizer(Version.LUCENE_47, input);
			OffsetAttribute offsetAttribute = t.addAttribute(OffsetAttribute.class);
			CharTermAttribute charTermAttribute = t.addAttribute(CharTermAttribute.class);
			TypeAttribute typeAttribute = t.addAttribute(TypeAttribute.class);

			t.reset();
			while (t.incrementToken()) {
				int startOffset = offsetAttribute.startOffset();
				int endOffset = offsetAttribute.endOffset();
				String term = charTermAttribute.toString();
				String type = typeAttribute.type();

				System.err.println(term + " at: " + startOffset + "," + endOffset + ". Type: " + type);
			}

			t.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
