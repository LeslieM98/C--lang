package cmm.compiler.utillity;

import java.util.*;

/**
 * This class exists for the purpose of managing variable/constants 
 * access in different scopes within a sourcecode file.<br>
 * {@code Current Scope} meaning all variables/constants a statement 
 * or expression can access in the current context.<br>
 * {@code Global Scope} meaning all variables/constants that are 
 * defined outside of any function.<br>
 * {@code Local Scope} meaning all variables/constants that are defined 
 * at a top level scope inside a function.<br>
 * {@code Temporary Scope} meaning all variables/constans that are 
 * defined within loop statements, if statements etc.
 * <br><br>
 * Temporary Scopes can be nested as deeply as needed but they are 
 * deleted at the time of leaving the scope. Temporary scopes act as 
 * a scope stack.
 * 
 * @author Leslie Marxen
 */


// TODO: needs testing
public class ScopeManager {
    private Map<String, String> globalConstants;
    private Map<String, String> globalVariables;

    private Map<String, String> currentConstants;    // Acts as pointer
    private Map<String, Integer> currentVariables;    // Acts as plointer

    private List<Map<String, String>> temporaryConstants;
    private List<Map<String, Integer>> temporaryVariables;

    private Map<Function, Map<String, String>> localConstantScopes;
    private Map<Function, Map<String, Integer>> localVariableScopes;



    /**
     * Default ctor for a ScopeManager. Only initializes fields.
     */
    public ScopeManager(){
        globalConstants = new HashMap<>();
        globalVariables = new HashMap<>();

        currentConstants = null;
        currentVariables = null;

        temporaryConstants = new ArrayList<>();
        temporaryVariables = new ArrayList<>();

        localVariableScopes = new HashMap<>();
        localConstantScopes = new HashMap<>();
    }


    public Identifier get(String name){
        String value;

        value = globalConstants.get(name);
        if(value != null){
            return new Identifier(Scope.GLOBAL, Type.CONSTANT, name, value);
        }

        value = globalVariables.get(name);
        if(value != null){
            return new Identifier(Scope.GLOBAL, Type.VARIABLE, name, value);
        }

        if(currentConstants != null){
            value = currentConstants.get(name);
        }
        if(value != null){
            return new Identifier(Scope.LOCAL, Type.CONSTANT, name, value);
        }

        if(currentVariables != null){
            if(currentVariables.containsKey(name)){
                value = Integer.toString(currentVariables.get(name));
            }
        }
        if(value != null){
            return new Identifier(Scope.LOCAL, Type.VARIABLE, name, value);
        }


        if(temporaryConstants != null){
            for (Map<String, String> x : temporaryConstants) {
                value = x.get(name);
                if(value != null){
                    return new Identifier(Scope.TEMPORARY, Type.CONSTANT, name, value);
                }
            }
        }

        if(temporaryVariables != null){
            for(Map<String, Integer> x : temporaryVariables){
                if(x.containsKey(name)){
                    value = Integer.toString(x.get(name));
                    if(value !=  null){
                        return new Identifier(Scope.TEMPORARY, Type.VARIABLE, name, value);
                    }
                }
            }
        }


        return null;

    }

    public boolean createLocalScope(Function f){
        if(localConstantScopes.containsKey(f) || localVariableScopes.containsKey(f)){
            return false;
        }

        localConstantScopes.putIfAbsent(f, new HashMap<>());
        localVariableScopes.putIfAbsent(f, new HashMap<>());
        return true;
    }

    public boolean switchContext(Function f){
        if(f == null) {
            switchToGlobalContext();
            return true;
        }
        if(!localConstantScopes.containsKey(f) || !localVariableScopes.containsKey(f)){
            return false;
        }

        currentConstants = localConstantScopes.get(f);
        currentVariables = localVariableScopes.get(f);

        resetTemporary();

        return true;
    }

    public void switchToGlobalContext(){
        currentConstants = null;
        currentVariables = null;

        temporaryConstants = null;
        temporaryVariables = null;
    }

    private void resetTemporary(){
        temporaryConstants = null;
        temporaryVariables = null;
    }

    public int currentTemporaryScopeDepth(){
        if(temporaryConstants == null) return 0;
        return temporaryConstants.size();
    }

    public boolean enterTemporaryScope(){
        if(temporaryConstants == null || temporaryVariables == null){
            temporaryConstants = new ArrayList<>();
            temporaryVariables = new ArrayList<>();
        }
        temporaryConstants.add(new HashMap<>());
        temporaryVariables.add(new HashMap<>());
        return true;
    }

