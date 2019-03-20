package cmm.compiler.exception;

import org.antlr.v4.runtime.Token;

/**
 * This exception gets thrown if any definable construct was allready defined.
 * F.e. Functions or constants.
 * 
 * @author Leslie Marxen
 */

public class UndefinedSymbolException extends CompileRuntimeException{

	private static final long serialVersionUID = 1331844359857598715L;

	/**
     * Constructor for AllreadyDefinedException.
     * {@code tk} should point to the token that 
     * contains the name of the causing failure.
     * @param tk The token that caused the exception.
     * @param msg A custom message that will be shown.
     */
    public UndefinedSymbolException(Token tk, String msg){
        super(tk, msg);
    }

    /**
     * Returns a prefix for the errormessage.
     * @return {@code Identifier Allready defined(identifier)}
     */
    @Override
    public String getPrefix() {
        return "Undefined symbol (" + tk.getText() + ")";
    }

}