package miniJava.syntacticAnalysis;

/**
 *   TokenKind is a simple enum of the different kinds of tokens
 *   
 *   
 */

public enum TokenKind{LEFTBRACK, RIGHTBRACK, LEFTPARA, RIGHTPARA, LEFTBRACE, RIGHTBRACE,
						CLASSKEY, PUBLICKEY, PRIVATEKEY, STATICKEY, VOIDKEY, INTKEY, 
						BOOLEANKEY, THISKEY, TRUEKEY, FALSEKEY, IFKEY, ELSEKEY, WHILEKEY,
						NEWKEY, RETURNKEY, UNILOGICAL, BI_LOGICAL, ASSIGNOP, RELATIONOP,
						BI_ARITH, NEGATION, COMMENT, SEMICOLON, PERIOD, COMMA,
						ID, NUM, EOT, ERROR}
