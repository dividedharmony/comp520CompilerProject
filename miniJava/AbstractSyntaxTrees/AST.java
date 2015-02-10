package miniJava.AbstractSyntaxTrees;

public abstract class AST {
	
	public abstract <Inh,Syn> Syn visit(Visitor<Inh,Syn> v, Inh o);

}
