//Part of the PA2 rewrite

package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

public class Scanner {
	
	private SourceFile sourceFile;
	private ErrorReporter errorReporter;
	
	private char currentChar;
	private char nextChar;
	private StringBuilder currentSpelling;
	
	public Scanner(SourceFile source, ErrorReporter er) {
		this.errorReporter = er;
		sourceFile = source;
	    readNextChar();
	    readChar();
	    
	  }
	
	public Token scan(){
		//ignore whitespace
		while((currentChar == ' ') || (currentChar == '\n')
				|| (currentChar == '\t') || (currentChar == '\r') ){
			skipIt();
		}
		SourcePosition pos = new SourcePosition();
		pos.start = sourceFile.getCurrentLine();
		currentSpelling = new StringBuilder();
		
		TokenKind kind = scanToken();
		pos.finish = sourceFile.getCurrentLine();
		
		return new Token(kind, currentSpelling.toString(), pos);
	}
	
	public TokenKind scanToken(){
		switch(currentChar){
		case '\u0003':
			return TokenKind.EOT;
		case ';':
			takeIt();
			return TokenKind.SEMICOLON;
		case '.':
			takeIt();
			return TokenKind.PERIOD;
		case ',':
			takeIt();
			return TokenKind.COMMA;
		case '{':
			takeIt();
			return TokenKind.LEFTBRACE;
		case '}':
			takeIt();
			return TokenKind.RIGHTBRACE;
		case '[':
			takeIt();
			return TokenKind.LEFTBRACK;
		case ']':
			takeIt();
			return TokenKind.RIGHTBRACK;
		case '(':
			takeIt();
			return TokenKind.LEFTPARA;
		case ')':
			takeIt();
			return TokenKind.RIGHTPARA;
		case '-':
			if(nextChar == '-'){
				takeIt();
				scanError("The '--' operator is not allowed in miniJava");
				return TokenKind.NEGATION;
			}else{
				takeIt();
				return TokenKind.NEGATION;
			}
		case '+':
		case '*':
			takeIt();
			return TokenKind.BI_ARITH;
		case '<':
		case '>':
			if(nextChar == '='){
				takeIt();
				takeIt();
				return TokenKind.RELATIONOP;
			}else{
				takeIt();
				return TokenKind.RELATIONOP;
			}
		case '=':
			if(nextChar == '='){
				takeIt();
				takeIt();
				return TokenKind.RELATIONOP;
			}else{
				takeIt();
				return TokenKind.ASSIGNOP;
			}
		case '!':
			if(nextChar == '='){
				takeIt();
				takeIt();
				return TokenKind.RELATIONOP;
			}else{
				takeIt();
				return TokenKind.UNILOGICAL;
			}
		case '&':
			if(nextChar != '&'){
				scanError("Expected character '&' not found");
			}else{
				takeIt();
				takeIt();
				return TokenKind.BI_LOGICAL;
			}
		case '|':
			if(nextChar != '|'){
				scanError("Expected character '|' not found");
				return TokenKind.ERROR;
			}else{
				takeIt();
				takeIt();
				return TokenKind.BI_LOGICAL;
			}
		case '/':
			if(nextChar == '/'){
				while((currentChar != '\n') && (currentChar != '\r')){
					if(currentChar == '\u0003'){
						return TokenKind.EOT;
					}
					takeIt();
				}
				takeIt();//here: '\n' is taken
				return TokenKind.COMMENT;
			}else if(nextChar == '*'){
				
				takeIt();// here: '/' is taken
				takeIt();// here: '*' is taken
				/*
				 * this is what a block comment looks like
				 * newline doesn't end a block comment but it does increment
				 * the currentLine count
				 */
				while(true){
					if(currentChar == '*'){
						if(nextChar == '/'){
							takeIt();
							takeIt();
							return TokenKind.COMMENT;
						}
					}else if(currentChar == '\u0003'){
						scanError("Unterminated block comment");
						return TokenKind.ERROR;
					}
					
					takeIt();
				}
			}else{
				takeIt();
				return TokenKind.BI_ARITH;
			}
		case '0': case '1': case '2': case '3': case '4': case '5':
		case '6': case '7': case '8': case '9':
			while(isDigit(currentChar)){
				takeIt();
			}
			return TokenKind.NUM;
		default:
			if(Character.isLetter(currentChar)){
				if(currentChar == 'b'){
					if(matchesKeyword("boolean")){
						return TokenKind.BOOLEANKEY;
					}
				}else if(currentChar == 'c'){
					if(matchesKeyword("class")){
						return TokenKind.CLASSKEY;
					}
				}else if(currentChar == 'e'){
					if(matchesKeyword("else")){
						return TokenKind.ELSEKEY;
					}
				}else if(currentChar == 'f'){
					if(matchesKeyword("false")){
						return TokenKind.FALSEKEY;
					}
				}else if(currentChar == 'i'){
					if(nextChar == 'f'){
						if(matchesKeyword("if")){
							return TokenKind.IFKEY;
						}
					}else if(nextChar == 'n'){
						if(matchesKeyword("int")){
							return TokenKind.INTKEY;
						}
					}
				}else if(currentChar == 'n'){
					if(matchesKeyword("new")){
						return TokenKind.NEWKEY;
					}
				}else if(currentChar == 'p'){
					if(nextChar == 'r'){
						if(matchesKeyword("private")){
							return TokenKind.PRIVATEKEY;
						}
					}else if(nextChar == 'u'){
						if(matchesKeyword("public")){
							return TokenKind.PUBLICKEY;
						}
					}
				}else if(currentChar == 'r'){
					if(matchesKeyword("return")){
						return TokenKind.RETURNKEY;
					}
				}else if(currentChar == 's'){
					if(matchesKeyword("static")){
						return TokenKind.STATICKEY;
					}
				}else if(currentChar == 't'){
					if(nextChar == 'h'){
						if(matchesKeyword("this")){
							return TokenKind.THISKEY;
						}
					}else if(nextChar == 'r'){
						if(matchesKeyword("true")){
							return TokenKind.TRUEKEY;
						}
					}
				}else if(currentChar == 'v'){
					if(matchesKeyword("void")){
						return TokenKind.VOIDKEY;
					}
				}else if(currentChar == 'w'){
					if(matchesKeyword("while")){
						return TokenKind.WHILEKEY;
					}
				}
				//identifier that is not a keyword
				while(Character.isLetterOrDigit(currentChar) || currentChar == '_'){
					takeIt();
				}
				return TokenKind.ID;
			}
			scanError("Unrecognized character '" + currentChar + "' in input");
			return TokenKind.ERROR;
		}
	}//end scanToken
	
