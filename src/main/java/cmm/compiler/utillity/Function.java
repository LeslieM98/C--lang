package cmm.compiler.utillity;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents metadata about function declarations
 * @author Leslie Marxen
 */

public class Function{
    private String identifier;
    private List<Pair<String, NativeTypes>> parameterList;
    private NativeTypes returnType;


    /**
     * Constructor
     * @param returnType The returntype of the defined funtction.
     * @param identifier The name of the function.
     * @param parameterList A parameterlist in correct order.
     */
    public Function(NativeTypes returnType, String identifier, List<Pair<String, NativeTypes>> parameterList){
        setIdentifier(identifier);
        this.parameterList = parameterList;
    }

    /**
     * Constructs a default function with returntype void and no parameters
     * @param identifier The name of the function.
     */
    public Function(String identifier){
        setIdentifier(identifier);
        parameterList = new ArrayList<>();
        returnType = NativeTypes.VOID;
    }

    /**
     * Constructor with vararg parameter.
     * @param returnType The returntype of the defined funtction.
     * @param identifier The name of the function.
     * @param parameterList A parameterlist in correct order.
     */
    public Function(NativeTypes returnType, String identifier, Pair<String, NativeTypes>... parameterList){
        setIdentifier(identifier);
        this.parameterList = new ArrayList<>();
        for(Pair<String, NativeTypes> i : parameterList){
            this.parameterList.add(i);
        }
    }


    /**
     * Setter for the identifier attribute.
     * @param identifier The new value.
     * @throws NullPointerException If identifier = null.
     * @throws IllegalStateException If identifier = "".
     */
    private void setIdentifier(String identifier){
        if(identifier == null){
            throw new NullPointerException("identifier cannot be null");
        }

        if(identifier.length() < 1){
            throw new IllegalStateException("Identifier cannot be a String of length < 1");
        }

        this.identifier = identifier;
    }

    /**
     * Setter for the returnType attribute.
     * @param returnType The new Value.
     * @throws NullPointerException If returnType = null.
     */
    private void setReturnType(NativeTypes returnType){
        if(returnType == null){
            throw new NullPointerException("Returntype cannot be null");
        }
        this.returnType = returnType;
    }

    /**
     * Getter for the identifier attribute.
     * @return Value of identifier.
     */
    public String getIdentifier(){
        return identifier;
    }

    /**
     * Getter for the returnType attribute.
     * @return Value of returnType.
     */
    public NativeTypes getReturnType(){
        return returnType;
    }

    /**
     * Returns the count of parameters this function has.
     * @return Count of parameters.
     */
    public int getParameterCount(){
        return parameterList.size();
    }

    /**
     * Returns metadata about the parameter at position i.
     * @param i Position number.
     * @return A pair of metadata.
     */
    public Pair<String, NativeTypes> getParameter(int i){
        return parameterList.get(i);
    }

    /**
     * Checks for equality.
     * Checks wether name, returntype and order/type of parameters are the same.
     * @return True if equal, false if not.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && getClass() == obj.getClass()) {
            Function o = (Function)obj;
            boolean b;
            b  = returnType == o.returnType;
            b &= identifier.equals(o.identifier);
            if(o.parameterList.size() != parameterList.size()){
                return false;
            }

            for(int i = 0; i < parameterList.size(); i++){
                b &= parameterList.get(i).getRight() == o.parameterList.get(i).getRight();
            }
            
            return b;
        }
        return false;
    }
}