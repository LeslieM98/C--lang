package cmm.compiler.utillity;

import java.util.*;

import org.antlr.v4.runtime.misc.Pair;

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

    private Map<String, Integer> currentConstants;    // Acts as pointer
    private Map<String, Integer> currentVariables;    // Acts as plointer

    private List<Map<String, Integer>> temporaryConstants;
    private List<Map<String, Integer>> temporaryVariables;

    private Map<String, Map<String, Integer>> localConstantScopes;
    private Map<String, Map<String, Integer>> localVariableScopes;



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

    
    /**
     * Checks if given variable/constant is usable in the current Scope.
     * Returns the {@link Type} of the identifier or null.
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
     * Returns a Pair of &lt;{@link Type}, Integer&gt; containing information about the identifier.
     * Checks if the given identifier is accessible from the current scope.
     * Returns null if not accessible.
     * This method accounts for Local and Temporary scopes.
     * @param identifier The identifier of either a Var or Const.
     * @return Information about given identifier, or null if not accessible.
     */
    public Pair<Type, Integer> get(String identifier){
        Pair<Type, Integer> ret = null;

        ret = getLocal(identifier);
        if(ret != null) return ret;

        ret = getTemporary(identifier);
        return ret;
    }

    /**
     * Returns a Pair of &lt;{@link Type}, String&gt; containing information about the identifier.
     * Checks if the given identifier is accessible from the current Scope
     * Returns null if not accessible.
     * This method only accounts for global scopes.
     * @param identifier The identifier of either a Var or Const.
     * @return Information about given identifier, or null if not accessible.
     */
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

    /**
     * Returns a Pair of &lt;{@link Type}, Integer&gt; containing information about the identifier.
     * Checks if the given identifier is accessible from the current Scope
     * Returns null if not accessible.
     * This method only accounts for local scopes.
     * @param identifier The identifier or either a var or const.
     * @return Information about given identifier or null if not accessible.
     */
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

    /**
     * Returns a Pair of &lt;{@link Type}, Integer&gt; containing information about the identifier.
     * Checks if the given identifier is accessible from the current Scope
     * Returns null if not accessible.
     * This method only accounts for temporary scopes.
     * @param identifier The identifier or either a var or const.
     * @return Information about given identifier or null if not accessible.
     */
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

    /**
     * Creates a new lookuptable for a local scope.
     * Returns true if succesfully created or false if 
     * a scope with the same name allready exists.
     * @param scopeIdentifier A string that identifies the new scope.
     * @return True if succesfully created, false if otherwise.
     */
    public boolean createLocalScope(String scopeIdentifier){
        if(localConstantScopes.containsKey(scopeIdentifier) || localVariableScopes.containsKey(scopeIdentifier)){
            return false;
        }
        localConstantScopes.put(scopeIdentifier, new HashMap<>());
        localVariableScopes.put(scopeIdentifier, new HashMap<>());

        return true;
    }

    /**
     * Creates a new temporary scope and steps into it.
     */
    public void enterTempScope(){
        temporaryConstants.add(new HashMap<>());
        temporaryVariables.add(new HashMap<>());
    }

    /**
     * Leaves the deepest current temporary scope and deletes it.
     */
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

    /**
     * Adds a constant to the local scope.
     * Inserts a value with the given identifier to the current local scope.
     * Also checks if the identifier was defined already in the current scope.
     * @param identifier A string to identifiy the constant.
     * @param val The value of the set constant.
     * @return true if sucessfully added, false if otherwise.
     */
    public boolean addLocalConstant(String identifier, int val){
        if(get(identifier) != null) return false;

        currentConstants.put(identifier, val);
        return true;
    }

    /**
     * Adds a constant to the local scope.
     * Inserts a value with the given identifier to the global scope.
     * Also checks if the identifier was defined already in the current scope.
     * @param identifier A string to identifiy the constant.
     * @param val The value of the set constant.
     * @return true if sucessfully added, false if otherwise.
     */
    public boolean addGlobalConstant(String identifier, String val){
        if(get(identifier) != null) return false;

        globalConstants.put(identifier, val);
        return true;
    }



    /**
     * Adds a variable to the current local scope.
     * Also finds the next free spot in the locals array.
     * Returns either a new spot or the old spot if the 
     * variable allready exists.
     * This function returns a negative value if it's somehow 
     * not possible to create a new local variable f.e. another 
     * variable with the same identifier is allready existing.
     * @param identifier The name of the variable.
     * @return The fixed spot in the locals array. Or negative val if no spot is available.
     */
    public int addLocalVariable(String identifier){
        int r = 0;

        if(get(identifier) != null) return -1;
        if(getGlobal(identifier) != null) return -2;

        while(currentVariables.containsValue(r)){
            r++;
        }

        currentVariables.put(identifier, r);
        return r;
    }

    /**
     * Adds a variable to the current deepest temporary scope.
     * Also dynamically finds the next free spot in the locals array.
     * This returned integer can change depending on how 
     * other local variables are stored.
     * The new variable is stored in an 
     * efficient manner inside the locals array.
     * This function returns a negative value if another variable
     * exists with the same name or if it is somehow not possible 
     * to assign a local spot to the variabls
     * @param identifier the name of the variable.
     * @return The dynamic spot in the locals array.
     */
    public int addTemporaryVariable(String identifier){
        int r = 0;
        boolean b;

        if(get(identifier) != null) return -1;
        if(getGlobal(identifier) != null) return 2;

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
}