package cmm.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

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

import cmm.compiler.generated.*;
import cmm.compiler.utillity.*;
import cmm.compiler.utillity.ScopeManager.*;
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
        OutputStream os;
        try{
            os = Files.newOutputStream(tmpFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            cf.write(os);
            os.close();

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

        assertNull(s.get("a"));
        assertNull(s.get("b"));
        assertNull(s.get("c"));

        s.putConstant("a", "1");
        s.putConstant("b", "2");
        s.putConstant("c", "3");

        assertEquals("1", s.get("a").getValue());
        assertEquals("2", s.get("b").getValue());
        assertEquals("3", s.get("c").getValue());


        /* Test if local constants returns correct values. */
        Function f1 = new Function(NativeTypes.VOID, "foo", new Pair<>("a", NativeTypes.NUM));
        s.createLocalScope(f1);
        s.switchContext(f1);
        assertEquals(Scope.LOCAL, s.currentScope());
        assertEquals(s.currentTemporaryScopeDepth(), 0);

        s.putConstant("la", "4");
        s.putConstant("lb", "5");
        s.putConstant("lc", "6");

        assertEquals("4", s.get("la").getValue());
        assertEquals("5", s.get("lb").getValue());
        assertEquals("6", s.get("lc").getValue());

        // Test wether global constants still work in local context.
        assertEquals("1", s.get("a").getValue());
        assertEquals("2", s.get("b").getValue());
        assertEquals("3", s.get("c").getValue());


        assertEquals("1", s.get("a").getValue());
        assertEquals("2", s.get("b").getValue());
        assertEquals("3", s.get("c").getValue());

        // Cannot declare local constant twice
        assertFalse(s.putConstant("la", "99"));
        assertFalse(s.putConstant("lb", "99"));
        assertFalse(s.putConstant("lc", "99"));

        assertEquals("4", s.get("la").getValue());
        assertEquals("5", s.get("lb").getValue());
        assertEquals("6", s.get("lc").getValue());

        // Cannot declare local constant when a global constant with same identifier exists
        assertFalse(s.putConstant("a", "1"));
        assertFalse(s.putConstant("b", "2"));
        assertFalse(s.putConstant("c", "3"));

        assertEquals("1", s.get("a").getValue());
        assertEquals("2", s.get("b").getValue());
        assertEquals("3", s.get("c").getValue());

        /* Test temporary entering temporary scope */
        s.enterTemporaryScope();
        assertEquals(s.currentTemporaryScopeDepth(), 1);
        assertEquals(Scope.TEMPORARY, s.currentScope());

        
        s.putConstant("at", "7");
        s.putConstant("bt", "8");
        s.putConstant("ct", "9");

        assertEquals("7", s.get("at").getValue());
        assertEquals("8", s.get("bt").getValue());
        assertEquals("9", s.get("ct").getValue());


        // Test wether global constants still work in temporary context.
        assertEquals("1", s.get("a").getValue());
        assertEquals("2", s.get("b").getValue());
        assertEquals("3", s.get("c").getValue());

        // Test wether local constants still work in temporary context.
        assertEquals("4", s.get("la").getValue());
        assertEquals("5", s.get("lb").getValue());
        assertEquals("6", s.get("lc").getValue());


        /* Test temporary scope at depth 2 */
        s.enterTemporaryScope();
        assertEquals(s.currentTemporaryScopeDepth(), 2);

        s.putConstant("att", "10");
        s.putConstant("btt", "11");
        s.putConstant("ctt", "12");

        assertEquals("10", s.get("att").getValue());
        assertEquals("11", s.get("btt").getValue());
        assertEquals("12", s.get("ctt").getValue());

        // Test wether constants at depth 1 still work
        assertEquals("7", s.get("at").getValue());
        assertEquals("8", s.get("bt").getValue());
        assertEquals("9", s.get("ct").getValue());

        // Test wether global constants still work in temporary context.
        assertEquals("1", s.get("a").getValue());
        assertEquals("2", s.get("b").getValue());
        assertEquals("3", s.get("c").getValue());

        // Test wether local constants still work in temporary context.
        assertEquals("4", s.get("la").getValue());
        assertEquals("5", s.get("lb").getValue());
        assertEquals("6", s.get("lc").getValue());

        /* Test leaving scope at depth 2*/
        s.leaveTemporaryScope();
        assertEquals(Scope.TEMPORARY, s.currentScope());
        assertEquals(s.currentTemporaryScopeDepth(), 1);

        // Depth 2 not accessible anymore
        assertNull(s.get("att"));
        assertNull(s.get("btt"));
        assertNull(s.get("ctt"));

        // Depth 1 still accessible
        assertEquals("7", s.get("at").getValue());
        assertEquals("8", s.get("bt").getValue());
        assertEquals("9", s.get("ct").getValue());


        /* Reentering depth 2 does not use the same variables */
        s.enterTemporaryScope();
        assertEquals(2, s.currentTemporaryScopeDepth());

        assertNull(s.get("att"));
        assertNull(s.get("btt"));
        assertNull(s.get("ctt"));

        // Redeclaring tmp constants from an allready left tmp scope
        s.putConstant("att", "10");
        s.putConstant("btt", "11");
        s.putConstant("ctt", "12");

        assertEquals("10", s.get("att").getValue());
        assertEquals("11", s.get("btt").getValue());
        assertEquals("12", s.get("ctt").getValue());

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
        s.putConstant("la", "0");
        s.putConstant("lb", "0");
        s.putConstant("lc", "0");

        assertEquals("0", s.get("la").getValue());
        assertEquals("0", s.get("lb").getValue());
        assertEquals("0", s.get("lc").getValue());

    }

    @Test
    public void testLocalVariables() {
    	
    }
    
    @Test
    public void testEquality(){
        String input, expected, actual;

        // !=
        input = "void main(){println(1 != 2);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(2 != 2);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        // ==
        input = "void main(){println(1 == 2);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(2 == 2);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));
    }

    @Test
    public void testRelational(){
        String input, expected;
        
        // <
        input = "void main(){println(1 < 2);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(2 < 2);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(3 < 2);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        // >
        input = "void main(){println(1 > 2);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(2 > 2);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(3 > 2);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        // <=
        input = "void main(){println(1 <= 2);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(2 <= 2);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(3 <= 2);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        // >=
        input = "void main(){println(1 >= 2);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(2 >= 2);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(3 >= 2);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));
    }

    @Test
    public void testNot(){
        String input, expected;

        input = "void main(){println(!0);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(!1);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(!20);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));


    }
    

    @Test
    public void testAndOr(){
        String input, expected;
        
        // &&
        input = "void main(){println(1 && 1);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(1 && 0);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(0 && 0);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(0 && 1);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(-100 && 100);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(-100 && 0);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(100 && 0);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));


        input = "void main(){println(1 && 0 && 1);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(1 && 1 && 1);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(0 && 0 && 0);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        // ||
        input = "void main(){println(1 || 1);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(1 || 0);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(0 || 0);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(0 || 1);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(-100 || 100);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(-100 || 0);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(100 || 0);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));



        input = "void main(){println(1 || 0 || 1);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(1 || 1 || 1);}";
        expected = "1" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(0 || 0 || 0);}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));
    }

    @Test
    public void testBooleanExpression(){
        String input, expected;


        input = "void main(){println((1 < 2 || 1 > 2) && (2 >= 2 && (!(2 < 3))));}";
        expected = "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));

        input = "void main(){println(1 < 2);println(1 > 2);println(2 >= 2);println(2 < 3);println(!(2 < 3));println(1 < 2 || 1 > 2);println(2 >= 2 && (!(2 < 3)));println((1 < 2 || 1 > 2) && (2 >= 2 && (!(2 < 3))));}";
        expected = "1" + System.lineSeparator() + 
        "0" + System.lineSeparator() + 
        "1" + System.lineSeparator() + 
        "1" + System.lineSeparator() + 
        "0" + System.lineSeparator() + 
        "1" + System.lineSeparator() + 
        "0" + System.lineSeparator() + 
        "0" + System.lineSeparator();
        assertEquals(expected, runCmm(input));
    }
    
    @Test
    public void testPlus() {
    	String input, expected;
    	
    	input = "void main(){println(1 + 3);}";
    	expected = "4" + System.lineSeparator();
    	assertEquals(expected, runCmm(input));
    }
    
    @Test
    public void testMinus() {
    	String input, expected;
    	
    	input = "void main(){println(5 - 2);}";
    	expected = "3" + System.lineSeparator();
    	assertEquals(expected, runCmm(input));
    }
    
    @Test
    public void testDivision() {
    	String input, expected;
    	
    	input = "void main(){println(6 / 2);}";
    	expected = "3" + System.lineSeparator();
    	assertEquals(expected, runCmm(input));
    }
    
    @Test
    public void testMultiplication() {
    	String input, expected;
    	
    	input = "void main(){println(2 * 4);}";
    	expected = "8" + System.lineSeparator();
    	assertEquals(expected, runCmm(input));
    }

    public static void main(String[] args) {
        App a = new App();
        String[] arg = {"-j", "test.txt"};
        a.start(arg);
    }
}