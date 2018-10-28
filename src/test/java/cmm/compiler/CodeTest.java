package cmm.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.*;
import java.io.PrintStream;
import java.net.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.junit.jupiter.api.*;

import cmm.compiler.exception.*;
import cmm.compiler.generated.*;

import jas.jasError;
import jasmin.ClassFile;



public class CodeTest{

    /**
     * Takes in jasmin assembly, assembles it, runs it and 
     * returns the output of System.out of the given code.
     * @param source Jasmin source code
     * @return Everything printed to System.out while the code was running.
     */
    private static String runJasmin(String source){
        ClassFile exec = new ClassFile();
        try{
            exec.readJasmin(new StringReader(source), "TestAsm", true); // Compiler Jasmin
        } catch (Exception e){
            return null;
        }

        String output = runClassFile(exec);

        return output;
    }

    /**
     * Takes in jasmin assembly, assembles it, runs it and 
     * returns the output of System.out of the given code.
     * @param source Jasmin source code
     * @return Everything printed to System.out while the code was running.
     */
    private static String runJasmin(Path source){
        List<String> asm;
        try{
            asm = Files.readAllLines(source);
        } catch (IOException e){
            asm = new ArrayList<>();
        }
        return runJasmin(String.join("", asm));
    }

    /**
     * Returns the produced output of the given classfile.
     * @param cf The Class that will be run.
     * @return The output given by the code. or null if the code was not able to run.
     */
    private static String runClassFile(ClassFile cf){

        // Redirect stdout
        PrintStream sysout = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(baos);
        System.setOut(capture);


        // write classfile to tmp file.
        String className = cf.getClassName();
        Path tmpFile = Paths.get(className + ".class");
        try{
            cf.write(Files.newOutputStream(tmpFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE));

            // invoke main via reflection
            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{tmpFile.toAbsolutePath().getParent().toUri().toURL()}, 
                Thread.currentThread().getContextClassLoader()
            );

            Class<?> cls = Class.forName(className, false, classLoader);
            Method m = cls.getMethod("main", String[].class);
            m.invoke(null, new String[1]);

        } catch (Exception e){
            return null;
        } finally {
            //Cleanup
            System.setOut(sysout);
            try{
                Files.delete(tmpFile);
            } catch(Exception e){
                return null;
            }
        }

        return baos.toString();
    } 

    /**
     * Takes in C-- sourcecode, compiles it to jasmin, assembles it, 
     * runs it and returns the output of System.out of the given code.
     * @param source C-- source code
     * @return Everything printed to System.out while the code was running.
     */
    public static String runCmm(String source){
        ParseTree tree = createParser(source).program();
        ProgramVisitor v = new ProgramVisitor();
        
        String asm = String.join(System.lineSeparator(), v.visit(tree));   

        String output = runJasmin(source);

        return output;
    }

    /**
     * Takes in C-- sourcecode, compiles it to jasmin, assembles it, 
     * runs it and returns the output of System.out of the given code.
     * @param source C-- source code
     * @return Everything printed to System.out while the code was running.
     */
    public static String runCmm(Path source){
        ParseTree tree = createParser(source).program();
        ProgramVisitor v = new ProgramVisitor();
        
        String asm = String.join(System.lineSeparator(), v.visit(tree));   

        String output = runJasmin(source);

        return output;
    }


    /**
     * Takes in a String of C-- source code and returns a corresponding Parser.
     * @param input The C--. input sourcecode to parse.
     * @return A functioning parser.
     */
    public static CmmParser createParser(String input){
        CmmLexer tmpLex = new CmmLexer(CharStreams.fromString(input));
        CommonTokenStream tmpTkStream = new CommonTokenStream(tmpLex);
        CmmParser tmpParser = new CmmParser(tmpTkStream);
        return tmpParser;
    }

    /**
     * Takes in a Path to a File containing C-- source code and returns a corresponding Parser.
     * @param input Path to a file containing C-- source code.
     * @return A functioning parser.
     */
    public static CmmParser createParser(Path input){
        CmmLexer tmpLex;
        try{
            tmpLex = new CmmLexer(CharStreams.fromPath(input));
        } catch (Exception e){
            return null;
        }
        CommonTokenStream tmpTkStream = new CommonTokenStream(tmpLex);
        CmmParser tmpParser = new CmmParser(tmpTkStream);
        return tmpParser;
    }


    @Test
    public void testAsmInput(){
        String asm = new StringBuilder()
            .append(".class public TestAsm" + System.lineSeparator())
            .append(".super java/lang/Object" + System.lineSeparator())
            .append(System.lineSeparator())
            .append(".method public static main([Ljava/lang/String;)V" + System.lineSeparator())
            .append(".limit stack 2" + System.lineSeparator())
            .append("getstatic java/lang/System/out Ljava/io/PrintStream;" + System.lineSeparator())
            .append("ldc \"Hello world!\"" + System.lineSeparator())
            .append("invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V" + System.lineSeparator())
            .append("return" + System.lineSeparator())
            .append(".end method" + System.lineSeparator())
                .toString();

        String output = runJasmin(asm);

        Assertions.assertEquals(output, "Hello world!" + System.lineSeparator());
    }

    @Test
    public void testConstants(){

        // // Test if a single constant does not throw an error.
        // String inputString = "const num test = 20;";
        // ParseTree tree = createParser(inputString).program();
        // ProgramVisitor v = new ProgramVisitor();
        // try{
        //     v.visit(tree);
        // } catch (Exception e){
        //     Assertions.fail("Exception was thrown" + e.getMessage());
        // }

        // // Test if 2 different constants with the same identifier throw an error.
        // boolean exThrown = false;
        // inputString = "const num test = 20; const num test = 21;";
        // tree = createParser(inputString).program();
        // v = new ProgramVisitor();
        // try{
        //     v.visit(tree);
        // } catch (Exception e){
        //     exThrown = true;
        // }
        // Assertions.assertTrue(exThrown);

        // // Test if 2 different constants with different identifiers do not throw an error.
        // inputString = "const num test = 20; const num test1 = 21;";
        // tree = createParser(inputString).program();
        // v = new ProgramVisitor();
        // try{
        //     v.visit(tree);
        // } catch (Exception e){
        //     Assertions.fail("Exception was thrown" + e.getMessage());
        // }

    }

    public static void main(String[] args) {
        ParseTree tree = createParser(Paths.get("test.txt")).program();
        ProgramVisitor v = new ProgramVisitor();
        v.visit(tree);
    }
}