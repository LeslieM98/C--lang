package cmm.compiler.utillity;

import java.util.*;

import org.antlr.v4.runtime.misc.Pair;


// TODO: needs testing
public class ScopeManager {
    private Map<String, String> globalConstants;
    private Map<String, String> globalVariables;

    private Map<String, Integer> currentConstants;    // Acts as pointer
    private Map<String, Integer> currentVariables;    // Acts as plointer

    private List<Map<String, Integer>> temporaryConstants;
    private List<Map<String, Integer>> temporaryVariables;

    private Map<String, Map<String, Integer>> localConstantScopes;
    private Map<String, Map<String, Integer>> localVariableScopes;



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

    
    /**
     * Checks if given variable/constant is usable in the current Scope.
     * Returns the {@see Type} of the identifier or null.
     * @param identifier The name of the variable/constant.
     * @return the type or null if not accessible.
     */
    // TODO: looks ugly
    public Type accessible(String identifier){
        Type ret;

        ret = globalConstants.get(identifier) == null ? Type.CONSTANT : null;
        if(ret != null) return ret;

        ret = globalVariables.get(identifier) == null ? Type.VARIABLE : null;
        if(ret != null) return ret;

        if(currentConstants != null){
            ret = currentConstants.get(identifier) == null ? Type.CONSTANT : null;
            if(ret != null) return ret;
        }

        if(currentVariables != null){
            ret = currentVariables.get(identifier) == null ? Type.VARIABLE : null;
            if(ret != null) return ret;
        }

        for(Map<String, Integer> i : temporaryConstants){
            ret = i.containsKey(identifier) ? Type.CONSTANT : null;
            if(ret != null) break;
        }
        if(ret != null) return ret;

        for(Map<String, Integer> i : temporaryVariables){
            ret = i.containsKey(identifier) ? Type.VARIABLE : null;
            if(ret != null) break;
        }
        return ret;
    }

    /**
     * Returns a Pair of <{@see Type}, Integer> containing information about the identifier.
     * Checks if the given identifier is accessible from the current scope.
     * Returns null if not accessible.
     * @param identifier The identifier or either a Var or Const.
     * @return Information about given identifier, or null if not accessible.
     */
    public Pair<Type, Integer> get(String identifier){
        Pair<Type, Integer> ret = null;

        ret = getLocal(identifier);
        if(ret != null) return ret;

        ret = getTemporary(identifier);
        return ret;
    }

    public Pair<Type, String> getGlobal(String identifier){

        String tmp = globalConstants.get(identifier);

        if(tmp != null){
            return new Pair<>(Type.CONSTANT, tmp);
        }

        tmp = globalVariables.get(identifier);
        if(tmp != null){
            return new Pair<>(Type.VARIABLE, tmp);
        }

        return null;
    }

    private Pair<Type, Integer> getLocal(String identifier){

        Integer tmp = currentConstants.get(identifier);

        if(tmp != null){
            return new Pair<>(Type.CONSTANT, tmp);
        }

        tmp = currentVariables.get(identifier);
        if(tmp != null){
            return new Pair<>(Type.VARIABLE, tmp);
        }

        return null;
    }

    private Pair<Type, Integer> getTemporary(String identifier){
        Integer tmp;

        for(Map<String, Integer> i : temporaryConstants){
            tmp = i.get(identifier);
            if(tmp != null) return new Pair<>(Type.CONSTANT, tmp);
        }

        for(Map<String, Integer> i : temporaryVariables){
            tmp = i.get(identifier);
            if(tmp != null) return new Pair<>(Type.VARIABLE, tmp);
        }

        return null;
    }

    /**
     * Sets the current Scope to a local scope.
     * Tries to set the current Scope to a different local scope.
     * Returns false if it's not possible to swich the context. 
     * The changes will be undone.
     * Also clears temporary scopes.
     * @param scopeIdentifier An identifier that represents the scope.
     * @return true if sucessfully switched, false if not.
     */
    public boolean switchContext(String scopeIdentifier){
        Map<String, Integer> tmpConst = localConstantScopes.get(scopeIdentifier);
        Map<String, Integer> tmpVar   = localVariableScopes.get(scopeIdentifier);

        if(tmpConst == null || tmpConst == null) return false;

        currentConstants = tmpConst;
        currentVariables = tmpVar;

        temporaryConstants = new ArrayList<>();
        temporaryVariables = new ArrayList<>();
        
        return true;
    }

    public boolean createLocalScope(String scopeIdentifier){
        if(localConstantScopes.containsKey(scopeIdentifier) || localVariableScopes.containsKey(scopeIdentifier)){
            return false;
        }
        localConstantScopes.put(scopeIdentifier, new HashMap<>());
        localVariableScopes.put(scopeIdentifier, new HashMap<>());

        return true;
    }

    public void enterTempScope(){
        temporaryConstants.add(new HashMap<>());
        temporaryVariables.add(new HashMap<>());
    }

    public void leaveTempScope(){
        temporaryConstants.remove(temporaryConstants.size()-1);
        temporaryVariables.remove(temporaryVariables.size()-1);
    }


    /**
     * Adds a a constant to the deepest current temporary scope.
     * This identifier is only usable in there and deeper scopes.
     * @param val The value of the constant.
     * @param identifier The identifier of the constant.
     * @return True if the constant was added successfully, false if not. 
     *         f.e. when a different constant allready exists with the same name.
     */
    public boolean addTemporaryConstant(String identifier, int val){
        if(get(identifier) != null) return false;

        temporaryConstants.get(temporaryVariables.size()-1)
            .put(identifier, val);

        return true;
    }

    public boolean addLocalConstant(String identifier, int val){
        if(get(identifier) != null) return false;

        currentConstants.put(identifier, val);
        return true;
    }

    public boolean addGlobalConstant(String identifier, String val){
        if(get(identifier) != null) return false;

        globalConstants.put(identifier, val);
        return true;
    }




    public int addLocalVariable(String identifier){
        int r = 0;
        if(get(identifier) != null) return -1;

        while(currentVariables.containsValue(r)){
            r++;
        }

        currentVariables.put(identifier, r);
        return r;
    }

    public int addTemporaryVariable(String identifier){
        int r = 0;
        boolean b;

        if(get(identifier) != null) return -1;

        while(true){
            b = false;
            for(int i = 0; i < temporaryVariables.size(); i++){
                b &= temporaryVariables.get(i).containsValue(r);
            }

            if (!b) break;

            r++;
        }

        return r;
    }

    public static enum Type{
        VARIABLE,
        CONSTANT;
    }
}