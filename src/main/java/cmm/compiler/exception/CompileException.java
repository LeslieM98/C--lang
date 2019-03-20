package cmm.compiler.exception;

import org.antlr.v4.runtime.Token;

/**
 * A class that sets the base for any Compile Error rised by compiling a 
 * C-- source document. Every Compile Error has to extend this class.
 * This Exception is unchecked and does not need to be catched.
 * and potentially overwrite {@link #getPrefix()} and
 * 
 * @author Leslie Marxen
 */

public abstract class CompileException extends RuntimeException{
	
	private static final long serialVersionUID = -5044538189423039146L;

	/**
     * The token that most likely caused the Exception
     */
    protected final Token tk;

    /**
     * A custom message.
     */
    protected final String msg;

    /**
     * A Base class for any Compile-Error that can rise while compiling.
     * @param tk The token that caused the Exception.
     * @param msg A message that can be displayed.
     */
    public CompileException(Token tk, String msg){
        super(msg);
        this.tk = tk;
        this.msg = msg;
    }

    /**
     * Returns in what line the error was caused.
     * @return The line number that caused the error.
     */
    public int getLine(){
        return tk.getLine();
    }

    /**
     * Returns at what positon in line the error was caused.
     * @return Char in line.
     */
    public int getCharInLine(){
        return tk.getCharPositionInLine();
    }

    /**
     * Returns a prefix that will be put to the beginning of the prepared message.
     * This method has to be overwritten by and other subclass.
     * @return A prefix that will be put before the {@link getPreparedMessage} String.
     */
    public abstract String getPrefix();

    /**
     * This method returns a prepared message ready to output to the console. 
     * The format is {@code <PREFIX>(<LINENR>:<CHAR_IN_LINE>): <MESSAGE>}
     * @return A prepared message in format {@code <PREFIX>(<LINENR>:<CHAR_IN_LINE>): <MESSAGE>}
     */
    public String getPreparedMessage(){
        return getPrefix() + "(" + getLine() + ":" + getCharInLine() + "): " + getMessage();
    }

}