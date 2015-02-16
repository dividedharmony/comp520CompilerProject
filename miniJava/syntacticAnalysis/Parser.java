package miniJava.syntacticAnalysis;
import miniJava.ErrorReporter;


/**
 * Parser
 * @author David Harmon
 * COMP 520 Compiler project 
 * PA1
 */

public class Parser {
	
	
	private Scanner scanner;
	private ErrorReporter errorReporter;
	private Token token;
	private boolean trace = true;
	
	public Parser(Scanner s, ErrorReporter r){
		this.scanner = s;
		this.errorReporter = r;
	}
	
	/**
	 * SyntaxError is used to unwind parse stack when parse fails
	 *
	 */
	
	class SyntaxError extends Error{
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * begin parse
	 */
	
	public void parse(){
		token = scanner.scan();
		while(token.kind == TokenKind.COMMENT){
			token = scanner.scan();
		}
		try{
			parseProgram();
			
		}catch(SyntaxError e){}
		
	}
	
	public void parseProgram(){
		//Program ::= (ClassDeclaration)* eot
		while(token.kind == TokenKind.CLASSKEY){
			parseClassDeclaration();
		}
		accept(TokenKind.EOT);
	}//end parseProgram()
	
	public void parseClassDeclaration(){
		//ClassDeclaration ::= class id { (FieldDeclaration | MethodDeclaration)* }
		accept(TokenKind.CLASSKEY);
		accept(TokenKind.ID);
		accept(TokenKind.LEFTBRACE);
		while(token.kind != TokenKind.RIGHTBRACE){
			parseDeclarators();
		}
		accept(TokenKind.RIGHTBRACE);
		
	}//end parseClassDeclaration
	
	public void parseMethodDeclaration(){
		//Declarators id (ParameterList) {Statement* (return Expression;)? }
		accept(TokenKind.LEFTPARA);
		parseParameterList();
		accept(TokenKind.RIGHTPARA);
		accept(TokenKind.LEFTBRACE);
		while((token.kind != TokenKind.RETURNKEY) && (token.kind != TokenKind.RIGHTBRACE)){
			parseStatement();
		}
		if(token.kind == TokenKind.RETURNKEY){
			acceptIt();
			parseExpression();
			accept(TokenKind.SEMICOLON);
		}
		accept(TokenKind.RIGHTBRACE);
		
	}
	
	public void parseDeclarators(){
		//Declarators ::= (public | private)? static? (Type | void)
		//FieldDeclaration ::= Declarators id;
		if(token.kind == TokenKind.PUBLICKEY){
			acceptIt();
		}else if(token.kind == TokenKind.PRIVATEKEY){
			acceptIt();
		}
		if(token.kind == TokenKind.STATICKEY){
			acceptIt();
		}
		if(token.kind == TokenKind.VOIDKEY){
			acceptIt();
			accept(TokenKind.ID); //method name
			parseMethodDeclaration();
		}else{
			parseType();
			accept(TokenKind.ID); //method or field name
			if(token.kind == TokenKind.SEMICOLON){
				acceptIt();
			}else{
				parseMethodDeclaration();
			}
		}
	}//end parseDeclarators
	
	public void parseType(){
		//Type ::= PrimType | id | ArrType
		//PrimType ::= int | boolean
		//ArrType ::= (int | id) []
		//Type ::=* boolean | (int | id) ([])?
		if(token.kind == TokenKind.BOOLEANKEY){
			acceptIt();
		}else if(token.kind == TokenKind.INTKEY){
			acceptIt();
			if(token.kind == TokenKind.LEFTBRACK){//case: integer array
				acceptIt();
				accept(TokenKind.RIGHTBRACK);
			}
		}else if(token.kind == TokenKind.ID){
			acceptIt();
			if(token.kind == TokenKind.LEFTBRACK){//case: class objects array
				acceptIt();
				accept(TokenKind.RIGHTBRACK);
			}
		}else{
			parseError("Type missing.");
		}
	}
	
