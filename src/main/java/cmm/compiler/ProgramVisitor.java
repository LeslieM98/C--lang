package cmm.compiler;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;

import org.antlr.v4.runtime.Token;

import cmm.compiler.generated.CmmParser.*;
import cmm.compiler.generated.*;
import cmm.compiler.exception.*;
import cmm.compiler.utillity.*;

public class ProgramVisitor extends CmmBaseVisitor<List<String>>{

    ScopeManager scopes;

    // constant identifiers
    // private Map<String, String> constTable;

    // Scopes
    // private Map<String, HashMap<String, String>> scopesTable;   // Scopename  -> ScopeTable
    // private final Map<String, String> globalScope;              // Identifier -> field id
    // private Map<String, String> currentScope;                   // Identifier -> local id

    // functionIdentifiers
    private List<Function> definedFunctions;


    public ProgramVisitor(){
        super();
        // constTable = new HashMap<>();
        // globalScope = new HashMap<>();
        scopes = new ScopeManager();
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


    /**
     * As soon as a const declaration occurs, the righthand side of 
     * the asignment gets treated as a string literal and gets added 
     * to the const table in the correct scope.
     * 
     * @throws AllreadyDefinedException If there is any constant or variable 
     *          accessible in the current scope this exception will be thrown.
     */
    @Override
    public List<String> visitConstdec(ConstdecContext ctx) {
        Token tk = ctx.dec.variableName;
        String name = ctx.dec.variableName.getText();
        String value = ctx.val.getText();

        boolean successfull = true;

        // Try to add in global scope
        if(!scopes.inLocalScope()){
            successfull &= scopes.addGlobalConstant(name, value);
        }
        
        if(scopes.currentTempScopeDepth() == 0){
            // Try to add in local scope
            successfull &= scopes.addLocalConstant(name, Integer.parseInt(value));
        } else {
            // Try to add in current temporary scope
            successfull &= scopes.addTemporaryConstant(name, Integer.parseInt(value));
        }

        if(!successfull){
            throw new AllreadyDefinedException(tk, "Redefinition of constant");
        }

        return null;
    }

    /**
     * Breaks up a function definition into name, return type, parametercount, 
     * parametertypes. Checks wether it is allready defined. Creates a new 
     * JVM ASM method with correctly inserted function metadata. 
     * Evaluates the sourcecode inside the function within the correct scope context.
     * 
     * @throws AllreadyDefinedException If the function was allready defined.
     */
    // TODO: Check for correct return
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
        List<Pair<String, NativeTypes>> params = new ArrayList<>();
        Pair<String, NativeTypes> tmpPair;
        Generic_variable_declarationContext c;
        for(int i = 0; i/2 < paramcount; i+=2){
            c = ctx.function_header().getChild(Generic_variable_declarationContext.class, 3 + i);

            tmpPair = new Pair<>(
                c.variableName.getText(),
                toNativeTypes(c.TYPE().getText())
            );

            if(params.contains(tmpPair)){
                throw new AllreadyDefinedException(c.variableName, "Param allready defined");
            }

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
        scopes.createLocalScope(name);
        scopes.switchContext(name);

        StringBuilder methodHead = new StringBuilder()
            .append(".method ")
            .append("public ")
            .append("static ")
            .append(name)
            .append("(")
            .append((paramcount == 0) ? "V" : "");

        // Append parameters
        for(int i = 0; i < paramcount; i++){
            methodHead.append("I");
        }

        methodHead.append(")")
            .append(f.getReturnType() == NativeTypes.VOID ? "V" : "I");

        asm.add(methodHead.toString());

        asm.addAll(visit(ctx.function_body()));

        asm.add(".end method");
        scopes.switchToGlobalContext();

        return asm;
    }
}