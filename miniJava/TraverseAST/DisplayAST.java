package miniJava.TraverseAST;
import miniJava.AbstractSyntaxTrees.*;

/** AST traversal to display fully parenthesized form of the AST
 *    Inherits - nothing (null)
 *    Synthesizes - String holding display form of the subtree rooted at this node
 */

//TODO taken directly from miniArith
public class DisplayAST implements Visitor<Object, String> {

	@Override
	public String visitBinExpr(BinExpr expr, Object arg) {
		return "(" + expr.left.visit(this, null) 
		    	+ " " + expr.oper.spelling
		        + " " + expr.right.visit(this, null) + ")";
	}

	@Override
	public String visitNumExpr(NumExpr expr, Object arg) {
		return expr.num.spelling;
	}
	
	

}
