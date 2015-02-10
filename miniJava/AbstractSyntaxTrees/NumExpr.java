package miniJava.AbstractSyntaxTrees;

import miniJava.syntacticAnalysis.Token;

//TODO taken directly from miniArith

public class NumExpr extends Expr {
	public Token num;
	
	public NumExpr(Token num){
		this.num = num;
	}

	@Override
	public <Inh, Syn> Syn visit(Visitor<Inh, Syn> v, Inh arg) {
		return v.visitNumExpr(this, arg);
	}
	
	

}
