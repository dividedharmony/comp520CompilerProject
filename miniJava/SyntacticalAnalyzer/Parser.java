package miniJava.SyntacticalAnalyzer;
import miniJava.ErrorReporter;
import miniJava.SyntacticalAnalyzer.Identifier.ID_Kind;

import java.util.ArrayList;

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
	private ArrayList<Identifier> publicList;
	ArrayList<Identifier> classIDs;
	ArrayList<Token> returnIDs;
	
	public Parser(Scanner s, ErrorReporter r){
		this.scanner = s;
		this.errorReporter = r;
		this.classIDs = new ArrayList<Identifier>();
		this.returnIDs = new ArrayList<Token>();
		this.publicList = new ArrayList<Identifier>();
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
		try{
			parseProgram();
			
		}catch(SyntaxError e){}
		
	}
	/**
	 * ArrayList programIDs will keep track of all identifiers
	 * used to name classes. If duplicates are found a
	 * parseError will be invoked.
	 * ArrayList returnIDs will keep track of all identifiers
	 * named as return types for a method. Since a method can
	 * name a class id before the class is declared programIDs
	 * cannot be used for this purpose
	 */
	
	public void parseProgram(){
		
		while(token.kind == TokenKind.CLASSKEY){
			parseClassDeclaration();
		}
		for(int i=0; i<returnIDs.size(); i++){ //make sure return types can be resolved
			boolean resolvedToType = false;
			for(int j=0; j<classIDs.size(); j++){
				if(returnIDs.get(i).spelling.equals(classIDs.get(j).spelling)){
					resolvedToType = true;
					break;
				}
			}
			if(!resolvedToType){
				parseError("Could not resolve " + returnIDs.get(i).spelling
						+ " to a type.");
			}
		}
		accept(TokenKind.EOT);
	}
	
	@SuppressWarnings("unchecked")
	public void parseClassDeclaration(){
		ArrayList<Identifier> privateList = new ArrayList<Identifier>();
		accept(TokenKind.CLASSKEY);
		if(token.kind == TokenKind.ID){
			if(!classExists(token, classIDs)){
				Identifier i = new Identifier(token.spelling, ID_Kind.CLASS);
				classIDs.add(i);
				acceptIt();
			}else{
				parseError("Identifier already used by another class");
			}
		}else{
			parseError("Identifier expected");
		}
		accept(TokenKind.LEFTBRACE);
		while(token.kind != TokenKind.RIGHTBRACE){
			parseDeclarator(privateList);
		}
		accept(TokenKind.RIGHTBRACE);
		ArrayList<Identifier> methodsAndFields = new ArrayList<Identifier>();
		methodsAndFields.addAll((ArrayList<Identifier>)privateList.clone());
		methodsAndFields.addAll((ArrayList<Identifier>)publicList.clone());
		alreadyExists(methodsAndFields);
		
	}
	
	public void parseDeclarator(ArrayList<Identifier> privateList){
		Identifier id = new Identifier(null, ID_Kind.CLASS);
		if(token.kind == TokenKind.PUBLICKEY){
			acceptIt();
			id.isPublic = true;
			publicList.add(id);
		}else if(token.kind == TokenKind.PRIVATEKEY){
			acceptIt();
			id.isPublic = false;
			privateList.add(id);
		}else{
			id.isPublic = true;
			publicList.add(id);
		}
		if(token.kind == TokenKind.STATICKEY){
			acceptIt();
			id.isStatic = true; //by default, if Static keyword is not there, id is not Static
		}
		if(token.kind == TokenKind.VOIDKEY){
			acceptIt();
			id.returnType = ID_Kind.VOID;
			id.holdsThisValue = ID_Kind.METHOD;
			id.spelling = token.spelling;
			accept(TokenKind.ID); 
			id.parameterList = parseMethodDeclaration(id, privateList);
		}else if(token.kind == TokenKind.BOOLEANKEY){// boolean foo()
			acceptIt();
			id.spelling = token.spelling;
			accept(TokenKind.ID);
			if(token.kind == TokenKind.LEFTPARA){
				id.holdsThisValue = ID_Kind.METHOD;
				id.returnType = ID_Kind.BOOLEAN;
			}else{
				accept(TokenKind.SEMICOLON);//Must be field declaration
			}
			
		}else if(token.kind == TokenKind.INTKEY){ // int i;
			acceptIt();
			if(token.kind == TokenKind.LEFTBRACK){//int[] gradeArray;
				acceptIt();
				accept(TokenKind.RIGHTBRACK);
				id.spelling = token.spelling;
				accept(TokenKind.ID);
				id.holdsThisValue = ID_Kind.INTARRAY;
			}else{//int age;
				id.holdsThisValue = ID_Kind.INT;
				id.spelling = token.spelling;
				accept(TokenKind.ID);
			}
			if(token.kind == TokenKind.LEFTPARA){
				id.returnType = id.holdsThisValue;
				id.holdsThisValue = ID_Kind.METHOD;
			}else{
				accept(TokenKind.SEMICOLON);//Must be field declaration
			}
			
		}else if(token.kind == TokenKind.ID){ // public static Student getHighestGrade()
			returnIDs.add(token);
			acceptIt();
			id.spelling = token.spelling;
			accept(TokenKind.ID);
			if(token.kind == TokenKind.LEFTBRACK){
				acceptIt();
				accept(TokenKind.RIGHTBRACK);
				id.holdsThisValue = ID_Kind.INSTANCEARRAY;
			}else{
				id.holdsThisValue = ID_Kind.INSTANCE;
			}
			if(token.kind == TokenKind.LEFTPARA){
				id.returnType = id.holdsThisValue;
				id.holdsThisValue = ID_Kind.METHOD;
				id.parameterList = parseMethodDeclaration(id, privateList);
			}else{
				accept(TokenKind.SEMICOLON);//Must be field declaration
			}
			
		}
	}//end parseDeclarator
	/**
	 * 
	 * @param returnIDs
	 * @param returnType
	 * returnType is needed to know if method returns the type
	 * that was declared it would (void, int, bool, id, array)
	 */
	
	public ArrayList<ID_Kind> parseMethodDeclaration(Identifier methodID,
												ArrayList<Identifier> privateList){
		//Declarators id (ParameterList?) {Statement* (return Expression;)?}
		accept(TokenKind.LEFTPARA);
		ArrayList<ID_Kind> parameterList = new ArrayList<ID_Kind>();
		ArrayList<Identifier> localList = new ArrayList<Identifier>();
		parseParameterList(returnIDs, parameterList, localList);
		accept(TokenKind.RIGHTPARA);
		accept(TokenKind.LEFTBRACE);
		while((token.kind != TokenKind.RETURNKEY) && (token.kind != TokenKind.RIGHTBRACE)){
			parseStatement(localList, privateList);
		}
		//parse the return statement, if needed
		if(methodID.returnType == ID_Kind.VOID){
			if(token.kind == TokenKind.RETURNKEY){
				acceptIt();
				accept(TokenKind.SEMICOLON);
				accept(TokenKind.RIGHTBRACE);
			}
			accept(TokenKind.RIGHTBRACE);
		}else{
			accept(TokenKind.RETURNKEY);
			if(parseExpression(localList, privateList) == methodID.returnType){
				accept(TokenKind.SEMICOLON);
				accept(TokenKind.RIGHTBRACE);
			}else{
				parseError("Type mismatch. Return Type must match method signature");
			}	
		}
		return parameterList;
	}//end parseMethodDeclaration
	
	public void parseParameterList(ArrayList<Token> returnIDs, 
									ArrayList<ID_Kind> parameterList,
									ArrayList<Identifier> localList){
		while(token.kind != TokenKind.RIGHTPARA){
			if(token.kind == TokenKind.BOOLEANKEY){//setStudent(boolean isSenior){}
				acceptIt();
				ID_Kind idKind = ID_Kind.BOOLEAN;
				parameterList.add(idKind);
				Identifier id = new Identifier(token.spelling, idKind);
				localList.add(id);
				accept(TokenKind.ID);
			}else if(token.kind == TokenKind.INTKEY){
				acceptIt();
				if(token.kind == TokenKind.ID){//setStudent(int grade)
					ID_Kind idKind = ID_Kind.INT;
					parameterList.add(idKind);
					Identifier id = new Identifier(token.spelling, idKind);
					localList.add(id);
					accept(TokenKind.ID);
				}else if(token.kind == TokenKind.LEFTBRACK){//setStudent(int[] overallGrades)
					accept(TokenKind.RIGHTBRACK);
					ID_Kind idKind = ID_Kind.INTARRAY;
					parameterList.add(idKind);
					Identifier id = new Identifier(token.spelling, idKind);
					localList.add(id);
					accept(TokenKind.ID);
				}else{
					parseError("Type mismatch in parameter list");
				}
			}else if(token.kind == TokenKind.ID){
				returnIDs.add(token);
				acceptIt();
				if(token.kind == TokenKind.ID){//setStudent(Student clone){}
					ID_Kind idKind = ID_Kind.INSTANCE;
					parameterList.add(idKind);
					Identifier id = new Identifier(token.spelling, idKind);
					localList.add(id);
					accept(TokenKind.ID);
				}else if(token.kind == TokenKind.LEFTBRACK){//setClass(Student[] studentArray)
					acceptIt();
					accept(TokenKind.RIGHTBRACK);
					ID_Kind idKind = ID_Kind.INSTANCEARRAY;
					parameterList.add(idKind);
					Identifier id = new Identifier(token.spelling, idKind);
					localList.add(id);
					accept(TokenKind.ID);
				}else{
					parseError("Type mismatch in parameter list");
				}
			}
			if(token.kind == TokenKind.COMMA){
				acceptIt();
			}else{
				break; //if there's no comma, the parameter list should end
			}
		}
	}
	
	public void parseStatement(ArrayList<Identifier> localList,
								ArrayList<Identifier> privateList){
		//first case: { statement* }
		if(token.kind == TokenKind.LEFTBRACE){
			acceptIt();
			while(token.kind != TokenKind.RIGHTBRACE){
				parseStatement(localList, privateList);
			}
			accept(TokenKind.RIGHTBRACE);
		//second case: Type id = Expression;
		}else if(token.kind == TokenKind.BOOLEANKEY){
			acceptIt();
			Identifier id = new Identifier(token.spelling, ID_Kind.BOOLEAN);
			localList.add(id);
			accept(TokenKind.ID);
			accept(TokenKind.ASSIGNOP);
			if(parseExpression(localList, privateList) == ID_Kind.BOOLEAN){
				//No statement needed here
			}else{
				parseError("Type mismatch. boolean value expected");
			}
			accept(TokenKind.SEMICOLON);
		}else if(token.kind == TokenKind.INTKEY){
			acceptIt();
			Identifier id = new Identifier(null, null);
			if(token.kind == TokenKind.LEFTBRACK){ //if int[] rather than just int
				acceptIt();
				accept(TokenKind.RIGHTBRACK);
				id.holdsThisValue = ID_Kind.INTARRAY;
			}else{
				id.holdsThisValue = ID_Kind.INT;
			}
			id.spelling = token.spelling;
			localList.add(id);
			accept(TokenKind.ID);
			accept(TokenKind.ASSIGNOP);
			if(parseExpression(localList, privateList) == id.holdsThisValue){
				//No statement needed here
			}else{
				parseError("Type mismatch.");
			}
			accept(TokenKind.SEMICOLON);

		}else if(token.kind == TokenKind.IFKEY){
			//third case: if(Expression) Statement (else Statement)?
			acceptIt();
			accept(TokenKind.LEFTPARA);
			if(parseExpression(localList, privateList) == ID_Kind.BOOLEAN){
				accept(TokenKind.RIGHTPARA);
				@SuppressWarnings("unchecked")
				ArrayList<Identifier> innerLocalList = 
						(ArrayList<Identifier>) localList.clone();
				parseStatement(innerLocalList, privateList);
				if(token.kind == TokenKind.ELSEKEY){ //(else Statement)?
					@SuppressWarnings("unchecked")
					ArrayList<Identifier> innerLocalList2 = 
							(ArrayList<Identifier>) localList.clone();
					parseStatement(innerLocalList2, privateList);
				}
			}else{
				parseError("type mismatch. 'if' statement requires boolean value");
			}
		//fourth case: while(Expression) Statement
		}else if(token.kind == TokenKind.WHILEKEY){
			acceptIt();
			accept(TokenKind.LEFTPARA);
			if(parseExpression(localList, privateList) == ID_Kind.BOOLEAN){
				accept(TokenKind.RIGHTPARA);
				@SuppressWarnings("unchecked")
				ArrayList<Identifier> innerLocalList = 
						(ArrayList<Identifier>) localList.clone();
				parseStatement(innerLocalList, privateList);
			}else{
				parseError("type mismatch. 'while' statement requires boolean value");
			}
			
		}
		/*
		 * if id is occured here, there are three possibilities:
		 * 1) ClassType id = expression;
		 * 2) lxReference = expression;
		 * 3) Reference ( ArgumentList? );
		 * 
		 */
		else if((token.kind == TokenKind.ID) || (token.kind == TokenKind.THISKEY)){
			Token tempToken = token;
			if(token.kind == TokenKind.ID){
				acceptIt();
			}else if(token.kind == TokenKind.THISKEY){
				acceptIt();
			}
			if(token.kind == TokenKind.PERIOD){ //Student.George.setIsSenior(true);
				while(token.kind == TokenKind.PERIOD){
					acceptIt();
					accept(TokenKind.ID);
				}
				if(token.kind == TokenKind.LEFTPARA){//Reference ( ArgumentList? );
					while(token.kind != TokenKind.RIGHTPARA){
						parseExpression(localList, privateList);
						if(token.kind == TokenKind.COMMA){
							acceptIt();
						}else{
							break; //if there is no comma, argument list should not continue
						}
					}
					
				}else if(token.kind == TokenKind.ASSIGNOP){
					acceptIt();
					parseExpression(localList, privateList);
				}
				accept(TokenKind.SEMICOLON);
				
			}else if(token.kind == TokenKind.ID){ //Student George = null;
				Identifier id = new Identifier(token.spelling, ID_Kind.INSTANCE);
				returnIDs.add(tempToken);
				accept(TokenKind.ASSIGNOP);
				if(parseExpression(localList, privateList) == ID_Kind.INSTANCE){
					localList.add(id);
					accept(TokenKind.SEMICOLON);
				}else{
					parseError("Type Mismatch. " + id.spelling + 
							" could not be resovled into an instance variable");
				}
				
			}else if(token.kind == TokenKind.LEFTBRACK){
				acceptIt();
				//Student[] studentArray = null;
				if(token.kind == TokenKind.RIGHTBRACK){
					returnIDs.add(tempToken); //check if valid return type
					acceptIt();
					Identifier id = new Identifier(token.spelling, ID_Kind.INSTANCEARRAY);
					accept(TokenKind.ID);
					accept(TokenKind.ASSIGNOP);
					if(parseExpression(localList, privateList) == ID_Kind.INSTANCEARRAY){
						localList.add(id);
						accept(TokenKind.SEMICOLON);
					}else{
						parseError("Type mismatch. Could not resolve " + id.spelling + 
								" into an instance array");
					}
					
				//studentArray[4] = null;
				}else if(parseExpression(localList, privateList) == ID_Kind.INT){
					accept(TokenKind.RIGHTBRACK);
					accept(TokenKind.ASSIGNOP);
					//TODO future editions should tell what kind of array this is
					parseExpression(localList, privateList);
					accept(TokenKind.SEMICOLON);
					
				}else{
					parseError("Type mismatch. Invalid use of brackets");
				}
				
			}else{
				parseError("Statement has a dangling identifier");
			}
		}
	}//end parseStatement
	
	
	
	public ID_Kind parseExpression(ArrayList<Identifier> localList,
								ArrayList<Identifier> privateList){
		ID_Kind returnType = null;
		if((token.kind == TokenKind.TRUEKEY) || token.kind == TokenKind.FALSEKEY){
			acceptIt();
			returnType = ID_Kind.BOOLEAN;
		}else if(token.kind == TokenKind.NUM){
			acceptIt();
			returnType = ID_Kind.INT;
		}else if(token.kind == TokenKind.LEFTPARA){
			acceptIt();
			returnType = parseExpression(localList, privateList);
			accept(TokenKind.RIGHTPARA);
		}else if(token.kind == TokenKind.NEWKEY){
			acceptIt();
			if(token.kind == TokenKind.ID){ 
				returnIDs.add(token);
				acceptIt();
				if(token.kind == TokenKind.LEFTPARA){// Student Rosio = new Student();
					acceptIt();
					accept(TokenKind.RIGHTPARA);
					returnType = ID_Kind.INSTANCE;
				}else{ //Student[] studentArray = new Student[5];
					accept(TokenKind.LEFTBRACK);
					if(parseExpression(localList, privateList) == ID_Kind.INT){
						accept(TokenKind.RIGHTBRACK);
						returnType = ID_Kind.INSTANCEARRAY;
					}
				}
				
			}else if(token.kind == TokenKind.INTKEY){ //int[] gradeArray = new int[100];
				acceptIt();
				accept(TokenKind.LEFTBRACK);
				if(parseExpression(localList, privateList) == ID_Kind.INT){
					accept(TokenKind.RIGHTBRACK);
					returnType = ID_Kind.INTARRAY;
				}else{
					parseError("Type mismatch. Need an 'int' value to intialize an array");
				}
			}
		}else if(token.kind == TokenKind.NEGATION){
			acceptIt();
			if(parseExpression(localList, privateList) == ID_Kind.INT){
				returnType = ID_Kind.INT;
			}else{
				parseError("Type Mismatch. Can only negate an 'int' type.");
			}
		}else if(token.kind == TokenKind.UNILOGICAL){// boolean isFailing = !isPassing;
			acceptIt();
			if(parseExpression(localList, privateList) == ID_Kind.BOOLEAN){
				returnType = ID_Kind.BOOLEAN;
			}else{
				parseError("Type Mismatch. '!' only operates on boolean values");
			}
		//Student George = this.Classroom.getStudent(5);
		}else if((token.kind == TokenKind.ID) || (token.kind == TokenKind.THISKEY)){
			Identifier id = parseLexicalReference(localList, privateList);
			if(id.holdsThisValue == ID_Kind.METHOD){ //.getStudent(5)
				returnType = id.returnType;
			}else{//Student George = this.valedictorian;
				returnType = id.holdsThisValue;
				
			}
		}//end of "first" expression
		
		
		if(token.kind == TokenKind.RELATIONOP){// (grade > 90)
			acceptIt();
			parseExpression(localList, privateList); //op '!=' might not be limited to int
			returnType = ID_Kind.BOOLEAN;
			
		}else if(token.kind == TokenKind.BI_LOGICAL){
			acceptIt();
			if(parseExpression(localList, privateList) == ID_Kind.BOOLEAN){
				returnType = ID_Kind.BOOLEAN;
			}else{
				parseError("Type mismatch. Logical biop only operate on boolean values");
			}
			
		}else if(token.kind == TokenKind.BI_ARITH){
			acceptIt();
			if(parseExpression(localList, privateList) == ID_Kind.INT){
				returnType = ID_Kind.INT;
			}else{
				parseError("Type mismatch. Arith biop only operate on int values");
			}
		}else if(token.kind == TokenKind.NEGATION){
			acceptIt();
			if(parseExpression(localList, privateList) == ID_Kind.INT){
				returnType = ID_Kind.INT;
			}else{
				parseError("Type mismatch. Arith biop only operate on int values");
			}
		}
		
		
		return returnType;
	}
	/*
	 * parseReference checks to see if a refernce begins with 'this' keyword
	 * Any reference beyond that cannot have the 'this' keyword and is thus
	 * handle by parsePastThisRefernce recursively until no more dot operators
	 * separate the identifiers
	 * 
	 * parseReference and its ilk need to return ID_Kind in order to know if the
	 * reference passes a valid type. In order to do this with overloaded methods
	 * argumentList needs to return argument list
	*/
	
	public Identifier parseReference(ArrayList<Identifier> privateList){
		//Reference . id | (this | id)
		Identifier returnID = new Identifier(null, ID_Kind.INSTANCE);
		if(token.kind == TokenKind.THISKEY){
			acceptIt();
			if(token.kind == TokenKind.PERIOD){
				acceptIt();
				returnID = parsePastThisReference(privateList);
			}else{ //case: end of reference chain with the this key
				returnID = new Identifier(null, ID_Kind.INSTANCE);
			}
		}else{
			parsePastThisReference(privateList);
		}
		return returnID;
	}
	
	private Identifier parsePastThisReference(ArrayList<Identifier> privateList){
		Identifier returnID = null;
		Token tempToken = token;
		accept(TokenKind.ID);
		if(token.kind == TokenKind.PERIOD){//case: continue reference chain
			acceptIt();
			returnID = parsePastThisReference(privateList);
		}else{//case: end of reference chain
			for(int i=0; i<privateList.size(); i++){
				if(privateList.get(i).spelling.equals(tempToken.spelling)){
					returnID = privateList.get(i);
					return returnID;
				}
			}
			for(int i=0; i<publicList.size(); i++){
				if(publicList.get(i).spelling.equals(tempToken.spelling)){
					returnID = publicList.get(i);
					return returnID;
				}
			}
			for(int i=0; i<classIDs.size(); i++){
				if(classIDs.get(i).spelling.equals(tempToken.spelling)){
					returnID = classIDs.get(i);
					return returnID;
				}
			}
		}
		return returnID;
	}
	
	
	public Identifier parseLexicalReference(ArrayList<Identifier> localList,
										ArrayList<Identifier> privateList){
		//Reference ( [Expression] )?
		Identifier referenceID = parseReference(privateList);
		Identifier returnID = new Identifier(null, null);
		if(token.kind == TokenKind.LEFTBRACK){
			acceptIt();
			if(parseExpression(localList, privateList) == ID_Kind.INT){
				accept(TokenKind.RIGHTBRACK);
				if(referenceID.holdsThisValue == ID_Kind.INTARRAY){
					returnID.holdsThisValue = ID_Kind.INT;
				}else if(referenceID.holdsThisValue == ID_Kind.INSTANCEARRAY){
					referenceID.holdsThisValue = ID_Kind.INSTANCE;
				}else{
					parseError("Invalid use of brackets");
				}
				
			}else{
				parseError("Type mismatch. Retrieving from array requires an 'int' value");
			}
			
		}else{//no brackets after intial reference
			returnID = referenceID;
			
		}
		return returnID;
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
	
	public boolean classExists(Token currentToken, ArrayList<Identifier> IDArray ){
		boolean exists = false;
		for(int i=0; i<IDArray.size(); i++){
			if(IDArray.get(i).spelling.equals(currentToken.spelling)){
				exists = true;
				break;
			}
		}
		return exists;
	}
	public void alreadyExists(ArrayList<Identifier> idArray){
		for(int i=0; i<idArray.size(); i++){
			for(int j=(i+1); j<idArray.size(); j++){
				if(idArray.get(i).spelling.equals(idArray.get(j).spelling)){
					if((idArray.get(i).holdsThisValue != ID_Kind.METHOD) 
							&& idArray.get(j).holdsThisValue != ID_Kind.METHOD){
						parseError("Duplicate error. field already exists");
					
					
					
					}else if((idArray.get(i).parameterList.size() ==
						idArray.get(j).parameterList.size())){
						for(int k=0; k<idArray.get(i).parameterList.size(); k++){
							if(idArray.get(i).parameterList.get(k) ==
									idArray.get(j).parameterList.get(k)){
										parseError("Duplicate error. method already exists");
									}
						}
					}
				}
			}
		}
		
	}
	

}
