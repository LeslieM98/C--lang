package cmm.compiler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

/**
 * @author Leslie Marxen
 * @version 1.0
 */

public class App{

    private Compiler comp;

    public static final String HELP_MSG = "Use following format:\n" + 
                                          "\t- cmmcomp <sourcefile> to compile\n" + 
                                          "\t- cmmcomp -j <sourcefile> to output Jasmin code" +
                                          "\t- cmmcomp --help to display this message";


    public App(){
        comp = null;
    }

    /**
     * Program entry.
     * @param args Command line 
    */
    public static void main(String[] args){
        App instance = new App();
        instance.start(args);
    }

    /**
     * Actual program logic.
     * @param args contains command line arguments just like in a main method
     */
    public void start(String[] args){
        if(!evaluateArguments(args)){
            System.exit(1);
        }
        comp.compile();
    }



    boolean evaluateArguments(String[] args){
        if(args.length > 2 || args.length == 0){
            System.out.println(HELP_MSG);
            return false;
        } else if (args[0].equals("-j")){

            
            Path p = Paths.get(args[1]);

            if(Files.isReadable(p)){
                comp = new Compiler(p, true);
                return true;
            } else {
                System.err.println("File not accessible");
                return false;
            }
        } else if(args[0].equals("--help")){
            System.out.println(HELP_MSG);
            System.exit(0);
            return true;
        } else {
            Path p = Paths.get(args[0]);
            if(Files.isReadable(p)){
                comp = new Compiler(p, false);
                return true;
            } else {
                System.err.println("File not accessible");
                return false;
            }
        }
    }

}