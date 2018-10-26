package cmm.compiler;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.*;

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
    private List<Function> definedFunctions;


    public ProgramVisitor(){
        super();
        constTable = new HashMap<>();
        globalScope = new HashMap<>();
        definedFunctions = new ArrayList<>();
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
        List<String> asm = new ArrayList<>();

        // Determine returntype and name
        String name = ctx.function_header().functionName.getText();
        NativeTypes retType = toNativeTypes(ctx.function_header().ret.getText());

        // Determine parametercount
        int paramcount = ctx.function_header().getChildCount();
        paramcount -= 4; // - num, name, (, )
        paramcount -= paramcount/2; // Extract ',' count

        // Determine parameters
        // TODO: check if multiple params have the same name
        List<Pair<String, NativeTypes>> params = new ArrayList<>();
        Generic_variable_declarationContext c;
        for(int i = 0; i/2 < paramcount; i+=2){
            c = ctx.function_header().getChild(Generic_variable_declarationContext.class, 3 + i);
            params.add(new Pair<>(
                c.variableName.getText(),
                toNativeTypes(c.TYPE().getText())
            ));
        }

        // Assemble function
        Function f = new Function(retType, name, params);
        if(definedFunctions.contains(f)){
            throw new AllreadyDefinedException(ctx.function_header().functionName, "Function allready defined");
        }
        definedFunctions.add(f);



        return asm;
    }

}