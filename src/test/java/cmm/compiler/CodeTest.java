package cmm.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import cmm.compiler.utillity.*;
import cmm.compiler.utillity.ScopeManager.Type;
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
        ProgramVisitor v = new ProgramVisitor("TestAsm");
        
        String asm = String.join(System.lineSeparator(), v.visit(tree));   

        String output = runJasmin(asm);

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
        ProgramVisitor v = new ProgramVisitor("TestAsm");
        
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

    @Test
    public void testScopeManager(){
        ScopeManager s = new ScopeManager();

        /*
         * CONSTANTS
         */

        /* Test if global constants return correct value */
        s.addGlobalConstant("a", "1");
        s.addGlobalConstant("b", "2");
        s.addGlobalConstant("c", "3");

        assertNull(s.get("a"));
        assertNull(s.get("b"));
        assertNull(s.get("c"));

        assertEquals(new Pair<Type, String>(Type.CONSTANT, "1"), s.getGlobal("a"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "2"), s.getGlobal("b"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "3"), s.getGlobal("c"));


        /* Test if local constants returns correct values. */
        Function f1 = new Function(NativeTypes.VOID, "foo", new Pair<>("a", NativeTypes.NUM));
        s.createLocalScope(f1);
        s.switchContext(f1);
        s.inLocalScope();
        assertEquals(s.currentTempScopeDepth(), 0);

        s.addLocalConstant("la", 4);
        s.addLocalConstant("lb", 5);
        s.addLocalConstant("lc", 6);

        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 4), s.get("la"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 5), s.get("lb"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 6), s.get("lc"));

        // Test wether global constants still work in local context.
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "1"), s.getGlobal("a"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "2"), s.getGlobal("b"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "3"), s.getGlobal("c"));


        assertEquals(new Pair<Type, String>(Type.CONSTANT, "1"), s.getGlobal("a"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "2"), s.getGlobal("b"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "3"), s.getGlobal("c"));

        // Cannot declare local constant twice
        assertFalse(s.addLocalConstant("la", 99));
        assertFalse(s.addLocalConstant("lb", 99));
        assertFalse(s.addLocalConstant("lc", 99));

        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 4), s.get("la"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 5), s.get("lb"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 6), s.get("lc"));

        // Cannot declare local constant when a global constant with same identifier exists
        assertFalse(s.addLocalConstant("a", 1));
        assertFalse(s.addLocalConstant("b", 2));
        assertFalse(s.addLocalConstant("c", 3));

        assertEquals(new Pair<Type, String>(Type.CONSTANT, "1"), s.getGlobal("a"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "2"), s.getGlobal("b"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "3"), s.getGlobal("c"));

        /* Test temporary entering temporary scope */
        s.enterTempScope();
        assertEquals(s.currentTempScopeDepth(), 1);
        assertTrue(s.inLocalScope());

        s.addTemporaryConstant("at", 7);
        s.addTemporaryConstant("bt", 8);
        s.addTemporaryConstant("ct", 9);

        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 7), s.get("at"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 8), s.get("bt"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 9), s.get("ct"));


        // Test wether global constants still work in temporary context.
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "1"), s.getGlobal("a"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "2"), s.getGlobal("b"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "3"), s.getGlobal("c"));

        // Test wether local constants still work in temporary context.
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 4), s.get("la"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 5), s.get("lb"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 6), s.get("lc"));


        /* Test temporary scope at depth 2 */
        s.enterTempScope();
        assertTrue(s.inLocalScope());
        assertEquals(s.currentTempScopeDepth(), 2);

        s.addTemporaryConstant("att", 10);
        s.addTemporaryConstant("btt", 11);
        s.addTemporaryConstant("ctt", 12);

        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 10), s.get("att"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 11), s.get("btt"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 12), s.get("ctt"));

        // Test wether constants at depth 1 still work
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 7), s.get("at"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 8), s.get("bt"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 9), s.get("ct"));

        // Test wether global constants still work in temporary context.
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "1"), s.getGlobal("a"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "2"), s.getGlobal("b"));
        assertEquals(new Pair<Type, String>(Type.CONSTANT, "3"), s.getGlobal("c"));

        // Test wether local constants still work in temporary context.
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 4), s.get("la"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 5), s.get("lb"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 6), s.get("lc"));

        /* Test leaving scope at depth 2*/
        s.leaveTempScope();
        assertTrue(s.inLocalScope());
        assertEquals(s.currentTempScopeDepth(), 1);

        // Depth 2 not accessible anymore
        assertNull(s.get("att"));
        assertNull(s.get("btt"));
        assertNull(s.get("ctt"));

        // Depth 1 still accessible
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 7), s.get("at"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 8), s.get("bt"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 9), s.get("ct"));


        /* Reentering depth 2 does not use the same variables */
        s.enterTempScope();
        assertEquals(2, s.currentTempScopeDepth());

        assertNull(s.get("att"));
        assertNull(s.get("btt"));
        assertNull(s.get("ctt"));

        // Redeclaring tmp constants from an allready left tmp scope
        s.addTemporaryConstant("att", 10);
        s.addTemporaryConstant("btt", 11);
        s.addTemporaryConstant("ctt", 12);

        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 10), s.get("att"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 11), s.get("btt"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 12), s.get("ctt"));

        /* test if scopes of 2 functions don't crash */
        s.switchToGlobalContext();
        Function f2 = new Function(NativeTypes.VOID, "foo");
        s.createLocalScope(f2);
        s.switchContext(f2);

        // Locals from f1 not accessible
        assertNull(s.get("la"));
        assertNull(s.get("lb"));
        assertNull(s.get("lc"));

        // can create constants with the same name as f1
        s.addLocalConstant("la", 0);
        s.addLocalConstant("lb", 0);
        s.addLocalConstant("lc", 0);

        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 0), s.get("la"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 0), s.get("lb"));
        assertEquals(new Pair<Type, Integer>(Type.CONSTANT, 0), s.get("lc"));

    }

    @Test
    public void testEquality(){
        String input, expected, actual;

        // !=

        input = "void main(){println(1 != 2);}";
        expected = "1.0" + System.lineSeparator();
        actual = runCmm(input);
        assertEquals(expected, actual);
    }

    public static void main(String[] args) {
        App a = new App();
        String[] arg = {"-j", "test.txt"};
        a.start(arg);
    }
}