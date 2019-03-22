package cmm.compiler.utillity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * functionCallValidator
 * @author Leslie Marxen
 * This class is used to determine if calls to functions are valid inside the program, 
 * meaning if a function is called it has to be defined somewhere in the program.
 */
public class FunctionCallValidator {

    private List<String> asm;
    private List<Function> defFunctions;
    private List<String> funSignatures;


    /**
     * Constuctor that takes the compiled assembly code and a list of defined functions.
     * @param asm the compiled assembly code one instruction per list element.
     * @param defFunctions a List (unsorted) that contains every function that has been defined within the program
     */
    public FunctionCallValidator(List<String> asm, List<Function> defFunctions){
        setAsm(asm);
        setdefFunctions(defFunctions);
        setFunSignatures(defFunctions
            .stream()
            .map(Function::toSignature)
            .collect(Collectors.toList())
        );
    }


    /**
     * Validates the compiled code. Collects various error messages in a List of strings.
     * If the list is empty, no error was found and the code is valid.
     * @return A list containing error messages if a function call has no defined function.
     */
    public List<String> validate(){
        List<String> errors = new ArrayList<>();
        for(int i = 0; i < asm.size(); i++){
            String line = asm.get(i);
            if(!isMethodCall(line)){
                continue;
            }
            String signature =  toSignature(line);
            if(!funSignatures.contains(signature)){
                String method = getSurroundingMethodSignature(i);
                errors.add(String.format("Undefined call to (%s) in method (%s)", signature, method));
            }
        }


        return errors;
    }

    /**
     * Determines wether an instruction calls a method.
     * @return true if a method is called, false if otherwise.
     */
    private boolean isMethodCall(String instruction){
        boolean b = false;
        b |= instruction.contains("invokevirtual");
        b |= instruction.contains("invokestatic");
        return b;
    }

    /**
     * Extracts the method signature from a methodcall.
     * @return The signature of the called method.
     */
    private String toSignature(String instruction){
        String[] tmp = instruction.trim().split("/");
        return tmp[tmp.length-1];
    }

    /**
     * Returns the method signature of the current scope.
     * @param line the line within the generated assembly.
     * @return the signature or "Global" of the surrounding method
     */
    private String getSurroundingMethodSignature(int line){
        for(int i = line; i >= 0; i--){
            String tmp = asm.get(i);
            if (tmp.contains(".method")){
                return getMethodSignature(tmp);
            }
        }
        return "Global";
    }

    /**
     * Returns the method signature of a .method definition
     * @param methodInstruction the .method line
     * @return The signature of the method definition.
     */
    private String getMethodSignature(String methodInstruction){
        String[] tmp = methodInstruction.trim().split(" ");
        return tmp[tmp.length-1];
    }


    /**
     * @return the asm
     */
    public List<String> getAsm() {
        return asm;
    }

    /**
     * @return the defFunctions
     */
    public List<Function> getDefFunctions() {
        return defFunctions;
    }

    /**
     * @param asm the asm to set
     */
    public void setAsm(List<String> asm) {
        this.asm = asm;
    }

    /**
     * @param defFunctions the defFunctions to set
     */
    public void setdefFunctions(List<Function> defFunctions) {
        this.defFunctions = defFunctions;
    }

    /**
     * @return the funSignatures
     */
    public List<String> getFunSignatures() {
        return funSignatures;
    }

    /**
     * @param funSignatures the funSignatures to set
     */
    public void setFunSignatures(List<String> funSignatures) {
        this.funSignatures = funSignatures;
    }
    
}