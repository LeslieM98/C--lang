package cmm.compiler.exception;

import org.antlr.v4.runtime.Token;

/**
 * This exception gets thrown if a function with return 
 * type contains no return statement.
 * @author Leslie Marxen
 */


public class NoReturnException extends CompileException {

    /**
     * Constructor for NoReturnException.
     * {@code tk} should point to the token that 
     * contains the name of the function causing failure.
     * @param tk The functiontoken that caused the exception.
     * @param msg A custom message that will be shown.
     */
    public NoReturnException(Token tk, String msg){
        super(tk, msg);
    }

    /**
     * Returns a prefix for the errormessage.
     * @return {@code No return statement in function(functionName)}
     */
    @Override
    public String getPrefix() {
        return "No return statement in function(" + tk.getText() + ")";
    }
}