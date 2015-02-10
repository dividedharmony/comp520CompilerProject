package miniJava.AbstractSyntaxTrees;

/**
 * An implementation of the Visitor interface provides a method visitX
 * for each non-abstract AST class X.  
 */

public interface Visitor<Inh, Syn> {
	
	//Expressions TODO directly from Arith
	public Syn visitBinExpr(BinExpr expr, Inh arg);
	public Syn visitNumExpr(NumExpr expr, Inh arg);
	

}
