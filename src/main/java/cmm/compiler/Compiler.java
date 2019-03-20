package cmm.compiler;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.*;
import jasmin.ClassFile;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.*;

import cmm.compiler.generated.*;

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
        int pos = file.lastIndexOf('.');
        file = file.substring(
            0,
            pos != -1 ? pos : file.length()
        );

        return file;
    }

    public void compile(){
        ParseTree pt = createParser(infile).program();
        ProgramVisitor v = new ProgramVisitor(programname);
        List<String> asm = v.visit(pt);
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
            CmmParser tmpParser = new CmmParser(tmpTkStream);
            return tmpParser;
        } catch (IOException e){
            return null;
        }
    }



}