	public void parseParameterList(){
		//ParameterList ::= Type id (, Type id)*
		while(token.kind != TokenKind.RIGHTPARA){
			parseType();
			accept(TokenKind.ID);
			if(token.kind == TokenKind.COMMA){
				acceptIt();
			}else{
				break;//if there's no comma, there should be no loop
			}
		}
	}
	
	public void parseArgumentList(){
		//ArgumentList ::= Expression (, Expression)*?
		while(token.kind != TokenKind.RIGHTPARA){
			parseExpression();
			if(token.kind == TokenKind.COMMA){
				acceptIt();
			}else{
				break; //if there's no comma, there should be no loop
			}
		}
	}
	
	public void parseReference(){
		//Reference ::= Reference.id | (this | id)
		if(token.kind == TokenKind.THISKEY){
			acceptIt();
		}else{
			accept(TokenKind.ID);
		}
		while(token.kind == TokenKind.PERIOD){
			acceptIt();
			accept(TokenKind.ID);
		}
		
	}
	
	public void parseIxReference(){
		parseReference();
		if(token.kind == TokenKind.LEFTBRACK){
			acceptIt();
			parseExpression();
			accept(TokenKind.RIGHTBRACK);
		}
	}
	
	public void parseStatement(){
		/*
		 * Statement ::=
		 * 		{ Statement* }
		 * 		Type id = Expression;
		 * 		lxReference = Expression;
		 * 		Reference (ArgumentList?)
		 * 		if (Expression) Statement (else Statement)?
		 * 		while (Expression) Statement
		 */
		
		if(token.kind == TokenKind.LEFTBRACE){//{Statement*}
			acceptIt();
			while(token.kind != TokenKind.RIGHTBRACE){
				parseStatement();
			}
			accept(TokenKind.RIGHTBRACE);
		}else if(token.kind == TokenKind.IFKEY){//if (Expression) Statement (else Statement)?
			acceptIt();
			accept(TokenKind.LEFTPARA);
			parseExpression();
			accept(TokenKind.RIGHTPARA);
			parseStatement();
			if(token.kind == TokenKind.ELSEKEY){
				acceptIt();
				parseStatement();
			}
		}else if(token.kind == TokenKind.WHILEKEY){//while (Expression) Statement
			acceptIt();
			accept(TokenKind.LEFTPARA);
			parseExpression();
			accept(TokenKind.RIGHTPARA);
			parseStatement();
		}else if((token.kind == TokenKind.BOOLEANKEY) || (token.kind == TokenKind.INTKEY)){
			//must be Type id = Expression;
			parseType();
			accept(TokenKind.ID);
			accept(TokenKind.ASSIGNOP);
			parseExpression();
			accept(TokenKind.SEMICOLON);
		}else if(token.kind == TokenKind.THISKEY){
			parseIxReference();
			if(token.kind == TokenKind.LEFTPARA){ //Reference ( ArguementList? );
				acceptIt();
				parseArgumentList();
				accept(TokenKind.RIGHTPARA);
				accept(TokenKind.SEMICOLON);
			}else{//lxReference = Expression;
				/*
				 * as per pa2 instructions,
				 * removed parsing restrictions that prevented
				 * lxReference from being used for method invocation
				if(token.kind == TokenKind.LEFTBRACK){
					acceptIt();
					parseExpression();
					accept(TokenKind.RIGHTBRACK);
				}
				*/
				accept(TokenKind.ASSIGNOP);
				parseExpression();
				accept(TokenKind.SEMICOLON);
			}
		}else{
			accept(TokenKind.ID);
			if(token.kind == TokenKind.PERIOD){ //mathDepartment.chairPerson = Julie;
				while(token.kind == TokenKind.PERIOD){
					acceptIt();
					accept(TokenKind.ID);
				}
			}else if(token.kind == TokenKind.ID){//Student student = George;
				acceptIt();
				accept(TokenKind.ASSIGNOP);
				parseExpression();
				accept(TokenKind.SEMICOLON);
				return;
				
			}
			if(token.kind == TokenKind.LEFTPARA){ //Reference ( ArguementList? );
				acceptIt();
				parseArgumentList();
				accept(TokenKind.RIGHTPARA);
				accept(TokenKind.SEMICOLON);
			}else{//lxReference = Expression;
				if(token.kind == TokenKind.LEFTBRACK){
					acceptIt();
					if(token.kind == TokenKind.RIGHTBRACK){//Student[]classroom = new Student[]
						acceptIt();
						accept(TokenKind.ID);
					}else{
						parseExpression();
						accept(TokenKind.RIGHTBRACK);
					}
				}
				accept(TokenKind.ASSIGNOP);
				parseExpression();
				accept(TokenKind.SEMICOLON);
			}
			
		}
		
	}//end parseStatement
	