    public void leaveTemporaryScope(){
        if(currentTemporaryScopeDepth() == 1){
            resetTemporary();
        }
        if(currentTemporaryScopeDepth() != 0){
            temporaryConstants.remove(temporaryConstants.size() - 1);
            temporaryVariables.remove(temporaryVariables.size() - 1);
        }
    }

    
    public boolean putVar(String name){
        switch (currentScope()) {
            case GLOBAL:
                return putGlobVar(name);
            case LOCAL:
                return putLocalVar(name);
            case TEMPORARY:
                return putTemporaryVar(name);
            default:
                return false;
        }
    }

    private boolean putGlobVar(String name){
        if(get(name) != null) return false;
        return globalVariables.putIfAbsent(name, name) != null;
    }

    private boolean putLocalVar(String name){
        if(currentVariables == null) return false;
        if(get(name) != null) return false;

        for (int i = 0; i < Integer.MAX_VALUE - 1; i++) {
            if(!currentVariables.containsValue(i)){
                currentVariables.put(name, i);  
                return true;
            }
        }
        return false;
    }

    private boolean putTemporaryVar(String name){
        if(get(name) != null) return false;
        if(temporaryVariables.size() == 0) return false;

        boolean b;

        for (int i = 0; i < Integer.MAX_VALUE - 1; i++) {
            Map<String, Integer> tmp;
            b = !currentVariables.containsValue(i);
            if(b == false) continue;
            for (Map<String, Integer> x : temporaryVariables) {
                b &= !x.containsValue(i);
                if(b == false) break;
            }
            if(b == true){
                tmp = temporaryVariables.get(temporaryVariables.size() - 1);
                tmp.put(name, i);
                return true;
            }
        }
        return false;
    }


    public boolean putConstant(String name, String value){
        switch (currentScope()) {
            case GLOBAL:
                return putGlobConst(name, value);
            case LOCAL :
                return putLocalConst(name, value);
            case TEMPORARY :
                return putTemporaryConst(name, value);
            default:
                return false;
        }
    }

    private boolean putGlobConst(String name, String value){
        if(get(name) != null) return false;
        return globalConstants.putIfAbsent(name, value) == null;
    }

    private boolean putLocalConst(String name, String value){
        if(get(name) != null) return false;
        if(currentConstants == null) return false;

        return currentConstants.putIfAbsent(name, value) != null;
    }

    private boolean putTemporaryConst(String name, String value){
        if(get(name) != null) return false;
        if(temporaryConstants.size() == 0) return false;

        Map<String, String> tmp = temporaryConstants.get(temporaryConstants.size() - 1);
        
        return tmp.putIfAbsent(name, value) == null;
    }

    public Scope currentScope(){
        if(currentConstants == null){
            return Scope.GLOBAL;
        }
        if(currentConstants != null && temporaryConstants == null){
            return Scope.LOCAL;
        }
        return Scope.TEMPORARY;
    }



    /**
     * Used as reference to what was returned. And how to interpret the result.<br>
     * {@code VARIABLE} meaning the returned result is to interpret as either an 
     * identifier in the locals array or a name of the global class attribute.<br>
     * {@code CONSTANT} meaning the returned result is allready a ready-to-use 
     * value and has just to be substituted in the expression or statement.
     */
    public static enum Type{
        VARIABLE,
        CONSTANT;
    }

    public static enum Scope{
        GLOBAL,
        LOCAL,
        TEMPORARY;
    }

    public static class Identifier{
        private final Scope scope;
        private final Type type;
        private final String name;
        private final String value;
    
        private Identifier(Scope scope, Type type, String name, String value){
            if(scope == null || type == null || name == null || value == null){
                throw new IllegalStateException("Cannot assign nullvalue");
            }
            this.scope = scope;
            this.type = type;
            this.name = name;
            this.value = value;
        }
    
        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
    
        /**
         * @return the scope
         */
        public Scope getScope() {
            return scope;
        }
    
        /**
         * @return the type
         */
        public Type getType() {
            return type;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }
    
    
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder()
                .append(scope.toString()).append(":")
                .append(type.toString()).append(":")
                .append(name);
    
            return sb.toString();
        }
    
        @Override
        public boolean equals(Object obj) {
            if (obj != null && getClass() == obj.getClass()) {
                Identifier o = (Identifier)obj;
                boolean b;
                b  = o.getName().equals(name);
                b &= o.getScope() == scope;
                b &= o.getType() == type;
                b &= o.getValue() == value;
                
                return b;
            }
        return false;
        }
    
    
    } 
}