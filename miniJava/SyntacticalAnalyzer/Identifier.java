package miniJava.SyntacticalAnalyzer;

import java.util.ArrayList;


public class Identifier {
	
	public enum ID_Kind{CLASS, INSTANCE, INT, BOOLEAN, INTARRAY, INSTANCEARRAY,
						METHOD, VOID};
	
	public String spelling;
	public ID_Kind holdsThisValue;
	public boolean isPublic;
	public boolean isStatic;
	public ID_Kind returnType;
	public ArrayList<ID_Kind> parameterList;
	
	public Identifier(String spelling, ID_Kind holdsThisValue){
		this.spelling = spelling;
		this.holdsThisValue = holdsThisValue;
		//By default, an identifier is public and not static
		this.isPublic = true;
		this.isStatic = false;
		//unless set as a Method Identifier, returnType and parameterList is null
		this.returnType = null;
		this.parameterList = null;
	}

}
