/*
    Copyright 2015 Alexandr Evstigneev

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
package com.perl5.lang.perl.lexer;

import com.perl5.lang.perl.util.PerlSubUtil;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

%%

// http://perldoc.perl.org/perlop.html#Gory-details-of-parsing-quoted-constructs
// http://perldoc.perl.org/perldata.html#Identifier-parsing

%class PerlLexerGenerated
%extends PerlBaseLexer
%abstract
%unicode
%public


%function perlAdvance
%type IElementType


%{

    protected int trenarCounter = 0;

	public abstract IElementType startRegexp();
	public abstract IElementType getIdentifierToken();
	public abstract IElementType captureString();
	public abstract boolean captureInterpolatedCode();

%}


/*
// Char classes
*/
NEW_LINE = \R
WHITE_SPACE     = [ \t\f]
ANY_SPACE = [ \t\f\n\r]
LINE_COMMENT = "#" .* \R
SPACES_OR_COMMENTS = ({ANY_SPACE}|{LINE_COMMENT})*
ESCAPED_WHITE_SPACE="\\"{WHITE_SPACE}
ESCAPED_CHARACTER = "\\"({ANY_SPACE}|"#")

// http://perldoc.perl.org/perldata.html#Identifier-parsing
PERL_XIDS = [\w && \p{XID_Start}_]
PERL_XIDC = [\w && \p{XID_Continue}]

IDENTIFIER = {PERL_XIDS} {PERL_XIDC}*
// following chunks may start with a number
IDENTIFIER_CONTINUE = {PERL_XIDC}+

FARROW = "=>"
BAREWORD_MINUS = "-" ? {IDENTIFIER}

// qualified identifer can't start with ', but variable can
QUALIFIED_IDENTIFIER = ("::"+ "'" ?) ? {IDENTIFIER} (("::"+ "'" ? | "::"* "'" ) {IDENTIFIER_CONTINUE} )*  "::" *
VARIABLE_QUALIFIED_IDENTIFIER = ("::"* "'" ?) ? {IDENTIFIER_CONTINUE} (("::"+ "'" ? | "::"* "'" ) {IDENTIFIER_CONTINUE} )*  "::" *

