/*
 * Copyright 2015 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* The following code was generated by JFlex 1.4.3 on 03.05.15 13:19 */

package com.perl5.lang.perl.lexer;


import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.perl5.lang.perl.util.PerlFunctionUtil;
import com.perl5.lang.perl.util.PerlPackageUtil;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerlLexer extends PerlLexerGenerated{

	protected IElementType lastSignificantTokenType;
	protected String lastSignificantToken;
	protected IElementType lastTokenType;
	protected HashMap<String,IElementType> knownPackages = new HashMap<>();

	public PerlLexer(java.io.Reader in) {
		super(in);
	}

	/**
	 * Lexers advance method. Parses some thing here, or just invoking generated flex parser
	 * @return next token type
	 * @throws IOException
	 */
	public IElementType advance() throws IOException{

		CharSequence buffer = getBuffer();
		int tokenStart = getTokenEnd();
		int bufferEnd = buffer.length();

		if( bufferEnd > 0 && tokenStart < bufferEnd )
		{
			int currentState = yystate();

			// higest priority, pre-parsed tokens
			if( currentState == LEX_PREPARSED_ITEMS )
			{
				IElementType nextTokenType = getParsedToken();
				if( nextTokenType != null )
					return nextTokenType;
			}

			// capture heredoc
			if( currentState == LEX_HEREDOC_WAITING && (tokenStart == 0 || buffer.charAt(tokenStart-1) =='\n'))
			{
				IElementType tokenType = captureHereDoc();
				if( tokenType != null )	// got something
					return tokenType;
			}
			// capture pod
			else if( buffer.charAt(tokenStart) == '=' && (tokenStart == 0 || buffer.charAt(tokenStart-1) =='\n'))
			{
				return capturePodBlock();
			}
			// capture string content from "" '' `` q qq qx
			else if( currentState == LEX_QUOTE_LIKE_CHARS)
			{
				int currentPosition = tokenStart;

				boolean isEscaped = false;
				boolean quotesDiffer = charOpener != charCloser;
				int quotesDepth = 0;	// for using with different quotes

				while(currentPosition < bufferEnd )
				{
					char currentChar = buffer.charAt(currentPosition);

					if( !isEscaped && quotesDepth == 0 && currentChar == charCloser)
						break;

					if( !isEscaped && quotesDiffer )
					{
						if( currentChar == charOpener)
							quotesDepth++;
						else if( currentChar == charCloser)
							quotesDepth--;
					}

					isEscaped = !isEscaped && currentChar == '\\';

					currentPosition++;
				}

				if( currentPosition == bufferEnd )
					// forces to exit lex state
					popState();
				else
					// switch to closer lex state
					yybegin(LEX_QUOTE_LIKE_CLOSER);

				if( currentPosition > tokenStart )
				{
					// found string
					setTokenStart(tokenStart);
					setTokenEnd(currentPosition);
					return PERL_STRING_CONTENT;
				}
				else
					// empty string
					return quoteLikeCloser(tokenStart);
			}
			// closing quote of string
			else if (currentState == LEX_QUOTE_LIKE_CLOSER)
				return quoteLikeCloser(tokenStart);
			// capture __DATA__ __END__
			else if( ((tokenStart < bufferEnd - 8 ) && "__DATA__".equals(buffer.subSequence(tokenStart, tokenStart + 8).toString()))
				|| ((tokenStart < bufferEnd - 7 ) && "__END__".equals(buffer.subSequence(tokenStart, tokenStart + 7).toString()))
			)
			{
				setTokenStart(tokenStart);
				setTokenEnd(bufferEnd);
				return PERL_COMMENT_BLOCK;
			}
			// capture line comment
			else if(
					buffer.charAt(tokenStart)=='#'
					&& (currentState != LEX_QUOTE_LIKE_OPENER || !allowSharpQuote)
					&& (currentState != LEX_TRANS_OPENER && currentState != LEX_TRANS_CLOSER || !allowSharpQuote)
					&& (currentState != LEX_TRANS_CHARS)
					&& (currentState != LEX_REGEX_OPENER)
			)
			{
				// comment may end on newline or ?>
				int currentPosition = tokenStart;
				setTokenStart(tokenStart);

				while( currentPosition < bufferEnd && !isCommentEnd(currentPosition) ){currentPosition++;}

				// catching annotations #@
				if( tokenStart+1 < bufferEnd && buffer.charAt(tokenStart+1) == '@')
				{
					if( currentPosition > tokenStart + 2)
						parseAnnotation(buffer.subSequence(tokenStart + 2, currentPosition), tokenStart+2);

					setTokenEnd(tokenStart+2);
					return ANNOTATION_PREFIX;
				}

				setTokenEnd(currentPosition);
				return PERL_COMMENT;
			}

		}

		IElementType tokenType = super.advance();

		lastTokenType = tokenType;
		if( tokenType != TokenType.NEW_LINE_INDENT
			&& tokenType != TokenType.WHITE_SPACE
			&& tokenType != PERL_COMMENT
			&& tokenType != PERL_COMMENT_BLOCK
		)
		{
			lastSignificantTokenType = tokenType;
			lastSignificantToken = yytext().toString();

			if( yystate() == 0 && tokenType != PERL_SEMI) // to enshure proper highlighting reparsing
				yybegin(LEX_CODE);
		}

		return tokenType;
	}


	public static Pattern annotationPattern = Pattern.compile("^(\\w+)(?:(\\s+)(.+)?)?$");

	public static Pattern annotationPatternPackage = Pattern.compile("^(\\w+(?:::\\w+)*)(.*)$");

	/**
	 * Parses annotation line and puts result into the pre-parsed buffer
	 * @param annotationLine - string with annotation after marker
	 */
	void parseAnnotation(CharSequence annotationLine, int baseOffset)
	{
		Matcher m = annotationPattern.matcher(annotationLine);
		tokensList.clear();
		CharSequence tailComment = null;

		if( m.matches())
		{
			String annotationKey = m.group(1);
			IElementType tokenType = PerlAnnotations.TOKEN_TYPES.get(m.group(1));

			if( tokenType == null )
				tokenType = ANNOTATION_UNKNOWN_KEY;

			tokensList.add(new CustomToken(baseOffset, baseOffset +m.group(1).length(), tokenType));
			baseOffset += m.group(1).length();

			if( m.group(2) != null)
			{
				tokensList.add(new CustomToken(baseOffset, baseOffset +m.group(2).length(), TokenType.WHITE_SPACE));
				baseOffset += m.group(2).length();
			}

			if( tokenType == ANNOTATION_RETURNS_KEY && m.group(3) != null)
			{
				// additional parsing
				String annotationRest = m.group(3);
				Matcher pm = annotationPatternPackage.matcher(annotationRest);

				if( pm.matches())
				{
					if( pm.group(1) != null && pm.group(1).length() > 0)
					{
						tokensList.add(new CustomToken(baseOffset, baseOffset + pm.group(1).length(), PERL_PACKAGE));
						baseOffset += pm.group(1).length();
					}

					tailComment = pm.group(2);
				}
				else
					tailComment = m.group(3);
			}
			else
				tailComment = m.group(3);
		}
		else
			tailComment = annotationLine;

		if( tailComment != null && tailComment.length() > 0 )
			tokensList.add(new CustomToken(baseOffset, baseOffset+tailComment.length(), PERL_COMMENT));

		if( tokensList.size() > 0 )
		{
			pushState();
			yybegin(LEX_PREPARSED_ITEMS);
		}
	}


	/**
	 * Processes quote closer token
	 * @param tokenStart	offset of current token start
	 * @return	quote element type
	 */
	IElementType quoteLikeCloser(int tokenStart)
	{
		popState();
		setTokenStart(tokenStart);
		setTokenEnd(tokenStart+1);
		return PERL_QUOTE;
	}


	/**
	 * Checking if comment is ended. Implemented for overriding in {@link com.perl5.lang.embedded.EmbeddedPerlLexer#isCommentEnd(int)} }
	 * @param currentPosition current position to check
	 * @return checking result
	 */
	public boolean isCommentEnd(int currentPosition)
	{
		return getBuffer().charAt(currentPosition) == '\n';
	}


	/**
	 * Captures pod block from current position
	 * @return PERL_POD token type
	 */
	public IElementType capturePodBlock()
	{
		CharSequence buffer = getBuffer();
		int tokenStart = getTokenEnd();
		setTokenStart(tokenStart);
		int bufferEnd = buffer.length();

		int currentPosition = tokenStart;
		int linePos = currentPosition;

		while( true )
		{
			while(linePos < bufferEnd && buffer.charAt(linePos) != '\n'){linePos++;}
			if( linePos < bufferEnd && buffer.charAt(linePos) == '\n' )
				linePos++;
			String line = buffer.subSequence(currentPosition, linePos).toString();
			currentPosition = linePos;

			if( linePos == bufferEnd || line.startsWith("=cut"))
			{
				setTokenEnd(linePos);
				break;
			}
		}

		return PERL_POD;
	}

	/**
	 * HEREDOC proceccing section
	 */

	// last captured heredoc marker
	public String heredocMarker;

	// pattern for getting marker
	public Pattern markerPattern = Pattern.compile("<<\\s*['\"`]?([^\"\'`]+)['\"`]?");

	/**
	 * Processing captured heredoc opener. Stores marker and switches to proper lexical state
	 * @return PERL_OPERATOR  for << operator
	 */
	public IElementType processHeredocOpener()
	{
		String openToken = yytext().toString();
		Matcher m = markerPattern.matcher(openToken);

		yypushback(openToken.length() - 2);

		if (m.matches())
		{
			if( m.group(1).matches("\\d+") )	// check if it's numeric shift
				return PERL_OPERATOR;
			heredocMarker = m.group(1);
		}

		pushState();
		yybegin(LEX_HEREDOC_WAITING);
		pushState();
		yybegin(LEX_HEREDOC_OPENER);

		return PERL_OPERATOR;
	}

	/**
	 * Captures HereDoc document and returns appropriate token type
	 * @return Heredoc token type
	 */
	public IElementType captureHereDoc()
	{
		CharSequence buffer = getBuffer();
		int tokenStart = getTokenEnd();
		setTokenStart(tokenStart);
		int bufferEnd = buffer.length();

		int currentPosition = tokenStart;
		int linePos = currentPosition;

		IElementType blockType;

//		if( "SQL".equals(heredocMarker))
//			blockType = PerlTokenType.HEREDOC_SQL;
//		else
			blockType = PERL_HEREDOC;

		String endPattern = "^" + heredocMarker + "[\r\n]+";

		while( true )
		{
			while(linePos < bufferEnd && buffer.charAt(linePos) != '\n' && buffer.charAt(linePos) != '\r'){linePos++;}
			if(linePos < bufferEnd && buffer.charAt(linePos) == '\r')
				linePos++;
			if(linePos < bufferEnd && buffer.charAt(linePos) == '\n' )
				linePos++;

			// reached the end of heredoc and got end marker
			if( Pattern.matches(endPattern, buffer.subSequence(currentPosition, linePos)))
			{
				yybegin(LEX_HEREDOC_MARKER);

				// non-empty heredoc and got the end
				if( currentPosition > tokenStart )
				{
					setTokenStart(tokenStart);
					setTokenEnd(currentPosition);
					return blockType;
				}
				// empty heredoc and got the end
				else
					return null;
			}
			// reached the end of file
			else if( linePos == bufferEnd)
			{
				popState();
				// non-empty heredoc and got the end of file
				if( currentPosition > tokenStart )
				{
					setTokenStart(tokenStart);
					setTokenEnd(currentPosition);
					return blockType;
				}
				// empty heredoc and got the end of file
				else
					return null;
			}
			currentPosition = linePos;
		}
	}

	public void reset(CharSequence buf, int start, int end, int initialState)
	{
//		if(end > 0)
//			System.out.printf("Reset to %d %d %d `%c`\n", start, end, initialState, buf.charAt(start));
		super.reset(buf,start,end,initialState);
		lastTokenType = null;
		lastSignificantTokenType = null;
	}

	/**
	 * Forces push back and reparsing
	 * @param newState exclusive state for re-parsing specific constructions
	 */
	public void startCustomBlock(int newState)
	{
		yypushback(yylength());
		pushState();
		yybegin(newState);
	}

	/**
	 * Ends custom block parsing
	 */
	public void endCustomBlock()
	{
		popState();
	}

	/**
	 *  States stack
	 **/
	private final Stack<Integer> stateStack = new Stack<Integer>();

	public void pushState()
	{
		stateStack.push(yystate());
	}

	public void popState()
	{
		setState(stateStack.pop());
	}

	/**
	 *  Quote-like, transliteration and regexps common part
	 */
	public boolean allowSharpQuote = true;
	public char charOpener;
	public char charCloser;
	public int stringContentStart;
	public boolean isEscaped = false;

	public int sectionsNumber = 0; 	// number of sections one or two
	public int currentSectionNumber = 0; // current section

	public final LinkedList<CustomToken> tokensList = new LinkedList<CustomToken>();

	private IElementType restoreToken( CustomToken token)
	{
		setTokenStart(token.getTokenStart());
		setTokenEnd(token.getTokenEnd());
		return token.getTokenType();
	}

	/**
	 * Disallows sharp delimiter on space occurance for quote-like operations
	 * @return whitespace token type
	 */
	public IElementType processOpenerWhiteSpace()
	{
		allowSharpQuote = false;
		return TokenType.WHITE_SPACE;
	}

	/**
	 *  Reading tokens from parsed queue, setting start and end and returns them one by one
	 * @return token type or null if queue is empty
	 */
	public IElementType getParsedToken()
	{
		if(tokensList.size() == 0 )
		{
			popState();
			return null;
		}
		else
		{
			return restoreToken(tokensList.removeFirst());
		}
	}

	/**
	 *	Regex processor qr{} m{} s{}{}
	 **/
	String regexCommand = null;

	// guess if this is a division or started regex
	public IElementType processDiv()
	{
		if(	// seems regex, @todo map types and words
				lastSignificantTokenType == null
			||	lastSignificantTokenType == PERL_OPERATOR
			|| lastSignificantTokenType == PERL_OPERATOR_X
			|| lastSignificantTokenType == PERL_LPAREN
			|| lastSignificantTokenType == PERL_LBRACE
			|| lastSignificantTokenType == PERL_LBRACK
			|| lastSignificantTokenType == PERL_SEMI
			|| "return".equals(lastSignificantToken)
			|| "split".equals(lastSignificantToken)
			|| "if".equals(lastSignificantToken)
			|| "unless".equals(lastSignificantToken)
			|| "grep".equals(lastSignificantToken)
			|| "map".equals(lastSignificantToken)
		)
		{
			allowSharpQuote = true;
			isEscaped = false;
			regexCommand = "m";
			sectionsNumber = 1;

			pushState();
			yypushback(1);
			yybegin(LEX_REGEX_OPENER);

			return null;
		}
		else
		{
			if( !isLastToken() && getBuffer().charAt(getNextTokenStart()) == '/')
			{
				setTokenEnd(getNextTokenStart()+1);
				return PERL_OPERATOR;
			}
			else
			{
				return PERL_OPERATOR_DIV;
			}
		}
	}


	/**
	 * Sets up regex parser
	 * @return command keyword
	 */
	public IElementType processRegexOpener()
	{
		allowSharpQuote = true;
		isEscaped = false;
		regexCommand = yytext().toString();

		if( "s".equals(regexCommand) )	// two sections s
			sectionsNumber = 2;
		else						// one section qr m
			sectionsNumber = 1;

		pushState();
		yybegin(LEX_REGEX_OPENER);
		return PERL_RESERVED;
	}

	/**
	 *  Parses regexp from the current position (opening delimiter) and preserves tokens in tokensList
	 *  REGEX_MODIFIERS = [msixpodualgcer]
	 *  @return opening delimiter type
	 */
	public IElementType parseRegex()
	{
		tokensList.clear();

		CharSequence buffer = getBuffer();
		int bufferEnd = getBufferEnd();

		// find block 1
		RegexBlock firstBlock = RegexBlock.parseBlock(buffer, getTokenStart() + 1, bufferEnd, yytext().charAt(0));

		if( firstBlock == null )
		{
//			System.err.println("Stop after first block");
			yybegin(YYINITIAL);
			return PERL_REGEX_QUOTE_OPEN;
		}
		int currentOffset = firstBlock.getEndOffset();

		// find block 2
		ArrayList<CustomToken> betweenBlocks = new ArrayList<CustomToken>();
		RegexBlock secondBLock = null;
		CustomToken secondBlockOpener = null;

		if( sectionsNumber == 2 && currentOffset < bufferEnd )
		{
			if(firstBlock.hasSameQuotes())
			{
				secondBLock = RegexBlock.parseBlock(buffer, currentOffset, bufferEnd, firstBlock.getOpeningQuote());
			}
			else
			{
				// spaces and comments between if {}, fill betweenBlock
				while( true )
				{
					char currentChar = buffer.charAt(currentOffset);
					if( RegexBlock.isWhiteSpace(currentChar) )	// white spaces
					{
						int whiteSpaceStart = currentOffset;
						while( RegexBlock.isWhiteSpace(buffer.charAt(currentOffset))){currentOffset++;}
						betweenBlocks.add(new CustomToken(whiteSpaceStart, currentOffset, TokenType.WHITE_SPACE));
					}
					else if( currentChar == '#' )	// line comment
					{
						int commentStart = currentOffset;
						while(buffer.charAt(currentOffset) != '\n'){currentOffset++;}
						betweenBlocks.add(new CustomToken(commentStart, currentOffset, PERL_COMMENT));
					}
					else
						break;
				}

				// read block
				secondBlockOpener = new CustomToken(currentOffset, currentOffset+1, PERL_REGEX_QUOTE_OPEN);
				secondBLock = RegexBlock.parseBlock(buffer, currentOffset + 1, bufferEnd, buffer.charAt(currentOffset));
			}

			if( secondBLock == null )
			{
//				System.err.println("Stop after second block");
				yybegin(YYINITIAL);
				return PERL_REGEX_QUOTE_OPEN;
			}
			currentOffset = secondBLock.getEndOffset();
		}

		// check modifiers for x
		boolean isExtended = false;
		boolean isEvaluated = false;
		List<Character> allowedModifiers = RegexBlock.allowedModifiers.get(regexCommand);
		int modifiersEnd = currentOffset;
		ArrayList<CustomToken> modifierTokens = new ArrayList<CustomToken>();

		while(true)
		{
			if( modifiersEnd == bufferEnd)	// eof
				break;
			else if( !allowedModifiers.contains(buffer.charAt(modifiersEnd)))	// unknown modifier
				break;
			else if( buffer.charAt(modifiersEnd) == 'x')	// mark as extended
				isExtended = true;
			else if( buffer.charAt(modifiersEnd) == 'e')	// mark as evaluated
				isEvaluated = true;

			modifierTokens.add(new CustomToken(modifiersEnd, modifiersEnd + 1, PERL_REGEX_MODIFIER));

			modifiersEnd++;
		}

		// parse block 1
		tokensList.addAll(firstBlock.tokenize(isExtended));

		if( secondBLock != null )
		{
			// parse spaces
			tokensList.addAll(betweenBlocks);

			if( secondBlockOpener != null)
				tokensList.add(secondBlockOpener);

			// parse block 2
			if( isEvaluated )
				tokensList.addAll(secondBLock.parseEval());
			else
				tokensList.addAll(secondBLock.tokenize(isExtended));
		}

		// parse modifiers
		tokensList.addAll(modifierTokens);

		yybegin(LEX_PREPARSED_ITEMS);

		return PERL_REGEX_QUOTE_OPEN;
	}


	/**
	 *	Transliteration processors tr y
	 **/

	public IElementType processTransOpener()
	{
		allowSharpQuote = true;
		isEscaped = false;
		currentSectionNumber = 0;
		pushState();
		yybegin(LEX_TRANS_OPENER);
		return PERL_RESERVED;
	}

	public IElementType processTransQuote()
	{
		charOpener = yytext().charAt(0);

		if( charOpener == '#' && !allowSharpQuote)
		{
			yypushback(1);
			popState();
			return null;
		}
		else charCloser = RegexBlock.getQuoteCloseChar(charOpener);

		yybegin(LEX_TRANS_CHARS);
		stringContentStart = getTokenStart() + 1;

		return PERL_REGEX_QUOTE_OPEN;
	}

	public IElementType processTransChar()
	{
		char currentChar = yytext().charAt(0);

		if( currentChar == charCloser && !isEscaped )
		{
			yypushback(1);
			setTokenStart(stringContentStart);
			yybegin(LEX_TRANS_CLOSER);
			return PERL_STRING_CONTENT;
		}
		else if( isLastToken() )
		{
			setTokenStart(stringContentStart);
			return PERL_STRING_CONTENT;
		}
		else
			isEscaped = ( currentChar == '\\' && !isEscaped );

		return null;
	}

	public IElementType processTransCloser()
	{
		if( currentSectionNumber == 0 ) // first section
		{
			currentSectionNumber++;
			if( charCloser == charOpener ) // next is replacements block
			{
				yybegin(LEX_TRANS_CHARS);
				stringContentStart = getTokenStart() + 1;
			}
			else	// next is new opener, possibly other
			{
				yybegin(LEX_TRANS_OPENER);
			}
		}
		else // last section
		{
			yybegin(LEX_TRANS_MODIFIERS);
		}
		return PERL_REGEX_QUOTE_CLOSE;
	}



	/**
	 *  Quote-like string procesors
	 **/
	public IElementType processQuoteLikeStringOpener()
	{
		allowSharpQuote = true;
		isEscaped = false;
		pushState();
		yybegin(LEX_QUOTE_LIKE_OPENER);
		return PERL_RESERVED;
	}

	public IElementType processQuoteLikeQuote()
	{
		charOpener = yytext().charAt(0);

		if( charOpener == '#' && !allowSharpQuote)
		{
			yypushback(1);
			yybegin(YYINITIAL);
			return null;
		}
		else charCloser = RegexBlock.getQuoteCloseChar(charOpener);

		if( !isLastToken() )
			yybegin(LEX_QUOTE_LIKE_CHARS);

		return PERL_QUOTE;
	}

	/**
	 *  Strings handler
	 */
	public IElementType processStringOpener()
	{
		isEscaped = false;
		charOpener = charCloser = yytext().charAt(0);
		pushState();
		if( !isLastToken() )
			yybegin(LEX_QUOTE_LIKE_CHARS);
		return PERL_QUOTE;
	}

	/**
	 *  Quote-like list procesors
	 **/

	public IElementType processQuoteLikeListOpener()
	{
		allowSharpQuote = true;
		pushState();
		yybegin(LEX_QUOTE_LIKE_LIST_OPENER);
		return PERL_RESERVED;
	}

	public IElementType processQuoteLikeListQuote()
	{
		charOpener = yytext().charAt(0);

		if( charOpener == '#' && !allowSharpQuote)
		{
			yypushback(1);
			yybegin(YYINITIAL);
			return null;
		}
		else charCloser = RegexBlock.getQuoteCloseChar(charOpener);

		yybegin(LEX_QUOTE_LIKE_WORDS);

		return PERL_QUOTE;
	}


	public IElementType processQuoteLikeWord()
	{
		CharSequence currentToken = yytext();

		isEscaped = false;

		for( int i = 0; i < currentToken.length(); i++ )
		{
			if( !isEscaped && currentToken.charAt(i) == charCloser )
			{
				yypushback(currentToken.length() - i);
				yybegin(LEX_QUOTE_LIKE_LIST_CLOSER);

				return i == 0 ? null: PERL_STRING_CONTENT;
			}

			isEscaped = !isEscaped && currentToken.charAt(i) == '\\';
		}
		return PERL_STRING_CONTENT;
	}


	public boolean waitingHereDoc(){return yystate() == LEX_HEREDOC_WAITING;}

	public IElementType processSemicolon()
	{
		if( !waitingHereDoc() )
			yybegin(YYINITIAL);
		else
		{
			stateStack.pop();
			stateStack.push(YYINITIAL);
		}
		return PERL_SEMI;
	}

	/**
	 * Logic for choosing type of braced bareword, like {defined}
	 * @return token type
	 */
	@Override
	public IElementType getBracedBarewordTokenType()
	{
		if( "defined".equals(yytext().toString()))
			return PERL_OPERATOR_UNARY;

		return PERL_STRING_CONTENT;
	}

	/**
	 * Detecting package type (built-in or regular). Register package in the internal hashmap
	 * @return token type
	 */
	@Override
	public IElementType getPackageType()
	{
		String packageName = PerlPackageUtil.getCanonicalPackageName(yytext().toString());
		if( !knownPackages.containsKey(packageName))
			knownPackages.put(packageName,PERL_PACKAGE);
		return knownPackages.get(packageName);
	}

	/**
	 * Guessing bareword as function or package, if it has been used before
	 * @return token type
	 */
	@Override
	public IElementType getBarewordTokenType()
	{
		String bareword = yytext().toString();
		if( knownPackages.containsKey(bareword) )
			return knownPackages.get(bareword);

		return PerlFunctionUtil.getFunctionType(bareword);
	}

	/**
	 * Checks if package has been used or is built in
	 * @return true if it's package, false otherwise
	 */
	@Override
	public boolean isKnownPackage()
	{
		String packageName = yytext().toString();
		return  knownPackages.containsKey(packageName) || PerlPackageUtil.isBuiltIn(packageName);
	}

	/**
	 * Parses token as built-in variable
	 */
	public static Pattern variablePattern = Pattern.compile("^(\\$#|\\$|@|%|\\*)(.+)$");
	public static Pattern bracedVariablePattern = Pattern.compile("^(\\$#|\\$|@|%|\\*)\\{(.+)\\}$");

	@Override
	public IElementType parseBuiltInVariable()
	{
		String tokenText = yytext().toString();
		int tokenStart = getTokenStart();
		pushState();
		yybegin(LEX_PREPARSED_ITEMS);

		tokensList.clear();

		Matcher m = variablePattern.matcher(tokenText);
		if( m.matches())
		{
			String sigil = m.group(1);
			String name = m.group(2);

			tokenStart += sigil.length();
			tokensList.add(new CustomToken(tokenStart, tokenStart + name.length(), PERL_VARIABLE_NAME));

			yypushback(tokenText.length()-sigil.length());
			return getSigilTokenType(sigil);
		}

		m = bracedVariablePattern.matcher(tokenText);
		if( m.matches())
		{
			String sigil = m.group(1);
			String name = m.group(2);

			tokenStart += sigil.length();
			tokensList.add(new CustomToken(tokenStart, tokenStart + 1, PERL_LBRACE));
			tokenStart++;
			tokensList.add(new CustomToken(tokenStart, tokenStart + name.length(), PERL_VARIABLE_NAME));
			tokenStart += name.length();
			tokensList.add(new CustomToken(tokenStart, tokenStart + 1, PERL_RBRACE));

			yypushback(tokenText.length()-sigil.length());
			return getSigilTokenType(sigil);
		}

		throw new RuntimeException("Unable to parse built-in variable: " + tokenText);
	}

	/**
	 * Returns token type for sigil
	 * @param sigil sigli text
	 * @return elementType
	 */
	public IElementType getSigilTokenType(String sigil)
	{
		if( "$#".equals(sigil))
			return PERL_SIGIL_SCALAR_INDEX;
		else if( "$".equals(sigil))
			return PERL_SIGIL_SCALAR;
		else if( "@".equals(sigil))
			return PERL_SIGIL_ARRAY;
		else if( "%".equals(sigil))
			return PERL_SIGIL_HASH;
		else if( "*".equals(sigil))
			return PERL_OPERATOR;
		else if( "&".equals(sigil))
			return PERL_OPERATOR;
		else
			throw new RuntimeException("Unknown sigil: " + sigil);
	}

}
