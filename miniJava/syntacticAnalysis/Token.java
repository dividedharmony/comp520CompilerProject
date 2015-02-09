package miniJava.syntacticAnalysis;


/**
 * 
 * Tokens have a kind and a spelling
 *
 */

public class Token {
	
	public TokenKind kind;
	public String spelling;
	
	public Token(TokenKind k, String s){
		this.kind = k;
		this.spelling = s;
	}

}
