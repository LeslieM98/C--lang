package cmm.compiler.exception;

import org.antlr.v4.runtime.Token;

class NoReturnException extends AllreadyDefinedException {
    public NoReturnException(Token tk, String msg){
        super(tk, msg);
    }

    @Override
    public String getPrefix() {
        return "No return statement in function(" + tk.getText() + ")";
    }
}