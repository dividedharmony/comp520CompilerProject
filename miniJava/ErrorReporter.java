package miniJava;

/**
 * recieves reports of errors from scanner and parser
 * and holds a count of total errors for use in 
 * the compiler driver
 *
 */

public class ErrorReporter {
	
	private int numErrors;
	
	public ErrorReporter(){
		this.numErrors = 0;
	}
	
	public boolean hasErrors(){
		return (numErrors > 0);
	}
	
	public void reportError(String message){
		System.out.println(message);
		numErrors++;
	}
	public int getNumErrors(){
		return numErrors;
	}

}
