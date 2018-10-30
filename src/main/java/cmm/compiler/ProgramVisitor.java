package cmm.compiler;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;

import org.antlr.v4.runtime.Token;

import cmm.compiler.generated.CmmParser.*;
import cmm.compiler.generated.*;
import cmm.compiler.exception.*;
import cmm.compiler.utillity.*;
import jas.IincInsn;

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
        return new ArrayList<>();
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
    public List<String> visitPrintln(PrintlnContext ctx) {
        String variableName = ctx.variableName.getText();
        String value = ctx.value.getText();
        // TODO Implement Jasmin Intstructions to return
        return super.visitPrintln(ctx);
    }


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
        } else {
            if(scopes.currentTempScopeDepth() == 0){
                // Try to add in local scope
                successfull &= scopes.addLocalConstant(name, Integer.parseInt(value));
            } else {
                // Try to add in current temporary scope
                successfull &= scopes.addTemporaryConstant(name, Integer.parseInt(value));
            }
        }

        if(!successfull){
            throw new AllreadyDefinedException(tk, "Redefinition of constant");
        }

        return null;
    }



    private static final Function PROGRAM_ENTRY = new Function(NativeTypes.VOID, "main");
    /**
     * Breaks up a function definition into name, return type, parametercount, 
     * parametertypes. Checks wether it is allready defined. Creates a new 
     * JVM ASM method with correctly inserted function metadata. 
     * Evaluates the sourcecode inside the function within the correct scope context.<br>
     * <br>
     * If a function was recognized having the signature {@code void main(){...}} it 
     * is considered as programentry and will be compiled as such.
     * 
     * @throws AllreadyDefinedException If the function was allready defined.
     */
    // TODO: Check for correct return
    // TODO: locals and stacksize
    // TODO: insert parameters correctly
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
        scopes.createLocalScope(f);
        scopes.switchContext(f);

        // Generate Jasmin
        StringBuilder methodHead = new StringBuilder()
            .append(".method ")
            .append("public ")
            .append("static ")
            .append(name)
            .append("(");
            if(f.equals(PROGRAM_ENTRY)){
                methodHead.append("[Ljava/lang/String;");
            } else {
                methodHead.append((paramcount == 0) ? "V" : "");

                // Append parameters
                for(int i = 0; i < paramcount; i++){
                    methodHead.append("I");
                }
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
     * Used to transform an equality operator to a JVM instruction. 
     * Can only transform ==, !=.
     * @param operator An equality operator mentiioned above.
     * @return A corresponding JVM instruction.
     */
    private String determineEqualityOperation(String operator){
        switch(operator){
            case "==" : return "if_icmpeq";
            case "!=" : return "if_icmpne";
            default  : return  null ;
        }
    }


    /**
     * Used to identify jumplabels in the equality visitor.
     */
    private int eqCounter;
    /**
     * Performs an equality operation. Pops 2 operands off the stack.
     * Allways results in either 1 or 0 depending on which operation. <br>
     * {@code ==}: push 1 if both are equal, 0 if both are different. <br>
     * {@code !=}: push 1 if both are different, 0 if both are equal. 
     * 
     */
    // TODO: Optimize asm
    @Override
    public List<String> visitEquality(EqualityContext ctx) {
        // Load left side of operation to stack
        List<String> asm = visit(ctx.left);

        // Load right side of operation to stack
        List<String> asmRight = visit(ctx.right);
        asm.addAll(asmRight);        

        String trueL, doneL;
        trueL = "EqBranch" + eqCounter;
        doneL = "EqualFinish" + eqCounter;

        String instruction = determineEqualityOperation(ctx.operator.getText());

        asm.add("instruction " + trueL);
        asm.add("ldc 0");
        asm.add("goto " + doneL);
        asm.add(trueL + ":");
        asm.add("ldc 1");
        asm.add(doneL + ":");

        eqCounter++;

        return asm;
    }


    /**
     * Used to identify jumplabels in the not operation.
     */
    private int notCounter;
    /**
     * Inverts a boolean value.<br>
     * 
     * Assuming the operand is on top of the stack.
     * Values equal to 0 will be transformed to a 1 value. 
     * nonzero values will be transformed to 0.
     */
    @Override
    public List<String> visitNot(NotContext ctx) {
        // Load operand to stack
        List<String> asm = visit(ctx.expr);

        String notL, doneL;
        notL = "NotBranch" + notCounter;
        doneL = "NotDone" + notCounter;

        asm.add("ifeq " + notL);
        asm.add("ldc0");
        asm.add("goto " + doneL);
        asm.add(notL + ":");
        asm.add("ldc 1");
        asm.add(doneL + ":");

        return asm;
    }

    /**
     * Performs the bitwise AND operation.<br>
     * 
     * Assuming both operands are on top the stack. 
     * Performs a standard AND operation between 2 values. 
     * Works for boolean logic and integer logic.
     */
    @Override
    public List<String> visitAnd(AndContext ctx) {

        // Load left side of operation to stack
        List<String> asm = visit(ctx.left);

        // Load right side of operation to stack
        asm.addAll(visit(ctx.right));

        asm.add("iand");

        return asm;
    }

    /**
     * Performs the bitwise OR operation.<br>
     * 
     * Assuming both operands are on top the stack. 
     * Performs a standard OR operation between 2 values. 
     * Works for boolean logic and integer logic.
     */
    @Override
    public List<String> visitOr(OrContext ctx) {

        List<String> asm = visit(ctx.left);
        asm.addAll(visit(ctx.right));

        asm.add("ior");

        return asm;
    }


    /**
     * Used to transform a relational operator to a JVM isntruction. 
     * Can only transform <, >, <=, >=.
     * @param operator A relational operator mentiioned above.
     * @return A corresponding JVM instruction.
     */
    private String determineRelationalOperation(String operator){
        switch(operator){
            case "<" : return "iflt";
            case ">" : return "ifgt";
            case "<=": return "ifle";
            case ">=": return "ifge";
            default  : return  null ;
        }
    }

    /**
     * Used to identify jumplabels in a relational operation.
     */
    private int relationalCounter;
    /**
     * Performs a relational operation resulting in either 1 or 0. 
     * Relational operations contain {@code <, >, <=, >=}. <br>
     * 
     * Pops 2 integers off the stack.
     * If the expression evaluates to true a 1 will be pushed to the stack, otherwise a 0.
     */
    @Override
    public List<String> visitRelational(RelationalContext ctx) {
        List<String> asm = visit(ctx.left);
        asm.addAll(visit(ctx.right));

        String relationalL, relationalDoneL;
        relationalL = "relBranch" + relationalCounter;
        relationalDoneL = "relDone" + relationalCounter;

        String instruction = determineRelationalOperation(ctx.operator.getText());
        
        asm.add("isub");
        asm.add(instruction + " " + relationalDoneL);
        asm.add("ldc 0");
        asm.add("goto " + relationalDoneL);
        asm.add(relationalL + ":");
        asm.add("ldc 1");
        asm.add(relationalDoneL + ":");

        relationalCounter++;

        return asm;
    }



    @Override
    public List<String> visitVariable(VariableContext ctx) {
        List<String> asm = new ArrayList<>();
        
        Pair<ScopeManager.Type, String> glob = scopes.getGlobal(ctx.variableName.getText());
        Pair<ScopeManager.Type, Integer> loc = scopes.get(ctx.variableName.getText());

        if(glob != null){
            switch(glob.getLeft()){
                case CONSTANT: 
                    asm.add("ldc " + glob.getRight());
                    break;
            }
        } else if(loc != null){
            switch(loc.getLeft()){
                case CONSTANT:
                    asm.add("ldc " + loc.getRight());
                    break;
            }

        } else {

        }






        return asm;
    }




}