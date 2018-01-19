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
 * --NOTE: Changed this class while porting to Lucene 7.2.1 based on StandardTokenizer class of that version.
 * It required change in the signature of some methods, however the core method incrementToken remained same
 * as was present in ePADD 4.0 (with lucene 4.7).
 */

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerImpl;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

final class StandardNumberTokenizer extends Tokenizer {
	private StandardTokenizerImpl	scanner;



	private int						skippedPositions;

	private static final int maxTokenLength	= StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;

	public StandardNumberTokenizer() {
		//this.setReader(reader);
		this.init();
	}

	public StandardNumberTokenizer( AttributeFactory factory) {
		super(factory);
		//this.setReader(reader);
		this.init();
	}

	private final void init() {
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
			if (tokenType == StandardTokenizerImpl.YYEOF) {
				return false;
			}

			if (scanner.yylength() <= maxTokenLength) {
				posIncrAtt.setPositionIncrement(skippedPositions + 1);
				scanner.getText(termAtt);
				final int start = scanner.yychar();
				offsetAtt.setOffset(correctOffset(start), correctOffset(start + termAtt.length()));
			if (tokenType == StandardTokenizer.NUM) {
					String term = "";
					int end = start + termAtt.length();
					while (true) {
						int type = scanner.getNextToken();
						if (type != StandardTokenizer.NUM) {
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
					typeAtt.setType(StandardTokenizer.TOKEN_TYPES[tokenType]);
				}
				else{
					typeAtt.setType(StandardTokenizer.TOKEN_TYPES[tokenType]);
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
            StringReader input = new StringReader("123-45-6789");
            StandardNumberTokenizer t = new StandardNumberTokenizer();
            t.setReader(input);
			OffsetAttribute offsetAttribute = t.addAttribute(OffsetAttribute.class);
			CharTermAttribute charTermAttribute = t.addAttribute(CharTermAttribute.class);
			TypeAttribute typeAttribute = t.addAttribute(TypeAttribute.class);

			t.reset();
			while (t.incrementToken()) {
				int startOffset = offsetAttribute.startOffset();
				int endOffset = offsetAttribute.endOffset();
				String term = charTermAttribute.toString();
				String type = typeAttribute.type();

				System.out.println(term + " at: " + startOffset + "," + endOffset + ". Type: " + type);
			}

			t.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
