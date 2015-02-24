//Part of the PA2 rewrite

package miniJava.SyntacticAnalyzer;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {
	
	private Scanner scanner;
	private ErrorReporter errorReporter;
	private Token token;
	private SourcePosition previousTokenPosition;
	
	private boolean trace = true;
	
	public Parser(Scanner s, ErrorReporter r){
		this.scanner = s;
		this.errorReporter = r;
		this.previousTokenPosition = new SourcePosition();
	}
	
	/**
	 * begin parse
	 */
	
	public Package parse(){
		token = scanner.scan();
		while(token.kind == TokenKind.COMMENT){
			token = scanner.scan();
		}
		try{
			return parseProgram();
						
		}catch(SyntaxError e){
			return null;
		}
		
	}
	
	public Package parseProgram(){
		//Program ::= (ClassDeclaration)* eot
		Package p = null;
		ClassDeclList cList = new ClassDeclList();
		SourcePosition packPos = new SourcePosition();
		
		
		start(packPos);
		while(token.kind == TokenKind.CLASSKEY){
			cList.add(parseClassDeclaration());
		}
		accept(TokenKind.EOT);
		finish(packPos);
		
		
		p = new Package(cList, packPos);
		return p;
	}
	
	public ClassDecl parseClassDeclaration(){
		//ClassDeclaration ::= class id { (FieldDeclaration | MethodDeclaration)* }
		
		ClassDecl cd = null;
		FieldDeclList fList = new FieldDeclList();
		MethodDeclList mList = new MethodDeclList();
		SourcePosition cPos = new SourcePosition();
		
		start(cPos);
		accept(TokenKind.CLASSKEY);
		
		String cdName = token.spelling;
		accept(TokenKind.ID); //class name
		
		accept(TokenKind.LEFTBRACE);
		while(token.kind != TokenKind.RIGHTBRACE){
			MemberDecl memD = parseDeclarators();
			if(memD instanceof MethodDecl){
				mList.add((MethodDecl) memD);
			}else if(memD instanceof FieldDecl){
				fList.add((FieldDecl) memD);
			}else{
				parseError("Could not cast " + memD.name + " as method or field");
			}
		}
		accept(TokenKind.RIGHTBRACE);
		finish(cPos);
		
		cd = new ClassDecl(cdName, fList, mList, cPos);
		return cd;
	}
	
	
	public MemberDecl parseDeclarators(){
		//Declarators ::= (public | private)? static? (Type | void)
		MemberDecl memD = null;
		Signature s = new Signature();
		SourcePosition dPos = new SourcePosition();
		
		start(dPos);
		if(token.kind == TokenKind.PUBLICKEY){
			s.isPrivate = false;
			acceptIt();
		}else if(token.kind == TokenKind.PRIVATEKEY){
			s.isPrivate = true;
			acceptIt();
		}
		if(token.kind == TokenKind.STATICKEY){
			s.isStatic = true;
			acceptIt();
		}
		s.type = parseType();
		s.name = token.spelling;
		accept(TokenKind.ID); //field or method name
		if(token.kind == TokenKind.SEMICOLON){
			//member is a field
			acceptIt();
			finish(dPos);
			memD = new FieldDecl(s.isPrivate, s.isStatic, s.type, s.name, dPos);
		}else if(token.kind == TokenKind.LEFTPARA){
			//member is a method
			//Declarators id (ParameterList) {Statement* (return Expression;)? }
			
			ParameterDeclList pList = new ParameterDeclList();
			StatementList sList = new StatementList();
			Expression e = null;
			
			accept(TokenKind.LEFTPARA);
			pList = parseParameterList();
			accept(TokenKind.RIGHTPARA);
			accept(TokenKind.LEFTBRACE);
			while((token.kind != TokenKind.RETURNKEY) && (token.kind != TokenKind.RIGHTBRACE)){
				sList.add(parseStatement());
			}
			if(token.kind == TokenKind.RETURNKEY){
				acceptIt();
				e = parseExpression();
				accept(TokenKind.SEMICOLON);
			}
			accept(TokenKind.RIGHTBRACE);
			finish(dPos);
			
			memD = new FieldDecl(s.isPrivate, s.isStatic, s.type, s.name, dPos);
			memD = new MethodDecl(memD, pList, sList, e, dPos);
			
		}else{
			parseError("Expected ';' or '(' but found '" + token.spelling + "'");
		}
		
		return memD;
	}
	
	public Type parseType(){
		//Type ::= PrimType | id | ArrType
		//PrimType ::= int | boolean
		//ArrType ::= (int | id) []
		//Type ::=* boolean | (int | id) ([])?
		
		Type type = null;
		SourcePosition tPos = new SourcePosition();
		
		start(tPos);
		if(token.kind == TokenKind.BOOLEANKEY){
			acceptIt();
			finish(tPos);
			type = new BaseType(TypeKind.BOOLEAN, tPos);
		}else if(token.kind == TokenKind.INTKEY){
			type = new BaseType(TypeKind.INT, token.position);
			acceptIt();
			if(token.kind == TokenKind.LEFTBRACK){//case: integer array
				acceptIt();
				accept(TokenKind.RIGHTBRACK);
				finish(tPos);
				type = new ArrayType(type, tPos);
			}
		}else if(token.kind == TokenKind.ID){
			type = new ClassType(new Identifier(token), token.position);
			acceptIt();
			if(token.kind == TokenKind.LEFTBRACK){//case: class objects array
				acceptIt();
				accept(TokenKind.RIGHTBRACK);
				finish(tPos);
				type = new ArrayType(type, tPos);
			}
		}else if(token.kind == TokenKind.VOIDKEY){
			acceptIt();
			finish(tPos);
			type = new BaseType(TypeKind.VOID, tPos);
		}else{
			parseError("Type missing.");
		}
		
		return type;
	}
	
	public ParameterDeclList parseParameterList(){
		//ParameterList ::= Type id (, Type id)*
		ParameterDeclList pList = new ParameterDeclList();
		SourcePosition pPos = new SourcePosition();
		
		
		while(token.kind != TokenKind.RIGHTPARA){
			start(pPos);
			Type t = parseType();
			if(t.typeKind == TypeKind.VOID){
				parseError("'void' is not a valid parameter type");
			}
			String pName = token.spelling;
			accept(TokenKind.ID);
			finish(pPos);
			pList.add(new ParameterDecl(t, pName, pPos));
			
			if(token.kind == TokenKind.COMMA){
				acceptIt();
			}else{
				break;//if there's no comma, there should be no loop
			}
		}
		
		return pList;
	}
	
	public ExprList parseArgumentList(){
		//ArgumentList ::= Expression (, Expression)*?
		ExprList eList = new ExprList();
		
		while(token.kind != TokenKind.RIGHTPARA){
			eList.add(parseExpression());
			if(token.kind == TokenKind.COMMA){
				acceptIt();
			}else{
				break; //if there's no comma, there should be no loop
			}
		}
		
		return eList;
	}
	
	public Reference parseReference(){
		//Reference ::= Reference.id | (this | id)
		Reference ref = null;
		SourcePosition refPos = new SourcePosition();
		
		start(refPos);
		if(token.kind == TokenKind.THISKEY){
			acceptIt();
			finish(refPos);
			ref = new ThisRef(refPos);
		}else{
			Token temp = token;
			accept(TokenKind.ID);
			finish(refPos);
			ref = new IdRef(new Identifier(temp), refPos);
		}
		while(token.kind == TokenKind.PERIOD){
			acceptIt();
			Token temp = token;
			Identifier id = new Identifier(temp);
			accept(TokenKind.ID);
			finish(refPos);
			ref = new QualifiedRef(ref, id, refPos);
		}
		
		return ref;
	}
	
	public IndexedRef parseIxReference(Reference ref){
		SourcePosition ixPos = new SourcePosition();
		ixPos.start = ref.posn.start;
		Expression e = null;
		
		if(token.kind == TokenKind.LEFTBRACK){
			acceptIt();
			e = parseExpression();
			accept(TokenKind.RIGHTBRACK);
		}
		finish(ixPos);
		
		return new IndexedRef(ref, e, ixPos);
	}
	
	public Statement parseStatement(){
		/*
		 * Statement ::=
		 * 		{ Statement* }
		 * 		Type id = Expression;
		 * 		lxReference = Expression;
		 * 		Reference (ArgumentList?)
		 * 		if (Expression) Statement (else Statement)?
		 * 		while (Expression) Statement
		 */
		Statement state = null;
		SourcePosition sPos = new SourcePosition();
		
		start(sPos);
		if(token.kind == TokenKind.LEFTBRACE){//{Statement*}
			acceptIt();
			StatementList sList = new StatementList();
			while(token.kind != TokenKind.RIGHTBRACE){
				sList.add(parseStatement());
			}
			accept(TokenKind.RIGHTBRACE);
			finish(sPos);
			
			state = new BlockStmt(sList, sPos);
		}else if(token.kind == TokenKind.IFKEY){//if (Expression) Statement (else Statement)?
			acceptIt();
			accept(TokenKind.LEFTPARA);
			Expression e = parseExpression();
			accept(TokenKind.RIGHTPARA);
			Statement thenStmt = parseStatement();
			if(token.kind == TokenKind.ELSEKEY){
				acceptIt();
				Statement elseStmt = parseStatement();
				finish(sPos);
				
				state = new IfStmt(e, thenStmt, elseStmt, sPos);
			}else{
				finish(sPos);
				
				state = new IfStmt(e, thenStmt, sPos);
			}
		}else if(token.kind == TokenKind.WHILEKEY){//while (Expression) Statement
			acceptIt();
			accept(TokenKind.LEFTPARA);
			Expression e = parseExpression();
			accept(TokenKind.RIGHTPARA);
			Statement s = parseStatement();
			finish(sPos);
			
			state = new WhileStmt(e, s, sPos);
		}else if((token.kind == TokenKind.BOOLEANKEY) || (token.kind == TokenKind.INTKEY)){
			//must be Type id = Expression;
			//ie VarDecl
			SourcePosition varPos = new SourcePosition();
			
			start(varPos);
			Type t = parseType();
			String name = token.spelling;
			accept(TokenKind.ID);
			finish(varPos);
			VarDecl vD = new VarDecl(t, name, varPos);
			
			accept(TokenKind.ASSIGNOP);
			Expression e = parseExpression();
			accept(TokenKind.SEMICOLON);
			finish(sPos);
			
			state = new VarDeclStmt(vD, e, sPos);
			
		}else if(token.kind == TokenKind.THISKEY){
			Reference r = parseReference();
			if(token.kind == TokenKind.LEFTBRACK){
				r = parseIxReference(r);
			}
			if(token.kind == TokenKind.LEFTPARA){ //Reference ( ArguementList? );
				acceptIt();
				ExprList argList = parseArgumentList();
				accept(TokenKind.RIGHTPARA);
				accept(TokenKind.SEMICOLON);
				finish(sPos);
				
				state = new CallStmt(r, argList, sPos);
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
				Expression e = parseExpression();
				accept(TokenKind.SEMICOLON);
				finish(sPos);
				
				state = new AssignStmt(r, e, sPos);
			}
		}else{
			Token temp = token;
			accept(TokenKind.ID);
			Reference r = null;
			if(token.kind == TokenKind.PERIOD){ //mathDepartment.chairPerson = Julie;
				Identifier id = new Identifier(temp);
				finish(sPos);
				r = new IdRef(id, sPos);
				while(token.kind == TokenKind.PERIOD){
					acceptIt();
					id = new Identifier(token);
					accept(TokenKind.ID);
					finish(sPos);
					r = new QualifiedRef(r, id, sPos);
				}
				if(token.kind == TokenKind.LEFTBRACK){
					acceptIt();
					Expression e = parseExpression();
					accept(TokenKind.RIGHTBRACK);
					finish(sPos);
					r = new IndexedRef(r, e, sPos);
				}
				
				//WHY NOT JUST RETURN R?
			}else if(token.kind == TokenKind.ID){//Student student = George;
				ClassType type = new ClassType(new Identifier(temp), temp.position);
				String name = token.spelling;
				acceptIt();
				finish(sPos);
				VarDecl vD = new VarDecl(type, name, sPos);
				
				accept(TokenKind.ASSIGNOP);
				Expression e = parseExpression();
				accept(TokenKind.SEMICOLON);
				finish(sPos);
				
				state = new VarDeclStmt(vD, e, sPos);
				return state;//Need to break in order to stop statement here
				
			}else{
				Identifier id = new Identifier(temp);
				finish(sPos);
				r = new IdRef(id, sPos);
			}
			if(token.kind == TokenKind.LEFTPARA){ //Reference ( ArguementList? );
				acceptIt();
				ExprList argList = parseArgumentList();
				accept(TokenKind.RIGHTPARA);
				accept(TokenKind.SEMICOLON);
				finish(sPos);
				
				state = new CallStmt(r, argList, sPos);
			}else{//lxReference = Expression;
				if(token.kind == TokenKind.LEFTBRACK){
					acceptIt();
					if(token.kind == TokenKind.RIGHTBRACK){//Student[]classroom = new Student[]
						acceptIt();
						finish(sPos);
						Identifier id = new Identifier(temp);
						ClassType cT = new ClassType(id, temp.position);
						ArrayType aT = new ArrayType(cT, sPos);
						String name = token.spelling;
						accept(TokenKind.ID);
						finish(sPos);
						VarDecl vD = new VarDecl(aT, name, sPos);
						
						accept(TokenKind.ASSIGNOP);
						Expression expr = parseExpression();
						accept(TokenKind.SEMICOLON);
						finish(sPos);
						
						state = new VarDeclStmt(vD, expr, sPos);
						return state;
					}else{
						Expression e = parseExpression();
						accept(TokenKind.RIGHTBRACK);
						finish(sPos);
						r = new IndexedRef(r, e, sPos);
						
					}
				}//end if (token == TokenKind.LEFTBRACK)
				accept(TokenKind.ASSIGNOP);
				Expression expr = parseExpression();
				accept(TokenKind.SEMICOLON);
				finish(sPos);
				
				state = new AssignStmt(r, expr, sPos);
			}
			
		}
		return state;
	}
	
	public Expression parseExpression(){
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
		Expression e = null;
		e = parseUnaryExpr();
		if((token.kind == TokenKind.BI_ARITH) || (token.kind == TokenKind.BI_LOGICAL)
				|| (token.kind == TokenKind.RELATIONOP) || (token.kind == TokenKind.NEGATION)){
			OpNode rightMost = null;
			while((token.kind == TokenKind.BI_ARITH) || (token.kind == TokenKind.BI_LOGICAL)
				|| (token.kind == TokenKind.RELATIONOP) || (token.kind == TokenKind.NEGATION)){
				rightMost = new OpNode(rightMost);
				rightMost.operator = new Operator(token);
				acceptIt();
				rightMost.leftChild = e;
				e = parseUnaryExpr();
				rightMost.rightChild = e;
			}
			OpNode leftMost = rightMost;
			for(;;){
				if(leftMost.leftOp != null){
					leftMost = leftMost.leftOp;
				}else{
					break;
				}
			}
			e = parseBinOpExpr(leftMost, rightMost);
		}
		
		return e;
	}
	
	///////////////////////////////////////////////////////////////////////
	///////////		ADDITIONAL PARSE EXPRESSION FUNCTIONS AND CLASSES
	//////////////////////////////////////////////////////////////////////
	private class BlankNode{}
	
	
	private class OpNode extends BlankNode{
		OpNode leftOp;
		OpNode rightOp;
		Operator operator;
		Expression leftChild;
		Expression rightChild;
		
		private OpNode(OpNode prev){
			leftOp = prev;
			rightOp = null;
			operator = null;
			leftChild = null;
			rightChild = null;
			
			if(prev != null){
				prev.rightOp = this;
			}
		}
	}//end OpNode class
	
	public Expression parseUnaryExpr(){
		System.out.println("Current token: " + token.spelling);
		Expression expr = null;
		SourcePosition ePos = new SourcePosition();
		
		start(ePos);
		if((token.kind == TokenKind.UNILOGICAL) || (token.kind == TokenKind.NEGATION)){//how
			Operator o = new Operator(token);
			acceptIt();
			Expression e = parseUnaryExpr();
			finish(ePos);
			expr = new UnaryExpr(o, e, ePos);
			return expr;
		}else if(token.kind == TokenKind.LEFTPARA){
			acceptIt();
			Expression e = parseExpression();
			accept(TokenKind.RIGHTPARA);
			finish(ePos);
			return e;
		}else if((token.kind == TokenKind.NUM)){
			Token temp = token;
			acceptIt();
			finish(ePos);
			
			expr = new LiteralExpr(new IntLiteral(temp), ePos);
			return expr;
		}else if((token.kind == TokenKind.TRUEKEY)){
			Token temp = token;
			acceptIt();
			finish(ePos);
			
			expr = new LiteralExpr(new BooleanLiteral(temp), ePos);
			return expr;
		}else if((token.kind == TokenKind.FALSEKEY)){
			Token temp = token;
			acceptIt();
			finish(ePos);
			
			expr = new LiteralExpr(new BooleanLiteral(temp), ePos);
			return expr;
		}else if(token.kind == TokenKind.NEWKEY){
			acceptIt();
			Token temp = token;
			if(token.kind == TokenKind.ID){// class object
				acceptIt();
				finish(ePos);
				ClassType t = new ClassType(new Identifier(temp), ePos);
				if(token.kind == TokenKind.LEFTBRACK){//class object array
					ArrayType aT = new ArrayType(t, ePos);
					acceptIt();
					Expression e = parseExpression();
					accept(TokenKind.RIGHTBRACK);
					finish(ePos);
					
					expr = new NewArrayExpr(aT, e, ePos);
					return expr;
				}else{//new class object
					accept(TokenKind.LEFTPARA);
					accept(TokenKind.RIGHTPARA);
					finish(ePos);
					
					expr = new NewObjectExpr(t, ePos);
					return expr;
				}
			}else{
				accept(TokenKind.INTKEY);
				finish(ePos);
				BaseType t = new BaseType(TypeKind.INT, ePos);
				ArrayType aT = new ArrayType(t, ePos);
				accept(TokenKind.LEFTBRACK);
				Expression e = parseExpression();
				accept(TokenKind.RIGHTBRACK);
				finish(ePos);
				
				expr = new NewArrayExpr(aT, e, ePos);
				return expr;
			}
		}else if((token.kind == TokenKind.ID) || (token.kind == TokenKind.THISKEY)){
			Reference r = parseReference(); //removed restriction on IxReference
			if(token.kind == TokenKind.LEFTBRACK){
				r = parseIxReference(r);
			}
			if(token.kind == TokenKind.LEFTPARA){//Call Expression
				acceptIt();
				ExprList eList = parseArgumentList();
				accept(TokenKind.RIGHTPARA);
				finish(ePos);
				
				expr = new CallExpr(r, eList, ePos);
				return expr;
			}else{
				finish(ePos);
				expr = new RefExpr(r, ePos);
				
			}
		}else{
			parseError("'" + token.spelling +  "' is not a valid way to start an expression");
		}//end of "first" expression
		return expr;
	}
	
	/**
	 * 
	 * @param opN, the right-most operator node given by parseExpression
	 * @return
	 */
	public Expression parseBinOpExpr(OpNode leftMost, OpNode rightMost){
		System.out.println("Entered BinOp");
		Expression binExpr = null;
		Expression e1 = null;
		Expression e2 = null;
		OpNode farLeft = leftMost;
		//rightMost does not move
		
		while(leftMost == rightMost){//disjunction (loosiest bound) tester
			if(leftMost.operator.spelling.equals("||")){
				Operator o = leftMost.operator;
				if(leftMost == farLeft){//ie nothing to the left
					e1 = leftMost.leftChild;
					if(leftMost == rightMost){//ie nothing more to the right
						e2 = leftMost.rightChild;
						binExpr = new BinaryExpr(o, e1, e2, 
								new SourcePosition(e1.posn.start, e2.posn.finish));
					}else{//ie there is a right operator
						e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
						binExpr = new BinaryExpr(o, e1, e2, 
								new SourcePosition(e1.posn.start, e2.posn.finish));
					}
				}else{//ie there is an operator more to the left
					e1 = parseBinOpExpr(farLeft, leftMost.leftOp);
					if(leftMost == rightMost){//ie there is nothing more to the right
						e2 = rightMost.rightChild;
						binExpr = new BinaryExpr(o, e1, e2, 
								new SourcePosition(e1.posn.start, e2.posn.finish));
					}else{//ie there is a more right operator
						e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
						binExpr = new BinaryExpr(o, e1, e2, 
								new SourcePosition(e1.posn.start, e2.posn.finish));
					}
				}
				return binExpr;
		}else{
			if(rightMost != leftMost){
				rightMost = rightMost.leftOp;
			}else{
				break;
			}
		}//end "||" tester
		
	}//end while loop
		
		
		leftMost = farLeft;
	
		
	while(rightMost != null){//conjunction test
		if(rightMost.operator.spelling.equals("&&")){
			Operator o = leftMost.operator;
			if(leftMost == farLeft){//ie nothing to the left
				e1 = leftMost.leftChild;
				if(leftMost == rightMost){//ie nothing more to the right
					e2 = leftMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}else{//ie there is an operator more to the left
				e1 = parseBinOpExpr(farLeft, leftMost.leftOp);
				if(leftMost == rightMost){//ie there is nothing more to the right
					e2 = rightMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a more right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}
			return binExpr;
		}else{
			if(rightMost != leftMost){
				rightMost = rightMost.leftOp;
			}else{
				break;
			}
		}//end "&&" tester
	
	}//end while loop
	
	leftMost = farLeft;
	
	while(rightMost != null){//equality tester
		if(rightMost.operator.spelling.equals("==") || (rightMost.operator.spelling.equals("!="))){
			Operator o = leftMost.operator;
			if(leftMost == farLeft){//ie nothing to the left
				e1 = leftMost.leftChild;
				if(leftMost == rightMost){//ie nothing more to the right
					e2 = leftMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}else{//ie there is an operator more to the left
				e1 = parseBinOpExpr(farLeft, leftMost.leftOp);
				if(leftMost == rightMost){//ie there is nothing more to the right
					e2 = rightMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a more right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}
			return binExpr;
		}else{
			if(rightMost != leftMost){
				rightMost = rightMost.leftOp;
			}else{
				break;
			}
		}//end "==" or "!=" tester
	
	}//end while loop
	
	leftMost = farLeft;
	
	while(rightMost != null){//relation operator tester
		if(rightMost.operator.spelling.equals(">") || (rightMost.operator.spelling.equals("<"))
				|| (rightMost.operator.spelling.equals(">="))
				|| (rightMost.operator.spelling.equals("<="))){
			Operator o = leftMost.operator;
			if(leftMost == farLeft){//ie nothing to the left
				e1 = leftMost.leftChild;
				if(leftMost == rightMost){//ie nothing more to the right
					e2 = leftMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}else{//ie there is an operator more to the left
				e1 = parseBinOpExpr(farLeft, leftMost.leftOp);
				if(leftMost == rightMost){//ie there is nothing more to the right
					e2 = rightMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a more right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}
			return binExpr;
		}else{
			if(rightMost != leftMost){
				rightMost = rightMost.leftOp;
			}else{
				break;
			}
		}//end "==" or "!=" tester
	
	}//end while loop
	
	leftMost = farLeft;
	
	while(rightMost != null){//additive tester
		if(rightMost.operator.spelling.equals("+") || (rightMost.operator.spelling.equals("-"))){
			Operator o = leftMost.operator;
			if(leftMost == farLeft){//ie nothing to the left
				e1 = leftMost.leftChild;
				if(leftMost == rightMost){//ie nothing more to the right
					e2 = leftMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}else{//ie there is an operator more to the left
				e1 = parseBinOpExpr(farLeft, leftMost.leftOp);
				if(leftMost == rightMost){//ie there is nothing more to the right
					e2 = rightMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a more right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}
			return binExpr;
		}else{
			if(rightMost != leftMost){
				rightMost = rightMost.leftOp;
			}else{
				break;
			}
		}//end "+" or "-" tester
	
	}//end while loop
	
	leftMost = farLeft;
	
	while(rightMost != null){//multiplicative tester
		if(rightMost.operator.spelling.equals("*") || (rightMost.operator.spelling.equals("/"))){
			Operator o = leftMost.operator;
			if(leftMost == farLeft){//ie nothing to the left
				e1 = leftMost.leftChild;
				if(leftMost == rightMost){//ie nothing more to the right
					e2 = leftMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}else{//ie there is an operator more to the left
				e1 = parseBinOpExpr(farLeft, leftMost.leftOp);
				if(leftMost == rightMost){//ie there is nothing more to the right
					e2 = rightMost.rightChild;
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}else{//ie there is a more right operator
					e2 = parseBinOpExpr(leftMost.rightOp, rightMost);
					binExpr = new BinaryExpr(o, e1, e2, 
							new SourcePosition(e1.posn.start, e2.posn.finish));
				}
			}
			return binExpr;
		}else{
			if(rightMost != leftMost){
				rightMost = rightMost.leftOp;
			}else{
				break;
			}
		}//end "*" or "/" tester
	
	}//end while loop
	
	if(binExpr == null){
		parseError("Could not parse Bin Expression. Check operator.");
	}
	
	return binExpr;
		
		
}//end parseBinOpExpr
	

	////////////// HELPER FUNCTIONS ///////////////
	
	// start records the position of the start of a phrase.
	// This is defined to be the position of the first
	// character of the first token of the phrase.
	void start(SourcePosition sp){
		sp.start = token.position.start;
	}
	
	// finish records the position of the end of a phrase.
	// This is defined to be the position of the last
	// character of the last token of the phrase.
	void finish(SourcePosition sp){
		sp.finish = previousTokenPosition.finish;
	}
	
	/**
	 * SyntaxError is used to unwind parse stack when parse fails
	 *
	 */
	
	class SyntaxError extends Error{
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * report parse error and unwind call stack to start of parse
	 * @param e  string with error detail
	 * @throws SyntaxError
	 */
	private void parseError(String message) throws SyntaxError{
		errorReporter.reportError("Parse error: " + message + " at line " + token.position.start);
		throw new SyntaxError();
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
			previousTokenPosition = token.position;
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
		
	private class Signature{
		
		private boolean isPrivate = false;
		private boolean isStatic = false;
		private Type type;
		private String name;
		
	}
		
	
	

}
