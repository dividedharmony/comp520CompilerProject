package miniJava.SyntacticAnalyzer;

public class SourcePosition {

	  public int start, finish;

	  public SourcePosition () {
	    start = 0;
	    finish = 0;
	  }

	  public SourcePosition (int s, int f) {
	    start = s;
	    finish = f;
	  }
	  public SourcePosition(SourcePosition s){
		  this.start = s.start;
		  this.finish = s.finish;
	  }

	  public String toString() {
	    return "(" + start + ", " + finish + ")";
	  }
	}
