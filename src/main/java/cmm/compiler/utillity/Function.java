package cmm.compiler.utillity;

import java.lang.annotation.Native;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.misc.*;

public class Function{
    private String identifier;
    private List<Pair<String, NativeTypes>> parameterList;
    private NativeTypes returnType;


    public Function(NativeTypes returnType, String identifier, List<Pair<String, NativeTypes>> parameterList){
        setIdentifier(identifier);
        this.parameterList = parameterList;
    }

    public Function(String identifier){
        setIdentifier(identifier);
        parameterList = new ArrayList<>();
    }

    public Function(NativeTypes returnType, String identifier, Pair<String, NativeTypes>... parameterList){
        setIdentifier(identifier);
        this.parameterList = new ArrayList<>();
        for(Pair<String, NativeTypes> i : parameterList){
            this.parameterList.add(i);
        }
    }


    private void setIdentifier(String identifier){
        if(identifier == null){
            throw new NullPointerException("identifier cannot be null");
        }

        if(identifier.length() < 1){
            throw new IllegalStateException("Identifier cannot be a String of length < 1");
        }

        this.identifier = identifier;
    }

    private void setReturnType(NativeTypes returnType){
        if(returnType == null){
            throw new NullPointerException("Returntype cannot be null");
        }
        this.returnType = returnType;
    }

    public String getIdentifier(){
        return identifier;
    }

    public NativeTypes getReturnType(){
        return returnType;
    }

    public int getParameterCount(){
        return parameterList.size();
    }

    public Pair<String, NativeTypes> getParameter(int i){
        return parameterList.get(i);
    }

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
                b &= parameterList.get(i).b == o.parameterList.get(i).b;
            }
            
            return b;
        }
        return false;
    }
}