DQ_STRING = "\"" ([^\"]|"\\\\"|"\\\"" )* "\""?
SQ_STRING = "\'" ([^\']|"\\\\"|"\\\'" )* "\'"?
XQ_STRING = "\`" ([^\`]|"\\\\"|"\\\`" )* "\`"?

QUOTE_LIKE_SUFFIX= ("'" {QUALIFIED_IDENTIFIER} ? )?
CORE_PREFIX = "CORE::"?

PERL_VERSION_CHUNK = [0-9][0-9_]*
PERL_VERSION = "v"?{PERL_VERSION_CHUNK}("." {PERL_VERSION_CHUNK})*
// heading _ removed to avoid @_ parsing as sigil-number

// duplicated in Pod lexer
NUMBER_EXP = [eE][+-]?[0-9_]+
NUMBER_INT = [0-9][0-9_]*
NUMBER_HEX = "0"[xX][0-9a-fA-F_]+
NUMBER_BIN = "0"[bB][01_]+

SPECIAL_VARIABLE_NAME = [\"\'\[\]\`\\\!\%\&\(\)\+\,\-\.\/\;\<\=\>\|\~\?\:\*\^\@\_\$]
CAPPED_SINGLE_LETTER_VARIABLE_NAME = "^"[\]\[A-Z\^_?\\]
VARIABLE_NAME = {VARIABLE_QUALIFIED_IDENTIFIER} | "::" | {CAPPED_SINGLE_LETTER_VARIABLE_NAME} | {SPECIAL_VARIABLE_NAME}
CAPPED_BRACED_VARIABLE = {CAPPED_SINGLE_LETTER_VARIABLE_NAME}[\w_]*

AMBIGUOUS_SIGIL_SUFFIX = {SPACES_OR_COMMENTS} ("{"|[\"\'\[\]\`\\\!\(\)\+\,\-\.\/\;\<\=\>\|\~\?\:\^\_]|{CAPPED_SINGLE_LETTER_VARIABLE_NAME} )

// atm making the same, but seems unary are different
PERL_OPERATORS_FILETEST = "-" [rwxoRWXOezsfdlpSbctugkTBMAC]

HEREDOC_MARKER_DQ = [^\"\n\r]*
HEREDOC_MARKER_SQ = [^\'\n\r]*
HEREDOC_MARKER_XQ = [^\`\n\r]*
QUOTED_HEREDOC_MARKER = {WHITE_SPACE}*(\'{HEREDOC_MARKER_SQ}\'| \"{HEREDOC_MARKER_DQ}\" | \`{HEREDOC_MARKER_XQ}\`)
UNQUOTED_HEREDOC_MARKER = [a-zA-Z_]{IDENTIFIER}?

END_BLOCK = "__END__" [^]+
DATA_BLOCK = "__DATA__" [^] +

SUB_PROTOTYPE = [\s\[\$\@\%\&\*\]\;\+\\]+

POD_START = "="[\w].* {NEW_LINE} ?
POD_LINE = (.+ {NEW_LINE} ? | {NEW_LINE})
POD_END = "=cut" ({WHITE_SPACE} .*)?

BLOCK_NAMES = "BEGIN"|"UNITCHECK"|"CHECK"|"INIT"|"END"|"AUTOLOAD"|"DESTROY"
TAG_NAMES = "__FILE__"|"__LINE__"|"__PACKAGE__"|"__SUB__"

// auto-generated by handlesubs.pl
NAMED_UNARY_OPERATORS = "values"|"umask"|"srand"|"sleep"|"shift"|"setservent"|"setprotoent"|"setnetent"|"sethostent"|"reset"|"readline"|"rand"|"prototype"|"pop"|"localtime"|"keys"|"gmtime"|"getsockname"|"getpwuid"|"getpwnam"|"getprotobyname"|"getpgrp"|"getpeername"|"getnetbyname"|"gethostbyname"|"getgrnam"|"getgrgid"|"exit"|"exists"|"each"|"caller"
BARE_HANDLE_ACCEPTORS = "truncate"|"syswrite"|"sysseek"|"sysread"|"sysopen"|"stat"|"select"|"seekdir"|"seek"|"read"|"opendir"|"open"|"lstat"|"ioctl"|"flock"|"fcntl"|"binmode"
NAMED_UNARY_BARE_HANDLE_ACCEPTORS = "write"|"telldir"|"tell"|"rewinddir"|"readdir"|"getc"|"fileno"|"eof"|"closedir"|"close"|"chdir"
LIST_OPERATORS = "warn"|"waitpid"|"vec"|"utime"|"untie"|"unshift"|"tied"|"tie"|"system"|"syscall"|"symlink"|"substr"|"sprintf"|"splice"|"socketpair"|"socket"|"shutdown"|"shmwrite"|"shmread"|"shmget"|"shmctl"|"setsockopt"|"setpriority"|"setpgrp"|"send"|"semop"|"semget"|"semctl"|"scalar"|"rindex"|"rename"|"recv"|"push"|"pipe"|"pack"|"msgsnd"|"msgrcv"|"msgget"|"msgctl"|"lock"|"listen"|"link"|"kill"|"join"|"index"|"import"|"getsockopt"|"getservbyport"|"getservbyname"|"getprotobynumber"|"getpriority"|"getnetbyaddr"|"gethostbyaddr"|"formline"|"exec"|"dump"|"die"|"delete"|"dbmopen"|"dbmclose"|"crypt"|"connect"|"chown"|"chmod"|"bless"|"bind"|"atan2"|"accept"
NAMED_ARGUMENTLESS = "wantarray"|"wait"|"times"|"time"|"setpwent"|"setgrent"|"getservent"|"getpwent"|"getprotoent"|"getppid"|"getnetent"|"getlogin"|"gethostent"|"getgrent"|"fork"|"endservent"|"endpwent"|"endprotoent"|"endnetent"|"endhostent"|"endgrent"|"break"
IMPLICIT_USERS = "unpack"|"unlink"|"ucfirst"|"uc"|"study"|"stat"|"sqrt"|"split"|"sin"|"rmdir"|"reverse"|"ref"|"readpipe"|"readlink"|"quotemeta"|"pos"|"ord"|"oct"|"mkdir"|"lstat"|"log"|"length"|"lcfirst"|"lc"|"int"|"hex"|"glob"|"fc"|"exp"|"evalbytes"|"defined"|"cos"|"chroot"|"chr"|"chop"|"chomp"|"alarm"|"abs"

REGEX_COMMENT = "(?#"[^)]*")"

%state END_BLOCK
%state POD_STATE

%state QUOTE_LIKE_OPENER_Q, QUOTE_LIKE_OPENER_QQ, QUOTE_LIKE_OPENER_QX, QUOTE_LIKE_OPENER_QW
%state TRANS_OPENER, REGEX_OPENER

%state PACKAGE_ARGUMENTS, VERSION_OR_OPERAND, REQUIRE_ARGUMENTS, SUB_DECLARATION, BRACED_STRING, ATTRIBUTES

%xstate STRING_Q, STRING_QQ, STRING_QX, STRING_LIST
%xstate MATCH_REGEX, EXTENDED_MATCH_REGEX, REPLACEMENT_REGEX

// custom lexical states to avoid crossing with navie ones
%state LEX_CUSTOM1, LEX_CUSTOM2, LEX_CUSTOM3, LEX_CUSTOM4, LEX_CUSTOM5, LEX_CUSTOM6, LEX_CUSTOM7, LEX_CUSTOM8, LEX_CUSTOM9, LEX_CUSTOM10
%state PREPARSED_ITEMS
%xstate SUB_PROTOTYPE
%state SUB_ATTRIBUTES
%state HANDLE_WITH_ANGLE

// this is a hack for stupid tailing commas in calls
%state QUOTED_HEREDOC_OPENER, BARE_HEREDOC_OPENER, BACKREF_HEREDOC_OPENER
%state BLOCK_AS_VALUE

%state USE_VARS_STRING

%state VARIABLE_UNBRACED, VARIABLE_BRACED

%state AFTER_VARIABLE, AFTER_IDENTIFIER, AFTER_DEREFERENCE, AFTER_COMMA, AFTER_VALUE
%state AFTER_IDENTIFIER_WITH_REGEX
%state AFTER_POSSIBLE_SIGIL
%state AFTER_ASSIGN

%xstate LEX_LABEL
%state AFTER_IDENTIFIER_WITH_LABEL

%%


<QUOTED_HEREDOC_OPENER>{
	{DQ_STRING}	{
				yybegin(AFTER_VALUE);
				pushState();
				heredocQueue.push(new PerlHeredocQueueElement(HEREDOC_QQ, yytext().subSequence(1,yylength()-1)));
				pullback(0);
				yybegin(QUOTE_LIKE_OPENER_QQ);
				return captureString();
			}
	{SQ_STRING} {
				yybegin(AFTER_VALUE);
				pushState();
				heredocQueue.push(new PerlHeredocQueueElement(HEREDOC, yytext().subSequence(1,yylength()-1)));
				pullback(0);
				yybegin(QUOTE_LIKE_OPENER_Q);
				return captureString();
			}
	{XQ_STRING} {
				yybegin(AFTER_VALUE);
				pushState();
				heredocQueue.push(new PerlHeredocQueueElement(HEREDOC_QX, yytext().subSequence(1,yylength()-1)));
				pullback(0);
				yybegin(QUOTE_LIKE_OPENER_QX);
				return captureString();
			}
}

<BACKREF_HEREDOC_OPENER>{
	{UNQUOTED_HEREDOC_MARKER} {	yybegin(AFTER_VALUE);heredocQueue.push(new PerlHeredocQueueElement(HEREDOC, yytext()));return STRING_CONTENT;}
}

<BARE_HEREDOC_OPENER>{
	"\\" 					  {yybegin(BACKREF_HEREDOC_OPENER);return OPERATOR_REFERENCE;}
	{UNQUOTED_HEREDOC_MARKER} {yybegin(AFTER_VALUE); heredocQueue.push(new PerlHeredocQueueElement(HEREDOC_QQ, yytext()));return STRING_CONTENT;}
}

<STRING_QQ,STRING_QX,MATCH_REGEX,EXTENDED_MATCH_REGEX,REPLACEMENT_REGEX>
{
	"@" / [^] 	{return startUnbracedVariable(SIGIL_ARRAY);}
	"$#" / [^] 	{return startUnbracedVariable(SIGIL_SCALAR_INDEX);}
	"$" / [^] 	{return startUnbracedVariable(SIGIL_SCALAR); }
}

<EXTENDED_MATCH_REGEX>{
	{ESCAPED_CHARACTER}	{return REGEX_TOKEN;}
	{ANY_SPACE}+		{return TokenType.WHITE_SPACE;}
}

<MATCH_REGEX,EXTENDED_MATCH_REGEX,REPLACEMENT_REGEX>
{
	{REGEX_COMMENT}	{return COMMENT_LINE;}
	"\\"[\$\@]		{return REGEX_TOKEN;}
	[^]				{return REGEX_TOKEN;}
}

<STRING_QQ>{
	"\\"[\$\@]					{return STRING_CONTENT_QQ;}
	// chars with special treatments
	[^$\@\\]+					{return STRING_CONTENT_QQ;}
	[^]							{return STRING_CONTENT_QQ;}
}

<STRING_QX>{
	"\\"[\$\@]					{return STRING_CONTENT_XQ;}
	// chars with special treatments
	[^\$\@\\]+					{return STRING_CONTENT_XQ;}
	[^]							{return STRING_CONTENT_XQ;}
}

<STRING_LIST>
{
	[^\s\n\r]+				{return STRING_CONTENT;}
	{ESCAPED_WHITE_SPACE}	{return STRING_CONTENT;}
	{ANY_SPACE}+			{return TokenType.WHITE_SPACE;}
}

<STRING_Q>{
	[^]+					{return STRING_CONTENT;}
}

<POD_STATE> {
	{POD_END} / {NEW_LINE}	{yybegin(YYINITIAL);return COMMENT_BLOCK;}
	{POD_LINE}				{return COMMENT_BLOCK;}
}

^{POD_START} 	{yybegin(POD_STATE);return COMMENT_BLOCK;}
{NEW_LINE}   	{return TokenType.NEW_LINE_INDENT;}
{WHITE_SPACE}+  {return TokenType.WHITE_SPACE;}
{END_BLOCK}		{yybegin(END_BLOCK);return COMMENT_BLOCK;}
{DATA_BLOCK}	{yybegin(END_BLOCK);return COMMENT_BLOCK;}

<HANDLE_WITH_ANGLE>{
	{IDENTIFIER} 	{return HANDLE;}
	">"				{yybegin(AFTER_IDENTIFIER);return RIGHT_ANGLE;}
	[^]				{yybegin(YYINITIAL);yypushback(1);break;}
}

<END_BLOCK>{
	[^]+ 		{return COMMENT_BLOCK;}
}

<SUB_DECLARATION>{
	{QUALIFIED_IDENTIFIER} 		{return IDENTIFIER;}
	"(" / {SUB_PROTOTYPE}? ")"	{yybegin(SUB_PROTOTYPE);return LEFT_PAREN;}
	"(" 						{yybegin(YYINITIAL);return LEFT_PAREN;}
}

<SUB_DECLARATION,SUB_ATTRIBUTES,ATTRIBUTES>{
	"{"     					{yybegin(YYINITIAL);return getLeftBraceCode();}
}

<SUB_DECLARATION,SUB_ATTRIBUTES>{
	":"							{yybegin(ATTRIBUTES);return COLON;}
	[^]							{yypushback(1);yybegin(YYINITIAL);break;}
}

<SUB_PROTOTYPE>{
	{SUB_PROTOTYPE}			{return SUB_PROTOTYPE_TOKEN;}
	")"						{yybegin(SUB_ATTRIBUTES);return RIGHT_PAREN;}
}

<ATTRIBUTES>
{
	":"					{return COLON;}
	{IDENTIFIER} / "("	{pushState();yybegin(QUOTE_LIKE_OPENER_Q);return IDENTIFIER;}
	{IDENTIFIER}		{return IDENTIFIER;}
	[^]					{yypushback(1);yybegin(YYINITIAL);}
}

<VERSION_OR_OPERAND,REQUIRE_ARGUMENTS>{
	{PERL_VERSION}					{yybegin(YYINITIAL);return NUMBER_VERSION;}
	[^]								{yypushback(1);yybegin(YYINITIAL);}
}

<PACKAGE_ARGUMENTS>{
	"constant"						{yybegin(VERSION_OR_OPERAND);return PACKAGE_PRAGMA_CONSTANT;}
	"vars"							{yybegin(VERSION_OR_OPERAND);return PACKAGE_PRAGMA_VARS;}
	{PERL_VERSION}					{yybegin(YYINITIAL);return NUMBER_VERSION;}
	{QUALIFIED_IDENTIFIER}			{yybegin(VERSION_OR_OPERAND);return PACKAGE;}
}

<USE_VARS_STRING>
{
	"@" 	{return startUnbracedVariable(SIGIL_ARRAY);}
	"$#" 	{return startUnbracedVariable(SIGIL_SCALAR_INDEX);}
	"$" 	{return startUnbracedVariable(SIGIL_SCALAR); }
	"%"		{return startUnbracedVariable(SIGIL_HASH);}
	"*" 	{return startUnbracedVariable(SIGIL_GLOB);}
	"&"		{return startUnbracedVariable(SIGIL_CODE);}
}

<VARIABLE_UNBRACED>{
	// this is a subset of builtins, $;, $, for example, can't be dereferenced
	"$" / [\{\"\'\[\]\`\\\!\%\&\(\+\-\.\/\<\=\>\|\~\?\:\*\^\@\_\$\:\w_\d]		{return processUnbracedScalarSigil();}
	"{"											{return startBracedVariable();}
	{VARIABLE_NAME}								{return getUnbracedVariableNameToken();}
}

<VARIABLE_BRACED>{
	{VARIABLE_NAME}	/ {SPACES_OR_COMMENTS}"}"				{return getBracedVariableNameToken();}
	{CAPPED_BRACED_VARIABLE} / {SPACES_OR_COMMENTS}"}"		{return getBracedVariableNameToken();}
	[^]														{yypushback(1);yybegin(YYINITIAL);}
}

// exclusive
<LEX_LABEL>{
	{SPACES_OR_COMMENTS}":"	{return getLabelToken();}
	[^]						{return getNonLabelToken();}
}

////////////////////////// COMMON PART /////////////////////////////////////////////////////////////////////////////////

<BRACED_STRING>{
	{BAREWORD_MINUS}	{return STRING_CONTENT;}
	"}"					{return getRightBrace(AFTER_VARIABLE);}
}


<YYINITIAL,AFTER_COMMA,AFTER_VARIABLE,AFTER_VALUE,AFTER_IDENTIFIER,AFTER_IDENTIFIER_WITH_REGEX,AFTER_IDENTIFIER_WITH_LABEL>{
	{CORE_PREFIX}"if"	 	{ checkIfLabel(YYINITIAL, RESERVED_IF);}
	{CORE_PREFIX}"unless"	{ checkIfLabel(YYINITIAL, RESERVED_UNLESS);}
	{CORE_PREFIX}"while"	{ checkIfLabel(YYINITIAL, RESERVED_WHILE);}
	{CORE_PREFIX}"until"	{ checkIfLabel(YYINITIAL, RESERVED_UNTIL);}
	{CORE_PREFIX}"for"	 	{ checkIfLabel(YYINITIAL, RESERVED_FOR);}
	{CORE_PREFIX}"foreach"	{ checkIfLabel(YYINITIAL, RESERVED_FOREACH);}
	{CORE_PREFIX}"when"	 	{ checkIfLabel(YYINITIAL, RESERVED_WHEN);}
}

<AFTER_IDENTIFIER_WITH_LABEL>{
	{IDENTIFIER}	{yybegin(YYINITIAL);return IDENTIFIER;}
	[^]				{yypushback(1);yybegin(YYINITIAL);}
}

<YYINITIAL,AFTER_COMMA,AFTER_IDENTIFIER>{
	":"  	{yybegin(YYINITIAL);return COLON;}
}

<AFTER_VALUE,AFTER_VARIABLE,AFTER_IDENTIFIER,AFTER_IDENTIFIER_WITH_REGEX>{
	"*" 	{yybegin(AFTER_POSSIBLE_SIGIL);return OPERATOR_MUL;}
	"%" 	{yybegin(AFTER_POSSIBLE_SIGIL);return OPERATOR_MOD;}
	"&" 	{yybegin(AFTER_POSSIBLE_SIGIL);return OPERATOR_BITWISE_AND;}

	"<" 	{yybegin(YYINITIAL);return OPERATOR_LT_NUMERIC;}
	"&&" 	{yybegin(YYINITIAL);return OPERATOR_AND;}
	"**"	{yybegin(YYINITIAL);return OPERATOR_POW;}
	"%=" 	{yybegin(YYINITIAL);return OPERATOR_MOD_ASSIGN;}
	"*=" 	{yybegin(YYINITIAL);return OPERATOR_MUL_ASSIGN;}
	"&=" 	{yybegin(YYINITIAL);return OPERATOR_BITWISE_AND_ASSIGN;}
	"**=" 	{yybegin(YYINITIAL);return OPERATOR_POW_ASSIGN;}
	"&&="	{yybegin(YYINITIAL);return OPERATOR_AND_ASSIGN;}

	// ambiguous with double negation
	"~~"	{yybegin(YYINITIAL);return OPERATOR_SMARTMATCH;}

	// ambiguous with nyi
	"..." 	{yybegin(YYINITIAL);return OPERATOR_HELLIP;}
}

<AFTER_VALUE,AFTER_VARIABLE,AFTER_IDENTIFIER,AFTER_IDENTIFIER_WITH_REGEX>{
	"x" / [\d]*	{checkIfLabel(YYINITIAL, OPERATOR_X);}
	"and"		{checkIfLabel(YYINITIAL, OPERATOR_AND_LP);}
	"or"		{checkIfLabel(YYINITIAL, OPERATOR_OR_LP);}
	"xor"		{checkIfLabel(YYINITIAL, OPERATOR_XOR_LP);}
	"lt" / {QUOTE_LIKE_SUFFIX}		{checkIfLabel(YYINITIAL, OPERATOR_LT_STR);}
	"gt" / {QUOTE_LIKE_SUFFIX}		{checkIfLabel(YYINITIAL, OPERATOR_GT_STR);}
	"le" / {QUOTE_LIKE_SUFFIX}		{checkIfLabel(YYINITIAL, OPERATOR_LE_STR);}
	"ge" / {QUOTE_LIKE_SUFFIX}		{checkIfLabel(YYINITIAL, OPERATOR_GE_STR);}
	"eq" / {QUOTE_LIKE_SUFFIX} 		{checkIfLabel(YYINITIAL, OPERATOR_EQ_STR);}
	"ne" / {QUOTE_LIKE_SUFFIX}		{checkIfLabel(YYINITIAL, OPERATOR_NE_STR);}
	"cmp" / {QUOTE_LIKE_SUFFIX} 	{checkIfLabel(YYINITIAL, OPERATOR_CMP_STR);}
}

<YYINITIAL>{
	"..." 	{yybegin(YYINITIAL);return OPERATOR_NYI;}
}

<AFTER_DEREFERENCE>{
	"$*"	{yybegin(AFTER_VALUE);return DEREF_SCALAR;}
	"$#*"	{yybegin(AFTER_VALUE);return DEREF_SCALAR_INDEX;}
	"@*"	{yybegin(AFTER_VALUE);return DEREF_ARRAY;}
	"%*"	{yybegin(AFTER_VALUE);return DEREF_HASH;}
	"**"	{yybegin(AFTER_VALUE);return DEREF_GLOB;}
	"&*"	{yybegin(AFTER_VALUE);return DEREF_CODE;}

	"@"		{yybegin(AFTER_VARIABLE); return SIGIL_ARRAY;}
	"%"		{yybegin(AFTER_VARIABLE); return SIGIL_HASH;}
	"*"		{yybegin(AFTER_VARIABLE); return SIGIL_GLOB;}
	"&"		{yybegin(AFTER_VARIABLE); return SIGIL_CODE;}
}

// normal variable, unambiguous
<YYINITIAL,BLOCK_AS_VALUE,AFTER_DEREFERENCE,AFTER_COMMA,AFTER_IDENTIFIER,AFTER_IDENTIFIER_WITH_REGEX,AFTER_VARIABLE,AFTER_POSSIBLE_SIGIL,AFTER_VALUE>{
	"@" 	{return startUnbracedVariable(AFTER_VARIABLE, SIGIL_ARRAY);}
	"$#" 	{return startUnbracedVariable(AFTER_VARIABLE, SIGIL_SCALAR_INDEX);}
	"$" 	{return startUnbracedVariable(AFTER_VARIABLE, SIGIL_SCALAR); }
}

// ambiguous variable in unambiguous situation
<YYINITIAL,BLOCK_AS_VALUE,AFTER_COMMA,AFTER_POSSIBLE_SIGIL>{
	"%"		{return startUnbracedVariable(AFTER_VARIABLE, SIGIL_HASH);}
	"*"		{return startUnbracedVariable(AFTER_VARIABLE, SIGIL_GLOB);}
	"&"		{return startUnbracedVariable(AFTER_VARIABLE, SIGIL_CODE);}
}

// ambiguous variable in ambiguous situation
<AFTER_IDENTIFIER,AFTER_IDENTIFIER_WITH_REGEX>
{
	"%" / {AMBIGUOUS_SIGIL_SUFFIX}	{return startUnbracedVariable(AFTER_VARIABLE, SIGIL_HASH);}
	"*"	/ {AMBIGUOUS_SIGIL_SUFFIX}	{return startUnbracedVariable(AFTER_VARIABLE, SIGIL_GLOB);}
	"&"	/ {AMBIGUOUS_SIGIL_SUFFIX}	{return startUnbracedVariable(AFTER_VARIABLE, SIGIL_CODE);}
}

// postfix operators
<AFTER_VARIABLE,AFTER_VALUE>{
	"++" 	{yybegin(AFTER_VALUE);return OPERATOR_PLUS_PLUS;}
	"--" 	{yybegin(AFTER_VALUE);return OPERATOR_MINUS_MINUS;}
}

// prefix operators
<YYINITIAL,BLOCK_AS_VALUE,AFTER_COMMA>{
	"++" 	{yybegin(YYINITIAL);return OPERATOR_PLUS_PLUS;}
	"--" 	{yybegin(YYINITIAL);return OPERATOR_MINUS_MINUS;}
}


// operands and starters
<YYINITIAL,BLOCK_AS_VALUE,AFTER_COMMA>{
	"<" / {IDENTIFIER}">"  		{yybegin(HANDLE_WITH_ANGLE);return LEFT_ANGLE;}
	"<"							{yybegin(AFTER_VALUE);pushState();yypushback(1);yybegin(QUOTE_LIKE_OPENER_QQ);return captureString();}
}

<AFTER_VALUE,AFTER_VARIABLE,AFTER_IDENTIFIER>{
	"/"   	{yybegin(YYINITIAL);return OPERATOR_DIV;}
	"//" 	{yybegin(YYINITIAL);return OPERATOR_OR_DEFINED;}
	"/=" 	{yybegin(YYINITIAL);return OPERATOR_DIV_ASSIGN;}
	"//=" 	{yybegin(YYINITIAL);return OPERATOR_OR_DEFINED_ASSIGN;}
}

<YYINITIAL,BLOCK_AS_VALUE,AFTER_COMMA,AFTER_IDENTIFIER_WITH_REGEX>{
	"/"   						{yybegin(AFTER_VALUE);return startRegexp();}
}

// known identifiers
<YYINITIAL,BLOCK_AS_VALUE,AFTER_COMMA,AFTER_IDENTIFIER,AFTER_IDENTIFIER_WITH_REGEX,AFTER_VARIABLE>{
	"split"						{checkIfLabel(AFTER_IDENTIFIER_WITH_REGEX, IDENTIFIER);}
	{BARE_HANDLE_ACCEPTORS}				{yybegin(YYINITIAL);return IDENTIFIER;}
	{NAMED_UNARY_BARE_HANDLE_ACCEPTORS}	{checkIfLabel(YYINITIAL, OPERATOR_NAMED_UNARY);}
	{NAMED_UNARY_OPERATORS}				{checkIfLabel(AFTER_IDENTIFIER, OPERATOR_NAMED_UNARY);}

	{IMPLICIT_USERS}					{yybegin(AFTER_IDENTIFIER);return IDENTIFIER;}
	{PERL_OPERATORS_FILETEST} / [^a-zA-Z0-9_] 	{yybegin(AFTER_IDENTIFIER);return OPERATOR_FILETEST;}

	{NAMED_ARGUMENTLESS}				{yybegin(AFTER_VALUE);return IDENTIFIER;}	// fixme we can return special token here to help parser
	{LIST_OPERATORS}					{yybegin(YYINITIAL);return IDENTIFIER;}

	"not"						{checkIfLabel(YYINITIAL, OPERATOR_NOT_LP);}

	{CORE_PREFIX}"my"			{checkIfLabel(YYINITIAL, RESERVED_MY);}
	{CORE_PREFIX}"our"			{checkIfLabel(YYINITIAL, RESERVED_OUR);}
	{CORE_PREFIX}"local"		{checkIfLabel(YYINITIAL, RESERVED_LOCAL);}
	{CORE_PREFIX}"state"		{checkIfLabel(YYINITIAL, RESERVED_STATE);}

	{CORE_PREFIX}"elsif"	 	{checkIfLabel(YYINITIAL, RESERVED_ELSIF);}
	{CORE_PREFIX}"else"	 		{checkIfLabel(YYINITIAL, RESERVED_ELSE);}
	{CORE_PREFIX}"given"	 	{checkIfLabel(YYINITIAL, RESERVED_GIVEN);}
	{CORE_PREFIX}"default"	 	{checkIfLabel(YYINITIAL, RESERVED_DEFAULT);}
	{CORE_PREFIX}"continue"	 	{checkIfLabel(YYINITIAL, RESERVED_CONTINUE);}

	{CORE_PREFIX}"format"	 							{checkIfLabel(AFTER_IDENTIFIER, RESERVED_FORMAT);}
	// distinct from label pattern
	{CORE_PREFIX}"sub" / ({SPACES_OR_COMMENTS}":")?		{yybegin(SUB_DECLARATION);return  RESERVED_SUB;}

	{CORE_PREFIX}"package"	 	{checkIfLabel(PACKAGE_ARGUMENTS, RESERVED_PACKAGE);}
	{CORE_PREFIX}"use"	 		{checkIfLabel(PACKAGE_ARGUMENTS, RESERVED_USE);}
	{CORE_PREFIX}"no"	 		{checkIfLabel(PACKAGE_ARGUMENTS, RESERVED_NO);}
	{CORE_PREFIX}"require"	 	{checkIfLabel(REQUIRE_ARGUMENTS, RESERVED_REQUIRE);}

	{CORE_PREFIX}"undef"		{checkIfLabel(AFTER_IDENTIFIER, RESERVED_UNDEF);}

	"switch"					{checkIfLabel(YYINITIAL, RESERVED_SWITCH);}
	"case"						{checkIfLabel(AFTER_IDENTIFIER_WITH_REGEX, RESERVED_CASE);}

	"method"					{checkIfLabel(YYINITIAL, RESERVED_METHOD);}
	"func"						{checkIfLabel(YYINITIAL, RESERVED_FUNC);}

	"try"						{checkIfLabel(YYINITIAL, RESERVED_TRY);}
	"catch"						{checkIfLabel(YYINITIAL, RESERVED_CATCH);}
	"finally"					{checkIfLabel(YYINITIAL, RESERVED_FINALLY);}

	"with"					{checkIfLabel(YYINITIAL, RESERVED_WITH);}
	"extends"				{checkIfLabel(YYINITIAL, RESERVED_EXTENDS);}
	"meta"					{checkIfLabel(YYINITIAL, RESERVED_META);}
	"override"				{checkIfLabel(YYINITIAL, RESERVED_OVERRIDE);}
	"around"				{checkIfLabel(YYINITIAL, RESERVED_AROUND);}
	"augment"				{checkIfLabel(YYINITIAL, RESERVED_AUGMENT);}
	"after"					{checkIfLabel(YYINITIAL, RESERVED_AFTER);}
	"before"				{checkIfLabel(YYINITIAL, RESERVED_BEFORE);}
	"has"					{checkIfLabel(YYINITIAL, RESERVED_HAS);}

	// special treatment?
	{CORE_PREFIX}"print"	 	{checkIfLabel(AFTER_IDENTIFIER, RESERVED_PRINT);}
	{CORE_PREFIX}"printf"	 	{checkIfLabel(AFTER_IDENTIFIER, RESERVED_PRINTF);}
	{CORE_PREFIX}"say"	 		{checkIfLabel(AFTER_IDENTIFIER, RESERVED_SAY);}

	{CORE_PREFIX}"grep"	 { checkIfLabel(AFTER_IDENTIFIER_WITH_REGEX, RESERVED_GREP);}
	{CORE_PREFIX}"map"	 { checkIfLabel(YYINITIAL, RESERVED_MAP);}
	{CORE_PREFIX}"sort"	 { checkIfLabel(YYINITIAL, RESERVED_SORT);}

	{CORE_PREFIX}"do"	 { checkIfLabel(BLOCK_AS_VALUE, RESERVED_DO);}
	{CORE_PREFIX}"eval"	 { checkIfLabel(BLOCK_AS_VALUE, RESERVED_EVAL);}

	{CORE_PREFIX}"goto"	 { checkIfLabel(AFTER_IDENTIFIER_WITH_LABEL, RESERVED_GOTO);}
	{CORE_PREFIX}"redo"	 { checkIfLabel(AFTER_IDENTIFIER_WITH_LABEL, RESERVED_REDO);}
	{CORE_PREFIX}"next"	 { checkIfLabel(AFTER_IDENTIFIER_WITH_LABEL, RESERVED_NEXT);}
	{CORE_PREFIX}"last"	 { checkIfLabel(AFTER_IDENTIFIER_WITH_LABEL, RESERVED_LAST);}

	{CORE_PREFIX}"return"	 { checkIfLabel(YYINITIAL, RESERVED_RETURN);}

	{BLOCK_NAMES} / {SPACES_OR_COMMENTS}"{"		{yybegin(YYINITIAL);return BLOCK_NAME;}
	{TAG_NAMES}									{checkIfLabel(AFTER_VALUE, TAG);}

	{CORE_PREFIX}"y"  / {QUOTE_LIKE_SUFFIX} {yybegin(AFTER_VALUE);return RESERVED_Y;}
	{CORE_PREFIX}"tr" / {QUOTE_LIKE_SUFFIX} {yybegin(AFTER_VALUE);return RESERVED_TR;}

	{CORE_PREFIX}"qr" / {QUOTE_LIKE_SUFFIX} {yybegin(AFTER_VALUE);return RESERVED_QR;}
	{CORE_PREFIX}"qw" / {QUOTE_LIKE_SUFFIX} {yybegin(AFTER_VALUE);return RESERVED_QW;}
	{CORE_PREFIX}"qq" / {QUOTE_LIKE_SUFFIX} {yybegin(AFTER_VALUE);return RESERVED_QQ;}
	{CORE_PREFIX}"qx" / {QUOTE_LIKE_SUFFIX} {yybegin(AFTER_VALUE);return RESERVED_QX;}
	{CORE_PREFIX}"q" / {QUOTE_LIKE_SUFFIX}  {yybegin(AFTER_VALUE);return RESERVED_Q;}

	{CORE_PREFIX}"m" / {QUOTE_LIKE_SUFFIX}  {return RESERVED_M;}
	{CORE_PREFIX}"s" / {QUOTE_LIKE_SUFFIX}  {return RESERVED_S;}
}

<AFTER_DEREFERENCE>{
	"follow_best_practice"		{yybegin(AFTER_VALUE);return RESERVED_FOLLOW_BEST_PRACTICE;}
	"mk_accessors"				{yybegin(AFTER_VALUE);return RESERVED_MK_ACCESSORS;}
	"mk_ro_accessors"			{yybegin(AFTER_VALUE);return RESERVED_MK_RO_ACCESSORS;}
	"mk_wo_accessors"			{yybegin(AFTER_VALUE);return RESERVED_MK_WO_ACCESSORS;}
	"helper"					{yybegin(AFTER_VALUE);return MOJO_HELPER_METHOD;}
	{QUALIFIED_IDENTIFIER} 		{yybegin(AFTER_VALUE);return IDENTIFIER;}
}

// following is for require
<REQUIRE_ARGUMENTS>{
	{QUALIFIED_IDENTIFIER} / "("	{yybegin(YYINITIAL);return IDENTIFIER;}
	{QUALIFIED_IDENTIFIER}			{yybegin(AFTER_IDENTIFIER);return PACKAGE;}
}

// block start after eval/do
<BLOCK_AS_VALUE>{
	"{"		{return startBracedBlock(AFTER_VALUE);}
}

// handles hash indexes after variable and/or ->
<AFTER_DEREFERENCE,AFTER_VARIABLE>{
	"["															{return startBracketedBlock(AFTER_VARIABLE);}
	"{"															{return startBracedBlock(AFTER_VARIABLE);}
	"{" / {WHITE_SPACE}* {BAREWORD_MINUS} {WHITE_SPACE}* "}"	{yybegin(BRACED_STRING);return getLeftBrace();}
}

<AFTER_DEREFERENCE> "("     	{yybegin(YYINITIAL);return LEFT_PAREN;}

<AFTER_VALUE,AFTER_VARIABLE> "." / {NUMBER_INT} {return OPERATOR_CONCAT;}

// always checked except explicit states
<YYINITIAL,BLOCK_AS_VALUE,AFTER_COMMA,AFTER_VARIABLE,AFTER_VALUE,AFTER_IDENTIFIER,AFTER_IDENTIFIER_WITH_REGEX,AFTER_POSSIBLE_SIGIL>{
	{FARROW} 	{yybegin(AFTER_COMMA);return FAT_COMMA;}
	"," 		{yybegin(AFTER_COMMA);return COMMA;}
	";"     	{yybegin(YYINITIAL);return SEMICOLON;}
	"=" 		{yybegin(BLOCK_AS_VALUE);return OPERATOR_ASSIGN;}
	"->" 		{yybegin(AFTER_DEREFERENCE); return OPERATOR_DEREFERENCE;}
	"["     	{yybegin(YYINITIAL);return getLeftBracket();}
	"("     	{yybegin(YYINITIAL);return LEFT_PAREN;}
	"{"     	{yybegin(YYINITIAL);return getLeftBrace();}
	"}"     	{return getRightBrace(YYINITIAL);}
	"]"     	{return getRightBracket(AFTER_VALUE);}
	")"     	{yybegin(AFTER_VALUE);return RIGHT_PAREN;}
	":"			{yybegin(YYINITIAL);return COLON;}

	"||" 	{yybegin(YYINITIAL);return OPERATOR_OR;}
	">="	{yybegin(YYINITIAL);return OPERATOR_GE_NUMERIC;}
	"<="	{yybegin(YYINITIAL);return OPERATOR_LE_NUMERIC;}
	"=="	{yybegin(YYINITIAL);return OPERATOR_EQ_NUMERIC;}
	"!="	{yybegin(YYINITIAL);return OPERATOR_NE_NUMERIC;}
	"+="	{yybegin(YYINITIAL);return OPERATOR_PLUS_ASSIGN;}
	"-="	{yybegin(YYINITIAL);return OPERATOR_MINUS_ASSIGN;}
	".=" 	{yybegin(YYINITIAL);return OPERATOR_CONCAT_ASSIGN;}
	"x=" 	{yybegin(YYINITIAL);return OPERATOR_X_ASSIGN;}
	"|=" 	{yybegin(YYINITIAL);return OPERATOR_BITWISE_OR_ASSIGN;}
	"^=" 	{yybegin(YYINITIAL);return OPERATOR_BITWISE_XOR_ASSIGN;}
	"<<=" 	{yybegin(YYINITIAL);return OPERATOR_SHIFT_LEFT_ASSIGN;}
	">>=" 	{yybegin(YYINITIAL);return OPERATOR_SHIFT_RIGHT_ASSIGN;}
	"||=" 	{yybegin(YYINITIAL);return OPERATOR_OR_ASSIGN;}
	"<=>" 	{yybegin(YYINITIAL);return OPERATOR_CMP_NUMERIC;}
	">" 	{yybegin(YYINITIAL);return OPERATOR_GT_NUMERIC;}

	"=~" 	{yybegin(YYINITIAL);return OPERATOR_RE;}
	"!~" 	{yybegin(YYINITIAL);return OPERATOR_NOT_RE;}

	"<<" 								{yybegin(YYINITIAL);return OPERATOR_SHIFT_LEFT;}
	"<<" / {QUALIFIED_IDENTIFIER}"(" 	{yybegin(YYINITIAL);return OPERATOR_SHIFT_LEFT;}
	">>" 	{yybegin(YYINITIAL);return OPERATOR_SHIFT_RIGHT;}

	"?"  	{yybegin(YYINITIAL);return QUESTION;}
	"|" 	{yybegin(YYINITIAL);return OPERATOR_BITWISE_OR;}
	"^" 	{yybegin(YYINITIAL);return OPERATOR_BITWISE_XOR;}

	"+" 		{yybegin(YYINITIAL);return OPERATOR_PLUS;}
	"-" 		{yybegin(YYINITIAL);return OPERATOR_MINUS;}
	"!" 		{yybegin(YYINITIAL);return OPERATOR_NOT;}
	"~" 		{yybegin(YYINITIAL);return OPERATOR_BITWISE_NOT;}
	"\\" 		{yybegin(YYINITIAL);return OPERATOR_REFERENCE;}

	".." 	{yybegin(YYINITIAL);return OPERATOR_FLIP_FLOP;}
	"." 	{yybegin(YYINITIAL);return OPERATOR_CONCAT;}

	"<<" / {QUOTED_HEREDOC_MARKER}   		{yybegin(QUOTED_HEREDOC_OPENER);return OPERATOR_HEREDOC;}
	"<<" / "\\"{UNQUOTED_HEREDOC_MARKER} 	{yybegin(BARE_HEREDOC_OPENER);return OPERATOR_HEREDOC;}
	"<<" / {UNQUOTED_HEREDOC_MARKER}  		{yybegin(BARE_HEREDOC_OPENER);return OPERATOR_HEREDOC;}

	{DQ_STRING}	{yybegin(AFTER_VALUE);pushState();pullback(0);yybegin(QUOTE_LIKE_OPENER_QQ);return captureString();}
	{SQ_STRING} {yybegin(AFTER_VALUE);pushState();pullback(0);yybegin(QUOTE_LIKE_OPENER_Q);return captureString();}
	{XQ_STRING} {yybegin(AFTER_VALUE);pushState();pullback(0);yybegin(QUOTE_LIKE_OPENER_QX);return captureString();}

	{BAREWORD_MINUS} / {SPACES_OR_COMMENTS}* {FARROW}	{yybegin(AFTER_VALUE);return STRING_CONTENT;}

	{NUMBER_BIN}									 {yybegin(AFTER_VALUE);return NUMBER;}
	{NUMBER_HEX}									 {yybegin(AFTER_VALUE);return NUMBER;}
	"." {NUMBER_INT} {NUMBER_EXP}?	 				 {yybegin(AFTER_VALUE);return NUMBER;}
	{NUMBER_INT} ("." {NUMBER_INT}? )? {NUMBER_EXP}? {yybegin(AFTER_VALUE);return NUMBER;}
	{PERL_VERSION}  		{yybegin(AFTER_VALUE);return NUMBER_VERSION;}

	{QUALIFIED_IDENTIFIER} 	{yybegin(AFTER_IDENTIFIER);return getIdentifierToken();}
}


/* error fallback [^] */
[^]    { return TokenType.BAD_CHARACTER; }
