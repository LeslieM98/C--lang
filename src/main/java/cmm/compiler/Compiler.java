package cmm.compiler;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.*;
import jasmin.ClassFile;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.*;

import cmm.compiler.exception.CompileRuntimeException;
import cmm.compiler.generated.*;
import cmm.compiler.utillity.FunctionCallValidator;
import cmm.compiler.utillity.ScopeManager.Type;

public class Compiler{

    
    private boolean generateJasmin;
    private String programname;
    private Path infile;

    public Compiler(Path infile, boolean generateJasmin){
        this.infile = infile; 
        programname = resolveProgramName(infile);
        this.generateJasmin = generateJasmin;
    }

    public static String resolveProgramName(Path inFile){
        String file = inFile.getFileName().toString();
        return removeFilenameExtension(file);
    }
    
    private static String removeFilenameExtension(String file) {
    	int pos = file.lastIndexOf('.');
    	return file.substring(0, pos != -1 ? pos : file.length());
    }

    public void compile(){
        ParseTree pt = createParser(infile).program();
        ProgramVisitor v = new ProgramVisitor(programname);
        
        try {
            List<String> compiled = v.visit(pt);
        } catch (CompileRuntimeException e){
            System.err.println(e.getPreparedMessage());
            System.exit(1);
        }
        
        final List<String> asm = new ArrayList<>();

        //inserting public fields
        v.getGlobalVariables().stream()
            .filter(x -> x.getType() == Type.VARIABLE)
            .forEach(x -> asm.add(2, ".field public " + x.getValue() + " I"));

        // Validating
        FunctionCallValidator fcv = new FunctionCallValidator(
            asm, 
            ((ProgramVisitor) v).getDefinedFunctions()
        );
        
        List<String> errors = fcv.validate();
        if(!errors.isEmpty()){
            errors.forEach(System.err::println);
            return;
        }

        if(generateJasmin){
            writeJasmin(asm);
        } else {
            writeClass(asm);
        }
    }

    private boolean writeJasmin(List<String> asm){
        Path out = Paths.get(programname + ".j");
        try{
            Files.write(out, asm);
        } catch (IOException e){
            return false;
        }

        return true;
    }

    private boolean writeClass(List<String> asm){
        ClassFile cf = new ClassFile();
        String joined = String.join(System.lineSeparator(), asm);
        Path out = Paths.get(programname + ".class");
        try{
            cf.readJasmin( new StringReader(joined), programname, false);
            cf.write(Files.newOutputStream(
                out, 
                StandardOpenOption.WRITE, 
                StandardOpenOption.TRUNCATE_EXISTING, 
                StandardOpenOption.CREATE
            ));
        } catch (Exception e){
            return false;
        }
        return true;
    }
    
    /**
     * Takes in a String of C-- source code and returns a corresponding Parser.
     * @param input The C--. input sourcecode to parse.
     * @return A functioning parser or null if not functioning.
     */
    public static CmmParser createParser(Path input){
        try{
            CmmLexer tmpLex = new CmmLexer(CharStreams.fromPath(input));
            CommonTokenStream tmpTkStream = new CommonTokenStream(tmpLex);
            return new CmmParser(tmpTkStream);
        } catch (IOException e) {
        	System.err.println("Could not create parser from inputfile. Terminating...");
        	System.exit(2);
            return null;
        }
    }

    /**
     * Validates the code and throws corresponding exceptions if anything is not working correctly.
     */


}