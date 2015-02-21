//Part of the Triangle rewrite
package miniJava.SyntacticAnalyzer;

public class Token {
	
	public TokenKind kind;
	public String spelling;
	public SourcePosition position;
	
	public Token(TokenKind k, String s, SourcePosition p){
		this.kind = k;
		this.spelling = s;
		this.position = p;
	}

}
