package cmm.compiler;

import cmm.parser.CmmBaseVisitor;

import java.util.ArrayList;
import java.util.List;

class CmmVisitor extends CmmBaseVisitor<List<String>>{


    public CmmVisitor(){
        super();
    }

    @Override
    protected List<String> defaultResult() {
        return new ArrayList<>();
    }

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

}