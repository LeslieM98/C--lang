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
        evaluateArguments(args);
        comp.compile();
    }



    void evaluateArguments(String[] args){
        if(args.length > 2){
            System.out.println(HELP_MSG);
        } else if (args[0].equals("-j")){

            Path p = Paths.get(args[1]);

            if(Files.isReadable(p)){
                comp = new Compiler(p, true);
            } else {
                System.err.println("File not accessible");
                System.exit(1);
            }

        } else if(args[0].equals("--help")){
            System.out.println(HELP_MSG);
            System.exit(0);
        } else {
            Path p = Paths.get(args[0]);
            if(Files.isReadable(p)){
                comp = new Compiler(p, false);
            } else {
                System.err.println("File not accessible");
                System.exit(1);
            }
        }
    }

}