package cmm.compiler;

import java.util.*;

import org.antlr.v4.runtime.Token;

import cmm.compiler.CmmParser.ConstdecContext;

import cmm.compiler.exception.*;

class ProgramVisitor extends CmmBaseVisitor<List<String>>{

    private HashMap<String, String> constTable;


    public ProgramVisitor(){
        super();
        constTable = new HashMap<>();
    }

    @Override
    protected List<String> defaultResult() {
        return new ArrayList<>();
    }

    String getConstant(String constName){
        return constTable.get(constName);
    }

    @Override
    protected List<String> aggregateResult(List<String> aggregate, List<String> nextResult) {
        if(aggregate == null && nextResult == null){
            return new ArrayList<>();
        }

        if(aggregate == null){
            return nextResult;
        }

        if(nextResult == null){
            return aggregate;
        }

        List<String> tmp = new ArrayList<>();

        tmp.addAll(aggregate);
        tmp.addAll(nextResult);

        return tmp;
    }

    // Context subroutines

    @Override
    public List<String> visitConstdec(ConstdecContext ctx) {
        Token tk = ctx.dec.variableName;
        String name = ctx.dec.variableName.getText();
        String value = ctx.val.getText();

        if(constTable.putIfAbsent(name, value) != null){
            throw new AllreadyDefinedException(tk, "Redefinition of constant");
        }

        return null;
    }

}