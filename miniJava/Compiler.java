package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.SyntacticalAnalyzer.*;

/**
 * 
 * @author David Harmon (dharmon)
 * COMP 520
 * miniJava scanner and parser
 * miniJava Grammar:
 *
 */

public class Compiler {
	
	/**
	 * @param args  if no args provided parse from keyboard input
	 *              else args[0] is name of file containing input to be parsed  
	 */

	public static void main(String[] args) {
		
		InputStream inputStream = null;
		if(args.length == 0){
			System.out.println("Enter Expression: ");
			inputStream = System.in;
		}
		else{
			try{
				inputStream = new FileInputStream(args[0]);
			}catch(FileNotFoundException e){
				System.out.println("Input file '" + args[0] + "' not found");
				System.exit(1);
			}
		}
		
		ErrorReporter errorReporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, errorReporter);
		Parser parser = new Parser(scanner, errorReporter);
		
		System.out.println("Beginning scan and parse...");
		parser.parse();
		System.out.println("Scan and parse complete!");
		
		if(errorReporter.hasErrors()){
			System.out.println("INVALID Statement.");
			System.exit(4);
		}else{
			System.out.println("Valid Statement");
			System.exit(0);
		}
		
		
		

	}

}

