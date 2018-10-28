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

    // functionIdentifiers
    private List<Function> definedFunctions;


    public ProgramVisitor(){
        super();
        scopes = new ScopeManager();
        definedFunctions = new ArrayList<>();

        // specific countervars for visit functions
        // eqCounter = 0;
    }

    private NativeTypes toNativeTypes(String in){
        switch(in){
            case "void":
                return NativeTypes.VOID;
            case "num":
                return NativeTypes.NUM;
            default:
                return null;
        }
    }


    /**
     * Basically every not implemented visit method returns a call to this method.
     */
    @Override
    protected List<String> defaultResult() {
        return null;
    }

    /**
     * When multiple children are visited in a default 
     * implemented method, they get aggregated using this function.<br>
     * Basically appending {@code aggregate} to a new List and then 
     * appending {@code nextResult} to that new List.
     */
    @Override
    protected List<String> aggregateResult(List<String> aggregate, List<String> nextResult) {
        if(aggregate == null && nextResult == null){
            return null;
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
    // TODO: locals and stacksize
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




    /**
     * Used to identify jumplabels in the equals visitor.
     */
    private int eqCounter;
    /**
     * Pushes 1 on the stack if both are equal, 0 if not.<br>
     * 
     * Assuming both operands are on the top of the stack.
     */
    // TODO: Optimize asm
    @Override
    public List<String> visitEquals(EqualsContext ctx) {
        List<String> asm = new ArrayList<>();

        String trueL, doneL;
        trueL = "EqTrue" + eqCounter;
        doneL = "EqualFinish" + eqCounter;

        asm.add("if_icmpeq " + trueL);
        asm.add("ldc 0");
        asm.add("goto " + doneL);
        asm.add(trueL + ":");
        asm.add("ldc 1");
        asm.add(doneL + ":");

        eqCounter++;

        return asm;
    }

    /**
     * Used to identify jumplabels in the not equals visitor.
     */
    private int neqCounter;
    /**
     * Pushes 1 on the stack if both are not equal, 1 if they are.<br>
     * 
     * Assuming both operands are on the top of the stack.
     */
    @Override
    public List<String> visitNotEquals(NotEqualsContext ctx) {
        List<String> asm = new ArrayList<>();

        String trueL, doneL;
        trueL = "NeTrue" + eqCounter;
        doneL = "NeDone" + eqCounter;

        asm.add("if_icmpne " + trueL);
        asm.add("ldc 0");
        asm.add("goto " + doneL);
        asm.add(trueL + ":");
        asm.add("ldc 1");
        asm.add(doneL + ":");

        neqCounter++;

        return asm;
    }


    /**
     * Used to identify jumplabels in the not operation.
     */
    private int notCounter;
    /**
     * Inverts a boolean value.<br>
     * 
     * Values equal to 0 will be transformed to a 1. Nonzero values will be transformed to 0.
     */
    @Override
    public List<String> visitNot(NotContext ctx) {
        List<String> asm = new ArrayList<>();

        String notL, doneL;
        notL = "Not"+notCounter;
        doneL = "NotDone"+notCounter;

        asm.add("ifeq " + notL);
        asm.add("ldc0");
        asm.add("goto " + doneL);
        asm.add(notL + ":");
        asm.add("ldc 1");
        asm.add(doneL + ":");

        return asm;
    }

}