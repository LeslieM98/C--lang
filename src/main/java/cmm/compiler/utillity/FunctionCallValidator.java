package cmm.compiler.utillity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * functionCallValidator
 */
public class FunctionCallValidator {

    private List<String> asm;
    private List<Function> defFunctions;
    private List<String> funSignatures;


    public FunctionCallValidator(List<String> asm, List<Function> defFunctions){
        setAsm(asm);
        setdefFunctions(defFunctions);
        setFunSignatures(defFunctions
            .stream()
            .map(Function::toSignature)
            .collect(Collectors.toList())
        );
    }


    public List<String> validate(){
        List<String> errors = new ArrayList<>();
        for(int i = 0; i < asm.size(); i++){
            String line = asm.get(i);
            if(!isMethodCall(line)){
                continue;
            }
            String signature =  toSignature(line);
            if(!funSignatures.contains(signature)){
                String method = getMethodSignatureFromLine(i);
                errors.add(String.format("Undefined call to (%s) in method (%s)", signature, method));
            }
        }


        return errors;
    }

    private boolean isMethodCall(String instruction){
        boolean b = false;
        b |= instruction.contains("invokevirtual");
        b |= instruction.contains("invokestatic");
        return b;
    }

    private String toSignature(String instruction){
        String[] tmp = instruction.trim().split("/");
        return tmp[tmp.length-1];
    }

    private String getMethodSignatureFromLine(int line){
        for(int i = line; i >= 0; i--){
            String tmp = asm.get(i);
            if (tmp.contains(".method")){
                return getMethodSignature(tmp);
            }
        }
        return null;
    }

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