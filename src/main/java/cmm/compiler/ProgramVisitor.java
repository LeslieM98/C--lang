package cmm.compiler;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;

import org.antlr.v4.runtime.Token;

import cmm.compiler.CmmParser.*;
import cmm.compiler.exception.*;
import cmm.compiler.utillity.*;

class ProgramVisitor extends CmmBaseVisitor<List<String>>{

    // constant identifiers
    private Map<String, String> constTable;

    // Scopes
    private Map<String, HashMap<String, String>> scopesTable;   // Scopename  -> ScopeTable
    private final Map<String, String> globalScope;              // Identifier -> field id
    private Map<String, String> currentScope;                   // Identifier -> local id

    // functionIdentifiers
    private Set<Function> definedFunctions;


    public ProgramVisitor(){
        super();
        constTable = new HashMap<>();
        globalScope = new HashMap<>();
    }

    private NativeTypes toNativeTypes(String in){
        if(in.equals("void")){
            return NativeTypes.VOID;
        } else if(in.equals("num")){
            return NativeTypes.NUM;
        } else if(in.equals("char")){
            return NativeTypes.CHARACTER;
        }
        return null;
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

    @Override

    public List<String> visitFunction_definition(Function_definitionContext ctx) {
        
        List<String> asm = visit(ctx.function_header());
        String param = ctx.function_header().getChild(ctx.function_header().getChildCount() - 2).getText();

        asm.addAll(visit(ctx.function_body()));

        if(toNativeTypes(param) != NativeTypes.VOID){

        }
        return asm;
    }

}