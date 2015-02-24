//Part of the PA2 rewrite

package miniJava;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.*;

/**
 * 
 * @author David Harmon (dharmon)
 * COMP 520
 * miniJava scanner and parser
 * and now AST
 *
 */

public class Compiler {
	
	private static Scanner scanner;
    private static Parser parser;
    private static ErrorReporter reporter;
	
	@SuppressWarnings("unused")
	public static void compile(String sourceName){
		System.out.println("********** " +
                "miniJava Compiler" +
                " **********");

		System.out.println("Syntactic Analysis ...");
		SourceFile source = new SourceFile(sourceName);

		if (source == null) {
			System.out.println("Can't access source file " + sourceName);
			System.exit(1);
		}
		
		reporter = new ErrorReporter();
		scanner = new Scanner(source, reporter);
		parser = new Parser(scanner, reporter);
		AST ast = null;
		
		System.out.println("Beginning scan and parse...");
		ast = parser.parse();
		System.out.println("Scan and parse complete!");
		
		
		
		if(reporter.hasErrors()){
			System.out.println("INVALID Statement.");
			System.exit(4);
		}else{
			System.out.println("Valid Statement");
			
			// traverse AST to construct explicitly parenthesized text representation
			ASTDisplay display = new ASTDisplay();
			display.showTree(ast);
	        
			System.exit(0);
		}
		
	}// end compiler method
	
	public static void main(String[] args){
		
		if (args.length != 1) {
            System.out.println("Usage: tc filename");
            System.exit(1);
        }

        String sourceName = args[0];
        compile(sourceName);
		
		
	}

}
