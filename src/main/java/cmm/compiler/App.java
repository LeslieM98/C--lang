package cmm.compiler;

/**
 * @author Leslie Marxen
 * @version 1.0
 */

public class App{

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
        System.out.println("Hello World!");
    }

}