	public void parseExpression(){
		/*
		 * Expression ::=
		 * lxReference
		 * Reference ( ArgumentList? )
		 * unop Expression %
		 * Expression binop Expression %
		 * ( Expression ) %
		 * num | true | false %
		 * new( id() | int[Expression] | id[Expression] ) %
		 */
		
		if((token.kind == TokenKind.UNILOGICAL) || (token.kind == TokenKind.NEGATION)){
			acceptIt();
			parseExpression();
		}else if(token.kind == TokenKind.LEFTPARA){
			acceptIt();
			parseExpression();
			accept(TokenKind.RIGHTPARA);
		}else if((token.kind == TokenKind.NUM) || (token.kind == TokenKind.TRUEKEY) 
				|| (token.kind == TokenKind.FALSEKEY)){
			acceptIt();
		}else if(token.kind == TokenKind.NEWKEY){
			acceptIt();
			if(token.kind == TokenKind.ID){
				acceptIt();
				if(token.kind == TokenKind.LEFTBRACK){
					acceptIt();
					parseExpression();
					accept(TokenKind.RIGHTBRACK);
				}else{
					accept(TokenKind.LEFTPARA);
					accept(TokenKind.RIGHTPARA);
				}
			}else{
				accept(TokenKind.INTKEY);
				accept(TokenKind.LEFTBRACK);
				parseExpression();
				accept(TokenKind.RIGHTBRACK);
				
			}
		}else if((token.kind == TokenKind.ID) || (token.kind == TokenKind.THISKEY)){
			parseIxReference(); //removed restriction on IxReference
			if(token.kind == TokenKind.LEFTPARA){
				acceptIt();
				parseArgumentList();
				accept(TokenKind.RIGHTPARA);
			}
		}else{
			parseError("'" + token.spelling +  "' is not a valid way to start an expression");
		}//end of "first" expression
		
		if((token.kind == TokenKind.BI_ARITH) || (token.kind == TokenKind.BI_LOGICAL) 
				|| (token.kind == TokenKind.RELATIONOP) || (token.kind == TokenKind.NEGATION)){
			acceptIt();
			parseExpression();
		}
	}
	
	/**
	 * accept current token and advance to next token
	 */
	public void acceptIt(){
		accept(token.kind);
	}
	
	/**
	 * verify that current token in input matches expected token and advance to next token
	 * @param expectedToken
	 * @throws SyntaxError  if match fails
	 */
	
	private void accept(TokenKind expectedTokenKind) throws SyntaxError{
		if(token.kind == expectedTokenKind){
			if(trace){
				pTrace();
			}
			token = scanner.scan();
			//ignore comment tokens
			if(token.kind == TokenKind.COMMENT){
				acceptIt();
			}
		}else{
			parseError("Expecting '" + expectedTokenKind + "' but found '" + token.kind + "'");
		}
	}
	/**
	 * report parse error and unwind call stack to start of parse
	 * @param e  string with error detail
	 * @throws SyntaxError
	 */
	private void parseError(String message) throws SyntaxError{
		errorReporter.reportError("Parse error: " + message);
		throw new SyntaxError();
	}
	
	//show parse stack whenever terminal is accepted
	private void pTrace(){
		StackTraceElement [] st1 = Thread.currentThread().getStackTrace();
		for(int i=st1.length - 1; i > 0; i--){
			if(st1[i].toString().contains("parse")){
				System.out.println(st1[i]);
			}
				
		}
		System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
		System.out.println();
	}
	/**
	 * the two _Exists methods are used to determine whether a class, field, or
	 * method name is already in use or not
	 * 
	 */
	
	
	

}
