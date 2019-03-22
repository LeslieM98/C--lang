package cmm.compiler;

import java.util.*;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import cmm.compiler.generated.CmmParser.*;
import cmm.compiler.generated.*;
import cmm.compiler.exception.*;
import cmm.compiler.utillity.*;
import cmm.compiler.utillity.ScopeManager.Identifier;
import cmm.compiler.utillity.ScopeManager.Type;


public class ProgramVisitor extends CmmBaseVisitor<List<String>>{

    private final String programName;
    private boolean allreadyAddedClassDef;

    private ScopeManager scopes;

    // functionIdentifiers
    private List<Function> definedFunctions;


    public ProgramVisitor(String programName){
        super();
        scopes = new ScopeManager();
        definedFunctions = new ArrayList<>();
        this.programName = programName;
        allreadyAddedClassDef = false;
        definedFunctions.add(SYSOUT);
    }

    @Override
    public List<String> visit(ParseTree tree) {
        List<String> asm = new ArrayList<>();

        if(!allreadyAddedClassDef){
            // Inheritance and def
            asm.add(".class public " + programName);
            asm.add(".super java/lang/Object");

            // Default ctor
            asm.add(".method public <init>()V");
            asm.add("aload_0");
            asm.add("invokespecial java/lang/Object/<init>()V");
            asm.add("return");
            asm.add(".end method");

            // Program entry
            asm.add(".method public static main([Ljava/lang/String;)V");
            asm.add(".limit stack 20");
            asm.add(".limit locals 1");
            asm.add("new " + programName);
            asm.add("dup");
            asm.add(String.format("invokespecial %s/<init>()V", programName));
            asm.add(String.format("invokevirtual %s/%s",programName ,PROGRAM_ENTRY.toSignature()));
            asm.add("return");
            asm.add(".end method");
            allreadyAddedClassDef = true;
        }

        asm.addAll(super.visit(tree));
        return asm;
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
        scopes.putConstant(name, value);


        if(!successfull){
            throw new AllreadyDefinedException(tk, "Redefinition of constant");
        }

        return null;
    }
    
    @Override
    public List<String> visitVardec(VardecContext ctx) {
    	String name = ctx.dec.variableName.getText();
    	scopes.putVar(name);
    	return null;
    }
    

    @Override
    public List<String> visitVardecassign(VardecassignContext ctx) {
    	String name = ctx.dec.variableName.getText();
    	String value = ctx.val.getText();
    	

    	
    	return null;
    }



    /**
     * Resolves the type and name of the function parameters.
     * @param ctx the node of the function header.
     * @return A list containing all parameters in correct order.
     */
    List<Pair<String, NativeTypes>> determineParameters(Function_headerContext ctx) {
        List<Pair<String, NativeTypes>> result = new ArrayList<>();
        
        List<Generic_variable_declarationContext> paramDecs = ctx.children.stream().
            filter(x -> x instanceof Generic_variable_declarationContext).
            map(x -> (Generic_variable_declarationContext) x).
            collect(Collectors.toList());

        for (Generic_variable_declarationContext x : paramDecs) {
            result.add(new Pair<String,NativeTypes>(x.IDENTIFIER().getText(), NativeTypes.NUM));
        }

        return result;
    }


    private static final Function PROGRAM_ENTRY = new Function(NativeTypes.VOID, "main", List.of());
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

        // Determine parameters
        List<Pair<String, NativeTypes>> params = determineParameters(ctx.function_header());

        // Assemble function
        Function f = new Function(retType, name, params);
        if(definedFunctions.contains(f)){
            throw new AllreadyDefinedException(ctx.function_header().functionName, "Function allready defined");
        }
        definedFunctions.add(f);
        scopes.createLocalScope(f);
        scopes.switchContext(f);

        // Add parameters as local variables.
        scopes.putVar("this_ptr"); // create this ptr
        params.forEach(x -> scopes.putVar(x.getLeft()));

        // Compile body
        List<String> functionBody = visit(ctx.function_body());

        // Resolve local variable count
        int localsCount = scopes.getLocals(f).size();

        // Generate Jasmin            

