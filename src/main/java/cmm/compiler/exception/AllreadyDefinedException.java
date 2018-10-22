package cmm.compiler.exception;

import org.antlr.v4.runtime.Token;

public class AllreadyDefinedException extends CompileException{

    public AllreadyDefinedException(Token tk, String msg){
        super(tk, msg);
    }

    @Override
    public String getPrefix() {
        return "Identifier Allready defined (" + tk.getText() + ")";
    }

}