	/////// HELPER FUNCTIONS ////////////
	
	public void takeIt(){
		currentSpelling.append(currentChar);
		advanceOneChar();
	}
	
	public void skipIt(){
		advanceOneChar();
	}
	
	public boolean isDigit(char c){
		return ((c >= '0') && (c <= '9'));
	}
	
	private void scanError(String e){
		this.errorReporter.reportError(e);
	}
	
	/**
	 * advance to next char in inputstream
	 * detect end of file
	 */
	
	private void advanceOneChar(){
		if((currentChar != '\u0003') && (nextChar != '\u0003')){
			readChar();
		}else if(nextChar == '\u0003'){
			currentChar = nextChar;
		}else{
			currentChar = '\u0003';
		}
	}
	
	private void readChar(){
		currentChar = nextChar;
		readNextChar();
		
	}
	
	private void readNextChar(){
		nextChar = sourceFile.getSource();
	}//end readNextChar
	
	/**
	 * doesNotExtend checks to see if an identifier has more letters or underscores
	 */
	private boolean doesNotExtend(){
		boolean doesNotExtend = false;
		if((!Character.isLetterOrDigit(currentChar)) && (currentChar != '_') ){
			doesNotExtend = true;
		}
		return doesNotExtend;
	}
	private boolean matchesKeyword(String m){
		boolean matches = true;
		for(int i=0; i<m.length(); i++){
			if(m.charAt(i) !=  currentChar){
				matches = false;
				break;
			}else{
				takeIt();
			}
		}
		if(!doesNotExtend()){
			matches = false;
		}
		return matches;
	}
}