        StringBuilder methodHead = new StringBuilder()
            .append(".method ")
            .append("public ")
            .append(f.toSignature());


        asm.add(methodHead.toString());
        asm.add(".limit stack " + (functionBody.size()));
        asm.add(".limit locals " + localsCount);
        asm.add("");

        asm.addAll(functionBody);
        asm.add((f.getReturnType() == NativeTypes.NUM) ? "ireturn" : "return");

        asm.add(".end method");
        scopes.switchToGlobalContext();

        return asm;
    }

    /**
     * Finds the parent statementcontext of a given context.
     * @param ctx A context.
     * @return The parentstatement of ctx or null if not existing.
     */
    private StatementContext getParentStatement(RuleContext ctx){
        RuleContext parent = ctx.parent;
        if(parent instanceof ProgramContext){
            return null;
        }
        if(parent instanceof StatementContext){
            return (StatementContext) parent;
        }
        return getParentStatement(ctx);
    }

    /**
     * Determins wether the functioncall is used as void or num return.
     * @param ctx The called function.
     * @return true if functions returns a value, false otherwise.
     */
    private boolean hasReturn(Function_callContext ctx){
        StatementContext stmnt = getParentStatement(ctx);
        if(stmnt == null){
            return false;
        }

        long numberOfVarDecs =  ctx.children
            .stream()
            .filter(x -> x instanceof Variable_declarationContext)
            .count();

        return numberOfVarDecs == 1;
    }

    /**
     * Determines the call arguments of a function call. 
     * @param ctx the called function
     * @return A list of Expression, Type pairs containing the arguments in oder from left to right.
     */
    List<Pair<ExpressionContext, NativeTypes>> determineArguments(Function_callContext ctx){
        List<Pair<ExpressionContext, NativeTypes>> result = new ArrayList<>();

        for (ExpressionContext x : ctx.expression_list().expressions) {
            result.add(new Pair(x, NativeTypes.NUM));
        }
        
        return result;
    }


    /**
     * Predefined function for printing to stdout.
     */
    private static final Function SYSOUT = new Function(NativeTypes.VOID, "println", List.of(new Pair("n", NativeTypes.NUM)));
    /**
     * If a function call was found this function determines what function was called based on the context. 
     * It differs between returning/non-returning functions and the parametercount if any
     */
    @Override
    public List<String> visitFunction_call(Function_callContext ctx) {
        List<String> asm = new ArrayList<>();

        List<Pair<ExpressionContext, NativeTypes>> args = determineArguments(ctx);
        List<Pair<String, NativeTypes>> rawArgs = args.stream()
            .map(x -> new Pair<>(x.getLeft().getText(), x.getRight()))
            .collect(Collectors.toList());

        NativeTypes returnValue = NativeTypes.VOID;
        if(hasReturn(ctx)){
            returnValue = NativeTypes.NUM;
        }

        Function f = new Function(returnValue, ctx.IDENTIFIER().getText(), rawArgs);

        StringBuilder functionCall = new StringBuilder("invokevirtual ");
            
        if(f.equals(SYSOUT)){
            functionCall.append("java/io/PrintStream/println(I)V");
            asm.add("getstatic java/lang/System/out Ljava/io/PrintStream;");
        } else {
            functionCall
                .append(programName + "/")
                .append(f.toSignature());
            asm.add("aload_0"); // push this ptr
        }
        


        asm.addAll(
            args.stream()
                .map(Pair::getLeft)
                .map(this::visit)
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
        );
        asm.add(functionCall.toString());
        

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
            case "==" : return "ifeq";
            case "!=" : return "ifne";
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
        asm.add("isub");    

        String trueL, doneL;
        trueL = "EqBranch" + eqCounter;
        doneL = "EqualFinish" + eqCounter;
        eqCounter++;

        String instruction = determineEqualityOperation(ctx.operator.getText());

        asm.add(instruction + " " + trueL);
        asm.add("iconst_0");
        asm.add("goto " + doneL);
        asm.add(trueL + ":");
        asm.add("iconst_1");
        asm.add(doneL + ":");


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
        notCounter++;
        
        asm.add("ifeq " + notL);
        asm.add("iconst_0");
        asm.add("goto " + doneL);
        asm.add(notL + ":");
        asm.add("iconst_1");
        asm.add(doneL + ":");


        return asm;
    }





     /**
     * Used to transform a conjunction operator to a JVM instruction. 
     * Can only transform && , ||.
     * @param operator A conjunctio operator mentiioned above.
     * @return A corresponding JVM instruction.
     */
    private String determineConjunctionOperation(String operator){
        switch(operator){
            case "&&" : return "iand";
            case "||" : return "ior";
            default  : return  null ;
        }
    }

    /** Used to identify jumplabels in the conjunction operation. */
    private int conjunctionCounter;
    /**
     * Treats both operands as boolean values meaning 0 = false and non 0 = true
     * performs a basic AND or OR operation pushing either a 0.0 or 1.0 double on the stack
     */
    @Override
    public List<String> visitConjunction(ConjunctionContext ctx) {
        String branchL, doneL;
        branchL = "ConjBranch" + conjunctionCounter;
        doneL   = "ConjDone"   + conjunctionCounter;

        conjunctionCounter++;

        String instruction = determineConjunctionOperation(ctx.operator.getText());

        List<String> asm = visit(ctx.left);
        asm.add("iconst_0");
        asm.add("isub");   // compare left with 0

        asm.addAll(visit(ctx.right));
        asm.add("iconst_0");
        asm.add("isub");   // compare right with 0

        asm.add(instruction);
        asm.add("ifne " + branchL);
        asm.add("iconst_0");
        asm.add("goto " + doneL);
        asm.add(branchL + ":");
        asm.add("iconst_1");
        asm.add(doneL + ":");

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

        relationalCounter++;

        String instruction = determineRelationalOperation(ctx.operator.getText());
        
        asm.add("isub");
        asm.add(instruction + " " + relationalL);
        asm.add("iconst_0");
        asm.add("goto " + relationalDoneL);
        asm.add(relationalL + ":");
        asm.add("iconst_1");
        asm.add(relationalDoneL + ":");

        return asm;
    }



    @Override
    public List<String> visitVariable(VariableContext ctx) {
        List<String> asm = new ArrayList<>();
        
        Identifier id = scopes.get(ctx.getText());

        if(id.getType() == Type.CONSTANT){
            asm.add(id.getValue());
        } else {

        }






        return asm;
    }


    @Override
    public List<String> visitNumber(NumberContext ctx) {
        List<String> asm = new ArrayList<>();

        int value = Integer.parseInt(ctx.number.getText());

        asm.add("ldc " + value);


        return asm;
    }

    @Override
    public List<String> visitPlus(PlusContext ctx) {
    	List<String> asm = new ArrayList<>();
    	asm.addAll(visit(ctx.left)); // evaluate left expression onto the stack
    	asm.addAll(visit(ctx.right)); // evaluate right expression onto the stack
    	asm.add("iadd"); // add left and right
    	return asm;
    }

    @Override
    public List<String> visitMinus(MinusContext ctx) {
    	List<String> asm = new ArrayList<>();
    	asm.addAll(visit(ctx.left)); // evaluate left expression onto the stack
    	asm.addAll(visit(ctx.right)); // evaluate right expression onto the stack
    	asm.add("isub"); // subtract right from left
    	return asm;
    }
    
    @Override
    public List<String> visitDivision(DivisionContext ctx) {
    	List<String> asm = new ArrayList<>();
    	asm.addAll(visit(ctx.left)); // evaluate left expression onto the stack
    	asm.addAll(visit(ctx.right)); // evaluate right expression onto the stack
    	asm.add("idiv"); // divide left by right
    	return asm;
    }
    
    @Override
    public List<String> visitMultiplication(MultiplicationContext ctx) {
    	List<String> asm = new ArrayList<>();
    	asm.addAll(visit(ctx.left)); // evaluate left expression onto the stack
    	asm.addAll(visit(ctx.right)); // evaluate right expression onto the stack
    	asm.add("imul"); // divide left by right
    	return asm;
    }

    /**
     * @return the definedFunctions
     */
    public List<Function> getDefinedFunctions() {
        return definedFunctions;
    }
}